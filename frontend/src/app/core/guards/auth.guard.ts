import { inject } from '@angular/core';
import { CanMatchFn, Router } from '@angular/router';
import { TokenStorageService } from '../services/token-storage.service';
import { signInRedirect } from './guard-redirects';

/** canMatch: lets any signed-in user through, everyone else goes to sign-in. */
export const authGuard: CanMatchFn = (_route, segments) => {
  const storage = inject(TokenStorageService);
  const router = inject(Router);
  return storage.isAuthenticated() ? true : signInRedirect(router, segments);
};
