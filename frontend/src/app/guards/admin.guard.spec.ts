import { signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { provideRouter, UrlTree } from '@angular/router';
import { User } from '../models/user.model';
import { AuthService } from '../services/auth.service';
import { adminGuard } from './admin.guard';
import { dashboardHomeGuard } from './dashboard-home.guard';

const admin: User = {
  id: 1,
  name: 'Admin',
  email: 'admin@test.com',
  role: 'ADMIN',
  createdAt: '2026-01-01T00:00:00Z',
  updatedAt: '2026-01-01T00:00:00Z',
};

const user: User = { ...admin, id: 2, name: 'Usuario', email: 'usuario@test.com', role: 'USER' };

function setup(sessionUser: User | null): void {
  const auth = {
    user: signal<User | null>(sessionUser),
    isAuthenticated: () => sessionUser !== null,
  } as unknown as AuthService;

  TestBed.configureTestingModule({
    providers: [{ provide: AuthService, useValue: auth }, provideRouter([])],
  });
}

describe('adminGuard', () => {
  afterEach(() => TestBed.resetTestingModule());

  it('allows an administrator', () => {
    setup(admin);

    const result = TestBed.runInInjectionContext(() => adminGuard({} as never, {} as never));

    expect(result).toBe(true);
  });

  it('redirects a normal user to their dashboard', () => {
    setup(user);

    const result = TestBed.runInInjectionContext(() => adminGuard({} as never, {} as never));

    expect(result).toBeInstanceOf(UrlTree);
    expect((result as UrlTree).toString()).toBe('/dashboard/user');
  });

  it('redirects an anonymous visitor to login', () => {
    setup(null);

    const result = TestBed.runInInjectionContext(() => adminGuard({} as never, {} as never));

    expect((result as UrlTree).toString()).toBe('/login');
  });
});

describe('dashboardHomeGuard', () => {
  afterEach(() => TestBed.resetTestingModule());

  it('sends an administrator to the admin dashboard', () => {
    setup(admin);

    const result = TestBed.runInInjectionContext(() => dashboardHomeGuard({} as never, {} as never));

    expect((result as UrlTree).toString()).toBe('/dashboard/admin');
  });

  it('keeps a normal user on the dashboard', () => {
    setup(user);

    const result = TestBed.runInInjectionContext(() => dashboardHomeGuard({} as never, {} as never));

    expect(result).toBe(true);
  });
});
