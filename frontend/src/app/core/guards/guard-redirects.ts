import { Router, UrlSegment, UrlTree } from '@angular/router';

/** 401 path: sign-in, remembering where the user wanted to go. */
export function signInRedirect(router: Router, segments: UrlSegment[]): UrlTree {
  const returnUrl = '/' + segments.map((s) => s.path).join('/');
  return router.createUrlTree(['/auth', 'sign-in'], { queryParams: { returnUrl } });
}

/** 403 path: the shared error page. */
export function forbiddenRedirect(router: Router): UrlTree {
  return router.createUrlTree(['/error'], { queryParams: { code: 403 } });
}
