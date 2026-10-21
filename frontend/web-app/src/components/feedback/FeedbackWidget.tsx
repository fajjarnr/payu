'use client';

import React, { useEffect, useState } from 'react';
import { useTranslations } from 'next-intl';
import { Camera, AlertCircle, CheckCircle, Bug, Lightbulb } from '@/components/icons';
import { Alert, Button, Checkbox, Input, Modal, Typography, theme } from 'antd';

const { TextArea } = Input;

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
  categories,
}) => {
  const t = useTranslations('feedback');
  const { token } = theme.useToken();
  const defaultCategories = [
    { value: 'bug', label: t('categoryBug'), icon: <Bug className="w-5 h-5" /> },
    { value: 'feature', label: t('categoryFeature'), icon: <Lightbulb className="w-5 h-5" /> },
    { value: 'other', label: t('categoryOther'), icon: <AlertCircle className="w-5 h-5" /> },
  ];
  const resolvedCategories = categories ?? defaultCategories;
  const [isOpen, setIsOpen] = useState(false);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [submitted, setSubmitted] = useState(false);
  const [submitError, setSubmitError] = useState<string | null>(null);
  const [announcement, setAnnouncement] = useState('');
  const [category, setCategory] = useState<'bug' | 'feature' | 'other'>('bug');
  const [subject, setSubject] = useState('');
  const [message, setMessage] = useState('');
  const [screenshot, setScreenshot] = useState<string | null>(null);
  const [includeScreenshot, setIncludeScreenshot] = useState(true);
  // Capture screenshot using modern Screen Capture API
  const captureScreenshot = async () => {
    try {
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
    return [];
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();

    // BUG-FE-102: Validation guard in case handleSubmit is called programmatically
    if (!subject.trim() || !message.trim()) return;

    setIsSubmitting(true);
    setSubmitError(null);

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
      setAnnouncement(t('successAnnouncement'));
    } catch {
      setSubmitError(t('submitError'));
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
    setSubmitError(null);
  };

  const handleClose = () => {
    setIsOpen(false);
    setSubmitted(false);
    setAnnouncement('');
    resetForm();
  };

  // Auto-dismiss the success panel; the polite live region above announces
  // the outcome first so SR users do not miss it (F-08).
  useEffect(() => {
    if (!submitted) return;
    const id = setTimeout(() => {
      setSubmitted(false);
      setIsOpen(false);
      resetForm();
    }, 3000);
    return () => clearTimeout(id);
  }, [submitted]);
  return (
    <>
      <Button
        type="primary"
        shape="circle"
        onClick={() => setIsOpen(true)}
        className="fixed right-4 md:right-8 z-40"
        style={{ width: 56, height: 56, bottom: 96 }}
        aria-label={t('title')}
        icon={<Camera className="w-6 h-6" aria-hidden="true" />}
      />

      <Modal
        open={isOpen}
        onCancel={handleClose}
        footer={null}
        centered
        width={512}
        destroyOnHidden
      >
        <div style={{ display: 'grid', gap: 16 }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', gap: 16 }}>
            <div style={{ display: 'grid', gap: 4 }}>
              <Typography.Title level={4} style={{ margin: 0 }}>{t('title')}</Typography.Title>
              <Typography.Text type="secondary">{t('subtitle')}</Typography.Text>
            </div>
            <Button
              type="text"
              onClick={handleClose}
              aria-label={t('close')}
              style={{ minWidth: 44, minHeight: 44 }}
            >
              {t('close')}
            </Button>
          </div>

          {/* F-08: polite live region announces auto-dismiss success before it closes. */}
          <span role="status" aria-live="polite" style={{ position: 'absolute', width: 1, height: 1, overflow: 'hidden', clip: 'rect(0 0 0 0)' }}>
            {announcement}
          </span>

          {submitted ? (
            <div role="status" style={{ textAlign: 'center', padding: '48px 0' }}>
              <div
                aria-hidden="true"
                style={{
                  display: 'inline-flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                  width: 80,
                  height: 80,
                  borderRadius: token.borderRadiusLG,
                  marginBottom: 24,
                  backgroundColor: token.colorSuccessBg,
                  border: `1px solid ${token.colorSuccessBorder}`,
                }}
              >
                <CheckCircle style={{ width: 40, height: 40, color: token.colorSuccessText }} />
              </div>
              <Typography.Title level={3} style={{ margin: '0 0 12px' }}>{t('successTitle')}</Typography.Title>
              <Typography.Text type="secondary">{t('successBody')}</Typography.Text>
            </div>
          ) : (
            <form onSubmit={handleSubmit} style={{ display: 'grid', gap: 32 }}>
              {submitError && (
                <Alert
                  role="alert"
                  type="error"
                  message={submitError}
                  action={
                    <Button size="small" type="link" onClick={() => setSubmitError(null)}>
                      {t('dismiss')}
                    </Button>
                  }
                  showIcon
                />
              )}
              <div>
                <Typography.Text strong id="feedback-category" style={{ display: 'block', marginBottom: 16 }}>
                  {t('categoryLabel')}
                </Typography.Text>
                <div role="group" aria-labelledby="feedback-category" style={{ display: 'grid', gridTemplateColumns: 'repeat(3, 1fr)', gap: 12 }}>
                  {resolvedCategories.map((cat) => {
                    const selected = category === cat.value;
                    return (
                      <Button
                        key={cat.value}
                        type={selected ? 'primary' : 'default'}
                        size="large"
                        aria-pressed={selected}
                        onClick={() => setCategory(cat.value as FeedbackData['category'])}
                        style={{
                          height: 96,
                          display: 'flex',
                          flexDirection: 'column',
                          alignItems: 'center',
                          justifyContent: 'center',
                          gap: 12,
                          borderRadius: token.borderRadiusLG,
                          borderWidth: 2,
                          borderColor: selected ? token.colorPrimary : token.colorBorderSecondary,
                          fontWeight: 700,
                        }}
                        icon={cat.icon}
                      >
                        <span style={{ fontSize: 12, fontWeight: 700, textTransform: 'uppercase', letterSpacing: '0.1em' }}>
                          {selected ? `✓ ${cat.label}` : cat.label}
                        </span>
                      </Button>
                    );
                  })}
                </div>
              </div>

              <div style={{ display: 'grid', gap: 12 }}>
                <label htmlFor="feedback-subject">{t('subjectLabel')}</label>
                <Input
                  id="feedback-subject"
                  value={subject}
                  onChange={(e) => setSubject(e.target.value)}
                  placeholder={t('subjectPlaceholder')}
                  required
                  maxLength={100}
                />
              </div>

              <div style={{ display: 'grid', gap: 12 }}>
                <label htmlFor="feedback-message">{t('messageLabel')}</label>
                <TextArea
                  id="feedback-message"
                  value={message}
                  onChange={(e) => setMessage(e.target.value)}
                  placeholder={t('messagePlaceholder')}
                  required
                  maxLength={1000}
                  autoSize={{ minRows: 4 }}
                />
                <Typography.Text type="secondary" style={{ textAlign: 'right', fontSize: 12 }} aria-live="polite">
                  {message.length} / 1000
                </Typography.Text>
              </div>

              <div style={{ display: 'flex', alignItems: 'center', gap: 12, padding: 16, borderRadius: token.borderRadiusLG, border: `1px solid ${token.colorBorderSecondary}`, backgroundColor: token.colorFillTertiary }}>
                <Checkbox
                  id="include-screenshot"
                  checked={includeScreenshot}
                  onChange={(e) => {
                    const checked = e.target.checked;
                    setIncludeScreenshot(checked);
                    if (checked && !screenshot) captureScreenshot();
                  }}
                >
                  {t('screenshotLabel')}
                </Checkbox>
                {screenshot && (
                  <Typography.Text strong style={{ fontSize: 12, color: token.colorSuccessText }}>
                    {t('screenshotCaptured')}
                  </Typography.Text>
                )}
              </div>

              <Typography.Text type="secondary" style={{ fontSize: 12 }}>
                {t('deviceNote')}
              </Typography.Text>

              <Button
                htmlType="submit"
                type="primary"
                size="large"
                block
                loading={isSubmitting}
                disabled={isSubmitting || !subject.trim() || !message.trim()}
                style={{ height: 56, fontWeight: 700, textTransform: 'uppercase', letterSpacing: '0.1em', fontSize: 12 }}
              >
                {isSubmitting ? t('submitting') : t('submit')}
              </Button>
            </form>
          )}
        </div>
      </Modal>
    </>
  );
};

export default FeedbackWidget;
