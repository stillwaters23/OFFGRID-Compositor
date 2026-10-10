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
    await page.getByRole("button", { name: "Apply appearance" }).click();
    await page.waitForFunction(() => getComputedStyle(document.documentElement).getPropertyValue("--laser-color").trim() === "#AABBCC");

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
    await page.screenshot({ path: join(screenshotDir, "laser-settings.png") });
    await page.getByText("Number color", { exact: true }).scrollIntoViewIfNeeded();
    await page.screenshot({ path: join(screenshotDir, "number-contrast.png") });
    await page.evaluate(() => window.__auric.setState({
      numberColor: { dark: "#000000", light: "#111111" },
    }));
    const resolvedNumber = await page.evaluate(() =>
      getComputedStyle(document.documentElement).getPropertyValue("--reel-number").trim(),
    );
    assert.notEqual(resolvedNumber, "#000000");
    assert.notEqual(resolvedNumber, "#111111");
  } finally {
    await browser.close();
  }
});

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
