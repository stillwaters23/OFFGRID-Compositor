import { cn } from "@/lib/utils";
import type { InstrumentSignal } from "@/lib/instrument-signal";

export function InstrumentLens({ signal }: { signal: InstrumentSignal }) {
  const on = signal.tone === "green";
  return (
    <span className="flex flex-col items-center gap-1">
      <span
        className={cn("auric-status-dot", signal.pulse && "instrument-lens-pulse")}
        data-tone={signal.tone}
        aria-hidden
      >
        {on ? "✓" : signal.pulse ? "▶" : "–"}
      </span>
      <span className="text-[10px] tracking-wide uppercase text-muted">{signal.label}</span>
    </span>
  );
}
