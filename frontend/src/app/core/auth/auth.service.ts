import { HttpClient } from '@angular/common/http';
import { Injectable, computed, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { Observable, tap } from 'rxjs';
import { Role, TokenResponse, UserProfile } from '../models/auth.models';

const STORAGE_KEY = 'nsr.session';

interface Session {
  token: string;
  expiresAt: string;
  user: UserProfile;
}

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly router = inject(Router);
  private readonly session = signal<Session | null>(this.restore());
  private expiryTimer?: ReturnType<typeof setTimeout>;

  readonly user = computed(() => this.session()?.user ?? null);
  readonly isSignedIn = computed(() => this.session() !== null);
  readonly role = computed<Role | null>(() => this.session()?.user.role ?? null);

  constructor() {
    this.scheduleExpiry();
  }

  token(): string | null {
    return this.session()?.token ?? null;
  }

  hasRole(role: Role): boolean {
    return this.role() === role;
  }

  login(email: string, password: string): Observable<TokenResponse> {
    return this.http.post<TokenResponse>('/api/auth/login', { email, password }).pipe(
      tap((response) => {
        const session = { token: response.accessToken, expiresAt: response.expiresAt, user: response.user };
        sessionStorage.setItem(STORAGE_KEY, JSON.stringify(session));
        this.session.set(session);
        this.scheduleExpiry();
      }),
    );
  }

  logout(redirect = true): void {
    sessionStorage.removeItem(STORAGE_KEY);
    this.session.set(null);
    clearTimeout(this.expiryTimer);
    if (redirect) {
      this.router.navigate(['/login']);
    }
  }

  private restore(): Session | null {
    try {
      const raw = sessionStorage.getItem(STORAGE_KEY);
      const session = raw ? (JSON.parse(raw) as Session) : null;
      return session && new Date(session.expiresAt).getTime() > Date.now() ? session : null;
    } catch {
      return null;
    }
  }

  private scheduleExpiry(): void {
    clearTimeout(this.expiryTimer);
    const current = this.session();
    if (current) {
      const remaining = new Date(current.expiresAt).getTime() - Date.now();
      this.expiryTimer = setTimeout(() => this.logout(), Math.max(remaining, 0));
    }
  }
}
