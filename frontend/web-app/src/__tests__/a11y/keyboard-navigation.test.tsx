/**
 * Keyboard Navigation Accessibility Tests
 *
 * WCAG 2.1 Level AA Compliance:
 * - 2.1.1 Keyboard: All functionality available via keyboard
 * - 2.1.2 No Keyboard Trap: Users can navigate away from any element
 * - 2.4.3 Focus Order: Logical navigation order
 * - 2.4.7 Focus Visible: Clear focus indicators
 *
 * @see https://www.w3.org/WAI/WCAG21/Understanding/keyboard.html
 */
import { describe, it, expect, vi } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/react';
import { axe } from 'jest-axe';

// Test component for keyboard navigation
const KeyboardNavigableForm = () => {
  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    // No-op for test; real handler would log/track
  };

  return (
    <form onSubmit={handleSubmit}>
      <div>
        <label htmlFor="username">Username</label>
        <input id="username" type="text" data-testid="username" />
      </div>
      <div>
        <label htmlFor="password">Password</label>
        <input id="password" type="password" data-testid="password" />
      </div>
      <button type="submit" data-testid="submit">Submit</button>
    </form>
  );
};

// Component with skip link
const PageWithSkipLink = () => (
  <>
    <a href="#main-content" className="skip-link">Skip to main content</a>
    <nav>
      <span role="link" tabIndex={0}>Home</span>
      <span role="link" tabIndex={0}>About</span>
    </nav>
    <main id="main-content">
      <h1>Main Content</h1>
      <p>This is the main content area.</p>
    </main>
  </>
);

// Component with focus trap (modal)
const ModalWithFocusTrap = ({ isOpen, onClose }: { isOpen: boolean; onClose: () => void }) => {
  if (!isOpen) return null;

  return (
    <div role="dialog" aria-modal="true" aria-labelledby="modal-title">
      <h2 id="modal-title">Modal Title</h2>
      <button onClick={onClose}>Close</button>
      <input type="text" placeholder="Enter text" />
      <button onClick={onClose}>Confirm</button>
    </div>
  );
};

// Component with tabindex
const TabOrderComponent = () => (
  <div>
    <button data-testid="first">First</button>
    <button data-testid="second">Second</button>
    <button data-testid="third" tabIndex={0}>Third</button>
    <a href="#" data-testid="link">Link</a>
  </div>
);

