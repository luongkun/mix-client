/* Skins for the wardrobe page.

   The launcher has no 3D renderer, so the page draws the skin as a flat paper
   doll on a canvas — that only needs the 64×64 PNG. The account skin comes from
   Mojang (profile lookup, then the session texture), cached for a few hours so
   the page opens instantly and keeps working offline. */

import fs from 'node:fs';
import path from 'node:path';
import { nativeImage } from 'electron';

const USER_AGENT = 'MixClient/1.0';
const CACHE_TTL_MS = 6 * 60 * 60 * 1000;

const cacheDir = (userData) => path.join(userData, 'skin-cache');
export const skinsDir = (userData) => path.join(userData, 'skins');

function dataUrl(file) {
  return `data:image/png;base64,${fs.readFileSync(file).toString('base64')}`;
}

async function fetchWithTimeout(url, timeoutMs) {
  const controller = new AbortController();
  const timer = setTimeout(() => controller.abort(), timeoutMs);
  try {
    return await fetch(url, { headers: { 'User-Agent': USER_AGENT }, signal: controller.signal });
  } finally {
    clearTimeout(timer);
  }
}

async function fetchJson(url) {
  try {
    const response = await fetchWithTimeout(url, 4000);
    return response.ok ? await response.json() : null;
  } catch {
    return null;
  }
}

function readCache(userData, username) {
  const file = path.join(cacheDir(userData), `${username.toLowerCase()}.png`);
  const meta = `${file}.json`;
  try {
    const saved = JSON.parse(fs.readFileSync(meta, 'utf8'));
    if (!fs.existsSync(file) || Date.now() - saved.fetchedAt > CACHE_TTL_MS) {
      return null;
    }
    return { url: dataUrl(file), slim: Boolean(saved.slim), source: 'account' };
  } catch {
    return null;
  }
}

/**
 * The player's own skin. Returns null when the name is not a licensed account
 * (offline names) or the lookup fails — the page then falls back to the bundled
 * default skin instead of showing an empty box.
 */
export async function accountSkin(userData, username) {
  if (!username) {
    return null;
  }
  const cached = readCache(userData, username);
  if (cached) {
    return cached;
  }

  const profile = await fetchJson(`https://api.mojang.com/users/profiles/minecraft/${encodeURIComponent(username)}`);
  if (!profile?.id) {
    return null;
  }
  const session = await fetchJson(`https://sessionserver.mojang.com/session/minecraft/profile/${profile.id}`);
  const encoded = session?.properties?.find((entry) => entry.name === 'textures')?.value;
  if (!encoded) {
    return null;
  }

  let decoded;
  try {
    decoded = JSON.parse(Buffer.from(encoded, 'base64').toString('utf8'));
  } catch {
    return null;
  }
  const skin = decoded?.textures?.SKIN;
  if (!skin?.url) {
    return null;
  }

  try {
    const response = await fetchWithTimeout(skin.url, 6000);
    if (!response.ok) {
      return null;
    }
    const bytes = Buffer.from(await response.arrayBuffer());
    const file = path.join(cacheDir(userData), `${username.toLowerCase()}.png`);
    fs.mkdirSync(path.dirname(file), { recursive: true });
    fs.writeFileSync(file, bytes);
    const slim = skin.metadata?.model === 'slim';
    fs.writeFileSync(`${file}.json`, JSON.stringify({ slim, fetchedAt: Date.now() }), 'utf8');
    return { url: dataUrl(file), slim, source: 'account' };
  } catch {
    return null;
  }
}

/* A 64×64 skin stores the right arm in the 44..48 band. On a slim skin that arm
   is three pixels wide, so the fourth column — pixel (54, 20) in the mirrored
   arm — is left transparent. Solid alpha there means a classic arm. */
export function detectVariant(file) {
  try {
    const image = nativeImage.createFromPath(file);
    const { width, height } = image.getSize();
    if (width !== 64 || (height !== 64 && height !== 32)) {
      return 'classic';
    }
    const alpha = image.toBitmap()[(20 * width + 54) * 4 + 3];
    return alpha === 0 ? 'slim' : 'classic';
  } catch {
    return 'classic';
  }
}

function variantOf(file) {
  try {
    const saved = JSON.parse(fs.readFileSync(`${file}.json`, 'utf8'));
    if (saved?.variant === 'slim' || saved?.variant === 'classic') {
      return saved.variant;
    }
  } catch {
    /* no sidecar yet: fall through to detection */
  }
  return detectVariant(file);
}

export function listLocal(userData) {
  const dir = skinsDir(userData);
  try {
    return fs
      .readdirSync(dir)
      .filter((name) => name.toLowerCase().endsWith('.png'))
      .map((name) => {
        const file = path.join(dir, name);
        return {
          id: name,
          name: name.replace(/\.png$/i, ''),
          url: dataUrl(file),
          variant: variantOf(file),
          source: 'local'
        };
      });
  } catch {
    return [];
  }
}

export function addLocal(userData, source) {
  const dir = skinsDir(userData);
  fs.mkdirSync(dir, { recursive: true });
  const base = path.basename(source).replace(/[^\w.\- ]+/g, '') || 'skin.png';
  let target = path.join(dir, base.toLowerCase().endsWith('.png') ? base : `${base}.png`);
  let index = 2;
  while (fs.existsSync(target)) {
    target = path.join(dir, `${base.replace(/\.png$/i, '')} (${index}).png`);
    index += 1;
  }
  fs.copyFileSync(source, target);
  const variant = detectVariant(target);
  fs.writeFileSync(`${target}.json`, JSON.stringify({ variant }), 'utf8');
  return { file: path.basename(target), variant };
}
