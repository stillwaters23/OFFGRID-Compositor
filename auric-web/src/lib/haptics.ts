export function haptic(ms = 8) {
  try {
    if (typeof navigator !== "undefined" && typeof navigator.vibrate === "function") {
      navigator.vibrate(ms);
    }
  } catch {
    /* ignore */
  }
}

export function hapticTick() {
  haptic(7);
}

/** Default wheel tick — loud enough to feel through a case. */
export function hapticReel() {
  try {
    if (typeof navigator !== "undefined" && typeof navigator.vibrate === "function") {
      navigator.vibrate([12, 18, 42]);
    }
  } catch {
    /* ignore */
  }
}

export function hapticLock() {
  haptic(18);
}

export function hapticError() {
  try {
    if (typeof navigator !== "undefined" && typeof navigator.vibrate === "function") {
      navigator.vibrate([12, 40, 18]);
    }
  } catch {
    /* ignore */
  }
}

export function hapticCelebrate() {
  try {
    if (typeof navigator !== "undefined" && typeof navigator.vibrate === "function") {
      navigator.vibrate([12, 30, 18, 30, 28]);
    }
  } catch {
    /* ignore */
  }
}

export function hapticStop() {
  try {
    if (typeof navigator !== "undefined" && typeof navigator.vibrate === "function") {
      navigator.vibrate(0);
    }
  } catch {
    /* ignore */
  }
}
