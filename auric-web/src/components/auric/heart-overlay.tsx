import { playClick } from "@/lib/audio";
import { useAuric } from "@/lib/store";
import { TxPanel } from "@/components/auric/termux-chrome";

export function HeartOverlay() {
  const open = useAuric((s) => s.heartbeatBeatOpen);
  const reminder = useAuric((s) => s.heartbeatReminder);
  const last = useAuric((s) => s.lastBeatAt);
  if (!open) return null;
  const when = last
    ? new Date(last).toLocaleString(undefined, {
        weekday: "short",
        month: "short",
        day: "numeric",
        hour: "numeric",
        minute: "2-digit",
        second: "2-digit",
      })
    : "";

  return (
    <div className="tx-heart-layer" role="dialog" aria-modal="true" aria-labelledby="tx-heart-title">
      <TxPanel className="tx-heart-panel" edge="green" solid>
        <p className="tx-prompt">~ $ auric heartbeat --ask</p>
        <h2 id="tx-heart-title" className="tx-heading">
          HEARTBEAT
        </h2>
        <p className="tx-ok">[pulse] log action required</p>
        <p className="tx-ink">{reminder.trim() || "Keep track. Log what you are doing."}</p>
        <p className="tx-muted">{when}</p>
        <div className="tx-action-row" style={{ marginTop: 16 }}>
          <button
            type="button"
            className="tx-btn tx-btn-open"
            onClick={() => {
              playClick();
              const s = useAuric.getState();
              s.setHeartbeatBeatOpen(false);
              s.openSession();
              s.enterModule("heartbeat-log");
            }}
          >
            LOG
          </button>
          <button
            type="button"
            className="tx-btn tx-btn-min"
            onClick={() => {
              playClick();
              useAuric.getState().setHeartbeatBeatOpen(false);
            }}
          >
            SKIP
          </button>
        </div>
        <button
          type="button"
          className="tx-btn tx-btn-close"
          onClick={() => {
            playClick();
            useAuric.getState().setHeartbeatBeatOpen(false);
            useAuric.getState().setHeartbeatEnabled(false);
          }}
        >
          OFF
        </button>
      </TxPanel>
    </div>
  );
}
