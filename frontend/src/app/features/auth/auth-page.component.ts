import { ChangeDetectionStrategy, Component } from '@angular/core';
import { TranslocoPipe } from '@jsverse/transloco';

/** Placeholder – real login/register forms come in the auth story. */
@Component({
  selector: 'app-auth-page',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [TranslocoPipe],
  template: `
    <section class="rounded-xl border border-slate-200 bg-white ps-6 pe-6 py-8 text-start">
      <h1 class="text-2xl font-bold text-slate-900">{{ 'AUTH.TITLE' | transloco }}</h1>
      <p class="mt-2 text-slate-600">{{ 'AUTH.PLACEHOLDER' | transloco }}</p>
    </section>
  `,
})
export class AuthPageComponent {}
