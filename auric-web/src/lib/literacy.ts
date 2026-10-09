export type LiteracyBand = "easy" | "standard" | "deep" | "savant";

export type LiteracyLevel = {
  code: string;
  name: string;
  band: LiteracyBand;
  blurb: string;
};

export const LITERACY: LiteracyLevel[] = [
  {
    code: "1",
    name: "Initiate",
    band: "easy",
    blurb: "Big type. Three doors. We walk you through the house.",
  },
  {
    code: "2",
    name: "House",
    band: "easy",
    blurb: "Colors, sounds, the timer. That's the whole house.",
  },
  {
    code: "7",
    name: "Cut",
    band: "standard",
    blurb: "All rooms. Short labels. No hex yet.",
  },
  {
    code: "77",
    name: "Atelier",
    band: "deep",
    blurb: "Hex, harmony, glow, export. You paint the ice.",
  },
  {
    code: "777",
    name: "Pavilion",
    band: "savant",
    blurb: "Every drum. PIN. Analog. The nerd table.",
  },
  {
    code: "888",
    name: "Absolute",
    band: "savant",
    blurb: "Nothing hidden. This instrument is yours.",
  },
];

export type PavilionRoomId = "floor" | "tick" | "cut" | "table" | "sessions" | "mark" | "heartbeat" | "girdle" | "device" | "clock";

export function literacyOf(index: number): LiteracyLevel {
  return LITERACY[index] ?? LITERACY[2]!;
}

export function bandOf(index: number): LiteracyBand {
  return literacyOf(index).band;
}

export function roomsFor(_index: number): PavilionRoomId[] {
  return ["clock", "cut", "tick", "device", "heartbeat", "sessions", "girdle"];
}

export function roomTitle(id: PavilionRoomId, _index: number) {
  switch (id) {
    case "clock":
      return { num: "01", name: "Clock & Display", casino: "Clock", hint: "Time format, zone, and how numbers look." };
    case "cut":
      return { num: "02", name: "Customizations", casino: "Look", hint: "Colors, themes, sizing, and opacity. Preview then Apply." };
    case "tick":
      return { num: "03", name: "Sounds & Voice", casino: "Sound", hint: "Clicks, alarms, app level, and narration." };
    case "device":
      return { num: "04", name: "Device", casino: "Device", hint: "Battery, threads, and optional network details." };
    case "heartbeat":
      return { num: "05", name: "Reminders", casino: "Heartbeat", hint: "Pulse interval while AURIC is open." };
    case "sessions":
      return { num: "06", name: "Logs & Exports", casino: "Logs", hint: "Notes, Prism, Heartbeat Log, and exports." };
    case "girdle":
      return { num: "07", name: "Crown & About", casino: "The Girdle", hint: "PIN, install, unlock." };
    case "table":
      return { num: "01", name: "Clock & Display", casino: "Clock", hint: "Time format, zone, and how numbers look." };
    case "mark":
      return { num: "06", name: "Logs & Exports", casino: "Logs", hint: "Notes, Prism, Heartbeat Log, and exports." };
    case "floor":
      return { num: "00", name: "Home Screen", casino: "The Floor", hint: "Tabs on the main screen." };
  }
}

export function copy(index: number) {
  const easy = bandOf(index) === "easy";
  const deep = bandOf(index) === "deep" || bandOf(index) === "savant";
  return {
    easy,
    deep,
    savant: bandOf(index) === "savant",
    start: "Start",
    vault: easy ? "History" : "History",
    stopwatch: "Stopwatch",
    timer: "Timer",
    alarm: "Alarm",
    remind: "Reminders",
    house: "Settings",
    pavilion: "Settings",
    colors: "Colors",
    sounds: "Sounds",
    setReel: easy ? "Spin to set time" : "Set the reel",
    onClock: easy ? "Running" : "On the clock",
  };
}
