import { TestBed } from '@angular/core/testing';
import { TokenStorageService } from './token-storage.service';

describe('TokenStorageService', () => {
  let service: TokenStorageService;

  beforeEach(() => {
    localStorage.clear();
    TestBed.configureTestingModule({
      providers: [TokenStorageService],
    });
    service = TestBed.inject(TokenStorageService);
  });

  afterEach(() => {
    vi.restoreAllMocks();
    localStorage.clear();
  });

  it('should return null when no token is stored', () => {
    expect(service.getToken()).toBeNull();
    expect(service.isAuthenticated()).toBe(false);
  });

  it('should save and retrieve JWT token and update authenticated signal', () => {
    service.saveToken('eyJhbGciOiJIUzI1NiJ9.test.signature');
    expect(service.getToken()).toBe('eyJhbGciOiJIUzI1NiJ9.test.signature');
    expect(service.isAuthenticated()).toBe(true);
  });

  it('should save and retrieve user role and check hasRole()', () => {
    expect(service.getRole()).toBeNull();
    expect(service.hasRole('ADMIN')).toBe(false);

    service.saveRole('ADMIN');
    expect(service.getRole()).toBe('ADMIN');
    expect(service.hasRole('ADMIN', 'OWNER')).toBe(true);
    expect(service.hasRole('BUYER_TENANT')).toBe(false);
  });

  it('should clear both token and role on clear()', () => {
    service.saveToken('jwt-token');
    service.saveRole('BUYER_TENANT');
    service.clear();

    expect(service.getToken()).toBeNull();
    expect(service.getRole()).toBeNull();
    expect(service.isAuthenticated()).toBe(false);
  });

  it('should treat whitespace-only token in localStorage as unauthenticated on boot', () => {
    localStorage.setItem('sakan_token', '   ');
    TestBed.resetTestingModule();
    TestBed.configureTestingModule({ providers: [TokenStorageService] });
    const freshService = TestBed.inject(TokenStorageService);

    expect(freshService.getToken()).toBeNull();
    expect(freshService.isAuthenticated()).toBe(false);
  });

  it('should fall back gracefully without crashing when localStorage throws SecurityError', () => {
    vi.spyOn(Storage.prototype, 'getItem').mockImplementation(() => {
      throw new DOMException('Blocked', 'SecurityError');
    });
    vi.spyOn(Storage.prototype, 'setItem').mockImplementation(() => {
      throw new DOMException('QuotaExceeded', 'QuotaExceededError');
    });

    TestBed.resetTestingModule();
    TestBed.configureTestingModule({ providers: [TokenStorageService] });
    const safeService = TestBed.inject(TokenStorageService);

    expect(safeService.getToken()).toBeNull();
    expect(() => safeService.saveToken('in-memory-jwt')).not.toThrow();
    expect(safeService.getToken()).toBe('in-memory-jwt');
    expect(safeService.isAuthenticated()).toBe(true);
  });
});
