import { classifyGesture, type GestureEvent } from "./folder_layout.ts";

export { classifyGesture };
export type { GestureEvent };

export function shouldSuppressClick(gesture: "tap" | "long-press" | "drag" | "cancel"): boolean {
  return gesture === "long-press" || gesture === "drag";
}
