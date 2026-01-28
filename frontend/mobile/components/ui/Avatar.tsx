import React from 'react';
import { View, Text, StyleSheet, Image, ViewStyle } from 'react-native';
import { useTheme } from '@react-navigation/native';

interface AvatarProps {
  source?: { uri: string };
  src?: string; // Alias for source.uri
  name?: string;
  size?: number;
  style?: ViewStyle;
}

export const Avatar: React.FC<AvatarProps> = ({
  source,
  src,
  name,
  size = 48,
  style,
}) => {
  const { colors } = useTheme();

  const getInitials = () => {
    if (!name) return '?';
    const parts = name.trim().split(' ');
    if (parts.length === 1) {
      return parts[0].charAt(0).toUpperCase();
    }
    return (parts[0].charAt(0) + parts[parts.length - 1].charAt(0)).toUpperCase();
  };

  const avatarStyle: ViewStyle = {
    width: size,
    height: size,
    borderRadius: size / 2,
    backgroundColor: '#10b981',
    justifyContent: 'center',
    alignItems: 'center',
    ...style,
  };

  const textStyle = {
    color: '#ffffff',
    fontSize: size / 2.5,
    fontWeight: '700' as const,
  };

  const imageSource = source || (src ? { uri: src } : undefined);

  if (imageSource?.uri) {
    const imageStyle: any = {
      width: size,
      height: size,
      borderRadius: size / 2,
      backgroundColor: colors.border,
    };
    return (
      <Image
        source={imageSource}
        style={imageStyle}
      />
    );
  }

  return (
    <View style={avatarStyle}>
      <Text style={textStyle}>{getInitials()}</Text>
    </View>
  );
};
