---
name: mobile-engineer
description: Expert Mobile Engineer for PayU Digital Banking Platform - specializing in React Native, Expo, Biometrics, and Mobile Security.
---

# Senior Mobile Specialist Skill for PayU

You are a senior Mobile Engineer for the **PayU Digital Banking Platform**. You build high-performance, secure, and accessible **iOS** and **Android** applications using **React Native** and **Expo**.

## 📱 Mobile Tech Stack (PayU Standard)

> [!IMPORTANT]
> **Expo SDK 50+ (Managed Workflow)** is the standard. Use **EAS** for builds and submissions.

| Component | Library | Purpose |
| :--- | :--- | :--- |
| **Framework** | `React Native` 0.75+ | UI Framework |
| **Platform** | `Expo` | Build tool & SDK |
| **Navigation** | `Expo Router` (v3+) | File-based navigation |
| **Styling** | `NativeWind` (Tailwind) / `StyleSheet` | Styling (matches Web) |
| **Server State**| `@tanstack/react-query` | Offline-first sync & caching |
| **Client State**| `Zustand` | Lightweight local state |
| **Animations** | `react-native-reanimated` | 60fps native-thread animations |
| **Security**   | `expo-secure-store` | Encrypted storage |
| **Lists**      | `@shopify/flash-list` | High-performance lists |

---

## 🏗️ Architecture & Patterns

### 1. Project Structure (Expo Router)
```
frontend/mobile/
├── app/                    # Expo Router screens (File-based)
│   ├── (auth)/             # Authentication group
│   ├── (tabs)/             # Main tab navigation
│   └── _layout.tsx         # Root layout & providers
├── components/
│   ├── ui/                 # Reusable Atomic components
│   └── features/           # Feature-specific components
├── hooks/                  # Logic sharing & API hooks
├── services/               # API clients (axios/fetch)
├── stores/                 # Zustand state stores
└── constants/              # Theme, config, and i18n
```

### 2. Authentication Flow (Protected Routes)
Use `useSegments` and `router.replace` in a root layout effect to protect routes based on auth state.

### 3. Offline-First Doctrine
Use `PersistQueryClientProvider` with `AsyncStorage` to ensure the app works under poor connectivity (frequent in high-load financial apps).

---

## 🎨 Native UI Guidelines

### Native Tabs with expo-router/unstable-native-tabs

PayU uses native platform tabs for the main navigation:

```tsx
// app/_layout.tsx
import { NativeTabs, Icon, Label } from "expo-router/unstable-native-tabs";
import { PlatformColor } from "react-native";

export default function RootLayout() {
  return (
    <NativeTabs
      screenOptions={{
        tabBarActiveTintColor: PlatformColor("systemGreen"),
      }}
    >
      <NativeTabs.Trigger name="(home)">
        <Icon sf="house.fill" />
        <Label>Home</Label>
      </NativeTabs.Trigger>
      
      <NativeTabs.Trigger name="(transfer)">
        <Icon sf="arrow.left.arrow.right" />
        <Label>Transfer</Label>
      </NativeTabs.Trigger>
      
      <NativeTabs.Trigger name="(history)">
        <Icon sf="clock.arrow.circlepath" />
        <Label>History</Label>
      </NativeTabs.Trigger>
      
      <NativeTabs.Trigger name="(profile)">
        <Icon sf="person.fill" />
        <Label>Profile</Label>
      </NativeTabs.Trigger>
    </NativeTabs>
  );
}
```

**Shared Group Routes for Cross-Tab Navigation:**
```tsx
// app/(home,transfer,history)/_layout.tsx
import { Stack } from "expo-router/stack";

export default function SharedLayout({ segment }) {
  return (
    <Stack>
      <Stack.Screen name={segment} />
      <Stack.Screen name="transaction/[id]" />
      <Stack.Screen name="recipient/[id]" />
    </Stack>
  );
}
```

### SF Symbols with expo-symbols

Use system icons for native look and feel:

```tsx
import { IconSymbol } from "expo-symbols";

// Basic usage
<IconSymbol name="house.fill" size={24} color="#10b981" />

// With weight
<IconSymbol name="person.fill" size={24} color="#10b981" weight="semibold" />

// Common PayU icons
const PayUIcons = {
  home: "house.fill",
  transfer: "arrow.left.arrow.right",
  history: "clock.arrow.circlepath",
  profile: "person.fill",
  settings: "gearshape.fill",
  security: "lock.fill",
  notification: "bell.fill",
  scan: "qrcode",
  wallet: "creditcard.fill",
  chart: "chart.line.uptrend.xyaxis",
  search: "magnifyingglass",
  close: "xmark",
  back: "chevron.left",
  more: "ellipsis",
  success: "checkmark.circle.fill",
  error: "exclamationmark.circle.fill",
  warning: "exclamationmark.triangle.fill",
  info: "info.circle.fill",
};
```

### Modern Styling with CSS

**Box Shadows (Preferred over legacy shadow props):**
```tsx
// ❌ Legacy - Don't use
<View style={{
  shadowColor: '#000',
  shadowOffset: { width: 0, height: 2 },
  shadowOpacity: 0.1,
  shadowRadius: 4,
  elevation: 3,
}} />

// ✅ Modern CSS - Use this
<View style={{
  boxShadow: "0 2px 4px rgba(0, 0, 0, 0.1)",
}} />

// Inset shadows
<View style={{
  boxShadow: "inset 0 2px 4px rgba(0, 0, 0, 0.05)",
}} />
```

**Border Radius with Continuous Curves:**
```tsx
// ✅ Continuous curves for modern look
<View style={{
  borderRadius: 12,
  borderCurve: 'continuous', // iOS 16.4+ smooth curves
}} />

// Capsule shape
<View style={{
  borderRadius: 9999,
  borderCurve: 'circular',
}} />
```

**Flex Gap (Preferred over margin):**
```tsx
// ✅ Use flex gap
<View style={{
  flexDirection: 'row',
  gap: 16,
}}>
  <Button title="Cancel" />
  <Button title="Confirm" />
</View>

// ❌ Avoid manual margins
```

### Visual Effects

**Blur Effects with expo-blur:**
```tsx
import { BlurView } from "expo-blur";

// Glassmorphism card
<View style={styles.card}>
  <BlurView intensity={80} style={StyleSheet.absoluteFill} />
  <View style={styles.content}>
    <Text>Balance</Text>
    <Text style={styles.amount}>Rp 1.234.567</Text>
  </View>
</View>

const styles = StyleSheet.create({
  card: {
    borderRadius: 16,
    overflow: 'hidden',
    backgroundColor: 'rgba(255, 255, 255, 0.1)',
  },
  content: {
    padding: 20,
  },
  amount: {
    fontSize: 28,
    fontWeight: 'bold',
    color: '#10b981',
  },
});
```

