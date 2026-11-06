import { HttpErrorResponse } from '@angular/common/http';

export function problemMessage(error: unknown, fallback = 'Something went wrong'): string {
  if (error instanceof HttpErrorResponse && error.error && typeof error.error === 'object') {
    const body = error.error as { detail?: string; errors?: Record<string, string> };
    const fieldErrors = body.errors ? Object.entries(body.errors).map(([field, msg]) => `${field}: ${msg}`) : [];
    return [body.detail, ...fieldErrors].filter(Boolean).join(' · ') || fallback;
  }
  return fallback;
}
