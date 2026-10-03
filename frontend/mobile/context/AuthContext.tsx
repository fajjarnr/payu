/**
 * AuthContext - Authentication Context for PayU Mobile App
 *
 * Security policy P2-C2: tokens live only in SecureStore (encrypted) and are never
 * held in this context, React state, or the React Query cache.
 * Server state is managed by TanStack Query; Zustand holds UI preferences only.
 */
import React, { createContext, useContext, useEffect, useState, ReactNode } from 'react';
import { useRouter, useSegments } from 'expo-router';
import { useAuthState, useInitializeAuth } from '@/src/hooks/useAuthQuery';
import { Logger } from '@/utils/logger';
import { User } from '@/types';

interface AuthContextType {
  isAuthenticated: boolean;
  isLoading: boolean;
  user: User | null;
}

const AuthContext = createContext<AuthContextType>({
  isAuthenticated: false,
  isLoading: true,
  user: null,
});

export const useAuthContext = () => useContext(AuthContext);

export const AuthProvider: React.FC<{ children: ReactNode }> = ({ children }) => {
  const router = useRouter();
  const segments = useSegments();
  const { getUser, getSession } = useAuthState();
  const { initialize } = useInitializeAuth();
  const [isLoading, setIsLoading] = useState(true);

  const user = getUser();
  const session = getSession();
  const isAuthenticated = session?.isAuthenticated ?? false;

  /**
   * Initialize auth state from SecureStore. P2-C2: no tokens loaded into memory;
   * P2-C3: sanitized logger prevents token leakage.
   */
  useEffect(() => {
    const initializeAuth = async () => {
      try {
        await initialize();
      } catch (error) {
        // Sanitized logging - no tokens in logs
        Logger.error('AuthContext', 'Auth initialization error', error);
      } finally {
        setIsLoading(false);
      }
    };

    initializeAuth();
  }, [initialize]);

  /** Route protection: unauthenticated users go to login, authenticated users away from auth screens. */
  useEffect(() => {
    if (isLoading) return;

    const inAuthGroup = segments[0] === '(auth)';

    if (isAuthenticated && inAuthGroup) {
      router.replace('/(tabs)');
    } else if (!isAuthenticated && !inAuthGroup) {
      router.replace('/(auth)/login');
    }
  }, [isAuthenticated, segments, isLoading, router]);

  return (
    <AuthContext.Provider value={{ isAuthenticated, isLoading, user }}>
      {children}
    </AuthContext.Provider>
  );
};
