import { useEffect, useMemo, useState, type CSSProperties } from "react";
import { Palette, Sparkles, Type, Droplets, Shapes, Zap } from "lucide-react";
import { DrumReel } from "@/components/auric/drum-reel";
import { ReelLookFolder } from "@/components/auric/reel-look-folder";
import { PitButton } from "@/components/auric/pit-button";
import {
  PARTS,
  RESTORE_CHROME,
  appearanceIssues,
  boundedPercent,
  canApplyAppearance,
  committedPalette,
  paletteStyle,
  previewPalette,
  type AppearanceMode,
} from "@/lib/appearance";
import { checkNumberColor } from "@/lib/contrast-guard";
import { hexToHsl, hexToRgb, hslToHex, isValidHex, normalizeHex, rgbToHex } from "@/lib/color";
import { CURATED, ROYGBIV } from "@/lib/palettes";
import { FLOORS, readFloorFile, floorSrc } from "@/lib/floors";
import { FACES, faceShort, facesForLiteracy } from "@/lib/faces";
import { useFace } from "@/lib/use-face";
import { playClick, stopPreview } from "@/lib/audio";
import { isAppearanceDirty, useAuric } from "@/lib/store";
import type { PaintSlot } from "@/lib/types";
import { ICON_CUTS } from "@/lib/banks";
import { cn } from "@/lib/utils";

const FOLDERS = [
  { id: "color" as const, name: "Color", hint: "Page, panel, digits, and accents.", icon: Palette },
  { id: "themes" as const, name: "Themes", hint: "Palettes, icons, light/dark, floor.", icon: Sparkles },
  { id: "sizing" as const, name: "Sizing", hint: "Number font and scale.", icon: Type },
  { id: "opacity" as const, name: "Opacity", hint: "Glass and glow. 0 is clear.", icon: Droplets },
  { id: "laser" as const, name: "Laser & Numbers", hint: "Beam color, intensity, and readable numerals.", icon: Zap },
];

export function AppearanceStudio() {
  const folder = useAuric((s) => s.customizationFolder);
  const dirty = useAuric(isAppearanceDirty);

  useEffect(() => {
    useAuric.getState().beginAppearanceDraft();
    return () => stopPreview();
  }, []);

  return (
    <div className="relative flex min-h-0 min-w-0 max-w-full flex-1 flex-col overflow-x-hidden overflow-y-auto px-5 pb-16">
      <p className="mt-1 font-display text-lg text-accent">{folder ? FOLDERS.find((f) => f.id === folder)?.name : "Choose a folder"}</p>
      <p className="text-sm text-muted">{dirty ? "Unsaved draft. Apply to keep it, Discard to leave it." : "Preview locally. Apply commits. Discard keeps the saved look."}</p>
      {!folder ? <FolderList /> : folder === "color" ? <ColorFolder /> : folder === "themes" || folder === "icons" ? <ThemesFolder /> : folder === "sizing" ? <SizingFolder /> : folder === "laser" ? <ReelLookFolder /> : <OpacityFolder />}
      <StudioActions />
    </div>
  );
}

function FolderList() {
  return (
    <div className="mt-4 grid gap-2">
      {FOLDERS.map((folder) => {
        const Icon = folder.icon;
        return (
          <button
            key={folder.id}
            type="button"
            className="flex min-h-14 min-w-0 items-center gap-3 rounded-2xl px-4 text-left panel-glass"
            onClick={() => useAuric.getState().setCustomizationFolder(folder.id)}
          >
            <Icon className="size-5 shrink-0 text-accent" />
            <span className="min-w-0">
              <span className="block font-display text-sm">{folder.name}</span>
              <span className="block text-xs text-muted">{folder.hint}</span>
            </span>
          </button>
        );
      })}
      <RestoreCard />
    </div>
  );
}

