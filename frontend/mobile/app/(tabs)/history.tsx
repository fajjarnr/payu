import React, { useCallback } from 'react';
import {
  View,
  Text,
  StyleSheet,
  ScrollView,
  RefreshControl,
  TouchableOpacity,
} from 'react-native';
import { useRouter } from 'expo-router';
import { useTheme } from '@react-navigation/native';
import { useTransactions } from '@/hooks/useTransactions';
import { TransactionItem } from '@/components/shared/TransactionItem';
import { Card } from '@/components/ui/Card';
import { isToday, isYesterday, formatDate } from '@/utils/date';

export default function HistoryScreen() {
  const router = useRouter();
  const { colors } = useTheme();
  const {
    transactions,
    isLoading,
    loadTransactions,
    loadMoreTransactions,
    isLoadingMore,
    hasMore,
  } = useTransactions();

  const [refreshing, setRefreshing] = React.useState(false);

  const onRefresh = useCallback(async () => {
    setRefreshing(true);
    await loadTransactions(true);
    setRefreshing(false);
  }, [loadTransactions]);

  const groupTransactionsByDate = () => {
    const groups: { [key: string]: typeof transactions } = {};

    transactions.forEach((transaction) => {
      const date = new Date(transaction.createdAt);
      let groupKey = '';

      if (isToday(date)) {
        groupKey = 'Today';
      } else if (isYesterday(date)) {
        groupKey = 'Yesterday';
      } else {
        groupKey = formatDate(date);
      }

      if (!groups[groupKey]) {
        groups[groupKey] = [];
      }
      groups[groupKey].push(transaction);
    });

    return groups;
  };

  const groupedTransactions = groupTransactionsByDate();

  const handleScroll = ({ nativeEvent }: any) => {
    const { layoutMeasurement, contentOffset, contentSize } = nativeEvent;
    const paddingToBottom = 100;
    const isCloseToBottom =
      layoutMeasurement.height + contentOffset.y >=
      contentSize.height - paddingToBottom;

    if (isCloseToBottom && hasMore && !isLoadingMore) {
      loadMoreTransactions();
    }
  };

  return (
    <ScrollView
      style={styles.container}
      contentContainerStyle={styles.content}
      showsVerticalScrollIndicator={false}
      refreshControl={
        <RefreshControl refreshing={refreshing} onRefresh={onRefresh} />
      }
      onScroll={handleScroll}
      scrollEventThrottle={400}
    >
      <View style={styles.header}>
        <Text style={[styles.title, { color: colors.text }]}>
          Transaction History
        </Text>
      </View>

      {transactions.length === 0 ? (
        <Card padding="lg" style={styles.emptyState}>
          <Text style={styles.emptyIcon}>📋</Text>
          <Text style={[styles.emptyTitle, { color: colors.text }]}>
            No Transactions Yet
          </Text>
          <Text style={[styles.emptyText, { color: colors.textSecondary }]}>
            Your transaction history will appear here
          </Text>
        </Card>
      ) : (
        Object.entries(groupedTransactions).map(([dateLabel, txs]) => (
          <View key={dateLabel} style={styles.dateGroup}>
            <Text style={[styles.dateLabel, { color: colors.textSecondary }]}>
              {dateLabel}
            </Text>
            <Card padding="none">
              {txs.map((transaction, index) => (
                <TransactionItem
                  key={transaction.id}
                  transaction={transaction}
                  onPress={() => router.push(`/transaction/${transaction.id}`)}
                  style={{
                    borderBottomWidth:
                      index < txs.length - 1 ? 1 : 0,
                    borderBottomColor: '#e5e7eb',
                  }}
                />
              ))}
            </Card>
          </View>
        ))
      )}

      {isLoadingMore && (
        <View style={styles.loadingMore}>
          <Text style={[styles.loadingText, { color: colors.textSecondary }]}>
            Loading more...
          </Text>
        </View>
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
  },
  dateGroup: {
    marginBottom: 24,
  },
  dateLabel: {
    fontSize: 13,
    fontWeight: '600',
    textTransform: 'uppercase',
    letterSpacing: 0.5,
    marginBottom: 12,
    marginLeft: 4,
  },
  loadingMore: {
    paddingVertical: 24,
    alignItems: 'center',
  },
  loadingText: {
    fontSize: 14,
  },
});
