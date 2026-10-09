import { create } from "zustand";
import { createJSONStorage, persist } from "zustand/middleware";
import { auricStorage } from "@/lib/storage";
import { alarmTimestamp, nextAlarmDate } from "@/lib/alarm-time";
import type {
  AchievementId,
  Alarm,
  AppMode,
  DigitFont,
  PaintSlot,
  Palette,
  ReelSound,
  SavedTimer,
  Session,
  StopwatchStatus,
  Task,
  TimerStatus,
  VaultSectionId,
  VaultSort,
  Notice,
  NoticeKind,
  BeatLogEntry,
} from "@/lib/types";
import { HOUSE_WAKE_ID, wakeById } from "@/lib/wake";
import { evaluateUnlocks, computeStreak } from "@/lib/achievements";
import { msFromHms, msFromSpan, spanFromMs, SPAN_MAX, type Span } from "@/lib/format";
import { uid } from "@/lib/utils";
import { ensureReadable, hexToRgb, isValidHex, normalizeHex } from "@/lib/color";
import type { PavilionRoomId } from "@/lib/literacy";
import type { FloorId } from "@/lib/floors";
import { HOUSE_CLICK_ID, tickById } from "@/lib/ticks";
import { HOUSE_ICON_ID, HOUSE_SETTINGS_ID, HOUSE_STOPWATCH_ID, HOUSE_HEARTBEAT_ID, bankCutById } from "@/lib/banks";
import { DEFAULT_PALETTE, paletteById } from "@/lib/palettes";
import { HOUSE_FACE_ID, faceById, faceLoadState } from "@/lib/faces";
import { HOUSE_CONTROL_ID, controlById } from "@/lib/controls";
import { timerFinishedCopy } from "@/lib/alert-copy";
import type {
  AppearanceDraft,
  AppearanceMode,
  FolderIconStyle,
  FolderLayout,
  NumberColor,
} from "@/lib/appearance";
import { canApplyAppearance, committedPalette, draftsDiffer, makeReadable, SAFE_DARK, previewPalette, boundedPercent } from "@/lib/appearance";
import type { HourCycle } from "@/lib/clock";
import { FREE_MODULE_IDS, MODULES, withFreeModules, type ModuleId } from "@/lib/modules";
import { checkNumberColor } from "@/lib/contrast-guard";


const DEFAULT_SECTIONS: VaultSectionId[] = [
  "sessions",
  "charts",
  "achievements",
  "export",
];

export type Toast = {
  id: string;
  title: string;
  body?: string;
  tone?: "gold" | "danger" | "mute";
};

export type AppState = {
  hydrated: boolean;
  mode: AppMode;
  hours: number;
  minutes: number;
  seconds: number;
  years: number;
  months: number;
  weeks: number;
  days: number;
  cs: number;
  timerStatus: TimerStatus;
  timerEndsAt: number | null;
  timerPausedRemaining: number;
  timerStartedAt: number | null;
  swStatus: StopwatchStatus;
  swStartedAt: number | null;
  swAccumulated: number;
  swLaps: number[];
  paletteId: string;
  customPalette: Palette | null;
  glowIntensity: number;
  glowMode: boolean;
  glowPulseUntil: number;
  digitFont: DigitFont;
  reelSound: ReelSound;
  alarmSound: string;
  stopwatchSound: string;
  iconSound: string;
  settingsSound: string;
  heartbeatSound: string;
  ringingAlarm: { id: string; label: string } | null;
  analogFace: boolean;
  isCrowned: boolean;
  crownSheetOpen: boolean;
  pinHash: string | null;
  pinLength: number | null;
  pinLocked: boolean;
  controlLock: boolean;
  sessions: Session[];
  savedTimers: SavedTimer[];
  alarms: Alarm[];
  tasks: Task[];
  heartbeatMs: number;
  controlId: string;
  digitScale: number;
  houseOpacity: number;
  floorId: FloorId;
  floorCustom: string;
  vaultSort: VaultSort;
  vaultSearch: string;
  exportEnabled: boolean;
  sectionOrder: VaultSectionId[];
  heartbeatEnabled: boolean;
  heartbeatIndex: number;
  heartbeatReminder: string;
  lastBeatAt: number;
  heartbeatLog: BeatLogEntry[];
  heartbeatBeatOpen: boolean;
  rexOn: boolean;
  callName: string;
  archivedFolders: NoticeKind[];
  hudOpen: boolean;
  floatingVisible: boolean;
  floatingX: number;
  floatingY: number;
  floatingUsed: boolean;
  paintMode: boolean;
  spectrumOpen: boolean;
  spectrumSlot: PaintSlot;
  houseOpen: boolean;
  pavilionRoom: PavilionRoomId | null;
  houseFace: "pavilion" | "pit" | "shadow";
  directoryMode: "reels" | "buttons";
  orbHex: string;
  literacyIndex: number;
  onboardingDone: boolean;
  culetFound: boolean;
  onboardingStep: number;
  reelTicks: number;
  unlocked: AchievementId[];
  streakInsuranceUsed: boolean;
  toasts: Toast[];
  notices: Notice[];
  noticeOpen: boolean;
  noteDraftId: string | null;
  now: number;
  appearanceMode: AppearanceMode;
  scheduleEnabled: boolean;
  scheduleLight: string;
  scheduleDark: string;
  appearanceDraft: AppearanceDraft | null;
  laserEnabled: boolean;
  laserIntensity: number;
  laserColor: string;
  numberColor: NumberColor;
  folderSizeAll: number;
  folderSizes: Record<string, number>;
  folderLayout: FolderLayout;
  folderIconStyle: Record<string, FolderIconStyle>;
  folderNames: Record<string, string>;
  folderOrder: string[];
  hourCycle: HourCycle;
  timeZoneLocked: boolean;
  timeZoneId: string;
  hapticEnabled: boolean;
  soundEnabled: boolean;
  deviceBatteryOn: boolean;
  deviceNetworkOn: boolean;
  customizationFolder: null | "color" | "themes" | "icons" | "sizing" | "opacity" | "laser";
  taskDetailId: string | null;
  shell: "session" | "home" | "tool" | "minimized" | "closed";
  resumeShell: "session" | "home" | "tool";
  openModuleId: ModuleId | null;
  previewModuleId: ModuleId | null;
  installedModules: ModuleId[];
};


