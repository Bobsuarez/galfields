<script setup lang="ts">
import type { InventoryProduct, ProductCategory, StockStatus } from '../../../types'
import { formatCurrency } from '../../../utils/currency'
import AppIcon from '../../../components/shared/AppIcon.vue'
import { deriveStatus } from '../composables/useInventory'

defineProps<{
  products: InventoryProduct[]
  categories: ProductCategory[]
  activeCategory: string
  selectedId: string | null
  searchQuery: string
  currentPage: number
  totalPages: number
  totalCount: number
  pageSize: number
}>()

const emit = defineEmits<{
  (e: 'update:searchQuery', value: string): void
  (e: 'update:pageSize', value: number): void
  (e: 'select', product: InventoryProduct): void
  (e: 'delete', id: string): void
  (e: 'category', id: string): void
  (e: 'prev'): void
  (e: 'next'): void
  (e: 'page', n: number): void
  (e: 'new'): void
}>()

const STATUS_LABEL: Record<StockStatus, string> = {
  'en-stock': 'En stock',
  'stock-bajo': 'Stock bajo',
  'sin-stock': 'Sin stock',
}

const CATEGORY_STYLE: Record<string, { bg: string; color: string }> = {
  bebidas:   { bg: 'rgba(33,150,243,0.15)',  color: '#64B5F6' },
  alimentos: { bg: 'rgba(76,175,80,0.15)',   color: '#81C784' },
  snacks:    { bg: 'rgba(255,152,0,0.15)',   color: '#FFB74D' },
  lacteos:   { bg: 'rgba(100,181,246,0.12)', color: '#90CAF9' },
  limpieza:  { bg: 'rgba(0,188,212,0.12)',   color: '#4DD0E1' },
  otros:     { bg: 'rgba(158,158,158,0.12)', color: '#BDBDBD' },
}

function categoryStyle(cat: string) {
  return CATEGORY_STYLE[cat] ?? CATEGORY_STYLE.otros
}

function statusClass(status: StockStatus) {
  return {
    'status--ok': status === 'en-stock',
    'status--warn': status === 'stock-bajo',
    'status--err': status === 'sin-stock',
  }
}

const PAGE_SIZES = [10, 25, 50]

function visiblePages(current: number, total: number): (number | '...')[] {
  if (total <= 5) return Array.from({ length: total }, (_, i) => i + 1)
  const pages: (number | '...')[] = [1]
  if (current > 3) pages.push('...')
  for (let i = Math.max(2, current - 1); i <= Math.min(total - 1, current + 1); i++) pages.push(i)
  if (current < total - 2) pages.push('...')
  pages.push(total)
  return pages
}
</script>

