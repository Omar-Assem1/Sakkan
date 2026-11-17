import { Routes } from '@angular/router';

export const VIEWINGS_ROUTES: Routes = [
  {
    path: '',
    loadComponent: () => import('./viewings-page.component').then((m) => m.ViewingsPageComponent),
    title: 'Sakan',
  },
];
