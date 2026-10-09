import assert from "node:assert/strict";
import { mkdirSync } from "node:fs";
import { resolve } from "node:path";
import { chromium } from "playwright";

const baseUrl = process.env.L1_BASE_URL ?? "http://127.0.0.1:8080";
const browser = await chromium.launch({
  headless: true,
  args: ["--no-sandbox", "--disable-dev-shm-usage"],
});
const page = await browser.newPage({ viewport: { width: 1280, height: 900 } });
const errors = [];
page.on("pageerror", (error) => errors.push(error.message));
page.on("console", (message) => {
  if (message.type() === "error") errors.push(message.text());
});

try {
  await page.goto(`${baseUrl}/laser-demo`, { waitUntil: "domcontentloaded" });
  await page.getByRole("heading", { name: "Laser Strip Lab" }).waitFor();
  assert.equal(await page.locator(".l1-demo-sample").count(), 15);
  assert.equal(await page.locator(".laser-strip").count(), 30);
  assert.equal(await page.locator(".l1-demo-sample .aperture").count(), 0);

  mkdirSync(resolve("screenshots/l1"), { recursive: true });
  await page.screenshot({ path: "screenshots/l1/laser-demo.png", fullPage: true });

  const geometry = await page.evaluate(() => {
    const strips = [...document.querySelectorAll(".laser-strip")];
    return {
      overflow: document.documentElement.scrollWidth > document.documentElement.clientWidth,
      barsInside: [...document.querySelectorAll(".barrel")].every((barrel) => {
        const box = barrel.getBoundingClientRect();
        return box.left >= -1 && box.right <= innerWidth + 1;
      }),
      selectedInside: [...document.querySelectorAll(".barrel")].every((barrel) => {
        const selected = barrel.querySelector('[data-selected="1"]');
        if (!selected) return false;
        const barrelBox = barrel.getBoundingClientRect();
        const selectedBox = selected.getBoundingClientRect();
        return (
          selectedBox.left >= barrelBox.left - 1 &&
          selectedBox.right <= barrelBox.right + 1 &&
          selectedBox.top >= barrelBox.top - 1 &&
          selectedBox.bottom <= barrelBox.bottom + 1
        );
      }),
      stripsAligned: strips.every((strip) => {
        const host = strip.parentElement;
        const barrels = [...host.querySelectorAll(".barrel")];
        const stripBox = strip.getBoundingClientRect();
        const firstBox = barrels[0].getBoundingClientRect();
        const lastBox = barrels.at(-1).getBoundingClientRect();
        return (
          Math.abs(stripBox.top + stripBox.height / 2 - (firstBox.top + firstBox.height / 2)) <= 1 &&
          stripBox.left <= firstBox.left - 15 &&
          stripBox.right >= lastBox.right + 15 &&
          stripBox.left >= -1 &&
          stripBox.right <= innerWidth + 1
        );
      }),
    };
  });
  assert.deepEqual(geometry, {
    overflow: false,
    barsInside: true,
    selectedInside: true,
    stripsAligned: true,
  });

  for (const mood of ["dark", "light"]) {
    for (const scale of [0, 50, 100]) {
      await page.getByRole("button", { name: `${mood} mood` }).click();
      await page.getByRole("button", { name: `Digit scale ${scale}` }).click();
      for (const width of [320, 390, 768, 1280]) {
        await page.setViewportSize({ width, height: 844 });
        await page.waitForTimeout(80);
        const matrix = await page.evaluate(() => ({
          overflow: document.documentElement.scrollWidth > document.documentElement.clientWidth,
          barsInside: [...document.querySelectorAll(".barrel")].every((barrel) => {
            const box = barrel.getBoundingClientRect();
            return box.left >= -1 && box.right <= innerWidth + 1;
          }),
          stripsAligned: [...document.querySelectorAll(".laser-strip")].every((strip) => {
            const barrels = [...strip.parentElement.querySelectorAll(".barrel")];
            const stripBox = strip.getBoundingClientRect();
            const first = barrels[0].getBoundingClientRect();
            const last = barrels.at(-1).getBoundingClientRect();
            return (
              Math.abs(stripBox.top + stripBox.height / 2 - (first.top + first.height / 2)) <= 1 &&
              stripBox.left <= first.left - 15 &&
              stripBox.right >= last.right + 15 &&
              stripBox.left >= -1 &&
              stripBox.right <= innerWidth + 1
            );
          }),
        }));
        assert.equal(matrix.overflow, false, `${mood} scale ${scale} width ${width}: overflow`);
        assert.equal(matrix.barsInside, true, `${mood} scale ${scale} width ${width}: barrel outside`);
        assert.equal(matrix.stripsAligned, true, `${mood} scale ${scale} width ${width}: strip misaligned`);
      }
    }
  }

  await page.setViewportSize({ width: 768, height: 844 });
  await page.getByRole("button", { name: "dark mood" }).click();
  await page.getByRole("button", { name: "Digit scale 50" }).click();
  const sample = page.locator(".l1-demo-sample").first();
  const tickOutput = sample.locator("[data-l1-ticks]");
  const slider = sample.getByRole("slider", { name: "Hours" });
  const beforeHold = Number(await tickOutput.textContent());
  await slider.focus();
  await page.waitForTimeout(200);
  assert.equal(Number(await tickOutput.textContent()), beforeHold, "still reel must not tick");

  const box = await slider.boundingBox();
  await page.mouse.move(box.x + box.width / 2, box.y + box.height / 2);
  await page.mouse.down();
  await page.mouse.move(box.x + box.width / 2, box.y + box.height / 2 - 84, { steps: 1 });
  assert.equal(Number(await tickOutput.textContent()), beforeHold + 3, "three dragged rows must tick three times");
  await page.mouse.up();
  await slider.dispatchEvent("pointercancel", { pointerId: 1, pointerType: "mouse" });
  const afterDrag = Number(await tickOutput.textContent());
  await slider.focus();
  for (let step = 0; step < 5; step += 1) await page.keyboard.press("ArrowUp");
  assert.equal(Number(await tickOutput.textContent()), afterDrag + 5, "five keyboard rows must tick five times");

  const cdp = await page.context().newCDPSession(page);
  const frameStats = async (throttleRate) => {
    await cdp.send("Emulation.setCPUThrottlingRate", { rate: throttleRate });
    const samples = page.evaluate(
      () =>
        new Promise((resolve) => {
          const times = [];
          let previous = performance.now();
          const start = previous;
          const frame = (now) => {
            times.push(now - previous);
            previous = now;
            if (now - start < 3000) requestAnimationFrame(frame);
            else {
              times.shift();
              times.sort((a, b) => a - b);
              resolve({
                medianMs: times[Math.floor(times.length * 0.5)],
                p95Ms: times[Math.floor(times.length * 0.95)],
              });
            }
          };
          requestAnimationFrame(frame);
        }),
    );
    const reelBox = await sample.getByRole("slider", { name: "Days" }).boundingBox();
    await page.mouse.move(reelBox.x + reelBox.width / 2, reelBox.y + reelBox.height / 2);
    await page.mouse.down();
    await page.mouse.move(reelBox.x + reelBox.width / 2, reelBox.y - 400, { steps: 6 });
    await page.mouse.up();
    return samples;
  };
  const perfNormal = await frameStats(1);
  const perfThrottled = await frameStats(4);
  await cdp.send("Emulation.setCPUThrottlingRate", { rate: 1 });
  console.log(JSON.stringify({ perfNormal, perfThrottled }));
  assert.deepEqual(errors, []);
} finally {
  await browser.close();
}
