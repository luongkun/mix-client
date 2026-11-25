import {
  Boxes,
  Cog,
  Compass,
  Home,
  Image,
  Layers,
  Newspaper,
  Shirt,
  Store,
  User,
  Users,
  Wrench
} from 'lucide-react';

/* Floating pill sidebar: navigation on top, system on the bottom. The active
   item is a soft blue disc with a light blue glyph. */

type Item = {
  id: string;
  label: string;
  Icon: typeof Home;
  ready?: boolean;
};

const MAIN: Item[] = [
  { id: 'home', label: 'Home', Icon: Home, ready: true },
  { id: 'versions', label: 'Versions', Icon: Layers, ready: true },
  { id: 'mods', label: 'Mods', Icon: Boxes, ready: true },
  { id: 'builder', label: 'Modpack Builder', Icon: Wrench },
  { id: 'servers', label: 'Servers', Icon: Compass },
  { id: 'wardrobe', label: 'Wardrobe', Icon: Shirt, ready: true },
  { id: 'screenshots', label: 'Screenshots', Icon: Image },
  { id: 'store', label: 'Store', Icon: Store },
  { id: 'news', label: 'News', Icon: Newspaper }
];

const SYSTEM: Item[] = [
  { id: 'friends', label: 'Friends', Icon: Users },
  { id: 'settings', label: 'Settings', Icon: Cog, ready: true },
  { id: 'account', label: 'Account', Icon: User, ready: true }
];

type Props = {
  page: string;
  onNavigate: (id: string) => void;
};

function RailButton({ item, active, onNavigate }: { item: Item; active: boolean; onNavigate: (id: string) => void }) {
  const { Icon, label, id, ready } = item;
  return (
    <button
      type="button"
      title={ready ? label : `${label} — coming soon`}
      aria-label={label}
      aria-current={active ? 'page' : undefined}
      onClick={() => ready && onNavigate(id)}
      className={`grid h-10 w-10 place-items-center rounded-full transition ${
        active
          ? 'bg-accent-soft text-accent-text'
          : ready
            ? 'text-ink-2 hover:bg-elevated hover:text-ink'
            : 'text-ink-3/70 hover:bg-elevated/60'
      }`}
    >
      <Icon size={19} strokeWidth={1.5} />
    </button>
  );
}

export default function Sidebar({ page, onNavigate }: Props) {
  return (
    <nav className="ml-4 flex w-14 flex-none flex-col items-center gap-3 self-start pt-2">
      <div className="flex w-14 flex-col items-center gap-1 rounded-full border border-line bg-surface py-2">
        {MAIN.map((item) => (
          <RailButton key={item.id} item={item} active={page === item.id} onNavigate={onNavigate} />
        ))}
      </div>

      <div className="flex w-14 flex-col items-center gap-1 rounded-full border border-line bg-surface py-2">
        {SYSTEM.map((item) => (
          <RailButton key={item.id} item={item} active={page === item.id} onNavigate={onNavigate} />
        ))}
      </div>
    </nav>
  );
}
