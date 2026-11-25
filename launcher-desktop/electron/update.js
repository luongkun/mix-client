/* Client update: a manifest lists the files to replace in the build folder.

   Two things this module guarantees, because a half-written jar means a client
   that will not start:
   - every download is verified (sha256, or sha1 for old manifests) before it is
     allowed near the build folder;
   - the swap keeps the previous file until the whole set is in place, so a
     failure half way through puts everything back.

   Paths and URLs are validated too: a manifest must not be able to write
   outside the folder it names. */

import crypto from 'node:crypto';
import fs from 'node:fs';
import path from 'node:path';
import { pipeline } from 'node:stream/promises';
import { Readable } from 'node:stream';

const USER_AGENT = 'MixClient/1.0';

function validateUrl(url) {
  if (!url || (!url.startsWith('https://') && !url.startsWith('http://127.0.0.1'))) {
    throw new Error(`Update URL must be HTTPS: ${url}`);
  }
}

function validatePath(entry) {
  if (!entry || entry.includes('..') || entry.startsWith('/') || entry.startsWith('\\') || /^[A-Za-z]:/.test(entry)) {
    throw new Error(`Invalid update path: ${entry}`);
  }
}

/* The launcher's own files live next to the Electron binary; only the asar and
   its unpacked side-car are updatable this way (the runtime itself needs the
   installer). */
function validateLauncherPath(entry) {
  validatePath(entry);
  if (entry !== 'app.asar' && !entry.startsWith('app.asar.unpacked/')) {
    throw new Error(`Invalid launcher update path: ${entry}`);
  }
}

async function readJson(url) {
  validateUrl(url);
  const response = await fetch(url, { headers: { 'User-Agent': USER_AGENT } });
  if (!response.ok) {
    throw new Error(`HTTP ${response.status} for ${url}`);
  }
  return response.json();
}

function readFiles(list, validate) {
  return (list ?? []).map((file) => {
    validate(file.path);
    validateUrl(file.url);
    if (path.basename(file.path) === 'MixClient-Launcher.jar') {
      throw new Error('The manifest must not replace the running launcher jar.');
    }
    return {
      path: file.path,
      url: file.url,
      sha256: file.sha256 ?? null,
      sha1: file.sha1 ?? null,
      size: Number(file.size) || 0
    };
  });
}

export async function check(manifestUrl) {
  const root = await readJson(manifestUrl);
  if (!root.version) {
    throw new Error('The update manifest has no version.');
  }
  const files = readFiles(root.files, validatePath);
  const launcher = root.launcher
    ? {
        version: String(root.launcher.version ?? root.version),
        files: readFiles(root.launcher.files, validateLauncherPath)
      }
    : null;
  if (launcher && launcher.files.length === 0) {
    throw new Error('The manifest has an empty launcher section.');
  }
  return { version: String(root.version), changelog: root.changelog ?? '', files, launcher };
}

export function localVersion(distDir) {
  try {
    const file = path.join(distDir, 'version.json');
    return JSON.parse(fs.readFileSync(file, 'utf8')).version ?? 'unknown';
  } catch {
    return 'unknown';
  }
}

export function isNewer(local, remote) {
  if (!local || local === 'unknown') {
    return true;
  }
  if (local === remote) {
    return false;
  }
  const left = String(local).split('.');
  const right = String(remote).split('.');
  const length = Math.max(left.length, right.length);
  for (let index = 0; index < length; index += 1) {
    const a = Number((left[index] ?? '0').replace(/\D/g, '')) || 0;
    const b = Number((right[index] ?? '0').replace(/\D/g, '')) || 0;
    if (a !== b) {
      return b > a;
    }
  }
  return local !== remote;
}

function digest(file, algorithm) {
  const hash = crypto.createHash(algorithm);
  hash.update(fs.readFileSync(file));
  return hash.digest('hex');
}

async function download(url, target, signal) {
  validateUrl(url);
  fs.mkdirSync(path.dirname(target), { recursive: true });
  const response = await fetch(url, { headers: { 'User-Agent': USER_AGENT }, signal });
  if (!response.ok) {
    throw new Error(`HTTP ${response.status} for ${url}`);
  }
  await pipeline(Readable.fromWeb(response.body), fs.createWriteStream(target));
}

function verify(file, target) {
  if (file.sha256 && digest(target, 'sha256') !== file.sha256.toLowerCase()) {
    throw new Error(`SHA-256 mismatch for ${file.path}`);
  }
  if (!file.sha256 && file.sha1 && digest(target, 'sha1') !== file.sha1.toLowerCase()) {
    throw new Error(`SHA-1 mismatch for ${file.path}`);
  }
  if (file.size && fs.statSync(target).size !== file.size) {
    throw new Error(`Size mismatch for ${file.path}`);
  }
}

/* A second check on the same build is the common case (every start), so a file
   that already matches is not downloaded again. */
function matches(file, target) {
  try {
    if (!fs.existsSync(target)) {
      return false;
    }
    if (file.sha256) {
      return digest(target, 'sha256') === file.sha256.toLowerCase();
    }
    if (file.sha1) {
      return digest(target, 'sha1') === file.sha1.toLowerCase();
    }
    return file.size > 0 && fs.statSync(target).size === file.size;
  } catch {
    return false;
  }
}

export async function apply(update, distDir, onProgress = () => {}, signal) {
  const staging = path.join(distDir, '.update');
  const backups = path.join(staging, 'backup');
  fs.rmSync(staging, { recursive: true, force: true });
  fs.mkdirSync(staging, { recursive: true });

  try {
    const staged = [];
    let index = 0;
    for (const file of update.files) {
      if (signal?.aborted) {
        throw new Error('Update cancelled.');
      }
      const target = path.join(distDir, file.path);
      index += 1;
      if (matches(file, target)) {
        onProgress({ done: index, total: update.files.length, label: `${file.path} is current` });
        continue;
      }
      onProgress({ done: index - 1, total: update.files.length, label: `Downloading ${file.path}` });
      const temp = path.join(staging, file.path);
      await download(file.url, temp, signal);
      verify(file, temp);
      staged.push({ file, temp, target });
      onProgress({ done: index, total: update.files.length, label: `Downloaded ${file.path}` });
    }

    /* Swap only after every file is downloaded and verified. The old file is
       kept under .update/backup until the whole set is in place. */
    const swapped = [];
    try {
      for (const item of staged) {
        fs.mkdirSync(path.dirname(item.target), { recursive: true });
        if (fs.existsSync(item.target)) {
          const backup = path.join(backups, item.file.path);
          fs.mkdirSync(path.dirname(backup), { recursive: true });
          fs.renameSync(item.target, backup);
        }
        fs.renameSync(item.temp, item.target);
        swapped.push(item);
      }
    } catch (error) {
      for (const item of swapped.reverse()) {
        try {
          fs.rmSync(item.target, { force: true });
          const backup = path.join(backups, item.file.path);
          if (fs.existsSync(backup)) {
            fs.renameSync(backup, item.target);
          }
        } catch {
          /* restoring is best effort; the error below is what the player sees */
        }
      }
      throw new Error(`Could not replace the game files: ${String(error.message ?? error)}`);
    }

    fs.writeFileSync(
      path.join(distDir, 'version.json'),
      JSON.stringify({ version: update.version, updatedAt: Date.now() }, null, 2),
      'utf8'
    );
    onProgress({ done: update.files.length, total: update.files.length, label: `Updated to ${update.version}` });
  } finally {
    fs.rmSync(staging, { recursive: true, force: true });
  }
}
