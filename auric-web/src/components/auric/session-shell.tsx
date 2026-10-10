import { useEffect, useRef, useState } from "react";
import { FolderGrid } from "@/components/auric/folder-grid";
import { FolderLayoutSheet } from "@/components/auric/folder-layout-sheet";
import { FolderStage } from "@/components/auric/folder-stage";
import {
  AuricWordmark,
  CountRail,
  Scene,
  SessionActions,
  StatusLine,
  TaskbarPill,
  TxPanel,
} from "@/components/auric/termux-chrome";
import { DEFAULT_FOLDER_PREFERENCES, normalizeFolderPreferences } from "@/lib/folder-layout";
import { FREE_MODULE_IDS, MODULES, moduleById } from "@/lib/modules";
import { unlockAudio } from "@/lib/audio";
import { cn } from "@/lib/utils";
import { useAuric } from "@/lib/store";

export function SessionShell() {
  const shell = useAuric((s) => s.shell);
  const notices = useAuric((s) => s.notices.filter((n) => !n.read && !n.archived).length);

  if (shell === "minimized") {
    return (
      <div className="tx-desk tx-desk-min">
        <TaskbarPill />
      </div>
    );
  }

  if (shell === "closed") {
    return (
      <div className="tx-desk tx-desk-session" onClick={() => unlockAudio()}>
        <AuricWordmark />
        <Scene />
        <TxPanel
          edge="red"
          className="tx-panel-hero"
          footer={<SessionActions outlined primary="OPEN SESSION" onPrimary={() => useAuric.getState().openSession()} />}
        >
          <StatusLine alerts={notices} />
          <h1 className="tx-heading">SESSION ENDED</h1>
          <p className="tx-prompt">~ $ logout</p>
          <p className="tx-ok">[ok] closed</p>
        </TxPanel>
      </div>
    );
  }

  if (shell === "session") {
    return <SessionScreen alerts={notices} />;
  }

  return <HomeDesk alerts={notices} />;
}

const SESSION_HEADING = "TERMUX SESSION";
const TYPE_MS = 18;
const PKG_TICK_MS = 70;
const PKG_STEPS = 5;
const FOLDER_PREFERENCES_KEY = "auric-folder-settings-v1";

function readFolderPreferences() {
  try {
    const saved = window.localStorage.getItem(FOLDER_PREFERENCES_KEY);
    return normalizeFolderPreferences(saved ? JSON.parse(saved) : null, MODULES.map((module) => module.id));
  } catch {
    return normalizeFolderPreferences(null, MODULES.map((module) => module.id));
  }
}

const SESSION_PACKAGES = [
  { id: "core", tag: "[core]", name: "time engine", done: "installed" },
  { id: "terminal", tag: "[terminal]", name: "session manager", done: "installed" },
  { id: "folders", tag: "[folders]", name: "module store", done: "ready" },
] as const;

function prefersReducedMotion() {
  return typeof window !== "undefined" && window.matchMedia("(prefers-reduced-motion: reduce)").matches;
}

function SessionScreen({ alerts }: { alerts: number }) {
  const [typed, setTyped] = useState(0);
  const [percents, setPercents] = useState(() => SESSION_PACKAGES.map(() => 0));

  useEffect(() => {
    if (prefersReducedMotion()) {
      setTyped(SESSION_HEADING.length);
      setPercents(SESSION_PACKAGES.map(() => 100));
      return;
    }

    const timers: number[] = [];
    const later = (ms: number, fn: () => void) => {
      timers.push(window.setTimeout(fn, ms));
    };

    for (let i = 1; i <= SESSION_HEADING.length; i++) {
      later(i * TYPE_MS, () => setTyped(i));
    }

    const installAt = SESSION_HEADING.length * TYPE_MS + 40;
    SESSION_PACKAGES.forEach((_, pkgIdx) => {
      for (let step = 1; step <= PKG_STEPS; step++) {
        const delay = installAt + pkgIdx * PKG_STEPS * PKG_TICK_MS + (step - 1) * PKG_TICK_MS;
        later(delay, () => {
          setPercents((prev) => {
            const next = prev.slice();
            next[pkgIdx] = Math.min(100, Math.round((step * 100) / PKG_STEPS));
            return next;
          });
        });
      }
    });

    return () => {
      timers.forEach((id) => window.clearTimeout(id));
    };
  }, []);

  const headingDone = typed >= SESSION_HEADING.length;
  const bootDone = headingDone && percents.every((p) => p >= 100);

  return (
    <div className="tx-desk tx-desk-session" onClick={() => unlockAudio()}>
      <AuricWordmark />
      <Scene />
      <TxPanel
        className="tx-panel-hero"
        footer={
          <SessionActions
            outlined
            primary="OPEN SESSION"
            onPrimary={() => useAuric.getState().openSession()}
            onClose={() => useAuric.getState().closeSession()}
            onMinimize={() => useAuric.getState().minimizeShell()}
          />
        }
      >
        <StatusLine alerts={alerts} />
        <TypeHeading text={SESSION_HEADING} count={typed} />
        {headingDone ? <p className="tx-prompt">~ $ auric --status</p> : null}
        {headingDone ? <PkgInstall percents={percents} /> : null}
        {bootDone ? <BootDone /> : null}
        {bootDone ? <NeuralCore /> : null}
      </TxPanel>
    </div>
  );
}

