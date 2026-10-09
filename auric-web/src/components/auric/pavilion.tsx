import { useEffect, useMemo, useRef, useState, type ReactNode } from "react";
import {
  ArrowLeft,
  Crown,
  Diamond,
  LayoutGrid,
  Lock,
  MonitorSmartphone,
  Shield,
  Sparkles,
  Type,
  Volume2,
  Watch,
} from "lucide-react";
import { DrumReel } from "@/components/auric/drum-reel";
import { OrbToggle } from "@/components/auric/switch";
import { PitButton } from "@/components/auric/pit-button";
import { AppearanceStudio } from "@/components/auric/appearance-studio";
import { ClockRoom } from "@/components/auric/clock-room";
import { DeviceRoom } from "@/components/auric/device-room";
import { LogsExports } from "@/components/auric/logs-exports";
import { InstallPhone } from "@/components/auric/install-phone";
import { RemindView } from "@/components/auric/remind-view";
import { playClick, playDescend, playLockIn, playReelTick, playScrollUnfurl, playSettingsClick, playSurface, previewWake } from "@/lib/audio";
import { FACES, faceById, faceShort, facesForLiteracy } from "@/lib/faces";
import { useFace } from "@/lib/use-face";
import { CONTROLS, controlById } from "@/lib/controls";
import { sha256Hex } from "@/lib/hash";
import { hapticTick } from "@/lib/haptics";
import { copy, roomTitle, roomsFor, type PavilionRoomId } from "@/lib/literacy";
import { isMobileBrowser } from "@/lib/platform";
import { useAuric } from "@/lib/store";
import { HEARTBEAT_CUTS, ICON_CUTS, SETTINGS_CUTS, STOPWATCH_CUTS } from "@/lib/banks";
import { TICK_FAMILIES, TICKS } from "@/lib/ticks";
import { WAKE_FAMILIES, WAKES } from "@/lib/wake";
import { ACHIEVEMENTS, computeStreak } from "@/lib/achievements";
import { cutScore, markColor } from "@/lib/mark";
import type { AppMode } from "@/lib/types";
import { cn } from "@/lib/utils";

const FLOOR_MODES: { id: AppMode; label: string }[] = [
  { id: "timer", label: "Timer" },
  { id: "stopwatch", label: "Stopwatch" },
  { id: "alarm", label: "Alarm" },
  { id: "remind", label: "Reminders" },
];

export function Pavilion() {
  const open = useAuric((s) => s.houseOpen);
  const room = useAuric((s) => s.pavilionRoom);
  const folder = useAuric((s) => s.customizationFolder);
  const literacy = useAuric((s) => s.literacyIndex);
  const words = copy(literacy);

  useEffect(() => {
    if (open) playScrollUnfurl();
  }, [open]);

  if (!open) return null;

  function close() {
    playSurface();
    useAuric.getState().setHouseOpen(false);
  }

  function back() {
    playSurface();
    useAuric.getState().leaveRoom();
  }

  function enter(id: PavilionRoomId) {
    playDescend();
    useAuric.getState().enterRoom(id);
  }

  const title = room ? roomTitle(room, literacy) : null;
  const plankton = Array.from({ length: 14 }, (_, i) => i);

  return (
    <div
      className="relative flex min-h-0 flex-1 flex-col overflow-hidden dragon-scroll"
      style={{
        background: "color-mix(in oklab, var(--felt) 98%, var(--chrome))",
        boxShadow: "inset 0 1px 0 color-mix(in oklab, var(--digit) 16%, transparent)",
        backdropFilter: "blur(28px) saturate(1.25)",
        WebkitBackdropFilter: "blur(28px) saturate(1.25)",
      }}
    >
      <div className="pointer-events-none absolute inset-0 overflow-hidden">
        {plankton.map((i) => (
          <span
            key={i}
            className="plankton"
            style={{
              left: `${(i * 13 + 7) % 100}%`,
              bottom: `${(i * 9) % 40}px`,
              width: 3 + (i % 3),
              height: 3 + (i % 3),
              animationDelay: `${(i * 0.28) % 4}s`,
              opacity: 0.45,
            }}
          />
        ))}
      </div>
      <div className="relative mx-5 mt-2 scroll-rod" />
      <div className="relative flex min-w-0 items-start gap-2 px-5 pb-2 pt-2">
        <button
          type="button"
          className="flex min-h-12 shrink-0 items-center gap-1 text-accent"
          onClick={() => {
            playSettingsClick();
            if (room === "cut" && folder) useAuric.getState().setCustomizationFolder(null);
            else if (room) back();
            else close();
          }}
          aria-label={room === "cut" && folder ? "Back to Customizations" : room ? "Back to House" : "Back to floor"}
        >
          <ArrowLeft className="size-6" />
          <span className="font-display text-sm tracking-wide">Back</span>
        </button>
        <div className="min-w-0 flex-1 pt-2">
          <p className="font-display text-xl leading-tight tracking-wide text-accent">
            {title ? (
              <>
                <span className="mr-2 text-muted">{title.num}</span>
                {title.name}
              </>
            ) : (
              words.house
            )}
          </p>
          <p className="mt-0.5 text-sm leading-snug text-muted">{title ? title.hint : "Spin a door. Same drums as the timer."}</p>
        </div>
        <CuletMark />
      </div>

      {room === "floor" ? (
        <FloorRoom />
      ) : room === "cut" ? (
        <AppearanceStudio />
      ) : room === "clock" ? (
        <ClockRoom />
      ) : room === "table" ? (
        <TableRoom />
      ) : room === "tick" ? (
        <TickRoom />
      ) : room === "device" ? (
        <DeviceRoom />
      ) : room === "sessions" ? (
        <LogsExports />
      ) : room === "mark" ? (
        <MarkRoom />
      ) : room === "heartbeat" ? (
        <RemindView />
      ) : room === "girdle" ? (
        <GirdleRoom />
      ) : (
        <Directory onEnter={enter} />
      )}
    </div>
  );
}

