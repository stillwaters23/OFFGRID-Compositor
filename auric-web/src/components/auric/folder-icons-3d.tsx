import { useId } from "react";
import type { ModuleSpec } from "@/lib/modules";
import { cn } from "@/lib/utils";

const ACCENTS: Record<ModuleSpec["glyph"], string> = {
  clock: "#75F0A1",
  watch: "#8AE8C4",
  bell: "#FFD17A",
  chips: "#64DDF6",
  type: "#D8F1FF",
  fade: "#8ED7EA",
  nest: "#B8E9D3",
  wave: "#71E8D2",
  pulses: "#7AF0A0",
  rings: "#73DBF2",
  ecg: "#77F3A0",
  "ecg-log": "#9BE7C0",
  sheets: "#D5EFFF",
  battery: "#8CF272",
  query: "#8ED7EA",
};

function HardwareMark({ kind, accent }: { kind: ModuleSpec["glyph"]; accent: string }) {
  const stroke = accent;
  switch (kind) {
    case "clock":
    case "watch":
      return (
        <>
          <circle cx="32" cy="32" r={kind === "watch" ? 13 : 15} fill="#12241f" stroke={stroke} strokeWidth="2" />
          <path d="M32 32V22M32 32l8 5" fill="none" stroke="#F1FFF8" strokeWidth="2" strokeLinecap="round" />
          {kind === "watch" ? <path d="M27 8h10v8H27zm0 40h10v8H27z" fill="#29433c" stroke={stroke} /> : null}
        </>
      );
    case "bell":
      return <path d="M18 39h28l-4-6V26a10 10 0 0 0-20 0v7zM28 44a4 4 0 0 0 8 0" fill="#263329" stroke={stroke} strokeWidth="2" strokeLinejoin="round" />;
    case "chips":
      return (
        <>
          <rect x="18" y="18" width="28" height="28" rx="5" fill="#1d3132" stroke={stroke} strokeWidth="2" />
          <path d="M24 13v5m8-5v5m8-5v5M24 46v5m8-5v5m8-5v5M13 24h5m-5 8h5m-5 8h5m28-16h5m-5 8h5m-5 8h5" stroke={stroke} strokeWidth="2" />
          <path d="M25 25h14v14H25z" fill="#6BE5FF" fillOpacity=".24" />
        </>
      );
    case "type":
      return <><path d="M17 44 29 16h7l12 28h-8l-2-6H26l-2 6z" fill="#26333b" stroke={stroke} strokeWidth="2" /><path d="M28 32h8" stroke="#f4fbff" strokeWidth="2" /></>;
    case "fade":
      return <><rect x="16" y="22" width="32" height="20" rx="4" fill="#25313a" stroke={stroke} strokeWidth="2" /><path d="M20 32h24" stroke={stroke} strokeWidth="3" strokeLinecap="round" /><circle cx="35" cy="32" r="5" fill="#DDF8FF" stroke={stroke} /></>;
    case "nest":
      return <><rect x="18" y="24" width="28" height="22" rx="4" fill="#293532" stroke={stroke} strokeWidth="2" /><rect x="23" y="19" width="22" height="22" rx="4" fill="#34443f" stroke="#e5fff2" strokeOpacity=".55" /><path d="M28 35h12M28 30h8" stroke={stroke} strokeWidth="2" /></>;
    case "wave":
    case "pulses":
    case "ecg":
    case "ecg-log":
      return (
        <>
          <rect x="15" y="21" width="34" height="22" rx="4" fill="#172b2d" stroke={stroke} strokeWidth="2" />
          <path
            d={kind === "wave" ? "M19 32c4 0 4-7 8-7s4 14 8 14 4-7 10-7" : "M19 32h7l4-9 5 18 4-9h6"}
            fill="none"
            stroke="#E9FFF6"
            strokeWidth="2"
            strokeLinecap="round"
            strokeLinejoin="round"
          />
          {kind === "ecg-log" ? <path d="M20 16h24" stroke={stroke} strokeWidth="2" /> : null}
        </>
      );
    case "rings":
      return <><circle cx="32" cy="32" r="16" fill="#1b3035" stroke={stroke} strokeWidth="2" /><circle cx="32" cy="32" r="10" fill="none" stroke="#f2fcff" strokeWidth="2" /><circle cx="32" cy="32" r="4" fill={stroke} /></>;
    case "sheets":
      return <><path d="M19 20h23v27H19z" fill="#293740" stroke={stroke} strokeWidth="2" /><path d="M24 16h22v26" fill="#36454c" stroke="#effaff" strokeOpacity=".7" strokeWidth="2" /><path d="M24 28h12m-12 6h12" stroke={stroke} strokeWidth="2" /></>;
    case "battery":
      return <><rect x="16" y="23" width="31" height="19" rx="4" fill="#203627" stroke={stroke} strokeWidth="2" /><path d="M47 29h4v7h-4" fill="#334b38" stroke={stroke} /><rect x="20" y="27" width="18" height="11" rx="2" fill={stroke} fillOpacity=".78" /></>;
    case "query":
      return <><path d="M32 14a18 18 0 1 0 0 36 18 18 0 0 0 0-36z" fill="#20313a" stroke={stroke} strokeWidth="2" /><text x="32" y="40" textAnchor="middle" fill="#f5fbff" fontSize="24" fontFamily="monospace">?</text></>;
  }
}

export function FolderIcon3D({ kind, className }: { kind: ModuleSpec["glyph"]; className?: string }) {
  const uid = useId().replaceAll(":", "");
  const accent = ACCENTS[kind];
  return (
    <svg
      className={cn("tx-folder-3d-icon", className)}
      viewBox="0 0 64 64"
      aria-hidden="true"
      focusable="false"
    >
      <defs>
        <linearGradient id={`housing-${uid}`} x1="0" y1="0" x2="0.9" y2="1">
          <stop stopColor="#F6FFFF" stopOpacity=".48" />
          <stop offset=".28" stopColor="#718A91" />
          <stop offset="1" stopColor="#10191C" />
        </linearGradient>
        <linearGradient id={`bevel-${uid}`} x1="0" y1="0" x2="1" y2="1">
          <stop stopColor="#FFFFFF" stopOpacity=".8" />
          <stop offset=".45" stopColor={accent} stopOpacity=".62" />
          <stop offset="1" stopColor="#07100F" stopOpacity=".85" />
        </linearGradient>
        <filter id={`depth-${uid}`} x="-30%" y="-30%" width="160%" height="170%">
          <feGaussianBlur in="SourceAlpha" stdDeviation="2" />
          <feOffset dy="3" />
          <feComponentTransfer><feFuncA type="linear" slope=".55" /></feComponentTransfer>
          <feMerge><feMergeNode /><feMergeNode in="SourceGraphic" /></feMerge>
        </filter>
      </defs>
      <g filter={`url(#depth-${uid})`}>
        <path d="M18 12h28a5 5 0 0 1 5 5v30a5 5 0 0 1-5 5H18a5 5 0 0 1-5-5V17a5 5 0 0 1 5-5z" fill={`url(#housing-${uid})`} stroke={`url(#bevel-${uid})`} strokeWidth="2" />
        <path d="M18 15h28M16 46h32" stroke="#FFFFFF" strokeOpacity=".36" strokeWidth="1" />
        <HardwareMark kind={kind} accent={accent} />
      </g>
      <circle cx="49" cy="15" r="2" fill={accent} />
    </svg>
  );
}
