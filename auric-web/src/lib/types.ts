export type AppMode = "timer" | "stopwatch" | "alarm" | "remind";

export type TimerStatus = "idle" | "running" | "paused" | "done";
export type StopwatchStatus = "idle" | "running" | "paused";

export type ReelSound = string;
export type DigitFont = string;

export type PaintSlot = "felt" | "chrome" | "digit" | "accent" | "danger" | "glow";

export type VaultSort = "az" | "recent" | "used" | "duration";

export type Palette = {
  id: string;
  name: string;
  premium: boolean;
  felt: string;
  chrome: string;
  digit: string;
  accent: string;
  danger: string;
  glow: string;
  light?: boolean;
};

export type Session = {
  id: string;
  kind: "timer" | "stopwatch" | "alarm" | "remind";
  startedAt: number;
  endedAt: number;
  durationMs: number;
  plannedMs?: number;
  noteArchived?: boolean;
  note: string;
  laps?: number[];
  usedCount: number;
};

export type SavedTimer = {
  id: string;
  name: string;
  hours: number;
  minutes: number;
  seconds: number;
  durationMs?: number;
  used: number;
};

export type Alarm = {
  id: string;
  year: number;
  month: number;
  day: number;
  hours: number;
  minutes: number;
  seconds: number;
  label: string;
  enabled: boolean;
  lastFiredKey: string;
};

export type Task = {
  id: string;
  text: string;
  done: boolean;
  createdAt: number;
};

export type NoticeKind = "alarm" | "timer" | "stopwatch" | "heartbeat";

export type BeatLogEntry = {
  id: string;
  at: number;
  body: string;
  archived?: boolean;
};

export type Notice = {
  id: string;
  kind: NoticeKind;
  title: string;
  body: string;
  at: number;
  read: boolean;
  archived: boolean;
};

export type AchievementId =
  | "first-mark"
  | "wheel-master"
  | "sevenfold"
  | "hour-keeper"
  | "lap-legend"
  | "always-on"
  | "crowned"
  | "night-shift"
  | "century";

export type Achievement = {
  id: AchievementId;
  title: string;
  blurb: string;
};

export type HeartbeatOption = {
  label: string;
  ms: number;
};

export type VaultSectionId = "sessions" | "charts" | "achievements" | "heartbeat" | "export";
