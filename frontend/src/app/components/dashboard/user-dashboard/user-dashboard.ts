import { CurrencyPipe, DatePipe } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Loan } from '../../../models/loan.model';
import { LoanService } from '../../../services/loan.service';
import { LoanStatusBadge } from '../../loans/loan-status-badge/loan-status-badge';

@Component({
  selector: 'app-user-dashboard',
  imports: [ReactiveFormsModule, CurrencyPipe, DatePipe, LoanStatusBadge],
  templateUrl: './user-dashboard.html',
})
export class UserDashboard {
  private readonly loanService = inject(LoanService);
  private readonly formBuilder = inject(FormBuilder);

  readonly loans = signal<Loan[]>([]);
  readonly submitting = signal(false);
  readonly message = signal<string | null>(null);
  readonly error = signal<string | null>(null);

  readonly form = this.formBuilder.nonNullable.group({
    amount: [1000, [Validators.required, Validators.min(100), Validators.max(50000)]],
    termMonths: [12, [Validators.required, Validators.min(1), Validators.max(60)]],
  });

  constructor() {
    this.loanService
      .watchMine()
      .pipe(takeUntilDestroyed())
      .subscribe(loans => this.loans.set(loans));
  }

  submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    this.submitting.set(true);
    this.message.set(null);
    this.error.set(null);

    const { amount, termMonths } = this.form.getRawValue();
    this.loanService.create({ amount, termMonths }).subscribe({
      next: loan => {
        this.loans.update(loans => [loan, ...loans]);
        this.form.reset({ amount: 1000, termMonths: 12 });
        this.message.set('Solicitud enviada. Queda pendiente de aprobación.');
        this.submitting.set(false);
      },
      error: (response: { error?: { message?: string } }) => {
        this.error.set(response.error?.message ?? 'No se pudo enviar la solicitud.');
        this.submitting.set(false);
      },
    });
  }
}
