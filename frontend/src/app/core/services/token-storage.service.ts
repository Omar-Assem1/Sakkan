import { Injectable, computed, signal } from '@angular/core';

export type UserRole = 'ADMIN' | 'OWNER' | 'AGENT' | 'BUYER_TENANT';

const TOKEN_KEY = 'sakan_token';
const ROLE_KEY = 'sakan_role';

/**
 * Stateless JWT storage. The backend keeps no session; the client only holds
 * the token + role and exposes them as Signals so the UI reacts instantly.
 */
@Injectable({ providedIn: 'root' })
export class TokenStorageService {
  private readonly token = signal<string | null>(readStorage(TOKEN_KEY));
  private readonly role = signal<string | null>(readStorage(ROLE_KEY));

  readonly isAuthenticated = computed(() => !!this.token());
  readonly currentRole = this.role.asReadonly();

  getToken(): string | null {
    return this.token();
  }

  saveToken(token: string): void {
    writeStorage(TOKEN_KEY, token);
    this.token.set(token);
  }

  getRole(): string | null {
    return this.role();
  }

  saveRole(role: UserRole | string): void {
    writeStorage(ROLE_KEY, role);
    this.role.set(role);
  }

  hasRole(...roles: string[]): boolean {
    const current = this.role();
    return current !== null && roles.includes(current);
  }

  clear(): void {
    removeStorage(TOKEN_KEY);
    removeStorage(ROLE_KEY);
    this.token.set(null);
    this.role.set(null);
  }
}

/** localStorage can throw (private mode, quota, disabled storage) – never crash the app. */
function readStorage(key: string): string | null {
  try {
    const value = localStorage.getItem(key);
    return value && value.trim() !== '' ? value : null;
  } catch {
    return null;
  }
}

function writeStorage(key: string, value: string): void {
  try {
    localStorage.setItem(key, value);
  } catch {
    /* storage unavailable – in-memory signal still works for this session */
  }
}

function removeStorage(key: string): void {
  try {
    localStorage.removeItem(key);
  } catch {
    /* ignore */
  }
}
