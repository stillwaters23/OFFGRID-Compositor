import { tickById, type TickRecipe } from "@/lib/ticks";

/** Schedule the click at the context clock. No queued lookahead. */
export const CLICK_LOOKAHEAD_SEC = 0;
/** Gain attack. Kept under the 10 ms preview budget. */
export const CLICK_ATTACK_SEC = 0.003;
export const CLICK_LATENCY_BUDGET_SEC = 0.01;
export const CLICK_LATENCY_HINT = "interactive" as const;

export type ClickSchedule = {
  start: number;
  attackEnd: number;
  stop: number;
  lookahead: number;
};

type ClickParam = {
  setValueAtTime: (value: number, time: number) => void;
  exponentialRampToValueAtTime: (value: number, time: number) => void;
  value?: number;
};

export type ClickOscillator = {
  type: string;
  frequency: ClickParam;
  connect: (node: unknown) => void;
  start: (time: number) => void;
  stop: (time: number) => void;
};

export type ClickGain = {
  gain: ClickParam;
  connect: (node: unknown) => void;
};

export type ClickFilter = {
  type: string;
  frequency: { value: number };
  connect: (node: unknown) => void;
};

export type ClickAudio = {
  state: string;
  currentTime: number;
  destination: unknown;
  resume: () => Promise<void>;
  createOscillator: () => ClickOscillator;
  createGain: () => ClickGain;
  createBiquadFilter: () => ClickFilter;
};

let context: ClickAudio | null = null;
let contextFactory: (() => ClickAudio | null) | null = null;

export function resetClickVoiceForTests() {
  context = null;
  contextFactory = null;
}

export function bindClickContextFactory(next: (() => ClickAudio | null) | null) {
  contextFactory = next;
  context = null;
}

export function scheduleClick(now: number, recipe: TickRecipe): ClickSchedule {
  const start = now + CLICK_LOOKAHEAD_SEC;
  return {
    start,
    attackEnd: start + CLICK_ATTACK_SEC,
    stop: start + recipe.dur + 0.04,
    lookahead: start - now,
  };
}

export function clickWithinBudget(schedule: ClickSchedule): boolean {
  return schedule.lookahead >= 0 && schedule.lookahead + CLICK_ATTACK_SEC <= CLICK_LATENCY_BUDGET_SEC;
}

function createBrowserClickContext(): ClickAudio | null {
  if (typeof window === "undefined") return null;
  const Ctor = window.AudioContext || (window as unknown as { webkitAudioContext?: typeof AudioContext }).webkitAudioContext;
  if (!Ctor) return null;
  try {
    return new Ctor({ latencyHint: CLICK_LATENCY_HINT }) as unknown as ClickAudio;
  } catch {
    try {
      return new Ctor() as unknown as ClickAudio;
    } catch {
      return null;
    }
  }
}

export function getClickContext(): ClickAudio | null {
  if (context && context.state !== "closed") return context;
  context = (contextFactory ?? createBrowserClickContext)();
  return context;
}

/** Resume the interactive context on pointer-down so the click itself is not waiting on it. */
export function primeClickVoice() {
  const audio = getClickContext();
  if (audio && audio.state === "suspended") void audio.resume().catch(() => undefined);
}

function envGain(audio: ClickAudio, start: number, peak: number, attack: number, decay: number) {
  const gain = audio.createGain();
  gain.gain.setValueAtTime(0.0001, start);
  gain.gain.exponentialRampToValueAtTime(Math.max(0.0002, peak), start + attack);
  gain.gain.exponentialRampToValueAtTime(0.0001, start + attack + decay);
  return gain;
}

function voiceAt(audio: ClickAudio, schedule: ClickSchedule, recipe: TickRecipe) {
  const osc = audio.createOscillator();
  osc.type = recipe.type;
  osc.frequency.setValueAtTime(Math.max(20, recipe.f0), schedule.start);
  if (recipe.f1 !== recipe.f0) {
    osc.frequency.exponentialRampToValueAtTime(Math.max(20, recipe.f1), schedule.start + recipe.dur);
  }
  const gain = envGain(audio, schedule.start, recipe.gain, CLICK_ATTACK_SEC, recipe.dur);
  if (recipe.filter && recipe.ff) {
    const filter = audio.createBiquadFilter();
    filter.type = recipe.filter === "hp" ? "highpass" : recipe.filter === "lp" ? "lowpass" : "bandpass";
    filter.frequency.value = recipe.ff;
    osc.connect(filter);
    filter.connect(gain);
  } else {
    osc.connect(gain);
  }
  gain.connect(audio.destination);
  osc.start(schedule.start);
  osc.stop(schedule.stop);
  if (recipe.layer) {
    const layer = recipe.layer;
    const layerStart = schedule.start + layer.delay;
    const second = audio.createOscillator();
    second.type = layer.type;
    second.frequency.setValueAtTime(Math.max(20, layer.f0), layerStart);
    second.frequency.exponentialRampToValueAtTime(Math.max(20, layer.f1), layerStart + layer.dur);
    const layerGain = envGain(audio, layerStart, layer.gain, CLICK_ATTACK_SEC, layer.dur);
    second.connect(layerGain);
    layerGain.connect(audio.destination);
    second.start(layerStart);
    second.stop(layerStart + layer.dur + 0.04);
  }
}

/** One procedural preview. Returns null when the recipe is silent or no context exists. */
export function playProceduralClick(id: string, audio: ClickAudio | null = getClickContext()): ClickSchedule | null {
  const recipe = tickById(id).recipe;
  if (recipe.silent || !audio) return null;
  if (audio.state === "suspended") void audio.resume().catch(() => undefined);
  const schedule = scheduleClick(audio.currentTime, recipe);
  voiceAt(audio, schedule, recipe);
  return schedule;
}
