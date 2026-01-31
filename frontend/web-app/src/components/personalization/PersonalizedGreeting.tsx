'use client';

import React from 'react';
import { motion } from 'framer-motion';
import { Progress } from '@/components/ui/progress';
import { Sun, Moon, Sparkles, Crown } from 'lucide-react';
import { useUserSegment } from '@/hooks/useUserSegment';
import { useAuthStore } from '@/stores';
import clsx from 'clsx';
import { useTranslations } from 'next-intl';

interface PersonalizedGreetingProps {
  showTimeBased?: boolean;
  showSegment?: boolean;
  className?: string;
}

export default function PersonalizedGreeting({
  showTimeBased = true,
  showSegment = true,
  className,
}: PersonalizedGreetingProps) {
  const t = useTranslations('dashboard');
  const user = useAuthStore((state) => state.user);
  const { currentTier, isVIP } = useUserSegment(user?.id);

  const getTimeBasedGreeting = () => {
    const hour = new Date().getHours();
    if (hour < 12) return { text: t('welcome_morning'), icon: Sun };
    if (hour < 15) return { text: t('welcome_afternoon'), icon: Sun };
    if (hour < 18) return { text: t('welcome_evening'), icon: Sun };
    return { text: t('welcome_night'), icon: Moon };
  };

  const timeGreeting = getTimeBasedGreeting();
  const TimeIcon = timeGreeting.icon;

  const getSegmentGreeting = () => {
    if (!currentTier) return '';
    switch (currentTier) {
      case 'VIP': return t('tier_vip');
      case 'DIAMOND': return t('tier_diamond');
      case 'PLATINUM': return t('tier_platinum');
      case 'GOLD': return t('tier_gold');
      case 'SILVER': return t('tier_silver');
      case 'BRONZE': return t('tier_bronze');
      default: return '';
    }
  };

  const segmentGreeting = getSegmentGreeting();

  return (
    <div className={clsx('space-y-1', className)}>
      <motion.div
        initial={{ opacity: 0, y: -10 }}
        animate={{ opacity: 1, y: 0 }}
        className="flex items-center gap-2"
      >
        {showTimeBased && (
          <>
            <TimeIcon className="h-4 w-4 text-emerald-500" />
            <span className="text-xs font-black uppercase tracking-[0.2em] text-muted-foreground">
              {timeGreeting.text}
            </span>
          </>
        )}
      </motion.div>

      <motion.h1
        initial={{ opacity: 0, y: -10 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ delay: 0.1 }}
        className="flex items-center gap-3 flex-wrap"
      >
        <span className="text-2xl font-black text-foreground uppercase tracking-tighter">
          {user?.fullName?.split(' ')[0] || 'User'}!
        </span>

        {showSegment && isVIP && (
          <motion.span
            initial={{ opacity: 0, scale: 0.8 }}
            animate={{ opacity: 1, scale: 1 }}
            transition={{ delay: 0.2, type: 'spring' }}
            className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full bg-gradient-to-r from-amber-500 to-orange-600 shadow-lg shadow-amber-500/20"
          >
            <Crown className="h-3.5 w-3.5 text-white" />
            <span className="text-[10px] font-bold tracking-[0.15em] text-white uppercase">
              {segmentGreeting}
            </span>
          </motion.span>
        )}
      </motion.h1>
    </div>
  );
}

interface PersonalizedWelcomeBannerProps {
  className?: string;
}

export function PersonalizedWelcomeBanner({ className }: PersonalizedWelcomeBannerProps) {
  const user = useAuthStore((state) => state.user);
  const { currentTier, isVIP, progressToNext, nextTier } = useUserSegment(user?.id);

  const getWelcomeMessage = () => {
    if (isVIP) {
      return {
        title: `Welcome back, ${user?.fullName?.split(' ')[0] || 'VIP Member'}!`,
        subtitle: 'Enjoy your exclusive benefits and premium services',
        gradient: 'from-amber-500 to-orange-600',
      };
    }
    if (currentTier === 'GOLD') {
      return {
        title: `Hello, ${user?.fullName?.split(' ')[0] || 'Gold Member'}!`,
        subtitle: 'You are enjoying premium benefits',
        gradient: 'from-yellow-500 to-amber-600',
      };
    }
    return {
      title: `Welcome, ${user?.fullName?.split(' ')[0] || 'Pengguna'}!`,
      subtitle: progressToNext && nextTier
        ? `You're ${progressToNext.toFixed(0)}% away from ${nextTier} status!`
        : 'Discover personalized offers for you',
      gradient: 'from-primary to-emerald-600',
    };
  };

  const welcome = getWelcomeMessage();

  return (
    <motion.div
      initial={{ opacity: 0, y: 20 }}
      animate={{ opacity: 1, y: 0 }}
      className={clsx(
        'bg-gradient-to-br rounded-2xl p-6 text-white relative overflow-hidden',
        welcome.gradient,
        className
      )}
    >
      {/* Decorative elements */}
      <div className="absolute top-0 right-0 w-48 h-48 bg-white/10 rounded-full blur-3xl -translate-y-1/2 translate-x-1/2" />
      <div className="absolute bottom-0 left-0 w-32 h-32 bg-white/5 rounded-full blur-2xl translate-y-1/2 -translate-x-1/2" />

      <div className="relative z-10">
        <div className="flex items-center gap-2 mb-2">
          <Sparkles className="h-4 w-4" />
          <p className="text-[10px] font-bold tracking-widest opacity-80">
            PERSONALIZED EXPERIENCE
          </p>
        </div>

        <h2 className="text-xl sm:text-2xl font-black mb-2">
          {welcome.title}
        </h2>

        <p className="text-sm opacity-90 mb-4 max-w-xl">
          {welcome.subtitle}
        </p>

        {progressToNext && nextTier && !isVIP && (
          <div className="max-w-md">
            <div className="flex items-center justify-between mb-2">
              <p className="text-[10px] font-bold opacity-80">
                Progress to {nextTier}
              </p>
              <p className="text-[10px] font-bold">
                {progressToNext.toFixed(0)}%
              </p>
            </div>
            <Progress 
              value={progressToNext} 
              className="h-2 bg-white/20" 
              indicatorClassName="bg-white"
            />
          </div>
        )}
      </div>
    </motion.div>
  );
}
