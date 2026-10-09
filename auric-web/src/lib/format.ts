export function pad2(n: number): string {
  return Math.floor(Math.abs(n)).toString().padStart(2, "0");
}

export const SPAN_MS = {
  year: 12 * 30 * 86_400_000,
  month: 30 * 86_400_000,
  week: 7 * 86_400_000,
  day: 86_400_000,
  hour: 3_600_000,
  minute: 60_000,
  second: 1_000,
} as const;

export type Span = {
  years: number;
  months: number;
  weeks: number;
  days: number;
  hours: number;
  minutes: number;
  seconds: number;
  cs: number;
};

/** Inclusive max on each drum. Calendar cascade: 12 mo / 4 wk / 7 d / 24 h. */
export const SPAN_MAX: Span = {
  years: 77,
  months: 11,
  weeks: 4,
  days: 6,
  hours: 23,
  minutes: 59,
  seconds: 59,
  cs: 99,
};

export const SPAN_COUNT: Record<keyof Span, number> = {
  years: 78,
  months: 12,
  weeks: 5,
  days: 7,
  hours: 24,
  minutes: 60,
  seconds: 60,
  cs: 100,
};

export const ZERO_SPAN: Span = {
  years: 0,
  months: 0,
  weeks: 0,
  days: 0,
  hours: 0,
  minutes: 0,
  seconds: 0,
  cs: 0,
};

export function hmsFromMs(ms: number): { h: number; m: number; s: number; cs: number } {
  const clamped = Math.max(0, Math.floor(ms));
  const h = Math.floor(clamped / 3_600_000);
  const m = Math.floor((clamped % 3_600_000) / 60_000);
  const s = Math.floor((clamped % 60_000) / 1000);
  const cs = Math.floor((clamped % 1000) / 10);
  return { h, m, s, cs };
}

export function spanFromMs(ms: number): Span {
  let n = Math.max(0, Math.floor(ms));
  const years = Math.floor(n / SPAN_MS.year);
  n -= years * SPAN_MS.year;
  const months = Math.floor(n / SPAN_MS.month);
  n -= months * SPAN_MS.month;
  const weeks = Math.floor(n / SPAN_MS.week);
  n -= weeks * SPAN_MS.week;
  const days = Math.floor(n / SPAN_MS.day);
  n -= days * SPAN_MS.day;
  const hours = Math.floor(n / SPAN_MS.hour);
  n -= hours * SPAN_MS.hour;
  const minutes = Math.floor(n / SPAN_MS.minute);
  n -= minutes * SPAN_MS.minute;
  const seconds = Math.floor(n / SPAN_MS.second);
  n -= seconds * SPAN_MS.second;
  return {
    years: Math.min(years, SPAN_MAX.years),
    months: Math.min(months, SPAN_MAX.months),
    weeks: Math.min(weeks, SPAN_MAX.weeks),
    days: Math.min(days, SPAN_MAX.days),
    hours: Math.min(hours, SPAN_MAX.hours),
    minutes: Math.min(minutes, SPAN_MAX.minutes),
    seconds: Math.min(seconds, SPAN_MAX.seconds),
    cs: Math.min(Math.floor(n / 10), SPAN_MAX.cs),
  };
}

export function msFromSpan(p: Partial<Span> & { hours?: number; minutes?: number; seconds?: number }): number {
  return (
    (p.years ?? 0) * SPAN_MS.year +
    (p.months ?? 0) * SPAN_MS.month +
    (p.weeks ?? 0) * SPAN_MS.week +
    (p.days ?? 0) * SPAN_MS.day +
    (p.hours ?? 0) * SPAN_MS.hour +
    (p.minutes ?? 0) * SPAN_MS.minute +
    (p.seconds ?? 0) * SPAN_MS.second +
    (p.cs ?? 0) * 10
  );
}

export function daysInMonth(year: number, month: number): number {
  return new Date(year, month, 0).getDate();
}

export function msFromHms(h: number, m: number, s: number): number {
  return ((h * 60 + m) * 60 + s) * 1000;
}

export function formatHms(ms: number, withHours = true): string {
  const { h, m, s } = hmsFromMs(ms);
  if (withHours || h > 0) return `${pad2(h)}:${pad2(m)}:${pad2(s)}`;
  return `${pad2(m)}:${pad2(s)}`;
}

