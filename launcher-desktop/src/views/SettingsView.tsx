import { useState } from 'react';
import {
  Cpu,
  Download,
  FolderOpen,
  HardDrive,
  Info,
  LogOut,
  Monitor,
  RefreshCw,
  Sliders,
  User
} from 'lucide-react';
import SubTabs from '../ui/SubTabs.jsx';
import ToggleSwitch from '../ui/ToggleSwitch.jsx';

/* Settings: sub-tabs on top, two columns of cards below. Each row is an icon,
   a label with an optional hint, and a control on the right. */

type State = {
  ramMb: number;
  javaPath: string;
  closeOnLaunch: boolean;
  autoUpdate: boolean;
  msClientId: string;
  updateUrl: string;
  activeProfile: string;
  profiles: { name: string }[];
};

type Props = {
  state: State;
  account: { name: string; uuid?: string } | null;
  info: { version: string; clientVersion: string; distDir: string; pendingLauncher?: string | null };
  update: (patch: Partial<State>) => void;
  onSignIn: () => void;
  onSignOut: () => void;
  onCheckUpdate: () => void;
  onOpenDir: (target: string) => void;
  status: string;
};

const TABS = [
  { id: 'general', label: 'General' },
  { id: 'minecraft', label: 'Minecraft' },
  { id: 'storage', label: 'Storage' },
  { id: 'cloud', label: 'Cloud' },
  { id: 'account', label: 'Account' },
  { id: 'about', label: 'About' }
];

function Row({
  Icon,
  label,
  hint,
  children
}: {
  Icon: typeof Cpu;
  label: string;
  hint?: string;
  children?: React.ReactNode;
}) {
  return (
    <div className="flex items-center gap-3 border-t border-line-soft px-4 py-3 first:border-t-0">
      <Icon size={18} strokeWidth={1.5} className="flex-none text-ink-3" />
      <div className="flex min-w-0 flex-1 flex-col">
        <span className="text-[13px] font-medium text-ink">{label}</span>
        {hint ? <small className="text-[11.5px] leading-snug text-ink-3">{hint}</small> : null}
      </div>
      {children}
    </div>
  );
}

function Card({ title, children }: { title: string; children: React.ReactNode }) {
  return (
    <section className="overflow-hidden rounded-card border border-line bg-surface">
      <header className="px-4 py-3">
        <h3 className="text-[12px] font-semibold uppercase tracking-[0.12em] text-ink-3">{title}</h3>
      </header>
      {children}
    </section>
  );
}

