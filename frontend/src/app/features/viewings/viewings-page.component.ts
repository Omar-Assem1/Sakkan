import { ChangeDetectionStrategy, Component } from '@angular/core';
import { TranslocoPipe } from '@jsverse/transloco';

/** Placeholder – Form 7 (availability) and Listing 8 (My Viewings) come in SCRUM-94 / SCRUM-102. */
@Component({
  selector: 'app-viewings-page',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [TranslocoPipe],
  template: `
    <section class="rounded-xl border border-slate-200 bg-white ps-6 pe-6 py-8 text-start">
      <h1 class="text-2xl font-bold text-slate-900">{{ 'VIEWINGS.TITLE' | transloco }}</h1>
      <p class="mt-2 text-slate-600">{{ 'VIEWINGS.PLACEHOLDER' | transloco }}</p>
    </section>
  `,
})
export class ViewingsPageComponent {}
