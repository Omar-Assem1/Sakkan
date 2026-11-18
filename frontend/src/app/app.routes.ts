import { Routes } from '@angular/router';
import { ErrorPageComponent } from './shared/components/error-page/error-page';

export const routes: Routes = [
  {
    path: 'error',
    component: ErrorPageComponent,
    title: 'Error – Sakan',
  },
  // wildcard – redirect unknown paths to the error page with a 404
  {
    path: '**',
    component: ErrorPageComponent,
    title: 'Page Not Found – Sakan',
    data: {
      error: { status: 404, title: 'Page Not Found', detail: 'The page you are looking for does not exist.' },
    },
  },
];
