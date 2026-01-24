'use client';

import React from 'react';
import { motion } from 'framer-motion';
import { Crown, Shield, Sparkles } from 'lucide-react';
import { useVIPStatus } from '@/hooks/useVIPStatus';
import clsx from 'clsx';

interface VIPBadgeProps {
  size?: 'sm' | 'md' | 'lg';
  showLabel?: boolean;
  showIcon?: boolean;
  variant?: 'badge' | 'card' | 'inline';
  className?: string;
}

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

  const sizeStyles = {
    sm: {
      badge: 'h-6 px-2 py-1 text-[10px]',
      card: 'p-3',
      icon: 'h-3 w-3',
    },
    md: {
      badge: 'h-8 px-3 py-1.5 text-xs',
      card: 'p-4',
      icon: 'h-4 w-4',
    },
    lg: {
      badge: 'h-10 px-4 py-2 text-sm',
      card: 'p-5',
      icon: 'h-5 w-5',
    },
  };

  const getIcon = () => {
    switch (tier) {
      case 'VIP':
        return Crown;
      case 'DIAMOND':
        return Sparkles;
      case 'PLATINUM':
        return Shield;
      default:
        return Crown;
    }
  };

  const Icon = getIcon();
  const currentSize = sizeStyles[size];

  if (variant === 'card') {
    return (
      <motion.div
        initial={{ opacity: 0, scale: 0.95 }}
        animate={{ opacity: 1, scale: 1 }}
        className={clsx(
          'bg-gradient-to-br from-amber-500/10 to-orange-600/10 border border-amber-500/20 rounded-xl',
          currentSize.card,
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
            <p className="text-[10px] text-muted-foreground font-bold tracking-widest">
              STATUS MEMBER
            </p>
            <p className="text-sm font-black text-foreground">
              {tierLabel}
            </p>
          </div>
        </div>
      </motion.div>
    );
  }

  if (variant === 'inline') {
    return (
      <motion.span
        initial={{ opacity: 0 }}
        animate={{ opacity: 1 }}
        className={clsx(
          'inline-flex items-center gap-1.5 font-black',
          className
        )}
        style={{ color: tierColor }}
      >
        {showIcon && <Icon className={currentSize.icon} />}
        {showLabel && (
          <span className={clsx(
            size === 'sm' ? 'text-[10px]' : size === 'md' ? 'text-xs' : 'text-sm'
          )}>
            {tierLabel}
          </span>
        )}
      </motion.span>
    );
  }

  return (
    <motion.div
      initial={{ opacity: 0, x: -10 }}
      animate={{ opacity: 1, x: 0 }}
      className={clsx(
        'inline-flex items-center gap-2 rounded-full bg-gradient-to-r from-amber-500 to-orange-600 shadow-lg shadow-amber-500/20',
        currentSize.badge,
        className
      )}
    >
      {showIcon && <Icon className={currentSize.icon} />}
      {showLabel && (
        <span className="font-bold tracking-wider text-white">
          {tierLabel}
        </span>
      )}
    </motion.div>
  );
}

interface VIPStatusIndicatorProps {
  showTier?: boolean;
  showBenefits?: boolean;
  className?: string;
}

export function VIPStatusIndicator({ showTier = true, showBenefits = false, className }: VIPStatusIndicatorProps) {
  const { isVIP, tier, tierLabel, benefits } = useVIPStatus();

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
