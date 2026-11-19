import { inject } from '@angular/core';
import { CanMatchFn, Router } from '@angular/router';
import { TokenStorageService } from '../services/token-storage.service';
import { forbiddenRedirect, signInRedirect } from './guard-redirects';

/** canMatch: signed in AND holding at least one of `roles`. No token -> sign-in, wrong role -> 403 page. */
export const roleGuard =
  (...roles: string[]): CanMatchFn =>
  (_route, segments) => {
    const storage = inject(TokenStorageService);
    const router = inject(Router);

    if (!storage.isAuthenticated()) {
      return signInRedirect(router, segments);
    }
    return storage.hasRole(...roles) ? true : forbiddenRedirect(router);
  };
