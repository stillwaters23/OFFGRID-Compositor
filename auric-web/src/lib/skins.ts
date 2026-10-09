import { hsvToHex } from "@/lib/color";
import { TICK_GRADES } from "@/lib/ticks";
import type { Palette } from "@/lib/types";

/**
 * Jewel houses — real stones, not pastel tints.
 * Abyss = deep sea. Sapphire = Hope-cut blue. Ruby/Emerald/Amethyst = winter jewels.
 * Onyx = black diamond. Pearl = ivory hall.
 */
const JEWELS = [
  { id: "abyss", name: "Abyss", h: 222, light: false, mute: false },
  { id: "sapphire", name: "Sapphire", h: 214, light: false, mute: false },
  { id: "ruby", name: "Ruby", h: 352, light: false, mute: false },
  { id: "emerald", name: "Emerald", h: 156, light: false, mute: false },
  { id: "amethyst", name: "Amethyst", h: 278, light: false, mute: false },
  { id: "onyx", name: "Onyx", h: 228, light: false, mute: true },
  { id: "pearl", name: "Pearl", h: 38, light: true, mute: false },
] as const;

export const SKINS: Palette[] = JEWELS.flatMap((fam, fi) =>
  TICK_GRADES.map((grade, gi) => {
    const t = gi / 10;
    if (fam.light) {
      return {
        id: `${fam.id}-${grade.toLowerCase()}`,
        name: `${fam.name} ${grade}`,
        premium: true,
        felt: hsvToHex({ h: fam.h, s: 6 + t * 4, v: 94 - t * 6, a: 100 }),
        chrome: hsvToHex({ h: fam.h, s: 10 + t * 6, v: 88 - t * 6, a: 100 }),
        digit: hsvToHex({ h: fam.h, s: 28, v: 12, a: 100 }),
        accent: hsvToHex({ h: fam.h, s: 42 + t * 18, v: 38 + t * 8, a: 100 }),
        glow: hsvToHex({ h: fam.h, s: 24, v: 72, a: 100 }),
        danger: hsvToHex({ h: 0, s: 62, v: 48, a: 100 }),
        light: true,
      };
    }
    const sat = fam.mute ? 10 + t * 6 : 58 + t * 12;
    return {
      id: `${fam.id}-${grade.toLowerCase()}`,
      name: `${fam.name} ${grade}`,
      premium: !(fi === 0 && gi === 0),
      felt: hsvToHex({ h: fam.h, s: fam.mute ? 18 : 64 + t * 8, v: 5 + t * 2, a: 100 }),
      chrome: hsvToHex({ h: fam.h, s: fam.mute ? 14 : 52, v: 11 + t * 3, a: 100 }),
      digit: hsvToHex({ h: fam.h, s: fam.mute ? 4 : 10, v: 95 - t * 3, a: 100 }),
      accent: hsvToHex({ h: fam.h, s: fam.mute ? 8 + t * 6 : sat, v: fam.mute ? 72 + t * 6 : 46 + t * 10, a: 100 }),
      glow: hsvToHex({ h: fam.h, s: fam.mute ? 12 : 48 + t * 10, v: 70 + t * 8, a: 100 }),
      danger: hsvToHex({ h: 352, s: 68, v: 62, a: 100 }),
    };
  }),
);

export const HOUSE_SKIN_ID = "abyss-whisper";

export function skinById(id: string | null | undefined): Palette {
  return SKINS.find((s) => s.id === id) ?? SKINS[0]!;
}
