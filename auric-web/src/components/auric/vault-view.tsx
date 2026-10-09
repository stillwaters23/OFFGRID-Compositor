import { useEffect, useMemo, useRef, useState, type ReactNode } from "react";
import { ChevronDown, ChevronUp, Download, Search, Trash2 } from "lucide-react";
import { Bar, BarChart, ResponsiveContainer, Tooltip, XAxis, YAxis } from "recharts";
import { PitButton } from "@/components/auric/pit-button";
import { OrbToggle } from "@/components/auric/switch";
import { ACHIEVEMENTS, computeStreak } from "@/lib/achievements";
import { dayKey, formatDay, formatDurationPretty, formatTime } from "@/lib/format";
import { useAuric } from "@/lib/store";
import type { Session, VaultSectionId, VaultSort } from "@/lib/types";
import { sessionsCsv } from "@/lib/export";
import { cn, downloadText } from "@/lib/utils";

const SORTS: { id: VaultSort; label: string }[] = [
  { id: "az", label: "A–Z" },
  { id: "recent", label: "Recent" },
  { id: "used", label: "Most-used" },
  { id: "duration", label: "Duration" },
];

function sortSessions(sessions: Session[], sort: VaultSort): Session[] {
  const copy = [...sessions];
  if (sort === "az") return copy.sort((a, b) => (a.note || "zzz").localeCompare(b.note || "zzz"));
  if (sort === "used") return copy.sort((a, b) => b.usedCount - a.usedCount || b.endedAt - a.endedAt);
  if (sort === "duration") return copy.sort((a, b) => b.durationMs - a.durationMs);
  return copy.sort((a, b) => b.endedAt - a.endedAt);
}

