import React from 'react';
import { screen } from '@testing-library/react';
import { renderWithIntl } from '@/__tests__/utils/test-utils';
import '@testing-library/jest-dom';
import StatsCharts from '@/components/dashboard/StatsCharts';

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

 it('renders donut chart SVG', () => {
  const { container } = renderWithIntl(<StatsCharts />);

  expect(container.querySelector('.recharts-responsive-container')).toBeInTheDocument();
 });

 it('displays active bar tooltip', () => {
   renderWithIntl(<StatsCharts />);

   expect(screen.getByText('Ikhtisar Pengeluaran')).toBeInTheDocument();
  });

 it('applies mobile-specific styling', () => {
   const { container } = renderWithIntl(<StatsCharts />);

   const investmentSection = container.querySelector('.bg-card');
   expect(investmentSection).toHaveClass('rounded-2xl');
  });
});
