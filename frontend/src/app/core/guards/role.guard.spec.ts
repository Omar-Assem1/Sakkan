import { TestBed } from '@angular/core/testing';
import { PartialMatchRouteSnapshot, Route, Router, UrlSegment, UrlTree, provideRouter } from '@angular/router';
import { roleGuard } from './role.guard';
import { TokenStorageService } from '../services/token-storage.service';

describe('roleGuard (canMatch)', () => {
  let storage: TokenStorageService;
  let router: Router;

  const route = {} as Route;
  const segments = [new UrlSegment('properties', {}), new UrlSegment('new', {})];
  const snapshot = {} as PartialMatchRouteSnapshot;

  const run = (...roles: string[]) =>
    TestBed.runInInjectionContext(() => roleGuard(...roles)(route, segments, snapshot)) as boolean | UrlTree;

  const url = (result: boolean | UrlTree) => router.serializeUrl(result as UrlTree);

  beforeEach(() => {
    localStorage.clear();
    TestBed.configureTestingModule({
      providers: [TokenStorageService, provideRouter([])],
    });
    storage = TestBed.inject(TokenStorageService);
    router = TestBed.inject(Router);
  });

  afterEach(() => localStorage.clear());

  it('redirects to sign-in with returnUrl when there is no token', () => {
    const result = run('OWNER', 'AGENT');
    expect(result instanceof UrlTree).toBe(true);
    expect(url(result)).toBe('/auth/sign-in?returnUrl=%2Fproperties%2Fnew');
  });

  it('allows a user holding one of the required roles', () => {
    storage.saveToken('t');
    storage.saveRoles(['OWNER']);
    expect(run('OWNER', 'AGENT')).toBe(true);
  });

  it('redirects to the 403 page when the role is not allowed', () => {
    storage.saveToken('t');
    storage.saveRoles(['BUYER_TENANT']);
    const result = run('OWNER', 'AGENT');
    expect(result instanceof UrlTree).toBe(true);
    expect(url(result)).toBe('/error?code=403');
  });

  it('redirects to the 403 page when the token has no roles', () => {
    storage.saveToken('t');
    const result = run('OWNER');
    expect(url(result)).toBe('/error?code=403');
  });

  it('allows a user with several roles when any one of them matches', () => {
    storage.saveToken('t');
    storage.saveRoles(['BUYER_TENANT', 'OWNER']);
    expect(run('OWNER')).toBe(true);
    expect(run('BUYER_TENANT')).toBe(true);
  });

  it('blocks a user with several roles when none matches', () => {
    storage.saveToken('t');
    storage.saveRoles(['BUYER_TENANT', 'OWNER']);
    expect(url(run('ADMIN'))).toBe('/error?code=403');
  });

  it('keeps ADMIN out of OWNER/AGENT routes', () => {
    storage.saveToken('t');
    storage.saveRoles(['ADMIN']);
    expect(url(run('OWNER', 'AGENT'))).toBe('/error?code=403');
  });

  it('allows MAINTENANCE_STAFF only where that role is listed', () => {
    storage.saveToken('t');
    storage.saveRoles(['MAINTENANCE_STAFF']);
    expect(run('MAINTENANCE_STAFF')).toBe(true);
    expect(url(run('OWNER'))).toBe('/error?code=403');
  });

  it('treats a cleared session as unauthenticated', () => {
    storage.saveToken('t');
    storage.saveRoles(['OWNER']);
    storage.clear();
    expect(url(run('OWNER'))).toBe('/auth/sign-in?returnUrl=%2Fproperties%2Fnew');
  });
});
