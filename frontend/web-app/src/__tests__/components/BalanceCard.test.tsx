import React from 'react';
import { render, screen } from '@testing-library/react';
import '@testing-library/jest-dom';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { vi } from 'vitest';
import BalanceCard from '@/components/dashboard/BalanceCard';

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

const createWrapper = () => {
  const queryClient = new QueryClient({
    defaultOptions: {
      queries: {
        retry: false,
      },
    },
  });

  const Wrapper = ({ children }: { children: React.ReactNode }) => (
    <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>
  );
  Wrapper.displayName = 'QueryClientWrapper';
  return Wrapper;
};

describe('BalanceCard', () => {
 it('renders balance card with correct values', () => {
   render(<BalanceCard balance={1000000} percentage={45.2} />, { wrapper: createWrapper() });

   expect(screen.getByText('Rp 1.000.000')).toBeInTheDocument();
   expect(screen.getByText('+45.2%')).toBeInTheDocument();
   expect(screen.getByText('Faktor Pertumbuhan')).toBeInTheDocument();
 });

 it('renders main wallet card visual representation', () => {
   render(<BalanceCard balance={5000000} />, { wrapper: createWrapper() });

   expect(screen.getByText('PayU')).toBeInTheDocument();
   expect(screen.getByText('PENGGUNA PAYU')).toBeInTheDocument();
   expect(screen.getByText(/•••/)).toBeInTheDocument();
 });

 it('renders summary stats correctly', () => {
   render(<BalanceCard balance={2000000} />, { wrapper: createWrapper() });

   expect(screen.getByText('Pemasukan')).toBeInTheDocument();
   expect(screen.getByText('Pengeluaran')).toBeInTheDocument();
   expect(screen.getAllByText('Bulan ini')).toHaveLength(2);
 });

 it('applies responsive classes for mobile screens', () => {
   const { container } = render(<BalanceCard balance={1000000} />, { wrapper: createWrapper() });

   const mainGrid = container.querySelector('.grid');
   expect(mainGrid).toHaveClass('grid-cols-1', 'md:grid-cols-12');

   const balanceSection = container.querySelector('.text-3xl');
   expect(balanceSection).toBeInTheDocument();
 });

 it('displays correct net worth calculation', () => {
   render(<BalanceCard balance={1000000} />, { wrapper: createWrapper() });

   expect(screen.getByText('Rp 1.500.000')).toBeInTheDocument();
 });

 it('shows correct date display', () => {
   const now = new Date();
   const expectedDate = now.toLocaleDateString('id-ID', { day: 'numeric', month: 'short', year: 'numeric' });

   render(<BalanceCard balance={1000000} />, { wrapper: createWrapper() });

   expect(screen.getByText(expectedDate)).toBeInTheDocument();
 });
});
