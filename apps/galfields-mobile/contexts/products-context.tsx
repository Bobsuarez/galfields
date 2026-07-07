import React, { createContext, useCallback, useContext, useEffect, useState } from 'react';
import { Product, ProductFormData } from '@/types/product';
import { createProduct, fetchCategories, RemoteCategory } from '@/services/products-api';

interface ProductsContextValue {
  products: Product[];
  addProduct: (data: ProductFormData) => Promise<Product>;
  searchProducts: (query: string) => Product[];
}

const ProductsContext = createContext<ProductsContextValue | null>(null);

const INITIAL_PRODUCTS: Product[] = [
  { id: '1', name: 'Gaseosa 600ml', category: 'Bebidas', price: 3500, stock: 24, unit: 'Unidad' },
  { id: '2', name: 'Agua 500ml', category: 'Bebidas', price: 2000, stock: 40, unit: 'Unidad' },
  { id: '3', name: 'Papas Fritas', category: 'Snacks', price: 3000, stock: 15, unit: 'Unidad' },
  { id: '4', name: 'Chocolate', category: 'Snacks', price: 2500, stock: 30, unit: 'Unidad' },
  { id: '5', name: 'Leche 1L', category: 'Lácteos', price: 4200, stock: 18, unit: 'Lt' },
  { id: '6', name: 'Pan de Molde', category: 'Panadería', price: 3000, stock: 20, unit: 'Unidad' },
  { id: '7', name: 'Café 100g', category: 'Bebidas', price: 5500, stock: 12, unit: 'Unidad' },
  { id: '8', name: 'Papel Higiénico', category: 'Limpieza', price: 3200, stock: 25, unit: 'Unidad' },
];

export function ProductsProvider({ children }: { children: React.ReactNode }) {
  const [products, setProducts] = useState<Product[]>(INITIAL_PRODUCTS);
  const [categories, setCategories] = useState<RemoteCategory[]>([]);

  useEffect(() => {
    fetchCategories()
      .then(setCategories)
      .catch(err => console.warn('No se pudieron cargar las categorías del backend:', err));
  }, []);

  const resolveCategoryId = useCallback(
    (categoryName: string): number => {
      const match = categories.find(c => c.name === categoryName);
      if (!match) {
        throw new Error(
          `La categoría "${categoryName}" no existe en el servidor. Sincroniza las categorías e intenta de nuevo.`,
        );
      }
      return match.categoryId;
    },
    [categories],
  );

  const addProduct = async (data: ProductFormData): Promise<Product> => {
    if (!data.barcode) {
      throw new Error('El código de barras es requerido para guardar el producto.');
    }

    const remoteProduct = await createProduct({
      name: data.name,
      categoryId: resolveCategoryId(data.category),
      price: data.price,
      barcode: data.barcode,
      initialStock: data.stock,
      imageUri: data.imageUri,
    });

    const newProduct: Product = { ...data, id: String(remoteProduct.productId) };
    setProducts(prev => [newProduct, ...prev]);
    return newProduct;
  };

  const searchProducts = (query: string): Product[] => {
    if (!query.trim()) return products;
    const lower = query.toLowerCase();
    return products.filter(p => p.name.toLowerCase().includes(lower));
  };

  return (
    <ProductsContext.Provider value={{ products, addProduct, searchProducts }}>
      {children}
    </ProductsContext.Provider>
  );
}

export function useProducts(): ProductsContextValue {
  const ctx = useContext(ProductsContext);
  if (!ctx) throw new Error('useProducts must be used within ProductsProvider');
  return ctx;
}
