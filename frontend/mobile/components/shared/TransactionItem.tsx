import React from 'react';
import { View, Text, StyleSheet, TouchableOpacity, ViewStyle } from 'react-native';
import { useTheme } from '@react-navigation/native';
import { Transaction } from '@/types';
import { formatCurrency, formatRelativeTime } from '@/utils';
import { Card } from '@/components/ui/Card';
import { Badge } from '@/components/ui/Badge';

interface TransactionItemProps {
  transaction: Transaction;
  onPress?: () => void;
  style?: ViewStyle;
  testID?: string;
}

export const TransactionItemComponent: React.FC<TransactionItemProps> = ({
  transaction,
  onPress,
  style,
  testID,
}) => {
  const { colors } = useTheme();

  const getIcon = () => {
    switch (transaction.type) {
      case 'transfer':
        return transaction.description.includes('received') ? '⬇️' : '⬆️';
      case 'payment':
        return '💳';
      case 'topup':
        return '➕';
      case 'qris':
        return '📱';
      case 'withdrawal':
        return '🏧';
      default:
        return '💸';
    }
  };

  const isIncome = ['topup', 'transfer'].includes(transaction.type) &&
                   transaction.description.toLowerCase().includes('received');

  const getStatusVariant = () => {
    switch (transaction.status) {
      case 'completed':
        return 'success';
      case 'pending':
        return 'warning';
      case 'failed':
      case 'cancelled':
        return 'error';
      default:
        return 'info';
    }
  };

  const getA11yLabel = () => {
    const typeText = transaction.type === 'transfer'
      ? (isIncome ? 'Received transfer' : 'Sent transfer')
      : transaction.type;
    const amountText = `${isIncome ? 'Plus' : 'Minus'} ${formatCurrency(transaction.amount)}`;
    const statusText = transaction.status !== 'completed'
      ? `Status: ${transaction.status}`
      : '';
    return `${typeText}: ${transaction.description}, ${amountText}. ${statusText}`.trim();
  };

  const getA11yHint = () => {
    return `Double tap to view transaction details for ${transaction.description}`;
  };

  return (
    <TouchableOpacity
      onPress={onPress}
      activeOpacity={0.7}
      accessibilityLabel={getA11yLabel()}
      accessibilityHint={getA11yHint()}
      accessibilityRole="button"
      testID={testID}
    >
      <Card variant="flat" padding="md" style={style as any}>
        <View style={styles.leftContainer}>
          <View
            style={[styles.iconContainer, { backgroundColor: `${'#10b981'}20` }]}
            accessible={false}
            importantForAccessibility="no"
          >
            <Text style={styles.icon} accessible={false}>{getIcon()}</Text>
          </View>
          <View style={styles.textContainer}>
            <Text
              style={[styles.description, { color: colors.text }]}
              accessibilityLabel={`Description: ${transaction.description}`}
            >
              {transaction.description}
            </Text>
            <Text
              style={[styles.time, { color: (colors as typeof colors & { textSecondary?: string }).textSecondary ?? '#6b7280' }]}
              accessibilityLabel={`Time: ${formatRelativeTime(transaction.createdAt)}`}
            >
              {formatRelativeTime(transaction.createdAt)}
            </Text>
          </View>
        </View>

        <View style={styles.rightContainer}>
          <Text
            style={[
              styles.amount,
              { color: isIncome ? '#10b981' : colors.text },
            ]}
            accessibilityLabel={`Amount: ${isIncome ? 'Plus' : 'Minus'} ${formatCurrency(transaction.amount)}`}
          >
            {isIncome ? '+' : '-'}{formatCurrency(transaction.amount)}
          </Text>
          {transaction.status !== 'completed' && (
            <Badge
              text={transaction.status}
              variant={getStatusVariant()}
              size="sm"
            />
          )}
        </View>
      </Card>
    </TouchableOpacity>
  );
};

export const TransactionItem = React.memo(TransactionItemComponent, (prevProps, nextProps) => {
  return (
    prevProps.transaction.id === nextProps.transaction.id &&
    prevProps.transaction.status === nextProps.transaction.status &&
    prevProps.transaction.amount === nextProps.transaction.amount &&
    prevProps.onPress === nextProps.onPress
  );
});

TransactionItem.displayName = 'TransactionItem';

const styles = StyleSheet.create({
  container: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
  },
  leftContainer: {
    flexDirection: 'row',
    alignItems: 'center',
    flex: 1,
  },
  iconContainer: {
    width: 44,
    height: 44,
    borderRadius: 12,
    justifyContent: 'center',
    alignItems: 'center',
    marginRight: 12,
  },
  icon: {
    fontSize: 20,
  },
  textContainer: {
    flex: 1,
  },
  description: {
    fontSize: 15,
    fontWeight: '600',
    marginBottom: 2,
  },
  time: {
    fontSize: 13,
  },
  rightContainer: {
    alignItems: 'flex-end',
  },
  amount: {
    fontSize: 16,
    fontWeight: '700',
    marginBottom: 4,
  },
});
