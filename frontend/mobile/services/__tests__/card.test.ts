import { cardService } from '../card.service';
import { apiClient } from '../api';
import { VirtualCard, ApiResponse } from '@/types';

// Mock the apiClient
jest.mock('../api', () => ({
  apiClient: {
    get: jest.fn(),
    post: jest.fn(),
    put: jest.fn(),
    delete: jest.fn(),
  },
}));

describe('cardService', () => {
  const mockGet = apiClient.get as jest.MockedFunction<typeof apiClient.get>;
  const mockPost = apiClient.post as jest.MockedFunction<typeof apiClient.post>;
  const mockPut = apiClient.put as jest.MockedFunction<typeof apiClient.put>;
  const mockDelete = apiClient.delete as jest.MockedFunction<typeof apiClient.delete>;

  beforeEach(() => {
    jest.clearAllMocks();
  });

  describe('getCards', () => {
    const mockCards: VirtualCard[] = [
      {
        id: 'card-1',
        lastFour: '1234',
        cardHolder: 'John Doe',
        expiryDate: '12/27',
        cvv: '123',
        status: 'active',
        balance: 5000000,
        limit: 10000000,
        spendingLimit: 5000000,
        isPhysical: false,
        createdAt: '2024-01-01T00:00:00Z',
      },
      {
        id: 'card-2',
        lastFour: '5678',
        cardHolder: 'John Doe',
        expiryDate: '06/28',
        cvv: '456',
        status: 'frozen',
        balance: 2000000,
        limit: 5000000,
        spendingLimit: 2000000,
        isPhysical: false,
        createdAt: '2024-02-01T00:00:00Z',
      },
    ];

    it('should get all cards successfully', async () => {
      const apiResponse: ApiResponse<VirtualCard[]> = {
        success: true,
        data: mockCards,
        message: 'Cards retrieved successfully',
      };

      mockGet.mockResolvedValueOnce({ data: apiResponse });

      const result = await cardService.getCards();

      expect(mockGet).toHaveBeenCalledWith('/cards');
      expect(mockGet).toHaveBeenCalledTimes(1);
      expect(result).toEqual(mockCards);
    });

    it('should return empty array when no cards exist', async () => {
      const apiResponse: ApiResponse<VirtualCard[]> = {
        success: true,
        data: [],
        message: 'No cards found',
      };

      mockGet.mockResolvedValueOnce({ data: apiResponse });

      const result = await cardService.getCards();

      expect(result).toEqual([]);
    });

    it('should handle network errors', async () => {
      const error = new Error('Network Error');
      mockGet.mockRejectedValueOnce(error);

      await expect(cardService.getCards()).rejects.toThrow('Network Error');
    });

    it('should handle unauthorized access', async () => {
      const error = new Error('Unauthorized');
      (error as any).response = { status: 401 };
      mockGet.mockRejectedValueOnce(error);

      await expect(cardService.getCards()).rejects.toThrow('Unauthorized');
    });
  });

  describe('getCard', () => {
    const mockCard: VirtualCard = {
      id: 'card-1',
      lastFour: '1234',
      cardHolder: 'John Doe',
      expiryDate: '12/27',
      cvv: '123',
      status: 'active',
      balance: 5000000,
      limit: 10000000,
      spendingLimit: 5000000,
      isPhysical: false,
      createdAt: '2024-01-01T00:00:00Z',
    };

    it('should get a specific card successfully', async () => {
      const apiResponse: ApiResponse<VirtualCard> = {
        success: true,
        data: mockCard,
        message: 'Card retrieved successfully',
      };

      mockGet.mockResolvedValueOnce({ data: apiResponse });

      const result = await cardService.getCard('card-1');

      expect(mockGet).toHaveBeenCalledWith('/cards/card-1');
      expect(mockGet).toHaveBeenCalledTimes(1);
      expect(result).toEqual(mockCard);
    });

    it('should handle card not found', async () => {
      const error = new Error('Card not found');
      (error as any).response = { status: 404 };
      mockGet.mockRejectedValueOnce(error);

      await expect(cardService.getCard('invalid-id')).rejects.toThrow('Card not found');
    });

    it('should handle invalid card ID', async () => {
      const error = new Error('Invalid card ID');
      mockGet.mockRejectedValueOnce(error);

      await expect(cardService.getCard('')).rejects.toThrow('Invalid card ID');
    });
  });

  describe('createCard', () => {
    const mockCard: VirtualCard = {
      id: 'card-new',
      lastFour: '9999',
      cardHolder: 'John Doe',
      expiryDate: '12/29',
      cvv: '999',
      status: 'active',
      balance: 0,
      limit: 10000000,
      spendingLimit: 10000000,
      isPhysical: false,
      createdAt: '2024-01-15T00:00:00Z',
    };

    it('should create a new card successfully', async () => {
      const apiResponse: ApiResponse<VirtualCard> = {
        success: true,
        data: mockCard,
        message: 'Card created successfully',
      };

      mockPost.mockResolvedValueOnce({ data: apiResponse });

      const result = await cardService.createCard();

      expect(mockPost).toHaveBeenCalledWith('/cards');
      expect(mockPost).toHaveBeenCalledTimes(1);
      expect(result).toEqual(mockCard);
    });

    it('should handle maximum cards limit reached', async () => {
      const error = new Error('Maximum number of cards reached');
      mockPost.mockRejectedValueOnce(error);

      await expect(cardService.createCard()).rejects.toThrow('Maximum number of cards reached');
    });

    it('should handle card creation failure', async () => {
      const error = new Error('Failed to create card');
      mockPost.mockRejectedValueOnce(error);

      await expect(cardService.createCard()).rejects.toThrow('Failed to create card');
    });
  });

  describe('freezeCard', () => {
    it('should freeze a card successfully', async () => {
      mockPost.mockResolvedValueOnce({ data: {} });

      await cardService.freezeCard('card-1');

      expect(mockPost).toHaveBeenCalledWith('/cards/card-1/freeze');
      expect(mockPost).toHaveBeenCalledTimes(1);
    });

    it('should handle already frozen card', async () => {
      const error = new Error('Card is already frozen');
      mockPost.mockRejectedValueOnce(error);

      await expect(cardService.freezeCard('card-1')).rejects.toThrow('Card is already frozen');
    });

    it('should handle card not found when freezing', async () => {
      const error = new Error('Card not found');
      mockPost.mockRejectedValueOnce(error);

      await expect(cardService.freezeCard('invalid-id')).rejects.toThrow('Card not found');
    });

    it('should handle unauthorized freeze attempt', async () => {
      const error = new Error('Unauthorized');
      mockPost.mockRejectedValueOnce(error);

      await expect(cardService.freezeCard('card-1')).rejects.toThrow('Unauthorized');
    });
  });

  describe('unfreezeCard', () => {
    it('should unfreeze a card successfully', async () => {
      mockPost.mockResolvedValueOnce({ data: {} });

      await cardService.unfreezeCard('card-1');

      expect(mockPost).toHaveBeenCalledWith('/cards/card-1/unfreeze');
      expect(mockPost).toHaveBeenCalledTimes(1);
    });

    it('should handle already active card', async () => {
      const error = new Error('Card is already active');
      mockPost.mockRejectedValueOnce(error);

      await expect(cardService.unfreezeCard('card-1')).rejects.toThrow('Card is already active');
    });

    it('should handle cancelled card unfreeze attempt', async () => {
      const error = new Error('Cannot unfreeze a cancelled card');
      mockPost.mockRejectedValueOnce(error);

      await expect(cardService.unfreezeCard('card-cancelled')).rejects.toThrow('Cannot unfreeze a cancelled card');
    });
  });

  describe('setSpendingLimit', () => {
    const limit = 5000000;

    it('should set spending limit successfully', async () => {
      mockPut.mockResolvedValueOnce({ data: {} });

      await cardService.setSpendingLimit('card-1', limit);

      expect(mockPut).toHaveBeenCalledWith('/cards/card-1/limit', { limit });
      expect(mockPut).toHaveBeenCalledTimes(1);
    });

    it('should handle limit above maximum', async () => {
      const error = new Error('Limit exceeds maximum allowed');
      mockPut.mockRejectedValueOnce(error);

      await expect(cardService.setSpendingLimit('card-1', 999999999))
        .rejects.toThrow('Limit exceeds maximum allowed');
    });

    it('should handle negative limit', async () => {
      const error = new Error('Limit must be positive');
      mockPut.mockRejectedValueOnce(error);

      await expect(cardService.setSpendingLimit('card-1', -1000))
        .rejects.toThrow('Limit must be positive');
    });

    it('should handle card not found', async () => {
      const error = new Error('Card not found');
      mockPut.mockRejectedValueOnce(error);

      await expect(cardService.setSpendingLimit('invalid-id', limit))
        .rejects.toThrow('Card not found');
    });
  });

  describe('cancelCard', () => {
    it('should cancel a card successfully', async () => {
      mockDelete.mockResolvedValueOnce({ data: {} });

      await cardService.cancelCard('card-1');

      expect(mockDelete).toHaveBeenCalledWith('/cards/card-1');
      expect(mockDelete).toHaveBeenCalledTimes(1);
    });

    it('should handle already cancelled card', async () => {
      const error = new Error('Card is already cancelled');
      mockDelete.mockRejectedValueOnce(error);

      await expect(cardService.cancelCard('card-1')).rejects.toThrow('Card is already cancelled');
    });

    it('should handle card with pending transactions', async () => {
      const error = new Error('Cannot cancel card with pending transactions');
      mockDelete.mockRejectedValueOnce(error);

      await expect(cardService.cancelCard('card-1')).rejects.toThrow('Cannot cancel card with pending transactions');
    });

    it('should handle card not found when cancelling', async () => {
      const error = new Error('Card not found');
      mockDelete.mockRejectedValueOnce(error);

      await expect(cardService.cancelCard('invalid-id')).rejects.toThrow('Card not found');
    });
  });

  describe('getCardDetails', () => {
    const mockDetails = {
      cvv: '123',
      cardNumber: '4111111111111234',
      expiryDate: '12/27',
    };

    it('should get card details successfully', async () => {
      const apiResponse: ApiResponse<typeof mockDetails> = {
        success: true,
        data: mockDetails,
        message: 'Card details retrieved',
      };

      mockGet.mockResolvedValueOnce({ data: apiResponse });

      const result = await cardService.getCardDetails('card-1');

      expect(mockGet).toHaveBeenCalledWith('/cards/card-1/details');
      expect(mockGet).toHaveBeenCalledTimes(1);
      expect(result).toEqual(mockDetails);
    });

    it('should handle frozen card details request', async () => {
      const error = new Error('Cannot retrieve details for frozen card');
      mockGet.mockRejectedValueOnce(error);

      await expect(cardService.getCardDetails('card-frozen')).rejects.toThrow('Cannot retrieve details for frozen card');
    });

    it('should handle cancelled card details request', async () => {
      const error = new Error('Cannot retrieve details for cancelled card');
      mockGet.mockRejectedValueOnce(error);

      await expect(cardService.getCardDetails('card-cancelled')).rejects.toThrow('Cannot retrieve details for cancelled card');
    });

    it('should handle unauthorized access to card details', async () => {
      const error = new Error('Unauthorized');
      mockGet.mockRejectedValueOnce(error);

      await expect(cardService.getCardDetails('card-1')).rejects.toThrow('Unauthorized');
    });

    it('should handle card not found', async () => {
      const error = new Error('Card not found');
      mockGet.mockRejectedValueOnce(error);

      await expect(cardService.getCardDetails('invalid-id')).rejects.toThrow('Card not found');
    });
  });
});
