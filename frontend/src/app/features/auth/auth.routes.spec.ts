import { AUTH_ROUTES } from './auth.routes';

describe('Google OAuth routes (SCRUM-56)', () => {
  it('exposes a callback route for the one-time Google login code', () => {
    const callback = AUTH_ROUTES.find((route) => route.path === 'google/callback');

    expect(callback).toBeDefined();
    expect(callback?.loadComponent).toBeDefined();
  });
});
