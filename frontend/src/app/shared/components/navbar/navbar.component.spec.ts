import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { TranslocoTestingModule } from '@jsverse/transloco';
import { NavbarComponent } from './navbar.component';
import { LanguageService } from '../../../core/services/language.service';
import { ThemeService } from '../../../core/services/theme.service';
import { TokenStorageService } from '../../../core/services/token-storage.service';

describe('NavbarComponent', () => {
  let component: NavbarComponent;
  let fixture: ComponentFixture<NavbarComponent>;
  let languageService: LanguageService;
  let themeService: ThemeService;
  let tokenStorage: TokenStorageService;

  beforeEach(async () => {
    localStorage.clear();
    await TestBed.configureTestingModule({
      imports: [
        NavbarComponent,
        TranslocoTestingModule.forRoot({
          langs: {
            en: {
              APP: { TITLE: 'Sakan' },
              NAV: {
                LISTINGS: 'Listings',
                VIEWINGS: 'Viewings',
                MY_PROPERTIES: 'My Properties',
                ADMIN: 'Admin',
                LOGIN: 'Sign In',
                LOGOUT: 'Sign Out',
                TOGGLE_LANG: 'العربية',
                TOGGLE_THEME_DARK: 'Dark',
                TOGGLE_THEME_LIGHT: 'Light',
                TOGGLE_THEME_LABEL: 'Toggle color theme',
              },
            },
            ar: {
              APP: { TITLE: 'سكن' },
              NAV: {
                LISTINGS: 'العقارات',
                VIEWINGS: 'المعاينات',
                MY_PROPERTIES: 'عقاراتي',
                ADMIN: 'الإدارة',
                LOGIN: 'تسجيل الدخول',
                LOGOUT: 'تسجيل الخروج',
                TOGGLE_LANG: 'English',
                TOGGLE_THEME_DARK: 'داكن',
                TOGGLE_THEME_LIGHT: 'فاتح',
                TOGGLE_THEME_LABEL: 'تبديل وضع الألوان',
              },
            },
          },
          translocoConfig: {
            availableLangs: ['en', 'ar'],
            defaultLang: 'en',
          },
        }),
      ],
      providers: [provideRouter([]), LanguageService, ThemeService, TokenStorageService],
    }).compileComponents();

    languageService = TestBed.inject(LanguageService);
    themeService = TestBed.inject(ThemeService);
    tokenStorage = TestBed.inject(TokenStorageService);
    fixture = TestBed.createComponent(NavbarComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  afterEach(() => {
    localStorage.clear();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should render listings link by default and show viewings link only for BUYER_TENANT, OWNER, or AGENT', () => {
    expect(fixture.nativeElement.innerHTML).toContain('/listings');
    expect(fixture.nativeElement.querySelector('[data-testid="nav-viewings"]')).toBeNull();

    tokenStorage.saveToken('admin-jwt');
    tokenStorage.saveRole('ADMIN');
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('[data-testid="nav-viewings"]')).toBeNull();

    tokenStorage.saveRole('MAINTENANCE_STAFF');
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('[data-testid="nav-viewings"]')).toBeNull();

    tokenStorage.saveRole('BUYER_TENANT');
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('[data-testid="nav-viewings"]')).toBeTruthy();

    tokenStorage.saveRole('OWNER');
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('[data-testid="nav-viewings"]')).toBeTruthy();
  });

  it('should render my properties link only when authenticated as OWNER or AGENT and hide it for BUYER_TENANT, ADMIN, or users without those roles', () => {
    expect(fixture.nativeElement.querySelector('[data-testid="nav-my-properties"]')).toBeNull();

    tokenStorage.saveToken('user-jwt');
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('[data-testid="nav-my-properties"]')).toBeNull();

    tokenStorage.saveRole('BUYER_TENANT');
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('[data-testid="nav-my-properties"]')).toBeNull();

    tokenStorage.saveRole('ADMIN');
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('[data-testid="nav-my-properties"]')).toBeNull();

    tokenStorage.saveRole('OWNER');
    fixture.detectChanges();
    const navProperties = fixture.nativeElement.querySelector('[data-testid="nav-my-properties"]');
    expect(navProperties).toBeTruthy();
    expect(navProperties.getAttribute('href')).toBe('/properties/mine');

    tokenStorage.saveRole('AGENT');
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('[data-testid="nav-my-properties"]')).toBeTruthy();
  });

  it('should hide admin link for non-admin users and display it when role is ADMIN', () => {
    expect(fixture.nativeElement.querySelector('[data-testid="nav-admin"]')).toBeNull();

    tokenStorage.saveToken('admin-jwt');
    tokenStorage.saveRole('ADMIN');
    fixture.detectChanges();

    expect(fixture.nativeElement.querySelector('[data-testid="nav-admin"]')).toBeTruthy();
  });

  it('should toggle language when the language switch button is clicked', () => {
    const toggleSpy = vi.spyOn(languageService, 'toggleLanguage');
    const langBtn = fixture.nativeElement.querySelector(
      '[data-testid="lang-toggle"] button',
    ) as HTMLButtonElement;

    expect(langBtn).toBeTruthy();
    langBtn.click();

    expect(toggleSpy).toHaveBeenCalledTimes(1);
    expect(languageService.currentLang()).toBe('ar');
  });

  it('should toggle dark mode when the theme switch button is clicked', () => {
    const toggleSpy = vi.spyOn(themeService, 'toggleTheme');
    const themeBtn = fixture.nativeElement.querySelector(
      '[data-testid="theme-toggle"] button',
    ) as HTMLButtonElement;

    expect(themeBtn).toBeTruthy();
    themeBtn.click();

    expect(toggleSpy).toHaveBeenCalledTimes(1);
    expect(themeService.isDark()).toBe(true);
  });

  it('should contain zero physical directional Tailwind classes in navbar template', () => {
    const allElements = fixture.nativeElement.querySelectorAll('*');
    allElements.forEach((el: Element) => {
      expect(el.className).not.toMatch(
        /\b(ml-|mr-|pl-|pr-|text-left|text-right|left-|right-|border-l|border-r)/,
      );
    });
  });
});
