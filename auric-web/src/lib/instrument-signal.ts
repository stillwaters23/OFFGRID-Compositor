import type { Alarm, StopwatchStatus, TimerStatus } from "@/lib/types";

export type SignalTone = "red" | "green";
export type SignalKind = "timer" | "stopwatch" | "alarm" | "remind";

export type InstrumentSignal = {
  kind: SignalKind;
  tone: SignalTone;
  label: string;
  pulse: boolean;
};

export function timerSignal(status: TimerStatus): InstrumentSignal {
  if (status === "running") return { kind: "timer", tone: "red", label: "Running", pulse: true };
  if (status === "paused") return { kind: "timer", tone: "red", label: "Paused", pulse: false };
  if (status === "done") return { kind: "timer", tone: "green", label: "Finished", pulse: false };
  return { kind: "timer", tone: "red", label: "Ready", pulse: false };
}

export function stopwatchSignal(status: StopwatchStatus): InstrumentSignal {
  if (status === "running") return { kind: "stopwatch", tone: "red", label: "Running", pulse: true };
  if (status === "paused") return { kind: "stopwatch", tone: "green", label: "Paused", pulse: false };
  return { kind: "stopwatch", tone: "red", label: "Ready", pulse: false };
}

export function alarmSignal(alarms: Alarm[], ringingId: string | null): InstrumentSignal {
  if (ringingId) return { kind: "alarm", tone: "red", label: "Ringing", pulse: true };
  const armed = alarms.filter((a) => a.enabled);
  if (armed.length === 0) return { kind: "alarm", tone: "red", label: "Off", pulse: false };
  return { kind: "alarm", tone: "green", label: "Armed", pulse: false };
}

/** Completion acknowledgement is distinct from an enabled device switch. */
export function reminderSignal(enabled: boolean, delivered: boolean): InstrumentSignal {
  return { kind: "remind", tone: delivered ? "green" : "red", label: delivered ? "Delivered" : enabled ? "Scheduled" : "Off", pulse: enabled && !delivered };
}
