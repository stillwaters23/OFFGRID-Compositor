import { useLayoutEffect, useRef, useState, type CSSProperties, type PointerEvent } from "react";
import "@/styles/folders.css";
import { FolderRename } from "@/components/auric/folder-rename";
import { FolderSizePopover } from "@/components/auric/folder-size-popover";
import { FolderIcon3D } from "@/components/auric/folder-icons-3d";
import { GlassFolder } from "@/components/auric/glass-folder";
import { folderMetrics, hitZone, LONG_PRESS_MS, MOVE_TOLERANCE_PX, orderFolderIds, type FolderPreferences, type Rect } from "@/lib/folder-layout";
import { clampToSafeArea, springStep } from "@/lib/folder-physics";
import { classifyGesture, shouldSuppressClick } from "@/lib/folder-gestures";
import { MODULES, isInstalled, type ModuleId, type ModuleSpec } from "@/lib/modules";
import { playClick } from "@/lib/audio";
import { hapticTick } from "@/lib/haptics";
import { useAuric } from "@/lib/store";
import { cn } from "@/lib/utils";

type FolderGridProps = {
  preferences: FolderPreferences;
  beforeRects: { current: Map<string, DOMRect> };
  onChangePreferences: (next: FolderPreferences) => void;
};

type Interaction = {
  id: string;
  pointerId: number;
  button: HTMLButtonElement;
  origin: Rect;
  startX: number;
  startY: number;
  startTime: number;
  zone: "label" | "body";
  insets: { top: number; right: number; bottom: number; left: number };
  events: { type: "start" | "move" | "end"; x: number; y: number; time: number }[];
  timer: number;
  moved: boolean;
  longPressed: boolean;
  swiping: boolean;
  x: number;
  y: number;
};

function safeInsets(): { top: number; right: number; bottom: number; left: number } {
  const root = document.querySelector(".tx-root");
  if (!root) return { top: 0, right: 0, bottom: 0, left: 0 };
  const style = getComputedStyle(root);
  const read = (side: string) => Number.parseFloat(style.getPropertyValue(`--tx-safe-${side}`)) || 0;
  return { top: read("top"), right: read("right"), bottom: read("bottom"), left: read("left") };
}

function reducedMotion(): boolean {
  return window.matchMedia("(prefers-reduced-motion: reduce)").matches;
}

function springBack(
  interaction: Interaction,
  target: { x: number; y: number },
  targetOrder: string[] | null,
  updatePreferences: (next: FolderPreferences) => void,
  preferences: FolderPreferences,
) {
  const { button, origin } = interaction;
  const insets = interaction.insets;
  const viewport = { width: window.innerWidth, height: window.innerHeight };
  const finish = () => {
    button.style.transform = "";
    button.style.zIndex = "";
    if (targetOrder) updatePreferences({ ...preferences, folderOrder: targetOrder });
  };

  if (reducedMotion()) {
    const clamped = clampToSafeArea(
      { x: origin.x + target.x, y: origin.y + target.y, width: origin.width, height: origin.height },
      viewport,
      insets,
    );
    button.style.transform = `translate(${clamped.x - origin.x}px, ${clamped.y - origin.y}px)`;
    finish();
    return;
  }

  let sx = { position: interaction.x, velocity: 0, target: target.x };
  let sy = { position: interaction.y, velocity: 0, target: target.y };
  let previous = performance.now();
  const step = (now: number) => {
    const delta = now - previous;
    previous = now;
    sx = springStep(sx, delta);
    sy = springStep(sy, delta);
    const clamped = clampToSafeArea(
      { x: origin.x + sx.position, y: origin.y + sy.position, width: origin.width, height: origin.height },
      viewport,
      insets,
    );
    const x = clamped.x - origin.x;
    const y = clamped.y - origin.y;
    button.style.transform = `translate(${x}px, ${y}px)`;
    if (Math.abs(sx.position - sx.target) < 0.5 && Math.abs(sy.position - sy.target) < 0.5) {
      finish();
      return;
    }
    window.requestAnimationFrame(step);
  };
  window.requestAnimationFrame(step);
}