export function formatSpanClock(ms: number): string {
  const p = spanFromMs(ms);
  const clock = `${pad2(p.hours)}:${pad2(p.minutes)}:${pad2(p.seconds)}.${pad2(p.cs)}`;
  const cal = [p.years ? `${p.years}y` : "", p.months ? `${p.months}mo` : "", p.weeks ? `${p.weeks}w` : "", p.days ? `${p.days}d` : ""]
    .filter(Boolean)
    .join(" ");
  return cal ? `${cal} ${clock}` : clock;
}

export function formatHmsCompact(ms: number): string {
  const { h, m, s } = hmsFromMs(ms);
  if (h > 0) return `${h}h ${m}m`;
  if (m > 0) return `${m}m ${s}s`;
  return `${s}s`;
}

export function formatDurationPretty(ms: number): string {
  const p = spanFromMs(ms);
  const parts: string[] = [];
  if (p.years) parts.push(`${p.years}y`);
  if (p.months) parts.push(`${p.months}mo`);
  if (p.weeks) parts.push(`${p.weeks}w`);
  if (p.days) parts.push(`${p.days}d`);
  if (p.hours) parts.push(`${p.hours}h`);
  if (p.minutes) parts.push(`${p.minutes}m`);
  if (p.seconds || parts.length === 0) parts.push(`${p.seconds}s`);
  return parts.join(" ");
}

export function dayKey(ts: number): string {
  const d = new Date(ts);
  return `${d.getFullYear()}-${pad2(d.getMonth() + 1)}-${pad2(d.getDate())}`;
}

export function formatDay(ts: number): string {
  return new Date(ts).toLocaleDateString(undefined, {
    weekday: "short",
    month: "short",
    day: "numeric",
  });
}

export function formatTime(ts: number): string {
  return new Date(ts).toLocaleTimeString(undefined, {
    hour: "numeric",
    minute: "2-digit",
  });
}

export type PitStamp = {
  weekday: string;
  longDate: string;
  time: string;
  numeric: string;
  tzAbbr: string;
  city: string;
};

export function pitStamp(ms: number): PitStamp {
  const d = new Date(ms);
  const weekday = d.toLocaleDateString(undefined, { weekday: "long" });
  const longDate = d.toLocaleDateString(undefined, { month: "long", day: "numeric", year: "numeric" });
  const time = d.toLocaleTimeString(undefined, { hour: "numeric", minute: "2-digit", second: "2-digit" });
  const numeric = d.toLocaleDateString(undefined, { month: "2-digit", day: "2-digit", year: "numeric" });
  const parts = new Intl.DateTimeFormat(undefined, { timeZoneName: "short" }).formatToParts(d);
  const tzAbbr = parts.find((p) => p.type === "timeZoneName")?.value ?? "";
  const iana = Intl.DateTimeFormat().resolvedOptions().timeZone || "";
  const city = iana.split("/").pop()?.replace(/_/g, " ") ?? "";
  return { weekday, longDate, time, numeric, tzAbbr, city };
}

export function parseDuration(raw: string): number | null {
  const t = raw.trim().toLowerCase().replace(/,/g, "");
  if (!t) return null;
  if (t === "off" || t === "0" || t === "00" || t === "00:00" || t === "00:00:00") return 0;
  const hms = t.match(/^(\d{1,3}):(\d{1,2})(?::(\d{1,2}))?$/);
  if (hms) {
    const h = Number(hms[1]);
    const m = Number(hms[2]);
    const s = Number(hms[3] ?? 0);
    if (m > 59 || s > 59) return null;
    return ((h * 60 + m) * 60 + s) * 1000;
  }
  const packed = t.match(/^(\d+(?:\.\d+)?)\s*(ms|s|sec|secs|second|seconds|m|min|mins|minute|minutes|h|hr|hrs|hour|hours|d|day|days)?$/);
  if (!packed) return null;
  const n = Number(packed[1]);
  if (!Number.isFinite(n) || n < 0) return null;
  const u = packed[2] ?? "m";
  if (u === "ms") return Math.round(n);
  if (u.startsWith("s")) return Math.round(n * 1_000);
  if (u.startsWith("h")) return Math.round(n * 3_600_000);
  if (u.startsWith("d")) return Math.round(n * 86_400_000);
  return Math.round(n * 60_000);
}

export function bracketDuration(ms: number): string {
  if (ms < 100) return "[off]";
  const p = spanFromMs(ms);
  if (p.years) return `[${p.years}y]`;
  if (p.months) return `[${p.months}mo]`;
  if (p.weeks) return `[${p.weeks}w]`;
  if (p.days) return `[${p.days}d]`;
  if (p.hours) return `[${p.hours}h]`;
  if (p.minutes) return `[${p.minutes}m]`;
  return `[${p.seconds}s]`;
}
