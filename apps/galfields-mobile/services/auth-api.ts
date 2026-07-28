import { apiBaseUrl } from './api-base-url';
import { parseApiErrorMessage } from './api-error';

/** Mirrors backend/pos's LoginResponse (see that repo's CLAUDE.md, "Employee
 * login / JWT") - the backend returns the employee/role info directly
 * alongside the token so callers don't need to decode the JWT just to
 * populate the session. */
export interface LoginResponse {
  token: string;
  employeeId: number;
  username: string;
  roleId: number;
  roleName: string;
  permissions: Record<string, boolean>;
  /** Always null from mobile - only desktop logins (which send terminalCode) get one. */
  terminalId: number | null;
}

/** POST /api/auth/login with no terminalCode - the backend treats that as a
 * mobile login, and rejects (401) any employee whose role has
 * can_login_mobile=false, e.g. a Cajero. */
export async function login(username: string, password: string): Promise<LoginResponse> {
  const path = '/api/auth/login';
  const url = `${apiBaseUrl()}${path}`;
  const response = await fetch(url, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ username, password }),
  });

  if (!response.ok) {
    const text = await response.text().catch(() => '');
    console.error(`[auth-api] POST ${path} -> ${response.status}`, text);
    throw new Error(parseApiErrorMessage(response.status, text));
  }

  const result = (await response.json()) as LoginResponse;
  console.log(`[auth-api] POST ${path} -> ${response.status}`);
  return result;
}
