import assert from "node:assert/strict";
import { test } from "node:test";
import { chromium } from "playwright";

const baseUrl = process.env.AURIC_BASE_URL ?? "http://127.0.0.1:8080";
const families = ["Click", "Ice", "Crystal", "Silk", "Gear", "Bolt", "Paper"];

test("Clicker Sounds lists, selects, previews, and stays within the viewport", async () => {
  const browser = await chromium.launch({ headless: true });
  const page = await browser.newPage({ viewport: { width: 390, height: 844 } });
  const pageErrors = [];
  page.on("pageerror", (error) => pageErrors.push(error.message));
  await page.addInitScript(() => {
    window.__auricOscillatorStarts = 0;
    window.__auricFrequencies = [];
    const original = AudioContext.prototype.createOscillator;
    AudioContext.prototype.createOscillator = function (...args) {
      const oscillator = original.apply(this, args);
      const start = oscillator.start.bind(oscillator);
      const setFrequency = oscillator.frequency.setValueAtTime.bind(oscillator.frequency);
      oscillator.start = (...startArgs) => {
        window.__auricOscillatorStarts += 1;
        return start(...startArgs);
      };
      oscillator.frequency.setValueAtTime = (value, ...setArgs) => {
        window.__auricFrequencies.push(value);
        return setFrequency(value, ...setArgs);
      };
      return oscillator;
    };
  });

  try {
    await page.goto(baseUrl);
    await page.waitForFunction(() => localStorage.getItem("auric-crowned-v21") !== null);
    await page.getByRole("button", { name: "OPEN SESSION" }).click();
    await page.getByRole("button", { name: "clicker-sounds unlocked" }).click();
    await page.getByRole("heading", { name: "Clicker Sounds" }).waitFor();
    assert.equal(await page.evaluate(() => window.__auricFrequencies.includes(1640)), false, "opening the folder must not preview House click");

    const tabs = page.getByRole("tab");
    assert.deepEqual(await tabs.allTextContents(), families);
    const defaultRow = page.locator('[data-tick-id="click-01"]');
    assert.equal(await defaultRow.getAttribute("aria-pressed"), "true");
    assert.equal(await defaultRow.getByText("House").count(), 1);
    assert.equal(await page.locator('[data-tick-id="click-50"]').getByText("Crown cut").count(), 0);
    assert.equal(await page.locator('[data-tick-id="click-51"]').getByText("Crown cut").count(), 1);

    const reachedIds = [];
    for (const family of families) {
      await page.getByRole("tab", { name: family }).click();
      const rows = page.locator("[data-tick-id]");
      assert.equal(await rows.count(), 77);
      const ids = await rows.evaluateAll((elements) => elements.map((element) => element.getAttribute("data-tick-id")));
      assert.equal(ids[0], `${family.toLowerCase()}-01`);
      assert.equal(ids.at(-1), `${family.toLowerCase()}-77`);
      reachedIds.push(...ids);
    }
    assert.equal(new Set(reachedIds).size, 539);

    await page.getByRole("tab", { name: "Click" }).click();
    const startsBefore = await page.evaluate(() => window.__auricOscillatorStarts);
    const secondCut = page.locator('[data-tick-id="click-02"]');
    for (let index = 0; index < 10; index += 1) await secondCut.click();
    assert.equal(await secondCut.getAttribute("aria-pressed"), "true");
    assert.equal(await defaultRow.getAttribute("aria-pressed"), "false");
    assert.equal(await page.evaluate(() => window.__auricOscillatorStarts) - startsBefore, 10);
    assert.equal(await page.evaluate(() => window.__auricFrequencies.filter((frequency) => frequency === 1346).length), 10);

    for (const mood of ["dark", "light"]) {
      await page.locator(".tx-root").evaluate((root, nextMood) => root.setAttribute("data-mood", nextMood), mood);
      for (const width of [320, 390, 768, 1280]) {
        await page.setViewportSize({ width, height: 844 });
        const dimensions = await page.evaluate(() => ({
          pageWidth: document.documentElement.scrollWidth,
          clientWidth: document.documentElement.clientWidth,
          tabWidth: document.querySelector(".tx-clicker-tabs").clientWidth,
          tabScrollWidth: document.querySelector(".tx-clicker-tabs").scrollWidth,
          rowHeight: document.querySelector("[data-tick-id]").getBoundingClientRect().height,
        }));
        assert.ok(dimensions.pageWidth <= dimensions.clientWidth, `${mood} at ${width}px overflows horizontally`);
        assert.ok(dimensions.rowHeight >= 44, `${mood} at ${width}px has a short row`);
        if (width === 320) assert.ok(dimensions.tabScrollWidth > dimensions.tabWidth, "the 320px tab strip should scroll internally");
      }
    }

    await page.locator(".tx-front-layer").getByRole("button", { name: "HOME" }).click();
    await page.getByRole("button", { name: "timer unlocked" }).click();
    const startsBeforeNotch = await page.evaluate(() => window.__auricOscillatorStarts);
    await page.getByRole("slider", { name: "Seconds" }).press("ArrowUp");
    assert.equal(await page.evaluate(() => window.__auricOscillatorStarts) - startsBeforeNotch, 1);
    assert.equal((await page.evaluate(() => window.__auricFrequencies)).at(-1), 1346);
    assert.deepEqual(pageErrors, []);
  } finally {
    await browser.close();
  }
});
