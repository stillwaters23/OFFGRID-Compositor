import type { Alarm } from "@/lib/types";

/** Dated alarms use the device's current local time zone, including seconds. */
export function alarmTimestamp(a: Pick<Alarm, "year" | "month" | "day" | "hours" | "minutes" | "seconds">): number {
  return new Date(a.year, a.month - 1, a.day, a.hours, a.minutes, a.seconds ?? 0, 0).getTime();
}

export function nextAlarmDate(hours: number, minutes: number, tomorrow: boolean, now = Date.now()): Date {
  const date = new Date(now);
  date.setHours(hours, minutes, 0, 0);
  if (tomorrow || date.getTime() <= now) date.setDate(date.getDate() + 1);
  return date;
}
