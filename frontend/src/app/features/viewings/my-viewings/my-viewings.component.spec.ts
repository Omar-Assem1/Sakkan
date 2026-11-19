import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpErrorResponse } from '@angular/common/http';
import { provideRouter } from '@angular/router';
import { TranslocoTestingModule } from '@jsverse/transloco';
import { of, throwError } from 'rxjs';
import { TokenStorageService } from '../../../core/services/token-storage.service';
import { Viewing, ViewingPage } from '../models/viewing.model';
import { ViewingService } from '../services/viewing.service';
import { MyViewingsComponent } from './my-viewings.component';

const TEST_TRANSLATIONS = {
  en: {
    VIEWINGS: {
      MY: {
        TITLE: 'My Viewings',
        SUBTITLE_BUYER: 'Track your requested viewing appointments and cancel if your plans change.',
        SUBTITLE_MANAGER: 'Review, confirm, complete, or cancel viewing requests on your properties.',
        MANAGE_AVAILABILITY: 'Manage Availability',
        BROWSE_PROPERTIES: 'Browse Properties',
        FILTER_ALL: 'All',
        LOADING: 'Loading viewings...',
        EMPTY: 'No viewing appointments match the current filter.',
        COL_PROPERTY: 'Property',
        COL_SLOT: 'Slot',
        COL_STATUS: 'Status',
        COL_NOTES: 'Notes',
        COL_CREATED: 'Requested At',
        COL_ACTIONS: 'Actions',
        NO_NOTES: '—',
        STATUS: {
          REQUESTED: 'Requested',
          CONFIRMED: 'Confirmed',
          CANCELLED: 'Cancelled',
          COMPLETED: 'Completed',
        },
        ACTIONS: {
          CONFIRM: 'Confirm',
          CANCEL: 'Cancel',
          COMPLETE: 'Complete',
        },
        PAGINATION: {
          PREVIOUS: 'Previous',
          NEXT: 'Next',
        },
        ERRORS: {
          LOAD_FAILED: 'Could not load your viewings. Please try again.',
          CONFLICT_409: 'This viewing can no longer be moved to that status.',
          FORBIDDEN_403: 'You do not have permission to perform this action on this viewing.',
          UNAUTHORIZED_401: 'Please sign in to view or manage your viewings.',
          GENERIC: 'Could not update the viewing status. Please try again.',
        },
      },
    },
  },
  ar: {
    VIEWINGS: {
      MY: {
        TITLE: 'معايناتي',
        SUBTITLE_BUYER: 'تابع مواعيد المعاينة التي طلبتها أو قم بإلغائها عند الحاجة.',
        SUBTITLE_MANAGER: 'راجع طلبات المعاينة لعقاراتك وقم بتأكيدها أو إتمامها أو إلغائها.',
        MANAGE_AVAILABILITY: 'إدارة المواعيد المتاحة',
        BROWSE_PROPERTIES: 'تصفح العقارات',
        FILTER_ALL: 'الكل',
        LOADING: 'جاري تحميل المعاينات...',
        EMPTY: 'لا توجد مواعيد معاينة تطابق الفلتر الحالي.',
        COL_PROPERTY: 'العقار',
        COL_SLOT: 'الموعد',
        COL_STATUS: 'الحالة',
        COL_NOTES: 'ملاحظات',
        COL_CREATED: 'تاريخ الطلب',
        COL_ACTIONS: 'الإجراءات',
        NO_NOTES: '—',
        STATUS: {
          REQUESTED: 'قيد الانتظار',
          CONFIRMED: 'مؤكد',
          CANCELLED: 'ملغي',
          COMPLETED: 'مكتمل',
        },
        ACTIONS: {
          CONFIRM: 'تأكيد',
          CANCEL: 'إلغاء',
          COMPLETE: 'إتمام',
        },
        PAGINATION: {
          PREVIOUS: 'السابق',
          NEXT: 'التالي',
        },
        ERRORS: {
          LOAD_FAILED: 'تعذر تحميل قائمة المعاينات. يرجى المحاولة مرة أخرى.',
          CONFLICT_409: 'لا يمكن تغيير حالة هذه المعاينة إلى الحالة المطلوبة.',
          FORBIDDEN_403: 'ليس لديك الصلاحية لتنفيذ هذا الإجراء على هذه المعاينة.',
          UNAUTHORIZED_401: 'يرجى تسجيل الدخول لعرض أو إدارة معايناتك.',
          GENERIC: 'تعذر تحديث حالة المعاينة. يرجى المحاولة مرة أخرى.',
        },
      },
    },
  },
};

