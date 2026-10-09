import { useRef } from "react";
import { formatHms } from "@/lib/format";
import { remainingMs, stopwatchMs, useAuric } from "@/lib/store";
import { cn } from "@/lib/utils";

export function PullHud() {
  const open = useAuric((s) => s.hudOpen);
  const mode = useAuric((s) => s.mode);
  const now = useAuric((s) => s.now);
  const store = useAuric();
  const startY = useRef(0);

  const ms =
    mode === "stopwatch"
      ? stopwatchMs(store, now)
      : remainingMs(store, now);

  return (
    <div className="relative z-30">
      <div
        className="flex h-4 cursor-grab items-end justify-center pt-1"
        onPointerDown={(e) => {
          startY.current = e.clientY;
          (e.currentTarget as HTMLElement).setPointerCapture(e.pointerId);
        }}
        onPointerUp={(e) => {
          const dy = e.clientY - startY.current;
          if (dy > 24) useAuric.getState().setHudOpen(true);
          if (dy < -12) useAuric.getState().setHudOpen(false);
          if (Math.abs(dy) < 12) useAuric.getState().setHudOpen(!open);
        }}
      >
        <span className="h-1 w-16 rounded-full bg-accent" />
      </div>
      <div
        className={cn(
          "overflow-hidden transition-[max-height,opacity] duration-300 ease-[cubic-bezier(0.22,1,0.36,1)]",
          open ? "max-h-16 opacity-100" : "max-h-0 opacity-0",
        )}
      >
        <div className="mx-3 mb-1 flex items-center justify-between rounded-md panel px-3 py-2">
          <span className="font-display text-[10px] tracking-[0.28em] uppercase text-accent">HUD</span>
          <span className="font-digits text-xl text-digit digit-glow">{formatHms(ms)}</span>
          <span className="text-[10px] tracking-widest uppercase text-muted">{mode}</span>
        </div>
      </div>
    </div>
  );
}
