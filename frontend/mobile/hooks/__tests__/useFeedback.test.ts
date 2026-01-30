import { renderHook, act } from '@testing-library/react-native';
import { Alert } from 'react-native';
import { useFeedback, useFeedbackWidget, useFeedbackSurvey } from '../useFeedback';
import { feedbackService } from '@/services/feedback.service';

// Mock dependencies
jest.mock('@/services/feedback.service', () => ({
  feedbackService: {
    submitFeedback: jest.fn(),
  },
}));

jest.mock('expo-device', () => ({
  modelName: 'iPhone 14',
}));

jest.mock('react-native', () => ({
  Alert: {
    alert: jest.fn(),
  },
  Platform: {
    OS: 'ios',
    Version: '16.0',
  },
}));

// Mock useAnalytics
jest.mock('../useAnalytics', () => ({
  useAnalytics: () => ({
    trackEvent: jest.fn(),
    trackError: jest.fn(),
  }),
}));

describe('useFeedback', () => {
  const mockSubmitFeedback = jest.fn();

  beforeEach(() => {
    jest.clearAllMocks();
    (feedbackService.submitFeedback as jest.Mock) = mockSubmitFeedback;
  });

  it('should initialize with default state', () => {
    const { result } = renderHook(() => useFeedback());

    expect(result.current.isSubmitting).toBe(false);
    expect(result.current.showFeedbackWidget).toBe(false);
  });

  it('should submit feedback successfully', async () => {
    mockSubmitFeedback.mockResolvedValue(undefined);

    const { result } = renderHook(() => useFeedback());

    const feedbackData = {
      category: 'bug' as const,
      rating: 4,
      message: 'Found a bug in the app',
    };

    let submitResult: boolean | undefined;
    await act(async () => {
      submitResult = await result.current.submitFeedback(feedbackData);
    });

    expect(submitResult).toBe(true);
    expect(mockSubmitFeedback).toHaveBeenCalledWith(
      expect.objectContaining({
        category: 'bug',
        rating: 4,
        message: 'Found a bug in the app',
      })
    );
    expect(Alert.alert).toHaveBeenCalledWith(
      'Thank You!',
      'Your feedback has been submitted successfully.'
    );
    expect(result.current.showFeedbackWidget).toBe(false);
  });

  it('should handle feedback submission error', async () => {
    const error = new Error('Network error');
    (error as any).response = { data: { message: 'Server error' } };
    mockSubmitFeedback.mockRejectedValue(error);

    const { result } = renderHook(() => useFeedback());

    const feedbackData = {
      category: 'feature' as const,
      message: 'Request new feature',
    };

    let submitResult: boolean | undefined;
    await act(async () => {
      submitResult = await result.current.submitFeedback(feedbackData);
    });

    expect(submitResult).toBe(false);
    expect(Alert.alert).toHaveBeenCalledWith(
      'Submission Failed',
      'Server error'
    );
    expect(result.current.isSubmitting).toBe(false);
  });

  it('should handle feedback submission error with fallback message', async () => {
    mockSubmitFeedback.mockRejectedValue(new Error('Network error'));

    const { result } = renderHook(() => useFeedback());

    const feedbackData = {
      category: 'other' as const,
      message: 'General feedback',
    };

    await act(async () => {
      await result.current.submitFeedback(feedbackData);
    });

    expect(Alert.alert).toHaveBeenCalledWith(
      'Submission Failed',
      'Failed to submit feedback. Please try again.'
    );
  });

  it('should show feedback widget', () => {
    const { result } = renderHook(() => useFeedback());

    act(() => {
      result.current.showFeedback();
    });

    expect(result.current.showFeedbackWidget).toBe(true);
  });

  it('should hide feedback widget', () => {
    const { result } = renderHook(() => useFeedback());

    act(() => {
      result.current.showFeedback();
    });

    expect(result.current.showFeedbackWidget).toBe(true);

    act(() => {
      result.current.hideFeedback();
    });

    expect(result.current.showFeedbackWidget).toBe(false);
  });

  it('should handle high rating and ask for app store review', () => {
    const { result } = renderHook(() => useFeedback());

    act(() => {
      result.current.rateExperience(5);
    });

    expect(Alert.alert).toHaveBeenCalledWith(
      'Glad you like it!',
      'Would you like to rate us on the app store?',
      [
        { text: 'Maybe Later', style: 'cancel' },
        { text: 'Yes!', onPress: expect.any(Function) },
      ]
    );
  });

  it('should show feedback form for low rating', () => {
    const { result } = renderHook(() => useFeedback());

    act(() => {
      result.current.rateExperience(2);
    });

    expect(result.current.showFeedbackWidget).toBe(true);
  });

  it('should report bug with screenshots', async () => {
    mockSubmitFeedback.mockResolvedValue(undefined);

    const { result } = renderHook(() => useFeedback());

    await act(async () => {
      await result.current.reportBug('App crashes on login', ['screenshot1.jpg', 'screenshot2.jpg']);
    });

    expect(mockSubmitFeedback).toHaveBeenCalledWith(
      expect.objectContaining({
        category: 'bug',
        message: 'App crashes on login',
        screenshots: ['screenshot1.jpg', 'screenshot2.jpg'],
      })
    );
  });

  it('should request feature', async () => {
    mockSubmitFeedback.mockResolvedValue(undefined);

    const { result } = renderHook(() => useFeedback());

    await act(async () => {
      await result.current.requestFeature('Add dark mode support');
    });

    expect(mockSubmitFeedback).toHaveBeenCalledWith(
      expect.objectContaining({
        category: 'feature',
        message: 'Add dark mode support',
      })
    );
  });

  it('should include device info in feedback', async () => {
    mockSubmitFeedback.mockResolvedValue(undefined);

    const { result } = renderHook(() => useFeedback());

    await act(async () => {
      await result.current.submitFeedback({
        category: 'bug',
        message: 'Test',
      });
    });

    expect(mockSubmitFeedback).toHaveBeenCalledWith(
      expect.objectContaining({
        deviceInfo: expect.objectContaining({
          appVersion: '1.0.0',
          os: 'ios',
          osVersion: '16.0',
          device: 'iPhone 14',
        }),
      })
    );
  });

  it('should set isSubmitting to true during submission', async () => {
    let resolveSubmission: () => void;
    const submissionPromise = new Promise<void>((resolve) => {
      resolveSubmission = resolve;
    });
    mockSubmitFeedback.mockReturnValue(submissionPromise);

    const { result } = renderHook(() => useFeedback());

    act(() => {
      result.current.submitFeedback({
        category: 'bug',
        message: 'Test',
      });
    });

    expect(result.current.isSubmitting).toBe(true);

    await act(async () => {
      resolveSubmission!();
      await submissionPromise;
    });

    expect(result.current.isSubmitting).toBe(false);
  });
});

