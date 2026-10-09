import { AcrylicReadout } from "@/components/auric/time-pad";
import { playStopwatchCut } from "@/lib/audio";
import { formatHms, formatSpanClock } from "@/lib/format";
import { stopwatchMs, useAuric } from "@/lib/store";

export function StopwatchView() {
  const status = useAuric((s) => s.swStatus);
  const laps = useAuric((s) => s.swLaps);
  const controlLock = useAuric((s) => s.controlLock);
  const now = useAuric((s) => s.now);
  const elapsed = stopwatchMs(useAuric.getState(), now);
  const running = status === "running";

  return (
    <div className="tx-tool">
      <AcrylicReadout value={formatSpanClock(elapsed)} pulse={running} />
      <p className="tx-muted">{running ? "running" : status === "paused" ? "held" : "stopwatch"}</p>
      {laps.length > 0 ? (
        <ul className="tx-laps min-w-0 max-w-full overflow-x-hidden">
          {laps
            .slice()
            .reverse()
            .map((lap, i) => {
              const idx = laps.length - i;
              const prev = laps[laps.length - i - 2] ?? 0;
              return (
                <li key={`${lap}-${idx}`} className="tx-core-line min-w-0 max-w-full flex-wrap overflow-x-hidden">
                  <span className="min-w-0">lap {idx}</span>
                  <span className="tx-muted min-w-0">{formatHms(lap - prev)}</span>
                  <span className="min-w-0">{formatHms(lap)}</span>
                </li>
              );
            })}
        </ul>
      ) : null}
      <div className="tx-action-row" style={{ marginTop: 12 }}>
        {running ? (
          <button
            type="button"
            className="tx-btn tx-btn-min"
            disabled={controlLock}
            onClick={() => {
              playStopwatchCut();
              useAuric.getState().pauseStopwatch();
            }}
          >
            HOLD
          </button>
        ) : status === "paused" ? (
          <button
            type="button"
            className="tx-btn tx-btn-open"
            disabled={controlLock}
            onClick={() => {
              playStopwatchCut();
              useAuric.getState().resumeStopwatch();
            }}
          >
            RESUME
          </button>
        ) : (
          <button
            type="button"
            className="tx-btn tx-btn-open"
            disabled={controlLock}
            onClick={() => {
              playStopwatchCut();
              useAuric.getState().startStopwatch();
            }}
          >
            START
          </button>
        )}
        <button
          type="button"
          className="tx-btn tx-btn-min"
          disabled={!running || controlLock}
          onClick={() => {
            playStopwatchCut();
            useAuric.getState().lapStopwatch();
          }}
        >
          LAP
        </button>
      </div>
      <button
        type="button"
        className="tx-btn tx-btn-min"
        onClick={() => {
          playStopwatchCut();
          useAuric.getState().resetStopwatch();
        }}
        disabled={controlLock}
      >
        RESET
      </button>
    </div>
  );
}
