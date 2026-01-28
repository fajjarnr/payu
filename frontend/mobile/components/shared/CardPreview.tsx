import React from 'react';
import { View, Text, StyleSheet, ViewStyle } from 'react-native';
import { Card } from '@/components/ui/Card';
import { VirtualCard } from '@/types';

interface CardPreviewProps {
  card: VirtualCard;
  style?: ViewStyle;
  showDetails?: boolean;
}

export const CardPreview: React.FC<CardPreviewProps> = ({
  card,
  style,
  showDetails = false,
}) => {
  const getCardColor = () => {
    switch (card.status) {
      case 'active':
        return ['#10b981', '#059669'];
      case 'frozen':
        return ['#6b7280', '#4b5563'];
      case 'cancelled':
        return ['#ef4444', '#dc2626'];
      default:
        return ['#10b981', '#059669'];
    }
  };

  const [gradientStart, gradientEnd] = getCardColor();

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
          {showDetails ? '•••• •••• •••• ' + card.lastFour : '•••• •••• •••• ••••'}
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
        <Text style={styles.balance}>Rp {card.balance.toLocaleString('id-ID')}</Text>
      </View>

      {/* Decorative elements */}
      <View style={[styles.circle1, { backgroundColor: 'rgba(255,255,255,0.1)' }]} />
      <View style={[styles.circle2, { backgroundColor: 'rgba(255,255,255,0.05)' }]} />
    </Card>
  );
};

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
