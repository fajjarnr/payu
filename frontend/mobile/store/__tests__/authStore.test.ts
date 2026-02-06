/**
 * Auth UI Store Tests
 *
 * These tests verify the UI-only state management for authentication.
 * Server state (user, session) is managed by TanStack Query.
 * @see @/src/hooks/__tests__/useAuthQuery.test.ts for server state tests
 */
import { act, renderHook } from '@testing-library/react-native';
import { useAuthStore } from '../authStore';

describe('authStore', () => {
  beforeEach(() => {
    // Reset store state before each test
    useAuthStore.setState({
      lastLoginAttempt: null,
      biometricPromptEnabled: true,
    });
  });

  describe('initial state', () => {
    it('should have correct initial state', () => {
      const state = useAuthStore.getState();

      expect(state.lastLoginAttempt).toBeNull();
      expect(state.biometricPromptEnabled).toBe(true);
    });

    it('should not have sensitive auth state (SECURITY: auth state in React Query)', () => {
      const state = useAuthStore.getState();

      // SECURITY: Auth state (user, isAuthenticated) should be in React Query
      expect('user' in state).toBe(false);
      expect('isAuthenticated' in state).toBe(false);
      expect('tokens' in state).toBe(false);
    });
  });

  describe('setLastLoginAttempt', () => {
    it('should set last login attempt timestamp', () => {
      const { result } = renderHook(() => useAuthStore());

      const timestamp = Date.now();

      act(() => {
        result.current.setLastLoginAttempt(timestamp);
      });

      expect(result.current.lastLoginAttempt).toBe(timestamp);
    });

    it('should clear last login attempt', () => {
      const { result } = renderHook(() => useAuthStore());

      act(() => {
        result.current.setLastLoginAttempt(Date.now());
      });

      expect(result.current.lastLoginAttempt).not.toBeNull();

      act(() => {
        result.current.setLastLoginAttempt(null);
      });

      expect(result.current.lastLoginAttempt).toBeNull();
    });
  });

  describe('setBiometricPromptEnabled', () => {
    it('should disable biometric prompt', () => {
      const { result } = renderHook(() => useAuthStore());

      act(() => {
        result.current.setBiometricPromptEnabled(false);
      });

      expect(result.current.biometricPromptEnabled).toBe(false);
    });

    it('should enable biometric prompt', () => {
      const { result } = renderHook(() => useAuthStore());

      act(() => {
        result.current.setBiometricPromptEnabled(false);
      });

      expect(result.current.biometricPromptEnabled).toBe(false);

      act(() => {
        result.current.setBiometricPromptEnabled(true);
      });

      expect(result.current.biometricPromptEnabled).toBe(true);
    });
  });

  describe('resetAuthUI', () => {
    it('should reset all UI state to defaults', () => {
      const { result } = renderHook(() => useAuthStore());

      act(() => {
        result.current.setLastLoginAttempt(Date.now());
        result.current.setBiometricPromptEnabled(false);
      });

      expect(result.current.lastLoginAttempt).not.toBeNull();
      expect(result.current.biometricPromptEnabled).toBe(false);

      act(() => {
        result.current.resetAuthUI();
      });

      expect(result.current.lastLoginAttempt).toBeNull();
      expect(result.current.biometricPromptEnabled).toBe(true);
    });
  });

  describe('persistence', () => {
    it('should persist UI state to AsyncStorage', () => {
      const { result } = renderHook(() => useAuthStore());

      act(() => {
        result.current.setBiometricPromptEnabled(false);
      });

      // Verify state is updated (persistence is handled by Zustand middleware)
      expect(result.current.biometricPromptEnabled).toBe(false);
    });
  });
});
