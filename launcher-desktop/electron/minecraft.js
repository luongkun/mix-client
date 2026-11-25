/* Minecraft 1.8.9 installation: version json, client jar, libraries, natives
   and the asset objects. Everything lands in the normal .minecraft layout, so
   the Java launcher and other launchers keep working with it. */

import crypto from 'node:crypto';
import fs from 'node:fs';
import os from 'node:os';
import path from 'node:path';
import zlib from 'node:zlib';
import { pipeline } from 'node:stream/promises';
import { Readable } from 'node:stream';

export const MC_VERSION = '1.8.9';
const VERSION_MANIFEST = 'https://piston-meta.mojang.com/mc/game/version_manifest_v2.json';
const ASSET_BASE = 'https://resources.download.minecraft.net';
const USER_AGENT = 'MixClient/1.0';

export function appDataDir() {
  return process.env.APPDATA || os.homedir();
}

export function minecraftDir() {
  const appdata = path.join(appDataDir(), '.minecraft');
  return fs.existsSync(appdata) ? appdata : path.join(os.homedir(), '.minecraft');
}

export function dataDir() {
  return path.join(appDataDir(), '.mixclient');
}

export function nativesDir() {
  return path.join(dataDir(), 'natives');
}

/* Mix Client plays in its own game folder — like the other clients do — so its
   mods, config and saves are not mixed with a vanilla or Forge install that
   happens to sit in .minecraft. Only the shared bits (client jar, libraries,
   assets) stay where every launcher expects them. */
export function gameDir() {
  return path.join(dataDir(), 'game');
}

export const versionDir = () => path.join(minecraftDir(), 'versions', MC_VERSION);
export const librariesDir = () => path.join(minecraftDir(), 'libraries');
export const assetsDir = () => path.join(minecraftDir(), 'assets');

function platformOs() {
  if (process.platform === 'win32') return 'windows';
  if (process.platform === 'darwin') return 'osx';
  return 'linux';
}

function archBits() {
  return process.arch === 'x64' ? '64' : '32';
}

function osArchName() {
  return process.arch === 'x64' ? 'x86_64' : process.arch === 'ia32' ? 'x86' : process.arch;
}

function allowed(library) {
  if (!Array.isArray(library.rules)) {
    return true;
  }
  let result = false;
  for (const rule of library.rules) {
    let applies = true;
    if (rule.os) {
      if (rule.os.name && rule.os.name !== platformOs()) applies = false;
      if (rule.os.arch && !osArchName().includes(rule.os.arch)) applies = false;
    }
    if (applies) {
      result = rule.action === 'allow';
    }
  }
  return result;
}

export function mavenPath(name, classifier) {
  const [group, artifact, version, extra] = name.split(':');
  const suffix = classifier ?? extra;
  const file = `${artifact}-${version}${suffix ? `-${suffix}` : ''}.jar`;
  return path.join(...group.split('.'), artifact, version, file);
}

function sha1Of(file) {
  const hash = crypto.createHash('sha1');
  hash.update(fs.readFileSync(file));
  return hash.digest('hex');
}

async function fetchJson(url, signal) {
  const response = await fetch(url, { headers: { 'User-Agent': USER_AGENT }, signal });
  if (!response.ok) {
    throw new Error(`HTTP ${response.status} for ${url}`);
  }
  return response.json();
}

export async function download(url, target, { sha1, signal } = {}) {
  if (fs.existsSync(target)) {
    if (!sha1 || sha1Of(target) === sha1.toLowerCase()) {
      return false;
    }
  }
  fs.mkdirSync(path.dirname(target), { recursive: true });
  const temp = `${target}.part`;
  const response = await fetch(url, { headers: { 'User-Agent': USER_AGENT }, signal });
  if (!response.ok) {
    throw new Error(`HTTP ${response.status} for ${url}`);
  }
  await pipeline(Readable.fromWeb(response.body), fs.createWriteStream(temp));
  if (sha1 && sha1Of(temp) !== sha1.toLowerCase()) {
    fs.rmSync(temp, { force: true });
    throw new Error(`Checksum mismatch for ${path.basename(target)}`);
  }
  fs.renameSync(temp, target);
  return true;
}

/* Minimal zip reader: the natives jars only use stored/deflated entries and
   this keeps the launcher free of native or shell dependencies. */
