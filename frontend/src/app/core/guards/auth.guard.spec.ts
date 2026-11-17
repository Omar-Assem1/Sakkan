import { TestBed } from '@angular/core/testing';
import { ActivatedRouteSnapshot, Router, RouterStateSnapshot, UrlTree, provideRouter } from '@angular/router';
import { authGuard } from './auth.guard';
import { TokenStorageService } from '../services/token-storage.service';

describe('authGuard', () => {
  let tokenStorage: TokenStorageService;
  let router: Router;

  const mockState = { url: '/viewings' } as RouterStateSnapshot;

  beforeEach(() => {
    localStorage.clear();
    TestBed.configureTestingModule({
      providers: [TokenStorageService, provideRouter([])],
    });
    tokenStorage = TestBed.inject(TokenStorageService);
    router = TestBed.inject(Router);
  });

  afterEach(() => {
    localStorage.clear();
  });

  it('should allow access when user is authenticated and no specific role is required', () => {
    tokenStorage.saveToken('valid-token');
    const route = { data: {} } as unknown as ActivatedRouteSnapshot;

    const result = TestBed.runInInjectionContext(() => authGuard(route, mockState));
    expect(result).toBe(true);
  });

  it('should block access and redirect to /auth when user is not authenticated', () => {
    tokenStorage.clear();
    const route = { data: {} } as unknown as ActivatedRouteSnapshot;

    const result = TestBed.runInInjectionContext(() => authGuard(route, mockState));
    expect(result instanceof UrlTree).toBe(true);
    expect(router.serializeUrl(result as UrlTree)).toBe('/auth');
  });

  it('should allow access when user has one of the required route roles', () => {
    tokenStorage.saveToken('valid-admin-token');
    tokenStorage.saveRole('ADMIN');
    const route = { data: { roles: ['ADMIN'] } } as unknown as ActivatedRouteSnapshot;

    const result = TestBed.runInInjectionContext(() => authGuard(route, mockState));
    expect(result).toBe(true);
  });

  it('should redirect to /listings when user is authenticated but lacks required role', () => {
    tokenStorage.saveToken('valid-buyer-token');
    tokenStorage.saveRole('BUYER_TENANT');
    const route = { data: { roles: ['ADMIN'] } } as unknown as ActivatedRouteSnapshot;

    const result = TestBed.runInInjectionContext(() => authGuard(route, mockState));
    expect(result instanceof UrlTree).toBe(true);
    expect(router.serializeUrl(result as UrlTree)).toBe('/listings');
  });

  it('should redirect to /listings when user has a token but no stored role on a role-protected route', () => {
    tokenStorage.saveToken('token-without-role');
    const route = { data: { roles: ['ADMIN'] } } as unknown as ActivatedRouteSnapshot;

    const result = TestBed.runInInjectionContext(() => authGuard(route, mockState));
    expect(result instanceof UrlTree).toBe(true);
    expect(router.serializeUrl(result as UrlTree)).toBe('/listings');
  });
});
