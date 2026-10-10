import assert from "node:assert/strict";
import { test } from "node:test";
import { clampToSafeArea, springStep } from "../src/lib/folder_physics.ts";

test("critical spring settles within 500ms without crossing its target", () => {
  let spring = { position: 120, velocity: 0, target: 0 };
  let elapsed = 0;
  while (elapsed < 500 && Math.abs(spring.position) > 0.5) {
    spring = springStep(spring, 16);
    elapsed += 16;
    assert.ok(spring.position >= 0);
  }
  assert.ok(elapsed < 500);
  assert.ok(Math.abs(spring.position) < 0.5);
});

test("safe-area clamp honors viewport edges and simulated insets", () => {
  const viewport = { width: 320, height: 640 };
  for (const inset of [0, 44]) {
    const rect = clampToSafeArea(
      { x: -40, y: 600, width: 80, height: 72 },
      viewport,
      { top: inset, right: inset, bottom: inset, left: inset },
    );
    assert.ok(rect.x >= inset + 8);
    assert.ok(rect.y >= inset + 8);
    assert.ok(rect.x + rect.width <= viewport.width - inset - 8);
    assert.ok(rect.y + rect.height <= viewport.height - inset - 8);
  }
});
