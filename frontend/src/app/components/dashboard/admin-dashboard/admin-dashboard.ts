import { CurrencyPipe, DatePipe } from '@angular/common';
import { Component, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { Loan, LoanDecision } from '../../../models/loan.model';
import { LoanService } from '../../../services/loan.service';
import { LoanStatusBadge } from '../../loans/loan-status-badge/loan-status-badge';
import { UserManagement } from '../user-management/user-management';

@Component({
  selector: 'app-admin-dashboard',
  imports: [CurrencyPipe, DatePipe, LoanStatusBadge, UserManagement],
  templateUrl: './admin-dashboard.html',
})
export class AdminDashboard {
  private readonly loanService = inject(LoanService);

  readonly loans = signal<Loan[]>([]);
  readonly decidingId = signal<number | null>(null);
  readonly error = signal<string | null>(null);

  readonly pending = computed(() => this.loans().filter(loan => loan.status === 'PENDING').length);
  readonly approved = computed(() => this.loans().filter(loan => loan.status === 'APPROVED').length);
  readonly rejected = computed(() => this.loans().filter(loan => loan.status === 'REJECTED').length);

  constructor() {
    this.loanService
      .watchAll()
      .pipe(takeUntilDestroyed())
      .subscribe(loans => this.loans.set(loans));
  }

  decide(loan: Loan, decision: LoanDecision): void {
    this.decidingId.set(loan.id);
    this.error.set(null);

    this.loanService.decide(loan.id, decision).subscribe({
      next: updated => {
        this.loans.update(loans => loans.map(item => (item.id === updated.id ? updated : item)));
        this.decidingId.set(null);
      },
      error: (response: { error?: { message?: string } }) => {
        this.error.set(response.error?.message ?? 'No se pudo actualizar el préstamo.');
        this.decidingId.set(null);
      },
    });
  }
}
