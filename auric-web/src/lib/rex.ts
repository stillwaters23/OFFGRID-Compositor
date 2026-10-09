let loaded = false;

function pickVoice(): SpeechSynthesisVoice | null {
  if (typeof window === "undefined" || !window.speechSynthesis) return null;
  const voices = window.speechSynthesis.getVoices();
  const want =
    voices.find((v) => /daniel|david|alex|fred|male|baritone/i.test(v.name) && /en/i.test(v.lang)) ??
    voices.find((v) => /en/i.test(v.lang)) ??
    voices[0] ??
    null;
  return want;
}

export function unlockRex() {
  if (typeof window === "undefined" || !window.speechSynthesis) return;
  if (loaded) return;
  loaded = true;
  window.speechSynthesis.getVoices();
  window.speechSynthesis.onvoiceschanged = () => {
    pickVoice();
  };
}

export function speakRex(line: string) {
  if (typeof window === "undefined" || !window.speechSynthesis) return;
  const text = line.trim();
  if (!text) return;
  unlockRex();
  window.speechSynthesis.cancel();
  const u = new SpeechSynthesisUtterance(text);
  const voice = pickVoice();
  if (voice) u.voice = voice;
  u.rate = 0.92;
  u.pitch = 0.78;
  u.volume = 1;
  window.speechSynthesis.speak(u);
}

export function rexLine(name: string, reminder: string, kind: "heartbeat" | "timer" | "alarm") {
  const who = name.trim() || "Auric";
  const body = reminder.trim();
  if (kind === "heartbeat") return body ? `${who}, time to ${body}.` : `${who}, keep track.`;
  if (kind === "timer") return `${who}, the wheel is still.`;
  return body ? `${who}, ${body}.` : `${who}, the alarm is ringing.`;
}
