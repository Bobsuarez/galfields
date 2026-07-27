<script setup lang="ts">
import { ref, watch, nextTick } from 'vue'

const props = defineProps<{ visible: boolean }>()
const emit  = defineEmits<{
  (e: 'confirm', code: string): void
  (e: 'cancel'): void
}>()

const code = ref('')
const inputRef = ref<HTMLInputElement | null>(null)

// Reset form every time the gate reappears (a fresh entry into Reportes).
watch(() => props.visible, (v) => {
  if (v) {
    code.value = ''
    nextTick(() => inputRef.value?.focus())
  }
})

function onInput(e: Event) {
  const target = e.target as HTMLInputElement
  code.value = target.value.replace(/\D/g, '').slice(0, 6)
}

function handleConfirm() {
  if (code.value.length !== 6) return
  emit('confirm', code.value)
}
</script>

<template>
  <Transition name="modal">
    <div v-if="visible" class="modal-backdrop">
      <div class="modal-card">
        <div class="modal-header">
          <span class="modal-icon">🔒</span>
          <h2 class="modal-title">Código de Acceso</h2>
        </div>

        <div class="modal-body">
          <p class="modal-explanation">
            Ingresa el código de 6 dígitos generado desde la app móvil
            (Configuración → Acceso a Reportes) para entrar a este módulo.
          </p>

          <div class="field">
            <label class="field-label">Código</label>
            <input
              ref="inputRef"
              :value="code"
              type="text"
              inputmode="numeric"
              maxlength="6"
              class="field-input"
              placeholder="000000"
              autofocus
              @input="onInput"
              @keydown.enter="handleConfirm"
              @keydown.esc="emit('cancel')"
            />
          </div>
        </div>

        <div class="modal-footer">
          <button class="btn-cancel" @click="emit('cancel')">Cancelar</button>
          <button class="btn-save" :disabled="code.length !== 6" @click="handleConfirm">
            🔓 Confirmar
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

.modal-explanation {
  font-size: 12px;
  line-height: 1.5;
  color: var(--color-text-muted);
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
  font-size: 20px;
  font-weight: 700;
  letter-spacing: 6px;
  text-align: center;
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
