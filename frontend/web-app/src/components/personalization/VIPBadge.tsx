'use client';

import React from 'react';
import { Badge } from 'antd';
import { Crown, Shield, Sparkles, type IconType } from '@/components/icons';
import { useVIPStatus } from '@/hooks/useVIPStatus';
import clsx from 'clsx';

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

  if (!isVIP || !tier) {
    return null;
  }

  const Icon = TIER_ICONS[tier] || Crown;

  if (variant === 'card') {
    return (
      <div
        className={clsx(
          'bg-gradient-to-br from-amber-500/10 to-orange-600/10 border border-amber-500/20 rounded-xl p-4',
          className
        )}
      >
        <div className="flex items-center gap-3">
          <div className="relative">
            <div className="h-10 w-10 rounded-full bg-gradient-to-br from-amber-500 to-orange-600 flex items-center justify-center">
              <Icon className="h-5 w-5 text-white" />
            </div>
            <div className="absolute -top-1 -right-1 h-3 w-3 bg-green-500 rounded-full border-2 border-card animate-pulse" />
          </div>

          <div>
            <p className="text-xs text-muted-foreground font-bold tracking-widest">
              STATUS MEMBER
            </p>
            <p className="text-sm font-bold text-foreground">
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
          'inline-flex items-center gap-1.5 font-bold',
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
        'bg-gradient-to-r from-amber-500 to-orange-600 hover:from-amber-600 hover:to-orange-700 text-white border-none shadow-lg shadow-amber-500/20 font-bold tracking-widest uppercase py-1',
        size === 'sm' ? 'px-2 h-6 text-xs' : size === 'md' ? 'px-3 h-8 text-xs' : 'px-4 h-10 text-xs',
        className
      )}
    >
      <div className="flex items-center gap-2">
        {showIcon && <Icon className={size === 'sm' ? 'h-3 w-3' : 'h-4 w-4'} />}
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
          <p className="text-xs font-bold text-muted-foreground tracking-wider">
            BENEFIT EKSKLUSIF
          </p>
          <ul className="space-y-1.5">
            {benefits.slice(0, 3).map((benefit, index) => (
              <li
                key={index}
                className="text-xs text-foreground font-medium flex items-start gap-2"
              >
                <div className="h-1.5 w-1.5 rounded-full bg-primary mt-1.5 shrink-0" />
                {benefit}
              </li>
            ))}
          </ul>
        </div>
      )}
    </div>
  );
}
