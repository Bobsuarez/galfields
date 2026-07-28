import { getAuthToken, notifyUnauthorized } from './auth-token';

/**
 * Thin wrapper around `fetch()` that attaches `Authorization: Bearer <jwt>`
 * when a session is active, and triggers a global logout + redirect to
 * `/login` (see `contexts/auth-context.tsx`) on any 401 response — shared
 * by every `services/*-api.ts` file that calls an authenticated backend
 * endpoint (see `backend/pos`'s CLAUDE.md, "Authorization", for which ones
 * those are). `POST /api/auth/login` itself (`services/auth-api.ts`) is the
 * one backend call site that deliberately stays on plain `fetch()` — it's
 * always public, and a 401 there means "wrong password", not "your session
 * expired", so it must never trigger the same global-logout handler.
 */
export async function authenticatedFetch(url: string, init?: RequestInit): Promise<Response> {
  const token = getAuthToken();
  const headers = new Headers(init?.headers);
  if (token) {
    headers.set('Authorization', `Bearer ${token}`);
  }

  const response = await fetch(url, { ...init, headers });
  if (response.status === 401) {
    notifyUnauthorized();
  }
  return response;
}
