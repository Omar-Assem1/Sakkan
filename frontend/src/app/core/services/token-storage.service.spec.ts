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

  it('should save and retrieve user role', () => {
    expect(service.getRole()).toBeNull();
    service.saveRole('ADMIN');
    expect(service.getRole()).toBe('ADMIN');
  });

  it('should clear both token and role on clear()', () => {
    service.saveToken('jwt-token');
    service.saveRole('BUYER_TENANT');
    service.clear();

    expect(service.getToken()).toBeNull();
    expect(service.getRole()).toBeNull();
    expect(service.isAuthenticated()).toBe(false);
  });
});
