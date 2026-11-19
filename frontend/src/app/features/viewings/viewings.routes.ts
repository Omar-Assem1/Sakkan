import { Routes } from '@angular/router';
import { roleGuard } from '../../core/guards/role.guard';

/** Same roles as the backend: slots = OWNER/AGENT, viewing requests = BUYER_TENANT/OWNER/AGENT. */
const MANAGE_SLOTS = [roleGuard('OWNER', 'AGENT')];
const REQUEST_VIEWING = [roleGuard('BUYER_TENANT', 'OWNER', 'AGENT')];

export const VIEWINGS_ROUTES: Routes = [
  {
    path: '',
    canMatch: MANAGE_SLOTS,
    loadComponent: () =>
      import('./availability-form/availability-form.component').then(
        (m) => m.AvailabilityFormComponent,
      ),
    title: 'Sakan',
  },
  {
    path: 'availability',
    canMatch: MANAGE_SLOTS,
    loadComponent: () =>
      import('./availability-form/availability-form.component').then(
        (m) => m.AvailabilityFormComponent,
      ),
    title: 'Sakan',
  },
  {
    path: 'availability/:propertyId',
    canMatch: MANAGE_SLOTS,
    loadComponent: () =>
      import('./availability-form/availability-form.component').then(
        (m) => m.AvailabilityFormComponent,
      ),
    title: 'Sakan',
  },
  {
    path: 'request',
    canMatch: REQUEST_VIEWING,
    loadComponent: () =>
      import('./request-viewing-form/request-viewing-form.component').then(
        (m) => m.RequestViewingFormComponent,
      ),
    title: 'Sakan',
  },
  {
    path: 'request/:propertyId',
    canMatch: REQUEST_VIEWING,
    loadComponent: () =>
      import('./request-viewing-form/request-viewing-form.component').then(
        (m) => m.RequestViewingFormComponent,
      ),
    title: 'Sakan',
  },
];
