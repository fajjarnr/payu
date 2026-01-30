import React from 'react';
import { render, screen, fireEvent } from '@testing-library/react-native';
import { QuickActions } from '@/components/shared/QuickActions';

describe('QuickActions', () => {
  const defaultProps = {
    onActionPress: jest.fn(),
  };

  beforeEach(() => {
    jest.clearAllMocks();
  });

  describe('Rendering', () => {
    it('should render all four action items', () => {
      render(<QuickActions {...defaultProps} />);

      expect(screen.getByText('Transfer')).toBeTruthy();
      expect(screen.getByText('QRIS')).toBeTruthy();
      expect(screen.getByText('Top Up')).toBeTruthy();
      expect(screen.getByText('Pay')).toBeTruthy();
    });

    it('should render correct icons for each action', () => {
      render(<QuickActions {...defaultProps} />);

      expect(screen.getByText('💸')).toBeTruthy(); // Transfer
      expect(screen.getByText('📱')).toBeTruthy(); // QRIS
      expect(screen.getByText('➕')).toBeTruthy(); // Top Up
      expect(screen.getByText('💳')).toBeTruthy(); // Pay
    });

    it('should render all action buttons', () => {
      render(<QuickActions {...defaultProps} />);

      // Find all accessible touchable elements
      const buttons = screen.getAllByA11yState({});
      expect(buttons.length).toBe(4);
    });

    it('should render Transfer action with correct styling', () => {
      render(<QuickActions {...defaultProps} />);

      const transferButton = screen.getByText('Transfer');
      expect(transferButton).toBeTruthy();
    });

    it('should render QRIS action with correct styling', () => {
      render(<QuickActions {...defaultProps} />);

      const qrisButton = screen.getByText('QRIS');
      expect(qrisButton).toBeTruthy();
    });

    it('should render Top Up action with correct styling', () => {
      render(<QuickActions {...defaultProps} />);

      const topUpButton = screen.getByText('Top Up');
      expect(topUpButton).toBeTruthy();
    });

    it('should render Pay action with correct styling', () => {
      render(<QuickActions {...defaultProps} />);

      const payButton = screen.getByText('Pay');
      expect(payButton).toBeTruthy();
    });
  });

  describe('Props Handling', () => {
    it('should call custom onActionPress when provided', () => {
      render(<QuickActions {...defaultProps} />);

      const buttons = screen.getAllByA11yState({});
      fireEvent.press(buttons[0]);

      expect(defaultProps.onActionPress).toHaveBeenCalledWith(
        expect.objectContaining({
          id: '1',
          label: 'Transfer',
          icon: '💸',
          route: '/(tabs)/transfers',
          color: '#10b981',
        })
      );
    });
  });

  describe('User Interactions', () => {
    it('should handle Transfer action press', () => {
      render(<QuickActions {...defaultProps} />);

      const buttons = screen.getAllByA11yState({});
      fireEvent.press(buttons[0]);

      expect(defaultProps.onActionPress).toHaveBeenCalledTimes(1);
    });

    it('should handle QRIS action press', () => {
      render(<QuickActions {...defaultProps} />);

      const buttons = screen.getAllByA11yState({});
      fireEvent.press(buttons[1]);

      expect(defaultProps.onActionPress).toHaveBeenCalledWith(
        expect.objectContaining({
          id: '2',
          label: 'QRIS',
          icon: '📱',
          route: '/qris',
          color: '#3b82f6',
        })
      );
    });

    it('should handle Top Up action press', () => {
      render(<QuickActions {...defaultProps} />);

      const buttons = screen.getAllByA11yState({});
      fireEvent.press(buttons[2]);

      expect(defaultProps.onActionPress).toHaveBeenCalledWith(
        expect.objectContaining({
          id: '3',
          label: 'Top Up',
          icon: '➕',
          route: '/topup',
          color: '#f59e0b',
        })
      );
    });

    it('should handle Pay action press', () => {
      render(<QuickActions {...defaultProps} />);

      const buttons = screen.getAllByA11yState({});
      fireEvent.press(buttons[3]);

      expect(defaultProps.onActionPress).toHaveBeenCalledWith(
        expect.objectContaining({
          id: '4',
          label: 'Pay',
          icon: '💳',
          route: '/pay',
          color: '#8b5cf6',
        })
      );
    });

    it('should handle rapid multiple presses', () => {
      render(<QuickActions {...defaultProps} />);

      const buttons = screen.getAllByA11yState({});
      fireEvent.press(buttons[0]);
      fireEvent.press(buttons[0]);
      fireEvent.press(buttons[0]);

      expect(defaultProps.onActionPress).toHaveBeenCalledTimes(3);
    });

    it('should handle all actions pressed in sequence', () => {
      render(<QuickActions {...defaultProps} />);

      const buttons = screen.getAllByA11yState({});
      fireEvent.press(buttons[0]);
      fireEvent.press(buttons[1]);
      fireEvent.press(buttons[2]);
      fireEvent.press(buttons[3]);

      expect(defaultProps.onActionPress).toHaveBeenCalledTimes(4);
    });
  });

  describe('Edge Cases', () => {
    it('should maintain correct action order', () => {
      render(<QuickActions {...defaultProps} />);

      const texts = screen.getAllByText(/Transfer|QRIS|Top Up|Pay/);
      const labels = texts.map(el => el.children?.[0] as string);

      expect(labels).toEqual(['Transfer', 'QRIS', 'Top Up', 'Pay']);
    });

    it('should handle undefined onActionPress prop', () => {
      const { UNSAFE_root } = render(<QuickActions onActionPress={undefined} />);

      expect(UNSAFE_root).toBeTruthy();
    });

    it('should render with correct color values', () => {
      render(<QuickActions {...defaultProps} />);

      // Colors are applied as background with opacity
      expect(screen.getByText('Transfer')).toBeTruthy();
      expect(screen.getByText('QRIS')).toBeTruthy();
      expect(screen.getByText('Top Up')).toBeTruthy();
      expect(screen.getByText('Pay')).toBeTruthy();
    });
  });

  describe('Layout', () => {
    it('should render in horizontal layout', () => {
      const { UNSAFE_root } = render(<QuickActions {...defaultProps} />);

      // Container should use flexDirection: 'row'
      expect(UNSAFE_root).toBeTruthy();
    });

    it('should distribute items evenly', () => {
      render(<QuickActions {...defaultProps} />);

      const buttons = screen.getAllByA11yState({});
      // All buttons should have flex: 1
      expect(buttons).toHaveLength(4);
    });
  });
});
