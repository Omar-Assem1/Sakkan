import { Route } from '@angular/router';
import { routes } from './app.routes';
import { VIEWINGS_ROUTES } from './features/viewings/viewings.routes';

const PUBLIC_PATHS = ['', 'auth', 'listings', 'error', '**'];

function flatten(list: Route[]): Route[] {
  return list.flatMap((r) => [r, ...(r.children ? flatten(r.children) : [])]);
}

describe('route RBAC configuration (SCRUM-65)', () => {
  const protectedRoutes = routes.filter((r) => !PUBLIC_PATHS.includes(r.path ?? ''));

  it('has the expected protected areas', () => {
    expect(protectedRoutes.map((r) => r.path).sort()).toEqual(['admin', 'properties', 'viewings']);
  });

  it('guards every non-public top-level route with canMatch', () => {
    for (const route of protectedRoutes) {
      expect(route.canMatch?.length, `route "${route.path}"`).toBeGreaterThan(0);
    }
  });

  it('does not use canActivate or data.roles for authorization anywhere', () => {
    for (const route of flatten(routes)) {
      expect(route.canActivate, `route "${route.path}"`).toBeUndefined();
      expect(route.data?.['roles'], `route "${route.path}"`).toBeUndefined();
    }
  });

  it('guards every viewings child route individually (availability and request need different roles)', () => {
    for (const route of VIEWINGS_ROUTES) {
      expect(route.canMatch?.length, `viewings route "${route.path}"`)
        .toBeGreaterThan(0);
    }
  });

  it('keeps public routes open', () => {
    for (const path of ['auth', 'listings', 'error']) {
      const route = routes.find((r) => r.path === path);
      expect(route, path).toBeDefined();
      expect(route?.canMatch, path).toBeUndefined();
    }
  });
});
