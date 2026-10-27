"use client";

import React from "react";
import { useTranslations } from "next-intl";
import type { CSSProperties } from "react";
import DashboardLayout from "@/components/DashboardLayout";
import { SkipLink } from "@/lib/a11y";
import { notify as toast } from "@/lib/notify";
import {
  Gift,
  Coins,
  DollarSign,
  Share2,
  TrendingUp,
  Copy,
  ArrowRight,
  Trophy,
  CheckCircle,
  Award,
  Calendar,
  Zap,
  History,
  Clock,
} from "@/components/icons";
import {
  Button,
  Card,
  Col,
  List,
  Row,
  Space,
  Tabs,
  Tag,
  Typography,
} from "antd";
import {
  useLoyaltyBalance,
  useCashbacks,
  useReferralSummary,
  useActivePromotions,
} from "@/hooks";
import {
  addCurrency,
  asMoney,
  formatCurrency,
  type Money,
} from "@/lib/currency";
import { useAuthStore } from "@/stores/authStore";
import { ON_PRIMARY, SAGE_ON_INK } from "@/theme/tokens";
import type {
  LoyaltyBalanceResponse,
  Cashback,
  ReferralSummaryResponse,
  Promotion,
} from "@/services/PromotionService";
const labelStyle: CSSProperties = {
  fontSize: 12,
  fontWeight: 600,
  color: "var(--ant-color-text-secondary)",
  textTransform: "uppercase",
  letterSpacing: "0.1em",
};

const lightLabelStyle: CSSProperties = {
  ...labelStyle,
  color: "rgba(255, 255, 255, 0.6)",
};

// Labels on the lime gradient cards: white is 1.47:1 there, ink is 13.05:1.
const onLimeLabelStyle: CSSProperties = {
  ...labelStyle,
  color: ON_PRIMARY,
};

const iconBoxStyle = (
  size: number,
  backgroundColor: string,
): CSSProperties => ({
  width: size,
  height: size,
  borderRadius: 24,
  backgroundColor,
  display: "flex",
  alignItems: "center",
  justifyContent: "center",
  flexShrink: 0,
});

