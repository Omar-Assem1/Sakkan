import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { PropertyBrowseService } from './property-browse.service';
import { PropertyFilterCriteria, PropertySummaryDto, PageResponse } from '../models/property-browse.model';

describe('PropertyBrowseService', () => {
  let service: PropertyBrowseService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [PropertyBrowseService],
    });
    service = TestBed.inject(PropertyBrowseService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should format all browse filter query parameters correctly', () => {
    const filters: PropertyFilterCriteria = {
      locationId: 5,
      minPrice: 1000000,
      maxPrice: 3000000,
      currency: 'EGP',
      propertyTypeId: 1,
      purpose: 'SALE',
      minBedrooms: 3,
      amenityIds: [1, 2],
      page: 0,
      size: 12,
      sort: 'price,desc',
    };

    const mockResponse: PageResponse<PropertySummaryDto> = {
      content: [],
      page: 0,
      size: 12,
      totalElements: 0,
      totalPages: 0,
    };

    service.getProperties(filters).subscribe((res) => {
      expect(res).toEqual(mockResponse);
    });

    const req = httpMock.expectOne((request) => {
      return (
        request.url === '/api/properties' &&
        request.params.get('locationId') === '5' &&
        request.params.get('minPrice') === '1000000' &&
        request.params.get('maxPrice') === '3000000' &&
        request.params.get('currency') === 'EGP' &&
        request.params.get('propertyTypeId') === '1' &&
        request.params.get('purpose') === 'SALE' &&
        request.params.get('minBedrooms') === '3' &&
        request.params.getAll('amenityIds')?.length === 2 &&
        request.params.get('page') === '0' &&
        request.params.get('size') === '12' &&
        request.params.get('sort') === 'price,desc'
      );
    });

    expect(req.request.method).toBe('GET');
    req.flush(mockResponse);
  });

  it('should load 4-level location hierarchy from /api/locations endpoints', () => {
    let actual: unknown[] = [];
    service.getLocations().subscribe((locs) => {
      actual = locs;
    });

    const govReq = httpMock.expectOne('/api/locations/governorates');
    govReq.flush([{ id: 1, parentId: null, type: 'GOVERNORATE', name: 'Cairo' }]);

    const cityReq = httpMock.expectOne('/api/locations/cities?parentId=1');
    cityReq.flush([{ id: 4, parentId: 1, type: 'CITY', name: 'New Cairo' }]);

    const distReq = httpMock.expectOne('/api/locations/districts?parentId=4');
    distReq.flush([{ id: 10, parentId: 4, type: 'DISTRICT', name: 'Fifth Settlement' }]);

    const compReq = httpMock.expectOne('/api/locations/compounds?parentId=10');
    compReq.flush([{ id: 18, parentId: 10, type: 'COMPOUND', name: 'Madinaty' }]);

    expect(actual.length).toBe(4);
  });
});
