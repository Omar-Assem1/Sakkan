import { ChangeDetectionStrategy, Component } from '@angular/core';
import { TranslocoPipe } from '@jsverse/transloco';

/** Placeholder – Form 7 (availability) and Listing 8 (My Viewings) come in SCRUM-94 / SCRUM-102. */
@Component({
  selector: 'app-viewings-page',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [TranslocoPipe],
  template: `
    <section
      class="rounded-2xl border border-slate-200 bg-white ps-6 pe-6 py-8 text-start shadow-2xs dark:border-slate-800 dark:bg-slate-900"
    >
      <h1 class="text-2xl font-bold text-slate-900 dark:text-slate-100">
        {{ 'VIEWINGS.TITLE' | transloco }}
      </h1>
      <p class="mt-2 text-slate-600 dark:text-slate-400">{{ 'VIEWINGS.PLACEHOLDER' | transloco }}</p>
    </section>
  `,
})
export class ViewingsPageComponent {}
