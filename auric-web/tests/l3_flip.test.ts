import assert from "node:assert/strict";
import { test } from "node:test";
import {
  animatedProperties,
  FLIP_DURATION_MS,
  frameIntervalStats,
  isOutsideClip,
  isSingleReadWritePass,
  passesFrameBudget,
  planFolderFlips,
  flipKeyframes,
  usesCompositorPropertiesOnly,
} from "../src/lib/folder_flip.ts";

const viewport = { left: 0, top: 0, right: 390, bottom: 844 };

test("folder FLIP plans invert only visible tiles that moved", () => {
  const first = new Map([
    ["timer", { left: 16, top: 200, right: 374, bottom: 272 }],
    ["alarm", { left: 16, top: 210, right: 374, bottom: 282 }],
    ["hidden", { left: 16, top: 900, right: 374, bottom: 972 }],
    ["still", { left: 16, top: 300, right: 374, bottom: 372 }],
  ]);
  const last = new Map([
    ["timer", { left: 16, top: 120, right: 190, bottom: 210 }],
    ["alarm", { left: 200, top: 120, right: 374, bottom: 210 }],
    ["hidden", { left: 16, top: 920, right: 374, bottom: 992 }],
    ["still", { left: 16, top: 300, right: 374, bottom: 372 }],
    ["fresh", { left: 16, top: 400, right: 374, bottom: 472 }],
  ]);

  assert.deepEqual(planFolderFlips(first, last, viewport), [
    { id: "timer", x: 0, y: 80 },
    { id: "alarm", x: -184, y: 90 },
  ]);
  assert.equal(isOutsideClip(first.get("hidden")!, viewport), true);
});

test("folder FLIP keyframes animate transform and opacity only", () => {
  const frames = flipKeyframes(12.5, -8);
  assert.equal(FLIP_DURATION_MS, 320);
  assert.equal(usesCompositorPropertiesOnly(frames), true);
  assert.deepEqual(animatedProperties(frames[0]), ["transform", "opacity"]);
  assert.equal(frames[0].transform, "translate3d(12.5px, -8px, 0)");
  assert.equal(frames[1].transform, "translate3d(0px, 0px, 0)");
  assert.equal(frames[0].opacity, 0.78);
  assert.equal(frames[1].opacity, 1);
  assert.equal(frames.some((frame) => "width" in frame || "top" in frame || "filter" in frame), false);
});

test("frame budget matches the 60fps gate and rejects the recorded miss", () => {
  const recordedMiss = [16.6, 33.3, 33.3, 33.3, 50, 116.7, 33.3, 16.8];
  const miss = frameIntervalStats(recordedMiss);
  assert.equal(miss.median, 33.3);
  assert.equal(miss.p95, 116.7);
  assert.equal(passesFrameBudget(miss), false);

  const steady = Array.from({ length: 19 }, () => 16.6);
  const hit = frameIntervalStats(steady);
  assert.ok(hit.median < 16.7);
  assert.ok(hit.p95 <= 25);
  assert.equal(passesFrameBudget({ ...hit, mean: 16.66 }), true);
  assert.equal(passesFrameBudget({ median: 16.700000000000045, p95: 16.8, mean: 16.666 }), true);
  assert.equal(passesFrameBudget({ median: 16.8, p95: 16.8, mean: 16.66 }), false);
  assert.equal(passesFrameBudget({ median: 16.7, p95: 16.8, mean: 16.7 }), false);
  assert.deepEqual(frameIntervalStats([]), { count: 0, median: 0, p95: 0, max: 0 });
});

test("FLIP measurement is one read pass followed by writes", () => {
  assert.equal(isSingleReadWritePass(["read", "read", "write", "write"]), true);
  assert.equal(isSingleReadWritePass(["write", "read"]), false);
  assert.equal(isSingleReadWritePass(["read", "write", "read", "write"]), false);
});
