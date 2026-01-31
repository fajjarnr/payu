import React, { useMemo, memo } from 'react';
import { View, Text, ViewStyle } from 'react-native';

interface BadgeProps {
  text: string;
  variant?: 'success' | 'warning' | 'error' | 'info';
  size?: 'sm' | 'md';
  style?: ViewStyle;
}

export const BadgeComponent: React.FC<BadgeProps> = ({
  text,
  variant = 'info',
  size = 'sm',
  style,
}) => {
  // Memoize badge style to avoid recalculations on every render
  const badgeStyle = useMemo<ViewStyle>(() => {
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

    return {
      backgroundColor: getBackgroundColor(),
      paddingHorizontal: size === 'sm' ? 8 : 12,
      paddingVertical: size === 'sm' ? 4 : 6,
      borderRadius: 8,
      alignSelf: 'flex-start',
      ...style,
    };
  }, [variant, size, style]);

  // Memoize text style
  const textStyle = useMemo(() => {
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

    return {
      color: getTextColor(),
      fontSize: size === 'sm' ? 12 : 14,
      fontWeight: '600' as const,
    };
  }, [variant, size]);

  return (
    <View style={badgeStyle}>
      <Text style={textStyle}>{text}</Text>
    </View>
  );
};

// Memoize Badge component for performance optimization in lists
export const Badge = memo(BadgeComponent, (prevProps, nextProps) => {
  return (
    prevProps.text === nextProps.text &&
    prevProps.variant === nextProps.variant &&
    prevProps.size === nextProps.size
  );
});

Badge.displayName = 'Badge';