type Actions = {
  setHydrated: () => void;
  setMode: (mode: AppMode) => void;
  setHms: (h: number, m: number, s: number) => void;
  setHours: (n: number) => void;
  setMinutes: (n: number) => void;
  setSeconds: (n: number) => void;
  setSpanPart: (key: keyof Span, n: number) => void;
  applyPreset: (ms: number) => void;
  startTimer: () => void;
  pauseTimer: () => void;
  resumeTimer: () => void;
  resetTimer: () => void;
  completeTimer: () => void;
  startStopwatch: () => void;
  pauseStopwatch: () => void;
  resumeStopwatch: () => void;
  resetStopwatch: () => void;
  lapStopwatch: () => void;
  tick: (now: number) => void;
  setPaletteId: (id: string) => void;
  setSlotColor: (slot: PaintSlot, hex: string) => void;
  setGlowIntensity: (n: number) => void;
  setGlowMode: (on: boolean) => void;
  pulseGlow: (ms?: number) => void;
  setDigitFont: (f: DigitFont) => void;
  setReelSound: (s: ReelSound) => void;
  setAlarmSound: (id: string) => void;
  setStopwatchSound: (id: string) => void;
  setIconSound: (id: string) => void;
  setSettingsSound: (id: string) => void;
  setHeartbeatSound: (id: string) => void;
  setHeartbeatBeatOpen: (open: boolean) => void;
  setRexOn: (on: boolean) => void;
  setCallName: (n: string) => void;
  archiveFolder: (kind: NoticeKind) => void;
  deleteFolderNotices: (kind: NoticeKind) => void;
  keepFolder: (kind: NoticeKind) => void;
  startRinging: (a: { id: string; label: string }) => void;
  snoozeRinging: () => void;
  dismissRinging: () => void;
  archiveNotice: (id: string) => void;
  deleteNotice: (id: string) => void;
  archiveBeatLog: (id: string) => void;
  deleteBeatLog: (id: string) => void;
  archiveAllBeatLog: () => void;
  deleteAllBeatLog: () => void;
  setAnalogFace: (on: boolean) => void;
  bumpReelTick: () => void;
  unlockCrown: () => void;
  setCrownSheet: (open: boolean) => void;
  requireCrown: () => boolean;
  setPinHash: (hash: string | null, length?: number | null) => void;
  setPinLocked: (locked: boolean) => void;
  setControlLock: (on: boolean) => void;
  addSession: (session: Omit<Session, "id" | "usedCount"> & { id?: string; usedCount?: number }) => string;
  updateSessionNote: (id: string, note: string, archived?: boolean) => void;
  updateTask: (id: string, text: string) => void;
  setTaskDetail: (id: string | null) => void;
  setCustomizationFolder: (folder: AppState["customizationFolder"]) => void;
  setLaserEnabled: (enabled: boolean) => void;
  setLaserIntensity: (value: number) => void;
  setLaserColor: (value: string) => { ok: true } | { ok: false; reason: string };
  setNumberColor: (theme: "dark" | "light", value: string | null) => { ok: true } | { ok: false; reason: string };
  setFolderSizeAll: (value: number) => void;
  setFolderSize: (id: string, value: number | null) => void;
  setFolderLayout: (value: FolderLayout) => void;
  setFolderIconStyle: (id: string, value: FolderIconStyle) => void;
  setFolderName: (id: string, value: string) => boolean;
  setFolderOrder: (ids: string[]) => void;
  setHourCycle: (cycle: HourCycle) => void;
  setTimeZoneLocked: (on: boolean) => void;
  setTimeZoneId: (id: string) => void;
  setHapticEnabled: (on: boolean) => void;
  setSoundEnabled: (on: boolean) => void;
  setDeviceBatteryOn: (on: boolean) => void;
  setDeviceNetworkOn: (on: boolean) => void;
  setAppearanceMode: (mode: AppearanceMode) => void;
  setScheduleEnabled: (on: boolean) => void;
  setScheduleHours: (light: string, dark: string) => void;
  beginAppearanceDraft: () => void;
  setDraftPalette: (palette: Palette) => void;
  setDraftOptions: (patch: Partial<Pick<AppearanceDraft, "appearanceMode" | "scheduleEnabled" | "scheduleLight" | "scheduleDark" | "glowMode" | "floorId" | "floorCustom">>) => void;
  setDraftSlot: (slot: PaintSlot, hex: string) => void;
  setDraftGlow: (n: number) => void;
  setDraftOpacity: (n: number) => void;
  setDraftFont: (id: string) => void;
  setDraftScale: (n: number) => void;
  setDraftLaserEnabled: (enabled: boolean) => void;
  setDraftLaserIntensity: (value: number) => void;
  setDraftLaserColor: (value: string) => boolean;
  setDraftNumberColor: (theme: "dark" | "light", value: string | null) => void;
  setDraftFolderSizeAll: (value: number) => void;
  setDraftFolderSize: (id: string, value: number | null) => void;
  setDraftFolderLayout: (value: FolderLayout) => void;
  setDraftFolderIconStyle: (id: string, value: FolderIconStyle) => void;
  setDraftFolderName: (id: string, value: string) => boolean;
  setDraftFolderOrder: (ids: string[]) => void;
  applyAppearance: () => { ok: true } | { ok: false; reason: string };
  discardAppearance: () => void;
  restoreReadableAppearance: () => void;
  makeDraftReadable: () => void;
  openSession: () => void;
  closeSession: () => void;
  goHome: () => void;
  toSession: () => void;
  minimizeShell: () => void;
  restoreShell: () => void;
  openTool: (mode: AppMode) => void;
  enterModule: (id: ModuleId) => void;
  leaveModule: () => void;
  previewModule: (id: ModuleId | null) => void;

  deleteSession: (id: string) => void;
  setVaultSort: (s: VaultSort) => void;
  setVaultSearch: (q: string) => void;
  setExportEnabled: (on: boolean) => void;
  moveSection: (id: VaultSectionId, dir: -1 | 1) => void;
  saveTimer: (name: string) => void;
  loadSavedTimer: (id: string) => void;
  deleteSavedTimer: (id: string) => void;
  setHeartbeatEnabled: (on: boolean) => void;
  setHeartbeatIndex: (i: number) => void;
  setHeartbeatReminder: (t: string) => void;
  setHeartbeatMs: (ms: number) => void;
  addAlarm: (a: {
    year: number;
    month: number;
    day: number;
    hours: number;
    minutes: number;
    seconds: number;
    label: string;
  }) => void;
  toggleAlarm: (id: string) => void;
  deleteAlarm: (id: string) => void;
  addTask: (text: string) => void;
  toggleTask: (id: string) => void;
  deleteTask: (id: string) => void;
  setControlId: (id: string) => void;
  setDigitScale: (n: number) => void;
  setHouseOpacity: (n: number) => void;
  setFloorId: (id: FloorId) => void;
  setFloorCustom: (dataUrl: string) => void;
  setHudOpen: (open: boolean) => void;
  setFloating: (visible: boolean) => void;
  setFloatingPos: (x: number, y: number) => void;
  setPaintMode: (on: boolean) => void;
  openSpectrum: (slot?: PaintSlot) => void;
  closeSpectrum: () => void;
  setSpectrumSlot: (slot: PaintSlot) => void;
  setHouseOpen: (open: boolean) => void;
  setHouseFace: (f: "pavilion" | "pit" | "shadow") => void;
  setDirectoryMode: (m: "reels" | "buttons") => void;
  setOrbHex: (hex: string) => void;
  enterRoom: (room: PavilionRoomId) => void;
  leaveRoom: () => void;
  setLiteracyIndex: (n: number) => void;
  discoverCulet: () => void;
  finishOnboarding: () => void;
  startOnboarding: () => void;
  setOnboardingStep: (n: number) => void;
  pushToast: (t: Omit<Toast, "id">) => void;
  dismissToast: (id: string) => void;
  pushNotice: (n: Omit<Notice, "id" | "at" | "read">) => void;
  markNoticesRead: () => void;
  markKindRead: (kind: NoticeKind) => void;
  setNoticeOpen: (open: boolean) => void;
  setNoteDraft: (id: string | null) => void;
};

export type AuricStore = AppState & Actions;

