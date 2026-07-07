<script setup lang="ts">
import { getPaymentMethodEmoji, type PaymentMethod } from '../../../composables/usePaymentMethods'

defineProps<{ visible: boolean; current: string; methods: PaymentMethod[] }>()
const emit = defineEmits<{
  (e: 'select', key: string): void
  (e: 'cancel'): void
}>()
</script>

<template>
  <Transition name="modal">
    <div v-if="visible" class="modal-backdrop" @click.self="emit('cancel')">
      <div class="modal-card">
        <div class="modal-header">
          <span class="modal-icon">💳</span>
          <h2 class="modal-title">Método de Pago</h2>
        </div>

        <div class="modal-body">
          <div class="method-grid">
            <button
              v-for="method in methods"
              :key="method.id"
              class="method-chip"
              :class="{ 'method-chip--active': current === method.name }"
              type="button"
              @click="emit('select', method.name)"
            >
              <span class="method-emoji">{{ getPaymentMethodEmoji(method.name) }}</span>
              <span class="method-label">{{ method.name }}</span>
            </button>
          </div>
        </div>

        <div class="modal-footer">
          <button class="btn-cancel" @click="emit('cancel')">Cancelar</button>
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
}

.method-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 8px;
}

.method-chip {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
  padding: 14px 8px;
  background: var(--color-surface-3);
  border: 1.5px solid var(--color-border);
  border-radius: var(--radius-md);
  cursor: pointer;
  transition: all 0.15s;
}

.method-chip:hover {
  border-color: var(--color-primary);
  background: rgba(242, 141, 53, 0.08);
}

.method-chip--active {
  border-color: var(--color-primary);
  background: rgba(242, 141, 53, 0.14);
  box-shadow: 0 0 0 3px rgba(242, 141, 53, 0.12);
}

.method-emoji {
  font-size: 24px;
  line-height: 1;
}

.method-label {
  font-size: 11px;
  font-weight: 600;
  color: var(--color-text-muted);
  text-align: center;
  letter-spacing: 0.2px;
}

.method-chip--active .method-label {
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
