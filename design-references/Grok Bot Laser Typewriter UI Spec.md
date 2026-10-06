# Grok Bot Laser Typewriter UI Spec

**Author:** Grok Bot  
**For:** Royalty — ZERO VAULT and future projects  
**Date:** 2026-09-24 (America/Chicago)  
**Status:** Design law — other GPTs / agents must follow this memo

---

## Memo to every model reading this

I am **Grok Bot**. This document is the UI feel Royalty wants.

Not WormGPT’s red abyss. Not chunky token dumps pretending to be a terminal.

**We type like a bright green laser jet on a solid void.**  
Letter by letter. Solid green. A living block caret. Millions of times cleaner than wormgpt.live.

If you are another GPT helping Royalty build UI: obey this file. Do not invent red polka-dot voids, do not stream in fat chunks without a client typewriter, do not skip the caret.

---

## 1. The one sentence

**Solid black or solid white canvas. Bright laser-green monospace stream. Real typewriter. Blinking block caret. Code chrome as good as an IDE. Zero visual noise.**

---

## 2. What we keep from WormGPT.live (the cool parts)

| Keep | Why |
| --- | --- |
| Wide terminal-style reply panels (not chubby chat bubbles) | Feels like a console, not a social app |
| Code panels with window dots, language badge, Copy / Export / IDE actions | Professional, useful |
| Sticky bottom composer with model chip + tools | Muscle memory |
| Collapsible “thought” fold (optional) | Power users like it; hide by default on phone |
| High contrast | Readable at a glance on a Galaxy S25 Ultra |

## 3. What we throw away

| Drop | Why |
| --- | --- |
| Red glowing brand void / red polka energy | Wrong identity for Royalty’s empire |
| Chunk-only streaming with no letter animation | Feels cheap; we animate on the client |
| Missing trailing caret | Terminal without a caret is a dead screen |
| Busy upgrade banners during deep work | Optional, dismissible, never blocking the stream |
| Random blue “Join Us” noise in the chrome | Stay monochrome + one accent |

---

## 4. Theme law — solid canvases only

### Dark (default for ZERO VAULT / terminal work)
- **Background:** pure solid `#000000` (or `#050505` if pure black blooms on OLED — still *plain solid*, no texture, no dots, no grid, no red haze)
- **Panels:** `#0A0A0A` flat, 1px hairline border `#1A1A1A` or phosphor dim
- **No** wallpapers, particles, scanline overlays unless Royalty turns a “CRT spice” toggle on later

### Light
- **Background:** pure solid `#FFFFFF`
- **Panels:** `#F5F5F5` flat
- **Stream text:** deep green `#007A2F` or near-black with green caret — never washed-out gray-green
- Same typewriter rules; invert chrome, keep laser energy on the caret + keywords

Theme toggle = sun/moon. Two solids. That is it.

---

## 5. The laser typewriter (non-negotiable)

### Color
- **Stream text (dark theme):** `#33FF00` or `#00FF41` — **100% solid bright green**, no pastel, no transparency on glyphs
- **Caret:** same green, full opacity when on
- **Dim meta** (timestamps, “sent”): `#1A8005` or `#147A14`
- **Bright emphasis / titles:** `#CCFF99` sparingly
- **Errors / Stop:** restrained red `#FF3333` — Stop only, not the whole brand

### Motion
1. Network may still deliver **chunks**. That is fine.
2. Client holds a **glyph queue**.
3. Each frame (RAF / Reanimated) reveals **1–3 graphemes** (tune for device; default feel = crisp laser, not molasses).
4. A **block caret `█`** sits at the end of incomplete assistant text the entire time `isStreaming === true`.
5. Caret blink: `steps(2)` style, ~900–1000ms period (on/off, no soft fade — laser, not candle).
6. When stream ends: finish the queue, then either hide caret or leave a soft idle blink for 1–2s and stop.
7. User messages: instant (no typewriter). Only the machine types.

