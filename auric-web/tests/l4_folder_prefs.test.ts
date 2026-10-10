import assert from "node:assert/strict";
import { test } from "node:test";
import { MODULES } from "../src/lib/modules.ts";
import {
  CLICKER_MODULE_ID,
  FOLDER_PREFERENCES_KEY,
  clickerFolderView,
  clickerTitlePx,
  orderWithClicker,
} from "../src/lib/clicker_folder_prefs.ts";

test("clicker-sounds reads Lane 3 size, icon style, and rename", () => {
  assert.equal(FOLDER_PREFERENCES_KEY, "auric-folder-settings-v1");
  const view = clickerFolderView({
    layout: "grid",
    folderSizeAll: 20,
    folderSizes: { [CLICKER_MODULE_ID]: 72 },
    folderOrder: ["timer"],
    folderIconStyle: { [CLICKER_MODULE_ID]: "etched" },
    folderNames: { [CLICKER_MODULE_ID]: "  House Clicks  " },
  });
  assert.equal(view.tileName, "House Clicks");
  assert.equal(view.customName, "House Clicks");
  assert.equal(view.size, 72);
  assert.equal(view.iconStyle, "etched");
  assert.equal(view.titlePx, 24);
});

test("blank names are refused, long names stop at 24, and missing fields use Lane 3 defaults", () => {
  const blank = clickerFolderView({
    folderSizeAll: 10,
    folderNames: { [CLICKER_MODULE_ID]: "   " },
    folderIconStyle: { [CLICKER_MODULE_ID]: "nope" },
  });
  assert.equal(blank.tileName, "clicker-sounds");
  assert.equal(blank.customName, "");
  assert.equal(blank.size, 10);
  assert.equal(blank.iconStyle, "flat");
  assert.equal(blank.titlePx, 16);

  const longName = "House Clicks Beyond Twenty Four";
  const clipped = clickerFolderView({ folderNames: { [CLICKER_MODULE_ID]: longName } });
  assert.equal(clipped.customName.length, 24);
  assert.equal(clipped.customName, longName.trim().slice(0, 24));
  assert.equal(clickerFolderView(null).size, 50);
  assert.equal(clickerTitlePx(0), 16);
  assert.equal(clickerTitlePx(49), 16);
  assert.equal(clickerTitlePx(50), 24);
});

test("a new clicker id appends after a saved order that does not know it yet", () => {
  const ids = MODULES.map((module) => module.id);
  const saved = ids.filter((id) => id !== CLICKER_MODULE_ID);
  const ordered = orderWithClicker(ids, saved);
  assert.equal(ordered.at(-1), CLICKER_MODULE_ID);
  assert.equal(ordered.length, ids.length);
  assert.deepEqual(ordered.slice(0, -1), saved);
});
