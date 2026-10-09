import { useState } from "react";
import { ArrowLeft, Check, Folder, Plus, Trash2 } from "lucide-react";
import { DrumReel } from "@/components/auric/drum-reel";
import { PitButton } from "@/components/auric/pit-button";
import { SpanReels } from "@/components/auric/span-reels";
import { OrbToggle } from "@/components/auric/switch";
import { playClick, playLockIn } from "@/lib/audio";
import { hapticTick } from "@/lib/haptics";
import { cutScore, markColor } from "@/lib/mark";
import { computeStreak } from "@/lib/achievements";
import { formatSpanClock, msFromSpan, spanFromMs } from "@/lib/format";
import { reminderSignal } from "@/lib/instrument-signal";
import { useAuric } from "@/lib/store";
import type { BeatLogEntry } from "@/lib/types";
import { cn } from "@/lib/utils";

const ACTIONS = ["Keep", "Archive", "Delete"] as const;
const DAY = 86_400_000;

function remainLabel(at: number, now: number) {
  const left = Math.max(0, DAY - (now - at));
  const h = Math.floor(left / 3_600_000);
  const m = Math.floor((left % 3_600_000) / 60_000);
  if (left <= 0) return "Archiving";
  return `${h}h ${m}m to archive`;
}

export function RemindView() {
  const tasks = useAuric((s) => s.tasks);
  const sessions = useAuric((s) => s.sessions);
  const insurance = useAuric((s) => s.streakInsuranceUsed);
  const beatOn = useAuric((s) => s.heartbeatEnabled);
  const beatMs = useAuric((s) => s.heartbeatMs);
  const reminder = useAuric((s) => s.heartbeatReminder);
  const lastBeatAt = useAuric((s) => s.lastBeatAt);
  const beatLog = useAuric((s) => s.heartbeatLog);
  const delivered = useAuric((s) => s.heartbeatBeatOpen);
  const detailId = useAuric((s) => s.taskDetailId);
  const now = useAuric((s) => s.now) || Date.now();
  const [draft, setDraft] = useState("");
  const { streak } = computeStreak(sessions, !insurance);
  const score = cutScore(sessions, streak);
  const color = markColor(score);
  const beat = spanFromMs(beatMs);
  const signal = reminderSignal(beatOn, delivered);
  const detail = tasks.find((t) => t.id === detailId);

  if (detail) {
    return <TaskDetail taskId={detail.id} text={detail.text} />;
  }

  return (
    <div className="flex min-h-0 w-full max-w-full flex-1 flex-col overflow-y-auto px-5 pb-10" style={{ overflowAnchor: "none" }}>
      <div className="heartbeat-pill mx-auto mt-3 flex min-h-12 items-center gap-3 rounded-full px-4">
        <span className={cn("auric-status-dot", signal.pulse && "instrument-lens-pulse")} data-tone={signal.tone} aria-hidden>
          {signal.tone === "green" ? "✓" : signal.pulse ? "▶" : "–"}
        </span>
        <div>
          <p className="font-display text-sm">{signal.label}</p>
          <p className="text-[11px] text-muted">{beatOn ? "Heartbeat is scheduled in this app session." : "Heartbeat is off."}</p>
        </div>
      </div>

      <div className="mt-4 flex flex-col items-center">
        <div
          className="relative grid size-36 place-items-center rounded-full"
          style={{
            boxShadow: `0 0 0 3px ${color}, 0 0 28px ${color}`,
            background: `conic-gradient(${color} ${score * 3.6}deg, color-mix(in oklab, var(--chrome) 80%, transparent) 0)`,
          }}
        >
          <div className="grid size-[7.2rem] place-items-center rounded-full bg-felt">
            <p className="font-digits text-4xl digit-cut text-digit">
              {score}
            </p>
            <p className="text-[10px] tracking-[0.28em] uppercase text-muted">Cut</p>
          </div>
        </div>
        <p className="mt-3 text-center text-xs text-muted">How the time was spent. White is the full prism.</p>
      </div>

      <p className="mt-6 font-display text-[10px] tracking-[0.32em] uppercase text-accent">Task list</p>
      <div className="mt-2 flex gap-2">
        <input
          value={draft}
          onChange={(e) => setDraft(e.target.value)}
          onKeyDown={(e) => {
            if (e.key === "Enter") {
              useAuric.getState().addTask(draft);
              setDraft("");
              playLockIn();
            }
          }}
          placeholder="What needs the time"
          className="h-11 min-w-0 flex-1 rounded-md bg-chrome/70 px-3 text-sm outline-none shadow-[0_0_0_1px_var(--line)]"
        />
        <PitButton
          className="h-11 px-3" aria-label="Add task"
          onClick={() => {
            useAuric.getState().addTask(draft);
            setDraft("");
            playLockIn();
          }}
        >
          <Plus className="size-4" />
        </PitButton>
      </div>
      <ul className="mt-3 space-y-1.5">
        {tasks.length === 0 ? (
          <li className="rounded-lg px-3 py-3 text-sm text-muted shadow-[0_0_0_1px_var(--line)]">
            No tasks. The House keeps them here.
          </li>
        ) : (
          tasks.map((t) => (
            <li key={t.id} className="flex min-h-12 items-center gap-2 rounded-lg px-2 shadow-[0_0_0_1px_var(--line)]">
              <button
                type="button"
                className={cn(
                  "grid size-11 shrink-0 place-items-center rounded-full",
                  t.done ? "bg-accent text-on-accent" : "shadow-[0_0_0_1px_var(--line)]",
                )}
                onClick={() => useAuric.getState().toggleTask(t.id)}
                aria-label={t.done ? "Undo" : "Complete"}
              >
                <Check className="size-4" />
              </button>
              <button type="button" className={cn("min-w-0 flex-1 break-words text-left text-sm", t.done && "text-muted line-through")} onClick={() => useAuric.getState().setTaskDetail(t.id)}>
                {t.text}
              </button>
              <button type="button" aria-label="Delete task" className="grid size-11 shrink-0 place-items-center" onClick={() => useAuric.getState().deleteTask(t.id)}>
                <Trash2 className="size-4 text-muted" />
              </button>
            </li>
          ))
        )}
      </ul>

      <p className="mt-7 font-display text-[10px] tracking-[0.32em] uppercase text-accent">Heartbeat</p>
      <p className="mt-1 text-xs text-muted">Set the gap. Start. It counts down, then logs the beat with live time.</p>
      <div className="mt-3">
        <SpanReels
          value={beat}
          onChange={(key, n) => {
            const next = { ...beat, [key]: n };
            useAuric.getState().setHeartbeatMs(Math.max(10, msFromSpan(next)));
          }}
          onTick={() => {
            hapticTick();
            playClick();
          }}
        />
      </div>
      <p className="mt-3 time-readout text-center font-digits text-3xl digit-cut">
        {beatOn ? formatSpanClock(Math.max(0, beatMs - (now - lastBeatAt))) : formatSpanClock(beatMs)}
      </p>
      <p className="mt-1 text-center text-xs text-accent">
        {beatOn ? "Next beat" : beatMs < 100 ? "Set at least 00:00:00.10 then Start" : "Armed — press Start"}
      </p>
      <div className="mt-3 flex min-w-0 items-center justify-center gap-3">
        <OrbToggle
          on={beatOn}
          size="lg"
          label="Heartbeat"
          onChange={(v) => {
            if (v && beatMs < 100) return;
            if (v) playLockIn();
            else playClick();
            useAuric.getState().setHeartbeatEnabled(v);
          }}
        />
        <p className="text-sm text-digit">{beatOn ? "On" : "Off"}</p>
      </div>
      <textarea
        value={reminder}
        onChange={(e) => useAuric.getState().setHeartbeatReminder(e.target.value)}
        placeholder="What should the beat say? Keep track of this…"
        rows={4}
        className="tx-note-field mt-3"
      />
      <p className="mt-4 font-display text-[10px] tracking-[0.32em] uppercase text-accent">Beat log</p>
      <BeatLogFolder log={beatLog} now={now} />
    </div>
  );
}

