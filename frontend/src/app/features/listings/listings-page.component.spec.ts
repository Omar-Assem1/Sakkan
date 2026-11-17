import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { TranslocoTestingModule } from '@jsverse/transloco';
import { of, throwError } from 'rxjs';
import { ListingsPageComponent } from './listings-page.component';
import { PropertyBrowseService } from './services/property-browse.service';
import { LanguageService } from '../../core/services/language.service';
import { PageResponse, PropertySummaryDto } from './models/property-browse.model';

describe('ListingsPageComponent', () => {
  let component: ListingsPageComponent;
  let fixture: ComponentFixture<ListingsPageComponent>;
  let browseService: PropertyBrowseService;

  const mockResponse: PageResponse<PropertySummaryDto> = {
    content: [
      {
        id: 1,
        title: 'Modern Alexandria Apartment',
        description: 'Near sea',
        purpose: 'SALE',
        propertyTypeId: 1,
        locationId: 10,
        price: 2500000,
        currency: 'EGP',
        bedrooms: 3,
        bathrooms: 2,
        areaSqm: 145,
        amenityIds: [1, 2],
        status: 'PUBLISHED',
        createdAt: '2026-11-17T12:00:00Z',
      },
    ],
    page: 0,
    size: 12,
    totalElements: 1,
    totalPages: 1,
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [
        ListingsPageComponent,
        TranslocoTestingModule.forRoot({
          langs: {
            en: {
              LISTINGS: {
                TITLE: 'Property Listings',
                FOUND_COUNT: 'properties found',
                SORT: { LABEL: 'Sort by', NEWEST: 'Newest' },
                FILTERS: { TITLE: 'Filters', CLEAR: 'Clear All', APPLY: 'Apply Filters' },
                CARD: { FOR_SALE: 'For Sale', FOR_RENT: 'For Rent', NO_IMAGE: 'No Image', BEDS: 'Beds', BATHS: 'Baths', SQM: 'm²' },
                EMPTY: { TITLE: 'No properties found', DESCRIPTION: 'Adjust filters', CLEAR: 'Reset Filters' },
                ERROR: { TITLE: 'Error loading', DESCRIPTION: 'Failed to load', RETRY: 'Try Again' },
                PAGINATION: { PREVIOUS: 'Previous', NEXT: 'Next' },
              },
            },
            ar: {},
          },
          translocoConfig: { availableLangs: ['en', 'ar'], defaultLang: 'en' },
        }),
      ],
      providers: [
        provideRouter([]),
        LanguageService,
        {
          provide: PropertyBrowseService,
          useValue: {
            getProperties: () => of(mockResponse),
            getLocations: () => of([]),
            getPropertyTypes: () => of([]),
            getAmenities: () => of([]),
          },
        },
      ],
    }).compileComponents();

    browseService = TestBed.inject(PropertyBrowseService);
    fixture = TestBed.createComponent(ListingsPageComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should initialize and load properties into state signals', () => {
    expect(component.isLoading()).toBe(false);
    expect(component.properties().length).toBe(1);
    expect(component.totalElements()).toBe(1);
    expect(component.isEmpty()).toBe(false);
  });

  it('should toggle empty state when totalElements is 0', () => {
    vi.spyOn(browseService, 'getProperties').mockReturnValue(
      of({ content: [], page: 0, size: 12, totalElements: 0, totalPages: 0 })
    );

    component.fetchProperties();

    expect(component.properties().length).toBe(0);
    expect(component.isEmpty()).toBe(true);
  });

  it('should set error state on HTTP failure', () => {
    vi.spyOn(browseService, 'getProperties').mockReturnValue(
      throwError(() => new Error('Server down'))
    );

    component.fetchProperties();

    expect(component.hasError()).toBe(true);
    expect(component.isLoading()).toBe(false);
  });

  it('should contain zero physical directional Tailwind classes in listings page template', () => {
    const allElements = fixture.nativeElement.querySelectorAll('*');
    allElements.forEach((el: Element) => {
      const cls = typeof el.className === 'string' ? el.className : el.getAttribute('class') || '';
      expect(cls).not.toMatch(
        /\b(ml-|mr-|pl-|pr-|text-left|text-right|left-|right-|border-l|border-r)/,
      );
    });
  });
});
