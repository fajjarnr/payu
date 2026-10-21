"use client";

import React from "react";
import { Button, Card, Progress, Skeleton, Space, Tag, Typography, theme } from "antd";
import {
  Gift,
  Percent,
  Coins,
  Zap,
  Ticket,
  ChevronRight,
} from "@/components/icons";
import { useSegmentedOffers } from "@/hooks/useSegmentedOffers";
import { useAuthStore } from "@/stores";
import clsx from "clsx";
import type { SegmentedOffer } from "@/services/SegmentationService";

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


export default function SegmentedOffers({
  className,
  maxOffers = 3,
}: SegmentedOffersProps) {
  const user = useAuthStore((state) => state.user);
  const { token } = theme.useToken();
  const { offers, isLoading, error } = useSegmentedOffers(
    user?.id,
    0,
    maxOffers,
  );

  if (isLoading) {
    return (
      <div className={clsx("space-y-4", className)}>
        <Skeleton
          title={false}
          paragraph={false}
          active
          style={{ height: 192, borderRadius: 12 }}
        />
      </div>
    );
  }

  if (error || !offers || offers.length === 0) {
    return null;
  }

  return (
    <Space direction="vertical" size={16} style={{ width: "100%" }} className={className}>
      <div style={{ display: "flex", alignItems: "center", justifyContent: "space-between" }}>
        <div>
          <Typography.Title level={4} style={{ margin: 0 }}>
            Penawaran Spesial Untuk Anda
          </Typography.Title>
          <Typography.Text type="secondary" style={{ fontSize: 12 }}>
            Berdasarkan status akun Anda
          </Typography.Text>
        </div>
        <Tag color="success" style={{ marginInlineEnd: 0 }}>
          {offers.length} Tersedia
        </Tag>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
        {offers.slice(0, maxOffers).map((offer) => (
          <OfferCard key={offer.id} offer={offer} />
        ))}
      </div>
    </Space>
  );
}

interface OfferCardProps {
  offer: SegmentedOffer;
}

function OfferCard({ offer }: OfferCardProps) {
  const { token } = theme.useToken();
  const Icon = OFFER_ICONS[offer.offerType] || Gift;

  const formatValue = () => {
    if (offer.percentage) {
      return `${offer.percentage}%`;
    }
    if (offer.currency && offer.value) {
      return `${offer.currency} ${offer.value.toLocaleString("id-ID")}`;
    }
    if (offer.value) {
      return offer.value.toLocaleString("id-ID");
    }
    return "Special";
  };

  const isValid = new Date(offer.validUntil) > new Date();
  const validityPercent = Math.max(
    0,
    Math.min(
      100,
      ((new Date(offer.validUntil).getTime() - new Date().getTime()) /
        (new Date(offer.validUntil).getTime() -
          new Date(offer.validFrom).getTime())) *
        100,
    ),
  );
  return (
    <Card style={{ borderRadius: 16, height: "100%" }}>
      <Space direction="vertical" size={12} style={{ width: "100%" }}>
        <div style={{ display: "flex", alignItems: "flex-start", justifyContent: "space-between" }}>
          <div
            style={{
              height: 48,
              width: 48,
              borderRadius: 12,
              backgroundColor: token.colorPrimaryBg,
              display: "flex",
              alignItems: "center",
              justifyContent: "center",
            }}
          >
            <Icon style={{ height: 24, width: 24, color: token.colorPrimaryText }} aria-hidden="true" />
          </div>

          {offer.promoCode && (
            <Tag style={{ fontFamily: "monospace", fontWeight: 700, marginInlineEnd: 0 }}>
              {offer.promoCode}
            </Tag>
          )}
        </div>

        <Typography.Title level={5} style={{ margin: 0 }} ellipsis={{ rows: 2 }}>
          {offer.title}
        </Typography.Title>

        <Typography.Paragraph type="secondary" style={{ margin: 0, minHeight: 40 }} ellipsis={{ rows: 2 }}>
          {offer.description}
        </Typography.Paragraph>

        <div
          style={{
            display: "flex",
            alignItems: "center",
            justifyContent: "space-between",
            paddingTop: 16,
            borderTop: `1px solid ${token.colorBorderSecondary}`,
          }}
        >
          <div>
            <Typography.Text type="secondary" style={{ fontSize: 12, display: "block" }}>
              {offer.offerType.replace("_", " ")}
            </Typography.Text>
            <Typography.Text strong style={{ fontSize: 18, color: token.colorPrimaryText }}>
              {formatValue()}
            </Typography.Text>
          </div>

          <Button
            type="primary"
            shape="circle"
            disabled={!isValid}
            aria-label="Lihat selengkapnya"
            icon={<ChevronRight style={{ height: 20, width: 20 }} aria-hidden="true" />}
            style={{ minHeight: 44, minWidth: 44 }}
          />
        </div>

        {offer.minTransaction && (
          <Typography.Text type="secondary" style={{ fontSize: 12 }}>
            Min. transaksi: Rp {offer.minTransaction.toLocaleString("id-ID")}
          </Typography.Text>
        )}

        <div style={{ display: "flex", alignItems: "center", gap: 6 }}>
          <Progress
            percent={validityPercent}
            showInfo={false}
            strokeColor={token.colorPrimary}
            trailColor={token.colorFillSecondary}
            style={{ flex: 1 }}
          />
          <Typography.Text
            strong
            style={{
              fontSize: 12,
              color: isValid ? token.colorPrimaryText : token.colorErrorText,
            }}
          >
            {isValid ? "Berlaku" : "Kedaluwarsa"}
          </Typography.Text>
        </div>
      </Space>
    </Card>
  );
}
