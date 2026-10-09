import { useRef, useState } from "react";
import { DrumReel } from "@/components/auric/drum-reel";
import { LaserStrip } from "@/components/auric/laser-strip";
import { SPAN_COUNT, type Span } from "@/lib/format";
import { cn } from "@/lib/utils";

const HMS: { key: "hours" | "minutes" | "seconds"; label: string }[] = [
  { key: "hours", label: "Hours" },
  { key: "minutes", label: "Minutes" },
  { key: "seconds", label: "Seconds" },
];

const CAL: { key: "days" | "weeks" | "months" | "years"; label: string }[] = [
  { key: "days", label: "Days" },
  { key: "weeks", label: "Weeks" },
  { key: "months", label: "Months" },
  { key: "years", label: "Years" },
];

type SpanReelsProps = {
  value: Span;
  onChange: (key: keyof Span, n: number) => void;
  onTick?: () => void;
  disabled?: boolean;
  className?: string;
  readout?: string;
};

export function SpanReels({
  value,
  onChange,
  onTick,
  disabled,
  className,
  readout,
}: SpanReelsProps) {
  const [long, setLong] = useState(true);
  const hmsHostRef = useRef<HTMLDivElement>(null);
  const hmsBankRef = useRef<HTMLDivElement>(null);
  const calendarHostRef = useRef<HTMLDivElement>(null);
  const calendarBankRef = useRef<HTMLDivElement>(null);

  return (
    <div className={cn("flex w-full min-w-0 max-w-full flex-col items-center px-1 pb-2", className)}>
      <div ref={hmsHostRef} className="reel-console-wrap w-full max-w-full" data-slot="chrome">
        <div className="reel-console">
          <div className="reel-console-sheen" aria-hidden />
          <div ref={hmsBankRef} className="reel-bank">
            {HMS.map((u, i) => (
              <div key={u.key} className="contents">
                {i > 0 ? (
                  <span className="reel-pips" aria-hidden>
                    <span />
                    <span />
                  </span>
                ) : null}
                <DrumReel
                  size="trio"
                  count={SPAN_COUNT[u.key]}
                  value={Math.min(Math.max(0, value[u.key]), SPAN_COUNT[u.key] - 1)}
                  onChange={(n) => onChange(u.key, n)}
                  onTick={onTick}
                  disabled={disabled}
                  label={u.label}
                  selector="none"
                  className="min-w-0 flex-1"
                />
              </div>
            ))}
          </div>
        </div>
        <LaserStrip hostRef={hmsHostRef} bankRef={hmsBankRef} />
        {readout ? (
          <div
            className="reel-time-plaque"
            data-slot="digit"
            aria-label={`Selected time ${readout}`}
          >
            <span className="font-digits digit-cut">{readout}</span>
          </div>
        ) : null}
      </div>

      <button
        type="button"
        className="mt-3 min-h-11 w-full min-w-0 rounded-lg px-3 text-sm text-accent shadow-[0_0_0_1px_var(--line)]"
        onClick={() => setLong((v) => !v)}
        aria-expanded={long}
      >
        {long ? "Hide days and years" : "Longer than a day"}
      </button>

      {long ? (
        <div ref={calendarHostRef} className="relative mt-3 w-full min-w-0">
          <p className="mb-2 text-center text-xs text-muted">Duration units: month = 30 days · year = 360 days</p>
          <div className="reel-calendar-bank relative grid w-full min-w-0 grid-cols-4 items-end gap-1" ref={calendarBankRef}>
          {CAL.map((u) => (
            <DrumReel
              key={u.key}
              size="sm"
              count={SPAN_COUNT[u.key]}
              value={Math.min(Math.max(0, value[u.key]), SPAN_COUNT[u.key] - 1)}
              onChange={(n) => onChange(u.key, n)}
              onTick={onTick}
              disabled={disabled}
              label={u.label}
              selector="none"
              className="min-w-0 flex-1"
            />
          ))}
          </div>
          <LaserStrip hostRef={calendarHostRef} bankRef={calendarBankRef} />
        </div>
      ) : null}
    </div>
  );
}
