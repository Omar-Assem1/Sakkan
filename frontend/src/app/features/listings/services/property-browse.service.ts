import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable, of } from 'rxjs';
import {
  AmenityDto,
  LocationNode,
  PageResponse,
  PropertyFilterCriteria,
  PropertySummaryDto,
  PropertyTypeDto
} from '../models/property-browse.model';

@Injectable({
  providedIn: 'root'
})
export class PropertyBrowseService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = '/api/properties';

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
    return of([
      { id: 1, nameEn: 'Alexandria', nameAr: 'الإسكندرية', type: 'GOVERNORATE', parentId: null },
      { id: 2, nameEn: 'Smouha', nameAr: 'سموحة', type: 'DISTRICT', parentId: 1 },
      { id: 3, nameEn: 'Kafr Abdo', nameAr: 'كفر عبده', type: 'DISTRICT', parentId: 1 },
      { id: 4, nameEn: 'Cairo', nameAr: 'القاهرة', type: 'GOVERNORATE', parentId: null },
      { id: 5, nameEn: 'New Cairo', nameAr: 'القاهرة الجديدة', type: 'CITY', parentId: 4 }
    ]);
  }

  getPropertyTypes(): Observable<PropertyTypeDto[]> {
    return of([
      { id: 1, code: 'APARTMENT', nameEn: 'Apartment', nameAr: 'شقة' },
      { id: 2, code: 'VILLA', nameEn: 'Villa', nameAr: 'فيلا' },
      { id: 3, code: 'DUPLEX', nameEn: 'Duplex', nameAr: 'دوبلكس' },
      { id: 4, code: 'CHALET', nameEn: 'Chalet', nameAr: 'شاليه' }
    ]);
  }

  getAmenities(): Observable<AmenityDto[]> {
    return of([
      { id: 1, nameEn: 'Elevator', nameAr: 'مصعد' },
      { id: 2, nameEn: 'Swimming Pool', nameAr: 'حمام سباحة' },
      { id: 3, nameEn: 'Private Parking', nameAr: 'موقف سيارات خاص' },
      { id: 4, nameEn: 'Security', nameAr: 'حراسة وأمن' }
    ]);
  }
}
