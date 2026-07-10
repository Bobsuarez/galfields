<script setup lang="ts">
import type { Product, ProductCategory, StockStatus } from '../../../types'
import { formatCurrency } from '../../../utils/currency'
import { deriveStockStatus } from '../../../utils/stock'
import AppIcon from '../../../components/shared/AppIcon.vue'

defineProps<{
  products: Product[]
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
  (e: 'select', product: Product): void
  (e: 'category', id: string): void
  (e: 'prev'): void
  (e: 'next'): void
  (e: 'page', n: number): void
}>()

const STATUS_LABEL: Record<StockStatus, string> = {
  'en-stock': 'En stock',
  'stock-bajo': 'Stock bajo',
  'sin-stock': 'Sin stock',
}

const CATEGORY_STYLE_PALETTE = [
  { bg: 'rgba(33,150,243,0.15)', color: '#64B5F6' },
  { bg: 'rgba(76,175,80,0.15)', color: '#81C784' },
  { bg: 'rgba(255,152,0,0.15)', color: '#FFB74D' },
  { bg: 'rgba(100,181,246,0.12)', color: '#90CAF9' },
  { bg: 'rgba(0,188,212,0.12)', color: '#4DD0E1' },
  { bg: 'rgba(158,158,158,0.12)', color: '#BDBDBD' },
]

// Categories are free-text from the cloud catalog now (not a fixed local
// list), so colors are assigned deterministically by name instead of a
// hardcoded id→style map.
function categoryStyle(cat: string) {
  if (!cat) return CATEGORY_STYLE_PALETTE[CATEGORY_STYLE_PALETTE.length - 1]
  let hash = 0
  for (let i = 0; i < cat.length; i++) hash = (hash * 31 + cat.charCodeAt(i)) | 0
  return CATEGORY_STYLE_PALETTE[Math.abs(hash) % CATEGORY_STYLE_PALETTE.length]
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
            <th class="col-id">Código</th>
            <th class="col-name">Producto</th>
            <th class="col-cat">Categoría</th>
            <th class="col-num">Stock</th>
            <th class="col-price">Precio</th>
            <th class="col-status">Estado</th>
          </tr>
        </thead>
        <tbody>
          <tr
            v-for="product in products"
            :key="product.id"
            class="table-row"
            :class="{ 'table-row--selected': selectedId === product.id, 'table-row--inactive': !product.isActive }"
            @click="emit('select', product)"
          >
            <td class="col-id">
              <span class="barcode">{{ product.barcode }}</span>
            </td>
            <td class="col-name">
              <span class="product-name">{{ product.productName }}</span>
            </td>
            <td class="col-cat">
              <span
                class="cat-badge"
                :style="{ background: categoryStyle(product.category).bg, color: categoryStyle(product.category).color }"
              >
                {{ product.category || 'Sin categoría' }}
              </span>
            </td>
            <td class="col-num">{{ product.stockQuantity }}</td>
            <td class="col-price">{{ formatCurrency(product.unitPrice) }}</td>
            <td class="col-status">
              <span v-if="!product.isActive" class="status-badge status--inactive">Desactivado</span>
              <span v-else class="status-badge" :class="statusClass(deriveStockStatus(product.stockQuantity))">
                {{ STATUS_LABEL[deriveStockStatus(product.stockQuantity)] }}
              </span>
            </td>
          </tr>
          <tr v-if="products.length === 0">
            <td colspan="6" class="empty-row">
              <AppIcon name="search" :size="20" />
              <span>Sin resultados</span>
            </td>
          </tr>
        </tbody>
      </table>
    </div>

    <div class="pagination">
      <span class="pagination-info">
        Mostrando {{ totalCount === 0 ? 0 : (currentPage - 1) * pageSize + 1 }}–{{ Math.min(currentPage * pageSize, totalCount) }} de {{ totalCount }} productos
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

/* Deactivated from the cloud (mobile app) or dropped out of the last sync's
   feed entirely - dimmed so it visually reads as "not sellable" without
   hiding it (still needs to show up for inventory visibility/history). */
.table-row--inactive { opacity: 0.5; }
.table-row--inactive:hover { background: rgba(242, 141, 53, 0.03); }

.inv-table td {
  padding: 9px 10px;
  vertical-align: middle;
}

.col-id { width: 110px; }
.col-cat { width: 130px; }
.col-num { width: 68px; text-align: center; }
.col-price { width: 100px; text-align: right; color: var(--color-primary); font-weight: 600; }
.col-status { width: 100px; }

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

.status--inactive {
  background: rgba(158, 158, 158, 0.2);
  color: #BDBDBD;
}

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
