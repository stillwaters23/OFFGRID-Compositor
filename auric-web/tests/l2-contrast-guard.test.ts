import assert from "node:assert/strict";
import { test } from "node:test";
import { checkNumberColor, laserVars, resolveReelNumberColor, wheelBackgroundSamples } from "../src/lib/contrast-guard.ts";
import { committedPalette } from "../src/lib/appearance.ts";
import { BLUE_DIAMOND, CURATED } from "../src/lib/palettes.ts";
import type { Palette } from "../src/lib/types.ts";
import { relativeLuminance } from "../src/lib/color.ts";

function ctx(palette: Palette, laserColor = "#FF0000", laserIntensity = 0) {
  return { palette, laserColor, laserIntensity };
}

test("wheel samples are opaque colors and respond to palette surface changes", () => {
  const samples = wheelBackgroundSamples(BLUE_DIAMOND);
  assert.equal(samples.length, 7);
  assert.ok(samples.every((hex) => /^#[\da-f]{6}$/i.test(hex)));
  const expectedDark = ["#081226", "#03040B", "#081226", "#15223B", "#03040A", "#2B3952", "#020309"];
  samples.forEach((color, index) => assert.ok(channelDelta(color, expectedDark[index]!) <= 1));
  const light = committedPalette({
    paletteId: BLUE_DIAMOND.id,
    customPalette: null,
    mode: "light",
    scheduleEnabled: false,
    lightStart: "07:00",
    darkStart: "19:00",
    nowMs: 1,
  });
  const expectedLight = ["#ECE5D8", "#D0CBC4", "#ECE5D9", "#DAD2C5", "#C9C5BE", "#BEB7AB", "#BDB9B2"];
  wheelBackgroundSamples(light).forEach((color, index) => assert.ok(channelDelta(color, expectedLight[index]!) <= 1));
  const changed = wheelBackgroundSamples({ ...BLUE_DIAMOND, felt: "#FFFFFF", chrome: "#DDEEFF" });
  assert.notDeepEqual(changed, samples);
});

function channelDelta(first: string, second: string): number {
  return Math.max(...[1, 3, 5].map((i) => Math.abs(parseInt(first.slice(i, i + 2), 16) - parseInt(second.slice(i, i + 2), 16))));
}

test("number-color guard enforces the near-black rule and theme contrast", () => {
  for (const theme of ["dark", "light"] as const) {
    const black = checkNumberColor("#000000", theme, ctx(BLUE_DIAMOND));
    assert.equal(black.ok, false);
    assert.match(black.message, new RegExp(`${theme}.*\\d+\\.\\d{2}:1.*4\\.5:1`, "i"));
    assert.ok(black.ratio > 0);
  }
  assert.equal(checkNumberColor("#111111", "dark", ctx(BLUE_DIAMOND)).ok, false);
  assert.equal(checkNumberColor("#FFFFFF", "dark", ctx(BLUE_DIAMOND)).ok, true);
  assert.equal(checkNumberColor("#FFFFFF", "light", ctx({ ...BLUE_DIAMOND, ...{
    felt: "#F6F1E8", chrome: "#E8E0D2", digit: "#1A1712", light: true,
  } })).ok, false);
  const lightPalette = committedPalette({
    paletteId: BLUE_DIAMOND.id,
    customPalette: null,
    mode: "light",
    scheduleEnabled: false,
    lightStart: "07:00",
    darkStart: "19:00",
    nowMs: 1,
  });
  assert.equal(checkNumberColor("#1F2A44", "light", ctx(lightPalette)).ok, true);
  assert.equal(checkNumberColor("#1F2A44", "dark", ctx(BLUE_DIAMOND)).ok, false);
});

test("number-color contrast ratios stay pinned to two decimals", () => {
  const lightPalette = committedPalette({
    paletteId: BLUE_DIAMOND.id,
    customPalette: null,
    mode: "light",
    scheduleEnabled: false,
    lightStart: "07:00",
    darkStart: "19:00",
    nowMs: 1,
  });
  const cases = [
    ["dark", "#FFFFFF", "11.60"],
    ["dark", "#EAF3FF", "10.37"],
    ["dark", "#00E5FC", "7.52"],
    ["dark", "#0032FF", "1.60"],
    ["dark", "#FF00A6", "3.21"],
    ["dark", "#1F2A44", "1.11"],
    ["light", "#FFFFFF", "1.25"],
    ["light", "#EAF3FF", "1.12"],
    ["light", "#00E5FC", "1.03"],
    ["light", "#0032FF", "3.64"],
    ["light", "#FF00A6", "1.82"],
    ["light", "#1F2A44", "7.16"],
  ] as const;
  for (const [theme, color, ratio] of cases) {
    const palette = theme === "dark" ? BLUE_DIAMOND : lightPalette;
    assert.equal(checkNumberColor(color, theme, ctx(palette)).ratio.toFixed(2), ratio, `${theme} ${color}`);
  }
});

test("laser bloom is considered when checking selected-row numeral contrast", () => {
  const palette = { ...BLUE_DIAMOND, felt: "#050505", chrome: "#080808", digit: "#FFFFFF" };
  const plain = checkNumberColor("#BBBBBB", "dark", ctx(palette));
  const bloom = checkNumberColor("#BBBBBB", "dark", ctx(palette, "#FFFFFF", 100));
  assert.equal(plain.ok, true);
  assert.equal(bloom.ok, false);
  assert.match(bloom.message, /laser bloom/);
});

test("invalid and translucent number colors are refused", () => {
  assert.equal(checkNumberColor("#GGG", "dark", ctx(BLUE_DIAMOND)).ok, false);
  assert.equal(checkNumberColor("#FFFFFF80", "dark", ctx(BLUE_DIAMOND)).ok, false);
});

test("laser intensity maps linearly and disabling removes its beam opacity", () => {
  assert.deepEqual(laserVars("#FF00A6", 0, true), {
    "--laser-color": "#FF00A6",
    "--laser-enabled": "1",
    "--laser-core-opacity": "0.2",
    "--laser-bloom-opacity": "0",
  });
  assert.equal(laserVars("#FF0000", 50, true)["--laser-core-opacity"], "0.6");
  assert.equal(laserVars("#FF0000", 50, true)["--laser-bloom-opacity"], "0.28");
  assert.equal(laserVars("#FF0000", 100, true)["--laser-core-opacity"], "1");
  assert.equal(laserVars("#FF0000", 100, true)["--laser-bloom-opacity"], "0.55");
  assert.equal(laserVars("#FF0000", 100, false)["--laser-core-opacity"], "0");
});

test("resolved reel numerals stay guard-approved and away from near-black for curated palettes", () => {
  for (const source of CURATED) {
    for (const theme of ["dark", "light"] as const) {
      const palette = committedPalette({
        paletteId: source.id,
        customPalette: null,
        mode: theme,
        scheduleEnabled: false,
        lightStart: "07:00",
        darkStart: "19:00",
        nowMs: 1,
      });
      const context = { palette, laserColor: "#FF0000", laserIntensity: 100 };
      const color = resolveReelNumberColor(palette, null, theme, context);
      assert.notEqual(color, "#000000");
      assert.ok(relativeLuminance(color) >= 0.012, `${source.id}/${theme} resolved to ${color}`);
      assert.equal(checkNumberColor(color, theme, context).ok, true, `${source.id}/${theme} resolved to ${color}`);
    }
  }
});
