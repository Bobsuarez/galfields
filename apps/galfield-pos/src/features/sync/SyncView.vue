<script setup lang="ts">
import { useRouter } from 'vue-router'
import { useProductSync } from './composables/useProductSync'
import { useToast } from '../../composables/useToast'
import AppIcon from '../../components/shared/AppIcon.vue'

const router = useRouter()
const { syncing, error, lastSummary, runSync } = useProductSync()
const { show: showToast } = useToast()

async function handleSync() {
  await runSync()
  if (error.value) {
    showToast(error.value, 'error', 5000)
  } else if (lastSummary.value) {
    showToast(`${lastSummary.value.variantsSynced} productos sincronizados`, 'success')
  }
}
</script>

<template>
  <div class="sync-view">
    <div class="sync-mascot">🐱</div>
    <h1 class="sync-title">Sincronización de catálogo</h1>
    <p class="sync-subtitle">
      Descarga el catálogo de productos más reciente desde la nube cuando lo necesites.
    </p>

    <!-- Idle: nothing synced yet this session -->
    <button v-if="!syncing && !lastSummary && !error" class="sync-btn" @click="handleSync">
      <AppIcon name="refresh" :size="18" />
      Sincronizar catálogo
    </button>

    <!-- Syncing -->
    <div v-else-if="syncing" class="sync-progress">
      <div class="sync-bar">
        <div class="sync-bar-fill" />
      </div>
      <p class="sync-step">Descargando catálogo desde el servidor...</p>
    </div>

    <!-- Success -->
    <div v-else-if="lastSummary" class="sync-result sync-result--success">
      <AppIcon name="check" :size="28" />
      <p class="sync-result-text">
        {{ lastSummary.variantsSynced }} productos sincronizados
        <span class="sync-result-sub">
          ({{ lastSummary.productsFetched }} productos del catálogo)
          <template v-if="lastSummary.productsDeactivated > 0">
            · {{ lastSummary.productsDeactivated }} desactivados (ya no están en el catálogo)
          </template>
        </span>
      </p>
      <div class="sync-actions">
        <button class="sync-btn sync-btn--ghost" @click="handleSync">
          <AppIcon name="refresh" :size="16" />
          Sincronizar de nuevo
        </button>
        <button class="sync-btn" @click="router.push('/pos')">Ir a Ventas</button>
      </div>
    </div>

    <!-- Error -->
    <div v-else-if="error" class="sync-result sync-result--error">
      <AppIcon name="alert-triangle" :size="28" />
      <p class="sync-result-text">{{ error }}</p>
      <button class="sync-btn" @click="handleSync">
        <AppIcon name="refresh" :size="16" />
        Reintentar
      </button>
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
  text-align: center;
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
}

.sync-subtitle {
  font-size: 13px;
  color: var(--color-text-muted);
  max-width: 380px;
}

.sync-btn {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  margin-top: 8px;
  padding: 12px 24px;
  border: none;
  border-radius: 10px;
  background: var(--color-primary);
  color: #fff;
  font-size: 14px;
  font-weight: 700;
  cursor: pointer;
  transition: opacity 0.15s, transform 0.15s;
}

.sync-btn:hover { opacity: 0.9; }
.sync-btn:active { transform: scale(0.97); }

.sync-btn--ghost {
  background: transparent;
  border: 1.5px solid var(--color-primary);
  color: var(--color-primary);
}

.sync-progress {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
  width: 100%;
  max-width: 380px;
}

.sync-bar {
  width: 100%;
  height: 8px;
  background: var(--color-surface-3);
  border-radius: 4px;
  overflow: hidden;
}

.sync-bar-fill {
  height: 100%;
  width: 40%;
  background: var(--color-primary);
  border-radius: 4px;
  animation: sync-indeterminate 1.1s ease-in-out infinite;
}

@keyframes sync-indeterminate {
  0%   { transform: translateX(-100%); }
  100% { transform: translateX(250%); }
}

.sync-step {
  font-size: 12px;
  color: var(--color-text-muted);
  font-style: italic;
}

.sync-result {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 10px;
  margin-top: 4px;
}

.sync-result--success { color: #7ee87e; }
.sync-result--error { color: #f07070; }

.sync-result-text {
  font-size: 14px;
  font-weight: 600;
  color: var(--color-cream);
}

.sync-result-sub {
  display: block;
  font-size: 12px;
  font-weight: 400;
  color: var(--color-text-muted);
  margin-top: 2px;
}

.sync-actions {
  display: flex;
  gap: 10px;
  margin-top: 6px;
}
</style>
