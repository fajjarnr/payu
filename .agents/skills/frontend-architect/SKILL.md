---
name: frontend-architect
description: PayU frontend architecture for Next.js 16 and React 19 web applications plus Expo 52 and React Native mobile applications, with BFF security, money-safe UI, accessibility, testing, performance, and Context7-first library verification.
---

# Frontend Architect — PayU

Use this skill when implementing, debugging, reviewing, or designing PayU web or mobile frontend work. The frontend is a client of financial APIs, not the source of truth for identity, balances, ledger state, authorization, or transaction success.

## Operating contract

1. Read the repository `AGENTS.md` and the relevant architecture/security guide before changing code.
2. Inspect the target app's `package.json`, lockfile, and existing pattern before choosing an API or adding a dependency.
3. For every third-party library or framework API, resolve the official library with Context7 and query the exact installed version. If that version is unavailable, query the nearest documented version, record the mismatch, and avoid undocumented behavior.
4. Re-run the Context7 check after changing a dependency, framework configuration, or major integration boundary.
5. Use the smallest change that satisfies the request. Do not add speculative abstractions, packages, design tokens, or state layers.
6. For new features, present the design/interaction/data-flow plan before implementation. For bugs, reproduce the behavior with a focused test before fixing it.
7. Report commands and evidence. Do not claim a build, test, accessibility scan, or security property without running the relevant check.

## Repository baseline

These versions are observed in this repository and can drift; verify them before work.

| Surface | Current baseline | Existing test/tooling |
|---|---|---|
| Web | Next.js 16.2.12, React 19.2.3, TypeScript strict, Tailwind 4 | Vitest + Testing Library, Playwright, axe |
| Mobile | Expo 52, React 18.3.1, React Native 0.76.9, Expo Router 4 | Jest + React Native Testing Library, Maestro |
| Web API boundary | Next.js BFF at `/api/v1/[...path]` | HttpOnly cookie forwarding, path allowlist, SSRF tests |

The version in the manifest wins over this table. Do not write a guide that silently assumes Next.js 15, React 18, or the latest Expo SDK.

## PayU invariants

### Money

- Never represent `amount`, `balance`, `fee`, `limit`, or exchange-rate values as JavaScript `number`, `float`, or `double`.
- Keep user-entered and API monetary values as canonical decimal strings at the UI boundary. Validate syntax and range, then let the backend perform authoritative arithmetic and rounding.
- Do not use `valueAsNumber`, numeric form coercion, `toFixed()` as business logic, or client-side optimistic subtraction of balances.
- The backend contract is `DECIMAL(19,4)` with `HALF_EVEN`. Display formatting may round for presentation, but must not change the value sent to the API.
- After a payment or transfer, display the server response or refetch authoritative account data. An optimistic UI may show pending state, never invent a new balance.

### Mutations and API contracts

- Payment, transfer, top-up, refund, and other financial writes require `X-Idempotency-Key`. Create it once per user intent and preserve it across retries.
- Never retry a financial write blindly. Retry only when the API contract and idempotency behavior make the outcome safe; otherwise show an ambiguous/pending state and reconcile.
- Use versioned plural kebab-case endpoints and RFC 9457-compatible error handling from the backend contract.
- Preserve correlation/request identifiers when surfacing errors. Never log tokens, PINs, NIK, full account numbers, or unmasked financial data.

### Identity and authorization

- A client-side `isAuthenticated` flag is presentation state, not authorization. The server and gateway must authorize every protected request.
- Never expose access or refresh tokens to web JavaScript. Never put them in localStorage, sessionStorage, Zustand, React Query, URLs, error objects, or telemetry.
- Do not make a security decision from user-controlled route params, decoded unverified JWT claims, or cached profile state.

## Context7-first framework usage

### Next.js App Router

- Prefer Server Components for data loading, metadata, and static composition. Add `'use client'` only at the smallest interactive leaf that needs browser APIs, event handlers, or client state.
- In Next.js 16, request APIs such as `cookies()`, `headers()`, and route `params`/`searchParams` are asynchronous. Follow the installed version's documented signatures; do not copy a synchronous example from an older release.
- Treat `use cache`, `cacheLife`, `cacheTag`, and `updateTag` as versioned APIs. Context7 documents them under Next.js 16's Cache Components model and requires `cacheComponents: true` for the cache APIs. Verify the app configuration before using them.
- Never cache a response containing a user's cookies, authorization context, balances, transactions, or other personalized financial data as shared content. Keep authenticated reads dynamic unless the exact cache boundary and invalidation contract are proven safe.
- A Server Action is a server endpoint. Authenticate, authorize, validate every argument, enforce idempotency for financial mutations, and return a typed result. Do not use `any` for action state or trust hidden form fields.
- Keep server-only secrets and gateway URLs out of client modules. Do not import server-only code across a client boundary.

