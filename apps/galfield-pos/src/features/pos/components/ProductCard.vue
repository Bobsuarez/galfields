<script setup lang="ts">
import type { Product } from '../../../types'
import { formatCurrency } from '../../../utils/currency'

defineProps<{ product: Product }>()
const emit = defineEmits<{ (e: 'add', product: Product): void }>()

const CATEGORY_BG: Record<string, string> = {
  bebidas:  'rgba(33, 150, 243, 0.15)',
  comida:   'rgba(76, 175, 80, 0.15)',
  snacks:   'rgba(255, 152, 0, 0.15)',
  lacteos:  'rgba(100, 181, 246, 0.12)',
  limpieza: 'rgba(0, 188, 212, 0.12)',
  otros:    'rgba(158, 158, 158, 0.12)',
}

const CATEGORY_EMOJI: Record<string, string> = {
  bebidas:   '🥤',
  beverages: '🥤',
  comida:    '🍽️',
  food:      '🍽️',
  snacks:    '🍟',
  lacteos:   '🥛',
  dairy:     '🥛',
  limpieza:  '🧹',
  cleaning:  '🧹',
}

function categoryEmoji(category: string): string {
  return CATEGORY_EMOJI[category?.toLowerCase()] ?? '📦'
}
</script>

<template>
  <button class="product-card" @click="emit('add', product)">
    <div
      class="product-image"
      :style="{ background: CATEGORY_BG[product.category] ?? 'rgba(242,141,53,0.1)' }"
    >
      <span class="product-emoji">{{ categoryEmoji(product.category) }}</span>
      <span v-if="product.stockQuantity <= 2" class="stock-badge">¡Poco!</span>
      <span class="add-btn" aria-hidden="true">+</span>
    </div>
    <div class="product-info">
      <p class="product-name">{{ product.productName }}</p>
      <p class="product-price">{{ formatCurrency(product.unitPrice) }}</p>
    </div>
  </button>
</template>

<style scoped>
.product-card {
  display: flex;
  flex-direction: column;
  background: var(--color-surface-2);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  overflow: hidden;
  cursor: pointer;
  transition: transform 0.15s, border-color 0.15s, box-shadow 0.15s;
  text-align: left;
  width: 100%;
}

.product-card:hover {
  border-color: var(--color-primary);
  transform: translateY(-2px);
  box-shadow: 0 4px 16px rgba(242, 141, 53, 0.15);
}

.product-card:active {
  transform: translateY(0);
}

.product-image {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
  height: 80px;
  width: 100%;
}

.product-emoji {
  font-size: 36px;
  line-height: 1;
  filter: drop-shadow(0 2px 4px rgba(0, 0, 0, 0.3));
}

.stock-badge {
  position: absolute;
  top: 6px;
  left: 6px;
  background: var(--color-danger);
  color: white;
  font-size: 9px;
  font-weight: 700;
  padding: 2px 5px;
  border-radius: 4px;
  text-transform: uppercase;
}

.add-btn {
  position: absolute;
  bottom: 6px;
  right: 6px;
  width: 24px;
  height: 24px;
  border-radius: 50%;
  background: var(--color-primary);
  color: #0D0D0D;
  font-size: 17px;
  font-weight: 700;
  display: flex;
  align-items: center;
  justify-content: center;
  line-height: 1;
  box-shadow: 0 2px 8px rgba(242, 141, 53, 0.4);
  transition: transform 0.15s, box-shadow 0.15s;
}

.product-card:hover .add-btn {
  transform: scale(1.15);
  box-shadow: 0 4px 14px rgba(242, 141, 53, 0.5);
}

.product-info {
  padding: 8px 10px;
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.product-name {
  font-size: 12px;
  font-weight: 500;
  color: var(--color-cream);
  line-height: 1.3;
  overflow: hidden;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

.product-price {
  font-size: 13px;
  font-weight: 700;
  color: var(--color-primary);
}
</style>
