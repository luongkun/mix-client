import { app, BrowserWindow, dialog, ipcMain, safeStorage, shell } from 'electron';
import { spawn } from 'node:child_process';
import crypto from 'node:crypto';
import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';
import * as auth from './auth.js';
import * as forge from './forge.js';
import * as launcherUpdate from './launcher-update.js';
import * as minecraft from './minecraft.js';
import * as mods from './mods.js';
import * as skins from './skins.js';
import * as updater from './update.js';

const here = path.dirname(fileURLToPath(import.meta.url));
const projectRoot = path.resolve(here, '..', '..');

/* Azure app registration shipped with the launcher, so Microsoft sign-in works
   for everyone without any setup. It is a public client: PKCE only, no secret. */
const DEFAULT_MS_CLIENT_ID = '9b19dce6-ef75-4294-8640-00ad95dd7e35';

const DEFAULTS = {
  ramMb: 2048,
  javaPath: 'java',
  closeOnLaunch: false,
  activeProfile: 'Default',
  version: '1.8.9',
  branch: 'RELEASE 1.8.0',
  /* exactly one loader runs: 'forge' loads mods and 'optifine' replaces the
     render pipeline without any mod support. OptiFine is the default because
     it ships inside the build; Forge needs its first-run download. */
  loader: 'optifine',
  username: 'Player',
  msClientId: DEFAULT_MS_CLIENT_ID,
  updateUrl: '',
  /* every start looks for a newer client build and installs it in place */
  autoUpdate: true,
  /* the launcher app updates itself the same way: staged, then swapped when
     the app closes */
  autoUpdateLauncher: true,
  /* one profile, created fresh: the old built-in trio is gone and the player's
     settings live in launcher.json under this name */
  profiles: [{ name: 'Default', note: 'Balanced settings, every visual module available.' }]
};

/* Profiles earlier builds shipped on every install. They are not the player's
   own, so an upgrade drops them and starts from a single Default again. */
const LEGACY_PROFILES = ['Default', 'PvP', 'Cinematic'];

/* A distribution build can bake the manifest in, so the player never has to
   type a URL: `MIXCLIENT_UPDATE_URL=https://… npm run dist:win`. */
const BAKED_UPDATE_URL = (process.env.MIXCLIENT_UPDATE_URL ?? '').trim();

let win = null;
let game = null;
let loginController = null;
let autoUpdateRunning = false;
let autoUpdatePending = false;
let restarting = false;

const stateFile = () => path.join(app.getPath('userData'), 'launcher.json');
const sessionFile = () => path.join(app.getPath('userData'), 'session.bin');
/* staged launcher downloads live next to the state, not in the install folder:
   the install folder is read-only for a per-machine installation */
const updateWorkDir = () => path.join(app.getPath('userData'), 'update');

/* Older builds stored two booleans (`optifine`, `forge`) and could sit on the
   `forge-optifine` combo that no longer exists. Collapse them into the single
   loader the Versions page switches, so an existing launcher.json keeps
   launching the game the player expects. */
function normalizeLoader(state) {
  if (state.loader === 'forge' || state.loader === 'optifine') {
    return state.loader;
  }
  if (state.loader === 'forge-optifine') {
    return 'forge';
  }
  return state.forge === true ? 'forge' : 'optifine';
}

function normalizeProfiles(state) {
  const profiles = Array.isArray(state.profiles)
    ? state.profiles.filter((profile) => profile && typeof profile.name === 'string')
    : [];
  const onlyLegacy = profiles.length > 0 && profiles.every((profile) => LEGACY_PROFILES.includes(profile.name));
  const next = onlyLegacy || profiles.length === 0 ? DEFAULTS.profiles : profiles;
  const activeProfile = next.some((profile) => profile.name === state.activeProfile)
    ? state.activeProfile
    : next[0].name;
  return { profiles: next, activeProfile };
}

