import { TICK_GRADES } from "@/lib/ticks";

export const CONTROL_ENGINES = ["drum", "roulette", "ring", "chip", "flip", "dice", "fan"] as const;
export type ControlEngine = (typeof CONTROL_ENGINES)[number];

export type ControlCut = {
  id: string;
  name: string;
  short: string;
  engine: ControlEngine;
  grade: string;
  premium: boolean;
};

export const CONTROLS: ControlCut[] = CONTROL_ENGINES.flatMap((engine, ei) =>
  TICK_GRADES.map((grade, gi) => ({
    id: `${engine}-${grade.toLowerCase()}`,
    name: `${engine[0]!.toUpperCase()}${engine.slice(1)} ${grade}`,
    short: `${engine.slice(0, 4)} ${grade.slice(0, 5)}`,
    engine,
    grade,
    premium: !(ei === 0 && gi === 0),
  })),
);

export const HOUSE_CONTROL_ID = "drum-whisper";

export function controlById(id: string | null | undefined): ControlCut {
  return CONTROLS.find((c) => c.id === id) ?? CONTROLS[0]!;
}
