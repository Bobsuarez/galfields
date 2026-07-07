<script setup lang="ts">
import { useToast } from '../../composables/useToast'

const { toast, dismiss } = useToast()
</script>

<template>
  <Transition name="toast">
    <div
      v-if="toast.visible"
      class="toast"
      :class="`toast--${toast.type}`"
      role="alert"
      @click="dismiss"
    >
      <span class="toast-icon">
        <template v-if="toast.type === 'success'">✓</template>
        <template v-else-if="toast.type === 'error'">✕</template>
        <template v-else>ℹ</template>
      </span>
      <span class="toast-message">{{ toast.message }}</span>
      <button class="toast-close" @click.stop="dismiss">✕</button>
    </div>
  </Transition>
</template>

<style scoped>
.toast {
  position: fixed;
  bottom: 24px;
  right: 24px;
  z-index: 9999;
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px 16px;
  border-radius: 10px;
  font-size: 13px;
  font-weight: 600;
  min-width: 240px;
  max-width: 380px;
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.4);
  cursor: default;
  border: 1px solid transparent;
}

.toast--success {
  background: #1a2e1a;
  border-color: rgba(74, 190, 74, 0.35);
  color: #7ee87e;
}

.toast--error {
  background: #2e1a1a;
  border-color: rgba(220, 70, 70, 0.35);
  color: #f07070;
}

.toast--info {
  background: #1a1e2e;
  border-color: rgba(70, 130, 220, 0.35);
  color: #70a8f0;
}

.toast-icon {
  font-size: 15px;
  font-weight: 900;
  flex-shrink: 0;
  width: 20px;
  text-align: center;
}

.toast-message {
  flex: 1;
  line-height: 1.3;
}

.toast-close {
  background: transparent;
  border: none;
  color: inherit;
  opacity: 0.6;
  font-size: 11px;
  cursor: pointer;
  padding: 0 2px;
  flex-shrink: 0;
  line-height: 1;
  transition: opacity 0.15s;
}

.toast-close:hover { opacity: 1; }

/* Transition */
.toast-enter-active { animation: toast-in 0.22s ease-out; }
.toast-leave-active { animation: toast-out 0.18s ease-in forwards; }

@keyframes toast-in {
  from { opacity: 0; transform: translateY(12px) scale(0.96); }
  to   { opacity: 1; transform: translateY(0)    scale(1);    }
}

@keyframes toast-out {
  from { opacity: 1; transform: translateY(0)    scale(1);    }
  to   { opacity: 0; transform: translateY(8px)  scale(0.96); }
}
</style>
