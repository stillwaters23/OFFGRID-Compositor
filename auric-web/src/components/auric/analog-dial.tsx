import { hmsFromMs } from "@/lib/format";

type AnalogDialProps = {
  ms: number;
  running?: boolean;
};

export function AnalogDial({ ms, running }: AnalogDialProps) {
  const { h, m, s } = hmsFromMs(ms);
  const sec = s + (ms % 1000) / 1000;
  const minute = m + sec / 60;
  const hour = (h % 12) + minute / 60;
  const sAngle = sec * 6;
  const mAngle = minute * 6;
  const hAngle = hour * 30;

  return (
    <div className="relative mx-auto aspect-square w-52 tx-analog">
      <div
        className={`absolute inset-0 rounded-full bezel bg-chrome/80 ${running ? "halo" : ""}`}
        data-slot="accent"
      />
      <svg viewBox="0 0 200 200" className="relative z-10 h-full w-full">
        <circle cx="100" cy="100" r="92" fill="none" stroke="var(--accent)" strokeWidth="1.4" opacity="0.7" />
        <circle cx="100" cy="100" r="84" fill="none" stroke="var(--accent)" strokeWidth="0.6" opacity="0.35" />
        {Array.from({ length: 60 }, (_, i) => {
          const a = (i * 6 * Math.PI) / 180;
          const inner = i % 5 === 0 ? 70 : 76;
          const outer = 82;
          return (
            <line
              key={i}
              x1={100 + Math.sin(a) * inner}
              y1={100 - Math.cos(a) * inner}
              x2={100 + Math.sin(a) * outer}
              y2={100 - Math.cos(a) * outer}
              stroke="var(--digit)"
              strokeWidth={i % 5 === 0 ? 1.6 : 0.5}
              opacity={i % 5 === 0 ? 0.9 : 0.4}
            />
          );
        })}
        <text
          x="100"
          y="62"
          textAnchor="middle"
          fill="var(--accent)"
          fontFamily="Cinzel, serif"
          fontSize="11"
          letterSpacing="3"
        >
          AURIC
        </text>
        <text
          x="100"
          y="148"
          textAnchor="middle"
          fill="var(--muted)"
          fontFamily="Outfit, sans-serif"
          fontSize="8"
          letterSpacing="2"
        >
          TIME CROWNED
        </text>
        <line
          x1="100"
          y1="100"
          x2="100"
          y2="48"
          stroke="var(--digit)"
          strokeWidth="3.2"
          strokeLinecap="round"
          transform={`rotate(${hAngle} 100 100)`}
        />
        <line
          x1="100"
          y1="100"
          x2="100"
          y2="38"
          stroke="var(--accent)"
          strokeWidth="2"
          strokeLinecap="round"
          transform={`rotate(${mAngle} 100 100)`}
        />
        <line
          x1="100"
          y1="108"
          x2="100"
          y2="30"
          stroke="var(--danger)"
          strokeWidth="1"
          strokeLinecap="round"
          transform={`rotate(${sAngle} 100 100)`}
        />
        <circle cx="100" cy="100" r="4.2" fill="var(--accent)" />
        <circle cx="100" cy="100" r="1.6" fill="var(--felt)" />
      </svg>
    </div>
  );
}