export function VaultView() {
  const sessions = useAuric((s) => s.sessions);
  const [showArchived, setShowArchived] = useState(false);
  const sort = useAuric((s) => s.vaultSort);
  const query = useAuric((s) => s.vaultSearch);
  const sections = useAuric((s) => s.sectionOrder).filter((id) => id !== "heartbeat");
  const unlocked = useAuric((s) => s.unlocked);
  const exportOn = useAuric((s) => s.exportEnabled);
  const insurance = useAuric((s) => s.streakInsuranceUsed);
  const { streak } = computeStreak(sessions, !insurance);
  const minutes = Math.round(sessions.reduce((a, s) => a + s.durationMs, 0) / 60_000);

  const filtered = useMemo(() => {
    const q = query.trim().toLowerCase();
    const base = q
      ? sessions.filter(
          (s) =>
            s.note.toLowerCase().includes(q) ||
            s.kind.includes(q) ||
            formatDurationPretty(s.durationMs).toLowerCase().includes(q),
        )
      : sessions;
    return sortSessions(base.filter((s) => showArchived || !s.noteArchived), sort);
  }, [sessions, query, sort, showArchived]);

  const chart = useMemo(() => {
    const days: { day: string; minutes: number }[] = [];
    for (let i = 6; i >= 0; i--) {
      const ts = Date.now() - i * 86_400_000;
      const key = dayKey(ts);
      const mins =
        sessions.filter((s) => dayKey(s.endedAt) === key).reduce((a, s) => a + s.durationMs, 0) / 60_000;
      days.push({
        day: new Date(ts).toLocaleDateString(undefined, { weekday: "short" }),
        minutes: Math.round(mins * 10) / 10,
      });
    }
    return days;
  }, [sessions]);

  return (
    <div className="flex min-h-0 min-w-0 max-w-full flex-1 flex-col overflow-x-hidden">
      <div className="sticky top-0 z-20 min-w-0 max-w-full overflow-x-hidden bg-transparent px-1 pt-2 pb-2 backdrop-blur-md">
        <label className="flex min-h-11 items-center gap-3 text-sm">
          <input type="checkbox" checked={showArchived} onChange={(event) => setShowArchived(event.target.checked)} /> Include archived notes
        </label>
        <div className="tx-sorts min-w-0 max-w-full pb-2">
          {SORTS.map((s) => (
            <button
              key={s.id}
              type="button"
              onClick={() => useAuric.getState().setVaultSort(s.id)}
              className={cn(
                "tx-chip min-w-0 shrink px-2.5 text-[11px]",
                sort === s.id ? "is-on bg-accent text-on-accent" : "text-digit shadow-[0_0_0_1px_var(--line)]",
              )}
            >
              {s.label}
            </button>
          ))}
        </div>
        <label className="relative block min-w-0 max-w-full overflow-x-hidden">
          <Search className="pointer-events-none absolute left-3 top-1/2 size-4 -translate-y-1/2 text-muted" />
          <input
            value={query}
            onChange={(e) => useAuric.getState().setVaultSearch(e.target.value)}
            placeholder="Search notes"
            className="h-11 w-full min-w-0 rounded-lg bg-black/25 pl-10 pr-3 text-base text-digit outline-none shadow-[0_0_0_1px_var(--line)] placeholder:text-muted"
          />
        </label>
      </div>

      <div className="no-scrollbar min-h-0 flex-1 overflow-x-hidden overflow-y-auto px-1 pb-4">
        <div className="tx-stat-grid">
          <Stat label="Streak" value={`${streak}d`} />
          <Stat label="Minutes" value={String(minutes)} />
          <Stat label="Sessions" value={String(sessions.length)} />
        </div>

        {sections.map((id) => (
          <VaultSection key={id} id={id}>
            {id === "charts" ? (
              <div className="h-40 pt-1">
                {chart.every((d) => d.minutes === 0) ? (
                  <p className="px-1 py-6 text-center text-sm text-muted">
                    Minutes you log will draw a seven-day pit chart here.
                  </p>
                ) : (
                  <ResponsiveContainer width="100%" height="100%">
                    <BarChart data={chart} margin={{ top: 8, right: 4, left: -24, bottom: 0 }}>
                      <XAxis dataKey="day" tick={{ fill: "var(--muted)", fontSize: 11 }} axisLine={false} tickLine={false} />
                      <YAxis tick={{ fill: "var(--muted)", fontSize: 10 }} axisLine={false} tickLine={false} />
                      <Tooltip
                        contentStyle={{
                          background: "var(--chrome)",
                          border: "1px solid var(--line)",
                          borderRadius: 8,
                          color: "var(--digit)",
                          fontSize: 12,
                        }}
                      />
                      <Bar dataKey="minutes" fill="var(--accent)" radius={[4, 4, 0, 0]} />
                    </BarChart>
                  </ResponsiveContainer>
                )}
              </div>
            ) : null}
            {id === "sessions" ? <SessionList sessions={filtered} /> : null}
            {id === "achievements" ? (
              <div className="grid min-w-0 grid-cols-3 gap-2 pt-1">
                {ACHIEVEMENTS.map((a) => {
                  const on = unlocked.includes(a.id);
                  return (
                    <div
                      key={a.id}
                      className={cn(
                        "min-w-0 overflow-hidden rounded-md p-2 text-center shadow-[0_0_0_1px_var(--line)]",
                        on ? "bg-accent/15" : "opacity-45",
                      )}
                    >
                      <p className="overflow-hidden font-display text-[10px] tracking-wide text-accent">{a.title}</p>
                      <p className="mt-1 overflow-hidden text-[10px] leading-snug text-muted">{a.blurb}</p>
                    </div>
                  );
                })}
              </div>
            ) : null}
            {id === "export" ? (
              <div className="pt-1">
                <div className="flex items-center justify-between gap-3">
                  <div>
                    <p className="text-sm text-digit">Export</p>
                    <p className="text-xs text-muted">Choose a full-history JSON file or spreadsheet CSV.</p>
                  </div>
                  <OrbToggle
                    on={exportOn}
                    onChange={(v) => useAuric.getState().setExportEnabled(v)}
                    label="Export"
                  />
                </div>
                {exportOn ? (
                  <div className="mt-3 flex min-w-0 flex-wrap gap-2">
                    <PitButton variant="ghost" className="min-w-0 flex-1" onClick={() => exportVault(sessions, "json")}>
                      <Download className="size-4" /> JSON
                    </PitButton>
                    <PitButton variant="ghost" className="min-w-0 flex-1" onClick={() => exportVault(sessions, "csv")}>
                      <Download className="size-4" /> CSV
                    </PitButton>
                  </div>
                ) : null}
              </div>
            ) : null}
          </VaultSection>
        ))}
      </div>
    </div>
  );
}

