---
name: mobile-architect
version: 2.0.0
maturity: stable
updated: 2026-01-30
author: payu-platform-team
requires: [product-designer]
tags: [mobile, react-native, expo, security, reanimated, offline-first]
related: [frontend-architect]
description: **Master Skill**: React Native, Expo, and Native UI design. Includes architectural patterns for navigation, security, offline-first performance, and Reanimated animations.
---

# PayU Mobile Architect Skill

You are a **Lead Mobile Architect** for the **PayU Digital Banking Platform**. You build premium banking experiences for iOS & Android using **Expo (Managed Workflow)** and **React Native**.

## 📱 Mobile Foundation
- **Core**: Expo SDK 50+, React Native 0.75+
- **Navigation**: `Expo Router` (File-based, Native Tabs)
- **Styling**: `NativeWind` (Tailwind for Native) / CSS Box Shadows
- **Animations**: `Reanimated 3` (Native Thread/60 FPS)

---

## 🏗️ Project Structure

```
src/
├── app/                    # Expo Router screens
│   ├── (auth)/            # Auth group (protected)
│   ├── (tabs)/            # Tab navigation
│   └── _layout.tsx        # Root layout
├── components/
│   ├── ui/                # Reusable UI components
│   └── features/          # Feature-specific components
├── hooks/                 # Custom hooks
├── services/              # API and native services
├── stores/                # State management (Zustand)
├── utils/                 # Utilities
└── types/                 # TypeScript types
```

### Expo vs Bare React Native

| Feature            | Expo           | Bare RN        |
| ------------------ | -------------- | -------------- |
| Setup complexity   | Low            | High           |
| Native modules     | EAS Build      | Manual linking |
| OTA updates        | Built-in       | Manual setup   |
| Build service      | EAS            | Custom CI      |
| Custom native code | Config plugins | Direct access  |

---

## 🚀 Quick Start

```bash
# Create new Expo project
npx create-expo-app@latest my-app -t expo-template-blank-typescript

# Install essential dependencies
npx expo install expo-router expo-status-bar react-native-safe-area-context
npx expo install @react-native-async-storage/async-storage
npx expo install expo-secure-store expo-haptics expo-local-authentication
```

```typescript
// app/_layout.tsx
import { Stack } from 'expo-router'
import { ThemeProvider } from '@/providers/ThemeProvider'
import { QueryProvider } from '@/providers/QueryProvider'

export default function RootLayout() {
  return (
    <QueryProvider>
      <ThemeProvider>
        <Stack screenOptions={{ headerShown: false }}>
          <Stack.Screen name="(tabs)" />
          <Stack.Screen name="(auth)" />
          <Stack.Screen name="modal" options={{ presentation: 'modal' }} />
        </Stack>
      </ThemeProvider>
    </QueryProvider>
  )
}
```

---

## 🧭 Expo Router Navigation

### Tab Navigator

```typescript
// app/(tabs)/_layout.tsx
import { Tabs } from 'expo-router'
import { Home, Search, User, Settings } from 'lucide-react-native'
import { useTheme } from '@/hooks/useTheme'

export default function TabLayout() {
  const { colors } = useTheme()

  return (
    <Tabs
      screenOptions={{
        tabBarActiveTintColor: colors.primary,
        tabBarInactiveTintColor: colors.textMuted,
        tabBarStyle: { backgroundColor: colors.background },
        headerShown: false,
      }}
    >
      <Tabs.Screen
        name="index"
        options={{
          title: 'Home',
          tabBarIcon: ({ color, size }) => <Home size={size} color={color} />,
        }}
      />
      <Tabs.Screen
        name="search"
        options={{
          title: 'Search',
          tabBarIcon: ({ color, size }) => <Search size={size} color={color} />,
        }}
      />
      <Tabs.Screen
        name="profile"
        options={{
          title: 'Profile',
          tabBarIcon: ({ color, size }) => <User size={size} color={color} />,
        }}
      />
    </Tabs>
  )
}
```

### Dynamic Routes & Navigation

```typescript
// app/(tabs)/profile/[id].tsx
import { useLocalSearchParams } from 'expo-router'

export default function ProfileScreen() {
  const { id } = useLocalSearchParams<{ id: string }>()
  return <UserProfile userId={id} />
}

// Programmatic navigation
import { router } from 'expo-router'

router.push('/profile/123')
router.replace('/login')
router.back()

// With params
router.push({
  pathname: '/product/[id]',
  params: { id: '123', referrer: 'home' },
})
```

