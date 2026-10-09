import { contrastRatio, hexToRgb, isValidHex, normalizeHex, relativeLuminance } from "@/lib/color";
import type { Palette } from "@/lib/types";

export type ContrastTheme = "dark" | "light";
export type NumberColorContext = {
  palette: Palette;
  laserColor: string;
  laserIntensity: number;
  laserEnabled?: boolean;
};

function linearToOklab(hex: string): [number, number, number] {
  const rgb = hexToRgb(hex)!;
  const linear = (value: number) => {
    const channel = value / 255;
    return channel <= 0.04045 ? channel / 12.92 : ((channel + 0.055) / 1.055) ** 2.4;
  };
  const r = linear(rgb.r);
  const g = linear(rgb.g);
  const b = linear(rgb.b);
  const l = Math.cbrt(0.4122214708 * r + 0.5363325363 * g + 0.0514459929 * b);
  const m = Math.cbrt(0.2119034982 * r + 0.6806995451 * g + 0.1073969566 * b);
  const s = Math.cbrt(0.0883024619 * r + 0.2817188376 * g + 0.6299787005 * b);
  return [
    0.2104542553 * l + 0.793617785 * m - 0.0040720468 * s,
    1.9779984951 * l - 2.428592205 * m + 0.4505937099 * s,
    0.0259040371 * l + 0.7827717662 * m - 0.808675766 * s,
  ];
}

function oklabToHex([L, a, b]: [number, number, number]): string {
  const l = (L + 0.3963377774 * a + 0.2158037573 * b) ** 3;
  const m = (L - 0.1055613458 * a - 0.0638541728 * b) ** 3;
  const s = (L - 0.0894841775 * a - 1.291485548 * b) ** 3;
  const channels = [
    4.0767416621 * l - 3.3077115913 * m + 0.2309699292 * s,
    -1.2684380046 * l + 2.6097574011 * m - 0.3413193965 * s,
    -0.0041960863 * l - 0.7034186147 * m + 1.707614701 * s,
  ].map((value) => {
    const srgb = value <= 0.0031308 ? 12.92 * value : 1.055 * value ** (1 / 2.4) - 0.055;
    return Math.max(0, Math.min(255, Math.round(srgb * 255)));
  });
  return `#${channels.map((value) => value.toString(16).padStart(2, "0")).join("")}`.toUpperCase();
}

function mixOklab(a: string, b: string, weightA: number): string {
  const first = linearToOklab(a);
  const second = linearToOklab(b);
  const weightB = 1 - weightA;
  return oklabToHex([
    first[0] * weightA + second[0] * weightB,
    first[1] * weightA + second[1] * weightB,
    first[2] * weightA + second[2] * weightB,
  ]);
}

function compositeOn(hex: string, backdrop: string, alpha: number): string {
  const foreground = hexToRgb(hex);
  const background = hexToRgb(backdrop);
  if (!foreground || !background) return backdrop;
  return mixOklab(
    `#${[foreground.r, foreground.g, foreground.b].map((n) => n.toString(16).padStart(2, "0")).join("")}`,
    `#${[background.r, background.g, background.b].map((n) => n.toString(16).padStart(2, "0")).join("")}`,
    alpha,
  );
}

function opaqueHex(value: string): string | null {
  if (!isValidHex(value)) return null;
  const rgb = hexToRgb(value);
  return rgb?.a === 1 ? normalizeHex(value) : null;
}

/**
 * Samples the opaque stops that sit behind the selected aperture and readout.
 * Mirrors `src/styles.css` .reel-console background (lines 371-373) and
 * .barrel background (lines 501-504); CSS color-mix is performed in oklab.
 */
export function wheelBackgroundSamples(palette: Palette): string[] {
  const black = "#000000";
  return [
    mixOklab(palette.chrome, palette.felt, 0.72),
    mixOklab(palette.felt, black, 0.88),
    mixOklab(palette.chrome, palette.felt, 0.7),
    mixOklab(palette.chrome, palette.digit, 0.94),
    mixOklab(palette.felt, black, 0.86),
    mixOklab(palette.chrome, palette.digit, 0.82),
    mixOklab(palette.felt, black, 0.82),
  ];
}

