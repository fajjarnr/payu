import { defineConfig, devices } from '@playwright/test';

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
    baseURL: 'http://localhost:3000',
    trace: 'on-first-retry',
    screenshot: 'only-on-failure',
    video: 'retain-on-failure',
    actionTimeout: 15000, // Increase action timeout
    navigationTimeout: 30000, // Increase navigation timeout
  },
  projects: [
    {
      name: 'chromium',
      use: { ...devices['Desktop Chrome'] },
    },
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
  ],
  webServer: {
    command: 'npm run dev',
    url: 'http://localhost:3000',
    reuseExistingServer: !process.env.CI,
    timeout: 120000, // Increase web server timeout
  },
});
