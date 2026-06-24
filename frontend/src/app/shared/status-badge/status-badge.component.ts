import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { EventStatusResponse } from '../../core/models/tracking.model';

@Component({
  selector: 'app-status-badge',
  standalone: true,
  imports: [CommonModule],
  template: `<span class="badge status-{{ status.id }}">{{ status.name }}</span>`,
  styles: [`
    .badge {
      display: inline-block;
      padding: 3px 10px;
      border-radius: 12px;
      font-size: 0.78rem;
      font-weight: 600;
      letter-spacing: 0.02em;
      white-space: nowrap;
    }
    .status-0 { background: #e2e8f0; color: #475569; }
    .status-1 { background: #dbeafe; color: #1d4ed8; }
    .status-2 { background: #fef3c7; color: #b45309; }
    .status-3 { background: #ede9fe; color: #6d28d9; }
    .status-4 { background: #cffafe; color: #0e7490; }
    .status-5 { background: #d1fae5; color: #065f46; }
  `]
})
export class StatusBadgeComponent {
  @Input({ required: true }) status!: EventStatusResponse;
}