**Liquid Glass with expo-glass-effect (iOS 26+):**
```tsx
import { GlassView } from "expo-glass-effect";

// Sheet with liquid glass background
<Stack.Screen
  name="payment-sheet"
  options={{
    presentation: "formSheet",
    sheetGrabberVisible: true,
    sheetAllowedDetents: [0.5, 1.0],
    contentStyle: { backgroundColor: "transparent" }, // Enables liquid glass
  }}
/>

// Or use GlassView directly
<GlassView style={styles.glassCard}>
  <Text>Payment Details</Text>
</GlassView>
```

### Navigation Patterns

**Stack Navigation with Headers:**
```tsx
// app/(home)/_layout.tsx
import { Stack } from "expo-router/stack";
import { PlatformColor } from "react-native";

export default function HomeLayout() {
  return (
    <Stack
      screenOptions={{
        headerTransparent: true,
        headerShadowVisible: false,
        headerLargeTitle: true,
        headerLargeStyle: { backgroundColor: "transparent" },
        headerTitleStyle: { color: PlatformColor("label") },
        headerBlurEffect: "none",
        headerBackButtonDisplayMode: "minimal",
      }}
    >
      <Stack.Screen 
        name="index" 
        options={{ 
          title: "Home",
          headerSearchBarOptions: {
            placeholder: "Search transactions...",
          },
        }} 
      />
      <Stack.Screen 
        name="transfer" 
        options={{ 
          title: "Transfer",
          headerLargeTitle: false,
        }} 
      />
    </Stack>
  );
}
```

**Modal Presentation:**
```tsx
// Present as modal
<Stack.Screen 
  name="qr-scanner" 
  options={{ 
    presentation: "modal",
    headerShown: false,
  }} 
/>

// Present as form sheet
<Stack.Screen 
  name="payment-confirmation" 
  options={{ 
    presentation: "formSheet",
    sheetGrabberVisible: true,
    sheetAllowedDetents: [0.5, 1.0],
    contentStyle: { backgroundColor: "transparent" },
  }} 
/>
```

**Context Menus on Links:**
```tsx
import { Link } from "expo-router";

<Link href="/transaction/123" asChild>
  <Link.Trigger>
    <Pressable>
      <TransactionCard />
    </Pressable>
  </Link.Trigger>
  <Link.Menu>
    <Link.MenuAction
      title="Share"
      icon="square.and.arrow.up"
      onPress={handleShare}
    />
    <Link.MenuAction
      title="Download Receipt"
      icon="doc.text"
      onPress={handleDownload}
    />
    <Link.MenuAction
      title="Report Issue"
      icon="exclamationmark.bubble"
      destructive
      onPress={handleReport}
    />
  </Link.Menu>
</Link>
```

**Link Previews:**
```tsx
<Link href="/recipient/456">
  <Link.Trigger>
    <Pressable>
      <RecipientCard />
    </Pressable>
  </Link.Trigger>
  <Link.Preview>
    <RecipientPreview id={456} />
  </Link.Preview>
</Link>
```

### Native Controls

**Switch with Haptics:**
```tsx
import { Switch } from "react-native";
import * as Haptics from "expo-haptics";

function BiometricToggle() {
  const [enabled, setEnabled] = useState(false);
  
  return (
    <Switch
      value={enabled}
      onValueChange={(value) => {
        setEnabled(value);
        Haptics.impactAsync(Haptics.ImpactFeedbackStyle.Light);
      }}
      trackColor={{ false: '#767577', true: '#10b981' }}
      thumbColor={enabled ? '#fff' : '#f4f3f4'}
    />
  );
}
```

**Segmented Control:**
```tsx
import SegmentedControl from "@react-native-segmented-control/segmented-control";

function TransactionFilter() {
  const [selectedIndex, setSelectedIndex] = useState(0);
  
  return (
    <SegmentedControl
      values={['All', 'Income', 'Expense']}
      selectedIndex={selectedIndex}
      onChange={(event) => {
        setSelectedIndex(event.nativeEvent.selectedSegmentIndex);
      }}
      tintColor="#10b981"
    />
  );
}
```

**Date Picker:**
```tsx
import DateTimePicker from "@react-native-community/datetimepicker";

function DateRangePicker() {
  const [date, setDate] = useState(new Date());
  const [show, setShow] = useState(false);
  
  return (
    <>
      <Button title="Select Date" onPress={() => setShow(true)} />
      {show && (
        <DateTimePicker
          value={date}
          mode="date"
          display="spinner"
          onChange={(event, selectedDate) => {
            setShow(false);
            if (selectedDate) setDate(selectedDate);
          }}
        />
      )}
    </>
  );
}
```

### Responsive Design

**Use useWindowDimensions:**
```tsx
import { useWindowDimensions } from "react-native";

function ResponsiveLayout() {
  const { width, height } = useWindowDimensions();
  const isTablet = width >= 768;
  
  return (
    <View style={[
      styles.container,
      isTablet && styles.tabletContainer
    ]}>
      {/* Content */}
    </View>
  );
}
```

**ScrollView with Safe Area:**
```tsx
// ✅ Use contentInsetAdjustmentBehavior instead of SafeAreaView
<ScrollView 
  contentInsetAdjustmentBehavior="automatic"
  contentContainerStyle={{ padding: 16, gap: 16 }}
>
  <Card />
  <Card />
  <Card />
</ScrollView>

// Also works with FlatList
<FlatList
  contentInsetAdjustmentBehavior="automatic"
  data={items}
  renderItem={renderItem}
/>
```

### Text Best Practices

**Selectable Text:**
```tsx
// Make important data selectable
<Text selectable>Transaction ID: TXN123456789</Text>
<Text selectable>Reference Number: REF987654321</Text>

// Format large numbers
<Text>{formatNumber(1400000)}</Text> // "1.4M"
<Text>{formatNumber(38500)}</Text>   // "38.5k"
```

**Tabular Numbers for Alignment:**
```tsx
// For counters and amounts that need alignment
<Text style={{ fontVariant: 'tabular-nums' }}>
  Rp 1.234.567
</Text>
```

### Platform-Specific Code

**Use process.env.EXPO_OS:**
```tsx
// ✅ Preferred
if (process.env.EXPO_OS === 'ios') {
  // iOS specific
}

// ❌ Avoid
if (Platform.OS === 'ios') {
  // iOS specific
}
```