function Stat({ label, value }: { label: string; value: string }) {
  return (
    <div className="tx-stat">
      <p className="tx-stat-value">{value}</p>
      <p className="tx-stat-label">{label}</p>
    </div>
  );
}

function VaultSection({ id, children }: { id: VaultSectionId; children: ReactNode }) {
  const [open, setOpen] = useState(true);
  const titles: Record<VaultSectionId, string> = {
    sessions: "Sessions",
    charts: "Minutes logged",
    achievements: "Achievements",
    heartbeat: "Heartbeat",
    export: "Export",
  };
  return (
    <section className="mb-3 rounded-xl panel p-3">
      <div className="flex items-center gap-2">
        <button type="button" className="flex min-h-11 flex-1 items-center justify-between" onClick={() => setOpen((v) => !v)}>
          <span className="font-display text-sm tracking-wide text-accent">{titles[id]}</span>
          {open ? <ChevronUp className="size-4 text-muted" /> : <ChevronDown className="size-4 text-muted" />}
        </button>
        <button
          type="button"
          className="grid size-11 place-items-center text-muted"
          aria-label="Move section up"
          onClick={() => useAuric.getState().moveSection(id, -1)}
        >
          <ChevronUp className="size-4" />
        </button>
        <button
          type="button"
          className="grid size-11 place-items-center text-muted"
          aria-label="Move section down"
          onClick={() => useAuric.getState().moveSection(id, 1)}
        >
          <ChevronDown className="size-4" />
        </button>
      </div>
      {open ? children : null}
    </section>
  );
}

function SessionList({ sessions }: { sessions: Session[] }) {
  if (sessions.length === 0) {
    return (
      <p className="py-6 text-center text-sm text-muted">
        No sessions yet. Start a timer or run a stopwatch, then jot what you did with that time.
      </p>
    );
  }
  return (
    <ul className="divide-y divide-line/50">
      {sessions.map((s) => (
        <li key={s.id} className="py-3">
          <div className="flex items-start justify-between gap-2">
            <div>
              <p className="text-sm text-digit">
                {s.kind === "timer" ? "Timer" : "Stopwatch"} · {formatDurationPretty(s.durationMs)}
              </p>
              <p className="text-xs text-muted">
                {formatDay(s.endedAt)} · {formatTime(s.endedAt)}
              </p>
            </div>
            <button
              type="button"
              className="grid size-11 place-items-center text-muted"
              aria-label="Delete session"
              onClick={() => useAuric.getState().deleteSession(s.id)}
            >
              <Trash2 className="size-4" />
            </button>
          </div>
          <NoteField id={s.id} initial={s.note} />
        </li>
      ))}
    </ul>
  );
}

function NoteField({ id, initial }: { id: string; initial: string }) {
  const [draft, setDraft] = useState(initial);
  const draftRef = useRef(draft);
  draftRef.current = draft;

  useEffect(() => {
    setDraft(initial);
    // Sync only when the row identity changes — never while this field is being typed.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [id]);

  useEffect(() => () => {
    useAuric.getState().updateSessionNote(id, draftRef.current);
  }, [id]);

  return (
    <textarea
      value={draft}
      onChange={(e) => setDraft(e.target.value)}
      onBlur={() => useAuric.getState().updateSessionNote(id, draft)}
      placeholder="What did you do with this time?"
      rows={2}
      autoComplete="off"
      enterKeyHint="done"
      aria-label="What did you do with this time?"
      className="tx-note-field mt-2 min-h-16"
    />
  );
}

function exportVault(sessions: Session[], format: "json" | "csv") {
  if (format === "json") {
    downloadText("auric-vault.json", JSON.stringify({ exportedAt: new Date().toISOString(), sessions }, null, 2), "application/json");
  } else {
    downloadText("auric-vault.csv", sessionsCsv(sessions), "text/csv;charset=utf-8");
  }
}
