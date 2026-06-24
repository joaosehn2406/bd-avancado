import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { TrackingResponse } from '../models/tracking.model';

@Injectable({ providedIn: 'root' })
export class TrackingService {
  private readonly http = inject(HttpClient);
  private readonly base = '/api/rastreio';

  getByCode(code: string): Observable<TrackingResponse> {
    return this.http.get<TrackingResponse>(`${this.base}/${code}`);
  }

  openStream(code: string): EventSource {
    return new EventSource(`${this.base}/${code}/stream`);
  }
}
