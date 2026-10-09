import { Settings } from "lucide-react";
import { NoticeBell } from "@/components/auric/notice-bell";
import { playSettingsClick } from "@/lib/audio";
import { clockStamp, deviceTimeZone } from "@/lib/clock";
import { copy } from "@/lib/literacy";
import { useAuric } from "@/lib/store";

export function InstrumentHeader() {
  const houseOpen = useAuric((s) => s.houseOpen);
  const literacy = useAuric((s) => s.literacyIndex);
  const now = useAuric((s) => s.now);
  const hourCycle = useAuric((s) => s.hourCycle);
  const zone = useAuric((s) => (s.timeZoneLocked ? s.timeZoneId : null));
  const words = copy(literacy);
  const stamp = clockStamp(now || Date.now(), hourCycle, zone || deviceTimeZone());

  return (
    <header className="relative z-10 min-w-0 w-full px-3 pb-1 pt-1">
      <div className="grid grid-cols-[minmax(5.5rem,1fr)_auto_minmax(5.5rem,1fr)] items-center gap-1">
        <span aria-hidden />
        <p className="auric-wordmark text-center">Auric Time Crowned</p>
        <div className="flex min-h-12 items-center justify-end gap-0.5">
          {houseOpen ? null : (
            <>
              <NoticeBell />
              <button
                type="button"
                className="flex min-h-12 min-w-11 shrink-0 flex-col items-center justify-center text-[11px] tracking-wide text-accent"
                onClick={() => {
                  playSettingsClick();
                  useAuric.getState().setHouseOpen(true);
                }}
                aria-label="Open Settings"
              >
                <Settings className="size-5" />
                {words.house}
              </button>
            </>
          )}
        </div>
      </div>
      <div className="panel-glass rounded-3xl px-3 py-4 text-center">
        <p
          className="font-digits text-[2.35rem] leading-none tracking-wide text-digit digit-cut sm:text-[2.7rem]"
          style={{ fontVariantNumeric: "tabular-nums" }}
          suppressHydrationWarning
        >
          {stamp.time}
        </p>
        <p className="mt-2 text-sm tracking-wide text-accent" suppressHydrationWarning>
          {stamp.weekday}, {stamp.date}
        </p>
        <p className="mt-1 text-[11px] tracking-[0.14em] uppercase text-muted" suppressHydrationWarning>
          {stamp.zone}
        </p>
      </div>
    </header>
  );
}