export function FolderGrid({ preferences, beforeRects, onChangePreferences }: FolderGridProps) {
  const installed = useAuric((s) => s.installedModules);
  const glow = useAuric((s) => s.glowIntensity);
  const gridRef = useRef<HTMLUListElement>(null);
  const tileRefs = useRef(new Map<string, HTMLLIElement>());
  const activeInteraction = useRef<Interaction | null>(null);
  const suppressClick = useRef(false);
  const [containerWidth, setContainerWidth] = useState(() =>
    typeof window === "undefined" ? 390 : window.innerWidth,
  );
  const [sizeId, setSizeId] = useState<string | null>(null);
  const [renameId, setRenameId] = useState<string | null>(null);
  const [renameValue, setRenameValue] = useState("");
  const [renamePlacement, setRenamePlacement] = useState({ left: 0, top: 0, width: 120, height: 48 });
  const [singleIndex, setSingleIndex] = useState(0);

  const updatePreferences = onChangePreferences;

  useLayoutEffect(() => {
    if (beforeRects.current.size) {
      for (const item of tileRefs.current.values()) {
        const button = item.querySelector<HTMLButtonElement>(".tx-folder-tile");
        button?.getAnimations().forEach((animation) => animation.cancel());
      }
      if (!reducedMotion()) {
        const flips: { button: HTMLButtonElement; dx: number; dy: number }[] = [];
        const scrollClip = gridRef.current?.closest<HTMLElement>(".tx-scroll")?.getBoundingClientRect();
        const clip = {
          left: Math.max(0, scrollClip?.left ?? 0),
          top: Math.max(0, scrollClip?.top ?? 0),
          right: Math.min(window.innerWidth, scrollClip?.right ?? window.innerWidth),
          bottom: Math.min(window.innerHeight, scrollClip?.bottom ?? window.innerHeight),
        };
        for (const [id, item] of tileRefs.current) {
          const button = item.querySelector<HTMLButtonElement>(".tx-folder-tile");
          const old = beforeRects.current.get(id);
          if (!button || !old) continue;
          if (old.right <= clip.left || old.left >= clip.right || old.bottom <= clip.top || old.top >= clip.bottom) continue;
          const next = button.getBoundingClientRect();
          if (
            next.right <= clip.left ||
            next.left >= clip.right ||
            next.bottom <= clip.top ||
            next.top >= clip.bottom
          ) continue;
          const dx = old.left - next.left;
          const dy = old.top - next.top;
          if (dx || dy) {
            flips.push({ button, dx, dy });
          }
        }
        let restoreDecorations = () => {};
        if (flips.length) {
          const root = document.querySelector(".tx-root");
          const competingAnimations = root?.getAnimations({ subtree: true }).filter((animation) =>
            !flips.some(({ button }) => button.getAnimations().includes(animation)),
          ) ?? [];
          competingAnimations.forEach((animation) => animation.pause());
          const rows = flips
            .map(({ button }) => button.querySelector<HTMLElement>(".tx-folder-row"))
            .filter((row): row is HTMLElement => row !== null);
          const previousBackdrops = rows.map((row) => row.style.backdropFilter);
          rows.forEach((row) => {
            row.style.backdropFilter = "none";
          });
          restoreDecorations = () => {
            rows.forEach((row, index) => {
              row.style.backdropFilter = previousBackdrops[index];
            });
            competingAnimations.forEach((animation) => {
              if (animation.playState === "paused") animation.play();
            });
          };
        }
        let remaining = flips.length;
        for (const { button, dx, dy } of flips) {
          button.dataset.flipping = "true";
          const animation = button.animate(
            [{ transform: `translate(${dx}px, ${dy}px)`, opacity: 0.78 }, { transform: "translate(0, 0)", opacity: 1 }],
            { duration: 320, easing: "cubic-bezier(.2,.8,.2,1)" },
          );
          let released = false;
          const releaseLayer = (defer = false) => {
            if (released) return;
            released = true;
            const cleanup = () => {
              delete button.dataset.flipping;
              remaining -= 1;
              if (remaining === 0) restoreDecorations();
            };
            if (defer) window.requestAnimationFrame(() => window.requestAnimationFrame(cleanup));
            else cleanup();
          };
          animation.finished.then(() => releaseLayer(true), () => releaseLayer());
          animation.addEventListener("finish", () => releaseLayer(true), { once: true });
          animation.addEventListener("cancel", () => releaseLayer(), { once: true });
        }
      }
    }
    beforeRects.current.clear();
  }, [preferences.layout, preferences.folderOrder, beforeRects]);

  useLayoutEffect(() => {
    const grid = gridRef.current;
    if (!grid) return;
    const update = () => setContainerWidth(grid.clientWidth);
    update();
    const observer = new ResizeObserver(update);
    observer.observe(grid);
    return () => observer.disconnect();
  }, []);

  const glowZero = glow <= 0;
  const defaultIds = MODULES.map((spec) => spec.id);
  const savedOrder = orderFolderIds(defaultIds, preferences.folderOrder);
  const position = new Map(savedOrder.map((id, index) => [id, index]));
  const rows = [...MODULES].sort((a, b) => {
    const owned = Number(!isInstalled(a.id, installed)) - Number(!isInstalled(b.id, installed));
    return owned || (position.get(a.id) ?? 0) - (position.get(b.id) ?? 0);
  });
  const visibleRows =
    preferences.layout === "single" ? rows.slice(Math.min(singleIndex, rows.length - 1), Math.min(singleIndex, rows.length - 1) + 1) : rows;

  const beginRename = (spec: ModuleSpec, button: HTMLButtonElement) => {
    const tile = button.getBoundingClientRect();
    const label = button.querySelector<HTMLElement>(".tx-folder-name")?.getBoundingClientRect();
    if (!label) return;
    const left = Math.max(8, Math.min(tile.width - 56, label.left - tile.left - 8));
    const top = Math.max(0, label.top - tile.top - Math.max(0, (48 - label.height) / 2));
    setRenamePlacement({ left, top, width: Math.max(48, Math.min(tile.width - left - 8, Math.max(120, label.width + 16))), height: 48 });
    setRenameValue(preferences.folderNames[spec.id] ?? spec.pkg);
    setRenameId(spec.id);
    setSizeId(null);
  };

  const onPointerDown = (event: PointerEvent<HTMLButtonElement>, spec: ModuleSpec) => {
    if ((event.target as HTMLElement).closest(".tx-folder-control")) return;
    const button = event.currentTarget;
    const tile = button.getBoundingClientRect();
    const label = button.querySelector<HTMLElement>(".tx-folder-name")?.getBoundingClientRect() ?? tile;
    const point = { x: event.clientX, y: event.clientY };
    const interaction: Interaction = {
      id: spec.id,
      pointerId: event.pointerId,
      button,
      origin: { x: tile.x, y: tile.y, width: tile.width, height: tile.height },
      startX: event.clientX,
      startY: event.clientY,
      startTime: performance.now(),
      zone: hitZone(point, tile, label),
      insets: safeInsets(),
      events: [{ type: "start", x: event.clientX, y: event.clientY, time: 0 }],
      timer: 0,
      moved: false,
      longPressed: false,
      swiping: false,
      x: 0,
      y: 0,
    };
    activeInteraction.current = interaction;
    interaction.timer = window.setTimeout(() => {
      if (activeInteraction.current !== interaction || interaction.moved) return;
      interaction.longPressed = true;
      suppressClick.current = true;
      if (interaction.zone === "label") {
        beginRename(spec, button);
      } else {
        hapticTick();
        setSizeId(spec.id);
        setRenameId(null);
      }
    }, LONG_PRESS_MS);
  };

  const onPointerMove = (event: PointerEvent<HTMLButtonElement>) => {
    const interaction = activeInteraction.current;
    if (!interaction || interaction.pointerId !== event.pointerId || interaction.longPressed) return;
    const time = performance.now() - interaction.startTime;
    interaction.events.push({ type: "move", x: event.clientX, y: event.clientY, time });
    const dx = event.clientX - interaction.startX;
    const dy = event.clientY - interaction.startY;
    if (!interaction.moved && Math.hypot(dx, dy) >= MOVE_TOLERANCE_PX) {
      interaction.moved = true;
      window.clearTimeout(interaction.timer);
      if (preferences.layout === "single" && Math.abs(dx) > Math.abs(dy)) {
        interaction.swiping = true;
        suppressClick.current = true;
      } else {
        interaction.button.setPointerCapture(event.pointerId);
        interaction.button.style.zIndex = "3";
      }
    }
    if (!interaction.moved || interaction.swiping) return;
    const clamped = clampToSafeArea(
      {
        x: interaction.origin.x + dx,
        y: interaction.origin.y + dy,
        width: interaction.origin.width,
        height: interaction.origin.height,
      },
      { width: window.innerWidth, height: window.innerHeight },
      interaction.insets,
    );
    interaction.x = clamped.x - interaction.origin.x;
    interaction.y = clamped.y - interaction.origin.y;
    interaction.button.style.transform = `translate(${interaction.x}px, ${interaction.y}px)`;
  };

  const onPointerUp = (event: PointerEvent<HTMLButtonElement>) => {
    const interaction = activeInteraction.current;
    if (!interaction || interaction.pointerId !== event.pointerId) return;
    window.clearTimeout(interaction.timer);
    interaction.events.push({
      type: "end",
      x: event.clientX,
      y: event.clientY,
      time: performance.now() - interaction.startTime,
    });
    const kind = classifyGesture(interaction.events);
    if (interaction.moved || interaction.longPressed || shouldSuppressClick(kind)) suppressClick.current = true;

    if (interaction.swiping) {
      const delta = event.clientX - interaction.startX;
      if (Math.abs(delta) >= 48) setSingleIndex((index) => Math.max(0, Math.min(rows.length - 1, index + (delta < 0 ? 1 : -1))));
    } else if (interaction.moved && !interaction.longPressed) {
      const insideViewport =
        event.clientX >= 0 &&
        event.clientX <= window.innerWidth &&
        event.clientY >= 0 &&
        event.clientY <= window.innerHeight;
      const hovered = insideViewport
        ? [...tileRefs.current].find(([id, item]) => {
            if (id === interaction.id) return false;
            const rect = item.getBoundingClientRect();
            return event.clientX >= rect.left && event.clientX <= rect.right && event.clientY >= rect.top && event.clientY <= rect.bottom;
          })
        : undefined;
      const targetId = hovered?.[0];
      const targetButton = hovered?.[1].querySelector<HTMLButtonElement>(".tx-folder-tile") ?? null;
      const targetRect = targetButton?.getBoundingClientRect();
      const targetOrder = targetId && targetId !== interaction.id ? [...savedOrder] : null;
      if (targetOrder) {
        const from = targetOrder.indexOf(interaction.id);
        const to = targetOrder.indexOf(targetId!);
        if (from >= 0 && to >= 0) {
          targetOrder.splice(from, 1);
          targetOrder.splice(to, 0, interaction.id);
        }
      }
      const target = targetRect
        ? { x: targetRect.x - interaction.origin.x, y: targetRect.y - interaction.origin.y }
        : { x: 0, y: 0 };
      springBack(interaction, target, targetOrder, updatePreferences, preferences);
    }
    if (suppressClick.current) window.setTimeout(() => (suppressClick.current = false), 0);
    activeInteraction.current = null;
  };

  const columns = folderMetrics(preferences.folderSizeAll, containerWidth, preferences.layout).columns;
  return (
    <>
      <ul
        ref={gridRef}
        className="tx-folder-grid"
        data-layout={preferences.layout}
        style={{ ["--tx-folder-columns" as string]: columns } as CSSProperties}
      >
        {visibleRows.map((spec) => {
          const owned = isInstalled(spec.id, installed);
          const style = preferences.folderIconStyle[spec.id] ?? "flat";
          const name = preferences.folderNames[spec.id] ?? spec.pkg;
          const size = preferences.folderSizes[spec.id] ?? preferences.folderSizeAll;
          const metrics = folderMetrics(size, containerWidth, preferences.layout);
          return (
            <li
              key={spec.id}
              data-folder-id={spec.id}
              ref={(element) => {
                if (element) tileRefs.current.set(spec.id, element);
                else tileRefs.current.delete(spec.id);
              }}
              style={
                {
                  ["--tx-folder-row-height" as string]: `${metrics.rowHeight}px`,
                  ["--tx-folder-icon-size" as string]: `${metrics.iconPx}px`,
                  ["--tx-folder-title-size" as string]: `${metrics.titlePx}px`,
                } as CSSProperties
              }
            >
              <button
                type="button"
                className={cn("tx-folder-tile", owned ? "is-on" : "is-off", style === "etched" && "is-etched")}
                aria-label={`${name} ${owned ? "unlocked" : "locked"}`}
                onClick={() => {
                  if (suppressClick.current) {
                    suppressClick.current = false;
                    return;
                  }
                  playClick();
                  useAuric.getState().enterModule(spec.id as ModuleId);
                }}
                onPointerDown={(event) => onPointerDown(event, spec)}
                onPointerMove={onPointerMove}
                onPointerUp={onPointerUp}
                onPointerCancel={onPointerUp}
                onContextMenu={(event) => {
                  event.preventDefault();
                  setSizeId(spec.id);
                  setRenameId(null);
                }}
                onKeyDown={(event) => {
                  if (event.key === "F2") {
                    event.preventDefault();
                    beginRename(spec, event.currentTarget);
                  } else if ((event.shiftKey && event.key === "F10") || event.key === "ContextMenu") {
                    event.preventDefault();
                    setSizeId(spec.id);
                    setRenameId(null);
                  }
                }}
              >
                <span className="tx-folder-row">
                  <span className={cn("tx-folder-icon", style === "etched" && "is-etched")}>
                    {style === "3d" ? (
                      <FolderIcon3D kind={spec.glyph} />
                    ) : (
                      <GlassFolder spec={spec} on={owned} glowZero={glowZero} etched={style === "etched"} />
                    )}
                  </span>
                  <span className="tx-folder-copy">
                    <span className={cn("tx-folder-name", !owned && "is-locked")} title={name} data-folder-label={spec.id}>
                      {name}
                    </span>
                    <span className={cn("tx-folder-state", owned ? "is-on" : "is-off")}>
                      {owned ? "unlocked" : "locked"}
                    </span>
                  </span>
                  <span className={cn("tx-folder-lockmark", owned ? "is-open" : "is-shut")} aria-hidden>
                    {owned ? (
                      <svg viewBox="0 0 16 16" width="16" height="16">
                        <rect x="3" y="7" width="10" height="7" rx="1.2" fill="none" stroke="currentColor" strokeWidth="1.4" />
                        <path d="M5 7 V4.8 a3 3 0 0 1 6 0 V5.2" fill="none" stroke="currentColor" strokeWidth="1.4" />
                      </svg>
                    ) : (
                      <svg viewBox="0 0 16 16" width="16" height="16">
                        <rect x="3" y="7" width="10" height="7" rx="1.2" fill="none" stroke="currentColor" strokeWidth="1.4" />
                        <path d="M5 7 V5.2 a3 3 0 0 1 6 0 V7" fill="none" stroke="currentColor" strokeWidth="1.4" />
                      </svg>
                    )}
                  </span>
                  <span className="tx-folder-chevron" aria-hidden>
                    ›
                  </span>
                </span>
              </button>
              {renameId === spec.id ? (
                <FolderRename
                  id={spec.id}
                  value={renameValue}
                  style={renamePlacement}
                  onSave={(value) => {
                    if (!value.trim()) return false;
                    updatePreferences({ ...preferences, folderNames: { ...preferences.folderNames, [spec.id]: value.slice(0, 24) } });
                    setRenameId(null);
                    return true;
                  }}
                  onCancel={() => setRenameId(null)}
                />
              ) : null}
              {sizeId === spec.id ? (
                <FolderSizePopover
                  id={spec.id}
                  value={size}
                  onChange={(value) =>
                    updatePreferences({ ...preferences, folderSizes: { ...preferences.folderSizes, [spec.id]: value } })
                  }
                  onClose={() => setSizeId(null)}
                />
              ) : null}
            </li>
          );
        })}
      </ul>
      {preferences.layout === "single" ? (
        <div className="tx-folder-single-controls">
          <button
            type="button"
            aria-label="Previous folder"
            disabled={singleIndex <= 0}
            onClick={() => setSingleIndex((index) => Math.max(0, index - 1))}
          >
            ‹
          </button>
          <span>{Math.min(singleIndex + 1, rows.length)} / {rows.length}</span>
          <button
            type="button"
            aria-label="Next folder"
            disabled={singleIndex >= rows.length - 1}
            onClick={() => setSingleIndex((index) => Math.min(rows.length - 1, index + 1))}
          >
            ›
          </button>
        </div>
      ) : null}
    </>
  );
}
