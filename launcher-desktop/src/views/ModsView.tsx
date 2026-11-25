import { useMemo, useState } from 'react';
import { ChevronLeft, ChevronRight, Plus, Search, Trash2 } from 'lucide-react';

/* Modpack builder: pack editor on the left, mod browser on the right. The list
   is the real mods folder, so Add/Remove act on the files that will load. */

type Mod = {
  file: string;
  name: string;
  title?: string;
  version?: string;
  author?: string;
  enabled: boolean;
  size?: number;
};

type Props = {
  mods: { dir: string; mods: Mod[] };
  busy: boolean;
  onAdd: () => void;
  onToggle: (file: string) => void;
  onRemove: (file: string) => void;
  onUseAsProfile: (name: string) => void;
  profileName: string;
  loader: 'optifine' | 'forge';
};

const PAGE_SIZE = 8;

function formatSize(bytes?: number) {
  if (!bytes) {
    return '—';
  }
  return bytes > 1024 * 1024 ? `${(bytes / 1024 / 1024).toFixed(1)} MB` : `${Math.round(bytes / 1024)} KB`;
}

export default function ModsView({ mods, busy, onAdd, onToggle, onRemove, onUseAsProfile, profileName, loader }: Props) {
  const [packName, setPackName] = useState('');
  const [summary, setSummary] = useState('');
  const [version, setVersion] = useState('1.8.9');
  const [query, setQuery] = useState('');
  const [page, setPage] = useState(1);

  const list = mods.mods ?? [];
  const enabled = list.filter((mod) => mod.enabled);

  const filtered = useMemo(() => {
    const needle = query.trim().toLowerCase();
    if (!needle) {
      return list;
    }
    return list.filter((mod) =>
      `${mod.title ?? ''} ${mod.name} ${mod.author ?? ''}`.toLowerCase().includes(needle)
    );
  }, [list, query]);

  const pages = Math.max(1, Math.ceil(filtered.length / PAGE_SIZE));
  const current = Math.min(page, pages);
  const slice = filtered.slice((current - 1) * PAGE_SIZE, current * PAGE_SIZE);

  /* Side by side once the window is wide enough, stacked below that: at the
     launcher's minimum size the browser column was pushed off screen. */
  return (
    <div className="grid h-full min-h-0 grid-cols-1 content-start gap-5 overflow-auto pb-6 pr-6 min-[1000px]:grid-cols-[minmax(0,340px)_minmax(0,1fr)] min-[1000px]:content-stretch min-[1000px]:overflow-hidden">
      {/* stacked, the pack card grows with its content so every field and the
          button stay reachable; side by side it gets its own scroll again */}
      <section className="flex min-w-0 flex-col overflow-visible rounded-card border border-line bg-surface p-5 min-[1000px]:min-h-0 min-[1000px]:overflow-auto">
        <header className="mb-4 flex items-center justify-between">
          <h2 className="page-title text-[20px] text-ink">New pack</h2>
          <button
            type="button"
            onClick={() => {
              setPackName('');
              setSummary('');
            }}
            className="inline-flex items-center gap-1.5 rounded-lg bg-elevated px-2.5 py-1.5 text-[12.5px] font-medium text-ink hover:bg-hover"
          >
            <Plus size={14} strokeWidth={1.8} />
            Start a new pack
          </button>
        </header>

        <div className="flex flex-col gap-3">
          <label className="flex flex-col gap-1.5">
            <span className="text-[11.5px] font-medium uppercase tracking-wider text-ink-3">Pack name</span>
            <input
              value={packName}
              onChange={(event) => setPackName(event.target.value)}
              placeholder="My 1.8.9 pack"
              className="h-10 rounded-[10px] border border-line bg-canvas px-3 text-[13px] text-ink outline-none placeholder:text-placeholder focus:border-accent/60"
            />
          </label>

          <label className="flex flex-col gap-1.5">
            <span className="text-[11.5px] font-medium uppercase tracking-wider text-ink-3">Summary</span>
            <textarea
              value={summary}
              onChange={(event) => setSummary(event.target.value)}
              rows={3}
              placeholder="What is this pack for?"
              className="resize-none rounded-[10px] border border-line bg-canvas px-3 py-2 text-[13px] text-ink outline-none placeholder:text-placeholder focus:border-accent/60"
            />
          </label>

          <label className="flex flex-col gap-1.5">
            <span className="text-[11.5px] font-medium uppercase tracking-wider text-ink-3">Minecraft</span>
            <select
              value={version}
              onChange={(event) => setVersion(event.target.value)}
              className="h-10 rounded-[10px] border border-line bg-elevated px-3 text-[13px] text-ink outline-none focus:border-accent/60"
            >
              <option value="1.8.9">1.8.9</option>
            </select>
          </label>
        </div>

        <div className="mt-5 flex items-center justify-between">
          <span className="text-[12.5px] font-medium text-ink-2">Contents</span>
          <span className="rounded-md bg-elevated px-2 py-0.5 text-[11.5px] text-ink-2">{enabled.length}</span>
        </div>

        <ul className="m-0 mt-2 flex list-none flex-col gap-1 p-0">
          {enabled.length === 0 ? (
            <li className="rounded-lg border border-dashed border-white/10 px-3 py-6 text-center text-[12.5px] text-ink-3">
              Nothing added yet — pick mods on the right.
            </li>
          ) : (
            enabled.slice(0, 8).map((mod) => (
              <li key={mod.file} className="flex items-center gap-2 rounded-lg bg-elevated/60 px-2.5 py-2">
                <span className="grid h-7 w-7 flex-none place-items-center rounded-md bg-canvas text-[10px] font-bold text-ink-3">
                  {(mod.title ?? mod.name).slice(0, 2).toUpperCase()}
                </span>
                <span className="min-w-0 flex-1 truncate text-[12.5px] text-ink">{mod.title ?? mod.name}</span>
                <button
                  type="button"
                  onClick={() => onRemove(mod.file)}
                  disabled={busy}
                  title="Remove"
                  className="grid h-7 w-7 place-items-center rounded-md text-ink-3 hover:bg-hover hover:text-ink"
                >
                  <Trash2 size={13} strokeWidth={1.7} />
                </button>
              </li>
            ))
          )}
        </ul>

        <div className="mt-auto pt-4">
          <button
            type="button"
            onClick={() => onUseAsProfile(packName.trim() || profileName)}
            className="h-10 w-full rounded-[10px] bg-elevated text-[13px] font-semibold text-ink transition hover:bg-hover"
          >
            Use as profile
          </button>
        </div>
      </section>

      <section className="flex min-h-[300px] min-w-0 flex-col min-[1000px]:min-h-0">
        {loader === 'optifine' ? (
          <p className="mb-3 rounded-lg border border-line bg-elevated/60 px-3 py-2 text-[11.5px] leading-snug text-ink-2">
            OptiFine is the current loader, so nothing in the mods folder loads. Switch to Forge on the Versions
            page to use mods.
          </p>
        ) : null}
        <div className="mb-3 flex flex-wrap items-center gap-2">
          <div className="relative ml-auto w-full max-w-[16rem]">
            <Search size={15} className="pointer-events-none absolute left-3 top-1/2 -translate-y-1/2 text-ink-3" />
            <input
              value={query}
              onChange={(event) => {
                setQuery(event.target.value);
                setPage(1);
              }}
              placeholder="Search mods..."
              className="h-9 w-full rounded-[10px] border border-line bg-surface pl-9 pr-3 text-[13px] text-ink outline-none placeholder:text-placeholder focus:border-accent/60"
            />
          </div>
          <button
            type="button"
            onClick={onAdd}
            disabled={busy}
            className="inline-flex h-9 items-center gap-1.5 rounded-[10px] bg-accent px-3 text-[12.5px] font-semibold text-[#0b0b0c] transition hover:bg-[#8cffa0] disabled:opacity-60"
          >
            <Plus size={14} strokeWidth={2} />
            Add mods
          </button>
        </div>

        <div className="flex min-h-0 flex-1 flex-col overflow-hidden rounded-card border border-line bg-surface">
          <ul className="m-0 flex-1 list-none divide-y divide-line-soft overflow-auto p-0">
            {slice.map((mod) => (
              <li key={mod.file} className="flex items-center gap-3.5 px-4 py-3">
                <span className="grid h-11 w-11 flex-none place-items-center rounded-[10px] border border-line bg-elevated text-[11px] font-bold text-ink-2">
                  {(mod.title ?? mod.name).slice(0, 2).toUpperCase()}
                </span>
                <span className="flex min-w-0 flex-1 flex-col gap-0.5">
                  <b className="truncate text-[13px] font-semibold text-ink">{mod.title ?? mod.name}</b>
                  <small className="truncate text-[11.5px] text-ink-3">
                    {mod.author ?? 'Unknown author'} · {mod.version ?? '1.8.9'} · {formatSize(mod.size)}
                  </small>
                  <small className="truncate text-[11.5px] text-ink-3">{mod.file}</small>
                </span>
                <button
                  type="button"
                  onClick={() => onToggle(mod.file)}
                  disabled={busy}
                  className={`h-8 flex-none rounded-lg px-3 text-[12.5px] font-medium transition ${
                    mod.enabled
                      ? 'bg-accent-soft text-accent-text hover:bg-[rgba(170,255,169,0.22)]'
                      : 'bg-elevated text-ink-2 hover:bg-hover hover:text-ink'
                  }`}
                >
                  {mod.enabled ? 'Added' : 'Add'}
                </button>
              </li>
            ))}
            {slice.length === 0 ? (
              <li className="px-4 py-10 text-center text-[12.5px] text-ink-3">No mods match that search.</li>
            ) : null}
          </ul>

          <footer className="flex items-center justify-between border-t border-line px-4 py-2.5">
            <span className="text-[11.5px] text-ink-3">
              {filtered.length} {filtered.length === 1 ? 'mod' : 'mods'}
              {mods.dir ? <span className="text-ink-3/80"> · {mods.dir}</span> : null}
            </span>
            <div className="flex items-center gap-1">
              <button
                type="button"
                onClick={() => setPage(Math.max(1, current - 1))}
                disabled={current === 1}
                className="inline-flex items-center gap-1 rounded-md px-2 py-1 text-[12px] text-ink-2 hover:bg-elevated disabled:opacity-40"
              >
                <ChevronLeft size={13} />
                Previous
              </button>
              {Array.from({ length: Math.min(3, pages) }, (_, index) => index + 1).map((number) => (
                <button
                  key={number}
                  type="button"
                  onClick={() => setPage(number)}
                  className={`h-7 w-7 rounded-md text-[12px] ${
                    current === number ? 'bg-elevated text-ink' : 'text-ink-2 hover:bg-elevated'
                  }`}
                >
                  {number}
                </button>
              ))}
              <button
                type="button"
                onClick={() => setPage(Math.min(pages, current + 1))}
                disabled={current === pages}
                className="inline-flex items-center gap-1 rounded-md px-2 py-1 text-[12px] text-ink-2 hover:bg-elevated disabled:opacity-40"
              >
                Next
                <ChevronRight size={13} />
              </button>
            </div>
          </footer>
        </div>
      </section>
    </div>
  );
}
