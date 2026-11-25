import { useState } from 'react';
import { Play, Search } from 'lucide-react';
/* Official Mojang key art and loader logos, bundled in public/assets. */
const ART = (id: string) => `./assets/versions/${id}.jpg`;
const LOGO = (name: string) => `./assets/loaders/${name}`;

/* Versions: pick the game build. Only 1.8.9 ships today; the others are shown
   so the roadmap is visible, but they cannot be selected yet. */

type Props = {
  version: string;
  branch: string;
  loader: Loader;
  launching: boolean;
  onVersion: (id: string) => void;
  onBranch: (branch: string) => void;
  onLoader: (loader: Loader) => void;
  onLaunch: () => void;
};

type Version = {
  id: string;
  label: string;
  available: boolean;
  badge?: string;
  notes: string;
  shot: string;
};

const FALLBACK_ART = ART('1.8.9');

const VERSIONS: Version[] = [
  {
    id: '1.8.9',
    label: 'MIX 1.8.9',
    available: true,
    notes:
      'Minecraft 1.8, the Bountiful Update, reworked combat, added slime blocks, armour stands and the new launcher. 1.8.9 is its final bug-fix build and the one every PvP server still runs, which is why Mix Client is built on it.',
    shot: ART('1.8.9')
  },
  {
    id: '1.7.10',
    label: 'MIX 1.7.10',
    available: false,
    notes:
      'The modding golden age: 1.7.10 still carries the largest mod library ever assembled for Minecraft, at the cost of a much older renderer and no off-hand slot.',
    shot: ART('1.7.10')
  },
  {
    id: '1.12.2',
    label: 'MIX 1.12.2',
    available: false,
    notes:
      'The World of Color Update brought concrete, parrots and the recipe book, and with them the second great era of modpacks.',
    shot: ART('1.12.2')
  },
  {
    id: '1.16.5',
    label: 'MIX 1.16.5',
    available: false,
    notes:
      'Nether Update: new biomes, ancient debris and netherite gear, running on a modern renderer with far better chunk performance.',
    shot: ART('1.16.5')
  },
  {
    id: '1.20.1',
    label: 'MIX 1.20.1',
    available: false,
    badge: 'NEW',
    notes:
      'Trails & Tales: archaeology, cherry groves, camels and the sniffer. The current modding baseline for new packs.',
    shot: ART('1.20.1')
  },
  {
    id: '1.21',
    label: 'MIX 1.21',
    available: false,
    notes:
      'Tricky Trials: trial chambers, the breeze and the mace, with the new combat balance pass on top.',
    shot: ART('1.21')
  }
];

/* Exactly one loader runs per launch. Forge owns the game, loads mods from
   mods/, and takes OptiFine as one of those mods; OptiFine alone replaces the
   render pipeline and loads nothing else. */
type Loader = 'optifine' | 'forge';

const BRANCHES = ['RELEASE 1.8.0', 'BETA 3.1', 'DEV BUILD'];