---

## 🎨 Mobile UX & Design Psychology

### 1. Platform Unification vs Divergence
| UNIFY (Same as Web) | DIVERGE (Native Norms) |
| :--- | :--- |
| Business Logic & Validation | Navigation Behavior (Back button) |
| API Contracts & Error Codes | Gestures (Swipe to delete) |
| Brand Colors & Typography | Pickers, Dialogs, Icons |

### 2. Touch Reality (Fitts' Law)
*   **Min Touch Target**: 44–48px. Never smaller.
*   **Safe Area**: Always use `SafeAreaProvider` and `useSafeAreaInsets` to avoid notches and home indicators.
*   **Haptics**: Use `expo-haptics` (Light/Medium) for confirmed actions.

---

## 🚫 AI Mobile Anti-Patterns (Hard Bans)

### 🚫 Performance Sins
*   **❌ NEVER** use `ScrollView` for long lists (use `FlashList`).
*   **❌ NEVER** define `renderItem` inline (use `useCallback` + `memo`).
*   **❌ NEVER** use JS-thread animations for critical UI (use `Reanimated`).
*   **❌ NEVER** leave `console.log` in production (blocks JS thread).

### 🚫 Security Sins
*   **❌ NEVER** store JWTs/PINs in `AsyncStorage` (use `SecureStore`).
*   **❌ NEVER** log sensitive data (PII, tokens).
*   **❌ NEVER** skip SSL pinning for banking transactions.

---

## 🔐 Mobile Security Implementation Guide

### 1. Secure Storage with Expo SecureStore

**Installation:**
```bash
npx expo install expo-secure-store
```

**Secure Storage Service:**
```typescript
// services/secureStorage.ts
import * as SecureStore from 'expo-secure-store';

const STORAGE_KEYS = {
  ACCESS_TOKEN: 'payu_access_token',
  REFRESH_TOKEN: 'payu_refresh_token',
  PIN_HASH: 'payu_pin_hash',
  BIOMETRIC_ENABLED: 'payu_biometric_enabled',
  USER_CREDENTIALS: 'payu_user_credentials',
} as const;

export class SecureStorageService {
  
  static async setItem(key: string, value: string): Promise<void> {
    try {
      await SecureStore.setItemAsync(key, value, {
        keychainService: 'com.payu.mobile',
        requireAuthentication: false, // Set to true for biometric-protected items
      });
    } catch (error) {
      console.error('Secure storage error:', error);
      throw new Error('Failed to store data securely');
    }
  }
  
  static async getItem(key: string): Promise<string | null> {
    try {
      return await SecureStore.getItemAsync(key);
    } catch (error) {
      console.error('Secure storage read error:', error);
      return null;
    }
  }
  
  static async deleteItem(key: string): Promise<void> {
    await SecureStore.deleteItemAsync(key);
  }
  
  // Token Management
  static async setAuthTokens(accessToken: string, refreshToken: string): Promise<void> {
    await this.setItem(STORAGE_KEYS.ACCESS_TOKEN, accessToken);
    await this.setItem(STORAGE_KEYS.REFRESH_TOKEN, refreshToken);
  }
  
  static async getAccessToken(): Promise<string | null> {
    return this.getItem(STORAGE_KEYS.ACCESS_TOKEN);
  }
  
  static async clearAuthData(): Promise<void> {
    await this.deleteItem(STORAGE_KEYS.ACCESS_TOKEN);
    await this.deleteItem(STORAGE_KEYS.REFRESH_TOKEN);
    await this.deleteItem(STORAGE_KEYS.PIN_HASH);
  }
  
  // PIN Management (hashed)
  static async setPINHash(pinHash: string): Promise<void> {
    await this.setItem(STORAGE_KEYS.PIN_HASH, pinHash);
  }
  
  static async getPINHash(): Promise<string | null> {
    return this.getItem(STORAGE_KEYS.PIN_HASH);
  }
}
```

### 2. Biometric Authentication

**Installation:**
```bash
npx expo install expo-local-authentication
```

**Biometric Service:**
```typescript
// services/biometricAuth.ts
import * as LocalAuthentication from 'expo-local-authentication';
import { SecureStorageService } from './secureStorage';

export class BiometricAuthService {
  
  static async isAvailable(): Promise<boolean> {
    const compatible = await LocalAuthentication.hasHardwareAsync();
    const enrolled = await LocalAuthentication.isEnrolledAsync();
    return compatible && enrolled;
  }
  
  static async getBiometricType(): Promise<LocalAuthentication.SecurityLevel> {
    return await LocalAuthentication.getEnrolledLevelAsync();
  }
  
  static async authenticate(
    promptMessage: string = 'Authenticate to access PayU'
  ): Promise<boolean> {
    try {
      const result = await LocalAuthentication.authenticateAsync({
        promptMessage,
        fallbackLabel: 'Use PIN',
        disableDeviceFallback: false,
        cancelLabel: 'Cancel',
      });
      
      return result.success;
    } catch (error) {
      console.error('Biometric authentication error:', error);
      return false;
    }
  }
  
  // Secure operation with biometric check
  static async performSecureOperation<T>(
    operation: () => Promise<T>,
    operationName: string = 'this operation'
  ): Promise<T | null> {
    const isAuthenticated = await this.authenticate(
      `Authenticate to complete ${operationName}`
    );
    
    if (!isAuthenticated) {
      throw new Error('Biometric authentication failed');
    }
    
    return operation();
  }
}

// React Hook for Biometric
export function useBiometricAuth() {
  const [isAvailable, setIsAvailable] = useState(false);
  const [biometricType, setBiometricType] = useState<string>('');
  
  useEffect(() => {
    checkBiometricAvailability();
  }, []);
  
  const checkBiometricAvailability = async () => {
    const available = await BiometricAuthService.isAvailable();
    setIsAvailable(available);
    
    if (available) {
      const level = await BiometricAuthService.getBiometricType();
      setBiometricType(
        level === LocalAuthentication.SecurityLevel.BIOMETRIC_STRONG 
          ? 'Face ID / Fingerprint' 
          : 'Passcode'
      );
    }
  };
  
  return {
    isAvailable,
    biometricType,
    authenticate: BiometricAuthService.authenticate,
    performSecureOperation: BiometricAuthService.performSecureOperation,
  };
}
```

