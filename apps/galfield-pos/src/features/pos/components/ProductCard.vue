<script setup lang="ts">
import { computed, ref } from 'vue'
import type { Product } from '../../../types'
import { formatCurrency } from '../../../utils/currency'

const props = defineProps<{ product: Product }>()
const emit = defineEmits<{ (e: 'add', product: Product): void }>()

// Falls back to the category emoji if there's no synced image, or if the
// image URL fails to load (e.g. an internal-only host from a misconfigured
// backend — see CLAUDE.md's Image URLs / CDN note on the backend side).
const imageFailed = ref(false)

// Deactivation always wins over a stock badge - it's a different, more
// fundamental reason the product can't be sold. Mirrors the precedence
// InventoryTable.vue/ProductDetail.vue already use.
const unavailableReason = computed(() => {
  if (!props.product.isActive) return 'Desactivado'
  if (props.product.stockQuantity <= 0) return 'Sin stock'
  return null
})

function handleAdd() {
  if (unavailableReason.value) return
  emit('add', props.product)
}

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
  <div
    class="product-card"
    :class="{ 'product-card--unavailable': unavailableReason }"
    role="button"
    tabindex="0"
    @click="handleAdd"
    @keydown.enter="handleAdd"
  >
    <div class="product-image">
      <img
        v-if="product.imagePath && !imageFailed"
        :src="product.imagePath"
        class="product-photo"
        alt=""
        @error="imageFailed = true"
      />
      <div v-else class="product-emoji-wrap" :style="{ background: CATEGORY_BG[product.category] ?? 'rgba(242,141,53,0.1)' }">
        <span class="product-emoji">{{ categoryEmoji(product.category) }}</span>
      </div>
      <span v-if="!unavailableReason && product.stockQuantity <= 5" class="stock-badge">¡Poco!</span>
    </div>

    <div class="product-info">
      <p class="product-name">{{ product.productName }}</p>
      <p class="product-price">{{ formatCurrency(product.unitPrice) }}</p>
    </div>

    <button class="add-btn" :disabled="!!unavailableReason" @click.stop="handleAdd">Agregar</button>

    <div v-if="unavailableReason" class="unavailable-badge">{{ unavailableReason }}</div>
  </div>
</template>

<style scoped>
/* Fixed footprint on purpose — a fluid `1fr` grid made cards stretch/shrink
   as the window resized, which looked bad. The grid that hosts these
   (ProductCatalog.vue's `.product-grid`) uses a fixed column size to match,
   so the card count per row changes instead of the card size. */
.product-card {
  position: relative;
  display: flex;
  flex-direction: column;
  width: 176px;
  flex-shrink: 0;
  background: #fff;
  border-radius: 16px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1), 0 1px 2px rgba(0, 0, 0, 0.08);
  padding: 16px;
  gap: 10px;
  cursor: pointer;
  transition: transform 0.15s ease, box-shadow 0.15s ease;
}

.product-card:hover {
  transform: translateY(-3px);
  box-shadow: 0 10px 22px rgba(0, 0, 0, 0.16);
}

.product-card:active {
  transform: translateY(-1px);
}

/* Deactivated or out-of-stock - shown dimmed with a centered stamp instead
   of being hidden, so the cashier sees *why* it can't be sold. */
.product-card--unavailable {
  cursor: not-allowed;
}

.product-card--unavailable:hover,
.product-card--unavailable:active {
  transform: none;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1), 0 1px 2px rgba(0, 0, 0, 0.08);
}

.product-card--unavailable .product-image,
.product-card--unavailable .product-info,
.product-card--unavailable .add-btn {
  opacity: 0.35;
}

.unavailable-badge {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%) rotate(-8deg);
  background: rgba(20, 20, 20, 0.85);
  color: #fff;
  font-size: 12px;
  font-weight: 800;
  text-transform: uppercase;
  letter-spacing: 0.5px;
  padding: 6px 14px;
  border-radius: 6px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.3);
  pointer-events: none;
  z-index: 2;
  white-space: nowrap;
}

.product-image {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
  height: 140px;
  width: 100%;
  flex-shrink: 0;
}

.product-photo {
  max-width: 100%;
  max-height: 100%;
  object-fit: contain;
}

.product-emoji-wrap {
  width: 92px;
  height: 92px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
}

.product-emoji {
  font-size: 40px;
  line-height: 1;
}

.stock-badge {
  position: absolute;
  top: 0;
  left: 0;
  background: var(--color-danger);
  color: white;
  font-size: 9px;
  font-weight: 700;
  padding: 2px 5px;
  border-radius: 4px;
  text-transform: uppercase;
}

.product-info {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.product-name {
  font-size: 14px;
  font-weight: 600;
  color: #1a1a1a;
  line-height: 1.3;
  height: 2.6em;
  overflow: hidden;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  line-clamp: 2;
  -webkit-box-orient: vertical;
}

.product-price {
  font-size: 19px;
  font-weight: 800;
  color: var(--color-primary);
}

.add-btn {
  width: 100%;
  height: 44px;
  border: none;
  border-radius: 999px;
  background: var(--color-primary);
  color: #0D0D0D;
  font-size: 13px;
  font-weight: 700;
  cursor: pointer;
  transition: background 0.15s, transform 0.1s;
}

.add-btn:hover { background: var(--color-primary-hover); }
.add-btn:active { transform: scale(0.97); }
.add-btn:disabled { cursor: not-allowed; }
</style>
