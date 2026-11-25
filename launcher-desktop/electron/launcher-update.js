/* Launcher self-update.

   The Electron app is `resources/app.asar`, and Windows will not let a running
   process replace the file it booted from. So the new asar is downloaded into
   the user data folder, and a small helper — the same Electron binary running
   with ELECTRON_RUN_AS_NODE=1, so no extra runtime is needed — waits for this
   process to exit, swaps the file (keeping the old one until the copy has been
   verified) and starts the app again.

   Only `app.asar` and `app.asar.unpacked/*` are handled here: the Electron
   runtime and the bundled JRE change only with a full installer. */

import crypto from 'node:crypto';
import fs from 'node:fs';
import path from 'node:path';
import { spawn } from 'node:child_process';
import { pipeline } from 'node:stream/promises';
import { Readable } from 'node:stream';

const USER_AGENT = 'MixClient/1.0';
const HELPER = 'apply-launcher.cjs';
const PENDING = 'pending.json';
const LOG = 'apply-launcher.log';

function digest(file) {
  return crypto.createHash('sha256').update(fs.readFileSync(file)).digest('hex');
}

async function download(url, target, signal) {
  fs.mkdirSync(path.dirname(target), { recursive: true });
  const response = await fetch(url, { headers: { 'User-Agent': USER_AGENT }, signal });
  if (!response.ok) {
    throw new Error(`HTTP ${response.status} for ${url}`);
  }
  await pipeline(Readable.fromWeb(response.body), fs.createWriteStream(target));
}

export const pendingPath = (workDir) => path.join(workDir, PENDING);
export const logPath = (workDir) => path.join(workDir, LOG);

/* Electron patches `fs` so that any path with an `app.asar` segment is treated
   as an archive: writing to `<staging>/app.asar` from the main process fails
   with "Invalid package". The staged copy therefore gets a different name and
   the helper — plain Node, no asar patching — renames it into place. */
const STAGED_ASAR = 'app.asar.download';
const stagedName = (file) => (file === 'app.asar' ? STAGED_ASAR : file);

/* The staged update is only usable while every file is still there and still
   hashes to what the manifest said. */
export function pending(workDir) {
  try {
    const state = JSON.parse(fs.readFileSync(pendingPath(workDir), 'utf8'));
    if (!state?.dir || !fs.existsSync(state.dir)) {
      return null;
    }
    for (const file of state.files ?? []) {
      const full = path.join(state.dir, file.staged ?? stagedName(file.path));
      if (!fs.existsSync(full)) {
        return null;
      }
      if (file.sha256 && digest(full) !== file.sha256.toLowerCase()) {
        return null;
      }
    }
    return state;
  } catch {
    return null;
  }
}

export function clear(workDir) {
  try {
    const state = pending(workDir);
    if (state?.dir) {
      fs.rmSync(state.dir, { recursive: true, force: true });
    }
  } catch {
    /* nothing to clean */
  }
  fs.rmSync(pendingPath(workDir), { force: true });
}

export async function stage({ version, files, workDir, onProgress = () => {}, signal }) {
  const dir = path.join(workDir, `staged-${version}`);
  fs.rmSync(dir, { recursive: true, force: true });
  fs.mkdirSync(dir, { recursive: true });

  const staged = [];
  let index = 0;
  for (const file of files) {
    if (signal?.aborted) {
      throw new Error('Update cancelled.');
    }
    onProgress({ done: index, total: files.length, label: `Downloading launcher ${file.path}` });
    const target = path.join(dir, stagedName(file.path));
    await download(file.url, target, signal);
    if (file.sha256 && digest(target) !== file.sha256.toLowerCase()) {
      throw new Error(`SHA-256 mismatch for the launcher ${file.path}`);
    }
    staged.push({ path: file.path, staged: stagedName(file.path), sha256: file.sha256 ?? null });
    index += 1;
  }

  const state = { version, dir, files: staged, stagedAt: Date.now() };
  fs.mkdirSync(workDir, { recursive: true });
  fs.writeFileSync(pendingPath(workDir), JSON.stringify(state, null, 2), 'utf8');
  onProgress({ done: files.length, total: files.length, label: `Launcher ${version} is ready` });
  return state;
}

/* The helper is plain CommonJS run by the Electron binary as Node: it has to
   work with no app, no window and no preload. */