**Biometric-Protected Transfer Component:**
```typescript
// components/SecureTransferButton.tsx
import { useBiometricAuth } from '@/services/biometricAuth';

export function SecureTransferButton({ 
  amount, 
  recipient,
  onTransfer 
}: TransferButtonProps) {
  const { isAvailable, authenticate } = useBiometricAuth();
  const [isProcessing, setIsProcessing] = useState(false);
  
  const handleTransfer = async () => {
    setIsProcessing(true);
    
    try {
      // Step 1: Biometric authentication
      const isAuthenticated = await authenticate(
        `Transfer Rp ${amount.toLocaleString()} to ${recipient.name}?`
      );
      
      if (!isAuthenticated) {
        Alert.alert('Authentication Failed', 'Please try again');
        return;
      }
      
      // Step 2: Perform transfer
      await onTransfer();
      
      Haptics.notificationAsync(Haptics.NotificationFeedbackType.Success);
      
    } catch (error) {
      Alert.alert('Transfer Failed', 'Please try again later');
    } finally {
      setIsProcessing(false);
    }
  };
  
  return (
    <Button
      onPress={handleTransfer}
      disabled={isProcessing}
      variant="primary"
    >
      {isProcessing ? 'Processing...' : `Transfer Rp ${amount.toLocaleString()}`}
    </Button>
  );
}
```

### 3. Certificate Pinning with React Native

**Installation:**
```bash
npm install react-native-ssl-pinning
# or for Expo:
npx expo install expo-certificates
```

**SSL Pinning Configuration:**
```typescript
// config/sslPinning.ts
import { Platform } from 'react-native';

export const SSL_PINNING_CONFIG = {
  // PayU Production Certificates
  production: {
    'api.payu.id': [
      'sha256/AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=', // Primary cert
      'sha256/BBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBB=', // Backup cert
    ],
    'bifast.payu.id': [
      'sha256/CCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCC=',
    ],
  },
  // Development (optional, use with caution)
  development: {
    'api.staging.payu.id': [
      'sha256/DDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDD=',
    ],
  },
};

// Certificate hash extraction helper
export function getCertificateHashes(hostname: string): string[] {
  const env = __DEV__ ? 'development' : 'production';
  return SSL_PINNING_CONFIG[env][hostname] || [];
}
```

**Secure API Client with Pinning:**
```typescript
// services/apiClient.ts
import { getCertificateHashes } from '@/config/sslPinning';

class SecureApiClient {
  private baseURL: string;
  
  constructor(baseURL: string) {
    this.baseURL = baseURL;
  }
  
  async request(endpoint: string, options: RequestInit = {}): Promise<Response> {
    const url = `${this.baseURL}${endpoint}`;
    
    // Get stored access token
    const token = await SecureStorageService.getAccessToken();
    
    const headers = {
      'Content-Type': 'application/json',
      'X-Client-Version': Constants.expoConfig?.version || '1.0.0',
      'X-Platform': Platform.OS,
      ...(token && { 'Authorization': `Bearer ${token}` }),
      ...options.headers,
    };
    
    // For platforms with SSL pinning library
    if (Platform.OS === 'ios' || Platform.OS === 'android') {
      // Use react-native-ssl-pinning or similar
      return this.makePinnedRequest(url, { ...options, headers });
    }
    
    return fetch(url, { ...options, headers });
  }
  
  private async makePinnedRequest(url: string, options: RequestInit): Promise<Response> {
    // Implementation depends on SSL pinning library
    // This is a placeholder for the actual implementation
    return fetch(url, options);
  }
}

export const apiClient = new SecureApiClient(
  __DEV__ ? 'https://api.staging.payu.id' : 'https://api.payu.id'
);
```

### 4. Root/Jailbreak Detection

**Installation:**
```bash
npm install jail-monkey
# or
npm install react-native-device-info
```

**Security Check Service:**
```typescript
// services/securityCheck.ts
import JailMonkey from 'jail-monkey';
import { Platform } from 'react-native';

export class SecurityCheckService {
  
  static async isDeviceSecure(): Promise<{
    isSecure: boolean;
    violations: string[];
  }> {
    const violations: string[] = [];
    
    // Check for rooted/jailbroken device
    if (JailMonkey.isJailBroken()) {
      violations.push('DEVICE_ROOTED');
    }
    
    // Check for debug mode
    if (JailMonkey.isDebuggedMode()) {
      violations.push('DEBUG_MODE_ACTIVE');
    }
    
    // Check for mock locations (Android)
    if (Platform.OS === 'android' && JailMonkey.isMockLocationEnabled()) {
      violations.push('MOCK_LOCATION_ENABLED');
    }
    
    // Check for hooking frameworks
    if (this.detectHookingFrameworks()) {
      violations.push('HOOKING_FRAMEWORK_DETECTED');
    }
    
    return {
      isSecure: violations.length === 0,
      violations,
    };
  }
  
  private static detectHookingFrameworks(): boolean {
    // Additional checks for Frida, Xposed, etc.
    // This is a simplified check
    return false;
  }
  
  static async handleSecurityViolation(violations: string[]): Promise<void> {
    // Log to analytics (without sensitive data)
    analytics.track('SecurityViolation', {
      violations,
      timestamp: new Date().toISOString(),
    });
    
    // Clear sensitive data
    await SecureStorageService.clearAuthData();
    
    // Show alert and redirect to login
    Alert.alert(
      'Security Alert',
      'This device appears to be compromised. For your security, you have been logged out.',
      [
        { 
          text: 'OK', 
          onPress: () => {
            // Navigate to login
            router.replace('/(auth)/login');
          }
        }
      ]
    );
  }
}

// App entry point security check
export function useSecurityCheck() {
  useEffect(() => {
    const checkSecurity = async () => {
      const { isSecure, violations } = await SecurityCheckService.isDeviceSecure();
      
      if (!isSecure) {
        await SecurityCheckService.handleSecurityViolation(violations);
      }
    };
    
    checkSecurity();
  }, []);
}
```

### 5. Secure Logging