export function laserBloomComposite(bg: string, laserColor: string, intensity: number): string {
  const laser = opaqueHex(laserColor);
  if (!laser || !Number.isFinite(intensity) || intensity <= 0) return bg;
  return compositeOn(laser, bg, Math.min(1, intensity / 100) * 0.36);
}

export function laserVars(laserColor: string, intensity: number, enabled: boolean) {
  const color = opaqueHex(laserColor) ?? "#FF0000";
  const percent = Number.isFinite(intensity) ? Math.max(0, Math.min(100, intensity)) : 100;
  return {
    "--laser-color": color,
    "--laser-enabled": enabled ? "1" : "0",
    "--laser-core-opacity": enabled ? String(Number((0.2 + 0.8 * (percent / 100)).toFixed(2))) : "0",
    "--laser-bloom-opacity": enabled ? String(Number((0.55 * (percent / 100)).toFixed(2))) : "0",
  };
}

export type NumberColorCheck = {
  ok: boolean;
  ratio: number;
  worstAgainst: string;
  message: string;
};

export function checkNumberColor(
  hex: string,
  theme: ContrastTheme,
  ctx: NumberColorContext,
): NumberColorCheck {
  const color = opaqueHex(hex);
  const themeName = theme === "dark" ? "dark" : "light";
  if (!color) {
    return { ok: false, ratio: 0, worstAgainst: "color format", message: "Use a valid opaque #RGB or #RRGGBB color." };
  }
  if (relativeLuminance(color) < 0.012) {
    return {
      ok: false,
      ratio: 0,
      worstAgainst: "near-black rule",
      message: `${color} is too dark on the ${themeName} wheel: below the near-black limit, needs 4.5:1. Pick a lighter color.`,
    };
  }

  const backgrounds = wheelBackgroundSamples(ctx.palette);
  const samples = backgrounds.map((bg, index) => ({ bg, label: `wheel surface ${index + 1}` }));
  if (ctx.laserEnabled !== false && ctx.laserIntensity > 0) {
    backgrounds.forEach((bg, index) =>
      samples.push({
        bg: laserBloomComposite(bg, ctx.laserColor, ctx.laserIntensity),
        label: `laser bloom ${index + 1}`,
      }),
    );
  }
  let worst = { ratio: Infinity, label: "" };
  for (const sample of samples) {
    const ratio = contrastRatio(color, sample.bg);
    if (ratio < worst.ratio) worst = { ratio, label: sample.label };
  }
  const ratio = worst.ratio;
  const ok = ratio >= 4.5;
  const qualifier = worst.label.startsWith("laser") ? "against the laser bloom" : `on the ${themeName} wheel`;
  const message = ok
    ? `${color} on the ${themeName} wheel: ${ratio.toFixed(2)}:1 contrast passes.`
    : `${color} does not pass ${qualifier}: ${ratio.toFixed(2)}:1, needs 4.5:1. Pick a lighter color.`;
  return { ok, ratio, worstAgainst: worst.label, message };
}

export function resolveReelNumberColor(
  palette: Palette,
  override: string | null,
  theme: ContrastTheme,
  ctx: NumberColorContext,
): string {
  const custom = override ? opaqueHex(override) : null;
  if (custom && checkNumberColor(custom, theme, ctx).ok) return custom;
  const candidates = [
    palette.digit,
    "#FFFFFF",
    "#EAF3FF",
    "#B8D5F2",
    "#40372D",
    "#332A22",
    "#4A4034",
    "#645744",
  ];
  const valid = candidates
    .map(opaqueHex)
    .filter((value): value is string => Boolean(value) && relativeLuminance(value!) >= 0.012);
  return valid.find((color) => checkNumberColor(color, theme, ctx).ok)
    ?? valid.sort((a, b) => checkNumberColor(b, theme, ctx).ratio - checkNumberColor(a, theme, ctx).ratio)[0]
    ?? "#FFFFFF";
}
