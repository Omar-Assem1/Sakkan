import { Routes } from '@angular/router';

export const PROPERTIES_ROUTES: Routes = [
  {
    path: 'new',
    loadComponent: () =>
      import('./property-form/property-form.component').then((m) => m.PropertyFormComponent),
    title: 'Sakan'
  },
  {
    path: ':id/edit',
    loadComponent: () =>
      import('./property-form/property-form.component').then((m) => m.PropertyFormComponent),
    title: 'Sakan'
  },
  {
    path: 'mine',
    loadComponent: () =>
      import('./my-properties/my-properties.component').then((m) => m.MyPropertiesComponent),
    title: 'Sakan'
  }
];