function CuletMark() {
  const taps = useRef(0);
  const found = useAuric((s) => s.culetFound);
  return (
    <button
      type="button"
      className={cn("grid size-11 place-items-center", found && "text-accent")}
      aria-label="Pavilion mark"
      onClick={() => {
        taps.current += 1;
        playClick();
        if (taps.current >= 3) {
          taps.current = 0;
          useAuric.getState().discoverCulet();
          playLockIn();
        }
      }}
    >
      <Diamond className="size-4 text-accent" />
    </button>
  );
}

function Directory({ onEnter }: { onEnter: (id: PavilionRoomId) => void }) {
  const literacy = useAuric((s) => s.literacyIndex);
  const ids = roomsFor(literacy);
  const culet = useAuric((s) => s.culetFound);
  const directoryMode = useAuric((s) => s.directoryMode);
  const [pick, setPick] = useState(0);
  const current = ids[pick] ?? ids[0];
  const meta = current ? roomTitle(current, literacy) : null;

  if (directoryMode === "buttons") {
    return (
      <div className="relative flex min-h-0 w-full max-w-full flex-1 flex-col overflow-y-auto px-5 pb-8">
        <p className="text-center text-sm text-muted">Open a room. Customizations stay a draft until you Apply.</p>
        <div className="mt-4 grid gap-2">
          {ids.map((id) => {
            const item = roomTitle(id, literacy);
            return (
              <button
                key={id}
                type="button"
                className="flex min-h-14 items-center gap-3 rounded-2xl px-4 text-left panel-glass"
                onClick={() => onEnter(id)}
              >
                <span className="font-digits text-sm text-muted">{item.num}</span>
                <span>
                  <span className="block font-display text-sm">{item.name}</span>
                  <span className="block text-xs text-muted">{item.hint}</span>
                </span>
              </button>
            );
          })}
        </div>
        {culet ? (
          <p className="mt-3 text-center text-[11px] tracking-wide text-accent">Culet open — Ice Absolute is in The Tick.</p>
        ) : (
          <p className="mt-3 text-center text-[11px] text-muted">The mark above is the tip of the diamond.</p>
        )}
      </div>
    );
  }

  return (
    <div className="relative flex min-h-0 w-full max-w-full flex-1 flex-col overflow-y-auto px-5 pb-8">
      <p className="text-center text-sm text-muted">Flick like Hours / Minutes / Seconds. Then open.</p>
      <div className="mt-4 flex w-full justify-center">
        <DrumReel
          size="lg"
          count={ids.length}
          value={pick}
          labels={ids.map((id) => roomTitle(id, literacy).name)}
          pad={0}
          fontClass="font-display"
          onChange={setPick}
          onTick={() => {
            hapticTick();
            playClick();
          }}
          label="Settings"
        />
      </div>
      <p className="mt-4 text-center font-display text-2xl text-accent">{meta?.name}</p>
      <p className="mt-1 text-center text-xs text-muted">
        {meta?.num} · {meta?.hint}
      </p>
      <button
        type="button"
        className="mt-4 min-h-12 w-full rounded-lg bg-accent text-on-accent"
        onClick={() => {
          if (current) onEnter(current);
        }}
      >
        Open
      </button>
      {culet ? (
        <p className="mt-3 text-center text-[11px] tracking-wide text-accent">Culet open — Ice Absolute is in The Tick.</p>
      ) : (
        <p className="mt-3 text-center text-[11px] text-muted">The mark above is the tip of the diamond.</p>
      )}
    </div>
  );
}

