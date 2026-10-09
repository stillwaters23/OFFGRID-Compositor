import assert from "node:assert/strict";
import { beforeEach, afterEach, test, mock } from "node:test";
import { useAuric, remainingMs, stopwatchMs } from "../src/lib/store.ts";
import { alarmTimestamp } from "../src/lib/alarm-time.ts";
import { contrastRatio, ensureReadable, readableInk, hexToRgb } from "../src/lib/color.ts";
import { FACES, loadFace, faceLoadState } from "../src/lib/faces.ts";
import { sessionsCsv } from "../src/lib/export.ts";
import { auricStorage } from "../src/lib/storage.ts";
import { withFreeModules, WEB_LOCKED, FREE_MODULE_IDS } from "../src/lib/modules.ts";
import { chicagoStamp } from "../src/lib/clock.ts";

const memory = new Map<string, string>();
Object.defineProperty(globalThis, "localStorage", { configurable: true, value: {
  getItem: (key: string) => memory.get(key) ?? null,
  setItem: (key: string, value: string) => memory.set(key, value),
  removeItem: (key: string) => memory.delete(key),
} });
const initial = useAuric.getInitialState();
const base = new Date(2026, 8, 12, 12, 0, 0).getTime();
let now = base;
beforeEach(() => {
  mock.method(Date, "now", () => now);
  useAuric.setState({ ...initial, hydrated: true });
});
afterEach(() => { now = base; mock.restoreAll(); });

test("timer deadlines recover once after a suspended tab and retain the true completion time", () => {
  const s = useAuric.getState(); s.applyPreset(5000); s.startTimer();
  now += 60_000; s.tick(now);
  assert.equal(useAuric.getState().timerStatus, "done");
  assert.equal(useAuric.getState().sessions[0].endedAt, base + 5000);
  assert.equal(useAuric.getState().sessions[0].durationMs, 5000);
  s.tick(now + 1000); s.completeTimer();
  assert.equal(useAuric.getState().sessions.length, 1);
});

test("paused countdown is stable and cannot be changed accidentally", () => {
  const s = useAuric.getState(); s.applyPreset(10_000); s.startTimer();
  now += 3000; s.pauseTimer(); s.applyPreset(1000); s.setSpanPart("seconds", 1);
  now += 60_000;
  assert.equal(remainingMs(useAuric.getState(), now), 7000);
  s.resumeTimer(); assert.equal(useAuric.getState().timerEndsAt, now + 7000);
  s.startTimer(); assert.equal(useAuric.getState().timerEndsAt, now + 7000);
});

test("control lock cannot trap a completed timer alert", () => {
  const s = useAuric.getState(); s.applyPreset(1000); s.startTimer();
  useAuric.setState({ controlLock: true }); now += 1000; s.tick(now); s.resetTimer();
  assert.equal(useAuric.getState().timerStatus, "idle");
});

test("long saved timers round-trip all duration components and clear stale parts", () => {
  const s = useAuric.getState(); useAuric.setState({ isCrowned: true });
  const duration = 32 * 86_400_000 + 5000; s.applyPreset(duration); s.saveTimer("Long");
  const id = useAuric.getState().savedTimers[0].id; s.resetTimer(); s.loadSavedTimer(id);
  assert.equal(remainingMs(useAuric.getState(), now), duration);
  useAuric.setState({ savedTimers: [{ id: "legacy", name: "Short", hours: 0, minutes: 1, seconds: 0, used: 0 }] });
  s.loadSavedTimer("legacy"); assert.equal(remainingMs(useAuric.getState(), now), 60_000);
});

test("stopwatch ignores duplicate starts and excludes held time", () => {
  const s = useAuric.getState(); s.startStopwatch(); now += 2000; s.startStopwatch();
  assert.equal(stopwatchMs(useAuric.getState(), now), 2000); s.pauseStopwatch(); now += 60_000;
  s.resumeStopwatch(); now += 1000; s.lapStopwatch();
  assert.deepEqual(useAuric.getState().swLaps, [3000]);
});

function addAlarm(seconds: number, label = "Alarm") {
  useAuric.getState().addAlarm({ year: 2026, month: 9, day: 12, hours: 12, minutes: 0, seconds, label });
}

