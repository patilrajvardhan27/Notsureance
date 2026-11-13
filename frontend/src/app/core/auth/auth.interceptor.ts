import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { MsalService } from '@azure/msal-angular';
import { InteractionRequiredAuthError } from '@azure/msal-browser';
import { Observable, catchError, from, map, of, switchMap, throwError } from 'rxjs';
import { ToastService } from '../ui/toast.service';
import { SIGN_IN_CONFIG } from './sign-in.config';

const PUBLIC_PATHS = ['/api/public/', '/api/quotes'];

export const authInterceptor: HttpInterceptorFn = (request, next) => {
  const msal = inject(MsalService);
  const scopes = inject(SIGN_IN_CONFIG).apiScopes;
  const toasts = inject(ToastService);
  const account = msal.instance.getActiveAccount();
  const needsToken = request.url.startsWith('/api/') && !PUBLIC_PATHS.some((path) => request.url.startsWith(path));

  const token$: Observable<string | null> =
    needsToken && account
      ? from(msal.instance.acquireTokenSilent({ scopes, account })).pipe(
          map((result) => result.accessToken),
          catchError((error) => {
            if (error instanceof InteractionRequiredAuthError) {
              msal.acquireTokenRedirect({ scopes, account });
            }
            return throwError(() => error);
          }),
        )
      : of(null);

  return token$.pipe(
    switchMap((token) => next(token ? request.clone({ setHeaders: { Authorization: `Bearer ${token}` } }) : request)),
    catchError((error: unknown) => {
      if (error instanceof HttpErrorResponse && (error.status === 0 || error.status >= 500)) {
        toasts.show('We could not reach the server. Try again in a moment.', 'error');
      }
      return throwError(() => error);
    }),
  );
};
