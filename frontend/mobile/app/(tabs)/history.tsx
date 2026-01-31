import React, { useCallback, useMemo, useRef } from 'react';
import {
  View,
  Text,
  StyleSheet,
  RefreshControl,
} from 'react-native';
import { useRouter } from 'expo-router';
import { useTheme } from '@react-navigation/native';
import { FlashList, ListRenderItem } from '@shopify/flash-list';
import { useInfiniteTransactions } from '@/src/hooks/useTransactionQuery';
import { TransactionItem } from '@/components/shared/TransactionItem';
import { Card } from '@/components/ui/Card';
import { isToday, isYesterday, formatDate } from '@/utils/date';

// Estimated height for transaction items (optimizes FlashList layout)
const ESTIMATED_ITEM_HEIGHT = 80;

export default function HistoryScreen() {
  const router = useRouter();
  const { colors } = useTheme();
  const isMountedRef = useRef(true);
  const {
    data: transactionsData,
    isLoading: isLoadingTransactions,
    hasNextPage,
    fetchNextPage,
    isFetchingNextPage,
    refetch,
  } = useInfiniteTransactions();

  const [refreshing, setRefreshing] = React.useState(false);

  // Flatten infinite query pages into a single array (memoized)
  const transactions = useMemo(
    () => transactionsData?.pages.flatMap(page => page.items) ?? [],
    [transactionsData]
  );
  const hasMore = hasNextPage ?? false;

  // Memoize onRefresh with mount check
  const onRefresh = useCallback(async () => {
    if (!isMountedRef.current) return;

    setRefreshing(true);
    try {
      await refetch();
    } finally {
      if (isMountedRef.current) {
        setRefreshing(false);
      }
    }
  }, [refetch]);

  // Cleanup on unmount
  React.useEffect(() => {
    return () => {
      isMountedRef.current = false;
    };
  }, []);

  // Transform data into grouped format with date headers
  const listData = useMemo(() => {
    const groups: ({ type: 'header'; date: string } | { type: 'item'; transaction: any })[] = [];

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
  }, [transactions]);

  const renderItem: ListRenderItem<typeof listData[0]> = useCallback(({ item, index }) => {
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
        onPress={() => router.push(`/transaction/${transaction.id}`)}
        style={{
          borderBottomWidth: isLastItem ? 0 : 1,
          borderBottomColor: '#e5e7eb',
        }}
        testID={`transaction-item-${transaction.id}`}
      />
    );
  }, [colors.textSecondary, router, listData.length]);

  const getItemType = useCallback((item: typeof listData[0]) => {
    return item.type === 'header' ? 'dateHeader' : 'transactionItem';
  }, []);

  const ListHeaderComponent = useCallback(() => (
    <View style={styles.header}>
      <Text style={[styles.title, { color: colors.text }]}>
        Transaction History
      </Text>
    </View>
  ), [colors.text]);

  const ListFooterComponent = useCallback(() => (
    <>
      {isFetchingNextPage && (
        <View style={styles.loadingMore}>
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
    <Card padding="lg" style={styles.emptyState}>
      <Text style={styles.emptyIcon}>⏳</Text>
      <Text style={[styles.emptyTitle, { color: colors.text }]}>
        Loading Transactions
      </Text>
      <Text style={[styles.emptyText, { color: colors.textSecondary }]}>
        Please wait while we fetch your transactions
      </Text>
    </Card>
  ), [colors.text, colors.textSecondary]);

  // Memoize key extractor for FlashList
  const keyExtractor = useCallback((item: typeof listData[0], index: number) => {
    if (item.type === 'header') {
      return `header-${item.date}-${index}`;
    }
    return `transaction-${item.transaction.id}`;
  }, []);

  // Memoize end reached handler
  const handleEndReached = useCallback(() => {
    if (hasMore && !isFetchingNextPage && isMountedRef.current) {
      fetchNextPage();
    }
  }, [hasMore, isFetchingNextPage, fetchNextPage]);

  // Show loading state separately
  if (isLoadingTransactions) {
    return (
      <View style={styles.container}>
        <View style={styles.content}>
          {ListHeaderComponent()}
          {LoadingComponent()}
        </View>
      </View>
    );
  }

  // Show empty state when there's no data
  if (transactions.length === 0) {
    return (
      <View style={styles.container}>
        <View style={styles.content}>
          {ListHeaderComponent()}
          {EmptyComponent()}
        </View>
      </View>
    );
  }

  return (
    <FlashList
      data={listData}
      renderItem={renderItem}
      keyExtractor={keyExtractor}
      getItemType={getItemType}
      refreshControl={
        <RefreshControl refreshing={refreshing} onRefresh={onRefresh} />
      }
      onEndReached={handleEndReached}
      onEndReachedThreshold={0.5}
      ListHeaderComponent={ListHeaderComponent}
      ListFooterComponent={ListFooterComponent}
      contentContainerStyle={styles.listContent}
      showsVerticalScrollIndicator={false}
      estimatedItemSize={ESTIMATED_ITEM_HEIGHT}
    />
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
  listContent: {
    paddingHorizontal: 20,
    paddingTop: 20,
    paddingBottom: 20,
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
  },
  loadingText: {
    fontSize: 14,
  },
});
