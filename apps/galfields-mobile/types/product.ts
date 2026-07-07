export type ProductCategory =
  | 'Bebidas'
  | 'Snacks'
  | 'Lácteos'
  | 'Panadería'
  | 'Limpieza'
  | 'Otro';

export type ProductUnit = 'Unidad' | 'Kg' | 'Lt' | 'Caja' | 'Docena';

export interface Product {
  id: string;
  name: string;
  category: ProductCategory;
  price: number;
  stock: number;
  unit: ProductUnit;
  barcode?: string;
  imageUri?: string;
}

export type ProductFormData = Omit<Product, 'id'>;

export const PRODUCT_CATEGORIES: ProductCategory[] = [
  'Bebidas',
  'Snacks',
  'Lácteos',
  'Panadería',
  'Limpieza',
  'Otro',
];

export const PRODUCT_UNITS: ProductUnit[] = [
  'Unidad',
  'Kg',
  'Lt',
  'Caja',
  'Docena',
];
