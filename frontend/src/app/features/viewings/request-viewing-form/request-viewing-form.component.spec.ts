import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpErrorResponse } from '@angular/common/http';
import { provideRouter } from '@angular/router';
import { TranslocoTestingModule } from '@jsverse/transloco';
import { of, throwError } from 'rxjs';
import { RequestViewingFormComponent } from './request-viewing-form.component';
import { ViewingSlotService } from '../services/viewing-slot.service';
import { ViewingService } from '../services/viewing.service';
import { ViewingSlot } from '../models/viewing-slot.model';
import { Viewing } from '../models/viewing.model';

const TEST_TRANSLATIONS = {
  en: {
    VIEWINGS: {
      REQUEST: {
        TITLE: 'Request a Viewing',
        SUBTITLE: 'Select an available time slot to book a viewing for this property.',
        PROPERTY_ID_LABEL: 'Property ID',
        SLOTS_LABEL: 'Available Time Slots',
        SELECT_PROPERTY_PROMPT: 'Enter a property ID above to view its available slots.',
        LOADING_SLOTS: 'Loading available slots...',
        EMPTY_SLOTS: 'No viewing slots are currently available for this property.',
        NOTES_LABEL: 'Notes (optional)',
        NOTES_PLACEHOLDER: 'Add any preferences or questions for the owner or agent...',
        SUBMIT: 'Request Viewing',
        SUCCESS: 'Your viewing request has been submitted and is awaiting confirmation.',
        ERRORS: {
          SLOT_REQUIRED: 'Please select a viewing time slot.',
          NOTES_MAXLENGTH: 'Notes cannot exceed 1000 characters.',
          CONFLICT_409:
            'This time slot was just booked by another user. Please select a different slot.',
          FORBIDDEN_403: 'You cannot book a viewing on your own property or with your current role.',
          UNAUTHORIZED_401: 'Please sign in to request a viewing.',
          GENERIC: 'Could not submit your viewing request. Please try again.',
        },
      },
    },
  },
  ar: {
    VIEWINGS: {
      REQUEST: {
        TITLE: 'طلب موعد معاينة',
        SUBTITLE: 'اختر فترة زمنية متاحة لحجز معاينة لهذا العقار.',
        PROPERTY_ID_LABEL: 'رقم العقار',
        SLOTS_LABEL: 'المواعيد المتاحة',
        SELECT_PROPERTY_PROMPT: 'أدخل رقم العقار أعلاه لعرض المواعيد المتاحة.',
        LOADING_SLOTS: 'جاري تحميل المواعيد المتاحة...',
        EMPTY_SLOTS: 'لا توجد مواعيد معاينة متاحة لهذا العقار حالياً.',
        NOTES_LABEL: 'ملاحظات (اختياري)',
        NOTES_PLACEHOLDER: 'أضف أي تفضيلات أو استفسارات للمالك أو الوكيل...',
        SUBMIT: 'إرسال طلب المعاينة',
        SUCCESS: 'تم إرسال طلب المعاينة بنجاح وهو بانتظار التأكيد.',
        ERRORS: {
          SLOT_REQUIRED: 'يرجى اختيار موعد للمعاينة.',
          NOTES_MAXLENGTH: 'يجب ألا تتجاوز الملاحظات 1000 حرف.',
          CONFLICT_409: 'تم حجز هذا الموعد للتو من قبل مستخدم آخر. يرجى اختيار موعد آخر.',
          FORBIDDEN_403: 'لا يمكنك حجز معاينة لعقارك الخاص أو بصلاحياتك الحالية.',
          UNAUTHORIZED_401: 'يرجى تسجيل الدخول لطلب معاينة.',
          GENERIC: 'تعذر إرسال طلب المعاينة. يرجى المحاولة مرة أخرى.',
        },
      },
    },
  },
};

