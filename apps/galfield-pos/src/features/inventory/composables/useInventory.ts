import { ref, computed, watch, onMounted } from 'vue'
import type { Product, ProductCategory } from '../../../types'
import { useProducts } from '../../../composables/useProducts'

/**
 * Read-only inventory list sourced from the local `products` table (synced
 * from the cloud catalog — see the Sincronización screen). There is no
 * create/update/delete command for products here on purpose: the cloud
 * catalog (managed from the mobile app) is the source of truth, and editing
 * a row locally would just get overwritten by the next sync.
 */
export function useInventory() {
  const { products, isLoading, loadProducts } = useProducts()
  const searchQuery = ref('')
  const activeCategory = ref('all')
  const currentPage = ref(1)
  const pageSize = ref(10)
  const selectedProduct = ref<Product | null>(null)

  onMounted(loadProducts)

  watch([searchQuery, activeCategory], () => { currentPage.value = 1 })

  const categories = computed<ProductCategory[]>(() => {
    const counts = new Map<string, number>()
    for (const p of products.value) {
      const name = p.category.trim()
      if (!name) continue
      counts.set(name, (counts.get(name) ?? 0) + 1)
    }
    return [
      { id: 'all', name: 'Todas las categorías', count: products.value.length },
      ...[...counts.keys()]
        .sort((a, b) => a.localeCompare(b, 'es', { sensitivity: 'base' }))
        .map(name => ({ id: name, name, count: counts.get(name)! })),
    ]
  })

  const filtered = computed(() => {
    let items = products.value
    if (activeCategory.value !== 'all') {
      items = items.filter(p => p.category === activeCategory.value)
    }
    if (searchQuery.value.trim()) {
      const q = searchQuery.value.toLowerCase()
      items = items.filter(p => p.productName.toLowerCase().includes(q) || p.barcode.includes(q))
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

  function selectProduct(product: Product) {
    selectedProduct.value = product
  }

  function closeDetail() {
    selectedProduct.value = null
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
    categories, searchQuery, activeCategory, currentPage, pageSize,
    totalPages, filtered, paginated, selectedProduct, isLoading,
    selectCategory, selectProduct, closeDetail,
    prevPage, nextPage, goToPage,
  }
}
