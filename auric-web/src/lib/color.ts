import { clamp } from "@/lib/utils";

export type HSV = { h: number; s: number; v: number; a: number };

export function hexToRgb(hex: string): { r: number; g: number; b: number; a: number } | null {
  const raw = hex.trim().replace(/^#/, "");
  if (!/^(?:[0-9a-f]{3}|[0-9a-f]{6}|[0-9a-f]{8})$/i.test(raw)) return null;
  if (raw.length === 3) {
    const r = parseInt(raw[0]! + raw[0], 16);
    const g = parseInt(raw[1]! + raw[1], 16);
    const b = parseInt(raw[2]! + raw[2], 16);
    return { r, g, b, a: 1 };
  }
  if (raw.length === 6 || raw.length === 8) {
    const r = parseInt(raw.slice(0, 2), 16);
    const g = parseInt(raw.slice(2, 4), 16);
    const b = parseInt(raw.slice(4, 6), 16);
    const a = raw.length === 8 ? parseInt(raw.slice(6, 8), 16) / 255 : 1;
    if ([r, g, b].some((n) => Number.isNaN(n))) return null;
    return { r, g, b, a };
  }
  return null;
}

export function rgbToHex(r: number, g: number, b: number, a = 1): string {
  const to = (n: number) =>
    clamp(Math.round(n), 0, 255).toString(16).padStart(2, "0").toUpperCase();
  const base = `#${to(r)}${to(g)}${to(b)}`;
  if (a >= 0.995) return base;
  return `${base}${to(a * 255)}`;
}

export function rgbToHsv(r: number, g: number, b: number, a = 1): HSV {
  r /= 255;
  g /= 255;
  b /= 255;
  const max = Math.max(r, g, b);
  const min = Math.min(r, g, b);
  const d = max - min;
  let h = 0;
  if (d !== 0) {
    if (max === r) h = ((g - b) / d) % 6;
    else if (max === g) h = (b - r) / d + 2;
    else h = (r - g) / d + 4;
    h *= 60;
    if (h < 0) h += 360;
  }
  const s = max === 0 ? 0 : d / max;
  return { h, s: s * 100, v: max * 100, a: a * 100 };
}

export function hsvToRgb(h: number, s: number, v: number, a = 100): { r: number; g: number; b: number; a: number } {
  s = clamp(s, 0, 100) / 100;
  v = clamp(v, 0, 100) / 100;
  h = wrapHue(h);
  const c = v * s;
  const x = c * (1 - Math.abs(((h / 60) % 2) - 1));
  const m = v - c;
  let r = 0,
    g = 0,
    b = 0;
  if (h < 60) [r, g, b] = [c, x, 0];
  else if (h < 120) [r, g, b] = [x, c, 0];
  else if (h < 180) [r, g, b] = [0, c, x];
  else if (h < 240) [r, g, b] = [0, x, c];
  else if (h < 300) [r, g, b] = [x, 0, c];
  else [r, g, b] = [c, 0, x];
  return {
    r: (r + m) * 255,
    g: (g + m) * 255,
    b: (b + m) * 255,
    a: clamp(a, 0, 100) / 100,
  };
}

export function hexToHsv(hex: string): HSV | null {
  const rgb = hexToRgb(hex);
  if (!rgb) return null;
  return rgbToHsv(rgb.r, rgb.g, rgb.b, rgb.a);
}

export function hsvToHex(hsv: HSV): string {
  const rgb = hsvToRgb(hsv.h, hsv.s, hsv.v, hsv.a);
  return rgbToHex(rgb.r, rgb.g, rgb.b, rgb.a);
}

export function wrapHue(h: number): number {
  return ((h % 360) + 360) % 360;
}

export function hexToHsl(hex: string): { h: number; s: number; l: number } | null {
  const rgb = hexToRgb(hex);
  if (!rgb) return null;
  const r = rgb.r / 255;
  const g = rgb.g / 255;
  const b = rgb.b / 255;
  const max = Math.max(r, g, b);
  const min = Math.min(r, g, b);
  const l = (max + min) / 2;
  if (max === min) return { h: 0, s: 0, l: l * 100 };
  const d = max - min;
  const s = l > 0.5 ? d / (2 - max - min) : d / (max + min);
  let h = 0;
  if (max === r) h = ((g - b) / d + (g < b ? 6 : 0)) * 60;
  else if (max === g) h = ((b - r) / d + 2) * 60;
  else h = ((r - g) / d + 4) * 60;
  return { h, s: s * 100, l: l * 100 };
}

export function hslToHex(h: number, s: number, l: number): string {
  const hh = wrapHue(h) / 360;
  const ss = clamp(s, 0, 100) / 100;
  const ll = clamp(l, 0, 100) / 100;
  if (ss === 0) {
    const v = ll * 255;
    return rgbToHex(v, v, v);
  }
  const hue2rgb = (p: number, q: number, t: number) => {
    if (t < 0) t += 1;
    if (t > 1) t -= 1;
    if (t < 1 / 6) return p + (q - p) * 6 * t;
    if (t < 1 / 2) return q;
    if (t < 2 / 3) return p + (q - p) * (2 / 3 - t) * 6;
    return p;
  };
  const q = ll < 0.5 ? ll * (1 + ss) : ll + ss - ll * ss;
  const p = 2 * ll - q;
  return rgbToHex(hue2rgb(p, q, hh + 1 / 3) * 255, hue2rgb(p, q, hh) * 255, hue2rgb(p, q, hh - 1 / 3) * 255);
}


export function isValidHex(input: string): boolean {
  return /^#?([0-9a-fA-F]{3}|[0-9a-fA-F]{6}|[0-9a-fA-F]{8})$/.test(input.trim());
}

export function normalizeHex(input: string): string | null {
  const rgb = hexToRgb(input);
  if (!rgb) return null;
  return rgbToHex(rgb.r, rgb.g, rgb.b, rgb.a);
}

export type HarmonyKind = "mono" | "analog" | "complement" | "split";

export function harmonyHues(h: number, kind: HarmonyKind): number[] {
  const base = wrapHue(h);
  switch (kind) {
    case "mono":
      return [base, base, wrapHue(base + 8)];
    case "analog":
      return [wrapHue(base - 30), wrapHue(base + 30), wrapHue(base + 60)];
    case "complement":
      return [wrapHue(base + 180), wrapHue(base + 150), wrapHue(base - 150)];
    case "split":
      return [wrapHue(base + 150), wrapHue(base + 210), wrapHue(base + 180)];
  }
}

export function relativeLuminance(hex: string): number {
  const rgb = hexToRgb(hex);
  if (!rgb) return 0;
  const lin = (c: number) => {
    const s = c / 255;
    return s <= 0.03928 ? s / 12.92 : ((s + 0.055) / 1.055) ** 2.4;
  };
  return 0.2126 * lin(rgb.r) + 0.7152 * lin(rgb.g) + 0.0722 * lin(rgb.b);
}

export function contrastRatio(a: string, b: string): number {
  const L1 = relativeLuminance(a);
  const L2 = relativeLuminance(b);
  const [hi, lo] = L1 > L2 ? [L1, L2] : [L2, L1];
  return (hi + 0.05) / (lo + 0.05);
}

/** Button ink must contrast with its own fill, including translucent custom colors. */
export function readableInk(background: string, backdrop = "#000000"): string {
  const fg = hexToRgb(background);
  const bg = hexToRgb(backdrop) ?? { r: 0, g: 0, b: 0, a: 1 };
  const fill = fg ? rgbToHex(
    fg.r * fg.a + bg.r * (1 - fg.a),
    fg.g * fg.a + bg.g * (1 - fg.a),
    fg.b * fg.a + bg.b * (1 - fg.a),
  ) : backdrop;
  return contrastRatio("#FFFFFF", fill) >= contrastRatio("#000000", fill) ? "#FFFFFF" : "#000000";
}

/** Ice Floor — digits stay readable. Accents may be painted freely. */
export function ensureReadable<T extends { felt: string; digit: string; accent: string }>(palette: T): T {
  const next = { ...palette };
  if (contrastRatio(next.digit, next.felt) < 4.6) {
    next.digit = contrastRatio("#FFFFFF", next.felt) >= contrastRatio("#000000", next.felt) ? "#FFFFFF" : "#000000";
  }
  return next;
}
