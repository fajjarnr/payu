import { act, renderHook } from '@testing-library/react-native';
import { useWalletStore } from '../walletStore';
import { walletService } from '@/services/wallet.service';
import { Wallet, Pocket } from '@/types';

// Mock dependencies
jest.mock('@/services/wallet.service');

describe('walletStore', () => {
  beforeEach(() => {
    jest.clearAllMocks();
    // Reset store state
    useWalletStore.setState({
      primaryWallet: null,
      pockets: [],
      balance: 0,
      isLoading: false,
      error: null,
    });
  });

  describe('initial state', () => {
    it('should have correct initial state', () => {
      const state = useWalletStore.getState();

      expect(state.primaryWallet).toBeNull();
      expect(state.pockets).toEqual([]);
      expect(state.balance).toBe(0);
      expect(state.isLoading).toBe(false);
      expect(state.error).toBeNull();
    });
  });

  describe('loadWallet', () => {
    const mockWallet: Wallet = {
      id: 'wallet-123',
      userId: 'user-123',
      balance: 1000000,
      currency: 'IDR',
      pocketType: 'primary',
      createdAt: '2024-01-01T00:00:00Z',
    };

    it('should set loading state when loading wallet', () => {
      (walletService.getPrimaryWallet as jest.Mock).mockImplementation(
        () => new Promise((resolve) => setTimeout(resolve, 100))
      );

      const { result } = renderHook(() => useWalletStore());

      act(() => {
        result.current.loadWallet();
      });

      expect(result.current.isLoading).toBe(true);
      expect(result.current.error).toBeNull();
    });

    it('should update state on successful wallet load', async () => {
      (walletService.getPrimaryWallet as jest.Mock).mockResolvedValue(mockWallet);

      const { result } = renderHook(() => useWalletStore());

      await act(async () => {
        await result.current.loadWallet();
      });

      expect(walletService.getPrimaryWallet).toHaveBeenCalled();
      expect(result.current.primaryWallet).toEqual(mockWallet);
      expect(result.current.balance).toBe(mockWallet.balance);
      expect(result.current.isLoading).toBe(false);
      expect(result.current.error).toBeNull();
    });

    it('should handle error when loading wallet fails', async () => {
      const errorMessage = 'Failed to fetch wallet';
      (walletService.getPrimaryWallet as jest.Mock).mockRejectedValue({
        response: { data: { message: errorMessage } },
      });

      const { result } = renderHook(() => useWalletStore());

      await act(async () => {
        await result.current.loadWallet();
      });

      expect(result.current.error).toBe(errorMessage);
      expect(result.current.isLoading).toBe(false);
      expect(result.current.primaryWallet).toBeNull();
    });

    it('should use default error message when response is undefined', async () => {
      (walletService.getPrimaryWallet as jest.Mock).mockRejectedValue(new Error('Network error'));

      const { result } = renderHook(() => useWalletStore());

      await act(async () => {
        await result.current.loadWallet();
      });

      expect(result.current.error).toBe('Failed to load wallet');
      expect(result.current.isLoading).toBe(false);
    });
  });

  describe('loadPockets', () => {
    const mockWallets: Wallet[] = [
      {
        id: 'wallet-primary',
        userId: 'user-123',
        balance: 1000000,
        currency: 'IDR',
        pocketType: 'primary',
        createdAt: '2024-01-01T00:00:00Z',
      },
      {
        id: 'wallet-savings-1',
        userId: 'user-123',
        balance: 500000,
        currency: 'IDR',
        pocketType: 'savings',
        createdAt: '2024-01-01T00:00:00Z',
      },
      {
        id: 'wallet-goals-1',
        userId: 'user-123',
        balance: 250000,
        currency: 'IDR',
        pocketType: 'goals',
        createdAt: '2024-01-01T00:00:00Z',
      },
    ];

    it('should set loading state when loading pockets', () => {
      (walletService.getWallets as jest.Mock).mockImplementation(
        () => new Promise((resolve) => setTimeout(resolve, 100))
      );

      const { result } = renderHook(() => useWalletStore());

      act(() => {
        result.current.loadPockets();
      });

      expect(result.current.isLoading).toBe(true);
    });

    it('should filter and transform wallets to pockets', async () => {
      (walletService.getWallets as jest.Mock).mockResolvedValue(mockWallets);

      const { result } = renderHook(() => useWalletStore());

      await act(async () => {
        await result.current.loadPockets();
      });

      expect(walletService.getWallets).toHaveBeenCalled();
      expect(result.current.pockets).toHaveLength(2);
      expect(result.current.pockets[0].type).toBe('savings');
      expect(result.current.pockets[1].type).toBe('goals');
      expect(result.current.pockets[0].id).toBe('wallet-savings-1');
      expect(result.current.isLoading).toBe(false);
    });

    it('should handle empty wallets array', async () => {
      (walletService.getWallets as jest.Mock).mockResolvedValue([]);

      const { result } = renderHook(() => useWalletStore());

      await act(async () => {
        await result.current.loadPockets();
      });

      expect(result.current.pockets).toEqual([]);
      expect(result.current.isLoading).toBe(false);
    });

    it('should handle error when loading pockets fails', async () => {
      const errorMessage = 'Failed to fetch pockets';
      (walletService.getWallets as jest.Mock).mockRejectedValue({
        response: { data: { message: errorMessage } },
      });

      const { result } = renderHook(() => useWalletStore());

      await act(async () => {
        await result.current.loadPockets();
      });

      expect(result.current.error).toBe(errorMessage);
      expect(result.current.pockets).toEqual([]);
    });
  });

  describe('createPocket', () => {
    const mockNewWallet: Wallet = {
      id: 'pocket-new',
      userId: 'user-123',
      balance: 100000,
      currency: 'IDR',
      pocketType: 'savings',
      createdAt: '2024-01-01T00:00:00Z',
    };

    const createPocketData = {
      name: 'Vacation Fund',
      type: 'savings' as const,
      initialBalance: 100000,
    };

    it('should set loading state when creating pocket', () => {
      (walletService.createPocket as jest.Mock).mockImplementation(
        () => new Promise((resolve) => setTimeout(resolve, 100))
      );

      const { result } = renderHook(() => useWalletStore());

      act(() => {
        result.current.createPocket(createPocketData);
      });

      expect(result.current.isLoading).toBe(true);
    });

    it('should add new pocket to state on success', async () => {
      (walletService.createPocket as jest.Mock).mockResolvedValue(mockNewWallet);

      const { result } = renderHook(() => useWalletStore());

      await act(async () => {
        await result.current.createPocket(createPocketData);
      });

      expect(walletService.createPocket).toHaveBeenCalledWith(createPocketData);
      expect(result.current.pockets).toHaveLength(1);
      expect(result.current.pockets[0].name).toBe(createPocketData.name);
      expect(result.current.pockets[0].balance).toBe(createPocketData.initialBalance);
      expect(result.current.pockets[0].type).toBe(createPocketData.type);
      expect(result.current.isLoading).toBe(false);
    });

    it('should handle zero initial balance', async () => {
      (walletService.createPocket as jest.Mock).mockResolvedValue({
        ...mockNewWallet,
        balance: 0,
      });

      const { result } = renderHook(() => useWalletStore());

      await act(async () => {
        await result.current.createPocket({
          name: 'Empty Pocket',
          type: 'goals',
        });
      });

      expect(result.current.pockets[0].balance).toBe(0);
    });

    it('should handle error when creating pocket fails', async () => {
      const errorMessage = 'Insufficient balance';
      (walletService.createPocket as jest.Mock).mockRejectedValue({
        response: { data: { message: errorMessage } },
      });

      const { result } = renderHook(() => useWalletStore());

      await expect(
        act(async () => {
          await result.current.createPocket(createPocketData);
        })
      ).rejects.toBeDefined();

      expect(result.current.error).toBe(errorMessage);
      expect(result.current.isLoading).toBe(false);
      expect(result.current.pockets).toHaveLength(0);
    });

    it('should append new pocket to existing pockets', async () => {
      const existingPocket: Pocket = {
        id: 'existing-pocket',
        name: 'Existing',
        balance: 50000,
        type: 'savings',
        color: '#10b981',
        icon: '💰',
      };

      useWalletStore.setState({ pockets: [existingPocket] });
      (walletService.createPocket as jest.Mock).mockResolvedValue(mockNewWallet);

      const { result } = renderHook(() => useWalletStore());

      await act(async () => {
        await result.current.createPocket(createPocketData);
      });

      expect(result.current.pockets).toHaveLength(2);
      expect(result.current.pockets[0].id).toBe('existing-pocket');
      expect(result.current.pockets[1].name).toBe('Vacation Fund');
    });
  });

  describe('transferToPocket', () => {
    const mockPrimaryWallet: Wallet = {
      id: 'wallet-primary',
      userId: 'user-123',
      balance: 1000000,
      currency: 'IDR',
      pocketType: 'primary',
      createdAt: '2024-01-01T00:00:00Z',
    };

    it('should set loading state when transferring', () => {
      (walletService.transferToPocket as jest.Mock).mockImplementation(
        () => new Promise((resolve) => setTimeout(resolve, 100))
      );

      const { result } = renderHook(() => useWalletStore());

      act(() => {
        result.current.transferToPocket('pocket-1', 'pocket-2', 50000, 'Test transfer');
      });

      expect(result.current.isLoading).toBe(true);
    });

    it('should call transfer service with correct parameters', async () => {
      (walletService.transferToPocket as jest.Mock).mockResolvedValue(undefined);
      (walletService.getPrimaryWallet as jest.Mock).mockResolvedValue(mockPrimaryWallet);
      (walletService.getWallets as jest.Mock).mockResolvedValue([]);

      const { result } = renderHook(() => useWalletStore());

      await act(async () => {
        await result.current.transferToPocket('pocket-1', 'pocket-2', 50000, 'Test transfer');
      });

      expect(walletService.transferToPocket).toHaveBeenCalledWith(
        'pocket-1',
        'pocket-2',
        50000,
        'Test transfer'
      );
    });

    it('should reload wallet and pockets after successful transfer', async () => {
      (walletService.transferToPocket as jest.Mock).mockResolvedValue(undefined);
      (walletService.getPrimaryWallet as jest.Mock).mockResolvedValue(mockPrimaryWallet);
      (walletService.getWallets as jest.Mock).mockResolvedValue([]);

      const { result } = renderHook(() => useWalletStore());

      await act(async () => {
        await result.current.transferToPocket('pocket-1', 'pocket-2', 50000);
      });

      expect(walletService.getPrimaryWallet).toHaveBeenCalled();
      expect(walletService.getWallets).toHaveBeenCalled();
    });

    it('should handle transfer without description', async () => {
      (walletService.transferToPocket as jest.Mock).mockResolvedValue(undefined);
      (walletService.getPrimaryWallet as jest.Mock).mockResolvedValue(mockPrimaryWallet);
      (walletService.getWallets as jest.Mock).mockResolvedValue([]);

      const { result } = renderHook(() => useWalletStore());

      await act(async () => {
        await result.current.transferToPocket('pocket-1', 'pocket-2', 50000);
      });

      expect(walletService.transferToPocket).toHaveBeenCalledWith(
        'pocket-1',
        'pocket-2',
        50000,
        undefined
      );
    });

    it('should handle error when transfer fails', async () => {
      const errorMessage = 'Insufficient balance';
      (walletService.transferToPocket as jest.Mock).mockRejectedValue({
        response: { data: { message: errorMessage } },
      });

      const { result } = renderHook(() => useWalletStore());

      await expect(
        act(async () => {
          await result.current.transferToPocket('pocket-1', 'pocket-2', 50000);
        })
      ).rejects.toBeDefined();

      expect(result.current.error).toBe(errorMessage);
      expect(result.current.isLoading).toBe(false);
    });
  });

  describe('clearError', () => {
    it('should clear error state', () => {
      useWalletStore.setState({ error: 'Some error' });

      const { result } = renderHook(() => useWalletStore());

      act(() => {
        result.current.clearError();
      });

      expect(result.current.error).toBeNull();
    });
  });
});
