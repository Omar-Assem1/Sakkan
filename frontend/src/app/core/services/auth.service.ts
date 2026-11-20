import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, tap } from 'rxjs';
import { TokenStorageService } from './token-storage.service';

export interface RegisterPayload {
  username: string;
  email: string;
  password: string;
  phone: string;
  role: string;
}

export interface LoginResponse {
  token: string;
  tokenType: string;
}

export interface GoogleExchangeResponse {
  registrationRequired: boolean;
  email?: string;
  token?: string;
  tokenType?: string;
}

export interface UserSummary {
  id: number;
  username: string;
  email: string;
  phone: string;
  roles: string[];
}

const ROLE_PRIORITY = ['ADMIN', 'OWNER', 'AGENT', 'MAINTENANCE_STAFF', 'BUYER_TENANT'];
@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly storage = inject(TokenStorageService);

  login(username: string, password: string): Observable<LoginResponse> {
    return this.http
      .post<LoginResponse>('/api/auth/login', { username, password })
      .pipe(tap((res) => this.storeSession(res)));
  }

  /** Admin sign-in (form 2). The server checks the ADMIN role; the UI only reads it from the token. */
  adminLogin(username: string, password: string): Observable<LoginResponse> {
    return this.http
      .post<LoginResponse>('/api/auth/admin/login', { username, password })
      .pipe(tap((res) => this.storeSession(res)));
  }

  exchangeGoogleCode(code: string): Observable<GoogleExchangeResponse> {
    return this.http.post<GoogleExchangeResponse>('/api/auth/google/exchange', { code }).pipe(
      tap((res) => {
        if (res.token && res.tokenType) this.storeSession({ token: res.token, tokenType: res.tokenType });
      }),
    );
  }

  completeGoogleRegistration(code: string, username: string): Observable<LoginResponse> {
    return this.http
      .post<LoginResponse>('/api/auth/google/complete', { code, username })
      .pipe(tap((res) => this.storeSession(res)));
  }

  private storeSession(res: LoginResponse): void {
    const roles = sortByPriority(rolesFromToken(res.token));
    this.storage.saveRoles(roles);
    this.storage.saveToken(res.token);
  }

  /** Registration does not log the user in: they sign in afterwards. */
  register(payload: RegisterPayload): Observable<UserSummary> {
    return this.http.post<UserSummary>('/api/auth/register', payload);
  }

  /** Fetches the current authenticated user's profile from GET /api/auth/me and refreshes roles. */
  me(): Observable<UserSummary> {
    return this.http.get<UserSummary>('/api/auth/me').pipe(
      tap((user) => {
        if (Array.isArray(user.roles) && user.roles.length > 0) {
          this.storage.saveRoles(sortByPriority(user.roles));
        }
      }),
    );
  }

  logout(): void {
    this.storage.clear();
  }
}

/** Reads the roles claim for the UI only. The server stays the authority on every request. */
function rolesFromToken(token: string): string[] {
  try {
    const b64 = token.split('.')[1].replace(/-/g, '+').replace(/_/g, '/');
    const padded = b64 + '='.repeat((4 - (b64.length % 4)) % 4);
    const roles = JSON.parse(atob(padded)).roles;
    return Array.isArray(roles) ? roles.map(String) : [];
  } catch {
    return [];
  }
}
/** Highest-priority role first, so the navbar (which shows the first role) behaves as before. */
function sortByPriority(roles: string[]): string[] {
  const rank = (r: string) => {
    const i = ROLE_PRIORITY.indexOf(r);
    return i === -1 ? ROLE_PRIORITY.length : i;
  };
  return [...roles].sort((a, b) => rank(a) - rank(b));
}