function FloorRoom() {
  const mode = useAuric((s) => s.mode);
  const idx = Math.max(0, FLOOR_MODES.findIndex((m) => m.id === mode));
  return (
    <div className="relative flex min-h-0 flex-1 flex-col overflow-y-auto px-4 pb-10">
      <p className="text-center text-sm text-muted">
        This is the floor. Pick what opens first. Everything else lives in the House.
      </p>
      <div className="mt-5 flex justify-center">
        <DrumReel
          size="plaque"
          count={FLOOR_MODES.length}
          value={idx < 0 ? 0 : idx}
          labels={FLOOR_MODES.map((m) => m.label)}
          pad={0}
          onChange={(i) => {
            const next = FLOOR_MODES[i];
            if (next) useAuric.getState().setMode(next.id);
          }}
          onTick={() => {
            hapticTick();
            playClick();
          }}
        />
      </div>
      <p className="mt-5 text-center font-display text-2xl text-accent">{FLOOR_MODES[idx]?.label ?? "Timer"}</p>
    </div>
  );
}

function MarkRoom() {
  const sessions = useAuric((s) => s.sessions);
  const unlocked = useAuric((s) => s.unlocked);
  const insurance = useAuric((s) => s.streakInsuranceUsed);
  const { streak } = computeStreak(sessions, !insurance);
  const score = cutScore(sessions, streak);
  const color = markColor(score);
  return (
    <div className="relative flex min-h-0 flex-1 flex-col overflow-y-auto px-4 pb-10">
      <div className="mt-4 flex flex-col items-center">
        <div
          className="relative grid size-36 place-items-center rounded-full"
          style={{
            boxShadow: `0 0 0 3px ${color}, 0 0 28px ${color}`,
            background: `conic-gradient(${color} ${score * 3.6}deg, color-mix(in oklab, var(--chrome) 80%, transparent) 0)`,
          }}
        >
          <div className="grid size-[7.2rem] place-items-center rounded-full bg-felt">
            <p className="font-digits text-4xl digit-cut" style={{ color }}>
              {score}
            </p>
          </div>
        </div>
        <p className="mt-3 text-xs text-muted">0 is raw. 100 is white light. Not a scold — a cut.</p>
      </div>
      <p className="mt-6 font-display text-[10px] tracking-[0.32em] uppercase text-accent">Achievements</p>
      <div className="mt-2 grid grid-cols-2 gap-2">
        {ACHIEVEMENTS.map((a) => {
          const on = unlocked.includes(a.id);
          return (
            <div
              key={a.id}
              className={cn("rounded-lg px-3 py-3 shadow-[0_0_0_1px_var(--line)]", on && "bg-accent/10")}
            >
              <p className="font-display text-xs tracking-wide" style={{ color: on ? color : undefined }}>
                {a.title}
              </p>
              <p className="mt-1 text-[11px] text-muted">{a.blurb}</p>
            </div>
          );
        })}
      </div>
    </div>
  );
}

