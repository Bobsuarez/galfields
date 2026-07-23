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

    <div class="catalog-body">
      <nav class="category-rail" aria-label="Categorías">
        <button
          v-for="cat in categories"
          :key="cat.id"
          class="category-rail-item"
          :class="{ 'category-rail-item--active': activeCategory === cat.id }"
          :title="cat.name"
          @click="selectCategory(cat.id)"
        >
          <span v-if="activeCategory === cat.id" class="category-rail-indicator" />
          <span class="category-rail-name">{{ cat.name }}</span>
          <span class="category-rail-count">{{ cat.count }}</span>
        </button>
      </nav>

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

/* Category filter is a vertical rail (not horizontal tabs): with a long,
   cloud-derived category list, horizontal scrolling meant reaching the last
   one took repeated scrolling. A vertical list scrolls independently of the
   product grid and every category is one glance away, regardless of count. */
.catalog-body {
  flex: 1;
  display: flex;
  overflow: hidden;
  min-height: 0;
}

.category-rail {
  width: 15%;
  flex-shrink: 0;
  overflow-y: auto;
  padding: 2px 8px 16px;
  display: flex;
  flex-direction: column;
  gap: 1px;
  border-right: 1px solid var(--color-border);
}

.category-rail-item {
  position: relative;
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 10px;
  border-radius: var(--radius-sm);
  background: transparent;
  color: var(--color-text-muted);
  text-align: left;
  transition: color 0.15s, background 0.15s;
}

.category-rail-item:hover {
  background: rgba(242, 141, 53, 0.07);
  color: var(--color-cream);
}

.category-rail-item--active {
  color: var(--color-primary);
  background: rgba(242, 141, 53, 0.1);
}

.category-rail-indicator {
  position: absolute;
  left: -8px;
  top: 50%;
  transform: translateY(-50%);
  width: 3px;
  height: 16px;
  background: var(--color-primary);
  border-radius: 0 2px 2px 0;
}

.category-rail-name {
  flex: 1;
  min-width: 0;
  font-size: 12px;
  font-weight: 500;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.category-rail-item--active .category-rail-name {
  font-weight: 600;
}

.category-rail-count {
  flex-shrink: 0;
  font-size: 10px;
  font-weight: 600;
  color: var(--color-text-dim);
}

.category-rail-item--active .category-rail-count {
  color: var(--color-primary);
}

/* Fixed-size columns (matching ProductCard's own fixed width) instead of
   `1fr` — cards keep a constant size as the window resizes, wrapping into
   more/fewer columns rather than stretching. */
.product-grid {
  flex: 1;
  overflow-y: auto;
  padding: 0 16px 16px 12px;
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
