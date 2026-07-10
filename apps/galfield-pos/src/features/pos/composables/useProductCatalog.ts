import { ref, computed, onMounted } from 'vue'
import type { ProductCategory } from '../../../types'
import { useProducts } from '../../../composables/useProducts'

export function useProductCatalog() {
  const { products, isLoading, loadProducts } = useProducts()
  const activeCategory = ref('all')
  const searchQuery    = ref('')

  onMounted(loadProducts)

  // Categories are whatever distinct `category` text is present on synced
  // products (real cloud category names), not a fixed local list.
  const categories = computed<ProductCategory[]>(() => {
    const names = new Set(
      products.value
        .filter(p => p.category.trim())
        .map(p => p.category),
    )
    return [
      { id: 'all', name: 'Todos' },
      ...[...names].sort().map(name => ({ id: name, name })),
    ]
  })

  // Deactivated/out-of-stock products stay in the grid (dimmed, with a
  // "Desactivado"/"Sin stock" badge — see ProductCard.vue) instead of being
  // hidden, so the cashier sees *why* something can't be sold rather than it
  // just silently disappearing.
  const filteredProducts = computed(() => {
    let items = products.value

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
    categories,
    activeCategory,
    searchQuery,
    filteredProducts,
    isLoading,
    selectCategory,
  }
}
