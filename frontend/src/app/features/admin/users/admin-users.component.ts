import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { TranslocoDirective } from '@jsverse/transloco';
import { AdminRole, AdminUserResponse } from '../models/admin-user.models';
import { AdminUserFilters, AdminUserService } from '../services/admin-user.service';

const ROLES: AdminRole[] = ['BUYER_TENANT', 'OWNER', 'AGENT', 'MAINTENANCE_STAFF', 'ADMIN'];

@Component({
  selector: 'app-admin-users', standalone: true, imports: [TranslocoDirective],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './admin-users.component.html'
})
export class AdminUsersComponent {
  private readonly service = inject(AdminUserService);
  readonly roles = ROLES;
  readonly users = signal<AdminUserResponse[]>([]);
  readonly isLoading = signal(false);
  readonly errorMessage = signal<string | null>(null);
  readonly currentPage = signal(0);
  readonly totalPages = signal(0);
  readonly totalElements = signal(0);
  readonly query = signal('');
  readonly selectedRole = signal<AdminRole | null>(null);
  readonly selectedEnabled = signal<boolean | null>(null);
  readonly roleOptions = ROLES;

  constructor() { this.load(); }

  load(): void {
    this.isLoading.set(true);
    this.errorMessage.set(null);
    this.service.list(this.filters(), this.currentPage(), 10).subscribe({
      next: (page) => {
        this.users.set(page.content); this.totalPages.set(page.totalPages);
        this.totalElements.set(page.totalElements); this.isLoading.set(false);
      },
      error: () => { this.errorMessage.set('ADMIN_USERS.ERRORS.LOAD_FAILED'); this.isLoading.set(false); }
    });
  }

  setQuery(value: string): void { this.query.set(value); }
  setRole(value: string): void { this.selectedRole.set(value ? value as AdminRole : null); }
  setEnabled(value: string): void { this.selectedEnabled.set(value === '' ? null : value === 'true'); }
  applyFilters(): void { this.currentPage.set(0); this.load(); }
  previous(): void { if (this.currentPage() > 0) { this.currentPage.update((p) => p - 1); this.load(); } }
  next(): void { if (this.currentPage() + 1 < this.totalPages()) { this.currentPage.update((p) => p + 1); this.load(); } }
  hasRole(user: AdminUserResponse, role: AdminRole): boolean { return user.roles.includes(role); }

  toggle(user: AdminUserResponse, role: AdminRole): void {
    if (user.seededAdmin) return;
    this.errorMessage.set(null);
    const request = this.hasRole(user, role) ? this.service.revokeRole(user.id, role) : this.service.grantRole(user.id, role);
    request.subscribe({
      next: (updated) => this.users.update((users) => users.map((item) => item.id === updated.id ? updated : item)),
      error: (error: HttpErrorResponse) => {
        const key = error.status === 403 ? 'ACTION_FORBIDDEN' : error.status === 409 ? 'ACTION_CONFLICT' : 'ACTION_FAILED';
        this.errorMessage.set(`ADMIN_USERS.ERRORS.${key}`);
      }
    });
  }

  private filters(): AdminUserFilters {
    return { q: this.query(), role: this.selectedRole(), enabled: this.selectedEnabled() };
  }
}