describe('RequestViewingFormComponent — Form 6 (SCRUM-100 / SKN-26)', () => {
  let fixture: ComponentFixture<RequestViewingFormComponent>;
  let component: RequestViewingFormComponent;
  let slotServiceSpy: { getSlots: ReturnType<typeof vi.fn> };
  let viewingServiceSpy: { requestViewing: ReturnType<typeof vi.fn> };

  const sampleSlots: ViewingSlot[] = [
    {
      id: 10,
      propertyId: 1,
      createdBy: 101,
      startTime: '2026-12-01T10:00:00Z',
      endTime: '2026-12-01T11:00:00Z',
      createdAt: '2026-11-18T10:00:00Z',
    },
    {
      id: 11,
      propertyId: 1,
      createdBy: 101,
      startTime: '2026-12-01T11:00:00Z',
      endTime: '2026-12-01T12:00:00Z',
      createdAt: '2026-11-18T10:00:00Z',
    },
  ];

  const createdViewing: Viewing = {
    id: 99,
    slotId: 10,
    propertyId: 1,
    buyerId: 301,
    status: 'REQUESTED',
    notes: 'Call me 10 mins before',
    createdAt: '2026-11-19T12:00:00Z',
    updatedAt: '2026-11-19T12:00:00Z',
  };

  beforeEach(async () => {
    slotServiceSpy = {
      getSlots: vi.fn().mockReturnValue(of(sampleSlots)),
    };
    viewingServiceSpy = {
      requestViewing: vi.fn().mockReturnValue(of(createdViewing)),
    };

    await TestBed.configureTestingModule({
      imports: [
        RequestViewingFormComponent,
        TranslocoTestingModule.forRoot({
          langs: TEST_TRANSLATIONS,
          translocoConfig: { availableLangs: ['en', 'ar'], defaultLang: 'en' },
        }),
      ],
      providers: [
        provideRouter([]),
        { provide: ViewingSlotService, useValue: slotServiceSpy },
        { provide: ViewingService, useValue: viewingServiceSpy },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(RequestViewingFormComponent);
    component = fixture.componentInstance;
    fixture.componentRef.setInput('propertyId', 1);
    fixture.detectChanges();
  });

  it('starts empty without loading property 1 when no propertyId input or route param is given', () => {
    slotServiceSpy.getSlots.mockClear();
    const standaloneFixture = TestBed.createComponent(RequestViewingFormComponent);
    standaloneFixture.detectChanges();

    expect(slotServiceSpy.getSlots).not.toHaveBeenCalled();
    expect(
      standaloneFixture.nativeElement.querySelector('[data-testid="select-property-prompt"]'),
    ).toBeTruthy();
  });

  it('loads and displays available viewing slots on init when propertyId is provided', () => {
    expect(slotServiceSpy.getSlots).toHaveBeenCalledWith(1);
    const el: HTMLElement = fixture.nativeElement;
    expect(el.querySelector('[data-testid="slot-option-10"]')).toBeTruthy();
    expect(el.querySelector('[data-testid="slot-option-11"]')).toBeTruthy();
  });

  it('shows validation error and does not call API when submitted without selecting a slot', () => {
    component.onSubmit();
    fixture.detectChanges();

    expect(viewingServiceSpy.requestViewing).not.toHaveBeenCalled();
    const el: HTMLElement = fixture.nativeElement;
    expect(el.querySelector('[data-testid="slot-required-error"]')?.textContent).toContain(
      'Please select a viewing time slot.',
    );
  });

  it('submits selected slot and notes and renders success banner on 201 Created', () => {
    component.requestForm.patchValue({ slotId: 10, notes: '  Call me 10 mins before  ' });
    component.onSubmit();
    fixture.detectChanges();

    expect(viewingServiceSpy.requestViewing).toHaveBeenCalledWith({
      slotId: 10,
      notes: 'Call me 10 mins before',
    });
    const el: HTMLElement = fixture.nativeElement;
    expect(el.querySelector('[data-testid="booking-success"]')?.textContent).toContain(
      'Your viewing request has been submitted',
    );
  });

  it('displays conflict error banner and refreshes slots when backend returns 409 Conflict', () => {
    viewingServiceSpy.requestViewing.mockReturnValueOnce(
      throwError(() => new HttpErrorResponse({ status: 409, statusText: 'Conflict' })),
    );

    component.requestForm.patchValue({ slotId: 10 });
    component.onSubmit();
    fixture.detectChanges();

    const el: HTMLElement = fixture.nativeElement;
    expect(el.querySelector('[data-testid="api-error"]')?.textContent).toContain(
      'just booked by another user',
    );
    expect(slotServiceSpy.getSlots).toHaveBeenCalledTimes(2);
  });

  it('displays forbidden error banner when owner/agent tries to book own property (403)', () => {
    viewingServiceSpy.requestViewing.mockReturnValueOnce(
      throwError(() => new HttpErrorResponse({ status: 403, statusText: 'Forbidden' })),
    );

    component.requestForm.patchValue({ slotId: 10 });
    component.onSubmit();
    fixture.detectChanges();

    const el: HTMLElement = fixture.nativeElement;
    expect(el.querySelector('[data-testid="api-error"]')?.textContent).toContain(
      'cannot book a viewing on your own property',
    );
  });
});
