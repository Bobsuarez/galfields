<script setup lang="ts">
import { useRouter, useRoute } from "vue-router";
import AppIcon from "./AppIcon.vue";
import type { NavItem } from "../../types";
import { useSidebarBanner } from "../../composables/useSidebarBanner";
import { formatCurrency, formatDate, formatTime } from "../../utils/currency";

const router = useRouter();
const route = useRoute();

const { activeView, lowStockItems, todaySalesTotal, bannerTimestamp, mascotSrc } =
  useSidebarBanner();

const navItems: NavItem[] = [
  { id: "pos", label: "Inicio", icon: "home", route: "/sync" },
  //{ id: "productos", label: "Productos", icon: "package", route: "/productos" },
  { id: "ventas", label: "Ventas", icon: "cart", route: "/pos" },
 // { id: "clientes", label: "Clientes", icon: "users", route: "/clientes" },
  {
    id: "inventario",
    label: "Inventario",
    icon: "clipboard",
    route: "/inventario",
  },
  { id: "reportes", label: "Reportes", icon: "bar-chart", route: "/reportes" },
 /* {
    id: "proveedores",
    label: "Proveedores",
    icon: "truck",
    route: "/proveedores",
  },*/
  //{ id: "compras", label: "Compras", icon: "shopping-bag", route: "/compras" },
  {
    id: "configuracion",
    label: "Configuración",
    icon: "settings",
    route: "/configuracion",
  },
];

const stubRoutes = [
  "/productos",
  "/ventas",
  "/clientes",
  "/proveedores",
  "/compras",
];

function isActive(item: NavItem): boolean {
  return route.path === item.route || route.path.startsWith(item.route + "/");
}

function navigate(item: NavItem) {
  if (!stubRoutes.includes(item.route)) {
    router.push(item.route);
  }
}
</script>

<template>
  <aside class="sidebar">
    <nav class="sidebar-nav">
      <button
        v-for="item in navItems"
        :key="item.id"
        class="nav-item"
        :class="{ 'nav-item--active': isActive(item) }"
        @click="navigate(item)"
      >
        <AppIcon :name="item.icon" :size="17" class="nav-icon" />
        <span class="nav-label">{{ item.label }}</span>
        <div v-if="isActive(item)" class="nav-indicator" />
      </button>
    </nav>

    <div class="sidebar-mascot">
      <Transition name="fade" mode="out-in">
        <img :key="mascotSrc" :src="mascotSrc" alt="Garfield" width="100%" height="100%" />
      </Transition>
    </div>

    <div class="sidebar-banner">
      <Transition name="fade" mode="out-in">
        <div v-if="activeView === 'stock'" key="stock" class="info-card info-card--list">
          <div class="info-icon">
            <AppIcon name="alert-triangle" :size="14" />
          </div>
          <div class="info-body">
            <p class="info-label">Stock Bajo</p>
            <p v-if="lowStockItems.length === 0" class="alerts-empty">Sin alertas de stock</p>
            <div
              v-for="alert in lowStockItems"
              :key="alert.productName"
              class="alert-row"
            >
              <span class="alert-name">{{ alert.productName }}</span>
              <span
                class="alert-stock"
                :class="alert.currentStock <= 1 ? 'critical' : 'warning'"
              >
                {{ alert.currentStock }}
              </span>
            </div>
          </div>
        </div>

        <div v-else key="sales" class="sales-cards">
          <div class="info-card">
            <div class="info-icon">
              <AppIcon name="calendar" :size="16" />
            </div>
            <div class="info-body">
              <p class="info-line info-line--muted">{{ formatDate(bannerTimestamp) }}</p>
              <p class="info-line info-line--strong">{{ formatTime(bannerTimestamp) }}</p>
            </div>
          </div>

          <div class="info-card">
            <div class="info-icon">
              <AppIcon name="bar-chart" :size="16" />
            </div>
            <div class="info-body">
              <p class="info-label">Venta de Hoy</p>
              <p class="info-value">{{ formatCurrency(todaySalesTotal) }}</p>
            </div>
          </div>
        </div>
      </Transition>
    </div>
  </aside>
</template>

<style scoped>
.sidebar {
  width: var(--sidebar-width);
  height: 100%;
  background: var(--color-bg);
  border-right: 1px solid var(--color-border);
  display: flex;
  flex-direction: column;
  overflow: hidden;
  flex-shrink: 0;
}

.sidebar-nav {
  flex: 1;
  padding: 8px 0;
  overflow-y: auto;
}

.nav-item {
  position: relative;
  display: flex;
  align-items: center;
  gap: 10px;
  width: 100%;
  padding: 10px 16px;
  background: transparent;
  color: var(--color-text-muted);
  border-radius: 0;
  text-align: left;
  cursor: pointer;
  transition:
    color 0.15s,
    background 0.15s;
}

.nav-item:hover {
  background: rgba(242, 141, 53, 0.07);
  color: var(--color-cream);
}

.nav-item--active {
  color: var(--color-primary);
  background: rgba(242, 141, 53, 0.1);
}

.nav-icon {
  flex-shrink: 0;
}

.nav-label {
  font-size: 12.5px;
  font-weight: 500;
  white-space: nowrap;
}

.nav-indicator {
  position: absolute;
  left: 0;
  top: 50%;
  transform: translateY(-50%);
  width: 3px;
  height: 20px;
  background: var(--color-primary);
  border-radius: 0 2px 2px 0;
}

.sidebar-mascot {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 100px 0 8px;
}

.mascot-img {
  font-size: 52px;
  line-height: 1;
  filter: drop-shadow(0 2px 8px rgba(242, 141, 53, 0.3));
}

.sidebar-banner {
  padding: 8px 10px 10px;
  border-top: 1px solid var(--color-border);
  min-height: 128px;
}

.info-card {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 12px;
  background: var(--color-surface-2);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
}

.info-card--list {
  align-items: flex-start;
}

.info-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 30px;
  height: 30px;
  flex-shrink: 0;
  border-radius: var(--radius-sm);
  background: rgba(242, 141, 53, 0.15);
  color: var(--color-primary);
}

.info-body {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.info-label {
  font-size: 10px;
  font-weight: 700;
  color: var(--color-text-muted);
  text-transform: uppercase;
  letter-spacing: 0.4px;
}

.info-value {
  font-size: 13px;
  font-weight: 800;
  color: var(--color-cream);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.info-line {
  font-size: 11.5px;
}

.info-line--muted {
  font-weight: 600;
  color: var(--color-text-muted);
}

.info-line--strong {
  font-size: 13px;
  font-weight: 800;
  color: var(--color-cream);
}

.sales-cards {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.alerts-empty {
  font-size: 11px;
  color: var(--color-text-dim);
  padding: 2px 0;
}

.alert-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 3px 0;
}

.alert-name {
  font-size: 11px;
  color: var(--color-text-muted);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  flex: 1;
}

.alert-stock {
  font-size: 11px;
  font-weight: 700;
  padding: 1px 6px;
  border-radius: 10px;
  flex-shrink: 0;
}

.alert-stock.warning {
  background: rgba(255, 152, 0, 0.15);
  color: var(--color-warning);
}

.alert-stock.critical {
  background: rgba(229, 57, 53, 0.15);
  color: var(--color-danger);
}

.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.4s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}
</style>