### Web BFF

The repository's web flow is:

```text
Browser → /api/auth/* or /api/v1/* → Next.js BFF → gateway → backend services
          HttpOnly cookies             server-side Bearer header
```

- Use the existing `/api/v1` client and BFF routes. Browser code must not call the gateway directly or construct a Bearer token.
- Login, refresh, and logout use the existing `/api/auth/*` routes. Tokens stay in Secure, HttpOnly, SameSite cookies; the response body must not contain them.
- Preserve the BFF's gateway URL configuration, path-prefix allowlist, traversal checks, body-size limit, timeout, security headers, and SSRF tests when changing proxy behavior.
- For cookie-authenticated writes, follow the repository's CSRF/origin policy. SameSite is a control, not permission to skip server-side origin and authorization checks.
- `next/image` sources must use the configured `remotePatterns`; do not add a wildcard host for convenience.

### State and data fetching

- Use React Query for remote/server state and invalidation. Use Zustand for small, ephemeral client state such as display preferences or the current in-memory session profile.
- Do not persist auth tokens or financial/PII data in browser storage or a query persister. Persist only explicitly approved non-sensitive UI state.
- A mutation's cache update must preserve server truth. Prefer invalidation/refetch or the returned canonical resource over hand-written financial arithmetic.
- Keep URL state in the URL when it represents navigation, filtering, or shareable view state; do not duplicate it in multiple stores.

## Mobile architecture

- Follow the installed Expo SDK's documented APIs, not examples from a newer SDK. The current app uses `expo-secure-store`, `expo-local-authentication`, React Query, and AsyncStorage.
- Store native access/refresh tokens only in SecureStore. Tokens must never enter React Query cache, persisted query storage, logs, screenshots, or ordinary AsyncStorage.
- Use LocalAuthentication only as a local gate for an already-established session; biometric success is not backend authorization and must not replace server validation.
- AsyncStorage may hold approved non-secret preferences or cache metadata. Do not put raw credentials, recovery secrets, or unreviewed financial request payloads there.
- Offline financial actions need durable idempotency and reconciliation semantics. If the existing storage cannot safely guarantee them, keep the operation online or stop and document the blocker; do not silently queue a debit.
- Use platform-specific secure storage and network behavior behind the existing mobile utilities. Do not create a second auth or storage abstraction for one screen.

## Components, TypeScript, and design system

- Reuse the existing PayU tokens, primitives, Radix components, and utility functions before adding a component or dependency.
- Keep public component props and API responses explicitly typed. Use `unknown` at untrusted boundaries and narrow it; do not use `any` to suppress a type error.
- Prefer composition and small semantic components over a large prop matrix. Extract a component when it owns a real behavior or accessibility boundary, not merely to shorten a file.
- Use stable public package imports. Do not import from `dist`, private paths, or undocumented package internals for bundle-size gains.
- Keep loading, empty, error, unauthorized, pending, success, and retry states explicit. Financial confirmation must distinguish accepted, pending, failed, and unknown outcomes.
- Use the existing design language and responsive layout tokens. Do not impose universal radius, font-size, width, or color rules when content and accessibility require a different treatment.
- Keep all user-visible copy translatable through the existing i18n setup. Do not render unsanitized HTML or interpolate PII into analytics/events.
- Respect `prefers-reduced-motion` and provide a non-animated path for every meaningful interaction.

## Money-safe form boundary

For a monetary input, the minimum safe shape is a string boundary:

```ts
type TransferInput = {
  recipientAccount: string;
  amount: string; // canonical decimal text; never number
  description?: string;
};

function isDecimalAmount(value: unknown): value is string {
  return typeof value === 'string' && /^(?:0|[1-9]\d{0,15})(?:\.\d{1,4})?$/.test(value);
}
```

Use the repository's installed validator after resolving its exact Context7 documentation. Use `inputMode="decimal"` for a friendly keyboard, but do not use browser numeric coercion. Send the validated string unchanged and let the backend enforce account ownership, limits, scale, rounding, idempotency, and final balance.

