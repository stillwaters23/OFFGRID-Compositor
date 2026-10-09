import type { StateStorage } from "zustand/middleware";

const written = new Map<string, string>();
const protectedKeys = new Set<string>();
let reported = false;

function reportStorageError() {
  if (typeof window === "undefined" || reported) return;
  reported = true;
  window.dispatchEvent(new Event("auric-storage-error"));
}

/** Clock ticks must not rewrite unchanged history to synchronous storage. */
export const auricStorage: StateStorage = {
  getItem(name) {
    written.delete(name);
    try {
      if (typeof localStorage === "undefined") return null;
      const raw = localStorage.getItem(name);
      if (raw === null) { protectedKeys.delete(name); return null; }
      try {
        const saved: unknown = JSON.parse(raw);
        if (!saved || typeof saved !== "object" || Array.isArray(saved) ||
          !("state" in saved) || !saved.state || typeof saved.state !== "object" || Array.isArray(saved.state) ||
          ("version" in saved && saved.version !== undefined && saved.version !== 0)) {
          throw new Error("Unsupported saved state");
        }
        protectedKeys.delete(name);
        return raw;
      } catch {
        // Never replace unreadable history unless its original bytes are safely copied.
        protectedKeys.add(name);
        let recoveryKey = `${name}.recovery.${Date.now()}`;
        for (let i = 1; localStorage.getItem(recoveryKey) !== null; i++) {
          recoveryKey = `${name}.recovery.${Date.now()}.${i}`;
        }
        localStorage.setItem(recoveryKey, raw);
        protectedKeys.delete(name);
        if (typeof window !== "undefined") window.dispatchEvent(new Event("auric-storage-recovered"));
        return null;
      }
    } catch {
      protectedKeys.add(name);
      reportStorageError();
      return null;
    }
  },
  setItem(name, value) {
    if (protectedKeys.has(name)) { reportStorageError(); return; }
    if (written.get(name) === value) return;
    try {
      if (typeof localStorage === "undefined") return;
      localStorage.setItem(name, value);
      written.set(name, value);
      reported = false;
    } catch { reportStorageError(); }
  },
  removeItem(name) {
    try { localStorage.removeItem(name); written.delete(name); protectedKeys.delete(name); }
    catch { reportStorageError(); }
  },
};