test("alarms honor seconds, recover when overdue, and queue coincident alarms", () => {
  const s = useAuric.getState(); addAlarm(30, "First"); addAlarm(30, "Second");
  s.tick(base + 29_999); assert.equal(useAuric.getState().ringingAlarm, null);
  now += 120_000; s.tick(now); const first = useAuric.getState().ringingAlarm!.id;
  assert.equal(useAuric.getState().notices.filter(n => n.kind === "alarm").length, 1);
  s.tick(now + 1000); assert.equal(useAuric.getState().ringingAlarm!.id, first);
  s.dismissRinging(); s.tick(now); assert.notEqual(useAuric.getState().ringingAlarm!.id, first);
  s.dismissRinging(); s.tick(now); assert.equal(useAuric.getState().ringingAlarm, null);
});

test("snooze schedules a full five minutes including seconds", () => {
  const s = useAuric.getState(); addAlarm(0); now += 37_000; s.tick(now); s.snoozeRinging();
  assert.equal(alarmTimestamp(useAuric.getState().alarms[0]), now + 300_000);
});

test("heartbeat anchor and settings survive hydration", async () => {
  const s = useAuric.getState(); s.setHeartbeatMs(60_000); s.setHeartbeatEnabled(true); s.setDigitFont("bungee");
  const saved = memory.get("auric-crowned-v21")!;
  useAuric.setState({ ...initial, hydrated: false }); memory.set("auric-crowned-v21", saved);
  await useAuric.persist.rehydrate();
  assert.equal(useAuric.getState().lastBeatAt, base);
  assert.equal(useAuric.getState().digitFont, "bungee");
  s.setHydrated(); now += 60_000; s.tick(now);
  assert.equal(useAuric.getState().heartbeatLog.length, 1);
});

test("storage skips unchanged snapshots and does not crash when full", () => {
  const set = mock.method(localStorage, "setItem", () => {});
  auricStorage.setItem("test", "a"); auricStorage.setItem("test", "a");
  assert.equal(set.mock.callCount(), 1);
  set.mock.mockImplementation(() => { throw new Error("QuotaExceededError"); });
  assert.doesNotThrow(() => auricStorage.setItem("test", "b"));
});

test("unreadable saved JSON is preserved and hydration still completes", async () => {
  const key = "auric-crowned-v21";
  const damaged = '{"state":{"sessions":[';
  memory.set(key, damaged);
  await useAuric.persist.rehydrate();
  assert.equal(useAuric.persist.hasHydrated(), true);
  assert.ok([...memory].some(([name, bytes]) => name.startsWith(`${key}.recovery.`) && bytes === damaged));
  useAuric.getState().setHydrated();
  assert.equal(JSON.parse(memory.get(key)!).state.timerStatus, "idle");
});

test("a failed recovery backup protects the original saved bytes", () => {
  const key = "unrecoverable";
  memory.set(key, "broken JSON");
  mock.method(localStorage, "setItem", () => { throw new Error("QuotaExceededError"); });
  assert.equal(auricStorage.getItem(key), null);
  auricStorage.setItem(key, JSON.stringify({ state: {}, version: 0 }));
  assert.equal(memory.get(key), "broken JSON");
});

test("readability chooses the higher contrast neutral for middle luminance backgrounds", () => {
  for (const felt of ["#888888", "#999999", "#FFFFFF", "#000000"]) {
    const safe = ensureReadable({ felt, digit: felt, accent: felt });
    assert.ok(contrastRatio(safe.digit, felt) >= 4.5);
  }
});

test("font loader waits for binary success, deduplicates, and retries failures", async () => {
  const links: any[] = [];
  const fonts = { load: async () => [{ status: "loaded" }] };
  Object.defineProperty(globalThis, "document", { configurable: true, value: {
    fonts, createElement: () => ({ dataset: {}, remove() { this.removed = true; } }),
    head: { appendChild: (link: any) => links.push(link) },
  } });
  try {
    const first = loadFace("bebas"); assert.equal(faceLoadState("bebas"), "loading");
    assert.equal(loadFace("bebas"), first); assert.equal(links.length, 1);
    links[0].onerror(); assert.equal(await first, false);
    const retry = loadFace("bebas"); links[1].onload(); assert.equal(await retry, true);
    assert.equal(faceLoadState("bebas"), "loaded");
    assert.ok(links[1].href.includes("wght@400"));
    fonts.load = async () => [];
    const empty = loadFace("anton"); links[2].onload(); assert.equal(await empty, false);
    assert.equal(faceLoadState("anton"), "error");
  } finally { Reflect.deleteProperty(globalThis, "document"); }
});

