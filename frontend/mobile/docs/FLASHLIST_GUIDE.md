# FlashList Implementation Guide for PayU Mobile App

## Overview
FlashList has been implemented to replace ScrollView + map for long lists in the PayU mobile app to improve performance and memory efficiency.

## What is FlashList?

FlashList is a performant list component from Shopify that:
- Recycles list items to reduce memory usage
- Provides smooth scrolling for large datasets
- Supports pull-to-refresh and infinite scrolling
- Better than FlatList for most use cases

## Installation

```bash
npm install @shopify/flash-list
```

## Usage Examples

### 1. Transaction History (app/(tabs)/history.tsx)

The main transaction history screen now uses FlashList with:
- **Date grouping**: Transactions are grouped by date headers
- **Pull-to-refresh**: Native RefreshControl integration
- **Infinite scroll**: Automatic pagination with onEndReached
- **Memoized items**: TransactionItem is memoized for performance

Key features:
```typescript
<FlashList
  data={listData}
  renderItem={renderItem}
  keyExtractor={keyExtractor}
  getItemType={getItemType}  // Important for performance with mixed item types
  estimatedItemSize={80}
  refreshControl={<RefreshControl refreshing={refreshing} onRefresh={onRefresh} />}
  onEndReached={fetchNextPage}
  onEndReachedThreshold={0.5}
/>
```

### 2. TransactionItem Component (components/shared/TransactionItem.tsx)

The TransactionItem component is now memoized to prevent unnecessary re-renders:

```typescript
export const TransactionItem = React.memo(TransactionItemComponent, (prevProps, nextProps) => {
  return (
    prevProps.transaction.id === nextProps.transaction.id &&
    prevProps.transaction.status === nextProps.transaction.status &&
    prevProps.transaction.amount === nextProps.transaction.amount &&
    prevProps.onPress === nextProps.onPress
  );
});
```

### 3. Reusable TransactionList Component

For consistency across the app, use the `TransactionList` component:

```typescript
import { TransactionList } from '@/components/shared/TransactionList';

<TransactionList
  limit={10}  // Optional: limit number of items
  showDateHeaders={true}  // Optional: show date grouping
  onTransactionPress={(id) => console.log('Pressed', id)}  // Optional: custom press handler
/>
```

### 4. Generic FlashListSection

For simple lists with custom components:

```typescript
import { FlashListSection } from '@/components/ui/FlashListSection';

const items = [
  { id: '1', component: <CustomComponent1 /> },
  { id: '2', component: <CustomComponent2 /> },
];

<FlashListSection
  items={items}
  estimatedItemSize={60}
/>
```

## Performance Best Practices

### 1. Always use `getItemType` for heterogeneous lists

When your list has different item types (headers, items, separators), use `getItemType`:

```typescript
const getItemType = useCallback((item: Item) => {
  if (item.type === 'header') return 'header';
  return 'item';
}, []);
```

### 2. Memoize render items

Use `React.memo` for list items and `useCallback` for render functions:

```typescript
const MemoizedItem = React.memo(ItemComponent);

const renderItem = useCallback(({ item }) => (
  <MemoizedItem data={item} />
), []);
```

### 3. Set appropriate `estimatedItemSize`

Set a reasonable estimated item size close to your actual item height:

```typescript
// Measure your average item height
estimatedItemSize={80}  // Adjust based on your design
```

### 4. Use stable keyExtractor

Ensure keys are unique and stable across re-renders:

```typescript
const keyExtractor = useCallback((item, index) => {
  return item.id || `item-${index}`;
}, []);
```

### 5. Optimize list item props comparison

When using React.memo, only re-render when relevant props change:

```typescript
React.memo(Component, (prev, next) => {
  return prev.id === next.id && prev.status === next.status;
});
```

## When to Use FlashList

### Use FlashList for:
- Transaction history (100+ items)
- Lists with pagination/infinite scroll
- Search results
- Settings with many items
- Any list with 20+ items

### Use ScrollView for:
- Forms with < 10 items
- Short static content
- Nested lists (FlashList inside ScrollView is not recommended)
- Quick Actions (4 items)

### Use map for:
- Very small lists (< 10 items)
- Static grids (like category selection)
- Lists that don't scroll

## Migration Checklist

When migrating from ScrollView + map to FlashList:

- [ ] Install @shopify/flash-list
- [ ] Flatten grouped data into a single array
- [ ] Create renderItem function
- [ ] Add keyExtractor
- [ ] Set estimatedItemSize
- [ ] Add getItemType if mixed item types
- [ ] Memoize list items with React.memo
- [ ] Test pull-to-refresh
- [ ] Test infinite scroll pagination
- [ ] Verify accessibility (a11y labels)
- [ ] Test on both iOS and Android

## Common Issues & Solutions

### Issue: Items overlapping or incorrect spacing
**Solution**: Adjust `estimatedItemSize` or use `overrideItemLayout`

```typescript
overrideItemLayout={(layout, item) => {
  if (item.type === 'header') {
    layout.size = 40;
  }
}}
```

### Issue: FlashList inside ScrollView doesn't work
**Solution**: Don't nest FlashList in ScrollView. Use parent FlashList with ListHeaderComponent.

### Issue: Performance not improved
**Solution**: Ensure items are memoized and getItemType is used for mixed lists.

## Testing

Test FlashList implementations with:
- Large datasets (1000+ items)
- Rapid scrolling
- Pull-to-refresh
- Infinite scroll pagination
- Orientation changes
- Different screen sizes

## Further Reading

- [FlashList Documentation](https://shopify.github.io/flash-list/)
- [React Native Performance](https://reactnative.dev/docs/performance)
- [useMemo and useCallback](https://react.dev/reference/react/useMemo)
