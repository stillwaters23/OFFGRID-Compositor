import { useCallback, useEffect, useRef } from "react";
import { controlById } from "@/lib/controls";
import { useAuric } from "@/lib/store";
import { cn, wrapIndex } from "@/lib/utils";

type DrumReelProps = {
  count: number;
  value: number;
  onChange: (n: number) => void;
  onTick?: () => void;
  disabled?: boolean;
  pad?: number;
  className?: string;
  labels?: string[];
  fontClass?: string;
  size?: "lg" | "md" | "sm" | "plaque" | "hero" | "trio";
  label?: string;
};

const ANGLE = 26;
const VISIBLE = 5;

export function DrumReel({
  count,
  value,
  onChange,
  onTick,
  disabled,
  pad = 2,
  className,
  labels,
  fontClass,
  size = "lg",
  label,
}: DrumReelProps) {
  const wrapRef = useRef<HTMLDivElement>(null);
  const pos = useRef(value);
  const vel = useRef(0);
  const dragging = useRef(false);
  const lastY = useRef(0);
  const lastT = useRef(0);
  const lastTick = useRef(value);
  const raf = useRef<number>(0);
  const onChangeRef = useRef(onChange);
  const onTickRef = useRef(onTick);
  onChangeRef.current = onChange;
  onTickRef.current = onTick;
  const engine = useAuric((s) => controlById(s.controlId).engine);

  const plaque = size === "plaque";
  const hero = size === "hero";
  const trio = size === "trio";
  const radius = hero ? 128 : trio ? 78 : size === "lg" ? 96 : size === "md" ? 68 : plaque ? 72 : 52;
  const baseFont = hero ? 56 : trio ? 30 : size === "lg" ? 40 : size === "md" ? 24 : plaque ? 22 : 18;
  const longest = labels?.reduce((m, s) => Math.max(m, s.length), 2) ?? Math.max(pad, String(count - 1).length, 2);
  const barrelW = hero ? 176 : trio ? 108 : size === "lg" ? 340 : size === "md" ? 72 : plaque ? 240 : 72;
  const fontSize = Math.max(14, Math.min(baseFont, Math.floor((barrelW * 0.88) / Math.max(2, longest))));
  const config = useRef({ count, labels, pad, radius });
  config.current = { count, labels, pad, radius };
  const paint = useCallback(() => {
    const root = wrapRef.current;
    if (!root) return;
    const { count, labels, pad, radius } = config.current;
    const center = wrapIndex(Math.round(pos.current), count);
    const frac = pos.current - Math.round(pos.current);
    root.querySelectorAll<HTMLElement>("[data-reel-item]").forEach((el) => {
      const offset = Number(el.dataset.offset);
      const n = wrapIndex(center + offset, count);
      const angle = (offset - frac) * ANGLE;
      const abs = Math.abs(angle);
      el.textContent = labels?.[n] ?? String(n).padStart(pad, "0");
      el.style.transform = `rotateX(${-angle}deg) translateZ(${radius}px)`;
      el.style.opacity = abs < 14 ? "1" : abs < 28 ? "0.55" : abs < 42 ? "0.28" : "0.1";
      el.dataset.selected = abs < ANGLE / 2 ? "1" : "0";
    });
  }, []);

  useEffect(() => {
    if (!dragging.current && Math.abs(vel.current) < 0.001) {
      pos.current = value;
      lastTick.current = value;
      paint();
    }
  }, [value, paint]);

  function emitIfNeeded() {
    const rounded = wrapIndex(Math.round(pos.current), config.current.count);
    if (rounded !== lastTick.current) {
      lastTick.current = rounded;
      onTickRef.current?.();
      onChangeRef.current(rounded);
    }
  }

  function loop() {
    if (dragging.current) return;
    const reduce =
      typeof window !== "undefined" && window.matchMedia("(prefers-reduced-motion: reduce)").matches;
    if (reduce) {
      vel.current = 0;
      pos.current = Math.round(pos.current);
      emitIfNeeded();
      paint();
      return;
    }
    vel.current *= 0.94;
    pos.current += vel.current;
    if (Math.abs(vel.current) < 0.004) {
      const target = Math.round(pos.current);
      const diff = target - pos.current;
      if (Math.abs(diff) < 0.002) {
        pos.current = target;
        vel.current = 0;
        emitIfNeeded();
        paint();
        return;
      }
      pos.current += diff * 0.22;
    }
    emitIfNeeded();
    paint();
    raf.current = requestAnimationFrame(loop);
  }

  function onPointerDown(e: React.PointerEvent) {
    if (disabled) return;
    dragging.current = true;
    lastY.current = e.clientY;
    lastT.current = performance.now();
    vel.current = 0;
    cancelAnimationFrame(raf.current);
    (e.currentTarget as HTMLElement).setPointerCapture(e.pointerId);
  }

  function onPointerMove(e: React.PointerEvent) {
    if (!dragging.current) return;
    const now = performance.now();
    const dy = e.clientY - lastY.current;
    const dt = Math.max(8, now - lastT.current);
    const delta = -dy / 28;
    pos.current += delta;
    vel.current = delta * (16 / dt);
    lastY.current = e.clientY;
    lastT.current = now;
    emitIfNeeded();
    paint();
  }

  function onPointerUp(e: React.PointerEvent) {
    if (!dragging.current) return;
    dragging.current = false;
    try {
      (e.currentTarget as HTMLElement).releasePointerCapture(e.pointerId);
    } catch {
      /* ignore */
    }
    raf.current = requestAnimationFrame(loop);
  }

  function onWheel(e: WheelEvent) {
    if (disabled) return;
    e.preventDefault();
    if (e.deltaY === 0) return;
    pos.current += e.deltaY > 0 ? 0.35 : -0.35;
    emitIfNeeded();
    paint();
    cancelAnimationFrame(raf.current);
    vel.current = e.deltaY > 0 ? 0.12 : -0.12;
    raf.current = requestAnimationFrame(loop);
  }

  const wheelHandler = useRef(onWheel);
  wheelHandler.current = onWheel;
  useEffect(() => {
    const root = wrapRef.current;
    const wheel = (event: WheelEvent) => wheelHandler.current(event);
    root?.addEventListener("wheel", wheel, { passive: false });
    return () => {
      root?.removeEventListener("wheel", wheel);
      cancelAnimationFrame(raf.current);
    };
  }, []);
  useEffect(() => { paint(); }, [count, radius, labels, pad, paint]);
  useEffect(() => {
    if (!disabled) return;
    dragging.current = false;
    vel.current = 0;
    cancelAnimationFrame(raf.current);
  }, [disabled]);

  const windowPx = Math.round(fontSize * 1.28);
  const baseH = hero ? 288 : trio ? 180 : size === "lg" ? 224 : size === "md" ? 160 : plaque ? 176 : 120;
  const barrelWpx = hero ? 176 : trio ? 108 : size === "lg" ? 260 : size === "md" ? 64 : plaque ? 200 : 72;

  const offsets = Array.from({ length: VISIBLE }, (_, i) => i - Math.floor(VISIBLE / 2));

  return (
    <div className={cn("flex w-full flex-col items-center", className)}>
      {label ? (
        <p className="mb-1.5 max-w-full px-0.5 text-center font-display text-xs tracking-[0.12em] text-accent uppercase">
          {label}
        </p>
      ) : null}
      <div
        className={cn(
          "barrel relative min-w-0 select-none touch-none overflow-hidden",
          size === "md" ? "shrink-0" : "w-full max-w-full flex-1",
          disabled && "pointer-events-none",
        )}
        data-engine={engine}
        data-reel-size={size}
        style={{
          containerType: "inline-size",
          ["--reel-window" as string]: `${windowPx}px`,
          ["--reel-font" as string]: `${fontSize}px`,
          height: `calc(${baseH}px * var(--digit-scale, 1))`,
          minHeight: trio ? 152 : size === "sm" ? 108 : size === "md" ? 140 : 120,
          width: size === "md" ? `${barrelWpx}px` : undefined,
          minWidth: trio || size === "sm" ? 64 : undefined,
          maxWidth: "100%",
        }}
      >
        <div
          ref={wrapRef}
          className="reel-mask absolute inset-x-0"
          style={{ top: "10%", bottom: "10%", perspective: hero ? 980 : trio || size === "lg" ? 820 : 640 }}
          onPointerDown={onPointerDown}
          onPointerMove={onPointerMove}
          onPointerUp={onPointerUp}
          onPointerCancel={() => {
            dragging.current = false;
            vel.current = 0;
            cancelAnimationFrame(raf.current);
            pos.current = lastTick.current;
            paint();
          }}
          tabIndex={disabled ? -1 : 0}
          onKeyDown={(event) => {
            if (disabled) return;
            const steps: Record<string, number> = { ArrowUp: 1, ArrowRight: 1, ArrowDown: -1, ArrowLeft: -1, PageUp: 10, PageDown: -10 };
            if (!(event.key in steps) && event.key !== "Home" && event.key !== "End") return;
            event.preventDefault();
            cancelAnimationFrame(raf.current);
            vel.current = 0;
            pos.current = event.key === "Home" ? 0 : event.key === "End" ? count - 1 : Math.round(pos.current) + steps[event.key]!;
            emitIfNeeded();
            paint();
          }}
          role="slider"
          aria-valuemin={0}
          aria-valuemax={count - 1}
          aria-valuenow={value}
          aria-valuetext={labels?.[value] ?? String(value)}
          aria-disabled={disabled}
          aria-orientation="vertical"
          aria-label={label ?? (plaque ? "Choose option" : "Choose value")}
        >
          <div
            className="absolute inset-0"
            style={{ transformStyle: "preserve-3d", transformOrigin: "center center" }}
          >
            {offsets.map((offset) => (
              <div
                key={offset}
                data-reel-item
                data-offset={offset}
                data-selected="0"
                className={cn(
                  "absolute inset-x-0 top-1/2 -mt-[0.58em] z-[1] text-center leading-none text-digit/70",
                  "whitespace-nowrap",
                  "data-[selected=1]:text-digit data-[selected=1]:digit-cut data-[selected=1]:digit-glow",
                  fontClass ?? (labels ? "font-sans" : "font-digits"),
                )}
                style={{
                  fontSize: `min(calc(${fontSize}px * var(--digit-scale, 1)), calc((100cqw - ${trio || hero ? 36 : 20}px) / ${longest * 1.1}))`,
                  backfaceVisibility: "hidden",
                  height: "1.16em",
                }}
              />
            ))}
          </div>
        </div>
        <div className="aperture" />
      </div>
    </div>
  );
}
