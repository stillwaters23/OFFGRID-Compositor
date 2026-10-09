import assert from "node:assert/strict";
import { test } from "node:test";
import { chromium } from "playwright";

const baseUrl = process.env.L3_BASE_URL ?? "http://127.0.0.1:8080";

test("L3 layout sheet, rename, size, and icon styles persist", { skip: !process.env.L3_E2E }, async (t) => {
  const browser = await chromium.launch({ headless: true });
  const page = await browser.newPage({ viewport: { width: 390, height: 844 } });
  t.after(() => browser.close());
  await page.addInitScript(() => {
    window.__folderVibrations = [];
    Object.defineProperty(navigator, "vibrate", {
      configurable: true,
      value: (value) => {
        window.__folderVibrations.push(value);
        return true;
      },
    });
  });
  await page.goto(baseUrl);
  await page.waitForSelector(".tx-root");
  await page.evaluate(() => {
    localStorage.setItem(
      "auric-crowned-v21",
      JSON.stringify({ state: { shell: "home", appearanceMode: "dark" }, version: 0 }),
    );
    localStorage.removeItem("auric-folder-settings-v1");
  });
  await page.reload();
  await page.waitForSelector(".tx-folder-grid");

  await page.getByRole("button", { name: "Folder layout settings" }).click();
  const sheet = page.getByRole("dialog", { name: "FOLDER LAYOUT" });
  await sheet.waitFor();
  await sheet.getByRole("button", { name: "GRID" }).click();
  await sheet.getByRole("button", { name: "BIG" }).click();
  await sheet.getByRole("button", { name: "SINGLE" }).click();
  assert.equal(await page.locator(".tx-folder-grid").getAttribute("data-layout"), "single");
  await sheet.getByLabel("Icon style for timer", { exact: true }).selectOption("3d");
  await sheet.getByRole("button", { name: "Close folder layout" }).click();
  await page.reload();
  await page.waitForSelector(".tx-folder-3d-icon");

  const name = page.locator('[data-folder-label="timer"]');
  const nameBox = await name.boundingBox();
  assert.ok(nameBox);
  await page.mouse.move(nameBox.x + nameBox.width / 2, nameBox.y + nameBox.height / 2);
  await page.mouse.down();
  await page.waitForTimeout(550);
  await page.mouse.up();
  const rename = page.getByRole("textbox", { name: "Rename timer folder" });
  await rename.waitFor();
  assert.equal(await rename.evaluate((input) => input === document.activeElement), true);
  assert.equal(await rename.evaluate((input) => input.selectionStart === 0 && input.selectionEnd === input.value.length), true);
  await rename.fill("Clockwork");
  await rename.press("Enter");
  assert.equal(await page.locator(".tx-folder-tile").getAttribute("aria-label"), "Clockwork unlocked");

  const icon = page.locator('[data-folder-id="timer"] .tx-folder-icon');
  const iconBox = await icon.boundingBox();
  assert.ok(iconBox);
  await page.mouse.move(iconBox.x + iconBox.width / 2, iconBox.y + iconBox.height / 2);
  await page.mouse.down();
  await page.waitForTimeout(550);
  await page.mouse.up();
  await page.getByRole("slider", { name: "Size timer folder" }).waitFor();
  assert.deepEqual(await page.evaluate(() => window.__folderVibrations), [7]);
  await page.getByRole("slider", { name: "Size timer folder" }).evaluate((input) => {
    Object.getOwnPropertyDescriptor(HTMLInputElement.prototype, "value").set.call(input, "72");
    input.dispatchEvent(new Event("input", { bubbles: true }));
  });
  await page.reload();
  await page.waitForFunction(() => localStorage.getItem("auric-folder-settings-v1")?.includes('"timer":72'));
  const saved = await page.evaluate(() => JSON.parse(localStorage.getItem("auric-folder-settings-v1")));
  assert.equal(saved.folderSizes.timer, 72);
  assert.equal(saved.folderNames.timer, "Clockwork");
  assert.equal(saved.folderIconStyle.timer, "3d");
});

test("L3 layout flip is transform and opacity only and reduced motion is instant", { skip: !process.env.L3_E2E }, async (t) => {
  const browser = await chromium.launch({ headless: true });
  const page = await browser.newPage({ viewport: { width: 390, height: 844 } });
  t.after(() => browser.close());
  await page.goto(baseUrl);
  await page.waitForSelector(".tx-root");
  await page.evaluate(() => {
    localStorage.setItem(
      "auric-crowned-v21",
      JSON.stringify({ state: { shell: "home", appearanceMode: "dark" }, version: 0 }),
    );
    localStorage.removeItem("auric-folder-settings-v1");
  });
  await page.reload();
  await page.waitForSelector(".tx-folder-grid");
  await page.getByRole("button", { name: "Folder layout settings" }).click();
  const sheet = page.getByRole("dialog", { name: "FOLDER LAYOUT" });
  await sheet.getByRole("button", { name: "GRID" }).click();
  const animations = await page.locator(".tx-folder-tile").evaluateAll((tiles) =>
    tiles.flatMap((tile) => tile.getAnimations().map((animation) => ({
      duration: animation.effect.getTiming().duration,
      keyframes: animation.effect.getKeyframes(),
    }))),
  );
  assert.ok(animations.some((animation) => animation.duration === 320));
  assert.ok(animations.flatMap((animation) => animation.keyframes).every((frame) =>
    Object.keys(frame).every((property) => ["offset", "computedOffset", "easing", "composite", "transform", "opacity"].includes(property)),
  ));

  await page.emulateMedia({ reducedMotion: "reduce" });
  await sheet.getByRole("button", { name: "BIG" }).click();
  assert.equal(await page.locator(".tx-folder-tile").evaluateAll((tiles) => tiles.every((tile) => tile.getAnimations().length === 0)), true);
});
