import { useState } from "react";
import { nextAlarmDate } from "@/lib/alarm-time";
import { AcrylicReadout } from "@/components/auric/time-pad";
import { DrumReel } from "@/components/auric/drum-reel";
import { playLockIn, playScrollUnfurl, reelFeel } from "@/lib/audio";
import { pad2 } from "@/lib/format";
import { ensureNotifyPermission } from "@/lib/notify";
import { useAuric } from "@/lib/store";
import { cn } from "@/lib/utils";

function to24(h12: number, pm: boolean): number {
  if (h12 === 0) return pm ? 12 : 0;
  return pm ? h12 + 12 : h12;
}

function from24(h: number): { h12: number; pm: boolean } {
  return { h12: h % 12, pm: h >= 12 };
}

function prettyTime(hours: number, minutes: number, seconds: number) {
  const { h12, pm } = from24(hours);
  const label = h12 === 0 ? 12 : h12;
  return `${label}:${pad2(minutes)}:${pad2(seconds)} ${pm ? "PM" : "AM"}`;
}

export function AlarmView() {
  const alarms = useAuric((s) => s.alarms);
  const currentTime = useAuric((s) => Math.floor(s.now / 1000));
  const now = new Date(currentTime * 1000);
  const start = from24(now.getHours());
  const [h12, setH12] = useState(start.h12 === 0 ? 12 : start.h12);
  const [minutes, setMinutes] = useState(now.getMinutes());
  const [seconds, setSeconds] = useState(0);
  const [pm, setPm] = useState(start.pm);
  const [when, setWhen] = useState<"today" | "tomorrow">("today");
  const [label, setLabel] = useState("");
  const [noteOpen, setNoteOpen] = useState(false);
  const [draft, setDraft] = useState("");

  const hour24 = to24(h12 === 12 ? 0 : h12, pm);
  const target = nextAlarmDate(hour24, minutes, when === "tomorrow", now.getTime());

  function openNote() {
    setDraft(label);
    setNoteOpen(true);
    playScrollUnfurl();
  }

  function saveNote() {
    setLabel(draft);
    setNoteOpen(false);
  }

  function closeNote() {
    setNoteOpen(false);
  }

  return (
    <div className="tx-tool">
      <AcrylicReadout value={prettyTime(hour24, minutes, seconds)} />
      <p className="tx-muted">Spin a clock time, including seconds. Rings while Auric is open.</p>
      <div className="reel-console-wrap w-full max-w-full">
        <div className="reel-console">
          <div className="reel-console-sheen" aria-hidden />
          <div className="reel-bank">
            <DrumReel
              size="trio"
              count={12}
              value={h12 % 12}
              labels={["12", "01", "02", "03", "04", "05", "06", "07", "08", "09", "10", "11"]}
              onChange={(n) => setH12(n === 0 ? 12 : n)}
              onTick={() => reelFeel()}
              label="Hours"
              className="min-w-0 flex-1"
            />
            <span className="reel-pips" aria-hidden>
              <span />
              <span />
            </span>
            <DrumReel
              size="trio"
              count={60}
              value={minutes}
              onChange={setMinutes}
              onTick={() => reelFeel()}
              label="Min"
              className="min-w-0 flex-1"
            />
            <span className="reel-pips" aria-hidden>
              <span />
              <span />
            </span>
            <DrumReel
              size="trio"
              count={60}
              value={seconds}
              onChange={setSeconds}
              onTick={() => reelFeel()}
              label="Sec"
              className="min-w-0 flex-1"
            />
          </div>
        </div>
      </div>
      <div className="tx-action-row">
        {(["AM", "PM"] as const).map((m) => (
          <button
            key={m}
            type="button"
            className={cn("tx-btn", (m === "PM") === pm ? "tx-btn-open" : "tx-btn-min")}
            onClick={() => setPm(m === "PM")}
          >
            {m}
          </button>
        ))}
      </div>
      <div className="tx-action-row">
        {(["today", "tomorrow"] as const).map((w) => (
          <button
            key={w}
            type="button"
            className={cn("tx-btn", when === w ? "tx-btn-open" : "tx-btn-min")}
            onClick={() => setWhen(w)}
          >
            {w}
          </button>
        ))}
      </div>
      <p className="tx-ok">rings {target.toLocaleDateString(undefined, { weekday: "short", month: "short", day: "numeric" })}</p>
      <button type="button" className="tx-note-open" onClick={openNote}>
        {label.trim() || "What is this alarm for?"}
      </button>
      {noteOpen ? (
        <div className="tx-unfurl-layer" role="dialog" aria-modal="true" aria-labelledby="tx-alarm-note-title">
          <div className="tx-panel is-solid tx-unfurl">
            <p className="tx-prompt">~ $ auric note --alarm</p>
            <h2 id="tx-alarm-note-title" className="tx-heading">
              What is this alarm for?
            </h2>
            <textarea
              className="tx-unfurl-note"
              autoFocus
              value={draft}
              placeholder="What is this alarm for?"
              onChange={(e) => setDraft(e.target.value)}
              onKeyDown={(e) => {
                if (e.key === "Escape") closeNote();
              }}
            />
            <div className="tx-action-row">
              <button type="button" className="tx-btn tx-btn-open" onClick={saveNote}>
                SAVE
              </button>
              <button type="button" className="tx-btn tx-btn-min" onClick={closeNote}>
                close
              </button>
            </div>
          </div>
        </div>
      ) : null}
      <button
        type="button"
        className="tx-btn tx-btn-open"
        onClick={() => {
          void ensureNotifyPermission();
          const at = nextAlarmDate(hour24, minutes, when === "tomorrow");
          useAuric.getState().addAlarm({
            year: at.getFullYear(),
            month: at.getMonth() + 1,
            day: at.getDate(),
            hours: hour24,
            minutes,
            seconds,
            label,
          });
          setLabel("");
          playLockIn();
        }}
      >
        SET ALARM
      </button>
      <ul className="tx-laps">
        {alarms.length === 0 ? (
          <li className="tx-muted">No alarms yet.</li>
        ) : (
          alarms.map((a) => (
            <li key={a.id} className="tx-core-line">
              <button type="button" className="tx-core-name" onClick={() => useAuric.getState().toggleAlarm(a.id)}>
                {prettyTime(a.hours, a.minutes, a.seconds ?? 0)}
                <span className="tx-core-hint">
                  {" "}
                  {a.label ? `${a.label} · ` : ""}
                  {a.enabled ? "armed" : "held"}
                </span>
              </button>
              <button type="button" className="tx-muted" aria-label="Delete alarm" onClick={() => useAuric.getState().deleteAlarm(a.id)}>
                delete
              </button>
            </li>
          ))
        )}
      </ul>
    </div>
  );
}
