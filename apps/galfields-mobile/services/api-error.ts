/**
 * The backend's GlobalExceptionHandler always responds with
 * { timestamp, status, error, message } on errors (see
 * backend/pos/.../exception/GlobalExceptionHandler.java) — pull out the
 * human-readable `message` instead of showing the user raw JSON.
 */
export function parseApiErrorMessage(status: number, rawBody: string): string {
  if (!rawBody) return `Error ${status}`;
  try {
    const parsed = JSON.parse(rawBody);
    if (parsed && typeof parsed.message === 'string') return parsed.message;
  } catch {
    // Not JSON (e.g. a proxy/gateway error page) - fall back to raw text.
  }
  return rawBody;
}
