import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter, Router, ActivatedRoute } from '@angular/router';
import { TranslocoTestingModule } from '@jsverse/transloco';
import { of, throwError } from 'rxjs';
import { PropertyFormComponent } from './property-form.component';
import { PropertyService } from '../services/property.service';
import { LocationNode, PropertyResponse, PropertyTypeDto, AmenityDto } from '../models/property.model';

const TEST_TRANSLATIONS = {
  en: {
    PROPERTY: {
      FORM: {
        ADD_TITLE: 'Add New Property',
        ADD_SUBTITLE: 'List a new property for sale or rent.',
        EDIT_TITLE: 'Edit Property',
        EDIT_SUBTITLE: 'Update your property listing details.',
        CANCEL: 'Cancel',
        SECTION_BASIC: 'Basic Information',
        TITLE_LABEL: 'Property Title',
        DESCRIPTION_LABEL: 'Description',
        PURPOSE_LABEL: 'Purpose',
        TYPE_LABEL: 'Property Type',
        SELECT_TYPE: 'Select property type',
        SECTION_LOCATION: 'Location Details',
        SECTION_SPECS: 'Pricing & Specifications',
        PRICE_LABEL: 'Price',
        CURRENCY_LABEL: 'Currency',
        AREA_LABEL: 'Area',
        BEDROOMS_LABEL: 'Bedrooms',
        BATHROOMS_LABEL: 'Bathrooms',
        SECTION_AMENITIES: 'Amenities',
        CREATE_SUBMIT: 'Create Property',
        UPDATE_SUBMIT: 'Save Changes'
      },
      LOCATION: {
        GOVERNORATE: 'Governorate',
        SELECT_GOVERNORATE: 'Select Governorate',
        CITY: 'City',
        SELECT_CITY: 'Select City',
        DISTRICT: 'District',
        SELECT_DISTRICT: 'Select District'
      },
      PURPOSE: {
        SALE: 'For Sale',
        RENT: 'For Rent'
      },
      VALIDATION: {
        TITLE_REQUIRED: 'Title is required.',
        TITLE_MAXLENGTH: 'Title cannot exceed 150 characters.',
        TYPE_REQUIRED: 'Property type is required.',
        GOVERNORATE_REQUIRED: 'Governorate is required.',
        CITY_REQUIRED: 'City is required.',
        LOCATION_REQUIRED: 'District is required.',
        PRICE_REQUIRED: 'Price is required.',
        PRICE_POSITIVE: 'Price must be greater than 0.',
        AREA_REQUIRED: 'Area is required.',
        AREA_POSITIVE: 'Area must be greater than 0.',
        BEDROOMS_MIN: 'Bedrooms cannot be negative.',
        BEDROOMS_MAX: 'Bedrooms cannot exceed 50.',
        BATHROOMS_MIN: 'Bathrooms cannot be negative.',
        BATHROOMS_MAX: 'Bathrooms cannot exceed 50.'
      },
      ERRORS: {
        SAVE_FAILED: 'Could not save property. Please try again.',
        LOAD_FAILED: 'Could not load property details.'
      }
    }
  },
  ar: {
    PROPERTY: {
      FORM: {
        ADD_TITLE: 'إضافة عقار جديد',
        ADD_SUBTITLE: 'أضف عقاراً جديداً للبيع أو الإيجار.',
        EDIT_TITLE: 'تعديل العقار',
        EDIT_SUBTITLE: 'تحديث بيانات العقار.',
        CANCEL: 'إلغاء',
        SECTION_BASIC: 'البيانات الأساسية',
        TITLE_LABEL: 'عنوان الإعلان',
        DESCRIPTION_LABEL: 'الوصف',
        PURPOSE_LABEL: 'الغرض',
        TYPE_LABEL: 'نوع العقار',
        SELECT_TYPE: 'اختر نوع العقار',
        SECTION_LOCATION: 'بيانات الموقع',
        SECTION_SPECS: 'السعر والمواصفات',
        PRICE_LABEL: 'السعر',
        CURRENCY_LABEL: 'العملة',
        AREA_LABEL: 'المساحة',
        BEDROOMS_LABEL: 'غرف النوم',
        BATHROOMS_LABEL: 'الحمامات',
        SECTION_AMENITIES: 'المميزات والمرافق',
        CREATE_SUBMIT: 'نشر العقار',
        UPDATE_SUBMIT: 'حفظ التعديلات'
      },
      LOCATION: {
        GOVERNORATE: 'المحافظة',
        SELECT_GOVERNORATE: 'اختر المحافظة',
        CITY: 'المدينة',
        SELECT_CITY: 'اختر المدينة',
        DISTRICT: 'المنطقة / الحي',
        SELECT_DISTRICT: 'اختر المنطقة'
      },
      PURPOSE: {
        SALE: 'للبيع',
        RENT: 'للإيجار'
      },
      VALIDATION: {
        TITLE_REQUIRED: 'عنوان الإعلان مطلوب.',
        TITLE_MAXLENGTH: 'يجب ألا يتجاوز العنوان 150 حرفاً.',
        TYPE_REQUIRED: 'نوع العقار مطلوب.',
        GOVERNORATE_REQUIRED: 'المحافظة مطلوبة.',
        CITY_REQUIRED: 'المدينة مطلوبة.',
        LOCATION_REQUIRED: 'المنطقة مطلوبة.',
        PRICE_REQUIRED: 'السعر مطلوب.',
        PRICE_POSITIVE: 'يجب أن يكون السعر أكبر من صفر.',
        AREA_REQUIRED: 'المساحة مطلوبة.',
        AREA_POSITIVE: 'يجب أن تكون المساحة أكبر من صفر.',
        BEDROOMS_MIN: 'عدد الغرف لا يمكن أن يكون سالباً.',
        BEDROOMS_MAX: 'عدد الغرف لا يمكن أن يتجاوز 50.',
        BATHROOMS_MIN: 'عدد الحمامات لا يمكن أن يكون سالباً.',
        BATHROOMS_MAX: 'عدد الحمامات لا يمكن أن يتجاوز 50.'
      },
      ERRORS: {
        SAVE_FAILED: 'تعذر حفظ العقار. يرجى المحاولة مرة أخرى.',
        LOAD_FAILED: 'تعذر تحميل بيانات العقار.'
      }
    }
  }
};

