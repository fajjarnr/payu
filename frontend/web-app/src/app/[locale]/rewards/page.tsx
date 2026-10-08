"use client";

import React from "react";
import type { CSSProperties } from "react";
import DashboardLayout from "@/components/DashboardLayout";
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
  formatExactDecimal,
  type Money,
} from "@/lib/currency";
import { useAuthStore } from "@/stores/authStore";
import type {
  LoyaltyBalanceResponse,
  Cashback,
  ReferralSummaryResponse,
  Promotion,
} from "@/services/PromotionService";

const labelStyle: CSSProperties = {
  fontSize: 12,
  fontWeight: 700,
  color: "var(--ant-color-text-secondary)",
  textTransform: "uppercase",
  letterSpacing: "0.1em",
};

const lightLabelStyle: CSSProperties = {
  ...labelStyle,
  color: "rgba(255, 255, 255, 0.6)",
};

const iconBoxStyle = (
  size: number,
  backgroundColor: string,
): CSSProperties => ({
  width: size,
  height: size,
  borderRadius: 12,
  backgroundColor,
  display: "flex",
  alignItems: "center",
  justifyContent: "center",
  flexShrink: 0,
});

export default function RewardsPage() {
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
      toast.success("Kode referral disalin");
    } catch {
      toast.error("Gagal menyalin kode referral");
    }
  };

  const handleShareReferral = async () => {
    try {
      if (typeof navigator !== "undefined" && navigator.share) {
        await navigator.share({
          title: "PayU",
          text: "Gabung ke PayU dengan kode referral saya",
          url: referralLink,
        });
        return;
      }
      await navigator.clipboard.writeText(referralLink);
      toast.success("Link referral disalin");
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

  const formatCurrency = (amount: Money) => {
    return formatExactDecimal(amount, 0, "id-ID");
  };

  const earnMethods: Array<{
    Icon: typeof Zap;
    title: string;
    description: string;
  }> = [
    {
      Icon: Zap,
      title: "Transaksi Rutin",
      description: "Dapatkan 1 poin untuk setiap Rp 10.000 transaksi",
    },
    {
      Icon: Calendar,
      title: "Login Harian",
      description: "Dapatkan 10-100 poin untuk login setiap hari",
    },
    {
      Icon: Share2,
      title: "Referral Teman",
      description:
        "Dapatkan 1000 poin untuk setiap teman yang berhasil bergabung",
    },
    {
      Icon: Award,
      title: "Event Khusus",
      description: "Bonus poin untuk event dan promosi tertentu",
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
      iconColor: "var(--ant-color-primary)",
      iconBg: "var(--ant-color-primary-bg)",
      title: "Berhasil Bergabung",
      description: `${referralStats.completedReferrals} teman telah berhasil bergabung`,
    },
    {
      Icon: Clock,
      iconColor: "var(--ant-color-warning)",
      iconBg: "var(--ant-color-warning-bg)",
      title: "Menunggu Konfirmasi",
      description: `${referralStats.pendingReferrals} teman dalam proses verifikasi`,
    },
    {
      Icon: Trophy,
      iconColor: "var(--ant-color-success)",
      iconBg: "var(--ant-color-success-bg)",
      title: "Total Penghasilan",
      description: `${referralStats.totalEarnings.toLocaleString()} poin dari referral`,
    },
  ];

  return (
    <DashboardLayout>
      <Space direction="vertical" size={24} style={{ width: "100%" }}>
        <Space direction="vertical" size={4}>
          <Typography.Title level={2} style={{ margin: 0 }}>
            Rewards &amp; Gamifikasi
          </Typography.Title>
          <Typography.Text type="secondary">
            Kumpulkan poin, dapatkan cashback, dan raih lebih banyak keuntungan.
          </Typography.Text>
        </Space>
        <Tabs
          defaultActiveKey="points"
          items={[
            {
              key: "points",
              label: (
                <Space size={8}>
                  <Coins style={{ width: 16, height: 16 }} />
                  Poin Loyalty
                </Space>
              ),
              children: (
                <Space direction="vertical" size={24} style={{ width: "100%" }}>
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
                          direction="vertical"
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
                                "rgba(255, 255, 255, 0.2)",
                              )}
                            >
                              <Coins
                                style={{
                                  width: 24,
                                  height: 24,
                                  color: "var(--ant-color-text-light-solid)",
                                }}
                              />
                            </div>
                            <Space direction="vertical" size={0}>
                              <Typography.Text
                                strong
                                style={{ ...lightLabelStyle, display: "block" }}
                              >
                                Saldo Poin
                              </Typography.Text>
                              <Typography.Title
                                level={3}
                                style={{
                                  margin: 0,
                                  color: "var(--ant-color-text-light-solid)",
                                }}
                              >
                                {loyaltyStats.currentBalance.toLocaleString()}
                              </Typography.Title>
                            </Space>
                          </Space>
                          <Typography.Text
                            style={{
                              color: "rgba(255, 255, 255, 0.8)",
                              fontSize: 14,
                            }}
                          >
                            Tukarkan poin Anda untuk berbagai hadiah menarik
                          </Typography.Text>
                          <Row gutter={24}>
                            <Col span={12}>
                              <Typography.Text
                                strong
                                style={{
                                  ...lightLabelStyle,
                                  display: "block",
                                  marginBottom: 4,
                                }}
                              >
                                Total Diperoleh
                              </Typography.Text>
                              <Typography.Title
                                level={4}
                                style={{
                                  margin: 0,
                                  color: "var(--ant-color-text-light-solid)",
                                }}
                              >
                                {loyaltyStats.totalEarned.toLocaleString()}
                              </Typography.Title>
                            </Col>
                            <Col span={12}>
                              <Typography.Text
                                strong
                                style={{
                                  ...lightLabelStyle,
                                  display: "block",
                                  marginBottom: 4,
                                }}
                              >
                                Total Ditukar
                              </Typography.Text>
                              <Typography.Title
                                level={4}
                                style={{
                                  margin: 0,
                                  color: "var(--ant-color-text-light-solid)",
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
                                backgroundColor: "rgba(255, 255, 255, 0.1)",
                                borderColor: "rgba(255, 255, 255, 0.15)",
                              }}
                              styles={{ body: { padding: 16 } }}
                            >
                              <Space size={12} align="start">
                                <Calendar
                                  style={{
                                    width: 20,
                                    height: 20,
                                    color: "var(--ant-color-warning)",
                                  }}
                                />
                                <Space direction="vertical" size={0}>
                                  <Typography.Text
                                    strong
                                    style={{
                                      ...lightLabelStyle,
                                      display: "block",
                                    }}
                                  >
                                    Poin Akan Kadaluarsa
                                  </Typography.Text>
                                  <Typography.Text
                                    strong
                                    style={{
                                      color:
                                        "var(--ant-color-text-light-solid)",
                                    }}
                                  >
                                    {loyaltyStats.pointsExpiring.toLocaleString()}{" "}
                                    poin - {loyaltyStats.expiryDate}
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
                            color: "var(--ant-color-text-light-solid)",
                          }}
                        />
                      </Card>
                    </Col>

                    <Col xs={24} lg={12}>
                      <Card style={{ height: "100%" }}>
                        <Typography.Title
                          level={4}
                          style={{ marginTop: 0, marginBottom: 24 }}
                        >
                          Cara Mendapatkan Poin
                        </Typography.Title>
                        <Space
                          direction="vertical"
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
                                    color: "var(--ant-color-primary)",
                                  }}
                                />
                              </div>
                              <Space direction="vertical" size={0}>
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
                    direction="vertical"
                    size={16}
                    style={{ width: "100%" }}
                  >
                    <Typography.Title level={4} style={{ margin: 0 }}>
                      Riwayat Poin
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
                                        color: "var(--ant-color-success)",
                                      }}
                                    />
                                  ) : (
                                    <History
                                      style={{
                                        width: 24,
                                        height: 24,
                                        color: "var(--ant-color-warning)",
                                      }}
                                    />
                                  )}
                                </div>
                                <Space direction="vertical" size={0}>
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
                                type={
                                  point.type === "EARNED"
                                    ? "success"
                                    : "warning"
                                }
                                style={{ fontSize: 18 }}
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
              label: (
                <Space size={8}>
                  <DollarSign style={{ width: 16, height: 16 }} />
                  Cashback
                </Space>
              ),
              children: (
                <Space direction="vertical" size={24} style={{ width: "100%" }}>
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
                          direction="vertical"
                          size={24}
                          style={{ width: "100%" }}
                        >
                          <Space size={12} align="start">
                            <div
                              style={iconBoxStyle(
                                56,
                                "rgba(255, 255, 255, 0.1)",
                              )}
                            >
                              <DollarSign
                                style={{
                                  width: 28,
                                  height: 28,
                                  color: "var(--ant-color-text-light-solid)",
                                }}
                              />
                            </div>
                            <Space direction="vertical" size={0}>
                              <Typography.Text
                                strong
                                style={{ ...lightLabelStyle, display: "block" }}
                              >
                                Total Cashback
                              </Typography.Text>
                              <Typography.Title
                                level={3}
                                style={{
                                  margin: 0,
                                  color: "var(--ant-color-text-light-solid)",
                                }}
                              >
                                {formatCurrency(cashbackTotal)}
                              </Typography.Title>
                            </Space>
                          </Space>
                          <Space
                            direction="vertical"
                            size={0}
                            style={{ width: "100%" }}
                          >
                            <Row
                              justify="space-between"
                              align="middle"
                              style={{
                                padding: "12px 0",
                                borderBottom:
                                  "1px solid rgba(255, 255, 255, 0.1)",
                              }}
                            >
                              <Typography.Text
                                style={{
                                  fontSize: 14,
                                  color: "rgba(255, 255, 255, 0.65)",
                                }}
                              >
                                Dikreditkan
                              </Typography.Text>
                              <Typography.Text strong type="success">
                                {formatCurrency(cashbackCredited)}
                              </Typography.Text>
                            </Row>
                            <Row
                              justify="space-between"
                              align="middle"
                              style={{
                                padding: "12px 0",
                                borderBottom:
                                  "1px solid rgba(255, 255, 255, 0.1)",
                              }}
                            >
                              <Typography.Text
                                style={{
                                  fontSize: 14,
                                  color: "rgba(255, 255, 255, 0.65)",
                                }}
                              >
                                Menunggu
                              </Typography.Text>
                              <Typography.Text strong type="warning">
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
                                  color: "rgba(255, 255, 255, 0.65)",
                                }}
                              >
                                Kadaluarsa
                              </Typography.Text>
                              <Typography.Text strong type="danger">
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
                          level={4}
                          style={{ marginTop: 0, marginBottom: 24 }}
                        >
                          Promosi Aktif
                        </Typography.Title>
                        <Space
                          direction="vertical"
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
                                        color: "var(--ant-color-primary)",
                                      }}
                                    />
                                  </div>
                                  <Space direction="vertical" size={0}>
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
                                  direction="vertical"
                                  size={0}
                                  align="end"
                                >
                                  <Typography.Text
                                    strong
                                    style={{
                                      fontSize: 18,
                                      color: "var(--ant-color-primary)",
                                    }}
                                  >
                                    {promo.value}
                                  </Typography.Text>
                                  <Typography.Text
                                    strong
                                    style={{ ...labelStyle, fontSize: 11 }}
                                  >
                                    Berakhir: {promo.endDate}
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
                    direction="vertical"
                    size={16}
                    style={{ width: "100%" }}
                  >
                    <Typography.Title level={4} style={{ margin: 0 }}>
                      Riwayat Cashback
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
                                        color: "var(--ant-color-success)",
                                      }}
                                    />
                                  ) : (
                                    <Clock
                                      style={{
                                        width: 24,
                                        height: 24,
                                        color: "var(--ant-color-warning)",
                                      }}
                                    />
                                  )}
                                </div>
                                <Space direction="vertical" size={4}>
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
                                        ? "Dikreditkan"
                                        : "Menunggu"}
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
                  Referral
                </Space>
              ),
              children: (
                <Space direction="vertical" size={24} style={{ width: "100%" }}>
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
                      direction="vertical"
                      size={24}
                      style={{ width: "100%", position: "relative", zIndex: 1 }}
                    >
                      <Row justify="space-between" align="middle">
                        <Typography.Title
                          level={4}
                          style={{
                            margin: 0,
                            color: "var(--ant-color-text-light-solid)",
                          }}
                        >
                          Kode Referral Anda
                        </Typography.Title>
                        <div
                          style={iconBoxStyle(48, "rgba(255, 255, 255, 0.1)")}
                        >
                          <Gift
                            style={{
                              width: 24,
                              height: 24,
                              color: "var(--ant-color-text-light-solid)",
                            }}
                          />
                        </div>
                      </Row>

                      <Card
                        size="small"
                        style={{
                          backgroundColor: "rgba(255, 255, 255, 0.1)",
                          borderColor: "rgba(255, 255, 255, 0.1)",
                        }}
                        styles={{ body: { padding: 24 } }}
                      >
                        <Row justify="space-between" align="middle">
                          <Typography.Title
                            level={2}
                            style={{
                              margin: 0,
                              letterSpacing: "0.15em",
                              color: "var(--ant-color-text-light-solid)",
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
                              color: "var(--ant-color-text-light-solid)",
                            }}
                            aria-label="Salin kode"
                            icon={<Copy style={{ width: 24, height: 24 }} />}
                          />
                        </Row>
                        <Typography.Text
                          style={{
                            display: "block",
                            marginTop: 16,
                            fontSize: 14,
                            color: "rgba(255, 255, 255, 0.8)",
                          }}
                        >
                          Bagikan kode ini kepada teman dan dapatkan{" "}
                          {referralStats.rewardPerReferral} poin untuk setiap
                          teman yang berhasil bergabung
                        </Typography.Text>
                      </Card>

                      <Row gutter={24}>
                        <Col span={12}>
                          <Typography.Text
                            strong
                            style={{
                              ...lightLabelStyle,
                              display: "block",
                              marginBottom: 4,
                            }}
                          >
                            Total Teman
                          </Typography.Text>
                          <Typography.Title
                            level={4}
                            style={{
                              margin: 0,
                              color: "var(--ant-color-text-light-solid)",
                            }}
                          >
                            {referralStats.totalReferrals}
                          </Typography.Title>
                        </Col>
                        <Col span={12}>
                          <Typography.Text
                            strong
                            style={{
                              ...lightLabelStyle,
                              display: "block",
                              marginBottom: 4,
                            }}
                          >
                            Berhasil Bergabung
                          </Typography.Text>
                          <Typography.Text
                            strong
                            type="success"
                            style={{ fontSize: 24 }}
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
                        color: "var(--ant-color-text-light-solid)",
                      }}
                    />
                  </Card>

                  <Card>
                    <Typography.Title
                      level={4}
                      style={{ marginTop: 0, marginBottom: 24 }}
                    >
                      Ringkasan Referral
                    </Typography.Title>
                    <Space
                      direction="vertical"
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
                            <Space direction="vertical" size={0}>
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
                        Bagikan Link Referral
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
