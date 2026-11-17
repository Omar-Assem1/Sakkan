import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, throwError } from 'rxjs';
import { TokenStorageService } from '../services/token-storage.service';

/** Only our backend API gets the JWT – never static assets (i18n JSON) or third-party URLs. */
const API_PREFIX = '/api/';

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const tokenStorage = inject(TokenStorageService);
  const router = inject(Router);

  const token = tokenStorage.getToken();
  const isApiRequest = req.url.startsWith(API_PREFIX);

  const authReq =
    token && isApiRequest
      ? req.clone({ setHeaders: { Authorization: `Bearer ${token}` } })
      : req;

  return next(authReq).pipe(
    catchError((error: unknown) => {
      if (error instanceof HttpErrorResponse && error.status === 401) {
        tokenStorage.clear();
        router.navigate(['/auth']);
      }
      return throwError(() => error);
    }),
  );
};
