import { DatePipe } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { ChangeDetectionStrategy, Component, OnInit, inject, signal } from '@angular/core';
import {
  AbstractControl,
  FormBuilder,
  ReactiveFormsModule,
  ValidationErrors,
  Validators,
} from '@angular/forms';
import { ActivatedRoute } from '@angular/router';
import { TranslocoPipe } from '@jsverse/transloco';
import { UiButtonComponent } from '../../../shared/components/ui-button/ui-button.component';
import { ViewingSlot } from '../models/viewing-slot.model';
import { ViewingSlotService } from '../services/viewing-slot.service';

function timeRangeValidator(group: AbstractControl): ValidationErrors | null {
  const start = group.get('startTime')?.value as string | null;
  const end = group.get('endTime')?.value as string | null;
  if (!start || !end) {
    return null;
  }
  const startMs = new Date(start).getTime();
  const endMs = new Date(end).getTime();
  if (Number.isNaN(startMs) || Number.isNaN(endMs) || endMs <= startMs) {
    return { invalidTimeRange: true };
  }
  return null;
}

@Component({
  selector: 'app-availability-form',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [ReactiveFormsModule, DatePipe, TranslocoPipe, UiButtonComponent],
  template: `
    <section class="mx-auto max-w-3xl space-y-6 text-start">
      <div class="rounded-xl border border-slate-200 bg-white ps-6 pe-6 py-6 shadow-xs">
        <h1 class="text-2xl font-bold text-slate-900">
          {{ 'VIEWINGS.AVAILABILITY.TITLE' | transloco }}
        </h1>
        <p class="mt-1 text-sm text-slate-600">
          {{ 'VIEWINGS.AVAILABILITY.SUBTITLE' | transloco }}
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

        <form [formGroup]="slotForm" (ngSubmit)="onSubmit()" class="mt-6 space-y-4" novalidate>
          <div>
            <label for="propertyId" class="block text-sm font-medium text-slate-700">
              {{ 'VIEWINGS.AVAILABILITY.PROPERTY_ID_LABEL' | transloco }}
            </label>
            <input
              id="propertyId"
              type="number"
              min="1"
              formControlName="propertyId"
              (change)="onPropertyChange()"
              class="mt-1 block w-full rounded-lg border border-slate-300 ps-3 pe-3 py-2 text-sm text-slate-900 focus:border-emerald-600 focus:outline-none"
            />
          </div>

          <div class="grid grid-cols-1 gap-4 sm:grid-cols-2">
            <div>
              <label for="startTime" class="block text-sm font-medium text-slate-700">
                {{ 'VIEWINGS.AVAILABILITY.START_TIME_LABEL' | transloco }}
              </label>
              <input
                id="startTime"
                type="datetime-local"
                formControlName="startTime"
                class="mt-1 block w-full rounded-lg border border-slate-300 ps-3 pe-3 py-2 text-sm text-slate-900 focus:border-emerald-600 focus:outline-none"
              />
            </div>

            <div>
              <label for="endTime" class="block text-sm font-medium text-slate-700">
                {{ 'VIEWINGS.AVAILABILITY.END_TIME_LABEL' | transloco }}
              </label>
              <input
                id="endTime"
                type="datetime-local"
                formControlName="endTime"
                class="mt-1 block w-full rounded-lg border border-slate-300 ps-3 pe-3 py-2 text-sm text-slate-900 focus:border-emerald-600 focus:outline-none"
              />
            </div>
          </div>

          @if (slotForm.touched && slotForm.hasError('invalidTimeRange')) {
            <p data-testid="time-range-error" class="text-sm font-medium text-rose-600">
              {{ 'VIEWINGS.AVAILABILITY.ERRORS.INVALID_RANGE' | transloco }}
            </p>
          }

          @if (
            slotForm.touched &&
            (slotForm.controls.startTime.hasError('required') ||
              slotForm.controls.endTime.hasError('required'))
          ) {
            <p data-testid="required-error" class="text-sm font-medium text-rose-600">
              {{ 'VIEWINGS.AVAILABILITY.ERRORS.REQUIRED' | transloco }}
            </p>
          }

          <div class="pt-2">
            <app-ui-button type="submit" variant="primary" [disabled]="submitting()">
              {{ 'VIEWINGS.AVAILABILITY.ADD_SLOT' | transloco }}
            </app-ui-button>
          </div>
        </form>
      </div>

      <div class="rounded-xl border border-slate-200 bg-white ps-6 pe-6 py-6 shadow-xs">
        <h2 class="text-lg font-semibold text-slate-900">
          {{ 'VIEWINGS.AVAILABILITY.SLOTS_HEADING' | transloco }}
        </h2>

        @if (slots().length === 0) {
          <p data-testid="empty-slots" class="mt-4 text-sm text-slate-500">
            {{ 'VIEWINGS.AVAILABILITY.EMPTY_SLOTS' | transloco }}
          </p>
        } @else {
          <ul class="mt-4 divide-y divide-slate-200">
            @for (slot of slots(); track slot.id) {
              <li
                data-testid="slot-item"
                class="flex flex-wrap items-center justify-between gap-4 py-3"
              >
                <div class="text-sm text-slate-800">
                  <span class="font-medium">{{ slot.startTime | date: 'medium' }}</span>
                  <span class="ms-2 me-2 text-slate-400">&ndash;</span>
                  <span class="font-medium">{{ slot.endTime | date: 'medium' }}</span>
                </div>
                <app-ui-button
                  variant="danger"
                  data-testid="delete-slot-btn"
                  (clicked)="removeSlot(slot.id)"
                >
                  {{ 'VIEWINGS.AVAILABILITY.REMOVE_SLOT' | transloco }}
                </app-ui-button>
              </li>
            }
          </ul>
        }
      </div>
    </section>
  `,
})
export class AvailabilityFormComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly route = inject(ActivatedRoute);
  private readonly slotService = inject(ViewingSlotService);

  readonly slots = signal<ViewingSlot[]>([]);
  readonly apiErrorKey = signal<string | null>(null);
  readonly submitting = signal<boolean>(false);

  readonly slotForm = this.fb.nonNullable.group(
    {
      propertyId: [1, [Validators.required, Validators.min(1)]],
      startTime: ['', [Validators.required]],
      endTime: ['', [Validators.required]],
    },
    { validators: [timeRangeValidator] },
  );

  ngOnInit(): void {
    const routePropId = Number(this.route.snapshot.paramMap.get('propertyId'));
    if (!Number.isNaN(routePropId) && routePropId > 0) {
      this.slotForm.controls.propertyId.setValue(routePropId);
    }
    this.loadSlots(this.slotForm.controls.propertyId.value);
  }

  onPropertyChange(): void {
    const propId = this.slotForm.controls.propertyId.value;
    if (propId >= 1) {
      this.loadSlots(propId);
    }
  }

  loadSlots(propertyId: number): void {
    this.apiErrorKey.set(null);
    this.slotService.getSlots(propertyId).subscribe({
      next: (loaded) => this.slots.set(loaded),
      error: () => this.slots.set([]),
    });
  }

  onSubmit(): void {
    this.slotForm.markAllAsTouched();
    this.apiErrorKey.set(null);
    if (this.slotForm.invalid) {
      return;
    }

    const { propertyId, startTime, endTime } = this.slotForm.getRawValue();
    const startIso = new Date(startTime).toISOString();
    const endIso = new Date(endTime).toISOString();

    this.submitting.set(true);
    this.slotService.createSlot(propertyId, { startTime: startIso, endTime: endIso }).subscribe({
      next: (created) => {
        this.submitting.set(false);
        this.slots.update((current) => [...current, created]);
        this.slotForm.controls.startTime.reset('');
        this.slotForm.controls.endTime.reset('');
      },
      error: (err: unknown) => {
        this.submitting.set(false);
        if (err instanceof HttpErrorResponse && err.status === 409) {
          this.apiErrorKey.set('VIEWINGS.AVAILABILITY.ERRORS.CONFLICT_409');
        } else if (err instanceof HttpErrorResponse && err.status === 403) {
          this.apiErrorKey.set('VIEWINGS.AVAILABILITY.ERRORS.FORBIDDEN_403');
        } else {
          this.apiErrorKey.set('VIEWINGS.AVAILABILITY.ERRORS.GENERIC');
        }
      },
    });
  }

  removeSlot(slotId: number): void {
    const propertyId = this.slotForm.controls.propertyId.value;
    this.apiErrorKey.set(null);
    this.slotService.deleteSlot(propertyId, slotId).subscribe({
      next: () => {
        this.slots.update((current) => current.filter((s) => s.id !== slotId));
      },
      error: (err: unknown) => {
        if (err instanceof HttpErrorResponse && err.status === 403) {
          this.apiErrorKey.set('VIEWINGS.AVAILABILITY.ERRORS.FORBIDDEN_403');
        } else {
          this.apiErrorKey.set('VIEWINGS.AVAILABILITY.ERRORS.GENERIC');
        }
      },
    });
  }
}
