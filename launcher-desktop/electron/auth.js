/* Microsoft / Xbox Live authentication for Minecraft.
   Two Microsoft flows are supported: the device code flow (headless, used by the
   Java launcher path) and an authorization-code + PKCE flow that opens a real
   sign-in window. Both end in finishAuth and produce the same session. */

import crypto from 'node:crypto';
import http from 'node:http';

const AUTHORIZE_URL = 'https://login.microsoftonline.com/consumers/oauth2/v2.0/authorize';
const DEVICE_CODE_URL = 'https://login.microsoftonline.com/consumers/oauth2/v2.0/devicecode';
const TOKEN_URL = 'https://login.microsoftonline.com/consumers/oauth2/v2.0/token';
const XBOX_AUTH_URL = 'https://user.auth.xboxlive.com/user/authenticate';
const XSTS_URL = 'https://xsts.auth.xboxlive.com/xsts/authorize';
const MC_LOGIN_URL = 'https://api.minecraftservices.com/authentication/login_with_xbox';
const MC_PROFILE_URL = 'https://api.minecraftservices.com/minecraft/profile';
const SCOPE = 'XboxLive.signin offline_access';
const USER_AGENT = 'MixClient/1.0';

const sleep = (ms) => new Promise((resolve) => setTimeout(resolve, ms));

function formBody(form) {
  return Object.entries(form)
    .map(([key, value]) => `${encodeURIComponent(key)}=${encodeURIComponent(value)}`)
    .join('&');
}

async function request(url, { method = 'GET', body, contentType, headers = {}, signal } = {}) {
  const response = await fetch(url, {
    method,
    signal,
    headers: {
      'User-Agent': USER_AGENT,
      Accept: 'application/json',
      ...(contentType ? { 'Content-Type': contentType } : {}),
      ...headers
    },
    body
  });
  const text = await response.text();
  let json = {};
  try {
    json = text ? JSON.parse(text) : {};
  } catch {
    json = { raw: text };
  }
  return { status: response.status, json };
}

function postForm(url, form, signal, headers) {
  return request(url, {
    method: 'POST',
    body: formBody(form),
    contentType: 'application/x-www-form-urlencoded',
    signal,
    headers
  });
}

function postJson(url, body, headers = {}, signal) {
  return request(url, {
    method: 'POST',
    body: JSON.stringify(body),
    contentType: 'application/json',
    headers,
    signal
  });
}

export async function requestDeviceCode(clientId, signal) {
  const { status, json } = await postForm(DEVICE_CODE_URL, { client_id: clientId, scope: SCOPE }, signal);
  if (status >= 400 || !json.device_code) {
    throw new Error(json.error_description || json.error || `Microsoft device code failed (HTTP ${status})`);
  }
  return {
    deviceCode: json.device_code,
    userCode: json.user_code,
    verificationUri: json.verification_uri,
    interval: Number(json.interval) || 5,
    expiresIn: Number(json.expires_in) || 900
  };
}

function xstsError(json) {
  const code = Number(json.XErr);
  if (code === 2148916233) {
    return 'No Xbox account linked to this Microsoft account. Create one at xbox.com first.';
  }
  if (code === 2148916235) {
    return 'Xbox Live is not available in this region.';
  }
  if (code === 2148916238) {
    return 'This is a child account. Add it to a Microsoft family first.';
  }
  return Number.isFinite(code) ? `XSTS error ${code}` : 'XSTS authorization failed.';
}

function canonicalUuid(id) {
  if (!id) {
    return null;
  }
  const value = id.replace(/-/g, '');
  if (value.length !== 32) {
    return id;
  }
  return `${value.slice(0, 8)}-${value.slice(8, 12)}-${value.slice(12, 16)}-${value.slice(16, 20)}-${value.slice(20)}`;
}

