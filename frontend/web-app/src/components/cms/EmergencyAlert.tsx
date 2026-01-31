'use client';

import React, { useState } from 'react';
import { X, AlertTriangle, Info, AlertCircle } from 'lucide-react';
import { motion, AnimatePresence } from 'framer-motion';
import clsx from 'clsx';
import { useRouter } from 'next/navigation';
import { useEmergencyAlerts } from '@/hooks';
import type { Content } from '@/services/CMSService';
import { Alert, AlertDescription, AlertTitle } from '@/components/ui/alert';
import { Button } from '@/components/ui/button';

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
  const getAlertType = (alert: Content): "default" | "destructive" => {
    const type = alert.metadata?.alertType as string;
    return type === 'ERROR' ? 'destructive' : 'default';
  };

  return (
    <div className={clsx("w-full space-y-2", className)}>
      <AnimatePresence>
        {activeAlerts.map((alert) => {
          const alertType = getAlertType(alert);
          const Icon = ALERT_ICONS[alert.metadata?.alertType as string] || ALERT_ICONS.DEFAULT;

          return (
            <motion.div
              key={alert.id}
              initial={{ opacity: 0, scale: 0.95, y: -10 }}
              animate={{ opacity: 1, scale: 1, y: 0 }}
              exit={{ opacity: 0, scale: 0.95, y: -10 }}
              transition={{ duration: 0.2 }}
            >
              <Alert 
                variant={alertType}
                className={clsx(
                  "relative pr-12 cursor-pointer transition-all hover:ring-2 hover:ring-primary/20 bg-background/50 backdrop-blur-md",
                  alertType === 'default' && "border-primary/20"
                )}
                onClick={() => handleAlertClick(alert)}
              >
                <Icon className="h-4 w-4" />
                <AlertTitle className="font-bold uppercase tracking-tight text-xs mb-1">
                  {alert.title}
                </AlertTitle>
                <AlertDescription className="text-xs font-medium opacity-80 line-clamp-2">
                  {alert.description}
                </AlertDescription>
                
                {/* Dismiss Button */}
                <Button
                  variant="ghost"
                  size="icon"
                  className="absolute top-2 right-2 h-8 w-8 rounded-lg hover:bg-black/5 dark:hover:bg-white/5"
                  onClick={(e) => handleDismiss(alert.id, e)}
                >
                  <X className="h-4 w-4" />
                </Button>
              </Alert>
            </motion.div>
          );
        })}
      </AnimatePresence>
    </div>
  );
}
