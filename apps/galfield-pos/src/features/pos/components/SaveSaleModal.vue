<script setup lang="ts">
import { ref, watch } from 'vue'
import { SALE_ICONS } from '../composables/useActiveSale'

const props = defineProps<{ visible: boolean }>()
const emit  = defineEmits<{
  (e: 'confirm', label: string, iconKey: string): void
  (e: 'cancel'): void
}>()

const label   = ref('')
const iconKey = ref('user')

// Reset form when modal opens
watch(() => props.visible, (v) => {
  if (v) { label.value = ''; iconKey.value = 'user' }
})

function handleConfirm() {
  if (!label.value.trim()) return
  emit('confirm', label.value.trim(), iconKey.value)
}
</script>

<template>
  <Transition name="modal">
    <div v-if="visible" class="modal-backdrop" @click.self="emit('cancel')">
      <div class="modal-card">
        <div class="modal-header">
          <span class="modal-icon">💾</span>
          <h2 class="modal-title">Guardar Venta</h2>
        </div>

        <div class="modal-body">
          <div class="field">
            <label class="field-label">Nombre de la venta</label>
            <input
              v-model="label"
              class="field-input"
              placeholder="Ej: Mesa 4, Juan P., WhatsApp..."
              autofocus
              @keydown.enter="handleConfirm"
              @keydown.esc="emit('cancel')"
            />
          </div>

          <div class="field">
            <label class="field-label">Canal / Tipo</label>
            <div class="icon-grid">
              <button
                v-for="icon in SALE_ICONS"
                :key="icon.key"
                class="icon-chip"
                :class="{ 'icon-chip--active': iconKey === icon.key }"
                type="button"
                @click="iconKey = icon.key"
              >
                <span class="icon-emoji">{{ icon.emoji }}</span>
                <span class="icon-label">{{ icon.label }}</span>
              </button>
            </div>
          </div>
        </div>

        <div class="modal-footer">
          <button class="btn-cancel" @click="emit('cancel')">Cancelar</button>
          <button class="btn-save" :disabled="!label.trim()" @click="handleConfirm">
            💾 Guardar
          </button>
        </div>
      </div>
    </div>
  </Transition>
</template>

<style scoped>
.modal-backdrop {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.72);
  z-index: 1000;
  display: flex;
  align-items: center;
  justify-content: center;
  backdrop-filter: blur(2px);
}

.modal-card {
  background: var(--color-surface-2);
  border: 1px solid var(--color-border-strong);
  border-radius: var(--radius-lg);
  width: 380px;
  max-width: calc(100vw - 32px);
  box-shadow: 0 24px 64px rgba(0, 0, 0, 0.6), 0 0 0 1px rgba(242, 141, 53, 0.08);
  overflow: hidden;
}

.modal-header {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 18px 20px 14px;
  border-bottom: 1px solid var(--color-border);
}

.modal-icon {
  font-size: 20px;
  line-height: 1;
}

.modal-title {
  font-size: 15px;
  font-weight: 700;
  color: var(--color-cream);
}

.modal-body {
  padding: 18px 20px;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.field {
  display: flex;
  flex-direction: column;
  gap: 7px;
}

.field-label {
  font-size: 10.5px;
  font-weight: 700;
  color: var(--color-text-muted);
  text-transform: uppercase;
  letter-spacing: 0.6px;
}

.field-input {
  background: var(--color-surface-3);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-sm);
  color: var(--color-text);
  font-size: 13px;
  padding: 9px 12px;
  font-family: inherit;
  transition: border-color 0.2s;
  width: 100%;
}

.field-input:focus {
  border-color: var(--color-primary);
  outline: none;
}

.field-input::placeholder {
  color: var(--color-text-dim);
}

.icon-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 8px;
}

.icon-chip {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 5px;
  padding: 10px 8px;
  background: var(--color-surface-3);
  border: 1.5px solid var(--color-border);
  border-radius: var(--radius-md);
  cursor: pointer;
  transition: all 0.15s;
}

.icon-chip:hover {
  border-color: var(--color-primary);
  background: rgba(242, 141, 53, 0.08);
}

.icon-chip--active {
  border-color: var(--color-primary);
  background: rgba(242, 141, 53, 0.14);
  box-shadow: 0 0 0 3px rgba(242, 141, 53, 0.12);
}

.icon-emoji {
  font-size: 22px;
  line-height: 1;
}

.icon-label {
  font-size: 10px;
  font-weight: 600;
  color: var(--color-text-muted);
  text-transform: uppercase;
  letter-spacing: 0.4px;
}

.icon-chip--active .icon-label {
  color: var(--color-primary);
}

.modal-footer {
  display: flex;
  gap: 8px;
  padding: 14px 20px;
  border-top: 1px solid var(--color-border);
  justify-content: flex-end;
}

.btn-cancel {
  padding: 8px 16px;
  background: transparent;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-sm);
  color: var(--color-text-muted);
  font-size: 12.5px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.15s;
  font-family: inherit;
}

.btn-cancel:hover {
  border-color: var(--color-text-muted);
  color: var(--color-cream);
}

.btn-save {
  padding: 8px 20px;
  background: var(--color-primary);
  border: none;
  border-radius: var(--radius-sm);
  color: #0D0D0D;
  font-size: 12.5px;
  font-weight: 700;
  cursor: pointer;
  transition: background 0.15s, transform 0.1s;
  font-family: inherit;
}

.btn-save:hover:not(:disabled) {
  background: var(--color-primary-hover);
  transform: translateY(-1px);
}

.btn-save:disabled {
  opacity: 0.45;
  cursor: not-allowed;
}

/* Transition */
.modal-enter-active { animation: modal-in  0.2s ease-out; }
.modal-leave-active { animation: modal-out 0.15s ease-in forwards; }

@keyframes modal-in  {
  from { opacity: 0; transform: scale(0.93) translateY(8px); }
  to   { opacity: 1; transform: scale(1)    translateY(0);   }
}
@keyframes modal-out {
  from { opacity: 1; transform: scale(1)    translateY(0);   }
  to   { opacity: 0; transform: scale(0.95) translateY(4px); }
}
</style>
