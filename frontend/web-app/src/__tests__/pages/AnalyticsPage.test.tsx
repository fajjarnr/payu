import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen } from '@testing-library/react';
import AnalyticsPage from '@/app/[locale]/analytics/page';

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
    user: { id: 'user_1' },
    isAuthenticated: true,
  }),
}));

vi.mock('@/hooks/useAnalytics', () => ({
  useAnalyticsWebSocket: () => ({
    isConnected: true,
    data: {
      totalIncome: 25000000,
      totalExpense: 15000000,
      monthlySavings: 10000000,
      investmentROI: 8.5,
      spendingByCategory: [],
      monthlyTrend: [],
    },
  }),
}));

// Mock recharts to avoid canvas rendering issues
vi.mock('recharts', () => ({
  BarChart: ({ children }: { children: React.ReactNode }) => <div data-testid="bar-chart">{children}</div>,
  Bar: () => <div />,
  PieChart: ({ children }: { children: React.ReactNode }) => <div data-testid="pie-chart">{children}</div>,
  Pie: () => <div />,
  Cell: () => <div />,
  XAxis: () => <div />,
  YAxis: () => <div />,
  CartesianGrid: () => <div />,
  Tooltip: () => <div />,
  Legend: () => <div />,
  ResponsiveContainer: ({ children }: { children: React.ReactNode }) => <div>{children}</div>,
}));

vi.mock('@/components/ui/chart', () => ({
  ChartContainer: ({ children }: { children: React.ReactNode }) => <div>{children}</div>,
  ChartTooltip: () => <div />,
  ChartTooltipContent: () => <div />,
}));

vi.mock('@/components/ui/card', () => ({
  Card: ({ children, ...props }: { children: React.ReactNode }) => <div {...props}>{children}</div>,
  CardContent: ({ children }: { children: React.ReactNode }) => <div>{children}</div>,
  CardHeader: ({ children }: { children: React.ReactNode }) => <div>{children}</div>,
  CardTitle: ({ children }: { children: React.ReactNode }) => <h3>{children}</h3>,
  CardDescription: ({ children }: { children: React.ReactNode }) => <p>{children}</p>,
}));

describe('AnalyticsPage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('should render within DashboardLayout', () => {
    render(<AnalyticsPage />);
    expect(screen.getByTestId('dashboard-layout')).toBeInTheDocument();
  });

  it('should render page title', () => {
    render(<AnalyticsPage />);
    expect(screen.getByText('Intelijen Keuangan')).toBeInTheDocument();
  });

  it('should render financial summary cards', () => {
    render(<AnalyticsPage />);
    expect(screen.getByText('Total Pemasukan')).toBeInTheDocument();
    expect(screen.getByText('Total Pengeluaran')).toBeInTheDocument();
    expect(screen.getByText('Tabungan Bulanan')).toBeInTheDocument();
    expect(screen.getByText('ROI Investasi')).toBeInTheDocument();
  });

  it('should render chart sections', () => {
    render(<AnalyticsPage />);
    expect(screen.getByText('Trajektori Pengeluaran')).toBeInTheDocument();
    expect(screen.getByText('Rincian Pengeluaran')).toBeInTheDocument();
  });

  it('should show live connection status', () => {
    render(<AnalyticsPage />);
    expect(screen.getByText('Live Update')).toBeInTheDocument();
  });
});
