import assert from "node:assert/strict";
import { test } from "node:test";
import {
  folderMetrics,
  hitZone,
  normalizeFolderPreferences,
  orderFolderIds,
  paddedLabelRect,
} from "../src/lib/folder-layout.ts";

test("folder metrics clamp size and choose layout columns by available width", () => {
  assert.equal(folderMetrics(100, 8, "grid").iconPx, 0);
  for (const width of [320, 390, 1280]) {
    for (const size of [0, 50, 100]) {
      const grid = folderMetrics(size, width, "grid");
      assert.equal(grid.columns, width < 600 ? 2 : 3);
      assert.ok(grid.tileWidth <= width / grid.columns);
      assert.ok(grid.iconPx <= grid.tileWidth);
      assert.ok(grid.titlePx >= 12);
      assert.ok(grid.rowHeight >= 44);
      assert.ok(folderMetrics(size, width, "big").columns === 1);
      assert.ok(folderMetrics(size, width, "single").columns === 1);
    }
  }

  assert.equal(folderMetrics(50, 390, "big").iconPx, 56);
  assert.equal(folderMetrics(-20, 390, "big").titlePx, 12);
  assert.equal(folderMetrics(150, 390, "big").titlePx, 16);
  assert.ok(folderMetrics(100, 320, "grid").iconPx < folderMetrics(100, 320, "big").iconPx);
});

test("label hit zone expands to 44px and stays within tile bounds", () => {
  const tile = { x: 0, y: 0, width: 160, height: 72 };
  const title = { x: 50, y: 28, width: 42, height: 14 };
  const label = paddedLabelRect(tile, title);
  assert.equal(label.x, 42);
  assert.equal(label.width, 58);
  assert.equal(label.height, 44);
  assert.equal(hitZone({ x: 42, y: 50 }, tile, title), "label");
  assert.equal(hitZone({ x: 41, y: 50 }, tile, title), "body");
  assert.equal(hitZone({ x: 159, y: 50 }, tile, title), "body");
});

test("folder preference hydration clamps values and appends newly installed module ids", () => {
  assert.deepEqual(orderFolderIds(["timer", "clock", "new"], ["clock", "removed"]), ["clock", "timer", "new"]);
  const restored = normalizeFolderPreferences(
    {
      layout: "single",
      folderSizeAll: 120,
      folderSizes: { timer: -5, clock: 72 },
      folderOrder: ["clock"],
      folderIconStyle: { timer: "3d", clock: "unknown" },
      folderNames: { timer: "  Clockwork  ", clock: "   ", long: "01234567890123456789012345" },
    },
    ["timer", "clock", "new"],
  );
  assert.deepEqual(restored, {
    layout: "single",
    folderSizeAll: 100,
    folderSizes: { timer: 0, clock: 72 },
    folderOrder: ["clock", "timer", "new"],
    folderIconStyle: { timer: "3d" },
    folderNames: { timer: "Clockwork", long: "012345678901234567890123" },
  });
});
