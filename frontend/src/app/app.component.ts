import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { LanguageService } from './core/services/language.service';
import { NavbarComponent } from './shared/components/navbar/navbar.component';

@Component({
  selector: 'app-root',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [RouterOutlet, NavbarComponent],
  template: `
    <div class="min-h-screen bg-slate-50 text-start text-slate-900">
      <app-navbar />
      <main class="mx-auto max-w-7xl ps-4 pe-4 py-6">
        <router-outlet />
      </main>
    </div>
  `,
})
export class AppComponent {
  /** Injected eagerly so <html dir/lang> + Transloco are restored from localStorage on boot. */
  private readonly language = inject(LanguageService);
}
