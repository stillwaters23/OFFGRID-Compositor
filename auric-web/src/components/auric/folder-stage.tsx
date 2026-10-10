import { AlarmView } from "@/components/auric/alarm-view";
import { AppearanceStudio } from "@/components/auric/appearance-studio";
import { ClickerSoundsFolder } from "@/components/auric/clicker_sounds_folder";
import { DeviceRoom } from "@/components/auric/device-room";
import { LogsExports } from "@/components/auric/logs-exports";
import { SpanReels } from "@/components/auric/span-reels";
import { GalaxyField } from "@/components/auric/time-pad";
import { StopwatchView } from "@/components/auric/stopwatch-view";
import { TimerView } from "@/components/auric/timer-view";
import { moduleById, type ModuleId } from "@/lib/modules";
import { boundedPercent } from "@/lib/appearance";
import { TICK_FAMILIES, TICKS } from "@/lib/ticks";
import { WAKE_FAMILIES, WAKES } from "@/lib/wake";
import { reelFeel } from "@/lib/audio";
import { bracketDuration, msFromSpan, parseDuration, spanFromMs } from "@/lib/format";
import { useAuric } from "@/lib/store";
import { cn } from "@/lib/utils";

export function FolderStage({ id, preview }: { id: ModuleId; preview?: boolean }) {
  const spec = moduleById(id);
  if (!spec) return null;
  if (preview) {
    return (
      <div className="tx-folder-stage is-preview">
        {id === "icons" ? <img src="/brand/auric-letter.png" alt="Auric — Time, Crowned" className="tx-logo-lockup" /> : null}
        <p className="tx-muted">This package lights up after install. Preview only.</p>
      </div>
    );
  }
  return (
    <div className="tx-folder-stage">
      <p className="tx-folder-kicker">{spec.pkg}</p>
      <h2 className="tx-heading">{spec.name}</h2>
      <p className="tx-muted">{spec.hint}</p>
      <div className="tx-folder-body">{interior(id)}</div>
    </div>
  );
}

function interior(id: ModuleId) {
  switch (id) {
    case "timer":
      return <TimerView />;
    case "stopwatch":
      return <StopwatchView />;
    case "alarm":
      return <AlarmView />;
    case "themes":
      return <AppearanceStudio />;
    case "sizing":
      return <SizingList />;
    case "opacity":
      return <OpacitySlider />;
    case "icons":
      return <IconsBody />;
    case "alarm-sounds":
      return <SoundList kind="alarm" />;
    case "timer-sounds":
      return <SoundList kind="timer" />;
    case "clicker-sounds":
      return <ClickerSoundsFolder />;
    case "haptics":
      return <HapticsList />;
    case "heartbeat":
      return <HeartbeatDesk />;
    case "heartbeat-log":
    case "logs-exports":
      return <LogsExports />;
    case "device":
      return <DeviceRoom />;
    case "tutorial":
      return <TutorialBody />;
  }
}

function PickList({
  items,
  value,
  onPick,
}: {
  items: { id: string; label: string }[];
  value: string;
  onPick: (id: string) => void;
}) {
  return (
    <ul className="tx-picks">
      {items.map((item) => (
        <li key={item.id}>
          <button
            type="button"
            className={cn("tx-core-line", item.id === value && "is-active")}
            onClick={() => onPick(item.id)}
          >
            <span>{item.label}</span>
            <span className="tx-bracket">{item.id === value ? "[✓]" : "[ ]"}</span>
          </button>
        </li>
      ))}
    </ul>
  );
}

const SIZE_STEPS = [20, 30, 40, 50, 60, 70, 80, 90, 100];

function SizingList() {
  const scale = boundedPercent(useAuric((s) => s.digitScale));
  return (
    <div>
      <p className="tx-muted">Digit scale {scale}. The face resizes when you pick a size.</p>
      <PickList
        items={SIZE_STEPS.map((n) => ({ id: String(n), label: String(n) }))}
        value={String(SIZE_STEPS.reduce((best, n) => (Math.abs(n - scale) < Math.abs(best - scale) ? n : best), 50))}
        onPick={(id) => useAuric.getState().setDigitScale(Number(id))}
      />
    </div>
  );
}

function OpacitySlider() {
  const glass = boundedPercent(useAuric((s) => s.houseOpacity));
  return (
    <div>
      <p className="tx-muted">Stationary glass. 0 is clear, 100 is solid. Essential text stays readable.</p>
      <label className="tx-glass-slider">
        <span className="tx-glass-read">{glass}</span>
        <input
          type="range"
          min={0}
          max={100}
          value={glass}
          aria-label="Opacity"
          onChange={(e) => useAuric.getState().setHouseOpacity(Number(e.target.value))}
        />
      </label>
    </div>
  );
}