function readState() {
  try {
    const raw = JSON.parse(fs.readFileSync(stateFile(), 'utf8'));
    const merged = { ...DEFAULTS, ...raw };
    merged.loader = normalizeLoader(merged);
    Object.assign(merged, normalizeProfiles(merged));
    /* the two loader booleans are gone from the model; do not carry them on */
    delete merged.optifine;
    delete merged.forge;
    /* a migration has to stick, or the removed profiles come back on the next
       start; a write failure must not keep the launcher from opening */
    if (JSON.stringify(raw) !== JSON.stringify(merged)) {
      try {
        fs.mkdirSync(path.dirname(stateFile()), { recursive: true });
        fs.writeFileSync(stateFile(), JSON.stringify(merged, null, 2), 'utf8');
      } catch {
        /* read-only user data: the normalised in-memory state still works */
      }
    }
    return merged;
  } catch {
    return { ...DEFAULTS };
  }
}

function writeState(patch) {
  const next = { ...readState(), ...patch };
  fs.mkdirSync(path.dirname(stateFile()), { recursive: true });
  fs.writeFileSync(stateFile(), JSON.stringify(next, null, 2), 'utf8');
  return next;
}

/* The Minecraft session token is worth protecting: use the OS keystore when
   Electron exposes one, otherwise fall back to plain JSON. */
function saveSession(session) {
  if (!session) {
    fs.rmSync(sessionFile(), { force: true });
    return;
  }
  const json = JSON.stringify(session);
  const payload = safeStorage.isEncryptionAvailable()
    ? safeStorage.encryptString(json)
    : Buffer.from(json, 'utf8');
  fs.mkdirSync(path.dirname(sessionFile()), { recursive: true });
  fs.writeFileSync(sessionFile(), payload);
}

function readSession() {
  try {
    const payload = fs.readFileSync(sessionFile());
    let json;
    try {
      json = safeStorage.isEncryptionAvailable() ? safeStorage.decryptString(payload) : payload.toString('utf8');
    } catch {
      json = payload.toString('utf8');
    }
    return JSON.parse(json);
  } catch {
    return null;
  }
}

function distDir() {
  const resources = path.join(process.resourcesPath ?? '', 'dist');
  /* installed builds keep the game files in resources/dist, so that folder
     comes first there; a checkout still resolves its build output */
  const candidates = (app.isPackaged
    ? [process.env.MIXCLIENT_DIST, resources, path.join(projectRoot, 'build', 'dist')]
    : [process.env.MIXCLIENT_DIST, path.join(projectRoot, 'build', 'dist'), resources]
  ).filter(Boolean);
  return candidates.find((dir) => fs.existsSync(path.join(dir, 'MixClient-Launcher.jar'))) ?? candidates[0];
}

function jarPath() {
  return path.join(distDir(), 'MixClient-Launcher.jar');
}

/* The installer carries its own Java 8 runtime, so Play works on a machine
   that never had a JDK. A path typed into Settings still wins. */
function bundledJavaHome() {
  const home = path.join(process.resourcesPath ?? '', 'jre');
  return fs.existsSync(path.join(home, 'bin', 'javaw.exe')) ? home : null;
}

function resolveJava(configured) {
  const custom = typeof configured === 'string' && configured.trim() !== '' && configured !== 'java'
    ? configured
    : null;
  if (custom && fs.existsSync(custom)) {
    return custom;
  }
  const bundled = bundledJavaHome();
  if (bundled) {
    return path.join(bundled, 'bin', 'javaw.exe');
  }
  const javaHome = process.env.JAVA_HOME
    ? path.join(process.env.JAVA_HOME, 'bin', 'javaw.exe')
    : null;
  if (javaHome && fs.existsSync(javaHome)) {
    return javaHome;
  }
  return custom ?? 'java';
}

function send(channel, payload) {
  if (win && !win.isDestroyed()) {
    win.webContents.send(channel, payload);
  }
}

function accountView() {
  const session = readSession();
  if (!session) {
    return null;
  }
  return {
    name: session.name,
    uuid: session.uuid,
    expiresAt: session.expiresAt ?? null,
    mode: session.offline ? 'offline' : 'microsoft'
  };
}

/** The UUID Minecraft gives an offline name: md5("OfflinePlayer:<name>"), v3. */
function offlineUuid(name) {
  const hash = crypto.createHash('md5').update(`OfflinePlayer:${name}`, 'utf8').digest();
  hash[6] = (hash[6] & 0x0f) | 0x30;
  hash[8] = (hash[8] & 0x3f) | 0x80;
  const hex = hash.toString('hex');
  return `${hex.slice(0, 8)}-${hex.slice(8, 12)}-${hex.slice(12, 16)}-${hex.slice(16, 20)}-${hex.slice(20)}`;
}

