import assert from "node:assert/strict";
import { mkdirSync } from "node:fs";
import { fileURLToPath } from "node:url";
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
  assert.deepEqual(saved.folderSizes, { timer: 72 });
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
  await page.evaluate(() => {
    window.__l3FrameIntervals = [];
    window.__l3LastFrame = null;
    window.__l3RecordFrames = true;
    const sample = (time) => {
      if (!window.__l3RecordFrames) return;
      if (window.__l3LastFrame !== null) window.__l3FrameIntervals.push(time - window.__l3LastFrame);
      window.__l3LastFrame = time;
      requestAnimationFrame(sample);
    };
    requestAnimationFrame(sample);
    window.setTimeout(() => (window.__l3RecordFrames = false), 320);
  });
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
  await page.waitForTimeout(340);
  const frameIntervals = await page.evaluate(() => window.__l3FrameIntervals);
  const sortedIntervals = [...frameIntervals].sort((a, b) => a - b);
  const medianFrame = sortedIntervals[Math.floor(sortedIntervals.length / 2)] ?? 0;
  const p95Frame = sortedIntervals[Math.max(0, Math.ceil(sortedIntervals.length * 0.95) - 1)] ?? 0;
  t.diagnostic(
    `L3 layout FLIP frame interval: median=${medianFrame.toFixed(2)}ms p95=${p95Frame.toFixed(2)}ms max=${Math.max(...sortedIntervals).toFixed(2)}ms samples=${JSON.stringify(sortedIntervals)}`,
  );
  assert.ok(medianFrame <= 17.5 && p95Frame <= 25, "layout FLIP frame intervals exceed 60fps target");

  await page.emulateMedia({ reducedMotion: "reduce" });
  await sheet.getByRole("button", { name: "BIG" }).click();
  assert.equal(await page.locator(".tx-folder-tile").evaluateAll((tiles) => tiles.every((tile) => tile.getAnimations().length === 0)), true);
  await sheet.getByRole("button", { name: "SINGLE" }).click();
  await sheet.getByRole("button", { name: "Close folder layout" }).click();
  const current = page.locator(".tx-folder-grid > li");
  assert.equal(await current.getAttribute("data-folder-id"), "timer");
  await page.getByRole("button", { name: "Next folder" }).click();
  assert.equal(await current.getAttribute("data-folder-id"), "stopwatch");
  await page.getByRole("button", { name: "Previous folder" }).click();
  assert.equal(await current.getAttribute("data-folder-id"), "timer");
  const box = await page.locator(".tx-folder-tile").boundingBox();
  assert.ok(box);
  await page.mouse.move(box.x + box.width / 2, box.y + box.height / 2);
  await page.mouse.down();
  await page.mouse.move(box.x + box.width / 2 - 80, box.y + box.height / 2, { steps: 8 });
  await page.mouse.up();
  assert.equal(await current.getAttribute("data-folder-id"), "stopwatch");
});