describe('PropertyFormComponent — Form 4 (SKN-21)', () => {
  let component: PropertyFormComponent;
  let fixture: ComponentFixture<PropertyFormComponent>;
  let propertyServiceSpy: {
    createProperty: ReturnType<typeof vi.fn>;
    updateProperty: ReturnType<typeof vi.fn>;
    getProperty: ReturnType<typeof vi.fn>;
    getMyProperties: ReturnType<typeof vi.fn>;
    publishProperty: ReturnType<typeof vi.fn>;
    archiveProperty: ReturnType<typeof vi.fn>;
    getLocations: ReturnType<typeof vi.fn>;
    getPropertyTypes: ReturnType<typeof vi.fn>;
    getAmenities: ReturnType<typeof vi.fn>;
  };
  let router: Router;

  const MOCK_LOCATIONS: LocationNode[] = [
    { id: 1, nameEn: 'Alexandria', nameAr: 'الإسكندرية', type: 'GOVERNORATE', parentId: null },
    { id: 2, nameEn: 'Alexandria City', nameAr: 'مدينة الإسكندرية', type: 'CITY', parentId: 1 },
    { id: 3, nameEn: 'Smouha', nameAr: 'سموحة', type: 'DISTRICT', parentId: 2 },
    { id: 4, nameEn: 'Kafr Abdo', nameAr: 'كفر عبده', type: 'DISTRICT', parentId: 2 },
    { id: 5, nameEn: 'Cairo', nameAr: 'القاهرة', type: 'GOVERNORATE', parentId: null },
    { id: 6, nameEn: 'New Cairo', nameAr: 'القاهرة الجديدة', type: 'CITY', parentId: 5 },
    { id: 7, nameEn: 'Fifth Settlement', nameAr: 'التجمع الخامس', type: 'DISTRICT', parentId: 6 }
  ];

  const MOCK_TYPES: PropertyTypeDto[] = [
    { id: 1, code: 'APARTMENT', nameEn: 'Apartment', nameAr: 'شقة' },
    { id: 2, code: 'VILLA', nameEn: 'Villa', nameAr: 'فيلا' }
  ];

  const MOCK_AMENITIES: AmenityDto[] = [
    { id: 1, nameEn: 'Elevator', nameAr: 'مصعد' },
    { id: 2, nameEn: 'Swimming Pool', nameAr: 'حمام سباحة' }
  ];

  const MOCK_PROPERTY_RESPONSE: PropertyResponse = {
    id: 10,
    ownerId: 1,
    title: 'Luxury Villa',
    description: 'Beautiful garden and pool',
    purpose: 'SALE',
    propertyTypeId: 2,
    locationId: 3,
    price: 5000000,
    currency: 'EGP',
    bedrooms: 4,
    bathrooms: 3,
    areaSqm: 350,
    amenityIds: [1, 2],
    status: 'DRAFT',
    createdAt: '2026-11-18T10:00:00Z',
    updatedAt: '2026-11-18T10:00:00Z'
  };

  beforeEach(async () => {
    propertyServiceSpy = {
      createProperty: vi.fn().mockReturnValue(of(MOCK_PROPERTY_RESPONSE)),
      updateProperty: vi.fn().mockReturnValue(of(MOCK_PROPERTY_RESPONSE)),
      getProperty: vi.fn().mockReturnValue(of(MOCK_PROPERTY_RESPONSE)),
      getMyProperties: vi.fn(),
      publishProperty: vi.fn(),
      archiveProperty: vi.fn(),
      getLocations: vi.fn().mockReturnValue(of(MOCK_LOCATIONS)),
      getPropertyTypes: vi.fn().mockReturnValue(of(MOCK_TYPES)),
      getAmenities: vi.fn().mockReturnValue(of(MOCK_AMENITIES))
    };

    await TestBed.configureTestingModule({
      imports: [
        PropertyFormComponent,
        TranslocoTestingModule.forRoot({
          langs: TEST_TRANSLATIONS,
          translocoConfig: {
            availableLangs: ['en', 'ar'],
            defaultLang: 'en'
          }
        })
      ],
      providers: [
        provideRouter([]),
        { provide: PropertyService, useValue: propertyServiceSpy },
        {
          provide: ActivatedRoute,
          useValue: {
            snapshot: {
              paramMap: {
                get: vi.fn().mockReturnValue(null)
              }
            }
          }
        }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(PropertyFormComponent);
    component = fixture.componentInstance;
    router = TestBed.inject(Router);
    fixture.detectChanges();
  });

  it('should load reference data (locations, property types, amenities) on init', () => {
    expect(propertyServiceSpy.getLocations).toHaveBeenCalled();
    expect(propertyServiceSpy.getPropertyTypes).toHaveBeenCalled();
    expect(propertyServiceSpy.getAmenities).toHaveBeenCalled();
    expect(component.governorates().length).toBe(2);
  });

  it('should filter cities when governorate is selected and reset downstream dropdowns', () => {
    component.onGovernorateChange(1);
    fixture.detectChanges();

    expect(component.cities().length).toBe(1);
    expect(component.cities()[0].id).toBe(2);
    expect(component.form.get('cityId')?.value).toBeNull();
    expect(component.form.get('locationId')?.value).toBeNull();

    component.onCityChange(2);
    fixture.detectChanges();

    expect(component.districts().length).toBe(2);
    expect(component.districts().map((d) => d.nameEn)).toContain('Smouha');
  });

  it('should validate required fields and display validation errors when touched', () => {
    component.form.controls['title'].setValue('');
    component.form.controls['title'].markAsTouched();
    component.form.controls['price'].setValue(null);
    component.form.controls['price'].markAsTouched();
    fixture.detectChanges();

    expect(component.form.invalid).toBe(true);
    const titleError = fixture.nativeElement.querySelector('[data-testid="error-title-required"]');
    expect(titleError).toBeTruthy();
    expect(titleError.textContent).toContain('Title is required.');

    const priceError = fixture.nativeElement.querySelector('[data-testid="error-price-required"]');
    expect(priceError).toBeTruthy();
    expect(priceError.textContent).toContain('Price is required.');
  });

  it('should enforce numeric constraints (bedrooms and bathrooms max 50, price positive)', () => {
    component.form.controls['bedrooms'].setValue(55);
    component.form.controls['bedrooms'].markAsTouched();
    component.form.controls['price'].setValue(-500);
    component.form.controls['price'].markAsTouched();
    fixture.detectChanges();

    expect(component.form.controls['bedrooms'].errors?.['max']).toBeTruthy();
    expect(component.form.controls['price'].errors?.['min']).toBeTruthy();

    const bedError = fixture.nativeElement.querySelector('[data-testid="error-bedrooms-max"]');
    expect(bedError).toBeTruthy();
    expect(bedError.textContent).toContain('Bedrooms cannot exceed 50.');
  });

  it('should toggle amenity selection when checkbox is clicked', () => {
    component.toggleAmenity(1);
    expect(component.form.get('amenityIds')?.value).toEqual([1]);

    component.toggleAmenity(2);
    expect(component.form.get('amenityIds')?.value).toEqual([1, 2]);

    component.toggleAmenity(1);
    expect(component.form.get('amenityIds')?.value).toEqual([2]);
  });

  it('should submit valid form in create mode and navigate to /properties/mine', () => {
    const navigateSpy = vi.spyOn(router, 'navigate');

    component.form.patchValue({
      title: 'Stunning Villa in Smouha',
      description: 'Luxury finishing',
      purpose: 'SALE',
      propertyTypeId: 2,
      governorateId: 1,
      cityId: 2,
      locationId: 3,
      price: 4500000,
      currency: 'EGP',
      bedrooms: 4,
      bathrooms: 3,
      areaSqm: 280,
      amenityIds: [1]
    });

    component.onSubmit();
    fixture.detectChanges();

    expect(propertyServiceSpy.createProperty).toHaveBeenCalledTimes(1);
    expect(propertyServiceSpy.createProperty).toHaveBeenCalledWith(
      expect.objectContaining({
        title: 'Stunning Villa in Smouha',
        price: 4500000,
        locationId: 3
      })
    );
    expect(navigateSpy).toHaveBeenCalledWith(['/properties/mine']);
  });

  it('should populate form and resolve cascading location hierarchy in edit mode', () => {
    component.locations.set(MOCK_LOCATIONS);
    component.loadProperty(10);
    fixture.detectChanges();

    expect(propertyServiceSpy.getProperty).toHaveBeenCalledWith(10);
    expect(component.selectedGovernorateId()).toBe(1);
    expect(component.selectedCityId()).toBe(2);
    expect(component.form.get('title')?.value).toBe('Luxury Villa');
    expect(component.form.get('locationId')?.value).toBe(3);
  });

  it('should submit updated data via updateProperty in edit mode', () => {
    const navigateSpy = vi.spyOn(router, 'navigate');
    component.isEditMode.set(true);
    component.propertyId.set(10);

    component.form.patchValue({
      title: 'Updated Villa',
      description: 'Updated desc',
      purpose: 'SALE',
      propertyTypeId: 2,
      governorateId: 1,
      cityId: 2,
      locationId: 3,
      price: 5200000,
      currency: 'EGP',
      bedrooms: 4,
      bathrooms: 3,
      areaSqm: 350,
      amenityIds: [1, 2]
    });

    component.onSubmit();
    fixture.detectChanges();

    expect(propertyServiceSpy.updateProperty).toHaveBeenCalledWith(
      10,
      expect.objectContaining({ title: 'Updated Villa', price: 5200000 })
    );
    expect(navigateSpy).toHaveBeenCalledWith(['/properties/mine']);
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
