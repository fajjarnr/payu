/**
 * AccessibleInput Component
 *
 * A text input component with comprehensive accessibility support including:
 * - Proper label association
 * - Error message accessibility
 * - Focus management
 * - Screen reader announcements for validation
 * - WCAG 2.1 AA compliance
 *
 * @module components/ui/AccessibleInput
 * @version 1.0.0
 */

import React, { useRef, useState, useCallback, useImperativeHandle, forwardRef } from 'react';
import {
  TextInput,
  TextInputProps,
  View,
  Text,
  StyleSheet,
  ViewStyle,
  TextStyle,
  TouchableOpacity,
  AccessibilityState,
} from 'react-native';
import { useTheme } from '@react-navigation/native';
import { Eye, EyeOff, X } from 'lucide-react-native';
import { generateA11yProps, createInputA11yProps, PayUAccessibilityProps } from '@/src/utils/accessibility';

// ============================================================================
// Types & Interfaces
// ============================================================================

/**
 * Props for the AccessibleInput component
 */
export interface AccessibleInputProps extends Omit<TextInputProps, 'style'> {
  /** Input label (visible and for screen reader) */
  label: string;
  /** Helper text displayed below input */
  helperText?: string;
  /** Error message displayed below input */
  error?: string;
  /** Whether the field is required */
  required?: boolean;
  /** Input variant */
  variant?: 'outlined' | 'filled' | 'underlined';
  /** Input size */
  size?: 'sm' | 'md' | 'lg';
  /** Left icon component */
  leftIcon?: React.ReactNode;
  /** Right icon component */
  rightIcon?: React.ReactNode;
  /** Whether to show clear button */
  clearable?: boolean;
  /** Whether input is for password */
  secure?: boolean;
  /** Custom container styles */
  containerStyle?: ViewStyle;
  /** Custom input styles */
  inputStyle?: TextStyle;
  /** Custom label styles */
  labelStyle?: TextStyle;
  /** Test ID for testing */
  testID?: string;
  /** Accessibility hint */
  accessibilityHint?: string;
  /** Callback when validation error should be announced */
  onErrorAnnounce?: (error: string) => void;
  /** Callback when input receives focus */
  onFocus?: (e: any) => void;
  /** Callback when input loses focus */
  onBlur?: (e: any) => void;
  /** Callback when value changes */
  onChangeText?: (text: string) => void;
}

/**
 * Ref methods exposed by AccessibleInput
 */
export interface AccessibleInputRef {
  /** Focus the input */
  focus: () => void;
  /** Blur the input */
  blur: () => void;
  /** Clear the input */
  clear: () => void;
  /** Set selection */
  setSelection: (start: number, end?: number) => void;
  /** Announce current value to screen reader */
  announceValue: () => void;
  /** Announce error to screen reader */
  announceError: (message: string) => void;
}

// ============================================================================
// Constants
// ============================================================================

const INPUT_SIZES = {
  sm: {
    paddingVertical: 8,
    paddingHorizontal: 12,
    fontSize: 14,
    minHeight: 40,
  },
  md: {
    paddingVertical: 12,
    paddingHorizontal: 16,
    fontSize: 16,
    minHeight: 48,
  },
  lg: {
    paddingVertical: 16,
    paddingHorizontal: 20,
    fontSize: 18,
    minHeight: 56,
  },
};

// ============================================================================
// Component
// ============================================================================

/**
 * AccessibleInput - A fully accessible text input component
 *
 * @example
 * ```tsx
 * <AccessibleInput
 *   label="Account Number"
 *   placeholder="Enter your account number"
 *   required
 *   error={errors.accountNumber}
 *   onChangeText={setAccountNumber}
 * />
 * ```
 */
