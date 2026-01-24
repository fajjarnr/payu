'use client';

import React, { useState, useEffect } from 'react';
import { X } from 'lucide-react';
import { motion, AnimatePresence } from 'framer-motion';
import clsx from 'clsx';
import { usePopups } from '@/hooks';
import type { Content } from '@/services/CMSService';

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
  const { data: popups, isLoading } = usePopups({ segment, location, device });
  const [isOpen, setIsOpen] = useState(false);
  const [currentPopupIndex, setCurrentPopupIndex] = useState(0);
  const [sessionState, setSessionState] = useState<PopupSession>({
    shownThisSession: false,
    timestamp: 0,
  });

  // Load session state from sessionStorage
  useEffect(() => {
    if (typeof window !== 'undefined') {
      try {
        const stored = sessionStorage.getItem(sessionKey);
        if (stored) {
          const parsed = JSON.parse(stored);
          // Reset if more than 30 minutes have passed
          const now = Date.now();
          if (now - parsed.timestamp > 30 * 60 * 1000) {
            sessionStorage.removeItem(sessionKey);
          } else {
            setSessionState(parsed);
          }
        }
      } catch (error) {
        console.error('Failed to load popup session state:', error);
      }
    }
  }, [sessionKey]);

  // Filter eligible popups
  const eligiblePopups = popups?.filter((popup) => {
    // Check if popup was permanently dismissed
    const dismissedKey = `${storageKey}-dismissed-${popup.id}`;
    const dismissed = localStorage.getItem(dismissedKey);
    if (dismissed === 'true') return false;

    // Check if popup was shown this session
    const sessionShownKey = `${sessionKey}-shown-${popup.id}`;
    const sessionShown = sessionStorage.getItem(sessionShownKey);
    if (sessionShown === 'true') return false;

    // Check date range
    const now = new Date();
    const startDate = new Date(popup.startDate);
    const endDate = new Date(popup.endDate);
    if (now < startDate || now > endDate) return false;

    return true;
  }) ?? [];

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
        window.location.href = currentPopup.actionUrl;
      }
    }
  };

  if (!isOpen || eligiblePopups.length === 0) {
    return null;
  }

  const currentPopup = eligiblePopups[currentPopupIndex];
  if (!currentPopup) return null;

  // Get background style from metadata
  const backgroundStyle = currentPopup.metadata?.backgroundImage
    ? {
        backgroundImage: `url(${currentPopup.metadata.backgroundImage as string})`,
        backgroundSize: 'cover',
        backgroundPosition: 'center',
      }
    : {};

  return (
    <AnimatePresence>
      {isOpen && currentPopup && (
        <>
          {/* Backdrop */}
          <motion.div
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            exit={{ opacity: 0 }}
            transition={{ duration: 0.2 }}
            className="fixed inset-0 bg-black/60 backdrop-blur-sm z-50"
            onClick={() => handleClose(false)}
          />

          {/* Modal */}
          <div className="fixed inset-0 z-50 flex items-center justify-center p-4">
            <motion.div
              initial={{ opacity: 0, scale: 0.9, y: 20 }}
              animate={{ opacity: 1, scale: 1, y: 0 }}
              exit={{ opacity: 0, scale: 0.9, y: 20 }}
              transition={{
                type: 'spring',
                damping: 25,
                stiffness: 300,
              }}
              className="relative w-full max-w-lg bg-card rounded-3xl shadow-2xl overflow-hidden"
              onClick={(e) => e.stopPropagation()}
            >
              {/* Close Button */}
              <button
                onClick={() => handleClose(false)}
                className="absolute top-4 right-4 z-10 p-2 rounded-full bg-black/20 hover:bg-black/40 text-white backdrop-blur-sm transition-colors"
                aria-label="Close popup"
              >
                <X className="h-5 w-5" />
              </button>

              {/* Image Banner */}
              {currentPopup.imageUrl && (
                <div className="relative h-48 sm:h-64">
                  <img
                    src={currentPopup.imageUrl}
                    alt={currentPopup.title}
                    className="w-full h-full object-cover"
                  />
                  {/* Gradient Overlay */}
                  <div className="absolute inset-0 bg-gradient-to-t from-black/60 to-transparent" />
                </div>
              )}

              {/* Content */}
              <div
                className={clsx(
                  'p-6 sm:p-8',
                  currentPopup.imageUrl ? '-mt-12 relative z-10' : ''
                )}
              >
                <div className="mb-6">
                  <span className="inline-block px-3 py-1 bg-bank-green/90 text-white text-xs font-bold tracking-wider rounded-full mb-3">
                    SPECIAL OFFER
                  </span>
                  <h3 className="text-2xl sm:text-3xl font-black text-foreground mb-3">
                    {currentPopup.title}
                  </h3>
                  {currentPopup.description && (
                    <p className="text-sm sm:text-base text-muted-foreground leading-relaxed">
                      {currentPopup.description}
                    </p>
                  )}
                </div>

                {/* Buttons */}
                <div className="flex flex-col sm:flex-row gap-3">
                  <button
                    onClick={handleAction}
                    className="flex-1 px-6 py-3 bg-bank-green hover:bg-bank-emerald text-white font-bold rounded-2xl shadow-lg shadow-bank-green/30 transition-all hover:scale-105 active:scale-95"
                  >
                    Claim Now
                  </button>
                  <button
                    onClick={() => handleClose(true)}
                    className="flex-1 px-6 py-3 bg-muted hover:bg-muted/80 text-foreground font-bold rounded-2xl transition-all hover:scale-105 active:scale-95"
                  >
                    Don't Show Again
                  </button>
                </div>

                {/* Pagination Indicator */}
                {eligiblePopups.length > 1 && (
                  <div className="flex justify-center gap-2 mt-6">
                    {eligiblePopups.map((_, index) => (
                      <div
                        key={index}
                        className={clsx(
                          'h-2 rounded-full transition-all duration-300',
                          index === currentPopupIndex
                            ? 'w-8 bg-bank-green'
                            : 'w-2 bg-muted-foreground/30'
                        )}
                      />
                    ))}
                  </div>
                )}

                {/* Validity Date */}
                {currentPopup.endDate && (
                  <p className="text-xs text-center text-muted-foreground mt-4">
                    Valid until{' '}
                    {new Date(currentPopup.endDate).toLocaleDateString('id-ID', {
                      day: 'numeric',
                      month: 'long',
                      year: 'numeric',
                    })}
                  </p>
                )}
              </div>
            </motion.div>
          </div>
        </>
      )}
    </AnimatePresence>
  );
}
