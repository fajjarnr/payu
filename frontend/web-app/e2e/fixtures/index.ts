/**
 * Extended Playwright test fixtures for PayU E2E tests
 *
 * These fixtures automatically handle authentication for protected routes.
 * The middleware.ts requires session cookies for routes like /investments, /dashboard, etc.
 *
 * The authPage fixture also intercepts BFF API calls (/api/v1/** and /api/auth/**)
 * to return mock data, preventing 401 → redirect loops caused by invalid mock tokens.
 *
 * NOTE: This file uses Playwright's fixture pattern which uses a `use` function
 * parameter that ESLint incorrectly flags as a React Hook. The react-hooks/rules-of-hooks
 * rule is disabled for this file since these are Playwright fixtures, not React components.
 */

/* eslint-disable react-hooks/rules-of-hooks */

import { test as base, expect, Page, BrowserContext, Route } from '@playwright/test';

// Extend the base test with custom fixtures
type PayUFixtures = {
  // Auto-authenticated page for protected routes
  authPage: Page;
};

type PayUWorkerFixtures = {
  // Setup for each worker
  context: BrowserContext;
};

/**
 * Setup authentication cookies for protected routes
 */
async function setupAuthCookies(context: BrowserContext) {
  await context.addCookies([
    {
      name: 'accessToken',
      value: 'mock-access-token-for-e2e-tests',
      domain: 'localhost',
      path: '/',
      httpOnly: true,
      secure: false,
      sameSite: 'Lax',
    },
    {
      name: 'payu_session',
      value: 'mock-session-for-e2e-tests',
      domain: 'localhost',
      path: '/',
      httpOnly: true,
      secure: false,
      sameSite: 'Lax',
    },
  ]);
}

/**
 * Mock API response handler for BFF proxy calls.
 * Returns contextual mock data based on URL path to prevent 401 redirect loops.
 */
