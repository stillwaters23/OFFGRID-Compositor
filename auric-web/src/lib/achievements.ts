import type { Achievement, AchievementId, Session } from "@/lib/types";
import { dayKey } from "@/lib/format";

export const ACHIEVEMENTS: Achievement[] = [
  { id: "first-mark", title: "First Mark", blurb: "Log your first session." },
  { id: "wheel-master", title: "Wheel Master", blurb: "Spin the reel fifty times." },
  { id: "sevenfold", title: "Sevenfold", blurb: "Keep a seven-day streak." },
  { id: "hour-keeper", title: "Hour Keeper", blurb: "Log sixty focused minutes." },
  { id: "lap-legend", title: "Lap Legend", blurb: "Take ten laps in one run." },
  { id: "always-on", title: "Always On", blurb: "Pin the in-app floating chip." },
  { id: "crowned", title: "Crowned", blurb: "Preview the Crown table." },
  { id: "night-shift", title: "Night Shift", blurb: "Finish a session after ten." },
  { id: "century", title: "Century", blurb: "Log one hundred sessions." },
];

export function computeStreak(
  sessions: Session[],
  insuranceAvailable: boolean,
): { streak: number; lastDay: string | null; insuranceUsed: boolean } {
  if (sessions.length === 0) return { streak: 0, lastDay: null, insuranceUsed: false };
  const days = Array.from(new Set(sessions.map((s) => dayKey(s.endedAt)))).sort().reverse();
  const today = dayKey(Date.now());
  const yesterday = dayKey(Date.now() - 86_400_000);

  const start = days[0]!;
  if (start !== today && start !== yesterday) {
    if (insuranceAvailable && days[0]) {
      return { streak: 1, lastDay: days[0], insuranceUsed: true };
    }
    return { streak: 0, lastDay: days[0] ?? null, insuranceUsed: false };
  }

  let streak = 0;
  let cursor = start === today || start === yesterday ? start : today;
  const set = new Set(days);
  let insuranceUsed = false;
  while (true) {
    if (set.has(cursor)) {
      streak += 1;
      cursor = dayKey(new Date(cursor + "T12:00:00").getTime() - 86_400_000);
      continue;
    }
    if (!insuranceUsed && insuranceAvailable && streak > 0) {
      insuranceUsed = true;
      cursor = dayKey(new Date(cursor + "T12:00:00").getTime() - 86_400_000);
      continue;
    }
    break;
  }
  return { streak, lastDay: days[0] ?? null, insuranceUsed };
}

export function evaluateUnlocks(args: {
  sessions: Session[];
  reelTicks: number;
  streak: number;
  totalMinutes: number;
  isCrowned: boolean;
  floatingUsed: boolean;
  already: AchievementId[];
}): AchievementId[] {
  const have = new Set(args.already);
  const next: AchievementId[] = [];
  const tryUnlock = (id: AchievementId, ok: boolean) => {
    if (ok && !have.has(id)) next.push(id);
  };

  tryUnlock("first-mark", args.sessions.length >= 1);
  tryUnlock("wheel-master", args.reelTicks >= 50);
  tryUnlock("sevenfold", args.streak >= 7);
  tryUnlock("hour-keeper", args.totalMinutes >= 60);
  tryUnlock("lap-legend", args.sessions.some((s) => (s.laps?.length ?? 0) >= 10));
  tryUnlock("always-on", args.floatingUsed);
  tryUnlock("crowned", args.isCrowned);
  tryUnlock(
    "night-shift",
    args.sessions.some((s) => {
      const h = new Date(s.endedAt).getHours();
      return h >= 22 || h < 5;
    }),
  );
  tryUnlock("century", args.sessions.length >= 100);
  return next;
}

export const HEARTBEAT_OPTIONS: { label: string; ms: number }[] = [
  { label: "1s", ms: 1_000 },
  { label: "5s", ms: 5_000 },
  { label: "15s", ms: 15_000 },
  { label: "30s", ms: 30_000 },
  { label: "1m", ms: 60_000 },
  { label: "2m", ms: 120_000 },
  { label: "5m", ms: 300_000 },
  { label: "10m", ms: 600_000 },
  { label: "15m", ms: 900_000 },
  { label: "30m", ms: 1_800_000 },
  { label: "1h", ms: 3_600_000 },
  { label: "4h", ms: 14_400_000 },
  { label: "12h", ms: 43_200_000 },
  { label: "24h", ms: 86_400_000 },
];
