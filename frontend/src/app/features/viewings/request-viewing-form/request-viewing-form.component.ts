import { DatePipe } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import {
  ChangeDetectionStrategy,
  Component,
  OnInit,
  inject,
  input,
  signal,
} from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';
import { TranslocoPipe } from '@jsverse/transloco';
import { UiButtonComponent } from '../../../shared/components/ui-button/ui-button.component';
import { ViewingSlot } from '../models/viewing-slot.model';
import { Viewing } from '../models/viewing.model';
import { ViewingSlotService } from '../services/viewing-slot.service';
import { ViewingService } from '../services/viewing.service';

@Component({
  selector: 'app-request-viewing-form',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [ReactiveFormsModule, DatePipe, TranslocoPipe, UiButtonComponent],
  template: `
    <section class="mx-auto max-w-3xl space-y-6 text-start">
      <div class="rounded-xl border border-slate-200 bg-white ps-6 pe-6 py-6 shadow-xs">
        <h1 class="text-2xl font-bold text-slate-900">
          {{ 'VIEWINGS.REQUEST.TITLE' | transloco }}
        </h1>
        <p class="mt-1 text-sm text-slate-600">
          {{ 'VIEWINGS.REQUEST.SUBTITLE' | transloco }}
        </p>

        @if (apiErrorKey()) {
          <div
            role="alert"
            data-testid="api-error"
            class="mt-4 rounded-lg border-s-4 border-rose-600 bg-rose-50 ps-4 pe-4 py-3 text-sm text-rose-800"
          >
            {{ apiErrorKey()! | transloco }}
          </div>
        }

        @if (bookedViewing()) {
          <div
            role="status"
            data-testid="booking-success"
            class="mt-4 rounded-lg border-s-4 border-emerald-600 bg-emerald-50 ps-4 pe-4 py-3 text-sm text-emerald-800"
          >
            {{ 'VIEWINGS.REQUEST.SUCCESS' | transloco }}
          </div>
        }

        <form [formGroup]="requestForm" (ngSubmit)="onSubmit()" class="mt-6 space-y-5" novalidate>
          @if (!propertyId()) {
            <div>
              <label for="requestPropertyId" class="block text-sm font-medium text-slate-700">
                {{ 'VIEWINGS.REQUEST.PROPERTY_ID_LABEL' | transloco }}
              </label>
              <input
                id="requestPropertyId"
                type="number"
                min="1"
                formControlName="propertyId"
                (change)="onPropertyChange()"
                class="mt-1 block w-full rounded-lg border border-slate-300 ps-3 pe-3 py-2 text-sm text-slate-900 focus:border-emerald-600 focus:outline-none"
              />
            </div>
          }

          <div>
            <span class="block text-sm font-medium text-slate-700">
              {{ 'VIEWINGS.REQUEST.SLOTS_LABEL' | transloco }}
            </span>

            @if (loadingSlots()) {
              <p data-testid="loading-slots" class="mt-2 text-sm text-slate-500">
                {{ 'VIEWINGS.REQUEST.LOADING_SLOTS' | transloco }}
              </p>
            } @else if (slots().length === 0) {
              <p data-testid="empty-slots" class="mt-2 text-sm text-slate-500">
                {{ 'VIEWINGS.REQUEST.EMPTY_SLOTS' | transloco }}
              </p>
            } @else {
              <div class="mt-2 grid grid-cols-1 gap-3 sm:grid-cols-2" role="radiogroup">
                @for (slot of slots(); track slot.id) {
                  <label
                    [attr.data-testid]="'slot-option-' + slot.id"
                    class="flex cursor-pointer items-center gap-3 rounded-lg border ps-4 pe-4 py-3 text-sm transition-colors"
                    [class.border-emerald-600]="requestForm.controls.slotId.value === slot.id"
                    [class.bg-emerald-50]="requestForm.controls.slotId.value === slot.id"
                    [class.border-slate-200]="requestForm.controls.slotId.value !== slot.id"
                  >
                    <input
                      type="radio"
                      formControlName="slotId"
                      [value]="slot.id"
                      class="h-4 w-4 text-emerald-600 focus:ring-emerald-600"
                    />
                    <span class="font-medium text-slate-800">
                      {{ slot.startTime | date: 'medium' }} – {{ slot.endTime | date: 'shortTime' }}
                    </span>
                  </label>
                }
              </div>
            }

            @if (requestForm.controls.slotId.touched && requestForm.controls.slotId.hasError('required')) {
              <p data-testid="slot-required-error" class="mt-2 text-sm font-medium text-rose-600">
                {{ 'VIEWINGS.REQUEST.ERRORS.SLOT_REQUIRED' | transloco }}
              </p>
            }
          </div>

          <div>
            <label for="viewingNotes" class="block text-sm font-medium text-slate-700">
              {{ 'VIEWINGS.REQUEST.NOTES_LABEL' | transloco }}
            </label>
            <textarea
              id="viewingNotes"
              rows="3"
              formControlName="notes"
              [placeholder]="'VIEWINGS.REQUEST.NOTES_PLACEHOLDER' | transloco"
              class="mt-1 block w-full rounded-lg border border-slate-300 ps-3 pe-3 py-2 text-sm text-slate-900 focus:border-emerald-600 focus:outline-none"
            ></textarea>
            @if (requestForm.controls.notes.touched && requestForm.controls.notes.hasError('maxlength')) {
              <p data-testid="notes-maxlength-error" class="mt-1 text-sm font-medium text-rose-600">
                {{ 'VIEWINGS.REQUEST.ERRORS.NOTES_MAXLENGTH' | transloco }}
              </p>
            }
          </div>

          <div class="pt-2">
            <app-ui-button
              type="submit"
              variant="primary"
              [disabled]="submitting() || slots().length === 0"
            >
              {{ 'VIEWINGS.REQUEST.SUBMIT' | transloco }}
            </app-ui-button>
          </div>
        </form>
      </div>
    </section>
  `,
})
export class RequestViewingFormComponent implements OnInit {
  readonly propertyId = input<number | null>(null);

