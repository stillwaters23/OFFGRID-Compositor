import { useId } from "react";
import { cn } from "@/lib/utils";

/** Sapphire brilliant — faceted, lit, with a caustic reflection. */
export function IceDiamond({ className }: { className?: string }) {
  const id = useId().replace(/:/g, "");
  return (
    <svg viewBox="0 0 120 132" className={cn("block overflow-visible", className)} aria-hidden>
      <defs>
        <radialGradient id={`${id}-halo`} cx="50%" cy="40%" r="60%">
          <stop offset="0" stopColor="#7fd0ff" stopOpacity="0.55" />
          <stop offset="0.5" stopColor="#2f74d0" stopOpacity="0.20" />
          <stop offset="1" stopColor="#2f74d0" stopOpacity="0" />
        </radialGradient>
        <linearGradient id={`${id}-crown`} x1="20" y1="20" x2="100" y2="52" gradientUnits="userSpaceOnUse">
          <stop offset="0" stopColor="#eaf6ff" />
          <stop offset="0.45" stopColor="#a9d8ff" />
          <stop offset="1" stopColor="#3f7fd6" />
        </linearGradient>
        <linearGradient id={`${id}-pav`} x1="30" y1="46" x2="72" y2="126" gradientUnits="userSpaceOnUse">
          <stop offset="0" stopColor="#bfe2ff" />
          <stop offset="0.4" stopColor="#3f7fd6" />
          <stop offset="0.75" stopColor="#1b3f7a" />
          <stop offset="1" stopColor="#0a1f45" />
        </linearGradient>
        <linearGradient id={`${id}-table`} x1="40" y1="18" x2="80" y2="46" gradientUnits="userSpaceOnUse">
          <stop offset="0" stopColor="#ffffff" />
          <stop offset="1" stopColor="#bfe0ff" />
        </linearGradient>
      </defs>

      <ellipse cx="60" cy="54" rx="58" ry="60" fill={`url(#${id}-halo)`} />
      <path d="M60 128 L34 108 L60 120 L86 108 Z" fill="#6cc8ff" opacity="0.30" />

      <path d="M12 46 L108 46 L60 126 Z" fill={`url(#${id}-pav)`} />
      <path d="M60 46 L108 46 L60 126 Z" fill="#0a1f45" opacity="0.26" />
      <path d="M32 46 L60 46 L60 126 Z" fill="#ffffff" opacity="0.10" />
      <path d="M12 46 L32 46 L60 126 Z" fill="#0a1f45" opacity="0.20" />

      <path d="M12 46 L30 20 L90 20 L108 46 Z" fill={`url(#${id}-crown)`} />
      <path d="M40 20 L80 20 L92 46 L28 46 Z" fill={`url(#${id}-table)`} opacity="0.92" />
      <path d="M30 20 L40 20 L28 46 L12 46 Z" fill="#ffffff" opacity="0.28" />
      <path d="M80 20 L90 20 L108 46 L92 46 Z" fill="#2f6fc0" opacity="0.30" />
      <path d="M28 46 L40 20 L44 46 Z" fill="#ffffff" opacity="0.16" />
      <path d="M92 46 L80 20 L76 46 Z" fill="#1b3f7a" opacity="0.22" />

      <path d="M12 46 L108 46" stroke="#eaf6ff" strokeOpacity="0.5" strokeWidth="1.2" />
      <path d="M12 46 L30 20 L90 20 L108 46 L60 126 Z" fill="none" stroke="#eaf6ff" strokeOpacity="0.5" strokeWidth="1" />
      <path d="M40 20 L80 20 L92 46 L28 46 Z" fill="none" stroke="#eaf6ff" strokeOpacity="0.35" strokeWidth="0.7" />

      <path d="M46 27 L47.4 32.6 L53 34 L47.4 35.4 L46 41 L44.6 35.4 L39 34 L44.6 32.6 Z" fill="#ffffff" />
      <circle cx="84" cy="30" r="1.6" fill="#ffffff" opacity="0.9" />
      <circle cx="60" cy="112" r="1.3" fill="#bfe2ff" opacity="0.8" />
    </svg>
  );
}
