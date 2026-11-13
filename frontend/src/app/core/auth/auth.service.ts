import { HttpClient } from '@angular/common/http';
import { Injectable, computed, inject, signal } from '@angular/core';
import { toObservable } from '@angular/core/rxjs-interop';
import { MsalBroadcastService, MsalService } from '@azure/msal-angular';
import { InteractionStatus } from '@azure/msal-browser';
import { Observable, filter, map, take } from 'rxjs';
import { Role, UserProfile } from '../models/auth.models';
import { SIGN_IN_CONFIG } from './sign-in.config';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly msal = inject(MsalService);
  private readonly config = inject(SIGN_IN_CONFIG);
  private readonly profile = signal<UserProfile | null>(null);
  private readonly settled = signal(false);
  readonly signInError = signal('');

  readonly user = this.profile.asReadonly();
  readonly isSignedIn = computed(() => this.profile() !== null);
  readonly role = computed<Role | null>(() => this.profile()?.role ?? null);

  constructor() {
    this.msal.handleRedirectObservable().subscribe();
    inject(MsalBroadcastService)
      .inProgress$.pipe(filter((status) => status === InteractionStatus.None))
      .subscribe(() => this.refreshAccount());
  }

  hasRole(...roles: Role[]): boolean {
    const role = this.role();
    return role !== null && roles.includes(role);
  }

  whenSettled(): Observable<UserProfile | null> {
    return toObservable(this.settled).pipe(filter(Boolean), take(1), map(() => this.profile()));
  }

  login(returnTo = '/dashboard'): void {
    this.msal.loginRedirect({ scopes: this.config.apiScopes, redirectStartPage: window.location.origin + returnTo });
  }

  logout(): void {
    const account = this.msal.instance.getActiveAccount();
    this.profile.set(null);
    this.msal.logoutRedirect({ account });
  }

  private refreshAccount(): void {
    const instance = this.msal.instance;
    const account = instance.getActiveAccount() ?? instance.getAllAccounts()[0] ?? null;
    if (!account) {
      this.settled.set(true);
      return;
    }
    instance.setActiveAccount(account);
    this.http.get<UserProfile>('/api/auth/me').subscribe({
      next: (profile) => {
        this.profile.set(profile);
        this.settled.set(true);
      },
      error: () => {
        this.signInError.set('Your Microsoft account is signed in but has no Notsureance role yet. Ask an admin to assign one.');
        this.settled.set(true);
      },
    });
  }
}
