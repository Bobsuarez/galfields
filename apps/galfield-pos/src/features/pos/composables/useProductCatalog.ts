import { ref, computed, onMounted } from 'vue'
import type { Product, ProductCategory } from '../../../types'
import { useProducts } from '../../../composables/useProducts'

/**
 * One tappable card's worth of sibling rows sharing the same physical stock
 * (e.g. "Media"/"Completa" of the same SKU, grouped by `remoteVariantId` —
 * see sync.rs's "Sale units" notes). `display` is the representative row
 * used for the card's name/image/price (the base unit, `conversionFactor
 * === 1`, when there is one); `units` is every sibling, base first. A group
 * with a single unit behaves exactly like a plain product always did; a
 * group with more than one is what triggers the unit-picker modal (see
 * ProductCatalog.vue).
 */
export interface ProductGroup {
  key: string
  display: Product
  units: Product[]
}

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

  // sync.rs appends the unit's own name to `productName` whenever a variant
  // has more than one unit (e.g. "Cigarrillos Malboro - Completa") so
  // Inventory/POS rows stay distinguishable before grouping. Once grouped
  // back into one card, that suffix has to come back off - the card/picker
  // title should read "Cigarrillos Malboro", not "Cigarrillos Malboro -
  // Unidad". Safe to derive: the suffix is always exactly " - " + the row's
  // own `unitName`.
  function baseNameOf(product: Product): string {
    const suffix = ` - ${product.unitName}`
    return product.productName.endsWith(suffix) ? product.productName.slice(0, -suffix.length) : product.productName
  }

  // Collapses sibling sale-unit rows into one card each (spec
  // 03-unidades-venta-conversion, step 6) — a row with no `remoteVariantId`
  // (never synced under a cloud variant) is its own singleton group by `id`,
  // so it never accidentally merges with another ungrouped row. Sorted by
  // conversionFactor so the base unit (factor 1) is always `units[0]` and
  // the natural pick for `display`.
  const groupedProducts = computed<ProductGroup[]>(() => {
    const groups = new Map<string, Product[]>()
    for (const p of filteredProducts.value) {
      const key = p.remoteVariantId != null ? `variant-${p.remoteVariantId}` : `row-${p.id}`
      const list = groups.get(key)
      if (list) list.push(p)
      else groups.set(key, [p])
    }

    return [...groups.entries()].map(([key, rows]) => {
      const units = [...rows].sort((a, b) => a.conversionFactor - b.conversionFactor)
      const representative = units.find(u => u.conversionFactor === 1) ?? units[0]
      // Only strip the suffix when there's actually more than one unit to
      // disambiguate from - a single-unit product's name is already plain.
      const display = units.length > 1
        ? { ...representative, productName: baseNameOf(representative) }
        : representative
      return { key, display, units }
    })
  })

  function selectCategory(id: string) {
    activeCategory.value = id
  }

  return {
    categories,
    activeCategory,
    searchQuery,
    filteredProducts,
    groupedProducts,
    isLoading,
    selectCategory,
    loadProducts,
  }
}
