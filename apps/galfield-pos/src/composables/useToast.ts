import { reactive } from 'vue'

export type ToastType = 'success' | 'error' | 'info'

interface ToastState {
  visible: boolean
  message: string
  type: ToastType
}

// Module-level singleton — shared across all consumers without Pinia
const state = reactive<ToastState>({
  visible: false,
  message: '',
  type: 'success',
})

let dismissTimer: ReturnType<typeof setTimeout> | null = null

export function useToast() {
  function show(message: string, type: ToastType = 'success', duration = 3000) {
    if (dismissTimer) clearTimeout(dismissTimer)
    state.message = message
    state.type    = type
    state.visible = true
    dismissTimer  = setTimeout(dismiss, duration)
  }

  function dismiss() {
    if (dismissTimer) { clearTimeout(dismissTimer); dismissTimer = null }
    state.visible = false
  }

  return { toast: state, show, dismiss }
}
