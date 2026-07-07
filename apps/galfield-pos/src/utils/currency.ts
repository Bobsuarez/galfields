import { useAppConfig } from '../composables/useAppConfig'

const CURRENCY_LOCALE: Record<string, string> = {
  COP: 'es-CO',
  USD: 'en-US',
  MXN: 'es-MX',
}

const CURRENCY_DECIMALS: Record<string, number> = {
  COP: 0,
  USD: 2,
  MXN: 2,
}

export function formatCurrency(amount: number): string {
  const { config } = useAppConfig()
  const currency = config.general.currency || 'COP'
  const locale   = CURRENCY_LOCALE[currency]   ?? 'es-CO'
  const decimals = CURRENCY_DECIMALS[currency] ?? 0
  return new Intl.NumberFormat(locale, {
    style:                 'currency',
    currency,
    minimumFractionDigits: decimals,
    maximumFractionDigits: decimals,
  }).format(amount)
}

export function formatTime(date: Date): string {
  const { config } = useAppConfig()
  const locale = CURRENCY_LOCALE[config.general.currency] ?? 'es-CO'
  return date.toLocaleTimeString(locale, { hour: '2-digit', minute: '2-digit' })
}

export function formatDate(date: Date): string {
  const { config } = useAppConfig()
  const locale = CURRENCY_LOCALE[config.general.currency] ?? 'es-CO'
  return date.toLocaleDateString(locale, { day: '2-digit', month: '2-digit', year: 'numeric' })
}
