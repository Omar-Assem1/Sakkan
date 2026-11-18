import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ViewingSlotService } from './viewing-slot.service';
import { CreateViewingSlotPayload, ViewingSlot } from '../models/viewing-slot.model';

describe('ViewingSlotService (SCRUM-96 / SKN-25)', () => {
  let service: ViewingSlotService;
  let httpMock: HttpTestingController;

  const PROPERTY_ID = 10;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [ViewingSlotService, provideHttpClient(), provideHttpClientTesting()],
    });

    service = TestBed.inject(ViewingSlotService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should fetch available slots for a property via GET /api/properties/{propertyId}/slots', () => {
    const mockSlots: ViewingSlot[] = [
      {
        id: 1,
        propertyId: PROPERTY_ID,
        createdBy: 101,
        startTime: '2026-12-01T10:00:00Z',
        endTime: '2026-12-01T11:00:00Z',
        createdAt: '2026-11-18T10:00:00Z',
      },
    ];

    let actual: ViewingSlot[] = [];
    service.getSlots(PROPERTY_ID).subscribe((slots) => {
      actual = slots;
    });

    const req = httpMock.expectOne(`/api/properties/${PROPERTY_ID}/slots`);
    expect(req.request.method).toBe('GET');
    req.flush(mockSlots);

    expect(actual).toEqual(mockSlots);
  });

  it('should create a viewing slot via POST /api/properties/{propertyId}/slots', () => {
    const payload: CreateViewingSlotPayload = {
      startTime: '2026-12-01T11:00:00Z',
      endTime: '2026-12-01T12:00:00Z',
    };
    const createdSlot: ViewingSlot = {
      id: 2,
      propertyId: PROPERTY_ID,
      createdBy: 101,
      ...payload,
      createdAt: '2026-11-18T10:05:00Z',
    };

    let actual: ViewingSlot | undefined;
    service.createSlot(PROPERTY_ID, payload).subscribe((slot) => {
      actual = slot;
    });

    const req = httpMock.expectOne(`/api/properties/${PROPERTY_ID}/slots`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual(payload);
    req.flush(createdSlot, { status: 201, statusText: 'Created' });

    expect(actual).toEqual(createdSlot);
  });

  it('should propagate 409 Conflict ProblemDetail when creating an overlapping slot', () => {
    const payload: CreateViewingSlotPayload = {
      startTime: '2026-12-01T10:30:00Z',
      endTime: '2026-12-01T11:30:00Z',
    };

    let errorStatus = 0;
    service.createSlot(PROPERTY_ID, payload).subscribe({
      error: (err) => {
        errorStatus = err.status;
      },
    });

    const req = httpMock.expectOne(`/api/properties/${PROPERTY_ID}/slots`);
    req.flush(
      {
        title: 'Conflict',
        status: 409,
        detail: 'Viewing slot overlaps with an existing slot on property 10',
      },
      { status: 409, statusText: 'Conflict' },
    );

    expect(errorStatus).toBe(409);
  });

  it('should delete a viewing slot via DELETE /api/properties/{propertyId}/slots/{slotId}', () => {
    let completed = false;
    service.deleteSlot(PROPERTY_ID, 2).subscribe(() => {
      completed = true;
    });

    const req = httpMock.expectOne(`/api/properties/${PROPERTY_ID}/slots/2`);
    expect(req.request.method).toBe('DELETE');
    req.flush(null, { status: 204, statusText: 'No Content' });

    expect(completed).toBe(true);
  });
});
