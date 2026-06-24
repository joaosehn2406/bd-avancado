import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { ShipmentService } from '../../core/services/shipment.service';
import { ShipmentRequest, ShipmentResponse, EventRequest } from '../../core/models/shipment.model';
import { EventStatusResponse } from '../../core/models/tracking.model';

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
  private readonly router = inject(Router);

  activeTab: Tab = 'shipments';

  // ── Criar encomenda ──────────────────────────────────────────
  shipmentForm: ShipmentRequest = { sender: '', recipient: '', origin: '', destination: '', weightKg: 0 };
  createdShipment: ShipmentResponse | null = null;
  shipmentError: string | null = null;
  shipmentLoading = false;

  // ── Buscar encomenda ─────────────────────────────────────────
  searchCode = '';
  foundShipment: ShipmentResponse | null = null;
  searchError: string | null = null;
  searchLoading = false;
  deleteConfirm = false;

  // ── Registrar evento ─────────────────────────────────────────
  eventCode = '';
  eventForm: EventRequest = { state: '', city: '', status: 3, latitude: null, longitude: null, notes: null };
  eventSuccess = false;
  eventError: string | null = null;
  eventLoading = false;

  readonly statusOptions = [
    { id: 0, name: 'Registrado' },
    { id: 1, name: 'Coletado' },
    { id: 2, name: 'Em separação' },
    { id: 3, name: 'Em trânsito' },
    { id: 4, name: 'Saiu para entrega' },
    { id: 5, name: 'Entregue' }
  ];

  setTab(tab: Tab) {
    this.activeTab = tab;
  }

  // ── Actions: criar encomenda ──────────────────────────────────
  createShipment() {
    this.shipmentLoading = true;
    this.shipmentError = null;
    this.createdShipment = null;

    this.shipmentService.create(this.shipmentForm).subscribe({
      next: (res) => {
        this.createdShipment = res;
        this.shipmentLoading = false;
        this.shipmentForm = { sender: '', recipient: '', origin: '', destination: '', weightKg: 0 };
      },
      error: (err) => {
        this.shipmentLoading = false;
        this.shipmentError = err.status === 409
          ? 'Código de rastreio já existe (LWT). Tente novamente.'
          : 'Erro ao criar encomenda. Verifique os dados.';
      }
    });
  }

  // ── Actions: buscar encomenda ─────────────────────────────────
  searchShipment() {
    if (!this.searchCode.trim()) return;
    this.searchLoading = true;
    this.searchError = null;
    this.foundShipment = null;
    this.deleteConfirm = false;

    this.shipmentService.getByCode(this.searchCode.trim().toUpperCase()).subscribe({
      next: (res) => {
        this.foundShipment = res;
        this.searchLoading = false;
      },
      error: (err) => {
        this.searchLoading = false;
        this.searchError = err.status === 404 ? 'Encomenda não encontrada.' : 'Erro ao buscar encomenda.';
      }
    });
  }

  deleteShipment() {
    if (!this.foundShipment) return;
    this.shipmentService.delete(this.foundShipment.trackingCode).subscribe({
      next: () => {
        this.foundShipment = null;
        this.searchCode = '';
        this.deleteConfirm = false;
      },
      error: () => { this.searchError = 'Erro ao deletar encomenda.'; }
    });
  }

  viewTracking(code: string) {
    this.router.navigate(['/rastreio', code]);
  }

  // ── Actions: registrar evento ─────────────────────────────────
  registerEvent() {
    if (!this.eventCode.trim()) return;
    this.eventLoading = true;
    this.eventError = null;
    this.eventSuccess = false;

    this.shipmentService.registerEvent(this.eventCode.trim().toUpperCase(), this.eventForm).subscribe({
      next: () => {
        this.eventSuccess = true;
        this.eventLoading = false;
        this.eventForm = { state: '', city: '', status: 3, latitude: null, longitude: null, notes: null };
      },
      error: (err) => {
        this.eventLoading = false;
        this.eventError = err.status === 404
          ? 'Encomenda não encontrada.'
          : 'Endpoint ainda não implementado no backend (POST /shipments/{code}/eventos).';
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
