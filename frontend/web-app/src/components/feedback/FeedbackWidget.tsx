// Feedback widget component for in-app user feedback
// Provides screenshot capture and context logging

'use client';

import React, { useState, useRef } from 'react';
import { Camera, X, AlertCircle, CheckCircle, Bug, Lightbulb } from 'lucide-react';
import { a11yUtils } from '@/lib/a11y';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Textarea } from '@/components/ui/textarea';
import { Checkbox } from '@/components/ui/checkbox';
import { Label } from '@/components/ui/label';
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogDescription,
} from '@/components/ui/dialog';

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

      setTimeout(() => {
        setSubmitted(false);
        setIsOpen(false);
        resetForm();
      }, 3000);
    } catch (error) {
      console.error('Feedback submission error:', error);
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
      <Button
        onClick={() => setIsOpen(true)}
        className="fixed bottom-24 right-4 md:bottom-8 md:right-8 z-40 h-14 w-14 rounded-full shadow-2xl bg-bank-green hover:bg-bank-emerald text-white animate-in slide-in-from-bottom-10"
        aria-label="Kirim Feedback"
      >
        <Camera className="w-6 h-6" />
      </Button>

      <Dialog open={isOpen} onOpenChange={setIsOpen}>
        <DialogContent className="max-w-lg bg-card rounded-3xl p-0 overflow-hidden border-border/10 shadow-3xl">
          <DialogHeader className="p-8 pb-4 border-b border-border/5">
            <DialogTitle className="text-xl font-bold uppercase tracking-tight">Kirim Feedback</DialogTitle>
            <DialogDescription className="text-xs font-bold uppercase tracking-[0.2em] opacity-60">
              Bantu kami meningkatkan layanan PayU
            </DialogDescription>
          </DialogHeader>

          <div className="p-8 pt-6">
            {submitted ? (
              <div className="text-center py-12 animate-in fade-in zoom-in duration-500">
                <div className="inline-flex items-center justify-center w-20 h-20 bg-bank-green/10 rounded-3xl mb-6">
                  <CheckCircle className="w-10 h-10 text-bank-green" />
                </div>
                <h3 className="font-bold text-2xl mb-3 uppercase tracking-tight">Terima Kasih!</h3>
                <p className="text-muted-foreground font-medium">
                  Feedback Anda sangat berharga bagi pengembangan platform kami.
                </p>
              </div>
            ) : (
              <form onSubmit={handleSubmit} className="space-y-8">
                <div>
                  <Label className="text-xs font-bold uppercase tracking-[0.2em] mb-4 block opacity-70">
                    Kategori Feedback
                  </Label>
                  <div className="grid grid-cols-3 gap-3">
                    {categories.map((cat) => (
                      <Button
                        key={cat.value}
                        type="button"
                        variant={category === cat.value ? 'default' : 'outline'}
                        onClick={() => setCategory(cat.value as FeedbackData['category'])}
                        className={`h-24 flex flex-col items-center justify-center gap-3 rounded-2xl border-2 transition-all ${
                          category === cat.value
                            ? 'bg-bank-green/10 text-bank-green border-bank-green shadow-lg shadow-bank-green/10'
                            : 'border-border/50 opacity-60 hover:opacity-100'
                        }`}
                      >
                        {cat.icon}
                        <span className="text-xs font-bold uppercase tracking-widest">{cat.label}</span>
                      </Button>
                    ))}
                  </div>
                </div>

                <div className="space-y-3">
                  <Label htmlFor="feedback-subject" className="text-xs font-bold uppercase tracking-[0.2em] opacity-70">
                    Subjek
                  </Label>
                  <Input
                    id="feedback-subject"
                    value={subject}
                    onChange={(e) => setSubject(e.target.value)}
                    placeholder="Masalah atau saran singkat..."
                    className="h-14 rounded-xl font-bold bg-muted/30 border-border/50 focus:border-bank-green/50 transition-all px-5"
                    required
                  />
                </div>

                <div className="space-y-3">
                  <Label htmlFor="feedback-message" className="text-xs font-bold uppercase tracking-[0.2em] opacity-70">
                    Pesan Detail
                  </Label>
                  <Textarea
                    id="feedback-message"
                    value={message}
                    onChange={(e) => setMessage(e.target.value)}
                    placeholder="Ceritakan detail lebih lanjut..."
                    className="rounded-xl font-bold bg-muted/30 border-border/50 focus:border-bank-green/50 transition-all p-5 min-h-[120px]"
                    required
                  />
                </div>

                <div className="flex items-center space-x-3 p-4 bg-muted/20 rounded-xl border border-border/5">
                  <Checkbox
                    id="include-screenshot"
                    checked={includeScreenshot}
                    onCheckedChange={(checked) => {
                      setIncludeScreenshot(!!checked);
                      if (checked && !screenshot) captureScreenshot();
                    }}
                  />
                  <Label htmlFor="include-screenshot" className="flex-1 text-xs font-bold cursor-pointer opacity-80">
                    Sertakan tangkapan layar otomatis
                  </Label>
                  {screenshot && (
                    <div className="text-xs font-bold text-bank-green flex items-center gap-1.5 uppercase tracking-widest">
                      <div className="h-1.5 w-1.5 bg-bank-green rounded-full animate-pulse" />
                      Captured
                    </div>
                  )}
                </div>

                <Button
                  type="submit"
                  disabled={isSubmitting || !subject.trim() || !message.trim()}
                  className="w-full h-16 bg-bank-green hover:bg-bank-emerald text-white font-bold uppercase tracking-[0.25em] text-xs rounded-2xl shadow-xl shadow-bank-green/20"
                >
                  {isSubmitting ? 'Mengirim Data...' : 'Kirim Sekarang'}
                </Button>
              </form>
            )}
          </div>
        </DialogContent>
      </Dialog>
    </>
  );
};

export default FeedbackWidget;
