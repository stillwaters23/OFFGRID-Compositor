import { useState, type CSSProperties } from "react";
import { createFileRoute } from "@tanstack/react-router";
import { SpanReels } from "@/components/auric/span-reels";
import { LASER_PRESETS, laserVariables } from "@/lib/laser";
import type { Span } from "@/lib/format";

export const Route = createFileRoute("/laser-demo")({ component: LaserDemo });

const SAMPLE: Span = {
  years: 77,
  months: 11,
  weeks: 2,
  days: 3,
  hours: 3,
  minutes: 2,
  seconds: 11,
  cs: 0,
};
const INTENSITIES = [0, 50, 100] as const;

const MOODS = {
  dark: {
    felt: "#05070e",
    chrome: "#0c1526",
    digit: "#eaf3ff",
    accent: "#ff465b",
    glow: "#f0243e",
  },
  light: {
    felt: "#f4f2ed",
    chrome: "#e3ded5",
    digit: "#161816",
    accent: "#a62632",
    glow: "#b91c1c",
  },
} as const;

function LaserDemo() {
  const [scale, setScale] = useState<(typeof INTENSITIES)[number]>(50);
  const [mood, setMood] = useState<keyof typeof MOODS>("dark");
  const palette = MOODS[mood];
  const scaleValue = 0.85 + (scale / 100) * 0.45;

  if (!import.meta.env.DEV) return null;

  return (
    <main
      className="tx-root l1-laser-demo min-h-dvh p-4"
      data-mood={mood}
      data-l1-scale={scale}
      style={
        {
          "--felt": palette.felt,
          "--chrome": palette.chrome,
          "--digit": palette.digit,
          "--accent": palette.accent,
          "--glow": palette.glow,
          background: palette.felt,
          color: palette.digit,
        } as CSSProperties
      }
    >
      <header className="mx-auto mb-4 w-full max-w-6xl">
        <h1 className="font-display text-2xl">Laser Strip Lab</h1>
        <p className="mt-2 text-sm text-muted">
          Real span reels at every laser color and intensity. Choose the wheel scale and mood to
          inspect the full set.
        </p>
        <div className="mt-4 flex flex-wrap gap-2" aria-label="Digit scale">
          {INTENSITIES.map((item) => (
            <button
              key={item}
              type="button"
              className="min-h-11 rounded-lg border border-line px-4"
              aria-pressed={scale === item}
              onClick={() => setScale(item)}
            >
              Digit scale {item}
            </button>
          ))}
        </div>
        <div className="mt-2 flex flex-wrap gap-2" aria-label="Mood">
          {(Object.keys(MOODS) as (keyof typeof MOODS)[]).map((item) => (
            <button
              key={item}
              type="button"
              className="min-h-11 rounded-lg border border-line px-4 capitalize"
              aria-pressed={mood === item}
              onClick={() => setMood(item)}
            >
              {item} mood
            </button>
          ))}
        </div>
      </header>
      <section
        className="l1-demo-grid mx-auto grid w-full max-w-6xl gap-4"
        aria-label="Laser color and intensity samples"
      >
        {LASER_PRESETS.flatMap((preset) =>
          INTENSITIES.map((intensity) => (
            <LaserSample
              key={`${preset.id}-${intensity}`}
              preset={preset}
              intensity={intensity}
              scaleValue={scaleValue}
            />
          )),
        )}
      </section>
    </main>
  );
}

type LaserSampleProps = {
  preset: (typeof LASER_PRESETS)[number];
  intensity: (typeof INTENSITIES)[number];
  scaleValue: number;
};

function LaserSample({ preset, intensity, scaleValue }: LaserSampleProps) {
  const [value, setValue] = useState(SAMPLE);
  const [ticks, setTicks] = useState(0);
  return (
    <article
      className="l1-demo-sample min-w-0 rounded-2xl border border-line p-4"
      data-laser-color={preset.id}
      data-laser-intensity={intensity}
      style={
        {
          ...laserVariables(preset.color, intensity),
          "--digit-scale": String(scaleValue),
        } as CSSProperties
      }
    >
      <h2 className="mb-2 text-sm font-semibold">
        {preset.label} · {intensity}%
      </h2>
      <output className="sr-only" data-l1-ticks>
        {ticks}
      </output>
      <SpanReels
        value={value}
        onChange={(key, n) => setValue((current) => ({ ...current, [key]: n }))}
        onTick={() => setTicks((count) => count + 1)}
        readout="03:02:11"
        className="l1-demo-reels"
      />
    </article>
  );
}
