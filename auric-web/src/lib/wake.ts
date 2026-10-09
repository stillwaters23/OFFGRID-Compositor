export const WAKE_FAMILIES = ["Siren", "Klaxon", "Piezo", "Bell", "Horn", "Sweep", "Pulse"] as const;
export const WAKE_PER_FAMILY = 77;
export const WAKE_GRADES = Array.from({ length: WAKE_PER_FAMILY }, (_, i) =>
  String(i + 1).padStart(2, "0"),
);

export type WakeKind = "sine" | "square" | "triangle" | "sawtooth";

export type WakeRecipe = {
  silent?: boolean;
  type: WakeKind;
  f0: number;
  f1: number;
  dur: number;
  gain: number;
  gap: number;
  filter?: "hp" | "lp" | "bp";
  ff?: number;
  layer?: { type: WakeKind; f0: number; f1: number; dur: number; gain: number; delay: number };
};

export type WakeCut = {
  id: string;
  name: string;
  short: string;
  family: (typeof WAKE_FAMILIES)[number];
  grade: string;
  premium: boolean;
  recipe: WakeRecipe;
};

/**
 * 77 unique wakes per style.
 * Piezo ≈ Seiko 4096 Hz watch beep. Bell ≈ twin-hammer wall clock.
 * Klaxon ≈ factory horn. Sweep ≈ Braun crescendo. Pulse ≈ Clocky chirp.
 */
function recipeFor(fi: number, gi: number): WakeRecipe {
  const t = gi / 76;
  const loud = 0.72 + t * 0.2;

  if (fi === 0) {
    const f0 = 480 + gi * 18;
    return {
      type: "sawtooth",
      f0,
      f1: f0 * (2.4 + (gi % 5) * 0.2),
      dur: 0.42 + (gi % 8) * 0.04,
      gain: loud,
      gap: 0.05 + (gi % 6) * 0.01,
      filter: "bp",
      ff: 800 + gi * 14,
      layer: { type: "square", f0: 160 + (gi % 9) * 12, f1: 380, dur: 0.4, gain: 0.38, delay: 0 },
    };
  }
  if (fi === 1) {
    const f0 = 320 + gi * 11;
    return {
      type: "square",
      f0,
      f1: f0 * (1.6 + (gi % 4) * 0.15),
      dur: 0.16 + (gi % 7) * 0.02,
      gain: loud + 0.04,
      gap: 0.04 + (gi % 5) * 0.008,
      filter: "hp",
      ff: 180,
      layer: { type: "square", f0: f0 * 2, f1: f0 * 2, dur: 0.14, gain: 0.42, delay: 0 },
    };
  }
  if (fi === 2) {
    const piezo = [4096, 2048, 1024, 3072, 2560][gi % 5]!;
    const f0 = piezo + (gi % 11) * 8;
    return {
      type: "square",
      f0,
      f1: f0,
      dur: 0.07 + (gi % 6) * 0.012,
      gain: loud + 0.08,
      gap: 0.04 + (gi % 4) * 0.01,
      layer: { type: "square", f0: 60, f1: 60, dur: 0.07, gain: 0.3, delay: 0 },
    };
  }
  if (fi === 3) {
    const a = 1120 + gi * 7;
    const b = 1410 + gi * 8;
    return {
      type: "square",
      f0: a,
      f1: a,
      dur: 0.048 + (gi % 5) * 0.006,
      gain: loud + 0.06,
      gap: 0.038 + (gi % 6) * 0.006,
      layer: { type: "square", f0: b, f1: b, dur: 0.048, gain: 0.58, delay: 0.01 + (gi % 3) * 0.004 },
    };
  }
  if (fi === 4) {
    return {
      type: "sawtooth",
      f0: 110 + gi * 4,
      f1: 70 + (gi % 8) * 3,
      dur: 0.55 + (gi % 9) * 0.03,
      gain: loud,
      gap: 0.1,
      filter: "lp",
      ff: 600 + gi * 6,
      layer: { type: "square", f0: 60, f1: 60, dur: 0.55, gain: 0.34, delay: 0 },
    };
  }
  if (fi === 5) {
    const f0 = 620 + gi * 16;
    return {
      type: "square",
      f0,
      f1: f0 * (3.2 + (gi % 4) * 0.4),
      dur: 0.32 + t * 0.2,
      gain: loud,
      gap: 0.05,
      layer: { type: "sawtooth", f0: 200 + gi * 5, f1: 900 + gi * 8, dur: 0.3, gain: 0.32, delay: 0 },
    };
  }
  const f0 = 1800 + gi * 22;
  return {
    type: "square",
    f0,
    f1: f0 + (gi % 2 === 0 ? 400 : -200),
    dur: 0.035 + (gi % 8) * 0.006,
    gain: loud + 0.08,
    gap: 0.028 + (gi % 5) * 0.006,
    layer: { type: "square", f0: 3200 + (gi % 9) * 40, f1: 3200, dur: 0.03, gain: 0.36, delay: 0.016 },
  };
}

export const WAKES: WakeCut[] = WAKE_FAMILIES.flatMap((family, fi) =>
  WAKE_GRADES.map((grade, gi) => ({
    id: `${family.toLowerCase()}-${grade}`,
    name: `${family} ${grade}`,
    short: grade,
    family,
    grade,
    premium: gi >= 50,
    recipe: recipeFor(fi, gi),
  })),
);

export const HOUSE_WAKE_ID = "bell-12";

export function wakeById(id: string | null | undefined): WakeCut {
  const mapped =
    id === "bell-storm" || id === "klaxon-storm" || id === "piezo-chamber" ? HOUSE_WAKE_ID : id;
  return WAKES.find((w) => w.id === mapped) ?? WAKES.find((w) => w.id === HOUSE_WAKE_ID) ?? WAKES[0]!;
}
