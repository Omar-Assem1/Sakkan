export interface PropertyRequest {
  title: string;
  description?: string;
  price: number;
  currency: 'EGP' | 'USD';
  purpose: 'SALE' | 'RENT';
  propertyTypeId: number;
  bedrooms: number;
  bathrooms: number;
  areaSqm: number;
  locationId: number;
  amenityIds?: number[];
}

export interface PropertyResponse {
  id: number;
  ownerId: number;
  agentId?: number | null;
  title: string;
  description?: string;
  price: number;
  currency: 'EGP' | 'USD';
  purpose: 'SALE' | 'RENT';
  propertyTypeId: number;
  locationId: number;
  amenityIds: number[];
  status: 'DRAFT' | 'PUBLISHED' | 'RENTED' | 'SOLD' | 'ARCHIVED';
  bedrooms: number;
  bathrooms: number;
  areaSqm: number;
  createdAt: string;
  updatedAt?: string;
  thumbnailUrl?: string | null;
}

export interface PropertyPageResponse {
  content: PropertyResponse[];
  totalElements: number;
  totalPages: number;
  size: number;
  page: number;
}
