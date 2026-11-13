import { Routes } from '@angular/router';
import { MsalGuard } from '@azure/msal-angular';
import { roleGuard } from './core/auth/auth.guards';

export const routes: Routes = [
  { path: '', pathMatch: 'full', redirectTo: 'dashboard' },
  {
    path: 'login',
    data: { page: 'login' },
    loadComponent: () => import('./features/auth/login.component').then((m) => m.LoginComponent),
  },
  {
    path: 'quote',
    data: { page: 'quote' },
    loadComponent: () => import('./features/quote/quote.component').then((m) => m.QuoteComponent),
  },
  {
    path: '',
    canActivate: [MsalGuard],
    children: [
      {
        path: 'dashboard',
        data: { page: 'dashboard' },
        loadComponent: () => import('./features/dashboard/dashboard.component').then((m) => m.DashboardComponent),
      },
      {
        path: 'claims',
        data: { page: 'claims' },
        loadComponent: () => import('./features/claims/claim-list.component').then((m) => m.ClaimListComponent),
      },
      {
        path: 'claims/new',
        data: { page: 'file' },
        canActivate: [roleGuard('AGENT')],
        loadComponent: () => import('./features/claims/file-claim.component').then((m) => m.FileClaimComponent),
      },
      {
        path: 'claims/:id',
        data: { page: 'detail' },
        loadComponent: () => import('./features/claims/claim-detail.component').then((m) => m.ClaimDetailComponent),
      },
      {
        path: 'queue',
        data: { page: 'queue' },
        canActivate: [roleGuard('ADJUSTER')],
        loadComponent: () => import('./features/queue/approval-queue.component').then((m) => m.ApprovalQueueComponent),
      },
      {
        path: 'leads',
        data: { page: 'leads' },
        canActivate: [roleGuard('AGENT')],
        loadComponent: () => import('./features/leads/leads.component').then((m) => m.LeadsComponent),
      },
      {
        path: 'insights',
        data: { page: 'insights' },
        canActivate: [roleGuard('ADJUSTER', 'MANAGER')],
        loadComponent: () => import('./features/insights/model-insights.component').then((m) => m.ModelInsightsComponent),
      },
    ],
  },
  { path: '**', redirectTo: 'dashboard' },
];
