import type { ModuleSpec } from "@/lib/modules";
import type { FolderIconStyle, FolderPreferences, FolderLayout } from "@/lib/folder_layout";

export function FolderLayoutSheet({
  open,
  specs,
  preferences,
  onChange,
  onClose,
}: {
  open: boolean;
  specs: readonly ModuleSpec[];
  preferences: FolderPreferences;
  onChange: (next: FolderPreferences) => void;
  onClose: () => void;
}) {
  if (!open) return null;
  const setStyle = (id: string, style: FolderIconStyle) => {
    onChange({ ...preferences, folderIconStyle: { ...preferences.folderIconStyle, [id]: style } });
  };
  const setLayout = (layout: FolderLayout) => onChange({ ...preferences, layout });

  return (
    <div className="tx-folder-sheet-backdrop" onMouseDown={(event) => event.target === event.currentTarget && onClose()}>
      <section className="tx-folder-sheet" role="dialog" aria-modal="true" aria-labelledby="tx-folder-sheet-title">
        <div className="tx-folder-sheet-heading">
          <h2 id="tx-folder-sheet-title">FOLDER LAYOUT</h2>
          <button type="button" className="tx-folder-sheet-close" onClick={onClose} aria-label="Close folder layout">
            ×
          </button>
        </div>
        <fieldset className="tx-folder-layout-options">
          <legend>LAYOUT</legend>
          {(["grid", "big", "single"] as const).map((layout) => (
            <button
              type="button"
              key={layout}
              aria-pressed={preferences.layout === layout}
              onClick={() => setLayout(layout)}
            >
              {layout === "grid" ? "GRID" : layout === "big" ? "BIG" : "SINGLE"}
            </button>
          ))}
        </fieldset>
        <label className="tx-folder-all-size">
          <span>ALL FOLDERS <output>{preferences.folderSizeAll}</output></span>
          <input
            type="range"
            min="0"
            max="100"
            value={preferences.folderSizeAll}
            aria-label="All folders size"
            onChange={(event) => onChange({ ...preferences, folderSizeAll: Number(event.currentTarget.value) })}
          />
        </label>
        <div className="tx-folder-style-list">
          {specs.map((spec) => (
            <div className="tx-folder-style-row" key={spec.id}>
              <span className="tx-folder-style-name">{preferences.folderNames[spec.id] ?? spec.pkg}</span>
              <label>
                <span className="sr-only">Icon style for {spec.pkg}</span>
                <select
                  value={preferences.folderIconStyle[spec.id] ?? "flat"}
                  aria-label={`Icon style for ${spec.pkg}`}
                  onChange={(event) => setStyle(spec.id, event.currentTarget.value as FolderIconStyle)}
                >
                  <option value="flat">Flat</option>
                  <option value="3d">3D</option>
                  <option value="etched">Etched</option>
                </select>
              </label>
              {preferences.folderNames[spec.id] ? (
                <button
                  type="button"
                  className="tx-folder-reset-name"
                  onClick={() => {
                    const folderNames = { ...preferences.folderNames };
                    delete folderNames[spec.id];
                    onChange({ ...preferences, folderNames });
                  }}
                >
                  Reset name
                </button>
              ) : null}
            </div>
          ))}
        </div>
      </section>
    </div>
  );
}