function RestoreCard() {
  return (
    <div className="mt-4 rounded-2xl p-4" style={RESTORE_CHROME as CSSProperties}>
      <p className="font-display text-sm tracking-wide">Restore readable appearance</p>
      <p className="mt-1 text-[11px] leading-snug" style={{ color: "#C9D7EE" }}>
        Restores readable colors, type size, and a plain background. Alarms, notes, and history stay.
      </p>
      <button
        type="button"
        className="mt-3 min-h-11 rounded-lg px-4 text-sm"
        style={{ background: "#7FC6FF", color: "#0A1730" }}
        onClick={() => useAuric.getState().restoreReadableAppearance()}
      >
        Restore
      </button>
    </div>
  );
}

function StudioActions() {
  const dirty = useAuric(isAppearanceDirty);
  const draft = useAuric((s) => s.appearanceDraft);
  const now = useAuric((s) => s.now);
  const zone = useAuric((s) => (s.timeZoneLocked ? s.timeZoneId : null));
  const currentFont = useAuric((s) => s.digitFont);
  const draftFace = useFace(draft?.digitFont ?? currentFont);
  const look = draft ? previewPalette(draft, now, zone) : undefined;
  const issues = look ? appearanceIssues(look) : [];
  const numberIssue = draft
    ? (["dark", "light"] as const).map((theme) => {
        const color = draft.numberColor[theme];
        if (!color) return null;
        const palette = committedPalette({
          paletteId: draft.palette.id,
          customPalette: draft.palette.id === "custom" ? draft.palette : null,
          mode: theme,
          scheduleEnabled: false,
          lightStart: draft.scheduleLight,
          darkStart: draft.scheduleDark,
          nowMs: 1,
        });
        return checkNumberColor(color, theme, {
          palette,
          laserColor: draft.laserColor,
          laserIntensity: draft.laserIntensity,
          laserEnabled: draft.laserEnabled,
        });
      }).find((check) => check && !check.ok)?.message
    : undefined;
  const readable = look && draft ? canApplyAppearance(look) && canApplyAppearance(draft.palette) && !numberIssue : true;
  const fontReady = !draft || draft.digitFont === currentFont || draftFace.status === "loaded";

  function apply() {
    const result = useAuric.getState().applyAppearance();
    if (!result.ok) useAuric.getState().pushToast({ title: "Could not apply", body: result.reason, tone: "danger" });
    else useAuric.getState().beginAppearanceDraft();
  }

  return (
    <div className="mt-6 space-y-2">
      {issues[0] ? <p className="text-sm text-danger">{issues[0].message}</p> : null}
      {numberIssue ? <p role="alert" className="text-sm text-danger">{numberIssue}</p> : null}
      {!fontReady ? <p className="text-sm text-muted">The selected font is still loading.</p> : null}
      <div className="flex min-w-0 flex-wrap gap-2">
        <PitButton data-testid="apply-appearance" disabled={!dirty || !readable || !fontReady} className="min-h-11 min-w-0 flex-1" onClick={apply}>
          Apply appearance
        </PitButton>
        <PitButton variant="ghost" className="min-h-11 min-w-0 flex-1" onClick={() => { useAuric.getState().discardAppearance(); useAuric.getState().beginAppearanceDraft(); }}>
          Discard
        </PitButton>
      </div>
      {!readable ? (
        <button type="button" className="min-h-11 w-full rounded-lg text-sm shadow-[0_0_0_1px_var(--line)]" onClick={() => useAuric.getState().makeDraftReadable()}>
          Make readable
        </button>
      ) : null}
    </div>
  );
}