  private readonly fb = inject(FormBuilder);
  private readonly route = inject(ActivatedRoute);
  private readonly slotService = inject(ViewingSlotService);
  private readonly viewingService = inject(ViewingService);

  readonly slots = signal<ViewingSlot[]>([]);
  readonly loadingSlots = signal(false);
  readonly submitting = signal(false);
  readonly apiErrorKey = signal<string | null>(null);
  readonly bookedViewing = signal<Viewing | null>(null);

  readonly requestForm = this.fb.group({
    propertyId: [1, [Validators.required, Validators.min(1)]],
    slotId: [null as number | null, [Validators.required]],
    notes: ['', [Validators.maxLength(1000)]],
  });

  ngOnInit(): void {
    const inputId = this.propertyId();
    const routeParam = this.route.snapshot.paramMap.get('propertyId');
    const resolvedId = inputId ?? (routeParam ? Number(routeParam) : 1);

    if (Number.isInteger(resolvedId) && resolvedId > 0) {
      this.requestForm.controls.propertyId.setValue(resolvedId);
      this.loadSlots(resolvedId);
    }
  }

  onPropertyChange(): void {
    const id = Number(this.requestForm.controls.propertyId.value);
    this.requestForm.controls.slotId.reset(null);
    this.bookedViewing.set(null);
    this.apiErrorKey.set(null);
    if (Number.isInteger(id) && id > 0) {
      this.loadSlots(id);
    }
  }

  loadSlots(propertyId: number): void {
    this.loadingSlots.set(true);
    this.slotService.getSlots(propertyId).subscribe({
      next: (loaded) => {
        this.slots.set(loaded);
        this.loadingSlots.set(false);
      },
      error: () => {
        this.slots.set([]);
        this.loadingSlots.set(false);
      },
    });
  }

  onSubmit(): void {
    this.apiErrorKey.set(null);
    this.bookedViewing.set(null);
    this.requestForm.markAllAsTouched();

    if (this.requestForm.invalid) {
      return;
    }

    const slotId = this.requestForm.controls.slotId.value;
    if (slotId === null) {
      return;
    }

    const rawNotes = this.requestForm.controls.notes.value?.trim();
    const payload = rawNotes ? { slotId, notes: rawNotes } : { slotId };

    this.submitting.set(true);
    this.viewingService.requestViewing(payload).subscribe({
      next: (created) => {
        this.bookedViewing.set(created);
        this.requestForm.controls.notes.reset('');
        this.requestForm.controls.slotId.reset(null);
        this.submitting.set(false);
      },
      error: (err: HttpErrorResponse) => {
        this.apiErrorKey.set(this.mapErrorKey(err.status));
        this.submitting.set(false);
        if (err.status === 409) {
          const propId = Number(this.requestForm.controls.propertyId.value);
          if (Number.isInteger(propId) && propId > 0) {
            this.loadSlots(propId);
          }
        }
      },
    });
  }

  private mapErrorKey(status: number): string {
    if (status === 409) {
      return 'VIEWINGS.REQUEST.ERRORS.CONFLICT_409';
    }
    if (status === 403) {
      return 'VIEWINGS.REQUEST.ERRORS.FORBIDDEN_403';
    }
    if (status === 401) {
      return 'VIEWINGS.REQUEST.ERRORS.UNAUTHORIZED_401';
    }
    return 'VIEWINGS.REQUEST.ERRORS.GENERIC';
  }
}
