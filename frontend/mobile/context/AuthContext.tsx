/**
 * AuthContext - Authentication Context for PayU Mobile App
 *
 * STATE MANAGEMENT ARCHITECTURE:
 * ==============================
 * This context uses a unified state management approach:
 * - Server State: TanStack Query (useAuthState, useInitializeAuth)
 * - UI State: Zustand (useAuthStore - minimal UI preferences only)
 * - Token Storage: SecureStore (encrypted) - ONLY location for tokens
 *
 * SECURITY POLICY P2-C2: Token Storage
 * =====================================
 * CRITICAL: Tokens are NEVER stored in this context, React state, or React Query cache.
 *
 * Token Storage Locations:
 * ------------------------
 * 1. SecureStore (encrypted) - ONLY location for tokens
 *    - Access token
 *    - Refresh token
 *    - Token expiry
 *
 * 2. TanStack Query Cache - Non-sensitive data only
 *    - User profile (public info)
 *    - isAuthenticated flag (boolean)
 *    - Session state
 *
 * 3. Zustand Store - UI preferences only
 *    - lastLoginAttempt (for rate limiting UI)
 *    - biometricPromptEnabled (UI preference)
 *
 * 4. NEVER stored:
 *    - AsyncStorage (excluded)
 *    - Context state (this file)
 *    - React state
 *
 * Initialization Flow:
 * --------------------
 * 1. App starts, AuthProvider initializes
 * 2. useInitializeAuth reads tokens from SecureStore
 * 3. If tokens exist, set isAuthenticated = true in React Query cache
 * 4. If no tokens, redirect to login
 *
 * MIGRATION NOTE:
 * ---------------
 * Previously used Zustand for auth state (user, isAuthenticated).
 * Now uses TanStack Query for server state, Zustand for UI state only.
 *
 * @module AuthContext
 * @version 3.0.0 - Unified State Management
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

  // Get auth state from React Query cache
  const user = getUser();
  const session = getSession();
  const isAuthenticated = session?.isAuthenticated ?? false;

  /**
   * Initialize authentication state
   *
   * SECURITY P2-C2: This only checks SecureStore for tokens.
   * No tokens are loaded into memory or state.
   *
   * Logging P2-C3: Sanitized logger used to prevent token leakage
   */
  useEffect(() => {
    const initializeAuth = async () => {
      try {
        // Initialize auth state from SecureStore
        // This populates the React Query cache
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

  /**
   * Route protection based on authentication state
   *
   * Redirects unauthenticated users to login
   * Redirects authenticated users away from auth screens
   */
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
