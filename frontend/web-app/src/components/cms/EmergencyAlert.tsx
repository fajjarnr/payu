'use client';

import React, { useState } from 'react';
import { X, AlertTriangle, Info, AlertCircle } from '@/components/icons';
import clsx from 'clsx';
import { useRouter } from '@/lib/navigation';
import { useEmergencyAlerts } from '@/hooks';
import type { Content } from '@/services/CMSService';
import { Alert, Button } from 'antd';

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

export default function EmergencyAlert({
  className,
  segment,
  location,
  device,
  storageKey = 'dismissed-alerts',
}: EmergencyAlertProps) {
  const router = useRouter();
  const { data: alerts, isLoading } = useEmergencyAlerts({ segment, location, device });
  // Initialize from localStorage lazily (avoids cascading render warning).
  // Server returns empty Set; first client render also returns empty Set to
  // match SSR. The actual localStorage value is then read on mount.
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

  const activeAlerts = alerts?.filter((alert) => !dismissedAlerts.has(alert.id)) ?? [];

  if (isLoading || activeAlerts.length === 0) {
    return null;
  }

  const getAlertType = (alert: Content): "default" | "destructive" => {
    const type = alert.metadata?.alertType as string;
    return type === 'ERROR' ? 'destructive' : 'default';
  };

  const getAlertClasses = (alert: Content) => {
    const type = alert.metadata?.alertType as string;
    if (type === 'WARNING') return 'bg-amber-50 dark:bg-amber-950/30 border-amber-300';
    if (type === 'INFO') return 'bg-blue-50 dark:bg-blue-950/30 border-blue-300';
    return '';
  };

  return (
    <div className={clsx("w-full space-y-2", className)}>
      {activeAlerts.map((alert) => {
        const alertType = getAlertType(alert);
        const Icon = ALERT_ICONS[alert.metadata?.alertType as string] || ALERT_ICONS.DEFAULT;

        return (
          <div
            key={alert.id}
            tabIndex={alert.actionUrl ? 0 : undefined}
            onKeyDown={alert.actionUrl ? (e) => { if (e.key === 'Enter' || e.key === ' ') { e.preventDefault(); handleAlertClick(alert); } } : undefined}
          >
            <Alert
              type={alertType === 'destructive' ? 'error' : 'info'}
              title={
                <span className="inline-flex items-center gap-2 font-bold uppercase tracking-tight text-xs mb-1">
                  <Icon className="h-4 w-4" />
                  {alert.title}
                </span>
              }
              description={
                <>
                  <span className="text-xs font-medium opacity-80 line-clamp-2">
                    {alert.description}
                  </span>
                  {alert.endDate && (
                    <p className="mt-2 text-xs text-muted-foreground">
                      Valid until {new Date(alert.endDate).toLocaleDateString()}
                    </p>
                  )}
                </>
              }
              action={
                <Button
                  type="text"
                  shape="circle"
                  className="absolute top-2 right-2 hover:bg-black/5 dark:hover:bg-white/5"
                  style={{ width: 44, height: 44 }}
                  onClick={(e) => handleDismiss(alert.id, e)}
                  aria-label="Dismiss alert"
                  icon={<X className="h-4 w-4" />}
                />
              }
              className={clsx(
                "relative p-4 pr-12 cursor-pointer transition-all hover:ring-2 hover:ring-primary/20 bg-background/50 backdrop-blur-md border-b-2",
                getAlertClasses(alert),
                alertType === 'default' && "border-primary/20"
              )}
              onClick={() => handleAlertClick(alert)}
            />
          </div>
        );
      })}
    </div>
  );
}