function SkinPreview({ slot }: { slot: PaintSlot }) {
  const draft = useAuric((s) => s.appearanceDraft);
  const font = useFace(draft?.digitFont ?? "pit");
  if (!draft) return null;
  const state = useAuric.getState();
  const look = previewPalette(draft, state.now, state.timeZoneLocked ? state.timeZoneId : null);
  const photo = floorSrc(draft.floorId, draft.floorCustom);
  const family = font.status === "loaded" ? `"${font.face.family}", ${font.face.fallback}` : font.face.fallback;
  return (
    <div
      className="skin-preview relative isolate overflow-hidden mt-2 rounded-2xl p-4"
      data-testid="skin-preview"
      style={{
        ...paletteStyle(look),
        fontFamily: family,
        ["--digit-face" as string]: family,
        ["--font-digits" as string]: family,
        ["--digit-weight" as string]: String(font.status === "loaded" ? font.face.weight : 700),
        ["--digit-scale" as string]: String(0.85 + boundedPercent(draft.digitScale) * 0.0045),
        ["--glow-intensity" as string]: String(boundedPercent(draft.glowIntensity)),
        ["--house-alpha" as string]: String(boundedPercent(draft.houseOpacity)),
      }}
    >
      {photo ? <img src={photo} alt="" aria-hidden className="pointer-events-none absolute inset-0 -z-10 h-full w-full object-cover" style={{ opacity: 0.2 + boundedPercent(draft.houseOpacity) * 0.0072 }} /> : null}
      <p className="text-center text-[10px] uppercase tracking-[0.18em] text-muted">Editing {PARTS.find((p) => p.id === slot)?.label}</p>
      <p className="mt-2 text-center font-digits text-4xl leading-none tracking-wide digit-cut" style={{ color: look.digit }}>12:34:56</p>
      <p className="mt-2 text-center text-sm" style={{ color: look.accent }}>Selected object preview</p>
    </div>
  );
}

function ColorFolder() {
  const slot = useAuric((s) => s.spectrumSlot);
  const draft = useAuric((s) => s.appearanceDraft);
  return (
    <div className="mt-3">
      <SkinPreview slot={slot} />
      <div className="mt-3 flex flex-wrap gap-1.5">
        {PARTS.map((part) => (
          <button
            key={part.id}
            type="button"
            className={cn("min-h-11 rounded-full px-3 text-xs", slot === part.id ? "bg-accent text-on-accent" : "shadow-[0_0_0_1px_var(--line)]")}
            onClick={() => useAuric.getState().setSpectrumSlot(part.id)}
          >
            {part.label}
          </button>
        ))}
      </div>
      <p className="mt-2 text-xs text-muted">{PARTS.find((p) => p.id === slot)?.hint}</p>
      {draft ? <ColorEditor key={slot} slot={slot} hex={draft.palette[slot]} /> : null}
    </div>
  );
}

