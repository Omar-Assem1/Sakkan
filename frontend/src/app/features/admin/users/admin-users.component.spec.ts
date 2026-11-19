import { HttpErrorResponse } from '@angular/common/http';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { TranslocoTestingModule } from '@jsverse/transloco';
import { Observable, of, throwError } from 'rxjs';
import { PageResponse } from '../../properties/models/property.model';
import { AdminUserResponse } from '../models/admin-user.models';
import { AdminUserFilters, AdminUserService } from '../services/admin-user.service';
import { AdminUsersComponent } from './admin-users.component';

const EN = {
  ADMIN_USERS: {
    TITLE: 'Manage users',
    SUBTITLE: 'Search users and change their roles.',
    FILTERS: { SEARCH: 'Search', ROLE: 'Role', STATUS: 'Status', ALL: 'All', ENABLED: 'Enabled', DISABLED: 'Disabled', APPLY: 'Apply' },
    TABLE: { USERNAME: 'Username', EMAIL: 'Email', ROLES: 'Roles', STATUS: 'Status', SEEDED: 'Protected' },
    ROLES: { BUYER_TENANT: 'Buyer/Tenant', OWNER: 'Owner', AGENT: 'Agent', MAINTENANCE_STAFF: 'Maintenance', ADMIN: 'Admin' },
    PAGINATION: { PREVIOUS: 'Previous', NEXT: 'Next' },
    EMPTY: { TITLE: 'No users found', DESCRIPTION: 'Try different filters.' },
    ERRORS: {
      LOAD_FAILED: 'Could not load users.',
      ACTION_FORBIDDEN: 'This change is not allowed.',
      ACTION_CONFLICT: 'This change conflicts with the current roles.',
      ACTION_FAILED: 'Could not change the role.'
    }
  }
};

function user(id: number, roles: string[], seededAdmin = false): AdminUserResponse {
  return { id, username: 'user' + id, email: `user${id}@sakan.com`, phone: null, roles, enabled: true, seededAdmin, authProvider: 'LOCAL' };
}

function page(content: AdminUserResponse[], pageNo = 0, totalPages = 1): PageResponse<AdminUserResponse> {
  return { content, page: pageNo, size: 10, totalElements: content.length, totalPages };
}

class FakeAdminUserService {
  listCalls: Array<{ filters: AdminUserFilters; page: number; size: number }> = [];
  grantCalls: Array<{ id: number; role: string }> = [];
  revokeCalls: Array<{ id: number; role: string }> = [];
  listResponse: () => Observable<PageResponse<AdminUserResponse>> = () => of(page([]));
  grantResponse: (id: number, role: string) => Observable<AdminUserResponse> = (id) => of(user(id, []));
  revokeResponse: (id: number, role: string) => Observable<AdminUserResponse> = (id) => of(user(id, []));

  list(filters: AdminUserFilters, pageNo: number, size: number) {
    this.listCalls.push({ filters, page: pageNo, size });
    return this.listResponse();
  }
  grantRole(id: number, role: string) {
    this.grantCalls.push({ id, role });
    return this.grantResponse(id, role);
  }
  revokeRole(id: number, role: string) {
    this.revokeCalls.push({ id, role });
    return this.revokeResponse(id, role);
  }
}