function TypeHeading({ text, count }: { text: string; count: number }) {
  const done = count >= text.length;
  return (
    <h1 className="tx-heading" aria-label={text}>
      {text.slice(0, count)}
      {done ? null : <span className="tx-type-caret" aria-hidden />}
    </h1>
  );
}

function PkgInstall({ percents }: { percents: number[] }) {
  return (
    <div className="tx-install">
      {SESSION_PACKAGES.map((pkg, i) => {
        const p = percents[i] ?? 0;
        const state = p >= 100 ? pkg.done : p > 0 ? "installing" : "queued";
        return (
          <div key={pkg.id}>
            <div className="tx-install-row">
              <span className="tx-pkg">{pkg.tag}</span>
              <span className="tx-install-name">{pkg.name}</span>
              <span className="tx-install-state">{state}</span>
            </div>
            {p > 0 ? <CountRail percent={p} install label={pkg.name} /> : null}
          </div>
        );
      })}
    </div>
  );
}

function BootDone() {
  return (
    <>
      <p className="tx-ok">clock synced - engine online</p>
      <pre className="tx-pkg">{`history:
  sessions kept on this device
  no accounts - no cloud - no tracking`}</pre>
    </>
  );
}

function NeuralCore() {
  return (
    <div className="tx-neural">
      <p className="tx-neural-title">[AURIC NEURAL CORE]</p>
      <p className="tx-neural-body">Status: Active • Chicago clock linked • Secure session initialized.</p>
    </div>
  );
}

