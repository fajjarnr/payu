# E2E Test Data-testid Summary

This document provides a comprehensive summary of all `data-testid` attributes added to the PayU web application for stable E2E test selectors.

## Overview

A total of **50+ components** have been updated with `data-testid` attributes across 15 files. These attributes provide stable selectors for E2E testing that won't break when CSS classes, text content, or component structure changes.

## Naming Convention

The following naming conventions were used:

- **Inputs**: `<element>-input` (e.g., `username-input`, `password-input`, `amount-input`)
- **Buttons**: `<action>-button` (e.g., `login-submit-button`, `confirm-transfer-button`)
- **Cards**: `<context>-card` (e.g., `balance-card`, `primary-balance-card`)
- **Links**: `<context>-link` (e.g., `forgot-password-link`, `register-link`)
- **Navigation**: `mobile-nav-<page>` (e.g., `mobile-nav-dashboard`)
- **Tables**: `transfer-row-<id>` (e.g., `transfer-row-1`)
- **Dynamic Lists**: `<type>-<index>` (e.g., `investment-product-0`)

## Components Updated

### 1. Authentication (`/login/page.tsx`)

| Element | data-testid | Purpose |
|---------|-------------|---------|
| Username input | `username-input` | Login username field |
| Password input | `password-input` | Login password field |
| Submit button | `login-submit-button` | Login form submission |
| Forgot password link | `forgot-password-link` | Password recovery |
| Register link | `register-link` | Navigate to registration |

### 2. Dashboard Layout (`DashboardLayout.tsx`)

| Element | data-testid | Purpose |
|---------|-------------|---------|
| Mobile menu trigger | `mobile-menu-trigger` | Open mobile sidebar |
| Search input | `search-input` | Global search |
| Notification button | `notification-button` | Open notifications |
| Profile menu trigger | `profile-menu-trigger` | Open user menu |
| Logout button | `logout-button` | User logout |

### 3. Mobile Navigation (`MobileNav.tsx`)

| Element | data-testid | Purpose |
|---------|-------------|---------|
| Mobile nav container | `mobile-nav` | Bottom nav bar |
| Dashboard link | `mobile-nav-dashboard` | Navigate to dashboard |
| Transfers link | `mobile-nav-transfers` | Navigate to transfers |
| Accounts link | `mobile-nav-accounts` | Navigate to pockets |
| Bills link | `mobile-nav-bills` | Navigate to bills |

### 4. Transfer Page (`/transfer/page.tsx`)

| Element | data-testid | Purpose |
|---------|-------------|---------|
| Recipient input | `recipient-account-input` | Enter recipient account |
| Amount input | `amount-input` | Enter transfer amount |
| Description input | `description-input` | Transfer memo |
| Review button | `review-transfer-button` | Review before submit |
| Back from review | `back-from-review-button` | Return to form |
| Confirm button | `confirm-transfer-button` | Authorize transfer |
| Transfer type - Internal | `transfer-type-internal_transfer` | Select internal transfer |
| Transfer type - BI-FAST | `transfer-type-bifast_transfer` | Select BI-FAST |
| Transfer type - SKN | `transfer-type-skn_transfer` | Select SKN |
| Transfer type - RTGS | `transfer-type-rtgs_transfer` | Select RTGS |
| Schedule - Now | `schedule-type-now` | Immediate transfer |
| Schedule - Scheduled | `schedule-type-scheduled` | Future date |
| Schedule - Recurring | `schedule-type-recurring` | Repeating transfer |
| Favorite contact - Anya | `favorite-contact-anya` | Quick select contact |
| Favorite contact - Budi | `favorite-contact-budi` | Quick select contact |
| Favorite contact - Citra | `favorite-contact-citra` | Quick select contact |
| Favorite contact - Dodi | `favorite-contact-dodi` | Quick select contact |

### 5. Balance Card (`dashboard/BalanceCard.tsx`)

| Element | data-testid | Purpose |
|---------|-------------|---------|
| Balance card container | `balance-card` | Main balance section |
| Primary balance card | `primary-balance-card` | Main balance display |
| Net worth card | `net-worth-card` | Net worth display |
| Income card | `income-card` | Income summary |
| Expense card | `expense-card` | Expense summary |

### 6. Quick Actions (`dashboard/QuickActions.tsx`)

| Element | data-testid | Purpose |
|---------|-------------|---------|
| Quick actions card | `quick-actions-card` | Actions container |
| Edit button | `edit-quick-actions-button` | Toggle edit mode |
| Action - Transfer | `quick-action-transfer` | Transfer action |
| Action - QRIS | `quick-action-qris` | QRIS action |
| Action - Bills | `quick-action-bills` | Bills action |
| Action - Pockets | `quick-action-pockets` | Pockets action |
| Action - Cards | `quick-action-cards` | Cards action |
| Action - Topup | `quick-action-topup` | Topup action |
| View all button | `view-all-features-button` | Show all features |

### 7. Transfer Activity (`dashboard/TransferActivity.tsx`)

| Element | data-testid | Purpose |
|---------|-------------|---------|
| Section container | `transfer-activity-section` | Activity section |
| Quick transfer card | `quick-transfer-card` | Quick send |
| Category - Bank | `quick-transfer-category-bank` | Bank transfers |
| Category - E-Wallet | `quick-transfer-category-e-wallet` | E-wallet transfers |
| Category - Tagihan | `quick-transfer-category-tagihan` | Bill payments |
| Category - Lain | `quick-transfer-category-lain` | Other |
| Send button | `quick-transfer-send-button` | Quick send action |
| Recent activity card | `recent-activity-card` | Transaction list |
| Transfer rows | `transfer-row-<id>` | Individual transactions |
| Mobile cards | `transfer-card-mobile-<id>` | Mobile transaction cards |
| Repeat button | `repeat-last-transfer-button` | Repeat last |
| Full history button | `view-full-history-button` | View all history |

