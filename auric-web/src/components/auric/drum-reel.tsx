import { useCallback, useEffect, useRef, useState } from "react";
import { controlById } from "@/lib/controls";
import { reelGeometry } from "@/lib/reel-geometry";
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
  selector?: "aperture" | "none";
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
  selector = "aperture",
}: DrumReelProps) {
  const wrapRef = useRef<HTMLDivElement>(null);
  const barrelRef = useRef<HTMLDivElement>(null);
  const resizeRaf = useRef<number>(0);
  const pos = useRef(value);
  const vel = useRef(0);
  const dragging = useRef(false);
  const lastY = useRef(0);
  const lastT = useRef(0);
  const lastTick = useRef(value);
  const lastTickPosition = useRef(value);
  const raf = useRef<number>(0);
  const onChangeRef = useRef(onChange);
  const onTickRef = useRef(onTick);
  onChangeRef.current = onChange;
  onTickRef.current = onTick;
  const engine = useAuric((s) => controlById(s.controlId).engine);

  const plaque = size === "plaque";
  const hero = size === "hero";
  const trio = size === "trio";
  const baseFont = hero ? 56 : trio ? 30 : size === "lg" ? 40 : size === "md" ? 24 : plaque ? 22 : 18;
  const longest = labels?.reduce((m, s) => Math.max(m, s.length), 2) ?? Math.max(pad, String(count - 1).length, 2);
  const baseH = hero ? 288 : trio ? 180 : size === "lg" ? 224 : size === "md" ? 160 : plaque ? 176 : 120;
  const barrelWpx = hero ? 176 : trio ? 108 : size === "lg" ? 260 : size === "md" ? 64 : plaque ? 200 : 72;
  const [measured, setMeasured] = useState({ width: barrelWpx, height: baseH, digitScale: 1 });
  const geometry = reelGeometry({
    width: measured.width,
    height: measured.height,
    baseFont,
    longest,
    digitScale: measured.digitScale,
  });
  const { fontSize, radius, rowPitch } = geometry;
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
      el.style.transform = `translateX(-50%) rotateX(${-angle}deg) translateZ(${radius}px)`;
      el.style.opacity = abs < 14 ? "1" : abs < 28 ? "0.55" : abs < 42 ? "0.28" : "0.1";
      el.dataset.selected = abs < ANGLE / 2 ? "1" : "0";
    });
  }, []);

  useEffect(() => {
    if (!dragging.current && Math.abs(vel.current) < 0.001) {
      pos.current = value;
      lastTick.current = value;
      lastTickPosition.current = value;
      paint();
    }
  }, [value, paint]);

  function emitIfNeeded() {
    const target = Math.round(pos.current);
    const crossed = target - lastTickPosition.current;
    if (crossed === 0) return;
    const direction = Math.sign(crossed);
    for (let step = 1; step <= Math.abs(crossed); step += 1) {
      lastTick.current = wrapIndex(lastTickPosition.current + direction, config.current.count);
      lastTickPosition.current += direction;
      onTickRef.current?.();
    }
    onChangeRef.current(wrapIndex(target, config.current.count));
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
    const barrel = barrelRef.current;
    if (!barrel || typeof ResizeObserver === "undefined") return;
    const observer = new ResizeObserver((entries) => {
      const entry = entries[0];
      if (!entry) return;
      const { width, height } = entry.contentRect;
      const nextScale = Number.parseFloat(getComputedStyle(barrel).getPropertyValue("--digit-scale"));
      cancelAnimationFrame(resizeRaf.current);
      resizeRaf.current = requestAnimationFrame(() => {
        setMeasured((current) => {
          const digitScale = Number.isFinite(nextScale) && nextScale >= 0 ? nextScale : current.digitScale;
          if (
            Math.abs(current.width - width) < 0.5 &&
            Math.abs(current.height - height) < 0.5 &&
            Math.abs(current.digitScale - digitScale) < 0.001
          ) return current;
          return { width, height, digitScale };
        });
      });
    });
    observer.observe(barrel);
    return () => {
      observer.disconnect();
      cancelAnimationFrame(resizeRaf.current);
    };
  }, []);
  useEffect(() => {
    if (!disabled) return;
    dragging.current = false;
    vel.current = 0;
    cancelAnimationFrame(raf.current);
  }, [disabled]);

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
          size === "md" ? "shrink-0" : "w-full max-w-full flex-none",
          disabled && "pointer-events-none",
        )}
        ref={barrelRef}
        data-engine={engine}
        data-reel-size={size}
        data-reel-selector={selector}
        data-selected-row-center={geometry.rowCenter}
        style={{
          containerType: "inline-size",
          ["--reel-window" as string]: `${Math.max(rowPitch, fontSize * 1.62)}px`,
          ["--reel-font" as string]: `${fontSize}px`,
          height: `min(max(96px, calc(${baseH}px * var(--digit-scale, 1))), max(96px, calc(100dvh - 96px)))`,
          width: size === "md" ? `${barrelWpx}px` : undefined,
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
            lastTickPosition.current = pos.current;
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
                  fontSize: `${fontSize}px`,
                  backfaceVisibility: "hidden",
                  height: "1.16em",
                  width: "max-content",
                  left: "50%",
                  right: "auto",
                }}
              />
            ))}
          </div>
        </div>
        {selector === "aperture" ? <div className="aperture" /> : null}
      </div>
    </div>
  );
}
