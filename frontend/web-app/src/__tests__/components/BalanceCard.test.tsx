import React from 'react';
import { screen } from '@testing-library/react';
import '@testing-library/jest-dom';
import { vi } from 'vitest';
import BalanceCard from '@/components/dashboard/BalanceCard';
import { renderWithIntl } from '@/__tests__/utils/test-utils';

// Mock the hooks
vi.mock('@/hooks/useUserSegment', () => ({
  useUserSegment: () => ({
    currentTier: undefined,
    isVIP: false,
    currentMembership: undefined,
    progressToNext: undefined,
    nextTier: undefined,
    totalScore: 0,
  }),
}));

// Mock zustand store properly
vi.mock('@/stores/authStore', () => ({
  useAuthStore: vi.fn((selector) => {
    const state = {
      user: { id: 'test-user', fullName: 'Test User' },
      token: null,
      refreshToken: null,
      accountId: null,
      isAuthenticated: false,
      setAuth: vi.fn(),
      setUser: vi.fn(),
      setToken: vi.fn(),
      logout: vi.fn(),
      clearAuth: vi.fn(),
    };
    return selector ? selector(state) : state;
  }),
}));

describe('BalanceCard', () => {
 it('renders balance card with correct values', () => {
   renderWithIntl(<BalanceCard balance={1000000} percentage={45.2} />);

   expect(screen.getByText('Rp 1.000.000')).toBeInTheDocument();
   expect(screen.getByText('+45.2%')).toBeInTheDocument();
   expect(screen.getByText('Faktor Pertumbuhan')).toBeInTheDocument();
 });

 it('renders main wallet card visual representation', () => {
   renderWithIntl(<BalanceCard balance={5000000} />);

   expect(screen.getByText('PayU')).toBeInTheDocument();
   expect(screen.getByText('PENGGUNA PAYU')).toBeInTheDocument();
   expect(screen.getByText(/•••/)).toBeInTheDocument();
 });

 it('renders summary stats correctly', () => {
   renderWithIntl(<BalanceCard balance={2000000} />);

   expect(screen.getByText('Pemasukan')).toBeInTheDocument();
   expect(screen.getByText('Pengeluaran')).toBeInTheDocument();
   expect(screen.getAllByText('Bulan ini')).toHaveLength(2);
 });

 it('applies responsive classes for mobile screens', () => {
   const { container } = renderWithIntl(<BalanceCard balance={1000000} />);

   const mainGrid = container.querySelector('.grid');
   expect(mainGrid).toHaveClass('grid-cols-1', 'lg:grid-cols-12');

   const balanceSection = container.querySelector('.text-2xl');
   expect(balanceSection).toBeInTheDocument();
 });

 it('does not invent net worth without portfolio data', () => {
   renderWithIntl(<BalanceCard balance={1000000} />);

   expect(screen.getByTestId('net-worth-card')).toHaveTextContent('Rp 0');
 });

 it('shows correct date display', () => {
   const now = new Date();
   const expectedDate = now.toLocaleDateString('id-ID', { day: 'numeric', month: 'short', year: 'numeric' });

   renderWithIntl(<BalanceCard balance={1000000} />);

   expect(screen.getByText(expectedDate)).toBeInTheDocument();
 });
});
