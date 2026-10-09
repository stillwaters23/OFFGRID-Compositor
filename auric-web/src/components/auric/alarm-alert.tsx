import * as Dialog from "@radix-ui/react-dialog";
import { Archive, Clock, Save, TriangleAlert, X } from "lucide-react";
import { useEffect, useRef, useState } from "react";
import { PitButton } from "@/components/auric/pit-button";
import { CountRail } from "@/components/auric/termux-chrome";
import { REFLECTION_HEADING, alarmRingingCopy, timerFinishedCopy } from "@/lib/alert-copy";
import { stopAlarm, stopPreview } from "@/lib/audio";
import { hapticStop } from "@/lib/haptics";
import { useAuric } from "@/lib/store";

type Reflection = { sessionId: string | null; at: number; kind: "timer" | "alarm"; note: string };

function silence() {
  stopPreview();
  stopAlarm();
  hapticStop();
}

export function AlarmAlert() {
  const ringing = useAuric((s) => s.ringingAlarm);
  const timerStatus = useAuric((s) => s.timerStatus);
  const pinLocked = useAuric((s) => s.pinLocked);
  const kind = ringing ? "alarm" : timerStatus === "done" ? "timer" : null;
  const [reflections, setReflections] = useState<Reflection[]>([]);
  const [alarmPercent, setAlarmPercent] = useState(100);
  const ringTitle = useRef<HTMLHeadingElement>(null);
  const current = reflections[0];

  useEffect(() => {
    if (!kind) return;
    const pulse = () => {
      if (useAuric.getState().hapticEnabled === false) return;
      if (window.matchMedia?.("(prefers-reduced-motion: reduce)").matches) return;
      try { navigator.vibrate?.([200, 100, 200]); } catch { /* Unsupported hardware. */ }
    };
    pulse();
    const id = window.setInterval(pulse, 1400);
    return () => { window.clearInterval(id); hapticStop(); };
  }, [kind]);

  useEffect(() => {
    if (kind !== "alarm") {
      setAlarmPercent(100);
      return;
    }
    setAlarmPercent(100);
    const started = Date.now();
    const duration = 60_000;
    const id = window.setInterval(() => {
      const remaining = Math.max(0, 100 - ((Date.now() - started) / duration) * 100);
      setAlarmPercent(remaining);
      if (remaining <= 0) window.clearInterval(id);
    }, 250);
    return () => window.clearInterval(id);
  }, [kind, ringing?.id]);

  useEffect(() => {
    if (kind) ringTitle.current?.focus();
  }, [kind]);

  function acknowledge(snooze: boolean) {
    const state = useAuric.getState();
    const session = kind === "timer" ? state.sessions.find((s) => s.id === state.noteDraftId) : undefined;
    const reflection: Reflection = {
      sessionId: session?.id ?? null,
      kind: kind === "alarm" ? "alarm" : "timer",
      at: session?.endedAt ?? Date.now(),
      note: session?.note ?? "",
    };
    silence();
    if (kind === "alarm") {
      if (snooze) state.snoozeRinging();
      else state.dismissRinging();
    } else {
      state.resetTimer();
      state.setNoteDraft(null);
    }
    if (!pinLocked) setReflections((items) => [...items, reflection]);
  }

  function next() {
    setReflections((items) => items.slice(1));
  }

  function save(archived: boolean, note: string) {
    if (!current) return;
    const state = useAuric.getState();
    if (state.pinLocked) return;
    const trimmed = note.trim();
    if (current.sessionId) state.updateSessionNote(current.sessionId, trimmed, archived);
    else if (trimmed) state.addSession({
      kind: current.kind, startedAt: current.at, endedAt: current.at,
      durationMs: 0, note: trimmed, noteArchived: archived,
    });
    next();
  }

  if (kind) return (
    <Dialog.Root open key={`ring:${ringing?.id ?? "timer"}`}>
      <Dialog.Portal>
        <Dialog.Overlay className="fixed inset-0 z-[89] bg-felt" />
        <Dialog.Content
          role="alertdialog"
          onEscapeKeyDown={(e) => e.preventDefault()}
          onInteractOutside={(e) => e.preventDefault()}
          className="fixed inset-0 z-[90] grid place-items-center overflow-y-auto px-5 py-6"
          style={{ background: "var(--felt)" }}
        >
          <div className="panel-glass w-full max-w-md rounded-3xl p-6 text-center">
            <div className="tx-wake" aria-hidden>
              <span className="tx-wake-halo" />
              <TriangleAlert className="tx-wake-sign size-14" />
            </div>
            <Dialog.Title asChild>
              <h2 ref={ringTitle} tabIndex={-1} className="mt-3 font-display text-3xl tracking-wide text-digit outline-none">
                {kind === "alarm" ? "Alarm" : "Timer"}
              </h2>
            </Dialog.Title>
            <Dialog.Description className="mt-2 text-sm text-digit">
              {kind === "alarm" ? alarmRingingCopy(ringing?.label) : timerFinishedCopy()}
            </Dialog.Description>
            <div className="mt-4 text-left">
              {kind === "timer" ? (
                <CountRail percent={0} label="timer finished" time="0%" />
              ) : (
                <CountRail percent={alarmPercent} label="alarm ringing" />
              )}
            </div>
            <div className="mt-5 flex min-w-0 gap-2">
              {kind === "alarm" ? (
                <PitButton variant="ghost" className="min-w-0 flex-1" onClick={() => acknowledge(true)}>
                  <Clock className="size-4" />
                  Snooze 5 min
                </PitButton>
              ) : null}
              <PitButton className="min-w-0 flex-1" onClick={() => acknowledge(false)}>
                <X className="size-4" />
                Dismiss
              </PitButton>
            </div>
          </div>
        </Dialog.Content>
      </Dialog.Portal>
    </Dialog.Root>
  );

  if (!current) return null;

  return (
    <ReflectionSheet
      key={current.sessionId ?? String(current.at)}
      current={current}
      onSave={save}
      onSkip={next}
    />
  );
}