/* Exchange a Microsoft token response for a Minecraft session. */
async function finishAuth(tokenResponse, { fallbackName, fallbackUuid, status, signal } = {}) {
  const msAccessToken = tokenResponse.access_token;
  const refreshToken = tokenResponse.refresh_token ?? null;
  if (!msAccessToken) {
    throw new Error('Microsoft returned no access token.');
  }

  status?.('Authenticating with Xbox Live…');
  const xbl = await postJson(
    XBOX_AUTH_URL,
    {
      Properties: {
        AuthMethod: 'RPS',
        SiteName: 'user.auth.xboxlive.com',
        RpsTicket: `d=${msAccessToken}`
      },
      RelyingParty: 'http://auth.xboxlive.com',
      TokenType: 'JWT'
    },
    {},
    signal
  );
  const xblToken = xbl.json.Token;
  const userHash = xbl.json?.DisplayClaims?.xui?.[0]?.uhs;
  if (!xblToken || !userHash) {
    throw new Error(`Xbox Live rejected the token (HTTP ${xbl.status}).`);
  }

  status?.('Authorizing with XSTS…');
  const xsts = await postJson(
    XSTS_URL,
    {
      Properties: { SandboxId: 'RETAIL', UserTokens: [xblToken] },
      RelyingParty: 'rp://api.minecraftservices.com/',
      TokenType: 'JWT'
    },
    {},
    signal
  );
  if (!xsts.json.Token) {
    const detail = (xsts.json.raw ?? JSON.stringify(xsts.json) ?? '').slice(0, 300);
    throw new Error(`${xstsError(xsts.json)} (HTTP ${xsts.status}) ${detail}`);
  }

  status?.('Logging into Minecraft services…');
  const login = await postJson(
    MC_LOGIN_URL,
    { identityToken: `XBL3.0 x=${userHash};${xsts.json.Token}` },
    {},
    signal
  );
  if (!login.json.access_token) {
    /* 403 here is almost always a policy answer, not a transport error, so the
       body is the only thing that says which one. */
    const detail = (login.json.raw ?? JSON.stringify(login.json) ?? '').slice(0, 400);
    /* Mojang allowlists every client ID before api.minecraftservices.com will
       redeem an XSTS token for it. A fresh Azure app is not on that list no
       matter how correct the registration is, so say that instead of the raw
       "Invalid app registration" string. */
    if (/Invalid app registration/i.test(detail)) {
      throw new Error(
        'Mix Client is not approved for Minecraft sign-in yet. Mojang allowlists each app ID ' +
        'before api.minecraftservices.com accepts it. Apply at https://aka.ms/mce-reviewappid with ' +
        'this client ID: ' + (tokenResponse.client_id ?? 'unknown')
      );
    }
    throw new Error(`Minecraft login rejected (HTTP ${login.status}): ${detail}`);
  }

  const expiresIn = Number(login.json.expires_in) || 86400;
  const session = {
    accessToken: login.json.access_token,
    refreshToken,
    expiresAt: Math.floor(Date.now() / 1000) + expiresIn,
    name: fallbackName ?? null,
    uuid: fallbackUuid ?? null,
    clientId: tokenResponse.client_id ?? null
  };

  try {
    const profile = await request(MC_PROFILE_URL, {
      headers: { Authorization: `Bearer ${session.accessToken}` },
      signal
    });
    if (profile.json?.id && profile.json?.name) {
      session.uuid = canonicalUuid(profile.json.id);
      session.name = profile.json.name;
    }
  } catch {
    /* keep the fallbacks */
  }

  session.name ??= 'Player';
  return session;
}

