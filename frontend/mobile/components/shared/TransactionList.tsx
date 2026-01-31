import React, { useCallback, useMemo } from 'react';
import {
  View,
  Text,
  StyleSheet,
  RefreshControl,
  ActivityIndicator,
} from 'react-native';
import { useRouter } from 'expo-router';
import { useTheme } from '@react-navigation/native';
import { FlashList } from '@shopify/flash-list';
import { useInfiniteTransactions, useRefreshTransactions } from '@/src/hooks/useTransactionQuery';
import { TransactionItem } from './TransactionItem';
import { Card } from '@/components/ui/Card';
import { isToday, isYesterday, formatDate } from '@/utils/date';

interface TransactionListProps {
  limit?: number;
  showDateHeaders?: boolean;
  onTransactionPress?: (transactionId: string) => void;
}

export const TransactionList: React.FC<TransactionListProps> = ({
  limit,
  showDateHeaders = true,
  onTransactionPress,
}) => {
  const { colors } = useTheme();
  const router = useRouter();

  const {
    data: transactionsData,
    isLoading: isLoadingTransactions,
    error: transactionsError,
    hasNextPage,
    fetchNextPage,
    isFetchingNextPage,
    refetch,
  } = useInfiniteTransactions({ limit });
  const { refresh } = useRefreshTransactions();

  const [refreshing, setRefreshing] = React.useState(false);

  // Flatten infinite query pages into a single array
  const transactions = transactionsData?.pages.flatMap(page => page.items) ?? [];
  const hasMore = hasNextPage ?? false;

  const onRefresh = useCallback(async () => {
    setRefreshing(true);
    await refetch();
    setRefreshing(false);
  }, [refetch]);

  // Transform data into grouped format with date headers
  const listData = useMemo(() => {
    if (!showDateHeaders) {
      return transactions.map(t => ({ type: 'item' as const, transaction: t }));
    }

    const groups: Array<{ type: 'header'; date: string } | { type: 'item'; transaction: any }> = [];
    const groupedByDate: { [key: string]: any[] } = {};

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

      if (!groupedByDate[groupKey]) {
        groupedByDate[groupKey] = [];
      }
      groupedByDate[groupKey].push(transaction);
    });

    // Convert to flat array with headers
    Object.entries(groupedByDate).forEach(([dateLabel, txs]) => {
      groups.push({ type: 'header', date: dateLabel });
      txs.forEach((transaction) => {
        groups.push({ type: 'item', transaction });
      });
    });

    return groups;
  }, [transactions, showDateHeaders]);

  const handleTransactionPress = useCallback((transactionId: string) => {
    if (onTransactionPress) {
      onTransactionPress(transactionId);
    } else {
      router.push(`/transaction/${transactionId}`);
    }
  }, [onTransactionPress, router]);

  const renderItem = useCallback(({ item, index }: { item: typeof listData[0]; index: number }) => {
    if (item.type === 'header') {
      return (
        <View style={styles.dateHeader}>
          <Text style={[styles.dateLabel, { color: colors.textSecondary }]}>
            {item.date}
          </Text>
        </View>
      );
    }

    const transaction = item.transaction;
    const isLastItem = index === listData.length - 1;

    return (
      <TransactionItem
        transaction={transaction}
        onPress={() => handleTransactionPress(transaction.id)}
        style={{
          borderBottomWidth: isLastItem ? 0 : 1,
          borderBottomColor: '#e5e7eb',
        }}
        testID={`transaction-item-${transaction.id}`}
      />
    );
  }, [colors.textSecondary, handleTransactionPress, listData.length]);

  const getItemType = useCallback((item: typeof listData[0]) => {
    return item.type === 'header' ? 'dateHeader' : 'transactionItem';
  }, []);

  const ListHeaderComponent = useCallback(() => (
    null
  ), []);

  const ListFooterComponent = useCallback(() => (
    <>
      {isFetchingNextPage && (
        <View style={styles.loadingMore}>
          <ActivityIndicator size="small" color={colors.textSecondary} />
          <Text style={[styles.loadingText, { color: colors.textSecondary }]}>
            Loading more...
          </Text>
        </View>
      )}
    </>
  ), [isFetchingNextPage, colors.textSecondary]);

  const EmptyComponent = useCallback(() => (
    <Card padding="lg" style={styles.emptyState}>
      <Text style={styles.emptyIcon}>📋</Text>
      <Text style={[styles.emptyTitle, { color: colors.text }]}>
        No Transactions Yet
      </Text>
      <Text style={[styles.emptyText, { color: colors.textSecondary }]}>
        Your transaction history will appear here
      </Text>
    </Card>
  ), [colors.text, colors.textSecondary]);

  const LoadingComponent = useCallback(() => (
    <View style={styles.loadingContainer}>
      <ActivityIndicator size="large" color="#10b981" />
      <Text style={[styles.loadingText, { color: colors.textSecondary }]}>
        Loading transactions...
      </Text>
    </View>
  ), [colors.textSecondary]);

  if (isLoadingTransactions) {
    return LoadingComponent();
  }

  if (transactions.length === 0) {
    return EmptyComponent();
  }

  return (
    <FlashList
      data={listData}
      renderItem={renderItem}
      keyExtractor={useCallback((item: typeof listData[0], index: number) => {
        if (item.type === 'header') {
          return `header-${item.date}-${index}`;
        }
        return `transaction-${item.transaction.id}`;
      }, [])}
      getItemType={getItemType}
      refreshControl={
        <RefreshControl refreshing={refreshing} onRefresh={onRefresh} />
      }
      onEndReached={() => {
        if (hasMore && !isFetchingNextPage && !limit) {
          fetchNextPage();
        }
      }}
      onEndReachedThreshold={0.5}
      ListHeaderComponent={ListHeaderComponent}
      ListFooterComponent={ListFooterComponent}
      showsVerticalScrollIndicator={false}
    />
  );
};

const styles = StyleSheet.create({
  dateHeader: {
    paddingTop: 8,
    paddingBottom: 8,
    backgroundColor: '#f9fafb',
  },
  dateLabel: {
    fontSize: 13,
    fontWeight: '600',
    textTransform: 'uppercase',
    letterSpacing: 0.5,
    marginLeft: 4,
  },
  loadingMore: {
    paddingVertical: 24,
    alignItems: 'center',
    flexDirection: 'row',
    justifyContent: 'center',
    gap: 8,
  },
  loadingText: {
    fontSize: 14,
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
  loadingContainer: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
    paddingVertical: 48,
  },
});
