import { Routes } from '@angular/router';
import { adminGuard } from './guards/admin.guard';
import { authGuard } from './guards/auth.guard';
import { dashboardHomeGuard } from './guards/dashboard-home.guard';
import { guestGuard } from './guards/guest.guard';

export const routes: Routes = [
  { path: '', pathMatch: 'full', redirectTo: 'dashboard' },
  {
    path: 'login',
    canActivate: [guestGuard],
    loadComponent: () => import('./pages/login/login').then((module) => module.LoginPage),
  },
  {
    path: 'dashboard',
    canActivate: [authGuard],
    loadComponent: () => import('./pages/dashboard/dashboard').then((module) => module.DashboardPage),
    children: [
      {
        path: '',
        pathMatch: 'full',
        canActivate: [dashboardHomeGuard],
        loadComponent: () =>
          import('./components/dashboard/user-dashboard/user-dashboard').then(
            (module) => module.UserDashboard,
          ),
      },
      {
        path: 'user',
        loadComponent: () =>
          import('./components/dashboard/user-dashboard/user-dashboard').then(
            (module) => module.UserDashboard,
          ),
      },
      {
        path: 'admin',
        canActivate: [adminGuard],
        loadComponent: () =>
          import('./components/dashboard/admin-dashboard/admin-dashboard').then(
            (module) => module.AdminDashboard,
          ),
      },
    ],
  },
  {
    path: '**',
    canActivate: [authGuard],
    loadComponent: () => import('./pages/not-found/not-found').then((module) => module.NotFoundPage),
  },
];
