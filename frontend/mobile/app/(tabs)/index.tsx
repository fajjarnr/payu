import React, { useState, useCallback, useMemo, useRef } from 'react';
import {
  View,
  Text,
  StyleSheet,
  RefreshControl,
  TouchableOpacity,
} from 'react-native';
import { useRouter } from 'expo-router';
import { useTheme } from '@react-navigation/native';
import { FlashList, ListRenderItem } from '@shopify/flash-list';
import { usePrimaryWallet, useRefreshWallets } from '@/src/hooks/useWalletQuery';
import { useInfiniteTransactions, useRefreshTransactions } from '@/src/hooks/useTransactionQuery';
import { BalanceCard } from '@/components/shared/BalanceCard';
import { QuickActions } from '@/components/shared/QuickActions';
import { TransactionItem } from '@/components/shared/TransactionItem';
import { Card } from '@/components/ui/Card';
import { Transaction } from '@/types';

export default function HomeScreen() {
  const router = useRouter();
  const { colors } = useTheme();
  const isMountedRef = useRef(true);
  const { data: wallet, isLoading: isLoadingWallet, error: walletError } = usePrimaryWallet();
  const { data: transactionsData, isLoading: isLoadingTransactions, error: transactionsError, hasNextPage, fetchNextPage, isFetchingNextPage, refetch } = useInfiniteTransactions();
  const { refreshPrimary } = useRefreshWallets();
  const { refresh } = useRefreshTransactions();

  const [showBalance, setShowBalance] = useState(true);
  const [refreshing, setRefreshing] = useState(false);

  // Memoize balance calculation
  const balance = useMemo(() => wallet?.balance ?? 0, [wallet?.balance]);

  // Flatten infinite query pages into a single array (memoized)
  const transactions = useMemo(
    () => transactionsData?.pages.flatMap(page => page.items) ?? [],
    [transactionsData]
  );

  // Display transactions (limited to 5, memoized)
  const displayTransactions = useMemo(
    () => transactions.slice(0, 5),
    [transactions]
  );

  // Memoize transaction press callback factory - MUST be defined before renderTransactionItem
  const handleTransactionPress = useCallback((transactionId: string) => {
    return () => router.push(`/transaction/${transactionId}`);
  }, [router]);

  // Render item for FlashList
  const renderTransactionItem: ListRenderItem<Transaction> = useCallback(({ item }) => (
    <TransactionItem
      transaction={item}
      onPress={handleTransactionPress(item.id)}
      style={{ marginBottom: 12 }}
    />
  ), [handleTransactionPress]);

  // Memoize onRefresh with proper cleanup
  const onRefresh = useCallback(async () => {
    if (!isMountedRef.current) return;

    setRefreshing(true);
    try {
      await Promise.all([refetch(), refreshPrimary()]);
    } catch (error) {
      console.error('Refresh failed:', error);
    } finally {
      if (isMountedRef.current) {
        setRefreshing(false);
      }
    }
  }, [refetch, refreshPrimary]);

  // Memoize load more function
  const loadMore = useCallback(() => {
    if (hasNextPage && !isFetchingNextPage && isMountedRef.current) {
      fetchNextPage();
    }
  }, [hasNextPage, isFetchingNextPage, fetchNextPage]);

  // Memoize toggle balance callback
  const handleToggleBalance = useCallback(() => {
    setShowBalance(prev => !prev);
  }, []);

  // Memoize notification press callback
  const handleNotificationPress = useCallback(() => {
    router.push('/notifications');
  }, [router]);

  // Memoize action press callback
  const handleActionPress = useCallback((action: any) => {
    router.push(action.route);
  }, [router]);

  // Memoize see all press callback
  const handleSeeAllPress = useCallback(() => {
    router.push('/(tabs)/history');
  }, [router]);

  // Cleanup on unmount
  React.useEffect(() => {
    return () => {
      isMountedRef.current = false;
    };
  }, []);

  const ListHeaderComponent = useCallback(() => (
    <>
      {/* Header */}
      <View style={styles.header}>
        <View>
          <Text style={[styles.greeting, { color: colors.text }]}>
            Good Morning 👋
          </Text>
          <Text style={[styles.subtitle, { color: colors.textSecondary }]}>
            Manage your finances easily
          </Text>
        </View>
        <TouchableOpacity
          style={styles.notificationButton}
          onPress={handleNotificationPress}
        >
          <Text style={styles.notificationIcon}>🔔</Text>
        </TouchableOpacity>
      </View>

      {/* Balance Card */}
      <BalanceCard
        balance={balance}
        accountNumber="•••• 1234"
        showBalance={showBalance}
        onToggleBalance={handleToggleBalance}
        style={styles.balanceCard}
      />

      {/* Quick Actions */}
      <View style={styles.section}>
        <Text style={[styles.sectionTitle, { color: colors.text }]}>
          Quick Actions
        </Text>
        <QuickActions
          onActionPress={handleActionPress}
        />
      </View>

      {/* Recent Transactions Header */}
      <View style={styles.sectionHeader}>
        <Text style={[styles.sectionTitle, { color: colors.text }]}>
          Recent Transactions
        </Text>
        <TouchableOpacity onPress={handleSeeAllPress}>
          <Text style={styles.seeAll}>See All</Text>
        </TouchableOpacity>
      </View>

      {isLoadingTransactions ? (
        <Card padding="lg">
          <Text style={[styles.emptyText, { color: colors.textSecondary }]}>
            Loading transactions...
          </Text>
        </Card>
      ) : transactions.length === 0 ? (
        <Card padding="lg">
          <Text style={[styles.emptyText, { color: colors.textSecondary }]}>
            No transactions yet
          </Text>
        </Card>
      ) : null}
    </>
  ), [colors.text, colors.textSecondary, balance, showBalance, handleToggleBalance, handleNotificationPress, handleActionPress, handleSeeAllPress, isLoadingTransactions, transactions.length]);

  const ListFooterComponent = useCallback(() => (
    hasNextPage ? (
      <TouchableOpacity
        style={styles.loadMoreButton}
        onPress={loadMore}
      >
        <Text style={styles.loadMoreText}>Load More</Text>
      </TouchableOpacity>
    ) : null
  ), [hasNextPage, loadMore]);

  // Show loading or empty state in a ScrollView
  if (isLoadingTransactions || transactions.length === 0) {
    return (
      <FlashList
        data={[]}
        renderItem={() => null}
        keyExtractor={() => 'empty'}
        ListHeaderComponent={ListHeaderComponent}
        contentContainerStyle={styles.listContent}
        showsVerticalScrollIndicator={false}
        refreshControl={
          <RefreshControl refreshing={refreshing} onRefresh={onRefresh} />
        }
      />
    );
  }

  return (
    <FlashList
      data={displayTransactions}
      renderItem={renderTransactionItem}
      keyExtractor={useCallback((item: Transaction) => `transaction-${item.id}`, [])}
      ListHeaderComponent={ListHeaderComponent}
      ListFooterComponent={ListFooterComponent}
      contentContainerStyle={styles.listContent}
      showsVerticalScrollIndicator={false}
      refreshControl={
        <RefreshControl refreshing={refreshing} onRefresh={onRefresh} />
      }
    />
  );
}

