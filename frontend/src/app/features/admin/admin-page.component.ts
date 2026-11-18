import { ChangeDetectionStrategy, Component } from '@angular/core';
import { TranslocoPipe } from '@jsverse/transloco';

/** Placeholder – admin moderation screens come in a later story. */
@Component({
  selector: 'app-admin-page',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [TranslocoPipe],
  template: `
    <section class="rounded-xl border border-slate-200 bg-white ps-6 pe-6 py-8 text-start">
      <h1 class="text-2xl font-bold text-slate-900">{{ 'ADMIN.TITLE' | transloco }}</h1>
      <p class="mt-2 text-slate-600">{{ 'ADMIN.PLACEHOLDER' | transloco }}</p>
    </section>
  `,
})
export class AdminPageComponent {}
