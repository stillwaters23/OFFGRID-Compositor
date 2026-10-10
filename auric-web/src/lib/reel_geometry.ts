export type ReelGeometryInput = {
  width: number;
  height: number;
  baseFont: number;
  longest: number;
  digitScale: number;
  angle?: number;
};

export type ReelGeometry = {
  fontSize: number;
  rowPitch: number;
  radius: number;
  rowCenter: number;
};

export function reelGeometry({
  width,
  height,
  baseFont,
  longest,
  digitScale,
  angle = 26,
}: ReelGeometryInput): ReelGeometry {
  const safeWidth = Math.max(0, width);
  const safeHeight = Math.max(0, height);
  const safeLongest = Math.max(1, longest);
  const widthLimit = (safeWidth - 8) / (safeLongest * 1.1);
  const heightLimit = safeHeight * 0.24;
  const fontSize = Math.max(
    0,
    Math.floor(Math.min(baseFont * Math.max(0, digitScale), widthLimit, heightLimit)),
  );
  const rowPitch = Math.max(0, Math.min(fontSize * 1.28, safeHeight * 0.28));
  const radius = rowPitch / (2 * Math.sin((angle * Math.PI) / 360));
  return { fontSize, rowPitch, radius, rowCenter: safeHeight / 2 };
}

/** Integer rows a reel crosses between the last tick and the current position. */
export function rowsCrossed(lastTickPosition: number, position: number): number {
  if (!Number.isFinite(lastTickPosition) || !Number.isFinite(position)) return 0;
  return Math.abs(Math.round(position) - lastTickPosition);
}
