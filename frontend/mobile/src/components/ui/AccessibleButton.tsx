/**
 * AccessibleButton Component
 *
 * A button component with comprehensive accessibility support including:
 * - Proper touch target size (44x44 minimum)
 * - Screen reader support with labels and hints
 * - Focus management
 * - Visual feedback for different states
 * - WCAG 2.1 AA compliance
 *
 * @module components/ui/AccessibleButton
 * @version 1.0.0
 */

import React, { useRef, useState, useCallback } from 'react';
import {
  TouchableOpacity,
  TouchableOpacityProps,
  StyleSheet,
  ViewStyle,
  TextStyle,
  View,
  Text,
} from 'react-native';
import type { AccessibilityRole } from 'react-native';
import { useTheme } from '@react-navigation/native';
import { generateA11yProps, PayUAccessibilityProps, validateTouchTarget } from '@/src/utils/accessibility';

// ============================================================================
// Types & Interfaces
// ============================================================================

/**
 * Props for the AccessibleButton component
 */
export interface AccessibleButtonProps extends Omit<TouchableOpacityProps, 'style' | 'children'> {
  /** Button label text (visible and for screen reader) */
  label: string;
  /** Optional hint describing the action */
  hint?: string;
  /** Icon component to display (left side) */
  icon?: React.ReactNode;
  /** Icon position relative to label */
  iconPosition?: 'left' | 'right';
  /** Button variant for styling */
  variant?: 'primary' | 'secondary' | 'tertiary' | 'danger' | 'ghost';
  /** Button size */
  size?: 'sm' | 'md' | 'lg';
  /** Whether the button fills container width */
  fullWidth?: boolean;
  /** Whether the button is in loading state */
  loading?: boolean;
  /** Loading text for screen reader */
  loadingLabel?: string;
  /** Custom styles */
  style?: ViewStyle;
  /** Label text styles */
  labelStyle?: TextStyle;
  /** Container styles */
  containerStyle?: ViewStyle;
  /** Accessibility role override */
  accessibilityRole?: AccessibilityRole;
  /** Whether to enforce minimum touch target size */
  enforceTouchTarget?: boolean;
  /** Custom accessibility props */
  a11yProps?: Partial<PayUAccessibilityProps>;
  /** Children elements (alternative to label) */
  children?: React.ReactNode;
  /** Callback when touch target validation fails */
  onTouchTargetError?: (error: string) => void;
}

// ============================================================================
// Constants
// ============================================================================

const TOUCH_TARGET_SIZE = 44;

const BUTTON_SIZES = {
  sm: {
    paddingVertical: 8,
    paddingHorizontal: 12,
    fontSize: 14,
    minHeight: 36,
  },
  md: {
    paddingVertical: 12,
    paddingHorizontal: 16,
    fontSize: 16,
    minHeight: 44,
  },
  lg: {
    paddingVertical: 16,
    paddingHorizontal: 24,
    fontSize: 18,
    minHeight: 56,
  },
};

// ============================================================================
// Component
// ============================================================================

/**
 * AccessibleButton - A fully accessible button component
 *
 * @example
 * ```tsx
 * <AccessibleButton
 *   label="Transfer"
 *   hint="Opens transfer screen"
 *   variant="primary"
 *   onPress={handleTransfer}
 * />
 * ```
 */
