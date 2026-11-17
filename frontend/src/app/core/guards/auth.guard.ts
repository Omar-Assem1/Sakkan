import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { TokenStorageService } from '../services/token-storage.service';

/**
 * Blocks unauthenticated users (-> /auth) and, when the route declares
 * `data: { roles: [...] }`, users without one of those roles (-> /listings).
 * UX only – the backend still enforces @PreAuthorize on every endpoint.
 */
export const authGuard: CanActivateFn = (route) => {
  const tokenStorage = inject(TokenStorageService);
  const router = inject(Router);

  if (!tokenStorage.isAuthenticated()) {
    return router.createUrlTree(['/auth']);
  }

  const roles = route.data?.['roles'] as string[] | undefined;
  if (roles && roles.length > 0 && !tokenStorage.hasRole(...roles)) {
    return router.createUrlTree(['/listings']);
  }

  return true;
};
