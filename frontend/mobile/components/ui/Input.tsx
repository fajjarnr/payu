import React from 'react';
import {
  View,
  TextInput,
  Text,
  StyleSheet,
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
  keyboardType?: 'default' | 'email-address' | 'numeric' | 'phone-pad';
  error?: string;
  disabled?: boolean;
  icon?: React.ReactNode;
  multiline?: boolean;
  numberOfLines?: number;
  style?: ViewStyle;
  onSubmitEditing?: () => void;
  returnKeyType?: 'done' | 'next' | 'go' | 'search';
}

export const Input: React.FC<InputProps> = ({
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
  style,
  onSubmitEditing,
  returnKeyType = 'done',
}) => {
  const { colors } = useTheme();

  const containerStyle: ViewStyle = {
    marginBottom: 16,
    ...style,
  };

  const inputContainerStyle: ViewStyle = {
    flexDirection: 'row',
    alignItems: 'center',
    backgroundColor: colors.card,
    borderWidth: 1,
    borderColor: error ? '#ef4444' : colors.border,
    borderRadius: 12,
    paddingHorizontal: 16,
    height: multiline ? 'auto' : 56,
    minHeight: multiline ? 100 : 56,
  };

  const inputStyle: TextStyle = {
    flex: 1,
    color: colors.text,
    fontSize: 16,
    paddingVertical: multiline ? 12 : 16,
    minHeight: multiline ? 76 : 0,
  };

  const labelStyle: TextStyle = {
    color: colors.text,
    fontSize: 14,
    fontWeight: '500',
    marginBottom: 8,
    marginLeft: 4,
  };

  const errorStyle: TextStyle = {
    color: '#ef4444',
    fontSize: 12,
    marginTop: 4,
    marginLeft: 4,
  };

  return (
    <View style={containerStyle}>
      {label && <Text style={labelStyle}>{label}</Text>}
      <View style={inputContainerStyle}>
        {icon && <View style={{ marginRight: 12 }}>{icon}</View>}
        <TextInput
          value={value}
          onChangeText={onChangeText}
          placeholder={placeholder}
          placeholderTextColor={colors.textSecondary}
          secureTextEntry={secureTextEntry}
          keyboardType={keyboardType}
          editable={!disabled}
          multiline={multiline}
          numberOfLines={numberOfLines}
          style={inputStyle}
          onSubmitEditing={onSubmitEditing}
          returnKeyType={returnKeyType}
        />
      </View>
      {error && <Text style={errorStyle}>{error}</Text>}
    </View>
  );
};
