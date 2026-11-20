import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable, catchError, forkJoin, map, of, switchMap, tap } from 'rxjs';
import {
  AmenityDto,
  LocationNode,
  PageResponse,
  PropertyFilterCriteria,
  PropertySummaryDto,
  PropertyTypeDto
} from '../models/property-browse.model';

interface LocationApiDto {
  id: number;
  parentId: number | null;
  type: 'GOVERNORATE' | 'CITY' | 'DISTRICT' | 'COMPOUND';
  name: string;
}

const SEEDED_LOCATIONS: LocationNode[] = [
  { id: 1, nameEn: 'Cairo', nameAr: 'القاهرة', type: 'GOVERNORATE', parentId: null },
  { id: 2, nameEn: 'Alexandria', nameAr: 'الإسكندرية', type: 'GOVERNORATE', parentId: null },
  { id: 3, nameEn: 'Giza', nameAr: 'الجيزة', type: 'GOVERNORATE', parentId: null },
  { id: 4, nameEn: 'New Cairo', nameAr: 'القاهرة الجديدة', type: 'CITY', parentId: 1 },
  { id: 5, nameEn: 'Nasr City', nameAr: 'مدينة نصر', type: 'CITY', parentId: 1 },
  { id: 6, nameEn: 'Heliopolis', nameAr: 'مصر الجديدة', type: 'CITY', parentId: 1 },
  { id: 7, nameEn: 'Alexandria City', nameAr: 'مدينة الإسكندرية', type: 'CITY', parentId: 2 },
  { id: 8, nameEn: '6th of October City', nameAr: 'مدينة السادس من أكتوبر', type: 'CITY', parentId: 3 },
  { id: 9, nameEn: 'Sheikh Zayed City', nameAr: 'مدينة الشيخ زايد', type: 'CITY', parentId: 3 },
  { id: 10, nameEn: 'Fifth Settlement', nameAr: 'التجمع الخامس', type: 'DISTRICT', parentId: 4 },
  { id: 11, nameEn: 'First Settlement', nameAr: 'التجمع الأول', type: 'DISTRICT', parentId: 4 },
  { id: 12, nameEn: 'District 1', nameAr: 'الحي الأول', type: 'DISTRICT', parentId: 5 },
  { id: 13, nameEn: 'District 7', nameAr: 'الحي السابع', type: 'DISTRICT', parentId: 5 },
  { id: 14, nameEn: 'Sidi Gaber', nameAr: 'سيدي جابر', type: 'DISTRICT', parentId: 7 },
  { id: 15, nameEn: 'Montaza', nameAr: 'المنتزه', type: 'DISTRICT', parentId: 7 },
  { id: 16, nameEn: 'Central District', nameAr: 'الحي المركزي', type: 'DISTRICT', parentId: 8 },
  { id: 17, nameEn: 'District 1', nameAr: 'الحي الأول', type: 'DISTRICT', parentId: 9 },
  { id: 18, nameEn: 'Madinaty', nameAr: 'مدينتي', type: 'COMPOUND', parentId: 10 },
  { id: 19, nameEn: 'Al Rehab', nameAr: 'الرحاب', type: 'COMPOUND', parentId: 10 },
  { id: 20, nameEn: 'Mountain View iCity', nameAr: 'ماونتن فيو آي سيتي', type: 'COMPOUND', parentId: 10 },
  { id: 21, nameEn: 'Palm Hills October', nameAr: 'بالم هيلز أكتوبر', type: 'COMPOUND', parentId: 16 },
  { id: 22, nameEn: 'Zed West', nameAr: 'زيد ويست', type: 'COMPOUND', parentId: 17 }
];

