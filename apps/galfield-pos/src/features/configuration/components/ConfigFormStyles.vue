<script setup lang="ts">
import type { ConfigSettings, ColorPreset } from '../../../types'
import { formatCurrency } from '../../../utils/currency'

defineProps<{
  settings: ConfigSettings
  colorPresets: ColorPreset[]
}>()

const emit = defineEmits<{ (e: 'apply-preset', preset: ColorPreset): void }>()

const THEMES = [
  { id: 'dark', label: 'Oscuro' },
  { id: 'light', label: 'Claro' },
  { id: 'auto', label: 'Automático' },
] as const

const COLOR_FIELDS = [
  { key: 'primaryColor' as const, label: 'Color Primario (Botones)' },
  { key: 'bgColor' as const, label: 'Color Fondo (Interfaz)' },
  { key: 'lightBg' as const, label: 'Color Claro (Fondo Oscuro)' },
  { key: 'secondaryText' as const, label: 'Color Texto Secundario' },
  { key: 'lightText' as const, label: 'Color Texto Claro' },
]
</script>

<template>
  <div class="col-forms">
    <section class="form-card">
      <h3 class="card-title">Personalización de Estilos</h3>

      <div class="field">
        <label class="field-label">Tema</label>
        <div class="theme-buttons">
          <button
            v-for="t in THEMES"
            :key="t.id"
            class="theme-btn"
            :class="{ 'theme-btn--active': settings.styles.theme === t.id }"
            @click="settings.styles.theme = t.id"
          >
            <span class="theme-icon">{{ t.id === 'dark' ? '🌙' : t.id === 'light' ? '☀️' : '⚙️' }}</span>
            {{ t.label }}
          </button>
        </div>
      </div>

      <div class="field">
        <label class="field-label">Paleta de Colores</label>
        <div class="color-presets">
          <button
            v-for="preset in colorPresets"
            :key="preset.name"
            class="preset-circle"
            :class="{ 'preset-circle--active': settings.styles.primaryColor === preset.primary }"
            :style="{ background: preset.primary }"
            :title="preset.name"
            @click="emit('apply-preset', preset)"
          >
            <span v-if="settings.styles.primaryColor === preset.primary" class="preset-check">✓</span>
          </button>
        </div>
      </div>

      <div class="section-label">Personalizar Colores</div>

      <div class="color-fields">
        <div v-for="cf in COLOR_FIELDS" :key="cf.key" class="color-field">
          <label class="color-field-label">{{ cf.label }}</label>
          <div class="color-input-wrap">
            <div class="color-swatch" :style="{ background: settings.styles[cf.key] }" />
            <input
              :value="settings.styles[cf.key]"
              @input="settings.styles[cf.key] = ($event.target as HTMLInputElement).value"
              type="text"
              class="color-hex-input"
              maxlength="7"
            />
            <input
              v-model="settings.styles[cf.key]"
              type="color"
              class="color-native"
              :title="cf.label"
            />
          </div>
        </div>
      </div>
    </section>

    <section class="form-card">
      <h3 class="card-title">Vista Previa del Sistema</h3>

      <div class="preview-widget" :style="{ '--pw-primary': settings.styles.primaryColor }">
        <div class="pw-product">
          <div class="pw-product-img">🥤</div>
          <div class="pw-product-info">
            <div class="pw-product-name">Gaseosa 600ml</div>
            <div class="pw-product-price">{{ formatCurrency(3500) }}</div>
          </div>
          <button class="pw-add-btn">+</button>
        </div>
        <div class="pw-divider" />
        <div class="pw-cart">
          <div class="pw-cart-title">Carrito</div>
          <div class="pw-cart-row">
            <span>Gaseosa 600ml</span>
            <span>{{ formatCurrency(3500) }}</span>
          </div>
          <div class="pw-cart-row">
            <span>Agua 500ml</span>
            <span>{{ formatCurrency(1500) }}</span>
          </div>
          <div class="pw-total">
            <span>Total</span>
            <span>{{ formatCurrency(5000) }}</span>
          </div>
        </div>
      </div>
    </section>
  </div>
</template>

