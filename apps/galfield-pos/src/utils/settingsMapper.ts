import type { ConfigSettings } from '../types'

export const DEFAULT_SETTINGS: ConfigSettings = {
  general: {
    storeName: 'GarPOS',
    taxId: '900123456-7',
    language: 'es',
    currency: 'COP',
    timezone: 'America/Bogota',
    dateFormat: 'DD/MM/YYYY',
  },
  store: {
    name: 'GarPOS Saas',
    address: 'Calle 123 # 45-67, Bogotá',
    phone: '+57 300 123 4567',
    email: 'tienda@garpos.com',
    slogan: 'Tu punto de venta inteligente',
  },
  defaults: {
    seller: 'Cajero 1',
    customer: 'Consumidor Final',
    mainCategory: 'beverages',
    paymentMethod: 'cash',
    taxPolicy: 'no_tax',
    printReceipt: true,
    emailReceipt: false,
    roundPrices: true,
    emailNotifications: false,
    validateStock: true,
    invoiceArchiveFolder: '',
  },
  sync: {
    backupInterval: '30min',
    priceSyncHours: 24,
    invoicePrefix: 'FAC-',
    salesRetryMinutes: 5,
  },
  styles: {
    theme: 'dark',
    primaryColor: '#F28D35',  // → --color-primary (buttons, accents)
    bgColor: '#0D0D0D',       // → --color-bg, --color-surface (main background)
    lightBg: '#1A1A1A',       // → --color-surface-2, --color-surface-3 (cards, panels)
    secondaryText: '#8C501B', // → --color-accent
    lightText: '#F2E399',     // → --color-text (main text color)
  },
  peripherals: {
    printerPort:       '',
    printerPaperWidth: '80mm',
    barcodePort:       '',
    cashDrawerPort:    '',
    cameraDevice:      '',
    fingerprintPort:   '',
  },
}

/** Writes values from a flat DB record into the target ConfigSettings object in place. */
export function applyRecord(target: ConfigSettings, record: Record<string, string>): void {
  const str  = (key: string, fallback: string)  => record[key] ?? fallback
  const bool = (key: string, fallback: boolean) => key in record ? record[key] === 'true' : fallback
  const num  = (key: string, fallback: number)  => key in record ? Number(record[key]) : fallback

  target.general.storeName  = str('general.store_name',  target.general.storeName)
  target.general.taxId      = str('general.tax_id',      target.general.taxId)
  target.general.language   = str('general.language',    target.general.language)
  target.general.currency   = str('general.currency',    target.general.currency)
  target.general.timezone   = str('general.timezone',    target.general.timezone)
  target.general.dateFormat = str('general.date_format', target.general.dateFormat)

  target.store.name    = str('store.name',    target.store.name)
  target.store.address = str('store.address', target.store.address)
  target.store.phone   = str('store.phone',   target.store.phone)
  target.store.email   = str('store.email',   target.store.email)
  target.store.slogan  = str('store.slogan',  target.store.slogan)

  target.defaults.seller             = str ('defaults.seller',               target.defaults.seller)
  target.defaults.customer           = str ('defaults.customer',             target.defaults.customer)
  target.defaults.mainCategory       = str ('defaults.main_category',        target.defaults.mainCategory)
  target.defaults.paymentMethod      = str ('defaults.payment_method',       target.defaults.paymentMethod)
  target.defaults.taxPolicy          = str ('defaults.tax_policy',           target.defaults.taxPolicy)
  target.defaults.printReceipt       = bool('defaults.print_receipt',        target.defaults.printReceipt)
  target.defaults.emailReceipt       = bool('defaults.email_receipt',        target.defaults.emailReceipt)
  target.defaults.roundPrices        = bool('defaults.round_prices',         target.defaults.roundPrices)
  target.defaults.emailNotifications = bool('defaults.email_notifications',  target.defaults.emailNotifications)
  target.defaults.validateStock      = bool('defaults.validate_stock',       target.defaults.validateStock)
  target.defaults.invoiceArchiveFolder = str('defaults.invoice_archive_folder', target.defaults.invoiceArchiveFolder)

  target.sync.backupInterval   = str('sync.backup_interval',      target.sync.backupInterval)
  target.sync.priceSyncHours   = num('sync.price_sync_hours',     target.sync.priceSyncHours)
  target.sync.invoicePrefix    = str('sync.invoice_prefix',       target.sync.invoicePrefix)
  target.sync.salesRetryMinutes = num('sync.sales_retry_minutes', target.sync.salesRetryMinutes)

  target.styles.theme         = str('styles.theme',          target.styles.theme) as ConfigSettings['styles']['theme']
  target.styles.primaryColor  = str('styles.primary_color',  target.styles.primaryColor)
  target.styles.bgColor       = str('styles.bg_color',       target.styles.bgColor)
  target.styles.lightBg       = str('styles.light_bg',       target.styles.lightBg)
  target.styles.secondaryText = str('styles.secondary_text', target.styles.secondaryText)
  target.styles.lightText     = str('styles.light_text',     target.styles.lightText)

  target.peripherals.printerPort     = str('peripherals.printer_port',      target.peripherals.printerPort)
  target.peripherals.printerPaperWidth = str('peripherals.printer_paper_width', target.peripherals.printerPaperWidth) as ConfigSettings['peripherals']['printerPaperWidth']
  target.peripherals.barcodePort     = str('peripherals.barcode_port',      target.peripherals.barcodePort)
  target.peripherals.cashDrawerPort  = str('peripherals.cash_drawer_port',  target.peripherals.cashDrawerPort)
  target.peripherals.cameraDevice    = str('peripherals.camera_device',     target.peripherals.cameraDevice)
  target.peripherals.fingerprintPort = str('peripherals.fingerprint_port',  target.peripherals.fingerprintPort)
}

