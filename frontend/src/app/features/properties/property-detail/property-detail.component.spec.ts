import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute, provideRouter } from '@angular/router';
import { TranslocoTestingModule } from '@jsverse/transloco';
import { of } from 'rxjs';
import { LanguageService } from '../../../core/services/language.service';
import { TokenStorageService } from '../../../core/services/token-storage.service';
import { ViewingSlotService } from '../../viewings/services/viewing-slot.service';
import { ViewingService } from '../../viewings/services/viewing.service';
import { PropertyResponse } from '../models/property.model';
import { PropertyImageService } from '../services/property-image.service';
import { PropertyService } from '../services/property.service';
import { PropertyDetailComponent } from './property-detail.component';

function createJwt(payload: Record<string, unknown>): string {
  const header = btoa(JSON.stringify({ alg: 'HS256', typ: 'JWT' }));
  const body = btoa(JSON.stringify(payload));
  return `${header}.${body}.sig`;
}

const MOCK_PROPERTY: PropertyResponse = {
  id: 42,
  ownerId: 10,
  title: 'Penthouse in Fifth Settlement',
  description: 'Panoramic view over New Cairo',
  purpose: 'SALE',
  propertyTypeId: 1,
  locationId: 10,
  price: 7500000,
  currency: 'EGP',
  bedrooms: 4,
  bathrooms: 3,
  areaSqm: 260,
  amenityIds: [1, 3],
  status: 'PUBLISHED',
  createdAt: '2026-11-18T10:00:00Z',
};

