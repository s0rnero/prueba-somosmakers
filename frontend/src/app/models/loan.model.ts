export type LoanStatus = 'PENDING' | 'APPROVED' | 'REJECTED';
export type LoanDecision = 'APPROVED' | 'REJECTED';

export interface Loan {
  id: number;
  userName: string;
  userEmail: string;
  amount: number;
  termMonths: number;
  status: LoanStatus;
  requestedAt: string;
  decidedAt: string | null;
  decidedBy: string | null;
}

export interface CreateLoanRequest {
  amount: number;
  termMonths: number;
}
