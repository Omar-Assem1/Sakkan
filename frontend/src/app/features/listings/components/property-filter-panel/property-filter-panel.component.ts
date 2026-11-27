import {
  ChangeDetectionStrategy,
  Component,
  EventEmitter,
  Input,
  OnChanges,
  OnInit,
  Output,
  SimpleChanges,
  computed,
  inject,
  signal
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { TranslocoPipe } from '@jsverse/transloco';
import { LanguageService } from '../../../../core/services/language.service';
import {
  AmenityDto,
  LocationNode,
  PropertyFilterCriteria,
  PropertyPurpose,
  PropertyTypeDto
} from '../../models/property-browse.model';

@Component({
  selector: 'app-property-filter-panel',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [CommonModule, FormsModule, TranslocoPipe],
  templateUrl: './property-filter-panel.component.html'
})
export class PropertyFilterPanelComponent implements OnInit, OnChanges {
  private readonly languageService = inject(LanguageService);

  private readonly locationsState = signal<LocationNode[]>([]);

  @Input() initialFilters: PropertyFilterCriteria = {};
  @Input()
  set locations(value: LocationNode[]) {
    this.locationsState.set(value ?? []);
  }
  get locations(): LocationNode[] {
    return this.locationsState();
  }
  @Input() propertyTypes: PropertyTypeDto[] = [];
  @Input() amenities: AmenityDto[] = [];

  @Output() filterChange = new EventEmitter<PropertyFilterCriteria>();
  @Output() resetFilters = new EventEmitter<void>();

  selectedGovernorateId = signal<number | null>(null);
  selectedCityId = signal<number | null>(null);
  selectedDistrictId = signal<number | null>(null);
  selectedCompoundId = signal<number | null>(null);

  minPrice = signal<number | null>(null);
  maxPrice = signal<number | null>(null);
  currency = signal<'EGP' | 'USD'>('EGP');
  selectedPropertyTypeId = signal<number | null>(null);
  purpose = signal<PropertyPurpose | null>(null);
  minBedrooms = signal<number | null>(null);
  selectedAmenityIds = signal<number[]>([]);

  governorates = computed(() => this.locationsState().filter((l) => l.type === 'GOVERNORATE'));
  cities = computed(() => {
    const govId = this.selectedGovernorateId();
    return govId ? this.locationsState().filter((l) => l.type === 'CITY' && l.parentId === govId) : [];
  });
  districts = computed(() => {
    const all = this.locationsState();
    const cityId = this.selectedCityId();
    if (cityId) {
      return all.filter((l) => l.type === 'DISTRICT' && l.parentId === cityId);
    }
    const govId = this.selectedGovernorateId();
    if (!govId) {
      return [];
    }
    const cityIds = new Set(
      all.filter((l) => l.type === 'CITY' && l.parentId === govId).map((l) => l.id)
    );
    return all.filter(
      (l) => l.type === 'DISTRICT' && (l.parentId === govId || (l.parentId !== null && cityIds.has(l.parentId)))
    );
  });
  compounds = computed(() => {
    const distId = this.selectedDistrictId();
    return distId ? this.locationsState().filter((l) => l.type === 'COMPOUND' && l.parentId === distId) : [];
  });

  isRtl = computed(() => this.languageService.currentLang() === 'ar');

  ngOnInit(): void {
    this.syncFromFilters(this.initialFilters);
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['initialFilters']) {
      this.syncFromFilters(this.initialFilters);
    }
    if (changes['locations']) {
      this.resolveLocationHierarchy(this.initialFilters.locationId ?? null);
    }
  }

  private syncFromFilters(filters: PropertyFilterCriteria): void {
    this.minPrice.set(filters.minPrice ?? null);
    this.maxPrice.set(filters.maxPrice ?? null);
    this.currency.set(filters.currency ?? 'EGP');
    this.selectedPropertyTypeId.set(filters.propertyTypeId ?? null);
    this.purpose.set(filters.purpose ?? null);
    this.minBedrooms.set(filters.minBedrooms ?? null);
    this.selectedAmenityIds.set(filters.amenityIds ? [...filters.amenityIds] : []);
    this.resolveLocationHierarchy(filters.locationId ?? null);
  }

  private resolveLocationHierarchy(locationId: number | null): void {
    const all = this.locationsState();
    if (!locationId || all.length === 0) {
      if (!locationId) {
        this.selectedGovernorateId.set(null);
        this.selectedCityId.set(null);
        this.selectedDistrictId.set(null);
        this.selectedCompoundId.set(null);
      }
      return;
    }
    const loc = all.find((l) => l.id === locationId);
    if (loc) {
      if (loc.type === 'COMPOUND') {
        this.selectedCompoundId.set(loc.id);
        const district = all.find((l) => l.id === loc.parentId);
        const city = district ? all.find((l) => l.id === district.parentId) : undefined;
        this.selectedDistrictId.set(district?.id ?? null);
        this.selectedCityId.set(city?.id ?? null);
        this.selectedGovernorateId.set(city?.parentId ?? district?.parentId ?? null);
      } else if (loc.type === 'DISTRICT') {
        this.selectedCompoundId.set(null);
        this.selectedDistrictId.set(loc.id);
        const parent = all.find((l) => l.id === loc.parentId);
        if (parent?.type === 'CITY') {
          this.selectedCityId.set(parent.id);
          this.selectedGovernorateId.set(parent.parentId);
        } else {
          this.selectedCityId.set(null);
          if (loc.parentId) {
            this.selectedGovernorateId.set(loc.parentId);
          }
        }
      } else if (loc.type === 'CITY') {
        this.selectedCompoundId.set(null);
        this.selectedCityId.set(loc.id);
        this.selectedDistrictId.set(null);
        if (loc.parentId) {
          this.selectedGovernorateId.set(loc.parentId);
        }
      } else if (loc.type === 'GOVERNORATE') {
        this.selectedCompoundId.set(null);
        this.selectedGovernorateId.set(loc.id);
        this.selectedCityId.set(null);
        this.selectedDistrictId.set(null);
      }
    }
  }

  onGovernorateChange(idVal: string | number | null): void {
    const id = idVal ? Number(idVal) : null;
    this.selectedGovernorateId.set(id);
    this.selectedCityId.set(null);
    this.selectedDistrictId.set(null);
    this.selectedCompoundId.set(null);
  }

  onCityChange(idVal: string | number | null): void {
    const id = idVal ? Number(idVal) : null;
    this.selectedCityId.set(id);
    this.selectedDistrictId.set(null);
    this.selectedCompoundId.set(null);
  }

  onDistrictChange(idVal: string | number | null): void {
    const id = idVal ? Number(idVal) : null;
    this.selectedDistrictId.set(id);
    this.selectedCompoundId.set(null);
  }

  toggleAmenity(id: number): void {
    const current = this.selectedAmenityIds();
    this.selectedAmenityIds.set(
      current.includes(id) ? current.filter((item) => item !== id) : [...current, id]
    );
  }

  resolveTargetLocationId(): number | null {
    return (
      this.selectedCompoundId() ||
      this.selectedDistrictId() ||
      this.selectedCityId() ||
      this.selectedGovernorateId() ||
      null
    );
  }

  applyFilters(): void {
    const filters: PropertyFilterCriteria = {
      locationId: this.resolveTargetLocationId(),
      minPrice: this.minPrice(),
      maxPrice: this.maxPrice(),
      currency: this.currency(),
      propertyTypeId: this.selectedPropertyTypeId(),
      purpose: this.purpose(),
      minBedrooms: this.minBedrooms(),
      amenityIds: this.selectedAmenityIds().length > 0 ? this.selectedAmenityIds() : null,
      page: 0
    };
    this.filterChange.emit(filters);
  }

  onReset(): void {
    this.selectedGovernorateId.set(null);
    this.selectedCityId.set(null);
    this.selectedDistrictId.set(null);
    this.selectedCompoundId.set(null);
    this.minPrice.set(null);
    this.maxPrice.set(null);
    this.currency.set('EGP');
    this.selectedPropertyTypeId.set(null);
    this.purpose.set(null);
    this.minBedrooms.set(null);
    this.selectedAmenityIds.set([]);
    this.resetFilters.emit();
  }
}
