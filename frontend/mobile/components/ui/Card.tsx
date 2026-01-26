import React from 'react';
import { View, StyleSheet, ViewStyle } from 'react-native';
import { useTheme } from '@react-navigation/native';

interface CardProps {
  children: React.ReactNode;
  variant?: 'elevated' | 'outlined' | 'flat';
  padding?: 'none' | 'sm' | 'md' | 'lg';
  className?: string;
  style?: ViewStyle;
}

export const Card: React.FC<CardProps> = ({
  children,
  variant = 'elevated',
  padding = 'md',
  className,
  style,
}) => {
  const { colors } = useTheme();

  const getPadding = () => {
    switch (padding) {
      case 'none':
        return 0;
      case 'sm':
        return 12;
      case 'md':
        return 16;
      case 'lg':
        return 24;
      default:
        return 16;
    }
  };

  const cardStyle: ViewStyle = {
    backgroundColor: colors.card,
    borderRadius: 20,
    padding: getPadding(),
    ...(variant === 'elevated' && {
      shadowColor: '#000',
      shadowOffset: { width: 0, height: 2 },
      shadowOpacity: 0.1,
      shadowRadius: 4,
      elevation: 4,
    }),
    ...(variant === 'outlined' && {
      borderWidth: 1,
      borderColor: colors.border,
    }),
    ...style,
  };

  return <View style={cardStyle}>{children}</View>;
};
