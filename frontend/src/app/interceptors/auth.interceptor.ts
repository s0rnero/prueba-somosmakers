import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, switchMap, throwError } from 'rxjs';
import { AuthService } from '../services/auth.service';

const AUTH_ENDPOINTS = ['/api/auth/login', '/api/auth/register', '/api/auth/refresh', '/api/auth/logout'];

export const authInterceptor: HttpInterceptorFn = (request, next) => {
  const isApiRequest = request.url.startsWith('/api');
  const isAuthEndpoint = AUTH_ENDPOINTS.some((endpoint) => request.url.startsWith(endpoint));

  if (!isApiRequest || isAuthEndpoint) return next(request);

  const auth = inject(AuthService);
  const accessToken = auth.accessToken();
  const authorized = accessToken
    ? request.clone({ setHeaders: { Authorization: `Bearer ${accessToken}` } })
    : request;

  return next(authorized).pipe(
    catchError((error: unknown) => {
      if (!(error instanceof HttpErrorResponse) || error.status !== 401) {
        return throwError(() => error);
      }

      return auth.refresh().pipe(
        switchMap(() => {
          const refreshedToken = auth.accessToken();
          if (!refreshedToken) return throwError(() => error);
          return next(request.clone({ setHeaders: { Authorization: `Bearer ${refreshedToken}` } }));
        }),
      );
    }),
  );
};
