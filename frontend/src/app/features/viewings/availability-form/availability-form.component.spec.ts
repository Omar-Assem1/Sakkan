import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpErrorResponse } from '@angular/common/http';
import { provideRouter } from '@angular/router';
import { TranslocoTestingModule } from '@jsverse/transloco';
import { of, throwError } from 'rxjs';
import { AvailabilityFormComponent } from './availability-form.component';
import { ViewingSlotService } from '../services/viewing-slot.service';
import { ViewingSlot } from '../models/viewing-slot.model';

const TEST_TRANSLATIONS = {
  en: {
    VIEWINGS: {
      AVAILABILITY: {
        TITLE: 'Manage Viewing Availability',
        SUBTITLE: 'Add or remove available time slots for property viewings.',
        PROPERTY_ID_LABEL: 'Property ID',
        START_TIME_LABEL: 'Start Time',
        END_TIME_LABEL: 'End Time',
        ADD_SLOT: 'Add Viewing Slot',
        SLOTS_HEADING: 'Available Time Slots',
        SELECT_PROPERTY_PROMPT: 'Enter a property ID above to manage its viewing slots.',
        EMPTY_SLOTS: 'No viewing slots configured for this property yet.',
        REMOVE_SLOT: 'Remove',
        ERRORS: {
          REQUIRED: 'Both start time and end time are required.',
          INVALID_RANGE: 'End time must be after start time.',
          PAST_TIME: 'Start time must be in the future.',
          CONFLICT_409:
            'This time slot overlaps with an existing slot or the property is not published.',
          FORBIDDEN_403: 'Only the property owner or assigned agent can manage viewing slots.',
          GENERIC: 'Could not save the viewing slot. Please try again.',
        },
      },
    },
  },
  ar: {
    VIEWINGS: {
      AVAILABILITY: {
        TITLE: 'إدارة مواعيد المعاينة المتاحة',
        SUBTITLE: 'أضف أو احذف الفترات الزمنية المتاحة لمعاينة العقار.',
        PROPERTY_ID_LABEL: 'رقم العقار',
        START_TIME_LABEL: 'وقت البدء',
        END_TIME_LABEL: 'وقت الانتهاء',
        ADD_SLOT: 'إضافة موعد معاينة',
        SLOTS_HEADING: 'المواعيد المتاحة',
        SELECT_PROPERTY_PROMPT: 'أدخل رقم العقار أعلاه لإدارة مواعيد المعاينة الخاصة به.',
        EMPTY_SLOTS: 'لا توجد مواعيد معاينة مضافة لهذا العقار حتى الآن.',
        REMOVE_SLOT: 'حذف',
        ERRORS: {
          REQUIRED: 'وقت البدء ووقت الانتهاء مطلوبان.',
          INVALID_RANGE: 'يجب أن يكون وقت الانتهاء بعد وقت البدء.',
          PAST_TIME: 'يجب أن يكون وقت البدء في المستقبل.',
          CONFLICT_409: 'يتعارض هذا الموعد مع موعد آخر موجود أو أن العقار غير منشور.',
          FORBIDDEN_403: 'يمكن لمالك العقار أو الوكيل المعين فقط إدارة مواعيد المعاينة.',
          GENERIC: 'تعذر حفظ موعد المعاينة. يرجى المحاولة مرة أخرى.',
        },
      },
    },
  },
};