function TickRoom() {
  const reelSound = useAuric((s) => s.reelSound);
  const alarmSound = useAuric((s) => s.alarmSound);
  const stopwatchSound = useAuric((s) => s.stopwatchSound);
  const iconSound = useAuric((s) => s.iconSound);
  const settingsSound = useAuric((s) => s.settingsSound);
  const heartbeatSound = useAuric((s) => s.heartbeatSound);

  return (
    <div className="relative flex min-h-0 w-full max-w-full flex-1 flex-col overflow-y-auto px-5 pb-10">
      <SoundDeck
        title="The Wheel"
        hint="77 clicks in this style. House Click 01 is first."
        families={[...TICK_FAMILIES]}
        cuts={TICKS}
        value={reelSound}
        onPick={(id) => {
          useAuric.getState().setReelSound(id);
          playReelTick(id);
        }}
      />
      <SoundDeck
        title="Alarm clock"
        hint="77 wakes in this style."
        families={[...WAKE_FAMILIES]}
        cuts={WAKES}
        value={alarmSound}
        onPick={(id) => {
          useAuric.getState().setAlarmSound(id);
          previewWake(id);
        }}
      />
      <SoundDeck
        title="Stopwatch"
        hint="77 cuts for start, hold, lap, and stop."
        families={[...TICK_FAMILIES]}
        cuts={STOPWATCH_CUTS}
        value={stopwatchSound}
        onPick={(id) => {
          useAuric.getState().setStopwatchSound(id);
          playReelTick(id);
        }}
      />
      <SoundDeck
        title="Icons"
        hint="77 taps for buttons and tabs."
        families={[...TICK_FAMILIES]}
        cuts={ICON_CUTS}
        value={iconSound}
        onPick={(id) => {
          useAuric.getState().setIconSound(id);
          playReelTick(id);
        }}
      />
      <SoundDeck
        title="Settings"
        hint="77 taps for the gear and Back."
        families={[...TICK_FAMILIES]}
        cuts={SETTINGS_CUTS}
        value={settingsSound}
        onPick={(id) => {
          useAuric.getState().setSettingsSound(id);
          playReelTick(id);
        }}
      />
      <SoundDeck
        title="Heartbeat"
        hint="77 soft pulses. Ice 01 is first."
        families={[...TICK_FAMILIES]}
        cuts={HEARTBEAT_CUTS}
        value={heartbeatSound}
        onPick={(id) => {
          useAuric.getState().setHeartbeatSound(id);
          playReelTick(id);
        }}
      />
    </div>
  );
}

function SoundDeck({
  title,
  hint,
  families,
  cuts,
  value,
  onPick,
}: {
  title: string;
  hint: string;
  families: string[];
  cuts: { id: string; name: string; short: string; family: string; premium: boolean }[];
  value: string;
  onPick: (id: string) => void;
}) {
  const [family, setFamily] = useState<string>(families[0] ?? "Click");
  const list = useMemo(() => cuts.filter((t) => t.family === family), [family, cuts]);
  let idx = list.findIndex((t) => t.id === value);
  if (idx < 0) idx = 0;
  const current = cuts.find((c) => c.id === value) ?? list[idx];

  return (
    <div className="mt-6 first:mt-1">
      <p className="chrome-type text-center font-display text-lg">{title}</p>
      <p className="mt-1 text-center text-sm text-muted">{hint}</p>
      <div className="mt-2 flex flex-wrap justify-center gap-1.5">
        {families.map((f) => (
          <Chip key={f} active={family === f} onClick={() => setFamily(f)}>
            {f}
          </Chip>
        ))}
      </div>
      {list.length ? (
        <div className="mt-4 flex w-full justify-center overflow-x-hidden">
          <DrumReel
            size="plaque"
            count={list.length}
            value={Math.max(0, idx)}
            labels={list.map((t) => t.short)}
            pad={0}
            onChange={(i) => {
              const next = list[i];
              if (next) onPick(next.id);
            }}
            onTick={() => hapticTick()}
          />
        </div>
      ) : (
        <p className="mt-4 text-center text-sm text-muted">No cuts in that family.</p>
      )}
      <p className="mt-4 text-center font-display text-xl text-accent">{list[idx]?.name ?? current?.name}</p>
      <p className="mt-1 text-center text-xs text-muted">{list[idx]?.premium ? "Crown cut" : "House cut"}</p>
    </div>
  );
}