test("catalog contains unique stable identities and supported regular or bold weights", () => {
  assert.equal(new Set(FACES.map(f => f.id)).size, FACES.length);
  assert.ok(FACES.every(f => Number.isFinite(f.weight) && f.weight >= 100 && f.weight <= 900));
  assert.equal(FACES.find(f => f.id === "big-shoulders")!.family, "Big Shoulders");
});


test("custom accent ink stays readable and malformed hex is rejected", () => {
  for (const fill of ["#FFFFFF", "#000000", "#777777", "#FF0000", "#FFD700", "#002266"]) {
    assert.ok(contrastRatio(readableInk(fill), fill) >= 4.5);
  }
  assert.equal(readableInk("#FFFFFF00", "#000000"), "#FFFFFF");
  for (const bad of ["#GGG", "#FFFFFFZZ", "NaN", "#12", "#12ZZZZ"]) assert.equal(hexToRgb(bad), null);
});


test("CSV export preserves quoted notes as data and neutralizes formulas", () => {
  const baseSession = { id: "session", kind: "timer" as const, startedAt: base, endedAt: base + 1000, durationMs: 1000, usedCount: 1 };
  const csv = sessionsCsv([{ ...baseSession, note: '=HYPERLINK("example")' }, { ...baseSession, note: 'A, "quoted" note\nsecond line' }]);
  assert.ok(csv.includes(`"'=HYPERLINK(""example"")"`));
  assert.ok(csv.includes('"A, ""quoted"" note\nsecond line"'));
  assert.ok(csv.includes('\r\n'));
  assert.ok(csv.split("\r\n")[0]!.includes("archived"));
});

test("web demo strips heartbeat trio even if an old snapshot listed them installed", () => {
  const next = withFreeModules(["timer", "heartbeat", "heartbeat-log", "logs-exports", "themes"]);
  assert.ok(next.includes("timer"));
  for (const id of WEB_LOCKED) assert.equal(next.includes(id), false);
  assert.equal(FREE_MODULE_IDS.includes("heartbeat"), false);
  assert.equal(FREE_MODULE_IDS.includes("heartbeat-log"), false);
  assert.equal(FREE_MODULE_IDS.includes("logs-exports"), false);
});

test("Chicago masthead clock uses America/Chicago and 12-hour time", () => {
  const stamp = chicagoStamp(Date.UTC(2026, 8, 18, 19, 1, 0));
  assert.match(stamp.line, /2026-09-18/);
  assert.match(stamp.line, /America\/Chicago/);
  assert.match(stamp.tz, /C[DS]T/);
  assert.match(stamp.time, /\d{1,2}:\d{2} (AM|PM)/);
});

test("opening the bell does not mark notices read; markKindRead clears one folder only", () => {
  const s = useAuric.getState();
  s.pushNotice({ kind: "timer", title: "Timer", body: "done", archived: false });
  s.pushNotice({ kind: "alarm", title: "Alarm", body: "ring", archived: false });
  s.setNoticeOpen(true);
  assert.equal(useAuric.getState().noticeOpen, true);
  assert.equal(useAuric.getState().notices.filter((n) => n.kind === "timer" && !n.read).length, 1);
  assert.equal(useAuric.getState().notices.filter((n) => n.kind === "alarm" && !n.read).length, 1);
  s.markKindRead("timer");
  assert.equal(useAuric.getState().notices.filter((n) => n.kind === "timer" && !n.read).length, 0);
  assert.equal(useAuric.getState().notices.filter((n) => n.kind === "timer" && n.read).length, 1);
  assert.equal(useAuric.getState().notices.filter((n) => n.kind === "alarm" && !n.read).length, 1);
});
