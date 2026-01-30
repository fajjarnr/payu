/**
 * useAccessibility Hook
 *
 * Provides accessibility-related utilities for React Native components:
 * - Screen reader status detection
 * - Announcement management
 * - Focus management
 * - Accessibility info utilities
 *
 * @module hooks/useAccessibility
 * @version 1.0.0
 */

import { useState, useEffect, useCallback, useRef, useMemo } from 'react';
import {
  AccessibilityInfo,
  Platform,
  findNodeHandle,
  View,
  AccessibilityChangeEvent,
  AccessibilityAnnouncementFinishedEvent,
} from 'react-native';

// ============================================================================
// Types & Interfaces
// ============================================================================

/**
 * Screen reader state
 */
export interface ScreenReaderState {
  /** Whether screen reader is currently enabled */
  isEnabled: boolean;
  /** Whether screen reader is currently active (speaking) */
  isActive: boolean;
}

/**
 * Announcement options
 */
export interface AnnouncementOptions {
  /** Announcement priority */
  priority?: 'polite' | 'assertive';
  /** Callback when announcement finishes */
  onFinish?: (event: AccessibilityAnnouncementFinishedEvent) => void;
}

/**
 * Focus management options
 */
export interface FocusOptions {
  /** Delay before focusing (ms) */
  delay?: number;
  /** Whether to announce focus change */
  announce?: boolean;
}

/**
 * Accessibility preferences
 */
export interface AccessibilityPreferences {
  /** Whether bold text is enabled */
  boldTextEnabled: boolean;
  /** Whether grayscale is enabled */
  grayscaleEnabled: boolean;
  /** Whether invert colors is enabled */
  invertColorsEnabled: boolean;
  /** Whether reduce motion is enabled */
  reduceMotionEnabled: boolean;
  /** Whether reduce transparency is enabled */
  reduceTransparencyEnabled: boolean;
  /** Whether screen reader is enabled */
  screenReaderEnabled: boolean;
}

// ============================================================================
// Hook: useScreenReader
// ============================================================================

/**
 * Hook to detect and monitor screen reader status
 *
 * @returns Screen reader state and utilities
 *
 * @example
 * ```tsx
 * const { isEnabled, isActive } = useScreenReader();
 *
 * if (isEnabled) {
 *   // Provide enhanced accessibility features
 * }
 * ```
 */
export function useScreenReader(): ScreenReaderState & {
  /** Refresh screen reader status */
  refresh: () => Promise<void>;
} {
  const [isEnabled, setIsEnabled] = useState(false);
  const [isActive, setIsActive] = useState(false);

  useEffect(() => {
    let isMounted = true;

    // Get initial state
    const fetchInitialState = async () => {
      try {
        const enabled = await AccessibilityInfo.isScreenReaderEnabled();
        if (isMounted) {
          setIsEnabled(enabled);
        }

        // iOS only: Check if VoiceOver is actively speaking
        if (Platform.OS === 'ios') {
          // @ts-ignore - iOS specific API
          const active = await AccessibilityInfo.isVoiceOverRunning?.();
          if (isMounted) {
            setIsActive(active || false);
          }
        }
      } catch (error) {
        console.warn('Failed to fetch screen reader state:', error);
      }
    };

    fetchInitialState();

    // Subscribe to changes
    const subscription = AccessibilityInfo.addEventListener(
      'screenReaderChanged',
      (enabled: AccessibilityChangeEvent) => {
        if (isMounted) {
          setIsEnabled(enabled);
        }
      }
    );

    return () => {
      isMounted = false;
      subscription.remove();
    };
  }, []);

  const refresh = useCallback(async () => {
    try {
      const enabled = await AccessibilityInfo.isScreenReaderEnabled();
      setIsEnabled(enabled);

      if (Platform.OS === 'ios') {
        // @ts-ignore - iOS specific API
        const active = await AccessibilityInfo.isVoiceOverRunning?.();
        setIsActive(active || false);
      }
    } catch (error) {
      console.warn('Failed to refresh screen reader state:', error);
    }
  }, []);

  return { isEnabled, isActive, refresh };
}

