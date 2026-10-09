import { contrastRatio, ensureReadable, hexToRgb } from "@/lib/color";
import { prefersDarkScheme, schemeFromSchedule } from "@/lib/clock";
import { BLUE_DIAMOND, paletteById } from "@/lib/palettes";
import type { Palette, PaintSlot } from "@/lib/types";
import type { FloorId } from "@/lib/floors";
import type { CSSProperties } from "react";

export type AppearanceMode = "system" | "light" | "dark";

export type AppearanceDraft = {
  palette: Palette;
  glowIntensity: number;
  houseOpacity: number;
  digitFont: string;
  digitScale: number;
  appearanceMode: AppearanceMode;
  scheduleEnabled: boolean;
  scheduleLight: string;
  scheduleDark: string;
  glowMode: boolean;
  floorId: FloorId;
  floorCustom: string;
};

export const SAFE_DARK: Palette = { ...BLUE_DIAMOND };

export const RESTORE_CHROME = {
  background: "#0A1730",
  color: "#C9D7EE",
  boxShadow: "0 0 0 1px #7FC6FF",
} as const;

export const PARTS: { id: PaintSlot; label: string; hint: string }[] = [
  { id: "felt", label: "Page surface", hint: "The felt behind the clock and reels." },
  { id: "chrome", label: "Panel surface", hint: "Cards, pills, and Settings glass." },
  { id: "digit", label: "Digits", hint: "Clock and reel numerals." },
  { id: "accent", label: "Accent ink", hint: "Selected controls and titles." },
  { id: "danger", label: "Alert ink", hint: "Warnings and overtime." },
  { id: "glow", label: "Decorative glow", hint: "Halo only. Never the only way to read a number." },
];

export function boundedPercent(value: number, fallback = 50): number {
  return Number.isFinite(value) ? Math.max(0, Math.min(100, value)) : fallback;
}

export function resolveScheme(args: {
  mode: AppearanceMode;
  scheduleEnabled: boolean;
  lightStart: string;
  darkStart: string;
  nowMs: number;
  zone?: string | null;
}): "light" | "dark" {
  if (args.mode === "light" || args.mode === "dark") return args.mode;
  if (args.scheduleEnabled) return schemeFromSchedule(args.nowMs, args.lightStart, args.darkStart, args.zone);
  return prefersDarkScheme() ? "dark" : "light";
}

function toLight(palette: Palette): Palette {
  if (palette.light) return palette;
  const felt = "#F6F1E8";
  const chrome = "#E8E0D2";
  const digit = "#1A1712";
  const accent = contrastRatio(palette.accent, felt) >= 3 ? palette.accent : "#1F4F8A";
  const danger = contrastRatio(palette.danger, felt) >= 3 ? palette.danger : "#9C1330";
  return {
    ...palette,
    felt,
    chrome,
    digit,
    accent,
    danger,
    light: true,
  };
}

function samePaint(a: Palette, b: Palette): boolean {
  return (
    a.id === b.id &&
    a.felt === b.felt &&
    a.chrome === b.chrome &&
    a.digit === b.digit &&
    a.accent === b.accent &&
    a.danger === b.danger &&
    a.glow === b.glow &&
    a.light === b.light
  );
}

/** Zustand selectors must return a stable reference; do not key this on ticking nowMs. */
let lastCommitted: Palette | null = null;

export function committedPalette(args: {
  paletteId: string;
  customPalette: Palette | null;
  mode: AppearanceMode;
  scheduleEnabled: boolean;
  lightStart: string;
  darkStart: string;
  nowMs: number;
  zone?: string | null;
}): Palette {
  const base = args.customPalette ?? paletteById(args.paletteId);
  const scheme = resolveScheme(args);
  const next = scheme === "light" ? toLight(base) : base.light ? { ...base, light: false } : base;
  if (lastCommitted && samePaint(lastCommitted, next)) return lastCommitted;
  lastCommitted = next;
  return next;
}

export function previewPalette(draft: AppearanceDraft, nowMs: number, zone?: string | null): Palette {
  return committedPalette({
    paletteId: draft.palette.id,
    customPalette: draft.palette.id === "custom" ? draft.palette : null,
    mode: draft.appearanceMode,
    scheduleEnabled: draft.scheduleEnabled,
    lightStart: draft.scheduleLight,
    darkStart: draft.scheduleDark,
    nowMs,
    zone,
  });
}

export type ContrastIssue = {
  pair: string;
  ratio: number;
  need: number;
  message: string;
};

export function appearanceIssues(palette: Palette): ContrastIssue[] {
  // Theme alpha is a separate decorative control; essential paint must be opaque.
  if (!palette || ["felt", "chrome", "digit", "accent", "danger", "glow"].some((key) => {
    const value = (palette as unknown as Record<string, unknown>)[key];
    return typeof value !== "string" || !/^#(?:[\da-f]{3}|[\da-f]{6})$/i.test(value);
  })) {
    return [{ pair: "Color format", ratio: 0, need: 1, message: "Use opaque #RGB or #RRGGBB colors for essential surfaces and text." }];
  }
  const issues: ContrastIssue[] = [];
  const push = (pair: string, a: string, b: string, need: number, message: string) => {
    const ratio = contrastRatio(a, b);
    if (ratio < need) issues.push({ pair, ratio, need, message });
  };
  push("Digits on page", palette.digit, palette.felt, 4.5, "Clock numerals would hide against the page.");
  push("Title on panel", palette.digit, palette.chrome, 3, "Headings would hide against panels.");
  push("Accent on page", palette.accent, palette.felt, 3, "Actions would blend into the page.");
  return issues;
}

export function canApplyAppearance(palette: Palette | null | undefined): boolean {
  try {
    return appearanceIssues(palette as Palette).length === 0;
  } catch {
    return false;
  }
}

export function draftsDiffer(a: AppearanceDraft, b: AppearanceDraft): boolean {
  return (
    a.appearanceMode !== b.appearanceMode ||
    a.scheduleEnabled !== b.scheduleEnabled ||
    a.scheduleLight !== b.scheduleLight ||
    a.scheduleDark !== b.scheduleDark ||
    a.glowMode !== b.glowMode ||
    a.floorId !== b.floorId ||
    a.floorCustom !== b.floorCustom ||
    a.glowIntensity !== b.glowIntensity ||
    a.houseOpacity !== b.houseOpacity ||
    a.digitFont !== b.digitFont ||
    a.digitScale !== b.digitScale ||
    a.palette.id !== b.palette.id ||
    a.palette.felt !== b.palette.felt ||
    a.palette.chrome !== b.palette.chrome ||
    a.palette.digit !== b.palette.digit ||
    a.palette.accent !== b.palette.accent ||
    a.palette.danger !== b.palette.danger ||
    a.palette.glow !== b.palette.glow
  );
}

export function makeReadable(palette: Palette): Palette {
  const next = ensureReadable({ ...palette });
  if (!hexToRgb(next.felt)) return { ...SAFE_DARK };
  return next;
}

export function paletteStyle(palette: Palette): CSSProperties {
  return {
    background: palette.felt,
    color: palette.digit,
    ["--felt" as string]: palette.felt,
    ["--chrome" as string]: palette.chrome,
    ["--digit" as string]: palette.digit,
    ["--accent" as string]: palette.accent,
    ["--danger" as string]: palette.danger,
    ["--glow" as string]: palette.glow,
  };
}