export default function SettingsView({ state, account, info, update, onSignIn, onSignOut, onCheckUpdate, onOpenDir, status }: Props) {
  const [tab, setTab] = useState('general');

  const field =
    'h-9 rounded-[10px] border border-line bg-canvas px-3 text-[12.5px] text-ink outline-none placeholder:text-placeholder focus:border-accent/60';

  return (
    <div className="flex h-full min-h-0 flex-col gap-4 pb-6 pr-6">
      <div className="flex-none">
        <SubTabs tabs={TABS} active={tab} onChange={setTab} />
      </div>

      {/* The window can be as narrow as 980 device pixels, which is ~784 CSS
          pixels at 125% Windows scaling: two columns of rows with a control on
          the right stop fitting long before that. One column until there is
          room for two. */}
      <div className="grid min-h-0 flex-1 grid-cols-1 content-start gap-4 overflow-auto pr-1 min-[1000px]:grid-cols-2 min-[1000px]:gap-6">
        {tab === 'general' ? (
          <>
            <Card title="Launcher">
              <Row Icon={Monitor} label="Close launcher when the game starts" hint="Frees the window while you play.">
                <ToggleSwitch
                  checked={state.closeOnLaunch}
                  onChange={(value) => update({ closeOnLaunch: value })}
                  label="Close launcher when the game starts"
                />
              </Row>
              <Row Icon={Sliders} label="Default profile" hint="Used for the launch button.">
                <select
                  value={state.activeProfile}
                  onChange={(event) => update({ activeProfile: event.target.value })}
                  className={field}
                >
                  {state.profiles.map((profile) => (
                    <option key={profile.name} value={profile.name}>
                      {profile.name}
                    </option>
                  ))}
                </select>
              </Row>
            </Card>

            <Card title="Updates">
              <Row Icon={RefreshCw} label="Check for updates on start" hint="Keeps the client build current.">
                <ToggleSwitch
                  checked={state.autoUpdate}
                  onChange={(value) => update({ autoUpdate: value })}
                  label="Check for updates on start"
                />
              </Row>
              <Row Icon={Download} label="Client build" hint={status || 'Up to date'}>
                <button
                  type="button"
                  onClick={onCheckUpdate}
                  className="h-9 rounded-[10px] bg-elevated px-3 text-[12.5px] font-medium text-ink hover:bg-hover"
                >
                  Check now
                </button>
              </Row>
            </Card>
          </>
        ) : null}

        {tab === 'minecraft' ? (
          <>
            <Card title="Launching">
              <Row Icon={Cpu} label="Memory" hint={`${state.ramMb} MB allocated to the game`}>
                <input
                  type="range"
                  min={1024}
                  max={8192}
                  step={256}
                  value={state.ramMb}
                  onChange={(event) => update({ ramMb: Number(event.target.value) })}
                  className="h-1 w-40 cursor-pointer appearance-none rounded-full bg-hover accent-accent"
                />
              </Row>
              <Row Icon={HardDrive} label="Java executable" hint="Path or command used to start the game.">
                <input
                  value={state.javaPath}
                  onChange={(event) => update({ javaPath: event.target.value })}
                  className={`${field} w-full max-w-[14rem]`}
                />
              </Row>
            </Card>

          </>
        ) : null}

        {tab === 'storage' ? (
          <>
            <Card title="Folders">
              <Row Icon={FolderOpen} label="Game folder" hint="Saves, configs and screenshots.">
                <button
                  type="button"
                  onClick={() => onOpenDir('minecraft')}
                  className="h-9 rounded-[10px] bg-elevated px-3 text-[12.5px] font-medium text-ink hover:bg-hover"
                >
                  Open
                </button>
              </Row>
              <Row Icon={FolderOpen} label="Mods folder" hint="Where Forge loads mods from.">
                <button
                  type="button"
                  onClick={() => onOpenDir('mods')}
                  className="h-9 rounded-[10px] bg-elevated px-3 text-[12.5px] font-medium text-ink hover:bg-hover"
                >
                  Open
                </button>
              </Row>
              <Row Icon={FolderOpen} label="Logs" hint="Latest game and launcher logs.">
                <button
                  type="button"
                  onClick={() => onOpenDir('logs')}
                  className="h-9 rounded-[10px] bg-elevated px-3 text-[12.5px] font-medium text-ink hover:bg-hover"
                >
                  Open
                </button>
              </Row>
            </Card>
          </>
        ) : null}

        {tab === 'cloud' ? (
          <>
            <Card title="Cloud">
              <Row Icon={Download} label="Update manifest" hint="Where launcher and client builds come from.">
                <input
                  value={state.updateUrl}
                  onChange={(event) => update({ updateUrl: event.target.value })}
                  placeholder="https://…/update.json"
                  className={`${field} w-full max-w-[16rem]`}
                />
              </Row>
              <Row Icon={Info} label="Pending launcher" hint={info.pendingLauncher ?? 'Nothing staged'}>
                <button
                  type="button"
                  onClick={onCheckUpdate}
                  className="h-9 rounded-[10px] bg-elevated px-3 text-[12.5px] font-medium text-ink hover:bg-hover"
                >
                  Refresh
                </button>
              </Row>
            </Card>
          </>
        ) : null}

        {tab === 'account' ? (
          <>
            <Card title="Account">
              <Row Icon={User} label={account ? account.name : 'Not signed in'} hint={account?.uuid ?? 'Offline session'}>
                {account ? (
                  <button
                    type="button"
                    onClick={onSignOut}
                    className="inline-flex h-9 items-center gap-2 rounded-[10px] bg-elevated px-3 text-[12.5px] font-medium text-ink hover:bg-hover"
                  >
                    <LogOut size={14} strokeWidth={1.8} />
                    Sign out
                  </button>
                ) : (
                  <button
                    type="button"
                    onClick={onSignIn}
                    className="h-9 rounded-[10px] bg-accent px-3.5 text-[12.5px] font-semibold text-[#0b0b0c] hover:bg-[#8cffa0]"
                  >
                    Sign in
                  </button>
                )}
              </Row>
              <Row Icon={User} label="Microsoft client ID" hint="Pre-filled for everyone. Only change it if you use your own Azure app.">
                <input
                  value={state.msClientId}
                  onChange={(event) => update({ msClientId: event.target.value })}
                  placeholder="optional"
                  className={`${field} w-full max-w-[16rem]`}
                />
              </Row>
            </Card>
          </>
        ) : null}

        {tab === 'about' ? (
          <>
            <Card title="About">
              <Row Icon={Info} label="Launcher" hint={info.distDir}>
                <span className="text-[12.5px] font-medium text-ink-2">v{info.version}</span>
              </Row>
              <Row Icon={Info} label="Client build" hint="Minecraft 1.8.9 · Forge 11.15.1.2318">
                <span className="text-[12.5px] font-medium text-ink-2">{info.clientVersion}</span>
              </Row>
            </Card>
          </>
        ) : null}
      </div>
    </div>
  );
}
