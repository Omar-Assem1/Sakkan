import { Injectable, computed, signal } from '@angular/core';

export type UserRole = 'ADMIN' | 'OWNER' | 'AGENT' | 'BUYER_TENANT' | 'MAINTENANCE_STAFF';

const TOKEN_KEY = 'sakan_token';
const ROLES_KEY = 'sakan_roles';
/** Key used before SCRUM-65 (single role). Still read so old sessions keep working. */
const LEGACY_ROLE_KEY = 'sakan_role';
const ROLE_PRIORITY = ['ADMIN', 'OWNER', 'AGENT', 'MAINTENANCE_STAFF', 'BUYER_TENANT'];

/**
 * Stateless JWT storage. The backend keeps no session; the client only holds
 * the token + roles and exposes them as Signals so the UI reacts instantly.
 * UX only: the backend still enforces @PreAuthorize on every endpoint.
 */
@Injectable({ providedIn: 'root' })
export class TokenStorageService {
  private readonly token = signal<string | null>(readStorage(TOKEN_KEY));
  private readonly roles = signal<string[]>(readRoles(this.token()));

  readonly isAuthenticated = computed(() => !!this.token());
  /** First role, kept for the navbar and other code written before multi-role support. */
  readonly currentRole = computed(() => this.roles()[0] ?? null);
  readonly currentUsername = computed(() => readJwtClaim(this.token(), 'username'));
  readonly currentUserId = computed(() => {
    const sub = readJwtClaim(this.token(), 'sub');
    const parsed = sub ? Number(sub) : NaN;
    return Number.isFinite(parsed) ? parsed : null;
  });

  getToken(): string | null {
    return this.token();
  }

  saveToken(token: string): void {
    writeStorage(TOKEN_KEY, token);
    this.token.set(token);
    if (this.roles().length === 0) {
      const jwtRoles = readJwtRoles(token);
      if (jwtRoles.length > 0) {
        this.saveRoles(jwtRoles);
      }
    }
  }

  getRole(): string | null {
    return this.roles()[0] ?? null;
  }

  getRoles(): string[] {
    return this.roles();
  }

  /** Single-role API kept for existing callers. */
  saveRole(role: UserRole | string): void {
    this.saveRoles([role]);
  }

  saveRoles(roles: readonly string[]): void {
    const copy = [...roles];
    writeStorage(ROLES_KEY, JSON.stringify(copy));
    removeStorage(LEGACY_ROLE_KEY);
    this.roles.set(copy);
  }

  hasRole(...wanted: string[]): boolean {
    return this.roles().some((r) => wanted.includes(r));
  }

  clear(): void {
    removeStorage(TOKEN_KEY);
    removeStorage(ROLES_KEY);
    removeStorage(LEGACY_ROLE_KEY);
    this.token.set(null);
    this.roles.set([]);
  }
}

function readRoles(token: string | null): string[] {
  const raw = readStorage(ROLES_KEY);
  if (raw !== null) {
    try {
      const parsed: unknown = JSON.parse(raw);
      return Array.isArray(parsed) && parsed.every((r) => typeof r === 'string') ? parsed : [];
    } catch {
      return [];
    }
  }
  const legacy = readStorage(LEGACY_ROLE_KEY);
  if (legacy) {
    return [legacy];
  }
  return readJwtRoles(token);
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

function parseJwtPayload(token: string | null): Record<string, unknown> | null {
  if (!token || !token.includes('.')) {
    return null;
  }
  try {
    const part = token.split('.')[1];
    if (!part) return null;
    const b64 = part.replace(/-/g, '+').replace(/_/g, '/');
    const padded = b64 + '='.repeat((4 - (b64.length % 4)) % 4);
    return JSON.parse(atob(padded)) as Record<string, unknown>;
  } catch {
    return null;
  }
}

function readJwtClaim(token: string | null, claim: string): string | null {
  const payload = parseJwtPayload(token);
  const val = payload?.[claim];
  return typeof val === 'string' && val.trim() !== '' ? val : null;
}

function readJwtRoles(token: string | null): string[] {
  const payload = parseJwtPayload(token);
  const roles = payload?.['roles'];
  if (!Array.isArray(roles)) {
    return [];
  }
  const rank = (r: string) => {
    const i = ROLE_PRIORITY.indexOf(r);
    return i === -1 ? ROLE_PRIORITY.length : i;
  };
  return roles.map(String).sort((a, b) => rank(a) - rank(b));
}
