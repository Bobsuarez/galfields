<script setup lang="ts">
import { ref } from 'vue'
import type { Product } from '../../../types'
import { formatCurrency } from '../../../utils/currency'
import { deriveStockStatus } from '../../../utils/stock'
import AppIcon from '../../../components/shared/AppIcon.vue'

defineProps<{ product: Product }>()
const emit = defineEmits<{ (e: 'close'): void }>()

// Same fallback rule as ProductCard.vue: no synced image, or the URL fails
// to load, falls back to the category emoji.
const imageFailed = ref(false)

const CATEGORY_EMOJI: Record<string, string> = {
  bebidas: '🥤', beverages: '🥤',
  comida: '🍽️', alimentos: '🍽️', food: '🍽️',
  snacks: '🍟',
  lacteos: '🥛', dairy: '🥛',
  limpieza: '🧼', cleaning: '🧼',
}

function categoryEmoji(category: string): string {
  return CATEGORY_EMOJI[category.toLowerCase()] ?? '📦'
}

const STATUS_LABEL = { 'en-stock': 'En stock', 'stock-bajo': 'Stock bajo', 'sin-stock': 'Sin stock' }
const STATUS_CLASS = { 'en-stock': 'badge--ok', 'stock-bajo': 'badge--warn', 'sin-stock': 'badge--err' }

function formatSyncDate(value: string | null): string {
  if (!value) return 'Nunca'
  return new Date(value).toLocaleString('es-CO', { dateStyle: 'medium', timeStyle: 'short' })
}
</script>

<template>
  <aside class="detail-panel" :class="{ 'detail-panel--inactive': !product.isActive }">
    <div class="detail-header">
      <h3 class="detail-title">Detalle del Producto</h3>
      <button class="close-btn" @click="emit('close')">
        <AppIcon name="x" :size="15" />
      </button>
    </div>

    <div class="detail-scroll">
      <div class="product-image-area" :class="{ 'product-image-area--photo': product.imagePath && !imageFailed }">
        <img
          v-if="product.imagePath && !imageFailed"
          :src="product.imagePath"
          class="product-photo"
          alt=""
          @error="imageFailed = true"
        />
        <span v-else class="product-emoji">{{ categoryEmoji(product.category) }}</span>
        <span v-if="!product.isActive" class="status-chip badge--inactive">Desactivado</span>
        <span v-else class="status-chip" :class="STATUS_CLASS[deriveStockStatus(product.stockQuantity)]">
          {{ STATUS_LABEL[deriveStockStatus(product.stockQuantity)] }}
        </span>
      </div>

      <p v-if="!product.isActive" class="inactive-banner">
        Este producto fue desactivado desde la app móvil (o ya no aparece en el catálogo de la nube) — no es vendible en el POS.
      </p>

      <div class="form-section">
        <div class="field">
          <span class="field-label">Nombre</span>
          <p class="field-value">{{ product.productName }}</p>
        </div>
        <div class="field">
          <span class="field-label">Categoría</span>
          <p class="field-value">{{ product.category || 'Sin categoría' }}</p>
        </div>
        <div class="field">
          <span class="field-label">Código de barras</span>
          <p class="field-value field-value--mono">{{ product.barcode || '—' }}</p>
        </div>
      </div>

      <div class="section-divider">
        <span>Inventario</span>
      </div>

      <div class="form-section">
        <div class="field-row">
          <div class="field">
            <span class="field-label">Stock actual</span>
            <p class="field-value">{{ product.stockQuantity }} uds.</p>
          </div>
          <div class="field">
            <span class="field-label">Precio</span>
            <p class="field-value">{{ formatCurrency(product.unitPrice) }}</p>
          </div>
        </div>
        <div class="field">
          <span class="field-label">Última sincronización</span>
          <p class="field-value">{{ formatSyncDate(product.lastSyncAt) }}</p>
        </div>
      </div>
    </div>

    <div class="detail-footer">
      <p class="detail-footer-hint">
        Este catálogo se administra desde la app móvil y se actualiza aquí con
        Sincronización — no es editable directamente en el POS.
      </p>
    </div>
  </aside>
</template>

<style scoped>
.detail-panel {
  width: 350px;
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  border-left: 1px solid var(--color-border);
  background: var(--color-surface-2);
  height: 100%;
  overflow: hidden;
}

.detail-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 14px 8px;
  flex-shrink: 0;
  border-bottom: 1px solid var(--color-border);
}

.detail-title {
  font-size: 13px;
  font-weight: 600;
  color: var(--color-cream);
}

.close-btn {
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
}

.close-btn:hover { border-color: var(--color-danger); color: var(--color-danger); }

.detail-scroll {
  flex: 1;
  overflow-y: auto;
  padding: 0 0 8px;
}

.product-image-area {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 250px;
  position: relative;
  flex-shrink: 0;
  background: rgba(242,141,53,0.12);
}

/* Real product photos get a clean white backdrop instead of the category
   tint, shown in full (never cropped) — the tint stays reserved for the
   category-emoji fallback. Matches ProductCard.vue's treatment. */
.product-image-area--photo {
  background: #fff;
}

.product-photo {
  max-width: 100%;
  max-height: 100%;
  object-fit: contain;
  padding: 10px;
  box-sizing: border-box;
}

.product-emoji {
  font-size: 84px;
  filter: drop-shadow(0 2px 6px rgba(0,0,0,0.3));
}

.status-chip {
  position: absolute;
  top: 8px;
  right: 10px;
  padding: 2px 8px;
  border-radius: 10px;
  font-size: 10px;
  font-weight: 700;
}

.badge--ok { background: rgba(76,175,80,0.2); color: #81C784; }
.badge--warn { background: rgba(255,152,0,0.2); color: #FFB74D; }
.badge--err { background: rgba(229,57,53,0.2); color: #EF5350; }
.badge--inactive { background: rgba(158,158,158,0.25); color: #BDBDBD; }

/* Whole panel dims when the product is deactivated, same treatment as the
   Inventory row - a clear "not currently sellable" read without hiding it. */
.detail-panel--inactive .form-section,
.detail-panel--inactive .product-image-area {
  opacity: 0.6;
}

.inactive-banner {
  margin: 10px 14px 0;
  padding: 8px 10px;
  background: rgba(158,158,158,0.15);
  border: 1px solid rgba(158,158,158,0.3);
  border-radius: var(--radius-sm);
  color: var(--color-text-muted);
  font-size: 11px;
  line-height: 1.5;
}

.form-section {
  padding: 10px 14px;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.field {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.field-label {
  font-size: 11px;
  font-weight: 600;
  color: var(--color-text-muted);
  text-transform: uppercase;
  letter-spacing: 0.4px;
}

.field-value {
  font-size: 13px;
  color: var(--color-text);
  background: var(--color-surface-3);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-sm);
  padding: 7px 10px;
}

.field-value--mono { font-family: monospace; }

.field-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 8px;
}

.section-divider {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 14px 2px;
  font-size: 11px;
  font-weight: 700;
  color: var(--color-primary);
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.section-divider::after {
  content: '';
  flex: 1;
  height: 1px;
  background: var(--color-border);
}

.detail-footer {
  padding: 10px 14px;
  border-top: 1px solid var(--color-border);
  flex-shrink: 0;
}

.detail-footer-hint {
  font-size: 11px;
  color: var(--color-text-muted);
  line-height: 1.5;
}
</style>
