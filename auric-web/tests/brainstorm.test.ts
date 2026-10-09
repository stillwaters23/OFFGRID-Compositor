import assert from "node:assert/strict";
import { afterEach, beforeEach, test } from "node:test";
import { useAuric, selectPalette } from "../src/lib/store.ts";
import { canApplyAppearance, previewPalette, boundedPercent } from "../src/lib/appearance.ts";
import { clockStamp, safeTimeZone, deviceTimeZone, schemeFromSchedule } from "../src/lib/clock.ts";
import { alarmSignal, reminderSignal, stopwatchSignal, timerSignal } from "../src/lib/instrument-signal.ts";

const initial = useAuric.getInitialState();
beforeEach(() => useAuric.setState({ ...initial }));
afterEach(() => useAuric.setState({ ...initial }));

test("floor, schedule and glow changes stay in the draft; Discard keeps committed values", () => {
  const state = useAuric.getState();
  state.beginAppearanceDraft();
  state.setDraftOptions({ appearanceMode: "light", scheduleEnabled: true, scheduleLight: "08:30", glowMode: !state.glowMode, floorId: "custom", floorCustom: "data:image/jpeg;base64,TEST" });
  for (const key of ["appearanceMode", "scheduleEnabled", "scheduleLight", "glowMode", "floorId", "floorCustom"] as const) assert.equal(useAuric.getState()[key], state[key]);
  state.discardAppearance();
  assert.equal(useAuric.getState().appearanceDraft, null);
  assert.equal(useAuric.getState().floorId, initial.floorId);
});

test("Apply commits every appearance option and matches the effective preview", () => {
  const state = useAuric.getState();
  state.beginAppearanceDraft();
  state.setDraftOptions({ appearanceMode: "light", scheduleEnabled: false, glowMode: true, floorId: "sunset" });
  state.setDraftScale(83);
  const draft = useAuric.getState().appearanceDraft!;
  const preview = previewPalette(draft, Date.now());
  assert.equal(state.applyAppearance().ok, true);
  assert.equal(selectPalette(useAuric.getState()).felt, preview.felt);
  assert.equal(useAuric.getState().floorId, "sunset");
  assert.equal(useAuric.getState().glowMode, true);
  assert.equal(useAuric.getState().digitScale, 83);
});

test("malformed or transparent essential colors fail closed without throwing", () => {
  const palette = selectPalette(initial);
  assert.equal(canApplyAppearance({ ...palette, digit: undefined as unknown as string }), false);
  assert.equal(canApplyAppearance({ ...palette, digit: "#FFFFFF00" }), false);
  assert.equal(canApplyAppearance(null as unknown as typeof palette), false);
});

test("Restore appearance repairs type size and background without changing user history", () => {
  const session = { id: "kept", kind: "timer" as const, note: "Keep this", startedAt: 1, endedAt: 2, durationMs: 1, usedCount: 1 };
  useAuric.setState({ digitScale: NaN, floorId: "custom", sessions: [session] });
  useAuric.getState().restoreReadableAppearance();
  assert.equal(useAuric.getState().digitScale, 50);
  assert.equal(useAuric.getState().floorId, "none");
  assert.deepEqual(useAuric.getState().sessions, [session]);
  assert.equal(boundedPercent(Infinity), 50);
});

test("invalid saved timezone recovers and explicit h23 uses midnight 00", () => {
  assert.equal(safeTimeZone("Moon/Unknown"), deviceTimeZone());
  assert.doesNotThrow(() => clockStamp(Date.now(), "h23", "Moon/Unknown"));
  assert.match(clockStamp(Date.UTC(2026, 8, 14), "h23", "UTC").time, /^00:/);
  assert.equal(schemeFromSchedule(Date.UTC(2026, 8, 14, 8), "07:00", "19:00", "UTC"), "light");
});

test("master activity signals distinguish running, completed, armed, and delivered", () => {
  assert.equal(timerSignal("running").tone, "red");
  assert.equal(timerSignal("running").pulse, true);
  assert.equal(timerSignal("done").tone, "green");
  assert.equal(stopwatchSignal("paused").tone, "green");
  assert.equal(alarmSignal([], "ring").pulse, true);
  assert.equal(reminderSignal(true, true).tone, "green");
});

