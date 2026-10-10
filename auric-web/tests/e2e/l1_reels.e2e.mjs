import assert from "node:assert/strict";
import { mkdirSync, writeFileSync } from "node:fs";
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
await page.route(/https:\/\/fonts\.(googleapis|gstatic)\.com\//, (route) =>
  route.fulfill({ status: 200, contentType: "text/css", body: "" }),
);
await page.route("https://grok.com/grok-app-builder/extensions.js", (route) =>
  route.fulfill({ status: 200, contentType: "text/javascript", body: "" }),
);

async function waitForSettledValue(page, slider) {
  const handle = await slider.elementHandle();
  await page
    .waitForFunction((element) => element.getAttribute("data-reel-settled") === "0", handle, {
      timeout: 1000,
    })
    .catch(() => undefined);
  await page.waitForFunction(
    (element) => element.getAttribute("data-reel-settled") === "1",
    handle,
    { timeout: 5000 },
  );
  return Number(await slider.getAttribute("aria-valuenow"));
}

try {
  await page.goto(`${baseUrl}/laser_demo`, { waitUntil: "domcontentloaded" });
  await page.getByRole("heading", { name: "Laser Strip Lab" }).waitFor();
  await page.waitForFunction(() =>
    [...document.querySelectorAll(".barrel [data-reel-item]")].some(
      (item) => item.dataset.selected === "1" && item.textContent,
    ),
  );
  assert.equal(await page.locator(".l1-demo-sample").count(), 15);
  assert.equal(await page.locator(".laser-strip").count(), 30);
  assert.equal(await page.locator(".l1-demo-sample .aperture").count(), 0);
  assert.equal(await page.locator("[data-l1-aperture-witness] .aperture").count(), 1);
  assert.equal(await page.locator("[data-l1-aperture-witness] .barrel").count(), 1);
  assert.equal(await page.locator('a[href="/laser_demo"]').count(), 0);
  assert.equal(await page.locator('a[href="/laser-demo"]').count(), 0);

  mkdirSync(resolve("screenshots/l1"), { recursive: true });
  await page.screenshot({ path: "screenshots/l1/laser_demo.png", fullPage: true });
  const brightCore = await page
    .locator('.l1-demo-sample[data-laser-intensity="100"] .laser-strip')
    .first()
    .evaluate((strip) => ({
      height: getComputedStyle(strip, "::before").height,
      background: getComputedStyle(strip, "::before").backgroundImage,
      opacity: getComputedStyle(strip).opacity,
      animation: getComputedStyle(strip).animationName,
    }));
  assert.equal(brightCore.height, "2px");
  assert.match(brightCore.background, /rgb\(255, 255, 255\)/);
  assert.equal(brightCore.opacity, "1");
  assert.equal(brightCore.animation, "none");
  const pixelStrip = page
    .locator('.l1-demo-sample[data-laser-color="red"][data-laser-intensity="100"] .laser-strip')
    .nth(1);
  const pixelPosition = await pixelStrip.evaluate((strip) => {
    const bounds = strip.getBoundingClientRect();
    const barrels = [...strip.parentElement.querySelectorAll(".barrel")];
    return {
      height: bounds.height,
      gap:
        (barrels[0].getBoundingClientRect().right + barrels[1].getBoundingClientRect().left) / 2 -
        bounds.left,
    };
  });
  const beamPixels = await pixelStrip.screenshot();
  writeFileSync("screenshots/l1/laser_strip.png", beamPixels);
  await pixelStrip.evaluate((strip) => {
    strip.style.opacity = "0";
  });
  const backgroundPixels = await pixelStrip.screenshot();
  await pixelStrip.evaluate((strip) => strip.style.removeProperty("opacity"));
  const samples = await page.evaluate(
    async ({ beam, background, position }) => {
      const decode = async (bytes) => {
        const image = new Image();
        image.src = `data:image/png;base64,${bytes}`;
        await image.decode();
        const canvas = document.createElement("canvas");
        canvas.width = image.width;
        canvas.height = image.height;
        const context = canvas.getContext("2d");
        context.drawImage(image, 0, 0);
        return { context, width: image.width, height: image.height };
      };
      const visible = await decode(beam);
      const blank = await decode(background);
      const read = (image, y) =>
        [...image.context.getImageData(Math.round(position.gap), y, 1, 1).data].slice(0, 3);
      return {
        core: read(visible, Math.round(position.height / 2)),
        bloom: read(visible, Math.round(position.height / 2) - 8),
        edge: read(visible, 0),
        backgroundEdge: read(blank, 0),
        dimensions: [visible.width, visible.height],
      };
    },
    {
      beam: beamPixels.toString("base64"),
      background: backgroundPixels.toString("base64"),
      position: pixelPosition,
    },
  );
  const bloomSaturation =
    (Math.max(...samples.bloom) - Math.min(...samples.bloom)) / Math.max(...samples.bloom);
  const edgeDifference =
    samples.edge.reduce(
      (sum, channel, index) => sum + Math.abs(channel - samples.backgroundEdge[index]),
      0,
    ) /
    (3 * 255);
  assert.ok(
    samples.core.every((channel) => channel >= 240),
    "laser core must sample near white",
  );
  assert.ok(bloomSaturation >= 0.6, "bloom 8px above the core must stay saturated");
  assert.ok(edgeDifference < 0.05, "bloom must fade below 5% at the spread edge");
  const glyphShot = await page
    .locator(
      '.l1-demo-sample[data-laser-color="red"][data-laser-intensity="100"] [data-selected="1"]',
    )
    .first()
    .screenshot();
  const glyph = await page.evaluate(async (bytes) => {
    const image = new Image();
    image.src = `data:image/png;base64,${bytes}`;
    await image.decode();
    const canvas = document.createElement("canvas");
    canvas.width = image.width;
    canvas.height = image.height;
    const context = canvas.getContext("2d");
    context.drawImage(image, 0, 0);
    const y0 = Math.floor(image.height * 0.35);
    const y1 = Math.ceil(image.height * 0.65);
    let best = [0, 0, 0];
    let bestScore = -1;
    for (let y = y0; y < y1; y += 1) {
      const row = context.getImageData(0, y, image.width, 1).data;
      for (let x = 0; x < image.width; x += 1) {
        const index = x * 4;
        const rgb = [row[index], row[index + 1], row[index + 2]];
        const score = rgb[0] + rgb[1] + rgb[2];
        if (score > bestScore) {
          best = rgb;
          bestScore = score;
        }
      }
    }
    return best;
  }, glyphShot.toString("base64"));
  assert.ok(
    glyph.every((channel) => channel >= 240),
    `selected glyph over the beam must stay near white, got ${glyph.join(",")}`,
  );
  await page.getByRole("button", { name: "light mood" }).click();
  await page.screenshot({ path: "screenshots/l1/laser_light.png", fullPage: true });
  await page.getByRole("button", { name: "dark mood" }).click();

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
        const center = stripBox.top + stripBox.height / 2;
        return (
          barrels.every(
            (barrel) =>
              Math.abs(
                center -
                  (barrel.getBoundingClientRect().top + barrel.getBoundingClientRect().height / 2),
              ) <= 1,
          ) &&
          stripBox.left <= firstBox.left - 16 &&
          stripBox.right >= lastBox.right + 16 &&
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

  const firstSample = page.locator(".l1-demo-sample").first();
  for (const [unit, expected] of [
    ["Days", 3],
    ["Weeks", 2],
    ["Months", 11],
    ["Years", 77],
  ]) {
    assert.equal(
      await firstSample.getByRole("slider", { name: unit }).getAttribute("aria-valuenow"),
      String(expected),
    );
  }
  for (const viewport of [
    { width: 1280, height: 800 },
    { width: 320, height: 844 },
    { width: 768, height: 844 },
    { width: 390, height: 844 },
    { width: 844, height: 390 },
  ]) {
    await page.setViewportSize(viewport);
    await page.waitForFunction(
      () =>
        [...document.querySelectorAll(".laser-strip")].every((strip) => {
          const host = strip.parentElement;
          const barrels = [...host.querySelectorAll(".barrel")];
          const stripBox = strip.getBoundingClientRect();
          const first = barrels[0].getBoundingClientRect();
          const last = barrels.at(-1).getBoundingClientRect();
          const center = stripBox.top + stripBox.height / 2;
          return (
            barrels.every(
              (barrel) =>
                Math.abs(
                  center -
                    (barrel.getBoundingClientRect().top +
                      barrel.getBoundingClientRect().height / 2),
                ) <= 1,
            ) &&
            stripBox.left <= first.left - 16 &&
            stripBox.right >= last.right + 16 &&
            stripBox.left >= -1 &&
            stripBox.right <= innerWidth + 1
          );
        }),
      null,
      { timeout: 5000 },
    );
    for (const [unit, expected] of [
      ["Days", 3],
      ["Weeks", 2],
      ["Months", 11],
      ["Years", 77],
    ]) {
      assert.equal(
        await firstSample.getByRole("slider", { name: unit }).getAttribute("aria-valuenow"),
        String(expected),
        `value changed during resize: ${unit}`,
      );
    }
  }

  for (const mood of ["dark", "light"]) {
    for (const scale of [0, 50, 100]) {
      await page.getByRole("button", { name: `${mood} mood` }).click();
      await page.getByRole("button", { name: `Digit scale ${scale}` }).click();
      for (const width of [320, 390, 768, 1280]) {
        await page.setViewportSize({ width, height: 844 });
        await page.waitForFunction(
          () =>
            [...document.querySelectorAll(".laser-strip")].every((strip) => {
              const host = strip.parentElement;
              const barrels = [...host.querySelectorAll(".barrel")];
              const stripBox = strip.getBoundingClientRect();
              const first = barrels[0].getBoundingClientRect();
              const last = barrels.at(-1).getBoundingClientRect();
              const center = stripBox.top + stripBox.height / 2;
              return (
                barrels.every(
                  (barrel) =>
                    Math.abs(
                      center -
                        (barrel.getBoundingClientRect().top +
                          barrel.getBoundingClientRect().height / 2),
                    ) <= 1,
                ) &&
                stripBox.left <= first.left - 16 &&
                stripBox.right >= last.right + 16 &&
                stripBox.left >= -1 &&
                stripBox.right <= innerWidth + 1
              );
            }),
          null,
          { timeout: 5000 },
        );
        const matrix = await page.evaluate(() => ({
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
          stripsAligned: [...document.querySelectorAll(".laser-strip")].every((strip) => {
            const barrels = [...strip.parentElement.querySelectorAll(".barrel")];
            const stripBox = strip.getBoundingClientRect();
            const first = barrels[0].getBoundingClientRect();
            const last = barrels.at(-1).getBoundingClientRect();
            const center = stripBox.top + stripBox.height / 2;
            return (
              barrels.every(
                (barrel) =>
                  Math.abs(
                    center -
                      (barrel.getBoundingClientRect().top +
                        barrel.getBoundingClientRect().height / 2),
                  ) <= 1,
              ) &&
              stripBox.left <= first.left - 16 &&
              stripBox.right >= last.right + 16 &&
              stripBox.left >= -1 &&
              stripBox.right <= innerWidth + 1
            );
          }),
        }));
        assert.equal(matrix.overflow, false, `${mood} scale ${scale} width ${width}: overflow`);
        assert.equal(
          matrix.barsInside,
          true,
          `${mood} scale ${scale} width ${width}: barrel outside`,
        );
        assert.equal(
          matrix.selectedInside,
          true,
          `${mood} scale ${scale} width ${width}: glyph clipped`,
        );
        assert.equal(
          matrix.stripsAligned,
          true,
          `${mood} scale ${scale} width ${width}: strip misaligned`,
        );
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
  assert.equal(
    Number(await tickOutput.textContent()),
    beforeHold + 3,
    "three dragged rows must tick three times",
  );
  await page.mouse.up();
  await slider.dispatchEvent("pointercancel", { pointerId: 1, pointerType: "mouse" });
  const afterDrag = Number(await tickOutput.textContent());
  await slider.focus();
  for (let step = 0; step < 5; step += 1) await page.keyboard.press("ArrowUp");
  assert.equal(
    Number(await tickOutput.textContent()),
    afterDrag + 5,
    "five keyboard rows must tick five times",
  );
  const minuteReel = sample.getByRole("slider", { name: "Minutes" });
  const minuteBefore = Number(await minuteReel.getAttribute("aria-valuenow"));
  const beforeWheel = Number(await tickOutput.textContent());
  const minuteBox = await minuteReel.boundingBox();
  await page.mouse.move(minuteBox.x + minuteBox.width / 2, minuteBox.y + minuteBox.height / 2);
  await page.mouse.wheel(0, 120);
  const minuteAfter = await waitForSettledValue(page, minuteReel);
  const wheelTicks = Number(await tickOutput.textContent()) - beforeWheel;
  const minuteRows = (minuteAfter - minuteBefore + 60) % 60;
  assert.equal(wheelTicks, minuteRows, "wheel ticks must match each changed minute row");
  assert.ok(wheelTicks > 0, "wheel rows must emit ticks");

  const secondsReel = sample.getByRole("slider", { name: "Seconds" });
  const secondsBefore = Number(await secondsReel.getAttribute("aria-valuenow"));
  const beforeFling = Number(await tickOutput.textContent());
  const secondsBox = await secondsReel.boundingBox();
  await page.mouse.move(secondsBox.x + secondsBox.width / 2, secondsBox.y + secondsBox.height / 2);
  await page.mouse.down();
  await page.mouse.move(
    secondsBox.x + secondsBox.width / 2,
    secondsBox.y + secondsBox.height / 2 - 84,
    { steps: 4 },
  );
  await page.mouse.up();
  const secondsAfter = await waitForSettledValue(page, secondsReel);
  const flingTicks = Number(await tickOutput.textContent()) - beforeFling;
  const secondRows = (secondsAfter - secondsBefore + 60) % 60;
  assert.equal(flingTicks, secondRows, "fling ticks must match each changed second row");
  assert.ok(flingTicks > 0, "fling rows must emit ticks");

  await page.addStyleTag({
    content: ".l1-demo-sample:not(:first-child) { display: none !important; }",
  });
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
    const ticksBefore = Number(await tickOutput.textContent());
    await page.mouse.move(reelBox.x + reelBox.width / 2, reelBox.y + reelBox.height / 2);
    await page.mouse.down();
    await page.mouse.move(reelBox.x + reelBox.width / 2, reelBox.y - 400, { steps: 6 });
    await page.mouse.up();
    const results = await samples;
    assert.ok(Number(await tickOutput.textContent()) > ticksBefore, "fling rows must emit ticks");
    return results;
  };
  const perfNormal = await frameStats(1);
  const perfThrottled = await frameStats(4);
  await cdp.send("Emulation.setCPUThrottlingRate", { rate: 1 });
  console.log(JSON.stringify({ perfNormal, perfThrottled }));
  assert.ok(perfNormal.medianMs <= 17.5, "unthrottled median frame interval exceeded 17.5ms");
  assert.ok(perfNormal.p95Ms <= 25, "unthrottled p95 frame interval exceeded 25ms");

  await page.emulateMedia({ reducedMotion: "reduce" });
  assert.equal(
    await page
      .locator(".laser-strip")
      .first()
      .evaluate((strip) => strip.getAnimations().length),
    0,
  );
  const days = sample.getByRole("slider", { name: "Days" });
  const beforeReducedMotionStep = Number(await days.getAttribute("aria-valuenow"));
  await days.press("ArrowUp");
  assert.equal(
    await days.getAttribute("aria-valuenow"),
    String((beforeReducedMotionStep + 1) % 7),
    "reduced-motion keyboard snap must commit immediately",
  );
  assert.deepEqual(errors, []);
} finally {
  await browser.close();
}
