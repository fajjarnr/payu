import { describe, it, expect, beforeEach, vi } from 'vitest';
import { renderHook, waitFor, act } from '@testing-library/react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { useAnalyticsWebSocket } from '@/hooks/useAnalytics';
import { useWebSocket } from '@/hooks/useWebSocket';

vi.mock('@/hooks/useWebSocket', () => ({
  useWebSocket: vi.fn()
}));

const mockUseWebSocket = useWebSocket as unknown as ReturnType<typeof vi.fn>;

describe('useAnalyticsWebSocket hook', () => {
  let queryClient: QueryClient;

  const wrapper = ({ children }: { children: React.ReactNode }) => (
    <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>
  );

  beforeEach(() => {
    queryClient = new QueryClient({
      defaultOptions: {
        queries: { retry: false },
        mutations: { retry: false }
      }
    });
    vi.clearAllMocks();
    vi.stubGlobal('fetch', vi.fn(async () => ({
      ok: true,
      json: async () => ({ ticket: 'tk-1', accountId: 'test-account-id' }),
    })) as unknown as typeof fetch);
  });

  it('should be defined', () => {
    expect(useAnalyticsWebSocket).toBeDefined();
  });

  it('should initialize with null analytics and disconnected state', () => {
    mockUseWebSocket.mockReturnValue({});

    const { result } = renderHook(() => useAnalyticsWebSocket('test-account-id'), {
      wrapper
    });

    expect(result.current.analytics).toBeNull();
    expect(result.current.isConnected).toBe(false);
  });

  it('should connect to WebSocket when accountId is provided', async () => {
    mockUseWebSocket.mockReturnValue({});

    const accountId = 'test-account-id';
    const expectedUrl = `wss://${window.location.host}/ws/analytics/${accountId}?ticket=tk-1`;

    renderHook(() => useAnalyticsWebSocket(accountId), { wrapper });

    await waitFor(() => expect(mockUseWebSocket).toHaveBeenCalledWith(expectedUrl, expect.objectContaining({
      enabled: true
    })));
  });

  it('should not connect to WebSocket when accountId is undefined', () => {
    mockUseWebSocket.mockReturnValue({});

    renderHook(() => useAnalyticsWebSocket(undefined), { wrapper });

    expect(mockUseWebSocket).toHaveBeenCalledWith(
      expect.any(String),
      expect.objectContaining({ enabled: false })
    );
  });

  it('should set isConnected to true when WebSocket opens', () => {
     
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    let capturedOptions: any = null;

    mockUseWebSocket.mockImplementation((_url, options) => {
      capturedOptions = options;
      return {};
    });

    const { result } = renderHook(() => useAnalyticsWebSocket('test-account-id'), {
      wrapper
    });

    expect(result.current.isConnected).toBe(false);

    act(() => { capturedOptions?.onOpen?.(new Event('open')); });

    expect(result.current.isConnected).toBe(true);
  });

  it('should set isConnected to false when WebSocket closes', () => {
     
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    let capturedOptions: any = null;

    mockUseWebSocket.mockImplementation((_url, options) => {
      capturedOptions = options;
      if (options?.onOpen) {
        setTimeout(() => options.onOpen!(new Event('open')), 0);
      }
      return {};
    });

    const { result } = renderHook(() => useAnalyticsWebSocket('test-account-id'), {
      wrapper
    });

    waitFor(() => {
      expect(result.current.isConnected).toBe(true);
    });

    act(() => { capturedOptions?.onClose?.(new CloseEvent('close')); });

    expect(result.current.isConnected).toBe(false);
  });

  it('should update analytics data when WALLET_BALANCE_CHANGED message is received', () => {
     
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    let capturedOptions: any = null;

    mockUseWebSocket.mockImplementation((_url, options) => {
      capturedOptions = options;
      return {};
    });

    const { result } = renderHook(() => useAnalyticsWebSocket('test-account-id'), {
      wrapper
    });

    const mockAnalyticsData = {
      totalIncome: 100000,
      totalExpenses: 50000,
      monthlySavings: 50000,
      investmentRoi: 10000,
      incomeChange: 10,
      expenseChange: -5,
      savingsChange: 15,
      roiChange: 8,
      spendingBreakdown: [
        { category: 'Food', amount: 20000, percentage: 40 },
        { category: 'Transport', amount: 10000, percentage: 20 }
      ]
    };

    act(() => {
      capturedOptions?.onMessage?.({ event_type: 'WALLET_BALANCE_CHANGED', data: mockAnalyticsData });
    });

    expect(result.current.analytics).toEqual(mockAnalyticsData);
  });

  it('should not update analytics for non-BALANCE_UPDATE messages', () => {
     
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    let capturedOptions: any = null;

    mockUseWebSocket.mockImplementation((_url, options) => {
      capturedOptions = options;
      return {};
    });

    const { result } = renderHook(() => useAnalyticsWebSocket('test-account-id'), {
      wrapper
    });

    const mockAnalyticsData = {
      totalIncome: 100000,
      totalExpenses: 50000,
      monthlySavings: 50000,
      investmentRoi: 10000,
      incomeChange: 10,
      expenseChange: -5,
      savingsChange: 15,
      roiChange: 8,
      spendingBreakdown: []
    };

    act(() => {
      capturedOptions?.onMessage?.({ event_type: 'OTHER_UPDATE', data: mockAnalyticsData });
    });

    expect(result.current.analytics).toBeNull();
  });

  it('should handle multiple message updates', () => {
     
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    let capturedOptions: any = null;

    mockUseWebSocket.mockImplementation((_url, options) => {
      capturedOptions = options;
      return {};
    });

    const { result } = renderHook(() => useAnalyticsWebSocket('test-account-id'), {
      wrapper
    });

    const firstUpdate = {
      totalIncome: 100000,
      totalExpenses: 50000,
      monthlySavings: 50000,
      investmentRoi: 10000,
      incomeChange: 10,
      expenseChange: -5,
      savingsChange: 15,
      roiChange: 8,
      spendingBreakdown: []
    };

    const secondUpdate = {
      totalIncome: 120000,
      totalExpenses: 60000,
      monthlySavings: 60000,
      investmentRoi: 12000,
      incomeChange: 20,
      expenseChange: 10,
      savingsChange: 20,
      roiChange: 20,
      spendingBreakdown: []
    };

    act(() => {
      capturedOptions?.onMessage?.({ event_type: 'WALLET_BALANCE_CHANGED', data: firstUpdate });
    });

    expect(result.current.analytics).toEqual(firstUpdate);

    act(() => {
      capturedOptions?.onMessage?.({ event_type: 'WALLET_BALANCE_CHANGED', data: secondUpdate });
    });

    expect(result.current.analytics).toEqual(secondUpdate);
  });

  it('should use custom WS_URL from environment when available', async () => {
    const originalWsUrl = process.env.NEXT_PUBLIC_WS_URL;
    process.env.NEXT_PUBLIC_WS_URL = 'wss://custom.example.com';

    mockUseWebSocket.mockReturnValue({});

    renderHook(() => useAnalyticsWebSocket('test-account-id'), { wrapper });

    await waitFor(() => expect(mockUseWebSocket).toHaveBeenCalledWith(
      'wss://custom.example.com/ws/analytics/test-account-id?ticket=tk-1',
      expect.any(Object)
    ));

    if (originalWsUrl === undefined) {
      delete process.env.NEXT_PUBLIC_WS_URL;
    } else {
      process.env.NEXT_PUBLIC_WS_URL = originalWsUrl;
    }
  });

  it('should clean up WebSocket on unmount', () => {
    const { unmount } = renderHook(() => useAnalyticsWebSocket('test-account-id'), {
      wrapper
    });

    expect(mockUseWebSocket).toHaveBeenCalled();

    unmount();

    // WebSocket cleanup is handled by the useWebSocket hook itself
    expect(mockUseWebSocket).toHaveBeenCalledTimes(1);
  });

  it('should handle connection errors gracefully', () => {
     
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    let capturedOptions: any = null;

    mockUseWebSocket.mockImplementation((_url, options) => {
      capturedOptions = options;
      return {};
    });

    const { result } = renderHook(() => useAnalyticsWebSocket('test-account-id'), {
      wrapper
    });

    capturedOptions?.onError?.(new Event('error'));

    expect(result.current).toBeDefined();
  });

  it('should update connection state correctly through lifecycle', () => {
     
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    let capturedOptions: any = null;

    mockUseWebSocket.mockImplementation((_url, options) => {
      capturedOptions = options;
      return {};
    });

    const { result } = renderHook(() => useAnalyticsWebSocket('test-account-id'), {
      wrapper
    });

    expect(result.current.isConnected).toBe(false);

    act(() => { capturedOptions?.onOpen?.(new Event('open')); });
    expect(result.current.isConnected).toBe(true);

    act(() => { capturedOptions?.onClose?.(new CloseEvent('close')); });
    expect(result.current.isConnected).toBe(false);
  });
});
