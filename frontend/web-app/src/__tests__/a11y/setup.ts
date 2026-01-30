/**
 * Accessibility Test Setup
 *
 * Configures jest-axe for WCAG 2.1 AA compliance testing
 * @see https://github.com/nickcolley/jest-axe
 */
import { expect } from 'vitest';
import { toHaveNoViolations } from 'jest-axe';

// Extend Vitest's expect with jest-axe matchers
expect.extend(toHaveNoViolations);

// Export axe configuration for consistent testing
// See: https://github.com/dequelabs/axe-core/blob/develop/doc/rule-descriptions.md
export const axeConfig = {
  rules: {
    // WCAG 2.1 Level AA rules
    'color-contrast': { enabled: true },
    'image-alt': { enabled: true },
    'label': { enabled: true },
    'button-name': { enabled: true },
    'link-name': { enabled: true },
    'aria-allowed-attr': { enabled: true },
    'aria-required-attr': { enabled: true },
    'aria-required-children': { enabled: true },
    'aria-roles': { enabled: true },
    'aria-valid-attr-value': { enabled: true },
    'heading-order': { enabled: true },
    'html-has-lang': { enabled: true },
    'valid-lang': { enabled: true },
    'landmark-one-main': { enabled: true },
    'landmark-unique': { enabled: true },
    'region': { enabled: true },
    'skip-link': { enabled: true },
    'focus-order-semantics': { enabled: true },
    'tabindex': { enabled: true },
    'duplicate-id': { enabled: true },
    'aria-valid-attr': { enabled: true },
  },
};
