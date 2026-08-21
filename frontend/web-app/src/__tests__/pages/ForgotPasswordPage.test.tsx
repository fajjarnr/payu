import { toast } from 'sonner';
import { describe, it, vi, beforeEach } from 'vitest';
import { screen, fireEvent } from '@testing-library/react';
import ForgotPasswordPage from '@/app/[locale]/forgot-password/page';
import { renderWithIntl } from '@/__tests__/utils/test-utils';

vi.mock('sonner', () => ({
  toast: { error: vi.fn(), info: vi.fn(), success: vi.fn() },
}));

vi.mock('@/lib/navigation', () => ({
  Link: ({ children, ...props }: React.AnchorHTMLAttributes<HTMLAnchorElement> & { children: React.ReactNode }) => (
    <a {...props}>{children}</a>
  ),
}));

describe('ForgotPasswordPage', () => {

  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('should render the form heading and email input', () => {
    renderWithIntl(<ForgotPasswordPage />);
    expect(screen.getByRole('heading')).toBeInTheDocument();
    expect(screen.getByLabelText('Email')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Kirim Instruksi' })).toBeInTheDocument();
  });

  it('should warn when submitting without an email', () => {
    renderWithIntl(<ForgotPasswordPage />);
    fireEvent.click(screen.getByRole('button', { name: 'Kirim Instruksi' }));
    expect(toast.error).toHaveBeenCalled();
  });

  it('should show success when submitting with an email', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue({ ok: true, json: async () => ({}) }));
    renderWithIntl(<ForgotPasswordPage />);
    fireEvent.change(screen.getByLabelText('Email'), { target: { value: 'user@payu.id' } });
    fireEvent.click(screen.getByRole('button', { name: 'Kirim Instruksi' }));
    await vi.waitFor(() => {
      expect(toast.success).toHaveBeenCalledWith('Instruksi reset telah dikirim');
    });
    vi.unstubAllGlobals();
  });
});
