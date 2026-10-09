import { bankCutById } from "@/lib/banks";
import { useAuric } from "@/lib/store";
import type { TickRecipe } from "@/lib/ticks";
import { wakeById } from "@/lib/wake";

let ctx: AudioContext | null = null;

function getCtx(): AudioContext | null {
  if (typeof window === "undefined") return null;
  const Ctor = window.AudioContext || (window as unknown as { webkitAudioContext?: typeof AudioContext }).webkitAudioContext;
  if (!Ctor) return null;
  try {
    if (!ctx || ctx.state === "closed") ctx = new Ctor();
    if (ctx.state === "suspended") void ctx.resume().catch(() => undefined);
    return ctx;
  } catch { return null; }
}

export function unlockAudio() {
  getCtx();
}

function envGain(ctx: AudioContext, start: number, peak: number, attack: number, decay: number) {
  const g = ctx.createGain();
  g.gain.setValueAtTime(0.0001, start);
  g.gain.exponentialRampToValueAtTime(Math.max(0.0002, peak), start + attack);
  g.gain.exponentialRampToValueAtTime(0.0001, start + attack + decay);
  return g;
}

function voice(ac: AudioContext, t: number, rec: TickRecipe) {
  if (rec.silent) return;
  const osc = ac.createOscillator();
  osc.type = rec.type;
  osc.frequency.setValueAtTime(Math.max(20, rec.f0), t);
  if (rec.f1 !== rec.f0) osc.frequency.exponentialRampToValueAtTime(Math.max(20, rec.f1), t + rec.dur);
  const g = envGain(ac, t, rec.gain, 0.003, rec.dur);
  if (rec.filter && rec.ff) {
    const f = ac.createBiquadFilter();
    f.type = rec.filter === "hp" ? "highpass" : rec.filter === "lp" ? "lowpass" : "bandpass";
    f.frequency.value = rec.ff;
    osc.connect(f);
    f.connect(g);
  } else {
    osc.connect(g);
  }
  g.connect(ac.destination);
  osc.start(t);
  osc.stop(t + rec.dur + 0.04);
  if (rec.layer) {
    const L = rec.layer;
    const o2 = ac.createOscillator();
    o2.type = L.type;
    o2.frequency.setValueAtTime(Math.max(20, L.f0), t + L.delay);
    o2.frequency.exponentialRampToValueAtTime(Math.max(20, L.f1), t + L.delay + L.dur);
    const g2 = envGain(ac, t + L.delay, L.gain, 0.004, L.dur);
    o2.connect(g2);
    g2.connect(ac.destination);
    o2.start(t + L.delay);
    o2.stop(t + L.delay + L.dur + 0.04);
  }
}

export function playClick() {
  playReelTick(useAuric.getState().iconSound);
}

export function playSettingsClick() {
  playReelTick(useAuric.getState().settingsSound);
}

export function playStopwatchCut() {
  playReelTick(useAuric.getState().stopwatchSound);
}

export function playHeartbeatCut() {
  playReelTick(useAuric.getState().heartbeatSound);
}

export function playReelTick(kind: string) {
  const cut = bankCutById(kind);
  if (cut.recipe.silent) return;
  const ac = getCtx();
  if (!ac) return;
  voice(ac, ac.currentTime, cut.recipe);
}

export function reelFeel() {
  const s = useAuric.getState();
  if (s.soundEnabled !== false) playReelTick(s.reelSound);
  try {
    navigator.vibrate?.([12, 18, 42]);
  } catch {
    /* ignore */
  }
}

function beep(freq: number, dur: number, type: OscillatorType, gain: number, delay = 0) {
  const ac = getCtx();
  if (!ac) return;
  const t = ac.currentTime + delay;
  const o = ac.createOscillator();
  const g = ac.createGain();
  o.type = type;
  o.frequency.setValueAtTime(freq, t);
  g.gain.setValueAtTime(gain, t);
  g.gain.exponentialRampToValueAtTime(0.0001, t + dur);
  o.connect(g);
  g.connect(ac.destination);
  o.start(t);
  o.stop(t + dur + 0.02);
}

export function playStart() {
  beep(440, 0.08, "sine", 0.05);
  beep(660, 0.12, "sine", 0.04, 0.07);
}

export function playPause() {
  beep(330, 0.1, "sine", 0.04);
}

export function playLap() {
  beep(880, 0.06, "triangle", 0.04);
}

export function playReset() {
  beep(220, 0.12, "sine", 0.04);
}

export function playAlarm() {
  playWake(undefined);
}

export function playWake(id?: string) {
  stopAlarm();
  const ac = getCtx();
  if (!ac) return;
  const rec = wakeById(id).recipe;
  if (rec.silent) return;
  const fire = () => voiceWake(ac, rec);
  fire();
  buzz();
  const period = Math.max(140, Math.round((rec.dur + rec.gap) * 1000));
  alarmHandle = window.setInterval(() => {
    fire();
    buzz();
  }, period);
}

export function previewWake(id: string) {
  stopAlarm();
  const ac = getCtx();
  if (!ac) return;
  const rec = wakeById(id).recipe;
  if (rec.silent) return;
  voiceWake(ac, rec);
}

