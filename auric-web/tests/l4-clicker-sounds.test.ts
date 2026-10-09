import assert from "node:assert/strict";
import { test } from "node:test";
import { useAuric } from "../src/lib/store.ts";
import { MODULES } from "../src/lib/modules.ts";
import { HOUSE_CLICK_ID, TICK_FAMILIES, TICK_PER_FAMILY, TICKS, tickById } from "../src/lib/ticks.ts";

test("Clicker Sounds is free, uses the existing pulses glyph, and preserves the module order", () => {
  const ids = MODULES.map((module) => module.id);
  const module = MODULES.find((item) => item.id === "clicker-sounds");
  assert.deepEqual(ids.filter((id) => id !== "clicker-sounds"), [
    "timer", "stopwatch", "alarm", "themes", "sizing", "opacity", "icons", "alarm-sounds",
    "timer-sounds", "haptics", "heartbeat", "heartbeat-log", "logs-exports", "device", "tutorial",
  ]);
  assert.deepEqual(
    { name: module?.name, pkg: module?.pkg, glyph: module?.glyph, tier: module?.tier },
    { name: "Clicker Sounds", pkg: "clicker-sounds", glyph: "pulses", tier: "free" },
  );
});

test("the seven tick families expose 77 ordered cuts apiece", () => {
  assert.deepEqual(TICK_FAMILIES, ["Click", "Ice", "Crystal", "Silk", "Gear", "Bolt", "Paper"]);
  assert.equal(TICKS.length, 539);
  for (const family of TICK_FAMILIES) {
    const cuts = TICKS.filter((tick) => tick.family === family);
    assert.equal(cuts.length, TICK_PER_FAMILY);
    assert.deepEqual(cuts.map((tick) => tick.id), Array.from({ length: 77 }, (_, index) => `${family.toLowerCase()}-${String(index + 1).padStart(2, "0")}`));
  }
});

test("the house click is the default selection and legacy tick ids still resolve", () => {
  assert.equal(HOUSE_CLICK_ID, "click-01");
  assert.equal(useAuric.getInitialState().reelSound, HOUSE_CLICK_ID);
  for (const [legacy, current] of [["chip", "click-01"], ["ice", "ice-01"], ["tumbler", "bolt-02"], ["silent", "click-01"]]) {
    assert.equal(tickById(legacy).id, current);
  }
});
