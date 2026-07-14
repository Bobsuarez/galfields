<script setup lang="ts">
import type { Product } from '../../../types'
import { useProductCatalog } from '../composables/useProductCatalog'
import ProductCard from './ProductCard.vue'
import AppIcon from '../../../components/shared/AppIcon.vue'

const emit = defineEmits<{ (e: 'product-added', product: Product): void }>()

const { categories, activeCategory, searchQuery, filteredProducts, isLoading, selectCategory, loadProducts } = useProductCatalog()

// Exposed so POSView can re-fetch stock/active-state right after checkout,
// since useProducts() only loads once on mount otherwise (see CLAUDE.md).
defineExpose({ reload: loadProducts })
</script>

<template>
  <section class="catalog">
    <div class="catalog-header">
      <h2 class="catalog-title">Catálogo de Productos</h2>
      <div class="catalog-search">
        <AppIcon name="search" :size="13" class="cat-search-icon" />
        <input
          v-model="searchQuery"
          type="text"
          placeholder="Buscar en catálogo..."
          class="cat-search-input"
        />
      </div>
    </div>

    <div class="category-tabs">
      <button
        v-for="cat in categories"
        :key="cat.id"
        class="category-tab"
        :class="{ 'category-tab--active': activeCategory === cat.id }"
        @click="selectCategory(cat.id)"
      >
        {{ cat.name }}
      </button>
    </div>

    <div class="product-grid">
      <template v-if="isLoading">
        <div class="empty-state">
          <span style="font-size: 32px">⏳</span>
          <p>Cargando catálogo...</p>
        </div>
      </template>
      <template v-else>
        <ProductCard
          v-for="product in filteredProducts"
          :key="product.id"
          :product="product"
          @add="emit('product-added', $event)"
        />
        <div v-if="filteredProducts.length === 0 && searchQuery" class="empty-state">
          <span style="font-size: 32px">🔍</span>
          <p>Sin resultados para "{{ searchQuery }}"</p>
        </div>
        <div v-else-if="filteredProducts.length === 0" class="empty-state">
          <span style="font-size: 32px">📦</span>
          <p>Aún no hay productos sincronizados. Ve a Sincronización para descargar el catálogo.</p>
        </div>
      </template>
    </div>
  </section>
</template>

<style scoped>
.catalog {
  display: flex;
  flex-direction: column;
  height: 100%;
  overflow: hidden;
  background: var(--color-surface);
}

.catalog-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 16px 8px;
  gap: 12px;
  flex-shrink: 0;
}

.catalog-title {
  font-size: 14px;
  font-weight: 600;
  color: var(--color-cream);
  white-space: nowrap;
}

.catalog-search {
  display: flex;
  align-items: center;
  background: var(--color-surface-2);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-sm);
  padding: 5px 10px;
  gap: 6px;
  flex: 1;
  max-width: 260px;
  transition: border-color 0.2s;
}

.catalog-search:focus-within {
  border-color: var(--color-primary);
}

.cat-search-icon {
  color: var(--color-text-muted);
}

.cat-search-input {
  background: transparent;
  border: none;
  color: var(--color-text);
  font-size: 12px;
  flex: 1;
}

.cat-search-input::placeholder {
  color: var(--color-text-dim);
}

.category-tabs {
  display: flex;
  gap: 6px;
  padding: 0 16px 10px;
  flex-shrink: 0;
  overflow-x: auto;
}

.category-tabs::-webkit-scrollbar {
  height: 2px;
}

.category-tab {
  padding: 5px 12px;
  border-radius: 20px;
  font-size: 12px;
  font-weight: 500;
  background: var(--color-surface-2);
  border: 1px solid var(--color-border);
  color: var(--color-text-muted);
  white-space: nowrap;
  transition: all 0.15s;
  cursor: pointer;
}

.category-tab:hover {
  border-color: var(--color-primary);
  color: var(--color-primary);
}

.category-tab--active {
  background: var(--color-primary);
  border-color: var(--color-primary);
  color: #0D0D0D;
  font-weight: 600;
}

/* Fixed-size columns (matching ProductCard's own fixed width) instead of
   `1fr` — cards keep a constant size as the window resizes, wrapping into
   more/fewer columns rather than stretching. */
.product-grid {
  flex: 1;
  overflow-y: auto;
  padding: 0 16px 16px;
  display: grid;
  grid-template-columns: repeat(auto-fill, 176px);
  justify-content: start;
  gap: 10px;
  align-content: start;
}

.empty-state {
  grid-column: 1 / -1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 10px;
  padding: 40px;
  color: var(--color-text-muted);
  font-size: 13px;
}
</style>
