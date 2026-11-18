import { HttpInterceptorFn, HttpErrorResponse } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, throwError } from 'rxjs';
import { ApiError } from '../../shared/components/error-page/error-page';

/**
 * Intercepts HTTP errors from the Sakan API.
 *
 * - Reads the RFC 9457 ProblemDetail body from the backend.
 * - Navigates to /error so the ErrorPageComponent renders
 *   the status code, title, and detail.
 * - Re-throws so callers can still react if they want to.
 */
export const errorInterceptor: HttpInterceptorFn = (req, next) => {
  const router = inject(Router);

  return next(req).pipe(
    catchError((err: HttpErrorResponse) => {
      // Build an ApiError from the ProblemDetail body (or fall back to defaults)
      const body = err.error ?? {};
      const apiError: ApiError = {
        status: body['status'] ?? err.status,
        title: body['title'] ?? httpStatusTitle(err.status),
        detail: body['detail'] ?? err.message,
        type: body['type'],
        timestamp: body['timestamp'],
      };

      // Navigate to the error page, passing the error via router state
      router.navigate(['/error'], { state: { error: apiError } });

      return throwError(() => apiError);
    }),
  );
};

function httpStatusTitle(status: number): string {
  const titles: Record<number, string> = {
    400: 'Bad Request',
    401: 'Authentication Required',
    403: 'Access Denied',
    404: 'Not Found',
    409: 'Conflict',
    422: 'Unprocessable Entity',
    500: 'Internal Server Error',
  };
  return titles[status] ?? `HTTP Error ${status}`;
}
