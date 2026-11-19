import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';
import { authInterceptor } from './auth.interceptor';
import { TokenStorageService } from '../services/token-storage.service';

interface NavigateCall {
  commands: unknown[];
  extras?: { queryParams?: Record<string, unknown> };
}

class FakeRouter {
  url = '/properties/mine';
  calls: NavigateCall[] = [];
  navigate(commands: unknown[], extras?: NavigateCall['extras']): Promise<boolean> {
    this.calls.push({ commands, extras });
    return Promise.resolve(true);
  }
}

describe('authInterceptor redirect rules (SCRUM-65)', () => {
  let http: HttpClient;
  let backend: HttpTestingController;
  let storage: TokenStorageService;
  let router: FakeRouter;

  beforeEach(() => {
    localStorage.clear();
    router = new FakeRouter();
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withInterceptors([authInterceptor])),
        provideHttpClientTesting(),
        TokenStorageService,
        { provide: Router, useValue: router },
      ],
    });
    http = TestBed.inject(HttpClient);
    backend = TestBed.inject(HttpTestingController);
    storage = TestBed.inject(TokenStorageService);
    storage.saveToken('t');
    storage.saveRoles(['OWNER']);
  });

  afterEach(() => {
    backend.verify();
    localStorage.clear();
  });

  function fail(url: string, status: number): void {
    http.get(url).subscribe({ error: () => undefined });
    backend.expectOne(url).flush({ status }, { status, statusText: 'err' });
  }

  it('401 on an API call clears the session and goes to sign-in with returnUrl', () => {
    fail('/api/properties/mine', 401);

    expect(storage.isAuthenticated()).toBe(false);
    expect(storage.getRoles()).toEqual([]);
    expect(router.calls.length).toBe(1);
    expect(router.calls[0].commands).toEqual(['/auth/sign-in']);
    expect(router.calls[0].extras?.queryParams?.['returnUrl']).toBe('/properties/mine');
  });

  it('403 on an API call goes to the 403 page and keeps the session', () => {
    fail('/api/properties/1', 403);

    expect(storage.isAuthenticated()).toBe(true);
    expect(router.calls.length).toBe(1);
    expect(router.calls[0].commands).toEqual(['/error']);
    expect(router.calls[0].extras?.queryParams?.['code']).toBe(403);
  });

  it('401 from the auth endpoints (wrong password) does not redirect or clear the session', () => {
    fail('/api/auth/login', 401);

    expect(storage.isAuthenticated()).toBe(true);
    expect(router.calls.length).toBe(0);
  });

  it('403 from a non-API URL is ignored', () => {
    fail('/assets/i18n/en.json', 403);

    expect(router.calls.length).toBe(0);
  });

  it('other errors (404, 500) do not redirect', () => {
    fail('/api/properties/999', 404);
    fail('/api/properties', 500);

    expect(router.calls.length).toBe(0);
    expect(storage.isAuthenticated()).toBe(true);
  });
});
