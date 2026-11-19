import { inject, Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { PageResponse } from '../../properties/models/property.model';
import { AdminRole, AdminUserResponse } from '../models/admin-user.models';

export interface AdminUserFilters { q?: string | null; role?: AdminRole | null; enabled?: boolean | null; }

@Injectable({ providedIn: 'root' })
export class AdminUserService {
  private readonly http = inject(HttpClient);
  private readonly url = '/api/admin/users';

  list(filters: AdminUserFilters = {}, page = 0, size = 10): Observable<PageResponse<AdminUserResponse>> {
    let params = new HttpParams().set('page', page).set('size', size);
    const q = filters.q?.trim();
    if (q) params = params.set('q', q);
    if (filters.role) params = params.set('role', filters.role);
    if (filters.enabled !== null && filters.enabled !== undefined) params = params.set('enabled', filters.enabled);
    return this.http.get<PageResponse<AdminUserResponse>>(this.url, { params });
  }

  grantRole(id: number, role: AdminRole): Observable<AdminUserResponse> {
    return this.http.post<AdminUserResponse>(`${this.url}/${id}/roles`, { role });
  }

  revokeRole(id: number, role: AdminRole): Observable<AdminUserResponse> {
    return this.http.delete<AdminUserResponse>(`${this.url}/${id}/roles/${role}`);
  }
}