const styles = StyleSheet.create({
  listContent: {
    padding: 20,
  },
  header: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginBottom: 24,
  },
  greeting: {
    fontSize: 24,
    fontWeight: '900',
    letterSpacing: -1,
  },
  subtitle: {
    fontSize: 14,
    marginTop: 4,
  },
  notificationButton: {
    width: 44,
    height: 44,
    borderRadius: 22,
    backgroundColor: '#ffffff',
    justifyContent: 'center',
    alignItems: 'center',
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 2 },
    shadowOpacity: 0.1,
    shadowRadius: 4,
    elevation: 3,
  },
  notificationIcon: {
    fontSize: 20,
  },
  balanceCard: {
    marginBottom: 32,
  },
  section: {
    marginBottom: 32,
  },
  sectionHeader: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginBottom: 16,
  },
  sectionTitle: {
    fontSize: 18,
    fontWeight: '700',
  },
  seeAll: {
    fontSize: 14,
    fontWeight: '600',
    color: '#10b981',
  },
  emptyText: {
    fontSize: 14,
    textAlign: 'center',
  },
  loadMoreButton: {
    paddingVertical: 16,
    alignItems: 'center',
    marginTop: 8,
  },
  loadMoreText: {
    fontSize: 14,
    fontWeight: '600',
    color: '#10b981',
  },
});
