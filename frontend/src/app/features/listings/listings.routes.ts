import { Routes } from '@angular/router';

export const LISTINGS_ROUTES: Routes = [
  {
    path: '',
    loadComponent: () => import('./listings-page.component').then((m) => m.ListingsPageComponent),
    title: 'Sakan',
  },
];
