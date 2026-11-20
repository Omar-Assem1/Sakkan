import { ChangeDetectionStrategy, Component, computed, inject } from '@angular/core';
import { Router, RouterLink, RouterLinkActive } from '@angular/router';
import { TranslocoPipe } from '@jsverse/transloco';
import { LanguageService } from '../../../core/services/language.service';
import { ThemeService } from '../../../core/services/theme.service';
import { TokenStorageService } from '../../../core/services/token-storage.service';
import { UiButtonComponent } from '../ui-button/ui-button.component';

@Component({
  selector: 'app-navbar',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [RouterLink, RouterLinkActive, TranslocoPipe, UiButtonComponent],
  template: `
    <header
      class="sticky top-0 z-30 border-b border-slate-200/80 bg-white/90 shadow-2xs backdrop-blur-md transition-colors dark:border-slate-800/80 dark:bg-slate-900/90"
    >
      <nav
        class="mx-auto flex max-w-7xl flex-wrap items-center gap-3 ps-4 pe-4 py-3 sm:ps-6 sm:pe-6 lg:ps-8 lg:pe-8"
        [attr.aria-label]="'NAV.MAIN' | transloco"
      >
        <a
          routerLink="/listings"
          class="me-3 inline-flex items-center gap-2.5 text-xl font-extrabold tracking-tight text-emerald-700 transition hover:text-emerald-800 dark:text-emerald-400 dark:hover:text-emerald-300"
        >
          <span
            aria-hidden="true"
            class="inline-flex h-8 w-8 items-center justify-center rounded-xl bg-emerald-600 text-sm font-black text-white shadow-2xs"
          >
            S
          </span>
          <span>{{ 'APP.TITLE' | transloco }}</span>
        </a>

        <ul class="flex flex-wrap items-center gap-1.5 text-sm font-medium">
          <li>
            <a
              routerLink="/listings"
              routerLinkActive="bg-emerald-50 text-emerald-700 ring-1 ring-emerald-200/70 dark:bg-emerald-950/70 dark:text-emerald-300 dark:ring-emerald-800/80"
              class="inline-flex items-center gap-1.5 rounded-lg ps-3 pe-3 py-2 text-slate-700 transition hover:bg-slate-100 dark:text-slate-200 dark:hover:bg-slate-800"
              data-testid="nav-listings"
              >{{ 'NAV.LISTINGS' | transloco }}</a
            >
          </li>
          @if (canAccessViewings()) {
            <li>
              <a
                routerLink="/viewings"
                routerLinkActive="bg-emerald-50 text-emerald-700 ring-1 ring-emerald-200/70 dark:bg-emerald-950/70 dark:text-emerald-300 dark:ring-emerald-800/80"
                class="inline-flex items-center gap-1.5 rounded-lg ps-3 pe-3 py-2 text-slate-700 transition hover:bg-slate-100 dark:text-slate-200 dark:hover:bg-slate-800"
                data-testid="nav-viewings"
                >{{ 'NAV.VIEWINGS' | transloco }}</a
              >
            </li>
          }
          @if (canAccessMyProperties()) {
            <li>
              <a
                routerLink="/properties/mine"
                routerLinkActive="bg-emerald-50 text-emerald-700 ring-1 ring-emerald-200/70 dark:bg-emerald-950/70 dark:text-emerald-300 dark:ring-emerald-800/80"
                class="inline-flex items-center gap-1.5 rounded-lg ps-3 pe-3 py-2 text-slate-700 transition hover:bg-slate-100 dark:text-slate-200 dark:hover:bg-slate-800"
                data-testid="nav-my-properties"
                >{{ 'NAV.MY_PROPERTIES' | transloco }}</a
              >
            </li>
          }
          @if (isAdmin()) {
            <li>
              <a
                routerLink="/admin"
                routerLinkActive="bg-emerald-50 text-emerald-700 ring-1 ring-emerald-200/70 dark:bg-emerald-950/70 dark:text-emerald-300 dark:ring-emerald-800/80"
                class="inline-flex items-center gap-1.5 rounded-lg ps-3 pe-3 py-2 text-slate-700 transition hover:bg-slate-100 dark:text-slate-200 dark:hover:bg-slate-800"
                data-testid="nav-admin"
                >{{ 'NAV.ADMIN' | transloco }}</a
              >
            </li>
          }
        </ul>

        <div class="ms-auto flex flex-wrap items-center gap-2">
          @if (isAuthenticated() && (currentUsername() || currentRole())) {
            <div
              class="hidden sm:inline-flex items-center gap-2 rounded-full border border-slate-200 bg-slate-50 ps-3 pe-3 py-1 text-xs font-medium text-slate-700 dark:border-slate-700 dark:bg-slate-800 dark:text-slate-200"
            >
              @if (currentUsername()) {
                <span class="font-semibold text-slate-900 dark:text-white">{{ currentUsername() }}</span>
              }
              @if (currentRole()) {
                <span
                  class="rounded-full bg-emerald-100 ps-2 pe-2 py-0.5 text-[11px] font-bold text-emerald-800 dark:bg-emerald-950 dark:text-emerald-300"
                >
                  {{ currentRole() }}
                </span>
              }
            </div>
          }

          <app-ui-button
            variant="outline"
            data-testid="theme-toggle"
            [ariaLabel]="'NAV.TOGGLE_THEME_LABEL' | transloco"
            (clicked)="theme.toggleTheme()"
          >
            <span aria-hidden="true">{{ theme.isDark() ? '☀' : '☾' }}</span>
            <span class="hidden md:inline">{{
              (theme.isDark() ? 'NAV.TOGGLE_THEME_LIGHT' : 'NAV.TOGGLE_THEME_DARK') | transloco
            }}</span>
          </app-ui-button>

          <app-ui-button
            variant="outline"
            data-testid="lang-toggle"
            [ariaLabel]="'NAV.TOGGLE_LANG_LABEL' | transloco"
            (clicked)="language.toggleLanguage()"
            >{{ 'NAV.TOGGLE_LANG' | transloco }}</app-ui-button
          >

          @if (isAuthenticated()) {
            <app-ui-button variant="secondary" data-testid="nav-logout" (clicked)="logout()">
              {{ 'NAV.LOGOUT' | transloco }}
            </app-ui-button>
          } @else {
            <a
              routerLink="/auth"
              class="inline-flex items-center justify-center rounded-lg bg-emerald-600 ps-4 pe-4 py-2 text-sm font-semibold text-white shadow-2xs transition hover:bg-emerald-700 dark:hover:bg-emerald-500"
              data-testid="nav-login"
              >{{ 'NAV.LOGIN' | transloco }}</a
            >
          }
        </div>
      </nav>
    </header>
  `,
})
export class NavbarComponent {
  protected readonly language = inject(LanguageService);
  protected readonly theme = inject(ThemeService);
  private readonly tokenStorage = inject(TokenStorageService);
  private readonly router = inject(Router);

  protected readonly isAuthenticated = this.tokenStorage.isAuthenticated;
  protected readonly currentRole = this.tokenStorage.currentRole;
  protected readonly currentUsername = this.tokenStorage.currentUsername;
  protected readonly canAccessViewings = computed(
    () =>
      this.tokenStorage.isAuthenticated() &&
      this.tokenStorage.hasRole('BUYER_TENANT', 'OWNER', 'AGENT'),
  );
  protected readonly canAccessMyProperties = computed(
    () =>
      this.tokenStorage.isAuthenticated() &&
      this.tokenStorage.hasRole('OWNER', 'AGENT'),
  );
  protected readonly isAdmin = computed(
    () => this.tokenStorage.isAuthenticated() && this.tokenStorage.hasRole('ADMIN'),
  );

  protected logout(): void {
    this.tokenStorage.clear();
    this.router.navigate(['/auth']);
  }
}