const HELPER_SOURCE = `const fs = require('node:fs');
const path = require('node:path');
const { spawn } = require('node:child_process');

const config = JSON.parse(fs.readFileSync(process.argv[2], 'utf8'));
const logFile = config.logFile;

function log(line) {
  try {
    fs.appendFileSync(logFile, '[' + new Date().toISOString() + '] ' + line + '\\n');
  } catch (error) {
    /* logging must never stop the update */
  }
}

function sleep(ms) {
  Atomics.wait(new Int32Array(new SharedArrayBuffer(4)), 0, 0, ms);
}

function alive(pid) {
  try {
    process.kill(pid, 0);
    return true;
  } catch (error) {
    return false;
  }
}

function copyFile(from, to) {
  fs.mkdirSync(path.dirname(to), { recursive: true });
  fs.copyFileSync(from, to);
}

log('helper started for pid ' + config.pid + ' (launcher ' + config.version + ')');

/* wait for the app to release app.asar, 60 s at most */
for (let attempt = 0; attempt < 120 && alive(config.pid); attempt += 1) {
  sleep(500);
}
if (alive(config.pid)) {
  log('pid ' + config.pid + ' is still running after 60 s, giving up');
  process.exit(1);
}

const replaced = [];
let failed = null;
for (const file of config.files) {
  const source = path.join(config.dir, file.staged || file.path);
  const target = path.join(config.targetDir, file.path);
  const backup = target + '.old';
  let copied = false;
  for (let attempt = 0; attempt < 40 && !copied; attempt += 1) {
    try {
      if (fs.existsSync(target)) {
        fs.rmSync(backup, { force: true });
        fs.renameSync(target, backup);
      }
      copyFile(source, target);
      copied = true;
    } catch (error) {
      if (attempt === 39) {
        failed = file.path + ': ' + error.message;
        /* put the previous file back if the rename already happened */
        try {
          if (!fs.existsSync(target) && fs.existsSync(backup)) {
            fs.renameSync(backup, target);
          }
        } catch (restoreError) {
          log('could not restore ' + file.path + ': ' + restoreError.message);
        }
      } else {
        sleep(250);
      }
    }
  }
  if (!copied) {
    break;
  }
  replaced.push({ target, backup });
  log('replaced ' + file.path);
}

if (failed) {
  log('update failed: ' + failed);
  for (const entry of replaced) {
    try {
      if (fs.existsSync(entry.backup)) {
        fs.rmSync(entry.target, { force: true });
        fs.renameSync(entry.backup, entry.target);
      }
    } catch (error) {
      log('rollback failed for ' + entry.target + ': ' + error.message);
    }
  }
  log('rolled back ' + replaced.length + ' file(s)');
} else {
  for (const entry of replaced) {
    try {
      fs.rmSync(entry.backup, { force: true });
    } catch (error) {
      /* a leftover .old is harmless */
    }
  }
  log('launcher ' + config.version + ' installed');
}

try {
  fs.rmSync(path.join(config.workDir, 'pending.json'), { force: true });
} catch (error) {
  /* ignore */
}

if (config.relaunch) {
  const env = { ...process.env };
  /* the child must not inherit ELECTRON_RUN_AS_NODE or it starts as plain node */
  delete env.ELECTRON_RUN_AS_NODE;
  delete env.NODE_OPTIONS;
  try {
    const child = spawn(config.exe, config.args ?? [], { detached: true, stdio: 'ignore', env, windowsHide: false });
    child.unref();
    log('relaunched ' + config.exe);
  } catch (error) {
    log('could not relaunch: ' + error.message);
  }
}
`;

function writeHelper(workDir) {
  fs.mkdirSync(workDir, { recursive: true });
  const file = path.join(workDir, HELPER);
  let current = '';
  try {
    current = fs.readFileSync(file, 'utf8');
  } catch {
    current = '';
  }
  if (current !== HELPER_SOURCE) {
    fs.writeFileSync(file, HELPER_SOURCE, 'utf8');
  }
  return file;
}

/**
 * Hand the staged files to the helper. `relaunch` decides whether the app
 * comes back on its own (the "Restart & update" button) or the player starts
 * it again later (a normal quit).
 */
export function applyOnQuit({ workDir, targetDir, exe, args = [], relaunch = false, pid = process.pid }) {
  const state = pending(workDir);
  if (!state) {
    return { ok: false, message: 'No launcher update is staged.' };
  }
  if (!fs.existsSync(path.join(targetDir, 'app.asar'))) {
    return { ok: false, message: `app.asar not found in ${targetDir}` };
  }

  const helper = writeHelper(workDir);
  const configFile = path.join(workDir, 'apply.json');
  fs.writeFileSync(
    configFile,
    JSON.stringify(
      {
        pid,
        version: state.version,
        dir: state.dir,
        workDir,
        files: state.files,
        targetDir,
        exe,
        args,
        relaunch,
        logFile: logPath(workDir)
      },
      null,
      2
    ),
    'utf8'
  );

  const env = { ...process.env, ELECTRON_RUN_AS_NODE: '1' };
  const child = spawn(process.execPath, [helper, configFile], {
    detached: true,
    stdio: 'ignore',
    windowsHide: true,
    env
  });
  child.unref();
  return { ok: true, version: state.version, log: logPath(workDir) };
}