/* Device code login. onCode fires as soon as the user code is known. */
export async function login(clientId, { onCode, status, signal } = {}) {
  if (!clientId) {
    throw new Error('Microsoft sign-in needs an Azure client ID. Create a free app registration at portal.azure.com, add http://localhost as a SPA platform, then paste the Application (client) ID into Settings.');
  }
  const device = await requestDeviceCode(clientId, signal);
  onCode?.(device);

  const deadline = Date.now() + device.expiresIn * 1000;
  let interval = Math.max(2, device.interval);
  while (Date.now() < deadline) {
    if (signal?.aborted) {
      throw new Error('Sign-in cancelled.');
    }
    await sleep(interval * 1000);
    const { json } = await postForm(
      TOKEN_URL,
      {
        grant_type: 'urn:ietf:params:oauth:grant-type:device_code',
        client_id: clientId,
        device_code: device.deviceCode
      },
      signal
    );
    if (json.access_token) {
      return finishAuth({ ...json, client_id: clientId }, { status, signal });
    }
    if (json.error === 'authorization_pending') {
      continue;
    }
    if (json.error === 'slow_down') {
      interval += 5;
      continue;
    }
    if (json.error === 'expired_token') {
      throw new Error('The device code expired, start again.');
    }
    throw new Error(json.error_description || json.error || 'Microsoft login failed.');
  }
  throw new Error('Microsoft login timed out.');
}

/* ---------------- authorization code + PKCE in a window ---------------- */