/** Offline / crack account: a name is all the game needs. */
function loginOffline(rawName) {
  const name = String(rawName ?? '').trim();
  if (!/^[A-Za-z0-9_]{3,16}$/.test(name)) {
    return { ok: false, message: 'Use 3–16 letters, numbers or underscores.' };
  }
  const session = {
    name,
    uuid: offlineUuid(name),
    accessToken: '0',
    offline: true,
    expiresAt: null
  };
  saveSession(session);
  send('auth:done', { name: session.name, uuid: session.uuid, mode: 'offline' });
  return { ok: true, name, uuid: session.uuid };
}


/* ---------------- account store ---------------- */

function accountsFile() {
  return path.join(app.getPath('userData'), 'accounts.json');
}

function readAccounts() {
  try {
    const parsed = JSON.parse(fs.readFileSync(accountsFile(), 'utf8'));
    if (Array.isArray(parsed.accounts)) {
      return { accounts: parsed.accounts, selectedId: parsed.selectedId ?? null };
    }
  } catch {
    /* first run */
  }
  return { accounts: [], selectedId: null };
}

function writeAccounts(data) {
  fs.mkdirSync(path.dirname(accountsFile()), { recursive: true });
  fs.writeFileSync(accountsFile(), JSON.stringify(data, null, 2), 'utf8');
}

/** Sessions live encrypted, one per account, so switching does not need a new sign-in. */
function sealSession(session) {
  if (!session) {
    return null;
  }
  const json = JSON.stringify(session);
  return safeStorage.isEncryptionAvailable()
    ? safeStorage.encryptString(json).toString('base64')
    : Buffer.from(json, 'utf8').toString('base64');
}

/* The packaged app has no console, so anything that goes wrong during sign-in is
   invisible. Mirror the important steps into userData/launcher.log. */
function logLine(message) {
  try {
    fs.appendFileSync(
      path.join(app.getPath('userData'), 'launcher.log'),
      `[${new Date().toISOString()}] ${message}\n`
    );
  } catch {
    /* never let logging break the flow */
  }
}

function unsealSession(sealed) {
  if (!sealed) {
    return null;
  }
  try {
    const buffer = Buffer.from(sealed, 'base64');
    const json = safeStorage.isEncryptionAvailable()
      ? safeStorage.decryptString(buffer)
      : buffer.toString('utf8');
    return JSON.parse(json);
  } catch {
    return null;
  }
}

function accountId(name, type) {
  return `${type}:${String(name).toLowerCase()}`;
}

function accountsView() {
  const data = readAccounts();
  return {
    accounts: data.accounts.map((entry) => ({
      id: entry.id,
      name: entry.name,
      uuid: entry.uuid,
      type: entry.type
    })),
    selectedId: data.selectedId ?? null
  };
}

function addOfflineAccount(rawName) {
  const name = String(rawName ?? '').trim();
  if (!/^[A-Za-z0-9_]{3,16}$/.test(name)) {
    return { ok: false, message: 'Use 3-16 letters, numbers or underscores.' };
  }
  const data = readAccounts();
  const id = accountId(name, 'offline');
  if (!data.accounts.some((entry) => entry.id === id)) {
    data.accounts.push({
      id,
      name,
      uuid: offlineUuid(name),
      type: 'offline',
      sealed: sealSession({ name, uuid: offlineUuid(name), accessToken: '0', offline: true })
    });
  }
  data.selectedId = id;
  writeAccounts(data);
  const session = unsealSession(data.accounts.find((entry) => entry.id === id).sealed);
  saveSession(session);
  send('auth:done', { name: session.name, uuid: session.uuid, mode: 'offline' });
  return { ok: true, ...accountsView() };
}

function selectAccount(id) {
  const data = readAccounts();
  const entry = data.accounts.find((item) => item.id === id);
  if (!entry) {
    return { ok: false, message: 'Unknown account.' };
  }
  const session = unsealSession(entry.sealed);
  if (!session) {
    return { ok: false, message: 'This account session is unavailable. Sign in again.' };
  }
  data.selectedId = id;
  writeAccounts(data);
  saveSession(session);
  if (session) {
    send('auth:done', { name: session.name, uuid: session.uuid, mode: entry.type });
  }
  return { ok: true, ...accountsView() };
}

