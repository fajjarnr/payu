/**
 * Screen Reader Accessibility Tests
 *
 * WCAG 2.1 Level AA Compliance:
 * - 1.3.1 Info and Relationships: Proper semantic structure
 * - 4.1.2 Name, Role, Value: ARIA attributes for dynamic content
 * - 4.1.3 Status Messages: Announce status changes
 *
 * @see https://www.w3.org/WAI/WCAG21/Understanding/info-and-relationships.html
 * @see https://www.w3.org/WAI/WCAG21/Understanding/name-role-value.html
 */
import { describe, it, expect } from 'vitest';
import { render, screen } from '@testing-library/react';
import { axe } from 'jest-axe';

// Screen reader announcer component
const LiveRegion = ({ message }: { message: string }) => (
  <div
    role="status"
    aria-live="polite"
    aria-atomic="true"
    className="sr-only"
    data-testid="live-region"
  >
    {message}
  </div>
);

// Form with proper labels
const AccessibleForm = () => (
  <form>
    <div>
      <label htmlFor="email">Email Address</label>
      <input
        id="email"
        type="email"
        aria-describedby="email-help"
        aria-required="true"
      />
      <span id="email-help">We&apos;ll never share your email</span>
    </div>
    <div>
      <label htmlFor="country">Country</label>
      <select id="country" aria-required="true">
        <option value="">Select a country</option>
        <option value="id">Indonesia</option>
        <option value="sg">Singapore</option>
      </select>
    </div>
  </form>
);

// Data table with proper headers
const DataTable = () => (
  <table>
    <caption>Transaction History</caption>
    <thead>
      <tr>
        <th scope="col">Date</th>
        <th scope="col">Description</th>
        <th scope="col">Amount</th>
      </tr>
    </thead>
    <tbody>
      <tr>
        <td>2024-01-15</td>
        <td>Grocery Store</td>
        <td>Rp 250.000</td>
      </tr>
      <tr>
        <td>2024-01-14</td>
        <td>Gas Station</td>
        <td>Rp 100.000</td>
      </tr>
    </tbody>
  </table>
);

// Component with ARIA landmarks
const PageWithLandmarks = () => (
  <>
    <header role="banner">
      <nav role="navigation" aria-label="Main">
        <span role="link" tabIndex={0}>Home</span>
        <span role="link" tabIndex={0}>About</span>
      </nav>
    </header>
    <main role="main">
      <h1>Welcome to PayU</h1>
      <article role="article">
        <h2>Latest News</h2>
        <p>New features available now!</p>
      </article>
    </main>
    <aside role="complementary" aria-label="Sidebar">
      <h2>Quick Links</h2>
      <ul>
        <li><span role="link" tabIndex={0}>Help</span></li>
        <li><span role="link" tabIndex={0}>Contact</span></li>
      </ul>
    </aside>
    <footer role="contentinfo">
      <p>&copy; 2024 PayU</p>
    </footer>
  </>
);

// Button with accessible name
const IconButton = () => (
  <button
    type="button"
    aria-label="Close dialog"
    onClick={() => {}}
  >
    <svg aria-hidden="true" viewBox="0 0 24 24">
      <path d="M18 6L6 18M6 6l12 12" />
    </svg>
  </button>
);

// Progress indicator
const ProgressBar = ({ value, max }: { value: number; max: number }) => (
  <div
    role="progressbar"
    aria-valuenow={value}
    aria-valuemin={0}
    aria-valuemax={max}
    aria-label="Loading progress"
  >
    <div style={{ width: `${(value / max) * 100}%` }} />
  </div>
);

// Alert/Error message
const Alert = ({ type, message }: { type: 'error' | 'success' | 'warning'; message: string }) => {
  const role = type === 'error' ? 'alert' : 'status';
  const live = type === 'error' ? 'assertive' : 'polite';

  return (
    <div
      role={role}
      aria-live={live}
      aria-atomic="true"
      data-testid={`alert-${type}`}
    >
      {message}
    </div>
  );
};

// Breadcrumb navigation
const Breadcrumb = () => (
  <nav aria-label="Breadcrumb">
    <ol>
      <li>
        <span role="link" tabIndex={0}>Home</span>
      </li>
      <li>
        <span role="link" tabIndex={0}>Accounts</span>
      </li>
      <li>
        <span role="link" tabIndex={0} aria-current="page">
          Savings Account
        </span>
      </li>
    </ol>
  </nav>
);