describe('PropertyDetailComponent', () => {
  let fixture: ComponentFixture<PropertyDetailComponent>;
  let component: PropertyDetailComponent;
  let propertyServiceSpy: {
    getProperty: ReturnType<typeof vi.fn>;
    getLocations: ReturnType<typeof vi.fn>;
    getPropertyTypes: ReturnType<typeof vi.fn>;
    getAmenities: ReturnType<typeof vi.fn>;
    publishProperty: ReturnType<typeof vi.fn>;
    archiveProperty: ReturnType<typeof vi.fn>;
  };
  let imageServiceSpy: { list: ReturnType<typeof vi.fn> };
  let slotServiceSpy: { getSlots: ReturnType<typeof vi.fn> };
  let viewingServiceSpy: { requestViewing: ReturnType<typeof vi.fn> };
  let tokenStorage: TokenStorageService;

  beforeEach(async () => {
    localStorage.clear();
    propertyServiceSpy = {
      getProperty: vi.fn().mockReturnValue(of(MOCK_PROPERTY)),
      getLocations: vi.fn().mockReturnValue(
        of([
          { id: 1, nameEn: 'Cairo', nameAr: 'القاهرة', type: 'GOVERNORATE', parentId: null },
          { id: 4, nameEn: 'New Cairo', nameAr: 'القاهرة الجديدة', type: 'CITY', parentId: 1 },
          { id: 10, nameEn: 'Fifth Settlement', nameAr: 'التجمع الخامس', type: 'DISTRICT', parentId: 4 },
        ]),
      ),
      getPropertyTypes: vi
        .fn()
        .mockReturnValue(of([{ id: 1, code: 'APARTMENT', nameEn: 'Apartment', nameAr: 'شقة' }])),
      getAmenities: vi.fn().mockReturnValue(
        of([
          { id: 1, nameEn: 'Air conditioning', nameAr: 'تكييف' },
          { id: 3, nameEn: 'Elevator', nameAr: 'مصعد' },
        ]),
      ),
      publishProperty: vi.fn().mockReturnValue(of({ ...MOCK_PROPERTY, status: 'PUBLISHED' })),
      archiveProperty: vi.fn().mockReturnValue(of({ ...MOCK_PROPERTY, status: 'ARCHIVED' })),
    };
    imageServiceSpy = {
      list: vi.fn().mockReturnValue(
        of([
          {
            id: 1,
            url: '/api/property-images/1-full.jpg',
            thumbnailUrl: '/api/property-images/1-thumb.jpg',
            mimeType: 'image/jpeg',
            fileSize: 12000,
            displayOrder: 0,
            primary: true,
            createdAt: '2026-11-18T10:00:00Z',
          },
        ]),
      ),
    };
    slotServiceSpy = {
      getSlots: vi.fn().mockReturnValue(
        of([
          {
            id: 77,
            propertyId: 42,
            createdBy: 10,
            startTime: '2026-12-15T10:00:00Z',
            endTime: '2026-12-15T11:00:00Z',
            createdAt: '2026-11-18T10:00:00Z',
          },
        ]),
      ),
    };
    viewingServiceSpy = {
      requestViewing: vi.fn().mockReturnValue(
        of({
          id: 500,
          slotId: 77,
          propertyId: 42,
          buyerId: 99,
          status: 'REQUESTED',
          notes: 'See you there',
          createdAt: '2026-11-20T10:00:00Z',
          updatedAt: '2026-11-20T10:00:00Z',
        }),
      ),
    };

    await TestBed.configureTestingModule({
      imports: [
        PropertyDetailComponent,
        TranslocoTestingModule.forRoot({
          langs: { en: {}, ar: {} },
          translocoConfig: { availableLangs: ['en', 'ar'], defaultLang: 'en' },
        }),
      ],
      providers: [
        provideRouter([]),
        LanguageService,
        TokenStorageService,
        { provide: PropertyService, useValue: propertyServiceSpy },
        { provide: PropertyImageService, useValue: imageServiceSpy },
        { provide: ViewingSlotService, useValue: slotServiceSpy },
        { provide: ViewingService, useValue: viewingServiceSpy },
        {
          provide: ActivatedRoute,
          useValue: {
            snapshot: {
              paramMap: {
                get: (key: string) => (key === 'id' ? '42' : null),
              },
            },
          },
        },
      ],
    }).compileComponents();

    tokenStorage = TestBed.inject(TokenStorageService);
    fixture = TestBed.createComponent(PropertyDetailComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  afterEach(() => {
    localStorage.clear();
  });

  it('loads property details, images, slots, and resolves location hierarchy path', () => {
    expect(propertyServiceSpy.getProperty).toHaveBeenCalledWith(42);
    expect(imageServiceSpy.list).toHaveBeenCalledWith(42);
    expect(slotServiceSpy.getSlots).toHaveBeenCalledWith(42);
    expect(component.property()?.title).toBe('Penthouse in Fifth Settlement');
    expect(component.locationPath()).toBe('Cairo › New Cairo › Fifth Settlement');
    expect(component.propertyAmenities().length).toBe(2);
    expect(
      fixture.nativeElement.querySelector('[data-testid="detail-title"]')?.textContent,
    ).toContain('Penthouse in Fifth Settlement');
  });

  it('books a viewing slot when authenticated', () => {
    tokenStorage.saveToken('jwt-token');
    tokenStorage.saveRole('BUYER_TENANT');
    component.bookingNotes.set('See you there');

    component.bookViewing();
    fixture.detectChanges();

    expect(viewingServiceSpy.requestViewing).toHaveBeenCalledWith({
      slotId: 77,
      notes: 'See you there',
    });
    expect(component.bookingSuccess()).toBe(true);
  });

  it('allows owner to archive and republish a property and hides Request Viewing panel for own property', () => {
    tokenStorage.saveRole('OWNER');
    tokenStorage.saveToken(createJwt({ sub: '10', roles: ['OWNER'] }));
    fixture.detectChanges();

    expect(fixture.nativeElement.querySelector('[data-testid="request-viewing-panel"]')).toBeNull();
    expect(fixture.nativeElement.querySelector('[data-testid="detail-book-btn"]')).toBeNull();
    expect(fixture.nativeElement.querySelector('[data-testid="owner-availability-panel"]')).toBeTruthy();

    const archiveBtn = fixture.nativeElement.querySelector(
      '[data-testid="detail-archive-btn"]',
    ) as HTMLButtonElement;
    expect(archiveBtn).toBeTruthy();
    archiveBtn.click();
    fixture.detectChanges();

    expect(propertyServiceSpy.archiveProperty).toHaveBeenCalledWith(42);
    expect(component.property()?.status).toBe('ARCHIVED');

    const publishBtn = fixture.nativeElement.querySelector(
      '[data-testid="detail-publish-btn"]',
    ) as HTMLButtonElement;
    expect(publishBtn).toBeTruthy();
    publishBtn.click();
    fixture.detectChanges();

    expect(propertyServiceSpy.publishProperty).toHaveBeenCalledWith(42);
    expect(component.property()?.status).toBe('PUBLISHED');
  });

  it('shows Request Viewing panel and hides owner actions for a different owner', () => {
    tokenStorage.saveRole('OWNER');
    tokenStorage.saveToken(createJwt({ sub: '99', roles: ['OWNER'] }));
    fixture.detectChanges();

    expect(fixture.nativeElement.querySelector('[data-testid="request-viewing-panel"]')).toBeTruthy();
    expect(fixture.nativeElement.querySelector('[data-testid="owner-availability-panel"]')).toBeNull();
    expect(fixture.nativeElement.querySelector('[data-testid="detail-edit-btn"]')).toBeNull();
    expect(fixture.nativeElement.querySelector('[data-testid="detail-archive-btn"]')).toBeNull();
  });

  it('uses strictly logical Tailwind CSS classes', () => {
    const allElements = fixture.nativeElement.querySelectorAll('*');
    allElements.forEach((el: Element) => {
      const cls = typeof el.className === 'string' ? el.className : el.getAttribute('class') || '';
      expect(cls).not.toMatch(
        /\b(ml-|mr-|pl-|pr-|text-left|text-right|left-|right-|border-l|border-r)/,
      );
    });
  });
});
