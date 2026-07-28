<script setup lang="ts">
import { onMounted, onUnmounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import AppLayout from './layouts/AppLayout.vue'
import AppToast from './components/shared/AppToast.vue'
import { useAppConfig } from './composables/useAppConfig'
import { useEmployeeSession } from './composables/useEmployeeSession'

const router = useRouter()
const route = useRoute()
const { loadConfig } = useAppConfig()
const { isAuthenticated, loadSession, refreshSession } = useEmployeeSession()

// Spec 01-login-empleados-roles step 17, the "foreground" half (the
// "arranque" half is loadSession() below, which already enforces
// expires_at via auth::session_if_valid). Fires when the OS window regains
// focus - e.g. the app sat backgrounded overnight and the cached session's
// midnight-Bogota expiry has since passed. If it just went from
// authenticated to not, and the user is sitting on a route that requires a
// session, force them back to /login instead of leaving a dead session
// silently in place until their next click fails.
async function handleWindowFocus(): Promise<void> {
  const wasAuthenticated = isAuthenticated.value
  await refreshSession()
  if (wasAuthenticated && !isAuthenticated.value && route.path !== '/login' && route.path !== '/configuracion') {
    router.push('/login')
  }
}

onMounted(() => {
  loadConfig()
  // Also called from the router guard (router/index.ts) if this hasn't
  // resolved yet by the time the first navigation is evaluated - loadSession
  // dedups both callers into a single invoke('get_session') round trip.
  loadSession()
  window.addEventListener('focus', handleWindowFocus)
})

onUnmounted(() => {
  window.removeEventListener('focus', handleWindowFocus)
})
</script>

<template>
  <AppLayout>
    <router-view />
  </AppLayout>
  <AppToast />
</template>
