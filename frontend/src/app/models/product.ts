// Mirrors the backend DTOs (com.futuretech.dto). Keep these in sync when the API changes.

export type ProductType =
  | 'CPU' | 'MOTHERBOARD' | 'RAM' | 'GPU' | 'STORAGE' | 'PSU' | 'CASE' | 'COOLER'
  | 'LAPTOP' | 'DESKTOP' | 'ACCESSORY' | 'PERIPHERAL';

export interface ProductSummary {
  id: number;
  sku: string;
  name: string;
  brand: string;
  price: number;
  inStock: boolean;
  stockQuantity: number;
  productType: ProductType;
  categorySlug: string;
  categoryName: string;
  performanceScore: number | null;
  imageUrl: string | null;
}

export interface Category {
  id: number;
  name: string;
  slug: string;
  description: string | null;
}

export interface PageResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}