const initial: AppState = {
  hydrated: false,
  mode: "timer",
  hours: 0,
  minutes: 0,
  seconds: 0,
  years: 0,
  months: 0,
  weeks: 0,
  days: 0,
  cs: 0,
  timerStatus: "idle",
  timerEndsAt: null,
  timerPausedRemaining: 0,
  timerStartedAt: null,
  swStatus: "idle",
  swStartedAt: null,
  swAccumulated: 0,
  swLaps: [],
  paletteId: DEFAULT_PALETTE.id,
  customPalette: null,
  glowIntensity: 36,
  glowMode: false,
  glowPulseUntil: 0,
  digitFont: HOUSE_FACE_ID,
  reelSound: HOUSE_CLICK_ID,
  alarmSound: HOUSE_WAKE_ID,
  stopwatchSound: HOUSE_STOPWATCH_ID,
  iconSound: HOUSE_ICON_ID,
  settingsSound: HOUSE_SETTINGS_ID,
  heartbeatSound: HOUSE_HEARTBEAT_ID,
  ringingAlarm: null,
  analogFace: false,
  isCrowned: false,
  crownSheetOpen: false,
  pinHash: null,
  pinLength: null,
  pinLocked: false,
  controlLock: false,
  sessions: [],
  savedTimers: [],
  alarms: [],
  tasks: [],
  heartbeatMs: 0,
  controlId: HOUSE_CONTROL_ID,
  digitScale: 50,
  houseOpacity: 55,
  floorId: "none",
  floorCustom: "",
  vaultSort: "recent",
  vaultSearch: "",
  exportEnabled: false,
  sectionOrder: DEFAULT_SECTIONS,
  heartbeatEnabled: false,
  heartbeatIndex: 4,
  heartbeatReminder: "",
  lastBeatAt: 0,
  heartbeatLog: [],
  heartbeatBeatOpen: false,
  rexOn: false,
  callName: "",
  archivedFolders: [],
  hudOpen: false,
  floatingVisible: false,
  floatingX: 16,
  floatingY: 120,
  floatingUsed: false,
  paintMode: false,
  spectrumOpen: false,
  spectrumSlot: "accent",
  houseOpen: false,
  pavilionRoom: null,
  houseFace: "pavilion",
  directoryMode: "buttons",
  orbHex: "",
  literacyIndex: 5,
  onboardingDone: true,
  culetFound: false,
  onboardingStep: 0,
  reelTicks: 0,
  unlocked: [],
  streakInsuranceUsed: false,
  toasts: [],
  notices: [],
  noticeOpen: false,
  noteDraftId: null,
  now: Date.now(),
  appearanceMode: "system",
  scheduleEnabled: false,
  scheduleLight: "07:00",
  scheduleDark: "19:00",
  appearanceDraft: null,
  laserEnabled: true,
  laserIntensity: 100,
  laserColor: "#FF0000",
  numberColor: { dark: null, light: null },
  folderSizeAll: 50,
  folderSizes: {},
  folderLayout: "big",
  folderIconStyle: {},
  folderNames: {},
  folderOrder: [],
  hourCycle: "auto",
  timeZoneLocked: false,
  timeZoneId: "",
  hapticEnabled: true,
  soundEnabled: true,
  deviceBatteryOn: false,
  deviceNetworkOn: false,
  customizationFolder: null,
  taskDetailId: null,
  shell: "session",
  resumeShell: "session",
  openModuleId: null,
  previewModuleId: null,
  installedModules: [...FREE_MODULE_IDS],
};

function blankDraft(s: AppState): AppearanceDraft {
  return {
    palette: s.customPalette ?? paletteById(s.paletteId),
    appearanceMode: s.appearanceMode,
    scheduleEnabled: s.scheduleEnabled,
    scheduleLight: s.scheduleLight,
    scheduleDark: s.scheduleDark,
    glowMode: s.glowMode,
    floorId: s.floorId,
    floorCustom: s.floorCustom,
    glowIntensity: s.glowIntensity,
    houseOpacity: s.houseOpacity,
    digitFont: s.digitFont,
    digitScale: s.digitScale,
    laserEnabled: s.laserEnabled,
    laserIntensity: s.laserIntensity,
    laserColor: s.laserColor,
    numberColor: { ...s.numberColor },
    folderSizeAll: s.folderSizeAll,
    folderSizes: { ...s.folderSizes },
    folderLayout: s.folderLayout,
    folderIconStyle: { ...s.folderIconStyle },
    folderNames: { ...s.folderNames },
    folderOrder: [...s.folderOrder],
  };
}

function numberColorContext(
  state: AppState,
  theme: "dark" | "light",
  source?: Pick<AppearanceDraft, "palette" | "laserEnabled" | "laserIntensity" | "laserColor">,
) {
  const palette = source?.palette ?? state.customPalette ?? paletteById(state.paletteId);
  const laserColor = source?.laserColor ?? state.laserColor;
  const laserIntensity = source?.laserIntensity ?? state.laserIntensity;
  const laserEnabled = source?.laserEnabled ?? state.laserEnabled;
  return {
    palette: committedPalette({
      paletteId: palette.id,
      customPalette: palette.id === "custom" ? palette : null,
      mode: theme,
      scheduleEnabled: false,
      lightStart: state.scheduleLight,
      darkStart: state.scheduleDark,
      nowMs: Number.isFinite(state.now) ? state.now : 1,
      zone: state.timeZoneLocked ? state.timeZoneId : null,
    }),
    laserColor,
    laserIntensity,
    laserEnabled,
  };
}

function isOpaqueHex(value: string): string | null {
  if (!isValidHex(value)) return null;
  const rgb = hexToRgb(value);
  return rgb?.a === 1 ? normalizeHex(value) : null;
}

function cleanFolderOrder(ids: string[]): string[] {
  const known = new Set(MODULES.map((module) => module.id));
  return [...new Set(ids.filter((id) => typeof id === "string" && known.has(id as ModuleId)))];
}

export function isAppearanceDirty(s: AppState): boolean {
  if (!s.appearanceDraft) return false;
  return draftsDiffer(s.appearanceDraft, blankDraft({ ...s, appearanceDraft: null }));
}

function totalMinutes(sessions: Session[]): number {
  return sessions.reduce((acc, s) => acc + s.durationMs, 0) / 60_000;
}

function withAchievements(s: AppState): AppState {
  const { streak } = computeStreak(s.sessions, !s.streakInsuranceUsed);
  const newly = evaluateUnlocks({
    sessions: s.sessions,
    reelTicks: s.reelTicks,
    streak,
    totalMinutes: totalMinutes(s.sessions),
    isCrowned: s.isCrowned,
    floatingUsed: s.floatingUsed,
    already: s.unlocked,
  });
  if (newly.length === 0) return s;
  return {
    ...s,
    unlocked: [...s.unlocked, ...newly],
    toasts: [
      ...s.toasts,
      ...newly.map((id) => ({
        id: uid(),
        title: "Achievement",
        body: id.replace(/-/g, " "),
        tone: "gold" as const,
      })),
    ],
  };
}

