import { ChangeDetectionStrategy, Component } from '@angular/core';
import { TranslocoDirective, TranslocoPipe } from '@jsverse/transloco';
import { RouterLink } from '@angular/router';

@Component({
  selector: 'app-admin-page',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [TranslocoPipe, TranslocoDirective, RouterLink],
  template: `
    <section class="rounded-xl border border-slate-200 bg-white ps-6 pe-6 py-8 text-start">
      <h1 class="text-2xl font-bold text-slate-900">{{ 'ADMIN.TITLE' | transloco }}</h1>
      <a routerLink="/admin/users" class="mt-4 inline-flex rounded bg-indigo-600 px-4 py-2 text-white" *transloco="let t">{{ t('ADMIN_USERS.TITLE') }}</a>
    </section>
  `,
})
export class AdminPageComponent {}
