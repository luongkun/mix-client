/* The mods folder, as the launcher sees it.

   FML only picks up files that end in `.jar`, so "disabled" is the usual
   `.jar.disabled` rename: the file stays where the player put it, nothing is
   copied around, and turning a mod back on is a rename again. Removing a mod
   moves it to `.mixclient/trash/mods` instead of deleting it — a mis-click in
   a mod list should not cost the player a download. */

import fs from 'node:fs';
import path from 'node:path';
import { dataDir, gameDir, readZipEntry } from './minecraft.js';

const DISABLED = '.disabled';
const MAX_META_BYTES = 96 * 1024 * 1024;
const metaCache = new Map();

/* FML reads `<gameDir>/mods`, and the game folder belongs to Mix Client, so a
   jar here is never picked up by another launcher (and theirs is never shown
   here). */
export function modsDir() {
  return path.join(gameDir(), 'mods');
}

function ensureDir() {
  const dir = modsDir();
  fs.mkdirSync(dir, { recursive: true });
  return dir;
}

/* Forge has no launchwrapper slot for OptiFine, so it has to arrive as an
   ordinary mod in mods/. The jar ships inside the build (dist/optifine), so
   picking the Forge loader silently drops it in: the player never has to
   download or install OptiFine by hand. An existing copy — any OptiFine
   build, not just the bundled one — is left alone. */
export function ensureOptiFine(distDir) {
  const dir = ensureDir();
  let existing = null;
  try {
    existing = fs.readdirSync(dir).find((file) => {
      if (!file.toLowerCase().endsWith('.jar')) {
        return false;
      }
      const base = file.toLowerCase();
      return base.startsWith('optifine') && base.includes('1.8.9');
    });
  } catch {
    existing = null;
  }
  if (existing) {
    return path.join(dir, existing);
  }

  const source = path.join(distDir, 'optifine');
  let bundled = null;
  try {
    bundled = fs.readdirSync(source).find((file) => /^optifine.*\.jar$/i.test(file));
  } catch {
    bundled = null;
  }
  if (!bundled) {
    return null;
  }
  const target = path.join(dir, bundled);
  fs.copyFileSync(path.join(source, bundled), target);
  return target;
}

function enabledName(file) {
  return file.endsWith(DISABLED) ? file.slice(0, -DISABLED.length) : file;
}

function isModFile(file) {
  const name = enabledName(file);
  return name.toLowerCase().endsWith('.jar');
}

function firstString(value) {
  if (Array.isArray(value)) {
    return value.filter(Boolean).join(', ');
  }
  return typeof value === 'string' ? value : '';
}

/* mcmod.info is what 1.8.9 mods use for a display name; the manifest is the
   fallback for mods that ship without one. Both are optional. */
function readMeta(fullPath, size) {
  if (size > MAX_META_BYTES) {
    return {};
  }
  try {
    const raw = readZipEntry(fullPath, 'mcmod.info');
    if (raw) {
      const parsed = JSON.parse(raw.toString('utf8'));
      /* 1.8.9 mods ship either a bare array or an object with modList/mods */
      const entry = Array.isArray(parsed)
        ? parsed[0]
        : parsed?.modList?.[0] ?? parsed?.mods?.[0] ?? parsed;
      if (entry && typeof entry === 'object') {
        return {
          title: firstString(entry.name) || firstString(entry.modid),
          version: firstString(entry.version),
          author: firstString(entry.authorList) || firstString(entry.author)
        };
      }
    }
  } catch {
    /* a broken or unusual archive is not worth a failed listing */
  }
  try {
    const manifest = readZipEntry(fullPath, 'META-INF/MANIFEST.MF');
    if (manifest) {
      const text = manifest.toString('utf8');
      const pick = (key) => {
        const match = text.match(new RegExp(`^${key}:\\s*(.+)$`, 'mi'));
        return match ? match[1].trim() : '';
      };
      return { title: pick('Implementation-Title'), version: pick('Implementation-Version') };
    }
  } catch {
    /* same */
  }
  return {};
}

