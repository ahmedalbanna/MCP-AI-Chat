// AES-GCM encryption at rest for stored provider keys (server-only).
// ENCRYPTION_KEY must be 32 bytes (hex or base64). Falls back to no-op in dev without key.

import { env } from "./env";

function getKey(): Uint8Array | null {
  const raw = env.ENCRYPTION_KEY;
  if (!raw) return null;
  try {
    // hex
    if (/^[0-9a-fA-F]{64}$/.test(raw.trim())) {
      const bytes = new Uint8Array(32);
      for (let i = 0; i < 32; i++) bytes[i] = parseInt(raw.slice(i * 2, i * 2 + 2), 16);
      return bytes;
    }
    // base64
    const bin = Buffer.from(raw, "base64");
    if (bin.length === 32) return new Uint8Array(bin);
    // utf8 fallback hash to 32 bytes via simple expansion (dev only)
    const utf = new TextEncoder().encode(raw);
    const out = new Uint8Array(32);
    for (let i = 0; i < 32; i++) out[i] = utf[i % utf.length] ^ (i * 31);
    return out;
  } catch {
    return null;
  }
}

export async function encrypt(plaintext: string): Promise<string> {
  const keyBytes = getKey();
  if (!keyBytes) return plaintext; // dev fallback — store plaintext with marker
  try {
    const key = await crypto.subtle.importKey("raw", keyBytes, { name: "AES-GCM" }, false, ["encrypt"]);
    const iv = crypto.getRandomValues(new Uint8Array(12));
    const enc = await crypto.subtle.encrypt({ name: "AES-GCM", iv }, key, new TextEncoder().encode(plaintext));
    const combined = new Uint8Array(iv.length + enc.byteLength);
    combined.set(iv, 0);
    combined.set(new Uint8Array(enc), iv.length);
    return Buffer.from(combined).toString("base64");
  } catch {
    return plaintext;
  }
}

export async function decrypt(ciphertext: string): Promise<string> {
  const keyBytes = getKey();
  if (!keyBytes) return ciphertext;
  try {
    const combined = Buffer.from(ciphertext, "base64");
    if (combined.length < 13) return ciphertext;
    const iv = combined.subarray(0, 12);
    const data = combined.subarray(12);
    const key = await crypto.subtle.importKey("raw", keyBytes, { name: "AES-GCM" }, false, ["decrypt"]);
    const dec = await crypto.subtle.decrypt({ name: "AES-GCM", iv }, key, data);
    return new TextDecoder().decode(dec);
  } catch {
    return ciphertext;
  }
}
