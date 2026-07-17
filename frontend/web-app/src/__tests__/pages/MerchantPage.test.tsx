import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import MerchantDashboard from '@/app/[locale]/merchant/page';
import { NextIntlClientProvider } from 'next-intl';
import messages from '../../../messages/id.json';

const renderMerchant = () => render(
  <NextIntlClientProvider locale="id" messages={messages}>
    <MerchantDashboard />
  </NextIntlClientProvider>
);

vi.mock('@/components/DashboardLayout', () => ({
  default: ({ children }: { children: React.ReactNode }) => <div>{children}</div>,
}));

vi.mock('@/stores/authStore', () => ({
  useAuthStore: () => ({ user: { id: '1' } }),
}));

vi.mock('next/link', () => ({
  default: ({ children, ...props }: { children: React.ReactNode; href: string }) => (
    <a {...props}>{children}</a>
  ),
}));

const mockGetProfile = vi.fn();

vi.mock('@/services/PartnerService', () => ({
  PartnerService: {
    getProfile: (...args: unknown[]) => mockGetProfile(...args),
  },
}));

describe('MerchantDashboard', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('should show loading state initially', () => {
    // Never resolve so we stay in loading state
    mockGetProfile.mockReturnValue(new Promise(() => {}));
    renderMerchant();
    expect(document.querySelector('.animate-spin')).toBeInTheDocument();
  });

  it('should render merchant portal when no partner found', async () => {
    mockGetProfile.mockRejectedValue(new Error('Not found'));
    renderMerchant();
    expect(await screen.findByText('Portal Merchant')).toBeInTheDocument();
  });

  it('should render register link when not registered', async () => {
    mockGetProfile.mockRejectedValue(new Error('Not found'));
    renderMerchant();
    expect(await screen.findByText('Daftar sebagai Merchant')).toBeInTheDocument();
  });

  it('should render merchant dashboard when partner exists', async () => {
    mockGetProfile.mockResolvedValue({
      id: 1,
      name: 'Test Merchant',
      email: 'test@merchant.com',
      type: 'INDIVIDUAL',
      active: true,
      clientId: 'test-client-id',
    });
    renderMerchant();
    expect(await screen.findByText('Dasbor Merchant')).toBeInTheDocument();
    await waitFor(() => {
      expect(screen.getByText('Test Merchant')).toBeInTheDocument();
    });
  });
});
