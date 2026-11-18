export interface PropertyRequest {
  title: string;
  description: string;
  price: number;
  currency?: string;
  purpose: 'SALE' | 'RENT';
  type: string;
  bedrooms?: number;
  bathrooms?: number;
  areaSqm: number;
  locationId?: number;
}

export interface PropertyResponse {
  id: number;
  title: string;
  description: string;
  price: number;
  currency: string;
  purpose: string;
  type: string;
  status: string;
  bedrooms?: number;
  bathrooms?: number;
  areaSqm: number;
  createdAt?: string;
}

export interface PropertyPageResponse {
  content: PropertyResponse[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
}
