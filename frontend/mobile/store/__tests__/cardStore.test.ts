import { act, renderHook } from '@testing-library/react-native';
import { useCardStore } from '../cardStore';
import { cardService } from '@/services/card.service';
import { VirtualCard } from '@/types';

// Mock dependencies
jest.mock('@/services/card.service');

describe('cardStore', () => {
  beforeEach(() => {
    jest.clearAllMocks();
    // Reset store state
    useCardStore.setState({
      cards: [],
      selectedCard: null,
      isLoading: false,
      error: null,
    });
  });

  describe('initial state', () => {
    it('should have correct initial state', () => {
      const state = useCardStore.getState();

      expect(state.cards).toEqual([]);
      expect(state.selectedCard).toBeNull();
      expect(state.isLoading).toBe(false);
      expect(state.error).toBeNull();
    });
  });

  describe('loadCards', () => {
    const mockCards: VirtualCard[] = [
      {
        id: 'card-1',
        lastFour: '1234',
        cardHolder: 'Test User',
        expiryDate: '12/25',
        cvv: '123',
        status: 'active',
        balance: 1000000,
        limit: 5000000,
        spendingLimit: 2000000,
        isPhysical: false,
        createdAt: '2024-01-01T00:00:00Z',
      },
      {
        id: 'card-2',
        lastFour: '5678',
        cardHolder: 'Test User',
        expiryDate: '06/26',
        cvv: '456',
        status: 'frozen',
        balance: 500000,
        limit: 3000000,
        spendingLimit: 1000000,
        isPhysical: true,
        createdAt: '2024-02-01T00:00:00Z',
      },
    ];

    it('should set loading state when loading cards', () => {
      (cardService.getCards as jest.Mock).mockImplementation(
        () => new Promise((resolve) => setTimeout(resolve, 100))
      );

      const { result } = renderHook(() => useCardStore());

      act(() => {
        result.current.loadCards();
      });

      expect(result.current.isLoading).toBe(true);
      expect(result.current.error).toBeNull();
    });

    it('should update state on successful cards load', async () => {
      (cardService.getCards as jest.Mock).mockResolvedValue(mockCards);

      const { result } = renderHook(() => useCardStore());

      await act(async () => {
        await result.current.loadCards();
      });

      expect(cardService.getCards).toHaveBeenCalled();
      expect(result.current.cards).toEqual(mockCards);
      expect(result.current.selectedCard).toEqual(mockCards[0]);
      expect(result.current.isLoading).toBe(false);
      expect(result.current.error).toBeNull();
    });

    it('should set selectedCard to null when no cards exist', async () => {
      (cardService.getCards as jest.Mock).mockResolvedValue([]);

      const { result } = renderHook(() => useCardStore());

      await act(async () => {
        await result.current.loadCards();
      });

      expect(result.current.cards).toEqual([]);
      expect(result.current.selectedCard).toBeNull();
    });

    it('should handle error when loading cards fails', async () => {
      const errorMessage = 'Failed to fetch cards';
      (cardService.getCards as jest.Mock).mockRejectedValue({
        response: { data: { message: errorMessage } },
      });

      const { result } = renderHook(() => useCardStore());

      await act(async () => {
        await result.current.loadCards();
      });

      expect(result.current.error).toBe(errorMessage);
      expect(result.current.isLoading).toBe(false);
      expect(result.current.cards).toEqual([]);
    });

    it('should use default error message when response is undefined', async () => {
      (cardService.getCards as jest.Mock).mockRejectedValue(new Error('Network error'));

      const { result } = renderHook(() => useCardStore());

      await act(async () => {
        await result.current.loadCards();
      });

      expect(result.current.error).toBe('Failed to load cards');
    });
  });

  describe('selectCard', () => {
    const mockCards: VirtualCard[] = [
      {
        id: 'card-1',
        lastFour: '1234',
        cardHolder: 'Test User',
        expiryDate: '12/25',
        cvv: '123',
        status: 'active',
        balance: 1000000,
        limit: 5000000,
        spendingLimit: 2000000,
        isPhysical: false,
        createdAt: '2024-01-01T00:00:00Z',
      },
      {
        id: 'card-2',
        lastFour: '5678',
        cardHolder: 'Test User',
        expiryDate: '06/26',
        cvv: '456',
        status: 'active',
        balance: 500000,
        limit: 3000000,
        spendingLimit: 1000000,
        isPhysical: true,
        createdAt: '2024-02-01T00:00:00Z',
      },
    ];

    it('should select card by id', () => {
      useCardStore.setState({ cards: mockCards });

      const { result } = renderHook(() => useCardStore());

      act(() => {
        result.current.selectCard('card-2');
      });

      expect(result.current.selectedCard).toEqual(mockCards[1]);
    });

    it('should set selectedCard to null if card not found', () => {
      useCardStore.setState({ cards: mockCards, selectedCard: mockCards[0] });

      const { result } = renderHook(() => useCardStore());

      act(() => {
        result.current.selectCard('non-existent');
      });

      expect(result.current.selectedCard).toBeNull();
    });

    it('should handle empty cards array', () => {
      const { result } = renderHook(() => useCardStore());

      act(() => {
        result.current.selectCard('card-1');
      });

      expect(result.current.selectedCard).toBeNull();
    });
  });

  describe('createCard', () => {
    const newCard: VirtualCard = {
      id: 'card-new',
      lastFour: '9999',
      cardHolder: 'Test User',
      expiryDate: '12/27',
      cvv: '999',
      status: 'active',
      balance: 0,
      limit: 10000000,
      spendingLimit: 5000000,
      isPhysical: false,
      createdAt: '2024-03-01T00:00:00Z',
    };

    it('should set loading state when creating card', () => {
      (cardService.createCard as jest.Mock).mockImplementation(
        () => new Promise((resolve) => setTimeout(resolve, 100))
      );

      const { result } = renderHook(() => useCardStore());

      act(() => {
        result.current.createCard();
      });

      expect(result.current.isLoading).toBe(true);
    });

    it('should add new card and select it on success', async () => {
      (cardService.createCard as jest.Mock).mockResolvedValue(newCard);

      const { result } = renderHook(() => useCardStore());

      await act(async () => {
        await result.current.createCard();
      });

      expect(cardService.createCard).toHaveBeenCalled();
      expect(result.current.cards).toHaveLength(1);
      expect(result.current.cards[0]).toEqual(newCard);
      expect(result.current.selectedCard).toEqual(newCard);
      expect(result.current.isLoading).toBe(false);
    });

    it('should append new card to existing cards', async () => {
      const existingCard: VirtualCard = {
        id: 'card-existing',
        lastFour: '1111',
        cardHolder: 'Test User',
        expiryDate: '01/25',
        cvv: '111',
        status: 'active',
        balance: 100000,
        limit: 1000000,
        spendingLimit: 500000,
        isPhysical: false,
        createdAt: '2024-01-01T00:00:00Z',
      };

      useCardStore.setState({ cards: [existingCard] });
      (cardService.createCard as jest.Mock).mockResolvedValue(newCard);

      const { result } = renderHook(() => useCardStore());

      await act(async () => {
        await result.current.createCard();
      });

      expect(result.current.cards).toHaveLength(2);
      expect(result.current.cards[0].id).toBe('card-existing');
      expect(result.current.cards[1].id).toBe('card-new');
    });

    it('should handle error when creating card fails', async () => {
      const errorMessage = 'Card limit reached';
      (cardService.createCard as jest.Mock).mockRejectedValue({
        response: { data: { message: errorMessage } },
      });

      const { result } = renderHook(() => useCardStore());

      await act(async () => {
        try {
          await result.current.createCard();
        } catch {
          // Expected to throw
        }
      });

      expect(result.current.error).toBe(errorMessage);
      expect(result.current.isLoading).toBe(false);
    });
  });

  describe('freezeCard', () => {
    const mockCards: VirtualCard[] = [
      {
        id: 'card-1',
        lastFour: '1234',
        cardHolder: 'Test User',
        expiryDate: '12/25',
        cvv: '123',
        status: 'active',
        balance: 1000000,
        limit: 5000000,
        spendingLimit: 2000000,
        isPhysical: false,
        createdAt: '2024-01-01T00:00:00Z',
      },
    ];

    it('should set loading state when freezing card', () => {
      (cardService.freezeCard as jest.Mock).mockImplementation(
        () => new Promise((resolve) => setTimeout(resolve, 100))
      );

      const { result } = renderHook(() => useCardStore());

      act(() => {
        result.current.freezeCard('card-1');
      });

      expect(result.current.isLoading).toBe(true);
    });

    it('should update card status to frozen on success', async () => {
      (cardService.freezeCard as jest.Mock).mockResolvedValue(undefined);
      useCardStore.setState({ cards: mockCards, selectedCard: mockCards[0] });

      const { result } = renderHook(() => useCardStore());

      await act(async () => {
        await result.current.freezeCard('card-1');
      });

      expect(cardService.freezeCard).toHaveBeenCalledWith('card-1');
      expect(result.current.cards[0].status).toBe('frozen');
      expect(result.current.selectedCard?.status).toBe('frozen');
      expect(result.current.isLoading).toBe(false);
    });

    it('should not update selectedCard if different card is frozen', async () => {
      const cards: VirtualCard[] = [
        { ...mockCards[0], id: 'card-1' },
        {
          ...mockCards[0],
          id: 'card-2',
          lastFour: '5678',
          status: 'active',
        },
      ];

      (cardService.freezeCard as jest.Mock).mockResolvedValue(undefined);
      useCardStore.setState({ cards, selectedCard: cards[0] });

      const { result } = renderHook(() => useCardStore());

      await act(async () => {
        await result.current.freezeCard('card-2');
      });

      expect(result.current.cards[1].status).toBe('frozen');
      expect(result.current.selectedCard?.status).toBe('active');
    });

    it('should handle error when freezing card fails', async () => {
      const errorMessage = 'Card already frozen';
      (cardService.freezeCard as jest.Mock).mockRejectedValue({
        response: { data: { message: errorMessage } },
      });

      useCardStore.setState({ cards: mockCards });

      const { result } = renderHook(() => useCardStore());

      await act(async () => {
        try {
          await result.current.freezeCard('card-1');
        } catch {
          // Expected to throw
        }
      });

      expect(result.current.error).toBe(errorMessage);
      expect(result.current.isLoading).toBe(false);
    });
  });

  describe('unfreezeCard', () => {
    const mockCards: VirtualCard[] = [
      {
        id: 'card-1',
        lastFour: '1234',
        cardHolder: 'Test User',
        expiryDate: '12/25',
        cvv: '123',
        status: 'frozen',
        balance: 1000000,
        limit: 5000000,
        spendingLimit: 2000000,
        isPhysical: false,
        createdAt: '2024-01-01T00:00:00Z',
      },
    ];

    it('should set loading state when unfreezing card', () => {
      (cardService.unfreezeCard as jest.Mock).mockImplementation(
        () => new Promise((resolve) => setTimeout(resolve, 100))
      );

      const { result } = renderHook(() => useCardStore());

      act(() => {
        result.current.unfreezeCard('card-1');
      });

      expect(result.current.isLoading).toBe(true);
    });

    it('should update card status to active on success', async () => {
      (cardService.unfreezeCard as jest.Mock).mockResolvedValue(undefined);
      useCardStore.setState({ cards: mockCards, selectedCard: mockCards[0] });

      const { result } = renderHook(() => useCardStore());

      await act(async () => {
        await result.current.unfreezeCard('card-1');
      });

      expect(cardService.unfreezeCard).toHaveBeenCalledWith('card-1');
      expect(result.current.cards[0].status).toBe('active');
      expect(result.current.selectedCard?.status).toBe('active');
      expect(result.current.isLoading).toBe(false);
    });

    it('should handle error when unfreezing card fails', async () => {
      const errorMessage = 'Card not frozen';
      (cardService.unfreezeCard as jest.Mock).mockRejectedValue({
        response: { data: { message: errorMessage } },
      });

      useCardStore.setState({ cards: mockCards });

      const { result } = renderHook(() => useCardStore());

      await act(async () => {
        try {
          await result.current.unfreezeCard('card-1');
        } catch {
          // Expected to throw
        }
      });

      expect(result.current.error).toBe(errorMessage);
      expect(result.current.isLoading).toBe(false);
    });
  });

  describe('setSpendingLimit', () => {
    const mockCards: VirtualCard[] = [
      {
        id: 'card-1',
        lastFour: '1234',
        cardHolder: 'Test User',
        expiryDate: '12/25',
        cvv: '123',
        status: 'active',
        balance: 1000000,
        limit: 5000000,
        spendingLimit: 2000000,
        isPhysical: false,
        createdAt: '2024-01-01T00:00:00Z',
      },
    ];

    it('should set loading state when setting limit', () => {
      (cardService.setSpendingLimit as jest.Mock).mockImplementation(
        () => new Promise((resolve) => setTimeout(resolve, 100))
      );

      const { result } = renderHook(() => useCardStore());

      act(() => {
        result.current.setSpendingLimit('card-1', 3000000);
      });

      expect(result.current.isLoading).toBe(true);
    });

    it('should update spending limit on success', async () => {
      (cardService.setSpendingLimit as jest.Mock).mockResolvedValue(undefined);
      useCardStore.setState({ cards: mockCards, selectedCard: mockCards[0] });

      const { result } = renderHook(() => useCardStore());

      await act(async () => {
        await result.current.setSpendingLimit('card-1', 3000000);
      });

      expect(cardService.setSpendingLimit).toHaveBeenCalledWith('card-1', 3000000);
      expect(result.current.cards[0].spendingLimit).toBe(3000000);
      expect(result.current.selectedCard?.spendingLimit).toBe(3000000);
      expect(result.current.isLoading).toBe(false);
    });

    it('should handle error when setting limit fails', async () => {
      const errorMessage = 'Limit exceeds maximum';
      (cardService.setSpendingLimit as jest.Mock).mockRejectedValue({
        response: { data: { message: errorMessage } },
      });

      useCardStore.setState({ cards: mockCards });

      const { result } = renderHook(() => useCardStore());

      await act(async () => {
        try {
          await result.current.setSpendingLimit('card-1', 10000000);
        } catch {
          // Expected to throw
        }
      });

      expect(result.current.error).toBe(errorMessage);
      expect(result.current.isLoading).toBe(false);
    });
  });

  describe('cancelCard', () => {
    const mockCards: VirtualCard[] = [
      {
        id: 'card-1',
        lastFour: '1234',
        cardHolder: 'Test User',
        expiryDate: '12/25',
        cvv: '123',
        status: 'active',
        balance: 1000000,
        limit: 5000000,
        spendingLimit: 2000000,
        isPhysical: false,
        createdAt: '2024-01-01T00:00:00Z',
      },
      {
        id: 'card-2',
        lastFour: '5678',
        cardHolder: 'Test User',
        expiryDate: '06/26',
        cvv: '456',
        status: 'active',
        balance: 500000,
        limit: 3000000,
        spendingLimit: 1000000,
        isPhysical: true,
        createdAt: '2024-02-01T00:00:00Z',
      },
    ];

    it('should set loading state when canceling card', () => {
      (cardService.cancelCard as jest.Mock).mockImplementation(
        () => new Promise((resolve) => setTimeout(resolve, 100))
      );

      const { result } = renderHook(() => useCardStore());

      act(() => {
        result.current.cancelCard('card-1');
      });

      expect(result.current.isLoading).toBe(true);
    });

    it('should remove card from state on success', async () => {
      (cardService.cancelCard as jest.Mock).mockResolvedValue(undefined);
      useCardStore.setState({ cards: mockCards, selectedCard: mockCards[1] });

      const { result } = renderHook(() => useCardStore());

      await act(async () => {
        await result.current.cancelCard('card-1');
      });

      expect(cardService.cancelCard).toHaveBeenCalledWith('card-1');
      expect(result.current.cards).toHaveLength(1);
      expect(result.current.cards[0].id).toBe('card-2');
      expect(result.current.selectedCard?.id).toBe('card-2');
      expect(result.current.isLoading).toBe(false);
    });

    it('should clear selectedCard if canceled card was selected', async () => {
      (cardService.cancelCard as jest.Mock).mockResolvedValue(undefined);
      useCardStore.setState({ cards: [mockCards[0]], selectedCard: mockCards[0] });

      const { result } = renderHook(() => useCardStore());

      await act(async () => {
        await result.current.cancelCard('card-1');
      });

      expect(result.current.cards).toHaveLength(0);
      expect(result.current.selectedCard).toBeNull();
    });

    it('should handle error when canceling card fails', async () => {
      const errorMessage = 'Cannot cancel card with pending transactions';
      (cardService.cancelCard as jest.Mock).mockRejectedValue({
        response: { data: { message: errorMessage } },
      });

      useCardStore.setState({ cards: mockCards });

      const { result } = renderHook(() => useCardStore());

      await act(async () => {
        try {
          await result.current.cancelCard('card-1');
        } catch {
          // Expected to throw
        }
      });

      expect(result.current.error).toBe(errorMessage);
      expect(result.current.isLoading).toBe(false);
      expect(result.current.cards).toHaveLength(2);
    });
  });

  describe('clearError', () => {
    it('should clear error state', () => {
      useCardStore.setState({ error: 'Some error' });

      const { result } = renderHook(() => useCardStore());

      act(() => {
        result.current.clearError();
      });

      expect(result.current.error).toBeNull();
    });
  });
});
