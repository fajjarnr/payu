import React, { useState } from 'react';
import {
  View,
  Text,
  StyleSheet,
  ScrollView,
  TouchableOpacity,
  Alert,
} from 'react-native';
import { useRouter } from 'expo-router';
import { useTheme } from '@react-navigation/native';
import { Button } from '@/components/ui/Button';
import { Input } from '@/components/ui/Input';
import { Card } from '@/components/ui/Card';
import { feedbackService } from '@/services/feedback.service';
import { FEEDBACK_CATEGORIES } from '@/constants/config';
import { FeedbackData } from '@/types';
import * as ImagePicker from 'expo-image-picker';

export default function FeedbackScreen() {
  const router = useRouter();
  const { colors } = useTheme();

  const [category, setCategory] = useState<string>('');
  const [rating, setRating] = useState(0);
  const [message, setMessage] = useState('');
  const [screenshots, setScreenshots] = useState<string[]>([]);
  const [submitting, setSubmitting] = useState(false);

  const handleRating = (value: number) => {
    setRating(value);
  };

  const handleSubmit = async () => {
    if (!category) {
      Alert.alert('Required', 'Please select a category');
      return;
    }

    if (rating === 0) {
      Alert.alert('Required', 'Please provide a rating');
      return;
    }

    if (!message.trim()) {
      Alert.alert('Required', 'Please enter your feedback');
      return;
    }

    setSubmitting(true);

    try {
      const feedback: FeedbackData = {
        category: category as any,
        rating,
        message,
        screenshots,
        deviceInfo: {
          appVersion: '1.0.0',
          os: 'ios',
          osVersion: '17.0',
          device: 'iPhone',
        },
      };

      await feedbackService.submitFeedback(feedback);

      Alert.alert('Success', 'Thank you for your feedback!', [
        {
          text: 'OK',
          onPress: () => router.back(),
        },
      ]);
    } catch (error: any) {
      Alert.alert(
        'Failed',
        error.response?.data?.message || 'Could not submit feedback'
      );
    } finally {
      setSubmitting(false);
    }
  };

  const handleAddScreenshot = async () => {
    try {
      const result = await ImagePicker.launchImageLibraryAsync({
        mediaTypes: ['images'],
        allowsEditing: true,
        aspect: [4, 3],
        quality: 0.8,
      });

      if (!result.canceled && result.assets[0]) {
        setScreenshots([...screenshots, result.assets[0].uri]);
      }
    } catch (error) {
      console.error('Error picking image:', error);
    }
  };

  const removeScreenshot = (index: number) => {
    setScreenshots(screenshots.filter((_, i) => i !== index));
  };

  return (
    <ScrollView
      style={styles.container}
      contentContainerStyle={styles.content}
      showsVerticalScrollIndicator={false}
    >
      <Text style={[styles.title, { color: colors.text }]}>Send Feedback</Text>

      {/* Category Selection */}
      <View style={styles.section}>
        <Text style={[styles.label, { color: colors.text }]}>Category</Text>
        <View style={styles.categoryGrid}>
          {FEEDBACK_CATEGORIES.map((cat) => (
            <TouchableOpacity
              key={cat.id}
              style={[
                styles.categoryItem,
                {
                  backgroundColor:
                    category === cat.id ? '#10b981' : colors.card,
                  borderColor: colors.border,
                },
              ]}
              onPress={() => setCategory(cat.id)}
            >
              <Text style={styles.categoryIcon}>{cat.icon}</Text>
              <Text
                style={[
                  styles.categoryLabel,
                  { color: category === cat.id ? '#fff' : colors.text },
                ]}
              >
                {cat.label}
              </Text>
            </TouchableOpacity>
          ))}
        </View>
      </View>

      {/* Rating */}
      <View style={styles.section}>
        <Text style={[styles.label, { color: colors.text }]}>
          How would you rate this?
        </Text>
        <View style={styles.ratingContainer}>
          {[1, 2, 3, 4, 5].map((value) => (
            <TouchableOpacity
              key={value}
              onPress={() => handleRating(value)}
              style={styles.starButton}
            >
              <Text style={styles.star}>
                {value <= rating ? '⭐' : '☆'}
              </Text>
            </TouchableOpacity>
          ))}
        </View>
      </View>

      {/* Message */}
      <View style={styles.section}>
        <Input
          label="Your Feedback"
          value={message}
          onChangeText={setMessage}
          placeholder="Tell us more about your experience..."
          multiline
          numberOfLines={5}
        />
      </View>

      {/* Screenshots */}
      <View style={styles.section}>
        <Text style={[styles.label, { color: colors.text }]}>
          Screenshots (Optional)
        </Text>
        <View style={styles.screenshotGrid}>
          {screenshots.map((screenshot, index) => (
            <TouchableOpacity
              key={index}
              style={styles.screenshotItem}
              onPress={() => removeScreenshot(index)}
            >
              <Text style={styles.screenshotRemove}>✕</Text>
            </TouchableOpacity>
          ))}
          {screenshots.length < 3 && (
            <TouchableOpacity
              style={[styles.addScreenshot, { borderColor: colors.border }]}
              onPress={handleAddScreenshot}
            >
              <Text style={styles.addScreenshotText}>+ Add</Text>
            </TouchableOpacity>
          )}
        </View>
      </View>

      {/* Submit Button */}
      <Button
        title="Submit Feedback"
        onPress={handleSubmit}
        loading={submitting}
        fullWidth
      />
    </ScrollView>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#f9fafb',
  },
  content: {
    padding: 20,
  },
  title: {
    fontSize: 28,
    fontWeight: '900',
    marginBottom: 32,
    letterSpacing: -1,
  },
  section: {
    marginBottom: 24,
  },
  label: {
    fontSize: 16,
    fontWeight: '600',
    marginBottom: 12,
  },
  categoryGrid: {
    flexDirection: 'row',
    flexWrap: 'wrap',
    gap: 12,
  },
  categoryItem: {
    flex: 1,
    minWidth: '45%',
    paddingVertical: 16,
    paddingHorizontal: 12,
    borderRadius: 12,
    alignItems: 'center',
    borderWidth: 1,
  },
  categoryIcon: {
    fontSize: 32,
    marginBottom: 8,
  },
  categoryLabel: {
    fontSize: 12,
    fontWeight: '600',
    textAlign: 'center',
  },
  ratingContainer: {
    flexDirection: 'row',
    justifyContent: 'center',
    gap: 8,
    paddingVertical: 16,
  },
  starButton: {
    padding: 8,
  },
  star: {
    fontSize: 40,
  },
  screenshotGrid: {
    flexDirection: 'row',
    gap: 12,
  },
  screenshotItem: {
    width: 80,
    height: 80,
    borderRadius: 12,
    backgroundColor: '#10b981',
    justifyContent: 'center',
    alignItems: 'center',
  },
  screenshotRemove: {
    fontSize: 24,
    color: '#ffffff',
    fontWeight: '700',
  },
  addScreenshot: {
    width: 80,
    height: 80,
    borderRadius: 12,
    borderWidth: 2,
    borderStyle: 'dashed',
    justifyContent: 'center',
    alignItems: 'center',
  },
  addScreenshotText: {
    fontSize: 12,
    fontWeight: '600',
    color: '#10b981',
  },
});