describe('MyViewingsComponent — Listing 8 (SCRUM-104 / SKN-27)', () => {
  let fixture: ComponentFixture<MyViewingsComponent>;
  let component: MyViewingsComponent;
  let tokenStorage: TokenStorageService;
  let viewingServiceSpy: {
    getMyViewings: ReturnType<typeof vi.fn>;
    confirmViewing: ReturnType<typeof vi.fn>;
    cancelViewing: ReturnType<typeof vi.fn>;
    completeViewing: ReturnType<typeof vi.fn>;
  };

  const sampleViewings: Viewing[] = [
    {
      id: 1,
      slotId: 10,
      propertyId: 100,
      buyerId: 301,
      status: 'REQUESTED',
      notes: 'Afternoon preferred',
      createdAt: '2026-11-19T10:00:00Z',
      updatedAt: '2026-11-19T10:00:00Z',
      slotStartTime: '2027-02-10T10:00:00Z',
      slotEndTime: '2027-02-10T11:00:00Z',
      propertyTitle: 'Smouha Sea View Flat',
    },
    {
      id: 2,
      slotId: 11,
      propertyId: 100,
      buyerId: 302,
      status: 'CONFIRMED',
      notes: null,
      createdAt: '2026-11-19T11:00:00Z',
      updatedAt: '2026-11-19T11:30:00Z',
    },
    {
      id: 3,
      slotId: 12,
      propertyId: 100,
      buyerId: 303,
      status: 'COMPLETED',
      notes: null,
      createdAt: '2026-11-18T09:00:00Z',
      updatedAt: '2026-11-18T12:00:00Z',
    },
  ];

  const samplePage: ViewingPage = {
    content: sampleViewings,
    page: 0,
    size: 20,
    totalElements: 3,
    totalPages: 1,
  };

  async function setupWithRole(role: string) {
    viewingServiceSpy = {
      getMyViewings: vi.fn().mockReturnValue(of(samplePage)),
      confirmViewing: vi.fn().mockReturnValue(of({ ...sampleViewings[0], status: 'CONFIRMED' })),
      cancelViewing: vi.fn().mockReturnValue(of({ ...sampleViewings[0], status: 'CANCELLED' })),
      completeViewing: vi.fn().mockReturnValue(of({ ...sampleViewings[1], status: 'COMPLETED' })),
    };

    await TestBed.configureTestingModule({
      imports: [
        MyViewingsComponent,
        TranslocoTestingModule.forRoot({
          langs: TEST_TRANSLATIONS,
          translocoConfig: { availableLangs: ['en', 'ar'], defaultLang: 'en' },
        }),
      ],
      providers: [
        provideRouter([]),
        { provide: ViewingService, useValue: viewingServiceSpy },
      ],
    }).compileComponents();

    tokenStorage = TestBed.inject(TokenStorageService);
    tokenStorage.saveRole(role);

    fixture = TestBed.createComponent(MyViewingsComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  }

  afterEach(() => {
    tokenStorage?.clear();
  });

  it('shows Confirm/Cancel on REQUESTED, Complete/Cancel on CONFIRMED, and no buttons on COMPLETED for OWNER', async () => {
    await setupWithRole('OWNER');
    const el: HTMLElement = fixture.nativeElement;

    expect(el.querySelector('[data-testid="confirm-btn-1"]')).toBeTruthy();
    expect(el.querySelector('[data-testid="cancel-btn-1"]')).toBeTruthy();
    expect(el.querySelector('[data-testid="complete-btn-1"]')).toBeFalsy();

    expect(el.querySelector('[data-testid="complete-btn-2"]')).toBeTruthy();
    expect(el.querySelector('[data-testid="cancel-btn-2"]')).toBeTruthy();
    expect(el.querySelector('[data-testid="confirm-btn-2"]')).toBeFalsy();

    expect(el.querySelector('[data-testid="confirm-btn-3"]')).toBeFalsy();
    expect(el.querySelector('[data-testid="complete-btn-3"]')).toBeFalsy();
    expect(el.querySelector('[data-testid="cancel-btn-3"]')).toBeFalsy();
  });

  it('hides Confirm and Complete buttons for BUYER_TENANT and shows only Cancel on active viewings', async () => {
    await setupWithRole('BUYER_TENANT');
    const el: HTMLElement = fixture.nativeElement;

    expect(el.querySelector('[data-testid="confirm-btn-1"]')).toBeFalsy();
    expect(el.querySelector('[data-testid="cancel-btn-1"]')).toBeTruthy();
    expect(el.querySelector('[data-testid="complete-btn-2"]')).toBeFalsy();
    expect(el.querySelector('[data-testid="cancel-btn-2"]')).toBeTruthy();
    expect(el.querySelector('[data-testid="browse-properties-link"]')).toBeTruthy();
  });

  it('filters viewings by status when a status pill is selected', async () => {
    await setupWithRole('OWNER');

    component.selectStatus('CONFIRMED');
    fixture.detectChanges();

    expect(viewingServiceSpy.getMyViewings).toHaveBeenLastCalledWith('CONFIRMED', 0);
  });

  it('updates viewing status in-place when OWNER confirms a REQUESTED viewing', async () => {
    await setupWithRole('OWNER');

    component.confirm(sampleViewings[0]);
    fixture.detectChanges();

    expect(viewingServiceSpy.confirmViewing).toHaveBeenCalledWith(1);
    const badge = fixture.nativeElement.querySelector('[data-testid="viewing-status-1"]');
    expect(badge?.textContent).toContain('Confirmed');
  });

  it('displays property title and formatted appointment date/time instead of raw slot ID', async () => {
    await setupWithRole('BUYER_TENANT');
    const el: HTMLElement = fixture.nativeElement;

    expect(el.querySelector('[data-testid="viewing-property-1"]')?.textContent).toContain(
      'Smouha Sea View Flat',
    );
    const slotCell = el.querySelector('[data-testid="viewing-slot-1"]')?.textContent ?? '';
    expect(slotCell).toContain('2027');
    expect(slotCell).not.toContain('#10');
  });

  it('displays forbidden error banner when loading viewings returns 403 for disallowed roles', async () => {
    await setupWithRole('ADMIN');
    viewingServiceSpy.getMyViewings.mockReturnValueOnce(
      throwError(() => new HttpErrorResponse({ status: 403, statusText: 'Forbidden' })),
    );

    component.loadViewings();
    fixture.detectChanges();

    const alert = fixture.nativeElement.querySelector('[data-testid="my-viewings-error"]');
    expect(alert?.textContent).toContain('do not have permission');
  });

  it('displays localized 409 error banner when a transition fails with Conflict', async () => {
    await setupWithRole('OWNER');
    viewingServiceSpy.confirmViewing.mockReturnValueOnce(
      throwError(() => new HttpErrorResponse({ status: 409, statusText: 'Conflict' })),
    );

    component.confirm(sampleViewings[0]);
    fixture.detectChanges();

    const alert = fixture.nativeElement.querySelector('[data-testid="my-viewings-error"]');
    expect(alert?.textContent).toContain('can no longer be moved to that status');
  });
});
