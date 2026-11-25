/* Packaged-app check for the launcher self-update.

   Runs the real unpacked build (release-test/win-unpacked) against a loopback
   feed, with a throwaway user-data folder, and asserts that the launcher
   downloads and stages its own app.asar without touching the running one.

   Needs a packaged build first:
     npm --prefix launcher-desktop run pack -- -c.directories.output=release-test

   node tools/update-test/packaged.mjs
*/

import crypto from 'node:crypto';
import fs from 'node:fs';
import http from 'node:http';
import os from 'node:os';
import path from 'node:path';
import { spawn, spawnSync } from 'node:child_process';
import { fileURLToPath } from 'node:url';

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..', '..');
const unpacked = path.resolve(process.argv[2] ?? path.join(root, 'launcher-desktop/release-test/win-unpacked'));
const exe = path.join(unpacked, 'MixClient.exe');
const resources = path.join(unpacked, 'resources');
const asar = path.join(resources, 'app.asar');

if (!fs.existsSync(exe) || !fs.existsSync(asar)) {
  console.error(`No packaged build at ${unpacked}. Run: npm --prefix launcher-desktop run pack -- -c.directories.output=release-test`);
  process.exit(2);
}

const work = fs.mkdtempSync(path.join(os.tmpdir(), 'mixclient-packaged-'));
const feed = path.join(work, 'feed');
const userData = path.join(work, 'userdata');
fs.mkdirSync(path.join(feed, 'launcher'), { recursive: true });
fs.mkdirSync(userData, { recursive: true });
fs.copyFileSync(asar, path.join(feed, 'launcher/app.asar'));

const sha256 = (file) => crypto.createHash('sha256').update(fs.readFileSync(file)).digest('hex');
const asarHash = sha256(asar);

let failures = 0;
const check = (label, condition, detail = '') => {
  console.log(`  ${condition ? 'ok  ' : 'FAIL'} ${label}${detail && !condition ? ` — ${detail}` : ''}`);
  if (!condition) failures += 1;
};

const server = http.createServer((request, response) => {
  const target = path.join(feed, decodeURIComponent(request.url.split('?')[0]));
  if (!target.startsWith(feed) || !fs.existsSync(target) || fs.statSync(target).isDirectory()) {
    response.writeHead(404).end('not found');
    return;
  }
  response.writeHead(200, { 'content-type': target.endsWith('.json') ? 'application/json' : 'application/octet-stream' });
  fs.createReadStream(target).pipe(response);
});
await new Promise((resolve) => server.listen(0, '127.0.0.1', resolve));
const base = `http://127.0.0.1:${server.address().port}`;

/* the game section is the build the package already ships (so it is a no-op);
   the launcher section is one version ahead */
const manifestFile = path.join(feed, 'update.json');
const tool = spawnSync(
  process.execPath,
  [
    path.join(root, 'tools/make-update-manifest.mjs'),
    '--base', base,
    '--dist', path.join(resources, 'dist'),
    '--out', manifestFile,
    '--version', '1.2.1',
    '--launcher', resources,
    '--launcher-version', '1.3.0',
    '--changelog', 'packaged self-update check'
  ],
  { encoding: 'utf8' }
);
if (tool.status !== 0) {
  console.error(tool.stderr || tool.stdout);
  process.exit(2);
}

console.log(`feed ${base} · userData ${userData}`);
/* the agent shell exports ELECTRON_RUN_AS_NODE, which would turn the app into
   plain node and exit immediately */
const appEnv = { ...process.env, MIXCLIENT_UPDATE_URL: `${base}/update.json`, ELECTRON_ENABLE_LOGGING: '0' };
delete appEnv.ELECTRON_RUN_AS_NODE;
delete appEnv.NODE_OPTIONS;
const app = spawn(exe, [`--user-data-dir=${userData}`], {
  env: appEnv,
  stdio: ['ignore', fs.openSync(path.join(work, 'app.out.log'), 'a'), fs.openSync(path.join(work, 'app.err.log'), 'a')],
  detached: false
});

const pendingFile = path.join(userData, 'update', 'pending.json');
let pending = null;
for (let attempt = 0; attempt < 60; attempt += 1) {
  await new Promise((resolve) => setTimeout(resolve, 500));
  try {
    pending = JSON.parse(fs.readFileSync(pendingFile, 'utf8'));
    break;
  } catch {
    /* still starting or still downloading */
  }
  if (app.exitCode !== null) {
    break;
  }
}

console.log('\npackaged launcher');
check('app started', app.exitCode === null, `exited with ${app.exitCode}`);
check('launcher update staged', pending?.version === '1.3.0', pending ? JSON.stringify(pending.version) : 'no pending.json');
if (pending) {
  /* Electron's fs refuses to touch a path named app.asar, so the staged copy
     is app.asar.download until the helper renames it */
  const staged = path.join(pending.dir, 'app.asar.download');
  check('staged asar downloaded', fs.existsSync(staged));
  check('staged asar matches the feed', fs.existsSync(staged) && sha256(staged) === asarHash);
  check('running asar untouched', sha256(asar) === asarHash);
}

/* leave nothing running or staged behind */
try {
  spawnSync('taskkill', ['/PID', String(app.pid), '/T', '/F'], { stdio: 'ignore' });
} catch {
  app.kill();
}
server.close();
console.log(`\n${failures === 0 ? 'PACKAGED UPDATE OK' : `${failures} FAILURE(S)`} · artifacts in ${work}`);
process.exit(failures === 0 ? 0 : 1);
