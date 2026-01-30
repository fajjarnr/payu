const React = require('react');

// Create mock components
const createMockComponent = (name) => {
  return function MockComponent(props) {
    return React.createElement(name, props, props.children);
  };
};

// Mock expo-secure-store
jest.mock('expo-secure-store', () => ({
  getItemAsync: jest.fn(),
  setItemAsync: jest.fn(),
  deleteItemAsync: jest.fn(),
}));

// Mock expo-local-authentication
jest.mock('expo-local-authentication', () => ({
  hasHardwareAsync: jest.fn(),
  isEnrolledAsync: jest.fn(),
  authenticateAsync: jest.fn(),
  supportedAuthenticationTypesAsync: jest.fn(),
  AuthenticationType: {
    FINGERPRINT: 1,
    FACIAL_RECOGNITION: 2,
    IRIS: 3,
  },
}));

// Mock expo-camera
jest.mock('expo-camera', () => ({
  Camera: {
    requestCameraPermissionsAsync: jest.fn(),
  },
  CameraView: jest.fn(),
}));

// Mock expo-notifications
jest.mock('expo-notifications', () => ({
  setNotificationHandler: jest.fn(),
  getPermissionsAsync: jest.fn(),
  requestPermissionsAsync: jest.fn(),
  getExpoPushTokenAsync: jest.fn(),
  scheduleNotificationAsync: jest.fn(),
  addNotificationReceivedListener: jest.fn(),
  addNotificationResponseReceivedListener: jest.fn(),
  setBadgeCountAsync: jest.fn(),
  dismissAllNotificationsAsync: jest.fn(),
}));

// Mock expo-device
jest.mock('expo-device', () => ({
  modelName: 'iPhone 14',
}));

// Mock expo-router
jest.mock('expo-router', () => ({
  useRouter: jest.fn(),
  useNavigation: jest.fn(),
}));

// Mock @react-native-async-storage/async-storage
jest.mock('@react-native-async-storage/async-storage', () => ({
  getItem: jest.fn(),
  setItem: jest.fn(),
  removeItem: jest.fn(),
}));

// Mock @react-native-community/netinfo
jest.mock('@react-native-community/netinfo', () => ({
  addEventListener: jest.fn(() => jest.fn()),
  fetch: jest.fn(),
}));

// Mock @react-navigation/native
jest.mock('@react-navigation/native', () => ({
  useNavigation: jest.fn(),
  useTheme: jest.fn(() => ({
    colors: {
      primary: '#10b981',
      background: '#111827',
      card: '#1f2937',
      text: '#ffffff',
      border: '#374151',
      notification: '#ef4444',
    },
    dark: true,
  })),
}));

// Mock lucide-react-native
jest.mock('lucide-react-native', () => ({
  Eye: () => null,
  EyeOff: () => null,
  Scan: () => null,
  Flashlight: () => null,
  FlashlightOff: () => null,
  X: () => null,
  Image: () => null,
  ZoomIn: () => null,
  ZoomOut: () => null,
  CreditCard: () => null,
  Copy: () => null,
  Lock: () => null,
  Unlock: () => null,
}));

// ============================================================================
// Accessibility Testing Setup
// ============================================================================

// Mock react-native AccessibilityInfo for a11y tests
const mockAccessibilityEventListeners = new Map();

jest.mock('react-native/Libraries/Components/AccessibilityInfo/AccessibilityInfo', () => ({
  isScreenReaderEnabled: jest.fn(() => Promise.resolve(false)),
  isVoiceOverRunning: jest.fn(() => Promise.resolve(false)),
  isBoldTextEnabled: jest.fn(() => Promise.resolve(false)),
  isGrayscaleEnabled: jest.fn(() => Promise.resolve(false)),
  isInvertColorsEnabled: jest.fn(() => Promise.resolve(false)),
  isReduceMotionEnabled: jest.fn(() => Promise.resolve(false)),
  isReduceTransparencyEnabled: jest.fn(() => Promise.resolve(false)),
  announceForAccessibility: jest.fn(),
  setAccessibilityFocus: jest.fn(),
  addEventListener: jest.fn((event, handler) => {
    mockAccessibilityEventListeners.set(event, handler);
    return {
      remove: () => mockAccessibilityEventListeners.delete(event),
    };
  }),
  removeEventListener: jest.fn((event) => {
    mockAccessibilityEventListeners.delete(event);
  }),
}));

// Helper to trigger accessibility events in tests
global.triggerAccessibilityEvent = (event, data) => {
  const handler = mockAccessibilityEventListeners.get(event);
  if (handler) {
    handler(data);
  }
};

// Extend expect with accessibility matchers (if needed)
expect.extend({
  toHaveValidA11yProps(received) {
    const hasLabel = received.accessibilityLabel || received.accessibilityLabel === '';
    const hasRole = received.accessibilityRole;
    const pass = hasLabel && hasRole;

    return {
      pass,
      message: () =>
        pass
          ? 'Expected element not to have valid accessibility props'
          : 'Expected element to have both accessibilityLabel and accessibilityRole',
    };
  },

  toHaveMinimumTouchTarget(received, minSize = 44) {
    const { width, height } = received;
    const pass = width >= minSize && height >= minSize;

    return {
      pass,
      message: () =>
        pass
          ? `Expected element not to have minimum touch target of ${minSize}x${minSize}`
          : `Expected element to have minimum touch target of ${minSize}x${minSize}, but got ${width}x${height}`,
    };
  },
});

// Cleanup after each test
afterEach(() => {
  mockAccessibilityEventListeners.clear();
});
