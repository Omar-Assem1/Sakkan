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
  // Blocked by SCRUM-51 (M1): AuthPageComponent (/auth) is currently a static placeholder
  // ("Placeholder – real login/register forms come in the auth story.") with no login form inputs.
  test.fixme(
    'signs in and displays the Browse Properties listing page (blocked by SCRUM-51 / SKN-15 login form)',
    async ({ page }) => {
      const username = process.env['E2E_USERNAME'];
      const password = process.env['E2E_PASSWORD'];
      if (!username || !password) {
        throw new Error('E2E_USERNAME and E2E_PASSWORD environment variables must be set');
      }

      await page.goto('/auth');
      await page.getByLabel(/email|username/i).fill(username);
      await page.getByLabel(/password/i).fill(password);
      await page.getByRole('button', { name: /sign in|login/i }).click();

      await page.goto('/listings');
      await expect(page.locator('app-property-filter-panel')).toBeVisible();
      await expect(page.locator('app-property-card').first()).toBeVisible();
    },
  );
});
