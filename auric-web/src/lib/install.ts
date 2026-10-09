import { isStandalone } from "@/lib/platform";

export type BeforeInstall = Event & {
  prompt: () => Promise<void>;
  userChoice: Promise<{ outcome: string }>;
};
const initial = { prompt: null as BeforeInstall | null, installed: false };
let snapshot = initial;
const listeners = new Set<() => void>();
export const installSnapshot = () => snapshot;
export const serverInstallSnapshot = () => initial;
export const subscribeInstall = (listener: () => void) => {
  listeners.add(listener);
  return () => { listeners.delete(listener); };
};
function update(patch: Partial<typeof initial>) {
  snapshot = { ...snapshot, ...patch };
  listeners.forEach(listener => listener());
}
export function consumeInstallPrompt() { update({ prompt: null }); }

/** Capture early: the Settings install card may mount after this event fires. */
export function observeInstall() {
  update({ installed: isStandalone() });
  const prompt = (event: Event) => { event.preventDefault(); update({ prompt: event as BeforeInstall }); };
  const installed = () => update({ prompt: null, installed: true });
  window.addEventListener("beforeinstallprompt", prompt);
  window.addEventListener("appinstalled", installed);
  return () => {
    window.removeEventListener("beforeinstallprompt", prompt);
    window.removeEventListener("appinstalled", installed);
  };
}
