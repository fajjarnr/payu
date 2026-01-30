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