### 8. Language Switcher (`LanguageSwitcher.tsx`)

| Element | data-testid | Purpose |
|---------|-------------|---------|
| Language button | `language-switcher-button` | Open language menu |
| Locale - ID | `locale-id` | Indonesian |
| Locale - EN | `locale-en` | English |

### 9. Theme Toggle (`ThemeToggle.tsx`)

| Element | data-testid | Purpose |
|---------|-------------|---------|
| Theme button | `theme-toggle-button` | Toggle dark/light mode |

### 10. Investments Page (`/investments/page.tsx`)

| Element | data-testid | Purpose |
|---------|-------------|---------|
| New investment button | `new-investment-button` | Create investment |
| Portfolio overview card | `portfolio-overview-card` | Portfolio summary |
| Optimize button | `optimize-portfolio-button` | Optimize allocation |
| Investment products | `investment-product-<0-2>` | Product cards |
| Buy buttons | `buy-investment-<0-2>` | Buy product |
| Review strategy button | `review-strategy-button` | Review allocation |

### 11. Lending Page (`/lending/page.tsx`)

| Element | data-testid | Purpose |
|---------|-------------|---------|
| Lending tabs | `lending-tabs` | Main tabs |
| Loans tab | `loans-tab` | Loans section |
| PayLater tab | `paylater-tab` | PayLater section |
| Activate PayLater button | `activate-paylater-button` | Activate feature |
| Loan products | `loan-product-<0-1>` | Loan cards |
| Apply loan buttons | `apply-loan-<0-1>` | Apply for loan |
| Pay bill button | `pay-bill-button` | Pay PayLater bill |
| Transaction rows | `transaction-<id>` | Individual transactions |

## Usage Examples

### Playwright Examples

```typescript
// Login
await page.getByTestId('username-input').fill('testuser');
await page.getByTestId('password-input').fill('password123');
await page.getByTestId('login-submit-button').click();

// Transfer
await page.getByTestId('recipient-account-input').fill('acc-any123');
await page.getByTestId('amount-input').fill('100000');
await page.getByTestId('review-transfer-button').click();
await page.getByTestId('confirm-transfer-button').click();

// Navigation
await page.getByTestId('mobile-nav-dashboard').click();
await page.getByTestId('logout-button').click();

// Quick Actions
await page.getByTestId('quick-action-transfer').click();
await page.getByTestId('quick-action-qris').click();

// Dashboard
await expect(page.getByTestId('balance-card')).toBeVisible();
await expect(page.getByTestId('primary-balance-card')).toContainText('Rp');
```

### Cypress Examples

```typescript
// Login
cy.getByTestId('username-input').type('testuser');
cy.getByTestId('password-input').type('password123');
cy.getByTestId('login-submit-button').click();

// Transfer
cy.getByTestId('transfer-type-bifast_transfer').click();
cy.getByTestId('recipient-account-input').type('acc-any123');
cy.getByTestId('amount-input').type('50000');
cy.getByTestId('review-transfer-button').click();
cy.getByTestId('confirm-transfer-button').click();
```

### Testing Library Examples

```typescript
// Login
await screen.getByTestId('username-input').sendKeys('testuser');
await screen.getByTestId('password-input').sendKeys('password123');
await screen.getByTestId('login-submit-button').click();

// Assertions
expect(await screen.getByTestId('balance-card')).isDisplayed();
```

## Files Modified

1. `/src/app/[locale]/login/page.tsx` - 5 testids
2. `/src/components/DashboardLayout.tsx` - 5 testids
3. `/src/components/MobileNav.tsx` - 5 testids
4. `/src/app/[locale]/transfer/page.tsx` - 16 testids
5. `/src/components/dashboard/BalanceCard.tsx` - 5 testids
6. `/src/components/dashboard/QuickActions.tsx` - 10 testids
7. `/src/components/dashboard/TransferActivity.tsx` - 11 testids
8. `/src/components/LanguageSwitcher.tsx` - 3 testids
9. `/src/components/ThemeToggle.tsx` - 1 testid
10. `/src/app/[locale]/investments/page.tsx` - 8 testids
11. `/src/app/[locale]/lending/page.tsx` - 11 testids

## Benefits

1. **Stable Selectors**: Test selectors won't break when:
   - CSS classes change (Tailwind updates)
   - Text content changes (i18n updates)
   - Component structure changes (refactoring)

2. **Semantic Naming**: Clear, descriptive names that make tests readable

3. **Maintainability**: Centralized list makes it easy to update tests

4. **Accessibility**: `data-testid` attributes don't affect styling or accessibility

5. **Cross-Framework Support**: Works with Playwright, Cypress, Testing Library, etc.

## Best Practices

1. **Prefer data-testid over**:
   - CSS selectors (fragile)
   - Text content (changes with i18n)
   - Element hierarchy (breaks with refactoring)

2. **Combine with role-based selectors** for accessibility-first testing:
   ```typescript
   // Good - accessible + stable
   await page.getByRole('button', { name: /login/i }).click();

   // Also good - stable for non-semantic elements
   await page.getByTestId('login-submit-button').click();
   ```

3. **Keep naming consistent** across the application

4. **Document new testids** in this file when adding features

## Future Additions

Consider adding testids to:
- Registration/Onboarding flow
- Bills payment flow
- Cards management
- Settings pages
- Analytics pages
- Support/help pages
- QRIS payment flow
