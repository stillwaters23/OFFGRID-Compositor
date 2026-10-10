import assert from "node:assert/strict";
import { test } from "node:test";
import { chromium } from "playwright";

const baseUrl = process.env.L3_BASE_URL ?? "http://127.0.0.1:8080";

test("L3 overflow matrix keeps folder controls within the viewport", { skip: !process.env.L3_E2E }, async (t) => {
  const browser = await chromium.launch({ headless: true });
  const page = await browser.newPage();
  t.after(() => browser.close());

  await page.goto(baseUrl);
  await page.waitForSelector(".tx-root");

  for (const width of [320, 360, 390, 768, 1280]) {
    for (const mood of ["dark", "light"]) {
      for (const layout of ["grid", "big", "single"]) {
        for (const size of [0, 50, 100]) {
          await page.setViewportSize({ width, height: 900 });
          await page.evaluate(({ mood, layout, size }) => {
            const key = "auric-crowned-v21";
            const state = JSON.parse(localStorage.getItem(key) ?? '{"state":{},"version":0}');
            state.state = { ...state.state, shell: "home", appearanceMode: mood };
            localStorage.setItem(key, JSON.stringify(state));
            localStorage.setItem(
              "auric-folder-settings-v1",
              JSON.stringify({
                layout,
                folderSizeAll: size,
                folderSizes: {},
                folderOrder: [],
                folderIconStyle: {},
                folderNames: {},
              }),
            );
          }, { mood, layout, size });
          await page.reload();
          await page.waitForFunction(({ mood, layout }) => {
            const root = document.querySelector(".tx-root");
            const grid = document.querySelector(".tx-folder-grid");
            return root?.getAttribute("data-mood") === mood && grid?.getAttribute("data-layout") === layout;
          }, { mood, layout });
          await page.waitForTimeout(350);

          const result = await page.evaluate(() => {
            const root = document.querySelector(".tx-root");
            const grid = document.querySelector(".tx-folder-grid");
            if (!root || !grid) return { error: "Folder list did not render" };
            const tiles = [...grid.querySelectorAll("li")].map((node) => {
              const rect = node.getBoundingClientRect();
              const bounds = grid.getBoundingClientRect();
              return rect.left >= bounds.left - 1 && rect.right <= bounds.right + 1;
            });
            const buttons = [...document.querySelectorAll(".tx-folder-grid button, .tx-folder-menu-button, .tx-folder-single-controls button")].map((button) => {
              const rect = button.getBoundingClientRect();
              return rect.width >= 44 && rect.height >= 44;
            });
            return {
              rootOverflow: root.scrollWidth <= root.clientWidth,
              pageOverflow: document.documentElement.scrollWidth <= document.documentElement.clientWidth,
              tiles,
              buttons,
            };
          });

          const controls = [];
          for (const tile of await page.locator(".tx-folder-grid > li").all()) {
            await tile.scrollIntoViewIfNeeded();
            controls.push(
              ...(await tile.evaluate((node) =>
                [...node.querySelectorAll(".tx-folder-name, .tx-folder-lockmark, .tx-folder-chevron")].map((detail) => {
                  const rect = detail.getBoundingClientRect();
                  const bounds = node.getBoundingClientRect();
                  const hit = document.elementFromPoint(rect.left + rect.width / 2, rect.top + rect.height / 2);
                  return {
                    inside: rect.left >= bounds.left - 1 && rect.top >= bounds.top - 1 && rect.right <= bounds.right + 1 && rect.bottom <= bounds.bottom + 1,
                    hit: hit === detail || detail.contains(hit),
                    visible: rect.width > 0 && rect.height > 0,
                  };
                }),
              )),
            );
          }
          assert.equal(result.error, undefined, `${width}px ${mood} ${layout} size=${size}`);
          assert.equal(result.rootOverflow, true, `${width}px ${mood} ${layout} size=${size}: root overflow`);
          assert.equal(result.pageOverflow, true, `${width}px ${mood} ${layout} size=${size}: page overflow`);
          assert.ok(result.tiles.every(Boolean), `${width}px ${mood} ${layout} size=${size}: tile out of bounds`);
          assert.ok(controls.every((control) => control.visible && control.inside && control.hit), `${width}px ${mood} ${layout} size=${size}: inaccessible folder detail`);
          assert.ok(result.buttons.every(Boolean), `${width}px ${mood} ${layout} size=${size}: undersized button`);
        }
      }
    }
  }
});
