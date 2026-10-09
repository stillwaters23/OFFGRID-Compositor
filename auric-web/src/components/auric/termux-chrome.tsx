import { useEffect, useId, useState, type ReactNode } from "react";
import { chicagoStamp } from "@/lib/clock";
import { remainingMs, stopwatchMs, useAuric } from "@/lib/store";
import { formatHms } from "@/lib/format";
import { playClick } from "@/lib/audio";
import { cn } from "@/lib/utils";

export function AuricWordmark() {
  return (
    <div className="tx-mark">
      <img
        src="/brand/auric-masthead.png?v=glass2"
        alt="Auric Time Crowned"
        className="tx-masthead"
        draggable={false}
      />
      <p className="tx-tagline">A private terminal for a cleaner tomorrow</p>
    </div>
  );
}

export function GlassBell({ count = 0 }: { count?: number }) {
  const hot = count > 0;
  return (
    <button
      type="button"
      className={cn("tx-bell", hot ? "is-hot" : "is-idle")}
      aria-label={count ? `${count} alerts` : "Alerts"}
      onClick={(e) => {
        e.stopPropagation();
        playClick();
        useAuric.getState().setNoticeOpen(true);
      }}
    >
      <LiquidBell hot={hot} />
      <span className="tx-bell-count">[{count}]</span>
    </button>
  );
}

function LiquidBell({ hot, size = 28 }: { hot: boolean; size?: number }) {
  const uid = useId().replace(/:/g, "");
  const fill = hot ? "#e8102a" : "#f7fbff";
  const deep = hot ? "#7a1018" : "#c5d0d8";
  const glow = hot ? "#ffd0d4" : "#ffffff";
  const g = `bell-g-${uid}`;
  const s = `bell-s-${uid}`;
  const ring = `bell-r-${uid}`;
  const d =
    "M32 8 C22 8 16 18 16 28 V38 C16 44 12 48 10 52 H54 C52 48 48 44 48 38 V28 C48 18 42 8 32 8 Z";
  return (
    <span className="tx-bell-shell">
      <span className="tx-bell-laser" aria-hidden />
      <svg viewBox="0 0 64 64" width={size} height={size} className="tx-bell-art" aria-hidden>
        <defs>
          <radialGradient id={g} cx="38%" cy="28%" r="70%">
            <stop offset="0%" stopColor={glow} stopOpacity="1" />
            <stop offset="38%" stopColor={fill} stopOpacity="0.95" />
            <stop offset="100%" stopColor={deep} stopOpacity="0.9" />
          </radialGradient>
          <linearGradient id={s} x1="0" y1="0" x2="1" y2="1">
            <stop offset="0%" stopColor="#fff" stopOpacity="0.95" />
            <stop offset="40%" stopColor="#fff" stopOpacity="0" />
          </linearGradient>
          <linearGradient id={ring} x1="0" y1="0" x2="1" y2="1">
            <stop offset="0%" stopColor="#fff" stopOpacity="0" />
            <stop offset="45%" stopColor="#fff" stopOpacity="1" />
            <stop offset="70%" stopColor={hot ? "#ffd0d4" : "#e8f6ff"} stopOpacity="1" />
            <stop offset="100%" stopColor="#fff" stopOpacity="0" />
          </linearGradient>
        </defs>
        <ellipse cx="32" cy="58" rx="10" ry="3" fill={hot ? "#e8102a" : "#dfe7ee"} opacity="0.4" />
        <path d={d} fill={`url(#${g})`} stroke={hot ? "#ffb0b8" : "#ffffff"} strokeWidth="1.8" />
        <ellipse cx="32" cy="54" rx="11" ry="4.2" fill={hot ? "#ff6b7a" : "#ffffff"} opacity="0.95" />
        <circle cx="32" cy="56" r="5.2" fill={hot ? "#ffd0d4" : "#ffffff"} />
        <path d="M22 12 Q32 6 42 12" fill="none" stroke="#fff" strokeWidth="1.5" opacity="0.85" />
        <path d="M28 8 Q32 4 36 8" fill="none" stroke="#fff" strokeWidth="2.4" />
        <path d="M18 20 Q28 14 30 28" fill={`url(#${s})`} opacity="0.7" />
        <path
          className="tx-bell-chase"
          d={d}
          fill="none"
          stroke={`url(#${ring})`}
          strokeWidth="2.4"
          strokeLinecap="round"
          pathLength="100"
        />
        <ellipse
          className="tx-bell-chase"
          cx="32"
          cy="54"
          rx="11"
          ry="4.2"
          fill="none"
          stroke="#fff"
          strokeWidth="1.6"
          pathLength="100"
        />
      </svg>
    </span>
  );
}

const LEDS = [
  { id: "online", label: "ONLINE", cls: "is-online" },
  { id: "standby", label: "STANDBY", cls: "is-standby" },
  { id: "syncing", label: "SYNCING", cls: "is-syncing" },
  { id: "offline", label: "OFFLINE", cls: "is-offline" },
] as const;

