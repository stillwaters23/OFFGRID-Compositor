export type FloorId = "none" | "sunset" | "footprints" | "lagoon" | "custom";

export const FLOORS: { id: FloorId; name: string; src: string; hint: string }[] = [
  { id: "none", name: "Felt only", src: "", hint: "Deep sea felt. No photo." },
  { id: "sunset", name: "Sunset palms", src: "/floors/sunset.jpg", hint: "Tropical dusk." },
  { id: "footprints", name: "White sand", src: "/floors/footprints.jpg", hint: "Footprints at first light." },
  { id: "lagoon", name: "Lagoon boats", src: "/floors/lagoon.jpg", hint: "Pastel water, a leaning palm." },
  { id: "custom", name: "Your photo", src: "", hint: "Stays on this device. Never uploaded." },
];

export function floorSrc(id: FloorId, custom: string): string {
  if (id === "custom") return custom;
  return FLOORS.find((f) => f.id === id)?.src ?? "";
}

export function readFloorFile(file: File): Promise<string> {
  return new Promise((resolve, reject) => {
    const url = URL.createObjectURL(file);
    const img = new Image();
    img.onload = () => {
      const max = 1280;
      const scale = Math.min(1, max / Math.max(img.width, img.height));
      const c = document.createElement("canvas");
      c.width = Math.max(1, Math.round(img.width * scale));
      c.height = Math.max(1, Math.round(img.height * scale));
      const ctx = c.getContext("2d");
      URL.revokeObjectURL(url);
      if (!ctx) {
        reject(new Error("canvas"));
        return;
      }
      ctx.drawImage(img, 0, 0, c.width, c.height);
      resolve(c.toDataURL("image/jpeg", 0.78));
    };
    img.onerror = () => {
      URL.revokeObjectURL(url);
      reject(new Error("image"));
    };
    img.src = url;
  });
}
