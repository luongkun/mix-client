import { useState } from 'react';
import {
  Award,
  Check,
  ChevronRight,
  Crown,
  Frame,
  Palette,
  Pencil,
  Plus,
  RotateCw,
  Shirt,
  Sparkles,
  Store,
  Upload,
  Feather
} from 'lucide-react';
import Skin3D from '../ui/Skin3D.jsx';
import SkinPreview from '../SkinPreview.jsx';

/* Wardrobe: 3D preview on the left, skins and cosmetics on the right. */

const COSMETICS = [
  { id: 'frame', name: 'Avatar frame', Icon: Frame },
  { id: 'badge', name: 'Badge', Icon: Award },
  { id: 'cape', name: 'Cape', Icon: Shirt },
  { id: 'headwear', name: 'Headwear', Icon: Crown },
  { id: 'theme', name: 'Launcher theme', Icon: Palette },
  { id: 'effect', name: 'Profile effect', Icon: Sparkles },
  { id: 'wings', name: 'Wings', Icon: Feather }
];

type Props = {
  skin: string | null;
  slim: boolean;
  localSkins: { id: string; name: string; url: string; variant?: 'slim' | 'classic' }[];
  selected: string;
  onSelect: (id: string) => void;
  onVariant: (variant: 'classic' | 'slim') => void;
  onAdd: () => void;
};