describe('useFeedbackWidget', () => {
  it('should initialize with widget hidden', () => {
    const { result } = renderHook(() => useFeedbackWidget());

    expect(result.current.showWidget).toBe(false);
  });

  it('should show widget when session count reaches trigger', () => {
    const { result } = renderHook(() => useFeedbackWidget(5));

    act(() => {
      result.current.checkTrigger(5);
    });

    expect(result.current.showWidget).toBe(true);
  });

  it('should not show widget when session count is below trigger', () => {
    const { result } = renderHook(() => useFeedbackWidget(5));

    act(() => {
      result.current.checkTrigger(3);
    });

    expect(result.current.showWidget).toBe(false);
  });

  it('should not show widget when trigger is not set', () => {
    const { result } = renderHook(() => useFeedbackWidget());

    act(() => {
      result.current.checkTrigger(100);
    });

    expect(result.current.showWidget).toBe(false);
  });

  it('should dismiss widget', () => {
    const { result } = renderHook(() => useFeedbackWidget(1));

    act(() => {
      result.current.checkTrigger(1);
    });

    expect(result.current.showWidget).toBe(true);

    act(() => {
      result.current.dismiss();
    });

    expect(result.current.showWidget).toBe(false);
  });
});

describe('useFeedbackSurvey', () => {
  it('should initialize with no active survey', () => {
    const { result } = renderHook(() => useFeedbackSurvey());

    expect(result.current.currentSurvey).toBeNull();
    expect(result.current.surveyResponses).toEqual({});
  });

  it('should start a survey', () => {
    const { result } = renderHook(() => useFeedbackSurvey());

    act(() => {
      result.current.startSurvey('survey-123');
    });

    expect(result.current.currentSurvey).toEqual({ id: 'survey-123' });
  });

  it('should record survey answers', () => {
    const { result } = renderHook(() => useFeedbackSurvey());

    act(() => {
      result.current.startSurvey('survey-123');
    });

    act(() => {
      result.current.answerQuestion('q1', 'answer1');
    });

    expect(result.current.surveyResponses).toEqual({ q1: 'answer1' });

    act(() => {
      result.current.answerQuestion('q2', 5);
    });

    expect(result.current.surveyResponses).toEqual({
      q1: 'answer1',
      q2: 5,
    });
  });

  it('should submit survey and reset state', async () => {
    const { result } = renderHook(() => useFeedbackSurvey());

    act(() => {
      result.current.startSurvey('survey-123');
    });

    act(() => {
      result.current.answerQuestion('q1', 'answer1');
    });

    let submitResult: boolean | undefined;
    await act(async () => {
      submitResult = await result.current.submitSurvey();
    });

    expect(submitResult).toBe(true);
    expect(result.current.currentSurvey).toBeNull();
    expect(result.current.surveyResponses).toEqual({});
  });

  it('should return false when submitting without active survey', async () => {
    const { result } = renderHook(() => useFeedbackSurvey());

    let submitResult: boolean | undefined;
    await act(async () => {
      submitResult = await result.current.submitSurvey();
    });

    expect(submitResult).toBe(false);
  });
});
