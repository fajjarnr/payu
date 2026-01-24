import React from 'react';
import { View, Text, StyleSheet, ViewStyle } from 'react-native';

interface BadgeProps {
  text: string;
  variant?: 'success' | 'warning' | 'error' | 'info';
  size?: 'sm' | 'md';
  style?: ViewStyle;
}

export const Badge: React.FC<BadgeProps> = ({
  text,
  variant = 'info',
  size = 'sm',
  style,
}) => {
  const getBackgroundColor = () => {
    switch (variant) {
      case 'success':
        return '#d1fae5';
      case 'warning':
        return '#fef3c7';
      case 'error':
        return '#fee2e2';
      case 'info':
        return '#dbeafe';
      default:
        return '#dbeafe';
    }
  };

  const getTextColor = () => {
    switch (variant) {
      case 'success':
        return '#065f46';
      case 'warning':
        return '#92400e';
      case 'error':
        return '#991b1b';
      case 'info':
        return '#1e40af';
      default:
        return '#1e40af';
    }
  };

  const badgeStyle: ViewStyle = {
    backgroundColor: getBackgroundColor(),
    paddingHorizontal: size === 'sm' ? 8 : 12,
    paddingVertical: size === 'sm' ? 4 : 6,
    borderRadius: 8,
    alignSelf: 'flex-start',
    ...style,
  };

  const textStyle = {
    color: getTextColor(),
    fontSize: size === 'sm' ? 12 : 14,
    fontWeight: '600',
  };

  return (
    <View style={badgeStyle}>
      <Text style={textStyle}>{text}</Text>
    </View>
  );
};
