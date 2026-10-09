export function alarmRingingCopy(label?: string | null): string {
  const name = typeof label === "string" ? label.trim() : "";
  return name ? `Your alarm is ringing: ${name}.` : "Your alarm is ringing.";
}

export function timerFinishedCopy(): string {
  return "Your timer has finished.";
}

export const REFLECTION_HEADING = "What did you do with your time?";
