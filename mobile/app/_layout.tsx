import { useEffect } from 'react';
import { Stack } from 'expo-router';
import { StatusBar } from 'expo-status-bar';
import * as SplashScreen from 'expo-splash-screen';
import { SafeAreaProvider } from 'react-native-safe-area-context';
import { ThemeProvider } from '@/context/ThemeContext';
import { AuthProvider } from '@/context/AuthContext';
import { NotificationProvider } from '@/context/NotificationContext';

// Keep the splash screen visible while we fetch resources
SplashScreen.preventAutoHideAsync();

export default function RootLayout() {
  useEffect(() => {
    SplashScreen.hideAsync();
  }, []);

  return (
    <SafeAreaProvider>
      <ThemeProvider>
        <AuthProvider>
          <NotificationProvider>
            <StatusBar style="auto" />
            <Stack screenOptions={{ headerShown: false }}>
              <Stack.Screen name="(auth)" options={{ headerShown: false }} />
              <Stack.Screen name="(tabs)" options={{ headerShown: false }} />
              <Stack.Screen
                name="qris"
                options={{
                  headerShown: true,
                  title: 'QRIS Scanner',
                  headerStyle: { backgroundColor: '#10b981' },
                  headerTintColor: '#fff',
                  headerTitleStyle: { fontWeight: '700' },
                }}
              />
              <Stack.Screen
                name="feedback"
                options={{
                  headerShown: true,
                  title: 'Send Feedback',
                  headerStyle: { backgroundColor: '#10b981' },
                  headerTintColor: '#fff',
                  headerTitleStyle: { fontWeight: '700' },
                }}
              />
            </Stack>
          </NotificationProvider>
        </AuthProvider>
      </ThemeProvider>
    </SafeAreaProvider>
  );
}
