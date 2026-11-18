import { TestBed } from '@angular/core/testing';
import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { Router } from '@angular/router';
import { authInterceptor } from './auth.interceptor';
import { TokenStorageService } from '../services/token-storage.service';

describe('authInterceptor', () => {
  let http: HttpClient;
  let httpMock: HttpTestingController;
  let tokenStorage: TokenStorageService;
  let routerSpy: { navigate: ReturnType<typeof vi.fn> };

  beforeEach(() => {
    localStorage.clear();
    routerSpy = { navigate: vi.fn() };

    TestBed.configureTestingModule({
      providers: [
        TokenStorageService,
        { provide: Router, useValue: routerSpy },
        provideHttpClient(withInterceptors([authInterceptor])),
        provideHttpClientTesting(),
      ],
    });

    http = TestBed.inject(HttpClient);
    httpMock = TestBed.inject(HttpTestingController);
    tokenStorage = TestBed.inject(TokenStorageService);
  });

  afterEach(() => {
    httpMock.verify();
    localStorage.clear();
  });

  it('should attach Authorization: Bearer <token> header when token exists on /api/ requests', () => {
    tokenStorage.saveToken('mock-jwt-token-123');

    http.get('/api/v1/viewings/my').subscribe();

    const req = httpMock.expectOne('/api/v1/viewings/my');
    expect(req.request.headers.has('Authorization')).toBe(true);
    expect(req.request.headers.get('Authorization')).toBe('Bearer mock-jwt-token-123');
    req.flush({ content: [] });
  });
  it('does not clear the token or redirect when /api/auth/login itself returns 401', () => {
    tokenStorage.saveToken('existing-token');

    http.post('/api/auth/login', {}).subscribe({ error: () => undefined });
    httpMock.expectOne('/api/auth/login').flush({}, { status: 401, statusText: 'Unauthorized' });

    expect(routerSpy.navigate).not.toHaveBeenCalled();
    expect(tokenStorage.getToken()).toBe('existing-token');
  });
  it('should omit Authorization header when no token exists', () => {
    tokenStorage.clear();

    http.get('/api/v1/properties').subscribe();

    const req = httpMock.expectOne('/api/v1/properties');
    expect(req.request.headers.has('Authorization')).toBe(false);
    req.flush({ content: [] });
  });

  it('should NOT leak Authorization header to non-API asset or external URLs even when token exists', () => {
    tokenStorage.saveToken('secret-jwt-token');

    http.get('/assets/i18n/en.json').subscribe();
    http.get('https://example.com/public-config').subscribe();

    const assetReq = httpMock.expectOne('/assets/i18n/en.json');
    expect(assetReq.request.headers.has('Authorization')).toBe(false);
    assetReq.flush({});

    const externalReq = httpMock.expectOne('https://example.com/public-config');
    expect(externalReq.request.headers.has('Authorization')).toBe(false);
    externalReq.flush({});
  });

  it('should clear stored token and redirect to /auth on 401 Unauthorized response', () => {
    tokenStorage.saveToken('expired-jwt-token');
    tokenStorage.saveRole('BUYER_TENANT');

    let errorStatus = 0;
    http.get('/api/v1/viewings/my').subscribe({
      error: (err) => {
        errorStatus = err.status;
      },
    });

    const req = httpMock.expectOne('/api/v1/viewings/my');
    req.flush({ title: 'Unauthorized', status: 401 }, { status: 401, statusText: 'Unauthorized' });

    expect(errorStatus).toBe(401);
    expect(tokenStorage.getToken()).toBeNull();
    expect(tokenStorage.getRole()).toBeNull();
    expect(routerSpy.navigate).toHaveBeenCalledWith(['/auth']);
  });

  it('should preserve token and NOT redirect to /auth on 403 Forbidden or 500 Server Error', () => {
    tokenStorage.saveToken('valid-jwt-token');
    tokenStorage.saveRole('BUYER_TENANT');

    let errorStatus = 0;
    http.post('/api/v1/properties/1/slots', {}).subscribe({
      error: (err) => {
        errorStatus = err.status;
      },
    });

    const req = httpMock.expectOne('/api/v1/properties/1/slots');
    req.flush({ title: 'Forbidden', status: 403 }, { status: 403, statusText: 'Forbidden' });

    expect(errorStatus).toBe(403);
    expect(tokenStorage.getToken()).toBe('valid-jwt-token');
    expect(tokenStorage.getRole()).toBe('BUYER_TENANT');
    expect(routerSpy.navigate).not.toHaveBeenCalled();
  });
});
