export type FaceCut = {
  id: string;
  name: string;
  family: string;
  fallback: string;
  premium: boolean;
  weight: number;
};

export const FACES: FaceCut[] = [
  { id: "pit", name: "Pit Board", family: "Barlow Condensed", weight: 700, fallback: "sans-serif", premium: false },
  { id: "oswald", name: "Oswald", family: "Oswald", weight: 700, fallback: "sans-serif", premium: false },
  { id: "bebas", name: "Bebas Neue", family: "Bebas Neue", weight: 400, fallback: "sans-serif", premium: false },
  { id: "anton", name: "Anton", family: "Anton", weight: 400, fallback: "sans-serif", premium: false },
  { id: "archivo", name: "Archivo Black", family: "Archivo Black", weight: 400, fallback: "sans-serif", premium: false },
  { id: "roboto-cond", name: "Roboto Condensed", family: "Roboto Condensed", weight: 700, fallback: "sans-serif", premium: false },
  { id: "roboto", name: "Roboto", family: "Roboto", weight: 700, fallback: "sans-serif", premium: false },
  { id: "roboto-mono", name: "Roboto Mono", family: "Roboto Mono", weight: 700, fallback: "monospace", premium: false },
  { id: "inter", name: "Inter", family: "Inter", weight: 700, fallback: "sans-serif", premium: false },
  { id: "outfit", name: "Outfit", family: "Outfit", weight: 700, fallback: "sans-serif", premium: false },
  { id: "cinzel", name: "Cinzel", family: "Cinzel", weight: 700, fallback: "serif", premium: false },
  { id: "playfair", name: "Playfair Display", family: "Playfair Display", weight: 700, fallback: "serif", premium: false },
  { id: "cormorant", name: "Cormorant Garamond", family: "Cormorant Garamond", weight: 700, fallback: "serif", premium: true },
  { id: "baskerville", name: "Libre Baskerville", family: "Libre Baskerville", weight: 700, fallback: "serif", premium: true },
  { id: "merriweather", name: "Merriweather", family: "Merriweather", weight: 700, fallback: "serif", premium: true },
  { id: "lora", name: "Lora", family: "Lora", weight: 700, fallback: "serif", premium: true },
  { id: "source-serif", name: "Source Serif 4", family: "Source Serif 4", weight: 700, fallback: "serif", premium: true },
  { id: "source-code", name: "Source Code Pro", family: "Source Code Pro", weight: 700, fallback: "monospace", premium: true },
  { id: "plex-mono", name: "IBM Plex Mono", family: "IBM Plex Mono", weight: 700, fallback: "monospace", premium: true },
  { id: "plex-sans", name: "IBM Plex Sans", family: "IBM Plex Sans", weight: 700, fallback: "sans-serif", premium: true },
  { id: "space-mono", name: "Space Mono", family: "Space Mono", weight: 700, fallback: "monospace", premium: true },
  { id: "space-grotesk", name: "Space Grotesk", family: "Space Grotesk", weight: 700, fallback: "sans-serif", premium: true },
  { id: "orbitron", name: "Orbitron", family: "Orbitron", weight: 700, fallback: "sans-serif", premium: true },
  { id: "audiowide", name: "Audiowide", family: "Audiowide", weight: 400, fallback: "sans-serif", premium: true },
  { id: "rajdhani", name: "Rajdhani", family: "Rajdhani", weight: 700, fallback: "sans-serif", premium: true },
  { id: "exo", name: "Exo 2", family: "Exo 2", weight: 700, fallback: "sans-serif", premium: true },
  { id: "russo", name: "Russo One", family: "Russo One", weight: 400, fallback: "sans-serif", premium: true },
  { id: "blackops", name: "Black Ops One", family: "Black Ops One", weight: 400, fallback: "sans-serif", premium: true },
  { id: "stencil", name: "Stardos Stencil", family: "Stardos Stencil", weight: 700, fallback: "serif", premium: true },
  { id: "fraktur", name: "UnifrakturMaguntia", family: "UnifrakturMaguntia", weight: 400, fallback: "serif", premium: true },
  { id: "press", name: "Press Start 2P", family: "Press Start 2P", weight: 400, fallback: "monospace", premium: true },
  { id: "silkscreen", name: "Silkscreen", family: "Silkscreen", weight: 700, fallback: "monospace", premium: true },
  { id: "vt323", name: "VT323", family: "VT323", weight: 400, fallback: "monospace", premium: true },
  { id: "chakra", name: "Chakra Petch", family: "Chakra Petch", weight: 700, fallback: "sans-serif", premium: true },
  { id: "quantico", name: "Quantico", family: "Quantico", weight: 700, fallback: "sans-serif", premium: true },
  { id: "titillium", name: "Titillium Web", family: "Titillium Web", weight: 700, fallback: "sans-serif", premium: true },
  { id: "saira", name: "Saira Condensed", family: "Saira Condensed", weight: 700, fallback: "sans-serif", premium: true },
  { id: "teko", name: "Teko", family: "Teko", weight: 700, fallback: "sans-serif", premium: true },
  { id: "francois", name: "Francois One", family: "Francois One", weight: 400, fallback: "sans-serif", premium: true },
  { id: "righteous", name: "Righteous", family: "Righteous", weight: 400, fallback: "sans-serif", premium: true },
  { id: "fredoka", name: "Fredoka", family: "Fredoka", weight: 700, fallback: "sans-serif", premium: true },
  { id: "nunito", name: "Nunito", family: "Nunito", weight: 700, fallback: "sans-serif", premium: true },
  { id: "comfortaa", name: "Comfortaa", family: "Comfortaa", weight: 700, fallback: "sans-serif", premium: true },
  { id: "josefin", name: "Josefin Sans", family: "Josefin Sans", weight: 700, fallback: "sans-serif", premium: true },
  { id: "josefin-slab", name: "Josefin Slab", family: "Josefin Slab", weight: 700, fallback: "serif", premium: true },
  { id: "cutive", name: "Cutive Mono", family: "Cutive Mono", weight: 400, fallback: "monospace", premium: true },
  { id: "special", name: "Special Elite", family: "Special Elite", weight: 400, fallback: "serif", premium: true },
  { id: "aldrich", name: "Aldrich", family: "Aldrich", weight: 400, fallback: "sans-serif", premium: true },
  { id: "syncopate", name: "Syncopate", family: "Syncopate", weight: 700, fallback: "sans-serif", premium: true },
  { id: "bungee", name: "Bungee", family: "Bungee", weight: 400, fallback: "sans-serif", premium: true },
  { id: "monoton", name: "Monoton", family: "Monoton", weight: 400, fallback: "sans-serif", premium: true },
  { id: "limelight", name: "Limelight", family: "Limelight", weight: 400, fallback: "serif", premium: true },
  { id: "poiret", name: "Poiret One", family: "Poiret One", weight: 400, fallback: "sans-serif", premium: true },
  { id: "julius", name: "Julius Sans One", family: "Julius Sans One", weight: 400, fallback: "sans-serif", premium: true },
  { id: "marcellus", name: "Marcellus", family: "Marcellus", weight: 400, fallback: "serif", premium: true },
  { id: "spectral", name: "Spectral", family: "Spectral", weight: 700, fallback: "serif", premium: true },
  { id: "cardo", name: "Cardo", family: "Cardo", weight: 700, fallback: "serif", premium: true },
  { id: "garamond", name: "EB Garamond", family: "EB Garamond", weight: 700, fallback: "serif", premium: true },
  { id: "fraunces", name: "Fraunces", family: "Fraunces", weight: 700, fallback: "serif", premium: true },
  { id: "instrument-serif", name: "Instrument Serif", family: "Instrument Serif", weight: 400, fallback: "serif", premium: true },
  { id: "instrument-sans", name: "Instrument Sans", family: "Instrument Sans", weight: 700, fallback: "sans-serif", premium: true },
  { id: "dm-sans", name: "DM Sans", family: "DM Sans", weight: 700, fallback: "sans-serif", premium: true },
  { id: "dm-serif", name: "DM Serif Display", family: "DM Serif Display", weight: 400, fallback: "serif", premium: true },
  { id: "dm-mono", name: "DM Mono", family: "DM Mono", weight: 500, fallback: "monospace", premium: true },
  { id: "jetbrains", name: "JetBrains Mono", family: "JetBrains Mono", weight: 700, fallback: "monospace", premium: true },
  { id: "manrope", name: "Manrope", family: "Manrope", weight: 700, fallback: "sans-serif", premium: true },
  { id: "sora", name: "Sora", family: "Sora", weight: 700, fallback: "sans-serif", premium: true },
  { id: "lexend", name: "Lexend", family: "Lexend", weight: 700, fallback: "sans-serif", premium: true },
  { id: "atkinson", name: "Atkinson Hyperlegible", family: "Atkinson Hyperlegible", weight: 700, fallback: "sans-serif", premium: true },
  { id: "big-shoulders", name: "Big Shoulders", family: "Big Shoulders", weight: 700, fallback: "sans-serif", premium: true },
  { id: "staatliches", name: "Staatliches", family: "Staatliches", weight: 400, fallback: "sans-serif", premium: true },
  { id: "michroma", name: "Michroma", family: "Michroma", weight: 400, fallback: "sans-serif", premium: true },
  { id: "electrolize", name: "Electrolize", family: "Electrolize", weight: 400, fallback: "sans-serif", premium: true },
  { id: "iceland", name: "Iceland", family: "Iceland", weight: 400, fallback: "sans-serif", premium: true },
  { id: "wallpoet", name: "Wallpoet", family: "Wallpoet", weight: 400, fallback: "sans-serif", premium: true },
  { id: "khand", name: "Khand", family: "Khand", weight: 700, fallback: "sans-serif", premium: true },
  { id: "kanit", name: "Kanit", family: "Kanit", weight: 700, fallback: "sans-serif", premium: true },
];

