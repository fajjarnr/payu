import React, { useCallback } from 'react';
import {
  View,
  StyleSheet,
} from 'react-native';
import { FlashList } from '@shopify/flash-list';
import { useTheme } from '@react-navigation/native';

export interface ListItem {
  id: string;
  component: React.ReactElement;
}

interface FlashListSectionProps {
  items: ListItem[];
  contentContainerStyle?: any;
  showsVerticalScrollIndicator?: boolean;
}

export const FlashListSection: React.FC<FlashListSectionProps> = ({
  items,
  contentContainerStyle,
  showsVerticalScrollIndicator = false,
}) => {
  const { colors } = useTheme();

  const renderItem = useCallback(({ item }: { item: ListItem }) => {
    return item.component;
  }, []);

  const keyExtractor = useCallback((item: ListItem) => item.id, []);

  return (
    <FlashList
      data={items}
      renderItem={renderItem}
      keyExtractor={keyExtractor}
      contentContainerStyle={contentContainerStyle}
      showsVerticalScrollIndicator={showsVerticalScrollIndicator}
      scrollEnabled={false}
    />
  );
};

const styles = StyleSheet.create({
  container: {
    flex: 1,
  },
});
