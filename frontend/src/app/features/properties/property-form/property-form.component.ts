import { ChangeDetectionStrategy, Component, OnInit, inject, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { TranslocoDirective } from '@jsverse/transloco';
import { LanguageService } from '../../../core/services/language.service';
import { PropertyService } from '../services/property.service';
import { PropertyImage, PropertyImageService } from '../services/property-image.service';
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
  private readonly imageService = inject(PropertyImageService);
  private readonly languageService = inject(LanguageService, { optional: true });

  readonly isRtl = computed(() => this.languageService?.currentLang() === 'ar');

  isEditMode = signal<boolean>(false);
  propertyId = signal<number | null>(null);
  isLoading = signal<boolean>(false);
  errorMessage = signal<string | null>(null);

  locations = signal<LocationNode[]>([]);
  propertyTypes = signal<PropertyTypeDto[]>([]);
  amenities = signal<AmenityDto[]>([]);
  selectedImages = signal<File[]>([]);
  imagePreviews = signal<string[]>([]);
  propertyImages = signal<PropertyImage[]>([]);
  uploadProgress = signal<number | null>(null);

  selectedGovernorateId = signal<number | null>(null);
  selectedCityId = signal<number | null>(null);
  selectedDistrictId = signal<number | null>(null);
  selectedCompoundId = signal<number | null>(null);

  governorates = computed(() => this.locations().filter((l) => l.type === 'GOVERNORATE'));
  cities = computed(() => {
    const govId = this.selectedGovernorateId();
    return govId ? this.locations().filter((l) => l.type === 'CITY' && l.parentId === govId) : [];
  });
  districts = computed(() => {
    const cityId = this.selectedCityId();
    return cityId ? this.locations().filter((l) => l.type === 'DISTRICT' && l.parentId === cityId) : [];
  });
  compounds = computed(() => {
    const distId = this.selectedDistrictId();
    return distId ? this.locations().filter((l) => l.type === 'COMPOUND' && l.parentId === distId) : [];
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
        this.imageService.list(id).subscribe(images => this.propertyImages.set(images));
      },
      error: () => {
        this.isLoading.set(false);
        this.errorMessage.set('PROPERTY.ERRORS.LOAD_FAILED');
      }
    });
  }

  resolveHierarchyForLocation(locationId: number): void {
    const all = this.locations();
    const loc = all.find((l) => l.id === locationId);
    if (!loc) return;

    if (loc.type === 'COMPOUND') {
      const district = all.find((l) => l.id === loc.parentId);
      const city = district ? all.find((l) => l.id === district.parentId) : undefined;
      if (district && city) {
        this.selectedGovernorateId.set(city.parentId);
        this.selectedCityId.set(city.id);
        this.selectedDistrictId.set(district.id);
        this.selectedCompoundId.set(loc.id);
        this.form.patchValue({
          governorateId: city.parentId,
          cityId: city.id,
          locationId: district.id
        });
      }
    } else if (loc.type === 'DISTRICT') {
      const city = all.find((l) => l.id === loc.parentId);
      if (city) {
        this.selectedGovernorateId.set(city.parentId);
        this.selectedCityId.set(city.id);
        this.selectedDistrictId.set(loc.id);
        this.selectedCompoundId.set(null);
        this.form.patchValue({
          governorateId: city.parentId,
          cityId: city.id,
          locationId: loc.id
        });
      }
    } else if (loc.type === 'CITY') {
      this.selectedGovernorateId.set(loc.parentId);
      this.selectedCityId.set(loc.id);
      this.selectedDistrictId.set(null);
      this.selectedCompoundId.set(null);
      this.form.patchValue({
        governorateId: loc.parentId,
        cityId: loc.id
      });
    } else if (loc.type === 'GOVERNORATE') {
      this.selectedGovernorateId.set(loc.id);
      this.selectedCityId.set(null);
      this.selectedDistrictId.set(null);
      this.selectedCompoundId.set(null);
      this.form.patchValue({
        governorateId: loc.id
      });
    }
  }

  onGovernorateChange(val: string | number | null): void {
    const govId = val ? Number(val) : null;
    this.selectedGovernorateId.set(govId);
    this.selectedCityId.set(null);
    this.selectedDistrictId.set(null);
    this.selectedCompoundId.set(null);
    this.form.patchValue({
      governorateId: govId,
      cityId: null,
      locationId: null
    });
  }

  onCityChange(val: string | number | null): void {
    const cityId = val ? Number(val) : null;
    this.selectedCityId.set(cityId);
    this.selectedDistrictId.set(null);
    this.selectedCompoundId.set(null);
    this.form.patchValue({
      cityId: cityId,
      locationId: null
    });
  }

  onDistrictChange(val: string | number | null): void {
    const distId = val ? Number(val) : null;
    this.selectedDistrictId.set(distId);
    this.selectedCompoundId.set(null);
  }

  onCompoundChange(val: string | number | null): void {
    const compId = val ? Number(val) : null;
    this.selectedCompoundId.set(compId);
  }

  toggleAmenity(amenityId: number): void {
    const current: number[] = this.form.get('amenityIds')?.value || [];
    const updated = current.includes(amenityId)
      ? current.filter((id) => id !== amenityId)
      : [...current, amenityId];
    this.form.patchValue({ amenityIds: updated });
  }

  async onImagesSelected(event: Event): Promise<void> {
    const input = event.target as HTMLInputElement;
    const files = Array.from(input.files ?? []);
    input.value = '';
    if (this.selectedImages().length + files.length + this.propertyImages().length > 10) {
      this.errorMessage.set('PROPERTY.IMAGES.MAX_COUNT'); return;
    }
    try {
      const compressed = await Promise.all(files.map(file => this.imageService.compress(file)));
      this.selectedImages.update(current => [...current, ...compressed]);
      this.imagePreviews.update(current => [...current, ...compressed.map(file => URL.createObjectURL(file))]);
      this.errorMessage.set(null);
    } catch (error) { this.errorMessage.set(error instanceof Error ? error.message : 'PROPERTY.IMAGES.COMPRESSION_FAILED'); }
  }

  removeSelectedImage(index: number): void {
    URL.revokeObjectURL(this.imagePreviews()[index]);
    this.selectedImages.update(files => files.filter((_, i) => i !== index));
    this.imagePreviews.update(urls => urls.filter((_, i) => i !== index));
  }

  deletePropertyImage(image: PropertyImage): void {
    const id = this.propertyId(); if (!id) return;
    this.imageService.remove(id, image.id).subscribe({
      next: () => this.propertyImages.update(items => items.filter(item => item.id !== image.id)),
      error: () => this.errorMessage.set('PROPERTY.ERRORS.SAVE_FAILED')
    });
  }

  makePrimaryImage(image: PropertyImage): void {
    const id = this.propertyId();
    if (!id || !this.imageService.reorder) return;
    const current = this.propertyImages();
    const reordered = [image, ...current.filter((item) => item.id !== image.id)];
    const ids = reordered.map((item) => item.id);
    this.imageService.reorder(id, ids).subscribe({
      next: () => {
        this.propertyImages.set(
          reordered.map((item, idx) => ({ ...item, primary: idx === 0, displayOrder: idx }))
        );
      },
      error: () => this.errorMessage.set('PROPERTY.ERRORS.SAVE_FAILED')
    });
  }

  onSubmit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    const val = this.form.value;
    const targetLocationId = this.selectedCompoundId() ?? Number(val.locationId);
    const request: PropertyRequest = {
      title: val.title,
      description: val.description,
      purpose: val.purpose,
      propertyTypeId: Number(val.propertyTypeId),
      locationId: targetLocationId,
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
        next: () => this.uploadPendingImages(this.propertyId()!),
        error: () => {
          this.isLoading.set(false);
          this.errorMessage.set('PROPERTY.ERRORS.SAVE_FAILED');
        }
      });
    } else {
      this.propertyService.createProperty(request).subscribe({
        next: property => {
          this.propertyId.set(property.id);
          this.isEditMode.set(true);
          this.uploadPendingImages(property.id);
        },
        error: () => {
          this.isLoading.set(false);
          this.errorMessage.set('PROPERTY.ERRORS.SAVE_FAILED');
        }
      });
    }
  }

  private uploadPendingImages(propertyId: number): void {
    const files = this.selectedImages();
    if (!files.length) { this.isLoading.set(false); this.router.navigate(['/properties/mine']); return; }
    this.imageService.upload(propertyId, files).subscribe({
      next: event => { if (event.type === 1 && event.total) this.uploadProgress.set(Math.round(100 * event.loaded / event.total)); },
      error: () => { this.isLoading.set(false); this.uploadProgress.set(null); this.errorMessage.set('PROPERTY.IMAGES.UPLOAD_FAILED'); },
      complete: () => { this.isLoading.set(false); this.uploadProgress.set(null); this.router.navigate(['/properties/mine']); }
    });
  }
}