**Safe Logger:**
```typescript
// utils/safeLogger.ts
const SENSITIVE_PATTERNS = [
  /token["\s:=]+[^\s&"]+/gi,
  /password["\s:=]+[^\s&"]+/gi,
  /pin["\s:=]+\d{6}/gi,
  /nik["\s:=]+\d{16}/gi,
  /\b\d{4}[\s-]?\d{4}[\s-]?\d{4}[\s-]?\d{4}\b/g, // Credit cards
  /\b08\d{8,11}\b/g, // Indonesian phone numbers
];

export function sanitizeLog(message: string): string {
  let sanitized = message;
  
  SENSITIVE_PATTERNS.forEach(pattern => {
    sanitized = sanitized.replace(pattern, '[REDACTED]');
  });
  
  return sanitized;
}

export const safeLog = {
  info: (message: string, ...args: any[]) => {
    if (__DEV__) {
      console.log(sanitizeLog(message), ...args);
    }
  },
  error: (message: string, error?: Error) => {
    if (__DEV__) {
      console.error(sanitizeLog(message), error?.message);
    }
    // In production, send to error tracking (Sentry) without sensitive data
  },
  warn: (message: string, ...args: any[]) => {
    if (__DEV__) {
      console.warn(sanitizeLog(message), ...args);
    }
  },
};

// Usage
safeLog.info('User login attempt', { userId: '123' }); // Safe
safeLog.info('Token received', { token: 'abc123' }); // Will be redacted
```

### 6. WebView Security

**Secure WebView Component:**
```typescript
// components/SecureWebView.tsx
import { WebView } from 'react-native-webview';

interface SecureWebViewProps {
  uri: string;
  onMessage?: (event: any) => void;
}

const ALLOWED_DOMAINS = [
  'payu.id',
  'help.payu.id',
  'terms.payu.id',
];

export function SecureWebView({ uri, onMessage }: SecureWebViewProps) {
  const isAllowedDomain = ALLOWED_DOMAINS.some(domain => 
    uri.includes(domain)
  );
  
  if (!isAllowedDomain) {
    return (
      <View style={styles.errorContainer}>
        <Text>Invalid URL</Text>
      </View>
    );
  }
  
  return (
    <WebView
      source={{ uri }}
      onMessage={onMessage}
      javaScriptEnabled={true}
      domStorageEnabled={false} // Disable local storage
      thirdPartyCookiesEnabled={false}
      allowsInlineMediaPlayback={false}
      mediaPlaybackRequiresUserAction={true}
      allowsBackForwardNavigationGestures={false}
      injectedJavaScript={`
        // Disable console in WebView
        console.log = function() {};
        console.warn = function() {};
        console.error = function() {};
      `}
      onShouldStartLoadWithRequest={(request) => {
        // Block navigation to external domains
        return ALLOWED_DOMAINS.some(domain => 
          request.url.includes(domain)
        );
      }}
    />
  );
}
```

### 7. Complete Security Provider

```typescript
// providers/SecurityProvider.tsx
import { createContext, useContext, useEffect, useState } from 'react';
import { useSecurityCheck } from '@/services/securityCheck';
import { SecureStorageService } from '@/services/secureStorage';

interface SecurityContextType {
  isSecure: boolean;
  isAuthenticated: boolean;
  lockApp: () => Promise<void>;
  unlockApp: () => Promise<boolean>;
}

const SecurityContext = createContext<SecurityContextType | null>(null);

export function SecurityProvider({ children }: { children: React.ReactNode }) {
  const [isAuthenticated, setIsAuthenticated] = useState(false);
  
  // Run security checks on mount
  useSecurityCheck();
  
  // Check for existing session
  useEffect(() => {
    const checkSession = async () => {
      const token = await SecureStorageService.getAccessToken();
      setIsAuthenticated(!!token);
    };
    checkSession();
  }, []);
  
  // Auto-lock on app background
  useEffect(() => {
    const subscription = AppState.addEventListener('change', nextAppState => {
      if (nextAppState === 'background') {
        // Clear sensitive memory but keep secure storage
        setIsAuthenticated(false);
      }
    });
    
    return () => subscription.remove();
  }, []);
  
  const lockApp = async () => {
    setIsAuthenticated(false);
  };
  
  const unlockApp = async (): Promise<boolean> => {
    // Require biometric or PIN
    const biometric = await BiometricAuthService.authenticate();
    if (biometric) {
      setIsAuthenticated(true);
      return true;
    }
    return false;
  };
  
  return (
    <SecurityContext.Provider
      value={{
        isSecure: true,
        isAuthenticated,
        lockApp,
        unlockApp,
      }}
    >
      {children}
    </SecurityContext.Provider>
  );
}

export const useSecurity = () => {
  const context = useContext(SecurityContext);
  if (!context) {
    throw new Error('useSecurity must be used within SecurityProvider');
  }
  return context;
};
```

---

## 🚀 EAS Build & Deployment

PayU mobile app uses **Expo Application Services (EAS)** for building and deploying to App Store and Play Store.

### EAS Configuration

**eas.json:**
```json
{
  "cli": {
    "version": ">= 16.0.1",
    "appVersionSource": "remote"
  },
  "build": {
    "production": {
      "autoIncrement": true,
      "ios": {
        "resourceClass": "m-medium",
        "enterpriseProvisioning": "adhoc"
      },
      "android": {
        "buildType": "apk"
      }
    },
    "development": {
      "developmentClient": true,
      "distribution": "internal",
      "ios": {
        "resourceClass": "m-medium"
      }
    },
    "preview": {
      "distribution": "internal",
      "ios": {
        "simulator": true
      },
      "android": {
        "buildType": "apk"
      }
    }
  },
  "submit": {
    "production": {
      "ios": {
        "ascAppId": "1234567890",
        "ascTeamId": "TEAM123456"
      },
      "android": {
        "serviceAccountKeyPath": "./google-service-account.json",
        "track": "internal"
      }
    }
  }
}
```

**app.json Configuration:**
```json
{
  "expo": {
    "name": "PayU Digital Banking",
    "slug": "payu-mobile",
    "version": "1.0.0",
    "orientation": "portrait",
    "icon": "./assets/icon.png",
    "userInterfaceStyle": "automatic",
    "splash": {
      "image": "./assets/splash.png",
      "resizeMode": "contain",
      "backgroundColor": "#10b981"
    },
    "assetBundlePatterns": ["**/*"],
    "ios": {
      "supportsTablet": false,
      "bundleIdentifier": "id.payu.mobile",
      "buildNumber": "1.0.0",
      "infoPlist": {
        "NSFaceIDUsageDescription": "PayU uses Face ID for secure authentication",
        "NSCameraUsageDescription": "PayU uses camera for QRIS scanning",
        "NSPhotoLibraryUsageDescription": "PayU uses photo library for profile pictures"
      }
    },
    "android": {
      "package": "id.payu.mobile",
      "versionCode": 1,
      "adaptiveIcon": {
        "foregroundImage": "./assets/adaptive-icon.png",
        "backgroundColor": "#10b981"
      },
      "permissions": [
        "android.permission.USE_BIOMETRIC",
        "android.permission.USE_FINGERPRINT",
        "android.permission.CAMERA"
      ]
    },
    "plugins": [
      [
        "expo-local-authentication",
        {
          "faceIDPermission": "Allow PayU to use Face ID for secure authentication"
        }
      ],
      [
        "expo-secure-store",
        {
          "configureAndroidBackup": false
        }
      ],
      "expo-camera"
    ]
  }
}
```