function ColorEditor({ slot, hex }: { slot: PaintSlot; hex: string }) {
  const rgb = hexToRgb(hex) ?? { r: 0, g: 0, b: 0, a: 1 };
  const hsl = hexToHsl(hex) ?? { h: 0, s: 0, l: 0 };
  const [hexText, setHexText] = useState(hex);
  const [hexError, setHexError] = useState("");
  const [editingHex, setEditingHex] = useState(false);

  useEffect(() => {
    if (!editingHex) { setHexText(hex); setHexError(""); }
  }, [hex, slot, editingHex]);

  function paint(next: string) {
    const norm = normalizeHex(next);
    if (!norm) return;
    useAuric.getState().setDraftSlot(slot, norm);
  }

  return (
    <div className="mt-3 space-y-3">
      <div className="h-12 rounded-xl" style={{ background: hex }} aria-hidden />
      <div className="flex flex-wrap gap-1">
        {ROYGBIV.map((swatch) => (
          <button key={swatch.name} type="button" className="size-11 rounded-full" style={{ background: swatch.hex }} aria-label={swatch.name} onClick={() => paint(swatch.hex)} />
        ))}
      </div>
      <label className="block text-xs text-muted">
        Hex
        <input
          value={hexText}
          onFocus={() => setEditingHex(true)}
          onBlur={() => setEditingHex(false)}
          spellCheck={false}
          aria-invalid={hexError ? true : undefined}
          placeholder="#RGB or #RRGGBB"
          className="mt-1 h-11 w-full min-w-0 rounded-md bg-felt px-3 text-digit"
          onChange={(e) => {
            const value = e.target.value;
            setHexText(value);
            if (!value || value === "#" || /^#?[0-9a-fA-F]{0,6}$/.test(value)) {
              setHexError("");
              if (isValidHex(value) && (value.replace("#", "").length === 3 || value.replace("#", "").length === 6)) {
                const norm = normalizeHex(value);
                if (norm) paint(norm);
              }
            } else setHexError("Use #RGB or #RRGGBB.");
          }}
        />
      </label>
      {hexError ? <p className="text-xs text-danger">{hexError}</p> : null}
      <div className="grid min-w-0 max-w-full grid-cols-3 gap-2 overflow-x-hidden">
        {(["r", "g", "b"] as const).map((ch) => (
          <label key={ch} className="min-w-0 text-xs uppercase text-muted">
            {ch}
            <input
              type="number"
              min={0}
              max={255}
              value={rgb[ch]}
              className="mt-1 h-11 w-full min-w-0 rounded-md bg-felt px-2 text-digit"
              onChange={(e) => paint(rgbToHex(ch === "r" ? Number(e.target.value) : rgb.r, ch === "g" ? Number(e.target.value) : rgb.g, ch === "b" ? Number(e.target.value) : rgb.b))}
            />
          </label>
        ))}
      </div>
      <label className="block text-xs text-muted">
        Hue {Math.round(hsl.h)}
        <input type="range" min={0} max={359} value={Math.round(hsl.h)} className="mt-1 w-full" onChange={(e) => paint(hslToHex(Number(e.target.value), hsl.s, hsl.l))} />
      </label>
      <label className="block text-xs text-muted">
        Saturation {Math.round(hsl.s)}
        <input type="range" min={0} max={100} value={Math.round(hsl.s)} className="mt-1 w-full" onChange={(e) => paint(hslToHex(hsl.h, Number(e.target.value), hsl.l))} />
      </label>
      <label className="block text-xs text-muted">
        Lightness {Math.round(hsl.l)}
        <input type="range" min={0} max={100} value={Math.round(hsl.l)} className="mt-1 w-full" onChange={(e) => paint(hslToHex(hsl.h, hsl.s, Number(e.target.value)))} />
      </label>
    </div>
  );
}

