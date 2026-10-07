'use client';

import { Progress, Typography } from 'antd';
import { Sun, Moon, Sparkles, Crown } from '@/components/icons';
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
      <div
        className="flex items-center gap-2"
      >
        {showTimeBased && (
          <>
            <TimeIcon className="h-4 w-4 text-primary" />
            <Typography.Text className="text-xs font-bold uppercase tracking-[0.2em] text-muted-foreground">
              {timeGreeting.text}
            </Typography.Text>
          </>
        )}
      </div>

      <div
        className="flex items-center gap-3 flex-wrap"
      >
        <Typography.Title
          level={1}
          style={{ margin: 0 }}
          className="text-2xl font-bold text-foreground uppercase tracking-tighter"
        >
          {user?.fullName?.split(' ')[0] || 'User'}!
        </Typography.Title>

        {showSegment && isVIP && (
          <span
            className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full bg-gradient-to-r from-warning to-accent shadow-lg shadow-warning/20"
          >
            <Crown className="h-3.5 w-3.5 text-surface" />
            <span className="text-xs font-bold tracking-[0.15em] text-surface uppercase">
              {segmentGreeting}
            </span>
          </span>
        )}
      </div>
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
        gradient: 'from-warning to-accent',
      };
    }
    if (currentTier === 'GOLD') {
      return {
        title: `Hello, ${user?.fullName?.split(' ')[0] || 'Gold Member'}!`,
        subtitle: 'You are enjoying premium benefits',
        gradient: 'from-warning to-warning',
      };
    }
    return {
      title: `Welcome, ${user?.fullName?.split(' ')[0] || 'Pengguna'}!`,
      subtitle: progressToNext && nextTier
        ? `You're ${progressToNext.toFixed(0)}% away from ${nextTier} status!`
        : 'Discover personalized offers for you',
      gradient: 'from-primary to-primary-dark',
    };
  };

  const welcome = getWelcomeMessage();

  return (
    <div
      className={clsx(
        'bg-gradient-to-br rounded-2xl p-6 text-surface relative overflow-hidden',
        welcome.gradient,
        className
      )}
    >
      {/* Decorative elements */}
      <div className="absolute top-0 right-0 w-48 h-48 bg-surface/10 rounded-full blur-3xl -translate-y-1/2 translate-x-1/2" />
      <div className="absolute bottom-0 left-0 w-32 h-32 bg-surface/5 rounded-full blur-2xl translate-y-1/2 -translate-x-1/2" />

      <div className="relative z-10">
        <div className="flex items-center gap-2 mb-2">
          <Sparkles className="h-4 w-4" />
          <Typography.Text className="text-xs font-bold tracking-widest opacity-80 text-surface">
            PERSONALIZED EXPERIENCE
          </Typography.Text>
        </div>

        <Typography.Title
          level={2}
          className="text-xl sm:text-2xl font-bold text-surface"
          style={{ margin: '0 0 8px' }}
        >
          {welcome.title}
        </Typography.Title>

        <Typography.Paragraph
          className="text-sm opacity-90 max-w-xl text-surface"
          style={{ marginBottom: 16 }}
        >
          {welcome.subtitle}
        </Typography.Paragraph>

        {progressToNext && nextTier && !isVIP && (
          <div className="max-w-md">
            <div className="flex items-center justify-between mb-2">
              <Typography.Text className="text-xs font-bold opacity-80 text-surface">
                Progress to {nextTier}
              </Typography.Text>
              <Typography.Text className="text-xs font-bold text-surface">
                {progressToNext.toFixed(0)}%
              </Typography.Text>
            </div>
            <Progress
              percent={progressToNext}
              showInfo={false}
              strokeColor="#ffffff"
              railColor="rgba(255,255,255,0.2)"
              size={['100%', 8]}
            />
          </div>
        )}
      </div>
    </div>
  );
}
