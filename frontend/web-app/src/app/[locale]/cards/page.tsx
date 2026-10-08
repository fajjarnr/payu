"use client";

/* eslint-disable no-restricted-syntax -- display percentage uses Number for chart width, not Money arithmetic (ADR-0047 display only) */

import React, { useState } from "react";
import DashboardLayout from "@/components/DashboardLayout";
import { Link } from "@/lib/navigation";
import { SkipLink } from "@/lib/a11y";
import {
  Eye,
  EyeOff,
  Lock,
  RefreshCw,
  Sliders,
  ShieldCheck,
  Zap,
  Plus,
  Loader2,
  Trash2,
  Settings,
} from "@/components/icons";
import {
  Alert,
  Button,
  Card,
  Col,
  Flex,
  Input,
  Modal,
  Progress,
  Row,
  Space,
  Switch,
  Tag,
  Typography,
  theme,
} from "antd";
import {
  useCards,
  useFreezeCard,
  useUnfreezeCard,
  useCreateCard,
  useDeleteCard,
  useUpdateCard,
} from "@/hooks";
import { useAuthStore } from "@/stores/authStore";
import { asMoney, parseCurrencyExact } from "@/lib/currency";
import type { VirtualCard } from "@/services/WalletService";

// Extended card properties that may come from backend but aren't in the base interface yet
interface ExtendedCardData extends VirtualCard {
  monthlyLimit?: number;
  dailySpent?: number;
  onlineEnabled?: boolean;
  internationalEnabled?: boolean;
  subscriptionEnabled?: boolean;
  atmEnabled?: boolean;
}

interface CardData {
  id: string;
  cardNumber: string;
  expiryDate: string;
  cardHolder: string;
  status: string;
}