describe('AvailabilityFormComponent — Form 7 (SCRUM-96 / SKN-25)', () => {
  let component: AvailabilityFormComponent;
  let fixture: ComponentFixture<AvailabilityFormComponent>;
  let slotServiceSpy: {
    getSlots: ReturnType<typeof vi.fn>;
    createSlot: ReturnType<typeof vi.fn>;
    deleteSlot: ReturnType<typeof vi.fn>;
  };

  const INITIAL_SLOTS: ViewingSlot[] = [
    {
      id: 11,
      propertyId: 1,
      createdBy: 101,
      startTime: '2026-12-10T10:00:00.000Z',
      endTime: '2026-12-10T11:00:00.000Z',
      createdAt: '2026-11-18T10:00:00.000Z',
    },
  ];

  beforeEach(async () => {
    slotServiceSpy = {
      getSlots: vi.fn().mockReturnValue(of(INITIAL_SLOTS)),
      createSlot: vi.fn(),
      deleteSlot: vi.fn().mockReturnValue(of(void 0)),
    };

    await TestBed.configureTestingModule({
      imports: [
        AvailabilityFormComponent,
        TranslocoTestingModule.forRoot({
          langs: TEST_TRANSLATIONS,
          translocoConfig: {
            availableLangs: ['en', 'ar'],
            defaultLang: 'en',
          },
        }),
      ],
      providers: [provideRouter([]), { provide: ViewingSlotService, useValue: slotServiceSpy }],
    }).compileComponents();

    fixture = TestBed.createComponent(AvailabilityFormComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should start empty without defaulting to property 1 and show prompt until a property is chosen', () => {
    expect(slotServiceSpy.getSlots).not.toHaveBeenCalled();
    expect(component.slotForm.controls.propertyId.value).toBeNull();
    expect(fixture.nativeElement.querySelector('[data-testid="select-property-prompt"]')).toBeTruthy();
  });

  it('should load and render existing viewing slots when a property ID is selected', () => {
    component.slotForm.controls.propertyId.setValue(1);
    component.onPropertyChange();
    fixture.detectChanges();

    expect(slotServiceSpy.getSlots).toHaveBeenCalledWith(1);
    const items = fixture.nativeElement.querySelectorAll('[data-testid="slot-item"]');
    expect(items.length).toBe(1);
  });

  it('should validate client-side that endTime must be after startTime and block submission', () => {
    component.slotForm.controls.propertyId.setValue(1);
    component.slotForm.controls.startTime.setValue('2026-12-15T14:00');
    component.slotForm.controls.endTime.setValue('2026-12-15T13:00');
    component.slotForm.markAllAsTouched();
    fixture.detectChanges();

    expect(component.slotForm.hasError('invalidTimeRange')).toBe(true);
    component.onSubmit();

    expect(slotServiceSpy.createSlot).not.toHaveBeenCalled();
    const rangeError = fixture.nativeElement.querySelector('[data-testid="time-range-error"]');
    expect(rangeError).toBeTruthy();
    expect(rangeError.textContent).toContain('End time must be after start time.');
  });

  it('should submit valid slot and append it to the displayed slot list', () => {
    const newSlot: ViewingSlot = {
      id: 12,
      propertyId: 1,
      createdBy: 101,
      startTime: '2026-12-15T11:00:00.000Z',
      endTime: '2026-12-15T12:00:00.000Z',
      createdAt: '2026-11-18T11:00:00.000Z',
    };
    slotServiceSpy.createSlot.mockReturnValue(of(newSlot));

    component.slotForm.controls.propertyId.setValue(1);
    component.onPropertyChange();
    component.slotForm.controls.startTime.setValue('2026-12-15T11:00');
    component.slotForm.controls.endTime.setValue('2026-12-15T12:00');
    component.onSubmit();
    fixture.detectChanges();

    expect(slotServiceSpy.createSlot).toHaveBeenCalledTimes(1);
    const items = fixture.nativeElement.querySelectorAll('[data-testid="slot-item"]');
    expect(items.length).toBe(2);
  });

  it('should display localized conflict error when backend returns 409 Conflict ProblemDetail', () => {
    slotServiceSpy.createSlot.mockReturnValue(
      throwError(
        () =>
          new HttpErrorResponse({
            status: 409,
            statusText: 'Conflict',
            error: {
              title: 'Conflict',
              status: 409,
              detail: 'Viewing slot overlaps with an existing slot',
            },
          }),
      ),
    );

    component.slotForm.controls.propertyId.setValue(1);
    component.slotForm.controls.startTime.setValue('2026-12-15T10:30');
    component.slotForm.controls.endTime.setValue('2026-12-15T11:30');
    component.onSubmit();
    fixture.detectChanges();

    const apiError = fixture.nativeElement.querySelector('[data-testid="api-error"]');
    expect(apiError).toBeTruthy();
    expect(apiError.textContent).toContain(
      'This time slot overlaps with an existing slot or the property is not published.',
    );
  });

  it('should remove slot from list when delete button is clicked', () => {
    component.slotForm.controls.propertyId.setValue(1);
    component.onPropertyChange();
    fixture.detectChanges();

    const deleteBtn = fixture.nativeElement.querySelector(
      '[data-testid="delete-slot-btn"] button',
    ) as HTMLButtonElement;
    expect(deleteBtn).toBeTruthy();

    deleteBtn.click();
    fixture.detectChanges();

    expect(slotServiceSpy.deleteSlot).toHaveBeenCalledWith(1, 11);
    const items = fixture.nativeElement.querySelectorAll('[data-testid="slot-item"]');
    expect(items.length).toBe(0);
    expect(fixture.nativeElement.querySelector('[data-testid="empty-slots"]')).toBeTruthy();
  });

  it('should use strictly logical Tailwind CSS classes and zero physical directional classes', () => {
    const allElements = fixture.nativeElement.querySelectorAll('*');
    allElements.forEach((el: Element) => {
      expect(el.className).not.toMatch(
        /\b(ml-|mr-|pl-|pr-|text-left|text-right|left-|right-|border-l|border-r)/,
      );
    });
  });
});
