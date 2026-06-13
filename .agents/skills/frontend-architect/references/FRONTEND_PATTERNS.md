# Frontend Architecture & Engineering Patterns

## 📦 State Management Strategy (Zustand & TanStack Query)
*   **Separation of Concerns**:
    *   **TanStack Query**: Handles server state (fetching, caching, synchronization). Use hooks like `useWallet()`, `useAuth()`.
    *   **Zustand**: Handles client-only state (UI filters, drawer toggles, optimistic updates, temporary form data).
*   **Optimistic Updates**: Use Zustand to show balance changes immediately upon transfer initiation while TanStack Query waits for actual API confirmation.
*   **Derived State**: Calculate unread notification counts or search summaries in Zustand or memoized hooks to avoid redundant server calls.

## 🚀 Next.js Performance Optimization
*   **Loading Skeletons (LCP)**: Create `loading.tsx` for all high-traffic routes (Dashboard, Transfer, Investments).
    *   **Pattern**: Skeleton shapes MUST match the final UI layout to reduce layout shift (CLS).
    *   **Styling**: Use `animate-pulse` (Tailwind) for consistent shimmer effects.
*   **Dynamic Imports**: Use `next/dynamic` for heavy components (charts, complex data tables) to improve initial bundle size.
*   **Static Search Index**: For small doc sites, use a client-side static JSON index for instant search (Cmd+K) without external dependencies.

## 🌐 BFF Path Whitelist — Maintenance Checklist (L-016)

The BFF proxy (`src/app/api/v1/[...path]/route.ts`) uses `ALLOWED_PATH_PREFIXES` for SSRF defense. When adding a new backend service or API path:

1.  Add the new prefix to `ALLOWED_PATH_PREFIXES` in the BFF route handler
2.  Use `startsWith(prefix + '/')` with trailing slash to prevent prefix overlap attacks
3.  Test that the new path is accessible through the BFF proxy
4.  Verify that similar-prefix paths (e.g., `/accounts` vs `/accountsEvil`) are correctly blocked

**Rule**: Treat the BFF whitelist as a mandatory checklist item when onboarding a new service or domain. Silent 400 responses from the BFF are almost always a missing prefix.

## 🌍 i18n Middleware — Locale Detection & Route Guarding (L-017)

**1. Config-driven locale pattern** — Build regex dynamically from config:
```typescript
const localePattern = new RegExp(`^/(${locales.join('|')})`);
```
Source of truth: `i18n/config.ts` (single file, imported everywhere).

**2. Disable automatic locale detection** — `localeDetection: false` in middleware. Auto-detection via `Accept-Language` causes Indonesian banking app users with English browser locale to be redirected to `/en/dashboard` unexpectedly.

**3. Segment-boundary route matching** — The original `publicRoutes.includes(path)` allowed `/login-debug` to bypass auth because it started with `/login`. Fixed to:
```typescript
pathWithoutLocale === route || pathWithoutLocale.startsWith(route + '/')
```

**4. Locale-aware navigation** — All route navigation must use `Link`, `useRouter`, `redirect` from `@/lib/navigation` (wrapping `next-intl/navigation`), NEVER raw `next/navigation`.

**Rule**: (a) Define `locales` and `defaultLocale` in exactly one file. (b) Disable `localeDetection` when default locale is contextually obvious. (c) Always use segment-boundary matching for route access control. (d) Import navigation primitives exclusively from `@/lib/navigation`.

## 🔄 SilentRefreshProvider — Session Refresh for All Authenticated Routes (L-020)

`SilentRefreshProvider` must wrap EVERY authenticated route layout, not just `dashboard/layout.tsx`. Users navigating directly to `/transfer` or `/settings` had no silent refresh running, causing mid-session 401 errors.

**Required Layouts**:
*   `app/[locale]/dashboard/layout.tsx`
*   `app/[locale]/transfer/layout.tsx`
*   `app/[locale]/settings/layout.tsx`
*   `app/[locale]/exchange/layout.tsx`

**Four Defensive Measures in `useSilentRefresh()`**:
1.  **Concurrency lock** (`isRefreshingRef`) — prevents parallel refresh calls
2.  **Ref mirror for reactive state** — `setTimeout` captures stale closure values; `useRef` always reads current `isAuthenticated`
3.  **Immediate refresh on mount** — if `tokenExpiresAt === null`, refresh immediately
4.  **Exponential backoff** — 2s, 4s, 8s, 16s, 32s, max 5 retries on failure

**Stale Closure Gotcha**: `setTimeout` captures `isAuthenticated` from the render when the timer was set. The `useRef` mirror solves this:
```typescript
const isAuthenticatedRef = useRef(isAuthenticated);
useEffect(() => { isAuthenticatedRef.current = isAuthenticated; }, [isAuthenticated]);
```

**Rule**: (a) Wrap EVERY authenticated layout in `<SilentRefreshProvider>`. (b) In hooks using `setTimeout`/`setInterval`, store reactive state in `useRef` mirror — never read Zustand/React state directly inside timer callbacks. (c) Implement concurrency lock for parallel refresh prevention. (d) Exponential backoff with 5 retry cap.

## 🧪 Frontend Testing & Quality
*   **Vitest over Jest**: Use Vitest for faster execution and better integration with Vite/Next.js. 
*   **Deprecation Strategy**: When migrating from Jest, rename old config files to `*.deprecated` to preserve history while ensuring the new runner is used.
*   **E2E Synchronization**: Always use `waitForLoadState('networkidle')` and `data-testid` selectors in Playwright tests to handle React hydration and dynamic content.

## 🛡️ Frontend Security
*   **JWT Protection**: Strictly use **httpOnly cookies** via a BFF (Backend-for-Frontend).
*   **CSP & SSRF**: Restrict `next.config.ts` `remotePatterns` to known domains (e.g., `*.payu.fajjjar.my.id`). Avoid `hostname: '**'` in production.
