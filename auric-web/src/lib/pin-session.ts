const PIN_SESSION = "auric-pin-ok";

export function markPinUnlocked(hash: string) {
  try { sessionStorage.setItem(PIN_SESSION, hash); } catch { /* Session-only convenience. */ }
}

export function pinSessionUnlocked(hash: string | null) {
  try { return !!hash && sessionStorage.getItem(PIN_SESSION) === hash; } catch { return false; }
}