<template>
  <div class="inv-table-panel">
    <div class="inv-toolbar">
      <div class="inv-search-wrap">
        <AppIcon name="search" :size="13" class="inv-search-icon" />
        <input
          :value="searchQuery"
          @input="emit('update:searchQuery', ($event.target as HTMLInputElement).value)"
          type="text"
          placeholder="Busca tu inventario..."
          class="inv-search"
        />
      </div>
      <div class="inv-toolbar-right">
        <button class="toolbar-btn" title="Importar">
          <AppIcon name="refresh" :size="14" />
          Importar
        </button>
        <button class="toolbar-btn" title="Filtros">
          <AppIcon name="settings" :size="14" />
          Filtros
        </button>
        <button class="toolbar-btn toolbar-btn--primary" @click="emit('new')">
          <AppIcon name="plus" :size="14" />
          Nuevo Producto
        </button>
      </div>
    </div>

    <div class="category-tabs">
      <button
        v-for="cat in categories"
        :key="cat.id"
        class="cat-tab"
        :class="{ 'cat-tab--active': activeCategory === cat.id }"
        @click="emit('category', cat.id)"
      >
        {{ cat.name }}
      </button>
    </div>

    <div class="table-wrap">
      <table class="inv-table">
        <thead>
          <tr>
            <th class="col-id">#</th>
            <th class="col-name">Producto</th>
            <th class="col-cat">Categoría</th>
            <th class="col-num">Stock</th>
            <th class="col-num">Ventas</th>
            <th class="col-num">Stock Mín.</th>
            <th class="col-price">Precio Venta</th>
            <th class="col-actions">Acciones</th>
          </tr>
        </thead>
        <tbody>
          <tr
            v-for="product in products"
            :key="product.id"
            class="table-row"
            :class="{ 'table-row--selected': selectedId === product.id }"
            @click="emit('select', product)"
          >
            <td class="col-id">
              <span class="barcode">{{ product.barcode }}</span>
            </td>
            <td class="col-name">
              <span class="product-name">{{ product.name }}</span>
            </td>
            <td class="col-cat">
              <span
                class="cat-badge"
                :style="{ background: categoryStyle(product.category).bg, color: categoryStyle(product.category).color }"
              >
                {{ categories.find(c => c.id === product.category)?.name ?? product.category }}
              </span>
            </td>
            <td class="col-num">{{ product.currentStock }}</td>
            <td class="col-num text-muted">{{ product.salesCount }}</td>
            <td class="col-num text-muted">{{ product.minStock }}</td>
            <td class="col-price">{{ formatCurrency(product.salePrice) }}</td>
            <td class="col-actions" @click.stop>
              <span class="status-badge" :class="statusClass(deriveStatus(product))">
                {{ STATUS_LABEL[deriveStatus(product)] }}
              </span>
              <button class="action-icon" title="Editar" @click="emit('select', product)">
                <AppIcon name="clipboard" :size="13" />
              </button>
              <button class="action-icon action-icon--danger" title="Eliminar" @click="emit('delete', product.id)">
                <AppIcon name="x" :size="13" />
              </button>
            </td>
          </tr>
          <tr v-if="products.length === 0">
            <td colspan="8" class="empty-row">
              <AppIcon name="search" :size="20" />
              <span>Sin resultados</span>
            </td>
          </tr>
        </tbody>
      </table>
    </div>

    <div class="pagination">
      <span class="pagination-info">
        Mostrando {{ (currentPage - 1) * pageSize + 1 }}–{{ Math.min(currentPage * pageSize, totalCount) }} de {{ totalCount }} productos
      </span>
      <div class="pagination-controls">
        <button class="page-btn" :disabled="currentPage === 1" @click="emit('prev')">
          <AppIcon name="chevron-right" :size="12" style="transform: rotate(180deg)" />
        </button>
        <template v-for="p in visiblePages(currentPage, totalPages)" :key="String(p)">
          <button
            v-if="p !== '...'"
            class="page-btn"
            :class="{ 'page-btn--active': p === currentPage }"
            @click="emit('page', p as number)"
          >{{ p }}</button>
          <span v-else class="page-ellipsis">…</span>
        </template>
        <button class="page-btn" :disabled="currentPage === totalPages" @click="emit('next')">
          <AppIcon name="chevron-right" :size="12" />
        </button>
      </div>
      <select
        :value="pageSize"
        @change="emit('update:pageSize', Number(($event.target as HTMLSelectElement).value))"
        class="page-size-select"
      >
        <option v-for="s in PAGE_SIZES" :key="s" :value="s">{{ s }} por página</option>
      </select>
    </div>
  </div>
</template>

<style scoped>
.inv-table-panel {
  display: flex;
  flex-direction: column;
  height: 100%;
  overflow: hidden;
}

.inv-toolbar {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px 16px 8px;
  flex-shrink: 0;
}

.inv-search-wrap {
  flex: 1;
  display: flex;
  align-items: center;
  gap: 8px;
  background: var(--color-surface-2);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-sm);
  padding: 7px 12px;
  transition: border-color 0.2s;
}

.inv-search-wrap:focus-within {
  border-color: var(--color-primary);
}

.inv-search-icon { color: var(--color-text-muted); }

.inv-search {
  flex: 1;
  background: transparent;
  border: none;
  color: var(--color-text);
  font-size: 13px;
}

.inv-search::placeholder { color: var(--color-text-dim); }

.inv-toolbar-right {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-shrink: 0;
}

.toolbar-btn {
  display: flex;
  align-items: center;
  gap: 5px;
  padding: 7px 12px;
  background: var(--color-surface-2);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-sm);
  color: var(--color-text-muted);
  font-size: 12px;
  font-weight: 500;
  transition: all 0.15s;
  cursor: pointer;
}

.toolbar-btn:hover { border-color: var(--color-primary); color: var(--color-primary); }

.toolbar-btn--primary {
  background: var(--color-primary);
  border-color: var(--color-primary);
  color: #0D0D0D;
  font-weight: 600;
}

