import { VaultView } from "@/components/auric/vault-view";
import { computeStreak } from "@/lib/achievements";
import { cutScore } from "@/lib/mark";
import { useAuric } from "@/lib/store";
import { formatTime } from "@/lib/format";

export function LogsExports() {
  return (
    <div className="flex min-h-0 min-w-0 max-w-full flex-1 flex-col overflow-x-hidden overflow-y-auto">
      <PrismPlaque />
      <HeartbeatLog />
      <VaultView />
    </div>
  );
}

export function PrismPlaque() {
  const sessions = useAuric((s) => s.sessions);
  const insurance = useAuric((s) => s.streakInsuranceUsed);
  const { streak } = computeStreak(sessions, !insurance);
  const score = cutScore(sessions, streak);
  const minutes = Math.round(sessions.reduce((a, s) => a + s.durationMs, 0) / 60_000);
  const notes = sessions.filter((s) => s.note.trim().length > 0).length;
  return (
    <div className="mx-1 mt-2 rounded-full panel-glass px-4 py-3">
      <div className="flex items-center justify-between gap-3">
        <p className="font-display text-sm tracking-wide text-accent">Prism</p>
        <p className="font-digits text-2xl digit-cut">{score}</p>
      </div>
      <p className="mt-1 text-[11px] leading-snug text-muted">
        Activity count, not a grade and not AI. min(100, sessions×4 + minutes×0.45 + notes×6 + streak×5).
        Right now that is {sessions.length}×4 + {minutes}×0.45 + {notes}×6 + {streak}×5.
      </p>
    </div>
  );
}

function HeartbeatLog() {
  const log = useAuric((s) => s.heartbeatLog);
  const visible = log.filter((entry) => !entry.archived);
  return (
    <div className="tx-fan">
      <p className="tx-prompt">~ $ auric log --heartbeat</p>
      {visible.length === 0 ? (
        <article className="tx-glass-tab">
          <p className="tx-muted">No pulses recorded yet. LOG on a heartbeat to fan a card here.</p>
        </article>
      ) : (
        visible.slice(0, 12).map((entry, i) => (
          <article key={entry.id} className="tx-glass-tab" style={{ transform: `translateY(${i * 6}px)` }}>
            <p className="tx-ok">heartbeat</p>
            <p className="tx-ink">{entry.body}</p>
            <p className="tx-muted">{formatTime(entry.at)}</p>
          </article>
        ))
      )}
    </div>
  );
}
