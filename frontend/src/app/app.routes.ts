import { Routes } from '@angular/router';
import { authGuard } from './core/guards/auth.guard';

export const routes: Routes = [
  { path: '', pathMatch: 'full', redirectTo: 'listings' },
  {
    path: 'auth',
    loadChildren: () => import('./features/auth/auth.routes').then((m) => m.AUTH_ROUTES),
  },
  {
    path: 'listings',
    loadChildren: () =>
      import('./features/listings/listings.routes').then((m) => m.LISTINGS_ROUTES),
  },
  {
    path: 'viewings',
    canActivate: [authGuard],
    loadChildren: () =>
      import('./features/viewings/viewings.routes').then((m) => m.VIEWINGS_ROUTES),
  },
  {
    path: 'properties',
    canActivate: [authGuard],
    loadChildren: () =>
      import('./features/properties/properties.routes').then((m) => m.PROPERTIES_ROUTES),
  },
  {
    path: 'admin',
    canActivate: [authGuard],
    data: { roles: ['ADMIN'] },
    loadChildren: () => import('./features/admin/admin.routes').then((m) => m.ADMIN_ROUTES),
  },
  {
    path: 'error',
    loadComponent: () =>
      import('./features/error/error-page.component').then((m) => m.ErrorPageComponent),
  },
  { path: '**', redirectTo: 'listings' },
];
