import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, throwError } from 'rxjs';
import { ToastService } from '../ui/toast.service';
import { AuthService } from './auth.service';

export const authInterceptor: HttpInterceptorFn = (request, next) => {
  const auth = inject(AuthService);
  const toasts = inject(ToastService);
  const token = auth.token();
  const isApiCall = request.url.startsWith('/api/');
  const authorized = token && isApiCall ? request.clone({ setHeaders: { Authorization: `Bearer ${token}` } }) : request;

  return next(authorized).pipe(
    catchError((error: HttpErrorResponse) => {
      if (error.status === 401 && token) {
        auth.logout();
        toasts.show('Your session ended. Please sign in again.', 'info');
      } else if (error.status === 0 || error.status >= 500) {
        toasts.show('We could not reach the server. Try again in a moment.', 'error');
      }
      return throwError(() => error);
    }),
  );
};
