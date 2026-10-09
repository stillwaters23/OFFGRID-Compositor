import assert from "node:assert/strict";
import { test } from "node:test";
import { chromium } from "playwright";

const baseUrl = process.env.L3_BASE_URL ?? "http://127.0.0.1:8080";

test("L3 drag snap-back stays in the viewport at every sampled frame", { skip: !process.env.L3_E2E }, async (t) => {
  const browser = await chromium.launch({ headless: true });
  const page = await browser.newPage({ viewport: { width: 320, height: 800 } });
  t.after(() => browser.close());
  await page.goto(baseUrl);
  await page.waitForSelector(".tx-root");
  await page.evaluate(() => localStorage.setItem("auric-crowned-v21", JSON.stringify({ state: { shell: "home", appearanceMode: "dark" }, version: 0 })));

  for (const width of [320, 390]) {
    await page.setViewportSize({ width, height: 800 });
    for (const edge of ["top", "bottom", "left", "right"]) {
      await page.evaluate(() => localStorage.removeItem("auric-folder-settings-v1"));
      await page.reload();
      await page.waitForSelector(".tx-folder-grid[data-layout='big']");
      await page.waitForTimeout(350);
      const tile = page.locator('[data-folder-id="timer"] .tx-folder-tile');
      await tile.scrollIntoViewIfNeeded();
      const start = await tile.boundingBox();
      assert.ok(start);
      const savedOrder = await page.evaluate(() => JSON.parse(localStorage.getItem("auric-folder-settings-v1") ?? "{}").folderOrder ?? []);
      const origin = { x: start.x + start.width / 2, y: start.y + start.height / 2 };
      const end = {
        x: edge === "left" ? -40 : edge === "right" ? width + 40 : origin.x,
        y: edge === "top" ? -40 : edge === "bottom" ? 840 : origin.y,
      };
      await page.evaluate(() => {
        window.__folderSamples = [];
        window.__sampleFolder = true;
        const sample = () => {
          if (!window.__sampleFolder) return;
          const button = document.querySelector('[data-folder-id="timer"] .tx-folder-tile');
          if (button) {
            const rect = button.getBoundingClientRect();
            window.__folderSamples.push([rect.left, rect.top, rect.right, rect.bottom]);
          }
          requestAnimationFrame(sample);
        };
        requestAnimationFrame(sample);
      });
      await page.mouse.move(origin.x, origin.y);
      await page.mouse.down();
      await page.mouse.move(end.x, end.y, { steps: 20 });
      await page.mouse.up();
      const releasedAt = Date.now();
      await page.waitForFunction(
        () => document.querySelector('[data-folder-id="timer"] .tx-folder-tile')?.style.transform === "",
        { timeout: 500 },
      );
      assert.ok(Date.now() - releasedAt < 500, `${width}px ${edge}: spring did not settle under 500ms`);
      await page.evaluate(() => (window.__sampleFolder = false));
      const samples = await page.evaluate(() => window.__folderSamples);
      const finalBox = await tile.boundingBox();
      assert.ok(samples.length > 0, `${width}px ${edge}: no animation frames sampled`);
      assert.ok(samples.every(([left, top, right, bottom]) => left >= 0 && top >= 0 && right <= width && bottom <= 800), `${width}px ${edge}: escaped viewport during spring`);
      assert.ok(finalBox);
      assert.ok(
        finalBox.x >= 0 && finalBox.y >= 0 && finalBox.x + finalBox.width <= width && finalBox.y + finalBox.height <= 800,
        `${width}px ${edge}: final folder box escaped viewport (${JSON.stringify(finalBox)})`,
      );
      assert.deepEqual(
        await page.evaluate(() => JSON.parse(localStorage.getItem("auric-folder-settings-v1") ?? "{}").folderOrder ?? []),
        savedOrder,
        `${width}px ${edge}: edge release must not reorder folders`,
      );
    }
  }
});

test("L3 reorder persists and reduced-motion snap-back is immediate", { skip: !process.env.L3_E2E }, async (t) => {
  const browser = await chromium.launch({ headless: true });
  const page = await browser.newPage({ viewport: { width: 390, height: 844 } });
  t.after(() => browser.close());
  await page.goto(baseUrl);
  await page.waitForSelector(".tx-root");
  await page.evaluate(() => {
    localStorage.setItem("auric-crowned-v21", JSON.stringify({ state: { shell: "home", appearanceMode: "dark" }, version: 0 }));
    localStorage.removeItem("auric-folder-settings-v1");
  });
  await page.reload();
  await page.waitForSelector(".tx-folder-grid[data-layout='big']");
  await page.waitForTimeout(350);

  const source = page.locator('[data-folder-id="timer"] .tx-folder-tile');
  const target = page.locator('[data-folder-id="stopwatch"] .tx-folder-tile');
  const sourceBox = await source.boundingBox();
  const targetBox = await target.boundingBox();
  assert.ok(sourceBox && targetBox);
  await page.mouse.move(sourceBox.x + sourceBox.width / 2, sourceBox.y + sourceBox.height / 2);
  await page.mouse.down();
  await page.mouse.move(targetBox.x + targetBox.width / 2, targetBox.y + targetBox.height / 2, { steps: 10 });
  await page.mouse.up();
  await page.waitForFunction(() => {
    const saved = JSON.parse(localStorage.getItem("auric-folder-settings-v1") ?? "{}");
    return saved.folderOrder?.indexOf("stopwatch") < saved.folderOrder?.indexOf("timer");
  });
  await page.reload();
  await page.waitForSelector(".tx-folder-grid");
  const order = await page.locator(".tx-folder-grid > li").evaluateAll((items) => items.map((item) => item.dataset.folderId));
  assert.ok(order.indexOf("stopwatch") < order.indexOf("timer"));

  await page.emulateMedia({ reducedMotion: "reduce" });
  const tile = page.locator('[data-folder-id="timer"] .tx-folder-tile');
  await tile.scrollIntoViewIfNeeded();
  const box = await tile.boundingBox();
  assert.ok(box);
  await page.mouse.move(box.x + box.width / 2, box.y + box.height / 2);
  await page.mouse.down();
  await page.mouse.move(-40, box.y + box.height / 2, { steps: 8 });
  await page.mouse.up();
  const releasedAt = Date.now();
  await page.waitForFunction(() => document.querySelector('[data-folder-id="timer"] .tx-folder-tile')?.style.transform === "", { timeout: 100 });
  assert.ok(Date.now() - releasedAt < 100);
});