function ThemesFolder() {
  const draft = useAuric((s) => s.appearanceDraft);
  const folder = useAuric((s) => s.customizationFolder);
  const mode = draft?.appearanceMode ?? "system";
  const scheduleOn = draft?.scheduleEnabled ?? false;
  if (!draft) return null;
  return (
    <div className="mt-3">
      <SkinPreview slot="felt" />
      <p className="mt-4 font-display text-sm uppercase tracking-[0.18em] text-accent">Light and dark</p>
      <div className="mt-2 flex min-w-0 flex-wrap gap-1">
        {(["system", "light", "dark"] as AppearanceMode[]).map((id) => (
          <button
            key={id}
            type="button"
            className={cn("min-h-11 min-w-0 flex-1 rounded-lg text-xs capitalize", mode === id ? "bg-accent text-on-accent" : "shadow-[0_0_0_1px_var(--line)]")}
            onClick={() => useAuric.getState().setDraftOptions({ appearanceMode: id })}
          >
            {id}
          </button>
        ))}
      </div>
      <label className="mt-3 flex min-h-11 items-center justify-between gap-3 text-sm">
        Local schedule
        <input type="checkbox" checked={scheduleOn} onChange={(e) => useAuric.getState().setDraftOptions({ scheduleEnabled: e.target.checked })} />
      </label>
      <p className="text-[11px] text-muted">Times are in the clock’s zone. Not sunrise. The schedule overrides System; explicit Light or Dark overrides the schedule.</p>
      {scheduleOn ? (
        <div className="mt-2 grid min-w-0 max-w-full grid-cols-2 gap-2 overflow-x-hidden">
          <label className="min-w-0 text-xs text-muted">
            Light from
            <input type="time" value={draft.scheduleLight} onChange={(e) => useAuric.getState().setDraftOptions({ scheduleLight: e.target.value })} className="mt-1 h-11 w-full min-w-0 rounded-md bg-felt px-2 text-digit" />
          </label>
          <label className="min-w-0 text-xs text-muted">
            Dark from
            <input type="time" value={draft.scheduleDark} onChange={(e) => useAuric.getState().setDraftOptions({ scheduleDark: e.target.value })} className="mt-1 h-11 w-full min-w-0 rounded-md bg-felt px-2 text-digit" />
          </label>
        </div>
      ) : null}
      <p className="mt-4 font-display text-sm uppercase tracking-[0.18em] text-accent">Palettes</p>
      <div className="mt-2 grid min-w-0 max-w-full grid-cols-2 gap-2 overflow-x-hidden">
        {CURATED.map((palette) => (
          <button
            key={palette.id}
            type="button"
            className="min-h-14 min-w-0 rounded-xl px-3 text-left text-sm"
            style={{ background: palette.felt, color: palette.digit, boxShadow: draft.palette.id === palette.id ? "0 0 0 2px var(--accent)" : "0 0 0 1px var(--line)" }}
            onClick={() => useAuric.getState().setDraftPalette(palette)}
          >
            {palette.name}
          </button>
        ))}
      </div>
      <FloorDraft />
      <div className="mt-4">
        <button type="button" className="flex min-h-11 items-center gap-2 text-sm text-accent" onClick={() => useAuric.getState().setCustomizationFolder(folder === "icons" ? "themes" : "icons")}>
          <Shapes className="size-4" /> {folder === "icons" ? "Hide Icons" : "Icons"}
        </button>
        {folder === "icons" ? (
          <div className="mt-2 grid min-w-0 max-w-full grid-cols-2 gap-2 overflow-x-hidden">
            {ICON_CUTS.slice(0, 12).map((cut) => (
              <p key={cut.id} className="min-w-0 rounded-xl px-3 py-2 text-xs panel">{cut.name}</p>
            ))}
          </div>
        ) : null}
      </div>
    </div>
  );
}

function FloorDraft() {
  const floorId = useAuric((s) => s.appearanceDraft?.floorId ?? s.floorId);
  const custom = useAuric((s) => s.appearanceDraft?.floorCustom ?? s.floorCustom);
  return (
    <div className="mt-5">
      <p className="font-display text-sm tracking-[0.18em] uppercase text-accent">Floor</p>
      <div className="mt-2 grid min-w-0 max-w-full grid-cols-2 gap-2 overflow-x-hidden">
        {FLOORS.map((floor) => (
          <div key={floor.id} className="relative min-h-16 overflow-hidden rounded-xl">
            {floor.id === "custom" ? (
              <input
                type="file"
                accept="image/*"
                aria-label="Choose a background photo"
                className="absolute inset-0 z-20 opacity-0"
                onChange={async (event) => {
                  const file = event.target.files?.[0];
                  if (!file) return;
                  try {
                    const data = await readFloorFile(file);
                    useAuric.getState().setDraftOptions({ floorCustom: data, floorId: "custom" });
                  } catch {
                    useAuric.getState().pushToast({ title: "Photo unavailable", body: "Choose a readable image. Your current background is unchanged." });
                  }
                }}
              />
            ) : (
              <button type="button" className="absolute inset-0 z-10" aria-label={floor.name} onClick={() => useAuric.getState().setDraftOptions({ floorId: floor.id })} />
            )}
            {floorSrc(floor.id, custom) ? <img src={floorSrc(floor.id, custom)} alt="" className="h-16 w-full object-cover" /> : <div className="h-16 bg-felt" />}
            <span className={cn("absolute bottom-1 left-1 text-[10px]", floorId === floor.id && "text-accent")}>{floor.name}</span>
          </div>
        ))}
      </div>
    </div>
  );
}

