import { ChangeDetectionStrategy, Component } from '@angular/core';
import { TranslocoPipe } from '@jsverse/transloco';

/** Placeholder – property browse/search listing comes in a later story. */
@Component({
  selector: 'app-listings-page',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [TranslocoPipe],
  template: `
    <section class="rounded-xl border border-slate-200 bg-white ps-6 pe-6 py-8 text-start">
      <h1 class="text-2xl font-bold text-slate-900">{{ 'LISTINGS.TITLE' | transloco }}</h1>
      <p class="mt-2 text-slate-600">{{ 'LISTINGS.PLACEHOLDER' | transloco }}</p>
    </section>
  `,
})
export class ListingsPageComponent {}
