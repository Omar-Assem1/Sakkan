import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, throwError } from 'rxjs';
import { TokenStorageService } from '../services/token-storage.service';

/** Only our backend API gets the JWT – never static assets (i18n JSON) or third-party URLs. */
function isBackendApiUrl(url: string): boolean {
  return url.startsWith('/api/') || /^https?:\/\/localhost(:\d+)?\/api\//.test(url);
}

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const tokenStorage = inject(TokenStorageService);
  const router = inject(Router);

  const token = tokenStorage.getToken();
  const isApi = isBackendApiUrl(req.url);

  const authReq =
    token && isApi ? req.clone({ setHeaders: { Authorization: `Bearer ${token}` } }) : req;

  return next(authReq).pipe(
    catchError((error: unknown) => {
      if (error instanceof HttpErrorResponse && isApi) {
        if (error.status === 401 && !req.url.startsWith('/api/auth/')) {
          // Expired or invalid token: drop the session and go to sign-in.
          tokenStorage.clear();
          router.navigate(['/auth/sign-in'], { queryParams: { returnUrl: router.url } });
        } else if (error.status === 403) {
          // Signed in but not allowed: keep the session, show the 403 page.
          router.navigate(['/error'], { queryParams: { code: 403 } });
        }
      }
      return throwError(() => error);
    }),
  );
};
