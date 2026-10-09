import assert from "node:assert/strict";
import { test } from "node:test";
import { folderMetrics, hitZone, paddedLabelRect } from "../src/lib/folder-layout.ts";

test("folder metrics clamp size and choose layout columns by available width", () => {
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
