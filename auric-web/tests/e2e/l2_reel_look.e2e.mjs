import assert from "node:assert/strict";
import { mkdir } from "node:fs/promises";
import { join } from "node:path";
import { test } from "node:test";
import { chromium } from "playwright";

const baseUrl = process.env.AURIC_E2E_URL;

test("Laser & Numbers drafts, applies, discards, persists, and fits all target widths", {
  skip: !baseUrl,
}, async () => {
  const browser = await chromium.launch({
    headless: true,
    ...(process.env.CHROMIUM_PATH ? { executablePath: process.env.CHROMIUM_PATH } : {}),
  });
  const page = await browser.newPage({ viewport: { width: 390, height: 844 } });
  try {
    await page.goto(baseUrl);
    await page.waitForFunction(() => window.__auric?.getState().hydrated);
    await openStudio(page);
    await page.getByText("Laser color", { exact: true }).waitFor();
    assert.equal(
      await page.locator(".reel-look-drum").first().evaluate((item) => getComputedStyle(item).fontSize),
      "24px",
    );
    const presetNames = await page.locator(".reel-look-preset").evaluateAll((items) =>
      items.map((item) => item.textContent.trim()),
    );
    assert.deepEqual(presetNames, ["Red", "Cyan", "Blue", "Magenta", "Violet"]);
    const presetHex = await page.locator(".reel-look-preset").evaluateAll((items) =>
      items.map((item) => item.getAttribute("data-laser-preset")),
    );
    assert.deepEqual(presetHex, ["#FF0000", "#00E5FC", "#0032FF", "#FF00A6", "#7B02B3"]);
    const motion = await page.locator(".reel-look-beam-shimmer").evaluate((item) => {
      const names = item.getAnimations().flatMap((animation) => {
        const effect = animation.effect;
        if (!effect || typeof effect.getKeyframes !== "function") return [];
        return effect.getKeyframes().flatMap((frame) => Object.keys(frame).filter((key) => key !== "offset" && key !== "computedOffset" && key !== "easing" && key !== "composite"));
      });
      return [...new Set(names)].sort();
    });
    assert.deepEqual(motion, ["opacity", "transform"]);
    const perf = await frameStats(page, 1000);
    console.log(`L2 motion median=${perf.median.toFixed(2)}ms p95=${perf.p95.toFixed(2)}ms frames=${perf.frames}`);
    assert.ok(perf.median <= 17.5, `median frame ${perf.median}ms`);
    assert.ok(perf.p95 <= 25, `p95 frame ${perf.p95}ms`);
    const hex = page.getByRole("textbox", { name: "Custom laser hex" });
    for (const invalid of ["#GGG", "red", ""]) {
      await hex.fill(invalid);
      await page.getByRole("alert").filter({ hasText: "valid opaque" }).waitFor();
    }
    await hex.fill("#abc");
    assert.equal(await page.evaluate(() => window.__auric.getState().appearanceDraft.laserColor), "#AABBCC");
    await hex.fill("#123456");
    assert.equal(await page.evaluate(() => window.__auric.getState().appearanceDraft.laserColor), "#123456");
    await hex.fill("#abc");
    const applied = await page.evaluate(() => new Promise((resolve) => {
      const apply = [...document.querySelectorAll("button")].find((button) => button.textContent?.trim() === "Apply appearance");
      apply.click();
      let frames = 0;
      const tick = () => {
        frames += 1;
        const css = getComputedStyle(document.documentElement).getPropertyValue("--laser-color").trim();
        const beam = getComputedStyle(document.querySelector("[data-laser-beam]")).getPropertyValue("--laser-color").trim();
        if ((css === "#AABBCC" && beam === "#AABBCC") || frames >= 3) resolve({ frames, css, beam });
        else requestAnimationFrame(tick);
      };
      requestAnimationFrame(tick);
    }));
    console.log(`L2 apply frames=${applied.frames} css=${applied.css} beam=${applied.beam}`);
    assert.equal(applied.css, "#AABBCC");
    assert.equal(applied.beam, "#AABBCC");
    assert.ok(applied.frames <= 1, `laser color landed in ${applied.frames} frames`);

    await page.evaluate(() => window.__auric.getState().setDraftNumberColor("dark", "#000000"));
    await page.getByRole("alert").filter({ hasText: /dark wheel.*4\.5:1/i }).waitFor();
    assert.equal(await page.getByRole("button", { name: "Apply appearance" }).isDisabled(), true);
    await page.evaluate(() => window.__auric.getState().setDraftNumberColor("dark", null));
    await page.getByRole("button", { name: "Magenta" }).click();
    await page.getByRole("button", { name: "Discard" }).click();
    assert.equal(await page.evaluate(() => window.__auric.getState().laserColor), "#AABBCC");

    const range = page.getByRole("slider", { name: "Laser intensity" });
    await range.focus();
    await page.keyboard.press("Home");
    await page.keyboard.press("ArrowRight");
    assert.equal(await page.evaluate(() => window.__auric.getState().appearanceDraft.laserIntensity), 1);
    await page.getByRole("switch", { name: "Laser on/off" }).click();
    assert.equal(await page.evaluate(() => window.__auric.getState().appearanceDraft.laserEnabled), false);
    await page.getByRole("button", { name: "Apply appearance" }).click();
    await page.waitForFunction(() => document.documentElement.dataset.laserEnabled === "false");
    const dimmed = await page.evaluate(() => {
      const beam = getComputedStyle(document.querySelector("[data-laser-beam]"));
      const hairline = document.querySelector("[data-laser-hairline]");
      const hair = getComputedStyle(hairline);
      return {
        beamOpacity: beam.opacity,
        hairOpacity: hair.opacity,
        hairHeight: hairline.getBoundingClientRect().height,
      };
    });
    assert.equal(dimmed.beamOpacity, "0");
    assert.equal(dimmed.hairOpacity, "1");
    assert.ok(dimmed.hairHeight >= 1 && dimmed.hairHeight <= 2, `hairline ${dimmed.hairHeight}px`);

    await page.evaluate(() => {
      const state = window.__auric.getState();
      state.setLaserColor("#FF00A6");
      state.setLaserIntensity(40);
      state.setLaserEnabled(false);
      state.setNumberColor("dark", "#EAF3FF");
    });
    await page.reload();
    await page.waitForFunction(() => window.__auric?.getState().hydrated);
    const persisted = await page.evaluate(() => {
      const state = window.__auric.getState();
      return [state.laserColor, state.laserIntensity, state.laserEnabled, state.numberColor.dark];
    });
    assert.deepEqual(persisted, ["#FF00A6", 40, false, "#EAF3FF"]);

    await openStudio(page);
    await page.getByText("Laser color", { exact: true }).waitFor();
    for (const mood of ["dark", "light"]) {
      await page.evaluate((value) => {
        window.__auric.setState({ appearanceMode: value });
      }, mood);
      await page.waitForFunction((value) => document.querySelector(".tx-root")?.dataset.mood === value, mood);
      for (const width of [320, 390, 768, 1280]) {
        await page.setViewportSize({ width, height: 844 });
        const overflow = await page.evaluate(() => document.documentElement.scrollWidth > window.innerWidth);
        assert.equal(overflow, false, `${mood} theme at ${width}px`);
      }
    }

    const screenshotDir = join(process.cwd(), "screenshots", "l2");
    await mkdir(screenshotDir, { recursive: true });
    await page.evaluate(() => window.__auric.setState({ toasts: [] }));
    await page.setViewportSize({ width: 390, height: 844 });
    await page.getByText("Laser color", { exact: true }).scrollIntoViewIfNeeded();
    await page.screenshot({ path: join(screenshotDir, "laser_settings.png") });
    await page.getByText("Number color", { exact: true }).scrollIntoViewIfNeeded();
    await page.screenshot({ path: join(screenshotDir, "number_contrast.png") });
    await page.evaluate(() => {
      const state = window.__auric.getState();
      state.setDraftLaserColor("#FF0000");
      state.setDraftLaserIntensity(100);
      state.setDraftLaserEnabled(true);
      window.__auric.setState({ appearanceMode: "dark" });
    });
    await page.getByLabel("Live laser preview").scrollIntoViewIfNeeded();
    await page.screenshot({ path: join(screenshotDir, "beam_on_dark.png") });
    await page.evaluate(() => window.__auric.setState({
      numberColor: { dark: "#000000", light: "#111111" },
    }));
    const resolvedNumber = await page.evaluate(() =>
      getComputedStyle(document.documentElement).getPropertyValue("--reel-number").trim(),
    );
    assert.notEqual(resolvedNumber, "#000000");
    assert.notEqual(resolvedNumber, "#111111");

    await page.emulateMedia({ reducedMotion: "reduce" });
    const reduced = await page.locator(".reel-look-beam-shimmer").evaluate((item) => item.getAnimations().length);
    assert.equal(reduced, 0);
  } finally {
    await browser.close();
  }
});

async function frameStats(page, duration) {
  return page.evaluate((ms) => new Promise((resolve) => {
    const deltas = [];
    let last = performance.now();
    const stopAt = last + ms;
    function tick(now) {
      deltas.push(now - last);
      last = now;
      if (now < stopAt) requestAnimationFrame(tick);
      else {
        const sorted = [...deltas].sort((a, b) => a - b);
        const pick = (p) => sorted[Math.min(sorted.length - 1, Math.max(0, Math.ceil(sorted.length * p) - 1))];
        resolve({ frames: deltas.length, median: pick(0.5), p95: pick(0.95) });
      }
    }
    requestAnimationFrame(tick);
  }), duration);
}

async function openStudio(page) {
  await page.evaluate(() => {
    const state = window.__auric.getState();
    window.__auric.setState({
      shell: "home",
      openModuleId: "themes",
      installedModules: [...state.installedModules, "themes"],
      customizationFolder: "laser",
    });
  });
}
