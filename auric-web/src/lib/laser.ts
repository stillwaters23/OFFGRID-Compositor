export const LASER_DEFAULT = "#f0243e";

export const LASER_PRESETS = [
  { id: "red", label: "Red", color: "#f0243e" },
  { id: "orange", label: "Orange", color: "#ff762e" },
  { id: "green", label: "Green", color: "#2ddc72" },
  { id: "blue", label: "Blue", color: "#27a9ff" },
  { id: "violet", label: "Violet", color: "#bd55ff" },
] as const;

export type LaserVariables = {
  "--laser-color": string;
  "--laser-core": string;
  "--laser-bloom-a": string;
  "--laser-spread": string;
  "--laser-on": string;
};

export function laserVariables(
  color = LASER_DEFAULT,
  intensity = 100,
): LaserVariables {
  const strength = Math.max(0, Math.min(100, Number.isFinite(intensity) ? intensity : 0)) / 100;
  return {
    "--laser-color": color,
    "--laser-core": "#ffffff",
    "--laser-bloom-a": `${Math.round(36 * strength)}%`,
    "--laser-spread": `${Math.round(48 * (0.5 + strength * 0.5))}px`,
    "--laser-on": strength > 0 ? "1" : "0",
  };
}

function parseHexColor(color: string): [number, number, number] {
  const hex = color.replace(/^#/, "");
  if (!/^(?:[\da-f]{3}|[\da-f]{6})$/i.test(hex)) return [0, 0, 0];
  const expanded = hex.length === 3 ? [...hex].map((c) => `${c}${c}`).join("") : hex;
  return [
    Number.parseInt(expanded.slice(0, 2), 16),
    Number.parseInt(expanded.slice(2, 4), 16),
    Number.parseInt(expanded.slice(4, 6), 16),
  ];
}

export function laserBloomComposite(
  color: string,
  alpha: number,
  background = "#05070e",
): string {
  const foreground = parseHexColor(color);
  const backdrop = parseHexColor(background);
  const opacity = Math.max(0, Math.min(1, Number.isFinite(alpha) ? alpha : 0));
  return `#${foreground
    .map((channel, index) =>
      Math.round(channel * opacity + backdrop[index]! * (1 - opacity))
        .toString(16)
        .padStart(2, "0"),
    )
    .join("")}`;
}
