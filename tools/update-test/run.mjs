/* End-to-end test for the update system, without touching a real install.

   Serves a fake feed on 127.0.0.1 (the launcher only accepts HTTPS or loopback)
   and drives the three real modules:
     update.js           — game files: verify, swap, skip, rollback
     launcher-update.js  — app.asar staging + the helper that swaps on exit
     make-update-manifest.mjs — the manifest both of them read

   node tools/update-test/run.mjs
*/

import crypto from 'node:crypto';
import fs from 'node:fs';
import http from 'node:http';
import os from 'node:os';
import path from 'node:path';
import { spawn, spawnSync } from 'node:child_process';
import { fileURLToPath, pathToFileURL } from 'node:url';

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..', '..');
const work = fs.mkdtempSync(path.join(os.tmpdir(), 'mixclient-update-'));
const feedDir = path.join(work, 'feed');
const dist = path.join(work, 'dist');
const resources = path.join(work, 'resources');
const userData = path.join(work, 'userdata');
const marker = path.join(work, 'relaunched.txt');

for (const dir of [feedDir, dist, resources, userData]) {
  fs.mkdirSync(dir, { recursive: true });
}

let failures = 0;
function check(label, condition, detail = '') {
  if (condition) {
    console.log(`  ok   ${label}`);
  } else {
    failures += 1;
    console.log(`  FAIL ${label}${detail ? ` — ${detail}` : ''}`);
  }
}

const sha256 = (buffer) => crypto.createHash('sha256').update(buffer).digest('hex');
const write = (file, content) => {
  fs.mkdirSync(path.dirname(file), { recursive: true });
  fs.writeFileSync(file, content);
  return content;
};
const read = (file) => (fs.existsSync(file) ? fs.readFileSync(file, 'utf8') : null);

/* ---------------- feed server ---------------- */

const server = http.createServer((request, response) => {
  const target = path.join(feedDir, decodeURIComponent(request.url.split('?')[0]));
  if (!target.startsWith(feedDir) || !fs.existsSync(target) || fs.statSync(target).isDirectory()) {
    response.writeHead(404).end('not found');
    return;
  }
  response.writeHead(200, { 'content-type': target.endsWith('.json') ? 'application/json' : 'application/octet-stream' });
  fs.createReadStream(target).pipe(response);
});

await new Promise((resolve) => server.listen(0, '127.0.0.1', resolve));
const base = `http://127.0.0.1:${server.address().port}`;
console.log(`feed ${base} · work ${work}`);

const updater = await import(pathToFileURL(path.join(root, 'launcher-desktop/electron/update.js')).href);
const launcherUpdate = await import(pathToFileURL(path.join(root, 'launcher-desktop/electron/launcher-update.js')).href);

function manifestFor(version, files, launcher) {
  return { version, changelog: 'test build', generatedAt: Date.now(), files, ...(launcher ? { launcher } : {}) };
}

function fileEntry(relative, content) {
  return { path: relative, url: `${base}/${relative}`, sha256: sha256(content), size: Buffer.byteLength(content) };
}

/* ---------------- 1. game update ---------------- */

console.log('\n[1] game files');
write(path.join(dist, 'version.json'), JSON.stringify({ version: '1.2.1' }));
write(path.join(dist, 'MixClient.jar'), 'old-client');
write(path.join(dist, 'lib/keep.jar'), 'keep-me');
write(path.join(feedDir, 'MixClient.jar'), 'new-client');
write(path.join(feedDir, 'lib/fresh.jar'), 'fresh-lib');
write(path.join(feedDir, 'lib/keep.jar'), 'keep-me');

const gameManifest = manifestFor('1.3.0', [
  fileEntry('MixClient.jar', 'new-client'),
  fileEntry('lib/fresh.jar', 'fresh-lib'),
  /* unchanged file: must not be downloaded */
  { path: 'lib/keep.jar', url: `${base}/does-not-exist.jar`, sha256: sha256('keep-me'), size: 7 }
]);
write(path.join(feedDir, 'update.json'), JSON.stringify(gameManifest));

const checked = await updater.check(`${base}/update.json`);
check('manifest parsed', checked.version === '1.3.0' && checked.files.length === 3);
check('newer detected', updater.isNewer('1.2.1', checked.version) === true);

const progress = [];
await updater.apply(checked, dist, (event) => progress.push(event.label));
check('jar replaced', read(path.join(dist, 'MixClient.jar')) === 'new-client');
check('new file added', read(path.join(dist, 'lib/fresh.jar')) === 'fresh-lib');
check('unchanged file untouched', read(path.join(dist, 'lib/keep.jar')) === 'keep-me');
check('version.json written last', JSON.parse(read(path.join(dist, 'version.json'))).version === '1.3.0');
check('staging cleaned up', !fs.existsSync(path.join(dist, '.update')));

/* second run with the feed gone: everything already matches, so nothing is fetched */
fs.rmSync(path.join(feedDir, 'MixClient.jar'));
fs.rmSync(path.join(feedDir, 'lib/fresh.jar'));
await updater.apply(checked, dist);
check('re-apply does not download', read(path.join(dist, 'MixClient.jar')) === 'new-client');

