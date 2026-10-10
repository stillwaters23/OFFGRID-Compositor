import type { Rect } from "./folder_layout.ts";

export type SpringState = { position: number; velocity: number; target: number };

const SPRING_OMEGA = 30;

export function springStep(state: SpringState, deltaMs: number): SpringState {
  const seconds = Number.isFinite(deltaMs) ? Math.max(0, deltaMs) / 1000 : 0;
  const displacement = state.position - state.target;
  const coefficient = state.velocity + SPRING_OMEGA * displacement;
  const decay = Math.exp(-SPRING_OMEGA * seconds);
  return {
    position: state.target + (displacement + coefficient * seconds) * decay,
    velocity: (state.velocity - SPRING_OMEGA * coefficient * seconds) * decay,
    target: state.target,
  };
}

export type Viewport = { width: number; height: number };
export type SafeInsets = { top?: number; right?: number; bottom?: number; left?: number };

export function clampToSafeArea(rect: Rect, viewport: Viewport, insets: SafeInsets = {}): Rect {
  const left = Math.max(0, insets.left ?? 0) + 8;
  const top = Math.max(0, insets.top ?? 0) + 8;
  const right = Math.max(left, viewport.width - Math.max(0, insets.right ?? 0) - 8);
  const bottom = Math.max(top, viewport.height - Math.max(0, insets.bottom ?? 0) - 8);
  const width = Math.min(Math.max(0, rect.width), right - left);
  const height = Math.min(Math.max(0, rect.height), bottom - top);

  return {
    x: Math.max(left, Math.min(rect.x, right - width)),
    y: Math.max(top, Math.min(rect.y, bottom - height)),
    width,
    height,
  };
}