function extractZip(zipPath, destination) {
  const buffer = fs.readFileSync(zipPath);
  let eocd = -1;
  for (let i = buffer.length - 22; i >= 0; i -= 1) {
    if (buffer.readUInt32LE(i) === 0x06054b50) {
      eocd = i;
      break;
    }
  }
  if (eocd < 0) {
    throw new Error(`Not a zip archive: ${zipPath}`);
  }
  const count = buffer.readUInt16LE(eocd + 10);
  let offset = buffer.readUInt32LE(eocd + 16);
  for (let index = 0; index < count; index += 1) {
    if (buffer.readUInt32LE(offset) !== 0x02014b50) {
      break;
    }
    const method = buffer.readUInt16LE(offset + 10);
    const compressedSize = buffer.readUInt32LE(offset + 20);
    const nameLength = buffer.readUInt16LE(offset + 28);
    const extraLength = buffer.readUInt16LE(offset + 30);
    const commentLength = buffer.readUInt16LE(offset + 32);
    const localOffset = buffer.readUInt32LE(offset + 42);
    const name = buffer.toString('utf8', offset + 46, offset + 46 + nameLength);
    offset += 46 + nameLength + extraLength + commentLength;
    if (name.endsWith('/') || name.startsWith('META-INF/')) {
      continue;
    }
    const localNameLength = buffer.readUInt16LE(localOffset + 26);
    const localExtraLength = buffer.readUInt16LE(localOffset + 28);
    const start = localOffset + 30 + localNameLength + localExtraLength;
    const raw = buffer.subarray(start, start + compressedSize);
    const content = method === 0 ? raw : zlib.inflateRawSync(raw);
    const target = path.join(destination, name);
    /* an entry called ../../something would otherwise be written outside the
       natives folder; the archives are ours, but the check costs nothing */
    const relative = path.relative(destination, target);
    if (!relative || relative.startsWith('..') || path.isAbsolute(relative)) {
      continue;
    }
    fs.mkdirSync(path.dirname(target), { recursive: true });
    fs.writeFileSync(target, content);
  }
}

/* One entry out of a zip, read through the central directory. The Forge
   installer keeps its version profile and universal jar inside itself, and
   pulling those two entries out is cheaper than unzipping 4 MB to disk. */
export function readZipEntry(zipPath, entryName) {
  const buffer = fs.readFileSync(zipPath);
  let eocd = -1;
  for (let i = buffer.length - 22; i >= 0; i -= 1) {
    if (buffer.readUInt32LE(i) === 0x06054b50) {
      eocd = i;
      break;
    }
  }
  if (eocd < 0) {
    throw new Error(`Not a zip archive: ${zipPath}`);
  }
  const count = buffer.readUInt16LE(eocd + 10);
  let offset = buffer.readUInt32LE(eocd + 16);
  for (let index = 0; index < count; index += 1) {
    if (buffer.readUInt32LE(offset) !== 0x02014b50) {
      return null;
    }
    const method = buffer.readUInt16LE(offset + 10);
    const compressedSize = buffer.readUInt32LE(offset + 20);
    const nameLength = buffer.readUInt16LE(offset + 28);
    const extraLength = buffer.readUInt16LE(offset + 30);
    const commentLength = buffer.readUInt16LE(offset + 32);
    const localOffset = buffer.readUInt32LE(offset + 42);
    const name = buffer.toString('utf8', offset + 46, offset + 46 + nameLength);
    offset += 46 + nameLength + extraLength + commentLength;
    if (name !== entryName) {
      continue;
    }
    const localNameLength = buffer.readUInt16LE(localOffset + 26);
    const localExtraLength = buffer.readUInt16LE(localOffset + 28);
    const start = localOffset + 30 + localNameLength + localExtraLength;
    const raw = buffer.subarray(start, start + compressedSize);
    return method === 0 ? Buffer.from(raw) : zlib.inflateRawSync(raw);
  }
  return null;
}

/* Version metadata, cached next to the game. */
export async function versionMeta(signal) {
  const cached = path.join(versionDir(), `${MC_VERSION}.json`);
  if (fs.existsSync(cached)) {
    try {
      return JSON.parse(fs.readFileSync(cached, 'utf8'));
    } catch {
      /* refetch below */
    }
  }
  const manifest = await fetchJson(VERSION_MANIFEST, signal);
  const entry = manifest.versions.find((version) => version.id === MC_VERSION);
  if (!entry) {
    throw new Error(`Minecraft ${MC_VERSION} is missing from the version manifest.`);
  }
  const meta = await fetchJson(entry.url, signal);
  fs.mkdirSync(versionDir(), { recursive: true });
  fs.writeFileSync(cached, JSON.stringify(meta, null, 2), 'utf8');
  return meta;
}

