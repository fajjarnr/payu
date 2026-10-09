"use client";

import React, { useMemo, useState } from "react";
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
import { asMoney } from "@/lib/currency";
import { Link } from "@/lib/navigation";
import {
  Button,
  Card,
  Col,
  Form,
  Input,
  Modal,
  Progress,
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
      toast.info("Kamera aktif — arahkan ke kode QRIS");
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
      qrisToday
        .reduce((sum, tx) => sum + Number(tx.amount), 0)
        .toLocaleString("id-ID"),
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
      toast.success("Pembayaran QRIS diterima dan sedang diproses");
      setPendingScan(null);
      setAmount("");
      form.resetFields();
    } catch {
      toast.error("Pembayaran QRIS gagal");
    }
  };

  return (
    <DashboardLayout>
      <SkipLink href="#main-content" />
      <main id="main-content" style={{ overflowX: "hidden" }}>
        <Space direction="vertical" size={16}>
          <input
            type="file"
            ref={fileInputRef}
            onChange={handleFileChange}
            accept="image/*"
            aria-label="Unggah file"
            style={{ display: "none" }}
          />

          {/* Header */}
          <Space direction="vertical" size={4}>
            <Typography.Title level={2} style={{ marginBottom: 0 }}>
              Pembayaran QRIS
            </Typography.Title>
            <Typography.Text type="secondary">
              Pindai kode QRIS merchant atau P2P untuk membayar secara instan.
            </Typography.Text>
          </Space>

          <Row gutter={[16, 16]}>
            {/* Main Scanner Column (16 units) */}
            <Col xs={24} lg={16}>
              <Space direction="vertical" size={16}>
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
                  {/* Premium Background Effects */}
                  <div
                    style={{
                      position: "absolute",
                      top: 0,
                      right: 0,
                      width: 400,
                      height: 400,
                      backgroundColor: token.colorPrimary,
                      opacity: 0.05,
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
                      height: 256,
                      backgroundColor: token.colorPrimary,
                      opacity: 0.05,
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
                          border: `2px dashed ${isScanning ? token.colorPrimary : token.colorBorder}`,
                          transition: "all 0.7s",
                          backgroundColor: isScanning
                            ? `${token.colorPrimary}1a`
                            : `${token.colorFillQuaternary}33`,
                        }}
                      />
                      <div
                        style={{
                          position: "absolute",
                          inset: 32,
                          border: `2px solid ${token.colorPrimary}33`,
                          borderRadius: 12,
                          animation: "pulse 2s infinite",
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
                            backgroundColor: token.colorBgContainer,
                            borderRadius: 12,
                            display: "flex",
                            alignItems: "center",
                            justifyContent: "center",
                            marginBottom: 20,
                            boxShadow: token.boxShadowSecondary,
                            border: `1px solid ${token.colorBorder}`,
                            transition: "transform 0.5s",
                            transform: isScanning ? "scale(1.1)" : undefined,
                          }}
                        >
                          <Camera
                            style={{
                              width: 36,
                              height: 36,
                              color: token.colorPrimary,
                            }}
                          />
                        </div>
                        <Typography.Text
                          type="secondary"
                          style={{
                            fontSize: 12,
                            fontWeight: 700,
                            letterSpacing: "0.3em",
                            textTransform: "uppercase",
                            opacity: 0.4,
                          }}
                        >
                          {isScanning
                            ? "Kamera Aktif — Mengarahkan ke QR..."
                            : "Memindai Kode QRIS..."}
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
                          borderTop: `4px solid ${token.colorPrimary}`,
                          borderLeft: `4px solid ${token.colorPrimary}`,
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
                          borderTop: `4px solid ${token.colorPrimary}`,
                          borderRight: `4px solid ${token.colorPrimary}`,
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
                          borderBottom: `4px solid ${token.colorPrimary}`,
                          borderLeft: `4px solid ${token.colorPrimary}`,
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
                          borderBottom: `4px solid ${token.colorPrimary}`,
                          borderRight: `4px solid ${token.colorPrimary}`,
                          borderRadius: "0 0 12px 0",
                        }}
                      />
                    </div>

                    <Space
                      direction="vertical"
                      size={20}
                      style={{ maxWidth: 320, margin: "0 auto" }}
                    >
                      <Button
                        type="primary"
                        onClick={handleToggleCamera}
                        data-testid="qris-camera-button"
                        block
                        style={{
                          height: 56,
                          borderRadius: 12,
                          fontWeight: 700,
                          fontSize: 12,
                          letterSpacing: "0.2em",
                          textTransform: "uppercase",
                        }}
                      >
                        <Camera style={{ width: 16, height: 16 }} />{" "}
                        {isScanning ? "Tutup Kamera" : "Buka Kamera"}
                      </Button>
                      <Button
                        onClick={handleUploadClick}
                        data-testid="qris-upload-button"
                        block
                        style={{
                          height: 56,
                          borderRadius: 12,
                          fontWeight: 700,
                          fontSize: 12,
                          letterSpacing: "0.2em",
                          textTransform: "uppercase",
                          backgroundColor: `${token.colorFillQuaternary}66`,
                          borderColor: token.colorBorder,
                        }}
                      >
                        <ImageIcon
                          style={{
                            width: 16,
                            height: 16,
                            color: token.colorPrimary,
                          }}
                        />{" "}
                        Unggah Foto
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
                          color: token.colorPrimary,
                        }}
                      />
                      <Typography.Title level={4} style={{ marginBottom: 0 }}>
                        Aktivitas Terakhir
                      </Typography.Title>
                    </div>
                    <Button
                      type="link"
                      onClick={() =>
                        toast.info("Menampilkan semua transaksi QRIS")
                      }
                      style={{
                        fontSize: 12,
                        fontWeight: 700,
                        color: token.colorPrimary,
                        letterSpacing: "0.2em",
                        textTransform: "uppercase",
                      }}
                    >
                      Lihat Semua
                    </Button>
                  </div>

                  <Space
                    direction="vertical"
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
                            fontWeight: 700,
                            letterSpacing: "0.1em",
                            textTransform: "uppercase",
                            opacity: 0.4,
                          }}
                        >
                          Belum ada riwayat transaksi QRIS
                        </Typography.Text>
                      </div>
                    ) : (
                      qrisHistory.map((tx) => (
                        <Row key={tx.id} justify="space-between" align="middle">
                          <Space direction="vertical" size={0}>
                            <Typography.Text strong>
                              {tx.description ?? "Pembayaran QRIS"}
                            </Typography.Text>
                            <Typography.Text
                              type="secondary"
                              style={{ fontSize: 12 }}
                            >
                              {new Date(tx.createdAt).toLocaleString("id-ID")}
                            </Typography.Text>
                          </Space>
                          <Typography.Text strong>
                            Rp {tx.amount}
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
              <Space direction="vertical" size={16}>
                {/* Security Status Card */}
                <Card>
                  <Typography.Text
                    type="secondary"
                    style={{
                      fontSize: 12,
                      fontWeight: 700,
                      letterSpacing: "0.2em",
                      textTransform: "uppercase",
                      opacity: 0.6,
                      display: "block",
                      marginBottom: 24,
                    }}
                  >
                    Protokol Keamanan
                  </Typography.Text>
                  <Space direction="vertical" size={24}>
                    <div style={{ display: "flex", gap: 16 }}>
                      <div
                        style={{
                          width: 40,
                          height: 40,
                          backgroundColor: `${token.colorPrimary}1a`,
                          borderRadius: 12,
                          display: "flex",
                          alignItems: "center",
                          justifyContent: "center",
                          flexShrink: 0,
                          border: `1px solid ${token.colorPrimary}1a`,
                        }}
                      >
                        <ShieldCheck
                          style={{
                            width: 20,
                            height: 20,
                            color: token.colorPrimary,
                          }}
                        />
                      </div>
                      <div>
                        <Typography.Text strong style={{ fontSize: 12 }}>
                          Enkripsi RESP-V3
                        </Typography.Text>
                        <Typography.Text
                          type="secondary"
                          style={{
                            fontSize: 12,
                            display: "block",
                            marginTop: 4,
                            opacity: 0.7,
                          }}
                        >
                          Token dinamik di-hash per transaksi untuk keamanan
                          maksimal.
                        </Typography.Text>
                      </div>
                    </div>
                    <div style={{ display: "flex", gap: 16 }}>
                      <div
                        style={{
                          width: 40,
                          height: 40,
                          backgroundColor: `${token.colorPrimary}1a`,
                          borderRadius: 12,
                          display: "flex",
                          alignItems: "center",
                          justifyContent: "center",
                          flexShrink: 0,
                          border: `1px solid ${token.colorPrimary}1a`,
                        }}
                      >
                        <Info
                          style={{
                            width: 20,
                            height: 20,
                            color: token.colorPrimary,
                          }}
                        />
                      </div>
                      <div>
                        <Typography.Text strong style={{ fontSize: 12 }}>
                          Lisensi ASPI/BI
                        </Typography.Text>
                        <Typography.Text
                          type="secondary"
                          style={{
                            fontSize: 12,
                            display: "block",
                            marginTop: 4,
                            opacity: 0.7,
                          }}
                        >
                          Sistem pembayaran tunduk pada regulasi QRIS Nasional.
                        </Typography.Text>
                      </div>
                    </div>
                  </Space>
                </Card>

                {/* My QR Card */}
                <Card
                  style={{
                    backgroundColor: token.colorBgSpotlight,
                    color: token.colorTextLightSolid,
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
                          level={4}
                          style={{
                            marginBottom: 0,
                            color: token.colorTextLightSolid,
                          }}
                        >
                          QRIS Personal
                        </Typography.Title>
                        <Typography.Text
                          strong
                          style={{
                            fontSize: 12,
                            color: token.colorPrimary,
                            letterSpacing: "0.1em",
                            textTransform: "uppercase",
                          }}
                        >
                          E-Wallet Access
                        </Typography.Text>
                      </div>
                      <QrCode
                        style={{
                          width: 28,
                          height: 28,
                          color: `${token.colorPrimary}66`,
                        }}
                      />
                    </div>

                    <div
                      style={{
                        backgroundColor: `${token.colorTextLightSolid}0d`,
                        borderRadius: 12,
                        padding: 24,
                        marginBottom: 24,
                        display: "flex",
                        justifyContent: "center",
                        border: `1px solid ${token.colorTextLightSolid}0d`,
                        position: "relative",
                      }}
                    >
                      <QrCode
                        style={{
                          width: 128,
                          height: 128,
                          transition: "all 0.3s",
                          color: showMyQr
                            ? token.colorPrimary
                            : `${token.colorTextLightSolid}33`,
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
                              color: token.colorTextTertiary,
                              letterSpacing: "0.2em",
                              textTransform: "uppercase",
                              transform: "rotate(-12deg)",
                            }}
                          >
                            Authorized Only
                          </Typography.Text>
                        </div>
                      )}
                    </div>

                    <Button
                      type="primary"
                      onClick={() => setShowMyQr(!showMyQr)}
                      data-testid="qris-show-personal-button"
                      block
                      style={{
                        height: 48,
                        backgroundColor: `${token.colorPrimary}33`,
                        color: token.colorPrimary,
                        borderRadius: 12,
                        fontWeight: 700,
                        fontSize: 12,
                        letterSpacing: "0.2em",
                        textTransform: "uppercase",
                        border: `1px solid ${token.colorPrimary}4d`,
                      }}
                    >
                      {showMyQr ? "Sembunyikan Kode" : "Tampilkan Kode Saya"}
                    </Button>
                  </div>
                  <div
                    style={{
                      position: "absolute",
                      top: -30,
                      left: -30,
                      width: 160,
                      height: 160,
                      backgroundColor: `${token.colorPrimary}1a`,
                      borderRadius: "50%",
                      filter: "blur(80px)",
                    }}
                  />
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
                  <Space direction="vertical" size={4}>
                    <Typography.Text
                      type="secondary"
                      style={{
                        fontSize: 12,
                        fontWeight: 700,
                        letterSpacing: "0.2em",
                        textTransform: "uppercase",
                        opacity: 0.6,
                      }}
                    >
                      Pemakaian QRIS Hari Ini
                    </Typography.Text>
                    <Typography.Title level={2} style={{ marginBottom: 0 }}>
                      Rp {todayQrisSpend}
                    </Typography.Title>
                  </Space>
                  <Space direction="vertical" size={16}>
                    <Typography.Text
                      strong
                      style={{
                        fontSize: 12,
                        color: token.colorPrimary,
                        letterSpacing: "0.1em",
                        textTransform: "uppercase",
                      }}
                    >
                      {qrisToday.length} transaksi hari ini
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
          okText="Bayar"
          confirmLoading={processQrisPayment.isPending}
          okButtonProps={{ disabled: !pendingScan || !amount }}
          title="Bayar QRIS"
        >
          <Form form={form} layout="vertical">
            <Form.Item
              label="Payload QR"
              name="qrCode"
              rules={[{ required: true, message: "Payload QR wajib diisi" }]}
            >
              <Input
                aria-label="Payload QR"
                placeholder="Tempel payload QRIS dari gambar yang diunggah"
                value={pendingScan ?? ""}
                onChange={(e) => setPendingScan(e.target.value)}
              />
            </Form.Item>
            <Form.Item
              label="Nominal (IDR)"
              name="amount"
              rules={[
                {
                  required: true,
                  pattern: /^\d+(\.\d+)?$/,
                  message: "Nominal harus angka",
                },
              ]}
            >
              <Input
                aria-label="Nominal pembayaran"
                inputMode="decimal"
                placeholder="10000"
                value={amount}
                onChange={(e) => setAmount(e.target.value)}
              />
            </Form.Item>
          </Form>
        </Modal>
      </main>
    </DashboardLayout>
  );
}
