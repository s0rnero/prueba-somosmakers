import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from '../services/auth.service';

export const dashboardHomeGuard: CanActivateFn = () => {
  const auth = inject(AuthService);

  if (!auth.isAuthenticated()) {
    return inject(Router).createUrlTree(['/login']);
  }

  if (auth.user()?.role === 'ADMIN') {
    return inject(Router).createUrlTree(['/dashboard/admin']);
  }

  return true;
};