export default function RewardsPage() {
  const t = useTranslations("rewards");
  const { accountId } = useAuthStore();
  const acctId = accountId ?? "";
  const { data: loyaltyData } = useLoyaltyBalance(acctId);
  const { data: cashbackData } = useCashbacks(acctId);
  const { data: referralData } = useReferralSummary(acctId);
  const { data: promotionsData } = useActivePromotions();

  // BUG-FE-023: Use real data with skeleton/empty state instead of hardcoded fake data
  const loyaltyBalance = loyaltyData as LoyaltyBalanceResponse | undefined;
  const loyaltyStats = {
    totalEarned: loyaltyBalance?.totalEarned ?? 0,
    totalRedeemed: loyaltyBalance?.totalRedeemed ?? 0,
    currentBalance: loyaltyBalance?.currentBalance ?? 0,
    pointsExpiring: loyaltyBalance?.pointsExpiring ?? 0,
    expiryDate: loyaltyBalance?.expiryDate ?? "-",
  };

  const recentPoints: Array<{
    id: number;
    type: string;
    points: number;
    description: string;
    date: string;
  }> = [];

  const cashbackList = (cashbackData ?? []) as Cashback[];
  const cashbackHistory = cashbackList.map((cb) => ({
    id: cb.id,
    merchant: cb.merchantName ?? "",
    amount: cb.amount,
    status: cb.status.toLowerCase(),
    date: cb.createdAt,
    description: cb.referenceId,
  }));

  const cashbackCredited = cashbackHistory
    .filter((cb) => cb.status === "credited")
    .reduce((sum, cb) => addCurrency(sum, cb.amount), asMoney("0"));
  const cashbackPending = cashbackHistory
    .filter((cb) => cb.status === "pending")
    .reduce((sum, cb) => addCurrency(sum, cb.amount), asMoney("0"));
  const cashbackTotal = addCurrency(cashbackCredited, cashbackPending);

  const referralSummary = referralData as ReferralSummaryResponse | undefined;
  const referralStats = {
    code: referralSummary?.referralCode ?? "-",
    totalReferrals: referralSummary?.totalReferrals ?? 0,
    completedReferrals: referralSummary?.completedReferrals ?? 0,
    pendingReferrals: referralSummary?.pendingReferrals ?? 0,
    rewardPerReferral: 0,
    totalEarnings: referralSummary?.totalEarnings ?? 0,
  };

  const referralLink = `https://payu.fajjjar.my.id/onboarding?ref=${referralStats.code}`;

  const handleCopyReferralCode = async () => {
    try {
      await navigator.clipboard.writeText(referralStats.code);
      toast.success(t("referralCodeCopied"));
    } catch {
      toast.error(t("referralCodeCopyFailed"));
    }
  };

  const handleShareReferral = async () => {
    try {
      if (typeof navigator !== "undefined" && navigator.share) {
        await navigator.share({
          title: "PayU",
          text: t("shareReferralText"),
          url: referralLink,
        });
        return;
      }
      await navigator.clipboard.writeText(referralLink);
      toast.success(t("referralLinkCopied"));
    } catch {
      // User dismissed the share sheet; nothing to report.
    }
  };

  const promotionsList = (promotionsData ?? []) as Promotion[];
  const activePromotions = promotionsList.map((p) => ({
    id: p.id,
    name: p.name,
    description: p.description,
    type: p.type,
    value: String(p.value),
    endDate: p.endDate,
  }));

  const earnMethods: Array<{
    Icon: typeof Zap;
    title: string;
    description: string;
  }> = [
    {
      Icon: Zap,
      title: t("earnRoutineTitle"),
      description: t("earnRoutineDescription"),
    },
    {
      Icon: Calendar,
      title: t("earnDailyTitle"),
      description: t("earnDailyDescription"),
    },
    {
      Icon: Share2,
      title: t("earnReferralTitle"),
      description: t("earnReferralDescription"),
    },
    {
      Icon: Award,
      title: t("earnEventTitle"),
      description: t("earnEventDescription"),
    },
  ];

  const referralSummaryRows: Array<{
    Icon: typeof CheckCircle;
    iconColor: string;
    iconBg: string;
    title: string;
    description: string;
  }> = [
    {
      Icon: CheckCircle,
      iconColor: "var(--ant-color-primary-text)",
      iconBg: "var(--ant-color-primary-bg)",
      title: t("joinedSuccessfully"),
      description: t("joinedDescription", {
        count: referralStats.completedReferrals,
      }),
    },
    {
      Icon: Clock,
      iconColor: "var(--ant-color-warning-text)",
      iconBg: "var(--ant-color-warning-bg)",
      title: t("pendingTitle"),
      description: t("pendingDescription", {
        count: referralStats.pendingReferrals,
      }),
    },
    {
      Icon: Trophy,
      iconColor: "var(--ant-color-success-text)",
      iconBg: "var(--ant-color-success-bg)",
      title: t("earningsTitle"),
      description: t("earningsDescription", {
        points: referralStats.totalEarnings.toLocaleString(),
      }),
    },
  ];

  return (
    <DashboardLayout>
      <SkipLink />
      <Space orientation="vertical" size={24} style={{ width: "100%" }}>
        <Space orientation="vertical" size={4}>
          <Typography.Title level={1} style={{ margin: 0 }}>
            {t("title")}
          </Typography.Title>
          <Typography.Text type="secondary">{t("subtitle")}</Typography.Text>
        </Space>
        <Tabs
          defaultActiveKey="points"
          items={[
            {
              key: "points",
              // A plain label keeps antd's tab markup accessible: with a
              // custom node the role="tab" lands on a nested element and the
              // tablist has no direct tab children (axe aria-required-children).
              label: t("pointsTab"),
              children: (
                <Space orientation="vertical" size={24} style={{ width: "100%" }}>
                  <Row gutter={[24, 24]}>
                    <Col xs={24} lg={12}>
                      <Card
                        style={{
                          background:
                            "linear-gradient(135deg, var(--ant-color-primary), var(--ant-color-primary-active))",
                          border: "none",
                          position: "relative",
                          overflow: "hidden",
                        }}
                        styles={{ body: { padding: 24 } }}
                      >
                        <Space
                          orientation="vertical"
                          size={24}
                          style={{
                            width: "100%",
                            position: "relative",
                            zIndex: 1,
                          }}
                        >
                          <Space size={12} align="start">
                            <div
                              style={iconBoxStyle(
                                48,
                                "rgba(14, 15, 12, 0.08)",
                              )}
                            >
                              <Coins
                                style={{
                                  width: 24,
                                  height: 24,
                                  color: ON_PRIMARY,
                                }}
                              />
                            </div>
                            <Space orientation="vertical" size={0}>
                              <Typography.Text
                                strong
                                style={{ ...onLimeLabelStyle, display: "block" }}
                              >
                                {t("pointsBalance")}
                              </Typography.Text>
                              <Typography.Title
                                level={2}
                                style={{
                                  margin: 0,
                                  color: ON_PRIMARY,
                                }}
                              >
                                {loyaltyStats.currentBalance.toLocaleString()}
                              </Typography.Title>
                            </Space>
                          </Space>
                          <Typography.Text
                            style={{
                              color: ON_PRIMARY,
                              fontSize: 14,
                            }}
                          >
                            {t("redeemPointsHint")}
                          </Typography.Text>
                          <Row gutter={24}>
                            <Col span={12}>
                              <Typography.Text
                                strong
                                style={{
                                  ...onLimeLabelStyle,
                                  display: "block",
                                  marginBottom: 4,
                                }}
                              >
                                {t("totalEarned")}
                              </Typography.Text>
                              <Typography.Title
                                level={2}
                                style={{
                                  margin: 0,
                                  color: ON_PRIMARY,
                                }}
                              >
                                {loyaltyStats.totalEarned.toLocaleString()}
                              </Typography.Title>
                            </Col>
                            <Col span={12}>
                              <Typography.Text
                                strong
                                style={{
                                  ...onLimeLabelStyle,
                                  display: "block",
                                  marginBottom: 4,
                                }}
                              >
                                {t("totalRedeemed")}
                              </Typography.Text>
                              <Typography.Title
                                level={2}
                                style={{
                                  margin: 0,
                                  color: ON_PRIMARY,
                                }}
                              >
                                {loyaltyStats.totalRedeemed.toLocaleString()}
                              </Typography.Title>
                            </Col>
                          </Row>
                          {loyaltyStats.pointsExpiring > 0 && (
                            <Card
                              size="small"
                              style={{
                                backgroundColor: "rgba(14, 15, 12, 0.06)",
                                borderColor: "rgba(14, 15, 12, 0.12)",
                              }}
                              styles={{ body: { padding: 16 } }}
                            >
                              <Space size={12} align="start">
                                <Calendar
                                  style={{
                                    width: 20,
                                    height: 20,
                                    color: ON_PRIMARY,
                                  }}
                                />
                                <Space orientation="vertical" size={0}>
                                  <Typography.Text
                                    strong
                                    style={{
                                      ...onLimeLabelStyle,
                                      display: "block",
                                    }}
                                  >
                                    {t("pointsExpiring")}
                                  </Typography.Text>
                                  <Typography.Text
                                    strong
                                    style={{
                                      color: ON_PRIMARY,
                                    }}
                                  >
                                    {t("pointsExpiringValue", {
                                      points:
                                        loyaltyStats.pointsExpiring.toLocaleString(),
                                      date: loyaltyStats.expiryDate,
                                    })}
                                  </Typography.Text>
                                </Space>
                              </Space>
                            </Card>
                          )}
                        </Space>
                        <Trophy
                          style={{
                            position: "absolute",
                            bottom: -40,
                            right: -40,
                            width: 192,
                            height: 192,
                            opacity: 0.08,
                            transform: "rotate(-12deg)",
                            color: ON_PRIMARY,
                          }}
                        />
                      </Card>
                    </Col>

                    <Col xs={24} lg={12}>
                      <Card style={{ height: "100%" }}>
                        <Typography.Title
                          level={2}
                          style={{ marginTop: 0, marginBottom: 24 }}
                        >
                          {t("howToEarnPoints")}
                        </Typography.Title>
                        <Space
                          orientation="vertical"
                          size={24}
                          style={{ width: "100%" }}
                        >
                          {earnMethods.map(({ Icon, title, description }) => (
                            <Space key={title} size={16} align="start">
                              <div
                                style={iconBoxStyle(
                                  40,
                                  "var(--ant-color-primary-bg)",
                                )}
                              >
                                <Icon
                                  style={{
                                    width: 20,
                                    height: 20,
                                    color: "var(--ant-color-primary-text)",
                                  }}
                                />
                              </div>
                              <Space orientation="vertical" size={0}>
                                <Typography.Text strong>
                                  {title}
                                </Typography.Text>
                                <Typography.Text
                                  type="secondary"
                                  style={{ fontSize: 14 }}
                                >
                                  {description}
                                </Typography.Text>
                              </Space>
                            </Space>
                          ))}
                        </Space>
                      </Card>
                    </Col>
                  </Row>

                  <Space
                    orientation="vertical"
                    size={16}
                    style={{ width: "100%" }}
                  >
                    <Typography.Title level={2} style={{ margin: 0 }}>
                      {t("history")}
                    </Typography.Title>
                    <Card styles={{ body: { padding: 0 } }}>
                      <List
                        dataSource={recentPoints}
                        renderItem={(point) => (
                          <List.Item style={{ padding: 24 }}>
                            <Row
                              justify="space-between"
                              align="middle"
                              style={{ width: "100%" }}
                            >
                              <Space size={16}>
                                <div
                                  style={iconBoxStyle(
                                    48,
                                    point.type === "EARNED"
                                      ? "var(--ant-color-success-bg)"
                                      : "var(--ant-color-warning-bg)",
                                  )}
                                >
                                  {point.type === "EARNED" ? (
                                    <TrendingUp
                                      style={{
                                        width: 24,
                                        height: 24,
                                        color:
                                          "var(--ant-color-success-text)",
                                      }}
                                    />
                                  ) : (
                                    <History
                                      style={{
                                        width: 24,
                                        height: 24,
                                        color:
                                          "var(--ant-color-warning-text)",
                                      }}
                                    />
                                  )}
                                </div>
                                <Space orientation="vertical" size={0}>
                                  <Typography.Text strong>
                                    {point.description}
                                  </Typography.Text>
                                  <Typography.Text strong style={labelStyle}>
                                    {point.date}
                                  </Typography.Text>
                                </Space>
                              </Space>
                              <Typography.Text
                                strong
                                style={{
                                  fontSize: 18,
                                  color:
                                    point.type === "EARNED"
                                      ? "var(--ant-color-success-text)"
                                      : "var(--ant-color-warning-text)",
                                }}
                              >
                                {point.type === "EARNED" ? "+" : ""}
                                {point.points}
                              </Typography.Text>
                            </Row>
                          </List.Item>
                        )}
                      />
                    </Card>
                  </Space>
                </Space>
              ),
            },
            {
              key: "cashback",
              label: t("cashback"),
              children: (
                <Space orientation="vertical" size={24} style={{ width: "100%" }}>
                  <Row gutter={[24, 24]}>
                    <Col xs={24} lg={8}>
                      <Card
                        style={{
                          background:
                            "linear-gradient(135deg, var(--ant-color-text), var(--ant-color-text-secondary))",
                          border: "none",
                          height: "100%",
                        }}
                        styles={{ body: { padding: 24 } }}
                      >
                        <Space
                          orientation="vertical"
                          size={24}
                          style={{ width: "100%" }}
                        >
                          <Space size={12} align="start">
                            <div
                              style={iconBoxStyle(
                                56,
                                "rgba(232, 235, 230, 0.12)",
                              )}
                            >
                              <DollarSign
                                style={{
                                  width: 28,
                                  height: 28,
                                  color: SAGE_ON_INK,
                                }}
                              />
                            </div>
                            <Space orientation="vertical" size={0}>
                              <Typography.Text
                                strong
                                style={{ ...lightLabelStyle, display: "block" }}
                              >
                                {t("totalCashback")}
                              </Typography.Text>
                              <Typography.Title
                                level={2}
                                style={{
                                  margin: 0,
                                  color: SAGE_ON_INK,
                                }}
                              >
                                {formatCurrency(cashbackTotal)}
                              </Typography.Title>
                            </Space>
                          </Space>
                          <Space
                            orientation="vertical"
                            size={0}
                            style={{ width: "100%" }}
                          >
                            <Row
                              justify="space-between"
                              align="middle"
                              style={{
                                padding: "12px 0",
                                borderBottom: "1px solid rgba(232, 235, 230, 0.14)",
                              }}
                            >
                              <Typography.Text
                                style={{
                                  fontSize: 14,
                                  color: SAGE_ON_INK,
                                  opacity: 0.72,
                                }}
                              >
                                {t("credited")}
                              </Typography.Text>
                              <Typography.Text
                                strong
                                style={{
                                  color: SAGE_ON_INK,
                                }}
                              >
                                {formatCurrency(cashbackCredited)}
                              </Typography.Text>
                            </Row>
                            <Row
                              justify="space-between"
                              align="middle"
                              style={{
                                padding: "12px 0",
                                borderBottom: "1px solid rgba(232, 235, 230, 0.14)",
                              }}
                            >
                              <Typography.Text
                                style={{
                                  fontSize: 14,
                                  color: SAGE_ON_INK,
                                  opacity: 0.72,
                                }}
                              >
                                {t("pending")}
                              </Typography.Text>
                              <Typography.Text
                                strong
                                style={{
                                  color: SAGE_ON_INK,
                                }}
                              >
                                {formatCurrency(cashbackPending)}
                              </Typography.Text>
                            </Row>
                            <Row
                              justify="space-between"
                              align="middle"
                              style={{ paddingTop: 12 }}
                            >
                              <Typography.Text
                                style={{
                                  fontSize: 14,
                                  color: SAGE_ON_INK,
                                  opacity: 0.72,
                                }}
                              >
                                {t("expired")}
                              </Typography.Text>
                              <Typography.Text
                                strong
                                style={{
                                  color: SAGE_ON_INK,
                                }}
                              >
                                {formatCurrency(asMoney("0"))}
                              </Typography.Text>
                            </Row>
                          </Space>
                        </Space>
                      </Card>
                    </Col>

                    <Col xs={24} lg={16}>
                      <Card style={{ height: "100%" }}>
                        <Typography.Title
                          level={2}
                          style={{ marginTop: 0, marginBottom: 24 }}
                        >
                          {t("activePromotions")}
                        </Typography.Title>
                        <Space
                          orientation="vertical"
                          size={16}
                          style={{ width: "100%" }}
                        >
                          {activePromotions.map((promo) => (
                            <Card key={promo.id} size="small">
                              <Row
                                justify="space-between"
                                align="middle"
                                gutter={[16, 16]}
                              >
                                <Space size={16}>
                                  <div
                                    style={iconBoxStyle(
                                      48,
                                      "var(--ant-color-primary-bg)",
                                    )}
                                  >
                                    <Gift
                                      style={{
                                        width: 24,
                                        height: 24,
                                        color:
                                          "var(--ant-color-primary-text)",
                                      }}
                                    />
                                  </div>
                                  <Space orientation="vertical" size={0}>
                                    <Typography.Text strong>
                                      {promo.name}
                                    </Typography.Text>
                                    <Typography.Text
                                      type="secondary"
                                      style={{ fontSize: 14 }}
                                    >
                                      {promo.description}
                                    </Typography.Text>
                                  </Space>
                                </Space>
                                <Space
                                  orientation="vertical"
                                  size={0}
                                  align="end"
                                >
                                  <Typography.Text
                                    strong
                                    style={{
                                      fontSize: 18,
                                      color: "var(--ant-color-primary-text)",
                                    }}
                                  >
                                    {promo.value}
                                  </Typography.Text>
                                  <Typography.Text
                                    strong
                                    style={{ ...labelStyle, fontSize: 11 }}
                                  >
                                    {t("endsAt", { date: promo.endDate })}
                                  </Typography.Text>
                                </Space>
                              </Row>
                            </Card>
                          ))}
                        </Space>
                      </Card>
                    </Col>
                  </Row>

                  <Space
                    orientation="vertical"
                    size={16}
                    style={{ width: "100%" }}
                  >
                    <Typography.Title level={2} style={{ margin: 0 }}>
                      {t("cashbackHistory")}
                    </Typography.Title>
                    <Card styles={{ body: { padding: 0 } }}>
                      <List
                        dataSource={cashbackHistory}
                        renderItem={(cb) => (
                          <List.Item style={{ padding: 24 }}>
                            <Row
                              justify="space-between"
                              align="middle"
                              style={{ width: "100%" }}
                            >
                              <Space size={16}>
                                <div
                                  style={iconBoxStyle(
                                    48,
                                    cb.status === "credited"
                                      ? "var(--ant-color-success-bg)"
                                      : "var(--ant-color-warning-bg)",
                                  )}
                                >
                                  {cb.status === "credited" ? (
                                    <CheckCircle
                                      style={{
                                        width: 24,
                                        height: 24,
                                        color:
                                          "var(--ant-color-success-text)",
                                      }}
                                    />
                                  ) : (
                                    <Clock
                                      style={{
                                        width: 24,
                                        height: 24,
                                        color:
                                          "var(--ant-color-warning-text)",
                                      }}
                                    />
                                  )}
                                </div>
                                <Space orientation="vertical" size={4}>
                                  <Typography.Text strong>
                                    {cb.merchant}
                                  </Typography.Text>
                                  <Space size={8}>
                                    <Typography.Text
                                      type="secondary"
                                      style={{ fontSize: 14 }}
                                    >
                                      {cb.description}
                                    </Typography.Text>
                                    <Tag
                                      color={
                                        cb.status === "credited"
                                          ? "success"
                                          : "warning"
                                      }
                                    >
                                      {cb.status === "credited"
                                        ? t("credited")
                                        : t("pending")}
                                    </Tag>
                                  </Space>
                                  <Typography.Text
                                    strong
                                    style={{ ...labelStyle, fontSize: 11 }}
                                  >
                                    {cb.date}
                                  </Typography.Text>
                                </Space>
                              </Space>
                              <Typography.Text strong style={{ fontSize: 18 }}>
                                {formatCurrency(cb.amount)}
                              </Typography.Text>
                            </Row>
                          </List.Item>
                        )}
                      />
                    </Card>
                  </Space>
                </Space>
              ),
            },
            {
              key: "referral",
              label: (
                <Space size={8}>
                  <Share2 style={{ width: 16, height: 16 }} />
                  {t("referral")}
                </Space>
              ),
              children: (
                <Space orientation="vertical" size={24} style={{ width: "100%" }}>
                  <Card
                    style={{
                      background:
                        "linear-gradient(135deg, var(--ant-color-primary), var(--ant-color-primary-active))",
                      border: "none",
                      position: "relative",
                      overflow: "hidden",
                    }}
                    styles={{ body: { padding: 24 } }}
                  >
                    <Space
                      orientation="vertical"
                      size={24}
                      style={{ width: "100%", position: "relative", zIndex: 1 }}
                    >
                      <Row justify="space-between" align="middle">
                        <Typography.Title
                          level={2}
                          style={{
                            margin: 0,
                            color: ON_PRIMARY,
                          }}
                        >
                          {t("yourReferralCode")}
                        </Typography.Title>
                        <div
                          style={iconBoxStyle(48, "rgba(14, 15, 12, 0.08)")}
                        >
                          <Gift
                            style={{
                              width: 24,
                              height: 24,
                              color: ON_PRIMARY,
                            }}
                          />
                        </div>
                      </Row>

                      <Card
                        size="small"
                        style={{
                          backgroundColor: "rgba(14, 15, 12, 0.06)",
                          borderColor: "rgba(14, 15, 12, 0.12)",
                        }}
                        styles={{ body: { padding: 24 } }}
                      >
                        <Row justify="space-between" align="middle">
                          <Typography.Title
                            level={2}
                            style={{
                              margin: 0,
                              letterSpacing: "0.15em",
                              color: ON_PRIMARY,
                            }}
                          >
                            {referralStats.code}
                          </Typography.Title>
                          <Button
                            shape="circle"
                            type="text"
                            onClick={handleCopyReferralCode}
                            style={{
                              width: 48,
                              height: 48,
                              color: ON_PRIMARY,
                            }}
                            aria-label={t("copyCode")}
                            icon={<Copy style={{ width: 24, height: 24 }} />}
                          />
                        </Row>
                        <Typography.Text
                          style={{
                            display: "block",
                            marginTop: 16,
                            fontSize: 14,
                            color: ON_PRIMARY,
                          }}
                        >
                          {t("shareCodeHint", {
                            points: referralStats.rewardPerReferral,
                          })}
                        </Typography.Text>
                      </Card>

                      <Row gutter={24}>
                        <Col span={12}>
                          <Typography.Text
                            strong
                            style={{
                              ...onLimeLabelStyle,
                              display: "block",
                              marginBottom: 4,
                            }}
                          >
                            {t("totalFriends")}
                          </Typography.Text>
                          <Typography.Title
                            level={2}
                            style={{
                              margin: 0,
                              color: ON_PRIMARY,
                            }}
                          >
                            {referralStats.totalReferrals}
                          </Typography.Title>
                        </Col>
                        <Col span={12}>
                          <Typography.Text
                            strong
                            style={{
                              ...onLimeLabelStyle,
                              display: "block",
                              marginBottom: 4,
                            }}
                          >
                            {t("joinedSuccessfully")}
                          </Typography.Text>
                          <Typography.Text
                            strong
                            style={{
                              fontSize: 24,
                              color: ON_PRIMARY,
                            }}
                          >
                            {referralStats.completedReferrals}
                          </Typography.Text>
                        </Col>
                      </Row>
                    </Space>
                    <Share2
                      style={{
                        position: "absolute",
                        bottom: -40,
                        right: -40,
                        width: 192,
                        height: 192,
                        opacity: 0.08,
                        transform: "rotate(-12deg)",
                        color: ON_PRIMARY,
                      }}
                    />
                  </Card>

                  <Card>
                    <Typography.Title
                      level={2}
                      style={{ marginTop: 0, marginBottom: 24 }}
                    >
                      {t("referralSummary")}
                    </Typography.Title>
                    <Space
                      orientation="vertical"
                      size={24}
                      style={{ width: "100%" }}
                    >
                      {referralSummaryRows.map(
                        ({ Icon, iconColor, iconBg, title, description }) => (
                          <Space key={title} size={16} align="start">
                            <div style={iconBoxStyle(40, iconBg)}>
                              <Icon
                                style={{
                                  width: 20,
                                  height: 20,
                                  color: iconColor,
                                }}
                              />
                            </div>
                            <Space orientation="vertical" size={0}>
                              <Typography.Text strong>{title}</Typography.Text>
                              <Typography.Text
                                type="secondary"
                                style={{ fontSize: 14 }}
                              >
                                {description}
                              </Typography.Text>
                            </Space>
                          </Space>
                        ),
                      )}
                    </Space>
                    <Button
                      type="primary"
                      size="large"
                      block
                      onClick={handleShareReferral}
                      style={{ marginTop: 32, height: 56 }}
                    >
                      <Space size={8}>
                        {t("shareReferralLink")}
                        <ArrowRight style={{ width: 16, height: 16 }} />
                      </Space>
                    </Button>
                  </Card>
                </Space>
              ),
            },
          ]}
        />
      </Space>
    </DashboardLayout>
  );
}
