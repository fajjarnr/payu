'use client';

import React, { useRef } from 'react';
import Image from 'next/image';
import Autoplay from 'embla-carousel-autoplay';
import { motion } from 'framer-motion';
import clsx from 'clsx';
import { Skeleton } from '@/components/ui/skeleton';
import { useBanners } from '@/hooks';
import { useRouter } from '@/lib/navigation';
import type { Content } from '@/services/CMSService';
import {
  Carousel,
  CarouselContent,
  CarouselItem,
  CarouselNext,
  CarouselPrevious,
} from '@/components/ui/carousel';

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
  const plugin = React.useRef(
    Autoplay({ delay: autoPlayInterval, stopOnInteraction: true })
  );
  // BUG-FE-011 FIX: Debounce navigation to prevent history flooding
  const isNavigating = useRef(false);

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
        <Skeleton className="w-full rounded-2xl aspect-[2.1/1] sm:aspect-[2.5/1] md:aspect-[3/1]" />
      </div>
    );
  }

  if (error || !banners || banners.length === 0) {
    return null;
  }

  return (
    <div className={clsx('relative w-full group/carousel', className)}>
      <Carousel
        plugins={[plugin.current]}
        className="w-full"
        onMouseEnter={plugin.current.stop}
        onMouseLeave={plugin.current.reset}
        opts={{
          align: "start",
          loop: true,
        }}
      >
        <CarouselContent className="-ml-0">
          {banners.map((banner, index) => (
            <CarouselItem key={banner.id} className="pl-0">
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
                <div className="relative z-10 h-full flex flex-col justify-center p-6 sm:p-10 md:p-16">
                  <motion.div
                    initial={{ opacity: 0, x: -20 }}
                    whileInView={{ opacity: 1, x: 0 }}
                    transition={{ delay: 0.2, duration: 0.5 }}
                    className="max-w-xl"
                  >
                    <span className="inline-block px-4 py-1.5 bg-bank-green/90 text-white text-xs font-bold tracking-[0.2em] rounded-full mb-4 backdrop-blur-md uppercase border border-white/20">
                      Exclusive Promo
                    </span>
                    <h3 className="text-2xl sm:text-4xl md:text-5xl font-bold text-white mb-3 leading-[1.1] tracking-tight uppercase">
                      {banner.title}
                    </h3>
                    <p className="text-sm sm:text-lg text-white/80 font-medium line-clamp-2 max-w-md leading-relaxed">
                      {banner.description}
                    </p>
                  </motion.div>
                </div>
              </div>
            </CarouselItem>
          ))}
        </CarouselContent>
        
        {/* Navigation - Premium styling */}
        <div className="hidden sm:block">
          <CarouselPrevious className="left-6 h-12 w-12 bg-white/10 hover:bg-white/20 border-white/20 text-white backdrop-blur-md opacity-0 group-hover/carousel:opacity-100 transition-all duration-300" />
          <CarouselNext className="right-6 h-12 w-12 bg-white/10 hover:bg-white/20 border-white/20 text-white backdrop-blur-md opacity-0 group-hover/carousel:opacity-100 transition-all duration-300" />
        </div>
      </Carousel>
    </div>
  );
}
