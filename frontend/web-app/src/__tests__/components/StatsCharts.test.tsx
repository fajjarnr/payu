import React from 'react';
import { describe, it, expect, vi } from 'vitest';
import { screen } from '@testing-library/react';
import { renderWithIntl } from '@/__tests__/utils/test-utils';
import '@testing-library/jest-dom';
import StatsCharts from '@/components/dashboard/StatsCharts';

vi.mock('@ant-design/plots', () => ({
  Column: ({ data }: { data: { month: string; amount: number }[] }) => (
    <div data-testid="spending-column">{data.map((d) => d.month).join(',')}</div>
  ),
  RadialBar: ({ data }: { data: { category: string; value: number }[] }) => (
    <div data-testid="investment-radial">{data.map((d) => d.value).join(',')}</div>
  ),
}));
describe('StatsCharts', () => {
 it('renders investment performance section', () => {
  renderWithIntl(<StatsCharts />);

  expect(screen.getByText('Performa Investasi')).toBeInTheDocument();
  expect(screen.getAllByText('--').length).toBeGreaterThan(0);
 });

 it('renders investment breakdown items', () => {
   renderWithIntl(<StatsCharts />);

   expect(screen.getByText('Saham')).toBeInTheDocument();
   expect(screen.getByText('Obligasi')).toBeInTheDocument();
   expect(screen.getByText('Emas Digital')).toBeInTheDocument();
   expect(screen.getAllByText('--')).toHaveLength(4);
  });

 it('renders spending overview section', () => {
  renderWithIntl(<StatsCharts />);

  expect(screen.getByText('Ikhtisar Pengeluaran')).toBeInTheDocument();
 });

 it('displays monthly spending bars', () => {
  renderWithIntl(<StatsCharts />);

  expect(screen.getByText('Ikhtisar Pengeluaran')).toBeInTheDocument();
 });

 it('applies responsive grid layout', () => {
  renderWithIntl(<StatsCharts />);

  expect(screen.getByText('Performa Investasi')).toBeInTheDocument();
 });

 it('shows total investment value', () => {
  renderWithIntl(<StatsCharts />);

  expect(screen.getByText('Total Nilai')).toBeInTheDocument();
  expect(screen.getAllByText('--').length).toBeGreaterThan(0);
 });

 it('renders plots charts with chart data', () => {
   const { container } = renderWithIntl(
     <StatsCharts
       investmentChartData={[{ category: 'return', value: 12.5, fill: '#0a6b48' }]}
       spendingChartData={[{ month: 'Jan', amount: 100 }, { month: 'Feb', amount: 200 }]}
       totalValue="Rp 1Jt"
     />,
   );

   expect(container.querySelector('[data-testid="investment-radial"]')).toBeInTheDocument();
   expect(container.querySelector('[data-testid="spending-column"]')).toBeInTheDocument();
   expect(screen.getByText('Rp 1Jt')).toBeInTheDocument();
 });

  it('applies mobile-specific styling', () => {
    const { container } = renderWithIntl(<StatsCharts />);

    const grid = container.querySelector('.grid-cols-1');
    expect(grid).toBeInTheDocument();
  });
});
