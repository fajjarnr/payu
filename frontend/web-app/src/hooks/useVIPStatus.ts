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

const TIER_CONFIG: Record<SegmentTier, { label: string; color: string }> = {
  BRONZE: { label: 'Bronze', color: '#cd7f32' },
  SILVER: { label: 'Silver', color: '#c0c0c0' },
  GOLD: { label: 'Gold', color: '#ffd700' },
  PLATINUM: { label: 'Platinum', color: '#e5e4e2' },
  DIAMOND: { label: 'Diamond', color: '#b9f2ff' },
  VIP: { label: 'VIP', color: '#10b981' },
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
  const { currentTier, currentMembership, isVIP } = useUserSegment(user?.id);

  const tierConfig = useMemo(() => {
    if (!currentTier) return { label: 'Standard', color: '#6b7280' };
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
