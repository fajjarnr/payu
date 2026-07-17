'use client';

import React, { useState, useEffect, useMemo } from 'react';
import Image from 'next/image';
import { useLocale } from 'next-intl';
import { X } from 'lucide-react';
import clsx from 'clsx';
import { useRouter } from '@/lib/navigation';
import { usePopups } from '@/hooks';
import { Button } from '@/components/ui/button';

/** Read dismissed/shown popup id sets from localStorage + sessionStorage. */
function readPopupSetsFromStorage(
  popups: Array<{ id: string }>,
  storageKey: string,
  sessionKey: string
): { dismissed: Set<string>; shown: Set<string> } {
  const dismissed = new Set<string>();
  const shown = new Set<string>();
  for (const popup of popups) {
    if (localStorage.getItem(`${storageKey}-dismissed-${popup.id}`) === 'true') {
      dismissed.add(popup.id);
    }
    if (sessionStorage.getItem(`${sessionKey}-shown-${popup.id}`) === 'true') {
      shown.add(popup.id);
    }
  }
  return { dismissed, shown };
}
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogDescription,
} from '@/components/ui/dialog';

interface PromoPopupProps {
  segment?: string;
  location?: string;
  device?: string;
  storageKey?: string;
  delay?: number;
  sessionKey?: string;
}

interface PopupSession {
  shownThisSession: boolean;
  timestamp: number;
}

