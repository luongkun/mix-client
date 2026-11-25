import { Minus, Plus, Settings, X, Zap } from 'lucide-react';

/* Top bar: logo, the current page title in condensed caps, then the utility
   cluster and the window controls. */

type Props = {
  title: string;
  onMinimize: () => void;
  onClose: () => void;
  onCreate?: () => void;
  onSettings?: () => void;
  onQuickLaunch?: () => void;
};

export default function TopBar({ title, onMinimize, onClose, onCreate, onSettings, onQuickLaunch }: Props) {
  return (
    <header className="flex h-16 flex-none items-center gap-4 pl-6 pr-3">
      <div className="flex items-center gap-3">
        <span className="grid h-8 w-8 place-items-center rounded-lg border border-line bg-surface">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8"
               strokeLinecap="round" strokeLinejoin="round" className="text-ink">
            <path d="M7 18a4 4 0 010-8 5.5 5.5 0 0110.5 1.6A3.2 3.2 0 0117.5 18z" />
          </svg>
        </span>
        <h1 className="page-title text-ink">{title}</h1>
      </div>

      <div className="flex-1" />

      <div className="flex items-center gap-1">
        <button
          type="button"
          onClick={onQuickLaunch}
          title="Quick launch"
          className="grid h-8 w-8 place-items-center rounded-lg text-ink-2 transition hover:bg-elevated hover:text-ink"
        >
          <Zap size={16} strokeWidth={1.5} />
        </button>
        <button
          type="button"
          onClick={onCreate}
          title="Create profile"
          className="grid h-8 w-8 place-items-center rounded-lg text-ink-2 transition hover:bg-elevated hover:text-ink"
        >
          <Plus size={16} strokeWidth={1.5} />
        </button>
        <button
          type="button"
          onClick={onSettings}
          title="Settings"
          className="grid h-8 w-8 place-items-center rounded-lg text-ink-2 transition hover:bg-elevated hover:text-ink"
        >
          <Settings size={16} strokeWidth={1.5} />
        </button>
        <span className="mx-1 h-5 w-px bg-line" />
      </div>

      <div className="flex items-center gap-1">
        <button
          type="button"
          onClick={onMinimize}
          aria-label="Minimize"
          className="grid h-8 w-8 place-items-center rounded-lg text-ink-2 transition hover:bg-elevated hover:text-ink"
        >
          <Minus size={16} strokeWidth={1.5} />
        </button>
        <button
          type="button"
          onClick={onClose}
          aria-label="Close"
          className="grid h-8 w-8 place-items-center rounded-lg text-ink-2 transition hover:bg-[#e0313f] hover:text-white"
        >
          <X size={16} strokeWidth={1.5} />
        </button>
      </div>
    </header>
  );
}
