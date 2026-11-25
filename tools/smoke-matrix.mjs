import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';
import { spawn, spawnSync } from 'node:child_process';
import crypto from 'node:crypto';

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');
const dist = path.join(root, 'build/dist');
const jar = path.join(dist, 'MixClient-Launcher.jar');
if (!fs.existsSync(jar)) throw new Error('Run npm run release:prepare before smoke tests.');
if (process.argv.length > 2) throw new Error('smoke-matrix accepts no arguments.');
const output = path.join(root, 'build/smoke', new Date().toISOString().replace(/[:.]/g, '-'));
const data = path.join(process.env.APPDATA || process.env.HOME || root, '.mixclient');
fs.mkdirSync(output, { recursive: true });
fs.writeFileSync(path.join(output, 'build.json'), JSON.stringify({
  startedAt: new Date().toISOString(),
  distribution: dist,
  version: JSON.parse(fs.readFileSync(path.join(dist, 'version.json'), 'utf8')),
  jars: ['MixClient.jar', 'MixClient-notch.jar', 'MixClient-srg.jar', 'MixClient-Launcher.jar']
    .map(name => ({ name, sha256: crypto.createHash('sha256')
      .update(fs.readFileSync(path.join(dist, name))).digest('hex') }))
}, null, 2) + '\n');
const modes = [
  ['plain', '--smoke-test', 'latest.log'],
  ['forge', '--forge-smoke', 'forge-smoke.log'],
  ['optifine', '--optifine-smoke', 'optifine-smoke.log']
];
const results = [];
for (const [mode, flag, logName] of modes) {
  const started = Date.now();
  const folder = path.join(output, mode);
  fs.mkdirSync(folder);
  console.log(`Smoke ${mode}: starting (180s timeout)`);
  let consoleLog = '';
  let timedOut = false;
  let spawnError = null;
  const child = spawn('java', ['-Dfile.encoding=UTF-8', `-Dmixclient.dist=${dist}`, '-jar', jar, flag],
    { cwd: root, windowsHide: true, detached: process.platform !== 'win32' });
  child.stdout.on('data', chunk => { consoleLog += chunk.toString(); });
  child.stderr.on('data', chunk => { consoleLog += chunk.toString(); });
  const timer = setTimeout(() => {
    timedOut = true;
    if (process.platform === 'win32') {
      spawnSync('taskkill', ['/pid', String(child.pid), '/t', '/f'], { windowsHide: true });
    } else {
      try { process.kill(-child.pid, 'SIGKILL'); } catch {}
    }
  }, 180_000);
  const code = await new Promise(resolve => {
    child.on('error', error => { spawnError = error.message; });
    child.on('close', resolve);
  });
  clearTimeout(timer);
  fs.writeFileSync(path.join(folder, 'launcher.log'), consoleLog);
  const sourceLog = path.join(data, 'logs', logName);
  let gameLog = '';
  if (fs.existsSync(sourceLog) && fs.statSync(sourceLog).mtimeMs >= started) {
    const fullLog = fs.readFileSync(sourceLog, 'utf8');
    gameLog = fullLog.slice(Math.max(0, fullLog.lastIndexOf('===== Mix Client launch ')));
    fs.writeFileSync(path.join(folder, 'game.log'), gameLog);
  }
  const screenshots = [];
  const crashReports = [];
  const smokeDir = path.join(data, 'smoke');
  if (fs.existsSync(smokeDir)) {
    for (const name of fs.readdirSync(smokeDir)) {
      const source = path.join(smokeDir, name);
      if (/^hs_err_pid\d+\.log$/.test(name) && fs.statSync(source).mtimeMs >= started) {
        fs.copyFileSync(source, path.join(folder, name));
        crashReports.push(name);
      }
    }
  }
  const sourceShots = path.join(data, 'smoke/screenshots');
  if (fs.existsSync(sourceShots)) {
    for (const name of fs.readdirSync(sourceShots)) {
      const source = path.join(sourceShots, name);
      if (fs.statSync(source).isFile() && fs.statSync(source).mtimeMs >= started) {
        fs.copyFileSync(source, path.join(folder, name));
        screenshots.push(name);
      }
    }
  }
  const mixinFailures = gameLog.split(/\r?\n/).filter(line => line.includes('Mixin apply failed'));
  const diagnostics = {
    lwjglSealingWarnings: gameLog.includes('security seal') || gameLog.includes('trying to seal'),
    legacyForgeJarScanWarnings: gameLog.includes('probably a corrupt zip'),
    shutdownDisplayError: gameLog.includes('java.lang.IllegalStateException: Display not created')
  };
  const expectedShots = ['ui-home', 'ui-module', 'ui-setting', 'ui-music', 'ui-hud'];
  const missingScreenshots = expectedShots.filter(name => !screenshots.includes(name) && !screenshots.includes(name + '.png'));
  const passed = code === 0 && !timedOut && !spawnError
    && gameLog.includes('[MixClient] MIXCLIENT_SMOKE_OK')
    && !gameLog.includes('MIXCLIENT_SMOKE_FAIL')
    && gameLog.includes('[uicapture] world ready')
    && !gameLog.includes('world wait timeout')
    && mixinFailures.length === 0 && missingScreenshots.length === 0 && crashReports.length === 0;
  results.push({ mode, passed, exitCode: code, timedOut, spawnError,
    durationMs: Date.now() - started, screenshots, missingScreenshots, mixinFailures, diagnostics, crashReports,
    dangoUnavailable: gameLog.includes('available=false') });
  fs.writeFileSync(path.join(output, 'results.json'), JSON.stringify(results, null, 2) + '\n');
  console.log(`Smoke ${mode}: ${passed ? 'PASS' : 'FAIL'} (${screenshots.length} screenshots)`);
}
console.log(`Smoke results: ${output}`);
if (results.some(result => !result.passed)) process.exitCode = 1;
