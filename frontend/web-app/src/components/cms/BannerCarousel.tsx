'use client';

import React, { useEffect, useRef, useState } from 'react';
import Image from 'next/image';
import { theme, Button, Carousel, Skeleton, Typography, Tag, Alert } from 'antd';
import { ChevronLeft, ChevronRight, Pause, Play } from '@/components/icons';
import type { CarouselRef } from 'antd/es/carousel';
import { useTranslations } from 'next-intl';
import { cn } from '@/lib/utils';
import { useBanners } from '@/hooks';
import { useRouter } from '@/lib/navigation';
import type { Content } from '@/services/CMSService';

interface BannerCarouselProps {
  className?: string;
  autoPlayInterval?: number;
  segment?: string;
  location?: string;
  device?: string;
  onBannerClick?: (banner: Content) => void;
}

export default function BannerCarousel({
  className,
  autoPlayInterval = 5000,
  segment,
  location,
  device,
  onBannerClick,
}: BannerCarouselProps) {
  const { data: banners, isLoading, error, refetch } = useBanners({ segment, location, device });
  const t = useTranslations('cms');
  // BUG-FE-101: Use Next.js router for DEEP_LINK navigation
  const router = useRouter();
  // BUG-FE-011 FIX: Debounce navigation to prevent history flooding
  const isNavigating = useRef(false);
  const carouselRef = useRef<CarouselRef>(null);
  const [current, setCurrent] = useState(0);
  // Transient hover/focus pause vs the user's explicit toggle (touch users get
  // no hover — the toggle must stick after pointer/focus leaves).
  const [userPaused, setUserPaused] = useState(false);
  const [isHovered, setIsHovered] = useState(false);
  const [reducedMotion, setReducedMotion] = useState(false);
  const { token } = theme.useToken();

  // F-07: honor prefers-reduced-motion — freeze autoplay + kill scale/pulse decor.
  useEffect(() => {
    const mq = window.matchMedia('(prefers-reduced-motion: reduce)');
    setReducedMotion(mq.matches);
    const onChange = (e: MediaQueryListEvent) => setReducedMotion(e.matches);
    mq.addEventListener('change', onChange);
    return () => mq.removeEventListener('change', onChange);
  }, []);

  const isPaused = isHovered || userPaused;
  const autoplayOn = !reducedMotion && !isPaused;
  useEffect(() => {
    if (!autoplayOn || !banners || banners.length <= 1) return;
    const id = setInterval(() => {
      const next = (current + 1) % banners.length;
      setCurrent(next);
      carouselRef.current?.goTo(next);
    }, autoPlayInterval);
    return () => clearInterval(id);
  }, [autoplayOn, autoPlayInterval, banners, current]);

  const handleBannerClick = (banner: Content) => {
    if (onBannerClick) {
      onBannerClick(banner);
    } else if (banner.actionUrl) {
      if (banner.actionType === 'LINK') {
        window.open(banner.actionUrl, '_blank', 'noopener,noreferrer');
      } else if (banner.actionType === 'DEEP_LINK') {
        if (isNavigating.current) return;
        isNavigating.current = true;
        // BUG-FE-011 FIX: Use replace instead of push to prevent history flooding
        router.replace(banner.actionUrl);
        setTimeout(() => { isNavigating.current = false; }, 1000);
      }
    }
  };

  if (isLoading) {
    return (
      <div className={cn('w-full', className)}>
        <Skeleton title={false} paragraph={false} className={cn('block animate-pulse w-full rounded-2xl aspect-[2.1/1] sm:aspect-[2.5/1] md:aspect-[3/1]')} />
      </div>
    );
  }
  // R-06: error resolves to error UI + retry, never silent null.
  if (error) {
    return (
      <div className={cn('w-full', className)}>
        <Alert
          type="error"
          showIcon
          message={t('bannerLoadError')}
          action={
            <Button size="small" onClick={() => refetch()}>
              {t('retry')}
            </Button>
          }
        />
      </div>
    );
  }

  if (!banners || banners.length === 0) {
    return null;
  }

  // Why: arrows keep focus-visible ring on all viewports (no hover-only reveal).
  const arrowRing = `
    .cms-arrow:focus-visible { outline: 3px solid ${token.colorPrimaryBorder}; outline-offset: 2px; }
  `;

  return (
    <div
      className={cn('relative w-full', className)}
      onMouseEnter={() => setIsHovered(true)}
      onMouseLeave={() => setIsHovered(false)}
      onFocus={() => setIsHovered(true)}
      onBlur={() => setIsHovered(false)}
    >
      <style>{arrowRing}</style>
      <div className="relative w-full">
        <Carousel
          ref={carouselRef}
          effect="scrollx"
          dots={false}
          afterChange={(index) => setCurrent(index)}
        >
          {banners.map((banner) => (
            <div key={banner.id} className="pl-0">
              <div
                className="relative overflow-hidden rounded-2xl cursor-pointer group/item"
                style={{ aspectRatio: '1.8/1', boxShadow: token.boxShadowSecondary }}
                onClick={() => handleBannerClick(banner)}
                role="button"
                tabIndex={0}
                aria-label={banner.title}
                onKeyDown={(e) => {
                  if (e.key === 'Enter' || e.key === ' ') {
                    e.preventDefault();
                    handleBannerClick(banner);
                  }
                }}
              >
                {/* Background Image — one accessible name lives on the slide, so alt stays empty */}
                <div className={cn('absolute inset-0', !reducedMotion && 'transition-transform duration-1000 group-hover/item:scale-105')}>
                  <Image
                    src={banner.imageUrl}
                    alt=""
                    fill
                    className="object-cover object-center"
                    priority={banners.indexOf(banner) === 0}
                    sizes="(max-width: 768px) 100vw, (max-width: 1200px) 80vw, 1200px"
                  />
                  {/* Gradient Overlay */}
                  <div className="absolute inset-0 bg-gradient-to-r from-black/80 via-black/40 to-transparent" />
                </div>

                {/* Content */}
                <div className="relative z-10 h-full flex flex-col justify-center p-5 sm:p-8 lg:p-10">
                  <div className="max-w-xl">
                    <Tag color="success" style={{ marginBottom: 16, textTransform: 'uppercase', letterSpacing: '0.2em' }}>
                      PROMO
                    </Tag>
                    <Typography.Title level={3} style={{ color: '#fff', marginBottom: 12, textTransform: 'uppercase' }}>
                      {banner.title}
                    </Typography.Title>
                    <Typography.Paragraph style={{ color: 'rgba(255,255,255,0.85)', marginBottom: 0 }} className="text-sm sm:text-lg line-clamp-2 max-w-md">
                      {banner.description}
                    </Typography.Paragraph>
                  </div>
                </div>
              </div>
            </div>
          ))}
        </Carousel>
        {/* Navigation — always rendered on every viewport, focus-visible ring ≥2px */}
        {banners.length > 1 && (
          <>
            <Button
              type="default"
              shape="circle"
              icon={<ChevronLeft className="h-4 w-4" />}
              aria-label={t('prevBanner')}
              onClick={(e) => { e.preventDefault(); setCurrent((p) => (p - 1 + banners.length) % banners.length); carouselRef.current?.prev(); }}
              className="cms-arrow absolute left-2 top-1/2 z-10 -translate-y-1/2 h-12 w-12"
            />
            <Button
              type="default"
              shape="circle"
              icon={<ChevronRight className="h-4 w-4" />}
              aria-label={t('nextBanner')}
              onClick={(e) => { e.preventDefault(); setCurrent((p) => (p + 1) % banners.length); carouselRef.current?.next(); }}
              className="cms-arrow absolute right-2 top-1/2 z-10 -translate-y-1/2 h-12 w-12"
            />
            {/* Touch users get no hover/focus pause — explicit toggle (WCAG 2.2.2). */}
            <Button
              type="default"
              shape="circle"
              icon={autoplayOn ? <Pause className="h-4 w-4" /> : <Play className="h-4 w-4" />}
              aria-label={t(autoplayOn ? 'pauseAutoplay' : 'resumeAutoplay')}
              aria-pressed={!autoplayOn}
              onClick={(e) => { e.preventDefault(); setUserPaused((p) => !p); }}
              className="cms-arrow absolute bottom-2 right-2 z-10 h-11 w-11"
            />
          </>
        )}
      </div>
      {banners.length > 1 && (
        <div className="mt-3 flex justify-center gap-2" role="group" aria-label={t('bannerPagination')}>
          {banners.map((banner, index) => (
            <Button
              key={banner.id}
              type="text"
              aria-label={t('goToBanner', { index: index + 1 })}
              aria-current={index === current ? 'true' : undefined}
              onClick={() => { setCurrent(index); carouselRef.current?.goTo(index); }}
              className="flex h-11 w-11 items-center justify-center cursor-pointer"
            >
              <span
                aria-hidden="true"
                className={cn('h-2 rounded-full transition-all', index === current ? 'w-8' : 'w-2')}
                style={{ background: index === current ? token.colorPrimary : token.colorTextQuaternary }}
              />
            </Button>
          ))}
        </div>
      )}
    </div>
  );
}
