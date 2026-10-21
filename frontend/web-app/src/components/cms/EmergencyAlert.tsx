'use client';

import React, { useState } from 'react';
import { X, AlertTriangle, Info, AlertCircle } from '@/components/icons';
import { useRouter } from '@/lib/navigation';
import { useEmergencyAlerts } from '@/hooks';
import { useTranslations } from 'next-intl';
import type { Content } from '@/services/CMSService';
import { theme, Alert, Button, Typography } from 'antd';

interface EmergencyAlertProps {
  className?: string;
  segment?: string;
  location?: string;
  device?: string;
  storageKey?: string;
}

const ALERT_ICONS: Record<string, React.ElementType> = {
  INFO: Info,
  WARNING: AlertTriangle,
  ERROR: AlertCircle,
  DEFAULT: AlertCircle,
};

// WARNING → warning, ERROR → error, INFO/else → info. Must stay 1:1.
const SEVERITY_TO_ALERT_TYPE = {
  WARNING: 'warning',
  ERROR: 'error',
  INFO: 'info',
} as const;

type AntdAlertType = 'warning' | 'error' | 'info';

function toAlertType(alert: Content): AntdAlertType {
  const severity = alert.metadata?.alertType as string | undefined;
  return (severity && severity in SEVERITY_TO_ALERT_TYPE
    ? SEVERITY_TO_ALERT_TYPE[severity as keyof typeof SEVERITY_TO_ALERT_TYPE]
    : 'info') as AntdAlertType;
}

export default function EmergencyAlert({
  className,
  segment,
  location,
  device,
  storageKey = 'dismissed-alerts',
}: EmergencyAlertProps) {
  const router = useRouter();
  const t = useTranslations('cms');
  const { token } = theme.useToken();
  const { data: alerts, isLoading, error, refetch } = useEmergencyAlerts({ segment, location, device });
  // Initialize from localStorage lazily (avoids cascading render warning).
  // Server returns empty Set; first client render also returns empty Set to
  // match SSR. The actual localStorage value is then read on mount.
  // F-08: the dismissal outcome must be announced; the alert list itself is
  // not a live region (removing a row would re-announce every other row).
  const [status, setStatus] = useState('');
  const [dismissedAlerts, setDismissedAlerts] = useState<Set<string>>(() => {
    if (typeof window === 'undefined') return new Set();
    try {
      const stored = localStorage.getItem(storageKey);
      return stored ? new Set(JSON.parse(stored) as string[]) : new Set();
    } catch (err) {
      console.error('[EmergencyAlert] Failed to read dismissed alerts:', err);
      return new Set();
    }
  });

  // React 19 "adjusting state during render" — re-seed the set when the
  // storageKey prop changes. Runs during render so no cascading-render
  // warning from setState-in-effect.
  const [trackedKey, setTrackedKey] = useState(storageKey);
  if (trackedKey !== storageKey) {
    setTrackedKey(storageKey);
    if (typeof window !== 'undefined') {
      try {
        const stored = localStorage.getItem(storageKey);
        setDismissedAlerts(stored ? new Set(JSON.parse(stored) as string[]) : new Set());
      } catch (err) {
        console.error('[EmergencyAlert] Failed to read dismissed alerts:', err);
      }
    }
  }

  const saveDismissedAlert = (alertId: string) => {
    const updated = new Set(dismissedAlerts);
    updated.add(alertId);
    setDismissedAlerts(updated);

    if (typeof window !== 'undefined') {
      try {
        localStorage.setItem(storageKey, JSON.stringify([...updated]));
      } catch (err) {
        console.error('[EmergencyAlert] Failed to save dismissed alert:', err);
      }
    }
  };

  const handleDismiss = (alert: Content, e: React.MouseEvent) => {
    e.stopPropagation();
    saveDismissedAlert(alert.id);
    setStatus(t('alertDismissed', { title: alert.title }));
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

  // R-06: fetch failure logs + renders error UI with retry; zero alerts stays silent.
  if (error) {
    console.error('[EmergencyAlert] Failed to load alerts:', error);
    return (
      <div className={className}>
        <Alert
          type="error"
          showIcon
          message={t('alertLoadError')}
          action={
            <Button size="small" onClick={() => refetch()}>
              {t('retry')}
            </Button>
          }
        />
      </div>
    );
  }

  const activeAlerts = alerts?.filter((alert) => !dismissedAlerts.has(alert.id)) ?? [];

  if (isLoading || activeAlerts.length === 0) {
    // The announcer must outlive the last alert, otherwise the dismissal
    // confirmation is removed from the DOM before it is announced.
    return status ? (
      <span role="status" aria-live="polite" className="sr-only">
        {status}
      </span>
    ) : null;
  }

  return (
    <>
      <div className={className} style={{ display: 'flex', flexDirection: 'column', gap: 8 }}>
        {activeAlerts.map((alert) => {
          const alertType = toAlertType(alert);
          const Icon = ALERT_ICONS[alert.metadata?.alertType as string] || ALERT_ICONS.DEFAULT;
          const clickable = Boolean(alert.actionUrl);

          return (
            <div
              key={alert.id}
              className={clickable ? 'payu-alert-row' : undefined}
              // F-03: focusable ⟺ clickable — inert alerts stay out of the tab order.
              {...(clickable
                ? {
                    tabIndex: 0,
                    role: 'link' as const,
                    onClick: () => handleAlertClick(alert),
                    onKeyDown: (e: React.KeyboardEvent) => {
                      // The dismiss button lives inside this wrapper: its key
                      // events bubble here, and handling them would cancel the
                      // button activation (Space) or navigate away (Enter).
                      if (e.target !== e.currentTarget) return;
                      if (e.key === 'Enter' || e.key === ' ') {
                        e.preventDefault();
                        handleAlertClick(alert);
                      }
                    },
                  }
                : {})}
            >
            <Alert
              type={alertType}
              showIcon
              icon={<Icon className="h-4 w-4" aria-hidden="true" />}
              message={
                <span className="inline-flex items-center gap-2 font-bold uppercase tracking-tight text-xs mb-1">
                  {alert.title}
                </span>
              }
              description={
                <>
                  <Typography.Text className="text-xs font-medium line-clamp-2">
                    {alert.description}
                  </Typography.Text>
                  {alert.endDate && (
                    <Typography.Text type="secondary" className="block mt-2 text-xs">
                      {t('alertValidUntil', {
                        date: new Date(alert.endDate).toLocaleDateString(
                          undefined,
                          { day: 'numeric', month: 'long', year: 'numeric' }
                        ),
                      })}
                    </Typography.Text>
                  )}
                </>
              }
              action={
                <Button
                  type="text"
                  shape="circle"
                  className="absolute top-2 right-2"
                  style={{ width: 44, height: 44, color: token.colorText }}
                  onClick={(e) => handleDismiss(alert, e)}
                  aria-label={t('dismissAlertNamed', { title: alert.title })}
                  icon={<X className="h-4 w-4" />}
                />
              }
              className="relative p-4 pr-16 rounded-2xl"
              style={
                clickable
                  ? { background: token.colorBgContainer, border: `1px solid ${token.colorBorderSecondary}`, cursor: 'pointer' }
                  : { background: token.colorBgContainer, border: `1px solid ${token.colorBorderSecondary}` }
              }
            />
            </div>
          );
        })}
      </div>
      <style>{`.payu-alert-row:focus-visible{outline:3px solid ${token.colorPrimaryBorder};outline-offset:2px;}`}</style>
      <span role="status" aria-live="polite" className="sr-only">
        {status}
      </span>
    </>
  );
}
