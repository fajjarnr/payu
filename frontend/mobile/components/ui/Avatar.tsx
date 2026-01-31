import React, { useMemo, memo, useCallback } from 'react';
import { View, Text, Image, ViewStyle, ImageStyle } from 'react-native';
import { useTheme } from '@react-navigation/native';

interface AvatarProps {
  source?: { uri: string };
  src?: string; // Alias for source.uri
  name?: string;
  size?: number;
  style?: ViewStyle;
}

// Performance: Memoized initials calculation with memoization
const getInitials = useCallback((name?: string): string => {
  if (!name) return '?';
  const parts = name.trim().split(' ');
  if (parts.length === 1) {
    return parts[0].charAt(0).toUpperCase();
  }
  return (parts[0].charAt(0) + parts[parts.length - 1].charAt(0)).toUpperCase();
}, []);

export const AvatarComponent: React.FC<AvatarProps> = ({
  source,
  src,
  name,
  size = 48,
  style,
}) => {
  const { colors } = useTheme();

  // Performance: Memoize initials to avoid recalculation on every render
  const initials = useMemo(() => getInitials(name), [name]);

  // Performance: Memoize avatar style
  const avatarStyle = useMemo<ViewStyle>(() => ({
    width: size,
    height: size,
    borderRadius: size / 2,
    backgroundColor: '#10b981',
    justifyContent: 'center',
    alignItems: 'center',
    ...style,
  }), [size, style]);

  // Performance: Memoize text style
  const textStyle = useMemo(() => ({
    color: '#ffffff',
    fontSize: size / 2.5,
    fontWeight: '700' as const,
  }), [size]);

  // Performance: Memoize image source
  const imageSource = useMemo(() => {
    return source || (src ? { uri: src } : undefined);
  }, [source, src]);

  // Performance: Memoize image style
  const imageStyle = useMemo<ImageStyle>(() => ({
    width: size,
    height: size,
    borderRadius: size / 2,
    backgroundColor: colors.border,
  }), [size, colors.border]);

  if (imageSource?.uri) {
    return (
      <Image
        source={imageSource}
        style={imageStyle}
      />
    );
  }

  return (
    <View style={avatarStyle}>
      <Text style={textStyle}>{initials}</Text>
    </View>
  );
};

// Performance: Memoize Avatar component to prevent unnecessary re-renders in lists
export const Avatar = memo(AvatarComponent, (prevProps, nextProps) => {
  return (
    prevProps.size === nextProps.size &&
    prevProps.name === nextProps.name &&
    prevProps.source?.uri === nextProps.source?.uri &&
    prevProps.src === nextProps.src
  );
});

Avatar.displayName = 'Avatar';
