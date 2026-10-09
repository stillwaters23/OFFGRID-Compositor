import { useEffect, useMemo, useState } from "react";
import { Check, Copy, Crown } from "lucide-react";
import { DrumReel } from "@/components/auric/drum-reel";
import { PitButton } from "@/components/auric/pit-button";
import { CURATED, ROYGBIV, SLOT_HINTS, SLOT_LABELS } from "@/lib/palettes";
import { SKINS } from "@/lib/skins";
import { FLOORS, readFloorFile } from "@/lib/floors";
import { copy } from "@/lib/literacy";
import { harmonyHues, hexToHsv, hsvToHex, isValidHex, normalizeHex, type HSV } from "@/lib/color";
import { playLockIn, playReelTick } from "@/lib/audio";
import { hapticTick } from "@/lib/haptics";
import { formatHms, msFromHms } from "@/lib/format";
import { remainingMs, useAuric } from "@/lib/store";
import { committedPalette, type AppearanceMode } from "@/lib/appearance";
import type { Palette, PaintSlot } from "@/lib/types";
import { cn } from "@/lib/utils";

const SLOTS: PaintSlot[] = ["felt", "chrome", "digit", "accent", "danger", "glow"];
const HUE_LABELS = Array.from({ length: 36 }, (_, i) => String(i * 10).padStart(3, "0"));
const SAT_LABELS = Array.from({ length: 21 }, (_, i) => String(i * 5).padStart(2, "0"));

export function SpectrumScroll() {
  return <CutRoom />;
}

function ThemeGrid({ racks }: { racks: Palette[] }) {
  const isCrowned = useAuric((s) => s.isCrowned);
  return (
    <div className="mt-2 grid w-full min-w-0 grid-cols-2 gap-2">
      {racks.map((rack) => {
        const locked = rack.premium && !isCrowned;
        return (
          <button
            key={rack.id}
            type="button"
            onClick={() => {
              if (locked) {
                useAuric.getState().setCrownSheet(true);
                return;
              }
              useAuric.getState().setPaletteId(rack.id);
              playLockIn();
            }}
            className="ice-card min-w-0 rounded-lg px-3 py-2.5 text-left transition-transform duration-150 active:scale-[0.98]"
          >
            <span className="flex min-w-0 items-center justify-between gap-2">
            <span className="min-w-0 text-sm text-digit">{rack.name}</span>
              {rack.premium ? <Crown className="size-3.5 shrink-0 text-accent" /> : null}
            </span>
            <span className="mt-2 flex -space-x-1">
              {(["felt", "accent", "digit", "glow"] as const).map((k) => (
                <span
                  key={k}
                  className="size-4 rounded-full"
                  style={{ background: rack[k], boxShadow: "0 0 0 1px color-mix(in oklab, var(--digit) 20%, transparent)" }}
                />
              ))}
            </span>
          </button>
        );
      })}
    </div>
  );
}

