import { useState } from 'react';
import { Alert } from 'react-native';
import { feedbackService } from '@/services/feedback.service';
import { useAnalytics } from './useAnalytics';

interface FeedbackData {
  category: 'bug' | 'feature' | 'improvement' | 'other';
  rating?: number;
  message: string;
  email?: string;
  screenshots?: string[];
}

export const useFeedback = () => {
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [showFeedbackWidget, setShowFeedbackWidget] = useState(false);
  const { trackEvent, trackError } = useAnalytics();

  const submitFeedback = async (data: FeedbackData) => {
    setIsSubmitting(true);

    try {
      await feedbackService.submitFeedback(data);

      trackEvent('feedback_submitted', {
        category: data.category,
        rating: data.rating,
      });

      Alert.alert(
        'Thank You!',
        'Your feedback has been submitted successfully.'
      );

      setShowFeedbackWidget(false);
      return true;
    } catch (error: any) {
      trackError(error, 'feedback_submission');

      Alert.alert(
        'Submission Failed',
        error.response?.data?.message || 'Failed to submit feedback. Please try again.'
      );
      return false;
    } finally {
      setIsSubmitting(false);
    }
  };

  const showFeedback = () => {
    setShowFeedbackWidget(true);
    trackEvent('feedback_widget_opened');
  };

  const hideFeedback = () => {
    setShowFeedbackWidget(false);
  };

  const rateExperience = async (rating: number) => {
    trackEvent('experience_rated', { rating });

    if (rating >= 4) {
      // High rating - ask for app store review
      Alert.alert(
        'Glad you like it!',
        'Would you like to rate us on the app store?',
        [
          { text: 'Maybe Later', style: 'cancel' },
          { text: 'Yes!', onPress: () => openAppStore() },
        ]
      );
    } else {
      // Low rating - show feedback form
      showFeedback();
    }
  };

  const openAppStore = () => {
    // Open app store for rating
    trackEvent('app_store_rating_requested');
  };

  const reportBug = (bugDescription: string, screenshots?: string[]) => {
    return submitFeedback({
      category: 'bug',
      message: bugDescription,
      screenshots,
    });
  };

  const requestFeature = (featureDescription: string) => {
    return submitFeedback({
      category: 'feature',
      message: featureDescription,
    });
  };

  return {
    isSubmitting,
    showFeedbackWidget,
    submitFeedback,
    showFeedback,
    hideFeedback,
    rateExperience,
    reportBug,
    requestFeature,
  };
};

// Feedback widget component hook
export const useFeedbackWidget = (triggerAfter?: number) => {
  const [showWidget, setShowWidget] = useState(false);
  const { trackEvent } = useAnalytics();

  const checkTrigger = (sessionCount: number) => {
    if (triggerAfter && sessionCount >= triggerAfter) {
      setShowWidget(true);
      trackEvent('feedback_widget_triggered', {
        session_count: sessionCount,
      });
    }
  };

  const dismiss = () => {
    setShowWidget(false);
    trackEvent('feedback_widget_dismissed');
  };

  return {
    showWidget,
    checkTrigger,
    dismiss,
  };
};

// In-app feedback survey hook
export const useFeedbackSurvey = () => {
  const [currentSurvey, setCurrentSurvey] = useState<any | null>(null);
  const [surveyResponses, setSurveyResponses] = useState<Record<string, any>>({});

  const startSurvey = (surveyId: string) => {
    // Load survey configuration
    setCurrentSurvey({ id: surveyId });
  };

  const answerQuestion = (questionId: string, answer: any) => {
    setSurveyResponses((prev) => ({
      ...prev,
      [questionId]: answer,
    }));
  };

  const submitSurvey = async () => {
    if (!currentSurvey) return;

    try {
      await feedbackService.submitSurvey({
        surveyId: currentSurvey.id,
        responses: surveyResponses,
      });

      setCurrentSurvey(null);
      setSurveyResponses({});

      return true;
    } catch (error) {
      return false;
    }
  };

  return {
    currentSurvey,
    surveyResponses,
    startSurvey,
    answerQuestion,
    submitSurvey,
  };
};
