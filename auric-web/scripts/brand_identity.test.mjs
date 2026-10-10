import assert from "node:assert/strict";
import { mkdtempSync } from "node:fs";
import { tmpdir } from "node:os";
import { join } from "node:path";
import test from "node:test";
import {
  readBrandIdentity,
  renderInstallPageHtml,
  renderWebManifest,
} from "./grok-pwa-shared.mjs";

test("the shipped brand identity is Auric with the three launcher icons", () => {
  const identity = readBrandIdentity();
  assert.equal(identity.name, "Auric");
  assert.equal(identity.shortName, "Auric");
  assert.deepEqual(
    identity.icons.map((icon) => icon.sizes),
    ["192x192", "512x512", "512x512"],
  );
});

test("the manifest and install page use the brand name, not the host slug", () => {
  const manifest = JSON.parse(renderWebManifest("wild-race.grok.me"));
  assert.equal(manifest.name, "Auric");
  assert.equal(manifest.short_name, "Auric");
  assert.equal(manifest.icons[0].src, "/icon-192.png");
  const html = renderInstallPageHtml("<title>Add {{APP_NAME}} to your Home Screen</title>", {
    host: "wild-race.grok.me",
    url: "/?install=1&platform=ios",
  });
  assert.match(html, /Add Auric to your/);
  assert.equal(html.includes("Wild Race"), false);
});

test("a workspace without brand.json falls back to the host slug", () => {
  const root = mkdtempSync(join(tmpdir(), "brand-identity-"));
  assert.equal(readBrandIdentity(root), null);
  const manifest = JSON.parse(renderWebManifest("wild-race.grok.me", root));
  assert.equal(manifest.name, "Wild Race");
  assert.equal(manifest.icons[0].src, "/__grok/icon-180.png");
  const html = renderInstallPageHtml("Add {{APP_NAME}}", {
    host: "<script>",
    url: "/",
    root,
  });
  assert.equal(html.includes("<script>"), false);
});
