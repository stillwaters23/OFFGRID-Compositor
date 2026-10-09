import { TICKS, type TickCut } from "@/lib/ticks";

function remap(prefix: string, pitch: number, gainMul: number): TickCut[] {
  return TICKS.map((t) => {
    const rec = t.recipe;
    if (rec.silent) {
      return { ...t, id: `${prefix}-${t.id}` };
    }
    return {
      ...t,
      id: `${prefix}-${t.id}`,
      recipe: {
        ...rec,
        f0: Math.max(40, rec.f0 * pitch),
        f1: Math.max(40, rec.f1 * pitch),
        gain: Math.min(0.12, rec.gain * gainMul),
        ff: rec.ff ? rec.ff * pitch : rec.ff,
        layer: rec.layer
          ? {
              ...rec.layer,
              f0: rec.layer.f0 * pitch,
              f1: rec.layer.f1 * pitch,
              gain: Math.min(0.1, rec.layer.gain * gainMul),
            }
          : undefined,
      },
    };
  });
}

export const STOPWATCH_CUTS = remap("sw", 0.78, 1.8);
export const ICON_CUTS = remap("ic", 1.12, 1.4);
export const SETTINGS_CUTS = remap("se", 0.94, 1.5);
export const HEARTBEAT_CUTS = remap("hb", 0.52, 1.15);

export const HOUSE_STOPWATCH_ID = STOPWATCH_CUTS[0]!.id;
export const HOUSE_ICON_ID = ICON_CUTS[0]!.id;
export const HOUSE_SETTINGS_ID = SETTINGS_CUTS[0]!.id;
export const HOUSE_HEARTBEAT_ID = HEARTBEAT_CUTS.find((c) => c.id.endsWith("ice-01"))?.id ?? HEARTBEAT_CUTS[0]!.id;

export function bankCutById(id: string): TickCut {
  return (
    STOPWATCH_CUTS.find((c) => c.id === id) ??
    ICON_CUTS.find((c) => c.id === id) ??
    SETTINGS_CUTS.find((c) => c.id === id) ??
    HEARTBEAT_CUTS.find((c) => c.id === id) ??
    TICKS.find((c) => c.id === id) ??
    TICKS[0]!
  );
}
