import { Component, computed, input } from '@angular/core';
import { LoanStatus } from '../../../models/loan.model';

const LABELS: Record<LoanStatus, string> = {
  PENDING: 'Pendiente',
  APPROVED: 'Aprobado',
  REJECTED: 'Rechazado',
};

const CLASSES: Record<LoanStatus, string> = {
  PENDING: 'bg-amber-100 text-amber-800',
  APPROVED: 'bg-emerald-100 text-emerald-800',
  REJECTED: 'bg-red-100 text-red-700',
};

@Component({
  selector: 'app-loan-status-badge',
  templateUrl: './loan-status-badge.html',
})
export class LoanStatusBadge {
  readonly status = input.required<LoanStatus>();
  readonly label = computed(() => LABELS[this.status()]);
  readonly classes = computed(() => CLASSES[this.status()]);
}