/* rollback: a bad hash must leave the previous build in place */
write(path.join(feedDir, 'MixClient.jar'), 'corrupt-client');
write(path.join(feedDir, 'lib/other.jar'), 'other');
const broken = manifestFor('1.3.1', [
  { path: 'MixClient.jar', url: `${base}/MixClient.jar`, sha256: sha256('expected-but-different'), size: 14 },
  fileEntry('lib/other.jar', 'other')
]);
let rollbackError = null;
try {
  await updater.apply(broken, dist);
} catch (error) {
  rollbackError = error.message;
}
check('bad hash rejected', Boolean(rollbackError) && /SHA-256/.test(rollbackError), rollbackError ?? 'no error');
check('build survived the failure', read(path.join(dist, 'MixClient.jar')) === 'new-client');
check('version untouched after failure', JSON.parse(read(path.join(dist, 'version.json'))).version === '1.3.0');
check('no stray file from the failed update', !fs.existsSync(path.join(dist, 'lib/other.jar')));

/* ---------------- 2. launcher staging ---------------- */

console.log('\n[2] launcher staging');
write(path.join(resources, 'app.asar'), 'old-asar');
write(path.join(feedDir, 'launcher/app.asar'), 'new-asar');
write(path.join(feedDir, 'launcher/app.asar.unpacked/native.node'), 'native');

const launcherFiles = [
  fileEntry('launcher/app.asar', 'new-asar'),
  fileEntry('launcher/app.asar.unpacked/native.node', 'native')
].map((file) => ({ ...file, path: file.path.replace(/^launcher\//, '') }));

const launcherManifest = manifestFor('1.3.0', [], { version: '1.4.0', files: launcherFiles });
write(path.join(feedDir, 'launcher.json'), JSON.stringify(launcherManifest));
const launcherChecked = await updater.check(`${base}/launcher.json`);
check('launcher block parsed', launcherChecked.launcher?.version === '1.4.0' && launcherChecked.launcher.files.length === 2);

const staged = await launcherUpdate.stage({
  version: launcherChecked.launcher.version,
  files: launcherChecked.launcher.files,
  workDir: userData
});
check('staged files downloaded', read(path.join(staged.dir, 'app.asar.download')) === 'new-asar');
check('pending marker written', launcherUpdate.pending(userData)?.version === '1.4.0');

/* a tampered staged file invalidates the pending update */
fs.writeFileSync(path.join(staged.dir, 'app.asar.download'), 'tampered');
check('tampered stage rejected', launcherUpdate.pending(userData) === null);
fs.writeFileSync(path.join(staged.dir, 'app.asar.download'), 'new-asar');
check('restored stage accepted', launcherUpdate.pending(userData)?.version === '1.4.0');

/* ---------------- 3. helper swap ---------------- */

console.log('\n[3] swap on exit');
/* the helper waits for this pid to disappear; a dummy process stands in for
   the launcher so the test can watch the swap happen */
const dummy = spawn(process.execPath, ['-e', 'setTimeout(() => {}, 60000)'], { stdio: 'ignore' });
const started = launcherUpdate.applyOnQuit({
  workDir: userData,
  targetDir: resources,
  exe: process.execPath,
  args: ['-e', `require('fs').writeFileSync(${JSON.stringify(marker)}, 'relaunched')`],
  relaunch: true,
  pid: dummy.pid
});
check('helper accepted the staged build', started.ok === true, started.message ?? '');

await new Promise((resolve) => setTimeout(resolve, 700));
check('old asar still in place while the app runs', read(path.join(resources, 'app.asar')) === 'old-asar');
dummy.kill();

for (let attempt = 0; attempt < 40 && read(path.join(resources, 'app.asar')) !== 'new-asar'; attempt += 1) {
  await new Promise((resolve) => setTimeout(resolve, 250));
}
check('asar swapped after exit', read(path.join(resources, 'app.asar')) === 'new-asar');
check('unpacked file written', read(path.join(resources, 'app.asar.unpacked/native.node')) === 'native');
check('backup removed on success', !fs.existsSync(path.join(resources, 'app.asar.old')));
check('pending marker cleared', launcherUpdate.pending(userData) === null);
check('relaunch command ran', read(marker) === 'relaunched');

const log = read(launcherUpdate.logPath(userData)) ?? '';
check('log records the install', /launcher 1\.4\.0 installed/.test(log), log.trim().split('\n').slice(-3).join(' | '));

/* ---------------- 4. manifest tool ---------------- */

console.log('\n[4] manifest generator');
const generated = path.join(feedDir, 'generated.json');
const tool = spawnSync(
  process.execPath,
  [
    path.join(root, 'tools/make-update-manifest.mjs'),
    '--base',
    base,
    '--dist',
    dist,
    '--out',
    generated,
    '--launcher',
    resources,
    '--launcher-version',
    '1.4.0',
    '--version',
    '1.3.0'
  ],
  { encoding: 'utf8' }
);
check('tool exits cleanly', tool.status === 0, tool.stderr?.trim() ?? '');
const emitted = JSON.parse(read(generated) ?? '{}');
check('tool emits the launcher section', emitted.launcher?.version === '1.4.0');
check('tool hashes the asar', emitted.launcher?.files?.[0]?.sha256 === sha256('new-asar'));
check('tool keeps the game files', emitted.files?.some((file) => file.path === 'MixClient.jar'));
const roundTrip = await updater.check(`${base}/generated.json`).catch(() => null);
check('emitted manifest is accepted by the client', Boolean(roundTrip?.launcher));

server.close();
console.log(`\n${failures === 0 ? 'UPDATE TESTS OK' : `${failures} FAILURE(S)`} · artifacts in ${work}`);
process.exit(failures === 0 ? 0 : 1);