/** Flattens a ConfigSettings object into the flat key-value format used by the DB. */
export function settingsToRecord(s: ConfigSettings): Record<string, string> {
  return {
    'general.store_name':          s.general.storeName,
    'general.tax_id':              s.general.taxId,
    'general.language':            s.general.language,
    'general.currency':            s.general.currency,
    'general.timezone':            s.general.timezone,
    'general.date_format':         s.general.dateFormat,

    'store.name':    s.store.name,
    'store.address': s.store.address,
    'store.phone':   s.store.phone,
    'store.email':   s.store.email,
    'store.slogan':  s.store.slogan,

    'defaults.seller':               s.defaults.seller,
    'defaults.customer':             s.defaults.customer,
    'defaults.main_category':        s.defaults.mainCategory,
    'defaults.payment_method':       s.defaults.paymentMethod,
    'defaults.tax_policy':           s.defaults.taxPolicy,
    'defaults.print_receipt':        String(s.defaults.printReceipt),
    'defaults.email_receipt':        String(s.defaults.emailReceipt),
    'defaults.round_prices':         String(s.defaults.roundPrices),
    'defaults.email_notifications':  String(s.defaults.emailNotifications),
    'defaults.validate_stock':       String(s.defaults.validateStock),
    'defaults.invoice_archive_folder': s.defaults.invoiceArchiveFolder,

    'sync.backup_interval':     s.sync.backupInterval,
    'sync.price_sync_hours':    String(s.sync.priceSyncHours),
    'sync.invoice_prefix':      s.sync.invoicePrefix,
    'sync.sales_retry_minutes': String(s.sync.salesRetryMinutes),

    'styles.theme':          s.styles.theme,
    'styles.primary_color':  s.styles.primaryColor,
    'styles.bg_color':       s.styles.bgColor,
    'styles.light_bg':       s.styles.lightBg,
    'styles.secondary_text': s.styles.secondaryText,
    'styles.light_text':     s.styles.lightText,

    'peripherals.printer_port':        s.peripherals.printerPort,
    'peripherals.printer_paper_width': s.peripherals.printerPaperWidth,
    'peripherals.barcode_port':      s.peripherals.barcodePort,
    'peripherals.cash_drawer_port':  s.peripherals.cashDrawerPort,
    'peripherals.camera_device':     s.peripherals.cameraDevice,
    'peripherals.fingerprint_port':  s.peripherals.fingerprintPort,
  }
}