## Accessibility and UX quality

- Target the repository's documented accessibility standard and verify it with real keyboard interaction, screen-reader semantics, and automated axe checks. Do not treat an axe pass as sufficient by itself.
- Every control needs an accessible name; every error needs an associated message; focus must move predictably after dialogs, navigation, and failed submissions.
- Use semantic HTML and native controls where they satisfy the interaction. Add ARIA only to express missing semantics, not to decorate invalid markup.
- Check contrast, reduced motion, zoom/reflow, touch targets, keyboard traps, and locale-dependent dates/numbers.

## Testing strategy

### Web

- Unit/component tests use the installed Vitest and Testing Library setup. Assert user-visible behavior with roles, labels, and text; avoid implementation state, class names, and private callbacks.
- Playwright uses the repository fixture/configuration. Reuse authenticated fixtures and isolated test data. Do not log in independently in every test or blanket-wait for `networkidle`; use web-first assertions and explicit readiness signals.
- Every BFF/auth change needs focused tests for cookie flags and token absence from JSON, unauthorized behavior, path allowlists/SSRF protection, refresh/logout, security headers, and error propagation.
- Financial flows need tests for idempotent replay, double submission, insufficient balance, ambiguous timeout, pending state, precision, and server-authoritative balance refresh.
- Use `@axe-core/playwright` or the existing accessibility test utilities for critical flows, then manually verify keyboard and focus behavior.

### Mobile

- Use Jest and React Native Testing Library for behavior and platform branches. Keep tokens out of test snapshots and persisted test fixtures.
- Use Maestro for smoke and critical end-to-end flows. Include login, biometric gate behavior, offline/online transitions, transfer confirmation, retry, and logout where the feature touches them.

### Test discipline

- Prefer real serialization, validation, BFF, and persistence boundaries over mocks that only prove a mock was called.
- Mock external systems only at a clear boundary and test their failure modes. Do not make coverage percentage the acceptance criterion; risk and invariant coverage are the criterion.
- For a bug, keep the regression test that failed before the fix. Run the narrow test first, then type-check/lint/build and the relevant suite.

## Performance and resilience

- Measure before optimizing. Track current Core Web Vitals, including INP (not the retired FID metric), LCP, CLS, route errors, and API latency.
- Use RSC, streaming/Suspense, route-level code splitting, and `next/image` when they fit the data boundary. Do not turn every component into a dynamic client bundle.
- Use public APIs and the installed bundler's documented optimization configuration. Avoid deep imports, premature `memo`/`useMemo`, and duplicated caches unless profiling shows a problem.
- Virtualize only genuinely large lists and preserve keyboard/focus semantics. Never mutate props or query-cache data in place; copy before sorting/filtering.
- Handle slow networks, retries, rate limits, cancellation, back navigation, duplicate taps, and stale data explicitly. A timeout does not prove a financial write failed.

## Review checklist

- [ ] Installed versions and Context7 queries are recorded for every library API used.
- [ ] Server/client boundary is minimal and no server secret crosses it.
- [ ] Money remains decimal text in the UI; no numeric coercion or optimistic balance arithmetic.
- [ ] Financial writes carry one idempotency key and have pending/unknown/reconciliation states.
- [ ] Web auth uses the BFF and HttpOnly cookies; mobile auth uses SecureStore; no token persistence leak exists.
- [ ] Authorization, origin/CSRF policy, SSRF allowlist, PII masking, and error redaction are preserved.
- [ ] Loading, empty, error, unauthorized, success, and retry states are accessible and localized.
- [ ] Tests cover real user behavior, critical financial invariants, security boundaries, and the relevant mobile/web path.
- [ ] Type-check, lint, focused tests, accessibility checks, and build evidence are attached to the handoff.

## Official documentation to resolve through Context7

Use Context7 before relying on any item below; links are starting points, not permission to assume an API is unchanged.

- Next.js App Router and Cache Components: https://nextjs.org/docs
- React `useActionState` and Server Components: https://react.dev/reference/react/useActionState
- Expo SecureStore and LocalAuthentication: https://docs.expo.dev/guides/authentication/
- Playwright locators, assertions, fixtures, and isolation: https://playwright.dev/docs
- Testing Library user-facing queries: https://testing-library.com/docs/queries/about/
