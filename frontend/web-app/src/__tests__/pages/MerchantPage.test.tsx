import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import MerchantDashboard from '@/app/[locale]/merchant/page';

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
    render(<MerchantDashboard />);
    expect(screen.getByText('Loading...')).toBeInTheDocument();
  });

  it('should render merchant portal when no partner found', async () => {
    mockGetProfile.mockRejectedValue(new Error('Not found'));
    render(<MerchantDashboard />);
    expect(await screen.findByText('Merchant Portal')).toBeInTheDocument();
  });

  it('should render register link when not registered', async () => {
    mockGetProfile.mockRejectedValue(new Error('Not found'));
    render(<MerchantDashboard />);
    expect(await screen.findByText('Register as Merchant')).toBeInTheDocument();
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
    render(<MerchantDashboard />);
    expect(await screen.findByText('Merchant Dashboard')).toBeInTheDocument();
    await waitFor(() => {
      expect(screen.getByText('Test Merchant')).toBeInTheDocument();
    });
  });
});