function describe(file) {
  const fullPath = path.join(modsDir(), file);
  const stat = fs.statSync(fullPath);
  const key = `${fullPath}:${stat.size}:${stat.mtimeMs}`;
  let meta = metaCache.get(key);
  if (!meta) {
    meta = readMeta(fullPath, stat.size);
    /* the cache only has to survive one listing pass; keep it tiny */
    if (metaCache.size > 200) {
      metaCache.clear();
    }
    metaCache.set(key, meta);
  }
  return {
    file,
    name: enabledName(file),
    enabled: !file.endsWith(DISABLED),
    size: stat.size,
    modified: stat.mtimeMs,
    title: meta.title || '',
    version: meta.version || '',
    author: meta.author || ''
  };
}

export function list() {
  const dir = ensureDir();
  let entries;
  try {
    entries = fs.readdirSync(dir, { withFileTypes: true });
  } catch (error) {
    return { dir, mods: [], error: String(error.message ?? error) };
  }
  const mods = entries
    .filter((entry) => entry.isFile() && isModFile(entry.name))
    .map((entry) => {
      try {
        return describe(entry.name);
      } catch {
        return null;
      }
    })
    .filter(Boolean)
    .sort((a, b) => a.name.localeCompare(b.name));
  return { dir, mods };
}

/* Windows keeps a jar the running game has open, so a rename fails with
   EBUSY/EPERM; say what to do instead of leaking the errno. */
function explain(error) {
  const message = String(error?.message ?? error);
  if (/EBUSY|EPERM|EACCES/.test(message)) {
    return 'The game has that jar open — close Minecraft and try again.';
  }
  return message;
}

export function setEnabled(file, enabled) {
  const dir = modsDir();
  const current = path.join(dir, path.basename(file));
  if (!fs.existsSync(current)) {
    return { ok: false, message: 'That mod is no longer in the folder.' };
  }
  const target = path.join(dir, enabled ? enabledName(path.basename(file)) : `${enabledName(path.basename(file))}${DISABLED}`);
  if (target === current) {
    return { ok: true };
  }
  if (fs.existsSync(target)) {
    return { ok: false, message: `${path.basename(target)} already exists.` };
  }
  try {
    fs.renameSync(current, target);
    return { ok: true, file: path.basename(target) };
  } catch (error) {
    return { ok: false, message: explain(error) };
  }
}

export function remove(file) {
  const dir = modsDir();
  const current = path.join(dir, path.basename(file));
  if (!fs.existsSync(current)) {
    return { ok: false, message: 'That mod is no longer in the folder.' };
  }
  const trash = path.join(dataDir(), 'trash', 'mods');
  fs.mkdirSync(trash, { recursive: true });
  const stamp = new Date().toISOString().replace(/[:.]/g, '-');
  const target = path.join(trash, `${stamp}-${path.basename(file)}`);
  try {
    fs.renameSync(current, target);
    return { ok: true, message: `Moved to ${trash}`, trash: target };
  } catch (error) {
    return { ok: false, message: explain(error) };
  }
}

function freeName(dir, name) {
  const extension = path.extname(name);
  const stem = name.slice(0, name.length - extension.length);
  let candidate = name;
  let index = 2;
  while (fs.existsSync(path.join(dir, candidate))) {
    candidate = `${stem} (${index})${extension}`;
    index += 1;
  }
  return candidate;
}

export function add(paths) {
  const dir = ensureDir();
  const added = [];
  const skipped = [];
  for (const source of paths ?? []) {
    try {
      const stat = fs.statSync(source);
      if (!stat.isFile() || !source.toLowerCase().endsWith('.jar')) {
        skipped.push(path.basename(source));
        continue;
      }
      const target = path.join(dir, freeName(dir, path.basename(source)));
      fs.copyFileSync(source, target);
      added.push(path.basename(target));
    } catch (error) {
      skipped.push(`${path.basename(source)}: ${String(error.message ?? error)}`);
    }
  }
  return { ok: added.length > 0, added, skipped, ...list() };
}
