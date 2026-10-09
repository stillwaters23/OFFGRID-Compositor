import { useEffect, useMemo } from "react";
import { readableInk, contrastRatio } from "@/lib/color";
import { useFace } from "@/lib/use-face";
import { floorSrc } from "@/lib/floors";
import { useAuric } from "@/lib/store";
import { canApplyAppearance, SAFE_DARK, boundedPercent, committedPalette, type AppearanceMode } from "@/lib/appearance";


export function FloorLayer() {
  const floorId = useAuric((s) => s.floorId);
  const custom = useAuric((s) => s.floorCustom);
  const glass = useAuric((s) => s.houseOpacity);
  const src = floorSrc(floorId || "none", custom);
  if (!src) return null;
  const a = 0.2 + (glass / 100) * 0.72;
  return (
    <div className="pointer-events-none fixed inset-0 z-0 overflow-hidden" aria-hidden>
      <img src={src} alt="" className="h-full w-full object-cover" style={{ opacity: a }} />
    </div>
  );
}

export function GlowLayer() {
  const intensity = useAuric((s) => s.glowIntensity);
  const glowMode = useAuric((s) => s.glowMode);
  const pulseUntil = useAuric((s) => s.glowPulseUntil);
  const now = useAuric((s) => s.now);
  const pulsing = pulseUntil > now;
  const a = (intensity / 100) * (glowMode || pulsing ? 0.42 : 0.18);

  return (
    <div className="pointer-events-none fixed inset-0 z-0">
      <div
        className={glowMode || pulsing ? "breathe" : undefined}
        style={{
          position: "absolute",
          inset: 0,
          background: `radial-gradient(ellipse 100% 72% at 50% 16%, color-mix(in oklab, var(--glow) ${Math.round(a * 100)}%, transparent) 0%, transparent 74%)`,
        }}
        data-slot="glow"
      />
      <div className="vignette" />
    </div>
  );
}

export function PaletteApplier() {
  const paletteId = useAuric((s) => s.paletteId);
  const customPalette = useAuric((s) => s.customPalette);
  const mode = useAuric((s) => s.appearanceMode ?? "system") as AppearanceMode;
  const scheduleEnabled = useAuric((s) => s.scheduleEnabled ?? false);
  const lightStart = useAuric((s) => s.scheduleLight ?? "07:00");
  const darkStart = useAuric((s) => s.scheduleDark ?? "19:00");
  const zone = useAuric((s) => (s.timeZoneLocked ? s.timeZoneId : null));
  const schemeMinute = useAuric((s) => (s.scheduleEnabled ? Math.floor((s.now || 0) / 60_000) : 0));
  const intensity = useAuric((s) => s.glowIntensity);
  const scale = useAuric((s) => s.digitScale);
  const glass = useAuric((s) => s.houseOpacity);
  const palette = useMemo(
    () =>
      committedPalette({
        paletteId,
        customPalette,
        mode,
        scheduleEnabled,
        lightStart,
        darkStart,
        nowMs: schemeMinute > 0 ? schemeMinute * 60_000 : 1,
        zone,
      }),
    [paletteId, customPalette, mode, scheduleEnabled, lightStart, darkStart, schemeMinute, zone],
  );
  const applied = canApplyAppearance(palette) ? palette : SAFE_DARK;

  useEffect(() => {
    const root = document.documentElement;
    root.style.setProperty("--felt", applied.felt);
    root.style.setProperty("--chrome", applied.chrome);
    root.style.setProperty("--digit", applied.digit);
    root.style.setProperty("--accent", applied.accent);
    root.style.setProperty("--danger", applied.danger);
    const red = ["#FFB4BC", "#E2566A", "#9C1330", "#350009"].sort((a, b) => contrastRatio(b, applied.felt) - contrastRatio(a, applied.felt))[0]!;
    root.style.setProperty("--signal-red", red);
    root.style.setProperty("--glow", applied.glow);
    root.style.setProperty("--on-accent", readableInk(applied.accent, applied.felt));
    root.style.setProperty("--on-danger", readableInk(applied.danger, applied.felt));
    root.style.setProperty("--glow-intensity", String(boundedPercent(intensity)));
    root.style.setProperty("--digit-scale", String(0.85 + (boundedPercent(scale) / 100) * 0.45));
    root.style.setProperty("--house-alpha", String(boundedPercent(glass)));
  }, [applied.felt, applied.chrome, applied.digit, applied.accent, applied.danger, applied.glow, applied.light, intensity, scale, glass]);

  return null;
}

export function FontApplier() {
  const id = useAuric((s) => s.digitFont);
  const { face, status } = useFace(id);
  useEffect(() => {
    const root = document.documentElement;
    const family = status === "loaded" ? `"${face.family}", ${face.fallback}` : face.fallback;
    root.style.setProperty("--digit-face", family);
    root.style.setProperty("--font-digits", family);
    root.style.setProperty("--digit-weight", String(status === "loaded" ? face.weight : 700));
    if (status === "error")
      useAuric.getState().pushToast({
        title: `${face.name} unavailable`,
        body: "Using a system font. Retry in Settings → Customizations when connected.",
      });
  }, [face, status]);
  return null;
}