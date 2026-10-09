import { useEffect, useState } from "react";
import { AlarmAlert } from "@/components/auric/alarm-alert";
import { ClockEngine } from "@/components/auric/clock-engine";
import { FontApplier, PaletteApplier } from "@/components/auric/glow";
import { HeartOverlay } from "@/components/auric/heart-overlay";
import { NoticeSheet } from "@/components/auric/notice-bell";
import { PinLock } from "@/components/auric/pin-lock";
import { SessionShell } from "@/components/auric/session-shell";
import { Toasts } from "@/components/auric/toasts";
import { unlockAudio } from "@/lib/audio";
import { useAuric } from "@/lib/store";
import { withFreeModules } from "@/lib/modules";
import { observeInstall } from "@/lib/install";
import { registerAuricWorker } from "@/lib/notify";
import { pinSessionUnlocked } from "@/lib/pin-session";
import type { AppMode } from "@/lib/types";

const FLOOR: AppMode[] = ["timer", "stopwatch", "alarm", "remind"];

/** OLED-dark on SSR and the first client paint. Mood may follow system after mount. */
function useTerminalMood() {
  const mode = useAuric((s) => s.appearanceMode ?? "system");
  const [mood, setMood] = useState<"light" | "dark">("dark");
  useEffect(() => {
    const apply = () => {
      if (mode === "light") setMood("light");
      else if (mode === "dark") setMood("dark");
      else setMood(window.matchMedia("(prefers-color-scheme: dark)").matches ? "dark" : "light");
    };
    apply();
    if (mode !== "system") return;
    const mq = window.matchMedia("(prefers-color-scheme: dark)");
    mq.addEventListener("change", apply);
    return () => mq.removeEventListener("change", apply);
  }, [mode]);
  return mood;
}

export function AuricApp() {
  const mood = useTerminalMood();
  const glow = useAuric((s) => s.glowIntensity);

  useEffect(() => {
    const stopObservingInstall = observeInstall();
    const storageError = () => useAuric.getState().pushToast({
      title: "Changes are not saved",
      body: "Device storage is unavailable or full. Export important history before closing AURIC.",
    });
    window.addEventListener("auric-storage-error", storageError);
    const storageRecovered = () => useAuric.getState().pushToast({
      title: "Settings recovered",
      body: "Saved data could not be read. A recovery copy remains on this device; default settings are active.",
    });
    window.addEventListener("auric-storage-recovered", storageRecovered);
    const finish = () => {
      const s = useAuric.getState();
      const installed = withFreeModules(s.installedModules);
      const patch: {
        hydrated: boolean;
        pinLocked?: boolean;
        mode?: AppMode;
        installedModules?: ReturnType<typeof withFreeModules>;
        shell?: typeof s.shell;
        heartbeatEnabled?: boolean;
      } = {
        hydrated: true,
        installedModules: installed,
      };
      if (!installed.includes("heartbeat")) patch.heartbeatEnabled = false;
      if (s.pinHash && !pinSessionUnlocked(s.pinHash)) patch.pinLocked = true;
      if (!FLOOR.includes(s.mode as AppMode)) patch.mode = "timer";
      if (s.shell === "minimized" || s.shell === "closed") patch.shell = "session";
      if (s.shell === "tool") patch.shell = "home";
      useAuric.setState(patch);
      if (s.timerStatus === "running" && s.timerEndsAt && s.timerEndsAt <= Date.now()) {
        s.completeTimer();
      }
      if (s.heartbeatEnabled) {
        useAuric.setState({ lastBeatAt: Date.now() });
      }
    };
    const unsub = useAuric.persist.onFinishHydration(finish);
    void useAuric.persist.rehydrate();
    if (useAuric.persist.hasHydrated()) finish();
    void registerAuricWorker();
    const unlock = () => unlockAudio();
    window.addEventListener("pointerdown", unlock, { once: true });
    return () => {
      unsub();
      stopObservingInstall();
      window.removeEventListener("pointerdown", unlock);
      window.removeEventListener("auric-storage-error", storageError);
      window.removeEventListener("auric-storage-recovered", storageRecovered);
    };
  }, []);

  useEffect(() => {
    const root = document.documentElement;
    root.style.background = mood === "light" ? "#F4F2ED" : "#000000";
    root.style.colorScheme = mood;
    document.querySelector('meta[name="theme-color"]')?.setAttribute("content", mood === "light" ? "#F4F2ED" : "#000000");
  }, [mood]);

  return (
    <div
      className="tx-root auric-shell relative mx-auto flex min-h-dvh w-full min-w-0 max-w-lg flex-col overflow-x-hidden"
      data-mood={mood}
      data-glow={glow <= 0 ? "zero" : "on"}
      onClick={() => unlockAudio()}
    >
      <PaletteApplier />
      <FontApplier />
      <ClockEngine />
      <SessionShell />
      <AlarmAlert />
      <HeartOverlay />
      <NoticeSheet />
      <PinLock />
      <Toasts />
    </div>
  );
}
