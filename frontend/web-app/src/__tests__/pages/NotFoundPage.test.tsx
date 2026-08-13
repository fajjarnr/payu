import { describe, it, vi } from 'vitest';
import { screen } from '@testing-library/react';
import NotFoundPage from '@/app/[locale]/not-found';
import { renderWithIntl } from '@/__tests__/utils/test-utils';

vi.mock('@/lib/navigation', () => ({
  Link: ({ children, ...props }: React.AnchorHTMLAttributes<HTMLAnchorElement> & { children: React.ReactNode }) => (
    <a {...props}>{children}</a>
  ),
}));

describe('NotFoundPage', () => {
  it('should render 404 and the not-found message', () => {
    renderWithIntl(<NotFoundPage />);
    expect(screen.getByText('404')).toBeInTheDocument();
    expect(screen.getByText('Halaman tidak ditemukan')).toBeInTheDocument();
  });

  it('should render a back-to-dashboard link', () => {
    renderWithIntl(<NotFoundPage />);
    expect(screen.getByRole('link', { name: 'Kembali ke Dasbor' })).toBeInTheDocument();
  });
});