function removeAccount(id) {
  const data = readAccounts();
  const removedSelected = data.selectedId === id;
  let session = null;
  let next;
  data.accounts = data.accounts.filter((entry) => entry.id !== id);
  if (removedSelected) {
    data.selectedId = data.accounts.length ? data.accounts[0].id : null;
    next = data.accounts.find((entry) => entry.id === data.selectedId);
    session = next ? unsealSession(next.sealed) : null;
    saveSession(session);
  }
  writeAccounts(data);
  if (removedSelected) {
    send('auth:done', session ? { name: session.name, uuid: session.uuid, mode: next.type } : null);
  }
  return { ok: true, ...accountsView() };
}

/* ---------------- authentication ---------------- */

async function beginLogin() {
  const state = readState();
  /* falls back to the Azure app registration bundled with the launcher, so a
     fresh install signs in without any setup */
  const clientId = state.msClientId || DEFAULT_MS_CLIENT_ID;
  if (!clientId) {
    return { ok: false, message: 'No Microsoft client ID available.' };
  }
  if (loginController) {
    loginController.abort();
  }
  loginController = new AbortController();
  const controller = loginController;

  logLine(`sign-in start: clientId=${clientId}`);

  auth
    .loginWindow(clientId, {
      BrowserWindow,
      signal: controller.signal,
      onStatus: (label) => send('auth:status', label)
    })
    .then((session) => {
      saveSession(session);
      logLine(`sign-in ok: name=${session.name} uuid=${session.uuid}`);
      try {
        const data = readAccounts();
        const id = accountId(session.name, 'microsoft');
        const existing = data.accounts.find((entry) => entry.id === id);
        if (existing) {
          existing.sealed = sealSession(session);
        } else {
          data.accounts.push({ id, name: session.name, uuid: session.uuid, type: 'microsoft', sealed: sealSession(session) });
        }
        data.selectedId = id;
        writeAccounts(data);
        logLine(`accounts written: ${data.accounts.map((entry) => entry.name).join(', ')}`);
      } catch (error) {
        logLine(`could not store the account: ${error?.stack ?? error}`);
      }
      send('auth:done', { name: session.name, uuid: session.uuid, mode: 'microsoft' });
    })
    .catch((error) => {
      logLine(`sign-in failed: ${error?.stack ?? error}`);
      if (!controller.signal.aborted) {
        send('auth:error', String(error.message ?? error));
      }
    })
    .finally(() => {
      if (loginController === controller) {
        loginController = null;
      }
    });

  return { ok: true, message: 'Waiting for Microsoft sign-in…' };
}

function cancelLogin() {
  if (loginController) {
    loginController.abort();
    loginController = null;
  }
  send('auth:error', 'Sign-in cancelled.');
  return { ok: true };
}

function signOut() {
  saveSession(null);
  send('auth:done', null);
  return { ok: true };
}

/* ---------------- game ---------------- */

