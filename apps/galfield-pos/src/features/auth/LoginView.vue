<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { useEmployeeSession } from '../../composables/useEmployeeSession'

const router = useRouter()
const { login } = useEmployeeSession()

const username = ref('')
const password = ref('')
const loading = ref(false)
const error = ref('')

async function handleLogin(): Promise<void> {
  if (!username.value.trim() || !password.value.trim()) {
    error.value = 'Ingresa usuario y contraseña'
    return
  }
  error.value = ''
  loading.value = true
  try {
    await login(username.value.trim(), password.value)
    router.push('/pos')
  } catch (e) {
    error.value = typeof e === 'string' ? e : 'No se pudo iniciar sesión'
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="login-view">
    <div class="login-card">
      <div class="login-mascot">🐱</div>
      <div class="login-brand">
        <span class="brand-gar">Gar</span><span class="brand-pos">POS</span>
      </div>
      <p class="login-tagline">Inicia sesión para continuar</p>

      <form class="login-form" @submit.prevent="handleLogin">
        <div class="field">
          <label class="field-label">Usuario</label>
          <input
            v-model="username"
            class="field-input"
            placeholder="Usuario"
            autocapitalize="none"
            autocomplete="username"
            @input="error = ''"
          />
        </div>
        <div class="field">
          <label class="field-label">Contraseña</label>
          <input
            v-model="password"
            type="password"
            class="field-input"
            placeholder="Contraseña"
            autocomplete="current-password"
            @input="error = ''"
          />
        </div>

        <p v-if="error" class="login-error">{{ error }}</p>

        <button class="login-btn" type="submit" :disabled="loading">
          {{ loading ? 'Ingresando…' : 'Iniciar sesión' }}
        </button>
      </form>

      <router-link to="/configuracion" class="login-config-link">Ir a Configuración</router-link>
    </div>
  </div>
</template>

<style scoped>
.login-view {
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px;
}

.login-card {
  width: 340px;
  max-width: 100%;
  background: var(--color-surface-2);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  padding: 28px 24px;
  display: flex;
  flex-direction: column;
  align-items: center;
  box-shadow: 0 24px 64px rgba(0, 0, 0, 0.35);
}

.login-mascot {
  font-size: 40px;
  margin-bottom: 8px;
}

.login-brand {
  font-size: 26px;
  font-weight: 800;
  margin-bottom: 4px;
}

.brand-gar { color: var(--color-cream); }
.brand-pos { color: var(--color-primary); }

.login-tagline {
  font-size: 12px;
  color: var(--color-text-muted);
  margin-bottom: 20px;
}

.login-form {
  width: 100%;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.field {
  display: flex;
  flex-direction: column;
  gap: 5px;
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
  font-size: 13px;
  padding: 9px 12px;
  font-family: inherit;
  transition: border-color 0.2s;
}

.field-input:focus {
  border-color: var(--color-primary);
  outline: none;
}

.login-error {
  font-size: 12px;
  color: var(--color-danger);
  text-align: center;
}

.login-btn {
  margin-top: 4px;
  padding: 10px;
  background: var(--color-primary);
  border: none;
  border-radius: var(--radius-sm);
  color: #0D0D0D;
  font-size: 13px;
  font-weight: 700;
  cursor: pointer;
  transition: background 0.15s, transform 0.1s;
  font-family: inherit;
}

.login-btn:hover:not(:disabled) {
  background: var(--color-primary-hover);
  transform: translateY(-1px);
}

.login-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.login-config-link {
  margin-top: 18px;
  font-size: 12px;
  color: var(--color-text-muted);
  text-decoration: none;
}

.login-config-link:hover {
  color: var(--color-primary);
}
</style>
