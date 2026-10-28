import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, catchError, filter, of, shareReplay, switchMap, timer } from 'rxjs';
import { CreateLoanRequest, Loan, LoanDecision } from '../models/loan.model';

const REFRESH_MS = 4000;

@Injectable({ providedIn: 'root' })
export class LoanService {
  private readonly http = inject(HttpClient);

  listMine(): Observable<Loan[]> {
    return this.http.get<Loan[]>('/api/loans/mine');
  }

  listAll(): Observable<Loan[]> {
    return this.http.get<Loan[]>('/api/loans');
  }

  create(request: CreateLoanRequest): Observable<Loan> {
    return this.http.post<Loan>('/api/loans', request);
  }

  decide(id: number, decision: LoanDecision): Observable<Loan> {
    return this.http.patch<Loan>(`/api/loans/${id}/decision`, { decision });
  }

  watchMine(): Observable<Loan[]> {
    return this.watch(() => this.listMine());
  }

  watchAll(): Observable<Loan[]> {
    return this.watch(() => this.listAll());
  }

  private watch(load: () => Observable<Loan[]>): Observable<Loan[]> {
    return timer(0, REFRESH_MS).pipe(
      switchMap(() => load().pipe(catchError(() => of(null)))),
      filter((loans): loans is Loan[] => loans !== null),
      shareReplay({ bufferSize: 1, refCount: true }),
    );
  }
}