async function launch(options = {}) {
  if (game && game.exitCode === null) {
    return { ok: false, message: 'The game is already running.' };
  }
  const jar = jarPath();
  if (!fs.existsSync(jar)) {
    return { ok: false, message: `Launcher jar not found: ${jar}` };
  }

  const state = readState();
  let session = readSession();
  if (session && session.offline) {
    /* nothing to refresh: the token is a placeholder */
    send('auth:done', { name: session.name, uuid: session.uuid, mode: 'offline' });
  } else if (session) {
    try {
      const refreshed = await auth.ensureSession(session, state.msClientId || DEFAULT_MS_CLIENT_ID);
      if (refreshed && refreshed.accessToken !== session.accessToken) {
        saveSession(refreshed);
      }
      session = refreshed;
      send('auth:done', { name: session.name, uuid: session.uuid });
    } catch (error) {
      session = null;
      send('auth:error', `Session expired: ${String(error.message ?? error)}`);
    }
  }

  const loader = options.loader === 'forge' || options.loader === 'optifine' ? options.loader : state.loader;
  const useForge = loader === 'forge';
  try {
    send('install:progress', { phase: 'start', done: 0, total: 1, label: 'Checking Minecraft 1.8.9…' });
    await minecraft.ensureInstalled({
      distDir: distDir(),
      onProgress: (progress) => send('install:progress', progress)
    });
    if (useForge) {
      await forge.ensure({ onProgress: (progress) => send('install:progress', progress) });
      /* OptiFine is part of the Forge loader rather than a separate choice:
         FML cannot take the bundled launchwrapper profile, so the launcher
         drops the shipped jar into mods/ and Forge loads it as a mod. */
      const optifineMod = mods.ensureOptiFine(distDir());
      if (optifineMod) {
        send('game:log', `> optifine (forge mod): ${path.basename(optifineMod)}`);
      }
    }
  } catch (error) {
    return { ok: false, message: String(error.message ?? error) };
  }

  const config = {
    username: session?.name || options.username || 'Player',
    uuid: session?.uuid,
    accessToken: session?.accessToken,
    userType: session && !session.offline ? 'msa' : 'legacy',
    accountMode: session && !session.offline ? 'microsoft' : 'offline',
    msClientId: state.msClientId || undefined,
    ramMb: Number(options.ramMb) || 2048,
    /* Exactly one loader runs. Forge owns the game through FML, loads mods/
       (OptiFine included — ensureOptiFine drops the shipped jar in there), and
       uses no launchwrapper of its own. The OptiFine loader is the other one:
       its own launchwrapper, no mods at all. */
    optifine: !useForge,
    forge: useForge,
    smokeTest: false
  };
  if (options.server) {
    config.server = options.server;
    config.port = Number(options.port) || 25565;
  }

  const launchFile = path.join(app.getPath('userData'), 'launch.json');
  fs.mkdirSync(path.dirname(launchFile), { recursive: true });
  fs.writeFileSync(launchFile, JSON.stringify(config, null, 2), 'utf8');

  const java = resolveJava(options.javaPath || state.javaPath);
  const args = ['-Dfile.encoding=UTF-8', '-jar', jar, '--launch-file', launchFile];
  send('game:log', `> ${java} ${args.join(' ')}`);
  send('game:log', `> account: ${config.username} (${config.accountMode})`);
  if (useForge) {
    const installed = mods.list();
    const enabled = installed.mods.filter((entry) => entry.enabled).length;
    send('game:log', `> forge ${forge.FORGE_VERSION} · ${enabled}/${installed.mods.length} mods enabled · ${installed.dir}`);
  }

  /* hand the runtime down to the launcher jar so the game itself starts on the
     same Java 8, even when java is not on PATH */
  const env = { ...process.env };
  const bundled = bundledJavaHome();
  if (bundled) {
    env.JAVA_HOME = bundled;
    env.MIXCLIENT_JAVA = java;
  }

  try {
    game = spawn(java, args, { cwd: distDir(), windowsHide: false, env });
  } catch (error) {
    return { ok: false, message: String(error.message ?? error) };
  }

  const pipe = (stream, tag) => {
    let buffer = '';
    stream.setEncoding('utf8');
    stream.on('data', (chunk) => {
      buffer += chunk;
      const lines = buffer.split(/\r?\n/);
      buffer = lines.pop() ?? '';
      lines.forEach((line) => send('game:log', tag + line));
    });
  };
  pipe(game.stdout, '');
  pipe(game.stderr, '[stderr] ');

  game.on('exit', (code) => {
    send('game:log', `> game exited with code ${code}`);
    send('game:exit', code);
    game = null;
    /* the jars are free again: install the update that was queued at start up */
    if (autoUpdatePending) {
      setTimeout(runAutoUpdate, 1200);
    }
  });
  game.on('error', (error) => {
    send('game:log', `> launch failed: ${error.message}`);
    send('game:exit', -1);
  });

  return { ok: true, message: `Launching as ${config.username}` };
}

/* ---------------- updates ---------------- */

function updateUrl() {
  const configured = readState().updateUrl;
  return (typeof configured === 'string' && configured.trim() !== '' ? configured.trim() : BAKED_UPDATE_URL);
}

async function checkUpdate() {
  const url = updateUrl();
  if (!url) {
    return { ok: false, message: 'Set an update manifest URL in Settings first.' };
  }
  const local = updater.localVersion(distDir());
  const update = await updater.check(url);
  const launcherLocal = app.getVersion();
  const launcherRemote = update.launcher?.version ?? null;
  return {
    ok: true,
    local,
    remote: update.version,
    newer: updater.isNewer(local, update.version),
    changelog: update.changelog,
    files: update.files.length,
    update,
    launcher: {
      local: launcherLocal,
      remote: launcherRemote,
      /* a checkout has no asar to swap: the app only updates once packaged */
      newer: Boolean(launcherRemote) && app.isPackaged && updater.isNewer(launcherLocal, launcherRemote),
      packaged: app.isPackaged,
      files: update.launcher?.files.length ?? 0
    },
    pending: launcherUpdate.pending(updateWorkDir())?.version ?? null
  };
}

