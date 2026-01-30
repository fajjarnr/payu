import { renderHook, waitFor, act } from '@testing-library/react-native';
import { useWallet } from '../useWallet';
import { useWalletStore } from '@/store/walletStore';

// Mock the wallet store
jest.mock('@/store/walletStore', () => ({
  useWalletStore: jest.fn(),
}));

describe('useWallet', () => {
  const mockLoadWallet = jest.fn();
  const mockLoadPockets = jest.fn();
  const mockCreatePocket = jest.fn();
  const mockTransferToPocket = jest.fn();
  const mockClearError = jest.fn();

  const mockPrimaryWallet = {
    id: 'wallet-123',
    userId: 'user-123',
    balance: 1000000,
    currency: 'IDR',
    pocketType: 'primary' as const,
    createdAt: '2024-01-01T00:00:00Z',
  };

  const mockPockets = [
    {
      id: 'pocket-1',
      name: 'Savings',
      balance: 500000,
      type: 'savings' as const,
      color: '#10b981',
      icon: '💰',
    },
    {
      id: 'pocket-2',
      name: 'Vacation',
      balance: 200000,
      type: 'goals' as const,
      color: '#10b981',
      icon: '💰',
    },
  ];

  beforeEach(() => {
    jest.clearAllMocks();
  });

  it('should return wallet state from store', () => {
    (useWalletStore as jest.Mock).mockReturnValue({
      primaryWallet: mockPrimaryWallet,
      pockets: mockPockets,
      balance: 1000000,
      isLoading: false,
      error: null,
      loadWallet: mockLoadWallet,
      loadPockets: mockLoadPockets,
      createPocket: mockCreatePocket,
      transferToPocket: mockTransferToPocket,
      clearError: mockClearError,
    });

    const { result } = renderHook(() => useWallet());

    expect(result.current.primaryWallet).toEqual(mockPrimaryWallet);
    expect(result.current.pockets).toEqual(mockPockets);
    expect(result.current.balance).toBe(1000000);
    expect(result.current.isLoading).toBe(false);
    expect(result.current.error).toBeNull();
  });

  it('should return loading state', () => {
    (useWalletStore as jest.Mock).mockReturnValue({
      primaryWallet: null,
      pockets: [],
      balance: 0,
      isLoading: true,
      error: null,
      loadWallet: mockLoadWallet,
      loadPockets: mockLoadPockets,
      createPocket: mockCreatePocket,
      transferToPocket: mockTransferToPocket,
      clearError: mockClearError,
    });

    const { result } = renderHook(() => useWallet());

    expect(result.current.isLoading).toBe(true);
    expect(result.current.primaryWallet).toBeNull();
  });

  it('should return error state', () => {
    const errorMessage = 'Failed to load wallet';
    (useWalletStore as jest.Mock).mockReturnValue({
      primaryWallet: null,
      pockets: [],
      balance: 0,
      isLoading: false,
      error: errorMessage,
      loadWallet: mockLoadWallet,
      loadPockets: mockLoadPockets,
      createPocket: mockCreatePocket,
      transferToPocket: mockTransferToPocket,
      clearError: mockClearError,
    });

    const { result } = renderHook(() => useWallet());

    expect(result.current.error).toBe(errorMessage);
  });

  it('should call loadWallet on mount', () => {
    (useWalletStore as jest.Mock).mockReturnValue({
      primaryWallet: null,
      pockets: [],
      balance: 0,
      isLoading: true,
      error: null,
      loadWallet: mockLoadWallet,
      loadPockets: mockLoadPockets,
      createPocket: mockCreatePocket,
      transferToPocket: mockTransferToPocket,
      clearError: mockClearError,
    });

    renderHook(() => useWallet());

    expect(mockLoadWallet).toHaveBeenCalled();
  });

  it('should call loadPockets on mount', () => {
    (useWalletStore as jest.Mock).mockReturnValue({
      primaryWallet: null,
      pockets: [],
      balance: 0,
      isLoading: true,
      error: null,
      loadWallet: mockLoadWallet,
      loadPockets: mockLoadPockets,
      createPocket: mockCreatePocket,
      transferToPocket: mockTransferToPocket,
      clearError: mockClearError,
    });

    renderHook(() => useWallet());

    expect(mockLoadPockets).toHaveBeenCalled();
  });

  it('should call loadWallet action', async () => {
    (useWalletStore as jest.Mock).mockReturnValue({
      primaryWallet: mockPrimaryWallet,
      pockets: mockPockets,
      balance: 1000000,
      isLoading: false,
      error: null,
      loadWallet: mockLoadWallet,
      loadPockets: mockLoadPockets,
      createPocket: mockCreatePocket,
      transferToPocket: mockTransferToPocket,
      clearError: mockClearError,
    });

    const { result } = renderHook(() => useWallet());

    await act(async () => {
      await result.current.loadWallet();
    });

    expect(mockLoadWallet).toHaveBeenCalled();
  });

  it('should call loadPockets action', async () => {
    (useWalletStore as jest.Mock).mockReturnValue({
      primaryWallet: mockPrimaryWallet,
      pockets: mockPockets,
      balance: 1000000,
      isLoading: false,
      error: null,
      loadWallet: mockLoadWallet,
      loadPockets: mockLoadPockets,
      createPocket: mockCreatePocket,
      transferToPocket: mockTransferToPocket,
      clearError: mockClearError,
    });

    const { result } = renderHook(() => useWallet());

    await act(async () => {
      await result.current.loadPockets();
    });

    expect(mockLoadPockets).toHaveBeenCalled();
  });

  it('should call createPocket action', async () => {
    const pocketData = {
      name: 'New Pocket',
      type: 'savings' as const,
      initialBalance: 100000,
    };

    (useWalletStore as jest.Mock).mockReturnValue({
      primaryWallet: mockPrimaryWallet,
      pockets: mockPockets,
      balance: 1000000,
      isLoading: false,
      error: null,
      loadWallet: mockLoadWallet,
      loadPockets: mockLoadPockets,
      createPocket: mockCreatePocket,
      transferToPocket: mockTransferToPocket,
      clearError: mockClearError,
    });

    const { result } = renderHook(() => useWallet());

    await act(async () => {
      await result.current.createPocket(pocketData);
    });

    expect(mockCreatePocket).toHaveBeenCalledWith(pocketData);
  });

  it('should call transferToPocket action', async () => {
    (useWalletStore as jest.Mock).mockReturnValue({
      primaryWallet: mockPrimaryWallet,
      pockets: mockPockets,
      balance: 1000000,
      isLoading: false,
      error: null,
      loadWallet: mockLoadWallet,
      loadPockets: mockLoadPockets,
      createPocket: mockCreatePocket,
      transferToPocket: mockTransferToPocket,
      clearError: mockClearError,
    });

    const { result } = renderHook(() => useWallet());

    await act(async () => {
      await result.current.transferToPocket('pocket-1', 'pocket-2', 50000, 'Test transfer');
    });

    expect(mockTransferToPocket).toHaveBeenCalledWith('pocket-1', 'pocket-2', 50000, 'Test transfer');
  });

  it('should call clearError action', () => {
    (useWalletStore as jest.Mock).mockReturnValue({
      primaryWallet: mockPrimaryWallet,
      pockets: mockPockets,
      balance: 1000000,
      isLoading: false,
      error: 'Some error',
      loadWallet: mockLoadWallet,
      loadPockets: mockLoadPockets,
      createPocket: mockCreatePocket,
      transferToPocket: mockTransferToPocket,
      clearError: mockClearError,
    });

    const { result } = renderHook(() => useWallet());

    act(() => {
      result.current.clearError();
    });

    expect(mockClearError).toHaveBeenCalled();
  });

  it('should handle empty pockets array', () => {
    (useWalletStore as jest.Mock).mockReturnValue({
      primaryWallet: mockPrimaryWallet,
      pockets: [],
      balance: 1000000,
      isLoading: false,
      error: null,
      loadWallet: mockLoadWallet,
      loadPockets: mockLoadPockets,
      createPocket: mockCreatePocket,
      transferToPocket: mockTransferToPocket,
      clearError: mockClearError,
    });

    const { result } = renderHook(() => useWallet());

    expect(result.current.pockets).toEqual([]);
    expect(result.current.pockets.length).toBe(0);
  });

  it('should handle zero balance', () => {
    (useWalletStore as jest.Mock).mockReturnValue({
      primaryWallet: { ...mockPrimaryWallet, balance: 0 },
      pockets: mockPockets,
      balance: 0,
      isLoading: false,
      error: null,
      loadWallet: mockLoadWallet,
      loadPockets: mockLoadPockets,
      createPocket: mockCreatePocket,
      transferToPocket: mockTransferToPocket,
      clearError: mockClearError,
    });

    const { result } = renderHook(() => useWallet());

    expect(result.current.balance).toBe(0);
    expect(result.current.primaryWallet?.balance).toBe(0);
  });

  it('should handle null primary wallet', () => {
    (useWalletStore as jest.Mock).mockReturnValue({
      primaryWallet: null,
      pockets: mockPockets,
      balance: 0,
      isLoading: false,
      error: null,
      loadWallet: mockLoadWallet,
      loadPockets: mockLoadPockets,
      createPocket: mockCreatePocket,
      transferToPocket: mockTransferToPocket,
      clearError: mockClearError,
    });

    const { result } = renderHook(() => useWallet());

    expect(result.current.primaryWallet).toBeNull();
    expect(result.current.balance).toBe(0);
  });
});
