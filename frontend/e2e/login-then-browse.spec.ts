import { expect, test } from '@playwright/test';

/**
 * SCRUM-21 (SKN-7): Playwright smoke test — log in, then browse properties.
 *
 * Credentials come exclusively from environment variables (never hard-coded):
 *   - E2E_USERNAME
 *   - E2E_PASSWORD
 *   - BASE_URL (optional, defaults to http://localhost:4200 in playwright.config.ts)
 */
test.describe('Smoke: login then browse properties', () => {
  test('signs in and displays the Browse Properties listing page', async ({ page, request }) => {
    const username = process.env['E2E_USERNAME'];
    const password = process.env['E2E_PASSWORD'];
    if (!username || !password) {
      throw new Error('E2E_USERNAME and E2E_PASSWORD environment variables must be set');
    }

    // Ensure the user account exists in the database (idempotent: 201 if new, 409 if already seeded)
    const registerResp = await request.post('/api/auth/register', {
      data: {
        username,
        email: `${username}@sakan.test`,
        password,
        phone: '+201012345678',
        role: 'BUYER_TENANT',
      },
    });
    expect([201, 409]).toContain(registerResp.status());

    // 1. Open the login page (/auth -> /auth/sign-in) and sign in
    await page.goto('/auth/sign-in');
    await expect(page.getByRole('heading', { name: /sign in/i })).toBeVisible();

    await page.getByLabel(/username/i).fill(username);
    await page.getByLabel(/password/i).fill(password);
    await page.getByRole('button', { name: /sign in/i }).click();

    // 2. Verify redirection to Browse Properties (/listings) and visible listing UI
    await expect(page).toHaveURL(/\/listings/);
    await expect(page.locator('app-property-filter-panel')).toBeVisible();
    await expect(page.locator('app-property-search-bar')).toBeVisible();
  });
});
