import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ViewingService } from './viewing.service';
import { RequestViewingPayload, Viewing } from '../models/viewing.model';

describe('ViewingService (SCRUM-100 / SKN-26)', () => {
  let service: ViewingService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [ViewingService, provideHttpClient(), provideHttpClientTesting()],
    });

    service = TestBed.inject(ViewingService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should request a viewing via POST /api/viewings', () => {
    const payload: RequestViewingPayload = {
      slotId: 42,
      notes: 'Prefer an afternoon call',
    };
    const mockResponse: Viewing = {
      id: 7,
      slotId: 42,
      propertyId: 10,
      buyerId: 301,
      status: 'REQUESTED',
      notes: 'Prefer an afternoon call',
      createdAt: '2026-11-19T12:00:00Z',
      updatedAt: '2026-11-19T12:00:00Z',
    };

    let actual: Viewing | undefined;
    service.requestViewing(payload).subscribe((viewing) => {
      actual = viewing;
    });

    const req = httpMock.expectOne('/api/viewings');
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual(payload);
    req.flush(mockResponse, { status: 201, statusText: 'Created' });

    expect(actual).toEqual(mockResponse);
  });

  it('should propagate 409 Conflict ProblemDetail when slot is already booked', () => {
    let statusCode = 0;
    service.requestViewing({ slotId: 42 }).subscribe({
      error: (err) => {
        statusCode = err.status;
      },
    });

    const req = httpMock.expectOne('/api/viewings');
    req.flush(
      { status: 409, title: 'Conflict', detail: 'Viewing slot 42 is already booked' },
      { status: 409, statusText: 'Conflict' },
    );

    expect(statusCode).toBe(409);
  });
});
