import { GlassFolder } from "@/components/auric/glass-folder";
import { MODULES, isInstalled, type ModuleId } from "@/lib/modules";
import { playClick } from "@/lib/audio";
import { useAuric } from "@/lib/store";
import { cn } from "@/lib/utils";

export function FolderGrid() {
  const installed = useAuric((s) => s.installedModules);
  const glow = useAuric((s) => s.glowIntensity);
  const glowZero = glow <= 0;
  const rows = [...MODULES].sort((a, b) => {
    const ao = isInstalled(a.id, installed) ? 0 : 1;
    const bo = isInstalled(b.id, installed) ? 0 : 1;
    return ao - bo;
  });
  return (
    <ul className="tx-folder-grid">
      {rows.map((spec) => {
        const owned = isInstalled(spec.id, installed);
        return (
          <li key={spec.id} data-folder-id={spec.id}>
            <button
              type="button"
              className={cn("tx-folder-tile", owned ? "is-on" : "is-off")}
              aria-label={`${spec.pkg} ${owned ? "unlocked" : "locked"}`}
              onClick={() => {
                playClick();
                useAuric.getState().enterModule(spec.id as ModuleId);
              }}
            >
              <span className="tx-folder-row">
                <span className="tx-folder-icon">
                  <GlassFolder spec={spec} on={owned} glowZero={glowZero} />
                </span>
                <span className="tx-folder-copy">
                  <span className={cn("tx-folder-name", !owned && "is-locked")}>{spec.pkg}</span>
                  <span className={cn("tx-folder-state", owned ? "is-on" : "is-off")}>
                    {owned ? "unlocked" : "locked"}
                  </span>
                </span>
                <span className={cn("tx-folder-lockmark", owned ? "is-open" : "is-shut")} aria-hidden>
                  {owned ? (
                    <svg viewBox="0 0 16 16" width="16" height="16">
                      <rect x="3" y="7" width="10" height="7" rx="1.2" fill="none" stroke="currentColor" strokeWidth="1.4" />
                      <path d="M5 7 V4.8 a3 3 0 0 1 6 0 V5.2" fill="none" stroke="currentColor" strokeWidth="1.4" />
                    </svg>
                  ) : (
                    <svg viewBox="0 0 16 16" width="16" height="16">
                      <rect x="3" y="7" width="10" height="7" rx="1.2" fill="none" stroke="currentColor" strokeWidth="1.4" />
                      <path d="M5 7 V5.2 a3 3 0 0 1 6 0 V7" fill="none" stroke="currentColor" strokeWidth="1.4" />
                    </svg>
                  )}
                </span>
                <span className="tx-folder-chevron" aria-hidden>
                  ›
                </span>
              </span>
            </button>
          </li>
        );
      })}
    </ul>
  );
}