async function handleMockApiRoute(route: Route) {
  const url = route.request().url();
  const method = route.request().method();
  const path = new URL(url).pathname;

  // Auth refresh — always succeed to prevent redirect loops
  if (path.includes('/api/auth/refresh')) {
    return route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({ success: true, message: 'Token refreshed' }),
    });
  }

  // Auth user info
  if (path.includes('/api/auth/me') || path.includes('/api/auth/user')) {
    return route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({
        success: true,
        data: {
          id: 'e2e-user-001',
          accountId: 'ACC-E2E-001',
          name: 'E2E Test User',
          email: 'e2e@payu.id',
          phone: '+6281234567890',
          role: 'CUSTOMER',
          kycStatus: 'VERIFIED',
          avatar: null,
        },
      }),
    });
  }

  // For API v1 calls — return contextual mock data
  if (path.startsWith('/api/v1/')) {
    const apiPath = path.replace('/api/v1/', '');

    // Account/profile endpoints
    if (apiPath.startsWith('accounts') || apiPath.startsWith('profile')) {
      return route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          success: true,
          data: {
            id: 'ACC-E2E-001',
            name: 'E2E Test User',
            email: 'e2e@payu.id',
            phone: '+6281234567890',
            accountNumber: '1234567890',
            accountType: 'SAVINGS',
            status: 'ACTIVE',
            kycStatus: 'VERIFIED',
            createdAt: '2026-01-01T00:00:00Z',
          },
        }),
      });
    }

    // Wallet/balance endpoints
    if (apiPath.startsWith('wallet') || apiPath.startsWith('balance') || apiPath.startsWith('pockets')) {
      if (method === 'GET') {
        // GET /pockets/total-balance/{currency}
        if (apiPath.includes('total-balance')) {
          return route.fulfill({
            status: 200,
            contentType: 'application/json',
            body: JSON.stringify({
              success: true,
              data: { totalBalance: 5000000, currency: 'IDR' },
            }),
          });
        }
        // GET /pockets (list) — returns Pocket[]
        if (apiPath.startsWith('pockets') && !apiPath.includes('/')) {
          return route.fulfill({
            status: 200,
            contentType: 'application/json',
            body: JSON.stringify({
              success: true,
              data: [
                { id: 'PKT-001', name: 'Tabungan Utama', balance: 3000000, type: 'SAVINGS', currency: 'IDR', createdAt: '2026-01-01T00:00:00Z', updatedAt: '2026-01-01T00:00:00Z', accountId: 'ACC-E2E-001' },
                { id: 'PKT-002', name: 'Dana Darurat', balance: 2000000, type: 'EMERGENCY', currency: 'IDR', createdAt: '2026-01-01T00:00:00Z', updatedAt: '2026-01-01T00:00:00Z', accountId: 'ACC-E2E-001' },
              ],
            }),
          });
        }
        // GET /wallets/{id}/transactions — returns WalletTransaction[]
        if (apiPath.includes('transactions')) {
          return route.fulfill({
            status: 200,
            contentType: 'application/json',
            body: JSON.stringify({
              success: true,
              data: [
                { id: 'TXN-001', type: 'CREDIT', amount: 100000, currency: 'IDR', status: 'SUCCESS', description: 'Top up via VA', createdAt: '2026-05-01T10:00:00Z' },
                { id: 'TXN-002', type: 'DEBIT', amount: 50000, currency: 'IDR', status: 'SUCCESS', description: 'Transfer keluar', createdAt: '2026-05-02T08:00:00Z' },
              ],
            }),
          });
        }
        // GET /wallets/{id}/balance — returns BalanceResponse
        if (apiPath.includes('balance')) {
          return route.fulfill({
            status: 200,
            contentType: 'application/json',
            body: JSON.stringify({
              success: true,
              data: { balance: 5000000, currency: 'IDR', available: 4800000, pending: 200000 },
            }),
          });
        }
        // Generic fallback for wallet/pockets GET
        return route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({
            success: true,
            data: { id: 'PKT-001', name: 'Tabungan Utama', balance: 5000000, currency: 'IDR', type: 'SAVINGS' },
          }),
        });
      }
      return route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({ success: true, data: { id: 'PKT-NEW', message: 'Operation successful' } }),
      });
    }

    // Transaction endpoints
    if (apiPath.startsWith('transactions') || apiPath.startsWith('transfers')) {
      return route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          success: true,
          data: {
            transactions: [
              { id: 'TXN-001', type: 'TRANSFER', amount: 100000, currency: 'IDR', status: 'SUCCESS', description: 'Transfer ke Budi', createdAt: '2026-03-15T10:00:00Z' },
              { id: 'TXN-002', type: 'PAYMENT', amount: 50000, currency: 'IDR', status: 'SUCCESS', description: 'Pembayaran PLN', createdAt: '2026-03-14T08:00:00Z' },
            ],
            totalElements: 2,
            page: 0,
            size: 10,
          },
        }),
      });
    }

    // Investment endpoints — specific routes before generic catch-all
    if (apiPath.startsWith('investments')) {
      // GET /investments/accounts/me → InvestmentAccount
      if (apiPath.includes('accounts/me') || apiPath.endsWith('accounts')) {
        return route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({
            success: true,
            data: {
              id: 'INV-ACC-001',
              userId: 'e2e-user-001',
              accountType: 'STANDARD',
              balance: 152800000,
              currency: 'IDR',
              status: 'ACTIVE',
              createdAt: '2026-01-01T00:00:00Z',
            },
          }),
        });
      }
      // GET /investments/gold/me → GoldHolding
      if (apiPath.includes('gold')) {
        return route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({
            success: true,
            data: {
              userId: 'e2e-user-001',
              totalWeightGrams: 5.0,
              currentValuePerGram: 1050000,
              totalValue: 5250000,
              holdings: [
                { purchaseDate: '2026-02-01', weightGrams: 5.0, purchasePrice: 1000000 },
              ],
            },
          }),
        });
      }
      // Generic investment fallback
      return route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({ success: true, data: {} }),
      });
    }

    // Portfolio endpoints (legacy)
    if (apiPath.startsWith('portfolio')) {
      return route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({ success: true, data: {} }),
      });
    }

    // Lending endpoints — specific routes before generic catch-all
    if (apiPath.startsWith('lending') || apiPath.startsWith('loans')) {
      // GET /lending/credit-score/{userId} → CreditScore
      if (apiPath.includes('credit-score')) {
        return route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({
            success: true,
            data: {
              id: 'CS-001',
              userId: 'e2e-user-001',
              score: 785,
              grade: 'A',
              factors: [
                'Pembayaran tepat waktu',
                'Rasio utang rendah',
                'Histori kredit panjang',
              ],
              lastUpdated: '20 Jan 2026',
            },
          }),
        });
      }
      // GET /lending/paylater/{userId}/transactions → PayLaterTransaction[]
      if (apiPath.includes('paylater') && apiPath.includes('transactions')) {
        return route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({
            success: true,
            data: [
              { id: '1', userId: 'e2e-user-001', type: 'PURCHASE', merchantName: 'TokoBapak', amount: 850000, balanceAfter: 850000, description: 'Belanja', createdAt: '20 Jan 2026' },
              { id: '2', userId: 'e2e-user-001', type: 'PAYMENT', merchantName: 'Traveloka', amount: 3200000, balanceAfter: 4050000, description: 'Travel', createdAt: '18 Jan 2026' },
              { id: '3', userId: 'e2e-user-001', type: 'PURCHASE', merchantName: 'Shopee', amount: 450000, balanceAfter: 4500000, description: 'Shopping', createdAt: '15 Jan 2026' },
            ],
          }),
        });
      }
      // GET /lending/paylater/{userId} → PayLater
      if (apiPath.includes('paylater')) {
        return route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({
            success: true,
            data: {
              id: 'PL-001',
              userId: 'e2e-user-001',
              creditLimit: 15000000,
              usedLimit: 4500000,
              availableLimit: 10500000,
              status: 'ACTIVE',
              dueDate: '25 Jan 2026',
              minimumPayment: 250000,
              createdAt: '2025-06-01T00:00:00Z',
            },
          }),
        });
      }
      // GET /lending/pre-approval/user/{userId}/active → PreApproval[]
      if (apiPath.includes('pre-approval')) {
        return route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({
            success: true,
            data: [
              {
                id: 'PA-001',
                userId: 'e2e-user-001',
                maxAmount: 50000000,
                interestRate: 12.5,
                maxTenureMonths: 36,
                status: 'APPROVED',
                validUntil: '2026-12-31T00:00:00Z',
                createdAt: '2026-01-15T00:00:00Z',
              },
            ],
          }),
        });
      }
      // Generic lending fallback
      return route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({ success: true, data: {} }),
      });
    }

    // Bill payment endpoints
    if (apiPath.startsWith('bills') || apiPath.startsWith('billers')) {
      return route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          success: true,
          data: {
            categories: [
              { id: 'PLN', name: 'PLN', icon: 'zap' },
              { id: 'PDAM', name: 'PDAM', icon: 'droplet' },
              { id: 'TELCO', name: 'Pulsa & Data', icon: 'phone' },
              { id: 'BPJS', name: 'BPJS', icon: 'shield' },
            ],
            recentBills: [],
          },
        }),
      });
    }

    // Exchange/FX endpoints
    if (apiPath.startsWith('exchange') || apiPath.startsWith('fx') || apiPath.startsWith('rates')) {
      // GET /fx/conversions → FxConversionResponse[]
      if (apiPath.includes('conversions')) {
        return route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({
            success: true,
            data: [],
          }),
        });
      }
      return route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          success: true,
          data: {
            rates: [
              { from: 'USD', to: 'IDR', rate: 15850, change: 0.12 },
              { from: 'EUR', to: 'IDR', rate: 17200, change: -0.05 },
              { from: 'SGD', to: 'IDR', rate: 11800, change: 0.08 },
            ],
            lastUpdated: '2026-03-18T10:00:00Z',
          },
        }),
      });
    }

    // Notification endpoints
    if (apiPath.startsWith('notifications')) {
      return route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          success: true,
          data: {
            notifications: [
              { id: 'NOT-001', type: 'TRANSACTION', title: 'Transfer Berhasil', message: 'Transfer Rp 100.000 berhasil', read: false, createdAt: '2026-03-18T09:00:00Z' },
              { id: 'NOT-002', type: 'PROMO', title: 'Promo Cashback', message: 'Dapatkan cashback 10%', read: true, createdAt: '2026-03-17T12:00:00Z' },
            ],
            unreadCount: 1,
          },
        }),
      });
    }

    // QRIS endpoints
    if (apiPath.startsWith('qris')) {
      return route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          success: true,
          data: {
            qrCode: 'MOCK-QR-CODE-DATA',
            dailyLimit: 10000000,
            dailyUsed: 0,
            recentPayments: [],
          },
        }),
      });
    }

    // Settings/preferences endpoints
    if (apiPath.startsWith('settings') || apiPath.startsWith('preferences')) {
      return route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          success: true,
          data: {
            language: 'id',
            currency: 'IDR',
            notifications: { push: true, email: true, sms: false },
            security: { biometric: false, twoFactor: false },
            theme: 'system',
          },
        }),
      });
    }

    // Rewards/promotions endpoints
    if (apiPath.startsWith('rewards') || apiPath.startsWith('promotions') || apiPath.startsWith('vouchers')) {
      return route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          success: true,
          data: {
            points: 12500,
            tier: 'GOLD',
            history: [
              { id: 'RWD-001', type: 'EARNED', points: 500, description: 'Transfer bonus', createdAt: '2026-03-15T10:00:00Z' },
            ],
            availableRewards: [],
          },
        }),
      });
    }

    // Support/tickets endpoints
    if (apiPath.startsWith('support') || apiPath.startsWith('tickets')) {
      return route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          success: true,
          data: {
            tickets: [],
            faqs: [
              { id: 'FAQ-001', question: 'Bagaimana cara transfer?', answer: 'Buka menu Transfer', category: 'TRANSFER' },
              { id: 'FAQ-002', question: 'Bagaimana cara top up?', answer: 'Buka menu Top Up', category: 'TOP_UP' },
            ],
            systemStatus: 'OPERATIONAL',
          },
        }),
      });
    }

    // Backoffice endpoints — list endpoints expect arrays
    if (apiPath.startsWith('backoffice') || apiPath.startsWith('admin')) {
      // KYC reviews, fraud cases, customer cases all expect arrays from their service methods
      return route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          success: true,
          data: [],
        }),
      });
    }

    // Analytics endpoints
    if (apiPath.startsWith('analytics')) {
      return route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          success: true,
          data: {
            income: 15000000,
            expense: 8000000,
            savings: 7000000,
            spendingByCategory: [
              { category: 'Makanan', amount: 3000000, percentage: 37.5 },
              { category: 'Transport', amount: 2000000, percentage: 25 },
              { category: 'Belanja', amount: 3000000, percentage: 37.5 },
            ],
          },
        }),
      });
    }

    // Scheduled transfers
    if (apiPath.startsWith('scheduled')) {
      return route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          success: true,
          data: {
            scheduledTransfers: [],
            totalScheduled: 0,
          },
        }),
      });
    }

    // GDPR audit endpoints (before compliance catch-all)
    if (apiPath.startsWith('gdpr-audit')) {
      return route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          success: true,
          data: [],
        }),
      });
    }

    // Compliance/KYC endpoints — audit-report search expects array
    if (apiPath.startsWith('compliance') || apiPath.startsWith('kyc')) {
      // /compliance/audit-report (search) — expects array of audit logs
      if (apiPath.includes('audit-report') || apiPath.includes('audit')) {
        return route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({
            success: true,
            data: [],
          }),
        });
      }
      return route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          success: true,
          data: { status: 'VERIFIED', level: 'FULL' },
        }),
      });
    }

    // Segmentation/segments endpoints
    if (apiPath.startsWith('segments')) {
      // GET /segments/user/{userId}/offers → SegmentedOffersResponse
      if (apiPath.includes('/offers')) {
        return route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({
            success: true,
            data: {
              offers: [
                {
                  id: 'OFFER-001',
                  title: 'Cashback 10%',
                  description: 'Cashback for all transactions',
                  offerType: 'CASHBACK',
                  segmentTier: 'GOLD',
                  isActive: true,
                  validFrom: '2026-01-01T00:00:00Z',
                  validUntil: '2026-12-31T23:59:59Z',
                  discount: 10,
                  maxDiscount: 50000,
                  minTransaction: 100000,
                },
              ],
              totalCount: 1,
            },
          }),
        });
      }
      // GET /segments/user/{userId} → UserSegmentsResponse
      if (apiPath.includes('user/')) {
        return route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({
            success: true,
            data: {
              memberships: [
                {
                  id: 'SM-001',
                  userId: 'e2e-user-001',
                  segmentId: 'SEG-GOLD',
                  segment: { id: 'SEG-GOLD', name: 'Gold', description: 'Gold tier', tier: 'GOLD', minBalance: 10000000, benefits: ['Free transfers'], requirements: ['Min balance 10M'], createdAt: '2026-01-01T00:00:00Z', updatedAt: '2026-01-01T00:00:00Z' },
                  status: 'ACTIVE',
                  joinedAt: '2026-01-01T00:00:00Z',
                  score: 750,
                },
              ],
              currentTier: 'GOLD',
              nextTier: 'PLATINUM',
              progressToNext: 65,
              totalScore: 750,
            },
          }),
        });
      }
      // Generic segments fallback
      return route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({ success: true, data: [] }),
      });
    }

    // Partner/merchant endpoints — expects array of partners
    if (apiPath.startsWith('partner') || apiPath.startsWith('merchant')) {
      return route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          success: true,
          data: [],
        }),
      });
    }

    // Default fallback — return empty array for any unmatched API endpoint
    // Most list endpoints expect arrays; {} causes crashes when array methods are called
    return route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({ success: true, data: [] }),
    });
  }

  // For non-API routes, continue normally
  return route.continue();
}

