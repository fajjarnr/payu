import { expect, afterEach, vi } from 'vitest';
import { cleanup } from '@testing-library/react';
import * as matchers from '@testing-library/jest-dom/matchers';
import { toHaveNoViolations as jestAxeToHaveNoViolations } from 'jest-axe';
import { act } from 'react-dom/test-utils';
import * as React from 'react';

// React 19.2+ does not export `act` from the top-level 'react' module (ESM).
// @testing-library/react 16.3.2 tries `React.act` first and falls back to
// `ReactDOMTestUtils.act` (deprecated). We polyfill `React.act` here so the
// testing library works without deprecation warnings in vitest/jsdom.
Object.assign(globalThis, {
  IS_REACT_ACT_ENVIRONMENT: true,
  React: { ...React, act },
});

expect.extend(matchers);
expect.extend({ toHaveNoViolations: jestAxeToHaveNoViolations.toHaveNoViolations });

afterEach(() => {
  cleanup();
});

class MockIntersectionObserver implements IntersectionObserver {
  readonly root = null;
  readonly rootMargin = '';
  readonly thresholds = [];
  disconnect = vi.fn();
  observe = vi.fn();
  takeRecords = vi.fn(() => []);
  unobserve = vi.fn();
}

class MockResizeObserver implements ResizeObserver {
  disconnect = vi.fn();
  observe = vi.fn();
  unobserve = vi.fn();
}

global.IntersectionObserver = MockIntersectionObserver;
global.ResizeObserver = MockResizeObserver;

Object.defineProperty(window, 'matchMedia', {
  configurable: true,
  value: vi.fn().mockImplementation((query: string) => ({
    matches: false,
    media: query,
    onchange: null,
    addListener: vi.fn(),
    removeListener: vi.fn(),
    addEventListener: vi.fn(),
    removeEventListener: vi.fn(),
    dispatchEvent: vi.fn(),
  })),
});

Object.defineProperty(HTMLMediaElement.prototype, 'play', {
  configurable: true,
  value: vi.fn().mockResolvedValue(undefined),
});

Object.defineProperty(HTMLCanvasElement.prototype, 'getContext', {
  configurable: true,
  value: vi.fn(() => ({ drawImage: vi.fn() })),
});

Object.defineProperty(HTMLCanvasElement.prototype, 'toDataURL', {
  configurable: true,
  value: vi.fn(() => 'data:image/png;base64,test'),
});

const nativeGetComputedStyle = window.getComputedStyle.bind(window);
window.getComputedStyle = (element: Element) => nativeGetComputedStyle(element);

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
