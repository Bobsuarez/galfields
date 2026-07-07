<script setup lang="ts">
import { ref, watch } from 'vue'
import type { InventoryProduct } from '../../../types'
import AppIcon from '../../../components/shared/AppIcon.vue'
import { deriveStatus } from '../composables/useInventory'

const props = defineProps<{ product: InventoryProduct; isNew: boolean }>()
const emit = defineEmits<{
  (e: 'save', product: InventoryProduct): void
  (e: 'close'): void
}>()

const draft = ref<InventoryProduct>({ ...props.product })

watch(() => props.product, (p) => { draft.value = { ...p } }, { immediate: true })

const CATEGORIES = [
  { id: 'bebidas', name: 'Bebidas' },
  { id: 'alimentos', name: 'Alimentos' },
  { id: 'snacks', name: 'Snacks' },
  { id: 'lacteos', name: 'Lácteos' },
  { id: 'limpieza', name: 'Limpieza' },
  { id: 'otros', name: 'Otros' },
]

const UNITS = ['Unidad', 'Caja', 'Paquete', 'Bolsa', 'Botella', 'Frasco', 'Lata', 'Tarro', 'Barra', 'Par']

const CATEGORY_EMOJI: Record<string, string> = {
  bebidas: '🥤', alimentos: '🍚', snacks: '🍟',
  lacteos: '🥛', limpieza: '🧼', otros: '📦',
}

const CATEGORY_BG: Record<string, string> = {
  bebidas: 'rgba(33,150,243,0.2)', alimentos: 'rgba(76,175,80,0.2)',
  snacks: 'rgba(255,152,0,0.2)', lacteos: 'rgba(100,181,246,0.15)',
  limpieza: 'rgba(0,188,212,0.15)', otros: 'rgba(158,158,158,0.15)',
}

const STATUS_LABEL = { 'en-stock': 'En stock', 'stock-bajo': 'Stock bajo', 'sin-stock': 'Sin stock' }
const STATUS_CLASS = { 'en-stock': 'badge--ok', 'stock-bajo': 'badge--warn', 'sin-stock': 'badge--err' }

function onSave() {
  emit('save', { ...draft.value })
}
</script>

<template>
  <aside class="detail-panel">
    <div class="detail-header">
      <h3 class="detail-title">{{ isNew ? 'Nuevo Producto' : 'Detalle del Producto' }}</h3>
      <button class="close-btn" @click="emit('close')">
        <AppIcon name="x" :size="15" />
      </button>
    </div>

    <div class="detail-scroll">
      <div class="product-image-area" :style="{ background: CATEGORY_BG[draft.category] ?? 'rgba(242,141,53,0.12)' }">
        <span class="product-emoji">{{ CATEGORY_EMOJI[draft.category] ?? '📦' }}</span>
        <span
          v-if="!isNew"
          class="status-chip"
          :class="STATUS_CLASS[deriveStatus(product)]"
        >{{ STATUS_LABEL[deriveStatus(product)] }}</span>
      </div>

      <p class="edit-image-link">Editar imagen</p>

      <div class="form-section">
        <div class="field">
          <label class="field-label">Código</label>
          <input v-model="draft.barcode" class="field-input" placeholder="0000" :readonly="!isNew" />
        </div>
        <div class="field">
          <label class="field-label">Nombre</label>
          <input v-model="draft.name" class="field-input" placeholder="Nombre del producto" />
        </div>
        <div class="field">
          <label class="field-label">Categoría</label>
          <select v-model="draft.category" class="field-input">
            <option v-for="cat in CATEGORIES" :key="cat.id" :value="cat.id">{{ cat.name }}</option>
          </select>
        </div>
        <div class="field">
          <label class="field-label">Descripción</label>
          <textarea v-model="draft.description" class="field-input field-textarea" rows="2" placeholder="Descripción del producto..." />
        </div>
      </div>

      <div class="section-divider">
        <span>Información de Inventario</span>
      </div>

      <div class="form-section">
        <div class="field-row">
          <div class="field">
            <label class="field-label">Stock Actual</label>
            <div class="field-suffix-wrap">
              <input v-model.number="draft.currentStock" type="number" min="0" class="field-input" />
              <span class="field-suffix">uds.</span>
            </div>
          </div>
          <div class="field">
            <label class="field-label">Stock Mínimo</label>
            <div class="field-suffix-wrap">
              <input v-model.number="draft.minStock" type="number" min="0" class="field-input" />
              <span class="field-suffix">uds.</span>
            </div>
          </div>
        </div>
        <div class="field">
          <label class="field-label">Unidad de Venta</label>
          <select v-model="draft.unitOfSale" class="field-input">
            <option v-for="u in UNITS" :key="u" :value="u">{{ u }}</option>
          </select>
        </div>
        <div class="field">
          <label class="field-label">Proveedor</label>
          <input v-model="draft.supplier" class="field-input" placeholder="Nombre del proveedor" />
        </div>
      </div>

      <div class="section-divider">
        <span>Información de Precios</span>
      </div>

      <div class="form-section">
        <div class="field">
          <label class="field-label">Precio de Compra</label>
          <div class="field-prefix-wrap">
            <span class="field-prefix">$</span>
            <input v-model.number="draft.purchasePrice" type="number" min="0" class="field-input" />
          </div>
        </div>
        <div class="field">
          <label class="field-label">Precio de Venta</label>
          <div class="field-prefix-wrap">
            <span class="field-prefix">$</span>
            <input v-model.number="draft.salePrice" type="number" min="0" class="field-input" />
          </div>
        </div>
        <div class="field">
          <label class="field-label">IVA (%)</label>
          <input v-model.number="draft.iva" type="number" min="0" max="100" class="field-input" />
        </div>
        <div v-if="!isNew" class="margin-row">
          <span class="margin-label">Margen de ganancia</span>
          <span class="margin-value">
            {{ draft.purchasePrice > 0
              ? Math.round((draft.salePrice - draft.purchasePrice) / draft.purchasePrice * 100) + '%'
              : '—' }}
          </span>
        </div>
      </div>
    </div>

    <div class="detail-footer">
      <button class="btn-save" @click="onSave">
        <AppIcon name="check" :size="14" />
        Guardar Cambios
      </button>
      <button class="btn-maintenance">
        <AppIcon name="refresh" :size="14" />
        Mantenimiento
      </button>
    </div>
  </aside>
