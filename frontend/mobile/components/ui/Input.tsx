import React, { useMemo, memo, useCallback } from 'react';
import {
  View,
  TextInput,
  Text,
  ViewStyle,
  TextStyle,
} from 'react-native';
import { useTheme } from '@react-navigation/native';

interface InputProps {
  label?: string;
  value: string;
  onChangeText: (text: string) => void;
  placeholder?: string;
  secureTextEntry?: boolean;
  keyboardType?: 'default' | 'email-address' | 'numeric' | 'phone-pad' | 'number-pad';
  error?: string;
  disabled?: boolean;
  icon?: React.ReactNode;
  multiline?: boolean;
  numberOfLines?: number;
  maxLength?: number;
  style?: ViewStyle;
  onSubmitEditing?: () => void;
  returnKeyType?: 'done' | 'next' | 'go' | 'search';
  autoCapitalize?: 'none' | 'sentences' | 'words' | 'characters';
  textAlign?: 'left' | 'center' | 'right';
}

export const InputComponent: React.FC<InputProps> = ({
  label,
  value,
  onChangeText,
  placeholder,
  secureTextEntry = false,
  keyboardType = 'default',
  error,
  disabled = false,
  icon,
  multiline = false,
  numberOfLines = 1,
  maxLength,
  style,
  onSubmitEditing,
  returnKeyType = 'done',
  textAlign,
}) => {
  const { colors } = useTheme();

  // Performance: Memoize placeholder text color
  const placeholderTextColor = useMemo(
    () => (colors as typeof colors & { textSecondary?: string }).textSecondary ?? '#6b7280',
    [colors]
  );

  // Performance: Memoize container style
  const containerStyle = useMemo<ViewStyle>(() => ({
    marginBottom: 16,
    ...style,
  }), [style]);

  // Performance: Memoize input container style
  const inputContainerStyle = useMemo<ViewStyle>(() => ({
    flexDirection: 'row' as const,
    alignItems: 'center',
    backgroundColor: colors.card,
    borderWidth: 1,
    borderColor: error ? '#ef4444' : colors.border,
    borderRadius: 12,
    paddingHorizontal: 16,
    height: multiline ? 'auto' : 56,
    minHeight: multiline ? 100 : 56,
  }), [colors.card, colors.border, error, multiline]);

  // Performance: Memoize input style
  const inputStyle = useMemo<TextStyle>(() => ({
    flex: 1,
    color: colors.text,
    fontSize: 16,
    paddingVertical: multiline ? 12 : 16,
    minHeight: multiline ? 76 : 0,
  }), [colors.text, multiline]);

  // Performance: Memoize label style
  const labelStyle = useMemo<TextStyle>(() => ({
    color: colors.text,
    fontSize: 14,
    fontWeight: '500',
    marginBottom: 8,
    marginLeft: 4,
  }), [colors.text]);

  // Performance: Memoize error style
  const errorStyle = useMemo<TextStyle>(() => ({
    color: '#ef4444',
    fontSize: 12,
    marginTop: 4,
    marginLeft: 4,
  }), []);

  // Performance: Memoize icon container style
  const iconContainerStyle = useMemo<ViewStyle>(() => ({
    marginRight: 12,
  }), []);

  return (
    <View style={containerStyle}>
      {label && <Text style={labelStyle}>{label}</Text>}
      <View style={inputContainerStyle}>
        {icon && <View style={iconContainerStyle}>{icon}</View>}
        <TextInput
          value={value}
          onChangeText={onChangeText}
          placeholder={placeholder}
          placeholderTextColor={placeholderTextColor}
          secureTextEntry={secureTextEntry}
          keyboardType={keyboardType}
          editable={!disabled}
          multiline={multiline}
          numberOfLines={numberOfLines}
          maxLength={maxLength}
          style={inputStyle}
          onSubmitEditing={onSubmitEditing}
          returnKeyType={returnKeyType}
          textAlign={textAlign}
        />
      </View>
      {error && <Text style={errorStyle}>{error}</Text>}
    </View>
  );
};

// Performance: Memoize Input component to prevent unnecessary re-renders
// Note: We skip value/onChangeText comparison since those change frequently
export const Input = memo(InputComponent, (prevProps, nextProps) => {
  return (
    prevProps.label === nextProps.label &&
    prevProps.placeholder === nextProps.placeholder &&
    prevProps.error === nextProps.error &&
    prevProps.disabled === nextProps.disabled &&
    prevProps.secureTextEntry === nextProps.secureTextEntry &&
    prevProps.multiline === nextProps.multiline &&
    prevProps.icon === nextProps.icon
  );
});

Input.displayName = 'Input';
