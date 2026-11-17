import { CurrencyPipe } from '@angular/common';
import { Component, computed, DestroyRef, inject, input, numberAttribute, signal } from '@angular/core';
import { toObservable, toSignal } from '@angular/core/rxjs-interop';
import { RouterLink } from '@angular/router';
import { catchError, map, of, startWith, switchMap } from 'rxjs';
import { ProductService } from '../services/product.service';
import { PageResponse, ProductSummary, ProductType } from '../models/product';

type ProductsState =
  | { status: 'loading' }
  | { status: 'error' }
  | { status: 'ready'; page: PageResponse<ProductSummary> };

const PAGE_SIZE = 12;

// Shown when a product has no image yet (most seeded products don't).
const TYPE_ICONS: Record<ProductType, string> = {
  CPU: '⚙️', MOTHERBOARD: '🧩', RAM: '🧠', GPU: '🎮', STORAGE: '💾', PSU: '🔋',
  CASE: '🗄️', COOLER: '❄️', LAPTOP: '💻', DESKTOP: '🖥️', ACCESSORY: '🔌', PERIPHERAL: '🖱️',
};

@Component({
  selector: 'app-shop',
  standalone: true,
  imports: [CurrencyPipe, RouterLink],
  templateUrl: './shop.component.html',
  styleUrl: './shop.component.scss',
})
export class ShopComponent {
  private readonly productService = inject(ProductService);

  // The URL (?category=&page=) is the single source of truth, so the nav links, tabs,
  // paging and shared links all stay in sync. Page is 0-based like the backend.
  readonly category = input<string | undefined>();
  readonly page = input(0, { transform: (v: unknown) => Math.max(0, numberAttribute(v, 0)) });

  readonly categories = toSignal(
    this.productService.getCategories().pipe(catchError(() => of([]))),
    { initialValue: [] },
  );

  private readonly query = computed(() => ({
    category: this.category(),
    page: this.page(),
    size: PAGE_SIZE,
  }));

  // switchMap cancels the previous request, so fast tab clicks can't show stale results.
  readonly state = toSignal(
    toObservable(this.query).pipe(
      switchMap((q) =>
        this.productService.searchProducts(q).pipe(
          map((page): ProductsState => ({ status: 'ready', page })),
          catchError(() => of<ProductsState>({ status: 'error' })),
          startWith<ProductsState>({ status: 'loading' }),
        ),
      ),
    ),
    { initialValue: { status: 'loading' } as ProductsState },
  );

  readonly justAdded = signal<number | null>(null);
  private addedTimer?: ReturnType<typeof setTimeout>;

  constructor() {
    inject(DestroyRef).onDestroy(() => clearTimeout(this.addedTimer));
  }

  iconFor(product: ProductSummary): string {
    return TYPE_ICONS[product.productType];
  }

  // null removes the param, so "All" and page 1 give a clean URL. Changing category resets paging.
  categoryParams(slug: string | null) {
    return { category: slug, page: null };
  }

  pageParams(page: number) {
    return { page: page === 0 ? null : page };
  }

  // TODO: no cart exists yet. This only shows a brief "Added ✓" on the button.
  addToCart(product: ProductSummary): void {
    clearTimeout(this.addedTimer);
    this.justAdded.set(product.id);
    this.addedTimer = setTimeout(() => this.justAdded.set(null), 1200);
  }
}