function distProblems(distDir) {
  const required = ['MixClient.jar', 'minecraft-1.8.9-mcp.jar', 'MixClient-Launcher.jar'];
  return required
    .filter((name) => !fs.existsSync(path.join(distDir, name)))
    .map((name) => path.join(distDir, name));
}

export async function ensureInstalled({ distDir, onProgress = () => {}, signal } = {}) {
  const missing = distProblems(distDir);
  if (missing.length) {
    throw new Error(`Build files are missing: ${missing.join(', ')}. Run gradlew dist.`);
  }

  const meta = await versionMeta(signal);
  onProgress({ phase: 'client', done: 0, total: 1, label: 'Checking the game files…' });

  const jar = path.join(versionDir(), `${MC_VERSION}.jar`);
  await download(meta.downloads.client.url, jar, { sha1: meta.downloads.client.sha1, signal });
  onProgress({ phase: 'client', done: 1, total: 1, label: 'Game jar ready' });

  const libraries = (meta.libraries ?? []).filter(allowed);
  let done = 0;
  for (const library of libraries) {
    if (signal?.aborted) throw new Error('Download cancelled.');
    const artifact = library.downloads?.artifact;
    if (!artifact) continue;
    await download(artifact.url, path.join(librariesDir(), mavenPath(library.name)), {
      sha1: artifact.sha1,
      signal
    });
    done += 1;
    onProgress({
      phase: 'libraries',
      done,
      total: libraries.length,
      label: `Libraries ${done}/${libraries.length}`
    });
  }

  const natives = nativesDir();
  fs.mkdirSync(natives, { recursive: true });
  for (const library of libraries) {
    const nativeClassifier = library.natives?.[platformOs()];
    if (!nativeClassifier) continue;
    const resolved = nativeClassifier.replace('${arch}', archBits());
    const target = path.join(librariesDir(), mavenPath(library.name, resolved));
    const artifact = library.downloads?.classifiers?.[resolved];
    if (artifact?.url) {
      await download(artifact.url, target, { sha1: artifact.sha1, signal });
    }
    if (fs.existsSync(target)) {
      extractZip(target, natives);
    } else {
      throw new Error(`Missing native library: ${library.name}:${resolved}`);
    }
  }
  onProgress({ phase: 'natives', done: 1, total: 1, label: 'Natives extracted' });

  const assetIndex = meta.assetIndex;
  const indexFile = path.join(assetsDir(), 'indexes', `${assetIndex.id}.json`);
  let index;
  if (fs.existsSync(indexFile)) {
    index = JSON.parse(fs.readFileSync(indexFile, 'utf8'));
  } else {
    index = await fetchJson(assetIndex.url, signal);
    fs.mkdirSync(path.dirname(indexFile), { recursive: true });
    fs.writeFileSync(indexFile, JSON.stringify(index), 'utf8');
  }

  const entries = Object.entries(index.objects ?? {});
  let downloaded = 0;
  let cursor = 0;
  const worker = async () => {
    while (cursor < entries.length) {
      if (signal?.aborted) return;
      const [, object] = entries[cursor];
      cursor += 1;
      const hash = object.hash;
      const target = path.join(assetsDir(), 'objects', hash.slice(0, 2), hash);
      if (!fs.existsSync(target) || fs.statSync(target).size !== object.size) {
        await download(`${ASSET_BASE}/${hash.slice(0, 2)}/${hash}`, target, { sha1: hash, signal });
      }
      downloaded += 1;
      if (downloaded % 25 === 0 || downloaded === entries.length) {
        onProgress({
          phase: 'assets',
          done: downloaded,
          total: entries.length,
          label: `Assets ${downloaded}/${entries.length}`
        });
      }
    }
  };
  await Promise.all(Array.from({ length: 8 }, worker));
  if (signal?.aborted) throw new Error('Download cancelled.');
  onProgress({ phase: 'assets', done: entries.length, total: entries.length, label: 'Assets ready' });

  return { dir: minecraftDir(), assets: assetIndex.id, jar };
}

export function installationStatus(distDir) {
  return {
    minecraftDir: minecraftDir(),
    gameDir: gameDir(),
    dataDir: dataDir(),
    distDir,
    jar: fs.existsSync(path.join(versionDir(), `${MC_VERSION}.jar`)),
    assets: fs.existsSync(path.join(assetsDir(), 'indexes', '1.8.json')),
    libraries: fs.existsSync(librariesDir()),
    missingBuild: distProblems(distDir)
  };
}