// ============================================================================
// Hook: useAccessibilityAnnounce
// ============================================================================

/**
 * Hook for announcing messages to screen readers
 *
 * @returns Announcement functions and state
 *
 * @example
 * ```tsx
 * const { announce, announcePolite, announceAssertive } = useAccessibilityAnnounce();
 *
 * // Announce transaction success
 * announceAssertive('Transfer completed successfully');
 *
 * // Announce balance update
 * announcePolite('Your balance has been updated');
 * ```
 */
export function useAccessibilityAnnounce() {
  const [lastAnnouncement, setLastAnnouncement] = useState<string>('');
  const finishCallbacks = useRef<Map<string, (event: AccessibilityAnnouncementFinishedEvent) => void>>(new Map());

  useEffect(() => {
    const subscription = AccessibilityInfo.addEventListener(
      'announcementFinished',
      (event: AccessibilityAnnouncementFinishedEvent) => {
        const callback = finishCallbacks.current.get(event.announcement);
        if (callback) {
          callback(event);
          finishCallbacks.current.delete(event.announcement);
        }
      }
    );

    return () => {
      subscription.remove();
    };
  }, []);

  const announce = useCallback(
    (message: string, options: AnnouncementOptions = {}) => {
      const { priority = 'polite', onFinish } = options;

      setLastAnnouncement(message);

      if (onFinish) {
        finishCallbacks.current.set(message, onFinish);
      }

      // Use announceForAccessibility for iOS
      if (Platform.OS === 'ios') {
        AccessibilityInfo.announceForAccessibility(message);
      } else {
        // Android: Use priority-based announcement
        if (priority === 'assertive') {
          // On Android, we can only announce - no priority control
          AccessibilityInfo.announceForAccessibility(message);
        } else {
          AccessibilityInfo.announceForAccessibility(message);
        }
      }
    },
    []
  );

  const announcePolite = useCallback(
    (message: string, onFinish?: (event: AccessibilityAnnouncementFinishedEvent) => void) => {
      announce(message, { priority: 'polite', onFinish });
    },
    [announce]
  );

  const announceAssertive = useCallback(
    (message: string, onFinish?: (event: AccessibilityAnnouncementFinishedEvent) => void) => {
      announce(message, { priority: 'assertive', onFinish });
    },
    [announce]
  );

  const announceCurrency = useCallback(
    (amount: number, currency: string = 'IDR', language: 'id' | 'en' = 'id') => {
      let message = '';
      const absAmount = Math.abs(amount);

      if (language === 'id') {
        if (absAmount >= 1000000000) {
          message = `${(absAmount / 1000000000).toFixed(1)} miliar rupiah`;
        } else if (absAmount >= 1000000) {
          message = `${(absAmount / 1000000).toFixed(1)} juta rupiah`;
        } else if (absAmount >= 1000) {
          message = `${(absAmount / 1000).toFixed(0)} ribu rupiah`;
        } else {
          message = `${absAmount} rupiah`;
        }
        if (amount < 0) message = `minus ${message}`;
      } else {
        message = `${amount} ${currency}`;
      }

      announce(message, { priority: 'polite' });
    },
    [announce]
  );

  const clearAnnouncement = useCallback(() => {
    // iOS only: Interrupt current speech
    if (Platform.OS === 'ios') {
      AccessibilityInfo.announceForAccessibility('');
    }
  }, []);

  return {
    announce,
    announcePolite,
    announceAssertive,
    announceCurrency,
    clearAnnouncement,
    lastAnnouncement,
  };
}

// ============================================================================
// Hook: useAccessibilityFocus
// ============================================================================

/**
 * Hook for managing focus in an accessible way
 *
 * @returns Focus management utilities
 *
 * @example
 * ```tsx
 * const { setFocus, focusRef, announceFocus } = useAccessibilityFocus();
 *
 * // Focus an element after action
 * useEffect(() => {
 *   if (transactionComplete) {
 *     setFocus(successMessageRef);
 *   }
 * }, [transactionComplete]);
 *
 * return (
 *   <View ref={successMessageRef} accessible accessibilityLabel="Success">
 *     ...
 *   </View>
 * );
 * ```
 */
