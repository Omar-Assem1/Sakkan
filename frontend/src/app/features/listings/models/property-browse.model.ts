export type PropertyPurpose = 'SALE' | 'RENT';
export type PropertyStatus = 'DRAFT' | 'PUBLISHED' | 'RENTED' | 'SOLD' | 'ARCHIVED';

export interface PropertySummaryDto {
  id: number;
  ownerId?: number;
  agentId?: number | null;
  title: string;
  description: string;
  purpose: PropertyPurpose;
  propertyTypeId: number;
  locationId: number;
  price: number;
  currency: 'EGP' | 'USD';
  bedrooms: number;
  bathrooms: number;
  areaSqm: number;
  amenityIds: number[];
  status: PropertyStatus;
  createdAt: string;
  thumbnailUrl?: string | null;
}

export interface PageResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export interface PropertyFilterCriteria {
  locationId?: number | null;
  minPrice?: number | null;
  maxPrice?: number | null;
  currency?: 'EGP' | 'USD' | null;
  propertyTypeId?: number | null;
  purpose?: PropertyPurpose | null;
  minBedrooms?: number | null;
  amenityIds?: number[] | null;
  page?: number;
  size?: number;
  sort?: string;
  q?: string | null;
}

export interface LocationNode {
  id: number;
  nameEn: string;
  nameAr: string;
  type: 'GOVERNORATE' | 'CITY' | 'DISTRICT' | 'COMPOUND';
  parentId: number | null;
}

export interface PropertyTypeDto {
  id: number;
  code: string;
  nameEn: string;
  nameAr: string;
}

export interface AmenityDto {
  id: number;
  nameEn: string;
  nameAr: string;
}
