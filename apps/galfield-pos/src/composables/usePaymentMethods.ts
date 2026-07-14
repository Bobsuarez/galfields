/**
 * Payment methods live in the `payment_method` table instead of a hardcoded
 * list, so adding one (a new wallet app, etc.) is a DB row, not a code
 * change. Shared between the POS checkout panel and the Configuration
 * screen's default-payment-method dropdown — both just render whatever this
 * returns.
 */

import { ref } from 'vue'
import { invoke } from '@tauri-apps/api/core'

export interface PaymentMethod {
  id: number
  name: string
  /** Empty string when the cloud has no image for this method — components
   *  fall back to `getPaymentMethodEmoji` in that case. */
  url: string
}

// Icons aren't stored in the DB (no icon column) — this is presentation-only
// polish, keyed by name. Anything not listed here still works fine, it just
// falls back to a generic icon.
const PAYMENT_METHOD_EMOJI: Record<string, string> = {
  efectivo: '💵',
  nequi: '💜',
  daviplata: '❤️',
  breb: '🔵',
  'bre-b': '🔵',
  dale: '🟢',
  bancolombia: '💛',
  tarjeta: '💳',
  'tarjeta débito/crédito': '💳',
}

export function getPaymentMethodEmoji(name: string): string {
  return PAYMENT_METHOD_EMOJI[name.toLowerCase()] ?? '💰'
}

export function usePaymentMethods() {
  const paymentMethods = ref<PaymentMethod[]>([])
  const isLoading = ref(false)

  async function loadPaymentMethods(): Promise<void> {
    isLoading.value = true
    try {
      paymentMethods.value = await invoke<PaymentMethod[]>('list_payment_methods')
    } catch (e) {
      console.error('[payment-methods] load failed:', e)
    } finally {
      isLoading.value = false
    }
  }

  return { paymentMethods, isLoading, loadPaymentMethods }
}
