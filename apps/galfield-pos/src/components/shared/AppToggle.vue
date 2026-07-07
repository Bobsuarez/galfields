<script setup lang="ts">
defineProps<{ modelValue: boolean; label?: string; description?: string }>()
const emit = defineEmits<{ (e: 'update:modelValue', value: boolean): void }>()
</script>

<template>
  <div class="toggle-row">
    <div v-if="label || description" class="toggle-info">
      <span class="toggle-label">{{ label }}</span>
      <span v-if="description" class="toggle-desc">{{ description }}</span>
    </div>
    <button
      class="toggle"
      :class="{ 'toggle--on': modelValue }"
      role="switch"
      :aria-checked="modelValue"
      @click="emit('update:modelValue', !modelValue)"
    />
  </div>
</template>

<style scoped>
.toggle-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 6px 0;
}

.toggle-info {
  display: flex;
  flex-direction: column;
  gap: 1px;
  flex: 1;
}

.toggle-label {
  font-size: 12px;
  color: var(--color-cream);
  line-height: 1.4;
}

.toggle-desc {
  font-size: 10px;
  color: var(--color-text-muted);
}

.toggle {
  flex-shrink: 0;
  width: 36px;
  height: 20px;
  background: var(--color-surface-3);
  border: 1px solid var(--color-border);
  border-radius: 10px;
  cursor: pointer;
  position: relative;
  transition: background 0.2s, border-color 0.2s;
}

.toggle::after {
  content: '';
  position: absolute;
  width: 14px;
  height: 14px;
  background: var(--color-text-muted);
  border-radius: 50%;
  top: 2px;
  left: 2px;
  transition: left 0.2s, background 0.2s;
}

.toggle--on {
  background: var(--color-primary);
  border-color: var(--color-primary);
}

.toggle--on::after {
  left: 18px;
  background: #0D0D0D;
}
</style>
