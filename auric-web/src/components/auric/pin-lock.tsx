import { useState } from "react";
import { Lock } from "lucide-react";
import { PitButton } from "@/components/auric/pit-button";
import { markPinUnlocked } from "@/lib/pin-session";
import { verifyPin } from "@/lib/hash";
import { hapticError, hapticLock } from "@/lib/haptics";
import { useAuric } from "@/lib/store";

export function PinLock() {
  const locked = useAuric((s) => s.pinLocked);
  const hash = useAuric((s) => s.pinHash);
  const [pin, setPin] = useState("");
  const [error, setError] = useState(false);
  const [busy, setBusy] = useState(false);

  if (!locked || !hash) return null;

  async function submit(next: string) {
    if (next.length < 1 || busy) return;
    setBusy(true);
    try {
      const ok = await verifyPin(next, hash!);
      if (ok) {
        hapticLock();
        markPinUnlocked(hash!);
        useAuric.getState().setPinLocked(false);
        setPin("");
        setError(false);
        return;
      }
      hapticError();
      setError(true);
      setPin("");
    } catch {
      setError(true);
    } finally {
      setBusy(false);
    }
  }

  return (
    <div className="fixed inset-0 z-[80] flex flex-col items-center justify-center bg-felt px-6">
      <Lock className="size-8 text-accent" />
      <p className="mt-4 font-display text-2xl tracking-[0.2em] text-accent">AURIC</p>
      <p className="mt-1 text-sm tracking-[0.2em] uppercase text-muted">Enter PIN</p>
      <input
        type="password"
        autoComplete="current-password"
        maxLength={77}
        value={pin}
        autoFocus
        onChange={(e) => {
          setPin(e.target.value.slice(0, 77));
          setError(false);
        }}
        onKeyDown={(e) => {
          if (e.key === "Enter") void submit(pin);
        }}
        aria-label="PIN"
        placeholder="Letters, numbers, symbols"
        className="mt-6 h-12 w-full max-w-xs rounded-md bg-felt px-3 text-center text-base text-digit outline-none shadow-[0_0_0_1px_var(--line)]"
      />
      {error ? <p className="mt-2 text-sm text-danger">That PIN did not match.</p> : null}
      <div className="mt-6 flex w-full max-w-xs flex-col gap-2">
        <PitButton className="w-full" disabled={pin.length < 1 || busy} onClick={() => void submit(pin)}>
          Unlock
        </PitButton>
        <PitButton variant="ghost" className="w-full" onClick={() => setPin("")}>
          Clear
        </PitButton>

      </div>
    </div>
  );
}
