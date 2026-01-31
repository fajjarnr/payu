import React, { useCallback, useMemo, memo } from 'react';
import {
  View,
  StyleSheet,
  ViewStyle,
} from 'react-native';
import { FlashList } from '@shopify/flash-list';
import { useTheme } from '@react-navigation/native';

export interface ListItem {
  id: string;
  component: React.ReactElement;
}

interface FlashListSectionProps {
  items: ListItem[];
  contentContainerStyle?: ViewStyle;
  showsVerticalScrollIndicator?: boolean;
  estimatedItemSize?: number;
}

// Default estimated item size for FlashList optimization
const DEFAULT_ESTIMATED_ITEM_SIZE = 60;

export const FlashListSectionComponent: React.FC<FlashListSectionProps> = ({
  items,
  contentContainerStyle,
  showsVerticalScrollIndicator = false,
  estimatedItemSize = DEFAULT_ESTIMATED_ITEM_SIZE,
}) => {
  const { colors } = useTheme();

  // Performance: Memoize render item callback
  const renderItem = useCallback(({ item }: { item: ListItem }) => {
    return item.component;
  }, []);

  // Performance: Memoize key extractor
  const keyExtractor = useCallback((item: ListItem) => item.id, []);

  // Performance: Memoize list style
  const listStyle = useMemo<ViewStyle>(() => ({
    backgroundColor: colors.background,
  }), [colors.background]);

  return (
    <FlashList
      data={items}
      renderItem={renderItem}
      keyExtractor={keyExtractor}
      contentContainerStyle={contentContainerStyle}
      showsVerticalScrollIndicator={showsVerticalScrollIndicator}
      scrollEnabled={false}
      estimatedItemSize={estimatedItemSize}
      style={listStyle}
    />
  );
};

// Performance: Memoize FlashListSection component to prevent unnecessary re-renders
export const FlashListSection = memo(FlashListSectionComponent, (prevProps, nextProps) => {
  return (
    prevProps.items === nextProps.items &&
    prevProps.showsVerticalScrollIndicator === nextProps.showsVerticalScrollIndicator
  );
});

FlashListSection.displayName = 'FlashListSection';

const styles = StyleSheet.create({
  container: {
    flex: 1,
  },
});