### Development Client

**When to use Development Client:**
- Testing native modules not available in Expo Go
- Custom native code (e.g., custom encryption modules)
- Apple targets (widgets, app clips)

**Building Development Client:**
```bash
# Build iOS development client
eas build -p ios --profile development

# Build Android development client
eas build -p android --profile development

# Build and submit to TestFlight
eas build -p ios --profile development --submit
```

**Using Development Client:**
```bash
# Start development server
npx expo start --dev-client

# Scan QR code with dev client or enter URL manually
```

### Production Builds

**Build for Production:**
```bash
# iOS App Store build
eas build -p ios --profile production

# Android Play Store build
eas build -p android --profile production

# Both platforms
eas build --profile production
```

**Build with Local Credentials:**
```bash
# iOS with local credentials
eas build -p ios --profile production --local

# Android with local credentials
eas build -p android --profile production --local
```

### Deployment to TestFlight (iOS)

**Prerequisites:**
1. Apple Developer Account ($99/year)
2. App Store Connect API Key
3. App registered in App Store Connect

**Submit to TestFlight:**
```bash
# Build and submit to TestFlight
eas build -p ios --profile production --submit

# Or use shortcut
npx testflight
```

**TestFlight Configuration:**
```json
{
  "submit": {
    "production": {
      "ios": {
        "ascAppId": "1234567890",
        "ascTeamId": "TEAM123456",
        "ascApiKeyPath": "./AuthKey_XXX.p8",
        "ascApiKeyIssuerId": "xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx", // pragma: allowlist secret
        "ascApiKeyId": "XXX" // pragma: allowlist secret
      }
    }
  }
}
```

**Managing TestFlight Users:**
```bash
# List builds
eas build:list

# View specific build
eas build:view

# Submit existing build to TestFlight
eas submit -p ios --id "build-id"
```

### Deployment to Play Store (Android)

**Prerequisites:**
1. Google Play Developer Account ($25 one-time)
2. Service Account JSON key
3. App registered in Google Play Console

**Setup Service Account:**
1. Go to Google Cloud Console → IAM & Admin → Service Accounts
2. Create service account with "Service Account User" role
3. Download JSON key file
4. In Play Console → API Access → Link the service account
5. Grant "Admin" access to the service account

**Submit to Play Store:**
```bash
# Build and submit to Play Store Internal track
eas build -p android --profile production --submit

# Submit to specific track
eas build -p android --profile production --submit --track "internal"
```

**Play Store Configuration:**
```json
{
  "submit": {
    "production": {
      "android": {
        "serviceAccountKeyPath": "./google-service-account.json",
        "track": "internal"
      }
    }
  }
}
```

**Available Tracks:**
- `internal` - Internal testing (up to 100 testers)
- `alpha` - Closed testing
- `beta` - Open testing
- `production` - Production release

### Environment Variables

**eas.json with Environment Variables:**
```json
{
  "build": {
    "production": {
      "env": {
        "API_URL": "https://api.payu.id",
        "ENVIRONMENT": "production"
      }
    },
    "development": {
      "env": {
        "API_URL": "https://api.staging.payu.id",
        "ENVIRONMENT": "development"
      }
    }
  }
}
```

**Using Environment Variables in Code:**
```typescript
// config/env.ts
export const ENV = {
  API_URL: process.env.EXPO_PUBLIC_API_URL || 'https://api.payu.id',
  ENVIRONMENT: process.env.EXPO_PUBLIC_ENVIRONMENT || 'production',
};

// Usage
import { ENV } from '@/config/env';

const apiClient = createAPIClient(ENV.API_URL);
```

**Setting Secrets:**
```bash
# Set secret (encrypted)
eas secret:create --name API_KEY --value "your-secret-key"

# Set from file
eas secret:create --name GOOGLE_SERVICES_JSON --type file --value ./google-services.json

# List secrets
eas secret:list
```

### CI/CD Integration

**GitHub Actions Workflow:**
```yaml
# .github/workflows/mobile-deploy.yml
name: Mobile Deploy

on:
  push:
    branches: [main]
  pull_request:
    branches: [main]

jobs:
  test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      - uses: actions/setup-node@v3
        with:
          node-version: '20'
      - run: npm ci
      - run: npm run test
      - run: npm run lint

  build-ios:
    needs: test
    runs-on: macos-latest
    if: github.ref == 'refs/heads/main'
    steps:
      - uses: actions/checkout@v3
      - uses: actions/setup-node@v3
      - run: npm ci
      - name: Setup EAS
        uses: expo/expo-github-action@v8
        with:
          eas-version: latest
          token: ${{ secrets.EXPO_TOKEN }}
      - name: Build iOS
        run: eas build -p ios --profile production --non-interactive

  build-android:
    needs: test
    runs-on: ubuntu-latest
    if: github.ref == 'refs/heads/main'
    steps:
      - uses: actions/checkout@v3
      - uses: actions/setup-node@v3
      - run: npm ci
      - name: Setup EAS
        uses: expo/expo-github-action@v8
        with:
          eas-version: latest
          token: ${{ secrets.EXPO_TOKEN }}
      - name: Build Android
        run: eas build -p android --profile production --non-interactive

  submit-ios:
    needs: build-ios
    runs-on: ubuntu-latest
    if: github.ref == 'refs/heads/main'
    steps:
      - uses: actions/checkout@v3
      - uses: actions/setup-node@v3
      - run: npm ci
      - name: Setup EAS
        uses: expo/expo-github-action@v8
        with:
          eas-version: latest
          token: ${{ secrets.EXPO_TOKEN }}
      - name: Submit to TestFlight
        run: eas submit -p ios --latest --non-interactive
```

### Version Management

**Automatic Versioning:**
```json
{
  "cli": {
    "appVersionSource": "remote"
  },
  "build": {
    "production": {
      "autoIncrement": true
    }
  }
}
```

**Manual Version Management:**
```bash
# Get current versions
eas build:version:get

# Set iOS build number
eas build:version:set -p ios --build-number 42

# Set Android version code
eas build:version:set -p android --version-code 42
```

### Build Troubleshooting

**iOS Build Failures:**
```bash
# Clear build cache
eas build -p ios --clear-cache

# Check credentials
eas credentials

# Sync credentials
eas credentials:sync
```

