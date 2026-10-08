"use client";

import {
  Smartphone,
  Zap,
  Droplets,
  Wifi,
  CreditCard,
  Heart,
  Tv,
  Gamepad2,
  Plus,
  ChevronRight,
  LifeBuoy,
  CheckCircle2,
} from "@/components/icons";
import { useMutation, useQuery } from "@tanstack/react-query";
import { CreatePaymentRequest, PaymentResponse } from "@/types";
import api from "@/lib/api";
import { useState } from "react";
import { useRouter } from "next/navigation";
import DashboardLayout from "@/components/DashboardLayout";
import { SkipLink } from "@/lib/a11y";
import {
  Button,
  Card,
  Col,
  Divider,
  Flex,
  Grid,
  Input,
  Row,
  Space,
  Tag,
  theme,
  Typography,
} from "antd";
import { useUIStore } from "@/stores";
import { useAuthStore } from "@/stores/authStore";
import { idempotencyKeyFor } from "@/lib/utils";
import { notify as toast } from "@/lib/notify";
import { formatCurrency, parseCurrencyExact } from "@/lib/currency";

type BillerIcon = React.ComponentType<{ style?: React.CSSProperties }>;

interface Biller {
  name: string;
  icon: BillerIcon;
  /** Tile background / icon colour from the antd theme (dark-mode aware). */
  bg: string;
  fg: string;
  code: string;
}

interface PaymentSuccess {
  billerName: string;
  billerIcon: BillerIcon;
  billerBg: string;
  billerFg: string;
  customerId: string;
  amount: string;
  timestamp: Date;
}