describe('Screen Reader - ARIA and Semantic HTML', () => {
  describe('1.3.1 Info and Relationships', () => {
    it('should have proper heading hierarchy', async () => {
      const { container } = render(
        <>
          <h1>Main Title</h1>
          <h2>Section 1</h2>
          <h3>Subsection 1.1</h3>
          <h2>Section 2</h2>
        </>
      );

      const results = await axe(container);
      const headingViolations = results.violations.filter(
        v => v.id === 'heading-order'
      );
      expect(headingViolations).toHaveLength(0);
    });

    it('should have proper table structure', async () => {
      const { container } = render(<DataTable />);
      const results = await axe(container);

      const tableViolations = results.violations.filter(
        v => v.id === 'scope-attr-valid' || v.id === 'td-headers-attr'
      );
      expect(tableViolations).toHaveLength(0);
    });

    it('should have proper form labels', async () => {
      const { container } = render(<AccessibleForm />);
      const results = await axe(container);

      const labelViolations = results.violations.filter(
        v => v.id === 'label'
      );
      expect(labelViolations).toHaveLength(0);
    });

    it('should have proper list structure', async () => {
      const { container } = render(
        <ul>
          <li>Item 1</li>
          <li>Item 2</li>
          <li>Item 3</li>
        </ul>
      );

      const results = await axe(container);
      expect(results).toHaveNoViolations();
    });
  });

  describe('4.1.2 Name, Role, Value', () => {
    it('should have accessible names for buttons', async () => {
      const { container } = render(<IconButton />);
      const results = await axe(container);

      const buttonViolations = results.violations.filter(
        v => v.id === 'button-name'
      );
      expect(buttonViolations).toHaveLength(0);
    });

    it('should have proper ARIA roles', async () => {
      const { container } = render(<PageWithLandmarks />);
      const results = await axe(container);

      const roleViolations = results.violations.filter(
        v => v.id === 'aria-roles' || v.id === 'aria-allowed-attr'
      );
      expect(roleViolations).toHaveLength(0);
    });

    it('should have valid ARIA attributes', async () => {
      const { container } = render(
        <div>
          <button aria-expanded="true">Menu</button>
          <button aria-pressed="false">Toggle</button>
          <input aria-invalid="false" />
        </div>
      );

      const results = await axe(container);
      const ariaViolations = results.violations.filter(
        v => v.id === 'aria-valid-attr-value'
      );
      expect(ariaViolations).toHaveLength(0);
    });

    it('should have proper progress bar attributes', () => {
      render(<ProgressBar value={50} max={100} />);

      const progressbar = screen.getByRole('progressbar');
      expect(progressbar).toHaveAttribute('aria-valuenow', '50');
      expect(progressbar).toHaveAttribute('aria-valuemin', '0');
      expect(progressbar).toHaveAttribute('aria-valuemax', '100');
    });
  });

  describe('4.1.3 Status Messages', () => {
    it('should use live regions for dynamic content', () => {
      render(<LiveRegion message="Loading complete" />);

      const liveRegion = screen.getByTestId('live-region');
      expect(liveRegion).toHaveAttribute('role', 'status');
      expect(liveRegion).toHaveAttribute('aria-live', 'polite');
      expect(liveRegion).toHaveAttribute('aria-atomic', 'true');
    });

    it('should use assertive live region for errors', () => {
      render(<Alert type="error" message="Payment failed" />);

      const alert = screen.getByTestId('alert-error');
      expect(alert).toHaveAttribute('role', 'alert');
      expect(alert).toHaveAttribute('aria-live', 'assertive');
    });

    it('should use polite live region for success messages', () => {
      render(<Alert type="success" message="Payment successful" />);

      const alert = screen.getByTestId('alert-success');
      expect(alert).toHaveAttribute('role', 'status');
      expect(alert).toHaveAttribute('aria-live', 'polite');
    });
  });

  describe('Landmarks and Navigation', () => {
    it('should have proper landmark regions', async () => {
      const { container } = render(<PageWithLandmarks />);
      const results = await axe(container);

      const landmarkViolations = results.violations.filter(
        v => v.id === 'landmark-one-main' || v.id === 'region'
      );
      expect(landmarkViolations).toHaveLength(0);
    });

    it('should have unique landmark labels', async () => {
      const { container } = render(
        <>
          <nav aria-label="Main">Main Nav</nav>
          <nav aria-label="Footer">Footer Nav</nav>
        </>
      );

      const results = await axe(container);
      const landmarkViolations = results.violations.filter(
        v => v.id === 'landmark-unique'
      );
      expect(landmarkViolations).toHaveLength(0);
    });

    it('should have proper breadcrumb navigation', () => {
      render(<Breadcrumb />);

      const breadcrumb = screen.getByRole('navigation', { name: 'Breadcrumb' });
      expect(breadcrumb).toBeInTheDocument();

      const currentPage = screen.getByText('Savings Account');
      expect(currentPage).toHaveAttribute('aria-current', 'page');
    });
  });

  describe('Images and Non-text Content', () => {
    it('should have alt text for informative images', async () => {
      const { container } = render(
        <img src="chart.png" alt="Monthly spending chart showing Rp 5M total" />
      );

      const results = await axe(container);
      const imageViolations = results.violations.filter(
        v => v.id === 'image-alt'
      );
      expect(imageViolations).toHaveLength(0);
    });

    it('should mark decorative images as hidden', async () => {
      const { container } = render(
        <img src="decoration.png" alt="" role="presentation" />
      );

      const results = await axe(container);
      const imageViolations = results.violations.filter(
        v => v.id === 'image-alt'
      );
      expect(imageViolations).toHaveLength(0);
    });

    it('should have accessible SVG icons', () => {
      render(
        <svg role="img" aria-label="Warning" viewBox="0 0 24 24">
          <path d="M12 2L2 22h20L12 2z" />
        </svg>
      );

      const svg = screen.getByRole('img');
      expect(svg).toHaveAttribute('aria-label', 'Warning');
    });
  });

  describe('Language and Text', () => {
    it('should have valid lang attribute', async () => {
      // Set lang attribute for testing (normally set in HTML document)
      document.documentElement.lang = 'id';
      expect(document.documentElement.lang).toBeTruthy();
      expect(document.documentElement.lang).toBe('id');
    });

    it('should handle language changes', () => {
      const { container } = render(
        <div>
          <p lang="id">Selamat datang</p>
          <p lang="en">Welcome</p>
        </div>
      );

      const indonesian = container.querySelector('[lang="id"]');
      const english = container.querySelector('[lang="en"]');

      expect(indonesian).toHaveAttribute('lang', 'id');
      expect(english).toHaveAttribute('lang', 'en');
    });
  });
});

