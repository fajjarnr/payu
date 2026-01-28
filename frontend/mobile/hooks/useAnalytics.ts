import { useEffect } from 'react';
import { useNavigation } from '@react-navigation/native';
import { Platform } from 'react-native';

// expo-screen-orientation is optional, only import if available
let ScreenOrientation: any;
try {
  ScreenOrientation = require('expo-screen-orientation');
} catch {
  // Module not available, will use fallback
}

interface AnalyticsEvent {
  name: string;
  params?: Record<string, any>;
}

class AnalyticsService {
  private static instance: AnalyticsService;
  private userId: string | null = null;
  private enabled: boolean = true;

  private constructor() {}

  static getInstance(): AnalyticsService {
    if (!AnalyticsService.instance) {
      AnalyticsService.instance = new AnalyticsService();
    }
    return AnalyticsService.instance;
  }

  setUserId(userId: string) {
    this.userId = userId;
  }

  enable() {
    this.enabled = true;
  }

  disable() {
    this.enabled = false;
  }

  trackScreenView(screenName: string, params?: Record<string, any>) {
    if (!this.enabled) return;

    const event: AnalyticsEvent = {
      name: 'screen_view',
      params: {
        screen_name: screenName,
        user_id: this.userId,
        platform: Platform.OS,
        ...params,
      },
    };

    this.logEvent(event);
  }

  trackEvent(eventName: string, params?: Record<string, any>) {
    if (!this.enabled) return;

    const event: AnalyticsEvent = {
      name: eventName,
      params: {
        user_id: this.userId,
        platform: Platform.OS,
        ...params,
      },
    };

    this.logEvent(event);
  }

  trackTransaction(type: string, amount: number, status: string) {
    this.trackEvent('transaction', {
      transaction_type: type,
      amount,
      status,
      currency: 'IDR',
    });
  }

  trackError(error: Error, context?: string) {
    this.trackEvent('error', {
      error_message: error.message,
      error_stack: error.stack,
      context,
    });
  }

  trackUserInteraction(action: string, element: string) {
    this.trackEvent('user_interaction', {
      action,
      element,
    });
  }

  private logEvent(event: AnalyticsEvent) {
    // In production, send to analytics service (Firebase Analytics, Mixpanel, etc.)
    console.log('[Analytics]', JSON.stringify(event, null, 2));

    // Example: Firebase Analytics
    // analytics().logEvent(event.name, event.params);
  }
}

export const analytics = AnalyticsService.getInstance();

export const useAnalytics = () => {
  const navigation = useNavigation();

  useEffect(() => {
    // Track screen views
    const unsubscribe = navigation?.addListener('state', (e) => {
      const screenName = e.data.state.routes[e.data.state.index]?.name || 'unknown';
      analytics.trackScreenView(screenName);
    });

    return unsubscribe;
  }, [navigation]);

  const trackScreenView = (screenName: string, params?: Record<string, any>) => {
    analytics.trackScreenView(screenName, params);
  };

  const trackEvent = (eventName: string, params?: Record<string, any>) => {
    analytics.trackEvent(eventName, params);
  };

  const trackTransaction = (type: string, amount: number, status: string) => {
    analytics.trackTransaction(type, amount, status);
  };

  const trackError = (error: Error, context?: string) => {
    analytics.trackError(error, context);
  };

  const trackUserInteraction = (action: string, element: string) => {
    analytics.trackUserInteraction(action, element);
  };

  return {
    trackScreenView,
    trackEvent,
    trackTransaction,
    trackError,
    trackUserInteraction,
  };
};

// Error tracking utility
export const trackError = (error: Error, context?: string) => {
  analytics.trackError(error, context);

  // In production, send to crash reporting service (Sentry, Bugsnag, etc.)
  console.error('[Error Tracking]', {
    message: error.message,
    stack: error.stack,
    context,
  });
};

// Performance tracking
export const trackPerformance = (metricName: string, duration: number) => {
  analytics.trackEvent('performance_metric', {
    metric_name: metricName,
    duration_ms: duration,
  });
};

// User engagement tracking
export const trackEngagement = (feature: string, duration: number) => {
  analytics.trackEvent('engagement', {
    feature,
    duration_seconds: duration,
  });
};
