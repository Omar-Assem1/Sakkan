import { TestBed } from '@angular/core/testing';
import { TokenStorageService } from './token-storage.service';

describe('TokenStorageService roles (SCRUM-65)', () => {
  function fresh(): TokenStorageService {
    TestBed.resetTestingModule();
    TestBed.configureTestingModule({ providers: [TokenStorageService] });
    return TestBed.inject(TokenStorageService);
  }

  beforeEach(() => localStorage.clear());
  afterEach(() => localStorage.clear());

  it('stores every role of the user', () => {
    const storage = fresh();
    storage.saveRoles(['BUYER_TENANT', 'OWNER']);
    expect(storage.getRoles()).toEqual(['BUYER_TENANT', 'OWNER']);
  });

  it('hasRole matches any held role', () => {
    const storage = fresh();
    storage.saveRoles(['BUYER_TENANT', 'OWNER']);
    expect(storage.hasRole('OWNER')).toBe(true);
    expect(storage.hasRole('ADMIN', 'BUYER_TENANT')).toBe(true);
    expect(storage.hasRole('ADMIN')).toBe(false);
  });

  it('accepts MAINTENANCE_STAFF', () => {
    const storage = fresh();
    storage.saveRoles(['MAINTENANCE_STAFF']);
    expect(storage.hasRole('MAINTENANCE_STAFF')).toBe(true);
  });

  it('restores roles from localStorage on a new instance', () => {
    localStorage.setItem('sakan_roles', JSON.stringify(['AGENT', 'BUYER_TENANT']));
    const storage = fresh();
    expect(storage.getRoles()).toEqual(['AGENT', 'BUYER_TENANT']);
  });

  it('falls back to the legacy single role key for sessions created before this change', () => {
    localStorage.setItem('sakan_role', 'OWNER');
    const storage = fresh();
    expect(storage.getRoles()).toEqual(['OWNER']);
    expect(storage.hasRole('OWNER')).toBe(true);
  });

  it('ignores malformed stored roles instead of crashing', () => {
    localStorage.setItem('sakan_roles', '{not json');
    const storage = fresh();
    expect(storage.getRoles()).toEqual([]);
    expect(storage.hasRole('OWNER')).toBe(false);
  });

  it('saveRole (single role, existing API) still works', () => {
    const storage = fresh();
    storage.saveRole('ADMIN');
    expect(storage.hasRole('ADMIN')).toBe(true);
    expect(storage.getRoles()).toEqual(['ADMIN']);
  });

  it('clear removes all roles and the token', () => {
    const storage = fresh();
    storage.saveToken('t');
    storage.saveRoles(['OWNER', 'AGENT']);
    storage.clear();
    expect(storage.getRoles()).toEqual([]);
    expect(storage.hasRole('OWNER')).toBe(false);
    expect(storage.isAuthenticated()).toBe(false);
    expect(localStorage.getItem('sakan_roles')).toBeNull();
  });
});