.toolbar-btn--primary:hover { background: var(--color-primary-hover); color: #0D0D0D; }

.category-tabs {
  display: flex;
  gap: 4px;
  padding: 0 16px 8px;
  overflow-x: auto;
  flex-shrink: 0;
}

.category-tabs::-webkit-scrollbar { height: 2px; }

.cat-tab {
  padding: 4px 10px;
  border-radius: 16px;
  font-size: 11.5px;
  font-weight: 500;
  background: var(--color-surface-2);
  border: 1px solid var(--color-border);
  color: var(--color-text-muted);
  white-space: nowrap;
  cursor: pointer;
  transition: all 0.15s;
}

.cat-tab:hover { border-color: var(--color-primary); color: var(--color-primary); }

.cat-tab--active {
  background: var(--color-primary);
  border-color: var(--color-primary);
  color: #0D0D0D;
  font-weight: 600;
}

.table-wrap {
  flex: 1;
  overflow: auto;
  padding: 0 16px;
}

.inv-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 12.5px;
}

.inv-table th {
  padding: 8px 10px;
  text-align: left;
  color: var(--color-text-muted);
  font-size: 11px;
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.4px;
  border-bottom: 1px solid var(--color-border);
  position: sticky;
  top: 0;
  background: var(--color-surface);
}

.table-row {
  cursor: pointer;
  transition: background 0.1s;
  border-bottom: 1px solid rgba(242,141,53,0.06);
}

.table-row:hover { background: rgba(242, 141, 53, 0.05); }
.table-row--selected { background: rgba(242, 141, 53, 0.1); }

.inv-table td {
  padding: 9px 10px;
  vertical-align: middle;
}

.col-id { width: 72px; }
.col-cat { width: 110px; }
.col-num { width: 68px; text-align: center; }
.col-price { width: 100px; text-align: right; }
.col-actions { width: 160px; }

.barcode {
  font-size: 11px;
  color: var(--color-text-muted);
  font-variant-numeric: tabular-nums;
  font-family: monospace;
}

.product-name {
  font-weight: 500;
  color: var(--color-cream);
}

.cat-badge {
  display: inline-block;
  padding: 2px 8px;
  border-radius: 10px;
  font-size: 11px;
  font-weight: 500;
}

.text-muted { color: var(--color-text-muted); }

.col-price { color: var(--color-primary); font-weight: 600; }

.col-actions {
  display: flex;
  align-items: center;
  gap: 6px;
}

.status-badge {
  display: inline-block;
  padding: 2px 8px;
  border-radius: 10px;
  font-size: 10.5px;
  font-weight: 600;
  flex-shrink: 0;
}

.status--ok {
  background: rgba(76, 175, 80, 0.15);
  color: #81C784;
}

.status--warn {
  background: rgba(255, 152, 0, 0.15);
  color: #FFB74D;
}

.status--err {
  background: rgba(229, 57, 53, 0.15);
  color: #EF5350;
}

.action-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 26px;
  height: 26px;
  background: transparent;
  border: 1px solid var(--color-border);
  border-radius: 5px;
  color: var(--color-text-muted);
  cursor: pointer;
  transition: all 0.15s;
  flex-shrink: 0;
}

.action-icon:hover { border-color: var(--color-primary); color: var(--color-primary); }
.action-icon--danger:hover { border-color: var(--color-danger); color: var(--color-danger); }

.empty-row {
  text-align: center;
  padding: 40px;
  color: var(--color-text-muted);
}

.empty-row { display: flex; flex-direction: column; align-items: center; gap: 8px; }

.pagination {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 16px;
  border-top: 1px solid var(--color-border);
  flex-shrink: 0;
  gap: 12px;
}

.pagination-info {
  font-size: 11px;
  color: var(--color-text-muted);
  white-space: nowrap;
}

.pagination-controls {
  display: flex;
  align-items: center;
  gap: 3px;
}

.page-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  min-width: 28px;
  height: 28px;
  padding: 0 6px;
  background: var(--color-surface-2);
  border: 1px solid var(--color-border);
  border-radius: 5px;
  color: var(--color-text-muted);
  font-size: 12px;
  cursor: pointer;
  transition: all 0.15s;
}

.page-btn:hover:not(:disabled) { border-color: var(--color-primary); color: var(--color-primary); }
.page-btn--active { background: var(--color-primary); border-color: var(--color-primary); color: #0D0D0D; font-weight: 700; }
.page-btn:disabled { opacity: 0.35; cursor: not-allowed; }
.page-ellipsis { color: var(--color-text-muted); font-size: 12px; padding: 0 3px; }

.page-size-select {
  background: var(--color-surface-2);
  border: 1px solid var(--color-border);
  border-radius: 5px;
  color: var(--color-text-muted);
  font-size: 11px;
  padding: 5px 8px;
  cursor: pointer;
}
</style>
