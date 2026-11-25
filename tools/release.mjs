import fs from 'node:fs';
import path from 'node:path';
import crypto from 'node:crypto';
import { fileURLToPath } from 'node:url';
import { spawnSync } from 'node:child_process';

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');
const desktop = path.join(root, 'launcher-desktop');
const dist = path.join(root, 'build', 'dist');
const receiptPath = path.join(root, 'build', 'release-receipt.json');
const flags = new Set(process.argv.slice(2));
for (const flag of flags) {
  if (!['--offline', '--prepare-only', '--dir'].includes(flag)) {
    throw new Error(`Unknown release option: ${flag}`);
  }
}
function run(command, args, cwd) {
  const result = spawnSync(command, args, { cwd, stdio: 'inherit' });
  if (result.error) throw result.error;
  if (result.status !== 0) throw new Error(`${command} failed (exit ${result.status})`);
}
function requireFile(relative) {
  const target = path.join(dist, relative);
  if (!fs.existsSync(target) || !fs.statSync(target).isFile() || fs.statSync(target).size === 0) {
    throw new Error(`Missing or empty distribution file: ${relative}`);
  }
  return target;
}

// Only the known generated game distribution is removed; source and caches stay intact.
fs.rmSync(receiptPath, { force: true });
fs.rmSync(dist, { recursive: true, force: true });
if (process.platform === 'win32') {
  run('cmd.exe', ['/d', '/c', 'gradlew.bat dist' + (flags.has('--offline') ? ' --offline' : '')], root);
} else {
  run('./gradlew', ['dist', ...(flags.has('--offline') ? ['--offline'] : [])], root);
}
const expectedVersion = fs.readFileSync(path.join(root, 'build.gradle'), 'utf8')
  .match(/^version\s*=\s*'([^']+)'/m)?.[1];
const metadata = JSON.parse(fs.readFileSync(requireFile('version.json'), 'utf8'));
if (!expectedVersion || metadata.version !== expectedVersion) {
  throw new Error(`Client version mismatch: expected ${expectedVersion}, got ${metadata.version}`);
}
for (const name of ['MixClient.jar', 'MixClient-notch.jar', 'MixClient-srg.jar',
  'MixClient-Launcher.jar', 'minecraft-1.8.9-mcp.jar', 'dango-mix-srg.jar',
  'lib/asm-7.2.jar', 'lib/asm-tree-7.2.jar', 'lib/dango-mix.jar']) requireFile(name);
const optifine = path.join(dist, 'optifine');
if (!fs.existsSync(optifine) || !fs.readdirSync(optifine).some(name => name.endsWith('.jar') && fs.statSync(path.join(optifine, name)).size > 0)) {
  throw new Error('Missing bundled OptiFine jar in optifine/. Restore it before releasing.');
}
run(process.execPath, [path.join(desktop, 'node_modules/vite/bin/vite.js'), 'build'], desktop);

function inventory(dir, prefix = '') {
  return fs.readdirSync(dir, { withFileTypes: true }).sort((a, b) => a.name.localeCompare(b.name))
    .flatMap(entry => {
      const relative = prefix + entry.name;
      if (entry.isDirectory()) return inventory(path.join(dir, entry.name), relative + '/');
      return [{ path: relative, sha256: crypto.createHash('sha256')
        .update(fs.readFileSync(path.join(dir, entry.name))).digest('hex') }];
    });
}
const launcherVersion = JSON.parse(fs.readFileSync(path.join(desktop, 'package.json'), 'utf8')).version;
const receipt = { clientVersion: metadata.version, launcherVersion,
  builtAt: new Date().toISOString(), gameFiles: inventory(dist), rendererFiles: inventory(path.join(desktop, 'dist')) };
fs.writeFileSync(receiptPath, JSON.stringify(receipt, null, 2) + '\n');
console.log(`Release prepared: client ${metadata.version}, launcher ${launcherVersion}`);
if (!flags.has('--prepare-only')) {
  if (process.platform !== 'win32') throw new Error('Windows packaging requires Windows. Use --prepare-only here.');
  if (!fs.existsSync(path.join(desktop, 'jre/bin/java.exe'))) throw new Error('Missing bundled JRE: launcher-desktop/jre/bin/java.exe');
  const output = path.join(desktop, 'release', `${launcherVersion}-${crypto.randomUUID()}`);
  run(process.execPath, [path.join(desktop, 'node_modules/electron-builder/cli.js'), '--win',
    ...(flags.has('--dir') ? ['--dir'] : ['nsis']), `-c.directories.output=${output}`], desktop);
  console.log(`Windows package ready: ${output}`);
}
