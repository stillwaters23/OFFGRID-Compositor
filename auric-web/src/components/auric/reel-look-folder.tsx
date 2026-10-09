import { useEffect, useState, type CSSProperties } from "react";
import { previewPalette } from "@/lib/appearance";
import { hexToRgb, isValidHex, normalizeHex } from "@/lib/color";
import { resolveReelNumberColor } from "@/lib/contrast-guard";
import { useAuric } from "@/lib/store";
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
        <h2 className="tx-title text-sm">Laser color</h2>
        <p className="tx-muted mt-2 text-xs">Decorative light · no text-contrast rule</p>
        <div className="mt-4 grid grid-cols-2 gap-2 sm:grid-cols-3">
          {LASER_PRESETS.map((preset) => (
            <button
              key={preset.hex}
              type="button"
              aria-pressed={draft.laserColor === preset.hex}
              className="min-h-12 rounded-2xl px-4 text-left text-sm panel-glass"
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
      </section>
    </div>
  );
}
