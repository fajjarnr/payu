import { act, renderHook, waitFor } from '@testing-library/react-native';
import AsyncStorage from '@react-native-async-storage/async-storage';
import { useOfflineMode } from '@/hooks/useOfflineMode';
import { transactionService } from '@/services/transaction.service';

jest.mock('@/services/transaction.service', () => ({
  transactionService: {
    transfer: jest.fn(),
    topUp: jest.fn(),
    payQRIS: jest.fn(),
  },
}));

jest.mock('@/utils/logger', () => ({
  Logger: {
    debug: jest.fn(),
    error: jest.fn(),
    info: jest.fn(),
    warn: jest.fn(),
  },
}));

describe('useOfflineMode', () => {
  it('keeps unsupported money operations queued instead of reporting success', async () => {
    await AsyncStorage.setItem(
      '@payu:offline_queue',
      JSON.stringify([
        {
          id: 'payment-1',
          type: 'payment',
          data: { amount: 100 },
          timestamp: Date.now(),
          idempotencyKey: 'payment::user-1::key-1',
          retryCount: 0,
          status: 'pending',
        },
      ])
    );

    const { result } = renderHook(() => useOfflineMode({ autoProcess: false }));

    await waitFor(() => expect(result.current.offlineQueue).toHaveLength(1));

    let processResult;
    await act(async () => {
      processResult = await result.current.processOfflineQueue();
    });

    expect(processResult).toEqual([
      expect.objectContaining({
        itemId: 'payment-1',
        success: false,
      }),
    ]);
    expect(result.current.offlineQueue[0]).toEqual(
      expect.objectContaining({
        id: 'payment-1',
        status: 'pending',
        retryCount: 1,
      })
    );
    expect(transactionService.transfer).not.toHaveBeenCalled();
    expect(transactionService.topUp).not.toHaveBeenCalled();
    expect(transactionService.payQRIS).not.toHaveBeenCalled();
  });
});
