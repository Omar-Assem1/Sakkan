import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ViewingService } from './viewing.service';
import { RequestViewingPayload, Viewing, ViewingPage } from '../models/viewing.model';

describe('ViewingService (SCRUM-100 & SCRUM-104)', () => {
  let service: ViewingService;
  let httpMock: HttpTestingController;

  const sampleViewing: Viewing = {
    id: 7,
    slotId: 42,
    propertyId: 10,
    buyerId: 301,
    status: 'REQUESTED',
    notes: 'Prefer an afternoon call',
    createdAt: '2026-11-19T12:00:00Z',
    updatedAt: '2026-11-19T12:00:00Z',
  };

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

    let actual: Viewing | undefined;
    service.requestViewing(payload).subscribe((viewing) => {
      actual = viewing;
    });

    const req = httpMock.expectOne('/api/viewings');
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual(payload);
    req.flush(sampleViewing, { status: 201, statusText: 'Created' });

    expect(actual).toEqual(sampleViewing);
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

  it('should fetch paginated viewings via GET /api/viewings/my with optional status filter', () => {
    const mockPage: ViewingPage = {
      content: [sampleViewing],
      page: 0,
      size: 20,
      totalElements: 1,
      totalPages: 1,
    };

    let actual: ViewingPage | undefined;
    service.getMyViewings('REQUESTED', 0, 20).subscribe((res) => {
      actual = res;
    });

    const req = httpMock.expectOne('/api/viewings/my?page=0&size=20&status=REQUESTED');
    expect(req.request.method).toBe('GET');
    req.flush(mockPage);

    expect(actual).toEqual(mockPage);
  });

  it('should confirm, cancel, and complete a viewing via PATCH endpoints', () => {
    let confirmed: Viewing | undefined;
    service.confirmViewing(7).subscribe((v) => (confirmed = v));
    const confirmReq = httpMock.expectOne('/api/viewings/7/confirm');
    expect(confirmReq.request.method).toBe('PATCH');
    confirmReq.flush({ ...sampleViewing, status: 'CONFIRMED' });
    expect(confirmed?.status).toBe('CONFIRMED');

    let cancelled: Viewing | undefined;
    service.cancelViewing(7).subscribe((v) => (cancelled = v));
    const cancelReq = httpMock.expectOne('/api/viewings/7/cancel');
    expect(cancelReq.request.method).toBe('PATCH');
    cancelReq.flush({ ...sampleViewing, status: 'CANCELLED' });
    expect(cancelled?.status).toBe('CANCELLED');

    let completed: Viewing | undefined;
    service.completeViewing(7).subscribe((v) => (completed = v));
    const completeReq = httpMock.expectOne('/api/viewings/7/complete');
    expect(completeReq.request.method).toBe('PATCH');
    completeReq.flush({ ...sampleViewing, status: 'COMPLETED' });
    expect(completed?.status).toBe('COMPLETED');
  });
});
