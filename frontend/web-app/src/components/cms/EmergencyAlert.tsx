'use client';

import React, { useState } from 'react';
import { X, AlertTriangle, Info, AlertCircle } from 'lucide-react';
import { motion, AnimatePresence } from 'framer-motion';
import clsx from 'clsx';
import { useRouter } from 'next/navigation';
import { useEmergencyAlerts } from '@/hooks';
import type { Content } from '@/services/CMSService';

interface EmergencyAlertProps {
  className?: string;
  segment?: string;
  location?: string;
  device?: string;
  storageKey?: string;
}

// Alert type icons mapping
const ALERT_ICONS: Record<string, React.ElementType> = {
  INFO: Info,
  WARNING: AlertTriangle,
  ERROR: AlertCircle,
  DEFAULT: AlertCircle,
};

// Alert color mapping
const ALERT_COLORS: Record<string, { bg: string; border: string; text: string; icon: string }> = {
  INFO: {
    bg: 'bg-blue-50 dark:bg-blue-950/30',
    border: 'border-blue-200 dark:border-blue-800',
    text: 'text-blue-900 dark:text-blue-100',
    icon: 'text-blue-600 dark:text-blue-400',
  },
  WARNING: {
    bg: 'bg-amber-50 dark:bg-amber-950/30',
    border: 'border-amber-200 dark:border-amber-800',
    text: 'text-amber-900 dark:text-amber-100',
    icon: 'text-amber-600 dark:text-amber-400',
  },
  ERROR: {
    bg: 'bg-red-50 dark:bg-red-950/30',
    border: 'border-red-200 dark:border-red-800',
    text: 'text-red-900 dark:text-red-100',
    icon: 'text-red-600 dark:text-red-400',
  },
  DEFAULT: {
    bg: 'bg-bank-green/10',
    border: 'border-bank-green/30',
    text: 'text-foreground',
    icon: 'text-bank-green',
  },
};

export default function EmergencyAlert({
  className,
  segment,
  location,
  device,
  storageKey = 'dismissed-alerts',
}: EmergencyAlertProps) {
  const router = useRouter();
  const { data: alerts, isLoading } = useEmergencyAlerts({ segment, location, device });
  const [dismissedAlerts, setDismissedAlerts] = useState<Set<string>>(() => {
    if (typeof window === 'undefined') return new Set();
    try {
      const stored = localStorage.getItem(storageKey);
      return stored ? new Set(JSON.parse(stored)) : new Set();
    } catch {
      return new Set();
    }
  });

  // Save dismissed alerts to localStorage
  const saveDismissedAlert = (alertId: string) => {
    const updated = new Set(dismissedAlerts);
    updated.add(alertId);
    setDismissedAlerts(updated);

    if (typeof window !== 'undefined') {
      try {
        localStorage.setItem(storageKey, JSON.stringify([...updated]));
      } catch (error) {
        console.error('Failed to save dismissed alert:', error);
      }
    }
  };

  const handleDismiss = (alertId: string, e: React.MouseEvent) => {
    e.stopPropagation();
    saveDismissedAlert(alertId);
  };

  const handleAlertClick = (alert: Content) => {
    if (alert.actionUrl) {
      if (alert.actionType === 'LINK') {
        window.open(alert.actionUrl, '_blank', 'noopener,noreferrer');
      } else if (alert.actionType === 'DEEP_LINK') {
        router.push(alert.actionUrl);
      }
    }
  };

  // Filter out dismissed alerts
  const activeAlerts = alerts?.filter((alert) => !dismissedAlerts.has(alert.id)) ?? [];

  if (isLoading || activeAlerts.length === 0) {
    return null;
  }

  // Get alert type from metadata
  const getAlertType = (alert: Content): string => {
    return (alert.metadata?.alertType as string) || 'DEFAULT';
  };

  return (
    <AnimatePresence>
      {activeAlerts.map((alert) => {
        const alertType = getAlertType(alert);
        const colors = ALERT_COLORS[alertType] || ALERT_COLORS.DEFAULT;
        const Icon = ALERT_ICONS[alertType] || ALERT_ICONS.DEFAULT;

        return (
          <motion.div
            key={alert.id}
            initial={{ opacity: 0, y: -20, height: 0 }}
            animate={{ opacity: 1, y: 0, height: 'auto' }}
            exit={{ opacity: 0, y: -20, height: 0 }}
            transition={{ duration: 0.3, ease: 'easeInOut' }}
            className={clsx(
              'w-full border-b-2 backdrop-blur-md transition-all cursor-pointer',
              colors.bg,
              colors.border,
              className
            )}
            onClick={() => handleAlertClick(alert)}
          >
            <div className="container mx-auto px-4 py-3">
              <div className="flex items-start gap-3">
                {/* Icon */}
                <div className={clsx('flex-shrink-0 mt-0.5', colors.icon)}>
                  <Icon className="h-5 w-5" />
                </div>

                {/* Content */}
                <div className="flex-1 min-w-0">
                  <div className="flex items-start justify-between gap-2">
                    <div className="flex-1">
                      <h4
                        className={clsx(
                          'text-sm font-bold leading-tight mb-1',
                          colors.text
                        )}
                      >
                        {alert.title}
                      </h4>
                      {alert.description && (
                        <p
                          className={clsx(
                            'text-xs leading-relaxed line-clamp-2',
                            colors.text
                          )}
                        >
                          {alert.description}
                        </p>
                      )}
                    </div>

                    {/* Dismiss Button */}
                    <button
                      onClick={(e) => handleDismiss(alert.id, e)}
                      className={clsx(
                        'flex-shrink-0 p-1 rounded-full hover:bg-black/10 dark:hover:bg-white/10 transition-colors',
                        colors.text
                      )}
                      aria-label="Dismiss alert"
                    >
                      <X className="h-4 w-4" />
                    </button>
                  </div>

                  {/* Timestamp if available */}
                  {alert.endDate && (
                    <p className="text-xs opacity-70 mt-1">
                      Valid until {new Date(alert.endDate).toLocaleDateString('id-ID', {
                        day: 'numeric',
                        month: 'short',
                        year: 'numeric',
                      })}
                    </p>
                  )}
                </div>
              </div>
            </div>
          </motion.div>
        );
      })}
    </AnimatePresence>
  );
}
