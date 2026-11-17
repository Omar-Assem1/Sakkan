import { Routes } from '@angular/router';

export const ADMIN_ROUTES: Routes = [
  {
    path: '',
    loadComponent: () => import('./admin-page.component').then((m) => m.AdminPageComponent),
    title: 'Sakan',
  },
];