function HomeDesk({ alerts }: { alerts: number }) {
  const openModuleId = useAuric((s) => s.openModuleId);
  const previewModuleId = useAuric((s) => s.previewModuleId);
  const stacked = Boolean(openModuleId || previewModuleId);
  const [folderPreferences, setFolderPreferences] = useState(DEFAULT_FOLDER_PREFERENCES);
  const [folderSettingsOpen, setFolderSettingsOpen] = useState(false);
  const [folderPreferencesReady, setFolderPreferencesReady] = useState(false);
  const folderBeforeRects = useRef(new Map<string, DOMRect>());

  const updateFolderPreferences = (next: typeof folderPreferences) => {
    if (
      next.layout !== folderPreferences.layout ||
      next.folderOrder.join("\u0000") !== folderPreferences.folderOrder.join("\u0000")
    ) {
      const before = new Map<string, DOMRect>();
      document.querySelectorAll<HTMLElement>(".tx-folder-grid > li[data-folder-id]").forEach((item) => {
        const id = item.dataset.folderId;
        const button = item.querySelector<HTMLButtonElement>(".tx-folder-tile");
        if (id && button) before.set(id, button.getBoundingClientRect());
      });
      folderBeforeRects.current = before;
    }
    setFolderPreferences(next);
  };

  useEffect(() => {
    setFolderPreferences(readFolderPreferences());
    setFolderPreferencesReady(true);
  }, []);

  useEffect(() => {
    if (!folderPreferencesReady) return;
    try {
      window.localStorage.setItem(FOLDER_PREFERENCES_KEY, JSON.stringify(folderPreferences));
    } catch {
      // Folder settings remain usable for the current session if storage is unavailable.
    }
  }, [folderPreferences, folderPreferencesReady]);

  return (
    <div className="tx-desk" onClick={() => unlockAudio()}>
      <AuricWordmark />
      <Scene />
      <div className="tx-stack">
        <div className={cn("tx-back-layer", stacked && "is-dim")}>
          <TxPanel
            stacked={stacked}
            className="tx-panel-home"
            footer={
              stacked ? null : (
                <SessionActions
                  primary="HOME"
                  onPrimary={() => useAuric.getState().toSession()}
                  onClose={() => useAuric.getState().closeSession()}
                  onMinimize={() => useAuric.getState().minimizeShell()}
                />
              )
            }
          >
            <StatusLine alerts={alerts} />
            <BootLog />
            <HelpBlock />
            <div className="tx-folder-command">
              <p className="tx-prompt">~ $ ls folders</p>
              <button
                type="button"
                className="tx-folder-menu-button"
                aria-label="Folder layout settings"
                aria-haspopup="dialog"
                aria-expanded={folderSettingsOpen}
                onClick={() => setFolderSettingsOpen(true)}
              >
                ⋯
              </button>
            </div>
            <FolderGrid
              preferences={folderPreferences}
              beforeRects={folderBeforeRects}
              onChangePreferences={updateFolderPreferences}
              compositorArmed={folderSettingsOpen}
            />
            <FolderLayoutSheet
              open={folderSettingsOpen}
              specs={MODULES}
              preferences={folderPreferences}
              onChange={updateFolderPreferences}
              onClose={() => setFolderSettingsOpen(false)}
            />
          </TxPanel>
        </div>
        {openModuleId ? (
          <div className="tx-front-layer">
            <div className="tx-fan-stage" aria-hidden>
              <span className="tx-ghost-card is-3" />
              <span className="tx-ghost-card is-2" />
              <span className="tx-ghost-card is-1" />
            </div>
            <TxPanel
              className="tx-panel-front tx-fan-card"
              footer={
                <SessionActions
                  primary="HOME"
                  onPrimary={() => useAuric.getState().leaveModule()}
                  onClose={() => useAuric.getState().closeSession()}
                  onMinimize={() => useAuric.getState().minimizeShell()}
                />
              }
            >
              <StatusLine alerts={alerts} />
              <FolderStage id={openModuleId} />
            </TxPanel>
          </div>
        ) : null}
        {previewModuleId ? <PreviewSheet id={previewModuleId} /> : null}
      </div>
    </div>
  );
}

function BootLog() {
  const folders = FREE_MODULE_IDS.length;
  return (
    <div className="tx-boot">
      <p className="tx-ok">[ok] session started · folders {folders} · no accounts</p>
      <p className="tx-ok">[ok] timer stopwatch alarm</p>
    </div>
  );
}

function HelpBlock() {
  return (
    <div className="tx-help">
      <p className="tx-prompt">~ $ auric --help</p>
      <p className="tx-ink">Folders: timer, stopwatch, alarm, heartbeat…</p>
    </div>
  );
}

function PreviewSheet({ id }: { id: string }) {
  const spec = moduleById(id);
  const [step, setStep] = useState<"look" | "buy">("look");
  if (!spec) return null;
  return (
    <div className="tx-preview" role="dialog" aria-labelledby="tx-preview-title">
      <TxPanel className="tx-panel-front" solid>
        <p className="tx-prompt">~ $ auric preview {spec.pkg}</p>
        <h2 id="tx-preview-title" className="tx-heading">
          {spec.name}
        </h2>
        {step === "look" ? (
          <>
            <p className="tx-muted">{spec.preview}</p>
            <p className="tx-ok">unlit — Crown module. Preview before price.</p>
            <FolderStage id={spec.id} preview />
            <div className="tx-action-row" style={{ marginTop: 16 }}>
              <button type="button" className="tx-btn tx-btn-open" onClick={() => setStep("buy")}>
                CONTINUE
              </button>
              <button type="button" className="tx-btn tx-btn-min" onClick={() => useAuric.getState().previewModule(null)}>
                close
              </button>
            </div>
          </>
        ) : (
          <>
            <p className="tx-warn">locked — Crown module. Billing is not live on this web demo.</p>
            <p className="tx-muted">Kai owns purchase on the APK. Nothing is charged here.</p>
            <button type="button" className="tx-btn tx-btn-open" onClick={() => useAuric.getState().previewModule(null)}>
              close
            </button>
          </>
        )}
      </TxPanel>
    </div>
  );
}
