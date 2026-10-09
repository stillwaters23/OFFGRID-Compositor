import { useEffect, useRef } from "react";
import { playHeartbeatCut, playWake, stopAlarm } from "@/lib/audio";
import { hapticCelebrate, hapticTick } from "@/lib/haptics";
import { showAuricNotification } from "@/lib/notify";
import { requestWakeLock } from "@/lib/platform";
import { rexLine, speakRex } from "@/lib/rex";
import { useAuric } from "@/lib/store";

export function ClockEngine() {
  const hydrated = useAuric((s) => s.hydrated);
  const timerStatus = useAuric((s) => s.timerStatus);
  const swStatus = useAuric((s) => s.swStatus);
  const ringing = useAuric((s) => s.ringingAlarm);
  const beatId = useAuric((s) => s.heartbeatLog[0]?.id);
  const lastBeat = useRef<string | undefined>(undefined);
  const beatInitialized = useRef(false);
  const signal = ringing ? `alarm:${ringing.id}` : timerStatus === "done" ? "timer" : "";

  useEffect(() => {
    if (!hydrated) return;
    const tick = () => useAuric.getState().tick(Date.now());
    tick();
    const id = window.setInterval(tick, 80);
    const onFocus = () => {
      const el = document.activeElement;
      if (el && (el.tagName === "TEXTAREA" || el.tagName === "INPUT")) return;
      tick();
    };
    document.addEventListener("visibilitychange", tick);
    window.addEventListener("pageshow", tick);
    window.addEventListener("focus", onFocus);
    return () => {
      window.clearInterval(id);
      document.removeEventListener("visibilitychange", tick);
      window.removeEventListener("pageshow", tick);
      window.removeEventListener("focus", onFocus);
    };
  }, [hydrated]);

  useEffect(() => {
    if (!hydrated || !signal) return;
    const state = useAuric.getState();
    const kind = state.ringingAlarm ? "alarm" : "timer";
    const body = state.ringingAlarm?.label || "Time crowned. Your timer is complete.";
    playWake(state.alarmSound);
    hapticCelebrate();
    if (state.rexOn) speakRex(rexLine(state.callName, body, kind));
    void showAuricNotification("AURIC", body, `auric-${kind}`);
    return () => stopAlarm();
  }, [signal, hydrated]);

  useEffect(() => {
    if (!hydrated) return;
    if (beatInitialized.current && beatId && beatId !== lastBeat.current) {
      playHeartbeatCut();
      hapticTick();
      const state = useAuric.getState();
      if (state.rexOn) speakRex(rexLine(state.callName, state.heartbeatReminder, "heartbeat"));
    }
    lastBeat.current = beatId;
    beatInitialized.current = true;
  }, [beatId, hydrated]);

  useEffect(() => {
    if (!hydrated || (timerStatus !== "running" && swStatus !== "running")) return;
    let disposed = false;
    let acquiring = false;
    let handle: Awaited<ReturnType<typeof requestWakeLock>> = null;
    const acquire = async () => {
      if (disposed || acquiring || document.visibilityState !== "visible" || (handle && !handle.released)) return;
      acquiring = true;
      const lock = await requestWakeLock();
      acquiring = false;
      if (disposed) { await lock?.release().catch(() => undefined); return; }
      handle = lock;
    };
    void acquire();
    const visible = () => { void acquire(); };
    document.addEventListener("visibilitychange", visible);
    return () => {
      disposed = true;
      document.removeEventListener("visibilitychange", visible);
      void handle?.release().catch(() => undefined);
    };
  }, [timerStatus, swStatus, hydrated]);
  return null;
}
