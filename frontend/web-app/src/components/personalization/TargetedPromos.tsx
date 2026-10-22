'use client';

import React from 'react';
import { Tag as TagIcon, TrendingUp, Star, ArrowRight } from '@/components/icons';
import { useSegmentedOffers } from '@/hooks/useSegmentedOffers';
import { useLocale, useTranslations } from 'next-intl';
import { useAuthStore } from '@/stores';
import { Alert, Button, Card, Skeleton, Space, Tag, Typography, theme } from 'antd';
import { formatCurrency } from '@/lib/currency';
import type { SegmentedOffer } from '@/services/SegmentationService';
import { INK_ON_EMERALD } from '@/theme/tokens';
import { notify } from '@/lib/notify';

interface TargetedPromosProps {
  /** Localized message for a failed load (R-06). */
  promosErrorMessage?: string;
  offerType?: SegmentedOffer['offerType'];
  className?: string;
  maxPromos?: number;
}

export default function TargetedPromos({ offerType, className, maxPromos = 2, promosErrorMessage }: TargetedPromosProps) {
  const t = useTranslations('promos');
  const user = useAuthStore((state) => state.user);
  const { offers, isLoading, error } = useSegmentedOffers(user?.id);
  const { token } = theme.useToken();

  const filteredOffers = React.useMemo(() => {
    const filtered = offerType
      ? offers.filter(offer => offer.offerType === offerType)
      : offers;

    return filtered
      .filter(offer => new Date(offer.validUntil) > new Date())
      .sort((a, b) => new Date(a.validUntil).getTime() - new Date(b.validUntil).getTime())
      .slice(0, maxPromos);
  }, [offers, offerType, maxPromos]);

  if (isLoading) {
    return (
      <div className={className}>
        <Skeleton title={false} paragraph={false} active style={{ height: 128, borderRadius: 12 }} />
      </div>
    );
  }

  // R-06: a failed segmentation query is not the same as "no offers" — it
  // must not vanish silently.
  if (error) {
    return (
      <Alert
        type="error"
        showIcon
        className={className}
        message={promosErrorMessage ?? t('loadError')}
      />
    );
  }

  if (filteredOffers.length === 0) {
    return null;
  }

  return (
    <Space orientation="vertical" size={12} style={{ width: '100%' }} className={className}>
      <Space size={8}>
        <TagIcon style={{ height: 16, width: 16, color: token.colorPrimary }} aria-hidden="true" />
        <Typography.Title level={5} style={{ margin: 0 }}>
          {t("forYou")}
        </Typography.Title>
      </Space>

      <Space orientation="vertical" size={12} style={{ width: '100%' }}>
        {filteredOffers.map((promo) => (
          <PromoItem key={promo.id} promo={promo} />
        ))}
      </Space>
    </Space>
  );
}

interface PromoItemProps {
  promo: SegmentedOffer;
}

