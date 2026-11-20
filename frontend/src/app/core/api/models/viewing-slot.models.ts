export interface CreateViewingSlotRequest {
  startTime: string;
  endTime: string;
}

export interface ViewingSlotResponse {
  id: number;
  propertyId: number;
  createdBy: number;
  startTime: string;
  endTime: string;
  createdAt: string;
}
