import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { Role } from '../models/auth.models';
import { AuthService } from './auth.service';

export const signedInGuard: CanActivateFn = (_route, state) => {
  const auth = inject(AuthService);
  return auth.isSignedIn() ? true : inject(Router).createUrlTree(['/login'], { queryParams: { next: state.url } });
};

export function roleGuard(role: Role): CanActivateFn {
  return () => {
    const auth = inject(AuthService);
    return auth.hasRole(role) ? true : inject(Router).createUrlTree(['/dashboard']);
  };
}
