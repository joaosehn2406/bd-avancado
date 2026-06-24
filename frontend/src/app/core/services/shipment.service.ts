import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { EventRequest, ShipmentRequest, ShipmentResponse } from '../models/shipment.model';

@Injectable({ providedIn: 'root' })
export class ShipmentService {
  private readonly http = inject(HttpClient);
  private readonly base = '/api/shipments';

  create(request: ShipmentRequest): Observable<ShipmentResponse> {
    return this.http.post<ShipmentResponse>(this.base, request);
  }

  getByCode(code: string): Observable<ShipmentResponse> {
    return this.http.get<ShipmentResponse>(`${this.base}/${code}`);
  }

  delete(code: string): Observable<void> {
    return this.http.delete<void>(`${this.base}/${code}`);
  }

  registerEvent(code: string, event: EventRequest): Observable<void> {
    return this.http.post<void>(`${this.base}/${code}/eventos`, event);
  }
}
