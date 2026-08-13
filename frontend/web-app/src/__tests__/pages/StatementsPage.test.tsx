import { describe, it, vi, beforeEach } from 'vitest';
import { screen } from '@testing-library/react';
import StatementsPage from '@/app/[locale]/statements/page';
import { renderWithIntl } from '@/__tests__/utils/test-utils';

vi.mock('@/components/DashboardLayout', () => ({
  default: ({ children }: { children: React.ReactNode }) => (
    <div data-testid="dashboard-layout">{children}</div>
  ),
}));

vi.mock('@/components/ui/Motion', () => ({
  PageTransition: ({ children }: { children: React.ReactNode }) => <div>{children}</div>,
}));

vi.mock('@/components/settings/statement-downloader', () => ({
  default: () => <div data-testid="statement-downloader">Statement Downloader</div>,
}));

describe('StatementsPage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('should render within DashboardLayout', () => {
    renderWithIntl(<StatementsPage />);
    expect(screen.getByTestId('dashboard-layout')).toBeInTheDocument();
  });

  it('should render the statement generator title', () => {
    renderWithIntl(<StatementsPage />);
    expect(screen.getByText('Generator E-Statement')).toBeInTheDocument();
  });

  it('should render the statement downloader component', () => {
    renderWithIntl(<StatementsPage />);
    expect(screen.getByTestId('statement-downloader')).toBeInTheDocument();
  });
});
