export interface NavItem {
  id: string
  label: string
  icon: string
  route: string
}

export interface ProductCategory {
  id: string
  name: string
}

export interface Product {
  id: string
  barcode: string
  productName: string
  unitPrice: number
  category: string
  isActive: boolean
  imagePath: string
  imageHash: string
  stockQuantity: number
  lastSyncAt: string | null
  createdAt: string
  updatedAt: string
}

export interface CartItem {
  product: Product
  quantity: number
  unitPrice: number
}

export interface PendingSale {
  id: string
  label: string
  iconKey: string
  items: CartItem[]
  subtotal: number
  discount: number
  total: number
  createdAt: Date
}

export interface StockAlert {
  productName: string
  currentStock: number
}

export interface ShortcutKey {
  key: string
  label: string
  action: string
}

export type StockStatus = 'en-stock' | 'stock-bajo' | 'sin-stock'

export interface InventoryProduct {
  id: string
  barcode: string
  name: string
  category: string
  currentStock: number
  minStock: number
  salesCount: number
  purchasePrice: number
  salePrice: number
  iva: number
  supplier: string
  description: string
  unitOfSale: string
}

export type ConfigTab = 'empresa' | 'usuarios' | 'impuestos' | 'avanzado' | 'seguridad' | 'sistema' | 'idioma' | 'perifericos'

export interface ColorPreset {
  name: string
  primary: string
}

export interface ConfigSettings {
  general: {
    storeName: string
    taxId: string
    language: string
    currency: string
    timezone: string
    dateFormat: string
  }
  store: {
    name: string
    address: string
    phone: string
    email: string
    slogan: string
  }
  defaults: {
    seller: string
    customer: string
    mainCategory: string
    paymentMethod: string
    taxPolicy: string
    printReceipt: boolean
    emailReceipt: boolean
    roundPrices: boolean
    emailNotifications: boolean
    validateStock: boolean
    invoiceArchiveFolder: string
  }
  sync: {
    backupInterval: string
    priceSyncHours: number
    invoicePrefix: string
  }
  styles: {
    theme: 'dark' | 'light' | 'auto'
    primaryColor: string
    bgColor: string
    lightBg: string
    secondaryText: string
    lightText: string
  }
  peripherals: {
    printerPort: string
    printerPaperWidth: '58mm' | '80mm'
    barcodePort: string
    cashDrawerPort: string
    cameraDevice: string
    fingerprintPort: string
  }
}
