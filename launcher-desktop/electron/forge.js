/* Forge 1.8.9 installation.

   Copying a mod into `mods/` only does something when the game actually runs
   FML, and FML is not part of the client build: it has to be installed the
   same way the official launcher does it — a version profile plus the
   libraries it names.

   The Forge installer jar carries both the profile (`install_profile.json`
   → `versionInfo`) and the universal jar, so a single 4 MB download is enough
   to build a complete, standalone install under `.minecraft`. */

import fs from 'node:fs';
import path from 'node:path';
import {
  dataDir,
  download,
  librariesDir,
  mavenPath,
  minecraftDir,
  readZipEntry,
  versionMeta
} from './minecraft.js';

/* pinned to the same build the client compiles against (build.gradle) */
export const FORGE_VERSION = '1.8.9-11.15.1.2318-1.8.9';
export const FORGE_ID = `1.8.9-forge${FORGE_VERSION}`;
const MAVEN = 'https://maven.minecraftforge.net/';
const INSTALLER_URL = `${MAVEN}net/minecraftforge/forge/${FORGE_VERSION}/forge-${FORGE_VERSION}-installer.jar`;
const UNIVERSAL_ENTRY = `forge-${FORGE_VERSION}-universal.jar`;
/* the profile names the forge artifact without a classifier, but the file on
   disk is the universal jar; the merged profile below says so explicitly */
const FORGE_LIBRARY = `net.minecraftforge:forge:${FORGE_VERSION}:universal`;

export const profileDir = () => path.join(minecraftDir(), 'versions', FORGE_ID);
export const profileFile = () => path.join(profileDir(), `${FORGE_ID}.json`);
export const universalFile = () => path.join(librariesDir(), mavenPath(FORGE_LIBRARY));
const cacheDir = () => path.join(dataDir(), 'cache');

export function status() {
  const profile = profileFile();
  const universal = universalFile();
  return {
    version: FORGE_VERSION,
    id: FORGE_ID,
    dir: profileDir(),
    profile: fs.existsSync(profile) ? profile : null,
    universal: fs.existsSync(universal) ? universal : null,
    installed: fs.existsSync(profile) && fs.existsSync(universal)
  };
}

/* Forge adds no natives of its own, so the vanilla set extracted by
   ensureInstalled() is the whole story; only the plain libraries matter. */
function mergeLibraries(vanilla, forgeInfo) {
  const byName = new Map();
  for (const library of vanilla.libraries ?? []) {
    byName.set(library.name, { ...library });
  }
  for (const library of forgeInfo.libraries ?? []) {
    if (library.clientreq === false) {
      continue;
    }
    const base = byName.get(library.name) ?? {};
    const merged = { ...base, ...library };
    /* Forge leaves the url out for the artifacts vanilla already ships
       (launchwrapper, asm-all, lzma, trove4j): keep the Mojang download info */
    if (!merged.url && base.url) {
      merged.url = base.url;
    }
    if (!merged.downloads && base.downloads) {
      merged.downloads = base.downloads;
    }
    if (library.name.startsWith('net.minecraftforge:forge:')) {
      merged.name = FORGE_LIBRARY;
    }
    byName.set(library.name, merged);
  }
  return [...byName.values()];
}

function remoteFor(library) {
  const artifact = library.downloads?.artifact;
  if (artifact?.url) {
    return { url: artifact.url, sha1: artifact.sha1 };
  }
  const base = library.url ?? 'https://libraries.minecraft.net/';
  const relative = mavenPath(library.name).split(path.sep).join('/');
  return { url: `${base.replace(/\/?$/, '/')}${relative}`, sha1: undefined };
}

/* Same file layout a normal launcher expects, so an existing Forge profile
   (or a second launcher) keeps working with what we write. */
export async function ensure({ onProgress = () => {}, signal } = {}) {
  const before = status();
  if (before.installed) {
    return { ...before, changed: false };
  }

  onProgress({ phase: 'forge', done: 0, total: 1, label: 'Downloading the Forge installer…' });
  const installer = path.join(cacheDir(), `forge-${FORGE_VERSION}-installer.jar`);
  await download(INSTALLER_URL, installer, { signal });

  const profileEntry = readZipEntry(installer, 'install_profile.json');
  if (!profileEntry) {
    throw new Error('The Forge installer has no install_profile.json.');
  }
  const info = JSON.parse(profileEntry.toString('utf8')).versionInfo;
  if (!info?.libraries) {
    throw new Error('The Forge installer profile has no libraries.');
  }

  const universal = readZipEntry(installer, UNIVERSAL_ENTRY);
  if (!universal) {
    throw new Error(`The Forge installer has no ${UNIVERSAL_ENTRY}.`);
  }
  const universalPath = universalFile();
  fs.mkdirSync(path.dirname(universalPath), { recursive: true });
  fs.writeFileSync(universalPath, universal);
  /* the unclassified name is what the raw profile points at */
  fs.writeFileSync(path.join(path.dirname(universalPath), `forge-${FORGE_VERSION}.jar`), universal);

  const vanilla = await versionMeta(signal);
  const libraries = mergeLibraries(vanilla, info);
  const installable = libraries.filter((library) => !library.natives);
  let done = 0;
  for (const library of installable) {
    if (signal?.aborted) {
      throw new Error('Download cancelled.');
    }
    const target = path.join(librariesDir(), mavenPath(library.name));
    const remote = remoteFor(library);
    if (!fs.existsSync(target) || fs.statSync(target).size === 0) {
      await download(remote.url, target, { sha1: remote.sha1, signal });
    }
    done += 1;
    if (done % 4 === 0 || done === installable.length) {
      onProgress({
        phase: 'forge',
        done,
        total: installable.length,
        label: `Forge libraries ${done}/${installable.length}`
      });
    }
  }

  const profile = {
    id: FORGE_ID,
    time: info.time,
    releaseTime: info.releaseTime,
    type: 'release',
    mainClass: info.mainClass ?? 'net.minecraft.launchwrapper.Launch',
    minecraftArguments: info.minecraftArguments,
    minimumLauncherVersion: info.minimumLauncherVersion ?? 14,
    inheritsFrom: '1.8.9',
    jar: '1.8.9',
    assets: info.assets ?? vanilla.assets,
    assetIndex: vanilla.assetIndex,
    libraries
  };
  fs.mkdirSync(profileDir(), { recursive: true });
  fs.writeFileSync(profileFile(), JSON.stringify(profile, null, 2), 'utf8');
  onProgress({ phase: 'forge', done: 1, total: 1, label: 'Forge ready' });

  return { ...status(), changed: true };
}
