export const LONG_PRESS_MS = 500;
export const MOVE_TOLERANCE_PX = 8;
export const FOLDER_LAYOUTS = ["grid", "big", "single"] as const;

export type FolderLayout = (typeof FOLDER_LAYOUTS)[number];

export type FolderMetrics = {
  iconPx: number;
  titlePx: number;
  rowHeight: number;
  columns: number;
  tileWidth: number;
};

export function folderMetrics(size: number, containerWidth: number, layout: FolderLayout): FolderMetrics {
  const safeSize = Number.isFinite(size) ? Math.max(0, Math.min(100, size)) : 50;
  const width = Number.isFinite(containerWidth) ? Math.max(0, containerWidth) : 0;
  const columns = layout === "grid" ? (width < 600 ? 2 : 3) : 1;
  const tileWidth = Math.max(0, (width - 16 * (columns - 1)) / columns);
  const preferredIcon = 32 + safeSize * 0.48;
  const reservedWidth = 16 + 16 + 24 + 24 + 40;
  const iconPx = Math.max(24, Math.min(preferredIcon, tileWidth - reservedWidth));

  return {
    iconPx,
    titlePx: 12 + safeSize * 0.04,
    rowHeight: Math.max(44, 56 + safeSize * 0.32),
    columns,
    tileWidth,
  };
}

export type Rect = { x: number; y: number; width: number; height: number };
export type Point = { x: number; y: number };

function clampRect(rect: Rect, bounds: Rect): Rect {
  const x = Math.max(bounds.x, Math.min(rect.x, bounds.x + bounds.width - rect.width));
  const y = Math.max(bounds.y, Math.min(rect.y, bounds.y + bounds.height - rect.height));
  return { ...rect, x, y };
}

export function paddedLabelRect(tileRect: Rect, labelRect: Rect): Rect {
  const left = Math.max(tileRect.x, labelRect.x - 8);
  const right = Math.min(tileRect.x + tileRect.width, labelRect.x + labelRect.width + 8);
  const height = Math.max(44, labelRect.height);
  const top = labelRect.y - (height - labelRect.height) / 2;
  return clampRect(
    { x: left, y: top, width: Math.max(0, right - left), height },
    tileRect,
  );
}

export function hitZone(point: Point, tileRect: Rect, labelRect: Rect): "label" | "body" {
  const label = paddedLabelRect(tileRect, labelRect);
  const inTile =
    point.x >= tileRect.x &&
    point.x <= tileRect.x + tileRect.width &&
    point.y >= tileRect.y &&
    point.y <= tileRect.y + tileRect.height;
  const inLabel =
    point.x >= label.x &&
    point.x <= label.x + label.width &&
    point.y >= label.y &&
    point.y <= label.y + label.height;
  return inTile && inLabel ? "label" : "body";
}

export type GestureEvent = { type: "start" | "move" | "end"; x: number; y: number; time: number };
export type GestureKind = "tap" | "long-press" | "drag" | "cancel";

export function classifyGesture(events: readonly GestureEvent[]): GestureKind {
  const start = events.find((event) => event.type === "start");
  if (!start) return "cancel";

  let end: GestureEvent | undefined;
  for (const event of events) {
    if (event.type === "move" && Math.hypot(event.x - start.x, event.y - start.y) >= MOVE_TOLERANCE_PX) {
      return event.time - start.time < LONG_PRESS_MS ? "drag" : "cancel";
    }
    if (event.type === "end") end = event;
  }
  if (!end) return "cancel";
  return end.time - start.time >= LONG_PRESS_MS ? "long-press" : "tap";
}