export function CutRoom() {
  const slot = useAuric((s) => s.spectrumSlot);
  const paletteId = useAuric((s) => s.paletteId);
  const customPalette = useAuric((s) => s.customPalette);
  const appearanceMode = useAuric((s) => s.appearanceMode ?? "system") as AppearanceMode;
  const scheduleEnabled = useAuric((s) => s.scheduleEnabled ?? false);
  const lightStart = useAuric((s) => s.scheduleLight ?? "07:00");
  const darkStart = useAuric((s) => s.scheduleDark ?? "19:00");
  const zone = useAuric((s) => (s.timeZoneLocked ? s.timeZoneId : null));
  const schemeMinute = useAuric((s) => (s.scheduleEnabled ? Math.floor((s.now || 0) / 60_000) : 0));
  const palette = useMemo(
    () =>
      committedPalette({
        paletteId,
        customPalette,
        mode: appearanceMode,
        scheduleEnabled,
        lightStart,
        darkStart,
        nowMs: schemeMinute > 0 ? schemeMinute * 60_000 : 1,
        zone,
      }),
    [paletteId, customPalette, appearanceMode, scheduleEnabled, lightStart, darkStart, schemeMinute, zone],
  );
  const sound = useAuric((s) => s.reelSound);
  const literacy = useAuric((s) => s.literacyIndex);
  const words = copy(literacy);
  const hex = palette[slot];
  const hsv0 = hexToHsv(hex) ?? { h: 196, s: 40, v: 90, a: 100 };
  const [draft, setDraft] = useState<HSV>(hsv0);
  const [hexText, setHexText] = useState(hex);
  const [copied, setCopied] = useState(false);
  const [more, setMore] = useState(false);
  const hours = useAuric((s) => s.hours);
  const minutes = useAuric((s) => s.minutes);
  const seconds = useAuric((s) => s.seconds);
  const now = useAuric((s) => s.now);
  const preview = formatHms(remainingMs(useAuric.getState(), now) || msFromHms(hours, minutes, seconds));

  useEffect(() => {
    const next = hexToHsv(palette[slot]) ?? { h: 0, s: 0, v: 0, a: 100 };
    setDraft(next);
    setHexText(palette[slot]);
  }, [slot, palette]);

  function applyHsv(next: HSV) {
    setDraft(next);
    const h = hsvToHex(next);
    setHexText(h);
    useAuric.getState().setSlotColor(slot, h);
  }

  function applyHex(raw: string) {
    setHexText(raw);
    if (isValidHex(raw)) {
      const n = normalizeHex(raw);
      if (n) {
        useAuric.getState().setSlotColor(slot, n);
        const hsv = hexToHsv(n);
        if (hsv) setDraft(hsv);
      }
    }
  }

  function tick() {
    hapticTick();
    playReelTick(sound);
  }

  return (
    <div className="flex min-h-0 w-full max-w-full flex-1 flex-col overflow-y-auto px-5 pb-8">
      <p className="mt-1 text-center font-digits text-3xl digit-cut" data-slot="digit">
        {preview}
      </p>
      <p className="mt-1 text-center text-sm text-muted">
        {words.easy ? "The timer stays visible. You cannot paint it away." : "Ice Floor — unreadable cuts are refused."}
      </p>

      <p className="mt-5 font-display text-sm tracking-[0.18em] uppercase text-accent">Floor</p>
      <p className="mt-1 text-sm text-muted">Photo under the glass. Skin still paints the chrome.</p>
      <FloorPicker />

      <p className="mt-5 font-display text-sm tracking-[0.18em] uppercase text-accent">Themes</p>
      <ThemeGrid racks={CURATED} />
      <button
        type="button"
        className="mt-2 min-h-11 w-full rounded-lg text-sm text-accent shadow-[0_0_0_1px_var(--line)]"
        onClick={() => setMore((v) => !v)}
      >
        {more ? "Hide more" : "More"}
      </button>
      {more ? <ThemeGrid racks={SKINS} /> : null}

      <p className="mt-5 font-display text-sm tracking-[0.18em] uppercase text-accent">What to color</p>
      <div className="mt-2 flex flex-wrap gap-1.5">
        {SLOTS.map((s) => (
          <button
            key={s}
            type="button"
            onClick={() => useAuric.getState().setSpectrumSlot(s)}
            className={cn(
              "min-h-11 min-w-0 rounded-full px-3 text-xs tracking-wide",
              slot === s ? "bg-accent text-on-accent" : "shadow-[0_0_0_1px_var(--line)]",
            )}
          >
            <span className="mr-1.5 inline-block size-2 rounded-full" style={{ background: palette[s] }} />
            {SLOT_LABELS[s]}
          </button>
        ))}
      </div>
      <p className="mt-2 text-xs text-muted">{SLOT_HINTS[slot]}</p>

      <p className="mt-5 font-display text-sm tracking-[0.18em] uppercase text-muted">Quick colors</p>
      <div className="mt-2 flex flex-wrap gap-1.5">
        {ROYGBIV.map((c) => (
          <button
            key={c.name}
            type="button"
            aria-label={c.name}
            title={c.name}
            onClick={() => applyHex(c.hex)}
            className="grid size-9 place-items-center rounded-full"
            style={{ background: c.hex, boxShadow: "0 0 0 1px color-mix(in oklab, var(--digit) 35%, transparent)" }}
          />
        ))}
      </div>

      <div className="mt-3 flex min-w-0 items-center gap-2">
        <span
          className="size-11 shrink-0 rounded-md"
          style={{ background: hex, boxShadow: "0 0 0 1px var(--line)" }}
          aria-hidden
        />
        <input
          value={hexText}
          onChange={(e) => applyHex(e.target.value)}
          placeholder="#FFF or #FFFFFF"
          className="h-11 min-w-0 flex-1 rounded-md bg-transparent px-3 font-mono text-sm uppercase outline-none shadow-[0_0_0_1px_var(--line)]"
        />
        <PitButton
          variant="ghost"
          className="h-11 w-11 shrink-0 px-0"
          aria-label="Copy hex"
          onClick={async () => {
            try {
              await navigator.clipboard.writeText(hexText);
              setCopied(true);
              setTimeout(() => setCopied(false), 1200);
            } catch {
              /* ignore */
            }
          }}
        >
          {copied ? <Check className="size-4" /> : <Copy className="size-4" />}
        </PitButton>
      </div>

      <p className="mt-5 font-display text-sm tracking-[0.18em] uppercase text-muted">Hue · Sat · Val · Alpha</p>
      <div className="mt-2 flex min-w-0 justify-center gap-2 overflow-x-auto">
        <DrumReel
          size="md"
          count={36}
          value={Math.round(draft.h / 10) % 36}
          labels={HUE_LABELS}
          pad={3}
          label="Hue"
          onChange={(i) => applyHsv({ ...draft, h: i * 10 })}
          onTick={tick}
        />
        <DrumReel
          size="md"
          count={21}
          value={Math.round(draft.s / 5)}
          labels={SAT_LABELS}
          pad={2}
          label="Sat"
          onChange={(i) => applyHsv({ ...draft, s: i * 5 })}
          onTick={tick}
        />
        <DrumReel
          size="md"
          count={21}
          value={Math.round(draft.v / 5)}
          labels={SAT_LABELS}
          pad={2}
          label="Val"
          onChange={(i) => applyHsv({ ...draft, v: i * 5 })}
          onTick={tick}
        />
        <DrumReel
          size="md"
          count={21}
          value={Math.round(draft.a / 5)}
          labels={SAT_LABELS}
          pad={2}
          label="Alpha"
          onChange={(i) => applyHsv({ ...draft, a: i * 5 })}
          onTick={tick}
        />
      </div>
      <p className="mt-4 font-display text-sm tracking-[0.18em] uppercase text-muted">Harmony</p>
      <HarmonyRow hsv={draft} slot={slot} />
    </div>
  );
}

