import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';
import { TranslocoTestingModule } from '@jsverse/transloco';
import { of, throwError } from 'rxjs';
import { MyPropertiesComponent } from './my-properties.component';
import { PropertyService } from '../services/property.service';
import { PageResponse, PropertyResponse } from '../models/property.model';

const TEST_TRANSLATIONS = {
  en: {
    MY_PROPERTIES: {
      TITLE: 'My Properties',
      SUBTITLE: 'Manage your property listings and status.',
      ADD_BUTTON: 'Add New Property',
      BEDS: 'Beds',
      BATHS: 'Baths',
      TABLE: {
        TITLE: 'Property',
        PURPOSE: 'Purpose',
        PRICE: 'Price',
        STATUS: 'Status',
        ACTIONS: 'Actions'
      },
      ACTIONS: {
        EDIT: 'Edit',
        PUBLISH: 'Publish',
        ARCHIVE: 'Archive'
      },
      EMPTY: {
        TITLE: 'No properties found',
        DESCRIPTION: 'You have not added any property listings yet.'
      },
      ERRORS: {
        LOAD_FAILED: 'Could not load your properties. Please try again.',
        ACTION_FAILED: 'Could not perform this action. Please try again.'
      }
    },
    PROPERTY: {
      PURPOSE: {
        SALE: 'For Sale',
        RENT: 'For Rent'
      },
      STATUS: {
        DRAFT: 'Draft',
        PUBLISHED: 'Published',
        RENTED: 'Rented',
        SOLD: 'Sold',
        ARCHIVED: 'Archived'
      }
    }
  },
  ar: {
    MY_PROPERTIES: {
      TITLE: 'عقاراتي',
      SUBTITLE: 'إدارة إعلاناتك وحالتها.',
      ADD_BUTTON: 'إضافة عقار جديد',
      BEDS: 'غرف',
      BATHS: 'حمامات',
      TABLE: {
        TITLE: 'العقار',
        PURPOSE: 'الغرض',
        PRICE: 'السعر',
        STATUS: 'الحالة',
        ACTIONS: 'الإجراءات'
      },
      ACTIONS: {
        EDIT: 'تعديل',
        PUBLISH: 'نشر',
        ARCHIVE: 'أرشفة'
      },
      EMPTY: {
        TITLE: 'لا توجد عقارات',
        DESCRIPTION: 'لم تقم بإضافة أي عقارات حتى الآن.'
      },
      ERRORS: {
        LOAD_FAILED: 'تعذر تحميل عقاراتك. يرجى المحاولة مرة أخرى.',
        ACTION_FAILED: 'تعذر تنفيذ هذا الإجراء.'
      }
    },
    PROPERTY: {
      PURPOSE: {
        SALE: 'للبيع',
        RENT: 'للإيجار'
      },
      STATUS: {
        DRAFT: 'مسودة',
        PUBLISHED: 'منشور',
        RENTED: 'مؤجر',
        SOLD: 'مباع',
        ARCHIVED: 'مؤرشف'
      }
    }
  }
};

