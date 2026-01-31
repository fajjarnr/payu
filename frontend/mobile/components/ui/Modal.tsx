import React, { useMemo, memo, useCallback } from 'react';
import {
  Modal as RNModal,
  View,
  Text,
  TouchableOpacity,
  StyleSheet,
  ViewStyle,
} from 'react-native';
import { useTheme } from '@react-navigation/native';

interface ModalProps {
  visible: boolean;
  onClose: () => void;
  title?: string;
  children: React.ReactNode;
  style?: ViewStyle;
}

export const ModalComponent: React.FC<ModalProps> = ({
  visible,
  onClose,
  title,
  children,
  style,
}) => {
  const { colors } = useTheme();

  // Performance: Memoize container style
  const containerStyle = useMemo<ViewStyle>(() => [
    styles.container,
    { backgroundColor: colors.background },
    style,
  ], [colors.background, style]);

  // Performance: Memoize title style
  const titleStyle = useMemo<ViewStyle>(() => [
    styles.title,
    { color: colors.text },
  ], [colors.text]);

  // Performance: Memoize close button handler
  const handleClose = useCallback(() => {
    onClose();
  }, [onClose]);

  if (!visible) {
    return null;
  }

  return (
    <RNModal
      visible={visible}
      transparent
      animationType="slide"
      onRequestClose={handleClose}
    >
      <View style={styles.overlay}>
        <View style={containerStyle}>
          {/* Header */}
          <View style={styles.header}>
            {title && (
              <Text style={titleStyle}>{title}</Text>
            )}
            <TouchableOpacity onPress={handleClose} style={styles.closeButton}>
              <Text style={styles.closeButtonText}>✕</Text>
            </TouchableOpacity>
          </View>

          {/* Content */}
          <View style={styles.content}>{children}</View>
        </View>
      </View>
    </RNModal>
  );
};

// Performance: Memoize Modal component to prevent unnecessary re-renders
export const Modal = memo(ModalComponent, (prevProps, nextProps) => {
  return (
    prevProps.visible === nextProps.visible &&
    prevProps.title === nextProps.title &&
    prevProps.children === nextProps.children
  );
});

Modal.displayName = 'Modal';

const styles = StyleSheet.create({
  overlay: {
    flex: 1,
    backgroundColor: 'rgba(0, 0, 0, 0.5)',
    justifyContent: 'flex-end',
  },
  container: {
    borderTopLeftRadius: 32,
    borderTopRightRadius: 32,
    padding: 24,
    maxHeight: '90%',
  },
  header: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginBottom: 24,
  },
  title: {
    fontSize: 20,
    fontWeight: '700',
  },
  closeButton: {
    width: 32,
    height: 32,
    borderRadius: 16,
    backgroundColor: '#f3f4f6',
    justifyContent: 'center',
    alignItems: 'center',
  },
  closeButtonText: {
    fontSize: 18,
    fontWeight: '600',
    color: '#6b7280',
  },
  content: {
    flex: 1,
  },
});
