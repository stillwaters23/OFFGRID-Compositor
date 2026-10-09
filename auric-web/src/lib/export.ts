import type { Session } from "@/lib/types";

function csvCell(value: string | number): string {
  const text = String(value);
  // User notes must remain text when opened in a spreadsheet.
  const safe = /^[\s]*[=+@-]|^[\t\r\n]/.test(text) ? `'${text}` : text;
  return `"${safe.replace(/"/g, '""')}"`;
}

export function sessionsCsv(sessions: Session[]): string {
  const header = "id,kind,startedAt,endedAt,durationMs,plannedMs,note,laps,archived";
  const rows = sessions.map((s) => [
    s.id, s.kind, new Date(s.startedAt).toISOString(), new Date(s.endedAt).toISOString(),
    s.durationMs, s.plannedMs ?? "", s.note, s.laps?.join("|") ?? "", s.noteArchived ? "yes" : "no",
  ].map(csvCell).join(","));
  return [header, ...rows].join("\r\n");
}
