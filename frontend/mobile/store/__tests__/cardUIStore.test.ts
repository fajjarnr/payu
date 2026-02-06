/**
 * Card UI Store Tests
 *
 * These tests verify the UI-only state management for cards.
 * Server state (card data) is managed by TanStack Query.
 * @see @/hooks/__tests__/useCardQuery.test.ts for server state tests
 */
import { act, renderHook } from '@testing-library/react-native';
import { useCardUIStore } from '../cardUIStore';

describe('cardUIStore', () => {
  beforeEach(() => {
    // Reset store state before each test
    useCardUIStore.setState({
      selectedCardId: null,
      cardViewMode: 'grid',
      showCardDetails: false,
    });
  });

  describe('initial state', () => {
    it('should have correct initial state', () => {
      const state = useCardUIStore.getState();

      expect(state.selectedCardId).toBeNull();
      expect(state.cardViewMode).toBe('grid');
      expect(state.showCardDetails).toBe(false);
    });
  });

  describe('selectCard', () => {
    it('should select card by id', () => {
      const { result } = renderHook(() => useCardUIStore());

      act(() => {
        result.current.selectCard('card-1');
      });

      expect(result.current.selectedCardId).toBe('card-1');
    });

    it('should change selected card', () => {
      const { result } = renderHook(() => useCardUIStore());

      act(() => {
        result.current.selectCard('card-1');
      });

      expect(result.current.selectedCardId).toBe('card-1');

      act(() => {
        result.current.selectCard('card-2');
      });

      expect(result.current.selectedCardId).toBe('card-2');
    });

    it('should clear selection when null is passed', () => {
      const { result } = renderHook(() => useCardUIStore());

      act(() => {
        result.current.selectCard('card-1');
      });

      expect(result.current.selectedCardId).toBe('card-1');

      act(() => {
        result.current.selectCard(null);
      });

      expect(result.current.selectedCardId).toBeNull();
    });
  });

  describe('setCardViewMode', () => {
    it('should change view mode to list', () => {
      const { result } = renderHook(() => useCardUIStore());

      act(() => {
        result.current.setCardViewMode('list');
      });

      expect(result.current.cardViewMode).toBe('list');
    });

    it('should change view mode to grid', () => {
      const { result } = renderHook(() => useCardUIStore());

      act(() => {
        result.current.setCardViewMode('list');
      });

      act(() => {
        result.current.setCardViewMode('grid');
      });

      expect(result.current.cardViewMode).toBe('grid');
    });
  });

  describe('toggleCardDetails', () => {
    it('should toggle card details visibility', () => {
      const { result } = renderHook(() => useCardUIStore());

      expect(result.current.showCardDetails).toBe(false);

      act(() => {
        result.current.toggleCardDetails();
      });

      expect(result.current.showCardDetails).toBe(true);

      act(() => {
        result.current.toggleCardDetails();
      });

      expect(result.current.showCardDetails).toBe(false);
    });
  });

  describe('resetCardUI', () => {
    it('should reset all UI state to defaults', () => {
      const { result } = renderHook(() => useCardUIStore());

      act(() => {
        result.current.selectCard('card-1');
        result.current.setCardViewMode('list');
        result.current.toggleCardDetails();
      });

      expect(result.current.selectedCardId).toBe('card-1');
      expect(result.current.cardViewMode).toBe('list');
      expect(result.current.showCardDetails).toBe(true);

      act(() => {
        result.current.resetCardUI();
      });

      expect(result.current.selectedCardId).toBeNull();
      expect(result.current.cardViewMode).toBe('grid');
      expect(result.current.showCardDetails).toBe(false);
    });
  });

  describe('selectors', () => {
    it('should use selectSelectedCardId selector', () => {
      const { result } = renderHook(() => useCardUIStore());

      act(() => {
        result.current.selectCard('card-1');
      });

      const selectedId = useCardUIStore.getState().selectedCardId;
      expect(selectedId).toBe('card-1');
    });

    it('should use selectCardViewMode selector', () => {
      const { result } = renderHook(() => useCardUIStore());

      act(() => {
        result.current.setCardViewMode('list');
      });

      const viewMode = useCardUIStore.getState().cardViewMode;
      expect(viewMode).toBe('list');
    });

    it('should use selectShowCardDetails selector', () => {
      const { result } = renderHook(() => useCardUIStore());

      act(() => {
        result.current.toggleCardDetails();
      });

      const showDetails = useCardUIStore.getState().showCardDetails;
      expect(showDetails).toBe(true);
    });
  });
});
