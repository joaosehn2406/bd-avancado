import { Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { ShipmentService } from '../../core/services/shipment.service';
import { HubService } from '../../core/services/hub.service';
import { ShipmentRequest, ShipmentResponse, EventRequest } from '../../core/models/shipment.model';
import { EventStatusResponse } from '../../core/models/tracking.model';
import { CityActivityResponse, StatusActivityResponse } from '../../core/models/hub.model';

type Tab = 'shipments' | 'events' | 'hub' | 'status';

@Component({
  selector: 'app-admin',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './admin.component.html',
  styleUrl: './admin.component.scss'
})
export class AdminComponent {
  private readonly shipmentService = inject(ShipmentService);
  private readonly hubService = inject(HubService);
  private readonly router = inject(Router);

  readonly activeTab = signal<Tab>('shipments');

  // ── Criar encomenda ──────────────────────────────────────────
  shipmentForm: ShipmentRequest = { sender: '', recipient: '', origin: '', destination: '', weightKg: 0 };
  readonly createdShipment = signal<ShipmentResponse | null>(null);
  readonly shipmentError = signal<string | null>(null);
  readonly shipmentLoading = signal(false);

  // ── Buscar encomenda ─────────────────────────────────────────
  searchCode = '';
  readonly foundShipment = signal<ShipmentResponse | null>(null);
  readonly searchError = signal<string | null>(null);
  readonly searchLoading = signal(false);
  readonly deleteConfirm = signal(false);

  // ── Registrar evento ─────────────────────────────────────────
  eventCode = '';
  eventForm: EventRequest = { state: '', city: '', status: 3, latitude: null, longitude: null, notes: null };
  readonly eventSuccess = signal(false);
  readonly eventError = signal<string | null>(null);
  readonly eventLoading = signal(false);

  // ── Hub (atividade por cidade) ────────────────────────────────
  hubCity = '';
  readonly hubResult = signal<CityActivityResponse | null>(null);
  readonly hubError = signal<string | null>(null);
  readonly hubLoading = signal(false);

  // ── Status (pacotes por status hoje) ─────────────────────────
  statusSelectedId = 3;
  readonly statusResult = signal<StatusActivityResponse | null>(null);
  readonly statusError = signal<string | null>(null);
  readonly statusLoading = signal(false);

  readonly statusOptions = [
    { id: 0, name: 'Registrado' },
    { id: 1, name: 'Coletado' },
    { id: 2, name: 'Em separação' },
    { id: 3, name: 'Em trânsito' },
    { id: 4, name: 'Saiu para entrega' },
    { id: 5, name: 'Entregue' }
  ];

  setTab(tab: Tab) {
    this.activeTab.set(tab);
  }

  // ── Actions: criar encomenda ──────────────────────────────────
  createShipment() {
    this.shipmentLoading.set(true);
    this.shipmentError.set(null);
    this.createdShipment.set(null);

    this.shipmentService.create(this.shipmentForm).subscribe({
      next: (res) => {
        this.createdShipment.set(res);
        this.shipmentLoading.set(false);
        this.shipmentForm = { sender: '', recipient: '', origin: '', destination: '', weightKg: 0 };
      },
      error: (err) => {
        this.shipmentLoading.set(false);
        this.shipmentError.set(err.status === 409
          ? 'Código de rastreio já existe (LWT). Tente novamente.'
          : 'Erro ao criar encomenda. Verifique os dados.');
      }
    });
  }

  // ── Actions: buscar encomenda ─────────────────────────────────
  searchShipment() {
    if (!this.searchCode.trim()) return;
    this.searchLoading.set(true);
    this.searchError.set(null);
    this.foundShipment.set(null);
    this.deleteConfirm.set(false);

    this.shipmentService.getByCode(this.searchCode.trim().toUpperCase()).subscribe({
      next: (res) => {
        this.foundShipment.set(res);
        this.searchLoading.set(false);
      },
      error: (err) => {
        this.searchLoading.set(false);
        this.searchError.set(err.status === 404 ? 'Encomenda não encontrada.' : 'Erro ao buscar encomenda.');
      }
    });
  }

  deleteShipment() {
    const shipment = this.foundShipment();
    if (!shipment) return;
    this.shipmentService.delete(shipment.trackingCode).subscribe({
      next: () => {
        this.foundShipment.set(null);
        this.searchCode = '';
        this.deleteConfirm.set(false);
      },
      error: () => { this.searchError.set('Erro ao deletar encomenda.'); }
    });
  }

  viewTracking(code: string) {
    this.router.navigate(['/rastreio', code]);
  }

  // ── Actions: registrar evento ─────────────────────────────────
  registerEvent() {
    if (!this.eventCode.trim()) return;
    this.eventLoading.set(true);
    this.eventError.set(null);
    this.eventSuccess.set(false);

    this.shipmentService.registerEvent(this.eventCode.trim().toUpperCase(), this.eventForm).subscribe({
      next: () => {
        this.eventSuccess.set(true);
        this.eventLoading.set(false);
        this.eventForm = { state: '', city: '', status: 3, latitude: null, longitude: null, notes: null };
      },
      error: (err) => {
        this.eventLoading.set(false);
        this.eventError.set(err.status === 404
          ? 'Encomenda não encontrada.'
          : 'Erro ao registrar evento.');
      }
    });
  }

  // ── Actions: hub activity ────────────────────────────────────
  loadHubActivity() {
    if (!this.hubCity.trim()) return;
    this.hubLoading.set(true);
    this.hubError.set(null);
    this.hubResult.set(null);

    this.hubService.getCityActivity(this.hubCity.trim()).subscribe({
      next: (res) => {
        this.hubResult.set(res);
        this.hubLoading.set(false);
      },
      error: () => {
        this.hubLoading.set(false);
        this.hubError.set('Erro ao buscar atividade do hub.');
      }
    });
  }

  // ── Actions: status activity ─────────────────────────────────
  loadStatusActivity() {
    this.statusLoading.set(true);
    this.statusError.set(null);
    this.statusResult.set(null);

    this.hubService.getStatusActivity(this.statusSelectedId).subscribe({
      next: (res) => {
        this.statusResult.set(res);
        this.statusLoading.set(false);
      },
      error: () => {
        this.statusLoading.set(false);
        this.statusError.set('Erro ao buscar pacotes por status.');
      }
    });
  }

  formatDate(iso: string): string {
    return new Date(iso).toLocaleString('pt-BR', {
      day: '2-digit', month: '2-digit', year: 'numeric',
      hour: '2-digit', minute: '2-digit'
    });
  }

  statusAsResponse(id: number): EventStatusResponse {
    const opt = this.statusOptions.find(s => s.id === id)!;
    return opt;
  }

  goHome() {
    this.router.navigate(['/']);
  }
}
