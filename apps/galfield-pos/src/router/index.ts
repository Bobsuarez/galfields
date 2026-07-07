import { createRouter, createWebHashHistory } from 'vue-router'

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
      path: '/sync',
      component: () => import('../features/sync/SyncView.vue'),
    },
    {
      path: '/configuracion',
      component: () => import('../features/configuration/ConfigView.vue'),
    },
  ],
})

export default router
