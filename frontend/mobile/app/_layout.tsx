import { useEffect } from 'react';
import { Stack } from 'expo-router';
import { StatusBar } from 'expo-status-bar';
import * as SplashScreen from 'expo-splash-screen';
import { SafeAreaProvider } from 'react-native-safe-area-context';
import { ThemeProvider } from '@/context/ThemeContext';
import { AuthProvider } from '@/context/AuthContext';
import { NotificationProvider } from '@/context/NotificationContext';
import { QueryProvider } from '@/src/providers/QueryProvider';
import { useDeeplinkHandler } from '@/hooks/useDeeplinkHandler';

// Keep the splash screen visible while we fetch resources
SplashScreen.preventAutoHideAsync();

// Deeplink handler wrapper component
function DeeplinkHandler({ children }: { children: React.ReactNode }) {
  useDeeplinkHandler();
  return children;
}

export default function RootLayout() {
  useEffect(() => {
    SplashScreen.hideAsync();
  }, []);

  return (
    <SafeAreaProvider>
      <QueryProvider>
        <ThemeProvider>
          <AuthProvider>
            <NotificationProvider>
              <DeeplinkHandler>
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
              </DeeplinkHandler>
            </NotificationProvider>
          </AuthProvider>
        </ThemeProvider>
      </QueryProvider>
    </SafeAreaProvider>
  );
}
