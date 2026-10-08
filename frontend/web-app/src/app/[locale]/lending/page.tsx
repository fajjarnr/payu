/* eslint-disable @typescript-eslint/no-unused-vars -- Calendar reserved */
"use client";

/* eslint-disable no-restricted-syntax -- display percentage uses Number for chart width, not Money arithmetic (ADR-0047 display only) */

import React from "react";
import DashboardLayout from "@/components/DashboardLayout";
import {
  CreditCard,
  Calendar,
  ShieldCheck,
  Wallet,
  ArrowRight,
  Percent,
  CheckCircle,
  Clock,
  Plus,
  FileText,
  TrendingUp,
} from "@/components/icons";
import {
  Button,
  Skeleton,
  Tabs,
  Card,
  Space,
  Row,
  Col,
  Divider,
  Typography,
  Tag,
  List,
  Progress,
  theme,
} from "antd";
import {
  useCreditScore,
  usePayLater,
  usePayLaterTransactions,
  useActivePreApprovals,
  useActivatePayLater,
  useApplyLoan,
  usePayLaterPayment,
} from "@/hooks";
import { useAuthStore } from "@/stores/authStore";
import { asMoney, formatCurrency } from "@/lib/currency";
import { notify as toast } from "@/lib/notify";

export default function LendingPage() {
  const { token } = theme.useToken();
  const { user } = useAuthStore();
  const userId = user?.id ?? "";
  const { data: creditScoreData, isLoading: isLoadingScore } =
    useCreditScore(userId);
  const { data: payLaterData, isLoading: _isLoadingPayLater } =
    usePayLater(userId);
  const { data: payLaterTxns } = usePayLaterTransactions(userId);
  const { data: preApprovals, isLoading: isLoadingPreApprovals } =
    useActivePreApprovals(userId);
  const activatePayLater = useActivatePayLater();
  const applyLoan = useApplyLoan();
  const payLaterPayment = usePayLaterPayment();

  // ponytail: minimal wiring for L1-L7 — real mutations instead of toast-only stubs
  const handleActivatePayLater = async () => {
    try {
      await activatePayLater.mutateAsync({
        userId,
        request: {
          monthlyIncome: asMoney("5000000"),
          employmentType: "FULL_TIME",
          employmentDurationMonths: 12,
        },
      });
      toast.success("PayLater berhasil diaktifkan");
    } catch (e: unknown) {
      toast.error(e instanceof Error ? e.message : "Gagal aktivasi PayLater");
    }
  };

  const handleApplyLoan = async (productName: string) => {
    try {
      // eslint-disable-next-line react-hooks/purity -- externalId generated per user action, not render
      const externalId = `ext-${Date.now()}`;
      await applyLoan.mutateAsync({
        externalId,
        loanType: "PERSONAL",
        principalAmount: asMoney("10000000"),
        tenureMonths: 12,
        purpose: productName,
      });
      toast.success(`Pengajuan ${productName} telah diterima`);
    } catch (e: unknown) {
      toast.error(e instanceof Error ? e.message : "Gagal mengajukan pinjaman");
    }
  };

  const handlePayBill = async () => {
    try {
      const amount = payLaterData?.minimumPayment ?? "100000";
      await payLaterPayment.mutateAsync({ userId, amount });
      toast.success("Pembayaran tagihan berhasil");
    } catch (e: unknown) {
      toast.error(e instanceof Error ? e.message : "Gagal membayar tagihan");
    }
  };

  const loanProducts = [
    {
      name: "Pinjaman Personal",
      description:
        "Pembiayaan fleksibel untuk kebutuhan pribadi dengan bunga kompetitif",
      minAmount: "2000000",
      maxAmount: "50000000",
      interestRate: "12.5% p.a",
      tenure: "6 - 36 bulan",
      processingTime: "1-2 hari kerja",
      icon: FileText,
      color: token.colorPrimary,
      bg: token.colorPrimaryBg,
    },
    {
      name: "Pinjaman Multiguna",
      description:
        "Gunakan aset Anda sebagai jaminan untuk limit pinjaman lebih tinggi",
      minAmount: "10000000",
      maxAmount: "200000000",
      interestRate: "10% p.a",
      tenure: "12 - 60 bulan",
      processingTime: "3-5 hari kerja",
      icon: TrendingUp,
      color: token.colorSuccess,
      bg: token.colorSuccessBg,
    },
  ];

  const payLaterStats = {
    creditLimit: payLaterData?.creditLimit ?? "0",
    usedLimit: payLaterData?.usedLimit ?? "0",
    availableLimit: payLaterData?.availableLimit ?? "0",
    minimumPayment: payLaterData?.minimumPayment ?? "0",
    dueDate: payLaterData?.dueDate ?? "--",
    transactions: (payLaterTxns ?? []).map((t) => ({
      id: String(t.id),
      merchant: t.merchantName ?? "Unknown",
      amount: t.amount,
      date: t.createdAt ?? "--",
      status: t.type === "PAYMENT" ? "paid" : "pending",
    })),
  };

  const creditScore = {
    score: creditScoreData?.score ?? 0,
    grade: creditScoreData?.grade ?? "--",
    maxScore: 850,
    lastUpdated: creditScoreData?.lastUpdated ?? "--",
    factors: creditScoreData?.factors ?? [],
  };

  const creditUtilization =
    Number(payLaterStats.creditLimit) > 0
      ? (Number(payLaterStats.usedLimit) / Number(payLaterStats.creditLimit)) *
        100
      : 0;

  return (
    <DashboardLayout>
      <Space direction="vertical" size={16} style={{ width: "100%" }}>
        <Row justify="space-between" align="bottom" gutter={[16, 16]}>
          <div>
            <Typography.Title level={2} style={{ marginBottom: 0 }}>
              Pinjaman & Kredit
            </Typography.Title>
            <Typography.Text type="secondary">
              Solusi pembiayaan fleksibel sesuai kebutuhan Anda.
            </Typography.Text>
          </div>
          <Button
            type="primary"
            size="large"
            onClick={handleActivatePayLater}
            disabled={activatePayLater.isPending}
            data-testid="activate-paylater-button"
          >
            <Plus style={{ width: 16, height: 16 }} />{" "}
            {activatePayLater.isPending ? "Memproses..." : "Aktifkan PayLater"}
          </Button>
        </Row>
        <Tabs
          defaultActiveKey="loans"
          data-testid="lending-tabs"
          items={[
            {
              key: "loans",
              label: (
                <span data-testid="loans-tab">Pinjaman</span>
              ),
              children: (
                <Space direction="vertical" size={16} style={{ width: "100%" }}>
                  <Row gutter={[16, 16]}>
                    <Col xs={24} lg={16}>
                      <Card
                        style={{
                          background: `linear-gradient(135deg, ${token.colorText}, ${token.colorTextSecondary})`,
                          color: token.colorBgContainer,
                        }}
                        styles={{ body: { padding: 24 } }}
                      >
                        <Row
                          justify="space-between"
                          align="top"
                          gutter={[16, 16]}
                        >
                          <div>
                            <Typography.Text
                              strong
                              style={{
                                fontSize: 12,
                                letterSpacing: "0.1em",
                                textTransform: "uppercase",
                                color: token.colorBgLayout,
                              }}
                            >
                              Skor Kredit Anda
                            </Typography.Text>
                            <Space align="baseline" size={12}>
                              <Typography.Title
                                level={1}
                                style={{
                                  marginBottom: 0,
                                  color: token.colorBgContainer,
                                }}
                              >
                                {isLoadingScore ? (
                                  <Skeleton.Input
                                    active
                                    size="large"
                                    style={{ width: 80 }}
                                  />
                                ) : (
                                  creditScore.score
                                )}
                              </Typography.Title>
                              <Tag color="success">
                                <Typography.Text
                                  strong
                                  style={{ color: token.colorSuccess }}
                                >
                                  Grade{" "}
                                  {isLoadingScore ? "..." : creditScore.grade}
                                </Typography.Text>
                              </Tag>
                            </Space>
                            <Typography.Text
                              strong
                              style={{
                                display: "block",
                                marginTop: 12,
                                fontSize: 12,
                                letterSpacing: "0.1em",
                                textTransform: "uppercase",
                                color: token.colorBgLayout,
                              }}
                            >
                              Terakhir diperbarui:{" "}
                              {isLoadingScore ? "..." : creditScore.lastUpdated}
                            </Typography.Text>
                          </div>
                          <div
                            style={{
                              width: 64,
                              height: 64,
                              borderRadius: 12,
                              display: "flex",
                              alignItems: "center",
                              justifyContent: "center",
                              background: "rgba(255,255,255,0.1)",
                            }}
                          >
                            <ShieldCheck
                              style={{
                                width: 32,
                                height: 32,
                                color: token.colorSuccess,
                              }}
                            />
                          </div>
                        </Row>

                        <Space
                          direction="vertical"
                          size={12}
                          style={{ marginTop: 32 }}
                        >
                          {isLoadingScore
                            ? [1, 2].map((i) => (
                                <Skeleton.Input
                                  key={i}
                                  active
                                  size="small"
                                  style={{ width: 192 }}
                                />
                              ))
                            : creditScore.factors.map((factor, i) => (
                                <Space key={i} size={12}>
                                  <div
                                    style={{
                                      width: 24,
                                      height: 24,
                                      borderRadius: "50%",
                                      display: "flex",
                                      alignItems: "center",
                                      justifyContent: "center",
                                      background: "rgba(255,255,255,0.2)",
                                    }}
                                  >
                                    <CheckCircle
                                      style={{
                                        width: 16,
                                        height: 16,
                                        color: token.colorSuccess,
                                      }}
                                    />
                                  </div>
                                  <Typography.Text
                                    style={{ color: token.colorBgContainer }}
                                  >
                                    {factor}
                                  </Typography.Text>
                                </Space>
                              ))}
                        </Space>

                        <Progress
                          percent={
                            (creditScore.score / creditScore.maxScore) * 100
                          }
                          showInfo={false}
                          strokeColor={token.colorSuccess}
                          style={{ marginTop: 16 }}
                        />
                      </Card>
                    </Col>
                    <Col xs={24} lg={8}>
                      <Card style={{ height: "100%" }}>
                        <div
                          style={{
                            width: 48,
                            height: 48,
                            borderRadius: 12,
                            display: "flex",
                            alignItems: "center",
                            justifyContent: "center",
                            marginBottom: 24,
                            background: token.colorPrimaryBg,
                          }}
                        >
                          <Wallet
                            style={{
                              width: 24,
                              height: 24,
                              color: token.colorPrimary,
                            }}
                          />
                        </div>
                        <Typography.Title
                          level={4}
                          style={{ marginBottom: 12 }}
                        >
                          Total Limit Pinjaman
                        </Typography.Title>
                        <Typography.Title
                          level={2}
                          style={{
                            marginBottom: 8,
                            color: token.colorPrimary,
                          }}
                        >
                          {isLoadingPreApprovals ? (
                            <Skeleton.Input
                              active
                              size="large"
                              style={{ width: 128 }}
                            />
                          ) : (
                            formatCurrency(preApprovals?.[0]?.maxAmount ?? "0")
                          )}
                        </Typography.Title>
                        <Typography.Text
                          strong
                          type="secondary"
                          style={{
                            fontSize: 12,
                            letterSpacing: "0.1em",
                            textTransform: "uppercase",
                          }}
                        >
                          Tersedia berdasarkan skor kredit
                        </Typography.Text>
                      </Card>
                    </Col>
                  </Row>

                  <Space
                    direction="vertical"
                    size={16}
                    style={{ marginTop: 16, width: "100%" }}
                  >
                    <Typography.Title level={4} style={{ marginBottom: 0 }}>
                      Produk Pinjaman
                    </Typography.Title>
                    <Row gutter={[16, 16]}>
                      {loanProducts.map((product, i) => (
                        <Col xs={24} md={12} key={i}>
                          <Card
                            data-testid={`loan-product-${i}`}
                            hoverable
                            style={{ height: "100%" }}
                          >
                            <Row
                              justify="space-between"
                              align="top"
                              style={{ marginBottom: 24 }}
                            >
                              <div
                                style={{
                                  width: 64,
                                  height: 64,
                                  borderRadius: 12,
                                  display: "flex",
                                  alignItems: "center",
                                  justifyContent: "center",
                                  background: product.bg,
                                  color: product.color,
                                }}
                              >
                                <product.icon
                                  style={{ width: 32, height: 32 }}
                                />
                              </div>
                              <div
                                style={{
                                  width: 40,
                                  height: 40,
                                  borderRadius: 8,
                                  display: "flex",
                                  alignItems: "center",
                                  justifyContent: "center",
                                  background: token.colorFillTertiary,
                                  color: token.colorTextSecondary,
                                }}
                              >
                                <Percent
                                  style={{ width: 20, height: 20 }}
                                />
                              </div>
                            </Row>
                            <div>
                              <Typography.Title
                                level={4}
                                style={{ marginBottom: 8 }}
                              >
                                {product.name}
                              </Typography.Title>
                              <Typography.Paragraph type="secondary">
                                {product.description}
                              </Typography.Paragraph>
                              <Space
                                direction="vertical"
                                size={12}
                                style={{ marginBottom: 24, width: "100%" }}
                              >
                                <Row justify="space-between" align="middle">
                                  <Typography.Text type="secondary">
                                    Limit Pinjaman
                                  </Typography.Text>
                                  <Typography.Text strong>
                                    {formatCurrency(product.minAmount)} -{" "}
                                    {formatCurrency(product.maxAmount)}
                                  </Typography.Text>
                                </Row>
                                <Row justify="space-between" align="middle">
                                  <Typography.Text type="secondary">
                                    Bunga
                                  </Typography.Text>
                                  <Typography.Text
                                    strong
                                    style={{ color: token.colorPrimary }}
                                  >
                                    {product.interestRate}
                                  </Typography.Text>
                                </Row>
                                <Row justify="space-between" align="middle">
                                  <Typography.Text type="secondary">
                                    Tenor
                                  </Typography.Text>
                                  <Typography.Text strong>
                                    {product.tenure}
                                  </Typography.Text>
                                </Row>
                                <Row justify="space-between" align="middle">
                                  <Typography.Text type="secondary">
                                    Proses
                                  </Typography.Text>
                                  <Typography.Text strong>
                                    {product.processingTime}
                                  </Typography.Text>
                                </Row>
                              </Space>
                              <Button
                                type="primary"
                                size="large"
                                block
                                onClick={() => handleApplyLoan(product.name)}
                                disabled={applyLoan.isPending}
                                data-testid={`apply-loan-${i}`}
                              >
                                {applyLoan.isPending ? (
                                  "Memproses..."
                                ) : (
                                  <>
                                    Ajukan Sekarang{" "}
                                    <ArrowRight
                                      style={{ width: 16, height: 16 }}
                                    />
                                  </>
                                )}
                              </Button>
                            </div>
                          </Card>
                        </Col>
                      ))}
                    </Row>
                  </Space>
                </Space>
              ),
            },
            {
              key: "paylater",
              label: (
                <span data-testid="paylater-tab">PayLater</span>
              ),
              children: (
                <Space direction="vertical" size={16} style={{ width: "100%" }}>
                  <Row gutter={[16, 16]}>
                    <Col xs={24} lg={16}>
                      <Card
                        style={{
                          background: `linear-gradient(135deg, ${token.colorPrimary}, ${token.colorPrimaryActive})`,
                          color: token.colorBgContainer,
                        }}
                        styles={{ body: { padding: 24 } }}
                      >
                        <Row
                          justify="space-between"
                          align="middle"
                          gutter={[16, 16]}
                          style={{ marginBottom: 24 }}
                        >
                          <div>
                            <Space
                              size={12}
                              align="center"
                              style={{ marginBottom: 12 }}
                            >
                              <div
                                style={{
                                  width: 48,
                                  height: 48,
                                  borderRadius: 12,
                                  display: "flex",
                                  alignItems: "center",
                                  justifyContent: "center",
                                  background: "rgba(255,255,255,0.2)",
                                }}
                              >
                                <CreditCard
                                  style={{ width: 24, height: 24 }}
                                />
                              </div>
                              <div>
                                <Typography.Text
                                  strong
                                  style={{
                                    display: "block",
                                    fontSize: 12,
                                    letterSpacing: "0.1em",
                                    textTransform: "uppercase",
                                    color: "rgba(255,255,255,0.8)",
                                  }}
                                >
                                  PayLater Limit
                                </Typography.Text>
                                <Typography.Title
                                  level={2}
                                  style={{
                                    marginBottom: 0,
                                    color: token.colorBgContainer,
                                  }}
                                >
                                  {formatCurrency(payLaterStats.creditLimit)}
                                </Typography.Title>
                              </div>
                            </Space>
                            <Space
                              size={16}
                              style={{
                                fontSize: 12,
                                color: "rgba(255,255,255,0.8)",
                              }}
                            >
                              <Typography.Text
                                strong
                                style={{ color: "rgba(255,255,255,0.8)" }}
                              >
                                Terpakai:{" "}
                                {formatCurrency(payLaterStats.usedLimit)}
                              </Typography.Text>
                              <Typography.Text
                                strong
                                style={{ color: "rgba(255,255,255,0.8)" }}
                              >
                                •
                              </Typography.Text>
                              <Typography.Text
                                strong
                                style={{ color: "rgba(255,255,255,0.8)" }}
                              >
                                Tersedia:{" "}
                                {formatCurrency(payLaterStats.availableLimit)}
                              </Typography.Text>
                            </Space>
                          </div>

                          <div style={{ minWidth: 200 }}>
                            <Row justify="space-between" align="middle">
                              <Typography.Text
                                strong
                                style={{
                                  fontSize: 12,
                                  letterSpacing: "0.1em",
                                  textTransform: "uppercase",
                                  color: "rgba(255,255,255,0.8)",
                                }}
                              >
                                Penggunaan Limit
                              </Typography.Text>
                              <Typography.Text
                                strong
                                style={{ color: token.colorBgContainer }}
                              >
                                {creditUtilization.toFixed(0)}%
                              </Typography.Text>
                            </Row>
                            <Progress
                              percent={creditUtilization}
                              showInfo={false}
                              strokeColor={token.colorBgContainer}
                              trailColor="rgba(255,255,255,0.2)"
                              style={{ marginTop: 8 }}
                            />
                          </div>
                        </Row>

                        <Divider style={{ borderColor: "rgba(255,255,255,0.2)" }} />

                        <Row
                          justify="space-between"
                          align="middle"
                          gutter={[16, 16]}
                        >
                          <div>
                            <Typography.Text
                              strong
                              style={{
                                display: "block",
                                fontSize: 12,
                                letterSpacing: "0.1em",
                                textTransform: "uppercase",
                                color: "rgba(255,255,255,0.6)",
                              }}
                            >
                              Pembayaran Minimum
                            </Typography.Text>
                            <Typography.Title
                              level={4}
                              style={{
                                marginBottom: 0,
                                color: token.colorBgContainer,
                              }}
                            >
                              {formatCurrency(payLaterStats.minimumPayment)}
                            </Typography.Title>
                          </div>
                          <Button
                            size="large"
                            onClick={handlePayBill}
                            disabled={payLaterPayment.isPending}
                            data-testid="pay-bill-button"
                            style={{
                              background: token.colorBgContainer,
                              color: token.colorPrimary,
                            }}
                          >
                            {payLaterPayment.isPending
                              ? "Memproses..."
                              : "Bayar Tagihan"}
                          </Button>
                        </Row>
                      </Card>
                    </Col>
                    <Col xs={24} lg={8}>
                      <Card style={{ height: "100%" }}>
                        <Row
                          justify="space-between"
                          align="top"
                          style={{ marginBottom: 24 }}
                        >
                          <Typography.Title
                            level={5}
                            style={{ marginBottom: 0 }}
                          >
                            Ringkasan Transaksi
                          </Typography.Title>
                          <div
                            style={{
                              width: 32,
                              height: 32,
                              borderRadius: 8,
                              display: "flex",
                              alignItems: "center",
                              justifyContent: "center",
                              background: token.colorPrimaryBg,
                            }}
                          >
                            <TrendingUp
                              style={{
                                width: 16,
                                height: 16,
                                color: token.colorPrimary,
                              }}
                            />
                          </div>
                        </Row>
                        <Space
                          direction="vertical"
                          size={16}
                          style={{ width: "100%" }}
                        >
                          <Row
                            justify="space-between"
                            align="middle"
                            style={{
                              paddingTop: 12,
                              paddingBottom: 12,
                              borderBottom: `1px solid ${token.colorBorderSecondary}`,
                            }}
                          >
                            <Typography.Text type="secondary">
                              Total Transaksi
                            </Typography.Text>
                            <Typography.Text strong style={{ fontSize: 16 }}>
                              {payLaterStats.transactions.length}
                            </Typography.Text>
                          </Row>
                          <Row
                            justify="space-between"
                            align="middle"
                            style={{
                              paddingTop: 12,
                              paddingBottom: 12,
                              borderBottom: `1px solid ${token.colorBorderSecondary}`,
                            }}
                          >
                            <Typography.Text type="secondary">
                              Pembayaran Berhasil
                            </Typography.Text>
                            <Typography.Text
                              strong
                              style={{
                                fontSize: 16,
                                color: token.colorSuccess,
                              }}
                            >
                              {
                                payLaterStats.transactions.filter(
                                  (t) => t.status === "paid",
                                ).length
                              }
                            </Typography.Text>
                          </Row>
                          <Row
                            justify="space-between"
                            align="middle"
                            style={{ paddingTop: 12, paddingBottom: 12 }}
                          >
                            <Typography.Text type="secondary">
                              Menunggu Pembayaran
                            </Typography.Text>
                            <Typography.Text
                              strong
                              style={{
                                fontSize: 16,
                                color: token.colorWarning,
                              }}
                            >
                              {
                                payLaterStats.transactions.filter(
                                  (t) => t.status !== "paid",
                                ).length
                              }
                            </Typography.Text>
                          </Row>
                        </Space>
                      </Card>
                    </Col>
                  </Row>
                  <Space
                    direction="vertical"
                    size={16}
                    style={{ marginTop: 16, width: "100%" }}
                  >
                    <Typography.Title level={4} style={{ marginBottom: 0 }}>
                      Riwayat Transaksi PayLater
                    </Typography.Title>
                    <Card styles={{ body: { padding: 0 } }}>
                      <List
                        dataSource={payLaterStats.transactions}
                        renderItem={(txn) => (
                          <List.Item
                            key={txn.id}
                            data-testid={`transaction-${txn.id}`}
                          >
                            <Row
                              justify="space-between"
                              align="middle"
                              style={{ width: "100%" }}
                              gutter={[16, 16]}
                            >
                              <Space size={16}>
                                <div
                                  style={{
                                    width: 48,
                                    height: 48,
                                    borderRadius: 12,
                                    display: "flex",
                                    alignItems: "center",
                                    justifyContent: "center",
                                    background:
                                      txn.status === "paid"
                                        ? token.colorSuccessBg
                                        : token.colorWarningBg,
                                    color:
                                      txn.status === "paid"
                                        ? token.colorSuccess
                                        : token.colorWarning,
                                  }}
                                >
                                  {txn.status === "paid" ? (
                                    <CheckCircle
                                      style={{ width: 24, height: 24 }}
                                    />
                                  ) : (
                                    <Clock style={{ width: 24, height: 24 }} />
                                  )}
                                </div>
                                <div>
                                  <Typography.Text strong>
                                    {txn.merchant}
                                  </Typography.Text>
                                  <Typography.Text
                                    strong
                                    type="secondary"
                                    style={{
                                      display: "block",
                                      fontSize: 12,
                                      letterSpacing: "0.1em",
                                    }}
                                  >
                                    {txn.date}
                                  </Typography.Text>
                                </div>
                              </Space>
                              <div style={{ textAlign: "right" }}>
                                <Typography.Text
                                  strong
                                  style={{ display: "block", fontSize: 16 }}
                                >
                                  {formatCurrency(txn.amount)}
                                </Typography.Text>
                                <Typography.Text
                                  strong
                                  style={{
                                    fontSize: 12,
                                    letterSpacing: "0.1em",
                                    color:
                                      txn.status === "paid"
                                        ? token.colorSuccess
                                        : token.colorWarning,
                                  }}
                                >
                                  {txn.status === "paid"
                                    ? "Dibayar"
                                    : "Menunggu Pembayaran"}
                                </Typography.Text>
                              </div>
                            </Row>
                          </List.Item>
                        )}
                      />
                    </Card>
                  </Space>
                </Space>
              ),
            },
          ]}
        />
      </Space>
    </DashboardLayout>
  );
}
