import { useEffect, useState } from 'react';
import { ChevronLeft, ChevronRight, Trash2 } from 'lucide-react';
import Skin3D from '../ui/Skin3D.jsx';
import SkinAvatar from '../ui/SkinAvatar';

/* Accounts: pick the nickname you play as, add offline names, link Microsoft. */

type Account = { id: string; name: string; uuid?: string; type: 'offline' | 'microsoft' };

type Props = {
  accounts: Account[];
  selectedId: string | null;
  skinFor: (name: string) => string | null;
  slimFor?: (name: string) => boolean;
  busy: boolean;
  onAddOffline: (name: string) => Promise<{ ok: boolean; message?: string }>;
  onAddMicrosoft: () => void;
  onSelect: (id: string) => void;
  onRemove: (id: string) => void;
};

const TYPE_LABEL: Record<string, string> = { offline: 'Offline', microsoft: 'Microsoft' };

export default function AccountsView({
  accounts,
  selectedId,
  skinFor,
  slimFor,
  busy,
  onAddOffline,
  onAddMicrosoft,
  onSelect,
  onRemove
}: Props) {
  const [name, setName] = useState('');
  const [error, setError] = useState('');
  const [adding, setAdding] = useState(false);

  const index = Math.max(0, accounts.findIndex((entry) => entry.id === selectedId));
  const current = accounts[index] ?? null;
  const previous = accounts.length > 1 ? accounts[(index - 1 + accounts.length) % accounts.length] : null;
  const next = accounts.length > 1 ? accounts[(index + 1) % accounts.length] : null;

  const submit = async () => {
    if (!name.trim() || adding) {
      return;
    }
    setAdding(true);
    const result = await onAddOffline(name);
    setAdding(false);
    if (result?.ok) {
      setName('');
      setError('');
    } else {
      setError(result?.message ?? 'Could not add that name');
    }
  };

  useEffect(() => {
    setError('');
  }, [selectedId]);

  return (
    <div className="flex h-full min-h-0 flex-col gap-5 pb-6 pr-6">
      <p className="m-0 font-sans text-[13px] text-ink-2">
        Pick the nickname you will appear as in game. Your Mix Client account stays the same.
      </p>

      <div className="grid min-h-0 flex-1 grid-cols-1 gap-6 overflow-auto min-[1000px]:grid-cols-[minmax(0,48%)_minmax(0,1fr)] min-[1000px]:overflow-hidden">
        {/* left: add + list */}
        <div className="flex min-h-0 flex-col gap-[10px] overflow-auto pr-1">
          <section className="rounded-card border border-[rgba(255,255,255,0.06)] bg-surface p-5">
            <h3 className="m-0 font-sans text-[14px] font-bold text-ink">Add account</h3>

            <div className="mt-4 flex items-stretch overflow-hidden rounded-[10px] border border-[rgba(255,255,255,0.08)] bg-elevated">
              <input
                value={name}
                onChange={(event) => setName(event.target.value)}
                onKeyDown={(event) => {
                  if (event.key === 'Enter') {
                    event.preventDefault();
                    submit();
                  }
                }}
                placeholder="Offline nickname (3–16)"
                maxLength={16}
                className="h-[42px] min-w-0 flex-1 bg-transparent px-3 font-sans text-[13px] text-ink outline-none placeholder:text-placeholder"
              />
              <button
                type="button"
                onClick={submit}
                disabled={busy || adding}
                className="h-[42px] flex-none bg-accent px-5 font-sans text-[13px] font-semibold text-[#0b0b0c] transition hover:bg-[#8cffa0] disabled:opacity-50"
              >
                Add
              </button>
            </div>
            {error ? <p className="m-0 mt-2 font-sans text-[11.5px] text-[#EF4444]">{error}</p> : null}

            <div className="my-4 flex items-center gap-3">
              <span className="h-px flex-1 bg-[rgba(255,255,255,0.06)]" />
              <span className="font-sans text-[11px] text-ink-3">or</span>
              <span className="h-px flex-1 bg-[rgba(255,255,255,0.06)]" />
            </div>

            <button
              type="button"
              onClick={onAddMicrosoft}
              disabled={busy}
              className="flex h-[42px] w-full items-center justify-center gap-2.5 rounded-[10px] border border-[rgba(255,255,255,0.08)] bg-elevated font-sans text-[13px] font-semibold text-ink transition hover:bg-hover disabled:opacity-60"
            >
              <MicrosoftMark />
              Add Microsoft account
            </button>
          </section>

          {accounts.map((entry) => {
            const selected = entry.id === selectedId;
            const skin = skinFor(entry.name);
            return (
              <div
                key={entry.id}
                role="button"
                tabIndex={0}
                onClick={() => onSelect(entry.id)}
                onKeyDown={(event) => {
                  if (event.key === 'Enter' || event.key === ' ') {
                    onSelect(entry.id);
                  }
                }}
                className={`flex h-16 cursor-pointer items-center gap-3 rounded-[14px] border px-4 transition ${
                  selected
                    ? 'border-[rgba(170,255,169,0.55)] bg-[#1b1b1e]'
                    : 'border-[rgba(255,255,255,0.06)] bg-surface hover:border-[rgba(255,255,255,0.12)]'
                }`}
              >
                <span className="grid h-[38px] w-[38px] flex-none place-items-center overflow-hidden rounded-[8px] bg-elevated">
                  {skin ? (
                    <SkinAvatar skin={skin} size={38} className="rounded-[8px]" />
                  ) : (
                    <span className="font-sans text-[13px] font-bold text-ink-2">
                      {entry.name.slice(0, 1).toUpperCase()}
                    </span>
                  )}
                </span>
                <span className="flex min-w-0 flex-1 flex-col">
                  <b className="truncate font-sans text-[14px] font-bold text-ink">{entry.name}</b>
                  <small className="font-sans text-[12px] text-ink-3">{TYPE_LABEL[entry.type]}</small>
                </span>
                {selected ? (
                  <span className="rounded-[6px] bg-[rgba(170,255,169,0.14)] px-2 py-1 font-sans text-[11px] font-semibold text-accent-text">
                    Selected
                  </span>
                ) : null}
                <button
                  type="button"
                  aria-label={`Remove ${entry.name}`}
                  onClick={(event) => {
                    event.stopPropagation();
                    onRemove(entry.id);
                  }}
                  className="grid h-8 w-8 flex-none place-items-center rounded-[8px] text-ink-3 transition hover:bg-[rgba(239,68,68,0.12)] hover:text-[#EF4444]"
                >
                  <Trash2 size={15} strokeWidth={1.8} />
                </button>
              </div>
            );
          })}

          {accounts.length === 0 ? (
            <p className="m-0 px-1 font-sans text-[12px] text-ink-3">No accounts yet. Add one above.</p>
          ) : null}
        </div>

        {/* right: 3D carousel */}
        <section className="relative flex min-h-0 flex-col overflow-hidden rounded-card border border-[rgba(255,255,255,0.06)] bg-surface">
          <div className="pointer-events-none absolute inset-0 bg-[radial-gradient(55%_55%_at_50%_42%,rgba(170,255,169,0.10)_0%,rgba(27,27,30,0)_70%)]" />

          <div className="relative flex min-h-0 flex-1 items-end justify-center">
            {previous ? (
              <div className="pointer-events-none absolute bottom-6 left-6 scale-75 opacity-40">
                <Skin3D skin={skinFor(previous.name)} slim={slimFor?.(previous.name) ?? false} width={150} height={210} zoom={0.8} />
              </div>
            ) : null}
            {next ? (
              <div className="pointer-events-none absolute bottom-6 right-6 scale-75 opacity-40">
                <Skin3D skin={skinFor(next.name)} slim={slimFor?.(next.name) ?? false} width={150} height={210} zoom={0.8} />
              </div>
            ) : null}

            {accounts.length > 1 ? (
              <>
                <button
                  type="button"
                  aria-label="Previous account"
                  onClick={() => previous && onSelect(previous.id)}
                  className="absolute left-4 top-1/2 grid h-[34px] w-[34px] -translate-y-1/2 place-items-center rounded-full bg-elevated text-ink-2 transition hover:bg-hover hover:text-ink"
                >
                  <ChevronLeft size={16} />
                </button>
                <button
                  type="button"
                  aria-label="Next account"
                  onClick={() => next && onSelect(next.id)}
                  className="absolute right-4 top-1/2 grid h-[34px] w-[34px] -translate-y-1/2 place-items-center rounded-full bg-elevated text-ink-2 transition hover:bg-hover hover:text-ink"
                >
                  <ChevronRight size={16} />
                </button>
              </>
            ) : null}

            {current ? (
              <div className="relative flex flex-col items-center">
                <div className="pointer-events-none absolute bottom-2 h-6 w-[120px] rounded-[50%] bg-black/45 blur-[10px]" />
                <Skin3D skin={skinFor(current.name)} slim={slimFor?.(current.name) ?? false} width={260} height={330} zoom={0.86} />
              </div>
            ) : (
              <p className="mb-24 font-sans text-[12.5px] text-ink-3">Add an account to preview it here.</p>
            )}
          </div>

          <div className="relative flex flex-col items-center gap-1 pb-6">
            <b className="font-display text-[16px] font-extrabold text-ink">{current?.name ?? '—'}</b>
            <small className="font-sans text-[12px] text-ink-3">
              {current ? (current.type === 'microsoft' ? 'Microsoft Account' : 'Offline') : 'No account selected'}
            </small>
          </div>
        </section>
      </div>
    </div>
  );
}

/* The four square Microsoft mark, drawn inline so there is no icon dependency. */
function MicrosoftMark() {
  return (
    <svg width="16" height="16" viewBox="0 0 16 16" aria-hidden="true">
      <rect x="0" y="0" width="7" height="7" fill="#F25022" />
      <rect x="9" y="0" width="7" height="7" fill="#7FBA00" />
      <rect x="0" y="9" width="7" height="7" fill="#00A4EF" />
      <rect x="9" y="9" width="7" height="7" fill="#FFB900" />
    </svg>
  );
}