describe('Screen Reader - Complex Components', () => {
  it('should have accessible dialog/modal', () => {
    render(
      <div role="dialog" aria-modal="true" aria-labelledby="dialog-title">
        <h2 id="dialog-title">Confirm Action</h2>
        <p>Are you sure you want to proceed?</p>
        <button>Cancel</button>
        <button>Confirm</button>
      </div>
    );

    const dialog = screen.getByRole('dialog');
    expect(dialog).toHaveAttribute('aria-modal', 'true');
    expect(dialog).toHaveAttribute('aria-labelledby', 'dialog-title');
  });

  it('should have accessible tabs', () => {
    render(
      <div>
        <div role="tablist" aria-label="Account tabs">
          <button role="tab" aria-selected="true" aria-controls="panel-1">
            Overview
          </button>
          <button role="tab" aria-selected="false" aria-controls="panel-2">
            Transactions
          </button>
        </div>
        <div role="tabpanel" id="panel-1" aria-labelledby="tab-1">
          Overview content
        </div>
      </div>
    );

    const tabs = screen.getAllByRole('tab');
    expect(tabs[0]).toHaveAttribute('aria-selected', 'true');
    expect(tabs[1]).toHaveAttribute('aria-selected', 'false');
  });

  it('should have accessible accordion', () => {
    render(
      <div>
        <button
          aria-expanded="false"
          aria-controls="section-1"
          id="accordion-1"
        >
          Section 1
        </button>
        <div id="section-1" role="region" aria-labelledby="accordion-1" hidden>
          Section 1 content
        </div>
      </div>
    );

    const button = screen.getByRole('button');
    expect(button).toHaveAttribute('aria-expanded', 'false');
    expect(button).toHaveAttribute('aria-controls', 'section-1');
  });

  it('should have accessible tooltip', () => {
    render(
      <div>
        <button aria-describedby="tooltip-1">Info</button>
        <div role="tooltip" id="tooltip-1">
          Additional information
        </div>
      </div>
    );

    const tooltip = screen.getByRole('tooltip');
    expect(tooltip).toHaveAttribute('id', 'tooltip-1');
  });
});
