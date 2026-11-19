import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import {
  RequestViewingPayload,
  Viewing,
  ViewingPage,
  ViewingStatus,
} from '../models/viewing.model';

@Injectable({ providedIn: 'root' })
export class ViewingService {
  private readonly http = inject(HttpClient);

  requestViewing(payload: RequestViewingPayload): Observable<Viewing> {
    return this.http.post<Viewing>('/api/viewings', payload);
  }

  getMyViewings(
    status?: ViewingStatus | null,
    page = 0,
    size = 20,
  ): Observable<ViewingPage> {
    let params = new HttpParams().set('page', String(page)).set('size', String(size));
    if (status) {
      params = params.set('status', status);
    }
    return this.http.get<ViewingPage>('/api/viewings/my', { params });
  }

  confirmViewing(id: number): Observable<Viewing> {
    return this.http.patch<Viewing>(`/api/viewings/${id}/confirm`, {});
  }

  cancelViewing(id: number): Observable<Viewing> {
    return this.http.patch<Viewing>(`/api/viewings/${id}/cancel`, {});
  }

  completeViewing(id: number): Observable<Viewing> {
    return this.http.patch<Viewing>(`/api/viewings/${id}/complete`, {});
  }
}