function TableRoom() {
  const font = useAuric((s) => s.digitFont);
  const [candidate, setCandidate] = useState(font);
  const preview = useFace(candidate);
  const sound = useAuric((s) => s.reelSound);
  const literacy = useAuric((s) => s.literacyIndex);
  const glow = useAuric((s) => s.glowIntensity);
  const glowMode = useAuric((s) => s.glowMode);
  const analog = useAuric((s) => s.analogFace);
  const [q, setQ] = useState("");
  const [az, setAz] = useState(false);

  const list = useMemo(() => {
    let rows = facesForLiteracy(literacy);
    if (q.trim()) {
      const n = q.trim().toLowerCase();
      rows = rows.filter((f) => f.name.toLowerCase().includes(n) || f.id.includes(n));
    }
    if (az) rows = [...rows].sort((a, b) => a.name.localeCompare(b.name));
    return rows;
  }, [q, az, literacy]);

  const current = faceById(font);
  let idx = list.findIndex((f) => f.id === candidate);
  if (idx < 0) idx = 0;
  const displayed = list[idx] ?? preview.face;
  const displayingCandidate = displayed.id === preview.face.id;

  return (
    <div className="relative flex min-h-0 flex-1 flex-col overflow-y-auto px-4 pb-10">
      <p className="face-preview text-center text-4xl digit-cut" style={{
        fontFamily: preview.status === "loaded" ? `"${preview.face.family}", ${preview.face.fallback}` : preview.face.fallback,
        fontWeight: preview.face.weight,
      }}>00:25:00</p>
      <p className="mt-2 text-center text-xs text-muted">Applied: {current.name}</p>
      <p className="mt-1 text-center text-xs text-muted">
        {list.length} of {FACES.length} faces. Pit Board is house.
      </p>
      <input
        value={q}
        onChange={(e) => setQ(e.target.value)}
        placeholder="Search faces"
        aria-label="Search faces"
        className="mt-3 h-11 rounded-md bg-felt/70 px-3 text-sm outline-none shadow-[0_0_0_1px_var(--line)]"
      />
      <div className="mt-2 flex gap-1.5">
        <Chip active={az} onClick={() => setAz((v) => !v)}>
          A–Z
        </Chip>
      </div>
      {list.length ? (
        <div className="mt-4 flex justify-center py-4">
          <DrumReel
            size="plaque"
            count={list.length}
            value={Math.max(0, idx)}
            labels={list.map((f) => faceShort(f))}
            label="Browse number faces"
            fontClass="font-sans"
            pad={0}
            onChange={(i) => {
              const next = list[i];
              if (!next) return;
              setCandidate(next.id);
              playClick();
            }}
            onTick={() => {
              hapticTick();
              playReelTick(sound);
            }}
          />
        </div>
      ) : null}
      <p className="mt-3 text-center font-display text-xl text-accent">{preview.face.name}</p>
      {!displayingCandidate && list.length > 0 ? (
        <button type="button" className="mt-2 min-h-11 text-accent" onClick={() => setCandidate(displayed.id)}>
          Preview {displayed.name}
        </button>
      ) : null}
      <p className="mt-2 text-center text-sm text-muted" role="status">
        {preview.status === "loaded" ? "Preview ready" : preview.status === "error" ? "Font unavailable. Preview uses a system fallback." : "Loading font…"}
      </p>
      {preview.status === "error" ? (
        <button type="button" className="mt-2 min-h-11 text-accent" onClick={() => void preview.retry()}>Retry font</button>
      ) : (
        <PitButton className="mt-3 w-full" disabled={preview.status !== "loaded" || font === candidate}
          onClick={() => { useAuric.getState().setDigitFont(candidate); playClick(); }}>
          {font === candidate ? "Applied" : `Apply ${preview.face.name}`}
        </PitButton>
      )}
      <p className="mt-6 font-display text-[10px] tracking-[0.32em] uppercase text-accent">Control skin</p>
      <ControlWheel />
      <Row icon={Sparkles} label="Glow intensity" hint={`${glow}%`}>
        <input
          type="range"
          min={0}
          max={100}
          value={glow}
          onChange={(e) => useAuric.getState().setGlowIntensity(Number(e.target.value))}
          className="w-full accent-[var(--accent)]"
        />
      </Row>
      <ToggleRow
        icon={Sparkles}
        label="Glow Mode"
        hint="Floating ice motes. Crown."
        on={glowMode}
        onChange={(v) => useAuric.getState().setGlowMode(v)}
      />
      <ToggleRow
        icon={Watch}
        label="Analog skin"
        hint="Original crowned dial — not a licensed face."
        on={analog}
        onChange={(v) => useAuric.getState().setAnalogFace(v)}
      />
    </div>
  );
}