test("archiving a note preserves recorded time and can be reversed by saving", () => {
  const state = useAuric.getState();
  const id = state.addSession({ kind: "timer", note: "draft", durationMs: 1000, startedAt: 1000, endedAt: 2000 });
  state.updateSessionNote(id, "Saved", true);
  assert.equal(useAuric.getState().sessions[0]!.noteArchived, true);
  assert.equal(useAuric.getState().sessions[0]!.durationMs, 1000);
  state.updateSessionNote(id, "Saved", false);
  assert.equal(useAuric.getState().sessions[0]!.noteArchived, false);
});

test("task save does not invent completion", () => {
  const state = useAuric.getState();
  state.addTask("Write");
  const id = useAuric.getState().tasks[0]!.id;
  state.updateTask(id, "Write more");
  assert.equal(useAuric.getState().tasks[0]!.done, false);
  assert.equal(useAuric.getState().tasks[0]!.text, "Write more");
});

test("device network and battery stay off until explicitly enabled", () => {
  assert.equal(useAuric.getState().deviceBatteryOn, false);
  assert.equal(useAuric.getState().deviceNetworkOn, false);
  useAuric.getState().setDeviceBatteryOn(true);
  assert.equal(useAuric.getState().deviceBatteryOn, true);
});

test("session shell opens home, folders stay visible, paid modules preview instead of installing", () => {
  const state = useAuric.getState();
  assert.equal(state.shell, "session");
  state.openSession();
  assert.equal(useAuric.getState().shell, "home");
  assert.equal(useAuric.getState().installedModules.includes("device"), true);
  assert.equal(useAuric.getState().installedModules.includes("themes"), false);
  assert.equal(useAuric.getState().installedModules.includes("timer"), true);
  assert.equal(useAuric.getState().installedModules.includes("stopwatch"), true);
  assert.equal(useAuric.getState().installedModules.includes("alarm"), true);
  state.enterModule("themes");
  assert.equal(useAuric.getState().previewModuleId, "themes");
  assert.equal(useAuric.getState().openModuleId, null);
  state.enterModule("device");
  assert.equal(useAuric.getState().openModuleId, "device");
  state.enterModule("timer");
  assert.equal(useAuric.getState().openModuleId, "timer");
  assert.equal(useAuric.getState().mode, "timer");
  assert.equal(useAuric.getState().shell, "home");
  state.minimizeShell();
  assert.equal(useAuric.getState().shell, "minimized");
  state.restoreShell();
  assert.equal(useAuric.getState().shell, "home");
  state.closeSession();
  assert.equal(useAuric.getState().shell, "closed");
});

test("heartbeat overlay is a single terminal ask and Off disables it", () => {
  const state = useAuric.getState();
  state.setHydrated();
  state.setHeartbeatEnabled(true);
  state.setHeartbeatMs(1_000);
  const t0 = Date.now();
  state.tick(t0);
  state.tick(t0 + 1_000);
  assert.equal(useAuric.getState().heartbeatBeatOpen, true);
  assert.equal(useAuric.getState().heartbeatLog.length, 1);
  state.tick(t0 + 2_000);
  state.tick(t0 + 3_000);
  assert.equal(useAuric.getState().heartbeatBeatOpen, true);
  assert.equal(useAuric.getState().heartbeatLog.length, 1);
  state.setHeartbeatBeatOpen(false);
  state.setHeartbeatEnabled(false);
  state.tick(t0 + 8_000);
  assert.equal(useAuric.getState().heartbeatBeatOpen, false);
  assert.equal(useAuric.getState().heartbeatLog.length, 1);
});


test("selectPalette keeps a stable reference while the look is unchanged", () => {
  const a = selectPalette(useAuric.getState());
  const b = selectPalette({ ...useAuric.getState(), now: useAuric.getState().now + 250 });
  assert.equal(a, b);
  useAuric.getState().beginAppearanceDraft();
  useAuric.getState().setDraftOptions({ appearanceMode: "light" });
  assert.equal(useAuric.getState().applyAppearance().ok, true);
  const light = selectPalette(useAuric.getState());
  assert.notEqual(light.felt, a.felt);
  assert.equal(selectPalette(useAuric.getState()), light);
});
