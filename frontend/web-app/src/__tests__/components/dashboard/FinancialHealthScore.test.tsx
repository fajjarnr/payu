import React from 'react';
import { render, screen, waitFor } from '@testing-library/react';
import { axe, toHaveNoViolations } from 'jest-axe';
import FinancialHealthScore from '@/components/dashboard/FinancialHealthScore';
import { renderWithIntl } from '@/__tests__/utils/test-utils';

expect.extend(toHaveNoViolations);

describe('FinancialHealthScore', () => {
  it('should render the financial health score correctly', () => {
    renderWithIntl(<FinancialHealthScore score={75} />);

    expect(screen.getByText('75')).toBeInTheDocument();
    expect(screen.getByText('dari 100')).toBeInTheDocument();
    expect(screen.getByText('Skor Kesehatan Finansial')).toBeInTheDocument();
  });

  it('should display correct health level for excellent score', () => {
    renderWithIntl(<FinancialHealthScore score={90} />);

    expect(screen.getByText('Sangat Baik')).toBeInTheDocument();
  });

  it('should display correct health level for poor score', () => {
    renderWithIntl(<FinancialHealthScore score={25} />);

    expect(screen.getByText('Sangat Kurang')).toBeInTheDocument();
  });

  it('should show score change when previous score is provided', () => {
    renderWithIntl(<FinancialHealthScore score={80} previousScore={75} />);

    expect(screen.getByText(/\+5/)).toBeInTheDocument();
  });

  it('should show negative score change when score decreased', () => {
    renderWithIntl(<FinancialHealthScore score={70} previousScore={75} />);

    expect(screen.getByText(/-5/)).toBeInTheDocument();
  });

  it('should render score factors', () => {
    const { container } = renderWithIntl(<FinancialHealthScore score={75} />);

    expect(screen.getByText('Tabungan')).toBeInTheDocument();
    expect(screen.getByText('Investasi')).toBeInTheDocument();
    expect(screen.getByText('Pengeluaran')).toBeInTheDocument();
  });

  it('should have no accessibility violations', async () => {
    const { container } = renderWithIntl(<FinancialHealthScore score={75} />);
    const results = await axe(container);

    expect(results).toHaveNoViolations();
  });

  it('should have proper ARIA labels', () => {
    renderWithIntl(<FinancialHealthScore score={75} />);

    const region = screen.getByRole('region');
    expect(region).toHaveAttribute('aria-labelledby', 'financial-health-title');

    const title = screen.getByText('Skor Kesehatan Finansial');
    expect(title).toBeInTheDocument();
    expect(title).toHaveAttribute('id', 'financial-health-title');
  });

  it('should announce score change to screen readers', () => {
    renderWithIntl(<FinancialHealthScore score={80} previousScore={75} />);

    const changeIndicator = screen.getByLabelText(/Skor berubah meningkat 5 poin/);
    expect(changeIndicator).toBeInTheDocument();
  });

  it('should have keyboard-navigable progress bars', () => {
    const { container } = renderWithIntl(<FinancialHealthScore score={75} />);

    const progressBars = container.querySelectorAll('[role="progressbar"]');
    expect(progressBars.length).toBeGreaterThan(0);

    progressBars.forEach((bar) => {
      expect(bar).toHaveAttribute('aria-valuemin', '0');
      expect(bar).toHaveAttribute('aria-valuemax', '100');
      expect(bar).toHaveAttribute('aria-valuenow');
    });
  });
});
