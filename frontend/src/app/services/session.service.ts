import { HttpClient } from '@angular/common/http';
import { Injectable, computed, inject, signal } from '@angular/core';

export type SessionRole = 'ADMIN' | 'USER';

export interface SessionUser {
  name: string;
  email: string;
  role: SessionRole;
}

interface SessionResponse {
  name?: string;
  email?: string;
  role?: string;
  roles?: string[];
}

@Injectable({ providedIn: 'root' })
export class SessionService {
  private readonly http = inject(HttpClient);
  private readonly _user = signal<SessionUser | null>(null);
  private readonly _failed = signal(false);

  readonly user = this._user.asReadonly();
  readonly failed = this._failed.asReadonly();
  readonly isAdmin = computed(() => this._user()?.role === 'ADMIN');

  load(): void {
    this._failed.set(false);
    this.http.get<SessionResponse>('/api/auth/me').subscribe({
      next: response => this._user.set(this.normalize(response)),
      error: () => this._failed.set(true),
    });
  }

  logout(): void {
    this._user.set(null);
    this.http.post('/api/auth/logout', {}).subscribe({ error: () => undefined });
  }

  private normalize(response: SessionResponse): SessionUser {
    const rawRoles = response.roles ?? (response.role ? [response.role] : []);
    const roles = rawRoles.map(role => role.replace('ROLE_', ''));
    const email = response.email ?? '';
    return {
      name: response.name?.trim() || email.split('@')[0] || 'Usuario',
      email,
      role: roles.includes('ADMIN') ? 'ADMIN' : 'USER',
    };
  }
}