function SizingFolder() {
  const draft = useAuric((s) => s.appearanceDraft);
  const literacy = useAuric((s) => s.literacyIndex);
  const [q, setQ] = useState("");
  const [cursor, setCursor] = useState(0);
  const list = useMemo(() => {
    let rows = facesForLiteracy(literacy);
    if (q.trim()) {
      const needle = q.trim().toLowerCase();
      rows = rows.filter((f) => f.name.toLowerCase().includes(needle) || f.family.toLowerCase().includes(needle));
    }
    return rows;
  }, [q, literacy]);
  if (!draft) return null;
  return (
    <div className="mt-3">
      <SkinPreview slot="digit" />
      <label className="mt-4 block text-sm">
        Digit scale {boundedPercent(draft.digitScale)}
        <input type="range" min={0} max={100} value={boundedPercent(draft.digitScale)} className="mt-2 w-full" onChange={(e) => useAuric.getState().setDraftScale(Number(e.target.value))} />
      </label>
      <p className="mt-4 font-display text-sm tracking-[0.18em] uppercase text-accent">Numbers</p>
      <input value={q} onChange={(e) => { setQ(e.target.value); setCursor(0); }} placeholder="Search faces" aria-label="Search faces" className="mt-3 h-11 w-full min-w-0 rounded-md bg-felt/70 px-3 text-sm outline-none shadow-[0_0_0_1px_var(--line)]" />
      {list.length ? (
        <div className="mt-3 flex flex-col items-center gap-2">
          <DrumReel
            key={q.trim().toLowerCase()}
            size="plaque"
            count={list.length}
            value={Math.min(cursor, list.length - 1)}
            labels={list.map(faceShort)}
            label="Choose number font"
            onChange={(index) => {
              setCursor(index);
              const face = list[index];
              if (face) useAuric.getState().setDraftFont(face.id);
            }}
          />
          <button type="button" className="min-h-11 w-full min-w-0 max-w-full whitespace-normal rounded-full px-3 text-sm text-accent shadow-[0_0_0_1px_var(--line)]"
            onClick={() => {
              const face = list[Math.min(cursor, list.length - 1)];
              if (face) { useAuric.getState().setDraftFont(face.id); playClick(); }
            }}>
            Preview {list[Math.min(cursor, list.length - 1)]?.name} · {list[Math.min(cursor, list.length - 1)]?.family}
          </button>
        </div>
      ) : <p className="mt-3 text-sm text-muted">No faces match. Your selected font is unchanged.</p>}
      <p className="mt-2 text-center text-xs text-muted">{list.length} of {FACES.length} · applied after Apply appearance</p>
    </div>
  );
}

function OpacityFolder() {
  const draft = useAuric((s) => s.appearanceDraft);
  if (!draft) return null;
  const glowMode = draft.glowMode;
  return (
    <div className="mt-3">
      <SkinPreview slot="chrome" />
      <label className="mt-4 block text-sm">
        Opacity {boundedPercent(draft.houseOpacity)} — 0 is clear, 100 is solid
        <input type="range" min={0} max={100} value={boundedPercent(draft.houseOpacity)} className="mt-2 w-full" onChange={(e) => useAuric.getState().setDraftOpacity(Number(e.target.value))} />
      </label>
      <label className="mt-3 block text-sm">
        Glow {boundedPercent(draft.glowIntensity)}
        <input type="range" min={0} max={100} value={boundedPercent(draft.glowIntensity)} className="mt-2 w-full" onChange={(e) => useAuric.getState().setDraftGlow(Number(e.target.value))} />
      </label>
      <label className="mt-3 flex min-h-11 items-center justify-between gap-3 text-sm">
        Glow motes
        <input type="checkbox" checked={glowMode} onChange={(e) => useAuric.getState().setDraftOptions({ glowMode: e.target.checked })} />
      </label>
    </div>
  );
}
