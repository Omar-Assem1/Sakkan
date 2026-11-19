import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { AdminUserService } from './admin-user.service';

describe('AdminUserService (SCRUM-69)', () => {
  let service: AdminUserService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
    service = TestBed.inject(AdminUserService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('GETs /api/admin/users with page, size and every filter', () => {
    service.list({ q: 'ali', role: 'OWNER', enabled: true }, 2, 10).subscribe();

    const req = http.expectOne((r) => r.url === '/api/admin/users');
    expect(req.request.method).toBe('GET');
    expect(req.request.params.get('page')).toBe('2');
    expect(req.request.params.get('size')).toBe('10');
    expect(req.request.params.get('q')).toBe('ali');
    expect(req.request.params.get('role')).toBe('OWNER');
    expect(req.request.params.get('enabled')).toBe('true');
    req.flush({ content: [], page: 2, size: 10, totalElements: 0, totalPages: 0 });
  });

  it('omits empty filters from the query string', () => {
    service.list({ q: '  ', role: null, enabled: null }, 0, 10).subscribe();

    const req = http.expectOne((r) => r.url === '/api/admin/users');
    expect(req.request.params.has('q')).toBe(false);
    expect(req.request.params.has('role')).toBe(false);
    expect(req.request.params.has('enabled')).toBe(false);
    req.flush({ content: [], page: 0, size: 10, totalElements: 0, totalPages: 0 });
  });

  it('sends enabled=false (a falsy value must not be dropped)', () => {
    service.list({ enabled: false }, 0, 10).subscribe();

    const req = http.expectOne((r) => r.url === '/api/admin/users');
    expect(req.request.params.get('enabled')).toBe('false');
    req.flush({ content: [], page: 0, size: 10, totalElements: 0, totalPages: 0 });
  });

  it('grantRole POSTs { role } to /api/admin/users/{id}/roles', () => {
    service.grantRole(5, 'ADMIN').subscribe();

    const req = http.expectOne('/api/admin/users/5/roles');
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({ role: 'ADMIN' });
    req.flush({});
  });

  it('revokeRole DELETEs /api/admin/users/{id}/roles/{role}', () => {
    service.revokeRole(5, 'ADMIN').subscribe();

    const req = http.expectOne('/api/admin/users/5/roles/ADMIN');
    expect(req.request.method).toBe('DELETE');
    req.flush({});
  });
});