function ControlWheel() {
  const id = useAuric((s) => s.controlId);
  const current = controlById(id);
  const idx = Math.max(0, CONTROLS.findIndex((c) => c.id === current.id));
  return (
    <div className="mt-3 flex flex-col items-center">
      <DrumReel
        size="plaque"
        count={CONTROLS.length}
        value={idx}
        labels={CONTROLS.map((c) => c.short)}
        pad={0}
        onChange={(i) => {
          const next = CONTROLS[i];
          if (!next) return;
          useAuric.getState().setControlId(next.id);
          playClick();
        }}
        onTick={() => hapticTick()}
      />
      <p className="mt-3 text-sm text-accent">{current.name}</p>
      <p className="text-[11px] text-muted">{CONTROLS.length} named cuts · 7 engines</p>
    </div>
  );
}

function Chip({ active, onClick, children }: { active: boolean; onClick: () => void; children: ReactNode }) {
  return (
    <button
      type="button"
      onClick={onClick}
      className={cn(
        "min-h-11 shrink-0 rounded-full px-3 text-xs tracking-wide",
        active ? "bg-accent text-on-accent" : "shadow-[0_0_0_1px_var(--line)]",
      )}
    >
      {children}
    </button>
  );
}

function GirdleRoom() {
  const controlLock = useAuric((s) => s.controlLock);
  const pinHash = useAuric((s) => s.pinHash);
  const floating = useAuric((s) => s.floatingVisible);
  const isCrowned = useAuric((s) => s.isCrowned);
  const scale = useAuric((s) => s.digitScale);
  const glass = useAuric((s) => s.houseOpacity);
  const rexOn = useAuric((s) => s.rexOn);
  const callName = useAuric((s) => s.callName);
  const houseFace = useAuric((s) => s.houseFace);
  const mobile = typeof window !== "undefined" ? isMobileBrowser() : true;
  const [pin, setPin] = useState("");
  const [pinMsg, setPinMsg] = useState("");

  return (
    <div className="relative flex min-h-0 w-full max-w-full flex-1 flex-col overflow-y-auto px-5 pb-10">
      <Row icon={LayoutGrid} label="House face" hint="Same rooms. Different chrome.">
        <div className="flex flex-wrap gap-1.5">
          {(
            [
              ["pavilion", "Pavilion"],
              ["pit", "Pit"],
              ["shadow", "Shadow"],
            ] as const
          ).map(([id, name]) => (
            <Chip key={id} active={houseFace === id} onClick={() => useAuric.getState().setHouseFace(id)}>
              {name}
            </Chip>
          ))}
        </div>
      </Row>
      <ToggleRow
        icon={Volume2}
        label="Rex (voice) · Preview"
        hint="Phone voice only. We never ask for an API key."
        on={rexOn}
        onChange={(v) => useAuric.getState().setRexOn(v)}
      />
      <Row icon={Type} label="What Rex calls you" hint="Blank becomes Auric.">
        <input
          value={callName}
          onChange={(e) => useAuric.getState().setCallName(e.target.value)}
          placeholder="Your name"
          className="h-11 w-full min-w-0 rounded-md bg-black/25 px-3 text-sm outline-none shadow-[0_0_0_1px_var(--line)]"
        />
      </Row>
      <Row icon={Type} label="Digit size" hint={`${scale}`}>
        <input
          type="range"
          min={0}
          max={100}
          value={scale}
          onChange={(e) => useAuric.getState().setDigitScale(Number(e.target.value))}
          className="w-full accent-[var(--accent)]"
        />
      </Row>
      <Row icon={Sparkles} label="House glass" hint="How much of the floor you still see">
        <input
          type="range"
          min={0}
          max={100}
          value={glass}
          onChange={(e) => useAuric.getState().setHouseOpacity(Number(e.target.value))}
          className="w-full accent-[var(--accent)]"
        />
      </Row>

      <ToggleRow
        icon={Shield}
        label="Control lock"
        hint="Ignore accidental taps on the reel."
        on={controlLock}
        onChange={(v) => useAuric.getState().setControlLock(v)}
      />

      <Row icon={Lock} label="PIN lock" hint="Hashed on-device. Never stored in plaintext.">
        {pinHash ? (
          <PitButton
            variant="ghost"
            className="w-full"
            onClick={() => {
              useAuric.getState().setPinHash(null);
              setPinMsg("PIN cleared.");
            }}
          >
            Remove PIN
          </PitButton>
        ) : (
          <div className="flex min-w-0 flex-col gap-2">
            <input
              type="password"
              autoComplete="new-password"
              maxLength={77}
              value={pin}
              onChange={(e) => setPin(e.target.value.slice(0, 77))}
              placeholder="1–77 letters, numbers, symbols"
              className="h-12 w-full min-w-0 rounded-md bg-felt/70 px-3 text-sm outline-none shadow-[0_0_0_1px_var(--line)]"
            />
            <PitButton
              disabled={pin.length < 1}
              onClick={async () => {
                if (!useAuric.getState().requireCrown()) return;
                const hash = await sha256Hex(pin);
                useAuric.getState().setPinHash(hash, pin.length);
                useAuric.getState().setPinLocked(false);
                setPin("");
                setPinMsg("PIN set. It will lock next visit.");
                playLockIn();
              }}
            >
              Set
            </PitButton>
          </div>
        )}
        {pinMsg ? <p className="mt-2 text-xs text-muted">{pinMsg}</p> : null}
      </Row>

      <ToggleRow
        icon={MonitorSmartphone}
        label="Floating chip"
        hint="Draggable in-app time pill."
        on={floating}
        onChange={(v) => useAuric.getState().setFloating(v)}
      />

      <Row icon={MonitorSmartphone} label="Always on" hint="Over other apps needs the native build.">
        {mobile ? (
          <p className="rounded-md bg-felt/60 px-3 py-2 text-xs leading-relaxed text-muted">
            Picture-in-picture and pin-on-top do not work in Android browsers. Desktop / coming to the app.
          </p>
        ) : (
          <p className="text-xs text-muted">The floating chip stays inside AURIC.</p>
        )}
      </Row>

      <InstallPhone />

      <Row icon={Crown} label="Crown">
        <PitButton
          variant={isCrowned ? "chrome" : "gold"}
          className="w-full"
          onClick={() => useAuric.getState().setCrownSheet(true)}
        >
          {isCrowned ? "Crowned (preview)" : "Preview Crown · $7.77"}
        </PitButton>
      </Row>

      <p className="mt-4 text-center text-[11px] text-muted">
        Culet = the tip of a diamond. Three taps on the Pavilion mark.
      </p>

      <PitButton variant="ghost" className="mt-2 w-full" onClick={() => useAuric.getState().startOnboarding()}>
        Replay walkthrough
      </PitButton>
    </div>
  );
}

function Row({
  icon: Icon,
  label,
  hint,
  children,
}: {
  icon: typeof Sparkles;
  label: string;
  hint?: string;
  children: ReactNode;
}) {
  return (
    <section className="mt-4">
      <div className="mb-2 flex items-center gap-2">
        <Icon className="size-4 text-accent" />
        <div>
          <p className="text-sm text-digit">{label}</p>
          {hint ? <p className="text-[11px] text-muted">{hint}</p> : null}
        </div>
      </div>
      {children}
    </section>
  );
}

function ToggleRow({
  icon: Icon,
  label,
  hint,
  on,
  onChange,
}: {
  icon: typeof Sparkles;
  label: string;
  hint?: string;
  on: boolean;
  onChange: (v: boolean) => void;
}) {
  return (
    <div className="mt-4 flex min-w-0 items-center gap-3">
      <div className="flex min-w-0 flex-1 items-center gap-2">
        <Icon className="size-4 shrink-0 text-accent" />
        <div className="min-w-0">
          <p className="text-sm">{label}</p>
          {hint ? <p className="truncate text-[11px] text-muted">{hint}</p> : null}
        </div>
      </div>
      <OrbToggle on={on} onChange={onChange} label={label} />
    </div>
  );
}
