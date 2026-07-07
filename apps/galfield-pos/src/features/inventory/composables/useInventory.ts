import { ref, computed, watch } from 'vue'
import type { InventoryProduct, StockStatus, ProductCategory } from '../../../types'

const INITIAL_PRODUCTS: InventoryProduct[] = [
  { id: '1', barcode: '0001', name: 'Gaseosa 600ml', category: 'bebidas', currentStock: 23, minStock: 10, salesCount: 11, purchasePrice: 2800, salePrice: 3500, iva: 0, supplier: 'Distribuidora Norte', description: 'Bebida gaseosa en presentación 600ml. Disponible en varios sabores.', unitOfSale: 'Unidad' },
  { id: '2', barcode: '0002', name: 'Agua 500ml', category: 'bebidas', currentStock: 56, minStock: 20, salesCount: 24, purchasePrice: 900, salePrice: 1500, iva: 0, supplier: 'Distribuidora Norte', description: 'Agua mineral purificada en presentación 500ml.', unitOfSale: 'Unidad' },
  { id: '3', barcode: '0003', name: 'Papas Fritas 90g', category: 'snacks', currentStock: 20, minStock: 15, salesCount: 34, purchasePrice: 2200, salePrice: 3000, iva: 8, supplier: 'Snacks Colombia', description: 'Papas fritas crujientes en presentación de 90 gramos.', unitOfSale: 'Paquete' },
  { id: '4', barcode: '0004', name: 'Chips Maxi 200g', category: 'snacks', currentStock: 19, minStock: 15, salesCount: 30, purchasePrice: 2400, salePrice: 3500, iva: 8, supplier: 'Snacks Colombia', description: 'Papas fritas en presentación grande 200g.', unitOfSale: 'Paquete' },
  { id: '5', barcode: '0005', name: 'Leche 1L', category: 'lacteos', currentStock: 15, minStock: 20, salesCount: 24, purchasePrice: 3200, salePrice: 4200, iva: 0, supplier: 'Lácteos La Sabana', description: 'Leche entera pasteurizada en presentación de 1 litro.', unitOfSale: 'Unidad' },
  { id: '6', barcode: '0006', name: 'Pan de Molde', category: 'alimentos', currentStock: 3, minStock: 10, salesCount: 8, purchasePrice: 2400, salePrice: 3200, iva: 0, supplier: 'Panadería Central', description: 'Pan de molde blanco en empaque de 500g.', unitOfSale: 'Empaque' },
  { id: '7', barcode: '0007', name: 'Leche 2L', category: 'lacteos', currentStock: 8, minStock: 20, salesCount: 19, purchasePrice: 5500, salePrice: 7500, iva: 0, supplier: 'Lácteos La Sabana', description: 'Leche entera pasteurizada en presentación de 2 litros.', unitOfSale: 'Unidad' },
  { id: '8', barcode: '0008', name: 'Café 500g', category: 'alimentos', currentStock: 22, minStock: 10, salesCount: 17, purchasePrice: 2600, salePrice: 3500, iva: 0, supplier: 'Café Colombia', description: 'Café molido colombiano premium en presentación de 500g.', unitOfSale: 'Bolsa' },
  { id: '9', barcode: '0009', name: 'Papel Higiénico x4', category: 'limpieza', currentStock: 4, minStock: 10, salesCount: 4, purchasePrice: 2800, salePrice: 4500, iva: 0, supplier: 'Higiene Total', description: 'Papel higiénico doble hoja, paquete de 4 rollos.', unitOfSale: 'Paquete' },
  { id: '10', barcode: '0010', name: 'Azúcar 1kg', category: 'alimentos', currentStock: 8, minStock: 15, salesCount: 7, purchasePrice: 2900, salePrice: 4000, iva: 0, supplier: 'Distribuidora Sur', description: 'Azúcar blanca refinada en presentación de 1 kilogramo.', unitOfSale: 'Bolsa' },
  { id: '11', barcode: '0011', name: 'Jugo Natural 300ml', category: 'bebidas', currentStock: 30, minStock: 15, salesCount: 20, purchasePrice: 2100, salePrice: 3200, iva: 0, supplier: 'Jugos Frescos', description: 'Jugo natural sin conservantes.', unitOfSale: 'Unidad' },
  { id: '12', barcode: '0012', name: 'Café Instantáneo', category: 'bebidas', currentStock: 18, minStock: 10, salesCount: 15, purchasePrice: 6500, salePrice: 8500, iva: 0, supplier: 'Café Colombia', description: 'Café instantáneo en frasco de 200g.', unitOfSale: 'Frasco' },
  { id: '13', barcode: '0013', name: 'Chips Rizadas', category: 'snacks', currentStock: 35, minStock: 20, salesCount: 28, purchasePrice: 2000, salePrice: 2800, iva: 8, supplier: 'Snacks Colombia', description: 'Papas rizadas con sal en paquete 90g.', unitOfSale: 'Paquete' },
  { id: '14', barcode: '0014', name: 'Maní Salado', category: 'snacks', currentStock: 28, minStock: 15, salesCount: 22, purchasePrice: 1800, salePrice: 2500, iva: 8, supplier: 'Snacks Colombia', description: 'Maní tostado con sal 100g.', unitOfSale: 'Paquete' },
  { id: '15', barcode: '0015', name: 'Yogur Natural 1kg', category: 'lacteos', currentStock: 10, minStock: 15, salesCount: 12, purchasePrice: 4200, salePrice: 5800, iva: 0, supplier: 'Lácteos La Sabana', description: 'Yogur natural sin azúcar, 1 kilogramo.', unitOfSale: 'Tarro' },
  { id: '16', barcode: '0016', name: 'Mantequilla 250g', category: 'lacteos', currentStock: 6, minStock: 10, salesCount: 8, purchasePrice: 4500, salePrice: 6200, iva: 0, supplier: 'Lácteos La Sabana', description: 'Mantequilla sin sal en barra de 250g.', unitOfSale: 'Barra' },
  { id: '17', barcode: '0017', name: 'Detergente 1kg', category: 'limpieza', currentStock: 15, minStock: 10, salesCount: 13, purchasePrice: 6200, salePrice: 8500, iva: 0, supplier: 'Higiene Total', description: 'Detergente en polvo multiusos 1kg.', unitOfSale: 'Bolsa' },
  { id: '18', barcode: '0018', name: 'Jabón de Platos 500ml', category: 'limpieza', currentStock: 12, minStock: 8, salesCount: 9, purchasePrice: 3800, salePrice: 5200, iva: 0, supplier: 'Higiene Total', description: 'Líquido lavaplatos concentrado 500ml.', unitOfSale: 'Botella' },
  { id: '19', barcode: '0019', name: 'Arroz 1kg', category: 'alimentos', currentStock: 30, minStock: 15, salesCount: 25, purchasePrice: 3000, salePrice: 4200, iva: 0, supplier: 'Distribuidora Sur', description: 'Arroz blanco de grano largo, 1 kilogramo.', unitOfSale: 'Bolsa' },
  { id: '20', barcode: '0020', name: 'Aceite Vegetal 1L', category: 'alimentos', currentStock: 2, minStock: 10, salesCount: 8, purchasePrice: 7200, salePrice: 9800, iva: 0, supplier: 'Distribuidora Sur', description: 'Aceite vegetal refinado 1 litro.', unitOfSale: 'Botella' },
  { id: '21', barcode: '0021', name: 'Atún en Lata 170g', category: 'alimentos', currentStock: 25, minStock: 15, salesCount: 20, purchasePrice: 3500, salePrice: 5500, iva: 0, supplier: 'Distribuidora Mar', description: 'Atún en agua 170g por lata.', unitOfSale: 'Lata' },
  { id: '22', barcode: '0022', name: 'Pasta 500g', category: 'alimentos', currentStock: 18, minStock: 10, salesCount: 14, purchasePrice: 2800, salePrice: 3800, iva: 0, supplier: 'Distribuidora Sur', description: 'Pasta espagueti 500g.', unitOfSale: 'Bolsa' },
  { id: '23', barcode: '0023', name: 'Galletas Soda 400g', category: 'snacks', currentStock: 50, minStock: 20, salesCount: 35, purchasePrice: 1500, salePrice: 2200, iva: 8, supplier: 'Snacks Colombia', description: 'Galletas de soda 400g paquete familiar.', unitOfSale: 'Paquete' },
  { id: '24', barcode: '0024', name: 'Sal de Mesa 500g', category: 'alimentos', currentStock: 22, minStock: 10, salesCount: 10, purchasePrice: 800, salePrice: 1200, iva: 0, supplier: 'Distribuidora Sur', description: 'Sal de mesa yodada 500g.', unitOfSale: 'Bolsa' },
  { id: '25', barcode: '0025', name: 'Queso Mozzarella 500g', category: 'lacteos', currentStock: 5, minStock: 10, salesCount: 8, purchasePrice: 10000, salePrice: 14000, iva: 0, supplier: 'Lácteos La Sabana', description: 'Queso mozzarella rallado 500g.', unitOfSale: 'Bolsa' },
  { id: '26', barcode: '0026', name: 'Pañales Talla M x12', category: 'otros', currentStock: 5, minStock: 5, salesCount: 3, purchasePrice: 25000, salePrice: 35000, iva: 0, supplier: 'Distribuidora Norte', description: 'Pañales desechables talla M, paquete x12.', unitOfSale: 'Paquete' },
  { id: '27', barcode: '0027', name: 'Shampoo 400ml', category: 'limpieza', currentStock: 8, minStock: 8, salesCount: 6, purchasePrice: 7000, salePrice: 10500, iva: 0, supplier: 'Higiene Total', description: 'Shampoo para todo tipo de cabello 400ml.', unitOfSale: 'Botella' },
  { id: '28', barcode: '0028', name: 'Crema Dental 100ml', category: 'limpieza', currentStock: 12, minStock: 10, salesCount: 9, purchasePrice: 3500, salePrice: 5000, iva: 0, supplier: 'Higiene Total', description: 'Crema dental con flúor blanqueadora 100ml.', unitOfSale: 'Tubo' },
  { id: '29', barcode: '0029', name: 'Avena en Hojuelas 500g', category: 'alimentos', currentStock: 15, minStock: 10, salesCount: 11, purchasePrice: 3800, salePrice: 5200, iva: 0, supplier: 'Distribuidora Sur', description: 'Avena en hojuelas tradicional 500g.', unitOfSale: 'Caja' },
  { id: '30', barcode: '0030', name: 'Chocolate 200g', category: 'snacks', currentStock: 20, minStock: 12, salesCount: 16, purchasePrice: 5500, salePrice: 7800, iva: 8, supplier: 'Snacks Colombia', description: 'Barra de chocolate semiamargo 200g.', unitOfSale: 'Barra' },
  { id: '31', barcode: '0031', name: 'Pilas AA x2', category: 'otros', currentStock: 20, minStock: 10, salesCount: 14, purchasePrice: 2500, salePrice: 3500, iva: 0, supplier: 'Eléctricos Plus', description: 'Pilas alcalinas AA duración extendida.', unitOfSale: 'Par' },
  { id: '32', barcode: '0032', name: 'Jabón de Manos 250ml', category: 'limpieza', currentStock: 18, minStock: 10, salesCount: 12, purchasePrice: 2200, salePrice: 3200, iva: 0, supplier: 'Higiene Total', description: 'Jabón líquido antibacterial 250ml.', unitOfSale: 'Botella' },
  { id: '33', barcode: '0033', name: 'Café Molido 250g', category: 'bebidas', currentStock: 12, minStock: 8, salesCount: 9, purchasePrice: 5200, salePrice: 7200, iva: 0, supplier: 'Café Colombia', description: 'Café molido selección especial 250g.', unitOfSale: 'Bolsa' },
  { id: '34', barcode: '0034', name: 'Cereal 500g', category: 'alimentos', currentStock: 8, minStock: 8, salesCount: 6, purchasePrice: 7500, salePrice: 10500, iva: 0, supplier: 'Distribuidora Sur', description: 'Cereal de maíz tostado 500g.', unitOfSale: 'Caja' },
  { id: '35', barcode: '0035', name: 'Bocadillo Guayaba', category: 'snacks', currentStock: 40, minStock: 20, salesCount: 30, purchasePrice: 1200, salePrice: 1800, iva: 8, supplier: 'Snacks Colombia', description: 'Bocadillo de guayaba tradicional 90g.', unitOfSale: 'Unidad' },
  { id: '36', barcode: '0036', name: 'Aceite de Oliva 500ml', category: 'alimentos', currentStock: 5, minStock: 8, salesCount: 4, purchasePrice: 18000, salePrice: 25000, iva: 0, supplier: 'Importaciones Gourmet', description: 'Aceite de oliva extra virgen 500ml.', unitOfSale: 'Botella' },
  { id: '37', barcode: '0037', name: 'Mermelada Fresa 300g', category: 'alimentos', currentStock: 10, minStock: 8, salesCount: 7, purchasePrice: 4500, salePrice: 6500, iva: 0, supplier: 'Distribuidora Sur', description: 'Mermelada de fresa sin conservantes 300g.', unitOfSale: 'Frasco' },
  { id: '38', barcode: '0038', name: 'Toallas Papel x3', category: 'limpieza', currentStock: 15, minStock: 10, salesCount: 10, purchasePrice: 3800, salePrice: 5500, iva: 0, supplier: 'Higiene Total', description: 'Toallas de papel absorbente doble hoja.', unitOfSale: 'Paquete' },
  { id: '39', barcode: '0039', name: 'Vinagre Blanco 750ml', category: 'alimentos', currentStock: 10, minStock: 8, salesCount: 5, purchasePrice: 2800, salePrice: 4000, iva: 0, supplier: 'Distribuidora Sur', description: 'Vinagre blanco para cocina 750ml.', unitOfSale: 'Botella' },
  { id: '40', barcode: '0040', name: 'Servilletas x100', category: 'limpieza', currentStock: 25, minStock: 15, salesCount: 18, purchasePrice: 1800, salePrice: 2800, iva: 0, supplier: 'Higiene Total', description: 'Servilletas de papel x100 unidades.', unitOfSale: 'Paquete' },
]

