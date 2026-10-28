import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, map } from 'rxjs';
import { User, UserRole } from '../models/user.model';

export interface UserPayload {
  name: string;
  email: string;
  role: UserRole;
  password?: string;
}

interface UsersPage {
  content?: User[];
}

@Injectable({ providedIn: 'root' })
export class UserService {
  private readonly http = inject(HttpClient);

  list(): Observable<User[]> {
    const params = new HttpParams().set('size', '100');
    return this.http
      .get<User[] | UsersPage>('/api/users', { params })
      .pipe(map(response => (Array.isArray(response) ? response : (response.content ?? []))));
  }

  create(payload: UserPayload): Observable<User> {
    return this.http.post<User>('/api/users', payload);
  }

  update(id: number, payload: UserPayload): Observable<User> {
    return this.http.put<User>(`/api/users/${id}`, payload);
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`/api/users/${id}`);
  }
}
