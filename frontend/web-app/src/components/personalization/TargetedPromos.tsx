'use client';

import React from 'react';
import { motion } from 'framer-motion';
import { Tag, TrendingUp, Star, ArrowRight } from 'lucide-react';
import { useSegmentedOffers } from '@/hooks/useSegmentedOffers';
import { useAuthStore } from '@/stores';
import { Skeleton } from '@/components/ui/Skeleton';
import clsx from 'clsx';
import type { SegmentedOffer } from '@/services/SegmentationService';

interface TargetedPromosProps {
  offerType?: SegmentedOffer['offerType'];
  className?: string;
  maxPromos?: number;
}

export default function TargetedPromos({ offerType, className, maxPromos = 2 }: TargetedPromosProps) {
  const user = useAuthStore((state) => state.user);
  const { offers, isLoading, error, cashbackOffers, discountOffers, rewardOffers } = useSegmentedOffers(user?.id);

  const filteredOffers = React.useMemo(() => {
    let filtered = offerType
      ? offers.filter(offer => offer.offerType === offerType)
      : offers;

    return filtered
      .filter(offer => new Date(offer.validUntil) > new Date())
      .sort((a, b) => new Date(a.validUntil).getTime() - new Date(b.validUntil).getTime())
      .slice(0, maxPromos);
  }, [offers, offerType, maxPromos]);

  if (isLoading) {
    return (
      <div className={clsx('space-y-3', className)}>
        <Skeleton className="h-32 rounded-xl" />
      </div>
    );
  }

  if (error || filteredOffers.length === 0) {
    return null;
  }

  return (
    <div className={clsx('space-y-3', className)}>
      <div className="flex items-center gap-2 mb-4">
        <Tag className="h-4 w-4 text-primary" />
        <h3 className="text-sm font-bold text-foreground">Promo Untuk Anda</h3>
      </div>

      <div className="space-y-3">
        {filteredOffers.map((promo, index) => (
          <PromoItem key={promo.id} promo={promo} index={index} />
        ))}
      </div>
    </div>
  );
}

interface PromoItemProps {
  promo: SegmentedOffer;
  index: number;
}

function PromoItem({ promo, index }: PromoItemProps) {
  const daysLeft = Math.ceil((new Date(promo.validUntil).getTime() - new Date().getTime()) / (1000 * 60 * 60 * 24));
  const isExpiringSoon = daysLeft <= 3;

  const getValueDisplay = () => {
    if (promo.percentage) {
      return `Hemat ${promo.percentage}%`;
    }
    if (promo.currency) {
      return `${promo.currency} ${promo.value.toLocaleString('id-ID')}`;
    }
    return 'Special Offer';
  };

  return (
    <motion.div
      initial={{ opacity: 0, x: -20 }}
      animate={{ opacity: 1, x: 0 }}
      transition={{ delay: index * 0.1 }}
      className="group relative overflow-hidden"
    >
      <div className="absolute inset-0 bg-gradient-to-r from-primary/5 to-transparent opacity-0 group-hover:opacity-100 transition-opacity duration-300" />

      <div className="relative bg-card border border-border rounded-xl p-4 hover:border-primary/30 transition-all duration-300">
        <div className="flex items-start gap-3">
          <div className={clsx(
            'h-10 w-10 rounded-lg flex items-center justify-center shrink-0',
            isExpiringSoon ? 'bg-orange-500/10' : 'bg-primary/10'
          )}>
            <Star className={clsx(
              'h-5 w-5',
              isExpiringSoon ? 'text-orange-500' : 'text-primary'
            )} />
          </div>

          <div className="flex-1 min-w-0">
            <div className="flex items-start justify-between gap-2 mb-1">
              <h4 className="text-sm font-bold text-foreground truncate">
                {promo.title}
              </h4>
              {promo.promoCode && (
                <span className="text-[10px] px-2 py-0.5 rounded-md bg-muted font-mono font-bold shrink-0">
                  {promo.promoCode}
                </span>
              )}
            </div>

            <p className="text-xs text-muted-foreground line-clamp-1 mb-2">
              {promo.description}
            </p>

            <div className="flex items-center justify-between">
              <div>
                <p className="text-[10px] text-muted-foreground font-medium">
                  {promo.offerType.replace('_', ' ').toLowerCase()}
                </p>
                <p className="text-sm font-black text-primary">
                  {getValueDisplay()}
                </p>
              </div>

              <div className="text-right">
                <p className={clsx(
                  'text-[10px] font-bold',
                  isExpiringSoon ? 'text-orange-500' : 'text-muted-foreground'
                )}>
                  {isExpiringSoon ? `Berakhir dalam ${daysLeft} hari` : `${daysLeft} hari lagi`}
                </p>
              </div>
            </div>

            {promo.minTransaction && (
              <p className="text-[10px] text-muted-foreground mt-2 font-medium">
                Min. transaksi: Rp {promo.minTransaction.toLocaleString('id-ID')}
              </p>
            )}
          </div>
        </div>
      </div>
    </motion.div>
  );
}

interface QuickPromoBannerProps {
  className?: string;
}

export function QuickPromoBanner({ className }: QuickPromoBannerProps) {
  const user = useAuthStore((state) => state.user);
  const { cashbackOffers, isLoading } = useSegmentedOffers(user?.id);

  const topPromo = cashbackOffers[0];

  if (isLoading || !topPromo) {
    return null;
  }

  return (
    <motion.div
      initial={{ opacity: 0, y: -10 }}
      animate={{ opacity: 1, y: 0 }}
      className={clsx(
        'bg-gradient-to-r from-primary to-emerald-600 rounded-xl p-4 text-white relative overflow-hidden',
        className
      )}
    >
      <div className="absolute top-0 right-0 w-32 h-32 bg-white/10 rounded-full blur-2xl -translate-y-1/2 translate-x-1/2" />

      <div className="relative z-10 flex items-center justify-between gap-4">
        <div className="flex-1">
          <div className="flex items-center gap-2 mb-1">
            <TrendingUp className="h-4 w-4" />
            <p className="text-[10px] font-bold tracking-wider opacity-80">
              PROMO SPESIAL
            </p>
          </div>
          <h3 className="text-base font-black mb-1">{topPromo.title}</h3>
          <p className="text-xs opacity-80 line-clamp-1">{topPromo.description}</p>
        </div>

        <motion.button
          whileHover={{ scale: 1.05 }}
          whileTap={{ scale: 0.95 }}
          className="h-10 px-4 bg-white text-primary rounded-lg font-bold text-xs tracking-wider flex items-center gap-2 shadow-lg"
        >
          Klaim
          <ArrowRight className="h-4 w-4" />
        </motion.button>
      </div>
    </motion.div>
  );
}