const ALARM_PICKS = WAKE_FAMILIES.map((family) => WAKES.find((w) => w.family === family)!);
const TICK_PICKS = TICK_FAMILIES.map((family) => TICKS.find((t) => t.family === family)!);

function SoundList({ kind }: { kind: "alarm" | "timer" }) {
  const alarm = useAuric((s) => s.alarmSound);
  const tick = useAuric((s) => s.reelSound);
  const rows = kind === "alarm" ? ALARM_PICKS : TICK_PICKS;
  const current = kind === "alarm" ? alarm : tick;
  const selected = rows.find((row) => row.id === current)?.id ?? rows[0]!.id;
  return (
    <PickList
      items={rows.map((row) => ({ id: row.id, label: row.family }))}
      value={selected}
      onPick={(id) => {
        if (kind === "alarm") useAuric.getState().setAlarmSound(id);
        else useAuric.getState().setReelSound(id);
      }}
    />
  );
}

function HapticsList() {
  const on = useAuric((s) => s.hapticEnabled !== false);
  return (
    <PickList
      items={[
        { id: "off", label: "Off" },
        { id: "on", label: "Tick" },
      ]}
      value={on ? "on" : "off"}
      onPick={(id) => useAuric.getState().setHapticEnabled(id !== "off")}
    />
  );
}

function IconsBody() {
  return (
    <div className="tx-brand-mark">
      <p className="tx-muted">House mark. The tile is never the only label.</p>
      <img src="/brand/auric-letter.png" alt="Auric — Time, Crowned" className="tx-logo-lockup" />
    </div>
  );
}

function HeartbeatDesk() {
  const ms = useAuric((s) => s.heartbeatMs);
  const on = useAuric((s) => s.heartbeatEnabled);
  const reminder = useAuric((s) => s.heartbeatReminder);
  const beat = spanFromMs(ms);
  return (
    <div className="tx-tool">
      <p className="tx-ok">{on ? `next pulse ${bracketDuration(ms)}` : `armed ${bracketDuration(ms)}`}</p>
      <SpanReels
        value={beat}
        readout={bracketDuration(ms)}
        onChange={(key, n) => {
          const next = msFromSpan({ ...beat, [key]: n });
          useAuric.getState().setHeartbeatMs(next);
          useAuric.getState().setHeartbeatEnabled(next >= 100);
        }}
        onTick={() => reelFeel()}
      />
      <GalaxyField
        label="tap to type an interval"
        display={bracketDuration(ms)}
        onSubmit={(raw) => {
          const parsed = parseDuration(raw);
          if (parsed == null) return "Could not read that interval.";
          useAuric.getState().setHeartbeatMs(parsed);
          useAuric.getState().setHeartbeatEnabled(parsed >= 100);
        }}
      />
      <label className="tx-galaxy" style={{ marginTop: 8 }}>
        <span className="tx-galaxy-kicker">reminder</span>
        <input
          className="tx-term-input"
          value={reminder}
          placeholder="Keep track. Log what you are doing."
          onChange={(e) => useAuric.getState().setHeartbeatReminder(e.target.value)}
        />
      </label>
      <div className="tx-action-row" style={{ marginTop: 12 }}>
        <button
          type="button"
          className={cn("tx-btn", on ? "tx-btn-min" : "tx-btn-open")}
          onClick={() => useAuric.getState().setHeartbeatEnabled(!on && ms >= 100)}
        >
          {on ? "HOLD" : "START"}
        </button>
        <button type="button" className="tx-btn tx-btn-close" onClick={() => useAuric.getState().setHeartbeatEnabled(false)}>
          OFF
        </button>
      </div>
    </div>
  );
}

function TutorialBody() {
  const mode = useAuric((s) => s.appearanceMode);
  return (
    <div className="tx-tutorial">
      <p>1. OPEN SESSION starts the desk.</p>
      <p>2. Timer, stopwatch, and alarm are folders.</p>
      <p>3. Unlocked folders sit slightly open, green, with a glow. Locked folders sit closed, red.</p>
      <p>4. CLOSE SESSION ends the session. MINIMIZE keeps time running as a pill.</p>
      <p className="tx-muted">System follows the device. Light is paper and ink. Dark is the OLED terminal. Green is the prompt and the open edge. Red is CLOSE SESSION and locked folders.</p>
      <div className="tx-mood-row">
        {(["system", "dark", "light"] as const).map((id) => (
          <button
            key={id}
            type="button"
            className={cn("tx-btn", mode === id ? "tx-btn-open" : "tx-btn-min")}
            onClick={() => useAuric.getState().setAppearanceMode(id)}
          >
            {id}
          </button>
        ))}
      </div>
      <p className="tx-muted" style={{ marginTop: 16 }}>
        Native lock-screen alarms are not this web demo. Kai owns the APK.
      </p>
    </div>
  );
}
