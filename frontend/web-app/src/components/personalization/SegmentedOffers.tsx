'use client';

import React from 'react';
import { motion } from 'framer-motion';
import { Gift, Percent, Coins, Zap, Ticket, ChevronRight } from 'lucide-react';
import { useSegmentedOffers } from '@/hooks/useSegmentedOffers';
import { useAuthStore } from '@/stores';
import { Skeleton } from '@/components/ui/Skeleton';
import clsx from 'clsx';
import type { SegmentedOffer } from '@/services/SegmentationService';

interface SegmentedOffersProps {
  className?: string;
  maxOffers?: number;
}

const OFFER_ICONS = {
  CASHBACK: Coins,
  DISCOUNT: Percent,
  REWARD_POINTS: Gift,
  FREE_TRANSFER: Zap,
  BONUS_INTEREST: Ticket,
};

const OFFER_STYLES = {
  CASHBACK: 'from-amber-500 to-orange-600',
  DISCOUNT: 'from-blue-500 to-indigo-600',
  REWARD_POINTS: 'from-purple-500 to-pink-600',
  FREE_TRANSFER: 'from-green-500 to-emerald-600',
  BONUS_INTEREST: 'from-cyan-500 to-teal-600',
};

export default function SegmentedOffers({ className, maxOffers = 3 }: SegmentedOffersProps) {
  const user = useAuthStore((state) => state.user);
  const { offers, isLoading, error } = useSegmentedOffers(user?.id, 0, maxOffers);

  if (isLoading) {
    return (
      <div className={clsx('space-y-4', className)}>
        <Skeleton className="h-48 rounded-xl" />
      </div>
    );
  }

  if (error || !offers || offers.length === 0) {
    return null;
  }

  return (
    <div className={clsx('space-y-4', className)}>
      <div className="flex items-center justify-between">
        <div>
          <h3 className="text-lg font-black text-foreground">Penawaran Spesial Untuk Anda</h3>
          <p className="text-xs text-muted-foreground font-medium tracking-wider">
            Berdasarkan status akun Anda
          </p>
        </div>
        <span className="text-xs px-3 py-1.5 rounded-full bg-primary/10 text-primary font-bold">
          {offers.length} Tersedia
        </span>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
        {offers.slice(0, maxOffers).map((offer, index) => (
          <OfferCard key={offer.id} offer={offer} index={index} />
        ))}
      </div>
    </div>
  );
}

interface OfferCardProps {
  offer: SegmentedOffer;
  index: number;
}

function OfferCard({ offer, index }: OfferCardProps) {
  const Icon = OFFER_ICONS[offer.offerType] || Gift;
  const gradientStyle = OFFER_STYLES[offer.offerType] || OFFER_STYLES.CASHBACK;

  const formatValue = () => {
    if (offer.percentage) {
      return `${offer.percentage}%`;
    }
    if (offer.currency && offer.value) {
      return `${offer.currency} ${offer.value.toLocaleString('id-ID')}`;
    }
    if (offer.value) {
      return offer.value.toLocaleString('id-ID');
    }
    return 'Special';
  };

  const isValid = new Date(offer.validUntil) > new Date();

  return (
    <motion.div
      initial={{ opacity: 0, y: 20 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ delay: index * 0.1 }}
      className="group relative"
    >
      <div className={clsx(
        'absolute inset-0 bg-gradient-to-br rounded-2xl opacity-0 group-hover:opacity-100 transition-opacity duration-300',
        gradientStyle
      )} />

      <div className="relative bg-card border border-border rounded-2xl p-6 h-full group-hover:shadow-xl transition-all duration-300">
        <div className="flex items-start justify-between mb-4">
          <div className={clsx(
            'h-12 w-12 rounded-xl bg-gradient-to-br flex items-center justify-center shadow-lg',
            gradientStyle
          )}>
            <Icon className="h-6 w-6 text-white" />
          </div>

          {offer.promoCode && (
            <span className="text-[10px] px-2 py-1 rounded-md bg-muted font-mono font-bold tracking-wider">
              {offer.promoCode}
            </span>
          )}
        </div>

        <h4 className="text-base font-bold text-foreground mb-2 line-clamp-2">
          {offer.title}
        </h4>

        <p className="text-sm text-muted-foreground mb-4 line-clamp-2 min-h-[40px]">
          {offer.description}
        </p>

        <div className="flex items-center justify-between pt-4 border-t border-border">
          <div>
            <p className="text-[10px] text-muted-foreground font-medium tracking-wider">
              {offer.offerType.replace('_', ' ')}
            </p>
            <p className="text-lg font-black text-primary">
              {formatValue()}
            </p>
          </div>

          <motion.button
            whileHover={{ scale: 1.05 }}
            whileTap={{ scale: 0.95 }}
            disabled={!isValid}
            className={clsx(
              'h-10 w-10 rounded-full flex items-center justify-center transition-all',
              isValid
                ? 'bg-primary text-primary-foreground shadow-lg shadow-primary/20'
                : 'bg-muted text-muted-foreground cursor-not-allowed'
            )}
          >
            <ChevronRight className="h-5 w-5" />
          </motion.button>
        </div>

        {offer.minTransaction && (
          <p className="text-[10px] text-muted-foreground mt-3 font-medium">
            Min. transaksi: Rp {offer.minTransaction.toLocaleString('id-ID')}
          </p>
        )}

        <div className="mt-3 flex items-center gap-1.5">
          <div className={clsx(
            'h-1.5 flex-1 rounded-full bg-muted overflow-hidden'
          )}>
            <div
              className={clsx(
                'h-full rounded-full bg-gradient-to-r',
                gradientStyle
              )}
              style={{
                width: `${Math.max(0, Math.min(100, (
                  (new Date(offer.validUntil).getTime() - new Date().getTime()) /
                  (new Date(offer.validUntil).getTime() - new Date(offer.validFrom).getTime())
                ) * 100))}%`
              }}
            />
          </div>
          <span className={clsx(
            'text-[10px] font-bold',
            isValid ? 'text-primary' : 'text-destructive'
          )}>
            {isValid ? 'Berlaku' : 'Kedaluwarsa'}
          </span>
        </div>
      </div>
    </motion.div>
  );
}
