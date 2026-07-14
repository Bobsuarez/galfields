/** RFC4122-ish v4 UUID via `Math.random()` — good enough for a
 * client-generated idempotency key (see `services/inventory-api.ts`), not
 * meant to be cryptographically secure. Avoids adding `expo-crypto` as a new
 * dependency just for this. */
export function generateUuid(): string {
  return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, c => {
    const r = (Math.random() * 16) | 0;
    const v = c === 'x' ? r : (r & 0x3) | 0x8;
    return v.toString(16);
  });
}
