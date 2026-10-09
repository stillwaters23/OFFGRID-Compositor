export type HourCycle = "auto" | "h12" | "h23";

export type ClockStamp = {
  time: string;
  date: string;
  weekday: string;
  zone: string;
};

const formatterCache = new Map<string, Intl.DateTimeFormat>();
const validZones = new Set<string>();
let lastSeenZone = "";

export const CHICAGO_ZONE = "America/Chicago";

/** Saved or manually entered zones are untrusted input to Intl. */
export function safeTimeZone(candidate?: string | null): string {
  if (typeof candidate === "string" && candidate.length <= 100) {
    if (validZones.has(candidate)) return candidate;
    try {
      new Intl.DateTimeFormat("en", { timeZone: candidate }).format(0);
      if (validZones.size >= 64) validZones.clear();
      validZones.add(candidate);
      return candidate;
    } catch {
      /* Recover the clock without deleting saved history. */
    }
  }
  return deviceTimeZone();
}

export function deviceTimeZone(): string {
  try {
    return Intl.DateTimeFormat().resolvedOptions().timeZone || "UTC";
  } catch {
    return "UTC";
  }
}

export function refreshClockFormatters(zone: string) {
  if (zone === lastSeenZone) return;
  lastSeenZone = zone;
  formatterCache.clear();
}

function formatter(key: string, options: Intl.DateTimeFormatOptions, timeZone?: string | null) {
  const zone = safeTimeZone(timeZone);
  refreshClockFormatters(zone);
  const cacheKey = `${key}|${zone}|${JSON.stringify(options)}`;
  let fmt = formatterCache.get(cacheKey);
  if (!fmt) {
    fmt = new Intl.DateTimeFormat("en-US", { ...options, timeZone: zone });
    formatterCache.set(cacheKey, fmt);
  }
  return fmt;
}

export function clockStamp(ms: number, cycle: HourCycle = "auto", timeZone?: string | null): ClockStamp {
  const zoneId = safeTimeZone(timeZone);
  const hourCycle = cycle === "h12" || cycle === "h23" ? cycle : undefined;
  const timeFmt = formatter("time", { hour: "numeric", minute: "2-digit", second: "2-digit", hourCycle }, zoneId);
  const dateFmt = formatter("date", { month: "long", day: "numeric", year: "numeric" }, zoneId);
  const weekFmt = formatter("week", { weekday: "long" }, zoneId);
  const zoneFmt = formatter("zone", { timeZoneName: "short" }, zoneId);
  const zoneParts = zoneFmt.formatToParts(ms);
  const zoneName = zoneParts.find((p) => p.type === "timeZoneName")?.value ?? zoneId;
  return {
    time: timeFmt.format(ms),
    date: dateFmt.format(ms),
    weekday: weekFmt.format(ms),
    zone: `${zoneName} · ${zoneId.replace(/_/g, " ")}`,
  };
}

export function sessionStamp(ms: number, timeZone?: string | null): { time: string; day: string } {
  const zoneId = safeTimeZone(timeZone);
  const time = formatter("session-time", { hour: "numeric", minute: "2-digit", hourCycle: "h12" }, zoneId).format(ms);
  const parts = formatter("session-day", { weekday: "short", month: "short", day: "numeric" }, zoneId).formatToParts(ms);
  const weekday = parts.find((p) => p.type === "weekday")?.value ?? "";
  const month = parts.find((p) => p.type === "month")?.value ?? "";
  const day = parts.find((p) => p.type === "day")?.value ?? "";
  return { time, day: `${weekday} ${month} ${day}` };
}

/** Live masthead clock: Fri 2026-09-18 · 8:01 PM CDT · America/Chicago */
export function chicagoStamp(ms: number): { weekday: string; date: string; time: string; tz: string; line: string } {
  const zoneId = CHICAGO_ZONE;
  const parts = formatter("chicago-parts", {
    weekday: "short",
    year: "numeric",
    month: "2-digit",
    day: "2-digit",
    hour: "numeric",
    minute: "2-digit",
    hourCycle: "h12",
    timeZoneName: "short",
  }, zoneId).formatToParts(ms);
  const grab = (type: Intl.DateTimeFormatPartTypes) => parts.find((p) => p.type === type)?.value ?? "";
  const weekday = grab("weekday");
  const year = grab("year");
  const month = grab("month");
  const day = grab("day");
  const hour = grab("hour");
  const minute = grab("minute");
  const dayPeriod = grab("dayPeriod");
  const tz = grab("timeZoneName") || "CT";
  const date = `${year}-${month}-${day}`;
  const time = `${hour}:${minute} ${dayPeriod}`.replace(/\s+/g, " ").trim();
  return {
    weekday,
    date,
    time,
    tz,
    line: `${weekday} ${date} · ${time} ${tz} · America/Chicago`,
  };
}

export function minutesFromHm(hm: string): number {
  if (typeof hm !== "string" || !/^([01]\d|2[0-3]):[0-5]\d$/.test(hm)) return 0;
  const [h, m] = hm.split(":").map(Number);
  return h! * 60 + m!;
}

export function schemeFromSchedule(
  nowMs: number,
  lightStart: string,
  darkStart: string,
  zone?: string | null,
): "light" | "dark" {
  const parts = formatter("hm", { hour: "2-digit", minute: "2-digit", hourCycle: "h23", numberingSystem: "latn" }, zone).formatToParts(nowMs);
  const hour = Number(parts.find((p) => p.type === "hour")?.value ?? 0);
  const minute = Number(parts.find((p) => p.type === "minute")?.value ?? 0);
  const now = hour * 60 + minute;
  const light = minutesFromHm(lightStart);
  const dark = minutesFromHm(darkStart);
  if (light === dark) return "dark";
  if (light < dark) return now >= light && now < dark ? "light" : "dark";
  return now >= light || now < dark ? "light" : "dark";
}

export function prefersDarkScheme(): boolean {
  if (typeof window === "undefined" || typeof window.matchMedia !== "function") return true;
  return window.matchMedia("(prefers-color-scheme: dark)").matches;
}