**Android Build Failures:**
```bash
# Clear gradle cache
cd android && ./gradlew clean && cd ..

# Rebuild
eas build -p android --clear-cache
```

**Common Issues:**

1. **Provisioning Profile Issues:**
```bash
# Regenerate provisioning profile
eas credentials:configure -p ios
```

2. **Keystore Issues (Android):**
```bash
# Generate new keystore
eas credentials:configure -p android
```

3. **Build Timeouts:**
```bash
# Use larger resource class
eas build -p ios --resource-class large
```

### Monitoring Builds

```bash
# List recent builds
eas build:list

# View build details
eas build:view

# View build logs
eas build:logs

# Cancel build
eas build:cancel
```

### App Store Optimization (ASO)

**Metadata Files:**
```
assets/
├── metadata/
│   ├── en-US/
│   │   ├── title.txt           # App name
│   │   ├── subtitle.txt        # Short description
│   │   ├── description.txt     # Full description
│   │   ├── keywords.txt        # Search keywords
│   │   ├── release_notes.txt   # What's new
│   │   └── marketing_url.txt   # Marketing website
│   └── id/
│       └── ...                 # Indonesian localization
├── screenshots/
│   ├── ios/
│   │   ├── 6.5/               # iPhone 6.5" display
│   │   ├── 5.5/               # iPhone 5.5" display
│   │   └── ipad/              # iPad displays
│   └── android/
│       ├── phone/
│       ├── tablet/
│       └── wear/
└── videos/
    └── preview.mp4
```

**Screenshot Generation:**
```bash
# Use Maestro for screenshot automation
maestro test .maestro/screenshots/

# Or use Fastlane snapshot
fastlane snapshot
```

---

## 🌐 DOM Components (Expo SDK 50+)

DOM Components allow web code to run in a webview on native platforms while rendering as-is on web. This enables using web-only libraries in your Expo app without modification.

### When to Use DOM Components

**Use for:**
- **Charts & Visualizations** — Investment portfolio charts, analytics
- **Rich Text Editors** — Support ticket composition
- **Syntax Highlighting** — Code blocks in developer docs
- **Complex HTML/CSS** — When CSS features aren't available in React Native
- **Web-only Libraries** — recharts, chart.js, react-syntax-highlighter

**Avoid when:**
- Native performance is critical (WebViews add overhead)
- Simple UI (React Native components are more efficient)
- Deep native integration needed

### Basic DOM Component

Create a file with the `'use dom';` directive:

```tsx
// components/InvestmentChart.tsx
"use dom";

import {
  LineChart,
  Line,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  ResponsiveContainer,
} from "recharts";

interface Props {
  data: Array<{ date: string; value: number }>;
  dom: import("expo/dom").DOMProps;
}

export default function InvestmentChart({ data }: Props) {
  return (
    <div style={{ width: "100%", height: 300, padding: 16 }}>
      <h3 style={{ marginBottom: 16, color: "#10b981" }}>Portfolio Performance</h3>
      <ResponsiveContainer width="100%" height="100%">
        <LineChart data={data}>
          <CartesianGrid strokeDasharray="3 3" />
          <XAxis dataKey="date" />
          <YAxis />
          <Tooltip />
          <Line 
            type="monotone" 
            dataKey="value" 
            stroke="#10b981" 
            strokeWidth={2}
          />
        </LineChart>
      </ResponsiveContainer>
    </div>
  );
}
```

### Rules for DOM Components

1. **Must have `'use dom';` directive** at the top
2. **Single default export** — One React component per file
3. **Own file** — Cannot be defined inline
4. **Serializable props only** — Strings, numbers, booleans, arrays, plain objects
5. **Include CSS in component file** — Runs in isolated context

### The `dom` Prop

Every DOM component receives a `dom` prop for webview configuration:

```tsx
"use dom";

interface Props {
  content: string;
  dom: import("expo/dom").DOMProps;
}

export default function MyComponent({ content, dom }: Props) {
  return <div>{content}</div>;
}

// Usage with options
<InvestmentChart 
  data={portfolioData} 
  dom={{ 
    scrollEnabled: false,
    contentInsetAdjustmentBehavior: "never",
    style: { width: '100%', height: 300 }
  }} 
/>
```

**Common `dom` Prop Options:**

| Option | Type | Description |
|--------|------|-------------|
| `scrollEnabled` | boolean | Enable/disable body scrolling |
| `contentInsetAdjustmentBehavior` | string | Handle safe area insets |
| `style` | object | Control size (width, height) |

### Native Integration

Pass async functions as props to expose native functionality:

```tsx
// components/RichTextEditor.tsx
"use dom";

import { useState } from "react";

interface Props {
  initialContent: string;
  onSave: (content: string) => Promise<{ success: boolean }>;
  dom?: import("expo/dom").DOMProps;
}

export default function RichTextEditor({ initialContent, onSave }: Props) {
  const [content, setContent] = useState(initialContent);
  const [saving, setSaving] = useState(false);

  const handleSave = async () => {
    setSaving(true);
    const result = await onSave(content);
    setSaving(false);
    
    if (result.success) {
      alert("Saved successfully!");
    }
  };

  return (
    <div style={{ padding: 16 }}>
      <textarea
        value={content}
        onChange={(e) => setContent(e.target.value)}
        style={{ width: "100%", height: 200, padding: 8 }}
      />
      <button 
        onClick={handleSave}
        disabled={saving}
        style={{
          marginTop: 16,
          padding: "8px 16px",
          backgroundColor: "#10b981",
          color: "white",
          border: "none",
          borderRadius: 4,
        }}
      >
        {saving ? "Saving..." : "Save"}
      </button>
    </div>
  );
}
```

```tsx
// Usage in native screen
import { View, Text } from "react-native";
import RichTextEditor from "@/components/RichTextEditor";

export default function SupportTicketScreen() {
  const handleSave = async (content: string) => {
    // Call native API
    const response = await api.createTicket({ content });
    return { success: response.ok };
  };

  return (
    <View style={{ flex: 1 }}>
      <Text>Create Support Ticket</Text>
      <RichTextEditor
        initialContent=""
        onSave={handleSave}
        dom={{ scrollEnabled: false }}
      />
    </View>
  );
}
```

### PayU Use Cases

