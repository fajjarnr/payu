import React from 'react';
import { View, StyleSheet, TouchableOpacity, Text } from 'react-native';
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

interface QuickActionsProps {
  onActionPress?: (action: QuickAction) => void;
}

export const QuickActions: React.FC<QuickActionsProps> = ({ onActionPress }) => {
  const { colors } = useTheme();
  const navigation = useNavigation();

  const handlePress = (action: QuickAction) => {
    if (onActionPress) {
      onActionPress(action);
    } else {
      // @ts-ignore - navigation type
      navigation.navigate(action.route);
    }
  };

  return (
    <View style={styles.container}>
      {quickActions.map((action) => (
        <TouchableOpacity
          key={action.id}
          style={[
            styles.actionItem,
            {
              backgroundColor: colors.card,
            },
          ]}
          onPress={() => handlePress(action)}
          activeOpacity={0.7}
        >
          <View
            style={[
              styles.iconContainer,
              {
                backgroundColor: `${action.color}20`,
              },
            ]}
          >
            <Text style={styles.icon}>{action.icon}</Text>
          </View>
          <Text style={[styles.label, { color: colors.text }]}>{action.label}</Text>
        </TouchableOpacity>
      ))}
    </View>
  );
};

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
