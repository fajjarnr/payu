/**
 * Color Contrast Accessibility Tests
 *
 * WCAG 2.1 Level AA Compliance:
 * - Normal text: 4.5:1 contrast ratio
 * - Large text (18pt+ or 14pt+ bold): 3:1 contrast ratio
 * - UI components and graphical objects: 3:1 contrast ratio
 *
 * @see https://www.w3.org/WAI/WCAG21/Understanding/contrast-minimum.html
 */
import { describe, it, expect } from 'vitest';
import { render } from '@testing-library/react';
import { axe } from 'jest-axe';
import { axeConfig } from './setup';

const TestComponents = {
  PrimaryButton: () => (
    <button className="bg-primary text-surface px-4 py-2 rounded">
      Primary Action
    </button>
  ),

  SecondaryButton: () => (
    <button className="bg-border text-text-primary px-4 py-2 rounded">
      Secondary Action
    </button>
  ),

  DarkModeText: () => (
    <div className="bg-text-primary text-surface p-4">
      <h1 className="text-2xl font-bold">Dashboard</h1>
      <p className="text-border">Welcome back to PayU</p>
    </div>
  ),

  ErrorMessage: () => (
    <div className="bg-error text-white p-3 rounded border border-error">
      Error: Invalid credentials
    </div>
  ),

  SuccessMessage: () => (
    <div className="bg-primary-light text-primary-dark p-3 rounded border border-primary-light">
      Success: Transaction completed
    </div>
  ),

  LinkText: () => (
    <a href="#" className="text-primary-dark hover:text-primary-dark underline">
      Learn more about PayU
    </a>
  ),

  // Lower contrast is acceptable for disabled elements
  DisabledButton: () => (
    <button disabled className="bg-border text-text-secondary px-4 py-2 rounded cursor-not-allowed">
      Disabled Action
    </button>
  ),

  // Lower contrast is acceptable for placeholders
  InputWithPlaceholder: () => (
    <input
      type="text"
      placeholder="Enter your name"
      className="border border-border px-3 py-2 rounded text-text-primary placeholder-text-disabled"
    />
  ),
};

describe('Color Contrast - WCAG 2.1 AA Compliance', () => {
  it('should have sufficient contrast on primary buttons', async () => {
    const { container } = render(<TestComponents.PrimaryButton />);
    const results = await axe(container, {
      rules: {
        ...axeConfig.rules,
        'color-contrast': { enabled: true },
      },
    });
    expect(results).toHaveNoViolations();
  });

  it('should have sufficient contrast on secondary buttons', async () => {
    const { container } = render(<TestComponents.SecondaryButton />);
    const results = await axe(container, {
      rules: {
        'color-contrast': { enabled: true },
      },
    });
    expect(results).toHaveNoViolations();
  });

  it('should have sufficient contrast in dark mode', async () => {
    const { container } = render(<TestComponents.DarkModeText />);
    const results = await axe(container, {
      rules: {
        'color-contrast': { enabled: true },
      },
    });
    expect(results).toHaveNoViolations();
  });

  it('should have sufficient contrast on error messages', async () => {
    const { container } = render(<TestComponents.ErrorMessage />);
    const results = await axe(container, {
      rules: {
        'color-contrast': { enabled: true },
      },
    });
    expect(results).toHaveNoViolations();
  });

  it('should have sufficient contrast on success messages', async () => {
    const { container } = render(<TestComponents.SuccessMessage />);
    const results = await axe(container, {
      rules: {
        'color-contrast': { enabled: true },
      },
    });
    expect(results).toHaveNoViolations();
  });

  it('should have sufficient contrast on links', async () => {
    const { container } = render(<TestComponents.LinkText />);
    const results = await axe(container, {
      rules: {
        'color-contrast': { enabled: true },
      },
    });
    expect(results).toHaveNoViolations();
  });

  it('should handle disabled button contrast (non-critical)', async () => {
    const { container } = render(<TestComponents.DisabledButton />);
    const results = await axe(container, {
      rules: {
        'color-contrast': { enabled: true },
      },
    });
    // Disabled elements may have lower contrast - acceptable per WCAG
    expect(results.violations.filter(v => v.id === 'color-contrast')).toHaveLength(0);
  });

  it('should have sufficient contrast on input placeholders', async () => {
    const { container } = render(<TestComponents.InputWithPlaceholder />);
    const results = await axe(container, {
      rules: {
        'color-contrast': { enabled: true },
      },
    });
    expect(results).toHaveNoViolations();
  });
});

describe('Color Contrast - Common UI Patterns', () => {
  it('should pass contrast checks on card components', async () => {
    const Card = () => (
      <div className="bg-surface dark:bg-text-primary rounded-lg shadow p-6">
        <h2 className="text-xl font-semibold text-text-primary dark:text-surface">
          Account Balance
        </h2>
        <p className="text-3xl font-bold text-primary-dark dark:text-primary mt-2">
          Rp 1.000.000
        </p>
        <p className="text-sm text-text-secondary dark:text-text-disabled mt-1">
          Last updated: Today
        </p>
      </div>
    );

    const { container } = render(<Card />);
    const results = await axe(container);
    expect(results).toHaveNoViolations();
  });

  it('should pass contrast checks on navigation', async () => {
    const Navigation = () => (
      <nav className="bg-text-primary text-surface px-4 py-3">
        <ul className="flex space-x-6">
          <li><span className="text-surface hover:text-primary cursor-pointer">Home</span></li>
          <li><span className="text-border hover:text-primary cursor-pointer">Transfer</span></li>
          <li><span className="text-border hover:text-primary cursor-pointer">History</span></li>
        </ul>
      </nav>
    );

    const { container } = render(<Navigation />);
    const results = await axe(container);
    expect(results).toHaveNoViolations();
  });

  it('should pass contrast checks on form elements', async () => {
    const Form = () => (
      <form className="space-y-4 bg-surface p-6 rounded-lg">
        <div>
          <label htmlFor="email" className="block text-sm font-medium text-text-primary">
            Email Address
          </label>
          <input
            id="email"
            type="email"
            className="mt-1 block w-full border border-border rounded-md shadow-sm text-text-primary"
          />
        </div>
        <div>
          <label htmlFor="amount" className="block text-sm font-medium text-text-primary">
            Amount
          </label>
          <input
            id="amount"
            type="number"
            className="mt-1 block w-full border border-border rounded-md shadow-sm text-text-primary"
          />
        </div>
        <button
          type="submit"
          className="w-full bg-primary-dark text-surface py-2 px-4 rounded-md hover:bg-primary-dark"
        >
          Submit
        </button>
      </form>
    );

    const { container } = render(<Form />);
    const results = await axe(container);
    expect(results).toHaveNoViolations();
  });
});
