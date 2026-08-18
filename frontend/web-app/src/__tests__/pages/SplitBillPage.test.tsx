import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/react';
import SplitBillPage from '@/app/[locale]/split-bill/page';

vi.mock('@/components/DashboardLayout', () => ({
  default: ({ children }: { children: React.ReactNode }) => (
    <div data-testid="dashboard-layout">{children}</div>
  ),
}));

vi.mock('@/components/ui/Motion', () => ({
  PageTransition: ({ children }: { children: React.ReactNode }) => <div>{children}</div>,
  StaggerContainer: ({ children }: { children: React.ReactNode }) => <div>{children}</div>,
  StaggerItem: ({ children }: { children: React.ReactNode }) => <div>{children}</div>,
  ButtonMotion: ({ children }: { children: React.ReactNode }) => <div>{children}</div>,
}));

vi.mock('@/stores/authStore', () => ({
  useAuthStore: () => ({
    user: { id: 'user_1', username: 'budi' },
    accountId: 'acct_creator',
    isAuthenticated: true,
  }),
}));

const createSplitBillMock = vi.fn();

vi.mock('@/hooks/useSplitBill', () => ({
  useSplitBills: () => ({ data: [], isLoading: false }),
  useSplitBill: () => ({ data: null, isLoading: false }),
  useCreateSplitBill: () => ({ mutate: createSplitBillMock, isPending: false }),
  useAcceptSplitBill: () => ({ mutateAsync: vi.fn() }),
  useDeclineSplitBill: () => ({ mutateAsync: vi.fn() }),
  useSplitBillPayment: () => ({ mutateAsync: vi.fn() }),
  useSettleSplitBill: () => ({ mutateAsync: vi.fn() }),
  useAddParticipant: () => ({ mutateAsync: vi.fn() }),
  useActivateSplitBill: () => ({ mutateAsync: vi.fn() }),
}));

vi.mock('@/services/TransactionService', () => ({
  SplitBillParticipant: {},
}));

describe('SplitBillPage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('should render within DashboardLayout', () => {
    render(<SplitBillPage />);
    expect(screen.getByTestId('dashboard-layout')).toBeInTheDocument();
  });

  it('should render page title', () => {
    render(<SplitBillPage />);
    expect(screen.getByText('Split Bill')).toBeInTheDocument();
  });

  it('should render create split bill button', () => {
    render(<SplitBillPage />);
    expect(screen.getByText('Split Bill Baru')).toBeInTheDocument();
  });

  it('should submit a split bill with non-empty participants (FE-SPLIT-001)', () => {
    render(<SplitBillPage />);
    fireEvent.click(screen.getByText('Split Bill Baru'));

    const inputs = screen.getAllByPlaceholderText(/Account ID|No\. Rekening|Nama/);
    fireEvent.change(inputs[0], { target: { value: 'acct_p1' } });
    fireEvent.change(inputs[1], { target: { value: '1001001' } });
    fireEvent.change(inputs[2], { target: { value: 'Andi' } });
    fireEvent.click(screen.getByRole('button', { name: /Tambah Peserta/ }));
    const inputs2 = screen.getAllByPlaceholderText(/Account ID|No\. Rekening|Nama/);
    fireEvent.change(inputs2[3], { target: { value: 'acct_p2' } });
    fireEvent.change(inputs2[4], { target: { value: '1001002' } });
    fireEvent.change(inputs2[5], { target: { value: 'Budi' } });
    fireEvent.change(screen.getByPlaceholderText('Makan siang, nonton bareng...'), { target: { value: 'Makan siang' } });
    fireEvent.change(screen.getByPlaceholderText('150000'), { target: { value: '300000' } });

    fireEvent.click(screen.getByRole('button', { name: 'Buat' }));

    expect(createSplitBillMock).toHaveBeenCalledTimes(1);
    const request = createSplitBillMock.mock.calls[0][0];
    expect(request.title).toBe('Makan siang');
    expect(request.participants.length).toBe(2);
    expect(request.participants[0].accountId).toBe('acct_p1');
    expect(request.participants[0].accountNumber).toBe('1001001');
    expect(request.participants[0].accountName).toBe('Andi');
    expect(request.participants[0].amountOwed).toBe('150000');
    expect(request.participants[1].amountOwed).toBe('150000');
  });
});
