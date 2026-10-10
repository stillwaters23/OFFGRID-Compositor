import { useEffect, useState, type CSSProperties } from "react";
import { committedPalette, previewPalette } from "@/lib/appearance";
import { hexToRgb, isValidHex, normalizeHex } from "@/lib/color";
import { LASER_PRESETS, checkNumberColor, laserVars, resolveReelNumberColor } from "@/lib/contrast-guard";
import { useAuric } from "@/lib/store";
import { OrbToggle } from "@/components/auric/switch";
import "@/styles/reel-look.css";

export function ReelLookFolder() {
  const draft = useAuric((state) => state.appearanceDraft);
  const [hexText, setHexText] = useState(draft?.laserColor ?? LASER_PRESETS[0].hex);
  const [hexError, setHexError] = useState("");
  const draftLaserColor = draft?.laserColor;
  const now = useAuric((state) => state.now);
  const zone = useAuric((state) => (state.timeZoneLocked ? state.timeZoneId : null));
  useEffect(() => {
    if (!draftLaserColor) return;
    setHexText(draftLaserColor);
    setHexError("");
  }, [draftLaserColor]);
  if (!draft) return null;

  function setCustomHex(value: string) {
    setHexText(value);
    if (!value.trim() || !isValidHex(value) || hexToRgb(value)?.a !== 1) {
      setHexError("Enter a valid opaque #RGB or #RRGGBB color.");
      return;
    }
    const normalized = normalizeHex(value);
    if (!normalized || !useAuric.getState().setDraftLaserColor(normalized)) {
      setHexError("Enter a valid opaque #RGB or #RRGGBB color.");
      return;
    }
    setHexError("");
  }

  const activePalette = previewPalette(draft, now, zone);
  const activeTheme = activePalette.light ? "light" : "dark";
  const beam = laserVars(draft.laserColor, draft.laserIntensity, draft.laserEnabled);
  const readout = resolveReelNumberColor(activePalette, draft.numberColor[activeTheme], activeTheme, {
    palette: activePalette,
    laserColor: draft.laserColor,
    laserIntensity: draft.laserIntensity,
    laserEnabled: draft.laserEnabled,
  });
  const themeChecks = (["dark", "light"] as const).map((theme) => {
    const palette = committedPalette({
      paletteId: draft.palette.id,
      customPalette: draft.palette.id === "custom" ? draft.palette : null,
      mode: theme,
      scheduleEnabled: false,
      lightStart: draft.scheduleLight,
      darkStart: draft.scheduleDark,
      nowMs: 1,
    });
    const context = {
      palette,
      laserColor: draft.laserColor,
      laserIntensity: draft.laserIntensity,
      laserEnabled: draft.laserEnabled,
    };
    const value = draft.numberColor[theme];
    const fallback = resolveReelNumberColor(palette, null, theme, context);
    const shownColor = value ?? fallback;
    return { theme, value, shownColor, fallback, check: checkNumberColor(shownColor, theme, context) };
  });

  return (
    <div className="mt-4 min-w-0 space-y-4">
      <div className="sticky top-0 z-10 rounded-2xl p-4 panel-glass" aria-label="Live laser preview">
        <p className="tx-prompt uppercase">Live witness · span reel</p>
        <div
          className="reel-look-witness-reel mt-2 flex items-center justify-center rounded-2xl"
          data-laser-witness=""
          style={{ ...beam, "--reel-number": readout } as CSSProperties}
        >
          <div className="reel-look-reels" aria-hidden>
            <span className="reel-look-drum">08</span>
            <span className="font-digits text-base" style={{ color: readout }}>:</span>
            <span className="reel-look-drum">24</span>
            <span className="font-digits text-base" style={{ color: readout }}>:</span>
            <span className="reel-look-drum">16</span>
          </div>
          <span className="reel-look-hairline" data-laser-hairline="" aria-hidden />
          <span className="reel-look-beam" data-laser-beam="" aria-hidden>
            <span className="reel-look-beam-shimmer">
              <span className="reel-look-beam-bloom" />
              <span className="reel-look-beam-core" />
            </span>
          </span>
        </div>
      </div>

      <section className="rounded-2xl p-4 panel-glass">
        <div className="flex min-h-12 items-center justify-between gap-4">
          <div>
            <h2 className="font-display text-base">Laser color</h2>
            <p className="tx-muted mt-2">Decorative light · no text-contrast rule</p>
          </div>
          <OrbToggle
            on={draft.laserEnabled}
            onChange={(enabled) => useAuric.getState().setDraftLaserEnabled(enabled)}
            label="Laser on/off"
          />
        </div>
        <div className="mt-4 grid grid-cols-2 gap-2 sm:grid-cols-3">
          {LASER_PRESETS.map((preset) => (
            <button
              key={preset.hex}
              type="button"
              data-laser-preset={preset.hex}
              aria-pressed={draft.laserColor === preset.hex}
              className="reel-look-preset min-h-12 rounded-2xl px-4 text-left text-base panel-glass"
              onClick={() => {
                useAuric.getState().setDraftLaserColor(preset.hex);
                setHexText(preset.hex);
                setHexError("");
              }}
            >
              <span
                className="mr-2 inline-block size-4 rounded-full align-middle"
                style={{ backgroundColor: preset.hex, boxShadow: `0 0 8px ${preset.hex}` }}
              />
              {preset.name}
            </button>
          ))}
        </div>
        <label className="mt-4 block text-base">
          Custom hex
          <input
            aria-label="Custom laser hex"
            value={hexText}
            onChange={(event) => setCustomHex(event.target.value)}
            className="reel-look-hex-input mt-2 min-h-12 w-full rounded-2xl bg-transparent px-4 font-mono text-base"
            autoComplete="off"
            spellCheck={false}
          />
        </label>
        {hexError ? <p role="alert" className="mt-2 text-base text-danger">{hexError}</p> : null}
        <label className="mt-4 block text-base">
          Laser intensity · {draft.laserIntensity}%
          <input
            aria-label="Laser intensity"
            type="range"
            min={0}
            max={100}
            step={1}
            value={draft.laserIntensity}
            className="reel-look-slider mt-2 block w-full"
            onChange={(event) => useAuric.getState().setDraftLaserIntensity(Number(event.target.value))}
          />
        </label>
      </section>

      <section className="rounded-2xl p-4 panel-glass">
        <h2 className="font-display text-base">Number color</h2>
        <p className="tx-muted mt-2">Selected-row numerals and the readout plaque · minimum 4.5:1</p>
        <p className="tx-muted mt-2">
          {themeChecks.map((entry, index) => (
            <span key={entry.theme}>
              {index > 0 ? " · " : null}
              {entry.theme === "dark" ? "Dark" : "Light"} {entry.check.ratio.toFixed(2)}:1 {entry.check.ok ? "✓" : "✗"}
            </span>
          ))}
        </p>
        {themeChecks.map((entry) => (
          <div key={entry.theme} className="mt-4 rounded-2xl p-4 shadow-[0_0_0_1px_var(--line)]">
            <div className="flex min-h-12 items-center gap-4">
              <input
                aria-label={`${entry.theme} number color`}
                type="color"
                value={entry.shownColor}
                onChange={(event) => useAuric.getState().setDraftNumberColor(entry.theme, event.target.value)}
                className="size-12 shrink-0 cursor-pointer rounded-lg border-0 bg-transparent"
              />
              <div className="min-w-0 flex-1">
                <p className="font-display text-base capitalize">{entry.theme} · {entry.value ?? "Follow palette digit"}</p>
                <p className={entry.check.ok ? "tx-muted" : "text-base text-danger"}>
                  {entry.theme === "dark" ? "Dark" : "Light"} {entry.check.ratio.toFixed(2)}:1 {entry.check.ok ? "✓" : "✗"}
                </p>
              </div>
              <button
                type="button"
                className="min-h-12 rounded-lg px-4 text-base text-accent shadow-[0_0_0_1px_var(--line)]"
                onClick={() => useAuric.getState().setDraftNumberColor(entry.theme, null)}
              >
                Follow palette
              </button>
            </div>
            {!entry.check.ok ? <p role="alert" className="mt-2 text-base text-danger">{entry.check.message}</p> : null}
          </div>
        ))}
      </section>
    </div>
  );
}
