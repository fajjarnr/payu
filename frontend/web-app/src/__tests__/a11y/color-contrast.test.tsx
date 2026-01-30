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

// Test components with various color schemes
const TestComponents = {
  // Primary button with bank-green background
  PrimaryButton: () => (
    <button className="bg-emerald-500 text-white px-4 py-2 rounded">
      Primary Action
    </button>
  ),

  // Secondary button with gray background
  SecondaryButton: () => (
    <button className="bg-gray-200 text-gray-800 px-4 py-2 rounded">
      Secondary Action
    </button>
  ),

  // Text on dark background (common in PayU dark mode)
  DarkModeText: () => (
    <div className="bg-gray-950 text-white p-4">
      <h1 className="text-2xl font-bold">Dashboard</h1>
      <p className="text-gray-300">Welcome back to PayU</p>
    </div>
  ),

  // Error message with red text
  ErrorMessage: () => (
    <div className="bg-red-50 text-red-700 p-3 rounded border border-red-200">
      Error: Invalid credentials
    </div>
  ),

  // Success message with green text
  SuccessMessage: () => (
    <div className="bg-emerald-50 text-emerald-700 p-3 rounded border border-emerald-200">
      Success: Transaction completed
    </div>
  ),

  // Link text
  LinkText: () => (
    <a href="#" className="text-emerald-600 hover:text-emerald-700 underline">
      Learn more about PayU
    </a>
  ),

  // Disabled button (lower contrast is acceptable for disabled elements)
  DisabledButton: () => (
    <button disabled className="bg-gray-300 text-gray-500 px-4 py-2 rounded cursor-not-allowed">
      Disabled Action
    </button>
  ),

  // Placeholder text (lower contrast is acceptable for placeholders)
  InputWithPlaceholder: () => (
    <input
      type="text"
      placeholder="Enter your name"
      className="border border-gray-300 px-3 py-2 rounded text-gray-900 placeholder-gray-400"
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
    // Disabled elements may have lower contrast - this is acceptable
    // We just check that the test runs without throwing
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
      <div className="bg-white dark:bg-gray-900 rounded-lg shadow p-6">
        <h2 className="text-xl font-semibold text-gray-900 dark:text-white">
          Account Balance
        </h2>
        <p className="text-3xl font-bold text-emerald-600 dark:text-emerald-400 mt-2">
          Rp 1.000.000
        </p>
        <p className="text-sm text-gray-500 dark:text-gray-400 mt-1">
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
      <nav className="bg-gray-950 text-white px-4 py-3">
        <ul className="flex space-x-6">
          <li><span className="text-white hover:text-emerald-400 cursor-pointer">Home</span></li>
          <li><span className="text-gray-300 hover:text-emerald-400 cursor-pointer">Transfer</span></li>
          <li><span className="text-gray-300 hover:text-emerald-400 cursor-pointer">History</span></li>
        </ul>
      </nav>
    );

    const { container } = render(<Navigation />);
    const results = await axe(container);
    expect(results).toHaveNoViolations();
  });

  it('should pass contrast checks on form elements', async () => {
    const Form = () => (
      <form className="space-y-4 bg-white p-6 rounded-lg">
        <div>
          <label htmlFor="email" className="block text-sm font-medium text-gray-700">
            Email Address
          </label>
          <input
            id="email"
            type="email"
            className="mt-1 block w-full border border-gray-300 rounded-md shadow-sm text-gray-900"
          />
        </div>
        <div>
          <label htmlFor="amount" className="block text-sm font-medium text-gray-700">
            Amount
          </label>
          <input
            id="amount"
            type="number"
            className="mt-1 block w-full border border-gray-300 rounded-md shadow-sm text-gray-900"
          />
        </div>
        <button
          type="submit"
          className="w-full bg-emerald-600 text-white py-2 px-4 rounded-md hover:bg-emerald-700"
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
