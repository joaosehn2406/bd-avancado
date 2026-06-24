import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { CityActivityResponse, StatusActivityResponse } from '../models/hub.model';

@Injectable({ providedIn: 'root' })
export class HubService {
  private readonly http = inject(HttpClient);

  getCityActivity(city: string): Observable<CityActivityResponse> {
    return this.http.get<CityActivityResponse>(`/api/hubs/${encodeURIComponent(city)}/hoje`);
  }

  getStatusActivity(statusId: number): Observable<StatusActivityResponse> {
    return this.http.get<StatusActivityResponse>(`/api/status/${statusId}/hoje`);
  }
}
