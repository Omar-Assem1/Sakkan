import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { AuthService } from './auth.service';
import { TokenStorageService } from './token-storage.service';

function fakeJwt(roles: string[]): string {
  const payload = { sub: '1', username: 'omar', roles, exp: Math.floor(Date.now() / 1000) + 3600 };
  const b64 = btoa(JSON.stringify(payload)).replace(/\+/g, '-').replace(/\//g, '_').replace(/=+$/, '');
  return `h.${b64}.s`;
}

describe('AuthService', () => {
  let service: AuthService;
  let storage: TokenStorageService;
  let http: HttpTestingController;

  beforeEach(() => {
    localStorage.clear();
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(AuthService);
    storage = TestBed.inject(TokenStorageService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('login posts credentials, stores the token and the role from the token', () => {
    const token = fakeJwt(['OWNER']);
    service.login('omar', 'Passw0rd1').subscribe();

    const req = http.expectOne('/api/auth/login');
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({ username: 'omar', password: 'Passw0rd1' });
    req.flush({ token, tokenType: 'Bearer' });

    expect(storage.getToken()).toBe(token);
    expect(storage.getRole()).toBe('OWNER');
    expect(storage.isAuthenticated()).toBe(true);
  });

  it('login failure (401) stores nothing', () => {
    service.login('omar', 'bad').subscribe({ error: () => undefined });

    http.expectOne('/api/auth/login').flush({ status: 401 }, { status: 401, statusText: 'Unauthorized' });

    expect(storage.isAuthenticated()).toBe(false);
  });

  it('register posts the payload and does NOT log the user in', () => {
    const payload = {
      username: 'omar', email: 'omar@sakan.com', password: 'Passw0rd1',
      phone: '+201012345678', role: 'OWNER',
    };
    service.register(payload).subscribe();

    const req = http.expectOne('/api/auth/register');
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual(payload);
    req.flush({ id: 1, username: 'omar' }, { status: 201, statusText: 'Created' });

    expect(storage.isAuthenticated()).toBe(false);
  });

  it('logout clears token and role', () => {
    service.login('omar', 'Passw0rd1').subscribe();
    http.expectOne('/api/auth/login').flush({ token: fakeJwt(['AGENT']), tokenType: 'Bearer' });

    service.logout();

    expect(storage.getToken()).toBeNull();
    expect(storage.getRole()).toBeNull();
  });
});