export default function PromoPopup({
  segment,
  location,
  device,
  storageKey = 'promo-popup-state',
  delay = 2000,
  sessionKey = 'promo-popup-session',
}: PromoPopupProps) {
  const router = useRouter();
  // BUG-FE-008 FIX: Use dynamic locale instead of hardcoded 'id-ID'
  const locale = useLocale();
  const bcp47Locale = locale === 'id' ? 'id-ID' : 'en-US';
  const { data: popups, isLoading } = usePopups({ segment, location, device });
  const [isOpen, setIsOpen] = useState(false);
  const [currentPopupIndex, setCurrentPopupIndex] = useState(0);
  // Lazy-init from sessionStorage. Server returns default; client first
  // render also returns the same default to avoid hydration mismatch.
  const [sessionState, setSessionState] = useState<PopupSession>(() => {
    if (typeof window === 'undefined') return { shownThisSession: false, timestamp: 0 };
    try {
      const stored = sessionStorage.getItem('promo-popup-session');
      if (!stored) return { shownThisSession: false, timestamp: 0 };
      const parsed = JSON.parse(stored) as PopupSession;
      if (Date.now() - parsed.timestamp > 30 * 60 * 1000) {
        sessionStorage.removeItem('promo-popup-session');
        return { shownThisSession: false, timestamp: 0 };
      }
      return parsed;
    } catch (err) {
      console.error('[PromoPopup] Failed to parse session state:', err);
      return { shownThisSession: false, timestamp: 0 };
    }
  });
  const [dismissedPopups, setDismissedPopups] = useState<Set<string>>(() => {
    if (typeof window === 'undefined' || !popups) return new Set();
    return readPopupSetsFromStorage(popups, storageKey, sessionKey).dismissed;
  });
  const [sessionShownPopups, setSessionShownPopups] = useState<Set<string>>(() => {
    if (typeof window === 'undefined' || !popups) return new Set();
    return readPopupSetsFromStorage(popups, storageKey, sessionKey).shown;
  });

  // Re-sync when popups list arrives (after the lazy initializer).
  // React 19 "adjusting state during render" pattern — runs during render
  // when popups/storageKey/sessionKey change to avoid cascading-render
  // warning from setState-in-effect.
  const [trackedDeps, setTrackedDeps] = useState({ popups, storageKey, sessionKey });
  const popupsChanged =
    trackedDeps.popups !== popups ||
    trackedDeps.storageKey !== storageKey ||
    trackedDeps.sessionKey !== sessionKey;
  if (popupsChanged && typeof window !== 'undefined' && popups) {
    setTrackedDeps({ popups, storageKey, sessionKey });
    try {
      const { dismissed, shown } = readPopupSetsFromStorage(popups, storageKey, sessionKey);
      setDismissedPopups(dismissed);
      setSessionShownPopups(shown);
    } catch (err) {
      console.error('[PromoPopup] Failed to read popup state:', err);
    }
  }

  // Filter eligible popups
  const eligiblePopups = useMemo(() =>
    popups?.filter((popup) => {
      if (dismissedPopups.has(popup.id)) return false;
      if (sessionShownPopups.has(popup.id)) return false;

      // Check date range
      const now = new Date();
      const startDate = new Date(popup.startDate);
      const endDate = new Date(popup.endDate);
      if (now < startDate || now > endDate) return false;

      return true;
    }) ?? [],
    [popups, dismissedPopups, sessionShownPopups]
  );

  // Auto-show popup after delay
  useEffect(() => {
    if (
      !isLoading &&
      eligiblePopups.length > 0 &&
      !sessionState.shownThisSession &&
      !isOpen
    ) {
      const timer = setTimeout(() => {
        setIsOpen(true);
      }, delay);

      return () => clearTimeout(timer);
    }
  }, [isLoading, eligiblePopups, sessionState, isOpen, delay]);

  const handleClose = (permanently = false) => {
    const currentPopup = eligiblePopups[currentPopupIndex];

    // Update session state
    const updatedSession = {
      shownThisSession: true,
      timestamp: Date.now(),
    };
    setSessionState(updatedSession);
    sessionStorage.setItem(sessionKey, JSON.stringify(updatedSession));

    // Mark as shown for this specific popup
    if (currentPopup) {
      const sessionShownKey = `${sessionKey}-shown-${currentPopup.id}`;
      sessionStorage.setItem(sessionShownKey, 'true');

      // If permanently dismissed
      if (permanently) {
        const dismissedKey = `${storageKey}-dismissed-${currentPopup.id}`;
        localStorage.setItem(dismissedKey, 'true');
      }

      // Move to next popup if available
      if (currentPopupIndex < eligiblePopups.length - 1) {
        setCurrentPopupIndex(currentPopupIndex + 1);
        setIsOpen(true);
      } else {
        setIsOpen(false);
      }
    } else {
      setIsOpen(false);
    }
  };

  const handleAction = () => {
    const currentPopup = eligiblePopups[currentPopupIndex];
    if (currentPopup?.actionUrl) {
      if (currentPopup.actionType === 'LINK') {
        window.open(currentPopup.actionUrl, '_blank', 'noopener,noreferrer');
      } else if (currentPopup.actionType === 'DEEP_LINK') {
        router.push(currentPopup.actionUrl);
      }
    }
  };

  if (eligiblePopups.length === 0) {
    return null;
  }

  const currentPopup = eligiblePopups[currentPopupIndex];
  if (!currentPopup) return null;

  return (
    <Dialog open={isOpen} onOpenChange={(open) => !open && handleClose(false)}>
      <DialogContent className="p-0 border-none bg-card max-w-lg rounded-3xl overflow-hidden shadow-2xl">
        <DialogHeader className="sr-only">
          <DialogTitle>{currentPopup.title}</DialogTitle>
          <DialogDescription>{currentPopup.description}</DialogDescription>
        </DialogHeader>

        {/* Close Button - Premium Position */}
        <Button
          variant="ghost"
          size="icon"
          onClick={() => handleClose(false)}
          className="absolute top-4 right-4 z-50 p-2 rounded-full bg-black/20 hover:bg-black/40 text-white backdrop-blur-md transition-all h-9 w-9"
          aria-label="Close popup"
        >
          <X className="h-4 w-4" />
        </Button>

        {/* Image Banner */}
        {currentPopup.imageUrl && (
          <div className="relative h-48 sm:h-64">
            <Image
              src={currentPopup.imageUrl}
              alt={currentPopup.title}
              fill
              className="object-cover"
              sizes="(max-width: 640px) 100vw, 512px"
            />
            {/* Gradient Overlay */}
            <div className="absolute inset-0 bg-gradient-to-t from-black/60 to-transparent" />
          </div>
        )}

        {/* Content */}
        <div
          className={clsx(
            'p-6 sm:p-10',
            currentPopup.imageUrl ? '-mt-12 relative z-10' : ''
          )}
        >
          <div className="mb-8">
            <span className="inline-block px-4 py-1.5 bg-bank-green/90 text-white text-xs font-bold tracking-[0.2em] rounded-full mb-4 uppercase border border-white/20 backdrop-blur-md">
              SPECIAL OFFER
            </span>
            <h3 className="text-2xl sm:text-4xl font-bold text-foreground mb-4 leading-tight uppercase">
              {currentPopup.title}
            </h3>
            {currentPopup.description && (
              <p className="text-sm sm:text-lg text-muted-foreground leading-relaxed font-medium">
                {currentPopup.description}
              </p>
            )}
          </div>

          {/* Buttons */}
          <div className="flex flex-col sm:flex-row gap-4 mb-8">
            <Button
              onClick={handleAction}
              className="flex-1 h-14 bg-bank-green hover:bg-bank-emerald text-white font-bold uppercase tracking-widest text-xs rounded-2xl shadow-xl shadow-bank-green/20"
            >
              Claim Now
            </Button>
            <Button
              variant="outline"
              onClick={() => handleClose(true)}
              className="flex-1 h-14 font-bold uppercase tracking-widest text-xs rounded-2xl border-border hover:bg-muted transition-all"
            >
              Don&apos;t Show Again
            </Button>
          </div>

          {/* Pagination Indicator */}
          {eligiblePopups.length > 1 && (
            <div className="flex justify-center gap-3 mb-6">
              {eligiblePopups.map((_, index) => (
                <div
                  key={index}
                  className={clsx(
                    'h-1.5 rounded-full transition-all duration-500',
                    index === currentPopupIndex
                      ? 'w-10 bg-bank-green'
                      : 'w-2 bg-muted-foreground/20'
                  )}
                />
              ))}
            </div>
          )}

          {/* Validity Date */}
          {currentPopup.endDate && (
            <p className="text-xs text-center text-muted-foreground font-bold uppercase tracking-[0.1em] opacity-60">
              Valid until{' '}
              {new Date(currentPopup.endDate).toLocaleDateString(bcp47Locale, {
                day: 'numeric',
                month: 'long',
                year: 'numeric',
              })}
            </p>
          )}
        </div>
      </DialogContent>
    </Dialog>
  );
}
