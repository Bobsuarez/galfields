import { ref, computed } from 'vue'
import type { Product, ProductCategory } from '../../../types'

const CATEGORIES: ProductCategory[] = [
  { id: 'all',      name: 'Todos'    },
  { id: 'bebidas',  name: 'Bebidas'  },
  { id: 'comida',   name: 'Comida'   },
  { id: 'snacks',   name: 'Snacks'   },
  { id: 'lacteos',  name: 'Lácteos'  },
  { id: 'limpieza', name: 'Limpieza' },
  { id: 'otros',    name: 'Otros'    },
]

const now = new Date().toISOString()

const PRODUCTS: Product[] = [
  { id: '1',  barcode: '7700000001', productName: 'Gaseosa 600ml',   unitPrice: 3500,  category: 'bebidas',  isActive: true, imagePath: '', imageHash: '', stockQuantity: 45, lastSyncAt: null, createdAt: now, updatedAt: now },
  { id: '2',  barcode: '7700000002', productName: 'Agua 500ml',      unitPrice: 1500,  category: 'bebidas',  isActive: true, imagePath: '', imageHash: '', stockQuantity: 80, lastSyncAt: null, createdAt: now, updatedAt: now },
  { id: '3',  barcode: '7700000003', productName: 'Jugo Natural',    unitPrice: 3200,  category: 'bebidas',  isActive: true, imagePath: '', imageHash: '', stockQuantity: 20, lastSyncAt: null, createdAt: now, updatedAt: now },
  { id: '4',  barcode: '7700000004', productName: 'Café 200g',       unitPrice: 8500,  category: 'bebidas',  isActive: true, imagePath: '', imageHash: '', stockQuantity: 15, lastSyncAt: null, createdAt: now, updatedAt: now },
  { id: '5',  barcode: '7700000005', productName: 'Pan de Molde',    unitPrice: 3200,  category: 'comida',   isActive: true, imagePath: '', imageHash: '', stockQuantity: 12, lastSyncAt: null, createdAt: now, updatedAt: now },
  { id: '6',  barcode: '7700000006', productName: 'Arroz 1kg',       unitPrice: 4200,  category: 'comida',   isActive: true, imagePath: '', imageHash: '', stockQuantity: 30, lastSyncAt: null, createdAt: now, updatedAt: now },
  { id: '7',  barcode: '7700000007', productName: 'Atún 170g',       unitPrice: 5500,  category: 'comida',   isActive: true, imagePath: '', imageHash: '', stockQuantity: 25, lastSyncAt: null, createdAt: now, updatedAt: now },
  { id: '8',  barcode: '7700000008', productName: 'Pasta 500g',      unitPrice: 3800,  category: 'comida',   isActive: true, imagePath: '', imageHash: '', stockQuantity: 18, lastSyncAt: null, createdAt: now, updatedAt: now },
  { id: '9',  barcode: '7700000009', productName: 'Papas Fritas',    unitPrice: 5500,  category: 'snacks',   isActive: true, imagePath: '', imageHash: '', stockQuantity: 40, lastSyncAt: null, createdAt: now, updatedAt: now },
  { id: '10', barcode: '7700000010', productName: 'Chips 200g',      unitPrice: 3500,  category: 'snacks',   isActive: true, imagePath: '', imageHash: '', stockQuantity: 35, lastSyncAt: null, createdAt: now, updatedAt: now },
  { id: '11', barcode: '7700000011', productName: 'Maní Salado',     unitPrice: 2800,  category: 'snacks',   isActive: true, imagePath: '', imageHash: '', stockQuantity: 28, lastSyncAt: null, createdAt: now, updatedAt: now },
  { id: '12', barcode: '7700000012', productName: 'Galletas Soda',   unitPrice: 2200,  category: 'snacks',   isActive: true, imagePath: '', imageHash: '', stockQuantity: 50, lastSyncAt: null, createdAt: now, updatedAt: now },
  { id: '13', barcode: '7700000013', productName: 'Leche 1L',        unitPrice: 4200,  category: 'lacteos',  isActive: true, imagePath: '', imageHash: '', stockQuantity: 20, lastSyncAt: null, createdAt: now, updatedAt: now },
  { id: '14', barcode: '7700000014', productName: 'Leche 2L',        unitPrice: 7500,  category: 'lacteos',  isActive: true, imagePath: '', imageHash: '', stockQuantity: 10, lastSyncAt: null, createdAt: now, updatedAt: now },
  { id: '15', barcode: '7700000015', productName: 'Yogur 1kg',       unitPrice: 5800,  category: 'lacteos',  isActive: true, imagePath: '', imageHash: '', stockQuantity:  8, lastSyncAt: null, createdAt: now, updatedAt: now },
  { id: '16', barcode: '7700000016', productName: 'Mantequilla',     unitPrice: 6200,  category: 'lacteos',  isActive: true, imagePath: '', imageHash: '', stockQuantity:  6, lastSyncAt: null, createdAt: now, updatedAt: now },
  { id: '17', barcode: '7700000017', productName: 'Detergente 1kg',  unitPrice: 8500,  category: 'limpieza', isActive: true, imagePath: '', imageHash: '', stockQuantity: 15, lastSyncAt: null, createdAt: now, updatedAt: now },
  { id: '18', barcode: '7700000018', productName: 'Papel Higiénico', unitPrice: 4500,  category: 'limpieza', isActive: true, imagePath: '', imageHash: '', stockQuantity:  1, lastSyncAt: null, createdAt: now, updatedAt: now },
  { id: '19', barcode: '7700000019', productName: 'Jabón de Platos', unitPrice: 5200,  category: 'limpieza', isActive: true, imagePath: '', imageHash: '', stockQuantity: 12, lastSyncAt: null, createdAt: now, updatedAt: now },
  { id: '20', barcode: '7700000020', productName: 'Pañal Talla M',   unitPrice: 35000, category: 'otros',    isActive: true, imagePath: '', imageHash: '', stockQuantity:  5, lastSyncAt: null, createdAt: now, updatedAt: now },
  { id: '21', barcode: '7700000021', productName: 'Aceite Vegetal',  unitPrice: 9800,  category: 'comida',   isActive: true, imagePath: '', imageHash: '', stockQuantity:  2, lastSyncAt: null, createdAt: now, updatedAt: now },
  { id: '22', barcode: '7700000022', productName: 'Azúcar 1kg',      unitPrice: 3800,  category: 'comida',   isActive: true, imagePath: '', imageHash: '', stockQuantity: 22, lastSyncAt: null, createdAt: now, updatedAt: now },
]

export function useProductCatalog() {
  const activeCategory = ref('all')
  const searchQuery    = ref('')

  const filteredProducts = computed(() => {
    let items = PRODUCTS.filter(p => p.isActive)

    if (activeCategory.value !== 'all') {
      items = items.filter(p => p.category === activeCategory.value)
    }

    if (searchQuery.value.trim()) {
      const q = searchQuery.value.toLowerCase()
      items = items.filter(p => p.productName.toLowerCase().includes(q))
    }

    return items
  })

  function selectCategory(id: string) {
    activeCategory.value = id
  }

  return {
    categories: CATEGORIES,
    activeCategory,
    searchQuery,
    filteredProducts,
    selectCategory,
  }
}
