import React, { useMemo, memo, useCallback } from 'react';
import {
  TouchableOpacity,
  Text,
  ActivityIndicator,
  ViewStyle,
  TextStyle,
} from 'react-native';
import { useTheme } from '@react-navigation/native';

interface ButtonProps {
  title: string;
  onPress: () => void;
  variant?: 'primary' | 'secondary' | 'outline' | 'ghost';
  size?: 'sm' | 'md' | 'lg';
  disabled?: boolean;
  loading?: boolean;
  icon?: React.ReactNode;
  fullWidth?: boolean;
  style?: ViewStyle;
  testID?: string;
}

export const ButtonComponent: React.FC<ButtonProps> = ({
  title,
  onPress,
  variant = 'primary',
  size = 'md',
  disabled = false,
  loading = false,
  icon,
  fullWidth = false,
  style,
  testID,
}) => {
  const { colors } = useTheme();

  // Memoize style calculations for performance
  const buttonStyle = useMemo<ViewStyle>(() => {
    const getBackgroundColor = () => {
      if (disabled) return colors.border;
      switch (variant) {
        case 'primary':
          return '#10b981';
        case 'secondary':
          return '#059669';
        case 'outline':
          return 'transparent';
        case 'ghost':
          return 'transparent';
        default:
          return '#10b981';
      }
    };

    const getBorderColor = () => {
      if (variant === 'outline') return '#10b981';
      return 'transparent';
    };

    const getHeight = () => {
      switch (size) {
        case 'sm':
          return 36;
        case 'md':
          return 48;
        case 'lg':
          return 56;
        default:
          return 48;
      }
    };

    return {
      backgroundColor: getBackgroundColor(),
      borderColor: getBorderColor(),
      borderWidth: variant === 'outline' ? 1 : 0,
      height: getHeight(),
      borderRadius: 16,
      paddingHorizontal: 24,
      justifyContent: 'center',
      alignItems: 'center',
      flexDirection: 'row' as const,
      width: fullWidth ? '100%' : 'auto',
      opacity: disabled ? 0.5 : 1,
      shadowColor: '#10b981',
      shadowOffset: { width: 0, height: 4 },
      shadowOpacity: variant === 'ghost' ? 0 : 0.2,
      shadowRadius: 8,
      elevation: variant === 'ghost' ? 0 : 4,
      ...style,
    };
  }, [colors.border, disabled, variant, size, fullWidth, style]);

  const textStyle = useMemo<TextStyle>(() => {
    const getTextColor = () => {
      if (disabled) return (colors as typeof colors & { textSecondary?: string }).textSecondary ?? '#6b7280';
      switch (variant) {
        case 'outline':
        case 'ghost':
          return '#10b981';
        default:
          return '#ffffff';
      }
    };

    const getFontSize = () => {
      switch (size) {
        case 'sm':
          return 14;
        case 'md':
          return 16;
        case 'lg':
          return 18;
        default:
          return 16;
      }
    };

    return {
      color: getTextColor(),
      fontSize: getFontSize(),
      fontWeight: '600',
      marginLeft: icon ? 8 : 0,
    };
  }, [colors, disabled, variant, size, icon]);

  // Memoize press handler to prevent unnecessary re-renders
  const handlePress = useCallback(() => {
    if (!disabled && !loading) {
      onPress();
    }
  }, [disabled, loading, onPress]);

  const textColor = textStyle.color;

  return (
    <TouchableOpacity
      onPress={handlePress}
      disabled={disabled || loading}
      style={buttonStyle}
      activeOpacity={0.8}
      testID={testID}
    >
      {loading ? (
        <ActivityIndicator color={textColor} />
      ) : (
        <>
          {icon}
          <Text style={textStyle}>{title}</Text>
        </>
      )}
    </TouchableOpacity>
  );
};

// Memoize Button component for performance optimization in lists
export const Button = memo(ButtonComponent, (prevProps, nextProps) => {
  return (
    prevProps.title === nextProps.title &&
    prevProps.variant === nextProps.variant &&
    prevProps.size === nextProps.size &&
    prevProps.disabled === nextProps.disabled &&
    prevProps.loading === nextProps.loading &&
    prevProps.onPress === nextProps.onPress &&
    prevProps.fullWidth === nextProps.fullWidth
  );
});

Button.displayName = 'Button';
