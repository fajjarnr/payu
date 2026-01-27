// Feedback widget component for in-app user feedback
// Provides screenshot capture and context logging

'use client';

import React, { useState, useRef } from 'react';
import { Camera, X, AlertCircle, CheckCircle, Bug, Lightbulb } from 'lucide-react';
import { a11yUtils } from '@/lib/a11y';

declare global {
  interface Window {
    gtag?: (...args: unknown[]) => void;
  }
}

interface FeedbackData {
  category: 'bug' | 'feature' | 'other';
  subject: string;
  message: string;
  screenshot?: string;
  deviceInfo: DeviceInfo;
  logs: string[];
}

interface DeviceInfo {
  userAgent: string;
  screenResolution: string;
  windowSize: string;
  language: string;
  timezone: string;
  platform: string;
}

interface FeedbackWidgetProps {
  apiEndpoint?: string;
  autoCapture?: boolean;
  categories?: Array<{ value: string; label: string; icon: React.ReactNode }>;
}

export const FeedbackWidget: React.FC<FeedbackWidgetProps> = ({
  apiEndpoint = '/api/v1/feedback',
  autoCapture = true,
  categories = [
    { value: 'bug', label: 'Laporan Bug', icon: <Bug className="w-5 h-5" /> },
    { value: 'feature', label: 'Saran Fitur', icon: <Lightbulb className="w-5 h-5" /> },
    { value: 'other', label: 'Lainnya', icon: <AlertCircle className="w-5 h-5" /> },
  ],
}) => {
  const [isOpen, setIsOpen] = useState(false);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [submitted, setSubmitted] = useState(false);
  const [category, setCategory] = useState<'bug' | 'feature' | 'other'>('bug');
  const [subject, setSubject] = useState('');
  const [message, setMessage] = useState('');
  const [screenshot, setScreenshot] = useState<string | null>(null);
  const [includeScreenshot, setIncludeScreenshot] = useState(true);

  const modalRef = useRef<HTMLDivElement>(null);
  a11yUtils.useFocusTrap(isOpen, modalRef);

  // Capture screenshot using html2canvas or browser API
  const captureScreenshot = async () => {
    try {
      // Try using modern Screen Capture API
      const mediaDevices = navigator.mediaDevices;
      if (mediaDevices?.getDisplayMedia) {
        const stream = await mediaDevices.getDisplayMedia({
          video: { cursor: 'always' } as MediaTrackConstraints
        });

        const video = document.createElement('video');
        video.srcObject = stream;
        await video.play();

        const canvas = document.createElement('canvas');
        canvas.width = window.innerWidth;
        canvas.height = window.innerHeight;

        const ctx = canvas.getContext('2d');
        ctx?.drawImage(video, 0, 0, canvas.width, canvas.height);

        stream.getTracks().forEach((track: MediaStreamTrack) => track.stop());

        setScreenshot(canvas.toDataURL('image/png'));
      }
    } catch (error) {
      console.warn('Screenshot capture failed:', error);
      setIncludeScreenshot(false);
    }
  };

  const getDeviceInfo = (): DeviceInfo => ({
    userAgent: navigator.userAgent,
    screenResolution: `${screen.width}x${screen.height}`,
    windowSize: `${window.innerWidth}x${window.innerHeight}`,
    language: navigator.language,
    timezone: Intl.DateTimeFormat().resolvedOptions().timeZone,
    platform: navigator.platform,
  });

  const getConsoleLogs = (): string[] => {
    // Collect recent console errors/warnings
    const logs: string[] = [];

    // In production, you might have a logging service that stores recent errors
    // For now, return empty array as we can't access console history
    return logs;
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setIsSubmitting(true);

    const feedbackData: FeedbackData = {
      category,
      subject,
      message,
      screenshot: includeScreenshot ? screenshot || undefined : undefined,
      deviceInfo: getDeviceInfo(),
      logs: autoCapture ? getConsoleLogs() : [],
    };

    try {
      const response = await fetch(apiEndpoint, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify(feedbackData),
      });

      if (!response.ok) {
        throw new Error('Failed to submit feedback');
      }

      setSubmitted(true);

      // Reset form after 3 seconds
      setTimeout(() => {
        setSubmitted(false);
        setIsOpen(false);
        resetForm();
      }, 3000);
    } catch (error) {
      console.error('Feedback submission error:', error);
      // Show error state
    } finally {
      setIsSubmitting(false);
    }
  };

  const resetForm = () => {
    setCategory('bug');
    setSubject('');
    setMessage('');
    setScreenshot(null);
    setIncludeScreenshot(true);
  };

  return (
    <>
      {/* Floating Button */}
      {!isOpen && (
        <button
          onClick={() => setIsOpen(true)}
          className="fixed bottom-24 right-4 md:bottom-8 md:right-8 z-40 bg-bank-green text-white p-4 rounded-full shadow-lg hover:bg-bank-emerald transition-all focus:outline-none focus:ring-4 focus:ring-bank-green/30"
          aria-label="Buka formulir feedback"
        >
          <Camera className="w-6 h-6" />
        </button>
      )}

      {/* Modal */}
      {isOpen && (
        <div
          className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/50 backdrop-blur-sm"
          onClick={() => setIsOpen(false)}
        >
          <div
            ref={modalRef}
            className="bg-card rounded-3xl shadow-2xl w-full max-w-lg max-h-[90vh] overflow-y-auto"
            onClick={(e) => e.stopPropagation()}
            role="dialog"
            aria-modal="true"
            aria-labelledby="feedback-title"
          >
            {/* Header */}
            <div className="sticky top-0 bg-card border-b border-border p-6 rounded-t-3xl">
              <div className="flex items-center justify-between">
                <h2 id="feedback-title" className="font-black text-xl tracking-tighter">
                  Kirim Feedback
                </h2>
                <button
                  onClick={() => setIsOpen(false)}
                  className="p-2 hover:bg-gray-100 dark:hover:bg-gray-800 rounded-full transition-colors"
                  aria-label="Tutup formulir feedback"
                >
                  <X className="w-5 h-5" />
                </button>
              </div>
            </div>

            {/* Content */}
            <div className="p-6">
              {submitted ? (
                <div className="text-center py-8">
                  <div className="inline-flex items-center justify-center w-16 h-16 bg-green-100 dark:bg-green-900/30 rounded-full mb-4">
                    <CheckCircle className="w-8 h-8 text-green-600 dark:text-green-400" />
                  </div>
                  <h3 className="font-semibold text-lg mb-2">Terima Kasih!</h3>
                  <p className="text-muted-foreground">
                    Feedback Anda telah terkirim. Kami akan menghubungi Anda jika diperlukan.
                  </p>
                </div>
              ) : (
                <form onSubmit={handleSubmit} className="space-y-6">
                  {/* Category Selection */}
                  <div>
                    <label className="block text-sm font-medium mb-3" id="category-label">
                      Kategori Feedback
                    </label>
                    <div className="grid grid-cols-3 gap-3" role="radiogroup" aria-labelledby="category-label">
                      {categories.map((cat) => (
                        <button
                          key={cat.value}
                          type="button"
                          onClick={() => setCategory(cat.value as FeedbackData['category'])}
                          className={`flex flex-col items-center gap-2 p-4 rounded-xl border-2 transition-all ${
                            category === cat.value
                              ? 'border-bank-green bg-bank-green/10'
                              : 'border-border hover:border-bank-green/50'
                          }`}
                          role="radio"
                          aria-checked={category === cat.value}
                        >
                          <div className={category === cat.value ? 'text-bank-green' : 'text-muted-foreground'}>
                            {cat.icon}
                          </div>
                          <span className="text-xs font-medium">{cat.label}</span>
                        </button>
                      ))}
                    </div>
                  </div>

                  {/* Subject */}
                  <div>
                    <label htmlFor="feedback-subject" className="block text-sm font-medium mb-2">
                      Subjek
                    </label>
                    <input
                      id="feedback-subject"
                      type="text"
                      value={subject}
                      onChange={(e) => setSubject(e.target.value)}
                      placeholder="Jelaskan masalah atau saran Anda secara singkat"
                      className="w-full px-4 py-3 rounded-xl border border-border focus:border-bank-green focus:ring-4 focus:ring-bank-green/10 outline-none transition-all"
                      required
                      maxLength={100}
                    />
                  </div>

                  {/* Message */}
                  <div>
                    <label htmlFor="feedback-message" className="block text-sm font-medium mb-2">
                      Pesan
                    </label>
                    <textarea
                      id="feedback-message"
                      value={message}
                      onChange={(e) => setMessage(e.target.value)}
                      placeholder="Ceritakan detail lebih lanjut..."
                      className="w-full px-4 py-3 rounded-xl border border-border focus:border-bank-green focus:ring-4 focus:ring-bank-green/10 outline-none transition-all resize-none"
                      rows={4}
                      required
                      maxLength={1000}
                    />
                    <p className="text-xs text-muted-foreground mt-1 text-right">
                      {message.length} / 1000
                    </p>
                  </div>

                  {/* Screenshot Option */}
                  <div className="flex items-center gap-3 p-4 bg-gray-50 dark:bg-gray-900 rounded-xl">
                    <input
                      id="include-screenshot"
                      type="checkbox"
                      checked={includeScreenshot}
                      onChange={(e) => {
                        setIncludeScreenshot(e.target.checked);
                        if (e.target.checked && !screenshot) {
                          captureScreenshot();
                        }
                      }}
                      className="w-5 h-5 rounded border-gray-300 text-bank-green focus:ring-bank-green"
                    />
                    <label htmlFor="include-screenshot" className="flex-1 text-sm cursor-pointer">
                      Sertakan tangkapan layar (screenshot)
                    </label>
                    {screenshot && (
                      <div className="text-xs text-green-600 dark:text-green-400 flex items-center gap-1">
                        <CheckCircle className="w-3 h-3" />
                        Captured
                      </div>
                    )}
                  </div>

                  {/* Info about data collection */}
                  <p className="text-xs text-muted-foreground">
                    Informasi perangkat dan log error akan dikirim bersama feedback Anda untuk membantu kami
                    dalam menganalisis masalah.
                  </p>

                  {/* Submit Button */}
                  <button
                    type="submit"
                    disabled={isSubmitting || !subject.trim() || !message.trim()}
                    className="w-full py-4 bg-bank-green hover:bg-bank-emerald text-white font-semibold rounded-xl transition-all disabled:opacity-50 disabled:cursor-not-allowed focus:outline-none focus:ring-4 focus:ring-bank-green/30"
                  >
                    {isSubmitting ? 'Mengirim...' : 'Kirim Feedback'}
                  </button>
                </form>
              )}
            </div>
          </div>
        </div>
      )}
    </>
  );
};

export default FeedbackWidget;
