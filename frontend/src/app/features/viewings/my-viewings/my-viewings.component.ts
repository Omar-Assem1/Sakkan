import { DatePipe } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import {
  ChangeDetectionStrategy,
  Component,
  OnInit,
  computed,
  inject,
  signal,
} from '@angular/core';
import { RouterLink } from '@angular/router';
import { TranslocoPipe } from '@jsverse/transloco';
import { Observable } from 'rxjs';
import { TokenStorageService } from '../../../core/services/token-storage.service';
import { UiButtonComponent } from '../../../shared/components/ui-button/ui-button.component';
import { Viewing, ViewingStatus } from '../models/viewing.model';
import { ViewingService } from '../services/viewing.service';

const STATUS_FILTERS: readonly (ViewingStatus | null)[] = [
  null,
  'REQUESTED',
  'CONFIRMED',
  'CANCELLED',
  'COMPLETED',
];

@Component({
  selector: 'app-my-viewings',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [DatePipe, RouterLink, TranslocoPipe, UiButtonComponent],
  template: `
    <section class="mx-auto max-w-5xl space-y-6 text-start">
      <div
        class="flex flex-col gap-4 rounded-2xl border border-slate-200 bg-white ps-6 pe-6 py-6 shadow-2xs transition-colors sm:flex-row sm:items-center sm:justify-between dark:border-slate-800 dark:bg-slate-900"
      >
        <div>
          <h1 class="text-2xl font-extrabold tracking-tight text-slate-900 dark:text-slate-100">
            {{ 'VIEWINGS.MY.TITLE' | transloco }}
          </h1>
          <p class="mt-1 text-sm text-slate-600 dark:text-slate-400">
            {{
              (isManager() ? 'VIEWINGS.MY.SUBTITLE_MANAGER' : 'VIEWINGS.MY.SUBTITLE_BUYER')
                | transloco
            }}
          </p>
        </div>

        <div class="flex flex-wrap items-center gap-2">
          @if (isManager()) {
            <a
              routerLink="/viewings/availability"
              data-testid="manage-availability-link"
              class="inline-flex items-center rounded-xl border border-slate-300 bg-white ps-4 pe-4 py-2 text-sm font-semibold text-slate-800 shadow-2xs transition hover:bg-slate-50 dark:border-slate-700 dark:bg-slate-800 dark:text-slate-200 dark:hover:bg-slate-700"
            >
              {{ 'VIEWINGS.MY.MANAGE_AVAILABILITY' | transloco }}
            </a>
          } @else {
            <a
              routerLink="/listings"
              data-testid="browse-properties-link"
              class="inline-flex items-center rounded-xl bg-emerald-600 ps-4 pe-4 py-2 text-sm font-semibold text-white shadow-2xs transition hover:bg-emerald-700 dark:hover:bg-emerald-500"
            >
              {{ 'VIEWINGS.MY.BROWSE_PROPERTIES' | transloco }}
            </a>
          }
        </div>
      </div>

      <div
        class="rounded-2xl border border-slate-200 bg-white ps-6 pe-6 py-6 shadow-2xs transition-colors dark:border-slate-800 dark:bg-slate-900"
      >
        <div class="flex flex-wrap items-center gap-2" role="group">
          @for (filter of statusFilters; track filter ?? 'ALL') {
            <button
              type="button"
              [attr.data-testid]="'filter-' + (filter ?? 'ALL')"
              (click)="selectStatus(filter)"
              class="cursor-pointer rounded-full border ps-4 pe-4 py-1.5 text-sm font-medium transition-colors"
              [class]="
                selectedStatus() === filter
                  ? 'border-emerald-600 bg-emerald-600 text-white'
                  : 'border-slate-300 bg-white text-slate-700 hover:bg-slate-50 dark:border-slate-700 dark:bg-slate-800 dark:text-slate-300 dark:hover:bg-slate-700'
              "
            >
              {{
                (filter ? 'VIEWINGS.MY.STATUS.' + filter : 'VIEWINGS.MY.FILTER_ALL') | transloco
              }}
            </button>
          }
        </div>

        @if (errorKey()) {
          <div
            role="alert"
            data-testid="my-viewings-error"
            class="mt-4 rounded-xl border-s-4 border-rose-600 bg-rose-50 ps-4 pe-4 py-3 text-sm font-medium text-rose-800 dark:bg-rose-950/50 dark:text-rose-200"
          >
            {{ errorKey()! | transloco }}
          </div>
        }

        @if (loading()) {
          <p data-testid="my-viewings-loading" class="mt-6 text-sm text-slate-500 dark:text-slate-400">
            {{ 'VIEWINGS.MY.LOADING' | transloco }}
          </p>
        } @else if (viewings().length === 0) {
          <p data-testid="my-viewings-empty" class="mt-6 text-sm text-slate-500 dark:text-slate-400">
            {{ 'VIEWINGS.MY.EMPTY' | transloco }}
          </p>
        } @else {
          <div class="mt-6 overflow-x-auto">
            <table class="min-w-full divide-y divide-slate-200 text-start text-sm dark:divide-slate-800">
              <thead
                class="bg-slate-50 text-xs font-semibold uppercase text-slate-600 dark:bg-slate-800/70 dark:text-slate-400"
              >
                <tr>
                  <th class="ps-4 pe-4 py-3 text-start">
                    {{ 'VIEWINGS.MY.COL_PROPERTY' | transloco }}
                  </th>
                  <th class="ps-4 pe-4 py-3 text-start">
                    {{ 'VIEWINGS.MY.COL_SLOT' | transloco }}
                  </th>
                  <th class="ps-4 pe-4 py-3 text-start">
                    {{ 'VIEWINGS.MY.COL_STATUS' | transloco }}
                  </th>
                  <th class="ps-4 pe-4 py-3 text-start">
                    {{ 'VIEWINGS.MY.COL_NOTES' | transloco }}
                  </th>
                  <th class="ps-4 pe-4 py-3 text-start">
                    {{ 'VIEWINGS.MY.COL_CREATED' | transloco }}
                  </th>
                  <th class="ps-4 pe-4 py-3 text-end">
                    {{ 'VIEWINGS.MY.COL_ACTIONS' | transloco }}
                  </th>
                </tr>
              </thead>
              <tbody class="divide-y divide-slate-200 bg-white dark:divide-slate-800 dark:bg-slate-900">
                @for (viewing of viewings(); track viewing.id) {
                  <tr
                    class="transition hover:bg-slate-50/80 dark:hover:bg-slate-800/50"
                    [attr.data-testid]="'viewing-row-' + viewing.id"
                  >
                    <td
                      [attr.data-testid]="'viewing-property-' + viewing.id"
                      class="ps-4 pe-4 py-3 font-medium text-slate-900 dark:text-slate-100"
                    >
                      <a
                        [routerLink]="['/listings', viewing.propertyId]"
                        class="font-bold text-slate-900 hover:text-emerald-700 hover:underline dark:text-slate-100 dark:hover:text-emerald-400"
                      >
                        {{ viewing.propertyTitle || '#' + viewing.propertyId }}
                      </a>
                    </td>
                    <td
                      [attr.data-testid]="'viewing-slot-' + viewing.id"
                      class="ps-4 pe-4 py-3 text-slate-700 dark:text-slate-300"
                    >
                      @if (viewing.slotStartTime && viewing.slotEndTime) {
                        {{ viewing.slotStartTime | date: 'medium' }} –
                        {{ viewing.slotEndTime | date: 'shortTime' }}
                      } @else {
                        #{{ viewing.slotId }}
                      }
                    </td>
                    <td class="ps-4 pe-4 py-3">
                      <span
                        [attr.data-testid]="'viewing-status-' + viewing.id"
                        class="inline-flex rounded-full ps-2.5 pe-2.5 py-0.5 text-xs font-semibold"
                        [class]="statusBadgeClass(viewing.status)"
                      >
                        {{ 'VIEWINGS.MY.STATUS.' + viewing.status | transloco }}
                      </span>
                    </td>
                    <td class="max-w-xs truncate ps-4 pe-4 py-3 text-slate-600 dark:text-slate-400">
                      {{ viewing.notes || ('VIEWINGS.MY.NO_NOTES' | transloco) }}
                    </td>
                    <td class="ps-4 pe-4 py-3 text-slate-600 dark:text-slate-400">
                      {{ viewing.createdAt | date: 'medium' }}
                    </td>
                    <td class="ps-4 pe-4 py-3 text-end">
                      <div class="inline-flex flex-wrap justify-end gap-2">
                        @if (canConfirm(viewing)) {
                          <app-ui-button
                            variant="primary"
                            [disabled]="busyId() === viewing.id"
                            [attr.data-testid]="'confirm-btn-' + viewing.id"
                            (clicked)="confirm(viewing)"
                          >
                            {{ 'VIEWINGS.MY.ACTIONS.CONFIRM' | transloco }}
                          </app-ui-button>
                        }
                        @if (canComplete(viewing)) {
                          <app-ui-button
                            variant="secondary"
                            [disabled]="busyId() === viewing.id"
                            [attr.data-testid]="'complete-btn-' + viewing.id"
                            (clicked)="complete(viewing)"
                          >
                            {{ 'VIEWINGS.MY.ACTIONS.COMPLETE' | transloco }}
                          </app-ui-button>
                        }
                        @if (canCancel(viewing)) {
                          <app-ui-button
                            variant="danger"
                            [disabled]="busyId() === viewing.id"
                            [attr.data-testid]="'cancel-btn-' + viewing.id"
                            (clicked)="cancel(viewing)"
                          >
                            {{ 'VIEWINGS.MY.ACTIONS.CANCEL' | transloco }}
                          </app-ui-button>
                        }
                      </div>
                    </td>
                  </tr>
                }
              </tbody>
            </table>
          </div>

          @if (totalPages() > 1) {
            <div
              class="mt-4 flex items-center justify-between border-t border-slate-200 pt-4 dark:border-slate-800"
            >
              <app-ui-button
                variant="outline"
                [disabled]="page() === 0"
                (clicked)="goToPage(page() - 1)"
              >
                {{ 'VIEWINGS.MY.PAGINATION.PREVIOUS' | transloco }}
              </app-ui-button>
              <span class="text-sm font-medium text-slate-600 dark:text-slate-300">
                {{ page() + 1 }} / {{ totalPages() }}
              </span>
              <app-ui-button
                variant="outline"
                [disabled]="page() + 1 >= totalPages()"
                (clicked)="goToPage(page() + 1)"
              >
                {{ 'VIEWINGS.MY.PAGINATION.NEXT' | transloco }}
              </app-ui-button>
            </div>
          }
        }
      </div>
    </section>
  `,
})
export class MyViewingsComponent implements OnInit {
  private readonly viewingService = inject(ViewingService);
  private readonly tokenStorage = inject(TokenStorageService);