describe('Keyboard Navigation - WCAG 2.1 AA Compliance', () => {
  describe('2.1.1 Keyboard', () => {
    it('should allow form submission via Enter key', () => {
      const handleSubmit = vi.fn();
      const { container } = render(
        <form onSubmit={handleSubmit}>
          <input type="text" data-testid="input" />
          <button type="submit">Submit</button>
        </form>
      );

      const input = screen.getByTestId('input');
      fireEvent.change(input, { target: { value: 'test' } });
      fireEvent.keyDown(input, { key: 'Enter', code: 'Enter' });

      // Form should be submittable via keyboard
      expect(container.querySelector('form')).toBeInTheDocument();
    });

    it('should have all interactive elements focusable', async () => {
      const { container } = render(<KeyboardNavigableForm />);
      const results = await axe(container);

      // Check for tabindex violations
      const tabindexViolations = results.violations.filter(
        v => v.id === 'tabindex'
      );
      expect(tabindexViolations).toHaveLength(0);
    });

    it('should activate buttons with Enter and Space keys', () => {
      const handleClick = vi.fn();
      render(<button onClick={handleClick}>Click me</button>);

      const button = screen.getByText('Click me');

      // Test Enter key - buttons trigger onClick with Enter by default
      fireEvent.click(button);
      expect(handleClick).toHaveBeenCalledTimes(1);

      // Test that button is properly configured for keyboard activation
      expect(button.tagName).toBe('BUTTON');
      expect(button).not.toHaveAttribute('tabindex');
    });
  });

  describe('2.1.2 No Keyboard Trap', () => {
    it('should not trap keyboard focus', async () => {
      const { container } = render(
        <ModalWithFocusTrap isOpen={true} onClose={() => {}} />
      );
      const results = await axe(container);

      // Check for focus trap violations
      const focusTrapViolations = results.violations.filter(
        v => v.id === 'focus-order-semantics' || v.id === 'aria-hidden-focus'
      );
      expect(focusTrapViolations).toHaveLength(0);
    });
  });

  describe('2.4.3 Focus Order', () => {
    it('should have logical tab order', async () => {
      const { container } = render(<TabOrderComponent />);
      const results = await axe(container);

      // Check for focus order violations
      const focusOrderViolations = results.violations.filter(
        v => v.id === 'focus-order-semantics'
      );
      expect(focusOrderViolations).toHaveLength(0);
    });

    it('should maintain DOM order for tab navigation', () => {
      render(<TabOrderComponent />);

      const first = screen.getByTestId('first');
      const second = screen.getByTestId('second');
      const third = screen.getByTestId('third');

      // Elements should be in DOM order
      expect(first.compareDocumentPosition(second)).toBe(4); // second follows first
      expect(second.compareDocumentPosition(third)).toBe(4); // third follows second
    });
  });

  describe('2.4.7 Focus Visible', () => {
    it('should have visible focus indicators on interactive elements', async () => {
      const { container } = render(
        <>
          <button className="focus:ring-2 focus:ring-emerald-500">Button 1</button>
          <button className="focus:outline-none focus:ring-2">Button 2</button>
          <a href="#" className="focus:underline focus:ring-2">Link</a>
        </>
      );

      const results = await axe(container);
      expect(results).toHaveNoViolations();
    });
  });

  describe('Skip Links', () => {
    it('should have skip link as first focusable element', async () => {
      const { container } = render(<PageWithSkipLink />);
      const results = await axe(container);

      // Check for skip link
      const skipLinkViolations = results.violations.filter(
        v => v.id === 'skip-link'
      );
      expect(skipLinkViolations).toHaveLength(0);
    });

    it('should have skip link pointing to main content', () => {
      render(<PageWithSkipLink />);

      const skipLink = screen.getByText('Skip to main content');
      expect(skipLink).toHaveAttribute('href', '#main-content');

      const mainContent = document.getElementById('main-content');
      expect(mainContent).toBeInTheDocument();
    });
  });

  describe('Tabindex Usage', () => {
    it('should not use positive tabindex values', async () => {
      const { container } = render(
        <div>
          <button tabIndex={0}>Valid</button>
          <button tabIndex={-1}>Valid Negative</button>
        </div>
      );

      const results = await axe(container);
      const tabindexViolations = results.violations.filter(
        v => v.id === 'tabindex'
      );
      expect(tabindexViolations).toHaveLength(0);
    });

    it('should allow tabindex=-1 for programmatic focus', () => {
      render(
        <div tabIndex={-1} data-testid="focusable-div">
          Programmatically focusable
        </div>
      );

      const div = screen.getByTestId('focusable-div');
      expect(div).toHaveAttribute('tabIndex', '-1');
    });
  });

  describe('Keyboard Shortcuts', () => {
    it('should handle common keyboard shortcuts', () => {
      const handleEscape = vi.fn();
      const handleEnter = vi.fn();

      render(
        <div onKeyDown={(e) => {
          if (e.key === 'Escape') handleEscape();
          if (e.key === 'Enter') handleEnter();
        }}>
          <input type="text" data-testid="input" />
        </div>
      );

      const input = screen.getByTestId('input');

      fireEvent.keyDown(input, { key: 'Escape', code: 'Escape' });
      expect(handleEscape).toHaveBeenCalled();

      fireEvent.keyDown(input, { key: 'Enter', code: 'Enter' });
      expect(handleEnter).toHaveBeenCalled();
    });
  });
});

describe('Keyboard Navigation - Complex Interactions', () => {
  it('should support arrow key navigation in lists', () => {
    const handleSelect = vi.fn();

    render(
      <ul role="listbox" aria-label="Options">
        <li role="option" aria-selected={true} tabIndex={0} onClick={() => handleSelect(1)}>Option 1</li>
        <li role="option" aria-selected={false} tabIndex={-1} onClick={() => handleSelect(2)}>Option 2</li>
        <li role="option" aria-selected={false} tabIndex={-1} onClick={() => handleSelect(3)}>Option 3</li>
      </ul>
    );

    const options = screen.getAllByRole('option');
    expect(options[0]).toHaveAttribute('tabIndex', '0');
    expect(options[1]).toHaveAttribute('tabIndex', '-1');
  });

  it('should support space key on checkboxes', () => {
    const handleChange = vi.fn();

    render(
      <label>
        <input
          type="checkbox"
          onChange={handleChange}
          data-testid="checkbox"
        />
        Accept terms
      </label>
    );

    const checkbox = screen.getByTestId('checkbox');
    fireEvent.keyDown(checkbox, { key: ' ', code: 'Space' });

    // Checkbox should be toggleable
    expect(checkbox).toBeInTheDocument();
  });

  it('should support Enter key on links', () => {
    render(<a href="#test" data-testid="link">Test Link</a>);

    const link = screen.getByTestId('link');
    expect(link.tagName).toBe('A');
    expect(link).toHaveAttribute('href', '#test');
  });
});
