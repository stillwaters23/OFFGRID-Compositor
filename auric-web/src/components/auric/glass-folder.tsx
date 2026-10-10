import type { ModuleSpec } from "@/lib/modules";
import { cn } from "@/lib/utils";

function Glyph({ kind, lit, fadeId, etched }: { kind: ModuleSpec["glyph"]; lit: boolean; fadeId: string; etched: boolean }) {
  const stroke = etched ? "currentColor" : lit ? "#3BE87B" : "#6A7370";
  const fill = etched ? "currentColor" : lit ? "#3BE87B" : "#6A7370";
  const o = etched ? 0.94 : lit ? 1 : 0.32;
  switch (kind) {
    case "clock":
      return (
        <g stroke={stroke} fill="none" strokeWidth="1.7" strokeLinecap="round" strokeLinejoin="round" opacity={o}>
          <circle cx="60" cy="58" r="12" />
          <path d="M60 58 L60 50 M60 58 L66 62" />
        </g>
      );
    case "watch":
      return (
        <g stroke={stroke} fill="none" strokeWidth="1.7" strokeLinecap="round" strokeLinejoin="round" opacity={o}>
          <circle cx="60" cy="58" r="11" />
          <path d="M60 47 V43 M54 43 H66" />
          <path d="M60 58 L66 54" />
        </g>
      );
    case "bell":
      return (
        <g stroke={stroke} fill={lit ? `url(#${fadeId})` : "none"} strokeWidth="1.5" strokeLinejoin="round" opacity={o}>
          <path d="M50 56 c0-7 4.5-12 10-12 s10 5 10 12 v7 H50 z" />
          <path d="M55 65 a5 5 0 0 0 10 0" fill="none" />
        </g>
      );
    case "chips":
      return (
        <g opacity={o}>
          <rect x="48" y="50" width="12" height="16" rx="1.6" fill={etched ? fill : lit ? "#5CE1FF" : fill} />
          <rect x="57" y="47" width="12" height="16" rx="1.6" fill={etched ? fill : lit ? "#3BE87B" : fill} />
          <rect x="66" y="52" width="12" height="16" rx="1.6" fill={etched ? fill : lit ? "#E6EFF5" : fill} />
        </g>
      );
    case "type":
      return (
        <g fill={fill} fontFamily="ui-monospace, monospace" opacity={o}>
          <text x="48" y="66" fontSize="20" fontWeight="700">
            A
          </text>
          <text x="68" y="60" fontSize="11">
            A
          </text>
        </g>
      );
    case "fade":
      return <rect x="46" y="51" width="32" height="16" rx="2" fill={etched ? fill : `url(#${fadeId})`} opacity={etched ? 0.94 : lit ? 0.9 : 0.25} />;
    case "nest":
      return (
        <g stroke={stroke} fill="none" strokeWidth="1.3" opacity={o}>
          <rect x="60" y="54" width="18" height="12" rx="1.2" opacity={0.4} />
          <rect x="54" y="50" width="18" height="12" rx="1.2" opacity={0.7} />
          <rect x="48" y="46" width="18" height="12" rx="1.2" />
        </g>
      );
    case "wave":
      return (
        <path
          d="M44 60 C50 60 50 48 58 48 C66 48 66 70 74 70 C82 70 84 60 90 60"
          fill="none"
          stroke={stroke}
          strokeWidth="1.7"
          strokeLinecap="round"
          opacity={o}
        />
      );
    case "pulses":
      return (
        <path
          d="M42 60 L52 60 L55 46 L64 72 L70 60 L90 60"
          fill="none"
          stroke={stroke}
          strokeWidth="1.7"
          strokeLinejoin="round"
          strokeLinecap="round"
          opacity={o}
        />
      );
    case "rings":
      return (
        <g stroke={stroke} fill="none" strokeWidth="1.15" opacity={o}>
          <circle cx="62" cy="58" r="3.2" />
          <circle cx="62" cy="58" r="7.4" />
          <circle cx="62" cy="58" r="11.6" />
        </g>
      );
    case "ecg":
      return (
        <path
          d="M40 60 L52 60 L56 42 L64 74 L70 52 L90 60"
          fill="none"
          stroke={stroke}
          strokeWidth="1.6"
          strokeLinejoin="round"
          strokeLinecap="round"
          opacity={o}
        />
      );
    case "ecg-log":
      return (
        <g opacity={o}>
          <rect x="50" y="42" width="26" height="8" rx="1.2" fill={stroke} opacity={0.18} />
          <rect x="48" y="47" width="26" height="8" rx="1.2" fill={stroke} opacity={0.28} />
          <path d="M40 66 L54 66 L58 52 L64 76 L70 60 L90 66" fill="none" stroke={stroke} strokeWidth="1.4" strokeLinejoin="round" />
        </g>
      );
    case "sheets":
      return (
        <g stroke={stroke} fill={stroke} fillOpacity={lit ? 0.12 : 0.05} strokeWidth="1.15" opacity={o}>
          <rect x="56" y="46" width="22" height="16" rx="1.2" />
          <rect x="52" y="51" width="22" height="16" rx="1.2" />
          <rect x="48" y="56" width="22" height="16" rx="1.2" />
        </g>
      );
    case "battery":
      return (
        <g stroke={stroke} fill="none" strokeWidth="1.5" opacity={o}>
          <rect x="46" y="51" width="32" height="16" rx="2.2" />
          <rect x="78" y="55" width="3.4" height="8" rx="0.6" fill={fill} />
          <rect x="49" y="54" width="16" height="10" rx="1.2" fill={fill} />
        </g>
      );
    case "query":
      return (
        <text x="62" y="68" textAnchor="middle" fill={fill} fontFamily="ui-monospace, monospace" fontSize="24" opacity={o}>
          ?
        </text>
      );
  }
}

