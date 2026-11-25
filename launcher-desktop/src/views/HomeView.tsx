import { ChevronRight, Circle, Play, Users } from 'lucide-react';
import Skin3D from '../ui/Skin3D.jsx';

/* Home: launch banner + news on the left, friends and partner servers right. */

type Props = {
  account: { name: string; uuid?: string } | null;
  skin: string | null;
  localSkins?: { id?: string; name?: string; url?: string; variant?: string }[];
  slim?: boolean;
  profileName: string;
  version?: string;
  ramMb: number;
  launching: boolean;
  status: string;
  statusTone: string;
  progress: { done: number; total: number } | null;
  onLaunch: () => void;
  /* everything account-related happens on the Accounts page, so Home only
     points at it instead of starting a sign-in of its own */
  onOpenAccounts: () => void;
  onOpenWardrobe: () => void;
};

const SERVERS = [
  { name: 'HYPIXEL', address: 'mc.hypixel.net', players: '48,213', online: true },
  { name: 'MIXCRAFT', address: 'play.mixcraft.net', players: '12,904', online: true },
  { name: 'ORIGINS', address: 'play.origins.gg', players: '3,417', online: true },
  { name: 'ARCHON', address: 'play.archon.gg', players: '0', online: false }
];

export default function HomeView({
  account,
  skin,
  localSkins = [],
  slim = false,
  profileName,
  version = '1.8.9',
  ramMb,
  launching,
  status,
  statusTone,
  progress,
  onLaunch,
  onOpenAccounts,
  onOpenWardrobe
}: Props) {
  const percent = progress && progress.total ? Math.min(100, Math.round((progress.done / progress.total) * 100)) : 0;
  /* Offline names have no Mojang profile, so accountSkin returns nothing. Fall
     back to the first local skin instead of leaving the panel empty. */
  const previewSkin = skin ?? localSkins[0]?.url ?? null;

  return (
    <div className="grid h-full min-h-0 grid-cols-1 gap-6 overflow-auto pb-6 pr-6 min-[1000px]:grid-cols-[minmax(0,68%)_minmax(0,1fr)] min-[1000px]:overflow-hidden">
      <div className="flex min-h-0 min-w-0 flex-col gap-6 overflow-visible pr-1 min-[1000px]:overflow-auto">
        <section className="relative h-60 flex-none overflow-hidden rounded-card border border-line bg-surface">
          <div className="absolute inset-0 bg-[radial-gradient(120%_140%_at_15%_20%,#1e1e21_0%,#171719_45%,#121214_100%)]" />
          <div className="absolute inset-0 bg-[linear-gradient(90deg,transparent_0%,rgba(19,19,20,0.35)_45%,rgba(19,19,20,0.92)_100%)]" />

          <div className="absolute bottom-1 left-10 flex h-[230px] items-end">
            <Skin3D skin={previewSkin} slim={slim} width={200} height={226} zoom={0.8} />
          </div>

          <div className="relative flex h-full flex-col items-end justify-center gap-3 px-8 text-right">
            <h2 className="font-display text-[28px] font-black uppercase leading-[1.05] tracking-tight text-white">
              {account ? 'Ready to play' : 'You are not signed in'}
            </h2>
            <p className="max-w-[380px] font-sans text-[12.5px] font-medium leading-relaxed text-ink-2">
              {account
                ? `${profileName} profile · ${ramMb} MB heap · Minecraft ${version}`
                : 'Sign in once and Mix Client keeps your session, skins and cosmetics in sync. Accounts, offline names and skins are managed on the Accounts page.'}
            </p>
            {account ? (
              <button
                type="button"
                onClick={onLaunch}
                disabled={launching}
                className="mt-1 inline-flex h-11 items-center gap-2 rounded-[10px] bg-accent px-6 font-display text-[13px] font-extrabold uppercase tracking-wider text-[#0b0b0c] transition hover:bg-[#8cffa0] disabled:opacity-60"
              >
                <Play size={15} strokeWidth={2} />
                {`Launch Mix Client ${version}`}
              </button>
            ) : (
              <button
                type="button"
                onClick={onOpenAccounts}
                className="mt-1 inline-flex h-11 items-center gap-2 rounded-[10px] bg-accent px-6 font-display text-[13px] font-extrabold uppercase tracking-wider text-[#0b0b0c] transition hover:bg-[#8cffa0]"
              >
                Sign in
              </button>
            )}
            <span
              className={`text-[11.5px] ${
                statusTone === 'error' ? 'text-[#f07074]' : statusTone === 'ok' ? 'text-online' : 'text-ink-3'
              }`}
            >
              {status}
            </span>
          </div>

          {progress ? (
            <div className="absolute inset-x-0 bottom-0 h-1 bg-white/10">
              <div className="h-full bg-accent" style={{ width: `${percent}%` }} />
            </div>
          ) : null}
        </section>

        <section className="flex flex-none flex-col gap-3">
          <header className="flex items-center justify-between">
            <h3 className="font-sans text-[14px] font-bold text-ink">What&apos;s new</h3>
            <button type="button" className="inline-flex items-center gap-1 font-sans text-[12px] font-semibold text-accent-text">
              All news
              <ChevronRight size={14} />
            </button>
          </header>

          <article className="relative flex h-40 flex-col justify-end overflow-hidden rounded-card border border-line bg-surface p-5">
            <div className="absolute inset-0 bg-[radial-gradient(120%_120%_at_80%_0%,#26262a_0%,#1a1a1d_55%,#131315_100%)]" />
            <div className="relative flex flex-col gap-2">
              <h4 className="font-display text-[18px] font-black uppercase tracking-tight text-white">Mix Client 1.8.0 is out</h4>
              <p className="max-w-[520px] font-sans text-[12.5px] font-normal leading-relaxed text-ink-2">
                New wardrobe with a real 3D preview, quieter rendering pipeline and a rebuilt settings layout.
              </p>
            </div>
            <span className="absolute bottom-4 right-5 text-[11.5px] text-ink-3">2d ago</span>
          </article>
        </section>
      </div>

      <div className="flex min-h-0 min-w-0 flex-col gap-6 overflow-visible pl-1 min-[1000px]:overflow-auto">
        <section className="rounded-card border border-line bg-surface p-4">
          <header className="mb-3 flex items-center justify-between">
            <h3 className="font-sans text-[14px] font-bold text-ink">Friends</h3>
            <button type="button" className="font-sans text-[12px] font-semibold text-accent-text">
              View all
            </button>
          </header>
          {account ? (
            <ul className="m-0 flex list-none flex-col gap-1 p-0">
              {[
                { name: 'Notch', state: 'In the launcher' },
                { name: 'Jeb_', state: 'Playing 1.8.9' },
                { name: 'Dinnerbone', state: 'Offline' }
              ].map((friend) => (
                <li key={friend.name} className="flex items-center gap-3 rounded-lg px-2 py-2 hover:bg-elevated">
                  <span className="grid h-8 w-8 place-items-center rounded-full bg-elevated text-[11px] font-semibold text-ink-2">
                    {friend.name.slice(0, 2).toUpperCase()}
                  </span>
                  <span className="flex min-w-0 flex-col">
                    <b className="truncate font-sans text-[13px] font-semibold text-ink">{friend.name}</b>
                    <small className="font-sans text-[11.5px] font-normal text-ink-3">{friend.state}</small>
                  </span>
                </li>
              ))}
            </ul>
          ) : (
            <div className="flex flex-col items-center gap-2 py-6 text-center">
              <Users size={20} className="text-ink-3" strokeWidth={1.5} />
              <p className="m-0 text-[12.5px] text-ink-2">Sign in to see who is online.</p>
              <button
                type="button"
                onClick={onOpenAccounts}
                className="mt-1 rounded-lg bg-elevated px-3 py-1.5 text-[12.5px] font-medium text-ink hover:bg-hover"
              >
                Sign in
              </button>
            </div>
          )}
        </section>

        <section className="rounded-card border border-line bg-surface p-4">
          <header className="mb-3 flex items-center justify-between">
            <h3 className="font-sans text-[14px] font-bold text-ink">Featured servers</h3>
            <button type="button" className="font-sans text-[12px] font-semibold text-accent-text">
              View all
            </button>
          </header>
          <ul className="m-0 flex list-none flex-col gap-1 p-0">
            {SERVERS.map((server) => (
              <li key={server.name} className="flex items-center gap-3 rounded-lg px-2 py-2 hover:bg-elevated">
                <span className="grid h-9 w-9 flex-none place-items-center rounded-lg border border-line bg-elevated text-[11px] font-bold text-ink-2">
                  {server.name.slice(0, 2)}
                </span>
                <span className="flex min-w-0 flex-1 flex-col">
                  <b className="truncate font-display text-[12.5px] font-extrabold uppercase tracking-wide text-ink">{server.name}</b>
                  <small className="truncate font-sans text-[11.5px] font-normal text-ink-3">{server.address}</small>
                </span>
                <span className="flex items-center gap-1.5 font-sans text-[12px] font-semibold tabular-nums text-ink-2">
                  <Circle
                    size={7}
                    className={server.online ? 'fill-online text-online' : 'fill-ink-3 text-ink-3'}
                    strokeWidth={0}
                  />
                  {server.players}
                </span>
              </li>
            ))}
          </ul>
        </section>

        <button
          type="button"
          onClick={onOpenWardrobe}
          className="flex items-center justify-between rounded-card border border-line bg-surface px-4 py-3 text-left transition hover:bg-elevated"
        >
          <span className="flex flex-col">
            <b className="text-[13px] font-semibold text-ink">Wardrobe</b>
            <small className="text-[11.5px] text-ink-3">Change your skin and cosmetics</small>
          </span>
          <ChevronRight size={16} className="text-ink-3" />
        </button>
      </div>
    </div>
  );
}
