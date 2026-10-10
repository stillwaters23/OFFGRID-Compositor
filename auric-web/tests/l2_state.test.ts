import assert from "node:assert/strict";
import { test } from "node:test";
import { useAuric } from "../src/lib/store.ts";
import { auricStorage } from "../src/lib/storage.ts";
import { BLUE_DIAMOND } from "../src/lib/palettes.ts";

const values = new Map<string, string>();
Object.defineProperty(globalThis, "localStorage", {
  configurable: true,
  value: {
    getItem: (key: string) => values.get(key) ?? null,
    setItem: (key: string, value: string) => values.set(key, value),
    removeItem: (key: string) => values.delete(key),
  },
});

function reset() {
  values.clear();
  useAuric.setState({ ...useAuric.getInitialState(), appearanceDraft: null });
}

test("L2 state contract defaults and all ten settings persist", () => {
  reset();
  const state = useAuric.getState();
  assert.deepEqual(state.numberColor, { dark: null, light: null });
  assert.equal(state.laserEnabled, true);
  assert.equal(state.laserIntensity, 100);
  assert.equal(state.laserColor, "#FF0000");
  assert.equal(state.folderSizeAll, 50);
  assert.deepEqual(state.folderSizes, {});
  assert.equal(state.folderLayout, "big");
  assert.deepEqual(state.folderIconStyle, {});
  assert.deepEqual(state.folderNames, {});
  assert.deepEqual(state.folderOrder, []);

  state.setLaserEnabled(false);
  state.setLaserIntensity(40);
  assert.deepEqual(state.setLaserColor("#f0a"), { ok: true });
  state.setFolderSizeAll(72);
  state.setFolderSize("timer", 64);
  state.setFolderLayout("grid");
  state.setFolderIconStyle("timer", "etched");
  assert.equal(state.setFolderName("timer", "Clock"), true);
  state.setFolderOrder(["timer", "unknown", "alarm", "timer"]);
  const partial = useAuric.persist.getOptions().partialize!(useAuric.getState() as never) as Record<string, unknown>;
  for (const key of [
    "laserEnabled", "laserIntensity", "laserColor", "numberColor", "folderSizeAll",
    "folderSizes", "folderLayout", "folderIconStyle", "folderNames", "folderOrder",
  ]) assert.ok(key in partial, `partialize includes ${key}`);
  assert.deepEqual(partial.folderOrder, ["timer", "alarm"]);
});

test("state setters clamp, validate, and preserve invalid updates", () => {
  reset();
  const state = useAuric.getState();
  state.setLaserIntensity(-5);
  assert.equal(useAuric.getState().laserIntensity, 0);
  state.setLaserIntensity(150);
  assert.equal(useAuric.getState().laserIntensity, 100);
  state.setLaserIntensity(Number.NaN);
  assert.equal(useAuric.getState().laserIntensity, 100);

  state.setFolderSizeAll(-5);
  assert.equal(useAuric.getState().folderSizeAll, 0);
  state.setFolderSizeAll(150);
  assert.equal(useAuric.getState().folderSizeAll, 100);
  state.setFolderSizeAll(Number.NaN);
  assert.equal(useAuric.getState().folderSizeAll, 50);
  state.setFolderSize("timer", Number.NaN);
  assert.equal(useAuric.getState().folderSizes.timer, 50);
  state.setFolderSize("timer", null);
  assert.equal("timer" in useAuric.getState().folderSizes, false);

  assert.equal(state.setFolderName("timer", "  "), false);
  assert.equal(state.setFolderName("timer", "x".repeat(25)), false);
  assert.equal(state.setFolderName("timer", "  Timer  "), true);
  assert.equal(useAuric.getState().folderNames.timer, "Timer");
  state.setFolderLayout("invalid" as never);
  assert.equal(useAuric.getState().folderLayout, "big");
  state.setFolderIconStyle("timer", "invalid" as never);
  assert.deepEqual(useAuric.getState().folderIconStyle, {});
  assert.deepEqual(state.setLaserColor("red"), {
    ok: false,
    reason: "Use a valid opaque #RGB or #RRGGBB laser color.",
  });
  assert.equal(useAuric.getState().laserColor, "#FF0000");
  const rejected = state.setNumberColor("dark", "#000000");
  assert.equal(rejected.ok, false);
  if (!rejected.ok) {
    assert.match(rejected.reason, /dark/);
    assert.match(rejected.reason, /4\.5:1/);
  }
  assert.equal(useAuric.getState().numberColor.dark, null);
  assert.deepEqual(state.setNumberColor("dark", "#FFFFFF"), { ok: true });

  state.setLaserEnabled(false);
  useAuric.setState({ laserIntensity: 40, laserColor: "#FF00A6", folderSizeAll: 64 });
  state.restoreReadableAppearance();
  assert.deepEqual(useAuric.getState().numberColor, { dark: null, light: null });
  assert.equal(useAuric.getState().laserEnabled, false);
  assert.equal(useAuric.getState().laserIntensity, 40);
  assert.equal(useAuric.getState().laserColor, "#FF00A6");
  assert.equal(useAuric.getState().folderSizeAll, 64);
});