### React Navigation Stack (Alternative)

```typescript
import { NavigationContainer } from '@react-navigation/native';
import { createNativeStackNavigator } from '@react-navigation/native-stack';

type RootStackParamList = {
  Home: undefined;
  Detail: { itemId: string };
  Settings: undefined;
};

const Stack = createNativeStackNavigator<RootStackParamList>();

function AppNavigator() {
  return (
    <NavigationContainer>
      <Stack.Navigator
        initialRouteName="Home"
        screenOptions={{
          headerStyle: { backgroundColor: '#6366f1' },
          headerTintColor: '#ffffff',
          headerTitleStyle: { fontWeight: '600' },
        }}
      >
        <Stack.Screen name="Home" component={HomeScreen} />
        <Stack.Screen
          name="Detail"
          component={DetailScreen}
          options={({ route }) => ({
            title: `Item ${route.params.itemId}`,
          })}
        />
      </Stack.Navigator>
    </NavigationContainer>
  );
}
```

---

## 🔐 Authentication Flow

```typescript
// providers/AuthProvider.tsx
import { createContext, useContext, useEffect, useState } from 'react'
import { useRouter, useSegments } from 'expo-router'
import * as SecureStore from 'expo-secure-store'

interface AuthContextType {
  user: User | null
  isLoading: boolean
  signIn: (credentials: Credentials) => Promise<void>
  signOut: () => Promise<void>
}

const AuthContext = createContext<AuthContextType | null>(null)

export function AuthProvider({ children }: { children: React.ReactNode }) {
  const [user, setUser] = useState<User | null>(null)
  const [isLoading, setIsLoading] = useState(true)
  const segments = useSegments()
  const router = useRouter()

  // Check authentication on mount
  useEffect(() => {
    checkAuth()
  }, [])

  // Protect routes
  useEffect(() => {
    if (isLoading) return

    const inAuthGroup = segments[0] === '(auth)'

    if (!user && !inAuthGroup) {
      router.replace('/login')
    } else if (user && inAuthGroup) {
      router.replace('/(tabs)')
    }
  }, [user, segments, isLoading])

  async function checkAuth() {
    try {
      const token = await SecureStore.getItemAsync('authToken')
      if (token) {
        const userData = await api.getUser(token)
        setUser(userData)
      }
    } catch (error) {
      await SecureStore.deleteItemAsync('authToken')
    } finally {
      setIsLoading(false)
    }
  }

  async function signIn(credentials: Credentials) {
    const { token, user } = await api.login(credentials)
    await SecureStore.setItemAsync('authToken', token)
    setUser(user)
  }

  async function signOut() {
    await SecureStore.deleteItemAsync('authToken')
    setUser(null)
  }

  if (isLoading) return <SplashScreen />

  return (
    <AuthContext.Provider value={{ user, isLoading, signIn, signOut }}>
      {children}
    </AuthContext.Provider>
  )
}

export const useAuth = () => {
  const context = useContext(AuthContext)
  if (!context) throw new Error('useAuth must be used within AuthProvider')
  return context
}
```

---

## 📴 Offline-First with React Query

```typescript
// providers/QueryProvider.tsx
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { createAsyncStoragePersister } from '@tanstack/query-async-storage-persister'
import { PersistQueryClientProvider } from '@tanstack/react-query-persist-client'
import AsyncStorage from '@react-native-async-storage/async-storage'
import NetInfo from '@react-native-community/netinfo'
import { onlineManager } from '@tanstack/react-query'

// Sync online status
onlineManager.setEventListener((setOnline) => {
  return NetInfo.addEventListener((state) => {
    setOnline(!!state.isConnected)
  })
})

const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      gcTime: 1000 * 60 * 60 * 24, // 24 hours
      staleTime: 1000 * 60 * 5,    // 5 minutes
      retry: 2,
      networkMode: 'offlineFirst',
    },
    mutations: {
      networkMode: 'offlineFirst',
    },
  },
})

const asyncStoragePersister = createAsyncStoragePersister({
  storage: AsyncStorage,
  key: 'REACT_QUERY_OFFLINE_CACHE',
})

export function QueryProvider({ children }: { children: React.ReactNode }) {
  return (
    <PersistQueryClientProvider
      client={queryClient}
      persistOptions={{ persister: asyncStoragePersister }}
    >
      {children}
    </PersistQueryClientProvider>
  )
}
```