/**
 * Install the game files and/or stage the launcher update.
 *
 * `parts` is 'game', 'launcher' or 'both'. Staging the launcher is cheap and
 * safe — nothing is replaced until the app closes — so the start-up update
 * stages it too and the player just gets a "Restart" button.
 */
async function applyUpdate(payload) {
  const url = updateUrl();
  const update = payload?.update ?? (await updater.check(url));
  const parts = payload?.parts ?? 'both';
  const result = { ok: true, version: updater.localVersion(distDir()), launcher: null };

  if (parts !== 'launcher') {
    await updater.apply(update, distDir(), (progress) => send('update:progress', progress));
    result.version = updater.localVersion(distDir());
  }

  if (parts !== 'game' && update.launcher?.files?.length) {
    if (!app.isPackaged) {
      result.launcher = { skipped: true, reason: 'A development checkout has no app.asar to replace.' };
    } else {
      const staged = await launcherUpdate.stage({
        version: update.launcher.version,
        files: update.launcher.files,
        workDir: updateWorkDir(),
        onProgress: (progress) => send('update:progress', progress)
      });
      result.launcher = { staged: true, version: staged.version };
    }
  }

  const messages = [];
  if (parts !== 'launcher') {
    messages.push(`client ${result.version}`);
  }
  if (result.launcher?.staged) {
    messages.push(`launcher ${result.launcher.version} ready`);
  }
  result.message = messages.length ? `Updated ${messages.join(' · ')}` : 'Nothing to install';
  return result;
}

/**
 * Restart into the staged launcher build. The helper takes over the moment
 * this process exits: it swaps app.asar and starts the app again.
 */
function restartToUpdate() {
  const result = launcherUpdate.applyOnQuit({
    workDir: updateWorkDir(),
    targetDir: process.resourcesPath,
    exe: process.execPath,
    relaunch: true
  });
  if (!result.ok) {
    return result;
  }
  restarting = true;
  send('update:status', { phase: 'launcher-restarting', version: result.version });
  setTimeout(() => app.exit(0), 300);
  return { ok: true, message: `Restarting to install ${result.version}` };
}

/**
 * The start-up update: check, download and install the newer client build
 * before the player presses Play, then stage the launcher build for the next
 * restart.
 *
 * The client jars are held open by a running game on Windows, so an update is
 * deferred (and re-run once the game closes) instead of failing on a rename.
 * Failures never block the launcher: the player keeps the build they have.
 */
async function runAutoUpdate() {
  if (autoUpdateRunning) {
    return;
  }
  const state = readState();
  if (state.autoUpdate === false && state.autoUpdateLauncher === false) {
    return;
  }
  if (!updateUrl()) {
    return;
  }
  if (game) {
    autoUpdatePending = true;
    send('update:status', { phase: 'deferred', message: 'Update queued until the game closes.' });
    return;
  }
  autoUpdatePending = false;
  autoUpdateRunning = true;
  try {
    send('update:status', { phase: 'checking' });
    const check = await checkUpdate();
    if (!check.ok) {
      send('update:status', { phase: 'error', message: check.message });
      return;
    }
    const gameWanted = check.newer && state.autoUpdate !== false;
    const launcherWanted = check.launcher.newer && state.autoUpdateLauncher !== false;
    if (!gameWanted && !launcherWanted) {
      send('update:status', { phase: 'current', version: check.local, launcher: check.launcher.local });
      return;
    }

    if (gameWanted) {
      send('update:status', {
        phase: 'downloading',
        version: check.remote,
        local: check.local,
        changelog: check.changelog,
        files: check.files
      });
      await applyUpdate({ update: check.update, parts: 'game' });
      send('update:status', {
        phase: 'done',
        version: updater.localVersion(distDir()),
        previous: check.local,
        changelog: check.changelog
      });
    }

    if (launcherWanted) {
      send('update:status', { phase: 'launcher-downloading', version: check.launcher.remote, local: check.launcher.local });
      const staged = await applyUpdate({ update: check.update, parts: 'launcher' });
      if (staged.launcher?.staged) {
        send('update:status', {
          phase: 'launcher-ready',
          version: staged.launcher.version,
          local: check.launcher.local,
          changelog: check.changelog
        });
      }
    }
  } catch (error) {
    /* no network is not an error the player needs to see: the build they have
       still works, and the next start tries again */
    const message = String(error?.message ?? error);
    const offline = error?.name === 'TypeError' || /fetch failed|ENOTFOUND|ECONNREFUSED|ETIMEDOUT/i.test(message);
    console.error(`[update] ${message}`);
    send('update:status', { phase: offline ? 'offline' : 'error', message });
  } finally {
    autoUpdateRunning = false;
  }
}

