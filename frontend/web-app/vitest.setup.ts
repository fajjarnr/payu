import { expect, afterEach, vi } from 'vitest';
import { cleanup } from '@testing-library/react';
import * as matchers from '@testing-library/jest-dom/matchers';
import { toHaveNoViolations as jestAxeToHaveNoViolations } from 'jest-axe';

expect.extend(matchers);
expect.extend({ toHaveNoViolations: jestAxeToHaveNoViolations.toHaveNoViolations });

afterEach(() => {
  cleanup();
});

global.IntersectionObserver = vi.fn().mockImplementation(() => ({
  observe: vi.fn(),
  disconnect: vi.fn(),
  unobserve: vi.fn(),
})) as unknown as typeof IntersectionObserver;

// Global mock for next/navigation (required by next-intl in jsdom environment)
vi.mock('next/navigation', () => ({
  usePathname: () => '/',
  useRouter: () => ({
    push: vi.fn(),
    replace: vi.fn(),
    prefetch: vi.fn(),
    back: vi.fn(),
    forward: vi.fn(),
    refresh: vi.fn(),
  }),
  useSearchParams: () => new URLSearchParams(),
  useParams: () => ({}),
  useSelectedLayoutSegment: () => null,
  useSelectedLayoutSegments: () => [],
  useServerInsertedHTML: vi.fn(),
  redirect: vi.fn(),
  permanentRedirect: vi.fn(),
  notFound: vi.fn(),
  forbidden: vi.fn(),
  unauthorized: vi.fn(),
  ReadonlyURLSearchParams: URLSearchParams,
  RedirectType: { push: 'push', replace: 'replace' },
  ServerInsertedHTMLContext: { Provider: ({ children }: { children: React.ReactNode }) => children },
}));
