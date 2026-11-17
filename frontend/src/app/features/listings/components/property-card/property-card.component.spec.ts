import { ComponentFixture, TestBed } from '@angular/core/testing';
import { TranslocoTestingModule } from '@jsverse/transloco';
import { PropertyCardComponent } from './property-card.component';
import { PropertySummaryDto } from '../../models/property-browse.model';

describe('PropertyCardComponent', () => {
  let component: PropertyCardComponent;
  let fixture: ComponentFixture<PropertyCardComponent>;

  const mockProperty: PropertySummaryDto = {
    id: 1,
    title: 'Sunny Smouha Apartment',
    description: 'Spacious apartment near Green Plaza with balcony',
    purpose: 'SALE',
    propertyTypeId: 1,
    locationId: 10,
    price: 3200000,
    currency: 'EGP',
    bedrooms: 3,
    bathrooms: 2,
    areaSqm: 160,
    amenityIds: [1, 2],
    status: 'PUBLISHED',
    createdAt: '2026-11-17T12:00:00Z',
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [
        PropertyCardComponent,
        TranslocoTestingModule.forRoot({
          langs: {
            en: {
              LISTINGS: {
                CARD: {
                  FOR_SALE: 'For Sale',
                  FOR_RENT: 'For Rent',
                  NO_IMAGE: 'No Image Available',
                  BEDS: 'Beds',
                  BATHS: 'Baths',
                  SQM: 'm²',
                },
              },
            },
            ar: {},
          },
          translocoConfig: {
            availableLangs: ['en', 'ar'],
            defaultLang: 'en',
          },
        }),
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(PropertyCardComponent);
    component = fixture.componentInstance;
    component.property = mockProperty;
    fixture.detectChanges();
  });

  it('should create and render property title and details', () => {
    expect(component).toBeTruthy();
    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.textContent).toContain('Sunny Smouha Apartment');
    expect(compiled.textContent).toContain('3,200,000');
    expect(compiled.textContent).toContain('EGP');
    expect(compiled.textContent).toContain('3 Beds');
    expect(compiled.textContent).toContain('2 Baths');
    expect(compiled.textContent).toContain('160 m²');
  });

  it('should render location name when provided', () => {
    fixture.componentRef.setInput('locationName', 'Alexandria, Smouha');
    fixture.detectChanges();
    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.textContent).toContain('Alexandria, Smouha');
  });

  it('should display FOR_RENT badge when purpose is RENT', () => {
    fixture.componentRef.setInput('property', { ...mockProperty, purpose: 'RENT' });
    fixture.detectChanges();
    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.textContent).toContain('For Rent');
  });

  it('should contain zero physical directional Tailwind classes in template', () => {
    const allElements = fixture.nativeElement.querySelectorAll('*');
    allElements.forEach((el: Element) => {
      const cls = typeof el.className === 'string' ? el.className : el.getAttribute('class') || '';
      expect(cls).not.toMatch(
        /\b(ml-|mr-|pl-|pr-|text-left|text-right|left-|right-|border-l|border-r)/,
      );
    });
  });
});