function voiceWake(ac: AudioContext, rec: { type: OscillatorType; f0: number; f1: number; dur: number; gain: number; filter?: string; ff?: number; layer?: { type: OscillatorType; f0: number; f1: number; dur: number; gain: number; delay: number } }) {
  const t = ac.currentTime;
  const master = ac.createGain();
  master.gain.value = 1;
  const comp = ac.createDynamicsCompressor();
  comp.threshold.value = -16;
  comp.knee.value = 8;
  comp.ratio.value = 5;
  comp.attack.value = 0.003;
  comp.release.value = 0.08;
  master.connect(comp);
  comp.connect(ac.destination);

  const o = ac.createOscillator();
  o.type = rec.type;
  o.frequency.setValueAtTime(Math.max(40, rec.f0), t);
  if (rec.f1 !== rec.f0) o.frequency.exponentialRampToValueAtTime(Math.max(40, rec.f1), t + rec.dur);
  const g = envGain(ac, t, rec.gain, 0.004, rec.dur);
  if (rec.filter && rec.ff) {
    const f = ac.createBiquadFilter();
    f.type = rec.filter === "hp" ? "highpass" : rec.filter === "lp" ? "lowpass" : "bandpass";
    f.frequency.value = rec.ff;
    o.connect(f);
    f.connect(g);
  } else {
    o.connect(g);
  }
  g.connect(master);
  o.start(t);
  o.stop(t + rec.dur + 0.04);

  const rumble = ac.createOscillator();
  rumble.type = "square";
  rumble.frequency.setValueAtTime(60, t);
  const rg = envGain(ac, t, Math.min(0.28, rec.gain * 0.4), 0.01, rec.dur);
  rumble.connect(rg);
  rg.connect(master);
  rumble.start(t);
  rumble.stop(t + rec.dur + 0.04);

  if (rec.layer) {
    const L = rec.layer;
    const o2 = ac.createOscillator();
    o2.type = L.type;
    o2.frequency.setValueAtTime(Math.max(40, L.f0), t + L.delay);
    o2.frequency.exponentialRampToValueAtTime(Math.max(40, L.f1), t + L.delay + L.dur);
    const g2 = envGain(ac, t + L.delay, L.gain, 0.004, L.dur);
    o2.connect(g2);
    g2.connect(master);
    o2.start(t + L.delay);
    o2.stop(t + L.delay + L.dur + 0.04);
  }
}

export function stopAlarm() {
  if (alarmHandle != null) {
    window.clearInterval(alarmHandle);
    alarmHandle = null;
  }
  try {
    navigator.vibrate?.(0);
  } catch {
    /* ignore */
  }
}

export function stopPreview() {
  stopAlarm();
}

function buzz() {
  try {
    navigator.vibrate?.([110, 60, 110]);
  } catch {
    /* ignore */
  }
}

let alarmHandle: number | null = null;

export function playUnlock() {
  beep(523, 0.08, "sine", 0.05);
  beep(784, 0.14, "sine", 0.05, 0.08);
}

export function playLock() {
  beep(392, 0.1, "sine", 0.04);
  beep(262, 0.14, "sine", 0.04, 0.08);
}

export function playAchieve() {
  beep(523, 0.09, "sine", 0.05);
  beep(659, 0.09, "sine", 0.05, 0.09);
  beep(784, 0.18, "sine", 0.06, 0.18);
}

export function playDescend() {
  const ac = getCtx();
  if (!ac) return;
  const t = ac.currentTime;
  const thud = ac.createOscillator();
  thud.type = "sine";
  thud.frequency.setValueAtTime(92, t);
  thud.frequency.exponentialRampToValueAtTime(48, t + 0.22);
  const tg = envGain(ac, t, 0.11, 0.008, 0.28);
  thud.connect(tg);
  tg.connect(ac.destination);
  thud.start(t);
  thud.stop(t + 0.34);

  const ping = ac.createOscillator();
  ping.type = "triangle";
  ping.frequency.setValueAtTime(1760, t + 0.08);
  ping.frequency.exponentialRampToValueAtTime(2480, t + 0.22);
  const pg = envGain(ac, t + 0.08, 0.055, 0.01, 0.32);
  ping.connect(pg);
  pg.connect(ac.destination);
  ping.start(t + 0.08);
  ping.stop(t + 0.46);
}

export function playSurface() {
  const ac = getCtx();
  if (!ac) return;
  const t = ac.currentTime;
  const ping = ac.createOscillator();
  ping.type = "sine";
  ping.frequency.setValueAtTime(1320, t);
  ping.frequency.exponentialRampToValueAtTime(660, t + 0.18);
  const pg = envGain(ac, t, 0.05, 0.008, 0.22);
  ping.connect(pg);
  pg.connect(ac.destination);
  ping.start(t);
  ping.stop(t + 0.28);
}

export function playLockIn() {
  playUnlock();
}

export function playSoftChime() {
  playAchieve();
}

export function playScrollUnfurl() {
  playDescend();
  beep(1174, 0.18, "sine", 0.03, 0.12);
}
