import { GalaxyField } from "@/components/auric/time-pad";
import { SpanReels } from "@/components/auric/span-reels";
import { CountRail } from "@/components/auric/termux-chrome";
import { playStart, reelFeel } from "@/lib/audio";
import { formatSpanClock, msFromSpan, parseDuration, spanFromMs, type Span } from "@/lib/format";
import { copy } from "@/lib/literacy";
import { remainingMs, useAuric } from "@/lib/store";
import { ensureNotifyPermission } from "@/lib/notify";

const PRESETS = [
  { label: "1m", ms: 60_000 },
  { label: "5m", ms: 300_000 },
  { label: "10m", ms: 600_000 },
  { label: "15m", ms: 900_000 },
  { label: "25m", ms: 1_500_000 },
  { label: "45m", ms: 2_700_000 },
  { label: "1h", ms: 3_600_000 },
  { label: "1d", ms: 86_400_000 },
];

export function TimerView() {
  const years = useAuric((s) => s.years);
  const months = useAuric((s) => s.months);
  const weeks = useAuric((s) => s.weeks);
  const days = useAuric((s) => s.days);
  const hours = useAuric((s) => s.hours);
  const minutes = useAuric((s) => s.minutes);
  const seconds = useAuric((s) => s.seconds);
  const cs = useAuric((s) => s.cs);
  const status = useAuric((s) => s.timerStatus);
  const controlLock = useAuric((s) => s.controlLock);
  const literacy = useAuric((s) => s.literacyIndex);
  const now = useAuric((s) => s.now);
  const store = useAuric();
  const words = copy(literacy);

  const running = status === "running";
  const paused = status === "paused";
  const remain = remainingMs(store, now);
  const idle: Span = { years, months, weeks, days, hours, minutes, seconds, cs };
  const display = running || paused || status === "done" ? remain : msFromSpan(idle);
  const live = running || paused || status === "done" ? spanFromMs(remain) : idle;
  const locked = controlLock || running || paused;
  const total = Math.max(msFromSpan(idle), remain, 1);
  const percent = total > 0 ? (remain / total) * 100 : 0;

  return (
    <div className="tx-tool">
      <SpanReels
        value={live}
        disabled={locked}
        readout={formatSpanClock(display)}
        onChange={(unit, n) => useAuric.getState().setSpanPart(unit, n)}
        onTick={() => reelFeel()}
      />
      {(running || paused) && (
        <CountRail percent={percent} label="countdown" time={formatSpanClock(display)} />
      )}
      <GalaxyField
        label="tap to type a duration"
        display={formatSpanClock(display)}
        onSubmit={(raw) => {
          const ms = parseDuration(raw);
          if (ms == null) return "Could not read that duration.";
          if (locked) return "Timer is running.";
          useAuric.getState().applyPreset(ms);
        }}
      />
      <div className="tx-presets">
        {PRESETS.map((p) => (
          <button
            key={p.label}
            type="button"
            className="tx-chip"
            disabled={locked}
            onClick={() => {
              reelFeel();
              useAuric.getState().applyPreset(p.ms);
            }}
          >
            {p.label}
          </button>
        ))}
      </div>
      <div className="tx-action-row" style={{ marginTop: 12 }}>
        {status === "running" ? (
          <button type="button" className="tx-btn tx-btn-min" disabled={controlLock} onClick={() => useAuric.getState().pauseTimer()}>
            HOLD
          </button>
        ) : status === "paused" ? (
          <button type="button" className="tx-btn tx-btn-open" disabled={controlLock} onClick={() => useAuric.getState().resumeTimer()}>
            RESUME
          </button>
        ) : (
          <button
            type="button"
            className="tx-btn tx-btn-open"
            disabled={msFromSpan(idle) <= 0 || controlLock}
            onClick={() => {
              playStart();
              void ensureNotifyPermission();
              useAuric.getState().startTimer();
            }}
          >
            {words.start.toUpperCase()}
          </button>
        )}
        <button type="button" className="tx-btn tx-btn-min" disabled={controlLock} onClick={() => useAuric.getState().resetTimer()}>
          RESET
        </button>
      </div>
    </div>
  );
}
