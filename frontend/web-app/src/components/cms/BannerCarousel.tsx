'use client';

import React, { useState, useEffect } from 'react';
import { ChevronLeft, ChevronRight } from 'lucide-react';
import { motion, AnimatePresence } from 'framer-motion';
import clsx from 'clsx';
import { Skeleton } from '@/components/ui/Skeleton';
import { useBanners } from '@/hooks';
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
  const { data: banners, isLoading, error } = useBanners({ segment, location, device });
  const [currentIndex, setCurrentIndex] = useState(0);
  const [direction, setDirection] = useState(0);

  // Auto-play functionality
  useEffect(() => {
    if (!banners || banners.length <= 1) return;

    const timer = setInterval(() => {
      setDirection(1);
      setCurrentIndex((prev) => (prev + 1) % banners.length);
    }, autoPlayInterval);

    return () => clearInterval(timer);
  }, [banners, autoPlayInterval]);

  const handlePrevious = () => {
    setDirection(-1);
    setCurrentIndex((prev) => (prev - 1 + banners!.length) % banners!.length);
  };

  const handleNext = () => {
    setDirection(1);
    setCurrentIndex((prev) => (prev + 1) % banners!.length);
  };

  const handleIndicatorClick = (index: number) => {
    setDirection(index > currentIndex ? 1 : -1);
    setCurrentIndex(index);
  };

  const handleBannerClick = (banner: Content) => {
    if (onBannerClick) {
      onBannerClick(banner);
    } else if (banner.actionUrl) {
      if (banner.actionType === 'LINK') {
        window.open(banner.actionUrl, '_blank', 'noopener,noreferrer');
      } else if (banner.actionType === 'DEEP_LINK') {
        window.location.href = banner.actionUrl;
      }
    }
  };

  // Loading state
  if (isLoading) {
    return (
      <div className={clsx('w-full', className)}>
        <Skeleton className="h-48 sm:h-64 md:h-80 rounded-2xl" />
      </div>
    );
  }

  // Error or empty state
  if (error || !banners || banners.length === 0) {
    return null;
  }

  const variants = {
    enter: (direction: number) => ({
      x: direction > 0 ? 1000 : -1000,
      opacity: 0,
    }),
    center: {
      zIndex: 1,
      x: 0,
      opacity: 1,
    },
    exit: (direction: number) => ({
      zIndex: 0,
      x: direction < 0 ? 1000 : -1000,
      opacity: 0,
    }),
  };

  const currentBanner = banners[currentIndex];

  return (
    <div className={clsx('relative w-full', className)}>
      {/* Carousel */}
      <div className="relative overflow-hidden rounded-2xl shadow-2xl shadow-bank-green/20 aspect-[2/1] sm:aspect-[2.5/1] md:aspect-[3/1]">
        <AnimatePresence initial={false} custom={direction}>
          <motion.div
            key={currentIndex}
            custom={direction}
            variants={variants}
            initial="enter"
            animate="center"
            exit="exit"
            transition={{
              x: { type: 'spring', stiffness: 300, damping: 30 },
              opacity: { duration: 0.2 },
            }}
            className="absolute inset-0 cursor-pointer group"
            onClick={() => handleBannerClick(currentBanner)}
          >
            {/* Background Image */}
            <div
              className="absolute inset-0 bg-cover bg-center transition-transform duration-700 group-hover:scale-105"
              style={{ backgroundImage: `url(${currentBanner.imageUrl})` }}
            >
              {/* Gradient Overlay */}
              <div className="absolute inset-0 bg-gradient-to-r from-black/70 via-black/40 to-transparent" />
            </div>

            {/* Content */}
            <div className="relative z-10 h-full flex flex-col justify-center p-6 sm:p-8 md:p-12">
              <motion.div
                initial={{ opacity: 0, y: 20 }}
                animate={{ opacity: 1, y: 0 }}
                transition={{ delay: 0.2 }}
                className="max-w-lg"
              >
                <span className="inline-block px-3 py-1 bg-bank-green/90 text-white text-xs font-bold tracking-wider rounded-full mb-3 backdrop-blur-sm">
                  PROMO
                </span>
                <h3 className="text-2xl sm:text-3xl md:text-4xl font-black text-white mb-2 leading-tight">
                  {currentBanner.title}
                </h3>
                <p className="text-sm sm:text-base text-white/90 font-medium line-clamp-2">
                  {currentBanner.description}
                </p>
              </motion.div>
            </div>
          </motion.div>
        </AnimatePresence>

        {/* Navigation Arrows */}
        {banners.length > 1 && (
          <>
            <button
              onClick={handlePrevious}
              className="absolute left-2 top-1/2 -translate-y-1/2 z-20 p-2 rounded-full bg-white/90 hover:bg-white text-foreground shadow-lg transition-all hover:scale-110 backdrop-blur-sm"
              aria-label="Previous banner"
            >
              <ChevronLeft className="h-5 w-5" />
            </button>
            <button
              onClick={handleNext}
              className="absolute right-2 top-1/2 -translate-y-1/2 z-20 p-2 rounded-full bg-white/90 hover:bg-white text-foreground shadow-lg transition-all hover:scale-110 backdrop-blur-sm"
              aria-label="Next banner"
            >
              <ChevronRight className="h-5 w-5" />
            </button>
          </>
        )}
      </div>

      {/* Indicators */}
      {banners.length > 1 && (
        <div className="flex justify-center gap-2 mt-4">
          {banners.map((_, index) => (
            <button
              key={index}
              onClick={() => handleIndicatorClick(index)}
              className={clsx(
                'h-2 rounded-full transition-all duration-300',
                index === currentIndex
                  ? 'w-8 bg-bank-green shadow-lg shadow-bank-green/50'
                  : 'w-2 bg-muted-foreground/30 hover:bg-muted-foreground/50'
              )}
              aria-label={`Go to banner ${index + 1}`}
            />
          ))}
        </div>
      )}
    </div>
  );
}
