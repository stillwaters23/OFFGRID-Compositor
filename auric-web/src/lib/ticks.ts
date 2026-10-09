export const TICK_FAMILIES = ["Click", "Ice", "Crystal", "Silk", "Gear", "Bolt", "Paper"] as const;
export const TICK_PER_FAMILY = 77;
export const TICK_GRADES = Array.from({ length: TICK_PER_FAMILY }, (_, i) =>
  String(i + 1).padStart(2, "0"),
);

export type OscKind = "sine" | "square" | "triangle" | "sawtooth";

export type TickRecipe = {
  silent?: boolean;
  type: OscKind;
  f0: number;
  f1: number;
  dur: number;
  gain: number;
  filter?: "hp" | "lp" | "bp";
  ff?: number;
  layer?: { type: OscKind; f0: number; f1: number; dur: number; gain: number; delay: number };
};

export type TickCut = {
  id: string;
  name: string;
  short: string;
  family: (typeof TICK_FAMILIES)[number];
  grade: string;
  premium: boolean;
  recipe: TickRecipe;
};

/** 77 unique clicks per style. Index 0 of Click is the original zip house click. */
function recipeFor(fi: number, gi: number): TickRecipe {
  if (fi === 0 && gi === 0) {
    return { type: "square", f0: 1640, f1: 1640, dur: 0.035, gain: 0.018 };
  }
  if (fi === 1 && gi === 0) {
    return { type: "sine", f0: 880, f1: 540, dur: 0.07, gain: 0.02, filter: "lp", ff: 1600 };
  }

  const t = gi / 76;
  const types: OscKind[] = ["square", "sine", "triangle", "sawtooth"];
  if (fi === 0) {
    const f0 = 1320 + gi * 17 + (gi % 5) * 9;
    return {
      type: "square",
      f0,
      f1: f0 * (0.97 - (gi % 7) * 0.008),
      dur: 0.028 + (gi % 9) * 0.003,
      gain: 0.016 + t * 0.014,
      filter: "hp",
      ff: 520 + gi * 11,
    };
  }
  if (fi === 1) {
    const f0 = 540 + gi * 14;
    return {
      type: "triangle",
      f0,
      f1: f0 * (0.48 + (gi % 4) * 0.04),
      dur: 0.05 + t * 0.06,
      gain: 0.018 + t * 0.014,
      filter: "lp",
      ff: 1200 + gi * 18,
    };
  }
  if (fi === 2) {
    const f0 = 1680 + gi * 22;
    return {
      type: "sine",
      f0,
      f1: f0 * (1.08 + (gi % 6) * 0.03),
      dur: 0.045 + t * 0.05,
      gain: 0.014 + t * 0.014,
      layer: gi % 4 === 3 ? { type: "triangle", f0: f0 * 1.5, f1: f0, dur: 0.08, gain: 0.01, delay: 0.015 } : undefined,
    };
  }
  if (fi === 3) {
    const f0 = 180 + gi * 9;
    return {
      type: "sine",
      f0,
      f1: f0 * 0.68,
      dur: 0.08 + t * 0.07,
      gain: 0.02 + t * 0.014,
      filter: "lp",
      ff: 400 + gi * 12,
    };
  }
  if (fi === 4) {
    const f0 = 160 + gi * 8;
    return {
      type: "sawtooth",
      f0,
      f1: f0 * (0.75 + (gi % 5) * 0.03),
      dur: 0.038 + t * 0.03,
      gain: 0.014 + t * 0.012,
      filter: "bp",
      ff: 360 + gi * 14,
    };
  }
  if (fi === 5) {
    const f0 = 64 + gi * 6;
    return {
      type: "square",
      f0,
      f1: f0 * 0.55,
      dur: 0.06 + t * 0.05,
      gain: 0.024 + t * 0.016,
      filter: "lp",
      ff: 220 + gi * 8,
    };
  }
  const f0 = 980 + gi * 16;
  return {
    type: types[gi % 4]!,
    f0,
    f1: f0 * (0.4 + (gi % 8) * 0.05),
    dur: 0.034 + t * 0.03,
    gain: 0.013 + t * 0.012,
    filter: "hp",
    ff: 640 + gi * 10,
  };
}

export const TICKS: TickCut[] = TICK_FAMILIES.flatMap((family, fi) =>
  TICK_GRADES.map((grade, gi) => ({
    id: `${family.toLowerCase()}-${grade}`,
    name: `${family} ${grade}`,
    short: grade,
    family,
    grade,
    premium: gi >= 50,
    recipe: recipeFor(fi, gi),
  })),
);

export const HOUSE_CLICK_ID = "click-01";
export const ICE_WHISPER_ID = "ice-01";

const LEGACY: Record<string, string> = {
  chip: HOUSE_CLICK_ID,
  ice: ICE_WHISPER_ID,
  "click-whisper": HOUSE_CLICK_ID,
  "ice-whisper": ICE_WHISPER_ID,
  crystal: "crystal-01",
  silk: "silk-01",
  gear: "gear-01",
  bolt: "bolt-01",
  tumbler: "bolt-02",
  tick: "silk-01",
  silent: HOUSE_CLICK_ID,
};

export function tickById(id: string | null | undefined): TickCut {
  const mapped = LEGACY[id ?? ""] ?? id ?? HOUSE_CLICK_ID;
  return TICKS.find((t) => t.id === mapped) ?? TICKS[0]!;
}

export function ticksForLiteracy(_index: number): TickCut[] {
  return TICKS;
}

export function verifyTicks(): string[] {
  const errors: string[] = [];
  const need = TICK_FAMILIES.length * TICK_PER_FAMILY;
  if (TICKS.length !== need) errors.push(`count ${TICKS.length} !== ${need}`);
  const ids = new Set(TICKS.map((t) => t.id));
  if (ids.size !== need) errors.push("duplicate ids");
  const house = TICKS[0]!;
  if (house.id !== HOUSE_CLICK_ID) errors.push("index 0 is not house click");
  if (house.recipe.type !== "square" || house.recipe.f0 !== 1640 || house.recipe.dur !== 0.035 || house.recipe.gain !== 0.018) {
    errors.push("house click does not match zip playClick");
  }
  return errors;
}
