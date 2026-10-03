'use client';

import React, { useEffect, useRef, useState } from 'react';
import Image from 'next/image';
import clsx from 'clsx';
import { Button, Carousel, Skeleton } from 'antd';
import { ChevronLeft, ChevronRight } from '@/components/icons';
import type { CarouselRef } from 'antd/es/carousel';
import { cn } from '@/lib/utils';
import { useBanners } from '@/hooks';
import { useRouter } from '@/lib/navigation';
import type { Content } from '@/services/CMSService';
type CarouselApi = {
  selectedScrollSnap: () => number;
  scrollSnapList: () => number[];
  scrollTo: (index: number) => void;
  on: (event: string, cb: () => void) => void;
  off: (event: string, cb: () => void) => void;
};

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
  const { data: banners, isLoading, error } = useBanners({ segment, location, device });
  // BUG-FE-101: Use Next.js router for DEEP_LINK navigation
  const router = useRouter();
  // BUG-FE-011 FIX: Debounce navigation to prevent history flooding
  const isNavigating = useRef(false);
  const carouselRef = useRef<CarouselRef>(null);
  const listeners = useRef<Set<() => void>>(new Set());
  const [current, setCurrent] = useState(0);

  const api = React.useMemo<CarouselApi>(
    () => ({
      selectedScrollSnap: () => current,
      scrollSnapList: () => Array.from({ length: banners?.length ?? 0 }, (_, i) => i),
      scrollTo: (index: number) => carouselRef.current?.goTo(index),
      on: (event, cb) => {
        if (event === 'select') listeners.current.add(cb);
      },
      off: (event, cb) => {
        if (event === 'select') listeners.current.delete(cb);
      },
    }),
    [current, banners?.length]
  );

  // ponytail: native autoplay via scrollTo — replaces embla-autoplay 8.6, 0 deps
  useEffect(() => {
    if (!banners || banners.length <= 1) return
    const id = setInterval(() => {
      const next = (current + 1) % banners.length
      carouselRef.current?.goTo(next)
    }, autoPlayInterval)
    return () => clearInterval(id)
  }, [current, autoPlayInterval, banners])

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
      <div className={clsx('w-full', className)}>
        <Skeleton title={false} paragraph={false} className={cn('block animate-pulse rounded-xl bg-muted/50', 'w-full rounded-2xl aspect-[2.1/1] sm:aspect-[2.5/1] md:aspect-[3/1]')} />
      </div>
    );
  }

  if (error || !banners || banners.length === 0) {
    return null;
  }

  return (
    <div className={clsx('relative w-full group/carousel', className)}>
      <div className="relative w-full">
        <Carousel
          ref={carouselRef}
          effect="scrollx"
          dots={false}
          afterChange={(index) => {
            setCurrent(index);
            listeners.current.forEach((cb) => {
              try {
                cb();
              } catch {
                /* listener error */
              }
            });
          }}
        >
          {banners.map((banner, index) => (
            <div key={banner.id} className="pl-0">
              <div
                className="relative overflow-hidden rounded-2xl shadow-2xl shadow-bank-green/20 aspect-[1.8/1] sm:aspect-[2.5/1] md:aspect-[3.2/1] cursor-pointer group/item"
                onClick={() => handleBannerClick(banner)}
              >
                {/* Background Image */}
                <div className="absolute inset-0 transition-transform duration-1000 group-hover/item:scale-105">
                  <Image
                    src={banner.imageUrl}
                    alt={banner.title}
                    fill
                    className="object-cover object-center"
                    priority={index === 0}
                    sizes="(max-width: 768px) 100vw, (max-width: 1200px) 80vw, 1200px"
                  />
                  {/* Gradient Overlay */}
                  <div className="absolute inset-0 bg-gradient-to-r from-black/80 via-black/40 to-transparent" />
                </div>

                {/* Content */}
                <div className="relative z-10 h-full flex flex-col justify-center p-5 sm:p-8 lg:p-10">
                  <div className="max-w-xl">
                    <span className="inline-block px-4 py-1.5 bg-bank-green/90 text-white text-xs font-bold tracking-[0.2em] rounded-full mb-4 backdrop-blur-md uppercase border border-white/20">
                      PROMO
                    </span>
                    <h3 className="text-2xl sm:text-4xl md:text-5xl font-bold text-white mb-3 leading-[1.1] tracking-tight uppercase">
                      {banner.title}
                    </h3>
                    <p className="text-sm sm:text-lg text-white/80 font-medium line-clamp-2 max-w-md leading-relaxed">
                      {banner.description}
                    </p>
                  </div>
                </div>
              </div>
            </div>
          ))}
        </Carousel>
        {/* Navigation - Premium styling */}
        {banners.length > 1 && (
          <div className="hidden sm:block">
            <Button type="text" shape="circle" icon={<ChevronLeft className="h-4 w-4" />} aria-label="Previous banner" onClick={(e) => { e.preventDefault(); carouselRef.current?.prev(); }} disabled={current <= 0} className="absolute left-2 top-1/2 z-10 -translate-y-1/2 inline-flex items-center justify-center gap-2 whitespace-nowrap rounded-xl text-xs font-bold uppercase tracking-[0.15em] transition-all focus-visible:outline-none focus-visible:ring-1 focus-visible:ring-ring disabled:pointer-events-none disabled:opacity-50 [&_svg]:pointer-events-none [&_svg]:size-4 [&_svg]:shrink-0 active:scale-95 cursor-pointer disabled:cursor-not-allowed border border-border bg-background shadow-sm hover:bg-muted hover:text-foreground hover:border-border h-10 w-10 p-0 left-6 h-12 w-12 bg-white/10 hover:bg-white/20 border-white/20 text-white backdrop-blur-md opacity-0 group-hover/carousel:opacity-100 transition-all duration-300">
              <span className="sr-only">Previous</span>
            </Button>
            <Button type="text" shape="circle" icon={<ChevronRight className="h-4 w-4" />} aria-label="Next banner" onClick={(e) => { e.preventDefault(); carouselRef.current?.next(); }} disabled={current >= banners.length - 1} className="absolute right-2 top-1/2 z-10 -translate-y-1/2 inline-flex items-center justify-center gap-2 whitespace-nowrap rounded-xl text-xs font-bold uppercase tracking-[0.15em] transition-all focus-visible:outline-none focus-visible:ring-1 focus:ring-ring disabled:pointer-events-none disabled:opacity-50 [&_svg]:pointer-events-none [&_svg]:size-4 [&_svg]:shrink-0 active:scale-95 cursor-pointer disabled:cursor-not-allowed border border-border bg-background shadow-sm hover:bg-muted hover:text-foreground hover:border-border h-10 w-10 p-0 right-6 h-12 w-12 bg-white/10 hover:bg-white/20 border-white/20 text-white backdrop-blur-md opacity-0 group-hover/carousel:opacity-100 transition-all duration-300">
              <span className="sr-only">Next</span>
            </Button>
          </div>
        )}
      </div>
      {banners.length > 1 && (
        <div className="mt-3 flex justify-center gap-2" role="group" aria-label="Banner pagination">
          {banners.map((banner, index) => (
            <Button
              key={banner.id}
              type="text"
              aria-label={`Go to banner ${index + 1}`}
              aria-current={index === current ? 'true' : undefined}
              onClick={() => api.scrollTo(index)}
              className="flex h-11 w-11 items-center justify-center cursor-pointer"
            >
              <span
                aria-hidden="true"
                className={clsx(
                  'h-2 rounded-full transition-all',
                  index === current ? 'w-8 bg-bank-green' : 'w-2 bg-muted-foreground/30'
                )}
              />
            </Button>
          ))}
        </div>
      )}
    </div>
  );
}
