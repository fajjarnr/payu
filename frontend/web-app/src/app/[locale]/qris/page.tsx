"use client";

import React, { useMemo, useState } from "react";
import { useLocale, useTranslations } from "next-intl";
import DashboardLayout from "@/components/DashboardLayout";
import { SkipLink } from "@/lib/a11y";
import {
  QrCode,
  Camera,
  History,
  Image as ImageIcon,
  ShieldCheck,
  Info,
} from "@/components/icons";
import { notify as toast } from "@/lib/notify";
import { useAuthStore } from "@/stores/authStore";
import { useProcessQrisPayment, useTransactions } from "@/hooks";
import { addCurrency, asMoney, formatCurrency } from "@/lib/currency";
import { INK_SURFACE, INK_SURFACE_SOFT, LIME_ACCENT, SAGE_ON_INK } from "@/theme/tokens";
import {
  Button,
  Card,
  Col,
  Form,
  Input,
  Modal,
  Row,
  Space,
  theme,
  Typography,
} from "antd";

// ponytail: minimal EMVCo CRC16 X25 (tag 63) — full TLV 26/30/54/59 + query GET /accounts/{id}/qris when backend live per ADR-0025
function _crc16X25(data: string): string {
  let crc = 0xffff;
  for (let i = 0; i < data.length; i++) {
    crc ^= data.charCodeAt(i) << 8;
    for (let j = 0; j < 8; j++)
      crc = crc & 0x8000 ? (crc << 1) ^ 0x1021 : crc << 1;
  }
  return ((crc ^ 0xffff) & 0xffff).toString(16).toUpperCase().padStart(4, "0");
}