export const AccessibleButton: React.FC<AccessibleButtonProps> = ({
  label,
  hint,
  icon,
  iconPosition = 'left',
  variant = 'primary',
  size = 'md',
  fullWidth = false,
  loading = false,
  loadingLabel = 'Loading, please wait',
  style,
  labelStyle,
  containerStyle,
  accessibilityRole = 'button',
  enforceTouchTarget = true,
  a11yProps,
  children,
  onTouchTargetError,
  disabled,
  onPress,
  onLayout,
  ...touchableProps
}) => {
  const { colors } = useTheme();
  const buttonRef = useRef<View>(null);
  const [touchTargetValid, setTouchTargetValid] = useState(true);
  const [measuredSize, setMeasuredSize] = useState({ width: 0, height: 0 });

  // Handle layout to validate touch target size
  const handleLayout = useCallback(
    (event: any) => {
      const { width, height } = event.nativeEvent.layout;
      setMeasuredSize({ width, height });

      if (enforceTouchTarget) {
        const validation = validateTouchTarget(width, height);
        setTouchTargetValid(validation.isValid);

        if (!validation.isValid && onTouchTargetError) {
          onTouchTargetError(validation.error || 'Touch target too small');
        }
      }

      onLayout?.(event);
    },
    [enforceTouchTarget, onLayout, onTouchTargetError]
  );

  // Generate accessibility props
  const getAccessibilityProps = (): PayUAccessibilityProps => {
    const currentLabel = loading ? loadingLabel : label;
    const currentHint = loading ? undefined : hint;

    return generateA11yProps({
      label: currentLabel,
      hint: currentHint,
      role: accessibilityRole as any,
      disabled: disabled || loading,
      busy: loading,
      testID: a11yProps?.testID || `${generateTestID(label)}-button`,
      ...a11yProps,
    });
  };

  // Get variant styles
  const getVariantStyles = (): { background: string; text: string; border?: string } => {
    switch (variant) {
      case 'primary':
        return {
          background: colors.primary,
          text: '#ffffff',
        };
      case 'secondary':
        return {
          background: colors.card,
          text: colors.text,
          border: colors.border,
        };
      case 'tertiary':
        return {
          background: 'transparent',
          text: colors.primary,
        };
      case 'danger':
        return {
          background: '#ef4444',
          text: '#ffffff',
        };
      case 'ghost':
        return {
          background: 'transparent',
          text: colors.text,
        };
      default:
        return {
          background: colors.primary,
          text: '#ffffff',
        };
    }
  };

  const variantStyles = getVariantStyles();
  const sizeStyles = BUTTON_SIZES[size];

  // Build button styles
  const buttonStyles: ViewStyle = {
    backgroundColor: variantStyles.background,
    paddingVertical: sizeStyles.paddingVertical,
    paddingHorizontal: sizeStyles.paddingHorizontal,
    minHeight: sizeStyles.minHeight,
    borderRadius: 12,
    borderWidth: variantStyles.border ? 1 : 0,
    borderColor: variantStyles.border,
    opacity: disabled || loading ? 0.6 : 1,
    flexDirection: iconPosition === 'left' ? 'row' : 'row-reverse',
    alignItems: 'center',
    justifyContent: 'center',
    gap: 8,
    ...(fullWidth && { width: '100%' }),
    ...style,
  };

  // Touch target wrapper style
  const wrapperStyle: ViewStyle = {
    minWidth: TOUCH_TARGET_SIZE,
    minHeight: TOUCH_TARGET_SIZE,
    justifyContent: 'center',
    alignItems: 'center',
    ...containerStyle,
  };

  // Label style
  const textStyle: TextStyle = {
    fontSize: sizeStyles.fontSize,
    fontWeight: '600',
    color: variantStyles.text,
    ...labelStyle,
  };

  // Loading spinner (simplified)
  const renderLoading = () => (
    <View
      style={styles.loadingSpinner}
      accessible={false}
      importantForAccessibility="no"
    >
      <View style={[styles.spinnerDot, { backgroundColor: variantStyles.text }]} />
    </View>
  );

  return (
    <View style={wrapperStyle} ref={buttonRef}>
      <TouchableOpacity
        {...getAccessibilityProps()}
        {...touchableProps}
        onPress={onPress}
        disabled={disabled || loading}
        onLayout={handleLayout}
        style={buttonStyles}
        activeOpacity={0.8}
      >
        {loading ? (
          renderLoading()
        ) : (
          <>
            {icon && (
              <View
                accessible={false}
                importantForAccessibility="no"
              >
                {icon}
              </View>
            )}
            {children || (
              <Text style={textStyle} accessible={false} importantForAccessibility="no">
                {label}
              </Text>
            )}
          </>
        )}
      </TouchableOpacity>
    </View>
  );
};

// ============================================================================
// Helper Functions
// ============================================================================

/**
 * Generates a testID from a label string
 */
function generateTestID(label: string): string {
  return label
    .toLowerCase()
    .replace(/[^a-z0-9]+/g, '-')
    .replace(/^-|-$/g, '');
}

// ============================================================================
// Styles
// ============================================================================

const styles = StyleSheet.create({
  loadingSpinner: {
    width: 20,
    height: 20,
    justifyContent: 'center',
    alignItems: 'center',
  },
  spinnerDot: {
    width: 4,
    height: 4,
    borderRadius: 2,
    opacity: 0.7,
  },
});

// ============================================================================
// Export
// ============================================================================

export default AccessibleButton;