export const HOUSE_FACE_ID = "pit";

export type FaceLoadState = "idle" | "loading" | "loaded" | "error";
const states = new Map<string, FaceLoadState>();
const pending = new Map<string, Promise<boolean>>();
const listeners = new Set<() => void>();

export function subscribeFaces(listener: () => void) {
  listeners.add(listener);
  return () => { listeners.delete(listener); };
}

export function faceLoadState(id: string): FaceLoadState {
  return states.get(faceById(id).id) ?? "idle";
}

function setLoadState(id: string, state: FaceLoadState) {
  states.set(id, state);
  listeners.forEach((listener) => listener());
}

export function faceStylesheetUrl(face: FaceCut): string {
  return `https://fonts.googleapis.com/css2?family=${encodeURIComponent(face.family)}:wght@${face.weight}&display=swap`;
}

/** A stylesheet response alone does not prove the font binary loaded. */
export function loadFace(id: string): Promise<boolean> {
  const face = faceById(id);
  if (typeof document === "undefined" || !document.fonts) return Promise.resolve(false);
  if (faceLoadState(face.id) === "loaded") return Promise.resolve(true);
  const inflight = pending.get(face.id);
  if (inflight) return inflight;
  const promise = new Promise<boolean>((resolve) => {
    const link = document.createElement("link");
    let settled = false;
    const finish = (ok: boolean) => {
      if (settled) return;
      settled = true;
      clearTimeout(timeout);
      link.onload = null;
      link.onerror = null;
      if (!ok) link.remove();
      pending.delete(face.id);
      setLoadState(face.id, ok ? "loaded" : "error");
      resolve(ok);
    };
    const timeout = setTimeout(() => finish(false), 12_000);
    link.rel = "stylesheet";
    link.dataset.auricFace = face.id;
    link.href = faceStylesheetUrl(face);
    link.onerror = () => finish(false);
    link.onload = () => {
      void document.fonts.load(`${face.weight} 32px "${face.family}"`, "0123456789:.")
        .then((fonts) => finish(fonts.length > 0 && fonts.every((font) => font.status === "loaded")))
        .catch(() => finish(false));
    };
    document.head.appendChild(link);
  });
  pending.set(face.id, promise);
  setLoadState(face.id, "loading");
  return promise;
}

export function faceById(id: string | null | undefined): FaceCut {
  const legacy: Record<string, string> = { mono: "roboto-mono", serif: "cinzel", neon: "orbitron" };
  const mapped = legacy[id ?? ""] ?? id ?? HOUSE_FACE_ID;
  return FACES.find((f) => f.id === mapped) ?? FACES[0]!;
}

export function facesForLiteracy(_index: number): FaceCut[] {
  return FACES;
}

export function faceShort(face: FaceCut): string {
  const first = face.name.split(/\s+/)[0] ?? face.name;
  return first.length > 10 ? first.slice(0, 9) : first;
}