export default function WardrobeView({ skin, slim, localSkins, selected, onSelect, onVariant, onAdd }: Props) {
  const [spin, setSpin] = useState(true);
  const [openType, setOpenType] = useState<string | null>(null);

  const entries = [
    ...(skin ? [{ id: 'account', name: 'Account skin', note: 'Minecraft', url: skin }] : []),
    ...localSkins.map((entry) => ({
      id: entry.id,
      name: entry.name,
      note: `Local · ${entry.variant === 'slim' ? 'Slim' : 'Classic'}`,
      url: entry.url
    }))
  ];
  const current = entries.find((entry) => entry.id === selected) ?? entries[0] ?? null;

  return (
    <div className="grid h-full min-h-0 grid-cols-1 gap-6 overflow-auto pb-6 pr-6 min-[1000px]:grid-cols-[minmax(0,38%)_minmax(0,1fr)] min-[1000px]:overflow-hidden">
      <section className="flex min-h-0 min-w-0 flex-col rounded-card border border-line bg-surface p-5">
        <header className="flex items-center justify-between gap-3">
          <div className="inline-flex rounded-[10px] border border-line bg-canvas p-[3px]">
            {(['classic', 'slim'] as const).map((id) => (
              <button
                key={id}
                type="button"
                onClick={() => onVariant(id)}
                className={`rounded-[7px] px-3.5 py-1.5 text-[12.5px] font-medium capitalize transition ${
                  (id === 'slim') === slim ? 'bg-elevated text-ink' : 'text-ink-2 hover:text-ink'
                }`}
              >
                {id}
              </button>
            ))}
          </div>
          <button
            type="button"
            onClick={() => setSpin((value) => !value)}
            className={`inline-flex items-center gap-2 rounded-lg px-2 py-1.5 text-[12.5px] transition ${
              spin ? 'text-ink hover:bg-elevated' : 'text-ink-2 hover:text-ink'
            }`}
          >
            <RotateCw size={14} strokeWidth={1.6} />
            Rotate
          </button>
        </header>

        <div className="relative grid min-h-0 flex-1 place-items-center py-2">
          <Skin3D skin={current?.url ?? null} slim={slim} spin={spin} width={300} height={400} zoom={0.86} />
          <span className="pointer-events-none absolute bottom-5 h-4 w-2/5 rounded-full bg-[radial-gradient(closest-side,rgba(0,0,0,0.55),transparent_72%)]" />
        </div>

        <footer className="flex items-center justify-between gap-3 border-t border-line-soft pt-4">
          <div className="grid min-w-0 gap-0.5">
            <b className="page-title truncate text-[19px] text-ink">
              {current ? current.name : 'Nothing equipped'}
            </b>
            <small className="text-[12px] text-ink-3">{current ? current.note : 'Nothing equipped'}</small>
          </div>
          <button
            type="button"
            disabled
            title="Uploading a skin needs a Mojang upload token — coming with the store"
            className="inline-flex h-9 flex-none items-center gap-2 whitespace-nowrap rounded-[10px] border border-line bg-elevated px-3.5 text-[12.5px] font-semibold text-ink-2 opacity-60"
          >
            <Check size={14} strokeWidth={2} />
            Apply to Minecraft
          </button>
        </footer>
      </section>

      <section className="flex min-h-0 flex-col overflow-auto pr-1">
        <header className="mb-3 flex items-center gap-2">
          <h2 className="text-[15px] font-semibold text-ink">Skins</h2>
          <span className="text-[13px] text-ink-3">{entries.length}</span>
          <div className="flex-1" />
          <button
            type="button"
            disabled
            className="inline-flex h-9 items-center gap-2 rounded-[10px] border border-line bg-elevated px-3.5 text-[12.5px] font-semibold text-ink-2 opacity-60"
          >
            <Store size={14} strokeWidth={1.8} />
            Open Store
          </button>
          <button
            type="button"
            onClick={onAdd}
            className="inline-flex h-9 items-center gap-2 rounded-[10px] bg-accent px-3.5 text-[12.5px] font-semibold text-[#0b0b0c] transition hover:bg-[#8cffa0]"
          >
            <Upload size={14} strokeWidth={2} />
            Add skin
          </button>
        </header>

        <div className="mb-6 flex flex-wrap gap-3">
          {entries.map((entry) => (
            <button
              key={entry.id}
              type="button"
              onClick={() => onSelect(entry.id)}
              className={`flex h-[140px] w-[120px] flex-col rounded-[12px] border p-3 text-left transition ${
                current?.id === entry.id
                  ? 'border-accent bg-[rgba(170,255,169,0.12)]'
                  : 'border-line bg-surface hover:border-white/10 hover:bg-elevated'
              }`}
            >
              <span className="grid flex-1 place-items-center text-ink-2">
                {entry.id === 'account' ? (
                  <Pencil size={20} strokeWidth={1.5} />
                ) : (
                  <SkinPreview skin={entry.url} slimModel={entry.variant === 'slim'} scale={4} headOnly />
                )}
              </span>
              <b className="truncate text-[12.5px] font-semibold text-ink">{entry.name}</b>
              <small className="mt-0.5 text-[11.5px] text-ink-3">{entry.note}</small>
            </button>
          ))}

          <button
            type="button"
            onClick={onAdd}
            className="flex h-[140px] w-[120px] flex-col items-center justify-center gap-1.5 rounded-[12px] border border-dashed border-white/15 text-center transition hover:bg-elevated"
          >
            <span className="grid h-9 w-9 place-items-center text-ink-3">
              <Plus size={20} strokeWidth={1.7} />
            </span>
            <b className="text-[12.5px] font-semibold text-ink">Add skin</b>
            <small className="text-[11.5px] text-ink-3">PNG 64×64</small>
          </button>
        </div>

        <header className="mb-3 flex items-center gap-2">
          <h2 className="text-[15px] font-semibold text-ink">Cosmetics</h2>
          <div className="flex-1" />
          <span className="text-[12px] text-ink-3">Open a type to equip</span>
        </header>

        <div className="grid grid-cols-1 gap-3 min-[820px]:grid-cols-2">
          {COSMETICS.map(({ id, name, Icon }) => (
            <button
              key={id}
              type="button"
              onClick={() => setOpenType(openType === id ? null : id)}
              className={`flex h-16 items-center gap-3 rounded-[12px] border px-3.5 text-left transition ${
                openType === id ? 'border-accent/60 bg-elevated' : 'border-line bg-surface hover:bg-elevated'
              }`}
            >
              <span className="grid h-8 w-8 flex-none place-items-center rounded-lg bg-elevated text-ink-2">
                <Icon size={16} strokeWidth={1.6} />
              </span>
              <span className="grid min-w-0 flex-1 gap-0.5">
                <b className="truncate text-[12.5px] font-semibold text-ink">{name}</b>
                <small className="text-[11.5px] text-ink-3">None owned</small>
              </span>
              <ChevronRight size={15} className="flex-none text-ink-3" />
            </button>
          ))}
        </div>

        {openType ? (
          <div className="mt-3 flex items-center justify-between gap-3 rounded-[12px] border border-line bg-surface px-4 py-3 text-[12.5px] text-ink-2">
            <span>
              Nothing owned in {COSMETICS.find((entry) => entry.id === openType)?.name} yet — cosmetics unlock from
              the store and from events.
            </span>
            <button
              type="button"
              onClick={() => setOpenType(null)}
              className="h-8 flex-none rounded-lg bg-elevated px-3 text-[12.5px] font-medium text-ink hover:bg-hover"
            >
              Close
            </button>
          </div>
        ) : null}
      </section>
    </div>
  );
}