function BeatLogFolder({ log, now }: { log: BeatLogEntry[]; now: number }) {
  const [open, setOpen] = useState(false);
  const [picked, setPicked] = useState<string | null>(null);
  const [folderAct, setFolderAct] = useState(false);
  const [act, setAct] = useState(0);
  const live = log.filter((b) => !b.archived);
  const latest = log.reduce((m, b) => Math.max(m, b.at), 0);
  const focus = log.find((b) => b.id === picked);

  function back() {
    playClick();
    if (picked) {
      setPicked(null);
      return;
    }
    if (folderAct) {
      setFolderAct(false);
      return;
    }
    setOpen(false);
  }

  if (!open) {
    return (
      <button
        type="button"
        className="ice-card mt-2 flex w-full items-center gap-3 rounded-lg px-3 py-3 text-left"
        onClick={() => {
          playClick();
          setOpen(true);
        }}
      >
        <Folder className="size-5 shrink-0 text-accent" />
        <span className="min-w-0 flex-1">
          <span className="flex items-baseline justify-between gap-2">
            <span className="font-display text-sm text-digit">Beat log</span>
            {live.length ? (
              <span className="font-digits text-[11px] text-accent">{live.length}</span>
            ) : null}
          </span>
          <span className="block text-[11px] text-muted">
            {latest
              ? new Date(latest).toLocaleString(undefined, { month: "short", day: "numeric", hour: "numeric", minute: "2-digit" })
              : "Empty"}
          </span>
        </span>
      </button>
    );
  }

  return (
    <div className="mt-2 rounded-xl px-1 pb-2" style={{ boxShadow: "0 0 0 1px color-mix(in oklab, var(--accent) 40%, transparent)" }}>
      <button type="button" className="flex min-h-12 items-center gap-1 px-2 text-accent" onClick={back} aria-label="Back">
        <ArrowLeft className="size-5" />
        <span className="font-display text-xs tracking-wide">Back</span>
      </button>

      {focus ? (
        <BeatWheel
          title="Beat"
          body={focus.body}
          at={focus.at}
          now={now}
          act={act}
          setAct={setAct}
          onConfirm={() => {
            playClick();
            if (act === 1) useAuric.getState().archiveBeatLog(focus.id);
            if (act === 2) useAuric.getState().deleteBeatLog(focus.id);
            setPicked(null);
            setAct(0);
          }}
        />
      ) : folderAct ? (
        <BeatWheel
          title="Beat log"
          body="Keep, archive, or clear the beats in this folder."
          at={now}
          now={now}
          hideBar
          act={act}
          setAct={setAct}
          onConfirm={() => {
            playClick();
            if (act === 1) useAuric.getState().archiveAllBeatLog();
            if (act === 2) useAuric.getState().deleteAllBeatLog();
            setFolderAct(false);
            setAct(0);
          }}
        />
      ) : (
        <>
          <button
            type="button"
            className="mb-3 w-full rounded-lg px-3 py-2 text-center text-xs text-accent shadow-[0_0_0_1px_var(--line)]"
            onClick={() => {
              playClick();
              setAct(0);
              setFolderAct(true);
            }}
          >
            Folder wheel — Keep / Archive / Delete
          </button>
          <ul className="space-y-2 px-1 pb-3">
            {live.length === 0 ? (
              <li className="rounded-lg px-3 py-4 text-center text-sm text-muted shadow-[0_0_0_1px_var(--line)]">
                Quiet in this folder.
              </li>
            ) : (
              live.map((b) => (
                <li key={b.id}>
                  <button
                    type="button"
                    className="w-full rounded-lg px-3 py-3 text-left shadow-[0_0_0_1px_var(--line)]"
                    onClick={() => {
                      playClick();
                      setPicked(b.id);
                      setAct(0);
                    }}
                  >
                    <div className="flex items-baseline justify-between gap-2">
                      <p className="text-[11px] text-accent">
                        {new Date(b.at).toLocaleString(undefined, {
                          weekday: "short",
                          month: "short",
                          day: "numeric",
                          year: "numeric",
                          hour: "numeric",
                          minute: "2-digit",
                          second: "2-digit",
                        })}
                      </p>
                      <p className="shrink-0 text-[10px] text-muted">{remainLabel(b.at, now)}</p>
                    </div>
                    <p className="mt-0.5 text-sm text-digit">{b.body}</p>
                    <div className="mt-2 h-1 overflow-hidden rounded-full bg-black/30">
                      <div
                        className="h-full bg-accent"
                        style={{ width: `${Math.max(0, Math.min(100, ((DAY - (now - b.at)) / DAY) * 100))}%` }}
                      />
                    </div>
                  </button>
                </li>
              ))
            )}
          </ul>
        </>
      )}
    </div>
  );
}

