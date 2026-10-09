import type { Session } from "@/lib/types";

/** Accent at rest. Jewel climb. Red only when something is actually wrong. */
export function markColor(score: number, overtime = false): string {
  if (overtime) return "var(--danger)";
  const n = Math.max(0, Math.min(100, score));
  if (n <= 0) return "var(--accent)";
  if (n >= 98) return "#F7FBFF";
  const stops: [number, string][] = [
    [1, "#E0A03A"],
    [20, "#E6C34A"],
    [40, "#4CAF7A"],
    [60, "#3D8FD1"],
    [80, "#7FC6FF"],
    [97, "#EAF3FF"],
  ];
  let i = 0;
  while (i < stops.length - 1 && n > stops[i + 1]![0]) i += 1;
  return stops[Math.min(i, stops.length - 1)]![1];
}

export function cutScore(sessions: Session[], streak: number): number {
  const minutes = sessions.reduce((a, s) => a + s.durationMs, 0) / 60_000;
  const notes = sessions.filter((s) => s.note.trim().length > 0).length;
  const raw = sessions.length * 4 + minutes * 0.45 + notes * 6 + streak * 5;
  return Math.max(0, Math.min(100, Math.round(raw)));
}
