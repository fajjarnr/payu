import { walletService } from '../wallet.service';
import { apiClient } from '../api';
import { Wallet, ApiResponse } from '@/types';

// Mock the apiClient
jest.mock('../api', () => ({
  apiClient: {
    get: jest.fn(),
    post: jest.fn(),
  },
}));

describe('walletService', () => {
  const mockGet = apiClient.get as jest.MockedFunction<typeof apiClient.get>;
  const mockPost = apiClient.post as jest.MockedFunction<typeof apiClient.post>;

  beforeEach(() => {
    jest.clearAllMocks();
  });

  describe('getWallets', () => {
    const mockWallets: Wallet[] = [
      {
        id: 'wallet-1',
        userId: 'user-123',
        balance: 1000000,
        currency: 'IDR',
        pocketType: 'primary',
        createdAt: '2024-01-01T00:00:00Z',
      },
      {
        id: 'wallet-2',
        userId: 'user-123',
        balance: 500000,
        currency: 'IDR',
        pocketType: 'savings',
        createdAt: '2024-01-01T00:00:00Z',
      },
    ];

    it('should get all wallets successfully', async () => {
      const apiResponse: ApiResponse<Wallet[]> = {
        success: true,
        data: mockWallets,
        message: 'Wallets retrieved successfully',
      };

      mockGet.mockResolvedValueOnce({ data: apiResponse });

      const result = await walletService.getWallets();

      expect(mockGet).toHaveBeenCalledWith('/wallets');
      expect(mockGet).toHaveBeenCalledTimes(1);
      expect(result).toEqual(mockWallets);
    });

    it('should return empty array when no wallets exist', async () => {
      const apiResponse: ApiResponse<Wallet[]> = {
        success: true,
        data: [],
        message: 'No wallets found',
      };

      mockGet.mockResolvedValueOnce({ data: apiResponse });

      const result = await walletService.getWallets();

      expect(result).toEqual([]);
    });

    it('should handle network errors', async () => {
      const error = new Error('Network Error');
      mockGet.mockRejectedValueOnce(error);

      await expect(walletService.getWallets()).rejects.toThrow('Network Error');
    });

    it('should handle unauthorized access', async () => {
      const error = new Error('Unauthorized');
      (error as any).response = { status: 401 };
      mockGet.mockRejectedValueOnce(error);

      await expect(walletService.getWallets()).rejects.toThrow('Unauthorized');
    });
  });

  describe('getWallet', () => {
    const walletId = 'wallet-1';
    const mockWallet: Wallet = {
      id: 'wallet-1',
      userId: 'user-123',
      balance: 1000000,
      currency: 'IDR',
      pocketType: 'primary',
      createdAt: '2024-01-01T00:00:00Z',
    };

    it('should get a specific wallet successfully', async () => {
      const apiResponse: ApiResponse<Wallet> = {
        success: true,
        data: mockWallet,
        message: 'Wallet retrieved successfully',
      };

      mockGet.mockResolvedValueOnce({ data: apiResponse });

      const result = await walletService.getWallet(walletId);

      expect(mockGet).toHaveBeenCalledWith(`/wallets/${walletId}`);
      expect(mockGet).toHaveBeenCalledTimes(1);
      expect(result).toEqual(mockWallet);
    });

    it('should handle wallet not found error', async () => {
      const error = new Error('Wallet not found');
      (error as any).response = { status: 404 };
      mockGet.mockRejectedValueOnce(error);

      await expect(walletService.getWallet('invalid-id')).rejects.toThrow('Wallet not found');
    });

    it('should handle invalid wallet ID format', async () => {
      const error = new Error('Invalid wallet ID');
      mockGet.mockRejectedValueOnce(error);

      await expect(walletService.getWallet('')).rejects.toThrow('Invalid wallet ID');
    });
  });

  describe('getPrimaryWallet', () => {
    const mockWallet: Wallet = {
      id: 'wallet-primary',
      userId: 'user-123',
      balance: 2000000,
      currency: 'IDR',
      pocketType: 'primary',
      createdAt: '2024-01-01T00:00:00Z',
    };

    it('should get primary wallet successfully', async () => {
      const apiResponse: ApiResponse<Wallet> = {
        success: true,
        data: mockWallet,
        message: 'Primary wallet retrieved',
      };

      mockGet.mockResolvedValueOnce({ data: apiResponse });

      const result = await walletService.getPrimaryWallet();

      expect(mockGet).toHaveBeenCalledWith('/wallets/primary');
      expect(mockGet).toHaveBeenCalledTimes(1);
      expect(result).toEqual(mockWallet);
    });

    it('should handle when no primary wallet exists', async () => {
      const error = new Error('Primary wallet not found');
      (error as any).response = { status: 404 };
      mockGet.mockRejectedValueOnce(error);

      await expect(walletService.getPrimaryWallet()).rejects.toThrow('Primary wallet not found');
    });

    it('should handle server error', async () => {
      const error = new Error('Internal Server Error');
      (error as any).response = { status: 500 };
      mockGet.mockRejectedValueOnce(error);

      await expect(walletService.getPrimaryWallet()).rejects.toThrow('Internal Server Error');
    });
  });

  describe('createPocket', () => {
    const pocketData = {
      name: 'Vacation Fund',
      type: 'savings' as const,
      initialBalance: 100000,
    };

    const mockWallet: Wallet = {
      id: 'wallet-pocket-1',
      userId: 'user-123',
      balance: 100000,
      currency: 'IDR',
      pocketType: 'savings',
      createdAt: '2024-01-01T00:00:00Z',
    };

    it('should create a pocket successfully', async () => {
      const apiResponse: ApiResponse<Wallet> = {
        success: true,
        data: mockWallet,
        message: 'Pocket created successfully',
      };

      mockPost.mockResolvedValueOnce({ data: apiResponse });

      const result = await walletService.createPocket(pocketData);

      expect(mockPost).toHaveBeenCalledWith('/wallets/pockets', pocketData);
      expect(mockPost).toHaveBeenCalledTimes(1);
      expect(result).toEqual(mockWallet);
    });

    it('should create pocket without initial balance', async () => {
      const pocketDataNoBalance = {
        name: 'Emergency Fund',
        type: 'goals' as const,
      };

      const apiResponse: ApiResponse<Wallet> = {
        success: true,
        data: { ...mockWallet, balance: 0 },
        message: 'Pocket created successfully',
      };

      mockPost.mockResolvedValueOnce({ data: apiResponse });

      const result = await walletService.createPocket(pocketDataNoBalance);

      expect(mockPost).toHaveBeenCalledWith('/wallets/pockets', pocketDataNoBalance);
      expect(result.balance).toBe(0);
    });

    it('should handle duplicate pocket name error', async () => {
      const error = new Error('Pocket name already exists');
      mockPost.mockRejectedValueOnce(error);

      await expect(walletService.createPocket(pocketData)).rejects.toThrow('Pocket name already exists');
    });

    it('should handle invalid pocket type', async () => {
      const error = new Error('Invalid pocket type');
      mockPost.mockRejectedValueOnce(error);

      await expect(walletService.createPocket({
        name: 'Test',
        type: 'invalid' as any,
      })).rejects.toThrow('Invalid pocket type');
    });

    it('should handle insufficient balance for initial deposit', async () => {
      const error = new Error('Insufficient balance for initial deposit');
      mockPost.mockRejectedValueOnce(error);

      await expect(walletService.createPocket({
        name: 'Big Fund',
        type: 'savings',
        initialBalance: 999999999,
      })).rejects.toThrow('Insufficient balance for initial deposit');
    });
  });

  describe('transferToPocket', () => {
    const transferData = {
      fromPocketId: 'wallet-1',
      toPocketId: 'wallet-2',
      amount: 50000,
      description: 'Monthly savings',
    };

    it('should transfer between pockets successfully', async () => {
      mockPost.mockResolvedValueOnce({ data: {} });

      await walletService.transferToPocket(
        transferData.fromPocketId,
        transferData.toPocketId,
        transferData.amount,
        transferData.description
      );

      expect(mockPost).toHaveBeenCalledWith('/wallets/internal-transfer', {
        fromPocketId: transferData.fromPocketId,
        toPocketId: transferData.toPocketId,
        amount: transferData.amount,
        description: transferData.description,
      });
      expect(mockPost).toHaveBeenCalledTimes(1);
    });

    it('should transfer without description', async () => {
      mockPost.mockResolvedValueOnce({ data: {} });

      await walletService.transferToPocket(
        'wallet-1',
        'wallet-2',
        10000
      );

      expect(mockPost).toHaveBeenCalledWith('/wallets/internal-transfer', {
        fromPocketId: 'wallet-1',
        toPocketId: 'wallet-2',
        amount: 10000,
        description: undefined,
      });
    });

    it('should handle insufficient balance error', async () => {
      const error = new Error('Insufficient balance');
      mockPost.mockRejectedValueOnce(error);

      await expect(walletService.transferToPocket('wallet-1', 'wallet-2', 999999999))
        .rejects.toThrow('Insufficient balance');
    });

    it('should handle same pocket transfer error', async () => {
      const error = new Error('Cannot transfer to the same pocket');
      mockPost.mockRejectedValueOnce(error);

      await expect(walletService.transferToPocket('wallet-1', 'wallet-1', 10000))
        .rejects.toThrow('Cannot transfer to the same pocket');
    });

    it('should handle invalid pocket ID', async () => {
      const error = new Error('Pocket not found');
      mockPost.mockRejectedValueOnce(error);

      await expect(walletService.transferToPocket('invalid', 'wallet-2', 10000))
        .rejects.toThrow('Pocket not found');
    });

    it('should handle negative amount', async () => {
      const error = new Error('Amount must be positive');
      mockPost.mockRejectedValueOnce(error);

      await expect(walletService.transferToPocket('wallet-1', 'wallet-2', -1000))
        .rejects.toThrow('Amount must be positive');
    });
  });
});
