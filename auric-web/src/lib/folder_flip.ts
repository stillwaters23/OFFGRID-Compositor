/**
 * Compositor-only FLIP for folder layout changes.
 *
 * Layout is measured once, then each tile animates with translate3d and opacity.
 * Those are the only properties the compositor can move without a layout or
 * paint on later frames. Callers must finish every geometry read before the
 * first animation write so the two phases never interleave.
 */

export const FLIP_DURATION_MS = 320;
export const FLIP_EASING = "cubic-bezier(.2,.8,.2,1)";
export const FRAME_BUDGET_MS = 16.7;
export const FRAME_P95_BUDGET_MS = 25;

const RECORDED_KEYFRAME_FIELDS = new Set(["offset", "computedOffset", "easing", "composite"]);

export type BoxLike = {
  left: number;
  top: number;
  right: number;
  bottom: number;
};

export type FlipPlan = {
  id: string;
  x: number;
  y: number;
};

export type FlipKeyframe = {
  transform: string;
  opacity: number;
};

export type FrameIntervalStats = {
  count: number;
  median: number;
  p95: number;
  max: number;
};

export function isOutsideClip(box: BoxLike, clip: BoxLike): boolean {
  return box.right <= clip.left || box.left >= clip.right || box.bottom <= clip.top || box.top >= clip.bottom;
}

/**
 * Invert the tiles that actually moved and stayed inside the clip.
 * Off-screen folders are omitted so a layout change does not animate work
 * the user cannot see.
 */
export function planFolderFlips(
  first: ReadonlyMap<string, BoxLike>,
  last: ReadonlyMap<string, BoxLike>,
  clip: BoxLike,
): FlipPlan[] {
  const plans: FlipPlan[] = [];
  for (const [id, from] of first) {
    const to = last.get(id);
    if (!to || isOutsideClip(from, clip) || isOutsideClip(to, clip)) continue;
    const x = from.left - to.left;
    const y = from.top - to.top;
    if (x === 0 && y === 0) continue;
    plans.push({ id, x, y });
  }
  return plans;
}

export function flipKeyframes(x: number, y: number): [FlipKeyframe, FlipKeyframe] {
  return [
    { transform: `translate3d(${x}px, ${y}px, 0)`, opacity: 0.78 },
    { transform: "translate3d(0px, 0px, 0)", opacity: 1 },
  ];
}

export function animatedProperties(frame: object): string[] {
  return Object.keys(frame).filter((property) => !RECORDED_KEYFRAME_FIELDS.has(property));
}

export function usesCompositorPropertiesOnly(frames: readonly object[]): boolean {
  return frames.every((frame) => animatedProperties(frame).every((property) => property === "transform" || property === "opacity"));
}

/**
 * Same median / p95 selection the layout e2e uses, so a unit failure and a
 * browser failure describe the same budget.
 */
export function frameIntervalStats(samples: readonly number[]): FrameIntervalStats {
  if (samples.length === 0) return { count: 0, median: 0, p95: 0, max: 0 };
  const sorted = [...samples].sort((left, right) => left - right);
  return {
    count: sorted.length,
    median: sorted[Math.floor(sorted.length / 2)] ?? 0,
    p95: sorted[Math.max(0, Math.ceil(sorted.length * 0.95) - 1)] ?? 0,
    max: sorted[sorted.length - 1] ?? 0,
  };
}

/**
 * Mean must stay under a 60Hz tick. The median may sit on the 16.7ms quantum
 * Chromium uses for that same tick; a millionth of a millisecond covers the
 * double that sorts just above the literal 16.7. Dropped frames at 33ms fail.
 */
export function passesFrameBudget(stats: Pick<FrameIntervalStats, "median" | "p95"> & { mean?: number }): boolean {
  const meanOk = stats.mean === undefined || stats.mean < FRAME_BUDGET_MS;
  return meanOk && stats.median <= FRAME_BUDGET_MS + 0.001 && stats.p95 <= FRAME_P95_BUDGET_MS;
}

/**
 * A FLIP pass is one read group followed by one write group. Any geometry
 * read after the first write is layout thrash and will drop frames.
 */
export function isSingleReadWritePass(events: readonly ("read" | "write")[]): boolean {
  const firstWrite = events.indexOf("write");
  if (firstWrite === -1) return true;
  return events.slice(firstWrite).every((event) => event === "write");
}
