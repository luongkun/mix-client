/* Builds the update manifest the launcher reads at start up.
 *
 *   node tools/make-update-manifest.mjs --base https://cdn.example.com/mixclient
 *   node tools/make-update-manifest.mjs --base https://cdn.example.com/mixclient \
 *        --version 1.1.2 --changelog "OptiFine flicker fix"
 *   node tools/make-update-manifest.mjs --base https://host/mixclient \
 *        --launcher launcher-desktop/release/win-unpacked/resources/app.asar
 *
 * Two sections:
 *   files    — the game build (build/dist): jars, lib/, optifine/
 *   launcher — the Electron app (app.asar), staged and swapped on quit
 *
 * Upload the whole dist folder plus update.json to --base; the launcher
 * downloads only what the manifest lists and verifies every sha256.
 */

import crypto from 'node:crypto';
import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

/* defaults resolve against the repo, not the caller's cwd, so the npm script
   works from launcher-desktop/ as well */
const repoRoot = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');

const SKIP = new Set([
  /* the running launcher jar cannot replace itself mid-flight */
  'MixClient-Launcher.jar',
  'version.json',
  'update.json',
  'launch-test.json'
]);
const SKIP_DIRS = new Set(['.update']);

function argument(name, fallback) {
  const index = process.argv.indexOf(`--${name}`);
  if (index >= 0 && process.argv[index + 1]) {
    return process.argv[index + 1];
  }
  const inline = process.argv.find((entry) => entry.startsWith(`--${name}=`));
  return inline ? inline.slice(name.length + 3) : fallback;
}

const dist = path.resolve(argument('dist', path.join(repoRoot, 'build/dist')));
const base = argument('base', '').replace(/\/+$/, '');
const changelog = argument('changelog', '');
const out = path.resolve(argument('out', path.join(dist, 'update.json')));

if (!base) {
  console.error('Missing --base (the URL prefix the dist folder is served from).');
  process.exit(1);
}
if (!/^https:\/\//.test(base) && !/^http:\/\/127\.0\.0\.1(:|$)/.test(base)) {
  console.error('--base must be HTTPS (the launcher refuses anything else).');
  process.exit(1);
}

function walk(dir, prefix = '') {
  const entries = [];
  for (const entry of fs.readdirSync(dir, { withFileTypes: true })) {
    const relative = prefix ? `${prefix}/${entry.name}` : entry.name;
    if (entry.isDirectory()) {
      if (SKIP_DIRS.has(entry.name)) {
        continue;
      }
      entries.push(...walk(path.join(dir, entry.name), relative));
      continue;
    }
    if (SKIP.has(relative)) {
      continue;
    }
    entries.push(relative);
  }
  return entries;
}

function sha256(file) {
  return crypto.createHash('sha256').update(fs.readFileSync(file)).digest('hex');
}

let version = argument('version', '');
if (!version) {
  try {
    version = JSON.parse(fs.readFileSync(path.join(dist, 'version.json'), 'utf8')).version;
  } catch {
    console.error('No --version given and no readable version.json in ' + dist);
    process.exit(1);
  }
}

const files = walk(dist)
  .sort()
  .map((relative) => {
    const absolute = path.join(dist, relative);
    return {
      path: relative,
      url: `${base}/${relative}`,
      sha256: sha256(absolute),
      size: fs.statSync(absolute).size
    };
  });

/* ---- launcher (app.asar) ---------------------------------------------- */

function readLauncherVersion() {
  try {
    return JSON.parse(fs.readFileSync(path.join(repoRoot, 'launcher-desktop/package.json'), 'utf8')).version;
  } catch {
    return version;
  }
}

/* Accepts the asar itself, the resources folder or an unpacked build. */
function resolveLauncher(input) {
  const candidate = path.resolve(input);
  try {
    if (fs.statSync(candidate).isFile()) {
      return { asar: candidate, root: path.dirname(candidate) };
    }
  } catch {
    /* not a file: try the folders below */
  }
  for (const relative of ['app.asar', 'resources/app.asar']) {
    const asar = path.join(candidate, relative);
    if (fs.existsSync(asar)) {
      return { asar, root: path.dirname(asar) };
    }
  }
  return null;
}

const launcherInput = argument('launcher', '');
const launcherVersion = argument('launcher-version', '') || readLauncherVersion();
let launcher = null;

if (launcherInput) {
  const resolved = resolveLauncher(launcherInput);
  if (!resolved) {
    console.error(`No app.asar found at ${launcherInput}`);
    process.exit(1);
  }
  const entries = ['app.asar'];
  const unpacked = path.join(resolved.root, 'app.asar.unpacked');
  if (fs.existsSync(unpacked)) {
    entries.push(...walk(unpacked, 'app.asar.unpacked'));
  }
  const launcherFiles = entries.sort().map((relative) => {
    const absolute = path.join(resolved.root, relative);
    return {
      path: relative,
      url: `${base}/launcher/${relative}`,
      sha256: sha256(absolute),
      size: fs.statSync(absolute).size
    };
  });
  launcher = { version: String(launcherVersion), files: launcherFiles };
}

const manifest = { version: String(version), changelog, generatedAt: Date.now(), files };
if (launcher) {
  manifest.launcher = launcher;
}
fs.writeFileSync(out, JSON.stringify(manifest, null, 2), 'utf8');

const bytes = files.reduce((total, file) => total + file.size, 0);
console.log(`update.json: version ${manifest.version}, ${files.length} files, ${(bytes / 1048576).toFixed(1)} MB`);
if (launcher) {
  const launcherBytes = launcher.files.reduce((total, file) => total + file.size, 0);
  console.log(
    `launcher ${launcher.version}: ${launcher.files.length} file(s), ${(launcherBytes / 1048576).toFixed(1)} MB (served from ${base}/launcher/)`
  );
} else {
  console.log('launcher: not included (pass --launcher <app.asar|resources dir>)');
}
console.log(`written to ${out}`);
for (const file of files) {
  console.log(`  ${file.path}  ${(file.size / 1024).toFixed(0)} KB`);
}
