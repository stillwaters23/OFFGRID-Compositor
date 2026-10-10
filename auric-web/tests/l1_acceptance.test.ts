import assert from "node:assert/strict";
import { existsSync, readdirSync, readFileSync, statSync } from "node:fs";
import { join } from "node:path";
import { fileURLToPath } from "node:url";
import test from "node:test";
import { rowsCrossed } from "../src/lib/reel_geometry.ts";

const SRC = fileURLToPath(new URL("../src", import.meta.url));

function walk(dir: string): string[] {
  return readdirSync(dir).flatMap((name) => {
    const path = join(dir, name);
    return statSync(path).isDirectory() ? walk(path) : [path];
  });
}

test("rowsCrossed counts each integer row and ignores a hold", () => {
  assert.equal(rowsCrossed(4, 4.2), 0);
  assert.equal(rowsCrossed(4, 7.4), 3);
  assert.equal(rowsCrossed(7, 4.6), 2);
  assert.equal(rowsCrossed(Number.NaN, 3), 0);
});

test("span reels are the only drums that hide the aperture", () => {
  const optedOut = walk(SRC)
    .filter((path) => path.endsWith(".tsx"))
    .filter((path) => readFileSync(path, "utf8").includes('selector="none"'))
    .map((path) => path.slice(path.indexOf("/src/") + 5));
  assert.deepEqual(optedOut, ["components/auric/span-reels.tsx"]);
});

test("new L1 files use underscores and the empty lane hooks are archived", () => {
  for (const path of [
    "components/auric/laser_strip.tsx",
    "lib/reel_geometry.ts",
    "routes/laser_demo.tsx",
  ]) {
    assert.equal(existsSync(join(SRC, path)), true, path);
  }
  for (const path of [
    "components/auric/laser-strip.tsx",
    "lib/reel-geometry.ts",
    "routes/laser-demo.tsx",
    "styles/reel-look.css",
    "styles/folders.css",
    "styles/clicker.css",
  ]) {
    assert.equal(existsSync(join(SRC, path)), false, path);
  }
  const css = readFileSync(join(SRC, "styles.css"), "utf8");
  assert.equal(css.includes("./styles/reel-look.css"), false);
  assert.equal(css.includes("./styles/folders.css"), false);
  assert.equal(css.includes("./styles/clicker.css"), false);
  assert.equal(css.includes(".laser-strip"), true);
  const route = readFileSync(join(SRC, "routes/laser_demo.tsx"), "utf8");
  assert.equal(route.includes('createFileRoute("/laser_demo")'), true);
  const root = fileURLToPath(new URL("..", import.meta.url));
  assert.equal(existsSync(join(root, "archive/2026_10_09_l1/README.md")), true);
  assert.equal(existsSync(join(root, "archive/2026-10-09-l1")), false);
});

test("Pavilion SoundDeck keeps the default aperture selector", () => {
  const source = readFileSync(join(SRC, "components/auric/pavilion.tsx"), "utf8");
  const deck = source.slice(source.indexOf("function SoundDeck"));
  const reel = deck.slice(deck.indexOf("<DrumReel"), deck.indexOf("/>"));
  assert.ok(reel.includes("<DrumReel"));
  assert.equal(reel.includes("selector"), false);
});
