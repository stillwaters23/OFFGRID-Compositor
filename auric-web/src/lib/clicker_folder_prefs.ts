/** Same storage key and field names as Lane 3 folder preferences. */
export const FOLDER_PREFERENCES_KEY = "auric-folder-settings-v1";
export const CLICKER_MODULE_ID = "clicker-sounds";

const ICON_STYLES = ["3d", "flat", "etched"] as const;
export type FolderIconStyle = (typeof ICON_STYLES)[number];

export type ClickerFolderView = {
  id: typeof CLICKER_MODULE_ID;
  tileName: string;
  customName: string;
  size: number;
  iconStyle: FolderIconStyle;
  titlePx: 16 | 24;
};

function clampSize(value: unknown, fallback: number) {
  return typeof value === "number" && Number.isFinite(value) ? Math.max(0, Math.min(100, value)) : fallback;
}

function record(value: unknown): Record<string, unknown> {
  return value && typeof value === "object" && !Array.isArray(value) ? (value as Record<string, unknown>) : {};
}

/** 16 px below the midpoint, 24 px at and above it. Both sit on the 8 px grid. */
export function clickerTitlePx(size: number): 16 | 24 {
  return size >= 50 ? 24 : 16;
}

/**
 * Resolve the clicker folder the way Lane 3's grid does:
 * per-id size, else folderSizeAll; icon style default flat; blank names refused; names trimmed to 24.
 */
export function clickerFolderView(raw: unknown, pkg = CLICKER_MODULE_ID): ClickerFolderView {
  const source = record(raw);
  const sizes = record(source.folderSizes);
  const styles = record(source.folderIconStyle);
  const names = record(source.folderNames);
  const size = clampSize(sizes[CLICKER_MODULE_ID], clampSize(source.folderSizeAll, 50));
  const styleValue = styles[CLICKER_MODULE_ID];
  const iconStyle: FolderIconStyle = ICON_STYLES.includes(styleValue as FolderIconStyle)
    ? (styleValue as FolderIconStyle)
    : "flat";
  const customName = typeof names[CLICKER_MODULE_ID] === "string" ? names[CLICKER_MODULE_ID].trim().slice(0, 24) : "";
  return {
    id: CLICKER_MODULE_ID,
    tileName: customName || pkg,
    customName,
    size,
    iconStyle,
    titlePx: clickerTitlePx(size),
  };
}

/** Lane 3 order: known saved ids stay, unknown and new ids append in default order. */
export function orderWithClicker(defaultIds: readonly string[], savedOrder: readonly string[]): string[] {
  const defaults = new Set(defaultIds);
  const ordered = savedOrder.filter((id, index) => defaults.has(id) && savedOrder.indexOf(id) === index);
  const present = new Set(ordered);
  return [...ordered, ...defaultIds.filter((id) => !present.has(id))];
}

export function readStoredFolderPreferences(): unknown {
  if (typeof window === "undefined") return null;
  try {
    const saved = window.localStorage.getItem(FOLDER_PREFERENCES_KEY);
    return saved ? JSON.parse(saved) : null;
  } catch {
    return null;
  }
}
