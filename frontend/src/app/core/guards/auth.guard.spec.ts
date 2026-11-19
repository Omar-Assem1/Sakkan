import { TestBed } from '@angular/core/testing';
import { PartialMatchRouteSnapshot, Route, Router, UrlSegment, UrlTree, provideRouter } from '@angular/router';
import { authGuard } from './auth.guard';
import { TokenStorageService } from '../services/token-storage.service';

describe('authGuard (canMatch)', () => {
  let storage: TokenStorageService;
  let router: Router;

  const route = {} as Route;
  const segments = [new UrlSegment('viewings', {}), new UrlSegment('request', {})];
  const snapshot = {} as PartialMatchRouteSnapshot;

  const run = () =>
    TestBed.runInInjectionContext(() => authGuard(route, segments, snapshot)) as boolean | UrlTree;

  beforeEach(() => {
    localStorage.clear();
    TestBed.configureTestingModule({
      providers: [TokenStorageService, provideRouter([])],
    });
    storage = TestBed.inject(TokenStorageService);
    router = TestBed.inject(Router);
  });

  afterEach(() => localStorage.clear());

  it('allows any authenticated user, whatever the role', () => {
    storage.saveToken('valid-token');
    expect(run()).toBe(true);
  });

  it('redirects to sign-in with the requested URL as returnUrl when not authenticated', () => {
    const result = run();
    expect(result instanceof UrlTree).toBe(true);
    expect(router.serializeUrl(result as UrlTree)).toBe(
      '/auth/sign-in?returnUrl=%2Fviewings%2Frequest',
    );
  });

  it('redirects to sign-in after the session was cleared', () => {
    storage.saveToken('valid-token');
    storage.clear();
    expect(run() instanceof UrlTree).toBe(true);
  });
});
