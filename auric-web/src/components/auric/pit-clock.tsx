import { PulseOrb } from "@/components/auric/switch";
import { pitStamp } from "@/lib/format";
import { useAuric } from "@/lib/store";

export function PitClock() {
  const second = useAuric((s) => Math.floor(s.now / 1000));
  const now = second * 1000 || Date.now();
  const orbHex = useAuric((s) => s.orbHex);
  const stamp = pitStamp(now);
  return (
    <div className="pit-clock relative z-10 min-w-0 px-3 pb-1 pt-0.5 text-center" data-slot="glow">
      <p className="font-display text-[13px] leading-tight tracking-[0.04em] text-accent" suppressHydrationWarning>
        {stamp.weekday}, {stamp.longDate}
      </p>
      <p className="digit-cut mt-0.5 font-digits text-[15px] leading-tight tracking-wide text-digit" suppressHydrationWarning>
        {stamp.time}
        <span className="mx-1.5 text-accent">·</span>
        {stamp.numeric}
      </p>
      <p className="mt-0.5 flex items-center justify-center gap-1.5 text-[11px] tracking-[0.12em] uppercase text-muted" suppressHydrationWarning>
        <PulseOrb on color={orbHex || undefined} title="Live clock" />
        {stamp.tzAbbr}
        {stamp.city && stamp.city.toUpperCase() !== stamp.tzAbbr.toUpperCase() ? ` · ${stamp.city}` : ""}
      </p>
    </div>
  );
}
