export interface CreateViewingSlotRequest {
  startTime: string;
  endTime: string;
}

export interface ViewingSlotResponse {
  id: number;
  propertyId: number;
  startTime: string;
  endTime: string;
  status: string;
}
