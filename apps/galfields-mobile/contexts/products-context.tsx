import React, { createContext, useCallback, useContext, useEffect, useState } from 'react';
import { Product, ProductInput } from '@/types/product';
import {
  createProduct,
  deactivateProduct,
  fetchProducts,
  RemoteProduct,
  updateProduct as updateProductApi,
} from '@/services/products-api';

interface ProductsContextValue {
  products: Product[];
  loading: boolean;
  loadingMore: boolean;
  hasMore: boolean;
  error: string | null;
  refresh: () => void;
  loadMore: () => void;
  addProduct: (data: ProductInput) => Promise<Product>;
  updateProduct: (productId: string, data: ProductInput) => Promise<Product>;
  removeProduct: (productId: string) => Promise<void>;
  searchProducts: (query: string) => Product[];
}

const ProductsContext = createContext<ProductsContextValue | null>(null);

/** Flattens a multi-variant RemoteProduct into the single-row-per-product
 * shape the Products list displays. */
function toLocalProduct(remote: RemoteProduct): Product {
  const firstVariant = remote.variants[0];
  return {
    id: String(remote.productId),
    name: remote.name,
    category: remote.categoryName ?? '',
    price: firstVariant?.price ?? 0,
    stock: remote.variants.reduce((sum, v) => sum + v.stock, 0),
    variantCount: remote.variants.length,
    barcode: firstVariant?.barcode,
    imageUri: remote.imageUrl ?? undefined,
  };
}

export function ProductsProvider({ children }: { children: React.ReactNode }) {
  const [products, setProducts] = useState<Product[]>([]);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(1);
  const [loading, setLoading] = useState(true);
  const [loadingMore, setLoadingMore] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const loadPage = useCallback(async (pageToLoad: number, replace: boolean) => {
    (pageToLoad === 0 ? setLoading : setLoadingMore)(true);
    setError(null);
    try {
      const result = await fetchProducts(pageToLoad);
      const mapped = result.content.map(toLocalProduct);
      setProducts(prev => (replace ? mapped : [...prev, ...mapped]));
      setPage(result.number);
      setTotalPages(result.totalPages);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'No se pudieron cargar los productos.');
    } finally {
      (pageToLoad === 0 ? setLoading : setLoadingMore)(false);
    }
  }, []);

  useEffect(() => {
    loadPage(0, true);
  }, [loadPage]);

  const refresh = () => loadPage(0, true);

  const loadMore = () => {
    if (loading || loadingMore || page + 1 >= totalPages) return;
    loadPage(page + 1, false);
  };

  const addProduct = async (data: ProductInput): Promise<Product> => {
    const remoteProduct = await createProduct(data);
    const newProduct = toLocalProduct(remoteProduct);
    setProducts(prev => [newProduct, ...prev]);
    return newProduct;
  };

  const updateProduct = async (productId: string, data: ProductInput): Promise<Product> => {
    const remoteProduct = await updateProductApi(productId, data);
    const updated = toLocalProduct(remoteProduct);
    setProducts(prev => prev.map(p => (p.id === updated.id ? updated : p)));
    return updated;
  };

  /** Soft delete — see `services/products-api.ts`'s `deactivateProduct`: the
   * backend has no real delete, it flips `active` to false so past sales
   * referencing this product stay intact. The Products catalog only ever
   * lists active products, so once deactivated it's dropped from local
   * state too instead of waiting for the next `refresh()`. */
  const removeProduct = async (productId: string): Promise<void> => {
    await deactivateProduct(Number(productId));
    setProducts(prev => prev.filter(p => p.id !== productId));
  };

  const searchProducts = (query: string): Product[] => {
    if (!query.trim()) return products;
    const lower = query.toLowerCase();
    return products.filter(p => p.name.toLowerCase().includes(lower));
  };

  return (
    <ProductsContext.Provider
      value={{
        products,
        loading,
        loadingMore,
        hasMore: page + 1 < totalPages,
        error,
        refresh,
        loadMore,
        addProduct,
        updateProduct,
        removeProduct,
        searchProducts,
      }}
    >
      {children}
    </ProductsContext.Provider>
  );
}

export function useProducts(): ProductsContextValue {
  const ctx = useContext(ProductsContext);
  if (!ctx) throw new Error('useProducts must be used within ProductsProvider');
  return ctx;
}
