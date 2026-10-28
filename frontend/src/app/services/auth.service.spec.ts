import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { AuthResponse } from '../models/auth.model';
import { AuthService } from './auth.service';

const response: AuthResponse = {
  accessToken: 'access-token',
  tokenType: 'Bearer',
  expiresIn: 900,
  user: {
    id: 1,
    name: 'Cesar Rios',
    email: 'usuario@test.com',
    role: 'USER',
    createdAt: '2026-10-28T00:00:00Z',
    updatedAt: '2026-10-28T00:00:00Z',
  },
};

describe('AuthService', () => {
  let service: AuthService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(AuthService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    http.verify();
    TestBed.resetTestingModule();
  });

  it('stores the access token and user after login', () => {
    service.login({ email: 'usuario@test.com', password: 'user1234' }).subscribe();

    const request = http.expectOne('/api/auth/login');
    expect(request.request.withCredentials).toBe(true);
    request.flush(response);

    expect(service.accessToken()).toBe('access-token');
    expect(service.isAuthenticated()).toBe(true);
    expect(service.user()?.name).toBe('Cesar Rios');
  });

  it('clears the session when refresh fails', async () => {
    const restored = service.restoreSession();

    const csrf = http.expectOne('/api/auth/csrf');
    csrf.flush(null, { status: 204, statusText: 'No Content' });

    const request = http.expectOne('/api/auth/refresh');
    request.flush({}, { status: 401, statusText: 'Unauthorized' });
    await restored;

    expect(service.accessToken()).toBeNull();
    expect(service.isAuthenticated()).toBe(false);
  });
});