function PromoItem({ promo }: PromoItemProps) {
  const { token } = theme.useToken();
  const t = useTranslations('promos');
  const locale = useLocale();
  const bcp47Locale = locale === 'id' ? 'id-ID' : 'en-US';
  const daysLeft = Math.ceil((new Date(promo.validUntil).getTime() - new Date().getTime()) / (1000 * 60 * 60 * 24));
  const isExpiringSoon = daysLeft <= 3;

  const getValueDisplay = () => {
    if (promo.percentage) {
      return t("savePercent", { percent: promo.percentage });
    }
    if (promo.currency) {
      return formatCurrency(promo.value, { symbol: promo.currency, locale: bcp47Locale });
    }
    return t("specialOffer");
  };

  return (
    <Card size="small" style={{ borderRadius: 12 }}>
      <div style={{ display: 'flex', alignItems: 'flex-start', gap: 12 }}>
        <div
          style={{
            height: 40,
            width: 40,
            borderRadius: 8,
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            flexShrink: 0,
            backgroundColor: isExpiringSoon ? token.colorWarningBg : token.colorPrimaryBg,
          }}
        >
          <Star
            style={{
              height: 20,
              width: 20,
              color: isExpiringSoon ? token.colorWarningText : token.colorPrimaryText,
            }}
            aria-hidden="true"
          />
        </div>

        <div style={{ flex: 1, minWidth: 0 }}>
          <div style={{ display: 'flex', alignItems: 'flex-start', justifyContent: 'space-between', gap: 8, marginBottom: 4 }}>
            <Typography.Text strong ellipsis style={{ fontSize: 14 }}>
              {promo.title}
            </Typography.Text>
            {promo.promoCode && (
              <Tag style={{ fontFamily: 'monospace', fontWeight: 700, flexShrink: 0, marginInlineEnd: 0 }}>
                {promo.promoCode}
              </Tag>
            )}
          </div>

          <Typography.Paragraph type="secondary" ellipsis={{ rows: 1 }} style={{ fontSize: 12, marginBottom: 8 }}>
            {promo.description}
          </Typography.Paragraph>

          <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
            <div>
              <Typography.Text type="secondary" style={{ fontSize: 12, display: 'block' }}>
                {promo.offerType.replace('_', ' ').toLowerCase()}
              </Typography.Text>
              <Typography.Text strong style={{ fontSize: 14, color: token.colorPrimaryText }}>
                {getValueDisplay()}
              </Typography.Text>
            </div>

            <Typography.Text
              strong
              style={{
                fontSize: 12,
                color: isExpiringSoon ? token.colorWarningText : token.colorTextSecondary,
              }}
            >
              {isExpiringSoon
                ? t("endsInDays", { days: daysLeft })
                : t("daysLeft", { days: daysLeft })}
            </Typography.Text>
          </div>

          {promo.minTransaction && (
            <Typography.Text type="secondary" style={{ fontSize: 12, display: 'block', marginTop: 8 }}>
              {t("minTransaction", {
                amount: formatCurrency(promo.minTransaction, { locale: bcp47Locale }),
              })}
            </Typography.Text>
          )}
        </div>
      </div>
    </Card>
  );
}

interface QuickPromoBannerProps {
  className?: string;
}

export function QuickPromoBanner({ className }: QuickPromoBannerProps) {
  const user = useAuthStore((state) => state.user);
  const { cashbackOffers, isLoading, error } = useSegmentedOffers(user?.id);
  const { token } = theme.useToken();
  const t = useTranslations('promos');
  const topPromo = cashbackOffers[0];

  // R-06: a failed cashback fetch is not "no promo" — say so.
  if (error) {
    return (
      <Alert type="error" showIcon className={className} message={t("loadError")} />
    );
  }

  if (isLoading || !topPromo) {
    return null;
  }

  return (
    <Card
      className={className}
      style={{
        borderRadius: 12,
        background: `linear-gradient(to right, ${token.colorPrimary}, ${token.colorPrimaryActive})`,
        border: 'none',
      }}
      styles={{ body: { padding: 16 } }}
    >
      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', gap: 16 }}>
        <div style={{ flex: 1, minWidth: 0 }}>
          <Space size={8} style={{ marginBottom: 4 }}>
            <TrendingUp style={{ height: 16, width: 16, color: INK_ON_EMERALD }} aria-hidden="true" />
            <Typography.Text strong style={{ fontSize: 12, letterSpacing: '0.05em', color: INK_ON_EMERALD }}>
              {t("specialPromo")}
            </Typography.Text>
          </Space>
          <Typography.Title level={5} style={{ margin: '0 0 4px', color: INK_ON_EMERALD }} ellipsis>
            {topPromo.title}
          </Typography.Title>
          <Typography.Paragraph style={{ fontSize: 12, margin: 0, color: INK_ON_EMERALD }} ellipsis={{ rows: 1 }}>
            {topPromo.description}
          </Typography.Paragraph>
        </div>

        <Space size={8} style={{ flexShrink: 0 }}>
          {/* A-04: the claim affordance copies the promo code — a styled text
              that does nothing is a dead control. */}
          <Button
            type="text"
            size="small"
            data-testid="quick-promo-claim"
            onClick={async () => {
              if (!topPromo.promoCode) return;
              try {
                await navigator.clipboard.writeText(topPromo.promoCode);
                notify.success(t('codeCopied'));
              } catch {
                notify.error(t('copyFailed'));
              }
            }}
            style={{ color: INK_ON_EMERALD, fontWeight: 700, fontSize: 12, letterSpacing: '0.05em' }}
          >
            {t("claim")}
          </Button>
          <ArrowRight style={{ height: 16, width: 16, color: INK_ON_EMERALD }} aria-hidden="true" />
        </Space>
      </div>
    </Card>
  );
}
