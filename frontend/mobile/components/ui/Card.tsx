import React, { useMemo, memo } from 'react';
import { View, ViewStyle } from 'react-native';
import { useTheme } from '@react-navigation/native';

interface CardProps {
  children: React.ReactNode;
  variant?: 'elevated' | 'outlined' | 'flat';
  padding?: 'none' | 'sm' | 'md' | 'lg';
  className?: string;
  style?: ViewStyle;
}

export const CardComponent: React.FC<CardProps> = ({
  children,
  variant = 'elevated',
  padding = 'md',
  className,
  style,
}) => {
  const { colors } = useTheme();

  // Memoize padding calculation
  const paddingValue = useMemo(() => {
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
  }, [padding]);

  // Memoize card style to avoid recalculations
  const cardStyle = useMemo<ViewStyle>(() => ({
    backgroundColor: colors.card,
    borderRadius: 20,
    padding: paddingValue,
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
  }), [colors.card, colors.border, paddingValue, variant, style]);

  return <View style={cardStyle}>{children}</View>;
};

// Memoize Card component for performance optimization in lists
export const Card = memo(CardComponent, (prevProps, nextProps) => {
  return (
    prevProps.variant === nextProps.variant &&
    prevProps.padding === nextProps.padding &&
    prevProps.children === nextProps.children &&
    prevProps.style === nextProps.style
  );
});

Card.displayName = 'Card';
