import React from 'react';
import { renderWithIntl } from '@/__tests__/utils/test-utils';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { axe, toHaveNoViolations } from 'jest-axe';
import BudgetTracking from '@/components/dashboard/BudgetTracking';

expect.extend(toHaveNoViolations);

describe('BudgetTracking', () => {
  const mockBudgets = [
    {
      id: 'food',
      category: 'Makanan & Minuman',
      limit: 3000000,
      spent: 2500000,
      remaining: 500000,
      percentage: 83.33,
      status: 'warning' as const,
    },
    {
      id: 'entertainment',
      category: 'Hiburan',
      limit: 800000,
      spent: 900000,
      remaining: -100000,
      percentage: 112.5,
      status: 'exceeded' as const,
    },
  ];

  it('should render budget tracking correctly', () => {
    renderWithIntl(<BudgetTracking budgets={mockBudgets} />);

    expect(screen.getByText('Pelacakan Anggaran')).toBeInTheDocument();
    expect(screen.getByText('Makanan & Minuman')).toBeInTheDocument();
    expect(screen.getByText('Hiburan')).toBeInTheDocument();
  });

  it('should calculate total budget summary correctly', () => {
    renderWithIntl(<BudgetTracking budgets={mockBudgets} />);

    expect(screen.getByText(/Rp\s*3\.800\.000/)).toBeInTheDocument(); // Total budget
  });

  it('should show alert for exceeded budgets', () => {
    renderWithIntl(<BudgetTracking budgets={mockBudgets} />);

    expect(screen.getByText(/1 anggaran terlampaui/)).toBeInTheDocument();
  });

  it('should expand budget details on click', async () => {
    renderWithIntl(<BudgetTracking budgets={mockBudgets} />);

    const budgetButton = screen.getByText('Makanan & Minuman').closest('button');
    expect(budgetButton).toBeInTheDocument();

    fireEvent.click(budgetButton!);

    await waitFor(() => {
      expect(screen.getByText('Edit')).toBeInTheDocument();
      expect(screen.getByText('Hapus')).toBeInTheDocument();
    });
  });

  it('should display correct status colors', () => {
    const { container } = renderWithIntl(<BudgetTracking budgets={mockBudgets} />);

    // The exceeded budget should have the ring class on its container
    const exceededBudget = screen.getByText('Hiburan').closest('button');
    expect(exceededBudget?.parentElement).toHaveClass('ring-2', 'ring-destructive/20');
  });

  it('should show remaining budget correctly', () => {
    renderWithIntl(<BudgetTracking budgets={mockBudgets} />);

    expect(screen.getByText('Sisa')).toBeInTheDocument();
  });

  it('should have no accessibility violations', async () => {
    const { container } = renderWithIntl(<BudgetTracking budgets={mockBudgets} />);
    const results = await axe(container);

    expect(results).toHaveNoViolations();
  });

  it('should have proper ARIA attributes', () => {
    renderWithIntl(<BudgetTracking budgets={mockBudgets} />);

    const region = screen.getByRole('region');
    expect(region).toHaveAttribute('aria-labelledby', 'budget-tracking-title');
  });

  it('should have accessible budget items', () => {
    renderWithIntl(<BudgetTracking budgets={mockBudgets} />);

    const budgetButtons = screen.getAllByRole('button').filter((btn) =>
      btn.textContent?.includes('Makanan') || btn.textContent?.includes('Hiburan')
    );

    budgetButtons.forEach((button) => {
      expect(button).toHaveAttribute('aria-expanded');
      expect(button).toHaveAttribute('aria-controls');
    });
  });

  it('should announce budget status to screen readers', () => {
    renderWithIntl(<BudgetTracking budgets={mockBudgets} />);

    // Look for the progress bar specifically by role
    const progressBars = screen.getAllByRole('progressbar');
    const exceededProgressBar = progressBars.find(bar =>
      bar.getAttribute('aria-label')?.includes('112.5%')
    );
    expect(exceededProgressBar).toBeInTheDocument();
  });

  it('should have accessible progress bars', () => {
    const { container } = renderWithIntl(<BudgetTracking budgets={mockBudgets} />);

    const progressBars = container.querySelectorAll('[role="progressbar"]');
    expect(progressBars.length).toBeGreaterThan(0);

    progressBars.forEach((bar) => {
      expect(bar).toHaveAttribute('aria-valuenow');
      expect(bar).toHaveAttribute('aria-valuemin', '0');
      expect(bar).toHaveAttribute('aria-valuemax', '100');
      expect(bar).toHaveAttribute('aria-label');
    });
  });

  it('should have accessible action buttons', () => {
    renderWithIntl(<BudgetTracking budgets={mockBudgets} />);

    // Click to expand
    const budgetButton = screen.getByText('Makanan & Minuman').closest('button');
    fireEvent.click(budgetButton!);

    const editButton = screen.getByLabelText('Edit anggaran Makanan & Minuman');
    const deleteButton = screen.getByLabelText('Hapus anggaran Makanan & Minuman');

    expect(editButton).toBeInTheDocument();
    expect(deleteButton).toBeInTheDocument();
  });
});