export function StatusLine({ alerts = 0 }: { alerts?: number }) {
  const now = useAuric((s) => s.now);
  const stamp = chicagoStamp(now || Date.now());
  const [online, setOnline] = useState(true);
  useEffect(() => {
    const sync = () => setOnline(navigator.onLine !== false);
    sync();
    window.addEventListener("online", sync);
    window.addEventListener("offline", sync);
    return () => {
      window.removeEventListener("online", sync);
      window.removeEventListener("offline", sync);
    };
  }, []);
  const active = online ? "online" : "offline";
  return (
    <div className="tx-status">
      <span className="tx-leds" role="status" aria-label={online ? "Online" : "Offline"}>
        {LEDS.map((led) => (
          <span key={led.id} className={cn("tx-led-item", active === led.id && "is-on")}>
            <span className={cn("tx-led", led.cls, active === led.id && "is-pulse")} aria-hidden>
              {led.id === "online" && active === "online" ? <span className="tx-led-orb" /> : null}
            </span>
            <span>{led.label}</span>
          </span>
        ))}
      </span>
      <span className="tx-status-right">
        <span className="tx-clock-line" suppressHydrationWarning title={stamp.line}>
          <svg viewBox="0 0 16 16" width="12" height="12" aria-hidden>
            <circle cx="8" cy="8" r="6.2" fill="none" stroke="currentColor" strokeWidth="1.3" />
            <path d="M8 4.6 V8 L10.4 9.6" fill="none" stroke="currentColor" strokeWidth="1.3" strokeLinecap="round" />
          </svg>
          {stamp.line}
        </span>
        <GlassBell count={alerts} />
      </span>
    </div>
  );
}

export function ToolLamp({
  tone,
  label,
  pulse,
}: {
  tone: "red" | "green";
  label: string;
  pulse?: boolean;
}) {
  return (
    <p className="tx-tool-lamp">
      <span className={cn("tx-led", tone === "green" ? "is-online" : "is-offline", pulse && "is-pulse")} aria-hidden />
      <span>{label}</span>
    </p>
  );
}

export function railTone(percent: number): "green" | "yellow" | "orange" | "red" {
  const p = Math.max(0, Math.min(100, percent));
  if (p > 60) return "green";
  if (p > 35) return "yellow";
  if (p > 15) return "orange";
  return "red";
}

export function CountRail({
  percent,
  label,
  time,
  install,
}: {
  percent: number;
  label?: string;
  time?: string;
  install?: boolean;
}) {
  const p = Math.max(0, Math.min(100, Math.round(percent)));
  const tone = install ? "green" : railTone(p);
  return (
    <div
      className={cn("tx-rail", install && "is-install")}
      data-tone={tone}
      role="progressbar"
      aria-valuenow={p}
      aria-valuemin={0}
      aria-valuemax={100}
      aria-label={label ?? "progress"}
    >
      <div className="tx-rail-meta">
        <span>{label ?? "remaining"}</span>
        <span>{time ? `${time} · ${p}%` : `${p}%`}</span>
      </div>
      <div className="tx-rail-track">
        <span className="tx-rail-fill" style={{ width: `${p}%` }} />
      </div>
    </div>
  );
}

export function TxPanel({
  children,
  footer,
  stacked,
  className,
  edge = "green",
  solid,
}: {
  children: ReactNode;
  footer?: ReactNode;
  stacked?: boolean;
  className?: string;
  edge?: "green" | "red";
  solid?: boolean;
}) {
  return (
    <div className={cn("tx-panel", stacked && "is-stacked", edge === "red" && "is-danger", solid && "is-solid", className)}>
      <div className="tx-scan" aria-hidden />
      <div className="tx-grain" aria-hidden />
      <div className="tx-scroll">
        {children}
        <div className="tx-scroll-end" aria-hidden />
      </div>
      {footer ? <div className="tx-footer">{footer}</div> : null}
    </div>
  );
}

export function SessionActions({
  primary,
  onPrimary,
  onClose,
  onMinimize,
  outlined,
}: {
  primary: string;
  onPrimary: () => void;
  onClose?: () => void;
  onMinimize?: () => void;
  outlined?: boolean;
}) {
  const count = 1 + (onClose ? 1 : 0) + (onMinimize ? 1 : 0);
  return (
    <div className={cn("tx-actions", outlined && "is-outline", count === 1 && "is-solo")}>
      <button
        type="button"
        className="tx-btn tx-btn-open"
        onClick={() => {
          playClick();
          onPrimary();
        }}
      >
        <span className="tx-btn-inner">
          <span className="tx-btn-sq" aria-hidden />
          <span className="tx-btn-label">{primary}</span>
          <span className="tx-btn-sq is-ghost" aria-hidden />
        </span>
      </button>
      {onClose ? (
        <button
          type="button"
          className="tx-btn tx-btn-close"
          onClick={() => {
            playClick();
            onClose();
          }}
        >
          <span className="tx-btn-inner">
            <span className="tx-btn-sq" aria-hidden />
            <span className="tx-btn-label">CLOSE SESSION</span>
            <span className="tx-btn-sq is-ghost" aria-hidden />
          </span>
        </button>
      ) : null}
      {onMinimize ? (
        <button
          type="button"
          className="tx-btn tx-btn-min"
          onClick={() => {
            playClick();
            onMinimize();
          }}
        >
          <span className="tx-btn-inner">
            <span className="tx-btn-sq" aria-hidden />
            <span className="tx-btn-label">MINIMIZE</span>
            <span className="tx-btn-sq is-ghost" aria-hidden />
          </span>
        </button>
      ) : null}
    </div>
  );
}

export function TaskbarPill() {
  const now = useAuric((s) => s.now);
  const mode = useAuric((s) => s.mode);
  const store = useAuric.getState();
  const ms = mode === "stopwatch" ? stopwatchMs(store, now) : remainingMs(store, now);
  const label = formatHms(ms).slice(0, 8);
  return (
    <button
      type="button"
      className="tx-pill"
      onClick={() => useAuric.getState().restoreShell()}
      aria-label="Restore Auric"
    >
      <span>Auric</span>
      <span className="tx-pill-time">{label}</span>
    </button>
  );
}

export function Scene() {
  return (
    <div className="tx-scene" aria-hidden>
      <span className="tx-scene-pane is-a" />
      <span className="tx-scene-pane is-b" />
      <span className="tx-scene-pane is-c" />
    </div>
  );
}