test("L3 body long-press opens size and label long-press handles rename and keyboard parity", { skip: !process.env.L3_E2E }, async (t) => {
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
    localStorage.setItem("auric-crowned-v21", JSON.stringify({ state: { shell: "home", appearanceMode: "dark" }, version: 0 }));
    localStorage.removeItem("auric-folder-settings-v1");
  });
  await page.reload();
  await page.waitForSelector('.tx-folder-grid[data-layout="big"]');
  await page.waitForTimeout(350);

  const hold = async (locator) => {
    const box = await locator.boundingBox();
    assert.ok(box);
    await page.mouse.move(box.x + box.width / 2, box.y + box.height / 2);
    await page.mouse.down();
    await page.waitForTimeout(550);
    await page.mouse.up();
  };
  for (const selector of [
    '[data-folder-id="timer"] .tx-folder-icon',
    '[data-folder-id="timer"] .tx-folder-chevron',
  ]) {
    await hold(page.locator(selector));
    await page.getByRole("slider", { name: "Size timer folder" }).waitFor();
    assert.equal(await page.locator(".tx-folder-stage").count(), 0);
    assert.equal(await page.evaluate(() => window.__folderVibrations.at(-1)), 7);
    await page.getByRole("button", { name: "Close folder size" }).click();
  }
  const row = page.locator('[data-folder-id="timer"] .tx-folder-row');
  const rowBox = await row.boundingBox();
  assert.ok(rowBox);
  await page.mouse.move(rowBox.x + rowBox.width - 48, rowBox.y + rowBox.height - 10);
  await page.mouse.down();
  await page.waitForTimeout(550);
  await page.mouse.up();
  await page.getByRole("slider", { name: "Size timer folder" }).waitFor();
  assert.equal(await page.evaluate(() => window.__folderVibrations.filter((value) => value === 7).length), 3);
  assert.equal(await page.locator(".tx-folder-stage").count(), 0);
  await page.getByRole("button", { name: "Close folder size" }).click();

  const tile = page.locator('[data-folder-id="timer"] .tx-folder-tile');
  await tile.focus();
  await tile.press("F2");
  const rename = page.getByRole("textbox", { name: "Rename timer folder" });
  await rename.waitFor();
  assert.equal(await rename.evaluate((input) => input.selectionStart === 0 && input.selectionEnd === input.value.length), true);
  await rename.fill("   ");
  await rename.press("Enter");
  await page.locator('.tx-folder-rename [role="alert"]').waitFor();
  assert.equal(await page.locator('.tx-folder-rename [role="alert"]').textContent(), "Name can't be empty");
  assert.equal(await rename.evaluate((input) => input === document.activeElement), true);
  await rename.press("Escape");
  assert.equal(await tile.getAttribute("aria-label"), "timer unlocked");

  await tile.focus();
  await tile.press("F2");
  await page.getByRole("textbox", { name: "Rename timer folder" }).fill("x".repeat(25));
  await page.getByRole("textbox", { name: "Rename timer folder" }).press("Enter");
  assert.equal(await tile.getAttribute("aria-label"), `${"x".repeat(24)} unlocked`);

  await tile.focus();
  await tile.press("F2");
  await page.getByRole("textbox", { name: "Rename timer folder" }).fill("Saved on blur");
  await page.getByRole("textbox", { name: "Rename timer folder" }).press("Tab");
  assert.equal(await tile.getAttribute("aria-label"), "Saved on blur unlocked");

  await tile.focus();
  await tile.press("Shift+F10");
  await page.getByRole("slider", { name: "Size timer folder" }).waitFor();
  assert.equal(await page.locator(".tx-folder-stage").count(), 0);
  await page.getByRole("button", { name: "Close folder size" }).click();
  await tile.focus();
  await tile.press("Enter");
  await page.locator(".tx-folder-stage").waitFor();
  await page.getByRole("button", { name: "HOME" }).first().click();
  await page.getByRole("button", { name: "Folder layout settings" }).click();
  await page.getByRole("button", { name: "Reset name" }).click();
  assert.equal(await tile.getAttribute("aria-label"), "timer unlocked");
});

