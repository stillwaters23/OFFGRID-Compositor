import { useState } from "react";
import { PitButton } from "@/components/auric/pit-button";
import { stopAlarm } from "@/lib/audio";
import { useAuric } from "@/lib/store";

export function DoneOverlay() {
  const status = useAuric((s) => s.timerStatus);
  const draftId = useAuric((s) => s.noteDraftId);
  const sessions = useAuric((s) => s.sessions);
  const session = sessions.find((s) => s.id === draftId);
  const [note, setNote] = useState(session?.note ?? "");

  if (status !== "done") return null;

  return (
    <div className="fixed inset-0 z-50 grid place-items-center px-5" style={{ background: "color-mix(in oklab, #000 18%, transparent)" }}>
      <div
        className="unfurl relative w-full min-w-0 max-w-md rounded-xl p-5"
        style={{
          background: "color-mix(in oklab, var(--chrome) 10%, transparent)",
          backdropFilter: "blur(22px) saturate(1.25)",
          WebkitBackdropFilter: "blur(22px) saturate(1.25)",
          boxShadow:
            "0 0 0 1px color-mix(in oklab, var(--accent) 45%, transparent), 0 0 40px color-mix(in oklab, var(--glow) 28%, transparent), inset 0 1px 0 color-mix(in oklab, #fff 18%, transparent)",
        }}
      >
        <p className="chrome-type font-display text-[10px] tracking-[0.28em] uppercase">Time, crowned</p>
        <h2 className="chrome-type mt-2 font-display text-2xl">The wheel is still.</h2>
        <p className="mt-1 text-sm text-digit/85">Log what you did with that time. It stays in History.</p>
        <textarea
          value={note}
          onChange={(e) => setNote(e.target.value)}
          rows={3}
          placeholder="Session note"
          className="tx-note-field mt-4"
          style={{ boxShadow: "0 0 0 1px color-mix(in oklab, var(--accent) 35%, transparent)" }}
        />
        <div className="mt-4 flex min-w-0 gap-2">
          <PitButton
            variant="ghost"
            className="min-w-0 flex-1"
            onClick={() => {
              stopAlarm();
              useAuric.getState().resetTimer();
              useAuric.getState().setNoteDraft(null);
            }}
          >
            Dismiss
          </PitButton>
          <PitButton
            className="min-w-0 flex-1"
            onClick={() => {
              stopAlarm();
              if (draftId) useAuric.getState().updateSessionNote(draftId, note);
              useAuric.getState().resetTimer();
              useAuric.getState().setNoteDraft(null);
              useAuric.getState().setMode("remind");
            }}
          >
            Save to History
          </PitButton>
        </div>
      </div>
    </div>
  );
}