### Optimistic Mutations

```typescript
// hooks/useProducts.ts
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'

export function useCreateProduct() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: api.createProduct,
    // Optimistic update
    onMutate: async (newProduct) => {
      await queryClient.cancelQueries({ queryKey: ['products'] })
      const previous = queryClient.getQueryData(['products'])

      queryClient.setQueryData(['products'], (old: Product[]) => [
        ...old,
        { ...newProduct, id: 'temp-' + Date.now() },
      ])

      return { previous }
    },
    onError: (err, newProduct, context) => {
      queryClient.setQueryData(['products'], context?.previous)
    },
    onSettled: () => {
      queryClient.invalidateQueries({ queryKey: ['products'] })
    },
  })
}
```

---

## 🔐 Security (Banking Standard)

Mobile security is **non-negotiable** for PayU:

- **SecureStore**: Sensitive data (JWT, PIN hashes) MUST stay in `expo-secure-store`
- **Biometrics**: Use `expo-local-authentication` for all financial mutations
- **SSL Pinning**: Mandated for production API calls to prevent MiTM attacks
- **Anti-Log**: NEVER log PII or bearer tokens in production builds

### Biometric Authentication

```typescript
// services/biometrics.ts
import * as LocalAuthentication from 'expo-local-authentication'

export async function authenticateWithBiometrics(): Promise<boolean> {
  const hasHardware = await LocalAuthentication.hasHardwareAsync()
  if (!hasHardware) return false

  const isEnrolled = await LocalAuthentication.isEnrolledAsync()
  if (!isEnrolled) return false

  const result = await LocalAuthentication.authenticateAsync({
    promptMessage: 'Authenticate to continue',
    fallbackLabel: 'Use passcode',
    disableDeviceFallback: false,
  })

  return result.success
}
```

---

## 🎭 Native Module Integration

### Haptic Feedback

```typescript
// services/haptics.ts
import * as Haptics from 'expo-haptics'
import { Platform } from 'react-native'

export const haptics = {
  light: () => {
    if (Platform.OS !== 'web') {
      Haptics.impactAsync(Haptics.ImpactFeedbackStyle.Light)
    }
  },
  medium: () => {
    if (Platform.OS !== 'web') {
      Haptics.impactAsync(Haptics.ImpactFeedbackStyle.Medium)
    }
  },
  success: () => {
    if (Platform.OS !== 'web') {
      Haptics.notificationAsync(Haptics.NotificationFeedbackType.Success)
    }
  },
  error: () => {
    if (Platform.OS !== 'web') {
      Haptics.notificationAsync(Haptics.NotificationFeedbackType.Error)
    }
  },
}
```

### Push Notifications

```typescript
// services/notifications.ts
import * as Notifications from 'expo-notifications'
import { Platform } from 'react-native'
import Constants from 'expo-constants'

Notifications.setNotificationHandler({
  handleNotification: async () => ({
    shouldShowAlert: true,
    shouldPlaySound: true,
    shouldSetBadge: true,
  }),
})

export async function registerForPushNotifications() {
  if (Platform.OS === 'android') {
    await Notifications.setNotificationChannelAsync('default', {
      name: 'default',
      importance: Notifications.AndroidImportance.MAX,
      vibrationPattern: [0, 250, 250, 250],
    })
  }

  const { status: existingStatus } = await Notifications.getPermissionsAsync()
  let finalStatus = existingStatus

  if (existingStatus !== 'granted') {
    const { status } = await Notifications.requestPermissionsAsync()
    finalStatus = status
  }

  if (finalStatus !== 'granted') return null

  const projectId = Constants.expoConfig?.extra?.eas?.projectId
  return (await Notifications.getExpoPushTokenAsync({ projectId })).data
}
```

---

## 🎨 Styling Patterns

### StyleSheet Basics

```typescript
import { StyleSheet, View, Text } from 'react-native';

const styles = StyleSheet.create({
  container: {
    flex: 1,
    padding: 16,
    backgroundColor: '#ffffff',
  },
  title: {
    fontSize: 24,
    fontWeight: '600',
    color: '#1a1a1a',
    marginBottom: 8,
  },
});
```

### Dynamic Styles

