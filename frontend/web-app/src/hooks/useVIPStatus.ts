'use client';

import { useMemo } from 'react';
import { useUserSegment } from './useUserSegment';
import { useAuthStore } from '@/stores';
import type { SegmentTier } from '@/services/SegmentationService';

export interface VIPStatus {
  isVIP: boolean;
  tier: SegmentTier | null;
  tierLabel: string;
  tierColor: string;
  benefits: string[];
  hasVIPAccess: boolean;
  prioritySupport: boolean;
  exclusiveOffers: boolean;
  higherLimits: boolean;
  feeWaivers: boolean;
}

// Tier colors are CSS variables (globals.css, per theme) because VIPBadge
// renders tierColor as text and icon color on the card surface, which must
// pass 4.5:1 in both light and dark. Guard: tier-colors.test.ts.
const TIER_CONFIG: Record<SegmentTier, { label: string; color: string }> = {
  BRONZE: { label: 'Bronze', color: 'var(--tier-bronze)' },
  SILVER: { label: 'Silver', color: 'var(--tier-silver)' },
  GOLD: { label: 'Gold', color: 'var(--tier-gold)' },
  PLATINUM: { label: 'Platinum', color: 'var(--tier-platinum)' },
  DIAMOND: { label: 'Diamond', color: 'var(--tier-diamond)' },
  VIP: { label: 'VIP', color: 'var(--tier-vip)' },
};

const VIP_BENEFITS = {
  VIP: [
    'Prioritas layanan pelanggan 24/7',
    'Bebas biaya transfer ke semua bank',
    'Limit transaksi tanpa batas',
    'Cashback khusus hingga 5%',
    'Akses eksklusif ke fitur investasi premium',
    'Personal relationship manager',
    'Invitation ke acara eksklusif',
  ],
  DIAMOND: [
    'Layanan pelanggan prioritas',
    'Bebas biaya transfer BI-FAST',
    'Limit transaksi tinggi',
    'Cashback hingga 3%',
    'Akses ke fitur investasi prioritas',
  ],
  PLATINUM: [
    'Layanan pelanggan prioritas',
    'Bebas biaya 50 transfer BI-FAST per bulan',
    'Limit transaksi meningkat',
    'Cashback hingga 2%',
  ],
  GOLD: [
    'Bebas biaya 20 transfer BI-FAST per bulan',
    'Cashback hingga 1.5%',
  ],
  SILVER: [
    'Bebas biaya 10 transfer BI-FAST per bulan',
    'Cashback hingga 1%',
  ],
  BRONZE: [
    'Cashback hingga 0.5%',
  ],
};

export const useVIPStatus = (): VIPStatus => {
  const user = useAuthStore((state) => state.user);
  const { currentTier, isVIP } = useUserSegment(user?.id);

  const tierConfig = useMemo(() => {
    if (!currentTier) return { label: 'Standard', color: 'var(--tier-standard)' };
    return TIER_CONFIG[currentTier] || TIER_CONFIG.BRONZE;
  }, [currentTier]);

  const benefits = useMemo(() => {
    if (!currentTier) return [];
    return VIP_BENEFITS[currentTier] || [];
  }, [currentTier]);

  return {
    isVIP,
    tier: currentTier || null,
    tierLabel: tierConfig.label,
    tierColor: tierConfig.color,
    benefits,
    hasVIPAccess: isVIP,
    prioritySupport: isVIP || currentTier === 'PLATINUM' || currentTier === 'DIAMOND',
    exclusiveOffers: isVIP || currentTier === 'DIAMOND',
    higherLimits: currentTier === 'GOLD' || currentTier === 'PLATINUM' || currentTier === 'DIAMOND' || currentTier === 'VIP',
    feeWaivers: isVIP || currentTier === 'DIAMOND' || currentTier === 'PLATINUM',
  };
};

export default useVIPStatus;