export default function VersionsView({
  version,
  branch,
  loader,
  launching,
  onVersion,
  onBranch,
  onLoader,
  onLaunch
}: Props) {
  const [query, setQuery] = useState('');

  const visible = VERSIONS.filter((entry) => entry.label.toLowerCase().includes(query.trim().toLowerCase()));
  const current = VERSIONS.find((entry) => entry.id === version) ?? VERSIONS[0];

  return (
    <div className="grid h-full min-h-0 grid-cols-1 gap-6 overflow-auto pb-6 pr-6 min-[1000px]:grid-cols-[minmax(0,68%)_minmax(0,1fr)] min-[1000px]:overflow-hidden">
      {/* left: search + grid */}
      <div className="flex min-h-0 min-w-0 flex-col gap-4">
        <div className="flex h-[42px] flex-none items-center gap-2.5 rounded-[12px] border border-[rgba(255,255,255,0.06)] bg-surface px-3.5">
          <Search size={15} className="text-ink-3" strokeWidth={2} />
          <input
            value={query}
            onChange={(event) => setQuery(event.target.value)}
            placeholder="Search versions..."
            className="h-full min-w-0 flex-1 bg-transparent font-sans text-[13px] text-ink outline-none placeholder:text-placeholder"
          />
        </div>

        <div className="grid min-h-0 flex-1 grid-cols-1 content-start gap-4 overflow-visible pr-1 min-[820px]:grid-cols-2 min-[820px]:overflow-auto">
          {visible.map((entry) => {
            const selected = entry.id === version;
            return (
              <button
                key={entry.id}
                type="button"
                disabled={!entry.available}
                onClick={() => entry.available && onVersion(entry.id)}
                className={`group relative h-[150px] overflow-hidden rounded-2xl text-left transition ${
                  entry.available ? 'cursor-pointer' : 'cursor-not-allowed'
                } ${selected ? 'ring-2 ring-[#aaff9f]' : 'ring-1 ring-[rgba(255,255,255,0.06)]'}`}
              >
                <img
                  src={entry.shot}
                  alt=""
                  loading="lazy"
                  draggable={false}
                  onError={(event) => {
                    event.currentTarget.src = FALLBACK_ART;
                  }}
                  className={`absolute inset-0 h-full w-full object-cover transition-all duration-200 ${
                    selected
                      ? 'grayscale-0'
                      : entry.available
                        ? 'grayscale brightness-50 group-hover:brightness-75 group-hover:grayscale-[40%]'
                        : 'grayscale brightness-[0.55]'
                  }`}
                />
                <span className="absolute inset-0 bg-gradient-to-t from-black/70 via-black/25 to-black/10" />

                {entry.badge ? (
                  <span className="absolute right-3 top-3 rounded-full bg-[#aaff9f] px-2 py-[3px] font-display text-[9px] font-extrabold uppercase tracking-wider text-[#0b0b0c]">
                    {entry.badge}
                  </span>
                ) : null}

                <span className="absolute inset-0 grid place-items-center">
                  <span className="font-display text-[22px] font-black uppercase tracking-tight text-white drop-shadow-md">
                    {entry.label}
                  </span>
                </span>

                {!entry.available ? (
                  <span className="absolute bottom-3 left-0 right-0 text-center font-sans text-[10.5px] font-medium uppercase tracking-wider text-white/45">
                    Coming soon
                  </span>
                ) : null}
              </button>
            );
          })}

          {visible.length === 0 ? (
            <p className="col-span-2 m-0 font-sans text-[12.5px] text-ink-3">No versions match that search.</p>
          ) : null}
        </div>
      </div>

      {/* right: inspector */}
      <section className="flex min-h-0 flex-col justify-between rounded-card border border-[rgba(255,255,255,0.06)] bg-surface p-5">
        <div className="flex flex-col gap-3">
          <div className="relative h-[125px] overflow-hidden rounded-[12px]">
            <img
              src={current.shot}
              alt=""
              draggable={false}
              onError={(event) => {
                event.currentTarget.src = FALLBACK_ART;
              }}
              className="absolute inset-0 h-full w-full object-cover"
            />
            <span className="absolute inset-0 bg-gradient-to-t from-black/75 via-black/35 to-black/25" />
            <span className="absolute inset-0 grid place-items-center">
              <span className="font-display text-[20px] font-black uppercase tracking-tight text-white drop-shadow-md">
                {current.label}
              </span>
            </span>
          </div>
          <p className="m-0 font-sans text-[12px] leading-relaxed text-ink-2">{current.notes}</p>
        </div>

        <div className="flex flex-col gap-3">
          <div className="flex flex-col gap-1.5">
            <span className="font-display text-[10px] font-bold uppercase tracking-wider text-ink-3">
              Mix Client Branch
            </span>
            <select
              value={branch}
              onChange={(event) => onBranch(event.target.value)}
              className="h-[38px] rounded-xl border border-[rgba(255,255,255,0.06)] bg-elevated px-3 font-sans text-[13px] text-ink outline-none"
            >
              {BRANCHES.map((entry) => (
                <option key={entry} value={entry} className="bg-[#222327]">
                  {entry}
                </option>
              ))}
            </select>
          </div>

          <div className="flex flex-col gap-1.5">
            <span className="font-display text-[10px] font-bold uppercase tracking-wider text-ink-3">
              Minecraft Version
            </span>
            <div className="flex h-[38px] items-center rounded-[10px] bg-elevated px-3 font-sans text-[13px] text-ink">
              {current.id}
            </div>
          </div>

          <div className="flex flex-col gap-1.5">
            <span className="font-display text-[10px] font-bold uppercase tracking-wider text-ink-3">
              Loader
            </span>
            <div className="grid grid-cols-2 gap-2">
              <LoaderButton active={loader === 'forge'} onClick={() => onLoader('forge')} label="Forge" hint="Mods + OptiFine">
                <img src={LOGO('forge.svg')} alt="" className="h-6 w-6 object-contain" draggable={false} />
              </LoaderButton>
              <LoaderButton
                active={loader === 'optifine'}
                onClick={() => onLoader('optifine')}
                label="OptiFine"
                hint="OptiFine only"
              >
                <img src={LOGO('optifine.png')} alt="" className="h-6 w-6 object-contain" draggable={false} />
              </LoaderButton>
            </div>
          </div>
        </div>

        <button
          type="button"
          onClick={onLaunch}
          disabled={launching}
          className="flex h-[46px] w-full items-center justify-center gap-2 rounded-[12px] bg-gradient-to-b from-[#aaff9f] to-[#11ffbd] font-display text-[15px] font-black uppercase tracking-wider text-[#0b0b0c] shadow-lg shadow-black/30 transition hover:to-[#0fdcb0] disabled:opacity-60"
        >
          <Play size={16} strokeWidth={2.4} />
          Launch game
        </button>
      </section>
    </div>
  );
}

function LoaderButton({
  active,
  onClick,
  label,
  hint,
  children
}: {
  active: boolean;
  onClick: () => void;
  label: string;
  hint: string;
  children: React.ReactNode;
}) {
  return (
    <button
      type="button"
      onClick={onClick}
      aria-pressed={active}
      title={`${label} — ${hint}`}
      className={`flex h-[54px] min-w-0 items-center gap-2.5 rounded-xl px-2.5 text-left transition ${
        active
          ? 'border-[1.5px] border-accent bg-elevated shadow-sm shadow-blue-500/20'
                : 'border border-line bg-[#1b1b1e] opacity-55 hover:opacity-90'
      }`}
    >
      <span className="grid h-7 w-7 flex-none place-items-center">{children}</span>
      <span className="flex min-w-0 flex-col">
        <b className="truncate font-display text-[12px] font-extrabold uppercase tracking-wide text-white">{label}</b>
        <small className="truncate text-[10.5px] text-ink-3">{hint}</small>
      </span>
    </button>
  );
}
