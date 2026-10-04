# Ant Design Migration Audit Report

> **STATUS: PRE-MIGRATION AUDIT (2026-10-03)** — Executed by commit `dd1c0dc00` (see `CHANGELOG.md`). Facts below describe the **pre-migration** state. The 102 phase checkboxes were never maintained. The `ui/*` shims listed under "Files to Delete" are now deleted.

### Post-migration deltas
- antd `^6.6.5` installed (not 6.6.1); `@ant-design/cssinjs`, `@ant-design/icons`, `@ant-design/plots` added
- Removed: `radix-ui/*`, `framer-motion`, `lucide-react`, `recharts`, `sonner`, `class-variance-authority`, `@dnd-kit/*`, `react-hook-form`, `@hookform/resolvers`
- `src/components/icons.ts` is a single file aliasing `@ant-design/icons` (not a directory)
- `src/components/ui/Logo.tsx` + `src/components/ui/Motion.tsx` deleted; `src/components/ui/` removed
- Charts use `@ant-design/plots` (Column, Pie, RadialBar); Recharts gone
- `clsx` + `tailwind-merge` remain (still in use)


**Project:** PayU Digital Banking Web App  
**Date:** 2026-10-03  
**Auditor:** antd-audit agent  
**Scope:** Comprehensive audit of frontend/web-app for Ant Design migration planning

---

## Table of Contents

