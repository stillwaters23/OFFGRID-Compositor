import { useId } from "react";
import { cn } from "@/lib/utils";

function prefersReduce() {
  return typeof window !== "undefined" && window.matchMedia("(prefers-reduced-motion: reduce)").matches;
}

export function OrbToggle({
  on,
  onChange,
  label,
  size = "md",
}: {
  on: boolean;
  onChange: (v: boolean) => void;
  label: string;
  size?: "sm" | "md" | "lg";
}) {
  const uid = useId().replace(/:/g, "");
  const reduce = prefersReduce();
  return (
    <button
      type="button"
      role="switch"
      aria-checked={on}
      aria-label={label}
      onClick={() => onChange(!on)}
      className={cn("ice-switch", on ? "is-on" : "is-off", size === "lg" && "ice-switch-lg")}
    >
      <span className="ice-switch-laser" aria-hidden />
      <span className="ice-switch-word">{on ? "ON" : "OFF"}</span>
      <span className="ice-switch-orb" aria-hidden>
        <span className="ice-switch-orb-laser" />
        <span className="ice-switch-liquid">
          <svg viewBox="0 0 64 64" width="100%" height="100%" aria-hidden>
            <defs>
              <radialGradient id={`liq-${uid}`} cx="35%" cy="30%" r="70%">
                <stop offset="0%" stopColor="#fff" stopOpacity="0.9" />
                <stop offset="38%" stopColor={on ? "#7dffb0" : "#ff8a96"} stopOpacity="0.85" />
                <stop offset="100%" stopColor={on ? "#0a4a22" : "#5a0810"} stopOpacity="1" />
              </radialGradient>
              <filter id={`flow-${uid}`} x="-20%" y="-20%" width="140%" height="140%">
                <feTurbulence type="fractalNoise" baseFrequency="0.05 0.08" numOctaves="2" result="t" seed="2">
                  {reduce ? null : (
                    <animate
                      attributeName="baseFrequency"
                      values="0.05 0.08;0.09 0.04;0.05 0.08"
                      dur="7s"
                      repeatCount="indefinite"
                    />
                  )}
                </feTurbulence>
                <feDisplacementMap in="SourceGraphic" in2="t" scale="7" />
              </filter>
            </defs>
            <circle cx="32" cy="32" r="30" fill={`url(#liq-${uid})`} filter={`url(#flow-${uid})`} />
          </svg>
        </span>
        <svg className="ice-switch-power" viewBox="0 0 24 24" aria-hidden>
          <path d="M12 3.2 V11" />
          <path d="M7.6 6.8 a6.2 6.2 0 1 0 8.8 0" />
        </svg>
      </span>
    </button>
  );
}

export function PulseOrb({
  on,
  color,
  title,
  className,
}: {
  on?: boolean;
  color?: string;
  title?: string;
  className?: string;
}) {
  return (
    <span
      title={title}
      aria-hidden
      className={cn("inline-block size-2.5 shrink-0 rounded-full", on ? "orb-pulse" : "", className)}
      style={{ background: color || "var(--accent)", boxShadow: `0 0 8px ${color || "var(--accent)"}` }}
    />
  );
}
