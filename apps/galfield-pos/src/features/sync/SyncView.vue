<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'

const router = useRouter()
const progress = ref(0)
const step = ref('Conectando...')

const steps = [
  { label: 'Conectando...', value: 20 },
  { label: 'Descargando datos...', value: 50 },
  { label: 'Cargando catálogo...', value: 75 },
  { label: 'Finalizando...', value: 100 },
]

onMounted(async () => {
  for (const s of steps) {
    await new Promise(r => setTimeout(r, 700))
    step.value = s.label
    progress.value = s.value
  }
  await new Promise(r => setTimeout(r, 500))
  router.push('/pos')
})
</script>

<template>
  <div class="sync-view">
    <div class="sync-mascot">🐱</div>
    <h1 class="sync-title">¡Hola! Preparando tu punto de venta</h1>
    <p class="sync-subtitle">Cargando información, esto puede tardar unos segundos...</p>

    <div class="sync-bar-wrap">
      <div class="sync-bar">
        <div class="sync-bar-fill" :style="{ width: progress + '%' }" />
      </div>
      <span class="sync-percent">{{ progress }}%</span>
    </div>

    <p class="sync-step">{{ step }}</p>

    <div class="sync-steps">
      <div v-for="s in steps" :key="s.value" class="sync-step-dot" :class="{ done: progress >= s.value }">
        <div class="dot" />
        <span>{{ s.label.replace('...', '') }}</span>
      </div>
    </div>
  </div>
</template>

<style scoped>
.sync-view {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
  gap: 16px;
  background: var(--color-surface);
  padding: 40px;
}

.sync-mascot {
  font-size: 72px;
  filter: drop-shadow(0 4px 16px rgba(242, 141, 53, 0.4));
  animation: float 2s ease-in-out infinite;
}

@keyframes float {
  0%, 100% { transform: translateY(0); }
  50% { transform: translateY(-8px); }
}

.sync-title {
  font-size: 22px;
  font-weight: 700;
  color: var(--color-cream);
  text-align: center;
}

.sync-subtitle {
  font-size: 13px;
  color: var(--color-text-muted);
  text-align: center;
}

.sync-bar-wrap {
  display: flex;
  align-items: center;
  gap: 12px;
  width: 100%;
  max-width: 440px;
}

.sync-bar {
  flex: 1;
  height: 8px;
  background: var(--color-surface-3);
  border-radius: 4px;
  overflow: hidden;
}

.sync-bar-fill {
  height: 100%;
  background: var(--color-primary);
  border-radius: 4px;
  transition: width 0.5s ease;
}

.sync-percent {
  font-size: 13px;
  font-weight: 700;
  color: var(--color-primary);
  min-width: 36px;
}

.sync-step {
  font-size: 12px;
  color: var(--color-text-muted);
  font-style: italic;
}

.sync-steps {
  display: flex;
  gap: 24px;
  margin-top: 8px;
}

.sync-step-dot {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
  color: var(--color-text-dim);
  font-size: 11px;
  transition: color 0.3s;
}

.sync-step-dot.done {
  color: var(--color-primary);
}

.dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  background: var(--color-surface-3);
  border: 2px solid currentColor;
  transition: background 0.3s;
}

.sync-step-dot.done .dot {
  background: var(--color-primary);
}
</style>