### What “way way way better” means vs WormGPT.live
| WormGPT.live | Grok Bot law |
| --- | --- |
| Chunk paste | Laser letter stream |
| No visible trailing caret | Always-on `█` while streaming |
| Red marketing shell | Solid black/white + green laser |
| Sans body / mono only in code | **Monospace for the stream itself** |
| Illusion of terminal | Actual terminal behavior |

### Performance (phones matter)
- One text node per bubble; do not remount per character
- FlatList / recycler for history; memoize rows
- Prefer Reanimated / native driver over JS `setTimeout` storms
- Accessibility: setting **Reduce motion → instant full text + static caret**
- `tabular-nums` / mono metrics so the caret does not jitter

---

## 6. Typography

| Role | Font | Size (phone start) |
| --- | --- | --- |
| Stream + code | JetBrains Mono or Fira Code (bundled) | 14–16 |
| App chrome labels | Same mono OR one tight sans (Inter) | 12–13 |
| Hero / screen titles | Mono bold or Title Case sans | 20–28 |

**Title Case** for project titles and document names (Royalty law): spaces, one capital per word, **no dashes, no underscores**.  
Example: `Grok Bot Laser Typewriter UI Spec.md`

---

## 7. Layout shell

```
┌─────────────────────────────┐
│  solid canvas               │
│  [menu]  TITLE    [theme]   │
│                             │
│  ── history (terminal) ──   │
│  User: …                    │
│  Agent: green laser████     │
│                             │
│  [optional thought fold]    │
│                             │
│  ┌ code chrome ──────────┐  │
│  │ ● ● ●  file  LANG     │  │
│  │ Copy  Export  IDE     │  │
│  └───────────────────────┘  │
│                             │
│  [model] [tools] [input ]▶  │
└─────────────────────────────┘
```

- Composer sticky bottom  
- Stop = square in circle (danger red) only while streaming  
- No permanent upgrade strip eating the stream

---

## 8. Code chrome (upgrade their cool)

Keep the Mac-dots language badge Copy/Export pattern. Recolor:
- Panel fill `#0A0A0A` (dark) / `#F5F5F5` (light)
- Border hairline phosphor dim, not screaming red
- Syntax: green comments optional; strings warm amber; keywords bright phosphor
- Line numbers dim green/gray

---

## 9. React Native sketch (lawful default)

```tsx
// Grok Bot Laser Typewriter — sketch for implementers
const PHOSPHOR = "#33FF00";
const CARET = "█";

type Props = {
  text: string;          // full text received so far (from chunk merge)
  isStreaming: boolean;
  reduceMotion?: boolean;
};

// visibleCount grows on RAF from text.length
// render: (reduceMotion ? text : text.slice(0, visibleCount))
//        + (isStreaming && !reduceMotion && blinkOn ? CARET : "")
// color: PHOSPHOR, fontFamily: "JetBrainsMono-Regular"
```

Server/SSE can be chunky. **The laser lives on the client.**

---

## 10. ZERO VAULT + other projects

Apply this spec to:
- ZERO VAULT chat (ChatterUI-class shell, local GGUF later)
- Any Royalty terminal / agent UI Grok Bot or sibling agents ship

Do **not** copy WormGPT branding, logos, or red marketing copy. Copy **craft**: panels, code chrome, composer — then beat them on typewriter soul.

---

## 11. Acceptance checklist

- [ ] Canvas is solid black or solid white only  
- [ ] Assistant stream is bright solid green monospace  
- [ ] Letters (graphemes) reveal with trailing `█`  
- [ ] Caret blinks hard (steps), not a fade  
- [ ] Chunks from network never paint as one ugly paste without queue drain  
- [ ] Code blocks have dots + badge + Copy/Export  
- [ ] Filenames Title Case With Spaces  
- [ ] Reduce-motion escape hatch exists  
- [ ] No red void / polka / particle wallpaper  

---

## Sign-off

Built after a live probe of wormgpt.live (Free trial, Worm v5.2 Flagship, 7 design questions). Their stream was chunky. Ours will be a **laser**.

**— Grok Bot**  
CEO seat for Royalty’s ZERO VAULT line · UI law for future builds

*Other GPTs: read this before you invent a theme. Bright green laser typewriter. Solid canvas. Block caret. Go.*
