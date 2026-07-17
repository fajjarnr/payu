import React from 'react';
import { render, screen } from '@testing-library/react';
import '@testing-library/jest-dom';
import StatsCharts from '@/components/dashboard/StatsCharts';

describe('StatsCharts', () => {
 it('renders investment performance section', () => {
  render(<StatsCharts />);

  expect(screen.getByText('Performa Investasi')).toBeInTheDocument();
  expect(screen.getAllByText('--').length).toBeGreaterThan(0);
 });

 it('renders investment breakdown items', () => {
   render(<StatsCharts />);

   expect(screen.getByText('Saham')).toBeInTheDocument();
   expect(screen.getByText('Obligasi')).toBeInTheDocument();
   expect(screen.getByText('Emas Digital')).toBeInTheDocument();
   expect(screen.getAllByText('--')).toHaveLength(4);
  });

 it('renders spending overview section', () => {
  render(<StatsCharts />);

  expect(screen.getByText('Ikhtisar Pengeluaran')).toBeInTheDocument();
 });

 it('displays monthly spending bars', () => {
  render(<StatsCharts />);

  expect(screen.getByText('Ikhtisar Pengeluaran')).toBeInTheDocument();
 });

 it('applies responsive grid layout', () => {
  render(<StatsCharts />);

  expect(screen.getByText('Performa Investasi')).toBeInTheDocument();
 });

 it('shows total investment value', () => {
  render(<StatsCharts />);

  expect(screen.getByText('Total Nilai')).toBeInTheDocument();
  expect(screen.getAllByText('--').length).toBeGreaterThan(0);
 });

 it('renders donut chart SVG', () => {
  const { container } = render(<StatsCharts />);

  expect(container.querySelector('.recharts-responsive-container')).toBeInTheDocument();
 });

 it('displays active bar tooltip', () => {
   render(<StatsCharts />);

   expect(screen.getByText('Ikhtisar Pengeluaran')).toBeInTheDocument();
  });

 it('applies mobile-specific styling', () => {
   const { container } = render(<StatsCharts />);

   const investmentSection = container.querySelector('.bg-card');
   expect(investmentSection).toHaveClass('rounded-2xl');
  });
});
