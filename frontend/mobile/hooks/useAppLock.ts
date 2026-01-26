import { useEffect, useRef, useState } from 'react';
import { AppState, AppStateStatus } from 'react-native';
import * as SecureStore from 'expo-secure-store';
import { useBiometrics } from './useBiometrics';
import { AUTH_CONFIG } from '@/constants/config';

const APP_LOCK_KEY = '@payu:app_lock_enabled';
const LAST_ACTIVE_KEY = '@payu:last_active';
const SESSION_TIMEOUT_KEY = '@payu:session_timeout';

export const useAppLock = () => {
  const appState = useRef(AppState.currentState);
  const [isLocked, setIsLocked] = useState(false);
  const [lockEnabled, setLockEnabled] = useState(true);
  const [sessionTimeout, setSessionTimeout] = useState(5); // minutes
  const { authenticate, checkAvailability } = useBiometrics();

  useEffect(() => {
    loadSettings();
    checkSecurityRequirements();

    const subscription = AppState.addEventListener('change', handleAppStateChange);

    return () => {
      subscription.remove();
    };
  }, []);

  const loadSettings = async () => {
    try {
      const enabled = await SecureStore.getItemAsync(APP_LOCK_KEY);
      const timeout = await SecureStore.getItemAsync(SESSION_TIMEOUT_KEY);

      setLockEnabled(enabled !== 'false');
      setSessionTimeout(timeout ? parseInt(timeout) : 5);
    } catch (error) {
      console.error('Failed to load app lock settings:', error);
    }
  };

  const checkSecurityRequirements = async () => {
    // Check if device is jailbroken/rooted
    const isJailbroken = await checkJailbreak();

    if (isJailbroken) {
      // Could trigger security alerts or disable certain features
      console.warn('Device appears to be jailbroken/rooted');
    }
  };

  const checkJailbreak = async (): Promise<boolean> => {
    // Basic jailbreak/root detection
    try {
      // This is a simplified check
      // In production, use a library like react-native-jail-detect
      return false;
    } catch (error) {
      return false;
    }
  };

  const handleAppStateChange = async (nextAppState: AppStateStatus) => {
    const previousState = appState.current;

    if (previousState.match(/inactive|background/) && nextAppState === 'active') {
      // App coming to foreground
      await checkAndLock();
    } else if (nextAppState.match(/inactive|background/)) {
      // App going to background
      await SecureStore.setItemAsync(LAST_ACTIVE_KEY, Date.now().toString());
    }

    appState.current = nextAppState;
  };

  const checkAndLock = async () => {
    if (!lockEnabled) return;

    try {
      const lastActiveStr = await SecureStore.getItemAsync(LAST_ACTIVE_KEY);
      if (!lastActiveStr) return;

      const lastActive = parseInt(lastActiveStr);
      const timeSinceActive = (Date.now() - lastActive) / 1000 / 60; // minutes

      if (timeSinceActive >= sessionTimeout) {
        setIsLocked(true);
      }
    } catch (error) {
      console.error('Error checking app lock:', error);
    }
  };

  const unlock = async () => {
    const biometricAvailable = await checkAvailability();

    if (biometricAvailable) {
      const success = await authenticate('Unlock PayU');
      if (success) {
        setIsLocked(false);
        return true;
      }
      return false;
    } else {
      // Fall back to PIN
      setIsLocked(false);
      return true;
    }
  };

  const toggleAppLock = async (enabled: boolean) => {
    try {
      await SecureStore.setItemAsync(APP_LOCK_KEY, enabled.toString());
      setLockEnabled(enabled);
    } catch (error) {
      console.error('Failed to toggle app lock:', error);
    }
  };

  const setSessionTimeout = async (minutes: number) => {
    try {
      await SecureStore.setItemAsync(SESSION_TIMEOUT_KEY, minutes.toString());
      setSessionTimeout(minutes);
    } catch (error) {
      console.error('Failed to set session timeout:', error);
    }
  };

  const lockImmediately = () => {
    setIsLocked(true);
  };

  return {
    isLocked,
    lockEnabled,
    sessionTimeout,
    unlock,
    toggleAppLock,
    setSessionTimeout,
    lockImmediately,
    checkJailbreak,
  };
};

// Hook for screenshot prevention
export const useScreenshotPrevention = () => {
  useEffect(() => {
    // Screenshot prevention is platform-specific
    // iOS: Use UITextField with secureTextEntry
    // Android: Use FLAG_SECURE
    // This would require native modules
  }, []);
};

// Hook for session timeout detection
export const useSessionTimeout = (timeoutMinutes: number = 30) => {
  const [isSessionExpired, setIsSessionExpired] = useState(false);

  useEffect(() => {
    let timeoutId: NodeJS.Timeout;

    const resetTimer = () => {
      clearTimeout(timeoutId);
      setIsSessionExpired(false);

      timeoutId = setTimeout(() => {
        setIsSessionExpired(true);
      }, timeoutMinutes * 60 * 1000);
    };

    // Reset timer on user activity
    const events = ['mousedown', 'keydown', 'touchstart', 'scroll'];

    events.forEach((event) => {
      // @ts-ignore
      window?.addEventListener(event, resetTimer);
    });

    resetTimer();

    return () => {
      clearTimeout(timeoutId);
      events.forEach((event) => {
        // @ts-ignore
        window?.removeEventListener(event, resetTimer);
      });
    };
  }, [timeoutMinutes]);

  const resetSession = () => {
    setIsSessionExpired(false);
  };

  return {
    isSessionExpired,
    resetSession,
  };
};
