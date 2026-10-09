export async function sha256Hex(text: string): Promise<string> {
  const data = new TextEncoder().encode(text);
  const buf = await crypto.subtle.digest("SHA-256", data);
  return Array.from(new Uint8Array(buf))
    .map((b) => b.toString(16).padStart(2, "0"))
    .join("");
}

export async function verifyPin(pin: string, hash: string): Promise<boolean> {
  const next = await sha256Hex(pin);
  if (next.length !== hash.length) return false;
  let mismatch = 0;
  for (let i = 0; i < next.length; i++) {
    mismatch |= next.charCodeAt(i) ^ hash.charCodeAt(i);
  }
  return mismatch === 0;
}
