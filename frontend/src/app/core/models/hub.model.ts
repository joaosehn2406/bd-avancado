import { EventStatusResponse } from './tracking.model';

export interface CityEventItem {
  trackingCode: string;
  timestamp: string;
  status: EventStatusResponse;
}

export interface CityActivityResponse {
  city: string;
  date: string;
  events: CityEventItem[];
}

export interface StatusEventItem {
  trackingCode: string;
  timestamp: string;
  city: string;
}

export interface StatusActivityResponse {
  status: EventStatusResponse;
  date: string;
  events: StatusEventItem[];
}
