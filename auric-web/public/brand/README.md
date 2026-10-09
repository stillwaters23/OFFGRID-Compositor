# AURIC 777 branding

`brand.json` is the shared path configuration for the application, document shell, notifications, install metadata, and Grok PWA renderer. Required assets live in `public/`; `.vercel/output` is generated.

## Locked master and provenance

The user's corrected `1000234137.png` attachment contains JPEG bytes, 1536 × 1536 RGB, with a black background and no alpha. Original bytes are retained as `auric_777_diamond_crown_emblem.original.jpg`. SHA-256: `0ab8d022504af2fff919b6cd019c5e5a1d81bcdff64b97248e86a5e447b52619`.

`auric_777_diamond_crown_emblem.png` is a genuine PNG with identical decoded RGB pixels. It preserves the diamond, crown, 777, AURIC, and TIME, CROWNED. Do not redraw the full identity or claim this file is transparent. The masthead uses the original artwork on a compact black-glass panel across all palettes.

`auric-launcher-source.png` is the retained icon-only derivative, 1254 × 1254 RGB. It simplifies the composition to diamond, crown, 777, and AURIC for small launchers. The subtitle remains on the full masthead and social card. Generation provenance and source hashes are in `provenance.json`.

## Exports

| Public URL | True dimensions | Composition / alpha |
|---|---|---|
| `/brand/auric-masthead.png` | 768 × 768 | Complete master, 20px added black margin; opaque |
| `/__grok/icon-180.png` | 180 × 180 | Launcher derivative; opaque Apple touch icon |
| `/icon-192.png` | 192 × 192 | Launcher derivative; opaque |
| `/icon-512.png` | 512 × 512 | Launcher derivative and boot mark; opaque |
| `/brand/icon-maskable-512.png` | 512 × 512 | Launcher inside the central safe circle; opaque |
| `/brand/notification-192.png` | 192 × 192 | Launcher derivative; opaque |
| `/brand/favicon-16.png` | 16 × 16 | Simple faceted diamond; real alpha |
| `/brand/favicon-32.png` | 32 × 32 | Simple faceted diamond; real alpha; browser default |
| `/brand/notification-badge-96.png` | 96 × 96 | White diamond geometry on real alpha |
| `/og.png` | 1200 × 630 | Full master on black, 30px vertical margin; opaque |

The manifest declares separate `any` 192/512 and `maskable` 512 entries. Maskable export places visible artwork within radius 0.38 of the image width; verification requires pixels above the brightness threshold inside radius 0.40. Never squash the full lockup into a square launcher. Tiny favicons and the badge omit text that cannot be read at their display size.

## Reproduce and verify

Committed sources and exports are sufficient for normal `npm run build`; no image generation service is needed at build time. Optional maintenance requires Python 3, Pillow, and Node:

```sh
npm run brand:export
npm run brand:verify
```

`scripts/export-brand.py` uses contain/Lanczos resizing with preserved aspect ratios. It validates a temporary PNG before atomically replacing each export, so an interruption cannot truncate an existing image. It draws tiny faceted-diamond favicon and badge geometry. It does not regenerate or reinterpret either source image.

`scripts/verify-brand.py` fully decodes and checks PNG integrity, sizes, alpha, margins, master bytes/pixels, launcher source hash, maskable containment, OG file size, actual manifest-renderer output, and configured local runtime image references. Results are in `artifacts/qa/brand-verification.json`. This is an asset check, not a rendered-browser test.

The masthead uses `object-fit: contain`, a viewport-related height cap, and a responsive grid with safe margins. Palette selection recolors the operating interface; branding stays blue diamond. Validate the actual page at 360, 390, 412, 768, and 1280 CSS pixels before release.

## Retained launcher generation prompt

The icon-only derivative was produced from the supplied master with this prompt. Retain the existing derivative for reproducibility; rerunning a generative model is not a deterministic export step.

> Use case: precise-object-edit / logo-brand derivative. Create the tiny application launcher derivative of the supplied authoritative AURIC 777 blue-diamond identity. The full master remains unchanged elsewhere; this is an icon-only technical simplification for legibility at 192px. Square 1024px composition on a completely uniform solid black background. A large clean faceted sapphire-blue diamond crown occupies the upper third, above three bold legible jewel-cut blue 7 digits on three polished platinum and black glass reel windows. The 777 must be the main readable element. Beneath it place the word 'AURIC' in large sharp engraved platinum serif capitals. OMIT the tiny TIME, CROWNED subtitle and side flourishes on this icon. Keep all four essential elements: blue diamond, crown, 777, AURIC. Match the supplied polished metal/blue diamond craft and proportions, simplify micro-facets and remove excessive little sparkles for sharp small-size recognition. No cartoon styling, no neon fog, no extra text, no border or rounded-square frame, no scenery. Symmetric compact composition with at least 10% plain black margins on every side. Never touch or crop the canvas edge. The blue diamond and 777 should remain identifiable even as a phone home-screen icon.