describe('MyPropertiesComponent — Form 5 (SKN-21)', () => {
  let component: MyPropertiesComponent;
  let fixture: ComponentFixture<MyPropertiesComponent>;
  let propertyServiceSpy: {
    getMyProperties: ReturnType<typeof vi.fn>;
    publishProperty: ReturnType<typeof vi.fn>;
    archiveProperty: ReturnType<typeof vi.fn>;
  };
  let router: Router;

  const MOCK_PROPERTIES: PropertyResponse[] = [
    {
      id: 1,
      ownerId: 10,
      title: 'Cozy Apartment in Smouha',
      description: 'Close to train station',
      purpose: 'RENT',
      propertyTypeId: 1,
      locationId: 3,
      price: 15000,
      currency: 'EGP',
      bedrooms: 2,
      bathrooms: 1,
      areaSqm: 110,
      amenityIds: [1],
      status: 'DRAFT',
      createdAt: '2026-11-18T10:00:00Z'
    },
    {
      id: 2,
      ownerId: 10,
      title: 'Spacious Villa in Kafr Abdo',
      description: 'Garden view',
      purpose: 'SALE',
      propertyTypeId: 2,
      locationId: 4,
      price: 8000000,
      currency: 'EGP',
      bedrooms: 5,
      bathrooms: 4,
      areaSqm: 400,
      amenityIds: [1, 2],
      status: 'PUBLISHED',
      createdAt: '2026-11-17T10:00:00Z'
    }
  ];

  const MOCK_PAGE_RESPONSE: PageResponse<PropertyResponse> = {
    content: MOCK_PROPERTIES,
    page: 0,
    size: 10,
    totalElements: 2,
    totalPages: 1
  };

  beforeEach(async () => {
    propertyServiceSpy = {
      getMyProperties: vi.fn().mockReturnValue(of(MOCK_PAGE_RESPONSE)),
      publishProperty: vi.fn(),
      archiveProperty: vi.fn()
    };

    await TestBed.configureTestingModule({
      imports: [
        MyPropertiesComponent,
        TranslocoTestingModule.forRoot({
          langs: TEST_TRANSLATIONS,
          translocoConfig: {
            availableLangs: ['en', 'ar'],
            defaultLang: 'en'
          }
        })
      ],
      providers: [provideRouter([]), { provide: PropertyService, useValue: propertyServiceSpy }]
    }).compileComponents();

    fixture = TestBed.createComponent(MyPropertiesComponent);
    component = fixture.componentInstance;
    router = TestBed.inject(Router);
    fixture.detectChanges();
  });

  it('should fetch user properties via getMyProperties and render rows on init', () => {
    expect(propertyServiceSpy.getMyProperties).toHaveBeenCalledWith(0, 10);
    const rows = fixture.nativeElement.querySelectorAll('[data-testid="property-row"]');
    expect(rows.length).toBe(2);

    const titleEl = fixture.nativeElement.querySelector('[data-testid="property-title"]');
    expect(titleEl.textContent).toContain('Cozy Apartment in Smouha');
  });

  it('should render correct status badges for DRAFT and PUBLISHED', () => {
    const badges = fixture.nativeElement.querySelectorAll('[data-testid="status-badge"]');
    expect(badges.length).toBe(2);
    expect(badges[0].textContent).toContain('Draft');
    expect(badges[1].textContent).toContain('Published');
  });

  it('should call publishProperty and update property status when publish is clicked', () => {
    const publishedProp: PropertyResponse = { ...MOCK_PROPERTIES[0], status: 'PUBLISHED' };
    propertyServiceSpy.publishProperty.mockReturnValue(of(publishedProp));

    const publishBtn = fixture.nativeElement.querySelector(
      '[data-testid="action-publish"]'
    ) as HTMLButtonElement;
    expect(publishBtn).toBeTruthy();

    publishBtn.click();
    fixture.detectChanges();

    expect(propertyServiceSpy.publishProperty).toHaveBeenCalledWith(1);
    expect(component.properties()[0].status).toBe('PUBLISHED');
  });

  it('should call archiveProperty and update status when archive is clicked on published property', () => {
    const archivedProp: PropertyResponse = { ...MOCK_PROPERTIES[1], status: 'ARCHIVED' };
    propertyServiceSpy.archiveProperty.mockReturnValue(of(archivedProp));

    const archiveBtn = fixture.nativeElement.querySelector(
      '[data-testid="action-archive"]'
    ) as HTMLButtonElement;
    expect(archiveBtn).toBeTruthy();

    archiveBtn.click();
    fixture.detectChanges();

    expect(propertyServiceSpy.archiveProperty).toHaveBeenCalledWith(2);
    expect(component.properties()[1].status).toBe('ARCHIVED');
  });

  it('should navigate to edit page when edit button is clicked', () => {
    const navigateSpy = vi.spyOn(router, 'navigate');

    const editBtns = fixture.nativeElement.querySelectorAll('[data-testid="action-edit"]');
    expect(editBtns.length).toBeGreaterThan(0);

    editBtns[0].click();
    expect(navigateSpy).toHaveBeenCalledWith(['/properties', 1, 'edit']);
  });

  it('should display empty state when user has no properties', () => {
    const emptyPage: PageResponse<PropertyResponse> = {
      content: [],
      page: 0,
      size: 10,
      totalElements: 0,
      totalPages: 0
    };
    propertyServiceSpy.getMyProperties.mockReturnValue(of(emptyPage));

    component.loadMyProperties(0);
    fixture.detectChanges();

    const emptyState = fixture.nativeElement.querySelector('[data-testid="empty-state"]');
    expect(emptyState).toBeTruthy();
    expect(emptyState.textContent).toContain('No properties found');
  });

  it('should display error message when loading properties fails', () => {
    propertyServiceSpy.getMyProperties.mockReturnValue(throwError(() => new Error('Server error')));

    component.loadMyProperties(0);
    fixture.detectChanges();

    const errorEl = fixture.nativeElement.querySelector('[data-testid="api-error"]');
    expect(errorEl).toBeTruthy();
    expect(errorEl.textContent).toContain('Could not load your properties.');
  });

  it('should contain strictly logical Tailwind CSS classes and no physical directional classes', () => {
    const allElements = fixture.nativeElement.querySelectorAll('*');
    allElements.forEach((el: Element) => {
      const cls = typeof el.className === 'string' ? el.className : el.getAttribute('class') || '';
      expect(cls).not.toMatch(
        /\b(ml-|mr-|pl-|pr-|text-left|text-right|left-|right-|border-l|border-r)\b/
      );
    });
  });
});