function BeatWheel({
  title,
  body,
  at,
  now,
  act,
  setAct,
  onConfirm,
  hideBar,
}: {
  title: string;
  body: string;
  at: number;
  now: number;
  act: number;
  setAct: (n: number) => void;
  onConfirm: () => void;
  hideBar?: boolean;
}) {
  return (
    <div className="px-2 pb-4">
      <p className="font-display text-base text-accent">{title}</p>
      <p className="mt-1 text-sm text-digit/85">{body}</p>
      {!hideBar ? (
        <>
          <p className="mt-2 text-xs text-muted">{remainLabel(at, now)}</p>
          <div className="mt-2 h-1 overflow-hidden rounded-full bg-black/30">
            <div className="h-full bg-accent" style={{ width: `${Math.max(0, Math.min(100, ((DAY - (now - at)) / DAY) * 100))}%` }} />
          </div>
        </>
      ) : null}
      <div className="mt-5 flex justify-center">
        <DrumReel size="plaque" count={3} value={act} labels={[...ACTIONS]} pad={0} onChange={setAct} onTick={() => hapticTick()} />
      </div>
      <p className="mt-3 text-center font-display text-xl text-accent">{ACTIONS[act]}</p>
      <button type="button" className="mt-4 flex min-h-12 w-full items-center justify-center rounded-lg bg-accent text-on-accent" onClick={onConfirm}>
        Confirm
      </button>
    </div>
  );
}

function TaskDetail({ taskId, text }: { taskId: string; text: string }) {
  const [value, setValue] = useState(text);
  return (
    <div className="flex min-h-0 flex-1 flex-col overflow-y-auto px-5 pb-16">
      <button type="button" className="flex min-h-11 items-center gap-1 text-accent" onClick={() => useAuric.getState().setTaskDetail(null)}>
        <ArrowLeft className="size-5" /> Reminders
      </button>
      <p className="mt-3 font-display text-xl text-accent">Task</p>
      <p className="text-sm text-muted">Save updates the wording. It does not mark the task complete.</p>
      <textarea
        value={value}
        onChange={(e) => setValue(e.target.value)}
        rows={5}
        className="tx-note-field mt-3 min-h-32"
      />
      <div className="mt-4 flex gap-2">
        <PitButton className="min-h-11 flex-1" onClick={() => { useAuric.getState().updateTask(taskId, value); useAuric.getState().setTaskDetail(null); }}>
          Save
        </PitButton>
        <PitButton variant="ghost" className="min-h-11 flex-1" onClick={() => useAuric.getState().setTaskDetail(null)}>
          Dismiss
        </PitButton>
      </div>
    </div>
  );
}
