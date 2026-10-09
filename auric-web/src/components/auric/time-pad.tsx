import { useState } from "react";
import { SPAN_MAX } from "@/lib/format";
import { playClick } from "@/lib/audio";
import { hapticTick } from "@/lib/haptics";
import { cn } from "@/lib/utils";

export type TimeUnit = "hours" | "minutes" | "seconds";

const KEYS = ["1", "2", "3", "4", "5", "6", "7", "8", "9", "C", "0", "⌫"] as const;

function cap(unit: TimeUnit, n: number) {
  const max = unit === "hours" ? SPAN_MAX.hours : unit === "minutes" ? SPAN_MAX.minutes : SPAN_MAX.seconds;
  return Math.max(0, Math.min(max, Math.floor(n)));
}

export function AcrylicReadout({ value, pulse }: { value: string; pulse?: boolean }) {
  return (
    <p className={cn("tx-acrylic", pulse && "is-live")} aria-live="polite">
      {value}
    </p>
  );
}

export function TimePad({
  hours,
  minutes,
  seconds,
  onChange,
  disabled,
}: {
  hours: number;
  minutes: number;
  seconds: number;
  onChange: (unit: TimeUnit, n: number) => void;
  disabled?: boolean;
}) {
  const [unit, setUnit] = useState<TimeUnit>("minutes");
  const values: Record<TimeUnit, number> = { hours, minutes, seconds };

  function step(dir: 1 | -1) {
    if (disabled) return;
    hapticTick();
    onChange(unit, cap(unit, values[unit] + dir));
  }

  function key(k: (typeof KEYS)[number]) {
    if (disabled) return;
    hapticTick();
    if (k === "C") {
      onChange(unit, 0);
      return;
    }
    const cur = values[unit];
    if (k === "⌫") {
      onChange(unit, cap(unit, Math.floor(cur / 10)));
      return;
    }
    onChange(unit, cap(unit, cur * 10 + Number(k)));
  }

  return (
    <div className="tx-pad">
      <div className="tx-pad-units">
        {(["hours", "minutes", "seconds"] as const).map((id) => (
          <button
            key={id}
            type="button"
            className={cn("tx-pad-unit", unit === id && "is-on")}
            disabled={disabled}
            onClick={() => setUnit(id)}
            aria-pressed={unit === id}
          >
            <span className="tx-pad-kicker">{id.slice(0, 3)}</span>
            <span className="tx-pad-num">{String(values[id]).padStart(2, "0")}</span>
          </button>
        ))}
      </div>
      <div className="tx-pad-step">
        <button type="button" className="tx-btn tx-btn-min" disabled={disabled} onClick={() => step(1)} aria-label={`Increase ${unit}`}>
          +
        </button>
        <button type="button" className="tx-btn tx-btn-min" disabled={disabled} onClick={() => step(-1)} aria-label={`Decrease ${unit}`}>
          −
        </button>
      </div>
      <div className="tx-pad-keys">
        {KEYS.map((k) => (
          <button
            key={k}
            type="button"
            className="tx-key"
            disabled={disabled}
            onClick={() => key(k)}
            aria-label={k === "⌫" ? "Backspace" : k === "C" ? "Clear" : k}
          >
            {k}
          </button>
        ))}
      </div>
    </div>
  );
}

export function GalaxyField({
  label,
  display,
  onSubmit,
}: {
  label: string;
  display: string;
  onSubmit: (raw: string) => string | void;
}) {
  const [open, setOpen] = useState(false);
  const [draft, setDraft] = useState("");
  const [err, setErr] = useState("");

  function apply() {
    const msg = onSubmit(draft);
    if (msg) {
      setErr(msg);
      return;
    }
    playClick();
    setOpen(false);
  }

  return (
    <>
      <button
        type="button"
        className="tx-galaxy"
        onClick={() => {
          playClick();
          setDraft("");
          setErr("");
          setOpen(true);
        }}
      >
        <span className="tx-galaxy-kicker">{label}</span>
        <span className="tx-galaxy-value">{display}</span>
      </button>
      {open ? (
        <div className="tx-modal-layer" role="dialog" aria-label="Manual entry">
          <div className="tx-panel is-solid tx-modal-panel">
            <p className="tx-prompt">~ $ auric set --manual</p>
            <p className="tx-muted">Type a value. Examples: 5m, 1h, 90s, 00:05:00, off.</p>
            <input
              className="tx-term-input"
              autoFocus
              value={draft}
              aria-label="Manual entry"
              onChange={(e) => setDraft(e.target.value)}
              onKeyDown={(e) => {
                if (e.key === "Enter") apply();
                if (e.key === "Escape") setOpen(false);
              }}
            />
            {err ? <p className="tx-warn">{err}</p> : null}
            <div className="tx-action-row" style={{ marginTop: 12 }}>
              <button type="button" className="tx-btn tx-btn-open" onClick={apply}>
                APPLY
              </button>
              <button type="button" className="tx-btn tx-btn-min" onClick={() => setOpen(false)}>
                close
              </button>
            </div>
          </div>
        </div>
      ) : null}
    </>
  );
}
