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

export interface UserSummary {
  id: number;
  username: string;
  email: string;
  phone: string;
  roles: string[];
}

const ROLE_PRIORITY = ['ADMIN', 'OWNER', 'AGENT', 'BUYER_TENANT'];

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

  private storeSession(res: LoginResponse): void {
    this.storage.saveToken(res.token);
    const role = pickRole(rolesFromToken(res.token));
    if (role) {
      this.storage.saveRole(role);
    }
  }

  /** Registration does not log the user in: they sign in afterwards. */
  register(payload: RegisterPayload): Observable<UserSummary> {
    return this.http.post<UserSummary>('/api/auth/register', payload);
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

function pickRole(roles: string[]): string | null {
  return ROLE_PRIORITY.find((r) => roles.includes(r)) ?? roles[0] ?? null;
}