test("L3 icon styles cover every glyph and etched text stays matte and readable", { skip: !process.env.L3_E2E }, async (t) => {
  const browser = await chromium.launch({ headless: true });
  const page = await browser.newPage({ viewport: { width: 390, height: 2200 } });
  t.after(() => browser.close());
  await page.goto(baseUrl);
  await page.waitForSelector(".tx-root");
  const ids = ["timer", "stopwatch", "alarm", "themes", "sizing", "opacity", "icons", "alarm-sounds", "timer-sounds", "haptics", "heartbeat", "heartbeat-log", "logs-exports", "device", "tutorial"];
  const setStyles = (style) =>
    page.evaluate(({ ids, style }) => {
      localStorage.setItem("auric-crowned-v21", JSON.stringify({ state: { shell: "home", appearanceMode: "dark" }, version: 0 }));
      localStorage.setItem(
        "auric-folder-settings-v1",
        JSON.stringify({
          layout: "big",
          folderSizeAll: 50,
          folderSizes: {},
          folderOrder: [],
          folderIconStyle: Object.fromEntries(ids.map((id) => [id, style])),
          folderNames: {},
        }),
      );
    }, { ids, style });
  await page.evaluate(() => {
    localStorage.setItem("auric-crowned-v21", JSON.stringify({ state: { shell: "home", appearanceMode: "dark" }, version: 0 }));
    localStorage.removeItem("auric-folder-settings-v1");
  });
  await page.reload();
  await page.waitForSelector(".tx-folder-grid[data-layout='big']");
  await page.waitForTimeout(350);
  await page.evaluate(() => document.fonts.ready);
  const flatBaseline = await page.locator(".tx-folder-grid").screenshot({ animations: "disabled" });
  await page.getByRole("button", { name: "Folder layout settings" }).click();
  const sheet = page.getByRole("dialog", { name: "FOLDER LAYOUT" });
  for (const id of ids) {
    await sheet.getByLabel(`Icon style for ${id}`, { exact: true }).selectOption("flat");
  }
  await sheet.getByRole("button", { name: "Close folder layout" }).click();
  await page.waitForTimeout(350);
  const flatComparison = await page.locator(".tx-folder-grid").screenshot({ animations: "disabled" });
  const differencePercent = await page.evaluate(async ([before, after]) => {
    const load = (imageData) => new Promise((resolve) => {
      const image = new Image();
      image.onload = () => resolve(image);
      image.src = `data:image/png;base64,${imageData}`;
    });
    const [first, second] = await Promise.all([load(before), load(after)]);
    if (first.width !== second.width || first.height !== second.height) return 100;
    const canvas = document.createElement("canvas");
    canvas.width = first.width;
    canvas.height = first.height;
    const context = canvas.getContext("2d");
    context.drawImage(first, 0, 0);
    const left = context.getImageData(0, 0, first.width, first.height).data;
    context.clearRect(0, 0, canvas.width, canvas.height);
    context.drawImage(second, 0, 0);
    const right = context.getImageData(0, 0, second.width, second.height).data;
    let changed = 0;
    for (let index = 0; index < left.length; index += 4) {
      if (left[index] !== right[index] || left[index + 1] !== right[index + 1] || left[index + 2] !== right[index + 2]) changed++;
    }
    return (changed / (first.width * first.height)) * 100;
  }, [flatBaseline.toString("base64"), flatComparison.toString("base64")]);
  assert.ok(differencePercent <= 1, `flat icon style differs by ${differencePercent.toFixed(2)}%`);

  const screenshotDir = fileURLToPath(new URL("../../screenshots/l3/", import.meta.url));
  mkdirSync(screenshotDir, { recursive: true });
  await setStyles("3d");
  await page.reload();
  await page.waitForFunction((count) => document.querySelectorAll(".tx-folder-3d-icon").length === count, ids.length);
  await page.locator(".tx-folder-grid").screenshot({ path: `${screenshotDir}/icons-3d.png` });
  await setStyles("etched");
  await page.reload();
  await page.waitForFunction((count) => document.querySelectorAll(".tx-folder-art.is-etched").length === count, ids.length);
  await page.locator(".tx-folder-grid").screenshot({ path: `${screenshotDir}/icons-etched.png` });

  for (const mood of ["dark", "light"]) {
    await page.evaluate((appearanceMode) => {
      const saved = JSON.parse(localStorage.getItem("auric-crowned-v21"));
      saved.state = { ...saved.state, appearanceMode };
      localStorage.setItem("auric-crowned-v21", JSON.stringify(saved));
    }, mood);
    await page.reload();
    await page.waitForFunction(
      ({ expectedMood, count }) =>
        document.querySelector(".tx-root")?.dataset.mood === expectedMood &&
        document.querySelectorAll(".tx-folder-grid .tx-folder-name").length === count,
      { expectedMood: mood, count: ids.length },
    );
    await page.waitForTimeout(350);
    const textStyles = await page.locator(".tx-folder-name").evaluateAll((titles) => {
      const root = document.querySelector(".tx-root");
      const surface = root.dataset.mood === "light" ? [228, 234, 240] : [7, 16, 20];
      const luminance = (rgb) => rgb
        .map((channel) => channel / 255)
        .map((channel) => channel <= 0.04045 ? channel / 12.92 : ((channel + 0.055) / 1.055) ** 2.4)
        .reduce((sum, channel, index) => sum + channel * [0.2126, 0.7152, 0.0722][index], 0);
      const contrast = (first, second) => {
        const values = [luminance(first), luminance(second)].sort((a, b) => b - a);
        return (values[0] + 0.05) / (values[1] + 0.05);
      };
      return titles.map((title) => {
        const style = getComputedStyle(title);
        const color = style.color.match(/\d+/g).slice(0, 3).map(Number);
        const layers = style.textShadow.split(/,(?![^()]*\))/);
        return {
          contrast: contrast(color, surface),
          shadowLayers: layers.length,
          shadowBlur: Math.max(...layers.map((layer) => Number(layer.match(/(-?[\d.]+)px\s*$/)?.[1] ?? 0))),
          backgroundClip: style.backgroundClip,
        };
      });
    });
    assert.equal(textStyles.length, ids.length);
    assert.ok(textStyles.every((style) => style.contrast >= 4.5), `${mood}: title contrast below 4.5:1`);
    assert.ok(textStyles.every((style) => style.shadowLayers <= 2 && style.shadowBlur <= 4), `${mood}: text shadow exceeds matte limit`);
    assert.ok(textStyles.every((style) => style.backgroundClip !== "text"), `${mood}: text sheen detected`);
  }
});
