/**
 * Synchronous, module-level cache of the current session's JWT — mirrors
 * `api-base-url.ts`'s pattern, since every `services/*-api.ts` file builds
 * its fetch calls synchronously and can't `await` AsyncStorage or call a
 * React hook. `contexts/auth-context.tsx` is the only writer (kept in sync
 * with its own `token` state via a `useEffect`, covering login/logout/the
 * restored-session case on boot) — everything else just reads.
 */
let cachedToken: string | null = null;

export function getAuthToken(): string | null {
  return cachedToken;
}

export function setAuthToken(token: string | null): void {
  cachedToken = token;
}

/**
 * `authenticated-fetch.ts` calls this on any 401 response from an
 * authenticated call — `contexts/auth-context.tsx`'s `AuthProvider`
 * registers the actual handler (log out + redirect to `/login`) once, on
 * mount. Kept here instead of importing that `.tsx` file into every plain
 * `services/*-api.ts` module for a purely synchronous, non-React concern.
 */
type UnauthorizedListener = () => void;
let unauthorizedListener: UnauthorizedListener | null = null;

export function onUnauthorized(listener: UnauthorizedListener | null): void {
  unauthorizedListener = listener;
}

export function notifyUnauthorized(): void {
  unauthorizedListener?.();
}
