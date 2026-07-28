import { createRouter, createWebHashHistory } from 'vue-router'
import { useEmployeeSession, hasRoutePermission, firstAllowedRoute } from '../composables/useEmployeeSession'

const router = createRouter({
  history: createWebHashHistory(),
  routes: [
    {
      path: '/',
      redirect: '/pos',
    },
    {
      path: '/pos',
      component: () => import('../features/pos/POSView.vue'),
    },
    {
      path: '/inventario',
      component: () => import('../features/inventory/InventoryView.vue'),
    },
    {
      path: '/reportes',
      component: () => import('../features/reports/ReportsView.vue'),
    },
    {
      path: '/facturas',
      component: () => import('../features/invoices/components/InvoiceHistoryView.vue'),
    },
    {
      path: '/sync',
      component: () => import('../features/sync/SyncView.vue'),
    },
    {
      path: '/configuracion',
      component: () => import('../features/configuration/ConfigView.vue'),
    },
    {
      path: '/login',
      component: () => import('../features/auth/LoginView.vue'),
    },
  ],
})

// Employee login gate (spec 01-login-empleados-roles). Configuración is the
// one route that must stay reachable without a session — it's where
// `invoicing.terminal_code` itself gets configured, and login depends on
// that being set (see auth.rs::login), so gating Configuración too would
// make first-time setup impossible. Every other route needs both a session
// AND, per spec step 15, the logged-in role's permission for that specific
// route (see ROUTE_PERMISSIONS in useEmployeeSession.ts) — a role with
// reportes:false is authenticated but still can't reach /reportes.
router.beforeEach(async (to) => {
  const { isAuthenticated, loaded, loadSession, session } = useEmployeeSession()

  if (!loaded.value) {
    await loadSession()
  }

  if (to.path === '/configuracion') {
    return true
  }

  if (to.path === '/login') {
    return isAuthenticated.value ? '/pos' : true
  }

  if (!isAuthenticated.value) {
    return '/login'
  }

  if (!hasRoutePermission(session.value?.permissions, to.path)) {
    return firstAllowedRoute(session.value?.permissions)
  }

  return true
})

export default router