/* ---------------- window ---------------- */

function createWindow() {
  win = new BrowserWindow({
    width: 1180,
    height: 740,
    minWidth: 980,
    minHeight: 620,
    frame: false,
    show: false,
    backgroundColor: '#0F0F23',
    webPreferences: {
      preload: path.join(here, 'preload.cjs'),
      contextIsolation: true,
      nodeIntegration: false
    }
  });

  win.once('ready-to-show', () => win.show());

  const devUrl = process.env.VITE_DEV_SERVER_URL;
  if (devUrl) {
    win.loadURL(devUrl);
  } else {
    win.loadFile(path.join(here, '..', 'dist', 'index.html'));
  }
}

ipcMain.handle('app:state', () => ({
  state: readState(),
  account: accountView(),
  distDir: distDir(),
  jarExists: fs.existsSync(jarPath()),
  install: minecraft.installationStatus(distDir()),
  clientVersion: updater.localVersion(distDir()),
  updateUrl: updateUrl(),
  updateConfigured: Boolean(updateUrl()),
  java: resolveJava(readState().javaPath),
  bundledJava: Boolean(bundledJavaHome()),
  forge: forge.status(),
  mods: mods.list(),
  launcherVersion: app.getVersion(),
  launcherPackaged: app.isPackaged,
  pendingLauncher: launcherUpdate.pending(updateWorkDir())?.version ?? null,
  version: app.getVersion(),
  packaged: app.isPackaged
}));

ipcMain.handle('app:save', (_event, patch) => writeState(patch ?? {}));
ipcMain.handle('auth:begin', () => beginLogin());
ipcMain.handle('auth:offline', (_event, username) => loginOffline(username));
ipcMain.handle('accounts:list', () => accountsView());
ipcMain.handle('accounts:add-offline', (_event, name) => addOfflineAccount(name));
ipcMain.handle('accounts:select', (_event, id) => selectAccount(id));
ipcMain.handle('accounts:remove', (_event, id) => removeAccount(id));
ipcMain.handle('auth:cancel', () => cancelLogin());
ipcMain.handle('auth:signout', () => signOut());
ipcMain.handle('game:launch', (_event, options) => launch(options ?? {}));
ipcMain.handle('install:ensure', async () => {
  try {
    const result = await minecraft.ensureInstalled({
      distDir: distDir(),
      onProgress: (progress) => send('install:progress', progress)
    });
    return { ok: true, message: `Ready in ${result.dir}`, install: minecraft.installationStatus(distDir()) };
  } catch (error) {
    return { ok: false, message: String(error.message ?? error) };
  }
});
ipcMain.handle('update:check', async () => {
  try {
    return await checkUpdate();
  } catch (error) {
    return { ok: false, message: String(error.message ?? error) };
  }
});
ipcMain.handle('update:apply', async (_event, payload) => {
  try {
    return await applyUpdate(payload);
  } catch (error) {
    return { ok: false, message: String(error.message ?? error) };
  }
});
ipcMain.handle('update:restart', () => {
  try {
    return restartToUpdate();
  } catch (error) {
    return { ok: false, message: String(error.message ?? error) };
  }
});

/* ---------------- wardrobe ---------------- */

ipcMain.handle('skin:account', async (_event, username) => {
  try {
    return await skins.accountSkin(app.getPath('userData'), username || readState().username);
  } catch {
    return null;
  }
});

ipcMain.handle('skin:local', () => skins.listLocal(app.getPath('userData')));

