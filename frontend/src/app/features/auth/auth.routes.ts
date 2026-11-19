import { Routes } from '@angular/router';

export const AUTH_ROUTES: Routes = [
  { path: '', pathMatch: 'full', redirectTo: 'sign-in' },
  {
    path: 'sign-in',
    loadComponent: () => import('./sign-in/sign-in.component').then((m) => m.SignInComponent),
    title: 'Sakan',
  },
  {
    path: 'sign-up',
    loadComponent: () => import('./sign-up/sign-up.component').then((m) => m.SignUpComponent),
    title: 'Sakan',
  },
  {
    path: 'admin-sign-in',
    loadComponent: () =>
      import('./admin-sign-in/admin-sign-in.component').then((m) => m.AdminSignInComponent),
    title: 'Sakan',
  },
];
