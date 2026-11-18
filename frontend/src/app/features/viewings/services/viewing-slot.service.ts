import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { CreateViewingSlotPayload, ViewingSlot } from '../models/viewing-slot.model';

@Injectable({ providedIn: 'root' })
export class ViewingSlotService {
  private readonly http = inject(HttpClient);

  getSlots(propertyId: number): Observable<ViewingSlot[]> {
    return this.http.get<ViewingSlot[]>(`/api/properties/${propertyId}/slots`);
  }

  createSlot(propertyId: number, payload: CreateViewingSlotPayload): Observable<ViewingSlot> {
    return this.http.post<ViewingSlot>(`/api/properties/${propertyId}/slots`, payload);
  }

  deleteSlot(propertyId: number, slotId: number): Observable<void> {
    return this.http.delete<void>(`/api/properties/${propertyId}/slots/${slotId}`);
  }
}
