import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { TranslocoTestingModule } from '@jsverse/transloco';
import { AppComponent } from './app.component';
import { routes } from './app.routes';
import { AuthPageComponent } from './features/auth/auth-page.component';
import { AdminPageComponent } from './features/admin/admin-page.component';
import { ListingsPageComponent } from './features/listings/listings-page.component';
import { ViewingsPageComponent } from './features/viewings/viewings-page.component';
import enTranslations from '../../public/assets/i18n/en.json';
import arTranslations from '../../public/assets/i18n/ar.json';

function flattenKeys(obj: Record<string, unknown>, prefix = ''): string[] {
  return Object.entries(obj).flatMap(([key, val]) => {
    const path = prefix ? `${prefix}.${key}` : key;
    return val && typeof val === 'object' && !Array.isArray(val)
      ? flattenKeys(val as Record<string, unknown>, path)
      : [path];
  });
}

describe('AppComponent, Lazy Routes & RTL/i18n Audit (SCRUM-38)', () => {
  beforeEach(async () => {
    localStorage.clear();
    await TestBed.configureTestingModule({
      imports: [
        AppComponent,
        TranslocoTestingModule.forRoot({
          langs: {
            en: enTranslations,
            ar: arTranslations,
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

  it('should maintain 100% translation key parity between en.json and ar.json', () => {
    const enKeys = flattenKeys(enTranslations).sort();
    const arKeys = flattenKeys(arTranslations).sort();

    expect(enKeys.length).toBeGreaterThan(0);
    expect(arKeys).toEqual(enKeys);
  });

  it('should use only logical Tailwind classes and zero physical directional classes across shell and feature stubs', () => {
    const components = [
      AppComponent,
      AuthPageComponent,
      AdminPageComponent,
      ListingsPageComponent,
      ViewingsPageComponent,
    ];

    for (const cmp of components) {
      const fixture = TestBed.createComponent(cmp);
      fixture.detectChanges();
      const elements = fixture.nativeElement.querySelectorAll('*');
      elements.forEach((el: Element) => {
        expect(el.className).not.toMatch(
          /\b(ml-|mr-|pl-|pr-|text-left|text-right|left-|right-|border-l|border-r)/
        );
      });
    }
  });
});
