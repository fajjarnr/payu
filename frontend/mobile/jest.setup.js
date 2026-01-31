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
const mockStorageData = new Map();
const mockAsyncStorage = {
  getItem: jest.fn((key) => Promise.resolve(mockStorageData.get(key) || null)),
  setItem: jest.fn((key, value) => {
    mockStorageData.set(key, value);
    return Promise.resolve();
  }),
  removeItem: jest.fn((key) => {
    mockStorageData.delete(key);
    return Promise.resolve();
  }),
  clear: jest.fn(() => {
    mockStorageData.clear();
    return Promise.resolve();
  }),
  getAllKeys: jest.fn(() => Promise.resolve(Array.from(mockStorageData.keys()))),
};

jest.mock('@react-native-async-storage/async-storage', () => mockAsyncStorage);

// Clear mock storage before each test
beforeEach(() => {
  mockStorageData.clear();
});

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

// Create mock functions that can be imported and controlled in tests
const mockIsScreenReaderEnabled = jest.fn(() => Promise.resolve(false));
const mockIsVoiceOverRunning = jest.fn(() => Promise.resolve(false));
const mockIsBoldTextEnabled = jest.fn(() => Promise.resolve(false));
const mockIsGrayscaleEnabled = jest.fn(() => Promise.resolve(false));
const mockIsInvertColorsEnabled = jest.fn(() => Promise.resolve(false));
const mockIsReduceMotionEnabled = jest.fn(() => Promise.resolve(false));
const mockIsReduceTransparencyEnabled = jest.fn(() => Promise.resolve(false));
const mockAnnounceForAccessibility = jest.fn();
const mockSetAccessibilityFocus = jest.fn();
const mockAddEventListener = jest.fn((event, handler) => {
  mockAccessibilityEventListeners.set(event, handler);
  return {
    remove: () => mockAccessibilityEventListeners.delete(event),
  };
});
const mockRemoveEventListener = jest.fn((event) => {
  mockAccessibilityEventListeners.delete(event);
});

jest.mock('react-native/Libraries/Components/AccessibilityInfo/AccessibilityInfo', () => ({
  isScreenReaderEnabled: mockIsScreenReaderEnabled,
  isVoiceOverRunning: mockIsVoiceOverRunning,
  isBoldTextEnabled: mockIsBoldTextEnabled,
  isGrayscaleEnabled: mockIsGrayscaleEnabled,
  isInvertColorsEnabled: mockIsInvertColorsEnabled,
  isReduceMotionEnabled: mockIsReduceMotionEnabled,
  isReduceTransparencyEnabled: mockIsReduceTransparencyEnabled,
  announceForAccessibility: mockAnnounceForAccessibility,
  setAccessibilityFocus: mockSetAccessibilityFocus,
  addEventListener: mockAddEventListener,
  removeEventListener: mockRemoveEventListener,
}));

// Export mocks for use in tests
global.mockAccessibility = {
  mockIsScreenReaderEnabled,
  mockIsVoiceOverRunning,
  mockIsBoldTextEnabled,
  mockIsGrayscaleEnabled,
  mockIsInvertColorsEnabled,
  mockIsReduceMotionEnabled,
  mockIsReduceTransparencyEnabled,
  mockAnnounceForAccessibility,
  mockSetAccessibilityFocus,
  mockAddEventListener,
  mockRemoveEventListener,
  mockAccessibilityEventListeners,
};

// Helper to trigger accessibility events in tests
global.triggerAccessibilityEvent = (event, data) => {
  const handler = mockAccessibilityEventListeners.get(event);
  if (handler) {
    handler(data);
  }
};

// Helper to reset accessibility mocks
beforeEach(() => {
  mockAccessibilityEventListeners.clear();
  mockIsScreenReaderEnabled.mockClear();
  mockIsVoiceOverRunning.mockClear();
  mockIsBoldTextEnabled.mockClear();
  mockIsGrayscaleEnabled.mockClear();
  mockIsInvertColorsEnabled.mockClear();
  mockIsReduceMotionEnabled.mockClear();
  mockIsReduceTransparencyEnabled.mockClear();
  mockAnnounceForAccessibility.mockClear();
  mockSetAccessibilityFocus.mockClear();
  mockAddEventListener.mockClear();
  mockRemoveEventListener.mockClear();
  // Set default return values
  mockIsScreenReaderEnabled.mockResolvedValue(false);
  mockIsVoiceOverRunning.mockResolvedValue(false);
  mockIsBoldTextEnabled.mockResolvedValue(false);
  mockIsGrayscaleEnabled.mockResolvedValue(false);
  mockIsInvertColorsEnabled.mockResolvedValue(false);
  mockIsReduceMotionEnabled.mockResolvedValue(false);
  mockIsReduceTransparencyEnabled.mockResolvedValue(false);
});

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