  readonly statusFilters = STATUS_FILTERS;
  readonly isManager = computed(() => this.tokenStorage.hasRole('OWNER', 'AGENT'));

  readonly viewings = signal<Viewing[]>([]);
  readonly selectedStatus = signal<ViewingStatus | null>(null);
  readonly page = signal(0);
  readonly totalPages = signal(0);
  readonly loading = signal(false);
  readonly busyId = signal<number | null>(null);
  readonly errorKey = signal<string | null>(null);

  ngOnInit(): void {
    this.loadViewings();
  }

  statusBadgeClass(status: ViewingStatus): string {
    switch (status) {
      case 'REQUESTED':
        return 'bg-amber-100 text-amber-800 dark:bg-amber-950/70 dark:text-amber-300';
      case 'CONFIRMED':
        return 'bg-emerald-100 text-emerald-800 dark:bg-emerald-950/70 dark:text-emerald-300';
      case 'CANCELLED':
        return 'bg-rose-100 text-rose-800 dark:bg-rose-950/70 dark:text-rose-300';
      case 'COMPLETED':
        return 'bg-sky-100 text-sky-800 dark:bg-sky-950/70 dark:text-sky-300';
    }
  }

  selectStatus(status: ViewingStatus | null): void {
    this.selectedStatus.set(status);
    this.page.set(0);
    this.loadViewings();
  }

