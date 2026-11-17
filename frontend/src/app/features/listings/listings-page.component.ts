import {
  ChangeDetectionStrategy,
  Component,
  OnInit,
  computed,
  inject,
  signal
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { TranslocoPipe } from '@jsverse/transloco';
import { LanguageService } from '../../core/services/language.service';
import { PropertyBrowseService } from './services/property-browse.service';
import { PropertyFilterPanelComponent } from './components/property-filter-panel/property-filter-panel.component';
import { PropertyCardComponent } from './components/property-card/property-card.component';
import {
  AmenityDto,
  LocationNode,
  PropertyFilterCriteria,
  PropertySummaryDto,
  PropertyTypeDto
} from './models/property-browse.model';

@Component({
  selector: 'app-listings-page',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    CommonModule,
    FormsModule,
    TranslocoPipe,
    PropertyFilterPanelComponent,
    PropertyCardComponent
  ],
  templateUrl: './listings-page.component.html'
})
export class ListingsPageComponent implements OnInit {
  private readonly browseService = inject(PropertyBrowseService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly languageService = inject(LanguageService);

  properties = signal<PropertySummaryDto[]>([]);
  locations = signal<LocationNode[]>([]);
  propertyTypes = signal<PropertyTypeDto[]>([]);
  amenities = signal<AmenityDto[]>([]);

  isLoading = signal<boolean>(true);
  hasError = signal<boolean>(false);

  page = signal<number>(0);
  size = signal<number>(12);
  totalPages = signal<number>(0);
  totalElements = signal<number>(0);
  currentSort = signal<string>('createdAt,desc');

  activeFilters = signal<PropertyFilterCriteria>({});
  isEmpty = computed(() => !this.isLoading() && this.properties().length === 0);

  getLocationName(locationId: number): string {
    const loc = this.locations().find((l) => l.id === locationId);
    if (!loc) return '';
    return this.languageService.currentLang() === 'ar' ? loc.nameAr : loc.nameEn;
  }

  ngOnInit(): void {
    this.loadMetadata();

    this.route.queryParams.subscribe((params) => {
      const parsedFilters: PropertyFilterCriteria = {
        locationId: params['locationId'] ? Number(params['locationId']) : null,
        minPrice: params['minPrice'] ? Number(params['minPrice']) : null,
        maxPrice: params['maxPrice'] ? Number(params['maxPrice']) : null,
        currency: params['currency'] || null,
        propertyTypeId: params['propertyTypeId'] ? Number(params['propertyTypeId']) : null,
        purpose: params['purpose'] || null,
        minBedrooms: params['minBedrooms'] ? Number(params['minBedrooms']) : null,
        amenityIds: params['amenityIds']
          ? Array.isArray(params['amenityIds'])
            ? params['amenityIds'].map(Number)
            : [Number(params['amenityIds'])]
          : null,
        page: params['page'] ? Number(params['page']) : 0,
        size: params['size'] ? Number(params['size']) : 12,
        sort: params['sort'] || 'createdAt,desc'
      };

      this.page.set(parsedFilters.page || 0);
      this.size.set(parsedFilters.size || 12);
      this.currentSort.set(parsedFilters.sort || 'createdAt,desc');
      this.activeFilters.set(parsedFilters);

      this.fetchProperties();
    });
  }

  loadMetadata(): void {
    this.browseService.getLocations().subscribe({
      next: (data) => this.locations.set(data),
      error: () => console.error('Failed to load locations')
    });
    this.browseService.getPropertyTypes().subscribe({
      next: (data) => this.propertyTypes.set(data),
      error: () => console.error('Failed to load types')
    });
    this.browseService.getAmenities().subscribe({
      next: (data) => this.amenities.set(data),
      error: () => console.error('Failed to load amenities')
    });
  }

  fetchProperties(): void {
    this.isLoading.set(true);
    this.hasError.set(false);

    const query: PropertyFilterCriteria = {
      ...this.activeFilters(),
      page: this.page(),
      size: this.size(),
      sort: this.currentSort()
    };

    this.browseService.getProperties(query).subscribe({
      next: (res) => {
        this.properties.set(res.content);
        this.page.set(res.page);
        this.size.set(res.size);
        this.totalPages.set(res.totalPages);
        this.totalElements.set(res.totalElements);
        this.isLoading.set(false);
      },
      error: () => {
        this.hasError.set(true);
        this.isLoading.set(false);
      }
    });
  }

  onFilterChanged(filters: PropertyFilterCriteria): void {
    this.syncParamsToUrl({ ...filters, page: 0 });
  }

  onSortChanged(sortValue: string): void {
    this.currentSort.set(sortValue);
    this.syncParamsToUrl({ ...this.activeFilters(), sort: sortValue, page: 0 });
  }

  goToPage(newPage: number): void {
    if (newPage >= 0 && newPage < this.totalPages()) {
      this.syncParamsToUrl({ ...this.activeFilters(), page: newPage });
      window.scrollTo({ top: 0, behavior: 'smooth' });
    }
  }

  resetAllFilters(): void {
    this.router.navigate([], {
      relativeTo: this.route,
      queryParams: { page: 0, size: 12, sort: 'createdAt,desc' }
    });
  }

  private syncParamsToUrl(filters: PropertyFilterCriteria): void {
    const queryParams: Record<string, string | number | number[] | null> = {
      locationId: filters.locationId || null,
      minPrice: filters.minPrice || null,
      maxPrice: filters.maxPrice || null,
      currency: filters.currency || null,
      propertyTypeId: filters.propertyTypeId || null,
      purpose: filters.purpose || null,
      minBedrooms: filters.minBedrooms || null,
      amenityIds: filters.amenityIds || null,
      page: filters.page ?? 0,
      size: filters.size ?? 12,
      sort: filters.sort || this.currentSort()
    };

    this.router.navigate([], {
      relativeTo: this.route,
      queryParams,
      queryParamsHandling: 'merge'
    });
  }
}
