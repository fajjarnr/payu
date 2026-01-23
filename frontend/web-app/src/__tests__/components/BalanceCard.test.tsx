import React from 'react';
import { render, screen } from '@testing-library/react';
import '@testing-library/jest-dom';
import BalanceCard from '@/components/dashboard/BalanceCard';

describe('BalanceCard', () => {
 it('renders balance card with correct values', () => {
  render(<BalanceCard balance={1000000} percentage={45.2} />);
  
  expect(screen.getByText('Rp 1.000.000')).toBeInTheDocument();
  expect(screen.getByText('+45.2%')).toBeInTheDocument();
  expect(screen.getByText('Faktor Pertumbuhan')).toBeInTheDocument();
 });

 it('renders main wallet card visual representation', () => {
  render(<BalanceCard balance={5000000} />);
  
  expect(screen.getByText('PayU')).toBeInTheDocument();
  expect(screen.getByText('PENGGUNA PAYU')).toBeInTheDocument();
  expect(screen.getByText('2984 5678 9838 3723')).toBeInTheDocument();
 });

 it('renders summary stats correctly', () => {
  render(<BalanceCard balance={2000000} />);
  
  expect(screen.getByText('Total Pemasukan')).toBeInTheDocument();
  expect(screen.getByText('Total Pengeluaran')).toBeInTheDocument();
  expect(screen.getAllByText('Bulan Lalu')).toHaveLength(2);
 });

 it('applies responsive classes for mobile screens', () => {
  const { container } = render(<BalanceCard balance={1000000} />);
  
  const mainGrid = container.querySelector('.grid');
  expect(mainGrid).toHaveClass('grid-cols-1', 'md:grid-cols-12');
  
  const balanceSection = container.querySelector('.text-2xl');
  expect(balanceSection).toBeInTheDocument();
 });

 it('displays correct net worth calculation', () => {
  render(<BalanceCard balance={1000000} />);
  
  expect(screen.getByText('Rp 1.500.000')).toBeInTheDocument();
 });

 it('shows correct date display', () => {
  render(<BalanceCard balance={1000000} />);
  
  expect(screen.getByText('22 Jan 2026')).toBeInTheDocument();
 });
});