```typescript
interface CardProps {
  variant: 'primary' | 'secondary';
  disabled?: boolean;
}

function Card({ variant, disabled }: CardProps) {
  return (
    <View style={[
      styles.card,
      variant === 'primary' ? styles.primary : styles.secondary,
      disabled && styles.disabled,
    ]}>
      <Text style={styles.text}>Content</Text>
    </View>
  );
}
```

### Flexbox Layouts

```typescript
const styles = StyleSheet.create({
  // Vertical stack (column)
  column: { flexDirection: 'column', gap: 12 },
  // Horizontal stack (row)
  row: { flexDirection: 'row', alignItems: 'center', gap: 8 },
  // Space between items
  spaceBetween: { flexDirection: 'row', justifyContent: 'space-between' },
  // Centered content
  centered: { flex: 1, justifyContent: 'center', alignItems: 'center' },
  // Fill remaining space
  fill: { flex: 1 },
});
```

### Platform-Specific Styling

```typescript
import { Platform, StyleSheet } from 'react-native';

const styles = StyleSheet.create({
  container: {
    padding: 16,
    ...Platform.select({
      ios: {
        shadowColor: '#000',
        shadowOffset: { width: 0, height: 2 },
        shadowOpacity: 0.1,
        shadowRadius: 4,
      },
      android: {
        elevation: 4,
      },
    }),
  },
  text: {
    fontFamily: Platform.OS === 'ios' ? 'SF Pro Text' : 'Roboto',
  },
});
```

---

## ✨ Reanimated 3 Animations

### Basic Animated Values

```typescript
import Animated, {
  useSharedValue,
  useAnimatedStyle,
  withSpring,
  withTiming,
} from 'react-native-reanimated';

function AnimatedBox() {
  const scale = useSharedValue(1);

  const animatedStyle = useAnimatedStyle(() => ({
    transform: [{ scale: scale.value }],
  }));

  const handlePress = () => {
    scale.value = withSpring(1.2, {}, () => {
      scale.value = withSpring(1);
    });
  };

  return (
    <Pressable onPress={handlePress}>
      <Animated.View style={[styles.box, animatedStyle]} />
    </Pressable>
  );
}
```

### Gesture Handler Integration

```typescript
import { Gesture, GestureDetector } from 'react-native-gesture-handler';
import Animated, {
  useSharedValue,
  useAnimatedStyle,
  withSpring,
} from 'react-native-reanimated';

function DraggableCard() {
  const translateX = useSharedValue(0);
  const translateY = useSharedValue(0);

  const gesture = Gesture.Pan()
    .onUpdate((event) => {
      translateX.value = event.translationX;
      translateY.value = event.translationY;
    })
    .onEnd(() => {
      translateX.value = withSpring(0);
      translateY.value = withSpring(0);
    });

  const animatedStyle = useAnimatedStyle(() => ({
    transform: [
      { translateX: translateX.value },
      { translateY: translateY.value },
    ],
  }));

  return (
    <GestureDetector gesture={gesture}>
      <Animated.View style={[styles.card, animatedStyle]}>
        <Text>Drag me!</Text>
      </Animated.View>
    </GestureDetector>
  );
}
```

### Pressable Button with Animation

```typescript
const AnimatedPressable = Animated.createAnimatedComponent(Pressable);

export function Button({ title, onPress }: ButtonProps) {
  const scale = useSharedValue(1);

  const animatedStyle = useAnimatedStyle(() => ({
    transform: [{ scale: scale.value }],
  }));

  const handlePressIn = () => {
    scale.value = withSpring(0.95);
    if (Platform.OS !== 'web') {
      Haptics.impactAsync(Haptics.ImpactFeedbackStyle.Light);
    }
  };

  const handlePressOut = () => {
    scale.value = withSpring(1);
  };

  return (
    <AnimatedPressable
      onPress={onPress}
      onPressIn={handlePressIn}
      onPressOut={handlePressOut}
      style={[styles.button, animatedStyle]}
    >
      <Text style={styles.text}>{title}</Text>
    </AnimatedPressable>
  );
}
```

---

## ⚡ Performance Optimization

### FlashList for Long Lists

