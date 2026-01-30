import React from 'react';
import { View, Text, StyleSheet, TouchableOpacity, ViewStyle } from 'react-native';
import { Eye, EyeOff } from 'lucide-react-native';
import { formatCurrency } from '@/utils/currency';
import { Card } from '@/components/ui/Card';
import { formatCurrencyForA11y } from '@/src/utils/accessibility';

interface BalanceCardProps {
  balance: number;
  accountNumber?: string;
  showBalance?: boolean;
  onToggleBalance?: () => void;
  style?: ViewStyle;
}

export const BalanceCard: React.FC<BalanceCardProps> = ({
  balance,
  accountNumber,
  showBalance = true,
  onToggleBalance,
  style,
}) => {
  // useTheme is imported but not needed in this component

  return (
    <Card
      style={style as any}
      padding="lg"
    >
      {/* Header */}
      <View style={styles.header}>
        <Text style={styles.label} accessibilityLabel="Total Balance">Total Balance</Text>
        {onToggleBalance && (
          <TouchableOpacity
            onPress={onToggleBalance}
            style={styles.eyeButton}
            accessibilityLabel={showBalance ? 'Hide balance' : 'Show balance'}
            accessibilityHint="Toggles balance visibility"
            accessibilityRole="button"
            hitSlop={{ top: 10, bottom: 10, left: 10, right: 10 }}
          >
            {showBalance ? (
              <Eye size={20} stroke="#ffffff" />
            ) : (
              <EyeOff size={20} stroke="#ffffff" />
            )}
          </TouchableOpacity>
        )}
      </View>

      {/* Balance */}
      <View style={styles.balanceContainer}>
        {showBalance ? (
          <Text
            style={styles.balance}
            accessibilityLabel={`Balance: ${formatCurrencyForA11y(balance, 'IDR', 'id')}`}
            accessibilityRole="text"
          >
            {formatCurrency(balance)}
          </Text>
        ) : (
          <Text
            style={styles.balance}
            accessibilityLabel="Balance hidden"
            accessibilityRole="text"
          >
            ••••••••
          </Text>
        )}
      </View>

      {/* Account Number */}
      {accountNumber && (
        <View style={styles.accountContainer}>
          <Text style={styles.accountLabel} accessibilityLabel="Account Number">Account Number</Text>
          <Text
            style={styles.accountNumber}
            accessibilityLabel={`Account number: ${accountNumber}`}
            accessibilityRole="text"
          >
            {accountNumber}
          </Text>
        </View>
      )}

      {/* Decorative gradient effect - hidden from screen readers */}
      <View style={styles.decorativeCircle1} accessible={false} importantForAccessibility="no" />
      <View style={styles.decorativeCircle2} accessible={false} importantForAccessibility="no" />
    </Card>
  );
};

const styles = StyleSheet.create({
  header: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginBottom: 8,
  },
  label: {
    fontSize: 14,
    fontWeight: '500',
    color: 'rgba(255, 255, 255, 0.8)',
  },
  eyeButton: {
    padding: 4,
  },
  balanceContainer: {
    marginBottom: 16,
  },
  balance: {
    fontSize: 36,
    fontWeight: '900',
    color: '#ffffff',
    letterSpacing: -1,
  },
  accountContainer: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
  },
  accountLabel: {
    fontSize: 12,
    color: 'rgba(255, 255, 255, 0.7)',
  },
  accountNumber: {
    fontSize: 14,
    fontWeight: '600',
    color: '#ffffff',
  },
  decorativeCircle1: {
    position: 'absolute',
    width: 150,
    height: 150,
    borderRadius: 75,
    backgroundColor: 'rgba(255, 255, 255, 0.1)',
    top: -50,
    right: -30,
  },
  decorativeCircle2: {
    position: 'absolute',
    width: 100,
    height: 100,
    borderRadius: 50,
    backgroundColor: 'rgba(255, 255, 255, 0.05)',
    bottom: -30,
    left: -20,
  },
});