const base64url = (buffer) => buffer.toString('base64').replace(/\+/g, '-').replace(/\//g, '_').replace(/=+$/, '');

const RESULT_PAGE = (ok) => `<!doctype html><html><head><meta charset="utf-8"><title>Mix Client</title></head>
<body style="margin:0;display:grid;place-items:center;height:100vh;background:#131314;color:#ffffff;font-family:Milos,sans-serif">
<div style="text-align:center"><h2 style="margin:0 0 6px">${ok ? 'Signed in' : 'Sign-in failed'}</h2>
<p style="margin:0;color:#9a9a9e;font-size:14px">${ok ? 'You can close this window and go back to Mix Client.' : 'You can close this window and try again.'}</p></div>
</body></html>`;

/**
 * Signs in by opening a Microsoft login window.
 *
 * <p>Uses OAuth 2.0 authorization code with PKCE against a loopback redirect, so
 * no client secret is ever stored and the flow works for a public client. The
 * window is a plain Electron BrowserWindow: the player signs in on Microsoft's
 * own page and Mix Client only ever sees the authorization code handed back to
 * localhost.
 */
export async function loginWindow(clientId, { BrowserWindow, onStatus, signal } = {}) {
  if (!clientId) {
    throw new Error('Microsoft sign-in needs an Azure client ID. Create a free app registration at portal.azure.com, add http://localhost as a SPA platform, then paste the Application (client) ID into Settings.');
  }
  if (typeof BrowserWindow !== 'function') {
    throw new Error('Sign-in window is only available inside the launcher.');
  }

  const verifier = base64url(crypto.randomBytes(32));
  const challenge = base64url(crypto.createHash('sha256').update(verifier).digest());

  const server = http.createServer();
  await new Promise((resolve, reject) => {
    server.once('error', reject);
    server.listen(0, '127.0.0.1', resolve);
  });
  const { port } = server.address();
  /* Azure matches the registered SPA redirect URI by scheme/host/path and ignores
   * the port for localhost, so the redirect has to land on "/" -- registering
   "http://localhost" and sending "/callback" is what produced
   AADSTS50011 "redirect_uri does not match". */
  const redirectUri = `http://localhost:${port}/`;

  const authorize = new URL(AUTHORIZE_URL);
  authorize.searchParams.set('client_id', clientId);
  authorize.searchParams.set('response_type', 'code');
  authorize.searchParams.set('redirect_uri', redirectUri);
  authorize.searchParams.set('response_mode', 'query');
  authorize.searchParams.set('scope', SCOPE);
  authorize.searchParams.set('code_challenge', challenge);
  authorize.searchParams.set('code_challenge_method', 'S256');
  authorize.searchParams.set('prompt', 'select_account');

  let settled = false;
  let resolveCode;
  let rejectCode;
  const codePromise = new Promise((resolve, reject) => {
    resolveCode = resolve;
    rejectCode = reject;
  });

  server.on('request', (req, res) => {
    let url;
    try {
      url = new URL(req.url, `http://localhost:${port}`);
    } catch {
      res.writeHead(400); res.end(); return;
    }
    /* the redirect lands on "/" (see redirectUri); accept "/callback" too so an
       older registration keeps working */
    if (url.pathname !== '/' && url.pathname !== '/callback') {
      res.writeHead(404); res.end(); return;
    }
    const code = url.searchParams.get('code');
    const error = url.searchParams.get('error');
    res.writeHead(200, { 'Content-Type': 'text/html; charset=utf-8' });
    res.end(RESULT_PAGE(Boolean(code)));
    if (settled) return;
    settled = true;
    if (code) resolveCode(code);
    else rejectCode(new Error(url.searchParams.get('error_description') || error || 'Microsoft sign-in failed.'));
  });

  const win = new BrowserWindow({
    width: 480,
    height: 660,
    title: 'Sign in with Microsoft',
    autoHideMenuBar: true,
    backgroundColor: '#131314',
    webPreferences: { nodeIntegration: false, contextIsolation: true }
  });

  const close = () => {
    server.close();
    if (!win.isDestroyed()) win.close();
  };

  win.on('closed', () => {
    server.close();
    if (!settled) {
      settled = true;
      rejectCode(new Error('Sign-in cancelled.'));
    }
  });

  signal?.addEventListener('abort', () => {
    if (!settled) {
      settled = true;
      rejectCode(new Error('Sign-in cancelled.'));
    }
    close();
  }, { once: true });

  onStatus?.('Waiting for Microsoft sign-in…');
  try {
    await win.loadURL(authorize.toString());
  } catch (error) {
    /* The callback closes the window mid-navigation, which makes loadURL reject
       with ERR_ABORTED. That is the success path, not a failure: the code is
       already waiting on codePromise. */
    const message = String(error?.message ?? error);
    if (!/ERR_ABORTED| ERR_ABORTED|window was closed|Navigation is not allowed/i.test(message)) {
      server.close();
      throw error;
    }
  }

  let code;
  try {
    code = await codePromise;
  } finally {
    close();
  }

  /* No Origin header: a Native app registration rejects cross-origin redemption
     unless the origin is on the AllowedOriginForNativeAppCorsRequestInOAuthToken
     allow list (AADSTS90023), and the Native client type is what Minecraft
   sign-in requires. A SPA registration would need the header instead, but SPA is
   rejected later with "Invalid app registration". */
  const { json } = await postForm(TOKEN_URL, {
    grant_type: 'authorization_code',
    client_id: clientId,
    code,
    redirect_uri: redirectUri,
    code_verifier: verifier
  }, signal);

  if (!json.access_token) {
    throw new Error(json.error_description || json.error || 'Microsoft login failed.');
  }
  return finishAuth({ ...json, client_id: clientId }, { status: onStatus, signal });
}

export async function refreshSession(old, clientId, signal) {
  if (!old?.refreshToken) {
    throw new Error('No Microsoft refresh token, sign in again.');
  }
  const id = clientId || old.clientId;
  const { status, json } = await postForm(
    TOKEN_URL,
    { grant_type: 'refresh_token', client_id: id, refresh_token: old.refreshToken, scope: SCOPE },
    signal
  );
  if (!json.access_token) {
    throw new Error(json.error_description || json.error || 'Could not refresh the session.');
  }
  return finishAuth({ ...json, client_id: id }, { fallbackName: old.name, fallbackUuid: old.uuid, signal });
}

/* Returns a session that is valid for at least five more minutes. */
export async function ensureSession(session, clientId) {
  if (!session?.accessToken) {
    return null;
  }
  if (session.expiresAt - 300 > Math.floor(Date.now() / 1000)) {
    return session;
  }
  return refreshSession(session, clientId);
}
