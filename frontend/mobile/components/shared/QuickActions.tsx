import React, { useCallback, useMemo, memo } from 'react';
import { View, StyleSheet, TouchableOpacity, Text, ViewStyle } from 'react-native';
import { useTheme } from '@react-navigation/native';
import { useNavigation } from 'expo-router';

interface QuickAction {
  id: string;
  label: string;
  icon: string;
  route: string;
  color: string;
}

const quickActions: QuickAction[] = [
  { id: '1', label: 'Transfer', icon: '💸', route: '/(tabs)/transfers', color: '#10b981' },
  { id: '2', label: 'QRIS', icon: '📱', route: '/qris', color: '#3b82f6' },
  { id: '3', label: 'Top Up', icon: '➕', route: '/topup', color: '#f59e0b' },
  { id: '4', label: 'Pay', icon: '💳', route: '/pay', color: '#8b5cf6' },
];

// Performance: Memoize accessibility hints to avoid recreation on every render
const getA11yHint = (label: string): string => {
  const hints: Record<string, string> = {
    'Transfer': 'Opens transfer screen to send money',
    'QRIS': 'Opens QRIS scanner for payments',
    'Top Up': 'Opens top up options for adding balance',
    'Pay': 'Opens payment options',
  };
  return hints[label] || `Opens ${label} screen`;
};

interface QuickActionsProps {
  onActionPress?: (action: QuickAction) => void;
}

// Individual action item component with memoization for list performance
interface QuickActionItemProps {
  action: QuickAction;
  cardColor: string;
  textColor: string;
  onPress: (action: QuickAction) => void;
}

const QuickActionItemComponent: React.FC<QuickActionItemProps> = ({
  action,
  cardColor,
  textColor,
  onPress,
}) => {
  // Memoize icon container style
  const iconContainerStyle = useMemo<ViewStyle>(() => ({
    backgroundColor: `${action.color}20`,
  }), [action.color]);

  const handlePress = useCallback(() => {
    onPress(action);
  }, [action, onPress]);

  return (
    <TouchableOpacity
      style={[styles.actionItem, { backgroundColor: cardColor }]}
      onPress={handlePress}
      activeOpacity={0.7}
      accessibilityLabel={action.label}
      accessibilityHint={getA11yHint(action.label)}
      accessibilityRole="menuitem"
      accessibilityState={{ selected: false }}
    >
      <View
        style={[styles.iconContainer, iconContainerStyle]}
        accessible={false}
        importantForAccessibility="no"
      >
        <Text style={styles.icon} accessible={false}>{action.icon}</Text>
      </View>
      <Text style={[styles.label, { color: textColor }]} accessible={false}>
        {action.label}
      </Text>
    </TouchableOpacity>
  );
};

const QuickActionItem = memo(QuickActionItemComponent, (prevProps, nextProps) => {
  return (
    prevProps.action.id === nextProps.action.id &&
    prevProps.cardColor === nextProps.cardColor &&
    prevProps.textColor === nextProps.textColor &&
    prevProps.onPress === nextProps.onPress
  );
});

QuickActionItem.displayName = 'QuickActionItem';

export const QuickActionsComponent: React.FC<QuickActionsProps> = ({ onActionPress }) => {
  const { colors } = useTheme();
  const navigation = useNavigation();

  // Performance: Memoize press handler to avoid recreating on every render
  const handleActionPress = useCallback((action: QuickAction) => {
    if (onActionPress) {
      onActionPress(action);
    } else {
      // @ts-ignore - navigation type
      navigation.navigate(action.route);
    }
  }, [onActionPress, navigation]);

  return (
    <View style={styles.container} accessibilityRole="menu">
      {quickActions.map((action) => (
        <QuickActionItem
          key={action.id}
          action={action}
          cardColor={colors.card}
          textColor={colors.text}
          onPress={handleActionPress}
        />
      ))}
    </View>
  );
};

// Performance: Memoize QuickActions component to prevent unnecessary re-renders
export const QuickActions = memo(QuickActionsComponent, (prevProps, nextProps) => {
  return prevProps.onActionPress === nextProps.onActionPress;
});

QuickActions.displayName = 'QuickActions';

const styles = StyleSheet.create({
  container: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    paddingHorizontal: 4,
  },
  actionItem: {
    alignItems: 'center',
    flex: 1,
    paddingVertical: 12,
  },
  iconContainer: {
    width: 56,
    height: 56,
    borderRadius: 16,
    justifyContent: 'center',
    alignItems: 'center',
    marginBottom: 8,
  },
  icon: {
    fontSize: 24,
  },
  label: {
    fontSize: 12,
    fontWeight: '600',
  },
});
