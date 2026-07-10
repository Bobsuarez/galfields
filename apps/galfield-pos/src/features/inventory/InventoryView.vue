<script setup lang="ts">
import { useInventory } from './composables/useInventory'
import InventoryTable from './components/InventoryTable.vue'
import ProductDetail from './components/ProductDetail.vue'

const {
  categories, searchQuery, activeCategory, currentPage, pageSize, totalPages,
  filtered, paginated, selectedProduct,
  selectCategory, selectProduct, closeDetail,
  prevPage, nextPage, goToPage,
} = useInventory()
</script>

<template>
  <div class="inventory-view">
    <div class="inv-header">
      <div>
        <h1 class="inv-title">Inventario</h1>
        <p class="inv-subtitle">Administra el stock de tus productos</p>
      </div>
    </div>

    <div class="inv-body">
      <InventoryTable
        :products="paginated"
        :categories="categories"
        :active-category="activeCategory"
        :selected-id="selectedProduct?.id ?? null"
        :search-query="searchQuery"
        :current-page="currentPage"
        :total-pages="totalPages"
        :total-count="filtered.length"
        :page-size="pageSize"
        @update:search-query="searchQuery = $event"
        @update:page-size="pageSize = $event"
        @select="selectProduct"
        @category="selectCategory"
        @prev="prevPage"
        @next="nextPage"
        @page="goToPage"
      />

      <ProductDetail
        v-if="selectedProduct"
        :key="selectedProduct.id"
        :product="selectedProduct"
        @close="closeDetail"
      />
    </div>
  </div>
</template>

<style scoped>
.inventory-view {
  display: flex;
  flex-direction: column;
  height: 100%;
  overflow: hidden;
  background: var(--color-surface);
}

.inv-header {
  padding: 14px 16px 8px;
  border-bottom: 1px solid var(--color-border);
  flex-shrink: 0;
}

.inv-title {
  font-size: 18px;
  font-weight: 700;
  color: var(--color-cream);
  line-height: 1.2;
}

.inv-subtitle {
  font-size: 12px;
  color: var(--color-text-muted);
  margin-top: 2px;
}

.inv-body {
  flex: 1;
  display: flex;
  overflow: hidden;
  min-height: 0;
}

.inv-body > :first-child {
  flex: 1;
  min-width: 0;
}
</style>
