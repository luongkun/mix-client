/* iOS-style pill switch: 44×24, blue when on, grey when off. */

type Props = {
  checked: boolean;
  onChange: (value: boolean) => void;
  disabled?: boolean;
  label?: string;
};

export default function ToggleSwitch({ checked, onChange, disabled = false, label }: Props) {
  return (
    <button
      type="button"
      role="switch"
      aria-checked={checked}
      aria-label={label}
      disabled={disabled}
      onClick={() => onChange(!checked)}
      className={`relative h-6 w-11 flex-none rounded-full transition ${
        checked ? 'bg-accent' : 'bg-hover'
      } ${disabled ? 'cursor-default opacity-50' : 'cursor-pointer'}`}
    >
      <span
        className={`absolute left-0.5 top-0.5 h-5 w-5 rounded-full bg-white transition-transform ${
          checked ? 'translate-x-[20px]' : 'translate-x-0'
        }`}
      />
    </button>
  );
}
