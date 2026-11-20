import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import {
  AuthResponse,
  CreateViewingSlotRequest,
  LoginRequest,
  PropertyPageResponse,
  PropertyRequest,
  PropertyResponse,
  RegisterRequest,
  UserSummaryResponse,
  ViewingSlotResponse,
} from '../models';

/**
 * Typed API Client generated from OpenAPI specification for the Sakan Backend.
 * SKN-13: openapi-generator step for the Angular client.
 */
@Injectable({
  providedIn: 'root',
})
export class ApiClientService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = '/api';

  // Auth endpoints
  register(request: RegisterRequest): Observable<UserSummaryResponse> {
    return this.http.post<UserSummaryResponse>(`${this.baseUrl}/auth/register`, request);
  }

  login(request: LoginRequest): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${this.baseUrl}/auth/login`, request);
  }

  adminLogin(request: LoginRequest): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${this.baseUrl}/auth/admin/login`, request);
  }

  me(): Observable<UserSummaryResponse> {
    return this.http.get<UserSummaryResponse>(`${this.baseUrl}/auth/me`);
  }

  // Property endpoints
  browseProperties(params?: {
    page?: number;
    size?: number;
    sort?: string;
    purpose?: string;
    propertyType?: string;
    propertyTypeId?: number;
    currency?: string;
    minPrice?: number;
    maxPrice?: number;
    minBedrooms?: number;
    locationId?: number;
    amenityIds?: number[];
    q?: string;
  }): Observable<PropertyPageResponse> {
    let httpParams = new HttpParams();
    if (params) {
      Object.entries(params).forEach(([key, value]) => {
        if (Array.isArray(value)) {
          value.forEach((v) => {
            httpParams = httpParams.append(key, v.toString());
          });
        } else if (value !== undefined && value !== null) {
          httpParams = httpParams.set(key, value.toString());
        }
      });
    }
    return this.http.get<PropertyPageResponse>(`${this.baseUrl}/properties`, { params: httpParams });
  }

  getPropertyById(id: number): Observable<PropertyResponse> {
    return this.http.get<PropertyResponse>(`${this.baseUrl}/properties/${id}`);
  }

  createProperty(request: PropertyRequest): Observable<PropertyResponse> {
    return this.http.post<PropertyResponse>(`${this.baseUrl}/properties`, request);
  }

  // Viewing slots endpoints
  createViewingSlot(propertyId: number, request: CreateViewingSlotRequest): Observable<ViewingSlotResponse> {
    return this.http.post<ViewingSlotResponse>(`${this.baseUrl}/properties/${propertyId}/slots`, request);
  }
}
