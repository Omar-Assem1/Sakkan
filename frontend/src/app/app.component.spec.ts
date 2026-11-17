import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { TranslocoTestingModule } from '@jsverse/transloco';
import { AppComponent } from './app.component';
import { routes } from './app.routes';

describe('AppComponent & Lazy Routes (SCRUM-38)', () => {
  beforeEach(async () => {
    localStorage.clear();
    await TestBed.configureTestingModule({
      imports: [
        AppComponent,
        TranslocoTestingModule.forRoot({
          langs: {
            en: { APP: { TITLE: 'Sakan' }, NAV: { LISTINGS: 'Listings', VIEWINGS: 'Viewings', ADMIN: 'Admin', LOGIN: 'Sign In', LOGOUT: 'Sign Out', TOGGLE_LANG: 'العربية' } },
            ar: { APP: { TITLE: 'سكن' }, NAV: { LISTINGS: 'العقارات', VIEWINGS: 'المعاينات', ADMIN: 'الإدارة', LOGIN: 'تسجيل الدخول', LOGOUT: 'تسجيل الخروج', TOGGLE_LANG: 'English' } },
          },
          translocoConfig: { availableLangs: ['en', 'ar'], defaultLang: 'en' },
        }),
      ],
      providers: [provideRouter(routes)],
    }).compileComponents();
  });

  afterEach(() => {
    localStorage.clear();
  });

  it('should create the app shell and render navbar and router-outlet', () => {
    const fixture = TestBed.createComponent(AppComponent);
    fixture.detectChanges();
    const compiled = fixture.nativeElement as HTMLElement;

    expect(compiled.querySelector('app-navbar')).toBeTruthy();
    expect(compiled.querySelector('router-outlet')).toBeTruthy();
  });

  it('should resolve all lazy feature route definitions (auth, admin, listings, viewings) without errors', async () => {
    const expectedPaths = ['auth', 'admin', 'listings', 'viewings'];

    for (const path of expectedPaths) {
      const routeDef = routes.find((r) => r.path === path);
      expect(routeDef).toBeDefined();
      expect(routeDef?.loadChildren || routeDef?.loadComponent).toBeDefined();

      if (routeDef?.loadChildren) {
        const loaded = await (routeDef.loadChildren() as Promise<unknown>);
        expect(loaded).toBeTruthy();
      } else if (routeDef?.loadComponent) {
        const loaded = await (routeDef.loadComponent() as Promise<unknown>);
        expect(loaded).toBeTruthy();
      }
    }
  });
});