export function useAccessibilityFocus() {
  const { announcePolite } = useAccessibilityAnnounce();

  const setFocus = useCallback(
    (ref: React.RefObject<View>, options: FocusOptions = {}) => {
      const { delay = 0, announce = true } = options;

      const focusAction = () => {
        if (ref.current) {
          const nodeHandle = findNodeHandle(ref.current);
          if (nodeHandle) {
            // @ts-ignore - setAccessibilityFocus exists on AccessibilityInfo
            AccessibilityInfo.setAccessibilityFocus(nodeHandle);

            if (announce) {
              // Get accessibility label or fallback
              // Note: In real implementation, you'd get this from the component
              announcePolite('Focused');
            }
          }
        }
      };

      if (delay > 0) {
        setTimeout(focusAction, delay);
      } else {
        focusAction();
      }
    },
    [announcePolite]
  );

  const focusRef = useCallback(
    (element: View | null, options: FocusOptions = {}) => {
      if (!element) return;

      const { delay = 0 } = options;
      const nodeHandle = findNodeHandle(element);

      if (nodeHandle) {
        const focusAction = () => {
          // @ts-ignore - setAccessibilityFocus exists on AccessibilityInfo
          AccessibilityInfo.setAccessibilityFocus(nodeHandle);
        };

        if (delay > 0) {
          setTimeout(focusAction, delay);
        } else {
          focusAction();
        }
      }
    },
    []
  );

  const announceFocus = useCallback(
    (elementName: string) => {
      announcePolite(`${elementName} focused`);
    },
    [announcePolite]
  );

  return {
    setFocus,
    focusRef,
    announceFocus,
  };
}

// ============================================================================
// Hook: useAccessibilityPreferences
// ============================================================================

/**
 * Hook to get user's accessibility preferences
 *
 * @returns Accessibility preferences state
 *
 * @example
 * ```tsx
 * const { preferences, isLoading } = useAccessibilityPreferences();
 *
 * if (preferences.reduceMotionEnabled) {
 *   // Disable animations
 * }
 * ```
 */
export function useAccessibilityPreferences() {
  const [preferences, setPreferences] = useState<AccessibilityPreferences>({
    boldTextEnabled: false,
    grayscaleEnabled: false,
    invertColorsEnabled: false,
    reduceMotionEnabled: false,
    reduceTransparencyEnabled: false,
    screenReaderEnabled: false,
  });
  const [isLoading, setIsLoading] = useState(true);

  useEffect(() => {
    let isMounted = true;

    const fetchPreferences = async () => {
      try {
        const [
          boldText,
          grayscale,
          invertColors,
          reduceMotion,
          reduceTransparency,
          screenReader,
        ] = await Promise.all([
          AccessibilityInfo.isBoldTextEnabled?.() || Promise.resolve(false),
          AccessibilityInfo.isGrayscaleEnabled?.() || Promise.resolve(false),
          AccessibilityInfo.isInvertColorsEnabled?.() || Promise.resolve(false),
          AccessibilityInfo.isReduceMotionEnabled?.() || Promise.resolve(false),
          AccessibilityInfo.isReduceTransparencyEnabled?.() || Promise.resolve(false),
          AccessibilityInfo.isScreenReaderEnabled(),
        ]);

        if (isMounted) {
          setPreferences({
            boldTextEnabled: boldText,
            grayscaleEnabled: grayscale,
            invertColorsEnabled: invertColors,
            reduceMotionEnabled: reduceMotion,
            reduceTransparencyEnabled: reduceTransparency,
            screenReaderEnabled: screenReader,
          });
          setIsLoading(false);
        }
      } catch (error) {
        console.warn('Failed to fetch accessibility preferences:', error);
        if (isMounted) {
          setIsLoading(false);
        }
      }
    };

    fetchPreferences();

    // Subscribe to changes
    const subscriptions: any[] = [];

    subscriptions.push(
      AccessibilityInfo.addEventListener('screenReaderChanged', (enabled) => {
        setPreferences((prev) => ({ ...prev, screenReaderEnabled: enabled }));
      })
    );

    subscriptions.push(
      AccessibilityInfo.addEventListener('reduceMotionChanged', (enabled) => {
        setPreferences((prev) => ({ ...prev, reduceMotionEnabled: enabled }));
      })
    );

    if (Platform.OS === 'ios') {
      subscriptions.push(
        AccessibilityInfo.addEventListener('boldTextChanged', (enabled) => {
          setPreferences((prev) => ({ ...prev, boldTextEnabled: enabled }));
        })
      );

      subscriptions.push(
        AccessibilityInfo.addEventListener('grayscaleChanged', (enabled) => {
          setPreferences((prev) => ({ ...prev, grayscaleEnabled: enabled }));
        })
      );
    }

    return () => {
      isMounted = false;
      subscriptions.forEach((sub) => sub.remove());
    };
  }, []);

  return { preferences, isLoading };
}

