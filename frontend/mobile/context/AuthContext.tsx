import React, { createContext, useContext, useEffect, useState, ReactNode } from 'react';
import { useRouter, useSegments } from 'expo-router';
import { useAuthStore } from '@/store/authStore';
import { storage } from '@/utils/storage';
import { AUTH_CONFIG } from '@/constants/config';
import { User } from '@/types';

/**
 * AuthContext - Authentication Context for PayU Mobile App
 *
 * SECURITY POLICY: Token Storage (P2-C2)
 * ========================================
 *
 * This context manages authentication state and routing protection.
 * CRITICAL: Tokens are NEVER stored in this context or React state.
 *
 * Token Storage Locations:
 * ------------------------
 * 1. SecureStore (encrypted) - ONLY location for tokens
 *    - Access token
 *    - Refresh token
 *    - Token expiry
 *
 * 2. Memory (Zustand store) - Non-sensitive data only
 *    - User profile (public info)
 *    - isAuthenticated flag (boolean)
 *    - isLoading flag
 *
 * 3. NEVER stored:
 *    - React Query cache (excluded via SENSITIVE_QUERY_KEYS)
 *    - AsyncStorage (excluded via noOpStorage in authStore)
 *    - Context state (this file)
 *
 * Initialization Flow:
 * --------------------
 * 1. App starts, AuthProvider initializes
 * 2. checkAuthStatus() reads tokens from SecureStore
 * 3. If tokens exist, set isAuthenticated = true
 * 4. If no tokens, redirect to login
 *
 * @module AuthContext
 * @version 2.0.0 - Secure token storage
 */

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
  const { user, isAuthenticated, checkAuthStatus } = useAuthStore();
  const [isLoading, setIsLoading] = useState(true);

  /**
   * Initialize authentication state
   *
   * SECURITY: This only checks SecureStore for tokens.
   * No tokens are loaded into memory or state.
   */
  useEffect(() => {
    const initializeAuth = async () => {
      try {
        // Check if valid tokens exist in SecureStore
        // This updates the Zustand store's isAuthenticated flag
        await checkAuthStatus();
      } catch (error) {
        console.error('Auth initialization error:', error);
      } finally {
        setIsLoading(false);
      }
    };

    initializeAuth();
  }, [checkAuthStatus]);

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
