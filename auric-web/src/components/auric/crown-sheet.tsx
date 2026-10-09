import { Crown, X } from "lucide-react";
import { PitButton } from "@/components/auric/pit-button";
import { useAuric } from "@/lib/store";

export function CrownSheet() {
  const open = useAuric((s) => s.crownSheetOpen);
  const crowned = useAuric((s) => s.isCrowned);
  if (!open) return null;

  return (
    <div className="fixed inset-0 z-[60] grid place-items-center px-5">
      <button
        type="button"
        className="absolute inset-0"
        style={{ background: "color-mix(in oklab, #000 16%, transparent)" }}
        aria-label="Close crown"
        onClick={() => useAuric.getState().setCrownSheet(false)}
      />
      <div
        className="unfurl relative w-full min-w-0 max-w-md rounded-xl p-5"
        style={{
          background: "color-mix(in oklab, var(--chrome) 8%, transparent)",
          backdropFilter: "blur(18px) saturate(1.2)",
          WebkitBackdropFilter: "blur(18px) saturate(1.2)",
          boxShadow:
            "0 0 0 1px color-mix(in oklab, var(--accent) 40%, transparent), 0 0 36px color-mix(in oklab, var(--glow) 22%, transparent), inset 0 1px 0 color-mix(in oklab, #fff 16%, transparent)",
        }}
      >
        <div className="flex items-start justify-between">
          <div className="flex items-center gap-2">
            <Crown className="size-5 text-accent" />
            <p className="chrome-type font-display text-xl tracking-wide">Crown</p>
          </div>
          <button
            type="button"
            className="grid size-11 place-items-center"
            onClick={() => useAuric.getState().setCrownSheet(false)}
            aria-label="Close"
          >
            <X className="size-5" />
          </button>
        </div>
        <p className="mt-1 font-display text-sm tracking-[0.18em] uppercase text-accent">$7.77 · Preview only</p>
        <p className="mt-3 text-sm leading-relaxed text-digit">
          This is a showroom unlock — not a real charge. Billing lives in the native app. Preview Crown opens ice
          collections, digit faces, analog skin, Glow Mode, the floating chip, PIN, export, and saved timers.
        </p>
        <ul className="mt-3 space-y-1.5 text-sm text-digit/75">
          <li>Blue Diamond collections, digit faces, analog dial</li>
          <li>Glow Mode and floating chip</li>
          <li>PIN, control lock, vault export</li>
        </ul>
        <PitButton
          className="mt-5 h-12 w-full rounded-lg"
          onClick={() => useAuric.getState().unlockCrown()}
          disabled={crowned}
        >
          {crowned ? "Already previewing" : "Preview Crown · no charge"}
        </PitButton>
      </div>
    </div>
  );
}
