import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable, of } from 'rxjs';
import {
  AmenityDto,
  LocationNode,
  PageResponse,
  PropertyRequest,
  PropertyResponse,
  PropertyTypeDto
} from '../models/property.model';

@Injectable({
  providedIn: 'root'
})
export class PropertyService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = '/api/properties';

  createProperty(request: PropertyRequest): Observable<PropertyResponse> {
    return this.http.post<PropertyResponse>(this.baseUrl, request);
  }

  updateProperty(id: number, request: PropertyRequest): Observable<PropertyResponse> {
    return this.http.put<PropertyResponse>(`${this.baseUrl}/${id}`, request);
  }

  getProperty(id: number): Observable<PropertyResponse> {
    return this.http.get<PropertyResponse>(`${this.baseUrl}/${id}`);
  }

  getMyProperties(page = 0, size = 20): Observable<PageResponse<PropertyResponse>> {
    const params = new HttpParams().set('page', page.toString()).set('size', size.toString());
    return this.http.get<PageResponse<PropertyResponse>>(`${this.baseUrl}/mine`, { params });
  }

  publishProperty(id: number): Observable<PropertyResponse> {
    return this.http.post<PropertyResponse>(`${this.baseUrl}/${id}/publish`, {});
  }

  archiveProperty(id: number): Observable<PropertyResponse> {
    return this.http.post<PropertyResponse>(`${this.baseUrl}/${id}/archive`, {});
  }

  getLocations(): Observable<LocationNode[]> {
    return of([
      { id: 1, nameEn: 'Alexandria', nameAr: 'الإسكندرية', type: 'GOVERNORATE', parentId: null },
      { id: 2, nameEn: 'Alexandria City', nameAr: 'مدينة الإسكندرية', type: 'CITY', parentId: 1 },
      { id: 3, nameEn: 'Smouha', nameAr: 'سموحة', type: 'DISTRICT', parentId: 2 },
      { id: 4, nameEn: 'Kafr Abdo', nameAr: 'كفر عبده', type: 'DISTRICT', parentId: 2 },
      { id: 5, nameEn: 'Cairo', nameAr: 'القاهرة', type: 'GOVERNORATE', parentId: null },
      { id: 6, nameEn: 'New Cairo', nameAr: 'القاهرة الجديدة', type: 'CITY', parentId: 5 },
      { id: 7, nameEn: 'Fifth Settlement', nameAr: 'التجمع الخامس', type: 'DISTRICT', parentId: 6 }
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
