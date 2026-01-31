import React, { useMemo, memo, useCallback } from 'react';
import { View, Text, StyleSheet, ViewStyle } from 'react-native';
import { Card } from '@/components/ui/Card';
import { VirtualCard } from '@/types';

interface CardPreviewProps {
  card: VirtualCard;
  style?: ViewStyle;
  showDetails?: boolean;
}

// Performance: Memoize card color calculation to avoid recalculation on every render
const getCardColor = useCallback((status: string): string[] => {
  switch (status) {
    case 'active':
      return ['#10b981', '#059669'];
    case 'frozen':
      return ['#6b7280', '#4b5563'];
    case 'cancelled':
      return ['#ef4444', '#dc2626'];
    default:
      return ['#10b981', '#059669'];
  }
}, []);

export const CardPreviewComponent: React.FC<CardPreviewProps> = ({
  card,
  style,
  showDetails = false,
}) => {
  // Performance: Memoize card number display
  const cardNumberDisplay = useMemo(() => {
    return showDetails ? `•••• •••• •••• ${card.lastFour}` : '•••• •••• •••• ••••';
  }, [showDetails, card.lastFour]);

  // Performance: Memoize balance display
  const balanceDisplay = useMemo(() => {
    return `Rp ${card.balance.toLocaleString('id-ID')}`;
  }, [card.balance]);

  // Performance: Memoize card colors (not currently used but kept for future styling)
  const cardColors = useMemo(() => getCardColor(card.status), [card.status]);

  // Performance: Memoize decorative circle styles
  const circle1Style = useMemo<ViewStyle>(() => [
    styles.circle1,
    { backgroundColor: 'rgba(255,255,255,0.1)' },
  ], []);

  const circle2Style = useMemo<ViewStyle>(() => [
    styles.circle2,
    { backgroundColor: 'rgba(255,255,255,0.05)' },
  ], []);

  return (
    <Card
      style={style as any}
      padding="lg"
    >
      {/* Card Header */}
      <View style={styles.header}>
        <Text style={styles.cardBrand}>PayU</Text>
        <View style={styles.cardType}>
          <Text style={styles.cardTypeText}>VIRTUAL</Text>
        </View>
      </View>

      {/* Card Number */}
      <View style={styles.cardNumberContainer}>
        <Text style={styles.cardNumber}>
          {cardNumberDisplay}
        </Text>
      </View>

      {/* Card Details */}
      <View style={styles.cardDetails}>
        <View>
          <Text style={styles.label}>Card Holder</Text>
          <Text style={styles.value}>{card.cardHolder}</Text>
        </View>
        <View>
          <Text style={styles.label}>Expires</Text>
          <Text style={styles.value}>{card.expiryDate}</Text>
        </View>
        {showDetails && (
          <View>
            <Text style={styles.label}>CVV</Text>
            <Text style={styles.value}>{card.cvv}</Text>
          </View>
        )}
      </View>

      {/* Balance */}
      <View style={styles.balanceContainer}>
        <Text style={styles.balanceLabel}>Available Balance</Text>
        <Text style={styles.balance}>{balanceDisplay}</Text>
      </View>

      {/* Decorative elements */}
      <View style={circle1Style} />
      <View style={circle2Style} />
    </Card>
  );
};

// Performance: Memoize CardPreview component to prevent unnecessary re-renders in lists
export const CardPreview = memo(CardPreviewComponent, (prevProps, nextProps) => {
  return (
    prevProps.card.id === nextProps.card.id &&
    prevProps.card.status === nextProps.card.status &&
    prevProps.card.balance === nextProps.card.balance &&
    prevProps.showDetails === nextProps.showDetails
  );
});

CardPreview.displayName = 'CardPreview';

const styles = StyleSheet.create({
  cardContainer: {
    position: 'relative',
    overflow: 'hidden',
    minHeight: 200,
  },
  header: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginBottom: 24,
  },
  cardBrand: {
    fontSize: 20,
    fontWeight: '900',
    color: '#ffffff',
    letterSpacing: 2,
  },
  cardType: {
    backgroundColor: 'rgba(255, 255, 255, 0.2)',
    paddingHorizontal: 12,
    paddingVertical: 4,
    borderRadius: 4,
  },
  cardTypeText: {
    fontSize: 10,
    fontWeight: '700',
    color: '#ffffff',
  },
  cardNumberContainer: {
    marginBottom: 24,
  },
  cardNumber: {
    fontSize: 22,
    fontWeight: '700',
    color: '#ffffff',
    letterSpacing: 4,
  },
  cardDetails: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    marginBottom: 20,
  },
  label: {
    fontSize: 10,
    color: 'rgba(255, 255, 255, 0.7)',
    marginBottom: 4,
  },
  value: {
    fontSize: 14,
    fontWeight: '600',
    color: '#ffffff',
  },
  balanceContainer: {
    borderTopWidth: 1,
    borderTopColor: 'rgba(255, 255, 255, 0.2)',
    paddingTop: 16,
  },
  balanceLabel: {
    fontSize: 12,
    color: 'rgba(255, 255, 255, 0.8)',
    marginBottom: 4,
  },
  balance: {
    fontSize: 24,
    fontWeight: '900',
    color: '#ffffff',
  },
  circle1: {
    position: 'absolute',
    width: 120,
    height: 120,
    borderRadius: 60,
    top: -40,
    right: -20,
  },
  circle2: {
    position: 'absolute',
    width: 80,
    height: 80,
    borderRadius: 40,
    bottom: -30,
    left: -20,
  },
});