export const INVENTORY_CATEGORIES: ProductCategory[] = [
  { id: 'all', name: 'Todas las categorías' },
  { id: 'bebidas', name: 'Bebidas' },
  { id: 'alimentos', name: 'Alimentos' },
  { id: 'snacks', name: 'Snacks' },
  { id: 'lacteos', name: 'Lácteos' },
  { id: 'limpieza', name: 'Limpieza' },
  { id: 'otros', name: 'Otros' },
]

export function deriveStatus(p: InventoryProduct): StockStatus {
  if (p.currentStock === 0) return 'sin-stock'
  if (p.currentStock <= p.minStock) return 'stock-bajo'
  return 'en-stock'
}

export function useInventory() {
  const products = ref<InventoryProduct[]>([...INITIAL_PRODUCTS])
  const searchQuery = ref('')
  const activeCategory = ref('all')
  const currentPage = ref(1)
  const pageSize = ref(10)
  const selectedProduct = ref<InventoryProduct | null>(null)
  const isNewProduct = ref(false)

  watch([searchQuery, activeCategory], () => { currentPage.value = 1 })

  const filtered = computed(() => {
    let items = products.value
    if (activeCategory.value !== 'all') {
      items = items.filter(p => p.category === activeCategory.value)
    }
    if (searchQuery.value.trim()) {
      const q = searchQuery.value.toLowerCase()
      items = items.filter(p => p.name.toLowerCase().includes(q) || p.barcode.includes(q))
    }
    return items
  })

  const totalPages = computed(() => Math.max(1, Math.ceil(filtered.value.length / pageSize.value)))

  const paginated = computed(() => {
    const start = (currentPage.value - 1) * pageSize.value
    return filtered.value.slice(start, start + pageSize.value)
  })

  function selectCategory(id: string) {
    activeCategory.value = id
  }

  function selectProduct(product: InventoryProduct) {
    selectedProduct.value = { ...product }
    isNewProduct.value = false
  }

  function closeDetail() {
    selectedProduct.value = null
  }

  function newProduct() {
    const barcode = String(products.value.length + 1).padStart(4, '0')
    selectedProduct.value = {
      id: '', barcode, name: '', category: 'otros',
      currentStock: 0, minStock: 5, salesCount: 0,
      purchasePrice: 0, salePrice: 0, iva: 0,
      supplier: '', description: '', unitOfSale: 'Unidad',
    }
    isNewProduct.value = true
  }

  function saveProduct(updated: InventoryProduct) {
    if (isNewProduct.value) {
      products.value.push({ ...updated, id: String(Date.now()) })
    } else {
      const idx = products.value.findIndex(p => p.id === updated.id)
      if (idx >= 0) products.value.splice(idx, 1, { ...updated })
    }
    closeDetail()
  }

  function deleteProduct(id: string) {
    products.value = products.value.filter(p => p.id !== id)
    if (selectedProduct.value?.id === id) closeDetail()
  }

  function prevPage() {
    if (currentPage.value > 1) currentPage.value--
  }

  function nextPage() {
    if (currentPage.value < totalPages.value) currentPage.value++
  }

  function goToPage(page: number) {
    currentPage.value = page
  }

  return {
    products, searchQuery, activeCategory, currentPage, pageSize,
    totalPages, filtered, paginated, selectedProduct, isNewProduct,
    selectCategory, selectProduct, closeDetail, newProduct,
    saveProduct, deleteProduct, prevPage, nextPage, goToPage,
  }
}
