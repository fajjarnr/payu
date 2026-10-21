'use client';

import { Progress, Typography, theme } from 'antd';
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
  const { token } = theme.useToken();
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
            <TimeIcon className="h-4 w-4" style={{ color: token.colorTextSecondary }} aria-hidden="true" />
            <Typography.Text type="secondary" style={{ fontSize: 12, fontWeight: 700, textTransform: 'uppercase', letterSpacing: '0.1em' }}>
              {timeGreeting.text}
            </Typography.Text>
          </>
        )}
      </div>

      <div
        className="flex items-center gap-3 flex-wrap"
      >
        <div
          className="text-2xl font-bold tracking-tight"
          style={{ color: token.colorText }}
          role="text"
          aria-label={`${user?.fullName?.split(' ')[0] || 'User'}!`}
        >
          {user?.fullName?.split(' ')[0] || 'User'}!
        </div>

        {showSegment && isVIP && (
          <span
            className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full shadow-lg"
            style={{ background: `linear-gradient(135deg, ${token.colorWarning}, ${token.colorError})`, color: '#06281F' }}
          >
            <Crown className="h-3.5 w-3.5" aria-hidden="true" />
            <span className="text-xs font-bold tracking-[0.1em] uppercase">
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
  const { token } = theme.useToken();
  const user = useAuthStore((state) => state.user);
  const { currentTier, isVIP, progressToNext, nextTier } = useUserSegment(user?.id);
  const getWelcomeMessage = () => {
    if (isVIP) {
      return {
        title: `Welcome back, ${user?.fullName?.split(' ')[0] || 'VIP Member'}!`,
        subtitle: 'Enjoy your exclusive benefits and premium services',
        gradient: `linear-gradient(135deg, ${token.colorWarning}, ${token.colorError})`,
      };
    }
    if (currentTier === 'GOLD') {
      return {
        title: `Hello, ${user?.fullName?.split(' ')[0] || 'Gold Member'}!`,
        subtitle: 'You are enjoying premium benefits',
        gradient: `linear-gradient(135deg, ${token.colorWarning}, ${token.colorWarningActive})`,
      };
    }
    return {
      title: `Welcome, ${user?.fullName?.split(' ')[0] || 'Pengguna'}!`,
      subtitle: progressToNext && nextTier
        ? `You're ${progressToNext.toFixed(0)}% away from ${nextTier} status!`
        : 'Discover personalized offers for you',
      gradient: `linear-gradient(135deg, ${token.colorPrimary}, ${token.colorPrimaryActive})`,
    };
  };

  const welcome = getWelcomeMessage();

  return (
    <div
      className={clsx('rounded-2xl p-6 relative overflow-hidden', className)}
      style={{ background: welcome.gradient, color: token.colorWhite }}
    >
      {/* Decorative elements */}
      <div className="absolute top-0 right-0 w-48 h-48 rounded-full blur-3xl -translate-y-1/2 translate-x-1/2" style={{ backgroundColor: 'rgba(255,255,255,0.12)' }} aria-hidden="true" />
      <div className="absolute bottom-0 left-0 w-32 h-32 rounded-full blur-2xl translate-y-1/2 -translate-x-1/2" style={{ backgroundColor: 'rgba(255,255,255,0.08)' }} aria-hidden="true" />

      <div className="relative z-10">
        <div className="flex items-center gap-2 mb-2">
          <Sparkles className="h-4 w-4" />
          <Typography.Text style={{ fontSize: 12, fontWeight: 700, letterSpacing: '0.1em', color: token.colorWhite }}>
            PERSONALIZED EXPERIENCE
          </Typography.Text>
        </div>

        <Typography.Title
          level={2}
          className="text-xl sm:text-2xl font-bold"
          style={{ margin: '0 0 8px', color: token.colorWhite }}
        >
          {welcome.title}
        </Typography.Title>

        <Typography.Paragraph
          style={{ marginBottom: 16, fontSize: 14, maxWidth: 576, color: token.colorWhite }}
        >
          {welcome.subtitle}
        </Typography.Paragraph>

        {progressToNext && nextTier && !isVIP && (
          <div className="max-w-md">
            <div className="flex items-center justify-between mb-2">
              <Typography.Text style={{ fontSize: 12, fontWeight: 700, color: token.colorWhite }}>
                Progress to {nextTier}
              </Typography.Text>
              <Typography.Text style={{ fontSize: 12, fontWeight: 700, color: token.colorWhite }}>
                {progressToNext.toFixed(0)}%
              </Typography.Text>
            </div>
            <Progress
              percent={progressToNext}
              showInfo={false}
              strokeColor={token.colorBgElevated}
              trailColor={token.colorFillSecondary}
              size={['100%', 8]}
            />
          </div>
        )}
      </div>
    </div>
  );
}
