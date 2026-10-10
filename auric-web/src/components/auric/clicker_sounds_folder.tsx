import { useEffect, useState } from "react";
import { playProceduralClick, primeClickVoice } from "@/lib/click_voice";
import {
  clickerFolderView,
  readStoredFolderPreferences,
  type ClickerFolderView,
} from "@/lib/clicker_folder_prefs";
import { HOUSE_CLICK_ID, TICK_FAMILIES, TICKS, tickById } from "@/lib/ticks";
import { useAuric } from "@/lib/store";
import "@/styles/clicker.css";

export function ClickerSoundsFolder() {
  const [family, setFamily] = useState<(typeof TICK_FAMILIES)[number]>(TICK_FAMILIES[0]);
  const [folderView, setFolderView] = useState<ClickerFolderView>(() => clickerFolderView(null));
  const reelSound = useAuric((s) => s.reelSound);
  const selected = tickById(reelSound).id;
  const cuts = TICKS.filter((tick) => tick.family === family);

  useEffect(() => {
    setFolderView(clickerFolderView(readStoredFolderPreferences()));
  }, []);

  return (
    <section
      className={`tx-clicker is-${folderView.iconStyle} is-title-${folderView.titlePx}`}
      data-folder-name={folderView.tileName}
      data-folder-size={folderView.size}
      data-icon-style={folderView.iconStyle}
      onPointerDown={primeClickVoice}
    >
      <div className="tx-clicker-tabs" role="tablist" aria-label="Clicker sound styles">
        {TICK_FAMILIES.map((style) => (
          <button
            key={style}
            id={`clicker-tab-${style.toLowerCase()}`}
            type="button"
            role="tab"
            aria-selected={style === family}
            aria-controls="clicker-sound-cuts"
            className="tx-clicker-tab"
            onClick={() => setFamily(style)}
          >
            {style}
          </button>
        ))}
      </div>
      {folderView.customName ? <p className="tx-clicker-rename">{folderView.customName}</p> : null}
      <div id="clicker-sound-cuts" className="tx-clicker-panel" role="tabpanel" aria-labelledby={`clicker-tab-${family.toLowerCase()}`}>
        <ul className="tx-clicker-list">
          {cuts.map((tick) => {
            const isSelected = tick.id === selected;
            return (
              <li key={tick.id}>
                <button
                  type="button"
                  className="tx-clicker-row"
                  data-tick-id={tick.id}
                  aria-pressed={isSelected}
                  onClick={() => {
                    playProceduralClick(tick.id);
                    useAuric.getState().setReelSound(tick.id);
                  }}
                >
                  <span className="tx-clicker-name">{tick.name}</span>
                  <span className="tx-clicker-badges">
                    {tick.id === HOUSE_CLICK_ID ? <span className="tx-clicker-badge is-house">House</span> : null}
                    {tick.premium ? <span className="tx-clicker-badge">Crown cut</span> : null}
                  </span>
                  <span className="tx-clicker-check" aria-hidden="true">{isSelected ? "✓" : ""}</span>
                </button>
              </li>
            );
          })}
        </ul>
      </div>
      <p className="tx-muted tx-clicker-footer">Ticks play on every wheel notch.</p>
    </section>
  );
}
