import { useMemo, useState } from "react";
import { DrumReel } from "@/components/auric/drum-reel";
import { GlassBell } from "@/components/auric/termux-chrome";
import { playClick, reelFeel } from "@/lib/audio";
import { useAuric } from "@/lib/store";
import type { NoticeKind } from "@/lib/types";
import { cn } from "@/lib/utils";

const ACTIONS = ["Keep", "Archive", "Delete"] as const;
const DAY = 86_400_000;
const FOLDERS: { kind: NoticeKind; name: string; hint: string }[] = [
  { kind: "heartbeat", name: "HEARTBEAT", hint: "Pulse log action" },
  { kind: "timer", name: "TIMER", hint: "Countdowns that finished" },
  { kind: "alarm", name: "ALARM", hint: "Rings while Auric is open" },
  { kind: "stopwatch", name: "STOPWATCH", hint: "Laps you kept" },
];

function remainLabel(at: number, now: number) {
  const left = Math.max(0, DAY - (now - at));
  const h = Math.floor(left / 3_600_000);
  const m = Math.floor((left % 3_600_000) / 60_000);
  if (left <= 0) return "Archiving";
  return `${h}h ${m}m to archive`;
}

function stamp(at: number) {
  return new Date(at).toLocaleString(undefined, { month: "short", day: "numeric", hour: "numeric", minute: "2-digit" });
}

function FolderGlyph({ id, hot }: { id: string; hot?: boolean }) {
  const rim = hot ? "#e8102a" : "#f4fbff";
  const fill = hot ? "#ff6b7a" : "#e8eef4";
  const glow = hot ? "#5a0810" : "#9aa6b0";
  const gid = `aglass-${id}`;
  return (
    <svg viewBox="0 0 32 28" width="40" height="36" className="shrink-0" aria-hidden>
      <defs>
        <linearGradient id={gid} x1="0" y1="0" x2="0.15" y2="1">
          <stop offset="0%" stopColor={fill} stopOpacity={hot ? 0.95 : 0.85} />
          <stop offset="42%" stopColor={rim} stopOpacity={hot ? 0.72 : 0.45} />
          <stop offset="100%" stopColor={glow} stopOpacity={hot ? 0.9 : 0.7} />
        </linearGradient>
      </defs>
      <ellipse cx="16" cy="26.4" rx="10" ry="1.35" fill={hot ? "#ff6b7a" : "#9aa6b0"} opacity={hot ? 0.42 : 0.18} />
      <path
        d="M2.4 9.4 C2.4 7.2 4.4 6 7.2 6 H14.2 C15.8 6 16.8 7 17.6 8 H28.2 C30.2 8 31.2 9.2 31.2 11.2 V22.4 C31.2 24.5 29.4 26 27 26 H5.4 C3.2 26 2.2 24.5 2.2 22.4 Z"
        fill={`url(#${gid})`}
        stroke={rim}
        strokeWidth="1.15"
      />
      <path
        d="M3.4 9.1 L12.8 3.7 H20.4 L16.2 9.1 Z"
        fill={glow}
        stroke={rim}
        strokeWidth="1"
      />
      <path d="M5 12 H26" stroke={hot ? "#ff6b7a" : "#f4fbff"} strokeWidth="0.7" opacity={hot ? 0.65 : 0.35} />
    </svg>
  );
}

export function NoticeBell() {
  const notices = useAuric((s) => s.notices);
  const unread = notices.filter((n) => !n.read && !n.archived).length;
  return <GlassBell count={unread} />;
}

