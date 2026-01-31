import React from 'react';
import { render, screen, fireEvent } from '@testing-library/react-native';
import { BalanceCard } from '@/components/shared/BalanceCard';
import { formatCurrency } from '@/utils/currency';

describe('BalanceCard', () => {
  const defaultProps = {
    balance: 1500000,
    accountNumber: '1234567890',
    showBalance: true,
    onToggleBalance: jest.fn(),
  };

  beforeEach(() => {
    jest.clearAllMocks();
  });

  describe('Rendering', () => {
    it('should render the card with correct label', () => {
      render(<BalanceCard {...defaultProps} />);

      expect(screen.getByText('Total Balance')).toBeTruthy();
    });

    it('should render formatted balance when showBalance is true', () => {
      render(<BalanceCard {...defaultProps} />);

      const expectedBalance = formatCurrency(defaultProps.balance);
      expect(screen.getByText(expectedBalance)).toBeTruthy();
    });

    it('should render masked balance when showBalance is false', () => {
      render(<BalanceCard {...defaultProps} showBalance={false} />);

      expect(screen.getByText('••••••••')).toBeTruthy();
    });

    it('should render account number when provided', () => {
      render(<BalanceCard {...defaultProps} />);

      expect(screen.getByText('Account Number')).toBeTruthy();
      expect(screen.getByText(defaultProps.accountNumber)).toBeTruthy();
    });

    it('should not render account section when accountNumber is not provided', () => {
      render(<BalanceCard {...defaultProps} accountNumber={undefined} />);

      expect(screen.queryByText('Account Number')).toBeNull();
    });

    it('should render with zero balance', () => {
      render(<BalanceCard {...defaultProps} balance={0} />);

      expect(screen.getByText(formatCurrency(0))).toBeTruthy();
    });

    it('should render with large balance', () => {
      const largeBalance = 999999999999;
      render(<BalanceCard {...defaultProps} balance={largeBalance} />);

      expect(screen.getByText(formatCurrency(largeBalance))).toBeTruthy();
    });
  });

  describe('Props Handling', () => {
    it('should apply custom style prop', () => {
      const customStyle = { marginTop: 20 };
      const { UNSAFE_root } = render(
        <BalanceCard {...defaultProps} style={customStyle} />
      );

      expect(UNSAFE_root).toBeTruthy();
    });

    it('should use default showBalance as true when not provided', () => {
      const { balance, accountNumber, onToggleBalance } = defaultProps;
      render(
        <BalanceCard
          balance={balance}
          accountNumber={accountNumber}
          onToggleBalance={onToggleBalance}
        />
      );

      const expectedBalance = formatCurrency(balance);
      expect(screen.getByText(expectedBalance)).toBeTruthy();
    });

    it('should handle negative balance', () => {
      render(<BalanceCard {...defaultProps} balance={-50000} />);

      expect(screen.getByText(formatCurrency(-50000))).toBeTruthy();
    });
  });

  describe('User Interactions', () => {
    it('should call onToggleBalance when eye button is pressed', () => {
      render(<BalanceCard {...defaultProps} />);

      // Find the eye button by accessibility label
      const eyeButton = screen.getByLabelText('Hide balance');
      fireEvent.press(eyeButton);

      expect(defaultProps.onToggleBalance).toHaveBeenCalledTimes(1);
    });

    it('should not render eye button when onToggleBalance is not provided', () => {
      render(<BalanceCard balance={defaultProps.balance} />);

      // Query for the hide/show balance button should return null
      const eyeButton = screen.queryByLabelText('Hide balance');
      expect(eyeButton).toBeNull();
    });
  });

  describe('Edge Cases', () => {
    it('should handle empty account number string', () => {
      render(<BalanceCard {...defaultProps} accountNumber="" />);

      // Empty string is falsy, so account section should not render
      expect(screen.queryByText('Account Number')).toBeNull();
    });

    it('should handle very long account numbers', () => {
      const longAccountNumber = '123456789012345678901234567890';
      render(<BalanceCard {...defaultProps} accountNumber={longAccountNumber} />);

      expect(screen.getByText(longAccountNumber)).toBeTruthy();
    });

    it('should handle decimal balance values', () => {
      render(<BalanceCard {...defaultProps} balance={1500000.75} />);

      // formatCurrency should handle decimals appropriately
      expect(screen.getByText(formatCurrency(1500000.75))).toBeTruthy();
    });

    it('should handle undefined accountNumber gracefully', () => {
      render(<BalanceCard balance={100000} accountNumber={undefined} />);

      expect(screen.getByText('Total Balance')).toBeTruthy();
      expect(screen.queryByText('Account Number')).toBeNull();
    });

    it('should handle rapid toggle presses', () => {
      render(<BalanceCard {...defaultProps} />);

      const eyeButton = screen.getByLabelText('Hide balance');
      fireEvent.press(eyeButton);
      fireEvent.press(eyeButton);
      fireEvent.press(eyeButton);

      expect(defaultProps.onToggleBalance).toHaveBeenCalledTimes(3);
    });
  });
});
