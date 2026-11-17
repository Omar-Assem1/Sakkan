import { ComponentFixture, TestBed } from '@angular/core/testing';
import { TranslocoTestingModule } from '@jsverse/transloco';
import { PropertyFilterPanelComponent } from './property-filter-panel.component';
import { LanguageService } from '../../../../core/services/language.service';

describe('PropertyFilterPanelComponent', () => {
  let component: PropertyFilterPanelComponent;
  let fixture: ComponentFixture<PropertyFilterPanelComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [
        PropertyFilterPanelComponent,
        TranslocoTestingModule.forRoot({
          langs: {
            en: {
              LISTINGS: {
                FILTERS: {
                  TITLE: 'Filters',
                  CLEAR: 'Clear All',
                  PURPOSE: 'Purpose',
                  FOR_SALE: 'For Sale',
                  FOR_RENT: 'For Rent',
                  LOCATION: 'Location',
                  SELECT_GOV: 'Select Governorate',
                  SELECT_DISTRICT: 'Select District',
                  PRICE_RANGE: 'Price Range',
                  MIN_PRICE: 'Min Price',
                  MAX_PRICE: 'Max Price',
                  TYPE: 'Property Type',
                  ALL_TYPES: 'All Types',
                  BEDROOMS: 'Bedrooms',
                  AMENITIES: 'Amenities',
                  APPLY: 'Apply Filters',
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
      providers: [LanguageService],
    }).compileComponents();

    fixture = TestBed.createComponent(PropertyFilterPanelComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should toggle amenity inclusion in selectedAmenityIds signal', () => {
    component.toggleAmenity(1);
    expect(component.selectedAmenityIds()).toEqual([1]);

    component.toggleAmenity(2);
    expect(component.selectedAmenityIds()).toEqual([1, 2]);

    component.toggleAmenity(1);
    expect(component.selectedAmenityIds()).toEqual([2]);
  });

  it('should reset city and district when governorate selection changes', () => {
    component.selectedGovernorateId.set(1);
    component.selectedCityId.set(10);
    component.selectedDistrictId.set(100);

    component.onGovernorateChange(2);

    expect(component.selectedGovernorateId()).toBe(2);
    expect(component.selectedCityId()).toBeNull();
    expect(component.selectedDistrictId()).toBeNull();
  });

  it('should emit filterChange on applyFilters()', () => {
    const emitSpy = vi.spyOn(component.filterChange, 'emit');
    component.minPrice.set(500000);
    component.maxPrice.set(1500000);
    component.applyFilters();

    expect(emitSpy).toHaveBeenCalledWith(
      expect.objectContaining({
        minPrice: 500000,
        maxPrice: 1500000,
        page: 0,
      })
    );
  });

  it('should reset all filter signals and emit resetFilters on onReset()', () => {
    const resetSpy = vi.spyOn(component.resetFilters, 'emit');
    component.selectedGovernorateId.set(1);
    component.minPrice.set(500000);
    component.purpose.set('SALE');
    component.selectedAmenityIds.set([1, 2]);

    component.onReset();

    expect(component.selectedGovernorateId()).toBeNull();
    expect(component.minPrice()).toBeNull();
    expect(component.purpose()).toBeNull();
    expect(component.selectedAmenityIds()).toEqual([]);
    expect(resetSpy).toHaveBeenCalled();
  });

  it('should update signals on ngOnChanges when initialFilters changes', () => {
    component.locations = [
      { id: 1, nameEn: 'Alexandria', nameAr: 'الإسكندرية', type: 'GOVERNORATE', parentId: null },
      { id: 10, nameEn: 'Smouha', nameAr: 'سموحة', type: 'DISTRICT', parentId: 1 },
    ];
    component.initialFilters = {
      minPrice: 800000,
      maxPrice: 2000000,
      currency: 'USD',
      purpose: 'RENT',
      locationId: 10,
    };

    component.ngOnChanges({
      initialFilters: {
        currentValue: component.initialFilters,
        previousValue: {},
        firstChange: false,
        isFirstChange: () => false,
      },
    });

    expect(component.minPrice()).toBe(800000);
    expect(component.maxPrice()).toBe(2000000);
    expect(component.currency()).toBe('USD');
    expect(component.purpose()).toBe('RENT');
    expect(component.selectedDistrictId()).toBe(10);
    expect(component.selectedGovernorateId()).toBe(1);
  });

  it('should contain zero physical directional Tailwind classes in filter panel template', () => {
    const allElements = fixture.nativeElement.querySelectorAll('*');
    allElements.forEach((el: Element) => {
      const cls = typeof el.className === 'string' ? el.className : el.getAttribute('class') || '';
      expect(cls).not.toMatch(
        /\b(ml-|mr-|pl-|pr-|text-left|text-right|left-|right-|border-l|border-r)/,
      );
    });
  });
});
