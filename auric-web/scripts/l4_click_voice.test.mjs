import assert from "node:assert/strict";
import { spawnSync } from "node:child_process";
import { dirname, join } from "node:path";
import { fileURLToPath } from "node:url";
import test from "node:test";

const root = join(dirname(fileURLToPath(import.meta.url)), "..");

test("L4 click voice, folder prefs, and catalog tests pass", () => {
  const run = spawnSync(
    process.execPath,
    [
      "--import",
      "./scripts/test-aliases.mjs",
      "--experimental-strip-types",
      "--test",
      "tests/l4_click_voice.test.ts",
      "tests/l4_folder_prefs.test.ts",
      "tests/l4_clicker_sounds.test.ts",
    ],
    { cwd: root, encoding: "utf8" },
  );
  assert.equal(run.status, 0, `${run.stdout}\n${run.stderr}`);
});
