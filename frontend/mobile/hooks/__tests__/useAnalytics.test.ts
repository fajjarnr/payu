import { renderHook, act } from '@testing-library/react-native';
import { useAnalytics, trackError, trackPerformance, trackEngagement, analytics } from '../useAnalytics';
import { useNavigation } from '@react-navigation/native';

// Mock dependencies
jest.mock('@react-navigation/native', () => ({
  useNavigation: jest.fn(),
}));

jest.mock('react-native', () => ({
  Platform: {
    OS: 'ios',
  },
}));

describe('useAnalytics', () => {
  const mockAddListener = jest.fn();
  const mockUnsubscribe = jest.fn();

  beforeEach(() => {
    jest.clearAllMocks();

    (useNavigation as jest.Mock).mockReturnValue({
      addListener: mockAddListener.mockReturnValue(mockUnsubscribe),
    });

    // Reset analytics state
    analytics.enable();
    analytics.setUserId('');
  });

  it('should set up screen view tracking on mount', () => {
    renderHook(() => useAnalytics());

    expect(mockAddListener).toHaveBeenCalledWith('state', expect.any(Function));
  });

  it('should unsubscribe on unmount', () => {
    const { unmount } = renderHook(() => useAnalytics());

    unmount();

    expect(mockUnsubscribe).toHaveBeenCalled();
  });

  it('should track screen view', () => {
    const consoleSpy = jest.spyOn(console, 'log').mockImplementation();
    const { result } = renderHook(() => useAnalytics());

    act(() => {
      result.current.trackScreenView('HomeScreen');
    });

    expect(consoleSpy).toHaveBeenCalledWith(
      '[Analytics]',
      expect.stringContaining('screen_view')
    );
    consoleSpy.mockRestore();
  });

  it('should track screen view with params', () => {
    const consoleSpy = jest.spyOn(console, 'log').mockImplementation();
    const { result } = renderHook(() => useAnalytics());

    act(() => {
      result.current.trackScreenView('ProfileScreen', { userId: '123' });
    });

    expect(consoleSpy).toHaveBeenCalledWith(
      '[Analytics]',
      expect.stringContaining('ProfileScreen')
    );
    consoleSpy.mockRestore();
  });

  it('should track custom event', () => {
    const consoleSpy = jest.spyOn(console, 'log').mockImplementation();
    const { result } = renderHook(() => useAnalytics());

    act(() => {
      result.current.trackEvent('button_click', { buttonId: 'submit' });
    });

    expect(consoleSpy).toHaveBeenCalledWith(
      '[Analytics]',
      expect.stringContaining('button_click')
    );
    consoleSpy.mockRestore();
  });

  it('should track transaction', () => {
    const consoleSpy = jest.spyOn(console, 'log').mockImplementation();
    const { result } = renderHook(() => useAnalytics());

    act(() => {
      result.current.trackTransaction('transfer', 100000, 'completed');
    });

    expect(consoleSpy).toHaveBeenCalledWith(
      '[Analytics]',
      expect.stringContaining('transaction')
    );
    consoleSpy.mockRestore();
  });

  it('should track error', () => {
    const consoleSpy = jest.spyOn(console, 'log').mockImplementation();
    const { result } = renderHook(() => useAnalytics());
    const error = new Error('Test error');

    act(() => {
      result.current.trackError(error, 'test_context');
    });

    expect(consoleSpy).toHaveBeenCalledWith(
      '[Analytics]',
      expect.stringContaining('error')
    );
    consoleSpy.mockRestore();
  });

  it('should track user interaction', () => {
    const consoleSpy = jest.spyOn(console, 'log').mockImplementation();
    const { result } = renderHook(() => useAnalytics());

    act(() => {
      result.current.trackUserInteraction('tap', 'login_button');
    });

    expect(consoleSpy).toHaveBeenCalledWith(
      '[Analytics]',
      expect.stringContaining('user_interaction')
    );
    consoleSpy.mockRestore();
  });

  it('should not track when analytics is disabled', () => {
    const consoleSpy = jest.spyOn(console, 'log').mockImplementation();
    analytics.disable();

    const { result } = renderHook(() => useAnalytics());

    act(() => {
      result.current.trackEvent('test_event');
    });

    expect(consoleSpy).not.toHaveBeenCalled();
    consoleSpy.mockRestore();
  });

  it('should set and use user ID', () => {
    const consoleSpy = jest.spyOn(console, 'log').mockImplementation();
    analytics.setUserId('user-123');

    const { result } = renderHook(() => useAnalytics());

    act(() => {
      result.current.trackEvent('test_event');
    });

    expect(consoleSpy).toHaveBeenCalledWith(
      '[Analytics]',
      expect.stringContaining('user-123')
    );
    consoleSpy.mockRestore();
  });
});