  goToPage(targetPage: number): void {
    if (targetPage < 0 || (this.totalPages() > 0 && targetPage >= this.totalPages())) {
      return;
    }
    this.page.set(targetPage);
    this.loadViewings();
  }

  canConfirm(viewing: Viewing): boolean {
    return this.isManager() && viewing.status === 'REQUESTED';
  }

  canComplete(viewing: Viewing): boolean {
    return this.isManager() && viewing.status === 'CONFIRMED';
  }

  canCancel(viewing: Viewing): boolean {
    return viewing.status === 'REQUESTED' || viewing.status === 'CONFIRMED';
  }

  confirm(viewing: Viewing): void {
    this.runTransition(viewing.id, this.viewingService.confirmViewing(viewing.id));
  }

  cancel(viewing: Viewing): void {
    this.runTransition(viewing.id, this.viewingService.cancelViewing(viewing.id));
  }

  complete(viewing: Viewing): void {
    this.runTransition(viewing.id, this.viewingService.completeViewing(viewing.id));
  }

  loadViewings(): void {
    this.loading.set(true);
    this.errorKey.set(null);
    this.viewingService.getMyViewings(this.selectedStatus(), this.page()).subscribe({
      next: (response) => {
        this.viewings.set(response.content);
        this.totalPages.set(response.totalPages);
        this.loading.set(false);
      },
      error: (err: HttpErrorResponse) => {
        this.viewings.set([]);
        this.loading.set(false);
        if (err.status === 401) {
          this.errorKey.set('VIEWINGS.MY.ERRORS.UNAUTHORIZED_401');
        } else if (err.status === 403) {
          this.errorKey.set('VIEWINGS.MY.ERRORS.FORBIDDEN_403');
        } else {
          this.errorKey.set('VIEWINGS.MY.ERRORS.LOAD_FAILED');
        }
      },
    });
  }

  private runTransition(viewingId: number, request$: Observable<Viewing>): void {
    this.busyId.set(viewingId);
    this.errorKey.set(null);
    request$.subscribe({
      next: (updated) => {
        const activeFilter = this.selectedStatus();
        if (activeFilter && updated.status !== activeFilter) {
          this.viewings.update((list) => list.filter((v) => v.id !== updated.id));
        } else {
          this.viewings.update((list) => list.map((v) => (v.id === updated.id ? updated : v)));
        }
        this.busyId.set(null);
      },
      error: (err: HttpErrorResponse) => {
        this.busyId.set(null);
        this.errorKey.set(this.mapTransitionError(err.status));
      },
    });
  }

  private mapTransitionError(status: number): string {
    if (status === 409) {
      return 'VIEWINGS.MY.ERRORS.CONFLICT_409';
    }
    if (status === 403) {
      return 'VIEWINGS.MY.ERRORS.FORBIDDEN_403';
    }
    if (status === 401) {
      return 'VIEWINGS.MY.ERRORS.UNAUTHORIZED_401';
    }
    return 'VIEWINGS.MY.ERRORS.GENERIC';
  }
}
