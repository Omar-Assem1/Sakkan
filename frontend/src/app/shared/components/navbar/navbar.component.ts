import { ChangeDetectionStrategy, Component, computed, inject } from '@angular/core';
import { Router, RouterLink, RouterLinkActive } from '@angular/router';
import { TranslocoPipe } from '@jsverse/transloco';
import { LanguageService } from '../../../core/services/language.service';
import { TokenStorageService } from '../../../core/services/token-storage.service';
import { UiButtonComponent } from '../ui-button/ui-button.component';

@Component({
  selector: 'app-navbar',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [RouterLink, RouterLinkActive, TranslocoPipe, UiButtonComponent],
  template: `
    <header class="border-b border-slate-200 bg-white">
      <nav
        class="mx-auto flex max-w-7xl flex-wrap items-center gap-4 ps-4 pe-4 py-3"
        [attr.aria-label]="'NAV.MAIN' | transloco"
      >
        <a routerLink="/listings" class="me-4 text-xl font-bold text-emerald-700">
          {{ 'APP.TITLE' | transloco }}
        </a>

        <ul class="flex flex-wrap items-center gap-2 text-sm font-medium">
          <li>
            <a
              routerLink="/listings"
              routerLinkActive="bg-emerald-50 text-emerald-700"
              class="rounded-md ps-3 pe-3 py-2 text-slate-700 hover:bg-slate-100"
              data-testid="nav-listings"
            >{{ 'NAV.LISTINGS' | transloco }}</a>
          </li>
          <li>
            <a
              routerLink="/viewings"
              routerLinkActive="bg-emerald-50 text-emerald-700"
              class="rounded-md ps-3 pe-3 py-2 text-slate-700 hover:bg-slate-100"
              data-testid="nav-viewings"
            >{{ 'NAV.VIEWINGS' | transloco }}</a>
          </li>
          @if (isAdmin()) {
            <li>
              <a
                routerLink="/admin"
                routerLinkActive="bg-emerald-50 text-emerald-700"
                class="rounded-md ps-3 pe-3 py-2 text-slate-700 hover:bg-slate-100"
                data-testid="nav-admin"
              >{{ 'NAV.ADMIN' | transloco }}</a>
            </li>
          }
        </ul>

        <div class="ms-auto flex items-center gap-2">
          <app-ui-button
            variant="outline"
            data-testid="lang-toggle"
            [ariaLabel]="'NAV.TOGGLE_LANG_LABEL' | transloco"
            (clicked)="language.toggleLanguage()"
          >{{ 'NAV.TOGGLE_LANG' | transloco }}</app-ui-button>

          @if (isAuthenticated()) {
            <app-ui-button variant="secondary" data-testid="nav-logout" (clicked)="logout()">
              {{ 'NAV.LOGOUT' | transloco }}
            </app-ui-button>
          } @else {
            <a
              routerLink="/auth"
              class="rounded-lg bg-emerald-600 ps-4 pe-4 py-2 text-sm font-medium text-white hover:bg-emerald-700"
              data-testid="nav-login"
            >{{ 'NAV.LOGIN' | transloco }}</a>
          }
        </div>
      </nav>
    </header>
  `,
})
export class NavbarComponent {
  protected readonly language = inject(LanguageService);
  private readonly tokenStorage = inject(TokenStorageService);
  private readonly router = inject(Router);

  protected readonly isAuthenticated = this.tokenStorage.isAuthenticated;
  protected readonly isAdmin = computed(
    () => this.tokenStorage.isAuthenticated() && this.tokenStorage.currentRole() === 'ADMIN',
  );

  protected logout(): void {
    this.tokenStorage.clear();
    this.router.navigate(['/auth']);
  }
}
