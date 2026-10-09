import { useEffect, useRef } from "react";
import { formatHms } from "@/lib/format";
import { remainingMs, stopwatchMs, useAuric } from "@/lib/store";

export function FloatingChip() {
  const visible = useAuric((s) => s.floatingVisible);
  const x = useAuric((s) => s.floatingX);
  const y = useAuric((s) => s.floatingY);
  const mode = useAuric((s) => s.mode);
  const now = useAuric((s) => s.now);
  const store = useAuric();
  const drag = useRef<{ dx: number; dy: number } | null>(null);
  const element = useRef<HTMLButtonElement>(null);
  useEffect(() => {
    if (!visible) return;
    const clamp = () => {
      const box = element.current?.getBoundingClientRect();
      const s = useAuric.getState();
      s.setFloatingPos(Math.max(8, Math.min(window.innerWidth - (box?.width ?? 120) - 8, s.floatingX)),
        Math.max(8, Math.min(window.innerHeight - (box?.height ?? 48) - 8, s.floatingY)));
    };
    clamp();
    window.addEventListener("resize", clamp);
    return () => window.removeEventListener("resize", clamp);
  }, [visible]);

  if (!visible) return null;

  const ms = mode === "stopwatch" ? stopwatchMs(store, now) : remainingMs(store, now);

  return (
    <button
      ref={element}
      type="button"
      aria-label={`Floating ${mode === "stopwatch" ? "stopwatch" : "timer"}: ${formatHms(ms)}`}
      className="fixed z-40 min-h-11 max-w-[calc(100%-1rem)] touch-none rounded-full bg-chrome/80 px-3.5 font-digits text-lg text-digit halo backdrop-blur-md"
      style={{ left: x, top: y }}
      onPointerDown={(e) => {
        drag.current = { dx: e.clientX - x, dy: e.clientY - y };
        (e.currentTarget as HTMLElement).setPointerCapture(e.pointerId);
      }}
      onPointerMove={(e) => {
        if (!drag.current) return;
        const box = e.currentTarget.getBoundingClientRect();
        const nx = Math.max(8, Math.min(window.innerWidth - box.width - 8, e.clientX - drag.current.dx));
        const ny = Math.max(8, Math.min(window.innerHeight - box.height - 8, e.clientY - drag.current.dy));
        useAuric.getState().setFloatingPos(nx, ny);
      }}
      onPointerUp={() => {
        drag.current = null;
      }}
      onPointerCancel={() => { drag.current = null; }}
      onLostPointerCapture={() => { drag.current = null; }}
    >
      {formatHms(ms)}
    </button>
  );
}
