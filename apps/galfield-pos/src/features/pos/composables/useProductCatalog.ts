import { ref, computed, onMounted } from 'vue'
import type { ProductCategory } from '../../../types'
import { useProducts } from '../../../composables/useProducts'

export function useProductCatalog() {
  const { products, isLoading, loadProducts } = useProducts()
  const activeCategory = ref('all')
  const searchQuery    = ref('')

  onMounted(loadProducts)

  // Categories are whatever distinct `category` text is present on synced
  // products (real cloud category names), not a fixed local list. `count`
  // is the total products in that category (unfiltered by search), shown
  // in the category rail so a long category list stays scannable.
  const categories = computed<ProductCategory[]>(() => {
    const counts = new Map<string, number>()
    for (const p of products.value) {
      const name = p.category.trim()
      if (!name) continue
      counts.set(name, (counts.get(name) ?? 0) + 1)
    }
    return [
      { id: 'all', name: 'Todos', count: products.value.length },
      ...[...counts.keys()]
        .sort((a, b) => a.localeCompare(b, 'es', { sensitivity: 'base' }))
        .map(name => ({ id: name, name, count: counts.get(name)! })),
    ]
  })

  // Deactivated/out-of-stock products stay in the grid (dimmed, with a
  // "Desactivado"/"Sin stock" badge — see ProductCard.vue) instead of being
  // hidden, so the cashier sees *why* something can't be sold rather than it
  // just silently disappearing.
  // Sorted by price ascending (cheapest first), then name as a tie-breaker
  // for same-priced products — overrides the backend's name-only order
  // (see products.rs::get_products) specifically for this catalog view.
  const filteredProducts = computed(() => {
    let items = products.value

    if (activeCategory.value !== 'all') {
      items = items.filter(p => p.category === activeCategory.value)
    }

    if (searchQuery.value.trim()) {
      const q = searchQuery.value.toLowerCase()
      items = items.filter(p => p.productName.toLowerCase().includes(q))
    }

    return [...items].sort((a, b) =>
      a.unitPrice - b.unitPrice || a.productName.localeCompare(b.productName, 'es', { sensitivity: 'base' }),
    )
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
    loadProducts,
  }
}
