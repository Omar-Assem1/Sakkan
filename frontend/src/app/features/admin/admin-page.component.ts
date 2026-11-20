import { ChangeDetectionStrategy, Component } from '@angular/core';
import { TranslocoDirective, TranslocoPipe } from '@jsverse/transloco';
import { RouterLink } from '@angular/router';

@Component({
  selector: 'app-admin-page',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [TranslocoPipe, TranslocoDirective, RouterLink],
  template: `
    <div class="mx-auto max-w-5xl space-y-6 text-start">
      <section
        class="rounded-2xl border border-slate-200 bg-white ps-6 pe-6 py-8 shadow-2xs transition-colors dark:border-slate-800 dark:bg-slate-900"
      >
        <span
          class="inline-flex items-center rounded-full bg-emerald-100 ps-3 pe-3 py-1 text-xs font-bold uppercase tracking-wider text-emerald-800 dark:bg-emerald-950 dark:text-emerald-300"
        >
          ADMIN
        </span>
        <h1
          class="mt-3 text-2xl font-extrabold tracking-tight text-slate-900 sm:text-3xl dark:text-slate-100"
        >
          {{ 'ADMIN.TITLE' | transloco }}
        </h1>
        <p class="mt-2 text-sm text-slate-600 dark:text-slate-400">
          {{ 'ADMIN.PLACEHOLDER' | transloco }}
        </p>

        <div class="mt-6 grid grid-cols-1 gap-4 sm:grid-cols-2">
          <div
            class="flex flex-col justify-between rounded-xl border border-slate-200 bg-slate-50/70 p-5 dark:border-slate-800 dark:bg-slate-800/60"
          >
            <div>
              <h2 class="text-base font-bold text-slate-900 dark:text-slate-100" *transloco="let t">
                {{ t('ADMIN_USERS.TITLE') }}
              </h2>
              <p class="mt-1 text-xs text-slate-600 dark:text-slate-400" *transloco="let t">
                {{ t('ADMIN_USERS.SUBTITLE') }}
              </p>
            </div>
            <div class="mt-4">
              <a
                routerLink="/admin/users"
                class="inline-flex items-center rounded-xl bg-emerald-600 ps-4 pe-4 py-2 text-sm font-semibold text-white shadow-2xs transition hover:bg-emerald-700 dark:hover:bg-emerald-500"
                *transloco="let t"
              >
                {{ t('ADMIN_USERS.TITLE') }}
              </a>
            </div>
          </div>

          <div
            class="flex flex-col justify-between rounded-xl border border-slate-200 bg-slate-50/70 p-5 dark:border-slate-800 dark:bg-slate-800/60"
          >
            <div>
              <h2 class="text-base font-bold text-slate-900 dark:text-slate-100">
                {{ 'LISTINGS.TITLE' | transloco }}
              </h2>
              <p class="mt-1 text-xs text-slate-600 dark:text-slate-400">
                {{ 'LISTINGS.PLACEHOLDER' | transloco }}
              </p>
            </div>
            <div class="mt-4">
              <a
                routerLink="/listings"
                class="inline-flex items-center rounded-xl border border-slate-300 bg-white ps-4 pe-4 py-2 text-sm font-semibold text-slate-700 shadow-2xs transition hover:bg-slate-50 dark:border-slate-700 dark:bg-slate-900 dark:text-slate-200 dark:hover:bg-slate-800"
              >
                {{ 'NAV.LISTINGS' | transloco }}
              </a>
            </div>
          </div>
        </div>
      </section>
    </div>
  `,
})
export class AdminPageComponent {}