export default function QRISPage() {
  const t = useTranslations("qris");
  const locale = useLocale();
  const bcp47Locale = locale === "id" ? "id-ID" : "en-US";
  const [isScanning, setIsScanning] = useState(false);
  const [showMyQr, setShowMyQr] = useState(false);
  const [pendingScan, setPendingScan] = useState<string | null>(null);
  const [amount, setAmount] = useState("");
  const [form] = Form.useForm();
  const fileInputRef = React.useRef<HTMLInputElement>(null);
  const { token } = theme.useToken();
  const { accountId } = useAuthStore();
  const processQrisPayment = useProcessQrisPayment();

  // QRIS payments are ordinary transactions tagged QRIS_PAYMENT; there is no
  // separate history endpoint, so filter the account feed.
  const { data: transactions } = useTransactions(accountId ?? undefined);
  const qrisHistory = useMemo(
    () =>
      (transactions ?? [])
        .filter((tx) => tx.type === "QRIS_PAYMENT")
        .slice(0, 5),
    [transactions],
  );

  const handleToggleCamera = () => {
    setIsScanning(!isScanning);
    if (!isScanning) {
      toast.info(t("cameraActive"));
    }
  };

  const qrisToday = useMemo(() => {
    const start = new Date();
    start.setHours(0, 0, 0, 0);
    return (transactions ?? []).filter(
      (tx) => tx.type === "QRIS_PAYMENT" && new Date(tx.createdAt) >= start,
    );
  }, [transactions]);

  const todayQrisSpend = useMemo(
    () =>
      qrisToday.reduce(
        (sum, tx) => addCurrency(sum, tx.amount),
        asMoney("0"),
      ),
    [qrisToday],
  );

  const handleUploadClick = () => {
    fileInputRef.current?.click();
  };

  const handleFileChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;
    setIsScanning(true);
    // No QR decoder runs in the browser here, so the raw QR payload cannot be
    // extracted from the image. Ask for it explicitly and send it to the real
    // POST /transactions/qris/pay instead of faking a merchant match.
    setPendingScan("");
    setIsScanning(false);
    e.target.value = "";
  };

  const handlePayQris = async () => {
    if (!pendingScan || !amount) return;
    try {
      await processQrisPayment.mutateAsync({
        qrCode: pendingScan,
        amount: asMoney(amount),
        accountId: accountId ?? "",
      });
      toast.success(t("paymentProcessing"));
      setPendingScan(null);
      setAmount("");
      form.resetFields();
    } catch {
      toast.error(t("paymentFailed"));
    }
  };

  return (
    <DashboardLayout>
      <SkipLink href="#main-content" />
      <div style={{ overflowX: "hidden" }}>
        <style>{`@keyframes qris-pulse { 0%, 100% { opacity: 1; } 50% { opacity: 0.4; } } .qris-scan-ring { animation: qris-pulse 2s ease-in-out infinite; } @media (prefers-reduced-motion: reduce) { .qris-scan-ring { animation: none; } }`}</style>
        <Space orientation="vertical" size={16}>
          <input
            type="file"
            ref={fileInputRef}
            onChange={handleFileChange}
            accept="image/*"
            aria-label={t("uploadFile")}
            style={{ display: "none" }}
          />

          {/* Header */}
          <Space orientation="vertical" size={4}>
            <Typography.Title level={1} style={{ marginBottom: 0 }}>
              {t("title")}
            </Typography.Title>
            <Typography.Text type="secondary">{t("subtitle")}</Typography.Text>
          </Space>

          <Row gutter={[16, 16]}>
            {/* Main Scanner Column (16 units) */}
            <Col xs={24} lg={16}>
              <Space orientation="vertical" size={16}>
                {/* Scanner Card */}
                <Card
                  style={{
                    minHeight: 320,
                    position: "relative",
                    overflow: "hidden",
                  }}
                  styles={{
                    body: {
                      minHeight: 320,
                      display: "flex",
                      flexDirection: "column",
                      alignItems: "center",
                      justifyContent: "center",
                    },
                  }}
                >
                  {/* Sage background effects (surface contrast, not glow) */}
                  <div
                    style={{
                      position: "absolute",
                      top: 0,
                      right: 0,
                      width: 400,
                      maxWidth: "100%",
                      height: 400,
                      backgroundColor: token.colorBgLayout,
                      borderRadius: "50%",
                      filter: "blur(100px)",
                    }}
                  />
                  <div
                    style={{
                      position: "absolute",
                      bottom: 0,
                      left: 0,
                      width: 256,
                      maxWidth: "100%",
                      height: 256,
                      backgroundColor: token.colorBgLayout,
                      borderRadius: "50%",
                      filter: "blur(80px)",
                    }}
                  />

                  <div
                    style={{
                      position: "relative",
                      zIndex: 10,
                      width: "100%",
                      maxWidth: 400,
                      textAlign: "center",
                    }}
                  >
                    <div
                      style={{
                        position: "relative",
                        aspectRatio: "1",
                        maxWidth: 350,
                        margin: "0 auto",
                      }}
                    >
                      {/* Scanner Frame */}
                      <div
                        style={{
                          position: "absolute",
                          inset: 0,
                          borderRadius: 12,
                          border: `2px dashed ${isScanning ? token.colorPrimaryText : token.colorBorder}`,
                          transition: "all 0.7s",
                          backgroundColor: isScanning ? token.colorPrimaryBg : token.colorFillQuaternary,
                        }}
                      />
                      <div
                        className="qris-scan-ring"
                        style={{
                          position: "absolute",
                          inset: 32,
                          border: `2px solid ${token.colorPrimaryBorder}`,
                          borderRadius: 12,
                        }}
                      />

                      {/* Floating Scanner Icon */}
                      <div
                        style={{
                          position: "absolute",
                          inset: 0,
                          display: "flex",
                          flexDirection: "column",
                          alignItems: "center",
                          justifyContent: "center",
                        }}
                      >
                        <div
                          style={{
                            width: 88,
                            height: 88,
                            backgroundColor: token.colorPrimaryBg,
                            borderRadius: 24,
                            display: "flex",
                            alignItems: "center",
                            justifyContent: "center",
                            marginBottom: 20,
                            border: `1px solid ${token.colorPrimaryBorder}`,
                            transition: "transform 0.5s",
                            transform: isScanning ? "scale(1.1)" : undefined,
                          }}
                        >
                          <Camera
                            style={{
                              width: 36,
                              height: 36,
                              color: token.colorPrimaryText,
                            }}
                          />
                        </div>
                        <Typography.Text
                          type="secondary"
                          style={{
                            fontSize: 12,
                            fontWeight: 600,
                            letterSpacing: "0.2em",
                            textTransform: "uppercase",
                            color: token.colorTextDescription,
                          }}
                        >
                          {isScanning
                            ? t("cameraActiveHint")
                            : t("scanningHint")}
                        </Typography.Text>
                      </div>

                      {/* Corner Borders */}
                      <div
                        style={{
                          position: "absolute",
                          top: 0,
                          left: 0,
                          width: 40,
                          height: 40,
                          borderTop: `4px solid ${token.colorPrimaryText}`,
                          borderLeft: `4px solid ${token.colorPrimaryText}`,
                          borderRadius: "12px 0 0 0",
                        }}
                      />
                      <div
                        style={{
                          position: "absolute",
                          top: 0,
                          right: 0,
                          width: 40,
                          height: 40,
                          borderTop: `4px solid ${token.colorPrimaryText}`,
                          borderRight: `4px solid ${token.colorPrimaryText}`,
                          borderRadius: "0 12px 0 0",
                        }}
                      />
                      <div
                        style={{
                          position: "absolute",
                          bottom: 0,
                          left: 0,
                          width: 40,
                          height: 40,
                          borderBottom: `4px solid ${token.colorPrimaryText}`,
                          borderLeft: `4px solid ${token.colorPrimaryText}`,
                          borderRadius: "0 0 0 12px",
                        }}
                      />
                      <div
                        style={{
                          position: "absolute",
                          bottom: 0,
                          right: 0,
                          width: 40,
                          height: 40,
                          borderBottom: `4px solid ${token.colorPrimaryText}`,
                          borderRight: `4px solid ${token.colorPrimaryText}`,
                          borderRadius: "0 0 12px 0",
                        }}
                      />
                    </div>

                    <Space
                      orientation="vertical"
                      size={20}
                      style={{ maxWidth: 320, margin: "0 auto" }}
                    >
                      <Button
                        type="primary"
                        onClick={handleToggleCamera}
                        data-testid="qris-camera-button"
                        block
                        size="large"
                      >
                        <Camera style={{ width: 16, height: 16 }} />{" "}
                        {isScanning ? t("closeCamera") : t("openCamera")}
                      </Button>
                      <Button
                        onClick={handleUploadClick}
                        data-testid="qris-upload-button"
                        block
                        size="large"
                        style={{
                          backgroundColor: token.colorBgLayout,
                          borderColor: token.colorBorder,
                        }}
                      >
                        <ImageIcon
                          style={{
                            width: 16,
                            height: 16,
                            color: token.colorPrimaryText,
                          }}
                        />{" "}
                        {t("uploadPhoto")}
                      </Button>
                    </Space>
                  </div>
                </Card>

                {/* Recent Payments Section */}
                <Card>
                  <div
                    style={{
                      display: "flex",
                      justifyContent: "space-between",
                      alignItems: "center",
                      marginBottom: 24,
                    }}
                  >
                    <div
                      style={{ display: "flex", alignItems: "center", gap: 12 }}
                    >
                      <History
                        style={{
                          width: 20,
                          height: 20,
                          color: token.colorPrimaryText,
                        }}
                      />
                      <Typography.Title level={3} style={{ marginBottom: 0 }}>
                        {t("recentActivity")}
                      </Typography.Title>
                    </div>
                    <Button
                      type="link"
                      onClick={() => toast.info(t("showingAllTransactions"))}
                      style={{
                        fontSize: 14,
                        fontWeight: 600,
                        color: token.colorPrimaryText,
                      }}
                    >
                      {t("viewAll")}
                    </Button>
                  </div>

                  <Space
                    orientation="vertical"
                    size={16}
                    style={{ width: "100%" }}
                  >
                    {qrisHistory.length === 0 ? (
                      <div
                        style={{
                          padding: 32,
                          textAlign: "center",
                          backgroundColor: `${token.colorFillQuaternary}1a`,
                          borderRadius: 12,
                          border: `1px dashed ${token.colorBorder}80`,
                        }}
                      >
                        <div
                          style={{
                            width: 64,
                            height: 64,
                            backgroundColor: `${token.colorFillQuaternary}33`,
                            borderRadius: "50%",
                            display: "flex",
                            alignItems: "center",
                            justifyContent: "center",
                            margin: "0 auto 16px",
                            opacity: 0.3,
                          }}
                        >
                          <History
                            style={{
                              width: 32,
                              height: 32,
                              color: token.colorText,
                            }}
                          />
                        </div>
                        <Typography.Text
                          type="secondary"
                          style={{
                            fontSize: 12,
                            fontWeight: 600,
                            letterSpacing: "0.1em",
                            textTransform: "uppercase",
                            color: token.colorTextDescription,
                          }}
                        >
                          {t("noHistory")}
                        </Typography.Text>
                      </div>
                    ) : (
                      qrisHistory.map((tx) => (
                        <Row key={tx.id} justify="space-between" align="middle">
                          <Space orientation="vertical" size={0}>
                            <Typography.Text strong>
                              {tx.description ?? t("paymentLabel")}
                            </Typography.Text>
                            <Typography.Text
                              type="secondary"
                              style={{ fontSize: 12 }}
                            >
                              {new Date(tx.createdAt).toLocaleString(
                                bcp47Locale,
                              )}
                            </Typography.Text>
                          </Space>
                          <Typography.Text
                            strong
                            style={{ fontVariantNumeric: "tabular-nums" }}
                          >
                            {formatCurrency(tx.amount, {
                              locale: bcp47Locale,
                            })}
                          </Typography.Text>
                        </Row>
                      ))
                    )}
                  </Space>
                </Card>
              </Space>
            </Col>

            {/* Right Sidebar Column (8 units) */}
            <Col xs={24} lg={8}>
              <Space orientation="vertical" size={16}>
                {/* Security Status Card */}
                <Card>
                  <Typography.Text
                    type="secondary"
                    style={{
                      fontSize: 12,
                      fontWeight: 600,
                      letterSpacing: "0.1em",
                      textTransform: "uppercase",
                      color: token.colorTextDescription,
                      display: "block",
                      marginBottom: 24,
                    }}
                  >
                    {t("securityProtocol")}
                  </Typography.Text>
                  <Space orientation="vertical" size={24}>
                    <div style={{ display: "flex", gap: 16 }}>
                      <div
                        style={{
                          width: 40,
                          height: 40,
                          backgroundColor: token.colorPrimaryBg,
                          borderRadius: 12,
                          display: "flex",
                          alignItems: "center",
                          justifyContent: "center",
                          flexShrink: 0,
                          border: `1px solid ${token.colorPrimaryBorder}`,
                        }}
                      >
                        <ShieldCheck
                          style={{
                            width: 20,
                            height: 20,
                            color: token.colorPrimaryText,
                          }}
                        />
                      </div>
                      <div>
                        <Typography.Text strong style={{ fontSize: 12 }}>
                          {t("respV3Encryption")}
                        </Typography.Text>
                        <Typography.Text
                          type="secondary"
                          style={{
                            fontSize: 12,
                            display: "block",
                            marginTop: 4,
                            color: token.colorTextDescription,
                          }}
                        >
                          {t("dynamicTokenDescription")}
                        </Typography.Text>
                      </div>
                    </div>
                    <div style={{ display: "flex", gap: 16 }}>
                      <div
                        style={{
                          width: 40,
                          height: 40,
                          backgroundColor: token.colorPrimaryBg,
                          borderRadius: 12,
                          display: "flex",
                          alignItems: "center",
                          justifyContent: "center",
                          flexShrink: 0,
                          border: `1px solid ${token.colorPrimaryBorder}`,
                        }}
                      >
                        <Info
                          style={{
                            width: 20,
                            height: 20,
                            color: token.colorPrimaryText,
                          }}
                        />
                      </div>
                      <div>
                        <Typography.Text strong style={{ fontSize: 12 }}>
                          {t("aspiLicense")}
                        </Typography.Text>
                        <Typography.Text
                          type="secondary"
                          style={{
                            fontSize: 12,
                            display: "block",
                            marginTop: 4,
                            color: token.colorTextDescription,
                          }}
                        >
                          {t("qrisRegulationDescription")}
                        </Typography.Text>
                      </div>
                    </div>
                  </Space>
                </Card>

                {/* My QR Card: Wise card-feature-dark — ink fill, lime text. */}
                <Card
                  style={{
                    backgroundColor: INK_SURFACE,
                    color: LIME_ACCENT,
                    position: "relative",
                    overflow: "hidden",
                  }}
                >
                  <div style={{ position: "relative", zIndex: 10 }}>
                    <div
                      style={{
                        display: "flex",
                        justifyContent: "space-between",
                        alignItems: "flex-start",
                        marginBottom: 24,
                      }}
                    >
                      <div>
                        <Typography.Title
                          level={2}
                          style={{
                            marginBottom: 0,
                            color: LIME_ACCENT,
                          }}
                        >
                          {t("personalQris")}
                        </Typography.Title>
                        <Typography.Text
                          strong
                          style={{
                            fontSize: 12,
                            color: SAGE_ON_INK,
                            letterSpacing: "0.1em",
                            textTransform: "uppercase",
                          }}
                        >
                          {t("eWalletAccess")}
                        </Typography.Text>
                      </div>
                      <QrCode
                        style={{
                          width: 28,
                          height: 28,
                          color: LIME_ACCENT,
                        }}
                      />
                    </div>

                    <div
                      style={{
                        backgroundColor: INK_SURFACE_SOFT,
                        borderRadius: 16,
                        padding: 24,
                        marginBottom: 24,
                        display: "flex",
                        justifyContent: "center",
                        position: "relative",
                      }}
                    >
                      <QrCode
                        style={{
                          width: 128,
                          height: 128,
                          transition: "all 0.3s",
                          color: showMyQr ? LIME_ACCENT : SAGE_ON_INK,
                          opacity: showMyQr ? 1 : 0.25,
                          transform: showMyQr ? "scale(1.05)" : undefined,
                        }}
                      />
                      {!showMyQr && (
                        <div
                          style={{
                            position: "absolute",
                            inset: 0,
                            display: "flex",
                            alignItems: "center",
                            justifyContent: "center",
                          }}
                        >
                          <Typography.Text
                            strong
                            style={{
                              fontSize: 12,
                              color: SAGE_ON_INK,
                              letterSpacing: "0.2em",
                              textTransform: "uppercase",
                              transform: "rotate(-12deg)",
                            }}
                          >
                            {t("authorizedOnly")}
                          </Typography.Text>
                        </div>
                      )}
                    </div>

                    <Button
                      type="primary"
                      onClick={() => setShowMyQr(!showMyQr)}
                      data-testid="qris-show-personal-button"
                      block
                      size="large"
                    >
                      {showMyQr ? t("hideCode") : t("showMyCode")}
                    </Button>
                  </div>
                </Card>

                {/* Daily Limit Card */}
                <Card
                  styles={{
                    body: {
                      minHeight: 180,
                      display: "flex",
                      flexDirection: "column",
                      justifyContent: "space-between",
                    },
                  }}
                >
                  <Space orientation="vertical" size={4}>
                    <Typography.Text
                      type="secondary"
                      style={{
                        fontSize: 12,
                        fontWeight: 600,
                        letterSpacing: "0.1em",
                        textTransform: "uppercase",
                        color: token.colorTextDescription,
                      }}
                    >
                      {t("todayUsage")}
                    </Typography.Text>
                    <Typography.Title
                      level={2}
                      style={{ marginBottom: 0, fontVariantNumeric: "tabular-nums" }}
                    >
                      {formatCurrency(todayQrisSpend, {
                        locale: bcp47Locale,
                      })}
                    </Typography.Title>
                  </Space>
                  <Space orientation="vertical" size={16}>
                    <Typography.Text
                      strong
                      style={{
                        fontSize: 12,
                        color: token.colorPrimaryText,
                        letterSpacing: "0.1em",
                        textTransform: "uppercase",
                      }}
                    >
                      {t("todayTransactions", { count: qrisToday.length })}
                    </Typography.Text>
                  </Space>
                </Card>
              </Space>
            </Col>
          </Row>
        </Space>

        <Modal
          open={pendingScan !== null}
          onCancel={() => {
            setPendingScan(null);
            form.resetFields();
          }}
          onOk={handlePayQris}
          okText={t("pay")}
          confirmLoading={processQrisPayment.isPending}
          okButtonProps={{ disabled: !pendingScan || !amount }}
          title={t("payTitle")}
        >
          <Form form={form} layout="vertical">
            <Form.Item
              label={t("qrPayload")}
              name="qrCode"
              rules={[{ required: true, message: t("qrPayloadRequired") }]}
            >
              <Input
                aria-label={t("qrPayload")}
                placeholder={t("qrPayloadPlaceholder")}
                value={pendingScan ?? ""}
                onChange={(e) => setPendingScan(e.target.value)}
              />
            </Form.Item>
            <Form.Item
              label={t("amountLabel")}
              name="amount"
              rules={[
                {
                  required: true,
                  pattern: /^\d+(\.\d+)?$/,
                  message: t("amountMustBeNumber"),
                },
              ]}
            >
              <Input
                aria-label={t("amountAriaLabel")}
                inputMode="decimal"
                placeholder="10000"
                value={amount}
                onChange={(e) => setAmount(e.target.value)}
              />
            </Form.Item>
          </Form>
        </Modal>
      </div>
    </DashboardLayout>
  );
}