/**
 * Extended test with authentication support
 *
 * Usage:
 *   import { test, expect } from './fixtures';
 *
 *   test.describe('Protected Route', () => {
 *     test('should access protected page', async ({ authPage }) => {
 *       await authPage.goto('/investments');
 *       // Test is already authenticated!
 *     });
 *   });
 */
export const test = base.extend<PayUFixtures, PayUWorkerFixtures>({
  // Automatically set up auth for each test
  authPage: async ({ page, context }, use) => {
    await setupAuthCookies(context);

    // Populate zustand auth store in localStorage BEFORE any navigation.
    // Lending hooks use `enabled: !!userId` — without this, all queries are disabled.
    await context.addInitScript(() => {
      const authData = {
        state: {
          user: {
            id: 'e2e-user-001',
            externalId: 'EXT-E2E-001',
            username: 'e2e_user',
            email: 'e2e@payu.id',
            phoneNumber: '+6281234567890',
            fullName: 'E2E Test User',
            nik: '3201010101010001',
            kycStatus: 'VERIFIED',
            createdAt: '2026-01-01T00:00:00Z',
            updatedAt: '2026-03-18T00:00:00Z',
          },
          accountId: 'ACC-E2E-001',
          isAuthenticated: true,
        },
        version: 0,
      };
      localStorage.setItem('payu-auth-storage', JSON.stringify(authData));
    });

    // Intercept BFF API calls to prevent 401 → redirect loops
    await page.route(/\/api\/(v1|auth)\//, handleMockApiRoute);
    await use(page);
  },

  // Ensure context is properly set up
  context: async ({ browser }, use) => {
    const context = await browser.newContext();
    await use(context);
    await context.close();
  },
});

// Re-export expect
export { expect };

/**
 * Helper to navigate to protected routes with auth
 */
export async function gotoProtected(page: Page, context: BrowserContext, path: string) {
  await setupAuthCookies(context);
  await page.goto(path);
  await page.waitForLoadState('domcontentloaded');
}

/**
 * Helper to perform login flow
 */
export async function performLogin(
  page: Page,
  phone: string = '+6281234567890',
  pin: string = '123456'
) {
  await page.goto('/login');
  await page.fill('input[name="phone"]', phone);
  await page.fill('input[name="pin"]', pin);
  await page.click('button[type="submit"]');
  await page.waitForURL('**/dashboard', { timeout: 10000 });
}
