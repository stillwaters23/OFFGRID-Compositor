import assert from "node:assert/strict";
import { test } from "node:test";
import { classifyGesture, shouldSuppressClick } from "../src/lib/folder_gestures.ts";

const start = { type: "start" as const, x: 10, y: 10, time: 0 };

test("gesture classifier separates taps, long-presses, and early drags", () => {
  assert.equal(classifyGesture([start, { type: "end", x: 10, y: 10, time: 499 }]), "tap");
  assert.equal(classifyGesture([start, { type: "end", x: 10, y: 10, time: 500 }]), "long-press");
  assert.equal(classifyGesture([start, { type: "move", x: 18, y: 10, time: 120 }]), "drag");
  assert.equal(classifyGesture([]), "cancel");
  assert.equal(shouldSuppressClick("long-press"), true);
  assert.equal(shouldSuppressClick("drag"), true);
  assert.equal(shouldSuppressClick("tap"), false);
});
