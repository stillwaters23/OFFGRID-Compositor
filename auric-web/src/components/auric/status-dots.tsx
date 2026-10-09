import { playClick } from "@/lib/audio";
import { useAuric } from "@/lib/store";
import { cn } from "@/lib/utils";

export function StatusDots() {
  const timerStatus = useAuric((s) => s.timerStatus);
  const swStatus = useAuric((s) => s.swStatus);
  const alarms = useAuric((s) => s.alarms);
  const houseOpen = useAuric((s) => s.houseOpen);
  const noticeOpen = useAuric((s) => s.noticeOpen);
  const ring = useAuric((s) => s.ringingAlarm);
  const beatOpen = useAuric((s) => s.heartbeatBeatOpen);
  const crown = useAuric((s) => s.crownSheetOpen);
  const armed = timerStatus === "running" || swStatus === "running" || alarms.some((a) => a.enabled);
  const sheet = houseOpen || noticeOpen || !!ring || beatOpen || crown;

  return (
    <div className="mt-1.5 flex items-center justify-center gap-2.5" aria-label="Status">
      <Dot tone="live" pulse title="Live clock" onClick={() => undefined} />
      <Dot tone="held" pulse={armed} title={armed ? "Armed" : "Idle"} onClick={() => undefined} />
      <Dot
        tone="close"
        pulse={sheet}
        title="Close sheet"
        onClick={() => {
          playClick();
          const s = useAuric.getState();
          if (s.ringingAlarm) s.dismissRinging();
          if (s.heartbeatBeatOpen) s.setHeartbeatBeatOpen(false);
          if (s.crownSheetOpen) s.setCrownSheet(false);
          if (s.noticeOpen) s.setNoticeOpen(false);
          if (s.houseOpen) s.setHouseOpen(false);
        }}
      />
    </div>
  );
}

function Dot({
  tone,
  pulse,
  title,
  onClick,
}: {
  tone: "live" | "held" | "close";
  pulse: boolean;
  title: string;
  onClick: () => void;
}) {
  const color =
    tone === "live"
      ? "color-mix(in oklab, #4dff9a 58%, var(--accent))"
      : tone === "held"
        ? "color-mix(in oklab, #ffd24a 62%, var(--glow))"
        : "color-mix(in oklab, #ff3b3b 55%, var(--danger))";
  return (
    <button
      type="button"
      title={title}
      aria-label={title}
      onClick={onClick}
      className={cn("status-orb", pulse && "status-orb-on")}
      style={{ background: color, boxShadow: `0 0 10px ${color}` }}
    />
  );
}
