import { Routes } from '@angular/router';
import { ShopComponent } from './shop/shop.component';
import { BuildComponent } from './build/build.component';

export const routes: Routes = [
  { path: '', component: ShopComponent },
  { path: 'build', component: BuildComponent },
  { path: '**', redirectTo: '' },
];
