import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { PropertyService } from './property.service';
import { LocationNode, PageResponse, PropertyRequest, PropertyResponse } from '../models/property.model';

describe('PropertyService (SKN-21)', () => {
  let service: PropertyService;
  let httpMock: HttpTestingController;

  const mockPropertyRequest: PropertyRequest = {
    title: 'Modern Apartment in Smouha',
    description: 'Spacious 3 bedroom apartment',
    purpose: 'SALE',
    propertyTypeId: 1,
    locationId: 3,
    price: 2500000,
    currency: 'EGP',
    bedrooms: 3,
    bathrooms: 2,
    areaSqm: 180,
    amenityIds: [1, 3]
  };

  const mockPropertyResponse: PropertyResponse = {
    id: 101,
    ownerId: 10,
    title: 'Modern Apartment in Smouha',
    description: 'Spacious 3 bedroom apartment',
    purpose: 'SALE',
    propertyTypeId: 1,
    locationId: 3,
    price: 2500000,
    currency: 'EGP',
    bedrooms: 3,
    bathrooms: 2,
    areaSqm: 180,
    amenityIds: [1, 3],
    status: 'DRAFT',
    createdAt: '2026-11-18T12:00:00Z',
    updatedAt: '2026-11-18T12:00:00Z'
  };

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [PropertyService, provideHttpClient(), provideHttpClientTesting()]
    });

    service = TestBed.inject(PropertyService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should create property via POST /api/properties', () => {
    let result: PropertyResponse | undefined;
    service.createProperty(mockPropertyRequest).subscribe((res) => {
      result = res;
    });

    const req = httpMock.expectOne('/api/properties');
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual(mockPropertyRequest);
    req.flush(mockPropertyResponse, { status: 201, statusText: 'Created' });

    expect(result).toEqual(mockPropertyResponse);
  });

  it('should update property via PUT /api/properties/{id}', () => {
    let result: PropertyResponse | undefined;
    service.updateProperty(101, mockPropertyRequest).subscribe((res) => {
      result = res;
    });

    const req = httpMock.expectOne('/api/properties/101');
    expect(req.request.method).toBe('PUT');
    expect(req.request.body).toEqual(mockPropertyRequest);
    req.flush(mockPropertyResponse);

    expect(result).toEqual(mockPropertyResponse);
  });

  it('should fetch property by id via GET /api/properties/{id}', () => {
    let result: PropertyResponse | undefined;
    service.getProperty(101).subscribe((res) => {
      result = res;
    });

    const req = httpMock.expectOne('/api/properties/101');
    expect(req.request.method).toBe('GET');
    req.flush(mockPropertyResponse);

    expect(result).toEqual(mockPropertyResponse);
  });

  it('should fetch current user properties via GET /api/properties/mine with pagination', () => {
    const mockPage: PageResponse<PropertyResponse> = {
      content: [mockPropertyResponse],
      page: 0,
      size: 10,
      totalElements: 1,
      totalPages: 1
    };

    let result: PageResponse<PropertyResponse> | undefined;
    service.getMyProperties(0, 10).subscribe((res) => {
      result = res;
    });

    const req = httpMock.expectOne('/api/properties/mine?page=0&size=10');
    expect(req.request.method).toBe('GET');
    req.flush(mockPage);

    expect(result).toEqual(mockPage);
  });

  it('should publish property via POST /api/properties/{id}/publish', () => {
    const publishedResponse: PropertyResponse = {
      ...mockPropertyResponse,
      status: 'PUBLISHED'
    };

    let result: PropertyResponse | undefined;
    service.publishProperty(101).subscribe((res) => {
      result = res;
    });

    const req = httpMock.expectOne('/api/properties/101/publish');
    expect(req.request.method).toBe('POST');
    req.flush(publishedResponse);

    expect(result?.status).toBe('PUBLISHED');
  });

  it('should archive property via POST /api/properties/{id}/archive', () => {
    const archivedResponse: PropertyResponse = {
      ...mockPropertyResponse,
      status: 'ARCHIVED'
    };

    let result: PropertyResponse | undefined;
    service.archiveProperty(101).subscribe((res) => {
      result = res;
    });

    const req = httpMock.expectOne('/api/properties/101/archive');
    expect(req.request.method).toBe('POST');
    req.flush(archivedResponse);

    expect(result?.status).toBe('ARCHIVED');
  });

  it('should return seeded location hierarchy', () => {
    let locations: LocationNode[] = [];
    service.getLocations().subscribe((locs) => {
      locations = locs;
    });

    const govReq = httpMock.expectOne('/api/locations/governorates');
    expect(govReq.request.method).toBe('GET');
    govReq.flush([{ id: 1, parentId: null, type: 'GOVERNORATE', name: 'Cairo' }]);

    const cityReq = httpMock.expectOne('/api/locations/cities?parentId=1');
    cityReq.flush([{ id: 4, parentId: 1, type: 'CITY', name: 'New Cairo' }]);

    const distReq = httpMock.expectOne('/api/locations/districts?parentId=4');
    distReq.flush([{ id: 10, parentId: 4, type: 'DISTRICT', name: 'Fifth Settlement' }]);

    const compReq = httpMock.expectOne('/api/locations/compounds?parentId=10');
    compReq.flush([{ id: 18, parentId: 10, type: 'COMPOUND', name: 'Madinaty' }]);

    expect(locations.length).toBe(4);
    expect(locations.some((l) => l.type === 'GOVERNORATE' && l.nameAr === 'القاهرة')).toBe(true);
    expect(locations.some((l) => l.type === 'COMPOUND' && l.nameEn === 'Madinaty')).toBe(true);
  });

  it('should fall back to seeded location hierarchy when location API fails', () => {
    let locations: LocationNode[] = [];
    service.getLocations().subscribe((locs) => {
      locations = locs;
    });

    const govReq = httpMock.expectOne('/api/locations/governorates');
    govReq.flush('Offline', { status: 503, statusText: 'Service Unavailable' });

    expect(locations.length).toBe(22);
    expect(locations.some((l) => l.type === 'GOVERNORATE')).toBe(true);
  });
});
