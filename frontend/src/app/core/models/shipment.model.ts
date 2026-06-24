export interface ShipmentRequest {
  sender: string;
  recipient: string;
  origin: string;
  destination: string;
  weightKg: number;
}

export interface ShipmentResponse {
  trackingCode: string;
  sender: string;
  recipient: string;
  origin: string;
  destination: string;
  createdAt: string;
  weightKg: number;
  qrCode: string | null;
}

export interface EventRequest {
  state: string;
  city: string;
  status: number;
  latitude: number | null;
  longitude: number | null;
  notes: string | null;
}
