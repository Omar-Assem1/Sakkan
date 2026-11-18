import { Routes } from '@angular/router';

export const VIEWINGS_ROUTES: Routes = [
  {
    path: '',
    loadComponent: () =>
      import('./availability-form/availability-form.component').then(
        (m) => m.AvailabilityFormComponent,
      ),
    title: 'Sakan',
  },
  {
    path: 'availability',
    loadComponent: () =>
      import('./availability-form/availability-form.component').then(
        (m) => m.AvailabilityFormComponent,
      ),
    title: 'Sakan',
  },
  {
    path: 'availability/:propertyId',
    loadComponent: () =>
      import('./availability-form/availability-form.component').then(
        (m) => m.AvailabilityFormComponent,
      ),
    title: 'Sakan',
  },
];