1. [Current Tech Stack Summary](#1-current-tech-stack-summary)
2. [Complete File Tree](#2-complete-file-tree)
3. [UI Component Inventory](#3-ui-component-inventory)
4. [Current Styling Approach & Design Tokens](#4-current-styling-approach--design-tokens)
5. [Ant Design Version & Compatibility Assessment](#5-ant-design-version--compatibility-assessment)
6. [Recommended Migration Strategy](#6-recommended-migration-strategy)
7. [Phased Rollout Plan](#7-phased-rollout-plan)
8. [Risks and Considerations](#8-risks-and-considerations)

---

## 1. Current Tech Stack Summary

| Category | Technology | Version |
|----------|-----------|---------|
| **Framework** | Next.js | 16.3.3 |
| **React** | React | 19.2.3 |
| **Language** | TypeScript | ^5 |
| **Routing** | App Router (Next.js 16) | — |
| **Styling** | Tailwind CSS | ^4 |
| **UI Component Base** | Radix UI (headless) | ^1.x |
| **State Management** | Zustand | ^5.0.10 |
| **Data Fetching** | TanStack React Query | ^5.90.19 |
| **Forms** | react-hook-form + zod | ^7.71.1 / ^4.3.5 |
| **Charts** | Recharts | ^2.15.4 |
| **Icons** | lucide-react | ^0.562.0 |
| **Animations** | Framer Motion | ^12.29.0 |
| **Theming** | next-themes | ^0.4.6 |
| **i18n** | next-intl | ^4.7.0 |
| **HTTP Client** | Axios | ^1.18.1 |
| **Validation** | Zod | ^4.3.5 |
| **Testing** | Vitest + Playwright | ^4.1.10 / 1.62.1 |
| **Logging** | Pino | ^10.3.1 |
| **DnD** | @dnd-kit/core + sortable | ^6.3.1 / ^10.0.0 |
| **Notifications** | sonner | ^2.0.7 |

### Key Observations

- **No existing Ant Design usage** — zero antd dependencies in package.json
- **Radix UI + Tailwind** is the current component architecture (shadcn/ui pattern)
- **Next.js 16** with App Router, `force-dynamic` rendering for CSP nonce support
- **React 19** with strict TypeScript
- **Zustand** for client-side state (auth, wallet, notifications, UI, transactions)
- **TanStack Query** for server state management
- **next-intl** for internationalization (id, en locales)
- **next-themes** for dark/light mode
- **Framer Motion** for animations (PageTransition, FadeIn, StaggerContainer, etc.)
- **Recharts** for data visualization
- **sonner** for toast notifications
- **@dnd-kit** for drag-and-drop (QuickActions component)

---

## 2. Complete File Tree

```
frontend/web-app/src/
├── app/                          # Next.js App Router
│   ├── globals.css               # Global styles + Tailwind theme
│   ├── providers.tsx             # Client providers (Query, Theme, Motion, Auth)
│   ├── global-error.tsx          # Global error boundary
│   └── [locale]/                 # Locale-based routing (id, en)
│       ├── layout.tsx            # Root layout (html, body, providers)
│       ├── page.tsx              # Landing page
│       ├── error.tsx             # Locale error boundary
│       ├── dashboard/
│       │   ├── page.tsx          # Dashboard home
│       │   └── loading.tsx       # Loading state
│       ├── transfer/
│       │   ├── page.tsx          # Transfer page
│       │   └── loading.tsx
│       ├── cards/
│       │   └── page.tsx          # Cards management
│       ├── pockets/
│       │   └── page.tsx          # Pockets/savings management
│       ├── bills/
│       │   ├── page.tsx          # Bill payments
│       │   └── loading.tsx
│       ├── exchange/
│       │   └── page.tsx          # FX exchange
│       ├── lending/
│       │   ├── page.tsx          # Loans & PayLater
│       │   └── loading.tsx
│       ├── investments/
│       │   ├── page.tsx          # Investment products
│       │   └── loading.tsx
│       ├── rewards/
│       │   └── page.tsx          # Rewards & gamification
│       ├── settings/
│       │   └── page.tsx          # User settings
│       ├── security/
│       │   └── page.tsx          # Security settings
│       ├── support/
│       │   └── page.tsx          # Support/FAQ
│       ├── login/
│       │   ├── page.tsx          # Login wrapper
│       │   └── login-form.tsx    # Login form (Keycloak redirect)
│       └── backoffice/
│           ├── partners/
│           │   └── page.tsx      # Partner management (admin)
│           └── fx-rates/
│               └── page.tsx      # FX rates admin
├── components/
│   ├── DashboardLayout.tsx       # Main dashboard shell (sidebar + header)
│   ├── MobileNav.tsx             # Bottom mobile navigation
│   ├── MobileHeader.tsx          # Mobile page header
│   ├── LanguageSwitcher.tsx      # Locale switcher dropdown
│   ├── ThemeToggle.tsx           # Dark/light mode toggle
│   ├── SessionBootstrap.tsx      # Session reconciliation
│   ├── ErrorBoundary.tsx         # React error boundary
│   ├── auth/
│   │   └── SilentRefreshProvider.tsx
│   ├── settings/
│   │   └── statement-downloader.tsx
│   ├── feedback/
│   │   └── FeedbackWidget.tsx    # Feedback form widget
│   ├── dashboard/
│   │   ├── BalanceCard.tsx       # Balance overview card
│   │   ├── QuickActions.tsx      # Quick action buttons (DnD)
│   │   ├── StatsCharts.tsx       # Charts (Recharts)
│   │   ├── TransferActivity.tsx  # Transaction table
│   │   ├── SpendingInsights.tsx   # Spending breakdown
│   │   ├── BudgetTracking.tsx    # Budget progress
│   │   ├── FinancialHealthScore.tsx
│   │   ├── InvestmentPerformance.tsx
│   │   └── index.ts
│   ├── cms/
│   │   ├── BannerCarousel.tsx    # Promo banner carousel
│   │   ├── PromoPopup.tsx        # Promo popup modal
│   │   ├── EmergencyAlert.tsx    # Emergency alert banner
│   │   └── index.ts
│   ├── ui/                       # shadcn/ui components (Radix + Tailwind)
│   │   ├── accordion.tsx
│   │   ├── alert.tsx
│   │   ├── avatar.tsx
│   │   ├── badge.tsx
│   │   ├── button.tsx
│   │   ├── calendar.tsx
│   │   ├── card.tsx
│   │   ├── carousel.tsx
│   │   ├── chart.tsx
│   │   ├── checkbox.tsx
│   │   ├── dialog.tsx
│   │   ├── dropdown-menu.tsx
│   │   ├── input.tsx
│   │   ├── label.tsx
│   │   ├── Logo.tsx
│   │   ├── Motion.tsx            # Framer Motion wrappers
│   │   ├── popover.tsx
│   │   ├── progress.tsx
│   │   ├── select.tsx
│   │   ├── sheet.tsx
│   │   ├── skeleton.tsx
│   │   ├── slider.tsx
│   │   ├── stepper.tsx
│   │   ├── switch.tsx
│   │   ├── table.tsx
│   │   ├── tabs.tsx
│   │   └── textarea.tsx
│   ├── personalization/
│   │   ├── VIPBadge.tsx
│   │   ├── SegmentedOffers.tsx
│   │   ├── TargetedPromos.tsx
│   │   ├── PersonalizedGreeting.tsx
│   │   └── index.ts
│   └── account/
│       └── BeneficiaryManager.tsx
├── hooks/                        # Custom React hooks
│   ├── useAuth.ts
│   ├── useUser.ts
│   ├── useWallet.ts
│   ├── useTransactions.ts
│   ├── useCards.ts
│   ├── usePockets.ts
│   ├── useBeneficiaries.ts
│   ├── useBudgets.ts
│   ├── useInvestments.ts
│   ├── useLending.ts
│   ├── useFx.ts
│   ├── useRewards.ts
│   ├── useNotifications.ts
│   ├── useCMS.ts
│   ├── useAnalytics.ts
│   ├── useCompliance.ts
│   ├── useSupport.ts
│   ├── usePartner.ts
│   ├── useBiometric.ts
│   ├── useVIPStatus.ts
│   ├── useUserSegment.ts
│   ├── useSegmentedOffers.ts
│   ├── useScheduledTransfers.ts
│   ├── useSplitBill.ts
│   ├── useSilentRefresh.ts
│   ├── useWebSocket.ts
│   └── index.ts
├── stores/                       # Zustand stores
│   ├── authStore.ts
│   ├── walletStore.ts
│   ├── notificationStore.ts
│   ├── transactionStore.ts
│   ├── uiStore.ts
│   └── index.ts
├── services/                     # API service layer
│   ├── AuthService.ts
│   ├── AccountService.ts
│   ├── WalletService.ts
│   ├── TransactionService.ts
│   ├── CardService.ts (implied)
│   ├── PocketService.ts (implied)
│   ├── BeneficiaryService.ts (implied)
│   ├── BudgetService.ts (implied)
│   ├── InvestmentService.ts
│   ├── LendingService.ts
│   ├── FxService.ts
│   ├── PromotionService.ts
│   ├── NotificationService.ts
│   ├── AnalyticsService.ts
│   ├── CMSService.ts
│   ├── StatementService.ts
│   ├── UserService.ts
│   ├── BackofficeService.ts
│   ├── BillingService.ts
│   ├── ComplianceService.ts
│   ├── KYCService.ts
│   ├── PartnerService.ts
│   ├── SegmentationService.ts
│   ├── SupportService.ts
│   └── index.ts
├── lib/                          # Utilities
│   ├── api.ts                    # Axios client with BFF proxy
│   ├── utils.ts                  # cn(), UUID, idempotency keys
│   ├── navigation.ts             # next-intl navigation helpers
│   ├── currency.ts               # Money formatting/parsing
│   ├── date.ts                   # Date utilities
│   ├── validation.ts             # Validation helpers
│   ├── a11y.tsx                  # Accessibility utilities
│   ├── mutation-config.ts        # React Query mutation config
│   ├── logger.ts                 # Pino logger
│   ├── edge-logger.ts            # Edge logging
│   └── proxy.ts                  # BFF proxy types
├── i18n/                         # Internationalization
│   ├── config.ts                 # Locale config (id, en)
│   └── request.ts                # i18n request handling
├── types/                        # TypeScript types
│   ├── index.ts                  # All shared types + Zod schemas
│   ├── jest-axe.d.ts
│   └── vitest-matchers.d.ts
├── __tests__/                    # Test files
│   ├── components/               # Component tests
│   ├── hooks/                    # Hook tests
│   ├── pages/                    # Page tests
│   ├── services/                 # Service tests
│   ├── lib/                      # Lib tests
│   ├── utils/                    # Test utilities
│   ├── api/                      # API tests
│   └── a11y/                     # Accessibility tests
└── proxy.ts                      # BFF proxy types
```

---

## 3. UI Component Inventory

### 3.1 shadcn/ui Components (src/components/ui/)

All components follow the **shadcn/ui pattern**: Radix UI primitives + Tailwind CSS styling + `cn()` utility for class merging.

| Component | Radix Primitive | Custom Styling | Key Props |
|-----------|----------------|----------------|-----------|
| **button.tsx** | @radix-ui/react-slot | cva variants (default, destructive, outline, secondary, ghost, link, emerald, glass) + sizes (default, sm, lg, icon) | `variant`, `size`, `asChild` |
| **card.tsx** | None (div) | rounded-2xl, border, shadow-card | Standard HTML div props |
| **input.tsx** | None (input) | h-14, rounded-xl, border, focus ring | Standard input props |
| **textarea.tsx** | None (textarea) | min-h-120px, rounded-xl | Standard textarea props |
| **dialog.tsx** | @radix-ui/react-dialog | Custom overlay, content, header, footer | Radix Dialog props |
| **dropdown-menu.tsx** | @radix-ui/react-dropdown-menu | Custom content, item, separator | Radix DropdownMenu props |
| **select.tsx** | @radix-ui/react-select | Custom trigger, content, item | Radix Select props |
| **tabs.tsx** | @radix-ui/react-tabs | Custom list, trigger, content | Radix Tabs props |
| **accordion.tsx** | @radix-ui/react-accordion | Custom trigger with ChevronDown | Radix Accordion props |
| **badge.tsx** | None (div) | cva variants (default, secondary, destructive, outline) | `variant` |
| **avatar.tsx** | @radix-ui/react-avatar | Custom root, image, fallback | Radix Avatar props |
| **switch.tsx** | @radix-ui/react-switch | Custom root, thumb | Radix Switch props |
| **checkbox.tsx** | @radix-ui/react-checkbox | Custom root with Check icon | Radix Checkbox props |
| **radio-group.tsx** | @radix-ui/react-radio-group | — | (Not found in ui/ but may exist) |
| **label.tsx** | @radix-ui/react-label | cva variants | Standard label props |
| **progress.tsx** | @radix-ui/react-progress | Custom root, indicator | `value`, `indicatorClassName` |
| **slider.tsx** | @radix-ui/react-slider | Custom root, track, range, thumb | Radix Slider props |
| **popover.tsx** | @radix-ui/react-popover | Custom content | Radix Popover props |
| **sheet.tsx** | @radix-ui/react-dialog (Sheet) | cva variants for sides | `side` (right, left, top, bottom) |
| **skeleton.tsx** | None (div) | animate-pulse, rounded-xl | `className` |
| **alert.tsx** | None (div) | cva variants (default, destructive) | `variant` |
| **table.tsx** | None (table) | Custom wrapper, header, body, row, cell | Standard table props |
| **calendar.tsx** | None (input[type=date]) | Custom date input wrapper | `selected`, `onSelect`, `disabled` |
| **carousel.tsx** | None (div) | Native CSS scroll-snap (replaced embla) | `orientation`, `opts` |
| **chart.tsx** | Recharts | Custom container, tooltip, legend | `config`, `data` |
| **stepper.tsx** | None (div) | Custom step indicator | `steps`, `currentStep` |
| **Logo.tsx** | None (div) | Next/image + text | `variant`, `width`, `height` |
| **Motion.tsx** | Framer Motion | PageTransition, FadeIn, ScaleIn, StaggerContainer, StaggerItem | Various motion props |

### 3.2 Custom Business Components

| Component | Location | Description | UI Elements Used |
|-----------|----------|-------------|------------------|
| **DashboardLayout** | components/DashboardLayout.tsx | Main app shell with sidebar, header, mobile nav | Button, Input, Avatar, Sheet, DropdownMenu, MobileNav, LanguageSwitcher, ThemeToggle, PersonalizedGreeting |
| **MobileNav** | components/MobileNav.tsx | Bottom tab bar for mobile | Link, lucide icons |
| **MobileHeader** | components/MobileHeader.tsx | Mobile page header with back button | Button, lucide icons |
| **LanguageSwitcher** | components/LanguageSwitcher.tsx | Locale dropdown | Button, DropdownMenu (custom) |
| **ThemeToggle** | components/ThemeToggle.tsx | Dark/light toggle | Button, next-themes |
| **BalanceCard** | components/dashboard/BalanceCard.tsx | Balance display with stats | Card, VIPBadge, lucide icons |
| **QuickActions** | components/dashboard/QuickActions.tsx | Draggable quick action buttons | Card, Button, @dnd-kit, Framer Motion |
| **StatsCharts** | components/dashboard/StatsCharts.tsx | Charts (bar, radial) | Card, Recharts, ChartContainer |
| **TransferActivity** | components/dashboard/TransferActivity.tsx | Transaction list with actions | Card, Table, Button, Badge, DropdownMenu, Skeleton |
| **SpendingInsights** | components/dashboard/SpendingInsights.tsx | Spending breakdown | Card, Accordion, Progress, Button |
| **BudgetTracking** | components/dashboard/BudgetTracking.tsx | Budget progress tracking | Card, Accordion, Progress, Button |
| **FinancialHealthScore** | components/dashboard/FinancialHealthScore.tsx | Health score gauge | Card, Progress, Framer Motion |
| **InvestmentPerformance** | components/dashboard/InvestmentPerformance.tsx | Investment radial chart | Card, Recharts, ChartContainer |
| **BannerCarousel** | components/cms/BannerCarousel.tsx | Promo banner carousel | Carousel, Skeleton, Framer Motion |
| **PromoPopup** | components/cms/PromoPopup.tsx | Promo popup dialog | Dialog, Button |
| **EmergencyAlert** | components/cms/EmergencyAlert.tsx | Emergency alert banner | Alert, Button, Framer Motion |
| **VIPBadge** | components/personalization/VIPBadge.tsx | VIP tier badge | Badge, Framer Motion |
| **SegmentedOffers** | components/personalization/SegmentedOffers.tsx | Personalized offers | Progress, Skeleton, Framer Motion |
| **TargetedPromos** | components/personalization/TargetedPromos.tsx | Targeted promotions | Skeleton, Framer Motion |
| **PersonalizedGreeting** | components/personalization/PersonalizedGreeting.tsx | Time-based greeting | Progress, Framer Motion |
| **BeneficiaryManager** | components/account/BeneficiaryManager.tsx | Beneficiary CRUD | Card, Button, Input, Label |
| **StatementDownloader** | components/settings/statement-downloader.tsx | Statement download UI | Framer Motion, lucide icons |
| **FeedbackWidget** | components/feedback/FeedbackWidget.tsx | Feedback form | Button, Input, Textarea, Checkbox, Label, Dialog |
| **SessionBootstrap** | components/SessionBootstrap.tsx | Session reconciliation | None (logic only) |
| **ErrorBoundary** | components/ErrorBoundary.tsx | Error boundary | Button, lucide icons |

### 3.3 Layout Structure

```
RootLayout (app/[locale]/layout.tsx)
├── NextIntlClientProvider (i18n)
├── ErrorBoundary
│   └── Providers (providers.tsx)
│       ├── QueryClientProvider (TanStack Query)
│       ├── ThemeProvider (next-themes)
│       ├── MotionConfig (Framer Motion)
│       ├── Toaster (sonner)
│       ├── SilentRefreshRunner
│       ├── AuthSessionExpiredHandler
│       ├── SessionBootstrap
│       ├── EmergencyAlert
│       └── {children}
│           └── DashboardLayout (for authenticated pages)
│               ├── Sidebar (desktop)
│               │   ├── Logo
│               │   ├── SidebarItem[] (nav links)
│               │   ├── PersonalizedGreeting
│               │   └── User section (Avatar, logout)
│               ├── Header (desktop)
│               │   ├── Search Input
│               │   ├── LanguageSwitcher
│               │   ├── ThemeToggle
│               │   └── Notification bell
│               ├── MobileNav (bottom tab bar)
│               └── {children} (page content)
```

---

## 4. Current Styling Approach & Design Tokens

### 4.1 Tailwind CSS v4 Configuration

The project uses **Tailwind CSS v4** with CSS-based configuration in `globals.css`:

```css
@import "tailwindcss";

@theme inline {
  /* Colors - HSL CSS variables */
  --color-background: hsl(var(--background));
  --color-foreground: hsl(var(--foreground));
  --color-primary: hsl(var(--primary));
  --color-primary-foreground: hsl(var(--primary-foreground));
  --color-card: hsl(var(--card));
  --color-card-foreground: hsl(var(--card-foreground));
  --color-accent: hsl(var(--accent));
  --color-accent-foreground: hsl(var(--accent-foreground));
  --color-muted: hsl(var(--muted));
  --color-muted-foreground: hsl(var(--muted-foreground));
  --color-destructive: hsl(var(--destructive));
  --color-border: hsl(var(--border));
  --color-ring: hsl(var(--ring));

  /* Semantic Aliases */
  --color-bank-green: hsl(var(--primary));
  --color-bank-emerald: hsl(145 50% 35%);
  --color-success-light: hsl(var(--success-light));

  /* Chart Colors */
  --color-chart-1: hsl(var(--chart-green1));
  --color-chart-2: hsl(var(--chart-green2));
  --color-chart-3: hsl(var(--chart-green3));
  --color-chart-4: hsl(var(--chart-green4));

  /* Shadows */
  --shadow-card: 0 2px 8px -2px rgba(0, 0, 0, 0.05), 0 4px 16px -4px rgba(0, 0, 0, 0.08);
  --shadow-glass: 0 8px 32px rgba(0, 0, 0, 0.12);

  /* Fonts */
  --font-sans: var(--font-inter), system-ui, sans-serif;
  --font-heading: var(--font-outfit), system-ui, sans-serif;
  --font-mono: var(--font-inter), ui-monospace, monospace;
}
```

### 4.2 CSS Custom Properties (Design Tokens)

Defined in `:root` and `.dark` selectors:

| Token | Light Mode | Dark Mode | Usage |
|-------|-----------|-----------|-------|
| `--background` | `0 0% 100%` | `0 0% 3.9%` | Page background |
| `--foreground` | `0 0% 9%` | `0 0% 98%` | Text color |
| `--primary` | `145 50% 35%` (emerald) | `145 50% 35%` | Primary brand color |
| `--primary-foreground` | `0 0% 100%` | `0 0% 100%` | Text on primary |
| `--card` | `0 0% 100%` | `0 0% 7%` | Card background |
| `--muted` | `0 0% 96.1%` | `0 0% 14.9%` | Muted background |
| `--muted-foreground` | `0 0% 45.1%` | `0 0% 63.9%` | Muted text |
| `--destructive` | `0 84.2% 60.2%` | `0 62.8% 30.6%` | Error/destructive |
| `--border` | `0 0% 89.8%` | `0 0% 14.9%` | Border color |
| `--ring` | `145 50% 35%` | `145 50% 35%` | Focus ring |
| `--success-light` | `145 50% 35%` | `145 50% 35%` | Success state |

### 4.3 Typography

- **Body font:** Inter (Google Font, `--font-inter`)
- **Heading font:** Outfit (Google Font, `--font-outfit`)
- **Base size:** Tailwind default (16px)
- **Font weights:** bold (700), font-black (900) used extensively

### 4.4 Spacing & Border Radius

- **Border radius:** `rounded-xl` (12px), `rounded-2xl` (16px), `rounded-3xl` (24px)
- **Card padding:** `p-5` to `p-8` (20px to 32px)
- **Gap:** `gap-4` to `gap-8` (16px to 32px)
- **Button height:** `h-12` (48px) default, `h-14` (56px) large, `h-9` (36px) small

### 4.5 Theme Support

- **Light/Dark mode** via `next-themes` with class strategy
- **High contrast mode** via `prefers-contrast: high` media query
- **Custom theme toggle** component in header

### 4.6 Animation

- **Framer Motion** for page transitions, fade-ins, stagger animations
- **CSS keyframes** for fade-in, float-3d
- **Tailwind transitions** for hover/active states

---

## 5. Ant Design Version & Compatibility Assessment

### 5.1 Current Ant Design Version

| Property | Value |
|----------|-------|
| **Latest stable** | 6.6.1 (npm) / 6.5.3 (GitHub) |
| **React peer dependency** | >=16.9.0 (React 19 fully supported) |
| **TypeScript** | Built-in type declarations |
| **Package size** | ~48 dependencies |
| **License** | MIT |

### 5.2 Compatibility Matrix

| PayU Stack | Ant Design Compatibility | Notes |
|------------|------------------------|-------|
| **React 19.2.3** | ✅ Full support | antd 6.x supports React 19 |
| **Next.js 16.3.3** | ✅ Compatible | Needs `ssr: false` for some components; App Router works |
| **TypeScript 5** | ✅ Full support | Built-in types |
| **Tailwind CSS 4** | ⚠️ Coexistence needed | antd uses CSS-in-JS; Tailwind for layout/spacing |
| **next-intl** | ✅ Compatible | antd has locale support; can use `ConfigProvider` locale |
| **next-themes** | ✅ Compatible | antd has built-in dark mode via `ConfigProvider` theme |
| **Zustand** | ✅ Compatible | No conflicts |
| **TanStack Query** | ✅ Compatible | No conflicts |
| **react-hook-form** | ✅ Compatible | antd Form has its own validation; can integrate with zod |
| **Framer Motion** | ✅ Compatible | Can coexist; antd has its own animations |
| **Recharts** | ⚠️ Overlap | antd has built-in charts (Ant Design Charts) |
| **sonner** | ⚠️ Overlap | antd has message/notification built-in |
| **lucide-react** | ⚠️ Overlap | antd has @ant-design/icons |
| **@dnd-kit** | ⚠️ Overlap | antd Table has drag-sort; Transfer component |

### 5.3 Key Migration Considerations

1. **CSS-in-JS vs Tailwind**: antd v6 uses CSS-in-JS (not Less like v5). This means no `less` dependency and better SSR support, but requires `ConfigProvider` for theming.

2. **Bundle Size**: antd is ~1MB+ gzipped. Consider modular imports (`import { Button } from 'antd'`) and tree-shaking.

3. **SSR with Next.js 16**: antd v6 supports SSR. Need to handle:
   - `ConfigProvider` with `ssr: true` for server-side rendering
   - Dark mode FOUC (Flash of Unstyled Content) — use cookie-based theme detection
   - `App.useApp()` for static methods (message, notification, modal)

4. **Form Migration**: Current uses `react-hook-form` + `zod`. antd Form has its own validation. Options:
   - Keep react-hook-form + zod, use antd Form.Item for layout only
   - Migrate to antd Form with zod validator
   - Use `Form.useForm()` for imperative API

5. **Icon Migration**: lucide-react → @ant-design/icons. Different API but similar usage.

6. **Animation Migration**: Framer Motion → antd animations + CSS transitions. antd has built-in motion for Modal, Drawer, etc.

---

## 6. Recommended Migration Strategy

### 6.1 Component Mapping: Custom → Ant Design

| Current Custom Component | Ant Design Equivalent | Migration Effort | Notes |
|-------------------------|----------------------|------------------|-------|
| **button.tsx** | `Button` | Low | Map variants: default→primary, destructive→primary danger, outline→default, secondary→default, ghost→text, link→link |
| **card.tsx** | `Card` | Low | Direct replacement; antd Card has built-in hoverable, cover, actions |
| **input.tsx** | `Input` | Low | antd Input has size, prefix, suffix, addonBefore/After |
| **textarea.tsx** | `Input.TextArea` | Low | Direct replacement |
| **dialog.tsx** | `Modal` | Low | antd Modal has more features (draggable, confirm) |
| **dropdown-menu.tsx** | `Dropdown` | Low | antd Dropdown with menu items |
| **select.tsx** | `Select` | Medium | antd Select has more features (search, tags, virtual scroll) |
| **tabs.tsx** | `Tabs` | Low | Direct replacement |
| **accordion.tsx** | `Collapse` | Low | antd Collapse has accordion mode |
| **badge.tsx** | `Badge` | Low | antd Badge has count, dot, status |
| **avatar.tsx** | `Avatar` | Low | Direct replacement |
| **switch.tsx** | `Switch` | Low | Direct replacement |
| **checkbox.tsx** | `Checkbox` | Low | Direct replacement |
| **label.tsx** | `Form.Item` label | Low | Integrate with Form |
| **progress.tsx** | `Progress` | Low | antd Progress has circle, dashboard, steps |
| **slider.tsx** | `Slider` | Low | Direct replacement |
| **popover.tsx** | `Popover` | Low | Direct replacement |
| **sheet.tsx** | `Drawer` | Low | antd Drawer has placement, size |
| **skeleton.tsx** | `Skeleton` | Low | antd Skeleton has avatar, title, paragraph, button |
| **alert.tsx** | `Alert` | Low | antd Alert has type, closable, banner |
| **table.tsx** | `Table` | Medium | antd Table has sorting, filtering, pagination, rowSelection |
| **calendar.tsx** | `DatePicker` | Medium | antd DatePicker has range, format, disabledDate |
| **carousel.tsx** | `Carousel` | Low | antd Carousel has autoplay, dots |
| **chart.tsx** | `@ant-design/plots` | High | Recharts → Ant Design Charts (different API) |
| **stepper.tsx** | `Steps` | Low | antd Steps has status, direction, progressDot |
| **Logo.tsx** | Custom (keep) | None | Keep as-is |
| **Motion.tsx** | antd motion + CSS | Medium | Replace Framer Motion with antd animations |
| **DashboardLayout** | `Layout` + `Sider` + `Header` | High | Major restructuring |
| **MobileNav** | `TabBar` (antd-mobile) or custom | Medium | Consider antd-mobile for mobile nav |
| **LanguageSwitcher** | `Dropdown` + `Button` | Low | Use antd Dropdown |
| **ThemeToggle** | `Switch` or `Button` | Low | Use antd Switch with Sun/Moon icons |
| **BalanceCard** | `Card` + `Statistic` | Low | antd Statistic for numbers |
| **QuickActions** | `Card` + `Button` + `Table` DnD | Medium | Replace @dnd-kit with antd Table drag |
| **StatsCharts** | `@ant-design/plots` | High | Recharts → AntV |
| **TransferActivity** | `Table` | Medium | antd Table with built-in features |
| **SpendingInsights** | `Card` + `Progress` + `Collapse` | Low | Direct component replacement |
| **BudgetTracking** | `Card` + `Progress` + `Collapse` | Low | Direct component replacement |
| **FinancialHealthScore** | `Card` + `Progress` (circle) | Low | Use antd Progress type="circle" |
| **InvestmentPerformance** | `@ant-design/plots` | High | Recharts → AntV |
| **BannerCarousel** | `Carousel` | Low | Direct replacement |
| **PromoPopup** | `Modal` | Low | Direct replacement |
| **EmergencyAlert** | `Alert` | Low | Direct replacement |
| **VIPBadge** | `Badge` | Low | Direct replacement |
| **SegmentedOffers** | `Card` + `Progress` | Low | Direct replacement |
| **TargetedPromos** | `Card` | Low | Direct replacement |
| **PersonalizedGreeting** | `Typography` + `Progress` | Low | Direct replacement |
| **BeneficiaryManager** | `Card` + `Form` + `Table` | Medium | Use antd Form |
| **StatementDownloader** | `Card` + `Button` + `Table` | Medium | Use antd Table |
| **FeedbackWidget** | `Modal` + `Form` | Medium | Use antd Form |
| **ErrorBoundary** | `Result` | Low | Use antd Result for error states |
| **SessionBootstrap** | Keep as-is | None | Logic only, no UI |

### 6.2 Infrastructure Migration

| Current | Ant Design Equivalent | Notes |
|---------|----------------------|-------|
| **sonner** (toasts) | `message` / `notification` | Use `App.useApp()` for static methods |
| **lucide-react** (icons) | `@ant-design/icons` | Different import pattern |
| **next-themes** | `ConfigProvider` theme | antd has built-in dark mode |
| **Framer Motion** | antd motion + CSS | antd has built-in animations |
| **Recharts** | `@ant-design/plots` | Different API (AntV vs Recharts) |
| **@dnd-kit** | `Table` drag-sort or `Transfer` | antd has built-in DnD for tables |
| **Tailwind CSS** | Keep for layout | Use Tailwind for spacing/layout, antd for components |
| **class-variance-authority** | Not needed | antd has built-in variants |
| **clsx + tailwind-merge** | Not needed | Use antd className prop |
| **next-intl** | `ConfigProvider` locale | antd has locale support |

### 6.3 Theming Strategy

Map current Tailwind design tokens to antd `ConfigProvider` theme:

```typescript
<ConfigProvider
  theme={{
    token: {
      colorPrimary: '#10b981',        // emerald-500 (bank-green)
      colorSuccess: '#10b981',        // success-light
      colorError: '#ef4444',          // destructive
      colorWarning: '#f59e0b',        // warning
      colorInfo: '#3b82f6',           // info
      borderRadius: 12,               // rounded-xl
      borderRadiusLG: 16,             // rounded-2xl
      borderRadiusXL: 24,             // rounded-3xl
      fontFamily: 'Inter, system-ui, sans-serif',
      fontSize: 14,
      wireframe: false,
    },
    components: {
      Button: {
        controlHeight: 48,
        controlHeightLG: 56,
        controlHeightSM: 36,
        fontWeight: 700,
        letterSpacing: '0.15em',
      },
      Card: {
        borderRadiusLG: 16,
        boxShadowTertiary: '0 2px 8px -2px rgba(0, 0, 0, 0.05)',
      },
      Input: {
        controlHeight: 48,
        borderRadius: 12,
      },
      Table: {
        headerBg: 'transparent',
        headerColor: 'rgba(0, 0, 0, 0.45)',
      },
    }}
  }
  locale={antdLocale}  // from next-intl locale
>
  <App>{children}</App>
</ConfigProvider>
```

---

## 7. Phased Rollout Plan

### Phase 0: Foundation (Week 1-2)

**Goal:** Set up antd infrastructure without changing visible UI.

- [ ] Install `antd` and `@ant-design/icons`
- [ ] Create `AntdProvider` component with `ConfigProvider` + `App`
- [ ] Map design tokens (colors, typography, spacing) to antd theme
- [ ] Set up dark mode with `ConfigProvider` theme algorithm
- [ ] Configure i18n locale mapping (id → id_ID, en → en_US)
- [ ] Create migration utility: `useAntdMessage()` hook wrapping `App.useApp()`
- [ ] Set up modular imports and tree-shaking
- [ ] Add `antd` to `transpilePackages` in next.config.ts if needed
- [ ] Create migration guide document for team

**Deliverable:** antd installed, themed, and ready for component migration. No visible UI changes.

### Phase 1: Basic Components (Week 3-4)

**Goal:** Replace shadcn/ui primitives with antd equivalents.

- [ ] Replace `button.tsx` → antd `Button`
- [ ] Replace `card.tsx` → antd `Card`
- [ ] Replace `input.tsx` → antd `Input`
- [ ] Replace `textarea.tsx` → antd `Input.TextArea`
- [ ] Replace `badge.tsx` → antd `Badge`
- [ ] Replace `avatar.tsx` → antd `Avatar`
- [ ] Replace `switch.tsx` → antd `Switch`
- [ ] Replace `checkbox.tsx` → antd `Checkbox`
- [ ] Replace `label.tsx` → antd `Form.Item` label
- [ ] Replace `progress.tsx` → antd `Progress`
- [ ] Replace `slider.tsx` → antd `Slider`
- [ ] Replace `skeleton.tsx` → antd `Skeleton`
- [ ] Replace `alert.tsx` → antd `Alert`
- [ ] Replace `Logo.tsx` → keep custom (no antd equivalent needed)

**Deliverable:** All basic UI primitives replaced. Pages still use same layout but with antd components.

### Phase 2: Complex Components (Week 5-6)

**Goal:** Replace complex interactive components.

- [ ] Replace `dialog.tsx` → antd `Modal`
- [ ] Replace `dropdown-menu.tsx` → antd `Dropdown`
- [ ] Replace `select.tsx` → antd `Select`
- [ ] Replace `tabs.tsx` → antd `Tabs`
- [ ] Replace `accordion.tsx` → antd `Collapse`
- [ ] Replace `popover.tsx` → antd `Popover`
- [ ] Replace `sheet.tsx` → antd `Drawer`
- [ ] Replace `table.tsx` → antd `Table`
- [ ] Replace `calendar.tsx` → antd `DatePicker`
- [ ] Replace `carousel.tsx` → antd `Carousel`
- [ ] Replace `stepper.tsx` → antd `Steps`
- [ ] Replace `chart.tsx` → antd charts (or keep Recharts initially)

**Deliverable:** All complex components replaced. Most pages now use antd components.

### Phase 3: Layout & Navigation (Week 7-8)

**Goal:** Replace custom layout with antd Layout.

- [ ] Replace `DashboardLayout` → antd `Layout` + `Sider` + `Header`
- [ ] Replace `MobileNav` → antd `Tabs` (mobile) or custom bottom nav
- [ ] Replace `MobileHeader` → antd `PageHeader` or custom
- [ ] Replace `LanguageSwitcher` → antd `Dropdown` + `Button`
- [ ] Replace `ThemeToggle` → antd `Switch` or `Button`
- [ ] Replace `PersonalizedGreeting` → antd `Typography` + `Progress`
- [ ] Replace `ThemeToggle` → antd `ConfigProvider` theme switch
- [ ] Implement antd `Breadcrumb` for navigation
- [ ] Implement antd `Menu` for sidebar navigation

**Deliverable:** Full layout migration. App shell now uses antd Layout.

### Phase 4: Business Components (Week 9-10)

**Goal:** Replace business-specific components.

- [ ] Replace `BalanceCard` → antd `Card` + `Statistic`
- [ ] Replace `QuickActions` → antd `Card` + `Button` (keep @dnd-kit or use antd Table)
- [ ] Replace `StatsCharts` → `@ant-design/plots` (or keep Recharts)
- [ ] Replace `TransferActivity` → antd `Table`
- [ ] Replace `SpendingInsights` → antd `Card` + `Progress` + `Collapse`
- [ ] Replace `BudgetTracking` → antd `Card` + `Progress` + `Collapse`
- [ ] Replace `FinancialHealthScore` → antd `Card` + `Progress` (circle)
- [ ] Replace `InvestmentPerformance` → `@ant-design/plots`
- [ ] Replace `BannerCarousel` → antd `Carousel`
- [ ] Replace `PromoPopup` → antd `Modal`
- [ ] Replace `EmergencyAlert` → antd `Alert`
- [ ] Replace `VIPBadge` → antd `Badge`
- [ ] Replace `SegmentedOffers` → antd `Card` + `Progress`
- [ ] Replace `TargetedPromos` → antd `Card`
- [ ] Replace `BeneficiaryManager` → antd `Card` + `Form` + `Table`
- [ ] Replace `StatementDownloader` → antd `Card` + `Button` + `Table`
- [ ] Replace `FeedbackWidget` → antd `Modal` + `Form`

**Deliverable:** All business components migrated.

### Phase 5: Forms & Validation (Week 11-12)

**Goal:** Migrate form handling to antd Form.

- [ ] Replace `react-hook-form` + `zod` with antd `Form` + zod validator
- [ ] Migrate transfer form to antd `Form`
- [ ] Migrate exchange form to antd `Form`
- [ ] Migrate settings form to antd `Form`
- [ ] Migrate beneficiary form to antd `Form`
- [ ] Migrate feedback form to antd `Form`
- [ ] Migrate login form to antd `Form`
- [ ] Implement antd `Form` validation with zod schemas

**Deliverable:** All forms use antd Form with validation.

### Phase 6: Notifications & Feedback (Week 13)

**Goal:** Replace sonner with antd message/notification.

- [ ] Replace `sonner` toasts with antd `message`
- [ ] Replace `useUIStore.addToast` with antd `message`
- [ ] Implement `App.useApp()` pattern for static methods
- [ ] Replace `notificationStore` with antd `notification`
- [ ] Implement antd `notification` for system notifications

**Deliverable:** All notifications use antd.

### Phase 7: Icons & Assets (Week 14)

**Goal:** Replace lucide-react with @ant-design/icons.

- [ ] Replace all lucide-react imports with @ant-design/icons
- [ ] Update icon usage patterns
- [ ] Remove lucide-react dependency

**Deliverable:** All icons use @ant-design/icons.

### Phase 8: Animation & Polish (Week 15)

**Goal:** Replace Framer Motion with antd animations.

- [ ] Replace `PageTransition` → antd `Fade` or CSS
- [ ] Replace `FadeIn` → antd motion or CSS
- [ ] Replace `StaggerContainer` → antd `List` motion or CSS
- [ ] Replace `ButtonMotion` → antd `Button` with loading state
- [ ] Remove framer-motion dependency (or keep for complex animations)

**Deliverable:** All animations use antd or CSS.

### Phase 9: Testing & Cleanup (Week 16-17)

**Goal:** Update tests and remove old dependencies.

- [ ] Update all component tests to use antd
- [ ] Update all page tests to use antd
- [ ] Update all hook tests to use antd
- [ ] Remove old shadcn/ui components
- [ ] Remove `class-variance-authority` dependency
- [ ] Remove `clsx` and `tailwind-merge` (if no longer needed)
- [ ] Remove `sonner` dependency
- [ ] Remove `lucide-react` dependency
- [ ] Remove `framer-motion` dependency (if fully replaced)
- [ ] Remove `@dnd-kit` dependency (if fully replaced)
- [ ] Remove `recharts` dependency (if fully replaced)
- [ ] Update `package.json` and lock file
- [ ] Update documentation

**Deliverable:** Clean antd-only codebase with updated tests.

### Phase 10: Optimization (Week 18)

**Goal:** Performance optimization and bundle analysis.

- [ ] Analyze bundle size with `@next/bundle-analyzer`
- [ ] Implement code splitting for antd components
- [ ] Optimize antd imports (modular)
- [ ] Implement virtual scroll for large tables
- [ ] Optimize charts rendering
- [ ] Lighthouse audit
- [ ] Core Web Vitals check

**Deliverable:** Optimized production build.

---

## 8. Risks and Considerations

### 8.1 High Risks

| Risk | Impact | Mitigation |
|------|--------|------------|
| **Bundle size increase** | antd adds ~1MB+ gzipped | Use modular imports, code splitting, tree-shaking. Consider `@ant-design/cssinjs` for smaller footprint. |
| **SSR hydration mismatch** | antd components may hydrate differently | Use `ConfigProvider` with `ssr: true`, handle dark mode with cookie detection, use `useEffect` for client-only state |
| **Form migration complexity** | react-hook-form → antd Form is non-trivial | Migrate forms one at a time, keep zod schemas, use `Form.useForm()` for imperative API |
| **Chart migration** | Recharts → AntV has different API | Consider keeping Recharts initially, migrate charts last, or use `@ant-design/plots` which has similar API |
| **Design consistency** | antd default design differs from current | Invest time in `ConfigProvider` theme mapping, create component overrides |
| **Team learning curve** | Team unfamiliar with antd | Provide training, create migration guide, pair programming |

### 8.2 Medium Risks

| Risk | Impact | Mitigation |
|------|--------|------------|
| **Tailwind coexistence** | Two styling systems may conflict | Use Tailwind for layout/spacing only, antd for components. Clear separation. |
| **Icon migration** | Different icon names and APIs | Create icon mapping document, automated codemod |
| **Animation regression** | Framer Motion → antd motion may lose polish | Keep Framer Motion for complex animations, use antd for simple transitions |
| **Test updates** | All tests need updating | Update tests incrementally per phase, use antd testing utilities |
| **i18n integration** | next-intl + antd locale | Map locales correctly, test all locales |

### 8.3 Low Risks

| Risk | Impact | Mitigation |
|------|--------|------------|
| **Dark mode** | antd has built-in dark mode | Use `ConfigProvider` theme algorithm, test thoroughly |
| **Accessibility** | antd has good a11y | Test with axe-core, maintain a11y standards |
| **TypeScript types** | antd has built-in types | Should be seamless with TypeScript 5 |
| **Performance** | antd is well-optimized | Use virtual scroll, pagination, lazy loading |

### 8.4 Breaking Changes to Watch

1. **antd v5 → v6**: If upgrading from v5, check [V5 to V6 migration guide](https://ant.design/docs/react/migration-v5-to-v6)
2. **CSS-in-JS**: antd v6 uses CSS-in-JS, no Less dependency
3. **Static methods**: `message`, `notification`, `modal` require `App.useApp()` in v5+
4. **Theme system**: `ConfigProvider` theme prop replaces `less` variables
5. **Form API**: antd Form has different API from react-hook-form

### 8.5 Recommended Approach

**Incremental Migration**: Use the phased approach above. Each phase delivers working software. This reduces risk and allows for course correction.

**Parallel Run**: Consider running both UI systems in parallel during migration:
- Use feature flags to toggle between old and new components
- Migrate one page at a time
- Keep old components until new ones are stable

**Testing Strategy**:
- Maintain existing tests during migration
- Add new tests for antd components
- Use Storybook for component isolation testing
- E2E tests with Playwright for critical flows

**Documentation**:
- Create antd migration guide for the team
- Document component mapping decisions
- Maintain a changelog of migrated components

---

## Appendix A: Dependency Changes

### New Dependencies
```json
{
  "dependencies": {
    "antd": "^6.6.1",
    "@ant-design/icons": "^5.x",
    "@ant-design/plots": "^2.x"  // if migrating charts
  }
}
```

### Removed Dependencies (after full migration)
```json
{
  "dependencies": {
    "@radix-ui/react-accordion": "^1.2.12",
    "@radix-ui/react-avatar": "^1.1.11",
    "@radix-ui/react-checkbox": "^1.3.3",
    "@radix-ui/react-dialog": "^1.1.15",
    "@radix-ui/react-dropdown-menu": "^2.1.16",
    "@radix-ui/react-label": "^2.1.8",
    "@radix-ui/react-popover": "^1.1.15",
    "@radix-ui/react-progress": "^1.1.8",
    "@radix-ui/react-select": "^2.2.6",
    "@radix-ui/react-slider": "^1.3.6",
    "@radix-ui/react-slot": "^1.2.4",
    "@radix-ui/react-switch": "^1.2.6",
    "@radix-ui/react-tabs": "^1.1.13",
    "class-variance-authority": "^0.7.1",
    "clsx": "^2.1.1",
    "framer-motion": "^12.29.0",
    "lucide-react": "^0.562.0",
    "recharts": "^2.15.4",
    "sonner": "^2.0.7",
    "tailwind-merge": "^3.4.0"
  }
}
```

### Kept Dependencies
```json
{
  "dependencies": {
    "@dnd-kit/core": "^6.3.1",        // if keeping DnD
    "@dnd-kit/sortable": "^10.0.0",
    "@dnd-kit/utilities": "^3.2.2",
    "@hookform/resolvers": "^5.2.2",  // if keeping RHF
    "@tanstack/react-query": "^5.90.19",
    "axios": "^1.18.1",
    "date-fns": "^4.4.0",
    "next": "16.3.3",
    "next-intl": "^4.7.0",
    "next-themes": "^0.4.6",
    "pino": "^10.3.1",
    "react": "19.2.3",
    "react-dom": "19.2.3",
    "react-hook-form": "^7.7.1",      // if keeping RHF
    "zod": "^4.3.5",
    "zustand": "^5.0.10"
  }
}
```

---

## Appendix B: Key Files for Migration

### Files to Create
1. `src/components/AntdProvider.tsx` — ConfigProvider + App wrapper
2. `src/components/ThemeProvider.tsx` — Dark mode with antd theme
1. `src/lib/antd-locale.ts` — Locale mapping
2. `src/hooks/useAntdMessage.ts` — message/notification hook
3. `src/hooks/useAntdModal.ts` — modal hook

### Files to Modify
1. `src/app/providers.tsx` — Add AntdProvider
2. `src/app/[locale]/layout.tsx` — Add ConfigProvider
3. `next.config.ts` — Add transpilePackages if needed
4. `package.json` — Add/remove dependencies

### Files to Delete (after migration)
1. `src/components/ui/*.tsx` — All shadcn/ui components
2. `src/components/ui/Motion.tsx` — Framer Motion wrappers

---

*End of Audit Report*