ipcMain.handle('skin:add', async () => {
  const result = await dialog.showOpenDialog(win, {
    title: 'Add a skin',
    filters: [{ name: 'Minecraft skin (64×64 PNG)', extensions: ['png'] }],
    properties: ['openFile']
  });
  if (result.canceled || !result.filePaths[0]) {
    return { ok: false, skins: skins.listLocal(app.getPath('userData')) };
  }
  let added = null;
  try {
    added = skins.addLocal(app.getPath('userData'), result.filePaths[0]);
  } catch (error) {
    return { ok: false, message: String(error.message ?? error), skins: skins.listLocal(app.getPath('userData')) };
  }
  return { ok: true, added, skins: skins.listLocal(app.getPath('userData')) };
});

ipcMain.handle('app:open-dir', (_event, target) => {
  if (target === 'minecraft') {
    return shell.openPath(minecraft.minecraftDir());
  }
  if (target === 'game') {
    fs.mkdirSync(minecraft.gameDir(), { recursive: true });
    return shell.openPath(minecraft.gameDir());
  }
  if (target === 'mods') {
    fs.mkdirSync(mods.modsDir(), { recursive: true });
    return shell.openPath(mods.modsDir());
  }
  return shell.openPath(distDir());
});

/* ---------------- mods ---------------- */

ipcMain.handle('mods:list', () => mods.list());
ipcMain.handle('mods:add', async () => {
  const result = await dialog.showOpenDialog(win, {
    title: 'Add Forge mods',
    buttonLabel: 'Add',
    properties: ['openFile', 'multiSelections'],
    filters: [{ name: 'Forge mods', extensions: ['jar'] }]
  });
  if (result.canceled || result.filePaths.length === 0) {
    return { ok: false, canceled: true };
  }
  return mods.add(result.filePaths);
});
ipcMain.handle('mods:toggle', (_event, payload) =>
  mods.setEnabled(payload?.file ?? '', payload?.enabled === true)
);
ipcMain.handle('mods:remove', (_event, file) => mods.remove(file));
ipcMain.handle('mods:reveal', (_event, file) => {
  const full = path.join(mods.modsDir(), path.basename(file ?? ''));
  if (fs.existsSync(full)) {
    shell.showItemInFolder(full);
  }
});

ipcMain.handle('forge:status', () => forge.status());
ipcMain.handle('forge:ensure', async () => {
  try {
    const result = await forge.ensure({ onProgress: (progress) => send('install:progress', progress) });
    return { ok: true, message: `Forge ${forge.FORGE_VERSION} ready`, forge: result };
  } catch (error) {
    return { ok: false, message: String(error.message ?? error) };
  }
});
ipcMain.handle('app:open-url', (_event, url) => {
  if (typeof url === 'string' && (url.startsWith('https://') || url.startsWith('http://'))) {
    return shell.openExternal(url);
  }
  return null;
});
ipcMain.handle('app:window', (_event, action) => {
  if (!win) {
    return;
  }
  if (action === 'minimize') {
    win.minimize();
  } else if (action === 'maximize') {
    win.isMaximized() ? win.unmaximize() : win.maximize();
  } else if (action === 'close') {
    win.close();
  }
});

app.whenReady().then(() => {
  createWindow();
  /* the update needs the renderer listening for progress, so it runs once the
     window has finished loading; a missing or failing manifest is silent */
  win?.webContents.once('did-finish-load', () => {
    setTimeout(runAutoUpdate, 400);
  });
});

app.on('window-all-closed', () => {
  if (process.platform !== 'darwin') {
    app.quit();
  }
});

/* A staged launcher build is installed on the way out: the helper waits for
   this process to disappear, then swaps app.asar. The "Restart & update"
   button has already spawned it, hence the flag. */
app.on('before-quit', () => {
  if (restarting || readState().autoUpdateLauncher === false) {
    return;
  }
  if (!launcherUpdate.pending(updateWorkDir())) {
    return;
  }
  const result = launcherUpdate.applyOnQuit({
    workDir: updateWorkDir(),
    targetDir: process.resourcesPath,
    exe: process.execPath,
    relaunch: false
  });
  if (result.ok) {
    send('update:status', { phase: 'launcher-installing', version: result.version });
  }
});

app.on('activate', () => {
  if (BrowserWindow.getAllWindows().length === 0) {
    createWindow();
  }
});