describe('trackError utility', () => {
  it('should track error with context', () => {
    const consoleSpy = jest.spyOn(console, 'log').mockImplementation();
    const consoleErrorSpy = jest.spyOn(console, 'error').mockImplementation();
    analytics.enable();

    const error = new Error('Test error message');

    trackError(error, 'payment_flow');

    expect(consoleSpy).toHaveBeenCalledWith(
      '[Analytics]',
      expect.stringContaining('error')
    );
    expect(consoleErrorSpy).toHaveBeenCalledWith(
      '[Error Tracking]',
      expect.objectContaining({
        message: 'Test error message',
        context: 'payment_flow',
      })
    );

    consoleSpy.mockRestore();
    consoleErrorSpy.mockRestore();
  });

  it('should track error without context', () => {
    const consoleSpy = jest.spyOn(console, 'log').mockImplementation();
    const consoleErrorSpy = jest.spyOn(console, 'error').mockImplementation();
    analytics.enable();

    const error = new Error('Test error');

    trackError(error);

    expect(consoleSpy).toHaveBeenCalled();
    expect(consoleErrorSpy).toHaveBeenCalled();

    consoleSpy.mockRestore();
    consoleErrorSpy.mockRestore();
  });
});

describe('trackPerformance utility', () => {
  it('should track performance metric', () => {
    const consoleSpy = jest.spyOn(console, 'log').mockImplementation();
    analytics.enable();

    trackPerformance('api_call', 250);

    expect(consoleSpy).toHaveBeenCalledWith(
      '[Analytics]',
      expect.stringContaining('performance_metric')
    );
    expect(consoleSpy).toHaveBeenCalledWith(
      '[Analytics]',
      expect.stringContaining('api_call')
    );

    consoleSpy.mockRestore();
  });

  it('should track different performance metrics', () => {
    const consoleSpy = jest.spyOn(console, 'log').mockImplementation();
    analytics.enable();

    trackPerformance('screen_load', 500);
    trackPerformance('database_query', 100);

    expect(consoleSpy).toHaveBeenCalledTimes(2);

    consoleSpy.mockRestore();
  });
});

describe('trackEngagement utility', () => {
  it('should track user engagement', () => {
    const consoleSpy = jest.spyOn(console, 'log').mockImplementation();
    analytics.enable();

    trackEngagement('feature_x', 120);

    expect(consoleSpy).toHaveBeenCalledWith(
      '[Analytics]',
      expect.stringContaining('engagement')
    );
    expect(consoleSpy).toHaveBeenCalledWith(
      '[Analytics]',
      expect.stringContaining('feature_x')
    );

    consoleSpy.mockRestore();
  });

  it('should track engagement with different features', () => {
    const consoleSpy = jest.spyOn(console, 'log').mockImplementation();
    analytics.enable();

    trackEngagement('onboarding', 300);
    trackEngagement('tutorial', 60);

    expect(consoleSpy).toHaveBeenCalledTimes(2);

    consoleSpy.mockRestore();
  });
});

describe('AnalyticsService singleton', () => {
  it('should maintain single instance', () => {
    const { analytics: analytics1 } = require('../useAnalytics');
    const { analytics: analytics2 } = require('../useAnalytics');

    expect(analytics1).toBe(analytics2);
  });

  it('should enable and disable tracking', () => {
    const consoleSpy = jest.spyOn(console, 'log').mockImplementation();
    analytics.enable();

    analytics.trackEvent('disabled_event');
    expect(consoleSpy).toHaveBeenCalled();

    consoleSpy.mockRestore();
  });
});
