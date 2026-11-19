import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { RequestViewingPayload, Viewing } from '../models/viewing.model';

@Injectable({ providedIn: 'root' })
export class ViewingService {
  private readonly http = inject(HttpClient);

  requestViewing(payload: RequestViewingPayload): Observable<Viewing> {
    return this.http.post<Viewing>('/api/viewings', payload);
  }
}
