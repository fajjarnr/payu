'use client';

import React from 'react';
import { Badge, Typography, theme } from 'antd';
import { Crown, Shield, Sparkles, type IconType } from '@/components/icons';
import { useVIPStatus } from '@/hooks/useVIPStatus';
import clsx from 'clsx';
import { ON_PRIMARY } from '@/theme/tokens';

interface VIPBadgeProps {
  size?: 'sm' | 'md' | 'lg';
  showLabel?: boolean;
  showIcon?: boolean;
  variant?: 'badge' | 'card' | 'inline';
  className?: string;
}

// Move icon mapping outside component to avoid creating components during render
const TIER_ICONS: Record<string, IconType> = {
  VIP: Crown,
  DIAMOND: Sparkles,
  PLATINUM: Shield,
};

export default function VIPBadge({
  size = 'md',
  showLabel = true,
  showIcon = true,
  variant = 'badge',
  className,
}: VIPBadgeProps) {
  const { isVIP, tier, tierLabel, tierColor } = useVIPStatus();
  const { token } = theme.useToken();

  // Absence is the empty state: non-VIP users see nothing (no skeleton,
  // no placeholder badge), so the layout collapses cleanly without a gap.
  if (!isVIP || !tier) {
    return null;
  }

  const Icon = TIER_ICONS[tier] || Crown;

  if (variant === 'card') {
    return (
      <div
        className={clsx('rounded-3xl p-4 border', className)}
        style={{
          background: `linear-gradient(135deg, ${token.colorWarningBg}, ${token.colorErrorBg})`,
          borderColor: token.colorWarningBorder,
        }}
      >
        <div className="flex items-center gap-3">
          <div className="relative">
            <div
              className="h-10 w-10 rounded-full flex items-center justify-center"
              style={{ background: `linear-gradient(135deg, ${token.colorWarning}, ${token.colorError})` }}
            >
              <Icon className="h-5 w-5" style={{ color: ON_PRIMARY }} aria-hidden="true" />
            </div>
            <div
              className="absolute -top-1 -right-1 h-3 w-3 rounded-full border-2 animate-pulse"
              style={{ backgroundColor: token.colorSuccess, borderColor: token.colorBgContainer }}
            />
          </div>

          <div>
            <Typography.Text type="secondary" style={{ fontSize: 12, fontWeight: 600, letterSpacing: '0.1em' }}>
              STATUS MEMBER
            </Typography.Text>
            <p className="text-sm font-semibold" style={{ color: token.colorText }}>
              {tierLabel}
            </p>
          </div>
        </div>
      </div>
    );
  }

  if (variant === 'inline') {
    return (
      <span
        className={clsx(
          'inline-flex items-center gap-1.5 font-semibold',
          className
        )}
        style={{ color: tierColor }}
      >
        {showIcon && <Icon className="h-4 w-4" />}
        {showLabel && (
          <span className={clsx(
            size === 'sm' ? 'text-xs' : size === 'md' ? 'text-xs' : 'text-sm'
          )}>
            {tierLabel}
          </span>
        )}
      </span>
    );
  }

  return (
    <Badge
      className={clsx(
        'border-none rounded-full font-semibold tracking-widest uppercase py-1',
        size === 'sm' ? 'px-2 h-6 text-xs' : size === 'md' ? 'px-3 h-8 text-xs' : 'px-4 h-10 text-xs',
        className
      )}
      style={{ background: `linear-gradient(90deg, ${token.colorWarning}, ${token.colorError})`, color: ON_PRIMARY }}
    >
      <div className="flex items-center gap-2">
        {showIcon && <Icon className={size === 'sm' ? 'h-3 w-3' : 'h-4 w-4'} aria-hidden="true" />}
        {showLabel && <span>{tierLabel}</span>}
      </div>
    </Badge>
  );
}

interface VIPStatusIndicatorProps {
  showTier?: boolean;
  showBenefits?: boolean;
  className?: string;
}

export function VIPStatusIndicator({ showTier = true, showBenefits = false, className }: VIPStatusIndicatorProps) {
  const { isVIP, benefits } = useVIPStatus();

  if (!isVIP) {
    return null;
  }

  return (
    <div className={clsx('space-y-3', className)}>
      {showTier && (
        <div className="flex items-center gap-3">
          <VIPBadge size="lg" variant="card" />
        </div>
      )}

      {showBenefits && benefits.length > 0 && (
        <div className="space-y-2">
          <Typography.Text type="secondary" style={{ fontSize: 12, fontWeight: 600, letterSpacing: '0.05em' }}>
            BENEFIT EKSKLUSIF
          </Typography.Text>
          <ul className="space-y-1.5">
            {benefits.slice(0, 3).map((benefit, index) => (
              <li
                key={index}
                className="text-xs font-medium flex items-start gap-2"
                style={{ color: 'var(--ant-color-text)' }}
              >
                <div className="h-1.5 w-1.5 rounded-full mt-1.5 shrink-0" style={{ backgroundColor: 'var(--ant-color-primary)' }} aria-hidden="true" />
                {benefit}
              </li>
            ))}
          </ul>
        </div>
      )}
    </div>
  );
}