test("v21 snapshots hydrate with defaults while preserving existing data", async () => {
  reset();
  const snapshot = {
    state: {
      hours: 2,
      minutes: 13,
      paletteId: BLUE_DIAMOND.id,
      customPalette: null,
      sessions: [{ id: "keep-this-session", durationMs: 20 }],
    },
    version: 0,
  };
  values.set("auric-crowned-v21", JSON.stringify(snapshot));
  await useAuric.persist.rehydrate();
  const migrated = useAuric.getState();
  assert.equal(migrated.hours, 2);
  assert.equal(migrated.minutes, 13);
  assert.equal(migrated.sessions[0]?.id, "keep-this-session");
  assert.equal(migrated.laserEnabled, true);
  assert.equal(migrated.laserIntensity, 100);
  assert.equal(migrated.laserColor, "#FF0000");
  assert.deepEqual(migrated.numberColor, { dark: null, light: null });
  assert.equal(migrated.folderSizeAll, 50);
  assert.deepEqual(migrated.folderSizes, {});
  assert.equal(migrated.folderLayout, "big");
  assert.deepEqual(migrated.folderIconStyle, {});
  assert.deepEqual(migrated.folderNames, {});
  assert.deepEqual(migrated.folderOrder, []);
  assert.equal(auricStorage.getItem("auric-crowned-v21") !== null, true);
});

test("laser and number choices survive a persistence round trip", async () => {
  reset();
  const state = useAuric.getState();
  assert.deepEqual(state.setLaserColor("#FF00A6"), { ok: true });
  state.setLaserIntensity(40);
  state.setLaserEnabled(false);
  assert.deepEqual(state.setNumberColor("dark", "#EAF3FF"), { ok: true });
  const saved = values.get("auric-crowned-v21");
  assert.ok(saved);
  useAuric.setState({ ...useAuric.getInitialState(), appearanceDraft: null });
  values.set("auric-crowned-v21", saved);
  await useAuric.persist.rehydrate();
  const restored = useAuric.getState();
  assert.equal(restored.laserColor, "#FF00A6");
  assert.equal(restored.laserIntensity, 40);
  assert.equal(restored.laserEnabled, false);
  assert.equal(restored.numberColor.dark, "#EAF3FF");
});

test("hydration clears stored number colors that no longer pass contrast", async () => {
  reset();
  values.set("auric-crowned-v21", JSON.stringify({
    state: {
      numberColor: { dark: "#000000", light: null },
      laserEnabled: false,
    },
    version: 0,
  }));
  await useAuric.persist.rehydrate();
  assert.deepEqual(useAuric.getState().numberColor, { dark: null, light: null });
  assert.equal(useAuric.getState().toasts.filter((toast) => toast.title === "Number color reset").length, 1);
});

test("empty storage still completes hydration with defaults", async () => {
  reset();
  values.delete("auric-crowned-v21");
  await useAuric.persist.rehydrate();
  assert.equal(useAuric.persist.hasHydrated(), true);
  assert.equal(useAuric.getState().laserEnabled, true);
});
