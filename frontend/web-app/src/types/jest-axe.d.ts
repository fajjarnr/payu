declare module 'jest-axe' {
  import type { RawMatcherFn } from '@vitest/expect';

  export interface AxeViolation {
    id: string;
    impact?: string;
    [key: string]: unknown;
  }
  export interface AxeResults {
    violations: AxeViolation[];
  }

  export function axe(
    container: Element | Document | string,
    options?: Record<string, unknown>
  ): Promise<AxeResults>;

  export function configureAxe(
    options?: Record<string, unknown>
  ): typeof axe;

  export const toHaveNoViolations: {
    toHaveNoViolations: RawMatcherFn;
  };
}
