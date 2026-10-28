import { HttpClient } from '@angular/common/http';
import { computed, inject, Injectable, signal } from '@angular/core';
import { catchError, finalize, firstValueFrom, Observable, of, switchMap, tap, throwError } from 'rxjs';
import { AuthResponse, LoginPayload, RegisterPayload } from '../models/auth.model';
import { User } from '../models/user.model';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly _user = signal<User | null>(null);
  private readonly _accessToken = signal<string | null>(null);
  private refreshRequest: Observable<AuthResponse> | null = null;
  private csrfReady = false;

  readonly user = this._user.asReadonly();
  readonly accessToken = this._accessToken.asReadonly();
  readonly isAuthenticated = computed(() => this._user() !== null);

  login(payload: LoginPayload): Observable<AuthResponse> {
    return this.http
      .post<AuthResponse>('/api/auth/login', payload, { withCredentials: true })
      .pipe(tap((response) => this.setSession(response)));
  }

  register(payload: RegisterPayload): Observable<AuthResponse> {
    return this.http
      .post<AuthResponse>('/api/auth/register', payload, { withCredentials: true })
      .pipe(tap((response) => this.setSession(response)));
  }

  refresh(): Observable<AuthResponse> {
    if (this.refreshRequest) return this.refreshRequest;

    this.refreshRequest = this.ensureCsrf()
      .pipe(
        switchMap(() => this.http.post<AuthResponse>('/api/auth/refresh', null, {
          withCredentials: true,
          headers: this.csrfHeaders(),
        })),
        tap((response) => this.setSession(response)),
        catchError((error: unknown) => {
          this.clearSession();
          return throwError(() => error);
        }),
        finalize(() => {
          this.refreshRequest = null;
        }),
      );

    return this.refreshRequest;
  }

  async restoreSession(): Promise<void> {
    try {
      await firstValueFrom(this.refresh());
    } catch {
      this.clearSession();
    }
  }

  logout(): Observable<void> {
    return this.http.post<void>('/api/auth/logout', null, { withCredentials: true }).pipe(
      catchError(() => of(undefined)),
      finalize(() => this.clearSession()),
    );
  }

  me(): Observable<User> {
    return this.http.get<User>('/api/auth/me').pipe(tap((user) => this._user.set(user)));
  }

  private ensureCsrf(): Observable<void> {
    if (this.csrfReady) return of(undefined);

    return this.http.get<void>('/api/auth/csrf', { withCredentials: true }).pipe(
      tap(() => {
        this.csrfReady = true;
      }),
      catchError((error: unknown) => {
        this.csrfReady = false;
        return throwError(() => error);
      }),
    );
  }

  private csrfHeaders(): Record<string, string> | undefined {
    const match = document.cookie.match(/(?:^|; )XSRF-TOKEN=([^;]+)/);
    return match ? { 'X-XSRF-TOKEN': decodeURIComponent(match[1]) } : undefined;
  }

  private setSession(response: AuthResponse): void {
    this._accessToken.set(response.accessToken);
    this._user.set(response.user);
  }

  private clearSession(): void {
    this._accessToken.set(null);
    this._user.set(null);
  }
}