const BODY =
  "M14 42 C14 38 17 36 22 36 H50 C54 36 57 38 59 42 H100 C105 42 108 45 108 50 V94 C108 100 103 104 97 104 H23 C17 104 12 100 12 94 V50 C12 45 14 42 14 42 Z";
const TAB = "M18 36 L22 22 C23 18 26 16 31 16 H52 C57 16 60 18 62 22 L70 36 Z";
const CLOSED_LID =
  "M14 42 C14 38 17 36 22 36 H50 C54 36 57 38 59 42 H100 C105 42 108 45 108 50 L12 50 C12 45 14 42 14 42 Z";

export function GlassFolder({
  spec,
  on,
  glowZero,
  etched = false,
}: {
  spec: ModuleSpec;
  on: boolean;
  glowZero?: boolean;
  etched?: boolean;
}) {
  const open = on;
  const lit = on && !glowZero && !etched;
  const locked = !on;
  const uid = spec.id;
  const rim = locked ? "#C45A5A" : "#CFF6FF";
  const glow = locked ? "#E8102A" : "#3BE87B";

  return (
    <svg viewBox="0 0 120 112" className={cn("tx-folder-art", open && "is-open", lit && "is-lit", locked && "is-locked", etched && "is-etched")} aria-hidden>
      <defs>
        <linearGradient id={`glass-${uid}`} x1="0.1" y1="0" x2="0.9" y2="1">
          <stop offset="0%" stopColor={locked ? "#F4C8C8" : "#F7FCFF"} stopOpacity={locked ? 0.26 : 0.22} />
          <stop offset="28%" stopColor={locked ? "#8A3038" : "#8ED7EA"} stopOpacity={locked ? 0.16 : 0.06} />
          <stop offset="100%" stopColor={locked ? "#2A0C12" : "#071014"} stopOpacity={locked ? 0.30 : 0.14} />
        </linearGradient>
        <linearGradient id={`flap-${uid}`} x1="0" y1="0" x2="0.2" y2="1">
          <stop offset="0%" stopColor="#FFFFFF" stopOpacity={lit ? 0.7 : 0.28} />
          <stop offset="100%" stopColor={locked ? "#2A0A10" : "#0C1816"} stopOpacity="0.35" />
        </linearGradient>
        <linearGradient id={`paper-${uid}`} x1="0" y1="0" x2="0" y2="1">
          <stop offset="0%" stopColor="#F7FBFF" stopOpacity={lit ? 0.72 : 0.35} />
          <stop offset="100%" stopColor="#C5D4DC" stopOpacity={lit ? 0.28 : 0.12} />
        </linearGradient>
        <linearGradient id={`spec-${uid}`} x1="0" y1="0" x2="1" y2="1">
          <stop offset="0%" stopColor="#FFFFFF" stopOpacity={lit ? 0.55 : 0.18} />
          <stop offset="32%" stopColor="#FFFFFF" stopOpacity="0" />
        </linearGradient>
        <linearGradient id={`fade-sq-${uid}`} x1="0" y1="0" x2="1" y2="0">
          <stop offset="0%" stopColor="#3BE87B" stopOpacity="0.95" />
          <stop offset="100%" stopColor="#3BE87B" stopOpacity="0" />
        </linearGradient>
        <radialGradient id={`pool-${uid}`} cx="50%" cy="50%" r="50%">
          <stop offset="0%" stopColor={glow} stopOpacity={glowZero ? 0 : lit ? 0.28 : locked ? 0.18 : 0} />
          <stop offset="100%" stopColor={glow} stopOpacity="0" />
        </radialGradient>
        <filter id={`halo-${uid}`} x="-35%" y="-35%" width="170%" height="170%">
          <feGaussianBlur stdDeviation="1.6" result="b" />
          <feMerge>
            <feMergeNode in="b" />
            <feMergeNode in="SourceGraphic" />
          </feMerge>
        </filter>
        <clipPath id={`body-${uid}`}>
          <path d={BODY} />
        </clipPath>
      </defs>

      <ellipse cx="60" cy="106" rx="34" ry="5" fill={`url(#pool-${uid})`} />

      {open ? (
        <>
          <path d={TAB} fill={`url(#flap-${uid})`} stroke={rim} strokeWidth="1.4" filter={lit ? `url(#halo-${uid})` : undefined} />
          <path d="M20 34 L68 34" stroke="#F4FBFF" strokeWidth="1" opacity={lit ? 0.45 : 0.12} strokeLinecap="round" />
          <g clipPath={`url(#body-${uid})`}>
            <rect x="34" y="28" width="52" height="58" rx="2" fill={`url(#paper-${uid})`} transform="rotate(-6 60 50)" />
            <rect x="30" y="32" width="54" height="58" rx="2" fill={`url(#paper-${uid})`} transform="rotate(4 60 54)" />
            <rect x="28" y="36" width="56" height="56" rx="2.2" fill={`url(#paper-${uid})`} />
          </g>
          <path d={BODY} fill={`url(#glass-${uid})`} stroke={rim} strokeWidth="1.7" filter={lit ? `url(#halo-${uid})` : undefined} />
          <path d={BODY} fill={`url(#spec-${uid})`} />
          <path d="M18 48 H96" stroke="#F4FBFF" strokeWidth="1.15" opacity={lit ? 0.5 : 0.14} strokeLinecap="round" />
        </>
      ) : (
        <>
          <path d={BODY} fill={`url(#glass-${uid})`} stroke={rim} strokeWidth="1.7" filter={!glowZero ? `url(#halo-${uid})` : undefined} />
          <path d={CLOSED_LID} fill={`url(#flap-${uid})`} stroke={rim} strokeWidth="1.5" />
          <path d="M14 50 H108" stroke="#E8102A" strokeWidth="1.4" opacity="0.55" />
        </>
      )}

      <Glyph kind={spec.glyph} lit={lit} fadeId={`fade-sq-${uid}`} etched={etched} />
    </svg>
  );
}