export default function BillsPage() {
  const router = useRouter();
  const { addToast } = useUIStore();
  const { accountId: authAccountId } = useAuthStore();
  const { token } = theme.useToken();
  const screens = Grid.useBreakpoint();
  const isSm = Boolean(screens.sm);
  const isMd = Boolean(screens.md);
  const [selectedBiller, setSelectedBiller] = useState<Biller | null>(null);
  const [customerId, setCustomerId] = useState("");
  const [amount, setAmount] = useState("");
  const [showSuccess, setShowSuccess] = useState(false);
  const [successData, setSuccessData] = useState<PaymentSuccess | null>(null);

  const billers: Biller[] = [
    {
      name: "Pulsa",
      icon: Smartphone,
      bg: token.colorPrimaryBg,
      fg: token.colorPrimaryText,
      code: "PULSA",
    },
    {
      name: "Listrik (PLN)",
      icon: Zap,
      bg: token.colorWarningBg,
      fg: token.colorWarningText,
      code: "PLN",
    },
    {
      name: "Air (PDAM)",
      icon: Droplets,
      bg: token.colorPrimaryBg,
      fg: token.colorPrimaryTextActive,
      code: "PDAM",
    },
    {
      name: "Internet/TV",
      icon: Wifi,
      bg: token.geekblue6,
      fg: token.colorTextLightSolid,
      code: "INTERNET",
    },
    {
      name: "Saldo Kartu",
      icon: CreditCard,
      bg: token.orange6,
      fg: token.colorTextLightSolid,
      code: "CARDS",
    },
    {
      name: "BPJS",
      icon: Heart,
      bg: token.colorSuccess,
      fg: token.colorTextLightSolid,
      code: "BPJS",
    },
    {
      name: "TV Kabel",
      icon: Tv,
      bg: token.magenta6,
      fg: token.colorTextLightSolid,
      code: "TV",
    },
    {
      name: "Game Voucher",
      icon: Gamepad2,
      bg: token.purple6,
      fg: token.colorTextLightSolid,
      code: "VOUCHER",
    },
  ];

  const accountId = authAccountId;

  const { data: recentPayments, isLoading } = useQuery({
    queryKey: ["recent-payments", accountId],
    queryFn: async () => {
      const response = await api.get("/payments?size=5");
      return response.data;
    },
    enabled: !!accountId,
  });

  const paymentMutation = useMutation({
    mutationFn: (data: CreatePaymentRequest) => {
      // FE-IDM-003: deterministic idempotency key so a safe retry of the same
      // bill payment reuses the same key (no duplicate mutation on timeout/retry).
      return api.post("/payments", data, {
        headers: {
          "X-Idempotency-Key": idempotencyKeyFor(
            "billing:pay",
            `${data.billerCode}:${data.customerId}:${data.amount}`,
          ),
        },
      });
    },
    onSuccess: () => {
      addToast(
        `Pembayaran ${selectedBiller?.name} sebesar ${formatCurrency(amount)} telah diproses.`,
        "success",
      );
      setSuccessData({
        billerName: selectedBiller?.name ?? "",
        billerIcon: selectedBiller?.icon ?? Zap,
        billerBg: selectedBiller?.bg ?? token.colorPrimaryBg,
        billerFg: selectedBiller?.fg ?? token.colorPrimaryText,
        customerId,
        amount,
        timestamp: new Date(),
      });
      setShowSuccess(true);
      setSelectedBiller(null);
      setCustomerId("");
      setAmount("");
    },
    onError: (error) => {
      console.error("Pembayaran gagal:", error);
      addToast(
        "Terjadi kesalahan saat memproses pembayaran. Silakan coba lagi.",
        "error",
      );
    },
  });

  const handlePay = () => {
    if (
      !selectedBiller ||
      !customerId ||
      !amount ||
      parseCurrencyExact(amount) === "0"
    ) {
      toast.warning("Silakan isi semua bidang yang diperlukan");
      return;
    }

    const data: CreatePaymentRequest = {
      accountId: authAccountId ?? "",
      billerCode: selectedBiller.code,
      customerId,
      amount: parseCurrencyExact(amount),
    };

    paymentMutation.mutate(data);
  };

  const recentBills = recentPayments?.content || [];

  if (showSuccess && successData) {
    const SuccessIcon = successData.billerIcon;
    return (
      <DashboardLayout>
        <SkipLink href="#main-content" />
        <main id="main-content" style={{ overflowX: "hidden" }}>
          <Flex
            vertical
            align="center"
            justify="center"
            style={{
              minHeight: "60vh",
              padding: "48px 16px",
              textAlign: "center",
            }}
          >
            <Flex vertical align="center" className="animate-fade-in">
              <Flex
                align="center"
                justify="center"
                style={{
                  width: 80,
                  height: 80,
                  marginBottom: 32,
                  borderRadius: "50%",
                  background: token.colorPrimaryBg,
                  color: token.colorPrimary,
                }}
              >
                <CheckCircle2 style={{ fontSize: 40 }} />
              </Flex>

              <Typography.Title level={2} style={{ marginBottom: 8 }}>
                Pembayaran Berhasil!
              </Typography.Title>
              <Typography.Text
                type="secondary"
                strong
                style={{ display: "block", marginBottom: 40, fontSize: 14 }}
              >
                Transaksi Anda telah diproses dengan aman.
              </Typography.Text>

              <Card style={{ width: "100%", maxWidth: 448, marginBottom: 40 }}>
                <Flex align="center" gap={16}>
                  <Flex
                    align="center"
                    justify="center"
                    style={{
                      width: 48,
                      height: 48,
                      borderRadius: 12,
                      background: successData.billerBg,
                      color: successData.billerFg,
                      boxShadow: token.boxShadowTertiary,
                    }}
                  >
                    <SuccessIcon style={{ fontSize: 24 }} />
                  </Flex>
                  <div style={{ textAlign: "left" }}>
                    <Typography.Text
                      type="secondary"
                      strong
                      style={{
                        display: "block",
                        marginBottom: 4,
                        fontSize: 12,
                        letterSpacing: "0.1em",
                        textTransform: "uppercase",
                      }}
                    >
                      Penyedia Layanan
                    </Typography.Text>
                    <Typography.Title level={4} style={{ margin: 0 }}>
                      {successData.billerName}
                    </Typography.Title>
                  </div>
                </Flex>

                <Divider />

                <Space direction="vertical" size={16} style={{ width: "100%" }}>
                  <Flex justify="space-between" align="center" gap={16}>
                    <Typography.Text
                      type="secondary"
                      strong
                      style={{
                        fontSize: 12,
                        letterSpacing: "0.1em",
                        textTransform: "uppercase",
                      }}
                    >
                      ID Pelanggan
                    </Typography.Text>
                    <Typography.Text strong style={{ fontSize: 14 }}>
                      {successData.customerId}
                    </Typography.Text>
                  </Flex>
                  <Flex justify="space-between" align="center" gap={16}>
                    <Typography.Text
                      type="secondary"
                      strong
                      style={{
                        fontSize: 12,
                        letterSpacing: "0.1em",
                        textTransform: "uppercase",
                      }}
                    >
                      Jumlah
                    </Typography.Text>
                    <Typography.Title
                      level={3}
                      style={{ margin: 0, fontVariantNumeric: "tabular-nums" }}
                    >
                      {formatCurrency(successData.amount)}
                    </Typography.Title>
                  </Flex>
                  <Flex justify="space-between" align="center" gap={16}>
                    <Typography.Text
                      type="secondary"
                      strong
                      style={{
                        fontSize: 12,
                        letterSpacing: "0.1em",
                        textTransform: "uppercase",
                      }}
                    >
                      Waktu
                    </Typography.Text>
                    <Typography.Text strong style={{ fontSize: 14 }}>
                      {successData.timestamp.toLocaleString("id-ID", {
                        day: "numeric",
                        month: "long",
                        year: "numeric",
                        hour: "2-digit",
                        minute: "2-digit",
                      })}
                    </Typography.Text>
                  </Flex>
                </Space>
              </Card>

              <Flex
                vertical={!isSm}
                gap={16}
                style={{ width: "100%", maxWidth: 448 }}
              >
                <Button
                  type="primary"
                  onClick={() => {
                    setShowSuccess(false);
                    setSuccessData(null);
                  }}
                  style={{ flex: isSm ? 1 : undefined, height: 56 }}
                >
                  Bayar Lagi
                </Button>
                <Button
                  type="default"
                  onClick={() => router.push("/dashboard")}
                  style={{ flex: isSm ? 1 : undefined, height: 56 }}
                >
                  Kembali ke Dashboard
                </Button>
              </Flex>
            </Flex>
          </Flex>
        </main>
      </DashboardLayout>
    );
  }

  if (selectedBiller) {
    return (
      <DashboardLayout>
        <SkipLink href="#main-content" />
        <main id="main-content" style={{ overflowX: "hidden" }}>
          <Space direction="vertical" size={24} style={{ width: "100%" }}>
            <Button
              type="default"
              onClick={() => setSelectedBiller(null)}
              aria-label="Kembali"
              icon={
                <ChevronRight
                  style={{ fontSize: 24, transform: "rotate(180deg)" }}
                />
              }
              style={{ width: 48, height: 48 }}
            />

            <Card style={{ position: "relative", overflow: "hidden" }}>
              <div
                aria-hidden="true"
                style={{
                  position: "absolute",
                  top: 0,
                  right: 0,
                  width: 256,
                  height: 256,
                  borderRadius: "50%",
                  background: selectedBiller.bg,
                  opacity: 0.1,
                  filter: "blur(48px)",
                  zIndex: 0,
                }}
              />

              <Flex
                align="center"
                gap={24}
                style={{ position: "relative", zIndex: 1 }}
              >
                <Flex
                  align="center"
                  justify="center"
                  style={{
                    width: 80,
                    height: 80,
                    borderRadius: 12,
                    background: selectedBiller.bg,
                    color: selectedBiller.fg,
                    boxShadow: token.boxShadowSecondary,
                  }}
                >
                  <selectedBiller.icon style={{ fontSize: 36 }} />
                </Flex>
                <div>
                  <Typography.Text
                    strong
                    style={{
                      display: "block",
                      marginBottom: 4,
                      fontSize: 12,
                      letterSpacing: "0.1em",
                      color: token.colorTextQuaternary,
                    }}
                  >
                    Penyedia Layanan
                  </Typography.Text>
                  <Typography.Title level={3} style={{ margin: 0 }}>
                    {selectedBiller.name}
                  </Typography.Title>
                  <Typography.Text
                    strong
                    style={{
                      fontSize: 12,
                      letterSpacing: "0.1em",
                      color: token.colorSuccessText,
                    }}
                  >
                    Mitra Pembayaran Resmi
                  </Typography.Text>
                </div>
              </Flex>

              <Divider />

              <Space
                direction="vertical"
                size={24}
                style={{ width: "100%", position: "relative", zIndex: 1 }}
              >
                <Flex vertical gap={8} style={{ width: "100%" }}>
                  <label
                    htmlFor="bills-customer-id"
                    style={{
                      fontSize: 12,
                      fontWeight: 700,
                      letterSpacing: "0.2em",
                      textTransform: "uppercase",
                      color: token.colorTextSecondary,
                    }}
                  >
                    ID Pelanggan / Nomor Rekening
                  </label>
                  <Input
                    id="bills-customer-id"
                    type="text"
                    value={customerId}
                    onChange={(e) => setCustomerId(e.target.value)}
                    placeholder="Masukkan ID unik Anda"
                    style={{ height: 64, fontSize: 20 }}
                  />
                </Flex>

                <Flex vertical gap={8} style={{ width: "100%" }}>
                  <label
                    htmlFor="bills-amount"
                    style={{
                      fontSize: 12,
                      fontWeight: 700,
                      letterSpacing: "0.2em",
                      textTransform: "uppercase",
                      color: token.colorTextSecondary,
                    }}
                  >
                    Jumlah Pembayaran (IDR)
                  </label>
                  <Input
                    id="bills-amount"
                    type="number"
                    value={amount}
                    onChange={(e) => setAmount(e.target.value)}
                    placeholder="0"
                    prefix={
                      <span
                        style={{
                          fontSize: 24,
                          fontWeight: 700,
                          color: token.colorTextQuaternary,
                        }}
                      >
                        Rp
                      </span>
                    }
                    style={{ height: 80, fontSize: 32 }}
                  />
                </Flex>
              </Space>
            </Card>

            <Flex vertical gap={24}>
              <Button
                type="primary"
                block
                onClick={handlePay}
                disabled={paymentMutation.isPending}
                style={{ height: 64 }}
              >
                {paymentMutation.isPending
                  ? "Sedang Memproses..."
                  : "Konfirmasi & Bayar Sekarang"}
              </Button>
              <Typography.Text
                type="secondary"
                strong
                style={{
                  display: "block",
                  textAlign: "center",
                  fontSize: 12,
                  letterSpacing: "0.1em",
                  textTransform: "uppercase",
                  opacity: 0.6,
                }}
              >
                Transaksi aman terenkripsi oleh Infrastruktur Protokol PayU
              </Typography.Text>
            </Flex>
          </Space>
        </main>
      </DashboardLayout>
    );
  }

  return (
    <DashboardLayout>
      <SkipLink href="#main-content" />
      <main id="main-content" style={{ overflowX: "hidden" }}>
        <Space direction="vertical" size={24} style={{ width: "100%" }}>
          <Flex
            vertical={!isSm}
            justify="space-between"
            align={isSm ? "flex-end" : "flex-start"}
            gap={16}
          >
            <div>
              <Typography.Title level={2} style={{ marginBottom: 0 }}>
                Tagihan & Top-up
              </Typography.Title>
              <Typography.Text type="secondary" strong style={{ fontSize: 14 }}>
                Bayar tagihan utilitas dan top up dompet digital Anda secara
                instan.
              </Typography.Text>
            </div>
            {isMd && (
              <Tag
                color="success"
                className="animate-pulse"
                style={{
                  margin: 0,
                  padding: "12px 24px",
                  borderRadius: 9999,
                  fontSize: 12,
                  fontWeight: 700,
                  letterSpacing: "0.1em",
                  textTransform: "uppercase",
                }}
              >
                Penyelesaian Real-time 24/7
              </Tag>
            )}
          </Flex>

          <Card style={{ position: "relative", overflow: "hidden" }}>
            <div
              aria-hidden="true"
              style={{
                position: "absolute",
                top: 0,
                right: 0,
                width: 256,
                height: 256,
                borderRadius: "50%",
                background: token.colorPrimaryBg,
                filter: "blur(48px)",
              }}
            />
            <Typography.Text
              type="secondary"
              strong
              style={{
                display: "block",
                marginBottom: 24,
                textAlign: "center",
                fontSize: 12,
                letterSpacing: "0.2em",
                textTransform: "uppercase",
                opacity: 0.6,
              }}
            >
              Kategori Layanan
            </Typography.Text>
            <Row gutter={[24, 24]} style={{ position: "relative", zIndex: 1 }}>
              {billers.map((item) => (
                <Col key={item.name} xs={8} sm={6}>
                  <Button
                    type="text"
                    onClick={() => setSelectedBiller(item)}
                    style={{
                      height: "auto",
                      width: "100%",
                      whiteSpace: "normal",
                    }}
                  >
                    <Flex vertical align="center" gap={16}>
                      <Flex
                        align="center"
                        justify="center"
                        style={{
                          width: 72,
                          height: 72,
                          borderRadius: 16,
                          background: item.bg,
                          color: item.fg,
                          boxShadow: token.boxShadowTertiary,
                        }}
                      >
                        <item.icon style={{ fontSize: 32 }} />
                      </Flex>
                      <Typography.Text
                        strong
                        style={{
                          fontSize: 12,
                          letterSpacing: "0.1em",
                          textTransform: "uppercase",
                        }}
                      >
                        {item.name}
                      </Typography.Text>
                    </Flex>
                  </Button>
                </Col>
              ))}
            </Row>
          </Card>

          <Space direction="vertical" size={24} style={{ width: "100%" }}>
            <Typography.Title level={4} style={{ margin: 0 }}>
              Aktivitas Terakhir
            </Typography.Title>
            {!isLoading && recentBills.length > 0 ? (
              <Row gutter={[24, 24]}>
                {recentBills.map((bill: PaymentResponse) => (
                  <Col key={bill.id} xs={24} md={12}>
                    <Card hoverable style={{ height: "100%" }}>
                      <Flex justify="space-between" align="center" gap={16}>
                        <Flex align="center" gap={24}>
                          <Flex
                            align="center"
                            justify="center"
                            style={{
                              width: 48,
                              height: 48,
                              borderRadius: 12,
                              background: token.colorPrimaryBg,
                              color: token.colorPrimary,
                            }}
                          >
                            <Zap style={{ fontSize: 24 }} />
                          </Flex>
                          <div>
                            <Typography.Text
                              strong
                              style={{
                                display: "block",
                                fontSize: 14,
                                letterSpacing: "0.05em",
                                textTransform: "uppercase",
                              }}
                            >
                              {bill.billerCode}
                            </Typography.Text>
                            <Typography.Text
                              type="secondary"
                              strong
                              style={{
                                display: "block",
                                marginTop: 4,
                                fontSize: 12,
                                letterSpacing: "0.2em",
                                textTransform: "uppercase",
                              }}
                            >
                              Ref: {bill.referenceNumber.slice(0, 10)}...
                            </Typography.Text>
                          </div>
                        </Flex>
                        <div style={{ textAlign: "right" }}>
                          <Typography.Text
                            strong
                            style={{
                              display: "block",
                              fontSize: 16,
                              fontVariantNumeric: "tabular-nums",
                            }}
                          >
                            {formatCurrency(bill.amount)}
                          </Typography.Text>
                          <Typography.Text
                            strong
                            style={{
                              display: "block",
                              marginTop: 4,
                              fontSize: 12,
                              letterSpacing: "0.2em",
                              textTransform: "uppercase",
                              color: token.colorPrimary,
                            }}
                          >
                            {bill.status}
                          </Typography.Text>
                        </div>
                      </Flex>
                    </Card>
                  </Col>
                ))}
              </Row>
            ) : (
              <Card
                style={{ borderStyle: "dashed", borderWidth: 2 }}
                styles={{ body: { padding: 48 } }}
              >
                <Flex vertical align="center" justify="center" gap={24}>
                  <LifeBuoy
                    style={{ fontSize: 64, color: token.colorTextQuaternary }}
                  />
                  <Typography.Text
                    type="secondary"
                    strong
                    style={{
                      maxWidth: 320,
                      textAlign: "center",
                      fontSize: 12,
                      letterSpacing: "0.2em",
                      textTransform: "uppercase",
                      opacity: 0.6,
                    }}
                  >
                    Pembayaran tagihan terakhir Anda akan muncul di sini.
                  </Typography.Text>
                </Flex>
              </Card>
            )}
          </Space>
        </Space>
      </main>
    </DashboardLayout>
  );
}