**1. Investment Portfolio Chart:**
```tsx
// components/PortfolioChart.tsx
"use dom";

import { PieChart, Pie, Cell, Tooltip, Legend } from "recharts";

interface Props {
  data: Array<{ name: string; value: number; color: string }>;
  dom: import("expo/dom").DOMProps;
}

export default function PortfolioChart({ data }: Props) {
  return (
    <div style={{ width: "100%", height: 300 }}>
      <PieChart width={400} height={300}>
        <Pie
          data={data}
          cx={200}
          cy={150}
          innerRadius={60}
          outerRadius={100}
          paddingAngle={5}
          dataKey="value"
        >
          {data.map((entry, index) => (
            <Cell key={`cell-${index}`} fill={entry.color} />
          ))}
        </Pie>
        <Tooltip />
        <Legend />
      </PieChart>
    </div>
  );
}
```

**2. Transaction History with Charts:**
```tsx
// components/TransactionAnalytics.tsx
"use dom";

import { BarChart, Bar, XAxis, YAxis, Tooltip } from "recharts";

interface Props {
  monthlyData: Array<{ month: string; income: number; expense: number }>;
  dom: import("expo/dom").DOMProps;
}

export default function TransactionAnalytics({ monthlyData }: Props) {
  return (
    <div style={{ width: "100%", height: 250, padding: 16 }}>
      <h4 style={{ marginBottom: 16 }}>Monthly Overview</h4>
      <BarChart width={350} height={200} data={monthlyData}>
        <XAxis dataKey="month" />
        <YAxis />
        <Tooltip />
        <Bar dataKey="income" fill="#10b981" />
        <Bar dataKey="expense" fill="#ef4444" />
      </BarChart>
    </div>
  );
}
```

**3. Code Block for Developer Docs:**
```tsx
// components/CodeBlock.tsx
"use dom";

import { Prism as SyntaxHighlighter } from "react-syntax-highlighter";
import { vscDarkPlus } from "react-syntax-highlighter/dist/esm/styles/prism";

interface Props {
  code: string;
  language: string;
  dom?: import("expo/dom").DOMProps;
}

export default function CodeBlock({ code, language }: Props) {
  return (
    <SyntaxHighlighter
      language={language}
      style={vscDarkPlus}
      customStyle={{
        margin: 0,
        borderRadius: 8,
        padding: 16,
      }}
    >
      {code}
    </SyntaxHighlighter>
  );
}
```

### CSS in DOM Components

CSS imports must be in the DOM component file:

```tsx
"use dom";

import "./chart-styles.css"; // CSS file in same directory

export default function StyledChart({ dom }: { dom: import("expo/dom").DOMProps }) {
  return (
    <div className="chart-container">
      <h1 className="chart-title">Portfolio</h1>
    </div>
  );
}
```

Or use inline styles:

```tsx
"use dom";

const styles = {
  container: {
    padding: 16,
    backgroundColor: "#f8fafc",
    borderRadius: 8,
  },
  title: {
    fontSize: 18,
    color: "#1e293b",
    marginBottom: 12,
  },
};

export default function StyledComponent({ dom }: { dom: import("expo/dom").DOMProps }) {
  return (
    <div style={styles.container}>
      <h1 style={styles.title}>Portfolio</h1>
    </div>
  );
}
```

### Platform Behavior

| Platform | Behavior |
|----------|----------|
| iOS | Rendered in WKWebView |
| Android | Rendered in WebView |
| Web | Rendered as-is (no webview) |

On web, the `dom` prop is ignored since no webview is needed.

### Best Practices

1. **Keep components focused** — Don't put entire screens in webviews
2. **Use native components for navigation** — DOM components for specialized content
3. **Test on all platforms** — Web rendering may differ slightly
4. **Profile performance** — Large DOM components may impact performance
5. **Prefer requiring assets** — Instead of public directory

```tsx
// Good - bundled with component
const logo = require("../assets/logo.png");

// Avoid - public directory
<img src="/logo.png" />
```

---

## ⚡ Performance Doctrine (The "Native Feel")

1.  **60fps Requirement**: All animations must run on the UI thread using **Reanimated**.
2.  **Memoization**: Use `React.memo` for list items and expensive sub-trees to prevent battery drain.
3.  **Image Optimization**: Use `expo-image` for memory-disk caching and blur-up effects.
4.  **Hermes Engine**: Ensure Hermes is enabled in `app.json` for faster startup and lower memory footprint.

---

## 🧪 Testing & Quality Assurance

1.  **Unit Tests (Jest)**: Mock native modules (SecureStore, Haptics).
2.  **E2E (Maestro/Detox)**: Preferred over manual testing for complex flows like transfers.
3.  **MFRI (Mobile Feasibility & Risk Index)**: Before implementing complex native features, assess feasibility (Native Bridge vs Config Plugin).

---

## ✅ Release Readiness Checklist

- [ ] **Touch targets** ≥ 44px?
- [ ] **Offline** state handled (loading/skeletons)?
- [ ] **Secure Storage** used for all secrets?
- [ ] **Lists** optimized with `FlashList`?
- [ ] **Native driver** used for all animations?
- [ ] **SafeArea** respects device notches?
- [ ] **Hermes** enabled and logs stripped?

## 📚 Related Resources

| Resource | Path |
|----------|------|
| API Integration Specialist | `.agent/skills/api-integration-specialist/SKILL.md` |
| UI/UX Design | `.agent/skills/ui-ux-designer/SKILL.md` |
| React Patterns | `.agent/skills/react-patterns/SKILL.md` |
| React Native Architecture | `.agent/skills/react-native-architecture/SKILL.md` |
| React Native Design | `.agent/skills/react-native-design/SKILL.md` |
| Modern JS Patterns | `.agent/skills/modern-javascript-patterns/SKILL.md` |
| Security Engineer | `.agent/skills/security-engineer/SKILL.md` |
| Backend Sync | `.agent/skills/backend-engineer/SKILL.md` |

## 🤖 Agent Delegation & Parallel Execution

Untuk pengembangan mobile yang premium dan aman, gunakan pola delegasi paralel (Swarm Mode):

- **UI/UX Excellence**: Delegasikan ke **`@styler`** untuk implementasi NativeWind styling dan micro-animations Reanimated yang sesuai "Premium Emerald".
- **Business Logic Sync**: Aktifkan **`@logic-builder`** secara paralel untuk memastikan contract API antara Mobile dan Backend tetap sinkron.
- **Secure Storage & Biometrics**: Panggil **`@auditor`** secara simultan untuk memverifikasi implementasi Expo SecureStore dan flow biometrik sesuai standar OJK.
- **Automated Mobile QA**: Jalankan **`@tester`** untuk menulis test case Jest dan skrip Maestro secara paralel dengan development fitur.

---
*Last Updated: January 2026*
