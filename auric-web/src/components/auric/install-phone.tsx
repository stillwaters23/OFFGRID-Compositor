import { useState, useSyncExternalStore } from "react";
import { consumeInstallPrompt, installSnapshot, serverInstallSnapshot, subscribeInstall } from "@/lib/install";
import { Smartphone } from "lucide-react";
import { PitButton } from "@/components/auric/pit-button";
import { playClick, playLockIn } from "@/lib/audio";
import { isAndroid, isIos, isMobileBrowser } from "@/lib/platform";

export function InstallPhone({ compact = false }: { compact?: boolean }) {
  const { prompt: promptEvent, installed: done } = useSyncExternalStore(subscribeInstall, installSnapshot, serverInstallSnapshot);
  const [message, setMessage] = useState("");
  const android = typeof window !== "undefined" ? isAndroid() : false;
  const ios = typeof window !== "undefined" ? isIos() : false;
  const mobile = typeof window !== "undefined" ? isMobileBrowser() : false;

  if (done) {
    return (
      <p className="rounded-md bg-felt/60 px-3 py-2 text-xs leading-relaxed text-accent">
        AURIC is on this phone. The diamond is the house now.
      </p>
    );
  }

  async function install() {
    playClick();
    if (promptEvent) {
      try {
        await promptEvent.prompt();
        const choice = await promptEvent.userChoice;
        if (choice.outcome === "accepted") {
          playLockIn();
          setMessage("Installation requested. Follow your browser's confirmation.");
        }
      } catch {
        setMessage("The install prompt is unavailable. Use the browser menu to install AURIC.");
      } finally { consumeInstallPrompt(); }
      return;
    }
    window.location.href = "/?install=1&platform=" + (ios ? "ios" : "android");
  }

  return (
    <div className={compact ? "" : "mt-4"}>
      <div className="mb-2 flex items-center gap-2">
        <Smartphone className="size-4 text-accent" />
        <div>
          <p className="text-sm text-digit">On this phone</p>
          <p className="text-[11px] text-muted">Add AURIC to your home screen for quick access.</p>
        </div>
      </div>
      <PitButton className="magnet w-full" onClick={() => void install()}>
        {promptEvent ? "Put AURIC on the home screen" : "How to install"}
      </PitButton>
      {message ? <p className="mt-2 text-sm text-muted" role="status">{message}</p> : null}
      <ol className="mt-3 list-decimal space-y-1.5 pl-4 text-xs leading-relaxed text-muted">
        {android ? (
          <>
            <li>Open this page in Chrome — not inside another app’s browser.</li>
            <li>Tap the three dots → <span className="text-digit">Install app</span> or <span className="text-digit">Add to Home screen</span>.</li>
            <li>The ice diamond lands on your launcher. That’s the app.</li>
          </>
        ) : ios ? (
          <>
            <li>Open this page in Safari.</li>
            <li>Share → <span className="text-digit">Add to Home Screen</span>.</li>
            <li>The diamond is the icon. Tap it like any other app.</li>
          </>
        ) : (
          <>
            <li>On your phone, open the published AURIC link in Chrome.</li>
            <li>Menu → Install app / Add to Home screen.</li>
            <li>Launch AURIC from your home screen.</li>
          </>
        )}
      </ol>
      {mobile ? null : (
        <p className="mt-2 text-[11px] text-muted">
          On desktop, use your browser’s install button when available.
        </p>
      )}
    </div>
  );
}
