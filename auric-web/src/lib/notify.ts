import { BRAND } from "@/lib/brand";
/**
 * Android Chrome throws on `new Notification(...)`.
 * Always use ServiceWorkerRegistration.showNotification.
 */

let swReady: Promise<ServiceWorkerRegistration | null> | null = null;

export function registerAuricWorker(): Promise<ServiceWorkerRegistration | null> {
  if (typeof window === "undefined" || !("serviceWorker" in navigator)) {
    return Promise.resolve(null);
  }
  if (!swReady) {
    swReady = navigator.serviceWorker
      .register("/sw.js")
      .then(async (reg) => {
        if (reg.active) return reg;
        const worker = reg.installing ?? reg.waiting;
        if (!worker) return null;
        return await new Promise<ServiceWorkerRegistration | null>((resolve) => {
          const finish = (ready: boolean) => {
            clearTimeout(timeout);
            worker.removeEventListener("statechange", check);
            resolve(ready ? reg : null);
          };
          const check = () => {
            if (worker.state === "activated") finish(true);
            else if (worker.state === "redundant") finish(false);
          };
          const timeout = setTimeout(() => finish(false), 10_000);
          worker.addEventListener("statechange", check);
          check();
        });
      })
      .then((reg) => { if (!reg) swReady = null; return reg; })
      .catch(() => { swReady = null; return null; });
  }
  return swReady;
}

export async function ensureNotifyPermission(): Promise<NotificationPermission> {
  if (typeof Notification === "undefined") return "denied";
  if (Notification.permission === "granted") return "granted";
  if (Notification.permission === "denied") return "denied";
  try {
    return await Notification.requestPermission();
  } catch {
    return "denied";
  }
}

export async function showAuricNotification(title: string, body: string, tag = "auric") {
  // Permission prompts belong to a user gesture, never an expired timer.
  if (typeof Notification === "undefined" || Notification.permission !== "granted") return false;
  try {
    const reg = await registerAuricWorker();
    if (!reg) return false;
    await reg.showNotification(title, {
      body,
      tag,
      icon: BRAND.notificationIcon,
      badge: BRAND.notificationBadge,
      silent: false,
    });
    return true;
  } catch {
    return false;
  }
}
