import assert from "node:assert/strict";
import { readdirSync, readFileSync, statSync } from "node:fs";
import { join } from "node:path";
import { fileURLToPath } from "node:url";
import test from "node:test";
import { rowsCrossed } from "../src/lib/reel-geometry.ts";

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

test("Pavilion SoundDeck keeps the default aperture selector", () => {
  const source = readFileSync(join(SRC, "components/auric/pavilion.tsx"), "utf8");
  const deck = source.slice(source.indexOf("function SoundDeck"));
  const reel = deck.slice(deck.indexOf("<DrumReel"), deck.indexOf("/>"));
  assert.ok(reel.includes("<DrumReel"));
  assert.equal(reel.includes("selector"), false);
});
