import React from 'react';
import {
  View,
  Text,
  StyleSheet,
  ScrollView,
  TouchableOpacity,
} from 'react-native';
import { useRouter } from 'expo-router';
import { useTheme } from '@react-navigation/native';
import { useCards } from '@/hooks/useCards';
import { CardPreview } from '@/components/shared/CardPreview';
import { Card } from '@/components/ui/Card';
import { Button } from '@/components/ui/Button';
import { Alert } from 'react-native';

export default function CardsScreen() {
  const router = useRouter();
  const { colors } = useTheme();
  const { cards, selectedCard, selectCard, createCard, freezeCard, unfreezeCard } = useCards();

  const handleToggleFreeze = async () => {
    if (!selectedCard) return;

    try {
      if (selectedCard.status === 'active') {
        await freezeCard(selectedCard.id);
      } else {
        await unfreezeCard(selectedCard.id);
      }
    } catch (error: any) {
      Alert.alert(
        'Action Failed',
        error.response?.data?.message || 'Something went wrong'
      );
    }
  };

  const handleCreateCard = async () => {
    try {
      await createCard();
    } catch (error: any) {
      Alert.alert(
        'Failed',
        error.response?.data?.message || 'Could not create card'
      );
    }
  };

  return (
    <ScrollView
      style={styles.container}
      contentContainerStyle={styles.content}
      showsVerticalScrollIndicator={false}
    >
      <View style={styles.header}>
        <Text style={[styles.title, { color: colors.text }]}>My Cards</Text>
      </View>

      {cards.length === 0 ? (
        <Card padding="lg" style={styles.emptyState}>
          <Text style={styles.emptyIcon}>💳</Text>
          <Text style={[styles.emptyTitle, { color: colors.text }]}>
            No Virtual Cards Yet
          </Text>
          <Text style={[styles.emptyText, { color: colors.textSecondary }]}>
            Create your first virtual card to start spending online
          </Text>
          <Button
            title="Create Virtual Card"
            onPress={handleCreateCard}
            style={styles.createButton}
          />
        </Card>
      ) : (
        <>
          {/* Card Carousel */}
          <ScrollView
            horizontal
            showsHorizontalScrollIndicator={false}
            style={styles.cardCarousel}
            contentContainerStyle={styles.cardCarouselContent}
            pagingEnabled
          >
            {cards.map((card) => (
              <TouchableOpacity
                key={card.id}
                onPress={() => selectCard(card.id)}
                activeOpacity={0.9}
              >
                <CardPreview
                  card={card}
                  style={{
                    width: 320,
                    marginRight: 16,
                  }}
                />
              </TouchableOpacity>
            ))}
          </ScrollView>

          {/* Card Actions */}
          {selectedCard && (
            <Card padding="lg" style={styles.cardActions}>
              <Text style={[styles.actionsTitle, { color: colors.text }]}>
                Card Actions
              </Text>

              <View style={styles.actionButtons}>
                <TouchableOpacity
                  style={[styles.actionButton, { borderColor: colors.border }]}
                  onPress={handleToggleFreeze}
                >
                  <Text style={styles.actionIcon}>
                    {selectedCard.status === 'active' ? '🔒' : '🔓'}
                  </Text>
                  <Text style={[styles.actionText, { color: colors.text }]}>
                    {selectedCard.status === 'active' ? 'Freeze' : 'Unfreeze'}
                  </Text>
                </TouchableOpacity>

                <TouchableOpacity
                  style={[styles.actionButton, { borderColor: colors.border }]}
                  onPress={() => router.push('/card/settings')}
                >
                  <Text style={styles.actionIcon}>⚙️</Text>
                  <Text style={[styles.actionText, { color: colors.text }]}>
                    Settings
                  </Text>
                </TouchableOpacity>

                <TouchableOpacity
                  style={[styles.actionButton, { borderColor: colors.border }]}
                  onPress={() => router.push('/card/details')}
                >
                  <Text style={styles.actionIcon}>👁️</Text>
                  <Text style={[styles.actionText, { color: colors.text }]}>
                    View Details
                  </Text>
                </TouchableOpacity>
              </View>
            </Card>
          )}

          {/* Card Info */}
          {selectedCard && (
            <Card padding="lg" style={styles.cardInfo}>
              <View style={styles.infoRow}>
                <Text style={[styles.infoLabel, { color: colors.textSecondary }]}>
                  Spending Limit
                </Text>
                <Text style={[styles.infoValue, { color: colors.text }]}>
                  Rp {selectedCard.spendingLimit.toLocaleString('id-ID')}
                </Text>
              </View>
              <View style={styles.infoRow}>
                <Text style={[styles.infoLabel, { color: colors.textSecondary }]}>
                  Status
                </Text>
                <Text
                  style={[
                    styles.infoValue,
                    {
                      color:
                        selectedCard.status === 'active'
                          ? '#10b981'
                          : selectedCard.status === 'frozen'
                          ? '#6b7280'
                          : '#ef4444',
                    },
                  ]}
                >
                  {selectedCard.status.toUpperCase()}
                </Text>
              </View>
            </Card>
          )}
        </>
      )}
    </ScrollView>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#f9fafb',
  },
  content: {
    padding: 20,
  },
  header: {
    marginBottom: 24,
  },
  title: {
    fontSize: 28,
    fontWeight: '900',
    letterSpacing: -1,
  },
  emptyState: {
    alignItems: 'center',
    paddingVertical: 48,
  },
  emptyIcon: {
    fontSize: 64,
    marginBottom: 16,
  },
  emptyTitle: {
    fontSize: 20,
    fontWeight: '700',
    marginBottom: 8,
  },
  emptyText: {
    fontSize: 14,
    textAlign: 'center',
    marginBottom: 24,
  },
  createButton: {
    minWidth: 200,
  },
  cardCarousel: {
    marginHorizontal: -20,
    marginBottom: 32,
  },
  cardCarouselContent: {
    paddingHorizontal: 20,
  },
  cardActions: {
    marginBottom: 16,
  },
  actionsTitle: {
    fontSize: 18,
    fontWeight: '700',
    marginBottom: 16,
  },
  actionButtons: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    gap: 12,
  },
  actionButton: {
    flex: 1,
    alignItems: 'center',
    paddingVertical: 16,
    borderRadius: 12,
    borderWidth: 1,
  },
  actionIcon: {
    fontSize: 24,
    marginBottom: 8,
  },
  actionText: {
    fontSize: 12,
    fontWeight: '600',
  },
  cardInfo: {
    marginBottom: 16,
  },
  infoRow: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    paddingVertical: 12,
    borderBottomWidth: 1,
    borderBottomColor: '#e5e7eb',
  },
  infoLabel: {
    fontSize: 14,
  },
  infoValue: {
    fontSize: 14,
    fontWeight: '600',
  },
});