<style scoped>
.col-forms {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.form-card {
  background: var(--color-surface-2);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  padding: 16px;
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.card-title {
  font-size: 13px;
  font-weight: 700;
  color: var(--color-cream);
  padding-bottom: 8px;
  border-bottom: 1px solid var(--color-border);
}

.field {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.field-label {
  font-size: 11px;
  font-weight: 600;
  color: var(--color-text-muted);
  text-transform: uppercase;
  letter-spacing: 0.4px;
}

.theme-buttons {
  display: flex;
  gap: 6px;
}

.theme-btn {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 5px;
  padding: 10px 8px;
  background: var(--color-surface-3);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-sm);
  color: var(--color-text-muted);
  font-size: 11.5px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.15s;
}

.theme-btn:hover { border-color: var(--color-primary); color: var(--color-cream); }

.theme-btn--active {
  border-color: var(--color-primary);
  background: rgba(242,141,53,0.12);
  color: var(--color-primary);
  font-weight: 700;
}

.theme-icon { font-size: 16px; }

.color-presets {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.preset-circle {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  cursor: pointer;
  border: 2px solid transparent;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: transform 0.15s, border-color 0.15s;
  box-shadow: 0 2px 6px rgba(0,0,0,0.3);
}

.preset-circle:hover { transform: scale(1.12); }

.preset-circle--active {
  border-color: white;
  box-shadow: 0 0 0 3px rgba(255,255,255,0.2), 0 2px 6px rgba(0,0,0,0.3);
}

.preset-check {
  font-size: 13px;
  color: white;
  font-weight: 900;
  text-shadow: 0 1px 2px rgba(0,0,0,0.5);
}

.section-label {
  font-size: 11px;
  font-weight: 700;
  color: var(--color-primary);
  text-transform: uppercase;
  letter-spacing: 0.5px;
  padding-top: 2px;
  border-top: 1px solid var(--color-border);
  padding-top: 10px;
}

.color-fields {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.color-field {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}

.color-field-label {
  font-size: 12px;
  color: var(--color-cream);
  flex: 1;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.color-input-wrap {
  display: flex;
  align-items: center;
  gap: 6px;
  flex-shrink: 0;
}

.color-swatch {
  width: 18px;
  height: 18px;
  border-radius: 4px;
  border: 1px solid var(--color-border);
  flex-shrink: 0;
}

.color-hex-input {
  width: 76px;
  background: var(--color-surface-3);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-sm);
  color: var(--color-text);
  font-size: 11.5px;
  font-family: monospace;
  padding: 4px 6px;
  transition: border-color 0.2s;
}

.color-hex-input:focus { border-color: var(--color-primary); outline: none; }

.color-native {
  width: 22px;
  height: 22px;
  padding: 0;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  background: transparent;
  overflow: hidden;
}

/* Preview Widget */
.preview-widget {
  background: #0D0D0D;
  border-radius: var(--radius-md);
  padding: 14px;
  border: 1px solid rgba(255,255,255,0.06);
}

.pw-product {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 12px;
}

.pw-product-img {
  font-size: 28px;
  width: 44px;
  height: 44px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(242,141,53,0.15);
  border-radius: 8px;
}

.pw-product-info { flex: 1; }

.pw-product-name {
  font-size: 12px;
  font-weight: 600;
  color: #F2E399;
}

.pw-product-price {
  font-size: 11px;
  color: var(--pw-primary, #F28D35);
  font-weight: 700;
}

.pw-add-btn {
  width: 28px;
  height: 28px;
  border-radius: 6px;
  background: var(--pw-primary, #F28D35);
  border: none;
  color: #0D0D0D;
  font-size: 16px;
  font-weight: 700;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.pw-divider {
  height: 1px;
  background: rgba(255,255,255,0.06);
  margin-bottom: 10px;
}

.pw-cart-title {
  font-size: 11px;
  font-weight: 700;
  color: #F2E399;
  margin-bottom: 6px;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.pw-cart-row {
  display: flex;
  justify-content: space-between;
  font-size: 11px;
  color: rgba(242,227,153,0.6);
  padding: 2px 0;
}

.pw-total {
  display: flex;
  justify-content: space-between;
  margin-top: 8px;
  padding-top: 6px;
  border-top: 1px solid rgba(255,255,255,0.06);
  font-size: 13px;
  font-weight: 700;
  color: var(--pw-primary, #F28D35);
}
</style>
