import { HttpClient, HttpParams } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { Category, PageResponse, ProductSummary } from '../models/product';

export interface ProductQuery {
  category?: string;
  page?: number;
  size?: number;
}

// Relative URLs: in development proxy.conf.json forwards /api to Spring Boot on :8080.
@Injectable({ providedIn: 'root' })
export class ProductService {
  private readonly http = inject(HttpClient);

  getCategories(): Observable<Category[]> {
    return this.http.get<Category[]>('/api/categories');
  }

  searchProducts(query: ProductQuery): Observable<PageResponse<ProductSummary>> {
    let params = new HttpParams()
      .set('page', query.page ?? 0)
      .set('size', query.size ?? 12);
    if (query.category) {
      params = params.set('category', query.category);
    }
    return this.http.get<PageResponse<ProductSummary>>('/api/products', { params });
  }
}