export const AccessibleInput = forwardRef<AccessibleInputRef, AccessibleInputProps>(
  (
    {
      label,
      helperText,
      error,
      required = false,
      variant = 'outlined',
      size = 'md',
      leftIcon,
      rightIcon,
      clearable = false,
      secure = false,
      containerStyle,
      inputStyle,
      labelStyle,
      testID,
      accessibilityHint,
      onErrorAnnounce,
      onFocus,
      onBlur,
      onChangeText,
      value,
      defaultValue,
      editable = true,
      ...textInputProps
    },
    ref
  ) => {
    const { colors } = useTheme();
    const inputRef = useRef<TextInput>(null);
    const [isFocused, setIsFocused] = useState(false);
    const [isSecureVisible, setIsSecureVisible] = useState(false);
    const [currentValue, setCurrentValue] = useState(value || defaultValue || '');

    // Sync with controlled value
    React.useEffect(() => {
      if (value !== undefined) {
        setCurrentValue(value);
      }
    }, [value]);

    // Announce error when it changes
    React.useEffect(() => {
      if (error && onErrorAnnounce) {
        onErrorAnnounce(error);
      }
    }, [error, onErrorAnnounce]);

    // Expose imperative methods
    useImperativeHandle(ref, () => ({
      focus: () => inputRef.current?.focus(),
      blur: () => inputRef.current?.blur(),
      clear: () => {
        inputRef.current?.clear();
        setCurrentValue('');
        onChangeText?.('');
      },
      setSelection: (start, end) => {
        // @ts-ignore - setSelection exists but types may not include it
        inputRef.current?.setSelection?.(start, end);
      },
      announceValue: () => {
        // This would integrate with screen reader announcement API
        // For now, it's a placeholder for future implementation
      },
      announceError: (message: string) => {
        onErrorAnnounce?.(message);
      },
    }));

    // Handle focus
    const handleFocus = useCallback(
      (e: any) => {
        setIsFocused(true);
        onFocus?.(e);
      },
      [onFocus]
    );

    // Handle blur
    const handleBlur = useCallback(
      (e: any) => {
        setIsFocused(false);
        onBlur?.(e);
      },
      [onBlur]
    );

    // Handle text change
    const handleChangeText = useCallback(
      (text: string) => {
        setCurrentValue(text);
        onChangeText?.(text);
      },
      [onChangeText]
    );

    // Handle clear
    const handleClear = useCallback(() => {
      inputRef.current?.clear();
      setCurrentValue('');
      onChangeText?.('');
      inputRef.current?.focus();
    }, [onChangeText]);

    // Toggle secure visibility
    const toggleSecureVisibility = useCallback(() => {
      setIsSecureVisible((prev) => !prev);
    }, []);

    // Generate accessibility props
    const getAccessibilityProps = (): PayUAccessibilityProps => {
      const baseProps = createInputA11yProps(label, {
        required,
        error,
        hint: accessibilityHint,
        testID: testID || generateTestID(label),
      });

      return {
        ...baseProps,
        accessibilityState: {
          ...baseProps.accessibilityState,
          focused: isFocused,
        } as AccessibilityState,
      };
    };

    // Get variant styles
    const getVariantStyles = () => {
      const baseStyles = {
        backgroundColor: variant === 'filled' ? colors.card : 'transparent',
        borderWidth: variant === 'outlined' ? 1 : variant === 'underlined' ? 0 : 0,
        borderBottomWidth: variant === 'underlined' ? 1 : undefined,
        borderColor: error ? '#ef4444' : isFocused ? colors.primary : colors.border,
        borderRadius: variant === 'underlined' ? 0 : 12,
      };
      return baseStyles;
    };

    const sizeStyles = INPUT_SIZES[size];
    const variantStyles = getVariantStyles();

    // Container styles
    const containerStyles: ViewStyle = {
      marginBottom: error || helperText ? 8 : 16,
      ...containerStyle,
    };

    // Input container styles
    const inputContainerStyles: ViewStyle = {
      flexDirection: 'row',
      alignItems: 'center',
      backgroundColor: variantStyles.backgroundColor,
      borderWidth: variantStyles.borderWidth,
      borderBottomWidth: variantStyles.borderBottomWidth,
      borderColor: variantStyles.borderColor,
      borderRadius: variantStyles.borderRadius as number,
      minHeight: sizeStyles.minHeight,
      opacity: editable ? 1 : 0.6,
    };

    // Input styles
    const baseInputStyles: TextStyle = {
      flex: 1,
      paddingVertical: sizeStyles.paddingVertical,
      paddingHorizontal: sizeStyles.paddingHorizontal,
      fontSize: sizeStyles.fontSize,
      color: colors.text,
      ...inputStyle,
    };

    // Label styles
    const labelStyles: TextStyle = {
      fontSize: size === 'sm' ? 12 : size === 'md' ? 14 : 16,
      fontWeight: '500',
      color: error ? '#ef4444' : colors.text,
      marginBottom: 6,
      ...labelStyle,
    };

    // Helper/error text styles
    const helperStyles: TextStyle = {
      fontSize: 12,
      color: error ? '#ef4444' : (colors as any).textSecondary || '#6b7280',
      marginTop: 4,
    };

    // Determine secure text entry
    const isSecureTextEntry = secure && !isSecureVisible;

    // Render right icon area
    const renderRightArea = () => {
      if (secure) {
        return (
          <TouchableOpacity
            onPress={toggleSecureVisibility}
            style={styles.iconButton}
            accessibilityLabel={isSecureVisible ? 'Hide password' : 'Show password'}
            accessibilityHint="Toggles password visibility"
            accessibilityRole="button"
          >
            {isSecureVisible ? (
              <EyeOff size={20} color={colors.text} />
            ) : (
              <Eye size={20} color={colors.text} />
            )}
          </TouchableOpacity>
        );
      }

      if (clearable && currentValue.length > 0) {
        return (
          <TouchableOpacity
            onPress={handleClear}
            style={styles.iconButton}
            accessibilityLabel="Clear input"
            accessibilityHint="Clears the current input value"
            accessibilityRole="button"
          >
            <X size={20} color={colors.text} />
          </TouchableOpacity>
        );
      }

      if (rightIcon) {
        return (
          <View
            style={styles.iconContainer}
            accessible={false}
            importantForAccessibility="no"
          >
            {rightIcon}
          </View>
        );
      }

      return null;
    };

    return (
      <View style={containerStyles}>
        {/* Label */}
        <Text style={labelStyles} accessibilityLabel={label}>
          {label}
          {required && <Text style={styles.requiredIndicator}> *</Text>}
        </Text>

        {/* Input Container */}
        <View style={inputContainerStyles}>
          {leftIcon && (
            <View
              style={styles.leftIconContainer}
              accessible={false}
              importantForAccessibility="no"
            >
              {leftIcon}
            </View>
          )}

          {/* Text Input */}
          <TextInput
            ref={inputRef}
            {...getAccessibilityProps()}
            {...textInputProps}
            value={value}
            defaultValue={defaultValue}
            onChangeText={handleChangeText}
            onFocus={handleFocus}
            onBlur={handleBlur}
            editable={editable}
            secureTextEntry={isSecureTextEntry}
            style={baseInputStyles}
            placeholderTextColor={(colors as any).textSecondary || '#6b7280'}
            accessibilityLabel={`${label}${required ? ', required' : ''}${error ? `, error: ${error}` : ''}`}
            accessibilityHint={accessibilityHint || `Enter ${label}`}
          />

          {renderRightArea()}
        </View>

        {/* Helper/Error Text */}
        {(helperText || error) && (
          <Text
            style={helperStyles}
            accessibilityLiveRegion={error ? 'assertive' : 'polite'}
            accessibilityLabel={error || helperText}
          >
            {error || helperText}
          </Text>
        )}
      </View>
    );
  }
);

// ============================================================================
// Helper Functions
// ============================================================================

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
  requiredIndicator: {
    color: '#ef4444',
  },
  leftIconContainer: {
    paddingLeft: 12,
  },
  iconContainer: {
    paddingRight: 12,
  },
  iconButton: {
    padding: 8,
    marginRight: 4,
  },
});

// ============================================================================
// Export
// ============================================================================

AccessibleInput.displayName = 'AccessibleInput';

export default AccessibleInput;