@Injectable({
  providedIn: 'root'
})
export class PropertyBrowseService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = '/api/properties';
  private cachedLocations: LocationNode[] | null = null;

  getProperties(filters: PropertyFilterCriteria): Observable<PageResponse<PropertySummaryDto>> {
    let params = new HttpParams();

    if (filters.locationId != null) {
      params = params.set('locationId', filters.locationId.toString());
    }
    if (filters.minPrice != null) {
      params = params.set('minPrice', filters.minPrice.toString());
    }
    if (filters.maxPrice != null) {
      params = params.set('maxPrice', filters.maxPrice.toString());
    }
    if (filters.currency) {
      params = params.set('currency', filters.currency);
    }
    if (filters.propertyTypeId != null) {
      params = params.set('propertyTypeId', filters.propertyTypeId.toString());
    }
    if (filters.purpose) {
      params = params.set('purpose', filters.purpose);
    }
    if (filters.minBedrooms != null) {
      params = params.set('minBedrooms', filters.minBedrooms.toString());
    }
    if (filters.amenityIds && filters.amenityIds.length > 0) {
      filters.amenityIds.forEach((id) => {
        params = params.append('amenityIds', id.toString());
      });
    }
    if (filters.page != null) {
      params = params.set('page', filters.page.toString());
    }
    if (filters.size != null) {
      params = params.set('size', filters.size.toString());
    }
    if (filters.sort) {
      params = params.set('sort', filters.sort);
    }
    if (filters.q && filters.q.trim().length > 0) {
      params = params.set('q', filters.q.trim());
    }

    return this.http.get<PageResponse<PropertySummaryDto>>(this.baseUrl, { params });
  }

  getLocations(): Observable<LocationNode[]> {
    if (this.cachedLocations) {
      return of(this.cachedLocations);
    }
    return this.getGovernorates('en').pipe(
      switchMap((govs) => {
        if (!govs || govs.length === 0) {
          return of(SEEDED_LOCATIONS);
        }
        const cityRequests = govs.map((g) =>
          this.getCitiesByGovernorate(g.id, 'en').pipe(catchError(() => of([] as LocationApiDto[])))
        );
        return forkJoin(cityRequests).pipe(
          map((cityGroups) => ({
            govs: govs as LocationApiDto[],
            cities: cityGroups.flat() as LocationApiDto[]
          }))
        );
      }),
      switchMap((state) => {
        if (Array.isArray(state)) {
          return of(state);
        }
        const { govs, cities } = state;
        if (cities.length === 0) {
          return of(this.toLocationNodes(govs));
        }
        const districtRequests = cities.map((c) =>
          this.getDistrictsByCity(c.id, 'en').pipe(catchError(() => of([] as LocationApiDto[])))
        );
        return forkJoin(districtRequests).pipe(
          map((districtGroups) => ({
            govs,
            cities,
            districts: districtGroups.flat() as LocationApiDto[]
          }))
        );
      }),
      switchMap((state) => {
        if (Array.isArray(state)) {
          return of(state);
        }
        const { govs, cities, districts } = state;
        if (districts.length === 0) {
          return of(this.toLocationNodes([...govs, ...cities]));
        }
        const compoundRequests = districts.map((d) =>
          this.getCompoundsByDistrict(d.id, 'en').pipe(catchError(() => of([] as LocationApiDto[])))
        );
        return forkJoin(compoundRequests).pipe(
          map((compoundGroups) =>
            this.toLocationNodes([...govs, ...cities, ...districts, ...(compoundGroups.flat() as LocationApiDto[])])
          )
        );
      }),
      tap((nodes) => {
        if (nodes.length > 0) {
          this.cachedLocations = nodes;
        }
      }),
      catchError(() => of(SEEDED_LOCATIONS))
    );
  }

  private toLocationNodes(dtos: LocationApiDto[]): LocationNode[] {
    return dtos.map((dto) => {
      const seeded = SEEDED_LOCATIONS.find(
        (s) => s.type === dto.type && (s.nameEn === dto.name || s.nameAr === dto.name || s.id === dto.id)
      );
      return {
        id: dto.id,
        parentId: dto.parentId,
        type: dto.type,
        nameEn: seeded ? seeded.nameEn : dto.name,
        nameAr: seeded ? seeded.nameAr : dto.name
      };
    });
  }

  getPropertyTypes(): Observable<PropertyTypeDto[]> {
    return of([
      { id: 1, code: 'APARTMENT', nameEn: 'Apartment', nameAr: 'شقة' },
      { id: 2, code: 'VILLA', nameEn: 'Villa', nameAr: 'فيلا' },
      { id: 3, code: 'TOWNHOUSE', nameEn: 'Townhouse', nameAr: 'تاون هاوس' },
      { id: 4, code: 'DUPLEX', nameEn: 'Duplex', nameAr: 'دوبلكس' },
      { id: 5, code: 'STUDIO', nameEn: 'Studio', nameAr: 'استوديو' },
      { id: 6, code: 'OFFICE', nameEn: 'Office', nameAr: 'مكتب' }
    ]);
  }

  getAmenities(): Observable<AmenityDto[]> {
    return of([
      { id: 1, nameEn: 'Air conditioning', nameAr: 'تكييف' },
      { id: 2, nameEn: 'Balcony', nameAr: 'شرفة' },
      { id: 3, nameEn: 'Elevator', nameAr: 'مصعد' },
      { id: 4, nameEn: 'Parking', nameAr: 'موقف سيارات' },
      { id: 5, nameEn: 'Security', nameAr: 'أمن' },
      { id: 6, nameEn: 'Swimming pool', nameAr: 'حمام سباحة' },
      { id: 7, nameEn: 'Garden', nameAr: 'حديقة' },
      { id: 8, nameEn: 'Central heating', nameAr: 'تدفئة مركزية' },
      { id: 9, nameEn: 'Furnished', nameAr: 'مفروش' },
      { id: 10, nameEn: 'Pet friendly', nameAr: 'يسمح بالحيوانات الأليفة' }
    ]);
  }

  getGovernorates(lang: 'en' | 'ar' = 'en') {
    return this.http.get<{ id: number; parentId: number | null; type: string; name: string }[]>(
      '/api/locations/governorates',
      { headers: { 'Accept-Language': lang } }
    );
  }

  getCitiesByGovernorate(parentId: number, lang: 'en' | 'ar' = 'en') {
    return this.http.get<{ id: number; parentId: number | null; type: string; name: string }[]>(
      '/api/locations/cities',
      { params: new HttpParams().set('parentId', parentId.toString()), headers: { 'Accept-Language': lang } }
    );
  }

  getDistrictsByCity(parentId: number, lang: 'en' | 'ar' = 'en') {
    return this.http.get<{ id: number; parentId: number | null; type: string; name: string }[]>(
      '/api/locations/districts',
      { params: new HttpParams().set('parentId', parentId.toString()), headers: { 'Accept-Language': lang } }
    );
  }

  getCompoundsByDistrict(parentId: number, lang: 'en' | 'ar' = 'en') {
    return this.http.get<{ id: number; parentId: number | null; type: string; name: string }[]>(
      '/api/locations/compounds',
      { params: new HttpParams().set('parentId', parentId.toString()), headers: { 'Accept-Language': lang } }
    );
  }
}
