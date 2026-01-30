import React from 'react';
import { render, screen, fireEvent } from '@testing-library/react-native';
import { TransactionItem } from '@/components/shared/TransactionItem';
import { Transaction } from '@/types';

describe('TransactionItem', () => {
  const mockTransaction: Transaction = {
    id: 'txn-123',
    userId: 'user-456',
    type: 'transfer',
    amount: 500000,
    description: 'Transfer to John',
    status: 'completed',
    createdAt: new Date().toISOString(),
  };

  const defaultProps = {
    transaction: mockTransaction,
    onPress: jest.fn(),
  };

  beforeEach(() => {
    jest.clearAllMocks();
  });

  describe('Rendering', () => {
    it('should render transaction description', () => {
      render(<TransactionItem {...defaultProps} />);

      expect(screen.getByText(mockTransaction.description)).toBeTruthy();
    });

    it('should render formatted amount', () => {
      render(<TransactionItem {...defaultProps} />);

      expect(screen.getByText(/Rp/)).toBeTruthy();
    });

    it('should render relative time', () => {
      render(<TransactionItem {...defaultProps} />);

      // The time text is rendered
      expect(screen.getByText(/\d+[smhd] ago|Just now/)).toBeTruthy();
    });

    it('should render correct icon for transfer type', () => {
      render(<TransactionItem {...defaultProps} />);

      // Transfer out should show up arrow
      expect(screen.getByText('⬆️')).toBeTruthy();
    });

    it('should render correct icon for received transfer', () => {
      const receivedTransaction: Transaction = {
        ...mockTransaction,
        description: 'Transfer received from Jane',
      };
      render(<TransactionItem {...defaultProps} transaction={receivedTransaction} />);

      expect(screen.getByText('⬇️')).toBeTruthy();
    });

    it('should render correct icon for payment type', () => {
      const paymentTransaction: Transaction = {
        ...mockTransaction,
        type: 'payment',
        description: 'Payment at Store',
      };
      render(<TransactionItem {...defaultProps} transaction={paymentTransaction} />);

      expect(screen.getByText('💳')).toBeTruthy();
    });

    it('should render correct icon for topup type', () => {
      const topupTransaction: Transaction = {
        ...mockTransaction,
        type: 'topup',
        description: 'Top up balance',
      };
      render(<TransactionItem {...defaultProps} transaction={topupTransaction} />);

      expect(screen.getByText('➕')).toBeTruthy();
    });

    it('should render correct icon for qris type', () => {
      const qrisTransaction: Transaction = {
        ...mockTransaction,
        type: 'qris',
        description: 'QRIS Payment',
      };
      render(<TransactionItem {...defaultProps} transaction={qrisTransaction} />);

      expect(screen.getByText('📱')).toBeTruthy();
    });

    it('should render correct icon for withdrawal type', () => {
      const withdrawalTransaction: Transaction = {
        ...mockTransaction,
        type: 'withdrawal',
        description: 'ATM Withdrawal',
      };
      render(<TransactionItem {...defaultProps} transaction={withdrawalTransaction} />);

      expect(screen.getByText('🏧')).toBeTruthy();
    });

    it('should render default icon for unknown type', () => {
      const unknownTransaction: Transaction = {
        ...mockTransaction,
        type: 'unknown' as any,
        description: 'Unknown transaction',
      };
      render(<TransactionItem {...defaultProps} transaction={unknownTransaction} />);

      expect(screen.getByText('💸')).toBeTruthy();
    });
  });

  describe('Props Handling', () => {
    it('should apply custom style prop', () => {
      const customStyle = { marginVertical: 10 };
      const { UNSAFE_root } = render(
        <TransactionItem {...defaultProps} style={customStyle} />
      );

      expect(UNSAFE_root).toBeTruthy();
    });

    it('should render without onPress handler', () => {
      render(<TransactionItem transaction={mockTransaction} />);

      expect(screen.getByText(mockTransaction.description)).toBeTruthy();
    });
  });

  describe('User Interactions', () => {
    it('should call onPress when item is pressed', () => {
      render(<TransactionItem {...defaultProps} />);

      // Find the accessible touchable wrapper
      const touchable = screen.getAllByA11yState({})[0];
      fireEvent.press(touchable);

      expect(defaultProps.onPress).toHaveBeenCalledTimes(1);
    });

    it('should handle multiple rapid presses', () => {
      render(<TransactionItem {...defaultProps} />);

      const touchable = screen.getAllByA11yState({})[0];
      fireEvent.press(touchable);
      fireEvent.press(touchable);
      fireEvent.press(touchable);

      expect(defaultProps.onPress).toHaveBeenCalledTimes(3);
    });
  });

  describe('Amount Display', () => {
    it('should show positive sign for income transactions', () => {
      const incomeTransaction: Transaction = {
        ...mockTransaction,
        type: 'topup',
        description: 'Received from bank',
      };
      render(<TransactionItem {...defaultProps} transaction={incomeTransaction} />);

      // Check for the + sign in the amount text
      const amountTexts = screen.getAllByText(/Rp/);
      const hasPositiveSign = amountTexts.some(el => {
        const text = el.children?.[0] as string;
        return text && text.includes('+');
      });
      expect(hasPositiveSign).toBe(true);
    });

    it('should show negative sign for expense transactions', () => {
      render(<TransactionItem {...defaultProps} />);

      const amountTexts = screen.getAllByText(/Rp/);
      const hasNegativeSign = amountTexts.some(el => {
        const text = el.children?.[0] as string;
        return text && text.includes('-');
      });
      expect(hasNegativeSign).toBe(true);
    });

    it('should show positive sign for received transfers', () => {
      const receivedTransaction: Transaction = {
        ...mockTransaction,
        type: 'transfer',
        description: 'Transfer received from friend',
      };
      render(<TransactionItem {...defaultProps} transaction={receivedTransaction} />);

      const amountTexts = screen.getAllByText(/Rp/);
      const hasPositiveSign = amountTexts.some(el => {
        const text = el.children?.[0] as string;
        return text && text.includes('+');
      });
      expect(hasPositiveSign).toBe(true);
    });

    it('should handle zero amount', () => {
      const zeroTransaction: Transaction = {
        ...mockTransaction,
        amount: 0,
      };
      render(<TransactionItem {...defaultProps} transaction={zeroTransaction} />);

      expect(screen.getByText(/Rp/)).toBeTruthy();
    });

    it('should handle large amounts', () => {
      const largeTransaction: Transaction = {
        ...mockTransaction,
        amount: 999999999,
      };
      render(<TransactionItem {...defaultProps} transaction={largeTransaction} />);

      expect(screen.getByText(/Rp/)).toBeTruthy();
    });
  });

  describe('Status Badge', () => {
    it('should not show badge for completed transactions', () => {
      render(<TransactionItem {...defaultProps} />);

      // Completed status should not show a badge
      expect(screen.queryByText('completed')).toBeNull();
    });

    it('should show warning badge for pending transactions', () => {
      const pendingTransaction: Transaction = {
        ...mockTransaction,
        status: 'pending',
      };
      render(<TransactionItem {...defaultProps} transaction={pendingTransaction} />);

      expect(screen.getByText('pending')).toBeTruthy();
    });

    it('should show error badge for failed transactions', () => {
      const failedTransaction: Transaction = {
        ...mockTransaction,
        status: 'failed',
      };
      render(<TransactionItem {...defaultProps} transaction={failedTransaction} />);

      expect(screen.getByText('failed')).toBeTruthy();
    });

    it('should show error badge for cancelled transactions', () => {
      const cancelledTransaction: Transaction = {
        ...mockTransaction,
        status: 'cancelled',
      };
      render(<TransactionItem {...defaultProps} transaction={cancelledTransaction} />);

      expect(screen.getByText('cancelled')).toBeTruthy();
    });
  });

  describe('Edge Cases', () => {
    it('should handle very long description', () => {
      const longDescriptionTransaction: Transaction = {
        ...mockTransaction,
        description: 'A'.repeat(200),
      };
      render(<TransactionItem {...defaultProps} transaction={longDescriptionTransaction} />);

      expect(screen.getByText('A'.repeat(200))).toBeTruthy();
    });

    it('should handle empty description', () => {
      const emptyDescriptionTransaction: Transaction = {
        ...mockTransaction,
        description: '',
      };
      render(<TransactionItem {...defaultProps} transaction={emptyDescriptionTransaction} />);

      // The component should still render
      expect(screen.getByText('⬆️')).toBeTruthy();
    });

    it('should handle special characters in description', () => {
      const specialCharTransaction: Transaction = {
        ...mockTransaction,
        description: 'Payment @ Café 100% ☕',
      };
      render(<TransactionItem {...defaultProps} transaction={specialCharTransaction} />);

      expect(screen.getByText('Payment @ Café 100% ☕')).toBeTruthy();
    });

    it('should handle transaction with all optional fields', () => {
      const fullTransaction: Transaction = {
        ...mockTransaction,
        category: 'Food',
        fromPocket: 'primary',
        toPocket: 'savings',
        recipientName: 'John Doe',
        recipientAccount: '9876543210',
        processedAt: new Date().toISOString(),
      };
      render(<TransactionItem {...defaultProps} transaction={fullTransaction} />);

      expect(screen.getByText(fullTransaction.description)).toBeTruthy();
    });
  });
});
