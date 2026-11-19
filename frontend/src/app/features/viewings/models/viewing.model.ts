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
}

export interface RequestViewingPayload {
  slotId: number;
  notes?: string;
}
