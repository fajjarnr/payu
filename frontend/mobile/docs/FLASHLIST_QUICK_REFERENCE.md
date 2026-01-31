# FlashList Quick Reference for PayU Mobile Developers

## Quick Start

### 1. Basic FlashList

```typescript
import { FlashList } from '@shopify/flash-list';

<FlashList
  data={items}
  renderItem={({ item }) => <MyItem data={item} />}
  keyExtractor={(item) => item.id}
  estimatedItemSize={80}
/>
```

### 2. With Pull-to-Refresh

```typescript
import { RefreshControl } from 'react-native';

const [refreshing, setRefreshing] = useState(false);

<FlashList
  data={items}
  renderItem={renderItem}
  keyExtractor={keyExtractor}
  estimatedItemSize={80}
  refreshControl={
    <RefreshControl refreshing={refreshing} onRefresh={onRefresh} />
  }
/>
```

### 3. With Infinite Scroll

```typescript
<FlashList
  data={items}
  renderItem={renderItem}
  keyExtractor={keyExtractor}
  estimatedItemSize={80}
  onEndReached={() => hasMore && fetchNextPage()}
  onEndReachedThreshold={0.5}
  ListFooterComponent={() => isFetching ? <Loader /> : null}
/>
```

### 4. With Multiple Item Types

```typescript
const data = [
  { type: 'header', title: 'Today' },
  { type: 'item', id: 1, name: 'Transaction 1' },
  { type: 'item', id: 2, name: 'Transaction 2' },
];

const renderItem = ({ item }) => {
  if (item.type === 'header') {
    return <Header title={item.title} />;
  }
  return <TransactionItem data={item} />;
};

const getItemType = (item) => {
  return item.type; // 'header' or 'item'
};

<FlashList
  data={data}
  renderItem={renderItem}
  keyExtractor={(item, index) => item.type + index}
  getItemType={getItemType}
  estimatedItemSize={80}
/>
```

### 5. Grid Layout

```typescript
<FlashList
  data={categories}
  renderItem={renderItem}
  keyExtractor={(item) => item.id}
  estimatedItemSize={120}
  numColumns={2}
  scrollEnabled={false}
/>
```

## Performance Tips

### 1. Memoize Render Functions

```typescript
// Good
const renderItem = useCallback(({ item }) => (
  <MemoizedItem data={item} />
), []);

// Bad
const renderItem = ({ item }) => (
  <Item data={item} />
);
```

### 2. Memoize List Items

```typescript
const MyItem = React.memo(({ data }) => {
  return <Text>{data.name}</Text>;
}, (prev, next) => prev.data.id === next.data.id);
```

### 3. Use Stable Keys

```typescript
// Good
keyExtractor={(item) => item.id}

// Bad - index as key
keyExtractor={(item, index) => index}
```

### 4. Set Correct Estimated Size

```typescript
// Measure your average item height
<FlashList estimatedItemSize={80} />

// Or override per item
overrideItemLayout={(layout, item) => {
  if (item.type === 'header') layout.size = 40;
}}
```

## Common Patterns in PayU

### Pattern 1: Grouped List with Headers

```typescript
// Transform grouped data to flat array
const listData = useMemo(() => {
  const result = [];
  Object.entries(groupedData).forEach(([key, items]) => {
    result.push({ type: 'header', key });
    items.forEach(item => result.push({ type: 'item', data: item }));
  });
  return result;
}, [groupedData]);
```

### Pattern 2: Memoized Item Component

```typescript
export const TransactionItem = React.memo(
  TransactionItemComponent,
  (prev, next) => {
    return prev.transaction.id === next.transaction.id &&
           prev.transaction.status === next.transaction.status;
  }
);
```

### Pattern 3: Custom Empty/Loading States

```typescript
const ListEmptyComponent = () => (
  <View style={styles.empty}>
    <Text>No data</Text>
  </View>
);

const ListFooterComponent = () => (
  isFetching ? <ActivityIndicator /> : null
);

<FlashList
  ListEmptyComponent={ListEmptyComponent}
  ListFooterComponent={ListFooterComponent}
/>
```

## Troubleshooting

### Items Overlapping
- Adjust `estimatedItemSize`
- Use `overrideItemLayout` for variable heights

### Poor Performance
- Add `getItemType` for mixed lists
- Memoize items with `React.memo`
- Use `useCallback` for renderItem

### Wrong Scroll Position
- Ensure `estimatedItemSize` is accurate
- Check for nested ScrollViews (not recommended)

### Items Not Visible
- Set `estimatedItemSize` if items have zero height
- Check parent container has height

## When NOT to Use FlashList

- Short lists (< 10 items): Use `map()`
- Static forms: Use `ScrollView`
- Nested lists: Use parent FlashList with ListHeaderComponent
- Horizontal carousels: Use `ScrollView` with `horizontal` prop

## Code Snippets

### Import Statement
```typescript
import { FlashList } from '@shopify/flash-list';
```

### TypeScript Types
```typescript
import type { ListRenderItemInfo } from 'react-native';

const renderItem = ({ item, index }: ListRenderItemInfo<ItemType>) => {
  // ...
};
```

### Extract Key Extractor
```typescript
const keyExtractor = useCallback((item: ItemType) => {
  return item.id;
}, []);
```

## Related Files in PayU

- `/home/ubuntu/payu/frontend/mobile/app/(tabs)/history.tsx` - Main implementation
- `/home/ubuntu/payu/frontend/mobile/components/shared/TransactionList.tsx` - Reusable component
- `/home/ubuntu/payu/frontend/mobile/components/ui/FlashListSection.tsx` - Generic wrapper
- `/home/ubuntu/payu/frontend/mobile/docs/FLASHLIST_GUIDE.md` - Full guide

## Support

- [FlashList GitHub](https://github.com/Shopify/flash-list)
- [FlashList Docs](https://shopify.github.io/flash-list/)
- Internal: Check `/docs/FLASHLIST_GUIDE.md` for detailed info
