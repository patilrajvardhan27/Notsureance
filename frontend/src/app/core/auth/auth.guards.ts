import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { map } from 'rxjs';
import { Role } from '../models/auth.models';
import { AuthService } from './auth.service';

export function roleGuard(...roles: Role[]): CanActivateFn {
  return () => {
    const router = inject(Router);
    return inject(AuthService)
      .whenSettled()
      .pipe(map((profile) => (profile && roles.includes(profile.role) ? true : router.createUrlTree(['/dashboard']))));
  };
}