export const useAuric = create<AuricStore>()(
  persist(
    (set, get) => ({
      ...initial,
      setHydrated: () => set({ hydrated: true }),
      setMode: (mode) => set({ mode }),
      setHms: (h, m, s) => set({ hours: h, minutes: m, seconds: s }),
      setHours: (n) => set({ hours: n }),
      setMinutes: (n) => set({ minutes: n }),
      setSeconds: (n) => set({ seconds: n }),
      setSpanPart: (key, n) => {
        if (get().controlLock || get().timerStatus === "running" || get().timerStatus === "paused") return;
        const max = SPAN_MAX[key];
        const v = Math.max(0, Math.min(max, Math.floor(n)));
        set({ [key]: v } as Partial<AppState>);
      },
      applyPreset: (ms) => {
        if (get().controlLock || get().timerStatus === "running" || get().timerStatus === "paused") return;
        set(spanFromMs(ms));
      },
      startTimer: () => {
        const s = get();
        if (s.controlLock || s.timerStatus === "running" || s.timerStatus === "paused") return;
        const remaining = msFromSpan(s);
        if (remaining <= 0) return;
        const now = Date.now();
        set({
          timerStatus: "running",
          timerEndsAt: now + remaining,
          timerStartedAt: now,
          timerPausedRemaining: remaining,
        });
      },
      pauseTimer: () => {
        const { timerEndsAt, controlLock, timerStatus } = get();
        if (controlLock || timerStatus !== "running") return;
        if (timerEndsAt !== null && timerEndsAt <= Date.now()) { get().completeTimer(); return; }
        const remaining = Math.max(0, (timerEndsAt ?? Date.now()) - Date.now());
        set({ timerStatus: "paused", timerPausedRemaining: remaining, timerEndsAt: null });
      },
      resumeTimer: () => {
        const { timerPausedRemaining, controlLock, timerStatus } = get();
        if (controlLock || timerStatus !== "paused" || timerPausedRemaining <= 0) return;
        set({ timerStatus: "running", timerEndsAt: Date.now() + timerPausedRemaining });
      },
      resetTimer: () => {
        if (get().controlLock && get().timerStatus !== "done") return;
        set({
          timerStatus: "idle",
          timerEndsAt: null,
          timerPausedRemaining: 0,
          timerStartedAt: null,
          years: 0,
          months: 0,
          weeks: 0,
          days: 0,
          hours: 0,
          minutes: 0,
          seconds: 0,
          cs: 0,
        });
      },
      completeTimer: () => {
        const s = get();
        if (s.timerStatus !== "running") return;
        const planned = msFromSpan(s);
        const started = s.timerStartedAt ?? Date.now() - planned;
        const ended = s.timerEndsAt ?? Date.now();
        const duration = Math.min(planned, Math.max(0, ended - started));
        const id = uid();
        const session: Session = {
          id,
          kind: "timer",
          startedAt: started,
          endedAt: ended,
          durationMs: duration,
          plannedMs: planned,
          note: "",
          usedCount: 1,
        };
        set(
          withAchievements({
            ...s,
            timerStatus: "done",
            timerEndsAt: null,
            sessions: [session, ...s.sessions],
            noteDraftId: id,
            glowPulseUntil: Date.now() + 4000,
            notices: [
              { id: uid(), kind: "timer" as const, title: "Timer", body: timerFinishedCopy(), at: Date.now(), read: false, archived: false },
              ...s.notices,
            ].slice(0, 77),
          }),
        );
      },
      startStopwatch: () => {
        if (get().controlLock || get().swStatus !== "idle") return;
        set({ swStatus: "running", swStartedAt: Date.now() });
      },
      pauseStopwatch: () => {
        const { swStartedAt, swAccumulated, controlLock, swStatus } = get();
        if (controlLock || swStatus !== "running") return;
        const extra = swStartedAt ? Date.now() - swStartedAt : 0;
        set({ swStatus: "paused", swAccumulated: swAccumulated + extra, swStartedAt: null });
      },
      resumeStopwatch: () => {
        if (get().controlLock || get().swStatus !== "paused") return;
        set({ swStatus: "running", swStartedAt: Date.now() });
      },
      resetStopwatch: () => {
        const s = get();
        if (s.controlLock) return;
        const elapsed = s.swAccumulated + (s.swStartedAt ? Date.now() - s.swStartedAt : 0);
        const zero = {
          swStatus: "idle" as const,
          swStartedAt: null,
          swAccumulated: 0,
          swLaps: [] as number[],
        };
        if (elapsed > 1000) {
          const id = uid();
          const session: Session = {
            id,
            kind: "stopwatch",
            startedAt: Date.now() - elapsed,
            endedAt: Date.now(),
            durationMs: elapsed,
            note: "",
            laps: s.swLaps.length ? s.swLaps : undefined,
            usedCount: 1,
          };
          set(
            withAchievements({
              ...s,
              ...zero,
              sessions: [session, ...s.sessions],
              noteDraftId: id,
              notices: [
                {
                  id: uid(),
                  kind: "stopwatch" as const,
                  title: "Stopwatch",
                  body: "Lap closed. Time is in History.",
                  at: Date.now(),
                  read: false,
                  archived: false,
                },
                ...s.notices,
              ].slice(0, 77),
            }),
          );
          return;
        }
        set(zero);
      },
      lapStopwatch: () => {
        const s = get();
        if (s.controlLock || s.swStatus !== "running") return;
        const elapsed = s.swAccumulated + (s.swStartedAt ? Date.now() - s.swStartedAt : 0);
        set({ swLaps: [...s.swLaps, elapsed] });
      },
      tick: (now) => {
        let s = get();
        if (!s.hydrated) return;
        if (s.timerStatus === "running" && s.timerEndsAt && now >= s.timerEndsAt) {
          get().completeTimer();
          s = get();
        }
        const patch: Partial<AppState> = { now };
        if (s.heartbeatEnabled && s.heartbeatMs >= 100 && !s.heartbeatBeatOpen) {
          const optionMs = s.heartbeatMs;
          const last = s.lastBeatAt || now;
          if (!s.lastBeatAt) patch.lastBeatAt = now;
          if (now - last >= optionMs) {
            patch.lastBeatAt = now;
            const body = s.heartbeatReminder.trim() || "Keep track. Log what you are doing.";
            const unreadHb = s.notices.find((n) => n.kind === "heartbeat" && !n.read && !n.archived);
            if (unreadHb) {
              patch.notices = s.notices.map((n) => (n.id === unreadHb.id ? { ...n, body, at: now } : n));
            } else {
              patch.notices = [
                { id: uid(), kind: "heartbeat" as const, title: "Heartbeat", body, at: now, read: false, archived: false },
                ...s.notices,
              ].slice(0, 77);
            }
            patch.heartbeatLog = [{ id: uid(), at: now, body, archived: false }, ...s.heartbeatLog].slice(0, 77);
            patch.heartbeatBeatOpen = true;
          }
        }
        // Ring one dated alarm at a time. Other due alarms remain queued until
        // dismissal/snooze; lastFiredKey makes recovery and refresh idempotent.
        const due = !s.ringingAlarm ? s.alarms.filter((a) => a.enabled && !a.lastFiredKey && alarmTimestamp(a) <= now)
          .sort((a, b) => alarmTimestamp(a) - alarmTimestamp(b))[0] : undefined;
        if (due) {
          patch.alarms = s.alarms.map((a) => a.id === due.id ? { ...a, lastFiredKey: String(alarmTimestamp(a)) } : a);
          patch.ringingAlarm = { id: due.id, label: due.label };
          patch.notices = [{ id: uid(), kind: "alarm" as const, title: "Alarm", body: due.label, at: now, read: false, archived: false }, ...(patch.notices ?? s.notices)].slice(0, 77);
          patch.glowPulseUntil = now + 4000;
        }
        const inbox = patch.notices ?? s.notices;
        const aged = inbox.map((n) =>
          !n.archived && now - n.at >= 86_400_000 ? { ...n, archived: true, read: true } : n,
        );
        if (aged.some((n, i) => n.archived !== inbox[i]?.archived)) patch.notices = aged;
        const beats = patch.heartbeatLog ?? s.heartbeatLog;
        const agedBeats = beats.map((b) =>
          !b.archived && now - b.at >= 86_400_000 ? { ...b, archived: true } : b,
        );
        if (agedBeats.some((b, i) => b.archived !== beats[i]?.archived)) patch.heartbeatLog = agedBeats;
        set(patch);
      },
      setPaletteId: (id) => set({ paletteId: paletteById(id).id, customPalette: null }),
      setSlotColor: (slot, hex) => {
        const base = get().customPalette ?? paletteById(get().paletteId);
        const next = ensureReadable({
          ...base,
          id: "custom",
          name: "Custom",
          premium: false,
          [slot]: hex,
        });
        set({ customPalette: next, paletteId: "custom" });
      },
      setGlowIntensity: (n) => set({ glowIntensity: boundedPercent(n) }),
      setGlowMode: (on) => set({ glowMode: on }),
      pulseGlow: (ms = 1200) => set({ glowPulseUntil: Date.now() + ms }),
      setDigitFont: (f) => set({ digitFont: faceById(f).id }),
      setReelSound: (s) => set({ reelSound: tickById(s).id }),
      setAlarmSound: (s) => set({ alarmSound: wakeById(s).id }),
      setStopwatchSound: (s) => set({ stopwatchSound: bankCutById(s).id }),
      setIconSound: (s) => set({ iconSound: bankCutById(s).id }),
      setSettingsSound: (s) => set({ settingsSound: bankCutById(s).id }),
      setHeartbeatSound: (s) => set({ heartbeatSound: bankCutById(s).id }),
      setHeartbeatBeatOpen: (open) =>
        set(open ? { heartbeatBeatOpen: true } : { heartbeatBeatOpen: false, lastBeatAt: Date.now() }),
      setRexOn: (on) => set({ rexOn: on }),
      setCallName: (n) => set({ callName: n.slice(0, 40) }),
      archiveFolder: (kind) =>
        set({
          archivedFolders: Array.from(new Set([...get().archivedFolders, kind])),
          notices: get().notices.map((n) => (n.kind === kind ? { ...n, archived: true, read: true } : n)),
        }),
      deleteFolderNotices: (kind) =>
        set({
          notices: get().notices.filter((n) => n.kind !== kind),
          archivedFolders: get().archivedFolders.filter((k) => k !== kind),
        }),
      keepFolder: (kind) =>
        set({
          archivedFolders: get().archivedFolders.filter((k) => k !== kind),
        }),
      startRinging: (a) =>
        set({
          ringingAlarm: a,
          notices: [
            { id: uid(), kind: "alarm" as const, title: "Alarm", body: a.label || "Get up. Now.", at: Date.now(), read: false, archived: false },
            ...get().notices,
          ].slice(0, 77),
        }),
      snoozeRinging: () => {
        const ring = get().ringingAlarm;
        if (!ring) return;
        const d = new Date(Date.now() + 5 * 60_000);
        set({
          ringingAlarm: null,
          alarms: get().alarms.map((a) =>
            a.id === ring.id
              ? {
                  ...a,
                  enabled: true,
                  lastFiredKey: "",
                  year: d.getFullYear(),
                  month: d.getMonth() + 1,
                  day: d.getDate(),
                  hours: d.getHours(),
                  minutes: d.getMinutes(),
                  seconds: d.getSeconds(),
                }
              : a,
          ),
        });
      },
      dismissRinging: () => {
        const ring = get().ringingAlarm;
        set({
          ringingAlarm: null,
          alarms: ring
            ? get().alarms.map((a) => (a.id === ring.id ? { ...a, enabled: false } : a))
            : get().alarms,
        });
      },
      setAnalogFace: (on) => {
        if (on && !get().isCrowned) {
          set({ crownSheetOpen: true });
          return;
        }
        set({ analogFace: on });
      },
      bumpReelTick: () => {
        const s = get();
        set(withAchievements({ ...s, reelTicks: s.reelTicks + 1 }));
      },
      unlockCrown: () => {
        const s = get();
        set(
          withAchievements({
            ...s,
            isCrowned: true,
            crownSheetOpen: false,
            toasts: [
              ...s.toasts,
              {
                id: uid(),
                title: "Crown preview",
                body: "Luxury table unlocked. No charge — demo only.",
                tone: "gold",
              },
            ],
          }),
        );
      },
      setCrownSheet: (open) => set({ crownSheetOpen: open }),
      requireCrown: () => {
        if (get().isCrowned) return true;
        set({ crownSheetOpen: true });
        return false;
      },
      setPinHash: (hash, length) =>
        set({
          pinHash: hash,
          pinLength: hash ? (length ?? null) : null,
          pinLocked: false,
        }),
      setPinLocked: (locked) => set({ pinLocked: locked }),
      setControlLock: (on) => {
        if (on && !get().isCrowned) {
          set({ crownSheetOpen: true });
          return;
        }
        set({ controlLock: on });
      },
      addSession: (session) => {
        const id = session.id ?? uid();
        const s = get();
        set(
          withAchievements({
            ...s,
            sessions: [{ ...session, id, usedCount: session.usedCount ?? 1 }, ...s.sessions],
          }),
        );
        return id;
      },
      updateSessionNote: (id, note, archived = false) =>
        set({ sessions: get().sessions.map((x) => (x.id === id ? { ...x, note, noteArchived: archived } : x)) }),
      updateTask: (id, text) =>
        set({ tasks: get().tasks.map((t) => (t.id === id ? { ...t, text: text.trim() || t.text } : t)) }),
      setTaskDetail: (id) => set({ taskDetailId: id }),
      setCustomizationFolder: (folder) => set({ customizationFolder: folder }),
      setLaserEnabled: (laserEnabled) => set({ laserEnabled }),
      setLaserIntensity: (value) => set({ laserIntensity: boundedPercent(value, 100) }),
      setLaserColor: (value) => {
        const laserColor = isOpaqueHex(value);
        if (!laserColor) return { ok: false, reason: "Use a valid opaque #RGB or #RRGGBB laser color." };
        set({ laserColor });
        return { ok: true };
      },
      setNumberColor: (theme, value) => {
        if (value === null) {
          set({ numberColor: { ...get().numberColor, [theme]: null } });
          return { ok: true };
        }
        const color = isOpaqueHex(value);
        if (!color) return { ok: false, reason: "Use a valid opaque #RGB or #RRGGBB number color." };
        const state = get();
        const result = checkNumberColor(color, theme, numberColorContext(state, theme));
        if (!result.ok) return { ok: false, reason: result.message };
        set({ numberColor: { ...state.numberColor, [theme]: color } });
        return { ok: true };
      },
      setFolderSizeAll: (value) => set({ folderSizeAll: boundedPercent(value, 50) }),
      setFolderSize: (id, value) =>
        set({
          folderSizes: (() => {
            const next = { ...get().folderSizes };
            if (!id.trim()) return next;
            if (value === null) delete next[id];
            else next[id] = boundedPercent(value, 50);
            return next;
          })(),
        }),
      setFolderLayout: (value) => {
        if (value === "grid" || value === "big" || value === "single") set({ folderLayout: value });
      },
      setFolderIconStyle: (id, value) => {
        if (!id.trim() || !["3d", "flat", "etched"].includes(value)) return;
        set({ folderIconStyle: { ...get().folderIconStyle, [id]: value } });
      },
      setFolderName: (id, value) => {
        const name = value.trim();
        if (!id.trim() || name.length < 1 || name.length > 24) return false;
        set({ folderNames: { ...get().folderNames, [id]: name } });
        return true;
      },
      setFolderOrder: (ids) => set({ folderOrder: cleanFolderOrder(ids) }),
      setHourCycle: (cycle) => set({ hourCycle: cycle }),
      setTimeZoneLocked: (on) => set({ timeZoneLocked: on }),
      setTimeZoneId: (id) => set({ timeZoneId: id }),
      setHapticEnabled: (on) => set({ hapticEnabled: on }),
      setSoundEnabled: (on) => set({ soundEnabled: on }),
      setDeviceBatteryOn: (on) => set({ deviceBatteryOn: on }),
      setDeviceNetworkOn: (on) => set({ deviceNetworkOn: on }),
      setAppearanceMode: (mode) => set({ appearanceMode: mode }),
      setScheduleEnabled: (on) => set({ scheduleEnabled: on }),
      setScheduleHours: (light, dark) => set({ scheduleLight: light, scheduleDark: dark }),

      deleteSession: (id) => set({ sessions: get().sessions.filter((x) => x.id !== id) }),
      setVaultSort: (s) => set({ vaultSort: s }),
      setVaultSearch: (q) => set({ vaultSearch: q }),
      setExportEnabled: (on) => {
        if (on && !get().isCrowned) {
          set({ crownSheetOpen: true });
          return;
        }
        set({ exportEnabled: on });
      },
      moveSection: (id, dir) => {
        const order = [...get().sectionOrder];
        const i = order.indexOf(id);
        if (i < 0) return;
        const j = i + dir;
        if (j < 0 || j >= order.length) return;
        const tmp = order[i]!;
        order[i] = order[j]!;
        order[j] = tmp;
        set({ sectionOrder: order });
      },
      saveTimer: (name) => {
        if (!get().requireCrown()) return;
        const { hours, minutes, seconds, savedTimers } = get();
        set({
          savedTimers: [
            { id: uid(), name: name.trim() || "Untitled", hours, minutes, seconds, durationMs: msFromSpan(get()), used: 0 },
            ...savedTimers,
          ],
        });
      },
      loadSavedTimer: (id) => {
        if (get().controlLock || get().timerStatus === "running" || get().timerStatus === "paused") return;
        const t = get().savedTimers.find((x) => x.id === id);
        if (!t) return;
        set({
          ...spanFromMs(t.durationMs ?? msFromHms(t.hours, t.minutes, t.seconds)),
          savedTimers: get().savedTimers.map((x) => (x.id === id ? { ...x, used: x.used + 1 } : x)),
          mode: "timer",
        });
      },
      deleteSavedTimer: (id) => set({ savedTimers: get().savedTimers.filter((t) => t.id !== id) }),
      setHeartbeatEnabled: (on) => set({ heartbeatEnabled: on, lastBeatAt: on ? Date.now() : get().lastBeatAt }),
      setHeartbeatIndex: (i) => set({ heartbeatIndex: i }),
      setHeartbeatReminder: (t) => set({ heartbeatReminder: t }),
      setHeartbeatMs: (ms) => set({ heartbeatMs: Math.max(0, ms), lastBeatAt: Date.now() }),
      addAlarm: (a) =>
        set({
          alarms: [
            {
              id: uid(),
              year: a.year,
              month: a.month,
              day: a.day,
              hours: a.hours,
              minutes: a.minutes,
              seconds: a.seconds,
              label: a.label.trim() || "Alarm",
              enabled: true,
              lastFiredKey: "",
            },
            ...get().alarms,
          ],
        }),
      toggleAlarm: (id) => set({
        alarms: get().alarms.map((a) => {
          if (a.id !== id) return a;
          if (a.enabled) return { ...a, enabled: false };
          if (alarmTimestamp(a) > Date.now()) return { ...a, enabled: true, lastFiredKey: "" };
          const next = nextAlarmDate(a.hours, a.minutes, false);
          return { ...a, enabled: true, lastFiredKey: "", year: next.getFullYear(), month: next.getMonth() + 1, day: next.getDate() };
        }),
        ringingAlarm: get().ringingAlarm?.id === id ? null : get().ringingAlarm,
      }),
      deleteAlarm: (id) => set({ alarms: get().alarms.filter((a) => a.id !== id), ringingAlarm: get().ringingAlarm?.id === id ? null : get().ringingAlarm }),
      addTask: (text) => {
        const t = text.trim();
        if (!t) return;
        set({ tasks: [{ id: uid(), text: t, done: false, createdAt: Date.now() }, ...get().tasks] });
      },
      toggleTask: (id) =>
        set({ tasks: get().tasks.map((t) => (t.id === id ? { ...t, done: !t.done } : t)) }),
      deleteTask: (id) => set({ tasks: get().tasks.filter((t) => t.id !== id) }),
      setControlId: (id) => set({ controlId: controlById(id).id }),
      setDigitScale: (n) => set({ digitScale: Math.max(0, Math.min(100, n)) }),
      setHouseOpacity: (n) => set({ houseOpacity: Math.max(0, Math.min(100, n)) }),
      setFloorId: (id) => set({ floorId: id }),
      setFloorCustom: (dataUrl) => set({ floorCustom: dataUrl, floorId: dataUrl ? "custom" : get().floorId }),
      setHudOpen: (open) => set({ hudOpen: open }),
      setFloating: (visible) => {
        if (visible && !get().isCrowned) {
          set({ crownSheetOpen: true });
          return;
        }
        set(
          withAchievements({
            ...get(),
            floatingVisible: visible,
            floatingUsed: get().floatingUsed || visible,
          }),
        );
      },
      setFloatingPos: (x, y) => set({ floatingX: x, floatingY: y }),
      setPaintMode: (on) => set({ paintMode: on }),
      openSpectrum: (slot) =>
        set({
          spectrumOpen: true,
          houseOpen: true,
          pavilionRoom: "cut",
          customizationFolder: "color",
          spectrumSlot: slot ?? get().spectrumSlot,
          paintMode: false,
          appearanceDraft: get().appearanceDraft ?? blankDraft(get()),
        }),
      closeSpectrum: () => set({ spectrumOpen: false, paintMode: false, pavilionRoom: null, houseOpen: false, customizationFolder: null }),
      setSpectrumSlot: (slot) => set({ spectrumSlot: slot }),
      setHouseOpen: (open) =>
        set({
          houseOpen: open,
          pavilionRoom: open ? get().pavilionRoom : null,
          spectrumOpen: open ? get().spectrumOpen : false,
          noticeOpen: open ? false : get().noticeOpen,
          customizationFolder: open ? get().customizationFolder : null,
        }),
      setHouseFace: (f) => set({ houseFace: f }),
      setDirectoryMode: (m) => set({ directoryMode: m }),
      setOrbHex: (hex) => set({ orbHex: hex }),
      enterRoom: (room) => set({
        houseOpen: true,
        pavilionRoom: room,
        spectrumOpen: room === "cut",
        appearanceDraft: room === "cut" ? (get().appearanceDraft ?? blankDraft(get())) : get().appearanceDraft,
        customizationFolder: room === "cut" ? get().customizationFolder : null,
      }),
      leaveRoom: () => set({ pavilionRoom: null, spectrumOpen: false, customizationFolder: null, taskDetailId: null }),
      setLiteracyIndex: (n) => set({ literacyIndex: n }),
      discoverCulet: () => {
        if (get().culetFound) return;
        set({ culetFound: true });
        get().pushToast({
          title: "The Culet",
          body: "Tip of the diamond. Ice Absolute + Iceland are yours.",
          tone: "gold",
        });
      },
      finishOnboarding: () => set({ onboardingDone: true, onboardingStep: 0 }),
      startOnboarding: () => set({ onboardingDone: false, onboardingStep: 0, houseOpen: false }),
      setOnboardingStep: (n) => set({ onboardingStep: n }),
      pushToast: (t) => set({ toasts: [...get().toasts, { ...t, id: uid() }].slice(-6) }),
      dismissToast: (id) => set({ toasts: get().toasts.filter((t) => t.id !== id) }),
      pushNotice: (n) =>
        set({
          notices: [{ ...n, id: uid(), at: Date.now(), read: false, archived: false }, ...get().notices].slice(0, 77),
        }),
      markNoticesRead: () => set({ notices: get().notices.map((n) => (n.archived ? n : { ...n, read: true })) }),
      markKindRead: (kind) =>
        set({ notices: get().notices.map((n) => (n.kind === kind && !n.archived ? { ...n, read: true } : n)) }),
      setNoticeOpen: (open) =>
        set({
          noticeOpen: open,
          houseOpen: open ? false : get().houseOpen,
        }),
      archiveNotice: (id) =>
        set({ notices: get().notices.map((n) => (n.id === id ? { ...n, archived: true, read: true } : n)) }),
      deleteNotice: (id) => set({ notices: get().notices.filter((n) => n.id !== id) }),
      archiveBeatLog: (id) =>
        set({ heartbeatLog: get().heartbeatLog.map((b) => (b.id === id ? { ...b, archived: true } : b)) }),
      deleteBeatLog: (id) => set({ heartbeatLog: get().heartbeatLog.filter((b) => b.id !== id) }),
      archiveAllBeatLog: () =>
        set({ heartbeatLog: get().heartbeatLog.map((b) => ({ ...b, archived: true })) }),
      deleteAllBeatLog: () => set({ heartbeatLog: [] }),
      setNoteDraft: (id) => set({ noteDraftId: id }),
      beginAppearanceDraft: () => {
        if (!get().appearanceDraft) set({ appearanceDraft: blankDraft(get()) });
      },
      setDraftOptions: (patch) => {
        const draft = get().appearanceDraft ?? blankDraft(get());
        set({ appearanceDraft: { ...draft, ...patch } });
      },
      setDraftPalette: (palette) => {
        const draft = get().appearanceDraft ?? blankDraft(get());
        set({ appearanceDraft: { ...draft, palette } });
      },
      setDraftSlot: (slot, hex) => {
        const draft = get().appearanceDraft ?? blankDraft(get());
        set({ appearanceDraft: { ...draft, palette: { ...draft.palette, id: "custom", name: "Custom", [slot]: hex } } });
      },
      setDraftGlow: (n) => {
        const draft = get().appearanceDraft ?? blankDraft(get());
        set({ appearanceDraft: { ...draft, glowIntensity: boundedPercent(n) } });
      },
      setDraftOpacity: (n) => {
        const draft = get().appearanceDraft ?? blankDraft(get());
        set({ appearanceDraft: { ...draft, houseOpacity: boundedPercent(n) } });
      },
      setDraftFont: (id) => {
        const draft = get().appearanceDraft ?? blankDraft(get());
        set({ appearanceDraft: { ...draft, digitFont: faceById(id).id } });
      },
      setDraftScale: (n) => {
        const draft = get().appearanceDraft ?? blankDraft(get());
        set({ appearanceDraft: { ...draft, digitScale: boundedPercent(n) } });
      },
      setDraftLaserEnabled: (laserEnabled) => {
        const draft = get().appearanceDraft ?? blankDraft(get());
        set({ appearanceDraft: { ...draft, laserEnabled } });
      },
      setDraftLaserIntensity: (value) => {
        const draft = get().appearanceDraft ?? blankDraft(get());
        set({ appearanceDraft: { ...draft, laserIntensity: boundedPercent(value, 100) } });
      },
      setDraftLaserColor: (value) => {
        const color = isOpaqueHex(value);
        if (!color) return false;
        const draft = get().appearanceDraft ?? blankDraft(get());
        set({ appearanceDraft: { ...draft, laserColor: color } });
        return true;
      },
      setDraftNumberColor: (theme, value) => {
        const draft = get().appearanceDraft ?? blankDraft(get());
        const color = value === null ? null : normalizeHex(value);
        set({ appearanceDraft: { ...draft, numberColor: { ...draft.numberColor, [theme]: color } } });
      },
      setDraftFolderSizeAll: (value) => {
        const draft = get().appearanceDraft ?? blankDraft(get());
        set({ appearanceDraft: { ...draft, folderSizeAll: boundedPercent(value, 50) } });
      },
      setDraftFolderSize: (id, value) => {
        const draft = get().appearanceDraft ?? blankDraft(get());
        const folderSizes = { ...draft.folderSizes };
        if (id.trim()) {
          if (value === null) delete folderSizes[id];
          else folderSizes[id] = boundedPercent(value, 50);
        }
        set({ appearanceDraft: { ...draft, folderSizes } });
      },
      setDraftFolderLayout: (folderLayout) => {
        if (!["grid", "big", "single"].includes(folderLayout)) return;
        const draft = get().appearanceDraft ?? blankDraft(get());
        set({ appearanceDraft: { ...draft, folderLayout } });
      },
      setDraftFolderIconStyle: (id, value) => {
        if (!id.trim() || !["3d", "flat", "etched"].includes(value)) return;
        const draft = get().appearanceDraft ?? blankDraft(get());
        set({ appearanceDraft: { ...draft, folderIconStyle: { ...draft.folderIconStyle, [id]: value } } });
      },
      setDraftFolderName: (id, value) => {
        const name = value.trim();
        if (!id.trim() || name.length < 1 || name.length > 24) return false;
        const draft = get().appearanceDraft ?? blankDraft(get());
        set({ appearanceDraft: { ...draft, folderNames: { ...draft.folderNames, [id]: name } } });
        return true;
      },
      setDraftFolderOrder: (ids) => {
        const draft = get().appearanceDraft ?? blankDraft(get());
        set({ appearanceDraft: { ...draft, folderOrder: cleanFolderOrder(ids) } });
      },
      applyAppearance: () => {
        const draft = get().appearanceDraft;
        if (!draft) return { ok: true };
        const state = get();
        const look = previewPalette(draft, Date.now(), state.timeZoneLocked ? state.timeZoneId : null);
        if (!canApplyAppearance(draft.palette) || !canApplyAppearance(look)) {
          return { ok: false, reason: "This look would hide the clock or controls." };
        }
        if (draft.scheduleEnabled && ![draft.scheduleLight, draft.scheduleDark].every((time) => /^([01]\d|2[0-3]):[0-5]\d$/.test(time))) {
          return { ok: false, reason: "Enter valid light and dark start times." };
        }
        for (const theme of ["dark", "light"] as const) {
          const numberColor = draft.numberColor[theme];
          if (!numberColor) continue;
          const result = checkNumberColor(numberColor, theme, numberColorContext(state, theme, draft));
          if (!result.ok) return { ok: false, reason: result.message };
        }
        if (typeof window !== "undefined" && draft.digitFont !== state.digitFont && faceLoadState(draft.digitFont) !== "loaded") {
          return { ok: false, reason: "The selected font has not loaded. Retry it or keep the current font." };
        }
        set({
          appearanceMode: draft.appearanceMode,
          scheduleEnabled: draft.scheduleEnabled,
          scheduleLight: draft.scheduleLight,
          scheduleDark: draft.scheduleDark,
          glowMode: draft.glowMode,
          floorId: draft.floorId,
          floorCustom: draft.floorCustom,
          customPalette: draft.palette.id === "custom" ? draft.palette : null,
          paletteId: draft.palette.id === "custom" ? "custom" : draft.palette.id,
          glowIntensity: boundedPercent(draft.glowIntensity),
          houseOpacity: boundedPercent(draft.houseOpacity),
          digitFont: faceById(draft.digitFont).id,
          digitScale: boundedPercent(draft.digitScale),
          laserEnabled: draft.laserEnabled,
          laserIntensity: boundedPercent(draft.laserIntensity, 100),
          laserColor: isOpaqueHex(draft.laserColor) ?? state.laserColor,
          numberColor: {
            dark: draft.numberColor.dark && checkNumberColor(draft.numberColor.dark, "dark", numberColorContext(state, "dark", draft)).ok
              ? normalizeHex(draft.numberColor.dark)
              : null,
            light: draft.numberColor.light && checkNumberColor(draft.numberColor.light, "light", numberColorContext(state, "light", draft)).ok
              ? normalizeHex(draft.numberColor.light)
              : null,
          },
          folderSizeAll: boundedPercent(draft.folderSizeAll, 50),
          folderSizes: Object.fromEntries(
            Object.entries(draft.folderSizes).map(([id, value]) => [id, boundedPercent(value, 50)]),
          ),
          folderLayout: draft.folderLayout,
          folderIconStyle: { ...draft.folderIconStyle },
          folderNames: { ...draft.folderNames },
          folderOrder: cleanFolderOrder(draft.folderOrder),
          appearanceDraft: null,
        });
        return { ok: true };
      },
      discardAppearance: () => set({ appearanceDraft: null }),
      restoreReadableAppearance: () =>
        set({
          customPalette: null,
          paletteId: SAFE_DARK.id,
          appearanceDraft: null,
          glowIntensity: 36,
          houseOpacity: 55,
          appearanceMode: "system",
          scheduleEnabled: false,
          digitFont: HOUSE_FACE_ID,
          digitScale: 50,
          numberColor: { dark: null, light: null },
          floorId: "none",
          glowMode: false,
        }),
      makeDraftReadable: () => {
        const draft = get().appearanceDraft ?? blankDraft(get());
        set({ appearanceDraft: { ...draft, palette: makeReadable(draft.palette) } });
      },
      openSession: () => set({ shell: "home", resumeShell: "home", openModuleId: null, previewModuleId: null, houseOpen: false }),
      closeSession: () => set({ shell: "closed", resumeShell: "session", openModuleId: null, previewModuleId: null, houseOpen: false }),
      goHome: () => set({ shell: "home", resumeShell: "home", openModuleId: null, previewModuleId: null, houseOpen: false }),
      toSession: () => set({ shell: "session", resumeShell: "session", openModuleId: null, previewModuleId: null, houseOpen: false }),
      minimizeShell: () => {
        const face = get().shell;
        const resume = face === "minimized" || face === "closed" ? get().resumeShell : face;
        set({ shell: "minimized", resumeShell: resume });
      },
      restoreShell: () => {
        const resume = get().resumeShell || "home";
        set({ shell: resume });
      },
      openTool: (mode) => {
        if (mode === "timer" || mode === "stopwatch" || mode === "alarm") {
          get().enterModule(mode);
          return;
        }
        get().enterModule("timer");
      },
      enterModule: (id) => {
        const installed = withFreeModules(get().installedModules);
        if (!installed.includes(id)) {
          set({ previewModuleId: id });
          return;
        }
        const patch: Partial<AppState> = { openModuleId: id, previewModuleId: null, houseOpen: false, shell: "home", resumeShell: "home" };
        if (id === "timer" || id === "stopwatch" || id === "alarm") patch.mode = id;
        set(patch);
      },
      leaveModule: () => set({ openModuleId: null, previewModuleId: null }),
      previewModule: (id) => set({ previewModuleId: id }),
    }),
    {
      name: "auric-crowned-v21",
      storage: createJSONStorage(() => auricStorage),
      skipHydration: true,
      partialize: (s) => ({
        mode: s.mode,
        hours: s.hours,
        minutes: s.minutes,
        seconds: s.seconds,
        years: s.years,
        months: s.months,
        weeks: s.weeks,
        days: s.days,
        cs: s.cs,
        timerStatus: s.timerStatus === "done" ? "idle" : s.timerStatus,
        timerEndsAt: s.timerEndsAt,
        timerPausedRemaining: s.timerPausedRemaining,
        timerStartedAt: s.timerStartedAt,
        swStatus: s.swStatus,
        swStartedAt: s.swStartedAt,
        swAccumulated: s.swAccumulated,
        swLaps: s.swLaps,
        paletteId: s.paletteId === "ice-whisper" ? "blue-diamond" : s.paletteId,
        customPalette: s.customPalette,
        glowIntensity: s.glowIntensity,
        glowMode: s.glowMode,
        digitFont: s.digitFont,
        reelSound: s.reelSound,
        alarmSound: s.alarmSound,
        stopwatchSound: s.stopwatchSound,
        iconSound: s.iconSound,
        settingsSound: s.settingsSound,
        heartbeatSound: s.heartbeatSound,
        notices: s.notices,
        analogFace: s.analogFace,
        isCrowned: s.isCrowned,
        pinHash: s.pinHash,
        pinLength: s.pinLength,
        controlLock: s.controlLock,
        sessions: s.sessions,
        savedTimers: s.savedTimers,
        alarms: s.alarms,
        ringingAlarm: s.ringingAlarm,
        tasks: s.tasks,
        heartbeatMs: s.heartbeatMs,
        controlId: s.controlId,
        digitScale: s.digitScale,
        houseOpacity: s.houseOpacity,
        floorId: s.floorId,
        floorCustom: s.floorCustom,
        houseFace: s.houseFace,
        directoryMode: s.directoryMode,
        orbHex: s.orbHex,
        vaultSort: s.vaultSort,
        exportEnabled: s.exportEnabled,
        sectionOrder: s.sectionOrder,
        heartbeatEnabled: s.heartbeatEnabled,
        lastBeatAt: s.lastBeatAt,
        heartbeatIndex: s.heartbeatIndex,
        heartbeatReminder: s.heartbeatReminder,
        heartbeatLog: s.heartbeatLog,
        rexOn: s.rexOn,
        callName: s.callName,
        archivedFolders: s.archivedFolders,
        floatingVisible: s.floatingVisible,
        floatingX: s.floatingX,
        floatingY: s.floatingY,
        floatingUsed: s.floatingUsed,
        laserEnabled: s.laserEnabled,
        laserIntensity: s.laserIntensity,
        laserColor: s.laserColor,
        numberColor: s.numberColor,
        folderSizeAll: s.folderSizeAll,
        folderSizes: s.folderSizes,
        folderLayout: s.folderLayout,
        folderIconStyle: s.folderIconStyle,
        folderNames: s.folderNames,
        folderOrder: s.folderOrder,
        onboardingDone: s.onboardingDone,
        literacyIndex: s.literacyIndex,
        culetFound: s.culetFound,
        reelTicks: s.reelTicks,
        unlocked: s.unlocked,
        streakInsuranceUsed: s.streakInsuranceUsed,
        appearanceMode: s.appearanceMode,
        scheduleEnabled: s.scheduleEnabled,
        scheduleLight: s.scheduleLight,
        scheduleDark: s.scheduleDark,
        hourCycle: s.hourCycle,
        timeZoneLocked: s.timeZoneLocked,
        timeZoneId: s.timeZoneId,
        hapticEnabled: s.hapticEnabled,
        soundEnabled: s.soundEnabled,
        deviceBatteryOn: s.deviceBatteryOn,
        deviceNetworkOn: s.deviceNetworkOn,
        shell: s.shell === "minimized" || s.shell === "closed" ? "session" : s.shell === "tool" ? "home" : s.shell,
        resumeShell: s.resumeShell === "tool" ? "home" : s.resumeShell,
        installedModules: withFreeModules(s.installedModules),
      }),
      merge: (persistedState, currentState) => {
        const persisted = persistedState && typeof persistedState === "object"
          ? persistedState as Partial<AppState>
          : {};
        const next = { ...currentState, ...persisted };
        const colorState = next as AppState;
        const candidateNumbers = persisted.numberColor && typeof persisted.numberColor === "object"
          ? persisted.numberColor
          : currentState.numberColor;
        const numberColor: NumberColor = { dark: null, light: null };
        let invalidNumberColor = false;
        for (const theme of ["dark", "light"] as const) {
          const value = candidateNumbers[theme];
          if (value === null || value === undefined) continue;
          const color = typeof value === "string" ? isOpaqueHex(value) : null;
          if (color && checkNumberColor(color, theme, numberColorContext(colorState, theme)).ok) {
            numberColor[theme] = color;
          } else {
            invalidNumberColor = true;
          }
        }
        const folderSizes = persisted.folderSizes && typeof persisted.folderSizes === "object"
          ? Object.fromEntries(
              Object.entries(persisted.folderSizes).filter(([id]) => id.trim()).map(([id, value]) => [
                id,
                boundedPercent(value, 50),
              ]),
            )
          : currentState.folderSizes;
        const folderIconStyle = persisted.folderIconStyle && typeof persisted.folderIconStyle === "object"
          ? Object.fromEntries(Object.entries(persisted.folderIconStyle).filter((entry): entry is [string, FolderIconStyle] =>
              ["3d", "flat", "etched"].includes(entry[1] as string),
            ))
          : currentState.folderIconStyle;
        const folderNames = persisted.folderNames && typeof persisted.folderNames === "object"
          ? Object.fromEntries(Object.entries(persisted.folderNames).flatMap(([id, value]) => {
              const name = typeof value === "string" ? value.trim() : "";
              return id.trim() && name.length >= 1 && name.length <= 24 ? [[id, name]] : [];
            }))
          : currentState.folderNames;
        return {
          ...next,
          laserEnabled: typeof persisted.laserEnabled === "boolean" ? persisted.laserEnabled : currentState.laserEnabled,
          laserIntensity: boundedPercent(persisted.laserIntensity ?? currentState.laserIntensity, 100),
          laserColor: isOpaqueHex(persisted.laserColor ?? currentState.laserColor) ?? currentState.laserColor,
          numberColor,
          folderSizeAll: boundedPercent(persisted.folderSizeAll ?? currentState.folderSizeAll, 50),
          folderSizes,
          folderLayout: ["grid", "big", "single"].includes(persisted.folderLayout as string)
            ? persisted.folderLayout!
            : currentState.folderLayout,
          folderIconStyle,
          folderNames,
          folderOrder: cleanFolderOrder(persisted.folderOrder ?? currentState.folderOrder),
          toasts: invalidNumberColor
            ? [...next.toasts, { id: "number-color-hydration-warning", title: "Number color reset", body: "A saved number color no longer meets the contrast guard and was reset to the palette digit.", tone: "mute" as const }]
            : next.toasts,
        };
      },
    },
  ),
);

declare global {
  interface Window {
    __auric?: typeof useAuric;
  }
}

if (typeof window !== "undefined") {
  window.__auric = useAuric;
}

export function selectPalette(s: AppState): Palette {
  return committedPalette({
    paletteId: s.paletteId,
    customPalette: s.customPalette,
    mode: s.appearanceMode ?? "system",
    scheduleEnabled: s.scheduleEnabled ?? false,
    lightStart: s.scheduleLight ?? "07:00",
    darkStart: s.scheduleDark ?? "19:00",
    nowMs: Number.isFinite(s.now) && s.now > 0 ? s.now : 0,
    zone: s.timeZoneLocked ? s.timeZoneId : null,
  });
}

export function remainingMs(s: AppState, now = Date.now()): number {
  if (s.timerStatus === "running" && s.timerEndsAt) return Math.max(0, s.timerEndsAt - now);
  if (s.timerStatus === "paused") return s.timerPausedRemaining;
  if (s.timerStatus === "done") return 0;
  return msFromSpan(s);
}

export function stopwatchMs(s: AppState, now = Date.now()): number {
  if (s.swStatus === "running" && s.swStartedAt) return s.swAccumulated + (now - s.swStartedAt);
  return s.swAccumulated;
}