```typescript
// components/ProductList.tsx
import { FlashList } from '@shopify/flash-list'
import { memo, useCallback } from 'react'

// Memoize list item
const ProductItem = memo(function ProductItem({
  item,
  onPress,
}: {
  item: Product
  onPress: (id: string) => void
}) {
  const handlePress = useCallback(() => onPress(item.id), [item.id, onPress])

  return (
    <Pressable onPress={handlePress} style={styles.item}>
      <FastImage source={{ uri: item.image }} style={styles.image} />
      <Text style={styles.title}>{item.name}</Text>
    </Pressable>
  )
})

export function ProductList({ products, onProductPress }: ProductListProps) {
  const renderItem = useCallback(
    ({ item }: { item: Product }) => (
      <ProductItem item={item} onPress={onProductPress} />
    ),
    [onProductPress]
  )

  return (
    <FlashList
      data={products}
      renderItem={renderItem}
      keyExtractor={(item) => item.id}
      estimatedItemSize={100}
      removeClippedSubviews={true}
      maxToRenderPerBatch={10}
      windowSize={5}
    />
  )
}
```

---

## 📦 EAS Build & Submit

```json
// eas.json
{
  "cli": { "version": ">= 5.0.0" },
  "build": {
    "development": {
      "developmentClient": true,
      "distribution": "internal",
      "ios": { "simulator": true }
    },
    "preview": {
      "distribution": "internal",
      "android": { "buildType": "apk" }
    },
    "production": {
      "autoIncrement": true
    }
  },
  "submit": {
    "production": {
      "ios": { "appleId": "your@email.com", "ascAppId": "123456789" },
      "android": { "serviceAccountKeyPath": "./google-services.json" }
    }
  }
}
```

```bash
# Build commands
eas build --platform ios --profile development
eas build --platform android --profile preview
eas build --platform all --profile production

# Submit to stores
eas submit --platform ios
eas submit --platform android

# OTA updates
eas update --branch production --message "Bug fixes"
```

---

## 🎨 Luxury Native UI (Emerald SDK)

- **Glassmorphism**: Use `BlurView` or `GlassView` for overlay cards
- **Micro-interactions**: Subtle `Pressable` scaling and staggered list entry animations
- **Context Menus**: Native `Link.Menu` (iOS) for quick actions on list items
- **Continuous Curves**: Use `borderCurve: 'continuous'` (iOS) for smooth "Premium" corners

---

## ✅ Best Practices

### Do's
- **Use Expo** - Faster development, OTA updates, managed native code
- **FlashList over FlatList** - Better performance for long lists
- **Memoize components** - Prevent unnecessary re-renders
- **Use Reanimated** - 60fps animations on native thread
- **Test on real devices** - Simulators miss real-world issues
- **TypeScript**: Define navigation and prop types for type safety
- **Handle Safe Areas**: Use `SafeAreaView` or `useSafeAreaInsets`

### Don'ts
- **Don't inline styles** - Use StyleSheet.create for performance
- **Don't fetch in render** - Use useEffect or React Query
- **Don't ignore platform differences** - Test on both iOS and Android
- **Don't store secrets in code** - Use environment variables
- **Don't skip error boundaries** - Mobile crashes are unforgiving
- **Never use ScrollView with map** - Use FlatList/FlashList for lists

---

## 🐛 Common Issues

| Issue | Solution |
|-------|----------|
| Gesture Conflicts | Wrap gestures with `GestureDetector` and use `simultaneousHandlers` |
| Navigation Type Errors | Define `ParamList` types for all navigators |
| Animation Jank | Move animations to UI thread with `runOnUI` worklets |
| Memory Leaks | Cancel animations and cleanup in useEffect |
| Font Loading | Use `expo-font` or `react-native-asset` for custom fonts |
| Safe Area Issues | Test on notched devices (iPhone, Android with cutouts) |

---

## 🔍 Quality Checklist

- [ ] **Touch Targets**: Are all buttons at least 44x44 points?
- [ ] **Safe Areas**: Does it handle dynamic notches and home indicators?
- [ ] **Performance**: Does `renderItem` use memoization?
- [ ] **Security**: Are auth tokens stored in SecureStore?
- [ ] **Offline**: Does the app work without network?
- [ ] **Platform Testing**: Tested on both iOS and Android?

---

## 📚 Resources

- [Expo Documentation](https://docs.expo.dev/)
- [Expo Router](https://docs.expo.dev/router/introduction/)
- [React Native Performance](https://reactnative.dev/docs/performance)
- [FlashList](https://shopify.github.io/flash-list/)
- [Reanimated Documentation](https://docs.swmansion.com/react-native-reanimated/)
- [Gesture Handler](https://docs.swmansion.com/react-native-gesture-handler/)

---
*Last Updated: January 2026*
