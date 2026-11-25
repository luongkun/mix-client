/* Pill tab bar used under the page title (Modpack Builder, Settings). */

type Props = {
  tabs: { id: string; label: string }[];
  active: string;
  onChange: (id: string) => void;
};

export default function SubTabs({ tabs, active, onChange }: Props) {
  return (
    <div className="inline-flex flex-wrap items-center gap-1 rounded-xl border border-line bg-surface p-1">
      {tabs.map((tab) => (
        <button
          key={tab.id}
          type="button"
          onClick={() => onChange(tab.id)}
          className={`rounded-lg px-3.5 py-1.5 text-[13px] font-medium transition ${
            active === tab.id ? 'bg-elevated text-ink' : 'text-ink-2 hover:text-ink'
          }`}
        >
          {tab.label}
        </button>
      ))}
    </div>
  );
}
