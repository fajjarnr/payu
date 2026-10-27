'use client';

import React, { useState, useEffect, useMemo } from 'react';
import Image from 'next/image';
import { useLocale, useTranslations } from 'next-intl';
import { X } from '@/components/icons';
import { useRouter } from '@/lib/navigation';
import { usePopups } from '@/hooks';
import { theme, Button, Modal, Typography, Tag, Alert } from 'antd';

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
  const t = useTranslations('promo');
  const tc = useTranslations('cms');
  const bcp47Locale = locale === 'id' ? 'id-ID' : 'en-US';
  const { token } = theme.useToken();
  const { data: popups, isLoading, error, refetch } = usePopups({ segment, location, device });
  const [isOpen, setIsOpen] = useState(false);
  // F-03: the timer only *announces* — it never opens or focuses the dialog.
  const [announced, setAnnounced] = useState(false);
  const [currentPopupIndex, setCurrentPopupIndex] = useState(0);
  // F-08: the dismissed promo must be announced, and the "promo available"
  // line must not keep claiming an offer the user just closed.
  const [dismissedTitle, setDismissedTitle] = useState<string | null>(null);
  // Lazy-init from sessionStorage. Server returns default; client first
  // render also returns the same default to avoid hydration mismatch.
  const [sessionState, setSessionState] = useState<PopupSession>(() => {
    if (typeof window === 'undefined') return { shownThisSession: false, timestamp: 0 };
    try {
      const stored = sessionStorage.getItem(sessionKey);
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

  // F-03: announce availability via polite region after delay; user opens the dialog.
  useEffect(() => {
    if (
      !isLoading &&
      eligiblePopups.length > 0 &&
      !sessionState.shownThisSession &&
      !announced
    ) {
      const timer = setTimeout(() => {
        setAnnounced(true);
      }, delay);

      return () => clearTimeout(timer);
    }
  }, [isLoading, eligiblePopups, sessionState, announced, delay]);

  const handleClose = (permanently = false) => {
    const currentPopup = eligiblePopups[currentPopupIndex];
    if (!currentPopup) {
      setIsOpen(false);
      return;
    }

    // Update session state
    const updatedSession = {
      shownThisSession: true,
      timestamp: Date.now(),
    };
    setSessionState(updatedSession);
    sessionStorage.setItem(sessionKey, JSON.stringify(updatedSession));

    // Mark as shown for this specific popup. The eligibility filter reads the
    // state, so without this the closed promo stays on screen ("Don't show
    // again" had no visible effect) and could be reopened in the same session.
    const sessionShownKey = `${sessionKey}-shown-${currentPopup.id}`;
    sessionStorage.setItem(sessionShownKey, 'true');
    setSessionShownPopups((prev) => new Set(prev).add(currentPopup.id));

    if (permanently) {
      const dismissedKey = `${storageKey}-dismissed-${currentPopup.id}`;
      localStorage.setItem(dismissedKey, 'true');
      setDismissedPopups((prev) => new Set(prev).add(currentPopup.id));
    }

    setDismissedTitle(currentPopup.title);

    // Move to the next popup if any remain eligible.
    const remaining = eligiblePopups.filter((popup) => popup.id !== currentPopup.id);
    if (remaining.length > 0) {
      setCurrentPopupIndex(Math.min(currentPopupIndex, remaining.length - 1));
      setIsOpen(true);
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

  // R-06: fetch failure renders error UI + retry, never silent null.
  if (error && !isLoading) {
    return (
      <Alert
        type="error"
        showIcon
        message={tc('popupLoadError')}
        action={
          <Button size="small" onClick={() => refetch()}>
            {tc('retry')}
          </Button>
        }
      />
    );
  }

  if (eligiblePopups.length === 0) {
    // The dismissal confirmation must outlive the popup it refers to.
    return dismissedTitle ? (
      <span role="status" aria-live="polite" className="sr-only">
        {tc('promoDismissed', { title: dismissedTitle })}
      </span>
    ) : null;
  }

  const currentPopup = eligiblePopups[currentPopupIndex];
  if (!currentPopup) return null;

  // F-03: pre-announcement render — dialog opens only via the user trigger below,
  // so the timer can never steal focus. Modal stays mounted for a11y tree stability.
  return (
    <>
      {announced && !isOpen && !dismissedTitle && (
        <div className="flex flex-col items-center gap-2">
          <div role="status">{tc('promoAnnouncement', { title: currentPopup.title })}</div>
          <Button type="primary" size="large" onClick={() => setIsOpen(true)}>
            {tc('viewPromo', { title: currentPopup.title })}
          </Button>
        </div>
      )}
      {dismissedTitle && (
        <span role="status" aria-live="polite" className="sr-only">
          {tc('promoDismissed', { title: dismissedTitle })}
        </span>
      )}
      <Modal
        open={isOpen}
        onCancel={() => handleClose(false)}
        footer={null}
        centered
        width={512}
        styles={{ body: { padding: 0 }, mask: { background: 'rgba(0,0,0,0.8)' }, container: { background: 'transparent', boxShadow: 'none', padding: 0, borderRadius: 24, overflow: 'visible' }, root: { background: 'transparent', boxShadow: 'none' } }}
        aria-modal="true"
      >
        <div
          className="relative z-50 mx-auto grid w-full max-w-lg gap-4 p-6 overflow-hidden"
          style={{ background: token.colorBgContainer, borderRadius: 24, border: `1px solid ${token.colorBorderSecondary}` }}
        >
          <div className="flex flex-col space-y-2 text-center sm:text-left sr-only">
            <div className="text-lg font-semibold leading-none tracking-tight">{currentPopup.title}</div>
            <div><Typography.Text type="secondary" className="text-sm">{currentPopup.description}</Typography.Text></div>
          </div>

          {/* Close Button - Premium Position */}
          <Button
            type="text"
            shape="circle"
            onClick={() => handleClose(false)}
            className="absolute top-4 right-4 z-50 p-2 transition-all"
            style={{ width: 44, height: 44 }}
            aria-label={tc('closePromo')}
            icon={<X className="h-4 w-4" />}
          />

          {/* Image Banner */}
          {currentPopup.imageUrl && (
            <div className="relative h-48 sm:h-64">
              <Image
                src={currentPopup.imageUrl}
                alt=""
                fill
                className="object-cover"
                loading="lazy"
                sizes="(max-width: 640px) 100vw, 512px"
              />
              {/* Gradient Overlay */}
              <div className="absolute inset-0 bg-gradient-to-t from-black/60 to-transparent" />
            </div>
          )}

          {/* Content — accessible name comes from title; image alt stays empty */}
          <div className="p-6 sm:p-8">
            <div className="mb-8">
              <Tag color="success" style={{ marginBottom: 16, textTransform: 'uppercase', letterSpacing: '0.2em' }}>
                {t('specialOffer')}
              </Tag>
              <h3 className="text-2xl sm:text-4xl font-semibold mb-4 leading-tight uppercase" style={{ color: token.colorText }}>
                {currentPopup.title}
              </h3>
              {currentPopup.description && (
                <Typography.Paragraph type="secondary" className="text-sm sm:text-lg leading-relaxed font-medium">
                  {currentPopup.description}
                </Typography.Paragraph>
              )}
            </div>

            {/* Buttons */}
            <div className="flex flex-col sm:flex-row gap-4 mb-8">
              {/* A-04: without a target the claim button was a silent no-op. */}
              {currentPopup.actionUrl && (
                <Button
                  type="primary"
                  size="large"
                  onClick={handleAction}
                  className="flex-1 h-14 font-semibold uppercase tracking-widest text-xs rounded-3xl"
                >
                  {t('claimNow')}
                </Button>
              )}
              <Button
                type="default"
                size="large"
                onClick={() => handleClose(true)}
                className="flex-1 h-14 font-semibold uppercase tracking-widest text-xs rounded-3xl transition-all"
              >
                {t('dismissForever')}
              </Button>
            </div>

            {/* Pagination Indicator — real buttons, not divs */}
            {eligiblePopups.length > 1 && (
              <div className="flex justify-center gap-3 mb-6">
                {eligiblePopups.map((popup, index) => (
                  <Button
                    key={popup.id}
                    type="text"
                    size="small"
                    aria-label={tc('goToPromo', { index: index + 1, title: popup.title })}
                    aria-current={index === currentPopupIndex ? 'true' : undefined}
                    onClick={() => setCurrentPopupIndex(index)}
                    className="flex h-11 min-w-11 items-center justify-center"
                  >
                    <span
                      aria-hidden="true"
                      className="h-1.5 rounded-full motion-safe:transition-all duration-500"
                      style={{
                        width: index === currentPopupIndex ? 40 : 8,
                        background: index === currentPopupIndex ? token.colorPrimary : token.colorTextQuaternary,
                        display: 'inline-block',
                      }}
                    />
                  </Button>
                ))}
              </div>
            )}

            {/* Validity Date */}
            {currentPopup.endDate && (
              <Typography.Text type="secondary" className="block text-xs text-center font-semibold uppercase" style={{ letterSpacing: '0.1em' }}>
                {tc('promoValidUntil', {
                  date: new Date(currentPopup.endDate).toLocaleDateString(bcp47Locale, {
                    day: 'numeric',
                    month: 'long',
                    year: 'numeric',
                  }),
                })}
              </Typography.Text>
            )}
          </div>
        </div>
      </Modal>
    </>
  );
}
