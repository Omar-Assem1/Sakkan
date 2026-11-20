export type PropertyPurpose = 'SALE' | 'RENT';
export type PropertyStatus = 'DRAFT' | 'PUBLISHED' | 'RENTED' | 'SOLD' | 'ARCHIVED';
export type PropertyCurrency = 'EGP' | 'USD';

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

export interface PropertyRequest {
  title: string;
  description?: string | null;
  purpose: PropertyPurpose;
  propertyTypeId: number;
  locationId: number;
  price: number;
  currency: PropertyCurrency;
  bedrooms: number;
  bathrooms: number;
  areaSqm: number;
  amenityIds?: number[];
}

export interface PropertyResponse {
  id: number;
  ownerId: number;
  agentId?: number | null;
  title: string;
  description?: string | null;
  purpose: PropertyPurpose;
  propertyTypeId: number;
  locationId: number;
  price: number;
  currency: PropertyCurrency;
  bedrooms: number;
  bathrooms: number;
  areaSqm: number;
  amenityIds: number[];
  status: PropertyStatus;
  createdAt: string;
  updatedAt?: string;
  thumbnailUrl?: string | null;
}

export interface PageResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}
