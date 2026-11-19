export type ViewingStatus = 'REQUESTED' | 'CONFIRMED' | 'CANCELLED' | 'COMPLETED';

export interface Viewing {
  id: number;
  slotId: number;
  propertyId: number;
  buyerId: number;
  status: ViewingStatus;
  notes: string | null;
  createdAt: string;
  updatedAt: string;
  slotStartTime?: string | null;
  slotEndTime?: string | null;
  propertyTitle?: string | null;
}

export interface RequestViewingPayload {
  slotId: number;
  notes?: string;
}

export interface ViewingPage {
  content: Viewing[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}