describe('AdminUsersComponent (SCRUM-69, listing 22)', () => {
  let fixture: ComponentFixture<AdminUsersComponent>;
  let fake: FakeAdminUserService;

  async function create(): Promise<void> {
    fixture = TestBed.createComponent(AdminUsersComponent);
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();
  }

  async function settle(): Promise<void> {
    await fixture.whenStable();
    fixture.detectChanges();
  }

  const el = (): HTMLElement => fixture.nativeElement as HTMLElement;
  const q = (testId: string): HTMLElement | null => el().querySelector(`[data-testid="${testId}"]`);
  const rows = (): HTMLElement[] => Array.from(el().querySelectorAll('[data-testid="user-row"]'));
  const toggle = (row: HTMLElement, role: string): HTMLButtonElement =>
    row.querySelector(`[data-testid="role-toggle-${role}"]`) as HTMLButtonElement;

  beforeEach(() => {
    fake = new FakeAdminUserService();
    TestBed.configureTestingModule({
      imports: [
        AdminUsersComponent,
        TranslocoTestingModule.forRoot({
          langs: { en: EN },
          translocoConfig: { availableLangs: ['en'], defaultLang: 'en' },
          preloadLangs: true
        })
      ],
      providers: [{ provide: AdminUserService, useValue: fake }]
    });
  });

  it('loads page 0 on init and renders one row per user', async () => {
    fake.listResponse = () => of(page([user(1, ['OWNER']), user(2, ['AGENT'])]));
    await create();

    expect(fake.listCalls[0].page).toBe(0);
    expect(rows().length).toBe(2);
    expect(q('loading-spinner')).toBeNull();
  });

  it('shows the empty state when no user matches', async () => {
    await create();

    expect(q('empty-state')).not.toBeNull();
    expect(rows().length).toBe(0);
  });

  it('shows the translated load error', async () => {
    fake.listResponse = () => throwError(() => new HttpErrorResponse({ status: 500 }));
    await create();

    expect(q('api-error')?.textContent).toContain('Could not load users.');
  });

  it('applying filters reloads from page 0 with the entered values', async () => {
    fake.listResponse = () => of(page([user(1, ['OWNER'])], 0, 3));
    await create();
    (q('next-page') as HTMLButtonElement).click();
    await settle();
    expect(fake.listCalls[fake.listCalls.length - 1].page).toBe(1);

    const search = q('filter-q') as HTMLInputElement;
    search.value = 'ali';
    search.dispatchEvent(new Event('input'));
    const role = q('filter-role') as HTMLSelectElement;
    role.value = 'OWNER';
    role.dispatchEvent(new Event('change'));
    (q('apply-filters') as HTMLButtonElement).click();
    await settle();

    const last = fake.listCalls[fake.listCalls.length - 1];
    expect(last.page).toBe(0);
    expect(last.filters.q).toBe('ali');
    expect(last.filters.role).toBe('OWNER');
  });

  it('next and previous buttons request the neighbouring page and are disabled at the edges', async () => {
    fake.listResponse = () => of(page([user(1, ['OWNER'])], 0, 2));
    await create();

    expect((q('prev-page') as HTMLButtonElement).disabled).toBe(true);
    (q('next-page') as HTMLButtonElement).click();
    await settle();

    expect(fake.listCalls[fake.listCalls.length - 1].page).toBe(1);
  });

  it('clicking a role the user does not hold grants it and updates the row', async () => {
    fake.listResponse = () => of(page([user(7, ['OWNER'])]));
    fake.grantResponse = (id) => of(user(id, ['OWNER', 'ADMIN']));
    await create();

    toggle(rows()[0], 'ADMIN').click();
    await settle();

    expect(fake.grantCalls).toEqual([{ id: 7, role: 'ADMIN' }]);
    expect(toggle(rows()[0], 'ADMIN').getAttribute('aria-pressed')).toBe('true');
  });

  it('clicking a role the user holds revokes it and updates the row', async () => {
    fake.listResponse = () => of(page([user(7, ['OWNER', 'ADMIN'])]));
    fake.revokeResponse = (id) => of(user(id, ['OWNER']));
    await create();

    toggle(rows()[0], 'ADMIN').click();
    await settle();

    expect(fake.revokeCalls).toEqual([{ id: 7, role: 'ADMIN' }]);
    expect(toggle(rows()[0], 'ADMIN').getAttribute('aria-pressed')).toBe('false');
  });

  it('disables every role toggle on the seeded admin row', async () => {
    fake.listResponse = () => of(page([user(1, ['ADMIN'], true), user(2, ['OWNER'])]));
    await create();

    const [seededRow, normalRow] = rows();
    for (const role of ['BUYER_TENANT', 'OWNER', 'AGENT', 'MAINTENANCE_STAFF', 'ADMIN']) {
      expect(toggle(seededRow, role).disabled, `seeded ${role}`).toBe(true);
      expect(toggle(normalRow, role).disabled, `normal ${role}`).toBe(false);
    }
  });

  it('a 403 from the server shows the forbidden message and keeps the row unchanged', async () => {
    fake.listResponse = () => of(page([user(7, ['OWNER', 'ADMIN'])]));
    fake.revokeResponse = () => throwError(() => new HttpErrorResponse({ status: 403 }));
    await create();

    toggle(rows()[0], 'ADMIN').click();
    await settle();

    expect(q('api-error')?.textContent).toContain('This change is not allowed.');
    expect(toggle(rows()[0], 'ADMIN').getAttribute('aria-pressed')).toBe('true');
  });

  it('a 409 from the server shows the conflict message and keeps the row unchanged', async () => {
    fake.listResponse = () => of(page([user(7, ['OWNER'])]));
    fake.revokeResponse = () => throwError(() => new HttpErrorResponse({ status: 409 }));
    await create();

    toggle(rows()[0], 'OWNER').click();
    await settle();

    expect(q('api-error')?.textContent).toContain('This change conflicts with the current roles.');
    expect(toggle(rows()[0], 'OWNER').getAttribute('aria-pressed')).toBe('true');
  });
});