</template>

<style scoped>
.detail-panel {
  width: 300px;
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
  height: 100px;
  position: relative;
  flex-shrink: 0;
}

.product-emoji {
  font-size: 52px;
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

.edit-image-link {
  text-align: center;
  font-size: 11px;
  color: var(--color-primary);
  padding: 6px 0 2px;
  cursor: pointer;
  text-decoration: underline;
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

.field-input {
  background: var(--color-surface-3);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-sm);
  color: var(--color-text);
  font-size: 12.5px;
  padding: 7px 10px;
  width: 100%;
  transition: border-color 0.2s;
  font-family: inherit;
}

.field-input:focus { border-color: var(--color-primary); }
.field-input[readonly] { opacity: 0.6; cursor: default; }

.field-textarea { resize: none; line-height: 1.5; }

.field-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 8px;
}

.field-suffix-wrap, .field-prefix-wrap {
  display: flex;
  align-items: center;
  background: var(--color-surface-3);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-sm);
  overflow: hidden;
  transition: border-color 0.2s;
}

.field-suffix-wrap:focus-within, .field-prefix-wrap:focus-within { border-color: var(--color-primary); }

.field-suffix-wrap .field-input,
.field-prefix-wrap .field-input {
  background: transparent;
  border: none;
  border-radius: 0;
  flex: 1;
}

.field-suffix, .field-prefix {
  font-size: 11px;
  color: var(--color-text-muted);
  padding: 0 8px;
  background: rgba(242,141,53,0.06);
  height: 100%;
  display: flex;
  align-items: center;
  border-left: 1px solid var(--color-border);
  flex-shrink: 0;
}

.field-prefix {
  border-left: none;
  border-right: 1px solid var(--color-border);
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

.margin-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 6px 10px;
  background: rgba(242,141,53,0.06);
  border-radius: var(--radius-sm);
  border: 1px solid rgba(242,141,53,0.12);
}

.margin-label { font-size: 11px; color: var(--color-text-muted); }
.margin-value { font-size: 13px; font-weight: 700; color: var(--color-primary); }

.detail-footer {
  padding: 10px 14px;
  border-top: 1px solid var(--color-border);
  display: flex;
  flex-direction: column;
  gap: 8px;
  flex-shrink: 0;
}

.btn-save {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  padding: 10px;
  background: var(--color-primary);
  border: none;
  border-radius: var(--radius-sm);
  color: #0D0D0D;
  font-size: 12.5px;
  font-weight: 700;
  cursor: pointer;
  transition: background 0.15s;
}

.btn-save:hover { background: var(--color-primary-hover); }

.btn-maintenance {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  padding: 8px;
  background: transparent;
  border: 1px solid var(--color-primary);
  border-radius: var(--radius-sm);
  color: var(--color-primary);
  font-size: 12px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.15s;
}

.btn-maintenance:hover { background: rgba(242,141,53,0.1); }
</style>
