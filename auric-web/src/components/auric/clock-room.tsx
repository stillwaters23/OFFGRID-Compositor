import { OrbToggle } from "@/components/auric/switch";
import { useAuric } from "@/lib/store";
import { deviceTimeZone } from "@/lib/clock";
import type { HourCycle } from "@/lib/clock";

export function ClockRoom() {
  const analog = useAuric((s) => s.analogFace);
  const hourCycle = useAuric((s) => s.hourCycle);
  const locked = useAuric((s) => s.timeZoneLocked);
  const zoneId = useAuric((s) => s.timeZoneId);
  const device = deviceTimeZone();

  return (
    <div className="relative flex min-h-0 flex-1 flex-col overflow-y-auto px-5 pb-16">
      <label className="flex min-h-12 items-center justify-between gap-3 text-sm">
        Analog face
        <OrbToggle on={analog} onChange={(on) => useAuric.getState().setAnalogFace(on)} label="Analog face" />
      </label>
      <p className="mt-4 font-display text-sm uppercase tracking-[0.18em] text-accent">Time format</p>
      <div className="mt-2 flex gap-1">
        {([
          ["auto", "Device"],
          ["h12", "12-hour"],
          ["h23", "24-hour"],
        ] as [HourCycle, string][]).map(([id, label]) => (
          <button
            key={id}
            type="button"
            className={`min-h-11 flex-1 rounded-lg text-xs ${hourCycle === id ? "bg-accent text-on-accent" : "shadow-[0_0_0_1px_var(--line)]"}`}
            onClick={() => useAuric.getState().setHourCycle(id)}
          >
            {label}
          </button>
        ))}
      </div>
      <label className="mt-4 flex min-h-12 items-center justify-between gap-3 text-sm">
        Use device time zone
        <OrbToggle on={!locked} onChange={(on) => useAuric.getState().setTimeZoneLocked(!on)} label="Device time zone" />
      </label>
      <p className="mt-2 text-xs text-muted">Device zone: {device.replace(/_/g, " ")}. Travel and DST follow this clock, not an IP lookup.</p>
      {locked ? (
        <label className="mt-3 block text-xs text-muted">
          Zone identifier
          <input
            value={zoneId}
            onChange={(e) => useAuric.getState().setTimeZoneId(e.target.value)}
            placeholder="America/Chicago"
            className="mt-1 h-11 w-full rounded-md bg-felt px-3 text-sm text-digit"
          />
        </label>
      ) : null}
    </div>
  );
}
