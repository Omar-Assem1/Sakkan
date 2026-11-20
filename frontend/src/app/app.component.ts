import { ChangeDetectionStrategy, Component, OnInit, inject } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { AuthService } from './core/services/auth.service';
import { LanguageService } from './core/services/language.service';
import { ThemeService } from './core/services/theme.service';
import { TokenStorageService } from './core/services/token-storage.service';
import { NavbarComponent } from './shared/components/navbar/navbar.component';

@Component({
  selector: 'app-root',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [RouterOutlet, NavbarComponent],
  template: `
    <div
      class="flex min-h-screen flex-col bg-slate-50 text-start text-slate-900 transition-colors duration-200 dark:bg-slate-950 dark:text-slate-100"
    >
      <app-navbar />
      <main class="mx-auto w-full max-w-7xl flex-1 ps-4 pe-4 py-6 sm:ps-6 sm:pe-6 lg:ps-8 lg:pe-8">
        <router-outlet />
      </main>
    </div>
  `,
})
export class AppComponent implements OnInit {
  /** Injected eagerly so <html dir/lang> + Transloco and color theme are restored on boot. */
  private readonly language = inject(LanguageService);
  private readonly theme = inject(ThemeService);
  private readonly tokenStorage = inject(TokenStorageService);
  private readonly auth = inject(AuthService, { optional: true });

  ngOnInit(): void {
    if (this.tokenStorage.isAuthenticated()) {
      this.auth?.me().subscribe({ error: () => undefined });
    }
  }
}
