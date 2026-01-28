import React from 'react';
import { View, Text, StyleSheet, TouchableOpacity, ViewStyle } from 'react-native';
import { useTheme } from '@react-navigation/native';
import { Eye, EyeOff } from 'lucide-react-native';
import { formatCurrency } from '@/utils/currency';
import { Card } from '@/components/ui/Card';

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
  const { colors } = useTheme();

  return (
    <Card
      style={style as any}
      padding="lg"
    >
      {/* Header */}
      <View style={styles.header}>
        <Text style={styles.label}>Total Balance</Text>
        {onToggleBalance && (
          <TouchableOpacity onPress={onToggleBalance} style={styles.eyeButton}>
            {showBalance ? (
              <Eye size={20} color="#ffffff" />
            ) : (
              <EyeOff size={20} color="#ffffff" />
            )}
          </TouchableOpacity>
        )}
      </View>

      {/* Balance */}
      <View style={styles.balanceContainer}>
        {showBalance ? (
          <Text style={styles.balance}>{formatCurrency(balance)}</Text>
        ) : (
          <Text style={styles.balance}>••••••••</Text>
        )}
      </View>

      {/* Account Number */}
      {accountNumber && (
        <View style={styles.accountContainer}>
          <Text style={styles.accountLabel}>Account Number</Text>
          <Text style={styles.accountNumber}>{accountNumber}</Text>
        </View>
      )}

      {/* Decorative gradient effect */}
      <View style={styles.decorativeCircle1} />
      <View style={styles.decorativeCircle2} />
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
