import { useEffect, useState, type CSSProperties } from "react";
import { committedPalette, previewPalette } from "@/lib/appearance";
import { hexToRgb, isValidHex, normalizeHex } from "@/lib/color";
import { checkNumberColor, resolveReelNumberColor } from "@/lib/contrast-guard";
import { useAuric } from "@/lib/store";
import { OrbToggle } from "@/components/auric/switch";
import "@/styles/reel-look.css";

const LASER_PRESETS = [
  { name: "Red", hex: "#FF0000" },
  { name: "Cyan", hex: "#00E5FC" },
  { name: "Blue", hex: "#0032FF" },
  { name: "Magenta", hex: "#FF00A6" },
  { name: "Violet", hex: "#7B02B3" },
] as const;

export function ReelLookFolder() {
  const draft = useAuric((state) => state.appearanceDraft);
  const [hexText, setHexText] = useState(draft?.laserColor ?? "#FF0000");
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
  const readout = resolveReelNumberColor(
    activePalette,
    draft.numberColor[activeTheme],
    activeTheme,
    {
      palette: activePalette,
      laserColor: draft.laserColor,
      laserIntensity: draft.laserIntensity,
      laserEnabled: draft.laserEnabled,
    },
  );

  return (
    <div className="mt-4 min-w-0 space-y-4">
      <div className="reel-look-witness sticky top-0 z-10 rounded-2xl p-4 panel-glass" aria-label="Live laser preview">
        <p className="tx-label text-xs uppercase tracking-[0.16em]">Live witness · span reel</p>
        <div className="reel-look-witness-reel mt-2 flex min-h-16 items-center justify-center rounded-2xl"
          style={{ "--witness-color": draft.laserColor, "--witness-opacity": draft.laserEnabled ? String(0.2 + 0.8 * draft.laserIntensity / 100) : "0" } as CSSProperties}>
          <div className="reel-look-reels" aria-hidden>
            <span className="reel-look-drum" style={{ color: readout }}>08</span>
            <span className="font-digits" style={{ color: readout }}>:</span>
            <span className="reel-look-drum" style={{ color: readout }}>24</span>
            <span className="font-digits" style={{ color: readout }}>:</span>
            <span className="reel-look-drum" style={{ color: readout }}>16</span>
          </div>
          <span className="reel-look-witness-beam" aria-hidden />
        </div>
      </div>

      <section className="rounded-2xl p-4 panel-glass">
        <div className="flex min-h-12 items-center justify-between gap-4">
          <div>
            <h2 className="tx-title text-sm">Laser color</h2>
            <p className="tx-muted mt-2 text-xs">Decorative light · no text-contrast rule</p>
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
              aria-pressed={draft.laserColor === preset.hex}
              className="reel-look-preset min-h-12 rounded-2xl px-4 text-left text-sm panel-glass"
              onClick={() => {
                useAuric.getState().setDraftLaserColor(preset.hex);
                setHexText(preset.hex);
                setHexError("");
              }}
            >
              <span className="mr-2 inline-block size-4 rounded-full align-middle shadow-[0_0_16px_var(--swatch)]"
                style={{ "--swatch": preset.hex, backgroundColor: preset.hex } as CSSProperties} />
              {preset.name}
            </button>
          ))}
        </div>
        <label className="mt-4 block text-sm">
          Custom hex
          <input
            aria-label="Custom laser hex"
            value={hexText}
            onChange={(event) => setCustomHex(event.target.value)}
            className="reel-look-hex-input mt-2 min-h-12 w-full rounded-2xl bg-transparent px-4 font-mono"
            autoComplete="off"
            spellCheck={false}
          />
        </label>
        {hexError ? <p role="alert" className="mt-2 text-sm text-danger">{hexError}</p> : null}
        <label className="mt-4 block text-sm">
          Laser intensity · {draft.laserIntensity}%
          <input
            aria-label="Laser intensity"
            type="range"
            min={0}
            max={100}
            step={1}
            value={draft.laserIntensity}
            className="reel-look-slider mt-2 block min-h-12 w-full"
            onChange={(event) => useAuric.getState().setDraftLaserIntensity(Number(event.target.value))}
          />
        </label>
      </section>

      <section className="rounded-2xl p-4 panel-glass">
        <h2 className="tx-title text-sm">Number color</h2>
        <p className="tx-muted mt-2 text-xs">Selected-row numerals and the readout plaque · minimum 4.5:1</p>
        {(["dark", "light"] as const).map((theme) => {
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
          const check = checkNumberColor(shownColor, theme, context);
          return (
            <div key={theme} className="mt-4 rounded-2xl p-4 shadow-[0_0_0_1px_var(--line)]">
              <div className="flex min-h-12 items-center gap-4">
                <input
                  aria-label={`${theme} number color`}
                  type="color"
                  value={shownColor}
                  onChange={(event) => useAuric.getState().setDraftNumberColor(theme, event.target.value)}
                  className="size-12 shrink-0 cursor-pointer rounded-lg border-0 bg-transparent p-1"
                />
                <div className="min-w-0 flex-1">
                  <p className="tx-title capitalize">{theme} · {value ?? "Follow palette digit"}</p>
                  <p className={check.ok ? "tx-muted text-xs" : "text-xs text-danger"}>
                    {theme === "dark" ? "Dark" : "Light"} {check.ratio.toFixed(2)}:1 {check.ok ? "✓" : "✗"}
                  </p>
                </div>
                <button
                  type="button"
                  className="min-h-12 rounded-lg px-4 text-xs text-accent shadow-[0_0_0_1px_var(--line)]"
                  onClick={() => useAuric.getState().setDraftNumberColor(theme, null)}
                >
                  Follow palette
                </button>
              </div>
              {!check.ok ? <p role="alert" className="mt-2 text-sm text-danger">{check.message}</p> : null}
            </div>
          );
        })}
      </section>
    </div>
  );
}