function FloorPicker() {
  const floorId = useAuric((s) => s.floorId);
  const custom = useAuric((s) => s.floorCustom);
  return (
    <div className="mt-2 grid w-full min-w-0 grid-cols-2 gap-2">
      {FLOORS.map((floor) => {
        const on = floorId === floor.id;
        const thumb = floor.id === "custom" ? custom : floor.src;
        return (
          <label
            key={floor.id}
            className={cn(
              "ice-card relative min-w-0 cursor-pointer overflow-hidden rounded-lg text-left",
              on && "shadow-[inset_0_0_0_1px_var(--digit)]",
            )}
          >
            {floor.id === "custom" ? (
              <input
                type="file"
                accept="image/*"
                className="absolute inset-0 z-10 cursor-pointer opacity-0"
                onChange={async (e) => {
                  const file = e.target.files?.[0];
                  e.currentTarget.value = "";
                  if (!file) return;
                  try {
                    const data = await readFloorFile(file);
                    useAuric.getState().setFloorCustom(data);
                    playLockIn();
                  } catch {
                    /* ignore */
                  }
                }}
              />
            ) : (
              <button
                type="button"
                className="absolute inset-0 z-10"
                aria-label={floor.name}
                onClick={() => {
                  useAuric.getState().setFloorId(floor.id);
                  playLockIn();
                }}
              />
            )}
            <span
              className="block h-16 w-full bg-felt"
              style={
                thumb
                  ? { backgroundImage: `url(${thumb})`, backgroundSize: "cover", backgroundPosition: "center" }
                  : undefined
              }
            />
            <span className="block min-w-0 px-3 py-2">
              <span className="block text-sm text-digit">{floor.name}</span>
              <span className="mt-0.5 block text-xs text-muted">{floor.hint}</span>
            </span>
          </label>
        );
      })}
    </div>
  );
}

function HarmonyRow({ hsv, slot }: { hsv: HSV; slot: PaintSlot }) {
  const kinds = [
    { id: "mono" as const, label: "Mono" },
    { id: "analog" as const, label: "Analog" },
    { id: "complement" as const, label: "Complement" },
    { id: "split" as const, label: "Split" },
  ];
  const targets: PaintSlot[] = useMemo(() => SLOTS.filter((s) => s !== slot).slice(0, 3), [slot]);

  return (
    <div className="mt-2 grid grid-cols-4 gap-1.5">
      {kinds.map((k) => {
        const hues = harmonyHues(hsv.h, k.id);
        return (
          <button
            key={k.id}
            type="button"
            className="ice-card min-w-0 rounded-md px-1 py-2"
            onClick={() => {
              const state = useAuric.getState();
              hues.forEach((h, i) => {
                const target = targets[i];
                if (!target) return;
                state.setSlotColor(target, hsvToHex({ ...hsv, h }));
              });
              playLockIn();
            }}
          >
            <span className="flex justify-center -space-x-1">
              {hues.map((h, i) => (
                <span
                  key={`${k.id}-${i}`}
                  className="size-4 rounded-full"
                  style={{ background: hsvToHex({ ...hsv, h }) }}
                />
              ))}
            </span>
            <span className="mt-1 block text-[10px] text-muted">{k.label}</span>
          </button>
        );
      })}
    </div>
  );
}