// ============================================================================
// Hook: useAccessibleForm
// ============================================================================

/**
 * Hook for managing accessible form interactions
 *
 * @returns Form accessibility utilities
 *
 * @example
 * ```tsx
 * const { announceFieldError, announceFormSubmit, announceFormReset } = useAccessibleForm();
 *
 * const handleSubmit = () => {
 *   if (errors.length > 0) {
 *     announceFieldError('Email', 'Please enter a valid email address');
 *   } else {
 *     announceFormSubmit('Transfer form');
 *   }
 * };
 * ```
 */
export function useAccessibleForm() {
  const { announceAssertive, announcePolite } = useAccessibilityAnnounce();

  const announceFieldError = useCallback(
    (fieldName: string, error: string) => {
      announceAssertive(`${fieldName} error: ${error}`);
    },
    [announceAssertive]
  );

  const announceFieldSuccess = useCallback(
    (fieldName: string) => {
      announcePolite(`${fieldName} is valid`);
    },
    [announcePolite]
  );

  const announceFormSubmit = useCallback(
    (formName: string, success: boolean = true) => {
      if (success) {
        announcePolite(`${formName} submitted successfully`);
      } else {
        announceAssertive(`${formName} submission failed. Please check the errors.`);
      }
    },
    [announcePolite, announceAssertive]
  );

  const announceFormReset = useCallback(
    (formName: string) => {
      announcePolite(`${formName} has been reset`);
    },
    [announcePolite]
  );

  const announceValidationSummary = useCallback(
    (errorCount: number) => {
      if (errorCount === 0) {
        announcePolite('All fields are valid');
      } else if (errorCount === 1) {
        announceAssertive('There is 1 error to fix');
      } else {
        announceAssertive(`There are ${errorCount} errors to fix`);
      }
    },
    [announcePolite, announceAssertive]
  );

  return {
    announceFieldError,
    announceFieldSuccess,
    announceFormSubmit,
    announceFormReset,
    announceValidationSummary,
  };
}

// ============================================================================
// Hook: useAccessibility (Main Hook)
// ============================================================================

/**
 * Main accessibility hook combining all accessibility features
 *
 * @returns Combined accessibility utilities
 *
 * @example
 * ```tsx
 * const a11y = useAccessibility();
 *
 * // Check if screen reader is enabled
 * if (a11y.screenReader.isEnabled) {
 *   a11y.announce.announcePolite('Screen reader detected');
 * }
 *
 * // Focus an element
 * a11y.focus.setFocus(myRef);
 *
 * // Get user preferences
 * const { reduceMotionEnabled } = a11y.preferences.preferences;
 * ```
 */
export function useAccessibility() {
  const screenReader = useScreenReader();
  const announce = useAccessibilityAnnounce();
  const focus = useAccessibilityFocus();
  const preferences = useAccessibilityPreferences();
  const form = useAccessibleForm();

  return useMemo(
    () => ({
      screenReader,
      announce,
      focus,
      preferences,
      form,
      // Convenience properties
      isScreenReaderEnabled: screenReader.isEnabled,
      reduceMotionEnabled: preferences.preferences.reduceMotionEnabled,
    }),
    [screenReader, announce, focus, preferences, form]
  );
}

// ============================================================================
// Export
// ============================================================================

export default useAccessibility;
