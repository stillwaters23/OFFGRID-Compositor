import assert from "node:assert/strict";
import test from "node:test";
import {
  LASER_DEFAULT,
  LASER_PRESETS,
  laserBloomComposite,
  laserVariables,
} from "../src/lib/laser.ts";
import { reelGeometry } from "../src/lib/reel-geometry.ts";

test("laser defaults are enabled at full intensity with the default red", () => {
  assert.equal(LASER_DEFAULT, "#f0243e");
  assert.deepEqual(laserVariables(), {
    "--laser-color": LASER_DEFAULT,
    "--laser-core": "#ffffff",
    "--laser-bloom-a": "36%",
    "--laser-spread": "48px",
    "--laser-on": "1",
  });
});

test("laser intensity clamps and all five presets are available", () => {
  assert.equal(LASER_PRESETS.length, 5);
  assert.equal(laserVariables("#27a9ff", 0)["--laser-on"], "0");
  assert.equal(laserVariables("#27a9ff", 50)["--laser-bloom-a"], "18%");
  assert.equal(laserVariables("#27a9ff", 500)["--laser-bloom-a"], "36%");
});

test("laser bloom composites to a deterministic color", () => {
  assert.equal(laserBloomComposite("#ff0000", 0.5, "#000000"), "#800000");
  assert.equal(laserBloomComposite("invalid", 1), "#000000");
});

test("reel geometry fits available width and centers the selected row", () => {
  const geometry = reelGeometry({
    width: 72,
    height: 120,
    baseFont: 18,
    longest: 3,
    digitScale: 1.3,
  });
  assert.ok(geometry.fontSize * 3.3 <= 64);
  assert.equal(geometry.rowCenter, 60);
  assert.ok(geometry.radius > 0);
  assert.ok(geometry.rowPitch <= 120 * 0.28);
});

test("reel geometry handles zero scale and zero-sized measurements", () => {
  assert.deepEqual(
    reelGeometry({
      width: 0,
      height: 0,
      baseFont: 30,
      longest: 2,
      digitScale: 0,
    }),
    { fontSize: 0, rowPitch: 0, radius: 0, rowCenter: 0 },
  );
});
