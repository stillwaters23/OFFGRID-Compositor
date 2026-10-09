export type ModuleId =
  | "timer"
  | "stopwatch"
  | "alarm"
  | "themes"
  | "sizing"
  | "opacity"
  | "icons"
  | "alarm-sounds"
  | "timer-sounds"
  | "haptics"
  | "heartbeat"
  | "heartbeat-log"
  | "logs-exports"
  | "device"
  | "tutorial";

export type ModuleTier = "free" | "paid";
export type ModuleAccent = "green" | "cyan" | "multi";

export type ModuleSpec = {
  id: ModuleId;
  name: string;
  pkg: string;
  hint: string;
  preview: string;
  tier: ModuleTier;
  accent: ModuleAccent;
  glyph: "clock" | "watch" | "bell" | "chips" | "type" | "fade" | "nest" | "wave" | "pulses" | "rings" | "ecg" | "ecg-log" | "sheets" | "battery" | "query";
};

export const MODULES: ModuleSpec[] = [
  { id: "timer", name: "Timer", pkg: "timer", hint: "Set hours, minutes, seconds.", preview: "Enter a duration. Start. Hold. Reset.", tier: "free", accent: "green", glyph: "clock" },
  { id: "stopwatch", name: "Stopwatch", pkg: "stopwatch", hint: "Run, lap, hold.", preview: "Elapsed time, laps, hold.", tier: "free", accent: "green", glyph: "watch" },
  { id: "alarm", name: "Alarm", pkg: "alarm", hint: "Clock time, including seconds.", preview: "Set a clock time. Rings while Auric is open.", tier: "free", accent: "green", glyph: "bell" },
  { id: "themes", name: "Themes", pkg: "themes", hint: "Color chips and light/dark.", preview: "A swatch picker and live hex preview live inside this folder.", tier: "paid", accent: "multi", glyph: "chips" },
  { id: "sizing", name: "Sizing", pkg: "sizing", hint: "Number scale and faces.", preview: "Pick a size. The digits resize.", tier: "paid", accent: "green", glyph: "type" },
  { id: "opacity", name: "Opacity", pkg: "opacity", hint: "Glass 0–100. Does not spin.", preview: "A stationary glass slider. 0 is clear, 100 is solid.", tier: "paid", accent: "green", glyph: "fade" },
  { id: "icons", name: "Icons", pkg: "icons", hint: "House mark and cuts.", preview: "The Auric lockup lives here. The tile is never the only label.", tier: "paid", accent: "green", glyph: "nest" },
  { id: "alarm-sounds", name: "Alarm Sounds", pkg: "alarm-sounds", hint: "Wake previews.", preview: "Pick a family. Preview, then apply.", tier: "paid", accent: "green", glyph: "wave" },
  { id: "timer-sounds", name: "Timer Sounds", pkg: "timer-sounds", hint: "Click and lock-in.", preview: "Two clean pulses. The click is the whole point.", tier: "paid", accent: "green", glyph: "pulses" },
  { id: "haptics", name: "Haptics", pkg: "haptics", hint: "Notch ticks and lock-in.", preview: "Concentric rings. Every notch is a tick.", tier: "paid", accent: "green", glyph: "rings" },
  { id: "heartbeat", name: "Heartbeat", pkg: "heartbeat", hint: "Pulse while Auric is open.", preview: "Crown module. Interval wheel, 1 second to 24 hours. Billing is not live on this web demo.", tier: "paid", accent: "green", glyph: "ecg" },
  { id: "heartbeat-log", name: "Heartbeat Log", pkg: "heartbeat-log", hint: "Pulses you kept.", preview: "Crown module. The same pulse, behind stacked sheets.", tier: "paid", accent: "green", glyph: "ecg-log" },
  { id: "logs-exports", name: "Logs & Exports", pkg: "logs-exports", hint: "Notes, Prism, CSV.", preview: "Crown module. Stacked sheets. Activity count, not a grade.", tier: "paid", accent: "green", glyph: "sheets" },
  { id: "device", name: "Device", pkg: "device", hint: "Battery, threads, optional IP.", preview: "Readings stay off until you ask. Unsupported stays Unavailable.", tier: "free", accent: "green", glyph: "battery" },
  { id: "tutorial", name: "Tutorial", pkg: "tutorial", hint: "How this instrument works.", preview: "A question mark. Open session, folders, heartbeat log action.", tier: "free", accent: "cyan", glyph: "query" },
];

/** Web demo keeps these paid even if an older persist snapshot listed them as installed. */
export const WEB_LOCKED: ModuleId[] = ["heartbeat", "heartbeat-log", "logs-exports"];

export const FREE_MODULE_IDS: ModuleId[] = MODULES.filter((m) => m.tier === "free").map((m) => m.id);

export function moduleById(id: string | null | undefined): ModuleSpec | undefined {
  return MODULES.find((m) => m.id === id);
}

export function isInstalled(id: ModuleId, installed: readonly string[]): boolean {
  return installed.includes(id);
}

export function withFreeModules(ids: readonly string[] | undefined): ModuleId[] {
  const allowed = new Set(MODULES.map((m) => m.id));
  const locked = new Set<ModuleId>(WEB_LOCKED);
  const next = new Set<ModuleId>(FREE_MODULE_IDS);
  for (const id of ids ?? []) {
    if (!allowed.has(id as ModuleId)) continue;
    if (locked.has(id as ModuleId)) continue;
    next.add(id as ModuleId);
  }
  return MODULES.map((m) => m.id).filter((id) => next.has(id));
}