export default function CardsPage() {
  const { token } = theme.useToken();
  const { accountId: authAccountId, user } = useAuthStore();
  const [showFullDetails, setShowFullDetails] = useState(false);
  const [isLimitModalOpen, setIsLimitModalOpen] = useState(false);
  const [isDeleteModalOpen, setIsDeleteModalOpen] = useState(false);
  const [selectedCard, setSelectedCard] = useState<VirtualCard | null>(null);
  const [limitForm, setLimitForm] = useState({
    dailyLimit: 25000000,
    monthlyLimit: 100000000,
  });

  const { data: cardsData } = useCards(authAccountId ?? undefined);
  const freezeCard = useFreezeCard();
  const unfreezeCard = useUnfreezeCard();
  const createCard = useCreateCard();
  const deleteCard = useDeleteCard();
  const updateCard = useUpdateCard();

  // Card payload from the API; the card serializer returns `cardHolder`, which
  // the shared VirtualCard type has not caught up with yet.
  const cards = (cardsData ?? []) as unknown as CardData[];
  // Backend sends extra card flags beyond the base VirtualCard type.
  const extendedPrimary = cardsData?.[0] as ExtendedCardData | undefined;
  const primaryCard = cards[0];
  const cardNumber =
    primaryCard?.cardNumber ??
    "\u2022\u2022\u2022\u2022 \u2022\u2022\u2022\u2022 \u2022\u2022\u2022\u2022 \u2022\u2022\u2022\u2022";
  const cardExpiry = primaryCard?.expiryDate ?? "--/--";
  // Placeholder glyphs are display-only; the create call needs a real holder name.
  const cardOwner =
    primaryCard?.cardHolder ?? user?.fullName ?? user?.username ?? "";
  const cardLast4 = cardNumber.slice(-4);
  const isFrozen = primaryCard?.status === "FROZEN";

  const dailySpent = extendedPrimary?.dailySpent ?? 0;
  const rawPercent =
    limitForm.dailyLimit > 0
      ? Math.round((dailySpent / limitForm.dailyLimit) * 100)
      : 0;
  const dailyLimitPercent = Math.min(100, rawPercent);

  const handleOpenLimitModal = () => {
    if (cardsData && cardsData.length > 0) {
      const card = cardsData[0];
      setSelectedCard(card);
      setLimitForm({
        dailyLimit: card.dailyLimit ? Number(card.dailyLimit) : 25000000,
        monthlyLimit: extendedPrimary?.monthlyLimit || 100000000,
      });
      setIsLimitModalOpen(true);
    }
  };

  const handleOpenDeleteModal = () => {
    if (cardsData && cardsData.length > 0) {
      setSelectedCard(cardsData[0]);
      setIsDeleteModalOpen(true);
    }
  };

  const handleUpdateLimit = async () => {
    if (!selectedCard || parseCurrencyExact(limitForm.dailyLimit) === "0")
      return;

    await updateCard.mutateAsync({
      cardId: selectedCard.id,
      data: {
        dailyLimit: parseCurrencyExact(limitForm.dailyLimit),
      },
    });

    setIsLimitModalOpen(false);
  };

  const handleDeleteCard = async () => {
    if (!selectedCard) return;

    await deleteCard.mutateAsync(selectedCard.id);
    setIsDeleteModalOpen(false);
  };

  const operations = [
    {
      label: "Transaksi Online",
      desc: "Situs web & retail",
      icon: Zap,
      status: extendedPrimary?.onlineEnabled ?? false,
      tag: "REKOMENDASI",
    },
    {
      label: "Internasional",
      desc: "Transaksi lintas negara",
      icon: ShieldCheck,
      status: extendedPrimary?.internationalEnabled ?? false,
      tag: "AMAN",
    },
    {
      label: "Langganan",
      desc: "Merchant & auto-debit",
      icon: RefreshCw,
      status: extendedPrimary?.subscriptionEnabled ?? false,
      tag: "AKTIF",
    },
    {
      label: "Penarikan ATM",
      desc: "Izin tarik tunai fisik",
      icon: Sliders,
      status: extendedPrimary?.atmEnabled ?? false,
      tag: "BLOKIR",
    },
  ];

  return (
    <DashboardLayout>
      <SkipLink href="#main-content" />
      <main id="main-content">
        <Space direction="vertical" size={24} style={{ width: "100%" }}>
          {/* Header */}
          <Flex justify="space-between" align="flex-end" gap={16} wrap>
            <div>
              <Typography.Title level={2} style={{ marginBottom: 4 }}>
                Kartu Virtual
              </Typography.Title>
              <Typography.Text type="secondary">
                Pembayaran online yang aman dengan rincian kartu instan.
              </Typography.Text>
            </div>
            <Button
              type="primary"
              onClick={() =>
                createCard.mutate({
                  accountId: authAccountId ?? "",
                  cardHolderName: cardOwner || "CARD HOLDER",
                  dailyLimit: asMoney("25000000.0000"),
                })
              }
              disabled={createCard.isPending}
              icon={createCard.isPending ? <Loader2 spin /> : <Plus />}
            >
              Kartu Baru
            </Button>
          </Flex>

          {/* Top Hero Section: Card Visualization & Limits */}
          <Row gutter={[16, 16]} align="stretch">
            {/* Left: Digital Card & Primary Actions */}
            <Col xs={24} lg={16}>
              <Card style={{ height: "100%" }}>
                <Space
                  direction="vertical"
                  size={24}
                  style={{ width: "100%" }}
                  align="center"
                >
                  {/* Digital Card Visualization */}
                  <div
                    style={{
                      width: "100%",
                      maxWidth: 440,
                      aspectRatio: "1.586 / 1",
                      borderRadius: 16,
                      position: "relative",
                      overflow: "hidden",
                      boxShadow: "0 25px 50px -12px rgba(0, 0, 0, 0.35)",
                    }}
                  >
                    <div
                      style={{
                        position: "absolute",
                        inset: 0,
                        background:
                          "linear-gradient(135deg, hsl(var(--primary)), var(--color-primary-dark))",
                      }}
                    />
                    <div
                      style={{
                        position: "absolute",
                        top: -32,
                        right: -40,
                        width: 256,
                        height: 256,
                        borderRadius: "50%",
                        background: "rgba(255,255,255,0.2)",
                        filter: "blur(48px)",
                      }}
                    />
                    <Flex
                      vertical
                      justify="space-between"
                      style={{
                        position: "relative",
                        zIndex: 1,
                        height: "100%",
                        padding: 24,
                        color: "#fff",
                      }}
                    >
                      <Flex justify="space-between" align="flex-start">
                        <Flex align="center" gap={12}>
                          <Flex
                            align="center"
                            justify="center"
                            style={{
                              width: 40,
                              height: 40,
                              borderRadius: 12,
                              background: "rgba(255,255,255,0.2)",
                              border: "1px solid rgba(255,255,255,0.2)",
                              fontWeight: 700,
                              fontSize: 20,
                            }}
                          >
                            U
                          </Flex>
                          <Typography.Text
                            style={{
                              color: "#fff",
                              fontSize: 20,
                              fontWeight: 700,
                              letterSpacing: "-0.02em",
                            }}
                          >
                            PayU
                          </Typography.Text>
                        </Flex>
                        <span
                          aria-hidden="true"
                          style={{
                            width: 8,
                            height: 8,
                            borderRadius: "50%",
                            background: "#fff",
                            boxShadow: "0 0 15px rgba(255,255,255,1)",
                          }}
                        />
                      </Flex>

                      <Space direction="vertical" size={16}>
                        <Typography.Text
                          style={{
                            color: "#fff",
                            fontSize: 24,
                            fontWeight: 700,
                            letterSpacing: "0.25em",
                            fontFamily: "monospace",
                            lineHeight: 1,
                            fontVariantNumeric: "tabular-nums",
                          }}
                        >
                          {showFullDetails
                            ? cardNumber
                            : `•••• •••• •••• ${cardLast4}`}
                        </Typography.Text>
                        <Flex justify="space-between" align="flex-end">
                          <Space direction="vertical" size={2}>
                            <Typography.Text
                              style={{
                                color: "rgba(255,255,255,0.5)",
                                fontSize: 12,
                                fontWeight: 700,
                                letterSpacing: "0.1em",
                                textTransform: "uppercase",
                              }}
                            >
                              Owner
                            </Typography.Text>
                            <Typography.Text
                              ellipsis
                              style={{
                                color: "#fff",
                                maxWidth: 150,
                                fontSize: 12,
                                fontWeight: 700,
                                letterSpacing: "0.1em",
                                textTransform: "uppercase",
                              }}
                            >
                              {cardOwner}
                            </Typography.Text>
                          </Space>
                          <Space direction="vertical" size={2} align="end">
                            <Typography.Text
                              style={{
                                color: "rgba(255,255,255,0.5)",
                                fontSize: 12,
                                fontWeight: 700,
                                letterSpacing: "0.1em",
                                textTransform: "uppercase",
                              }}
                            >
                              Exp
                            </Typography.Text>
                            <Typography.Text
                              style={{
                                color: "#fff",
                                fontFamily: "monospace",
                                fontWeight: 700,
                                fontSize: 12,
                              }}
                            >
                              {cardExpiry}
                            </Typography.Text>
                          </Space>
                        </Flex>
                      </Space>
                    </Flex>
                  </div>

                  {/* Centered Actions */}
                  <Row
                    gutter={[16, 16]}
                    style={{ width: "100%", maxWidth: 440 }}
                  >
                    <Col span={12}>
                      <Button
                        type="primary"
                        block
                        onClick={() => setShowFullDetails(!showFullDetails)}
                        icon={showFullDetails ? <EyeOff /> : <Eye />}
                      >
                        Detail Kartu
                      </Button>
                    </Col>
                    <Col span={12}>
                      <Button
                        block
                        onClick={() =>
                          primaryCard?.id &&
                          (isFrozen
                            ? unfreezeCard.mutate(primaryCard.id)
                            : freezeCard.mutate(primaryCard.id))
                        }
                        icon={<Lock />}
                      >
                        {isFrozen ? "Aktifkan" : "Bekukan"}
                      </Button>
                    </Col>
                    <Col span={12}>
                      <Button
                        block
                        onClick={handleOpenLimitModal}
                        icon={<Settings />}
                      >
                        Ubah Limit
                      </Button>
                    </Col>
                    <Col span={12}>
                      <Button
                        block
                        danger
                        onClick={handleOpenDeleteModal}
                        disabled={deleteCard.isPending}
                        icon={
                          deleteCard.isPending ? <Loader2 spin /> : <Trash2 />
                        }
                      >
                        Hapus Kartu
                      </Button>
                    </Col>
                  </Row>
                </Space>
              </Card>
            </Col>

            {/* Right: Daily Limit */}
            <Col xs={24} lg={8}>
              <Card
                style={{
                  height: "100%",
                  background: token.colorText,
                  color: token.colorBgContainer,
                  border: "none",
                  boxShadow: "0 20px 25px -5px rgba(0,0,0,0.15)",
                }}
              >
                <Flex justify="space-between" align="center">
                  <Typography.Title
                    level={4}
                    style={{ marginBottom: 0, color: token.colorBgContainer }}
                  >
                    Limit Harian
                  </Typography.Title>
                  <Button
                    shape="circle"
                    style={{
                      minWidth: 44,
                      minHeight: 44,
                      background: `${token.colorBgContainer}0D`,
                      borderColor: `${token.colorBgContainer}1A`,
                      color: token.colorBgContainer,
                    }}
                    onClick={handleOpenLimitModal}
                    aria-label="Pengaturan kartu"
                    icon={<Sliders />}
                  />
                </Flex>

                <Space
                  direction="vertical"
                  size={24}
                  style={{ width: "100%", marginTop: 24 }}
                >
                  <div>
                    <Typography.Text
                      style={{
                        display: "block",
                        color: token.colorBgContainer,
                        opacity: 0.4,
                        fontSize: 12,
                        fontWeight: 700,
                        letterSpacing: "0.1em",
                        textTransform: "uppercase",
                        marginBottom: 8,
                      }}
                    >
                      Terpakai Hari Ini
                    </Typography.Text>
                    <Typography.Text
                      style={{
                        color: token.colorBgContainer,
                        fontSize: 30,
                        fontWeight: 700,
                        fontVariantNumeric: "tabular-nums",
                      }}
                    >
                      {primaryCard
                        ? `Rp ${dailySpent.toLocaleString("id-ID")}`
                        : "\u2014"}
                    </Typography.Text>
                  </div>

                  <Flex
                    align="center"
                    gap={12}
                    style={{
                      padding: 16,
                      borderRadius: 12,
                      background: `${token.colorBgContainer}0D`,
                      border: `1px solid ${token.colorBgContainer}1A`,
                    }}
                  >
                    <Flex
                      align="center"
                      justify="center"
                      style={{
                        width: 32,
                        height: 32,
                        borderRadius: 8,
                        background: `${token.colorBgContainer}1A`,
                        color: token.colorBgContainer,
                      }}
                    >
                      <ShieldCheck />
                    </Flex>
                    <div>
                      <Typography.Text
                        style={{
                          display: "block",
                          color: token.colorBgContainer,
                          fontSize: 12,
                          fontWeight: 700,
                        }}
                      >
                        Status Aktif
                      </Typography.Text>
                      <Typography.Text
                        style={{
                          color: token.colorBgContainer,
                          opacity: 0.4,
                          fontSize: 12,
                          fontWeight: 500,
                        }}
                      >
                        Terlindungi Protokol Keamanan
                      </Typography.Text>
                    </div>
                  </Flex>

                  <Space
                    direction="vertical"
                    size={16}
                    style={{ width: "100%" }}
                  >
                    <Progress
                      percent={dailyLimitPercent}
                      showInfo={false}
                      strokeColor={token.colorBgContainer}
                      trailColor={`${token.colorBgContainer}1A`}
                      size="small"
                    />
                    <Flex justify="space-between" align="flex-end">
                      <Typography.Text
                        style={{
                          color: token.colorBgContainer,
                          fontSize: 12,
                          fontWeight: 700,
                        }}
                      >
                        {rawPercent}% Terpakai
                      </Typography.Text>
                      <Typography.Text
                        style={{
                          color: token.colorBgContainer,
                          opacity: 0.4,
                          fontSize: 12,
                          fontWeight: 700,
                          fontVariantNumeric: "tabular-nums",
                        }}
                      >
                        Limit: Rp {(limitForm.dailyLimit / 1000000).toFixed(1)}{" "}
                        jt
                      </Typography.Text>
                    </Flex>
                    <Button
                      block
                      style={{
                        background: `${token.colorBgContainer}1A`,
                        borderColor: `${token.colorBgContainer}1A`,
                        color: token.colorBgContainer,
                      }}
                      onClick={handleOpenLimitModal}
                    >
                      Ubah Batas Transaksi
                    </Button>
                  </Space>
                </Space>
              </Card>
            </Col>
          </Row>

          {/* Mid Section: Catalog Style Operations */}
          <Space direction="vertical" size={16} style={{ width: "100%" }}>
            <Typography.Title level={3} style={{ marginBottom: 0 }}>
              Kontrol Operasional
            </Typography.Title>
            <Row gutter={[16, 16]}>
              {operations.map((item, i) => (
                <Col key={i} xs={24} sm={12} lg={6}>
                  <Card style={{ height: "100%" }}>
                    <Flex justify="space-between" align="flex-start">
                      <Flex
                        align="center"
                        justify="center"
                        style={{
                          width: 40,
                          height: 40,
                          borderRadius: 12,
                          background: item.status
                            ? `${token.colorPrimary}1A`
                            : token.colorFillTertiary,
                          border: `1px solid ${
                            item.status
                              ? `${token.colorPrimary}33`
                              : token.colorBorder
                          }`,
                          color: item.status
                            ? token.colorPrimary
                            : token.colorTextTertiary,
                        }}
                      >
                        <item.icon />
                      </Flex>
                      <Tag
                        bordered={false}
                        color={
                          item.status ? token.colorPrimary : token.colorFill
                        }
                        style={{
                          fontSize: 12,
                          fontWeight: 700,
                          letterSpacing: "0.1em",
                          borderRadius: 999,
                          marginInlineEnd: 0,
                        }}
                      >
                        {item.tag}
                      </Tag>
                    </Flex>
                    <Space
                      direction="vertical"
                      size={2}
                      style={{ marginTop: 24, width: "100%" }}
                    >
                      <Typography.Text
                        style={{
                          fontSize: 12,
                          fontWeight: 700,
                          letterSpacing: "0.1em",
                          textTransform: "uppercase",
                        }}
                        type="secondary"
                      >
                        {item.status ? "Aktif" : "Non-aktif"}
                      </Typography.Text>
                      <Typography.Text strong>{item.label}</Typography.Text>
                      <Typography.Text
                        type="secondary"
                        style={{ fontSize: 12, opacity: 0.7 }}
                      >
                        {item.desc}
                      </Typography.Text>
                    </Space>
                    <Flex
                      justify="space-between"
                      align="center"
                      style={{
                        marginTop: 24,
                        paddingTop: 24,
                        borderTop: `1px solid ${token.colorBorderSecondary}`,
                      }}
                    >
                      <Typography.Text
                        style={{
                          fontSize: 12,
                          fontWeight: 700,
                          letterSpacing: "0.1em",
                          textTransform: "uppercase",
                          color: token.colorPrimary,
                        }}
                      >
                        Atur Izin
                      </Typography.Text>
                      <Switch
                        defaultChecked={item.status}
                        aria-label={`Atur Izin ${item.label}`}
                      />
                    </Flex>
                  </Card>
                </Col>
              ))}
            </Row>
          </Space>

          {/* Bottom Banner Area */}
          <Card
            styles={{ body: { padding: 24 } }}
            style={{
              background: `${token.colorPrimary}0D`,
              border: `1px solid ${token.colorPrimary}1A`,
            }}
          >
            <Flex align="center" gap={24} wrap justify="space-between">
              <Flex align="center" gap={24}>
                <Flex
                  align="center"
                  justify="center"
                  style={{
                    width: 48,
                    height: 48,
                    borderRadius: 16,
                    background: `${token.colorPrimary}1A`,
                    border: `1px solid ${token.colorPrimary}33`,
                    color: token.colorPrimary,
                  }}
                >
                  <SecurityIcon />
                </Flex>
                <div style={{ maxWidth: 640 }}>
                  <Typography.Title level={4} style={{ marginBottom: 4 }}>
                    Protokol Keamanan Aktif.
                  </Typography.Title>
                  <Typography.Text type="secondary">
                    Sistem AI kami mendeteksi aktivitas mencurigakan secara
                    real-time. Upgrade ke Premium untuk perlindungan asuransi
                    saldo hingga Rp 50.000.000.
                  </Typography.Text>
                </div>
              </Flex>
              <Link href="/settings">
                <Button type="primary" size="large">
                  Upgrade Sekarang
                </Button>
              </Link>
            </Flex>
          </Card>

          {/* Limit Modal */}
          <Modal
            open={isLimitModalOpen}
            onCancel={() => setIsLimitModalOpen(false)}
            footer={null}
            centered
            width={512}
            title={
              <Typography.Title level={4} style={{ marginBottom: 0 }}>
                Ubah Batas Transaksi
              </Typography.Title>
            }
          >
            <Space direction="vertical" size={16} style={{ width: "100%" }}>
              <Typography.Text type="secondary">
                Sesuaikan limit harian dan bulanan untuk kartu {cardLast4}.
              </Typography.Text>

              {updateCard.isError && (
                <Alert
                  type="error"
                  showIcon
                  message="Gagal mengubah limit. Silakan coba lagi."
                />
              )}

              <Space
                direction="vertical"
                size={16}
                style={{ width: "100%", paddingBlock: 16 }}
              >
                <div>
                  <Typography.Text
                    strong
                    style={{ display: "block", marginBottom: 8 }}
                  >
                    <label htmlFor="card-daily-limit">Limit Harian (IDR)</label>
                  </Typography.Text>
                  <Input
                    id="card-daily-limit"
                    type="number"
                    value={limitForm.dailyLimit}
                    onChange={(e) =>
                      setLimitForm((prev) => ({
                        ...prev,
                        dailyLimit: parseInt(e.target.value) || 0,
                      }))
                    }
                    placeholder="25000000"
                  />
                  <Typography.Text type="secondary" style={{ fontSize: 12 }}>
                    Maksimum transaksi per hari
                  </Typography.Text>
                </div>
                <div>
                  <Typography.Text
                    strong
                    style={{ display: "block", marginBottom: 8 }}
                  >
                    <label htmlFor="card-monthly-limit">
                      Limit Bulanan (IDR)
                    </label>
                  </Typography.Text>
                  <Input
                    id="card-monthly-limit"
                    type="number"
                    value={limitForm.monthlyLimit}
                    onChange={(e) =>
                      setLimitForm((prev) => ({
                        ...prev,
                        monthlyLimit: parseInt(e.target.value) || 0,
                      }))
                    }
                    placeholder="100000000"
                  />
                  <Typography.Text type="secondary" style={{ fontSize: 12 }}>
                    Maksimum transaksi per bulan
                  </Typography.Text>
                </div>
              </Space>

              <Flex justify="flex-end" gap={8}>
                <Button onClick={() => setIsLimitModalOpen(false)}>
                  Batal
                </Button>
                <Button
                  type="primary"
                  onClick={handleUpdateLimit}
                  disabled={updateCard.isPending}
                  icon={updateCard.isPending ? <Loader2 spin /> : undefined}
                >
                  Simpan Perubahan
                </Button>
              </Flex>
            </Space>
          </Modal>

          {/* Delete Modal */}
          <Modal
            open={isDeleteModalOpen}
            onCancel={() => setIsDeleteModalOpen(false)}
            footer={null}
            centered
            width={512}
            title={
              <Typography.Title
                level={4}
                style={{ marginBottom: 0, color: token.colorError }}
              >
                Hapus Kartu
              </Typography.Title>
            }
          >
            <Space direction="vertical" size={16} style={{ width: "100%" }}>
              <Typography.Text type="secondary">
                Apakah Anda yakin ingin menghapus kartu berakhiran {cardLast4}?
                Tindakan ini tidak dapat dibatalkan.
              </Typography.Text>

              {deleteCard.isError && (
                <Alert
                  type="error"
                  showIcon
                  message="Gagal menghapus kartu. Silakan coba lagi."
                />
              )}

              <Flex justify="flex-end" gap={8}>
                <Button onClick={() => setIsDeleteModalOpen(false)}>
                  Batal
                </Button>
                <Button
                  danger
                  type="primary"
                  onClick={handleDeleteCard}
                  disabled={deleteCard.isPending}
                  icon={deleteCard.isPending ? <Loader2 spin /> : <Trash2 />}
                >
                  Hapus Kartu
                </Button>
              </Flex>
            </Space>
          </Modal>
        </Space>
      </main>
    </DashboardLayout>
  );
}

function SecurityIcon() {
  return (
    <svg
      width="28"
      height="28"
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      strokeWidth="2"
      strokeLinecap="round"
      strokeLinejoin="round"
    >
      <path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10" />
      <path d="m9 12 2 2 4-4" />
    </svg>
  );
}
