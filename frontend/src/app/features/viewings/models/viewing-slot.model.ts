export interface ViewingSlot {
  id: number;
  propertyId: number;
  createdBy: number;
  startTime: string;
  endTime: string;
  createdAt: string;
}

export interface CreateViewingSlotPayload {
  startTime: string;
  endTime: string;
}
