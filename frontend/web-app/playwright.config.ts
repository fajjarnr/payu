import { defineConfig, devices } from '@playwright/test';

/**
 * PayU Digital Banking - Playwright E2E Configuration
 *
 * IMPORTANT: This configuration assumes the web app is already running:
 *   - npm run dev: http://localhost:3000
 *   - podman-compose: http://localhost:3001 (set PLAYWRIGHT_BASE_URL=http://localhost:3001)
 *
 * The webServer is disabled to avoid port conflicts with the containerized
 * environment. Tests will reuse the existing server.
 */

export default defineConfig({
  testDir: './e2e',
  fullyParallel: false, // Run tests sequentially for better stability
  forbidOnly: !!process.env.CI,
  retries: process.env.CI ? 2 : 1, // Add 1 retry for local development
  workers: 1, // Use single worker for stability
  reporter: 'html',
  timeout: 60000, // Increase global timeout to 60 seconds
  expect: {
    timeout: 10000, // Increase expect timeout to 10 seconds
  },
  use: {
    // Use port 3001 for tests (web-app podman container defaults to host port 3001)
    baseURL: process.env.PLAYWRIGHT_BASE_URL || 'http://localhost:3001',
    trace: 'on-first-retry',
    screenshot: 'only-on-failure',
    video: 'retain-on-failure',
    actionTimeout: 15000, // Increase action timeout
    navigationTimeout: 30000, // Increase navigation timeout
    // Set default locale for i18n routing
    locale: 'id',
  },
  projects: [
    {
      name: 'chromium',
      use: {
        ...devices['Desktop Chrome'],
        viewport: { width: 1280, height: 720 },
      },
    },
    // Temporarily disable other browsers to focus on Chromium first
    // Uncomment after Chromium tests pass
    /*
    {
      name: 'firefox',
      use: { ...devices['Desktop Firefox'] },
    },
    {
      name: 'webkit',
      use: { ...devices['Desktop Safari'] },
    },
    {
      name: 'Mobile Chrome',
      use: { ...devices['Pixel 5'] },
    },
    {
      name: 'Mobile Safari',
      use: { ...devices['iPhone 12'] },
    },
    */
  ],
  // webServer disabled — app runs in podman container on port 3001.
  // Set PLAYWRIGHT_BASE_URL=http://localhost:3001 to override baseURL.
  // webServer: {
  //   command: 'npm run dev',
  //   url: 'http://localhost:3000',
  //   reuseExistingServer: !process.env.CI,
  //   timeout: 120000,
  // },
});
