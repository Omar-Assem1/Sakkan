import { Routes } from '@angular/router';

export const ADMIN_ROUTES: Routes = [
  { path: 'users', loadComponent: () => import('./users/admin-users.component').then((m) => m.AdminUsersComponent), title: 'Manage users' },
  {
    path: '',
    loadComponent: () => import('./admin-page.component').then((m) => m.AdminPageComponent),
    title: 'Sakan',
  },
];
