import { DrumReel } from "@/components/auric/drum-reel";
import { SPAN_COUNT, type Span } from "@/lib/format";
import { cn } from "@/lib/utils";

const SPAN_UNITS: { key: keyof Span; label: string }[] = [
  { key: "years", label: "Years" },
  { key: "months", label: "Months" },
  { key: "weeks", label: "Weeks" },
  { key: "days", label: "Days" },
  { key: "hours", label: "Hours" },
  { key: "minutes", label: "Minutes" },
  { key: "seconds", label: "Seconds" },
  { key: "cs", label: "MS" },
];

type PitWheelProps = {
  value: Span;
  unitIndex: number;
  onUnit: (i: number) => void;
  onChange: (key: keyof Span, n: number) => void;
  onTick?: () => void;
  disabled?: boolean;
};

export function PitWheel({ value, unitIndex, onUnit, onChange, onTick, disabled }: PitWheelProps) {
  const unit = SPAN_UNITS[unitIndex] ?? SPAN_UNITS[5]!;
  const count = SPAN_COUNT[unit.key];
  const n = Math.min(Math.max(0, value[unit.key]), count - 1);

  return (
    <div className="flex w-full flex-col items-center gap-4 px-2">
      <DrumReel
        size="plaque"
        count={SPAN_UNITS.length}
        value={unitIndex}
        labels={SPAN_UNITS.map((u) => u.label)}
        pad={0}
        onChange={onUnit}
        onTick={onTick}
        disabled={disabled}
        label="Unit"
      />
      <div className={cn("pit-bezel grid place-items-center p-4", disabled && "opacity-60")}>
        <DrumReel
          size="hero"
          count={count}
          value={n}
          onChange={(v) => onChange(unit.key, v)}
          onTick={onTick}
          disabled={disabled}
          label={unit.label}
        />
      </div>
    </div>
  );
}
