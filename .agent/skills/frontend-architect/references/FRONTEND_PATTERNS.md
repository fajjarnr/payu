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

## 🧪 Frontend Testing & Quality
*   **Vitest over Jest**: Use Vitest for faster execution and better integration with Vite/Next.js. 
*   **Deprecation Strategy**: When migrating from Jest, rename old config files to `*.deprecated` to preserve history while ensuring the new runner is used.
*   **E2E Synchronization**: Always use `waitForLoadState('networkidle')` and `data-testid` selectors in Playwright tests to handle React hydration and dynamic content.

## 🛡️ Frontend Security
*   **JWT Protection**: Strictly use **httpOnly cookies** via a BFF (Backend-for-Frontend).
*   **CSP & SSRF**: Restrict `next.config.ts` `remotePatterns` to known domains (e.g., `*.payu.fajjjar.my.id`). Avoid `hostname: '**'` in production.