export function NoticeSheet() {
  const open = useAuric((s) => s.noticeOpen);
  const notices = useAuric((s) => s.notices);
  const archivedFolders = useAuric((s) => s.archivedFolders);
  const now = useAuric((s) => s.now) || Date.now();
  const [folder, setFolder] = useState<NoticeKind | null>(null);
  const [picked, setPicked] = useState<string | null>(null);
  const [folderAct, setFolderAct] = useState(false);
  const [act, setAct] = useState(0);

  const ranked = useMemo(() => {
    const rows = FOLDERS.map((f) => {
      const items = notices.filter((n) => n.kind === f.kind);
      const latest = items.reduce((m, n) => Math.max(m, n.at), 0);
      const unread = items.filter((n) => !n.read && !n.archived).length;
      const live = items.filter((n) => !n.archived);
      return { ...f, latest, unread, live, items, archived: archivedFolders.includes(f.kind) };
    });
    const rest = rows.filter((r) => r.kind !== "heartbeat");
    rest.sort((a, b) => b.latest - a.latest);
    const heart = rows.find((r) => r.kind === "heartbeat");
    return heart ? [heart, ...rest] : rest;
  }, [notices, archivedFolders]);

  if (!open) return null;

  const current = ranked.find((r) => r.kind === folder);
  const focus = notices.find((n) => n.id === picked);
  const unread = notices.filter((n) => !n.read && !n.archived).length;

  function back() {
    playClick();
    if (picked) {
      setPicked(null);
      return;
    }
    if (folderAct) {
      setFolderAct(false);
      return;
    }
    if (folder) {
      setFolder(null);
      return;
    }
    useAuric.getState().setNoticeOpen(false);
  }

  function closeSheet() {
    playClick();
    useAuric.getState().setNoticeOpen(false);
  }

  const title = picked ? "ALERT" : folderAct ? "FOLDER" : folder ? current?.name ?? "FOLDER" : "NOTIFICATIONS";

  return (
    <div className="tx-alerts-layer flex flex-col" role="dialog" aria-modal="true" aria-label="Alerts">
      <div className="unfurl flex min-h-0 flex-1 flex-col overflow-hidden px-4 pb-5 pt-2">
        <div className="tx-notice-bar">
          <button type="button" className="tx-back-pill" onClick={back} aria-label="Back">
            ← BACK
          </button>
          <h2 className="tx-notice-title">{title}</h2>
          <button type="button" className="tx-notice-orb" onClick={closeSheet} aria-label="Minimize alerts">
            −
          </button>
          <button type="button" className="tx-notice-orb is-close" onClick={closeSheet} aria-label="Close alerts">
            ×
          </button>
          <GlassBell count={unread} />
        </div>

        <div className="min-h-0 flex-1 overflow-y-auto pb-6 pt-3">
          {focus ? (
            <ItemWheel
              nTitle={focus.title}
              body={focus.body}
              at={focus.at}
              now={now}
              act={act}
              setAct={setAct}
              onConfirm={() => {
                playClick();
                if (act === 1) useAuric.getState().archiveNotice(focus.id);
                if (act === 2) useAuric.getState().deleteNotice(focus.id);
                setPicked(null);
                setAct(0);
              }}
            />
          ) : folderAct && folder ? (
            <ItemWheel
              nTitle={current?.name ?? "FOLDER"}
              body="Does not remove the feature. Only the alerts inside."
              at={now}
              now={now}
              hideBar
              act={act}
              setAct={setAct}
              onConfirm={() => {
                playClick();
                if (act === 0) useAuric.getState().keepFolder(folder);
                if (act === 1) useAuric.getState().archiveFolder(folder);
                if (act === 2) useAuric.getState().deleteFolderNotices(folder);
                setFolderAct(false);
                setAct(0);
              }}
            />
          ) : folder && current ? (
            <>
              <button
                type="button"
                className="tx-btn tx-btn-min"
                onClick={() => {
                  playClick();
                  setAct(0);
                  setFolderAct(true);
                }}
              >
                Keep / Archive / Delete folder
              </button>
              <ul className="tx-notice-list">
                {current.live.length === 0 ? (
                  <li className="tx-notice-row">
                    <p className="tx-muted">Empty</p>
                  </li>
                ) : (
                  current.live.map((n) => (
                    <li key={n.id}>
                      <button
                        type="button"
                        className="tx-notice-row"
                        onClick={() => {
                          playClick();
                          setPicked(n.id);
                          setAct(0);
                        }}
                      >
                        <FolderGlyph id={n.id} hot={false} />
                        <span className="min-w-0">
                          <span className="tx-folder-name block">{n.title}</span>
                          <span className="tx-muted block" style={{ fontSize: 12 }}>
                            {n.body}
                          </span>
                        </span>
                        <span className="tx-folder-chevron">›</span>
                      </button>
                    </li>
                  ))
                )}
              </ul>
            </>
          ) : (
            <ul className="tx-notice-list">
              {ranked.map((f) => {
                const hot = f.unread > 0 && !f.archived;
                const meta = f.archived ? "Archived folder" : f.latest ? stamp(f.latest) : f.hint;
                return (
                  <li key={f.kind}>
                    <button
                      type="button"
                      className={cn("tx-notice-row", hot ? "is-hot" : "is-idle", f.archived && "opacity-60")}
                      onClick={() => {
                        playClick();
                        useAuric.getState().markKindRead(f.kind);
                        setFolder(f.kind);
                      }}
                    >
                      <FolderGlyph id={f.kind} hot={hot} />
                      <span className="min-w-0">
                        <span className={cn("tx-folder-name block", hot && "is-on")}>
                          {f.name}
                        </span>
                        <span className="tx-muted block" style={{ fontSize: 12 }}>
                          {f.unread ? `${f.unread} new · ${meta}` : meta}
                        </span>
                      </span>
                      <span className="tx-folder-chevron">›</span>
                    </button>
                  </li>
                );
              })}
            </ul>
          )}
        </div>
      </div>
    </div>
  );
}

function ItemWheel({
  nTitle,
  body,
  at,
  now,
  act,
  setAct,
  onConfirm,
  hideBar,
}: {
  nTitle: string;
  body: string;
  at: number;
  now: number;
  act: number;
  setAct: (n: number) => void;
  onConfirm: () => void;
  hideBar?: boolean;
}) {
  return (
    <div>
      <p className="tx-ok">{nTitle}</p>
      <p className="tx-ink mt-1">{body}</p>
      {!hideBar ? (
        <>
          <p className="tx-muted mt-2">{remainLabel(at, now)}</p>
          <div className="mt-2 h-1 overflow-hidden rounded-full" style={{ background: "rgba(0, 0, 0, 0.45)" }}>
            <div
              className="h-full"
              style={{
                width: `${Math.max(0, Math.min(100, ((DAY - (now - at)) / DAY) * 100))}%`,
                background: "var(--tx-green, #3be87b)",
              }}
            />
          </div>
        </>
      ) : null}
      <div className="mt-5 flex justify-center">
        <DrumReel size="plaque" count={3} value={act} labels={[...ACTIONS]} pad={0} onChange={setAct} onTick={() => reelFeel()} />
      </div>
      <p className="tx-heading mt-3 text-center">{ACTIONS[act]}</p>
      <button type="button" className="tx-btn tx-btn-open mt-4 w-full" onClick={onConfirm}>
        CONFIRM
      </button>
    </div>
  );
}
