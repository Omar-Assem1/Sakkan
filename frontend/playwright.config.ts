import { defineConfig, devices } from '@playwright/test';

declare const process: { env: Record<string, string | undefined> };

const baseURL = process.env['BASE_URL'] || 'http://localhost:4200';
const isWindowsLocal = !process.env['CI'] && process.env['OS'] === 'Windows_NT';

export default defineConfig({
  testDir: './e2e',
  fullyParallel: true,
  forbidOnly: !!process.env['CI'],
  retries: process.env['CI'] ? 2 : 0,
  reporter: 'list',
  use: {
    baseURL,
    trace: 'on-first-retry',
  },
  projects: [
    {
      name: 'chromium',
      use: {
        ...devices['Desktop Chrome'],
        ...(isWindowsLocal ? { channel: 'chrome' } : {}),
      },
    },
  ],
});
