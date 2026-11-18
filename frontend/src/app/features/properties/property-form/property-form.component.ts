import { ChangeDetectionStrategy, Component, OnInit, inject, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { TranslocoDirective } from '@jsverse/transloco';
import { PropertyService } from '../services/property.service';
import { AmenityDto, LocationNode, PropertyRequest, PropertyTypeDto } from '../models/property.model';

@Component({
  selector: 'app-property-form',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [CommonModule, ReactiveFormsModule, RouterModule, TranslocoDirective],
  templateUrl: './property-form.component.html'
})
export class PropertyFormComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly propertyService = inject(PropertyService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);

  isEditMode = signal<boolean>(false);
  propertyId = signal<number | null>(null);
  isLoading = signal<boolean>(false);
  errorMessage = signal<string | null>(null);

  locations = signal<LocationNode[]>([]);
  propertyTypes = signal<PropertyTypeDto[]>([]);
  amenities = signal<AmenityDto[]>([]);

  selectedGovernorateId = signal<number | null>(null);
  selectedCityId = signal<number | null>(null);

  governorates = computed(() => this.locations().filter((l) => l.type === 'GOVERNORATE'));
  cities = computed(() => {
    const govId = this.selectedGovernorateId();
    return govId ? this.locations().filter((l) => l.type === 'CITY' && l.parentId === govId) : [];
  });
  districts = computed(() => {
    const cityId = this.selectedCityId();
    return cityId ? this.locations().filter((l) => l.type === 'DISTRICT' && l.parentId === cityId) : [];
  });

  form: FormGroup = this.fb.group({
    title: ['', [Validators.required, Validators.maxLength(150)]],
    description: ['', [Validators.maxLength(5000)]],
    purpose: ['SALE', [Validators.required]],
    propertyTypeId: [null, [Validators.required]],
    governorateId: [null, [Validators.required]],
    cityId: [null, [Validators.required]],
    locationId: [null, [Validators.required]],
    price: [null, [Validators.required, Validators.min(0.01)]],
    currency: ['EGP', [Validators.required]],
    bedrooms: [1, [Validators.required, Validators.min(0), Validators.max(50)]],
    bathrooms: [1, [Validators.required, Validators.min(0), Validators.max(50)]],
    areaSqm: [null, [Validators.required, Validators.min(1)]],
    amenityIds: [[]]
  });

  ngOnInit(): void {
    this.loadReferenceData();
    const idParam = this.route.snapshot.paramMap.get('id');
    if (idParam) {
      this.isEditMode.set(true);
      this.propertyId.set(Number(idParam));
      this.loadProperty(Number(idParam));
    }
  }

  loadReferenceData(): void {
    this.propertyService.getLocations().subscribe((locs) => this.locations.set(locs));
    this.propertyService.getPropertyTypes().subscribe((types) => this.propertyTypes.set(types));
    this.propertyService.getAmenities().subscribe((ams) => this.amenities.set(ams));
  }

  loadProperty(id: number): void {
    this.isLoading.set(true);
    this.propertyService.getProperty(id).subscribe({
      next: (prop) => {
        this.isLoading.set(false);
        this.resolveHierarchyForLocation(prop.locationId);
        this.form.patchValue({
          title: prop.title,
          description: prop.description,
          purpose: prop.purpose,
          propertyTypeId: prop.propertyTypeId,
          locationId: prop.locationId,
          price: prop.price,
          currency: prop.currency,
          bedrooms: prop.bedrooms,
          bathrooms: prop.bathrooms,
          areaSqm: prop.areaSqm,
          amenityIds: prop.amenityIds || []
        });
      },
      error: () => {
        this.isLoading.set(false);
        this.errorMessage.set('PROPERTY.ERRORS.LOAD_FAILED');
      }
    });
  }

  resolveHierarchyForLocation(locationId: number): void {
    const loc = this.locations().find((l) => l.id === locationId);
    if (!loc) return;

    if (loc.type === 'DISTRICT') {
      const city = this.locations().find((l) => l.id === loc.parentId);
      if (city) {
        this.selectedGovernorateId.set(city.parentId);
        this.selectedCityId.set(city.id);
        this.form.patchValue({
          governorateId: city.parentId,
          cityId: city.id,
          locationId: loc.id
        });
      }
    }
  }

  onGovernorateChange(val: string | number | null): void {
    const govId = val ? Number(val) : null;
    this.selectedGovernorateId.set(govId);
    this.selectedCityId.set(null);
    this.form.patchValue({
      governorateId: govId,
      cityId: null,
      locationId: null
    });
  }

  onCityChange(val: string | number | null): void {
    const cityId = val ? Number(val) : null;
    this.selectedCityId.set(cityId);
    this.form.patchValue({
      cityId: cityId,
      locationId: null
    });
  }

  toggleAmenity(amenityId: number): void {
    const current: number[] = this.form.get('amenityIds')?.value || [];
    const updated = current.includes(amenityId)
      ? current.filter((id) => id !== amenityId)
      : [...current, amenityId];
    this.form.patchValue({ amenityIds: updated });
  }

  onSubmit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    const val = this.form.value;
    const request: PropertyRequest = {
      title: val.title,
      description: val.description,
      purpose: val.purpose,
      propertyTypeId: Number(val.propertyTypeId),
      locationId: Number(val.locationId),
      price: Number(val.price),
      currency: val.currency,
      bedrooms: Number(val.bedrooms),
      bathrooms: Number(val.bathrooms),
      areaSqm: Number(val.areaSqm),
      amenityIds: val.amenityIds || []
    };

    this.isLoading.set(true);
    if (this.isEditMode() && this.propertyId()) {
      this.propertyService.updateProperty(this.propertyId()!, request).subscribe({
        next: () => {
          this.isLoading.set(false);
          this.router.navigate(['/properties/mine']);
        },
        error: () => {
          this.isLoading.set(false);
          this.errorMessage.set('PROPERTY.ERRORS.SAVE_FAILED');
        }
      });
    } else {
      this.propertyService.createProperty(request).subscribe({
        next: () => {
          this.isLoading.set(false);
          this.router.navigate(['/properties/mine']);
        },
        error: () => {
          this.isLoading.set(false);
          this.errorMessage.set('PROPERTY.ERRORS.SAVE_FAILED');
        }
      });
    }
  }
}
