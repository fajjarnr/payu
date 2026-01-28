import React from 'react';
import { renderWithIntl } from '@/__tests__/utils/test-utils';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { axe, toHaveNoViolations } from 'jest-axe';
import { Utensils, ShoppingCart } from 'lucide-react';
import SpendingInsights from '@/components/dashboard/SpendingInsights';

expect.extend(toHaveNoViolations);

describe('SpendingInsights', () => {
  const mockCategories = [
    {
      id: 'food',
      name: 'Makanan & Minuman',
      amount: 2500000,
      percentage: 35,
      trend: 'up' as const,
      trendValue: 12,
      color: 'bg-chart-1',
      icon: Utensils,
    },
    {
      id: 'shopping',
      name: 'Belanja',
      amount: 1800000,
      percentage: 25,
      trend: 'down' as const,
      trendValue: 8,
      color: 'bg-chart-2',
      icon: ShoppingCart,
    },
  ];

  it('should render spending insights correctly', () => {
    renderWithIntl(<SpendingInsights data={mockCategories} />);

    expect(screen.getByText('Wawasan Pengeluaran')).toBeInTheDocument();
    // Use getAllByText since the category name appears in both the list and summary
    expect(screen.getAllByText('Makanan & Minuman').length).toBeGreaterThan(0);
    expect(screen.getAllByText('Belanja').length).toBeGreaterThan(0);
  });

  it('should calculate total spending correctly', () => {
    renderWithIntl(<SpendingInsights data={mockCategories} />);

    // Total is 2500000 + 1800000 = 4300000
    expect(screen.getByText('Rp 4.300.000')).toBeInTheDocument();
  });

  it('should display highest category', () => {
    renderWithIntl(<SpendingInsights data={mockCategories} />);

    expect(screen.getByText('Kategori Terbesar')).toBeInTheDocument();
    expect(screen.getAllByText('Makanan & Minuman').length).toBeGreaterThan(0);
  });

  it('should expand category details on click', async () => {
    renderWithIntl(<SpendingInsights data={mockCategories} />);

    // Find the first category button by looking for button with the category text
    const categoryButtons = screen.getAllByRole('button');
    const foodCategoryButton = categoryButtons.find(btn =>
      btn.textContent?.includes('Makanan')
    );

    expect(foodCategoryButton).toBeInTheDocument();
    fireEvent.click(foodCategoryButton!);

    await waitFor(() => {
      expect(screen.getByText('Lihat Transaksi')).toBeInTheDocument();
      expect(screen.getByText('Set Anggaran')).toBeInTheDocument();
    });
  });

  it('should toggle view mode', () => {
    renderWithIntl(<SpendingInsights data={mockCategories} />);

    const categoryButton = screen.getByLabelText('Tampilan per kategori');
    const monthlyButton = screen.getByLabelText('Tampilan bulanan');

    expect(categoryButton).toHaveAttribute('aria-pressed', 'true');
    expect(monthlyButton).toHaveAttribute('aria-pressed', 'false');

    fireEvent.click(monthlyButton);

    expect(monthlyButton).toHaveAttribute('aria-pressed', 'true');
    expect(categoryButton).toHaveAttribute('aria-pressed', 'false');
  });

  it('should have no accessibility violations', async () => {
    const { container } = renderWithIntl(<SpendingInsights data={mockCategories} />);
    const results = await axe(container);

    expect(results).toHaveNoViolations();
  });

  it('should have proper ARIA attributes', () => {
    renderWithIntl(<SpendingInsights data={mockCategories} />);

    const region = screen.getByRole('region');
    expect(region).toHaveAttribute('aria-labelledby', 'spending-insights-title');

    const list = screen.getByRole('list');
    expect(list).toHaveAttribute('aria-label', 'Daftar kategori pengeluaran');
  });

  it('should have keyboard-navigable category items', () => {
    renderWithIntl(<SpendingInsights data={mockCategories} />);

    // Get category buttons by looking for buttons with category text
    const categoryButtons = screen.getAllByRole('button').filter((btn) =>
      btn.textContent?.includes('Makanan') || btn.textContent?.includes('Belanja')
    );

    expect(categoryButtons.length).toBeGreaterThan(0);

    categoryButtons.forEach((button) => {
      expect(button).toHaveAttribute('aria-expanded');
      expect(button).toHaveAttribute('aria-controls');
    });
  });

  it('should announce trend changes to screen readers', () => {
    renderWithIntl(<SpendingInsights data={mockCategories} />);

    const trendUp = screen.getByLabelText(/Tren naik 12%/);
    const trendDown = screen.getByLabelText(/Tren turun 8%/);

    expect(trendUp).toBeInTheDocument();
    expect(trendDown).toBeInTheDocument();
  });

  it('should have accessible progress bars', () => {
    const { container } = renderWithIntl(<SpendingInsights data={mockCategories} />);

    const progressBars = container.querySelectorAll('[role="progressbar"]');
    expect(progressBars.length).toBeGreaterThan(0);

    progressBars.forEach((bar) => {
      expect(bar).toHaveAttribute('aria-valuenow');
      expect(bar).toHaveAttribute('aria-valuemin', '0');
      expect(bar).toHaveAttribute('aria-valuemax', '100');
      expect(bar).toHaveAttribute('aria-label');
    });
  });
});
