import { reactive, computed } from 'vue'
import { invoke } from '@tauri-apps/api/core'

// Spec 01-login-empleados-roles. Mirrors src-tauri/src/auth.rs's
// EmployeeSession (camelCase via serde) — the JWT itself never crosses the
// Tauri IPC boundary, only what the UI actually needs to know.
export interface EmployeeSessionData {
  employeeId: number
  username: string
  roleName: string
  permissions: Record<string, boolean>
  /** ISO datetime, always the next midnight in America/Bogota (see
   * auth.rs). Checked against "now" entirely on the Rust side
   * (`auth::session_if_valid`, called by the `get_session` command) — by
   * the time a session reaches this composable it's already known-valid;
   * an expired one comes back as `null`, not a session with a past date. */
  expiresAt: string
}

/** Route path -> permission key required to reach it (spec
 * 01-login-empleados-roles, step 15). `/configuracion` and `/login` are
 * deliberately absent — both bypass this check entirely in the router
 * guard (router/index.ts), regardless of session/permissions.
 * `/inventario` has no backend endpoint of its own to gate server-side
 * (see backend/pos's CLAUDE.md: "inventario ... no llama al backend
 * directamente") — this map is the *only* place that permission is ever
 * enforced. `/facturas` (Historial de facturas) maps to `reportes` since
 * it's the same financial-visibility concern as the Reportes module,
 * even though it's backed by local `sale_history.rs` queries, not a
 * direct cloud reports call. Shared by the router guard and
 * AppSidebar.vue's nav filtering so the two never drift apart. */
export const ROUTE_PERMISSIONS: Record<string, string> = {
  '/pos': 'pos',
  '/inventario': 'inventario',
  '/reportes': 'reportes',
  '/facturas': 'reportes',
  '/sync': 'sync',
}

/** True if `path` needs no permission at all (not in the map above), or the
 * given permissions grant the one it does need. */
export function hasRoutePermission(permissions: Record<string, boolean> | undefined, path: string): boolean {
  const required = ROUTE_PERMISSIONS[path]
  if (!required) return true
  return !!permissions?.[required]
}

/** Where to send a logged-in user who just got blocked from a route their
 * role doesn't allow — the first permission-gated route they *do* have, or
 * Configuración (always reachable) if none. Order matches ROUTE_PERMISSIONS'
 * declaration order above. */
export function firstAllowedRoute(permissions: Record<string, boolean> | undefined): string {
  for (const path of Object.keys(ROUTE_PERMISSIONS)) {
    if (hasRoutePermission(permissions, path)) return path
  }
  return '/configuracion'
}

// ── Module-level singleton ──────────────────────────────────────────────────
// Shared across the entire app without Pinia, same pattern as useAppConfig.ts.

const state = reactive<{ session: EmployeeSessionData | null; loaded: boolean }>({
  session: null,
  loaded: false,
})

// Dedups concurrent loadSession() callers (App.vue's onMounted and the
// router guard both call it) into a single invoke('get_session') round trip.
let loadPromise: Promise<void> | null = null

async function fetchSession(): Promise<void> {
  try {
    state.session = await invoke<EmployeeSessionData | null>('get_session')
  } catch (e) {
    console.error('[employee-session] failed to load cached session:', e)
    state.session = null
  } finally {
    state.loaded = true
  }
}

export function useEmployeeSession() {
  function loadSession(): Promise<void> {
    loadPromise ??= fetchSession()
    return loadPromise
  }

  /** Unlike `loadSession()`, always performs a fresh `invoke('get_session')`
   * — called when the window regains focus (`App.vue`, spec step 17's
   * "foreground" half), since the whole point is to catch a session that
   * was valid at boot but has expired since (e.g. the app sat backgrounded
   * overnight). `get_session` itself enforces `expires_at` on the Rust
   * side, so this just re-syncs whatever it now says. */
  function refreshSession(): Promise<void> {
    return fetchSession()
  }

  /** Throws on invalid credentials/network failure — callers (LoginView)
   * catch and show the message, same convention as every other invoke()
   * call site in this codebase. */
  async function login(username: string, password: string): Promise<void> {
    state.session = await invoke<EmployeeSessionData>('login', { username, password })
    state.loaded = true
  }

  async function logout(): Promise<void> {
    await invoke('logout')
    state.session = null
  }

  return {
    session: computed(() => state.session),
    isAuthenticated: computed(() => state.session !== null),
    loaded: computed(() => state.loaded),
    loadSession,
    refreshSession,
    login,
    logout,
  }
}
