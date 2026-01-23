import { useWebSocket, useAnalyticsWebSocket } from '@/hooks';
import { renderHook, act } from '@testing-library/react';

jest.mock('@/stores', () => ({
  useAuthStore: () => ({
    token: 'test-token'
  })
}));

const mockWebSocketInstances: {
  url: string;
  readyState: number;
  onopen: ((event: Event) => void) | null;
  onmessage: ((event: MessageEvent) => void) | null;
  onerror: ((event: Event) => void) | null;
  onclose: ((event: CloseEvent) => void) | null;
  send(data: string): void;
  close(): void;
}[] = [];

class MockWebSocketClass {
  static CONNECTING = 0;
  static OPEN = 1;
  static CLOSING = 2;
  static CLOSED = 3;

  readyState = 0;
  url = '';
  onopen: ((event: Event) => void) | null = null;
  onmessage: ((event: MessageEvent) => void) | null = null;
  onerror: ((event: Event) => void) | null = null;
  onclose: ((event: CloseEvent) => void) | null = null;
  
  constructor(url: string) {
    this.url = url;
    mockWebSocketInstances.push(this);
    setTimeout(() => {
      this.readyState = 1;
      this.onopen?.(new Event('open'));
    }, 0);
  }

  send(data: string) {
    if (this.readyState === 1) {
      setTimeout(() => {
        this.onmessage?.(new MessageEvent('message', { data }));
      }, 0);
    }
  }

  close() {
    this.readyState = 3;
    this.onclose?.(new CloseEvent('close'));
    const index = mockWebSocketInstances.indexOf(this);
    if (index > -1) {
      mockWebSocketInstances.splice(index, 1);
    }
  }
}

global.WebSocket = MockWebSocketClass as unknown as typeof WebSocket;

describe('useWebSocket hook', () => {
  beforeEach(() => {
    mockWebSocketInstances.length = 0;
    jest.clearAllMocks();
  });

  test('hook should be defined', () => {
    expect(useWebSocket).toBeDefined();
  });

  test('hook should create WebSocket connection when enabled', () => {
    const onMessage = jest.fn();
    const { result } = renderHook(() => useWebSocket('ws://localhost:8080/test', { 
      onMessage,
      enabled: true 
    }));

    expect(result.current.ws).toBeDefined();
  });

  test('hook should not create WebSocket connection when disabled', () => {
    const { result } = renderHook(() => useWebSocket('ws://localhost:8080/test', { 
      enabled: false 
    }));

    expect(result.current.ws).toBeNull();
  });

  test('hook should handle incoming messages', () => {
    const onMessage = jest.fn();
    renderHook(() => useWebSocket('ws://localhost:8080/test', { onMessage }));

    act(() => {
      const ws = mockWebSocketInstances[0];
      const mockEvent = new MessageEvent('message', {
        data: JSON.stringify({ type: 'test', data: { value: 123 } })
      });
      ws.onmessage?.(mockEvent);
    });

    expect(onMessage).toHaveBeenCalledWith({ type: 'test', data: { value: 123 } });
  });

  test('hook should cleanup WebSocket on unmount', () => {
    const { unmount } = renderHook(() => useWebSocket('ws://localhost:8080/test'));

    expect(mockWebSocketInstances.length).toBe(1);

    unmount();

    expect(mockWebSocketInstances.length).toBe(0);
  });

  test('hook should call disconnect method', () => {
    const { result } = renderHook(() => useWebSocket('ws://localhost:8080/test'));

    expect(mockWebSocketInstances.length).toBe(1);

    act(() => {
      result.current.disconnect();
    });

    expect(mockWebSocketInstances.length).toBe(0);
  });
});

describe('useAnalyticsWebSocket hook', () => {
  beforeEach(() => {
    mockWebSocketInstances.length = 0;
    jest.clearAllMocks();
  });

  test('hook should be defined', () => {
    expect(useAnalyticsWebSocket).toBeDefined();
  });

  test('hook should return analytics data when connected', () => {
    const { result } = renderHook(() => useAnalyticsWebSocket('test-account-id'));

    act(() => {
      const ws = mockWebSocketInstances[0];
      const mockEvent = new MessageEvent('message', {
        data: JSON.stringify({
          type: 'BALANCE_UPDATE',
          data: {
            totalIncome: 100000,
            totalExpenses: 50000,
            monthlySavings: 50000,
            investmentRoi: 10000,
            incomeChange: 10,
            expenseChange: -5,
            savingsChange: 15,
            roiChange: 8,
            spendingBreakdown: []
          }
        })
      });
      ws.onmessage?.(mockEvent);
    });

    expect(result.current.analytics).toEqual({
      totalIncome: 100000,
      totalExpenses: 50000,
      monthlySavings: 50000,
      investmentRoi: 10000,
      incomeChange: 10,
      expenseChange: -5,
      savingsChange: 15,
      roiChange: 8,
      spendingBreakdown: []
    });
  });

  test('hook should initialize with default values', () => {
    const { result } = renderHook(() => useAnalyticsWebSocket('test-account-id'));

    expect(result.current.analytics).toBeNull();
    expect(mockWebSocketInstances.length).toBe(1);
  });

  test('hook should not connect without accountId', () => {
    const { result } = renderHook(() => useAnalyticsWebSocket(undefined));

    expect(result.current.isConnected).toBe(false);
    expect(result.current.analytics).toBeNull();
    expect(mockWebSocketInstances.length).toBe(0);
  });
});