function ReflectionSheet({
  current,
  onSave,
  onSkip,
}: {
  current: Reflection;
  onSave: (archived: boolean, note: string) => void;
  onSkip: () => void;
}) {
  const [note, setNote] = useState(current.note);
  const field = useRef<HTMLTextAreaElement>(null);

  return (
    <Dialog.Root open>
      <Dialog.Portal>
        <Dialog.Overlay className="fixed inset-0 z-[89] bg-felt/90" />
        <Dialog.Content
          className="fixed inset-0 z-[90] grid place-items-center overflow-y-auto px-5 py-6"
          onOpenAutoFocus={(e) => {
            e.preventDefault();
            requestAnimationFrame(() => field.current?.focus());
          }}
          onCloseAutoFocus={(e) => e.preventDefault()}
          onPointerDownOutside={(e) => e.preventDefault()}
          onFocusOutside={(e) => e.preventDefault()}
          onInteractOutside={(e) => e.preventDefault()}
        >
          <div className="panel-glass w-full max-w-md rounded-3xl p-6">
            <Dialog.Title asChild>
              <h2 className="font-display text-2xl text-digit outline-none">
                {REFLECTION_HEADING}
              </h2>
            </Dialog.Title>
            <p className="mt-1 text-xs uppercase tracking-widest text-muted">
              {current.kind === "alarm" ? "Alarm" : "Timer"} · optional
            </p>
            <textarea
              ref={field}
              aria-label="Session note"
              value={note}
              onChange={(e) => setNote(e.target.value)}
              rows={4}
              placeholder="Tap here to write a note"
              autoComplete="off"
              autoCorrect="on"
              spellCheck
              enterKeyHint="done"
              className="tx-note-field"
            />
            <p className="mt-2 text-[11px] text-muted">This draft stays in memory until you save it.</p>
            <div className="mt-4 grid grid-cols-2 gap-2">
              <PitButton className="min-h-11" onClick={() => onSave(false, note)}>
                <Save className="size-4" /> Save
              </PitButton>
              <PitButton variant="chrome" className="min-h-11" onClick={() => onSave(true, note)}>
                <Archive className="size-4" /> Archive
              </PitButton>
              <PitButton variant="ghost" className="min-h-11" onClick={onSkip}>
                Skip
              </PitButton>
              <PitButton variant="ghost" className="min-h-11 text-danger" onClick={onSkip}>
                <TriangleAlert className="size-4" /> Delete draft
              </PitButton>
            </div>
          </div>
        </Dialog.Content>
      </Dialog.Portal>
    </Dialog.Root>
  );
}
