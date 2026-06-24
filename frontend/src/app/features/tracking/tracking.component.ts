import { Component, OnInit, OnDestroy, AfterViewChecked, inject } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { CommonModule } from '@angular/common';
import { TrackingService } from '../../core/services/tracking.service';
import { TrackingResponse, TimelineEvent } from '../../core/models/tracking.model';
import { StatusBadgeComponent } from '../../shared/status-badge/status-badge.component';
import * as L from 'leaflet';

@Component({
  selector: 'app-tracking',
  standalone: true,
  imports: [CommonModule, StatusBadgeComponent],
  templateUrl: './tracking.component.html',
  styleUrl: './tracking.component.scss'
})
export class TrackingComponent implements OnInit, OnDestroy, AfterViewChecked {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly trackingService = inject(TrackingService);

  tracking: TrackingResponse | null = null;
  loading = true;
  errorMessage: string | null = null;

  private map: L.Map | null = null;
  private mapInitialized = false;
  private eventSource: EventSource | null = null;

  ngOnInit() {
    const code = this.route.snapshot.paramMap.get('code')!;
    this.loadTracking(code);
  }

  ngAfterViewChecked() {
    if (this.tracking && !this.mapInitialized) {
      const eventsWithCoords = this.eventsWithCoords();
      if (eventsWithCoords.length > 0) {
        this.initMap(eventsWithCoords);
      }
    }
  }

  ngOnDestroy() {
    this.eventSource?.close();
    this.map?.remove();
  }

  private loadTracking(code: string) {
    this.trackingService.getByCode(code).subscribe({
      next: (data) => {
        this.tracking = data;
        this.loading = false;
        this.connectSSE(code);
      },
      error: (err) => {
        this.loading = false;
        this.errorMessage = err.status === 404
          ? 'Código de rastreio não encontrado.'
          : 'Erro ao consultar o servidor. Tente novamente.';
      }
    });
  }

  private connectSSE(code: string) {
    this.eventSource = this.trackingService.openStream(code);
    this.eventSource.onmessage = (event) => {
      const newEvent: TimelineEvent = JSON.parse(event.data);
      if (this.tracking) {
        this.tracking = {
          ...this.tracking,
          currentStatus: newEvent.status,
          events: [newEvent, ...this.tracking.events]
        };
        this.mapInitialized = false;
      }
    };
    this.eventSource.onerror = () => this.eventSource?.close();
  }

  eventsWithCoords(): TimelineEvent[] {
    return (this.tracking?.events ?? [])
      .filter(e => e.latitude != null && e.longitude != null)
      .slice()
      .reverse();
  }

  private initMap(events: TimelineEvent[]) {
    const el = document.getElementById('route-map');
    if (!el || this.mapInitialized) return;

    this.mapInitialized = true;

    this.map = L.map('route-map', { zoomControl: true });
    L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
      attribution: '© OpenStreetMap contributors'
    }).addTo(this.map);

    const coords = events.map(e => [e.latitude!, e.longitude!] as [number, number]);

    L.polyline(coords, { color: '#3b82f6', weight: 3 }).addTo(this.map);

    events.forEach((e, i) => {
      const isLast = i === events.length - 1;
      const icon = L.divIcon({
        className: '',
        html: `<div class="map-pin ${isLast ? 'map-pin--current' : ''}"></div>`,
        iconSize: [14, 14],
        iconAnchor: [7, 7]
      });
      L.marker([e.latitude!, e.longitude!], { icon })
        .bindPopup(`<b>${e.city}</b><br>${e.status.name}<br><small>${this.formatDate(e.timestamp)}</small>`)
        .addTo(this.map!);
    });

    this.map.fitBounds(L.latLngBounds(coords), { padding: [32, 32] });
  }

  formatDate(iso: string): string {
    return new Date(iso).toLocaleString('pt-BR', {
      day: '2-digit', month: '2-digit', year: 'numeric',
      hour: '2-digit', minute: '2-digit'
    });
  }

  goBack() {
    this.router.navigate(['/']);
  }
}
