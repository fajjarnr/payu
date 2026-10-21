import { describe, it, expect, vi, beforeEach } from 'vitest';
import { renderWithIntl as render, screen, waitFor } from '@/__tests__/utils/test-utils';
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

const mockGetMyPartner = vi.fn();

vi.mock('@/services/PartnerService', () => ({
  PartnerService: {
    getMyPartner: (...args: unknown[]) => mockGetMyPartner(...args),
  },
}));

describe('MerchantDashboard', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('should show loading state initially', () => {
    // Never resolve so we stay in loading state: the portal content is absent
    // while the partner lookup is pending.
    const { promise } = Promise.withResolvers<void>();
    mockGetMyPartner.mockReturnValue(promise as Promise<never>);
    renderMerchant();
    expect(screen.queryByText(messages.merchant.title)).not.toBeInTheDocument();
  });

  it('should render merchant portal when no partner found', async () => {
    // A successful lookup that returns no partner is the empty state.
    mockGetMyPartner.mockResolvedValue(null);
    renderMerchant();
    expect(await screen.findByText('Portal Merchant')).toBeInTheDocument();
  });

  it('should render register link when not registered', async () => {
    mockGetMyPartner.mockResolvedValue(null);
    renderMerchant();
    expect(await screen.findByText('Daftar sebagai Merchant')).toBeInTheDocument();
  });

  it('R-06: a failed lookup shows an error with retry, not "not registered"', async () => {
    mockGetMyPartner.mockRejectedValue(new Error('boom'));
    renderMerchant();
    expect(await screen.findByText('Gagal memuat data merchant')).toBeInTheDocument();
    expect(screen.queryByText('Daftar sebagai Merchant')).not.toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Coba lagi' })).toBeInTheDocument();
  });

  it('should render merchant dashboard when partner exists', async () => {
    mockGetMyPartner.mockResolvedValue({
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
