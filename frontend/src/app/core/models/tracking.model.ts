export interface EventStatusResponse {
  id: number;
  name: string;
}

export interface TimelineEvent {
  timestamp: string;
  state: string | null;
  city: string;
  status: EventStatusResponse;
  latitude: number | null;
  longitude: number | null;
  notes: string | null;
}

export interface TrackingResponse {
  trackingCode: string;
  origin: string;
  destination: string;
  createdAt: string;
  currentStatus: EventStatusResponse;
  events: TimelineEvent[];
}
