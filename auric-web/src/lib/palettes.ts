import type { Palette, PaintSlot } from "@/lib/types";
import { skinById as skinLookup } from "@/lib/skins";

export const ABYSS: Palette = {
  id: "abyss",
  name: "Abyss",
  premium: false,
  felt: "#05070E",
  chrome: "#0C1526",
  digit: "#EAF3FF",
  accent: "#9FD4FF",
  danger: "#E2566A",
  glow: "#5CC4FF",
};

export const BLUE_DIAMOND: Palette = {
  id: "blue-diamond",
  name: "Blue Diamond",
  premium: false,
  felt: "#04060F",
  chrome: "#0A1730",
  digit: "#EAF3FF",
  accent: "#7FC6FF",
  danger: "#E2566A",
  glow: "#49B6FF",
};

export const CURATED: Palette[] = [
  BLUE_DIAMOND,
  ABYSS,
  {
    id: "royal",
    name: "Royal",
    premium: false,
    felt: "#0B1D4D",
    chrome: "#132A66",
    digit: "#F4F7FF",
    accent: "#7AA6FF",
    danger: "#E2566A",
    glow: "#5B8CFF",
  },
  {
    id: "blood",
    name: "Blood",
    premium: false,
    felt: "#14060A",
    chrome: "#2B0F16",
    digit: "#F8E8EA",
    accent: "#C43B4A",
    danger: "#E2566A",
    glow: "#E07A7A",
  },
  {
    id: "jade",
    name: "Jade",
    premium: false,
    felt: "#06140F",
    chrome: "#0F3D2E",
    digit: "#E8F7F0",
    accent: "#3D9B74",
    danger: "#E2566A",
    glow: "#5CDBA8",
  },
  {
    id: "violet",
    name: "Violet",
    premium: true,
    felt: "#0C0716",
    chrome: "#1E1238",
    digit: "#F0E8FF",
    accent: "#8B6CFF",
    danger: "#E2566A",
    glow: "#B49CFF",
  },
  {
    id: "amber",
    name: "Amber",
    premium: false,
    felt: "#120E08",
    chrome: "#2A1C0C",
    digit: "#FFF4E0",
    accent: "#E0A03A",
    danger: "#E2566A",
    glow: "#F0C060",
  },
  {
    id: "pearl",
    name: "Pearl",
    premium: false,
    felt: "#F4F1EA",
    chrome: "#E7E2D6",
    digit: "#1A1814",
    accent: "#8A7048",
    danger: "#B33A48",
    glow: "#D4C4A0",
    light: true,
  },
  {
    id: "ivory",
    name: "Ivory",
    premium: false,
    felt: "#F7F4EE",
    chrome: "#EDE8DC",
    digit: "#1C1914",
    accent: "#A89060",
    danger: "#B33A48",
    glow: "#E8D9B0",
    light: true,
  },
  {
    id: "gold",
    name: "Gold",
    premium: true,
    felt: "#100E08",
    chrome: "#241E10",
    digit: "#FFF6DC",
    accent: "#D4AF37",
    danger: "#E2566A",
    glow: "#F0D060",
  },
  {
    id: "obsidian",
    name: "Obsidian",
    premium: false,
    felt: "#07080A",
    chrome: "#141618",
    digit: "#EEF1F4",
    accent: "#C5CCD4",
    danger: "#E2566A",
    glow: "#E8EEF4",
  },
  {
    id: "ice",
    name: "Ice",
    premium: false,
    felt: "#071018",
    chrome: "#0E2436",
    digit: "#EAF6FF",
    accent: "#7EC8EA",
    danger: "#E2566A",
    glow: "#A8E0F8",
  },
];

export const HOUSE_RACKS: Palette[] = CURATED;
export const DEFAULT_PALETTE = BLUE_DIAMOND;
export const HOUSE_PALETTE_ID = "blue-diamond";

export function paletteById(id: string | null | undefined): Palette {
  if (id === "blue-diamond") return BLUE_DIAMOND;
  if (id === "abyss" || id === "ice-whisper") return ABYSS;
  return CURATED.find((p) => p.id === id) ?? HOUSE_RACKS.find((p) => p.id === id) ?? skinLookup(id);
}

export const SLOT_LABELS: Record<PaintSlot, string> = {
  felt: "Background",
  chrome: "Panels",
  digit: "Numbers",
  accent: "Highlights",
  danger: "Alerts",
  glow: "Glow",
};

export const SLOT_HINTS: Record<PaintSlot, string> = {
  felt: "Changes the page background.",
  chrome: "Changes panels and cards.",
  digit: "Changes the numbers.",
  accent: "Changes highlights and selected controls.",
  danger: "Changes alerts and overtime.",
  glow: "Changes the halo around time.",
};

export const ROYGBIV = [
  { name: "Red", hex: "#E2566A" },
  { name: "Orange", hex: "#E07A2F" },
  { name: "Yellow", hex: "#E6C34A" },
  { name: "Green", hex: "#3D9B74" },
  { name: "Blue", hex: "#7FC6FF" },
  { name: "Indigo", hex: "#4B6FD6" },
  { name: "Violet", hex: "#8B6CFF" },
  { name: "White", hex: "#FFFFFF" },
  { name: "Black", hex: "#04060F" },
] as const;
