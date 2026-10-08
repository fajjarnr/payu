"use client";

/* eslint-disable no-restricted-syntax -- display percentage uses Number for chart width, not Money arithmetic (ADR-0047 display only) */

import React, { useState } from "react";
import {
  Plus,
  Target,
  Lock,
  TrendingUp,
  ChevronRight,
  Wallet,
  History,
  ArrowUpRight,
  ShieldCheck,
  Coins,
  Users,
  UserPlus,
  MoreVertical,
  ArrowDownLeft,
  Trash2,
  TriangleAlert,
} from "@/components/icons";
import { useQuery } from "@tanstack/react-query";
import { useLocale } from "next-intl";
import { BalanceResponse, WalletTransaction, Pocket } from "@/types";
import api from "@/lib/api";
import DashboardLayout from "@/components/DashboardLayout";
import { SkipLink } from "@/lib/a11y";
import { Link } from "@/lib/navigation";
import { useAuthStore } from "@/stores";
import {
  usePockets,
  usePocketsTotalBalance,
  useCreatePocket,
  useCreditPocket,
  useDebitPocket,
  useFreezePocket,
  useUnfreezePocket,
  useClosePocket,
} from "@/hooks";
import {
  Button,
  Card,
  Col,
  Dropdown,
  Empty,
  Flex,
  Input,
  List,
  Modal,
  Progress,
  Row,
  Skeleton,
  Space,
  Tag,
  Typography,
  theme,
} from "antd";
import { notify as toast } from "@/lib/notify";
import {
  addCurrency,
  formatCurrency,
  formatCurrencyWithoutSymbol,
  parseCurrencyExact,
} from "@/lib/currency";

interface SharedMember {
  accountId: string;
  fullName: string;
  role: "OWNER" | "ADMIN" | "MEMBER";
  joinedAt: string;
}

interface SharedPocket extends Pocket {
  target?: number;
  type?: string;
  sharedMembers?: SharedMember[];
  isShared?: boolean;
}

/** Backend sends `target` as a decimal string; absent from the base Pocket type. */
type PocketWithTarget = Pocket & { target?: string };

function pocketTarget(p: Pocket): string | undefined {
  const withTarget = p as PocketWithTarget;
  return withTarget.target;
}

/** Extended Pocket type for UI display (target/type not in backend Pocket) */
type PocketWithGoal = Pocket & { target?: number; type?: string };

export default function PocketsPage() {
  const { token } = theme.useToken();
  // SECURITY: Get accountId from auth store, NOT localStorage
  const accountId = useAuthStore((state) => state.accountId) || "";
  const locale = useLocale();
  const bcp47Locale = locale === "id" ? "id-ID" : "en-US";
  const [selectedPocket, setSelectedPocket] = useState<string | null>(null);
  const [isCreateModalOpen, setIsCreateModalOpen] = useState(false);
  const [isCreditModalOpen, setIsCreditModalOpen] = useState(false);
  const [isDebitModalOpen, setIsDebitModalOpen] = useState(false);
  const [isCloseModalOpen, setIsCloseModalOpen] = useState(false);
  const [selectedPocketForAction, setSelectedPocketForAction] =
    useState<Pocket | null>(null);

  const [newPocketName, setNewPocketName] = useState("");
  const [newPocketTarget, setNewPocketTarget] = useState("");
  const [newPocketType, setNewPocketType] = useState<"SAVINGS" | "GOAL">(
    "SAVINGS",
  );
  const [amount, setAmount] = useState("");

  const { data: pocketsData, isLoading: pocketsLoading } = usePockets();
  const { data: totalBalance } = usePocketsTotalBalance("IDR");
  const createPocket = useCreatePocket();
  const creditPocket = useCreditPocket();
  const debitPocket = useDebitPocket();
  const freezePocket = useFreezePocket();
  const unfreezePocket = useUnfreezePocket();
  const closePocket = useClosePocket();

  const { data: balance, isLoading: balanceLoading } = useQuery({
    queryKey: ["wallet-balance", accountId],
    queryFn: async () => {
      const response = await api.get<BalanceResponse>(
        `/wallets/${accountId}/balance`,
      );
      return response.data;
    },
    enabled: !!accountId,
  });

  const { data: transactions, isLoading: transactionsLoading } = useQuery({
    queryKey: ["wallet-transactions", accountId],
    queryFn: async () => {
      const response = await api.get<WalletTransaction[]>(
        `/wallets/${accountId}/transactions`,
      );
      return response.data;
    },
    enabled: !!accountId,
  });

  const handleCreatePocket = async () => {
    if (!newPocketName.trim()) {
      toast.error("Nama kantong wajib diisi");
      return;
    }

    try {
      await createPocket.mutateAsync({
        accountId,
        name: newPocketName,
        currency: "IDR",
        description: newPocketTarget ? `Target: ${newPocketTarget}` : undefined,
      });
      toast.success("Kantong berhasil dibuat");
      setIsCreateModalOpen(false);
      setNewPocketName("");
      setNewPocketTarget("");
    } catch {
      toast.error("Gagal membuat kantong");
    }
  };

  const handleCredit = async () => {
    if (
      !selectedPocketForAction ||
      !amount ||
      parseCurrencyExact(amount) === "0"
    )
      return;

    try {
      await creditPocket.mutateAsync({
        pocketId: selectedPocketForAction.id,
        amount: parseCurrencyExact(amount),
        description: "Top up pocket",
      });
      toast.success("Berhasil menambah dana");
      setIsCreditModalOpen(false);
      setAmount("");
      setSelectedPocketForAction(null);
    } catch {
      toast.error("Gagal menambah dana");
    }
  };

  const handleDebit = async () => {
    if (
      !selectedPocketForAction ||
      !amount ||
      parseCurrencyExact(amount) === "0"
    )
      return;

    try {
      await debitPocket.mutateAsync({
        pocketId: selectedPocketForAction.id,
        amount: parseCurrencyExact(amount),
        description: "Withdraw from pocket",
      });
      toast.success("Berhasil mengambil dana");
      setIsDebitModalOpen(false);
      setAmount("");
      setSelectedPocketForAction(null);
    } catch {
      toast.error("Gagal mengambil dana");
    }
  };

  const handleFreeze = async (pocketId: string) => {
    try {
      await freezePocket.mutateAsync(pocketId);
      toast.success("Kantong berhasil dibekukan");
    } catch {
      toast.error("Gagal membekukan kantong");
    }
  };

  const handleUnfreeze = async (pocketId: string) => {
    try {
      await unfreezePocket.mutateAsync(pocketId);
      toast.success("Kantong berhasil diaktifkan kembali");
    } catch {
      toast.error("Gagal mengaktifkan kantong");
    }
  };

  const handleClose = async () => {
    if (!selectedPocketForAction) return;

    try {
      await closePocket.mutateAsync(selectedPocketForAction.id);
      toast.success("Kantong berhasil ditutup");
      setIsCloseModalOpen(false);
      setSelectedPocketForAction(null);
    } catch {
      toast.error("Gagal menutup kantong");
    }
  };

  const openCreditModal = (pocket: Pocket) => {
    setSelectedPocketForAction(pocket);
    setIsCreditModalOpen(true);
  };

  const openDebitModal = (pocket: Pocket) => {
    setSelectedPocketForAction(pocket);
    setIsDebitModalOpen(true);
  };

  const openCloseModal = (pocket: Pocket) => {
    setSelectedPocketForAction(pocket);
    setIsCloseModalOpen(true);
  };

  // Real data from DB via usePockets — no mock
  const savingGoals = (pocketsData ?? [])
    .filter((p: Pocket) => !p.isShared)
    .slice(0, 2)
    .map((p: Pocket, idx: number) => ({
      id: p.id,
      name: p.name,
      target: String(pocketTarget(p) ?? p.balance),
      current: String(p.balance),
      color: idx === 0 ? "bank-green" : "bank-emerald",
      icon: idx === 0 ? Target : Lock,
      isShared: false,
      locked: idx === 1,
      interestRate: idx === 1 ? "4.5% p.a" : undefined,
    }));

  const sharedPockets: SharedPocket[] = (pocketsData ?? [])
    .filter((p: Pocket) => !!p.isShared)
    .map((p: Pocket) => ({
      id: p.id,
      accountId: p.accountId,
      name: p.name,
      balance: p.balance,
      target: Number(pocketTarget(p) ?? 0),
      type: "SHARED" as const,
      isShared: true,
      createdAt: p.createdAt,
      updatedAt: p.updatedAt,
      sharedMembers: p.sharedMembers ?? [],
    }));

  const reservedPercent = (() => {
    const res = Number(balance?.reservedBalance ?? 0);
    const avail = Number(balance?.availableBalance ?? 0);
    const tot = res + avail;
    return tot > 0 ? Math.min(100, Math.round((res / tot) * 100)) : 0;
  })();

  const labelStyle: React.CSSProperties = {
    display: "block",
    fontSize: 12,
    fontWeight: 700,
    letterSpacing: "0.1em",
    textTransform: "uppercase",
  };

  return (
    <DashboardLayout>
      <SkipLink href="#main-content" />
      <main id="main-content">
        <Space direction="vertical" size={24} style={{ width: "100%" }}>
          {/* Header */}
          <Flex justify="space-between" align="flex-end" gap={16} wrap>
            <div>
              <Typography.Title level={2} style={{ marginBottom: 4 }}>
                Manajemen Kantong
              </Typography.Title>
              <Typography.Text type="secondary">
                Kelola dan alokasikan dana Anda dengan presisi tinggi.
              </Typography.Text>
            </div>
            <Flex gap={12}>
              <Button
                icon={<Users />}
                onClick={() =>
                  document
                    .getElementById("shared-pockets")
                    ?.scrollIntoView({ behavior: "smooth" })
                }
              >
                Kantong Bersama
              </Button>
              <Button
                type="primary"
                icon={<Plus />}
                onClick={() => setIsCreateModalOpen(true)}
              >
                Tambah Kantong
              </Button>
            </Flex>
          </Flex>

          <Row gutter={[16, 16]} align="stretch">
            {/* Main wallet card */}
            <Col xs={24} lg={16}>
              <Card
                styles={{
                  body: {
                    minHeight: 240,
                    display: "flex",
                    flexDirection: "column",
                    position: "relative",
                  },
                }}
              >
                <Flex vertical justify="space-between" style={{ flex: 1 }}>
                  <Flex justify="space-between" align="flex-start">
                    <div>
                      <Flex align="center" gap={8}>
                        <span
                          aria-hidden="true"
                          style={{
                            width: 8,
                            height: 8,
                            borderRadius: "50%",
                            background: token.colorPrimary,
                          }}
                        />
                        <Typography.Text
                          style={{ ...labelStyle, color: token.colorPrimary }}
                        >
                          Dompet Aktif
                        </Typography.Text>
                      </Flex>
                      <Typography.Title level={3} style={{ marginBottom: 0 }}>
                        Kantong Utama Cair
                      </Typography.Title>
                    </div>
                    <Flex
                      align="center"
                      justify="center"
                      style={{
                        width: 48,
                        height: 48,
                        borderRadius: 12,
                        background: token.colorFillTertiary,
                        border: `1px solid ${token.colorBorderSecondary}`,
                        color: token.colorPrimary,
                      }}
                    >
                      <Wallet />
                    </Flex>
                  </Flex>

                  <div style={{ marginTop: "auto" }}>
                    <Typography.Text
                      type="secondary"
                      style={{ ...labelStyle, opacity: 0.6 }}
                    >
                      Likuiditas Tersedia
                    </Typography.Text>
                    <Typography.Title
                      level={1}
                      style={{
                        marginBottom: 0,
                        fontVariantNumeric: "tabular-nums",
                      }}
                    >
                      {balanceLoading ? (
                        <Skeleton.Input
                          active
                          style={{ height: 48, width: 256 }}
                        />
                      ) : (
                        formatCurrency(balance?.balance ?? "0", {
                          locale: bcp47Locale,
                        })
                      )}
                    </Typography.Title>
                  </div>
                </Flex>

                <Button
                  type="text"
                  aria-label="Buka detail"
                  icon={<ArrowUpRight />}
                  onClick={() =>
                    document
                      .getElementById("pocket-list")
                      ?.scrollIntoView({ behavior: "smooth" })
                  }
                  style={{
                    position: "absolute",
                    bottom: 24,
                    right: 24,
                    color: token.colorPrimary,
                  }}
                />
              </Card>
            </Col>

            {/* Right column */}
            <Col xs={24} lg={8}>
              <Space direction="vertical" size={16} style={{ width: "100%" }}>
                <Card styles={{ body: { minHeight: 160 } }}>
                  <Typography.Text
                    type="secondary"
                    style={{ ...labelStyle, opacity: 0.6 }}
                  >
                    Protokol Cadangan
                  </Typography.Text>
                  <Typography.Title
                    level={3}
                    style={{
                      marginTop: 4,
                      marginBottom: 0,
                      fontVariantNumeric: "tabular-nums",
                    }}
                  >
                    {formatCurrency(balance?.reservedBalance ?? "0", {
                      locale: bcp47Locale,
                    })}
                  </Typography.Title>
                  <Progress
                    percent={reservedPercent}
                    showInfo={false}
                    strokeColor={`${token.colorPrimary}66`}
                    trailColor={token.colorFillTertiary}
                    size="small"
                    style={{ marginTop: 16 }}
                  />
                </Card>
                <Card
                  styles={{
                    body: {
                      minHeight: 160,
                      position: "relative",
                      overflow: "hidden",
                    },
                  }}
                  style={{
                    background: token.colorText,
                    color: token.colorBgContainer,
                    border: "none",
                  }}
                >
                  <Flex align="center" gap={16}>
                    <Flex
                      align="center"
                      justify="center"
                      style={{
                        width: 48,
                        height: 48,
                        borderRadius: 12,
                        background: `${token.colorBgContainer}1A`,
                        border: `1px solid ${token.colorBgContainer}1A`,
                        color: token.colorBgContainer,
                      }}
                    >
                      <ShieldCheck />
                    </Flex>
                    <div>
                      <Typography.Text
                        strong
                        style={{
                          display: "block",
                          color: token.colorBgContainer,
                        }}
                      >
                        Keamanan Tier-1
                      </Typography.Text>
                      <Typography.Text
                        style={{
                          ...labelStyle,
                          color: token.colorBgContainer,
                          opacity: 0.6,
                        }}
                      >
                        OJK & ASPI Compliant
                      </Typography.Text>
                    </div>
                  </Flex>
                  <div style={{ marginTop: 24 }}>
                    <Typography.Text
                      style={{
                        ...labelStyle,
                        color: token.colorBgContainer,
                        opacity: 0.3,
                      }}
                    >
                      Status Enkripsi
                    </Typography.Text>
                    <Typography.Text
                      style={{
                        display: "block",
                        fontFamily: "monospace",
                        fontSize: 12,
                        color: `${token.colorBgContainer}B3`,
                      }}
                    >
                      RESP-V3 ACTIVE
                    </Typography.Text>
                  </div>
                </Card>
              </Space>
            </Col>
          </Row>

          {/* Pockets List with CRUD */}
          <Space direction="vertical" size={16} style={{ width: "100%" }}>
            <Flex justify="space-between" align="center">
              <Typography.Title level={3} style={{ marginBottom: 0 }}>
                Kantong Saya
              </Typography.Title>
              <Tag bordered style={{ fontFamily: "monospace" }}>
                Total:{" "}
                {formatCurrency(totalBalance?.totalBalance ?? "0", {
                  locale: bcp47Locale,
                })}
              </Tag>
            </Flex>

            {pocketsLoading ? (
              <Row gutter={[16, 16]}>
                {[1, 2, 3].map((i) => (
                  <Col key={i} xs={24} sm={12} lg={8}>
                    <Card loading />
                  </Col>
                ))}
              </Row>
            ) : pocketsData && pocketsData.length > 0 ? (
              <Row gutter={[16, 16]}>
                {pocketsData.map((_pocket) => {
                  const pocket = _pocket as PocketWithGoal;
                  const percentage = pocket.target
                    ? Math.round((Number(pocket.balance) / pocket.target) * 100)
                    : 0;
                  return (
                    <Col key={pocket.id} xs={24} sm={12} lg={8}>
                      <Card style={{ height: "100%" }}>
                        <Flex justify="space-between" align="flex-start">
                          <Flex align="center" gap={12}>
                            <Flex
                              align="center"
                              justify="center"
                              style={{
                                width: 40,
                                height: 40,
                                borderRadius: 12,
                                background:
                                  pocket.status === "FROZEN"
                                    ? `${token.colorWarning}1A`
                                    : `${token.colorPrimary}1A`,
                                color:
                                  pocket.status === "FROZEN"
                                    ? token.colorWarning
                                    : token.colorPrimary,
                              }}
                            >
                              {pocket.type === "GOAL" ? <Target /> : <Wallet />}
                            </Flex>
                            <div>
                              <Typography.Text strong>
                                {pocket.name}
                              </Typography.Text>
                              <div>
                                <Tag bordered style={{ fontSize: 12 }}>
                                  {pocket.type}
                                </Tag>
                              </div>
                            </div>
                          </Flex>
                          <Dropdown
                            trigger={["click"]}
                            placement="bottomRight"
                            menu={{
                              style: { minWidth: 192 },
                              items: [
                                {
                                  key: "credit",
                                  label: (
                                    <Flex align="center" gap={8}>
                                      <ArrowDownLeft
                                        style={{ color: token.colorPrimary }}
                                      />
                                      Tambah Dana
                                    </Flex>
                                  ),
                                  onClick: () => openCreditModal(pocket),
                                },
                                {
                                  key: "debit",
                                  label: (
                                    <Flex align="center" gap={8}>
                                      <ArrowUpRight
                                        style={{ color: token.colorPrimary }}
                                      />
                                      Ambil Dana
                                    </Flex>
                                  ),
                                  onClick: () => openDebitModal(pocket),
                                },
                                pocket.status === "ACTIVE"
                                  ? {
                                      key: "freeze",
                                      label: (
                                        <Flex align="center" gap={8}>
                                          <Lock
                                            style={{
                                              color: token.colorWarning,
                                            }}
                                          />
                                          Bekukan
                                        </Flex>
                                      ),
                                      onClick: () => handleFreeze(pocket.id),
                                    }
                                  : {
                                      key: "unfreeze",
                                      label: (
                                        <Flex align="center" gap={8}>
                                          <UnlockIcon
                                            style={{
                                              color: token.colorPrimary,
                                            }}
                                          />
                                          Aktifkan
                                        </Flex>
                                      ),
                                      onClick: () => handleUnfreeze(pocket.id),
                                    },
                                {
                                  key: "close",
                                  danger: true,
                                  label: (
                                    <Flex align="center" gap={8}>
                                      <Trash2 />
                                      Tutup Kantong
                                    </Flex>
                                  ),
                                  onClick: () => openCloseModal(pocket),
                                },
                              ],
                            }}
                          >
                            <Button
                              type="text"
                              aria-label="Opsi kantong"
                              icon={<MoreVertical />}
                              style={{ minWidth: 44, minHeight: 44 }}
                            />
                          </Dropdown>
                        </Flex>

                        <Space
                          direction="vertical"
                          size={12}
                          style={{ width: "100%", marginTop: 16 }}
                        >
                          <Flex justify="space-between" align="flex-end">
                            <Typography.Title
                              level={3}
                              style={{ marginBottom: 0 }}
                            >
                              Rp{" "}
                              {formatCurrencyWithoutSymbol(pocket.balance, {
                                locale: bcp47Locale,
                              })}
                            </Typography.Title>
                            {pocket.target && (
                              <Typography.Text
                                strong
                                style={{
                                  color: token.colorPrimary,
                                  fontSize: 12,
                                }}
                              >
                                {percentage}%
                              </Typography.Text>
                            )}
                          </Flex>
                          {pocket.target && (
                            <Progress
                              percent={Math.min(percentage, 100)}
                              showInfo={false}
                              strokeColor={token.colorPrimary}
                              trailColor={token.colorFillTertiary}
                              size="small"
                            />
                          )}
                          <Flex justify="space-between" align="center">
                            <Tag
                              bordered={false}
                              color={
                                pocket.status === "ACTIVE" ? "green" : undefined
                              }
                              style={{ fontSize: 12 }}
                            >
                              {pocket.status}
                            </Tag>
                            <Typography.Text
                              type="secondary"
                              style={{ fontSize: 12 }}
                            >
                              {pocket.currency}
                            </Typography.Text>
                          </Flex>
                        </Space>
                      </Card>
                    </Col>
                  );
                })}
              </Row>
            ) : (
              <Card>
                <Empty
                  image={<Wallet style={{ fontSize: 40 }} />}
                  description={
                    <Space direction="vertical" size={4}>
                      <Typography.Text strong style={{ fontSize: 16 }}>
                        Belum Ada Kantong
                      </Typography.Text>
                      <Typography.Text type="secondary">
                        Buat kantong pertama Anda untuk mulai mengalokasikan
                        dana
                      </Typography.Text>
                    </Space>
                  }
                >
                  <Button
                    type="primary"
                    icon={<Plus />}
                    onClick={() => setIsCreateModalOpen(true)}
                  >
                    Buat Kantong
                  </Button>
                </Empty>
              </Card>
            )}
          </Space>

          {/* Special Goals + Ledger */}
          <Row gutter={[16, 16]}>
            <Col xs={24} lg={14}>
              <Space direction="vertical" size={16} style={{ width: "100%" }}>
                <Flex justify="space-between" align="center">
                  <Typography.Title level={3} style={{ marginBottom: 0 }}>
                    Tujuan Khusus
                  </Typography.Title>
                  <Link href="/investments">
                    <Button type="link">Kelola Portofolio</Button>
                  </Link>
                </Flex>

                <Row gutter={[16, 16]}>
                  {savingGoals.map((goal) => {
                    const percentage = Math.round(
                      (Number(goal.current) / Number(goal.target)) * 100,
                    );
                    const Icon = goal.icon;
                    const isGreen = goal.color === "bank-green";
                    return (
                      <Col key={goal.id} xs={24} sm={12}>
                        <Card style={{ height: "100%" }}>
                          <Flex align="center" gap={20}>
                            <Flex
                              align="center"
                              justify="center"
                              style={{
                                width: 48,
                                height: 48,
                                borderRadius: 12,
                                background: isGreen
                                  ? `${token.colorPrimary}1A`
                                  : "hsl(145 50% 35% / 0.1)",
                                border: isGreen
                                  ? `1px solid ${token.colorPrimary}1A`
                                  : "1px solid hsl(145 50% 35% / 0.1)",
                                color: isGreen
                                  ? token.colorPrimary
                                  : "hsl(145 50% 35%)",
                              }}
                            >
                              <Icon />
                            </Flex>
                            <div>
                              <Typography.Text strong>
                                {goal.name}
                              </Typography.Text>
                              <Typography.Text
                                type="secondary"
                                style={labelStyle}
                              >
                                Target:{" "}
                                {formatCurrency(goal.target, {
                                  locale: bcp47Locale,
                                })}
                              </Typography.Text>
                            </div>
                          </Flex>

                          <Space
                            direction="vertical"
                            size={16}
                            style={{ width: "100%", marginTop: 24 }}
                          >
                            <Flex justify="space-between" align="flex-end">
                              <Typography.Title
                                level={3}
                                style={{ marginBottom: 0 }}
                              >
                                {formatCurrency(goal.current, {
                                  locale: bcp47Locale,
                                })}
                              </Typography.Title>
                              {goal.locked ? (
                                <Tag
                                  bordered={false}
                                  color={token.colorPrimary}
                                  style={{ borderRadius: 999, fontWeight: 700 }}
                                >
                                  {goal.interestRate}
                                </Tag>
                              ) : (
                                <Typography.Text
                                  strong
                                  style={{
                                    color: token.colorPrimary,
                                    fontSize: 12,
                                  }}
                                >
                                  +{percentage}%
                                </Typography.Text>
                              )}
                            </Flex>
                            {goal.locked ? (
                              <Typography.Text
                                style={{
                                  ...labelStyle,
                                  color: token.colorPrimary,
                                }}
                              >
                                <Lock /> Dana Terkunci & Dijamin
                              </Typography.Text>
                            ) : (
                              <>
                                <Progress
                                  percent={percentage}
                                  showInfo={false}
                                  strokeColor={token.colorPrimary}
                                  trailColor={token.colorFillTertiary}
                                  size="small"
                                />
                                <Typography.Text
                                  type="secondary"
                                  style={{ ...labelStyle, textAlign: "right" }}
                                >
                                  Sisa:{" "}
                                  {formatCurrency(
                                    addCurrency(
                                      goal.target,
                                      `-${goal.current}`,
                                    ),
                                    { locale: bcp47Locale },
                                  )}
                                </Typography.Text>
                              </>
                            )}
                          </Space>
                        </Card>
                      </Col>
                    );
                  })}
                </Row>
              </Space>
            </Col>

            <Col xs={24} lg={10}>
              <Space direction="vertical" size={16} style={{ width: "100%" }}>
                <Flex justify="space-between" align="center">
                  <Typography.Title level={3} style={{ marginBottom: 0 }}>
                    Buku Besar Terakhir
                  </Typography.Title>
                  <Flex
                    align="center"
                    justify="center"
                    style={{
                      width: 40,
                      height: 40,
                      borderRadius: 12,
                      background: token.colorFillTertiary,
                      border: `1px solid ${token.colorBorderSecondary}`,
                      color: token.colorTextSecondary,
                    }}
                  >
                    <History />
                  </Flex>
                </Flex>

                <Card
                  styles={{
                    body: {
                      minHeight: 320,
                      display: "flex",
                      flexDirection: "column",
                    },
                  }}
                >
                  {transactionsLoading ? (
                    <Space
                      direction="vertical"
                      size={16}
                      style={{ width: "100%" }}
                    >
                      {[1, 2, 3, 4, 5].map((i) => (
                        <Skeleton
                          key={i}
                          active
                          avatar={{ shape: "square" }}
                          paragraph={{ rows: 1 }}
                          title={false}
                        />
                      ))}
                    </Space>
                  ) : (
                    <List
                      style={{ flex: 1 }}
                      dataSource={transactions ?? []}
                      locale={{
                        emptyText: (
                          <Flex
                            vertical
                            align="center"
                            justify="center"
                            style={{
                              padding: "32px 24px",
                              textAlign: "center",
                            }}
                          >
                            <History style={{ fontSize: 48, opacity: 0.2 }} />
                            <Typography.Text strong type="secondary">
                              Tidak Ada Aktivitas
                            </Typography.Text>
                            <Typography.Text
                              type="secondary"
                              style={{ ...labelStyle, opacity: 0.6 }}
                            >
                              Aktivitas keuangan Anda akan muncul di sini
                            </Typography.Text>
                          </Flex>
                        ),
                      }}
                      renderItem={(tx) => {
                        const isCredit = tx.type === "CREDIT";
                        return (
                          <List.Item>
                            <Flex
                              justify="space-between"
                              align="center"
                              gap={16}
                              style={{ width: "100%" }}
                            >
                              <Flex align="center" gap={16}>
                                <Flex
                                  align="center"
                                  justify="center"
                                  style={{
                                    width: 48,
                                    height: 48,
                                    borderRadius: 12,
                                    background: isCredit
                                      ? `${token.colorPrimary}1A`
                                      : `${token.colorError}0D`,
                                    border: `1px solid ${
                                      isCredit
                                        ? `${token.colorPrimary}1A`
                                        : `${token.colorError}1A`
                                    }`,
                                    color: isCredit
                                      ? token.colorPrimary
                                      : token.colorError,
                                  }}
                                >
                                  {isCredit ? (
                                    <TrendingUp />
                                  ) : (
                                    <ChevronRight
                                      style={{ transform: "rotate(90deg)" }}
                                    />
                                  )}
                                </Flex>
                                <div>
                                  <Typography.Text strong>
                                    {tx.description}
                                  </Typography.Text>
                                  <Typography.Text
                                    type="secondary"
                                    style={{ display: "block", fontSize: 12 }}
                                  >
                                    {new Date(tx.createdAt).toLocaleDateString(
                                      undefined,
                                      { day: "2-digit", month: "short" },
                                    )}{" "}
                                    • {isCredit ? "Masuk" : "Keluar"}
                                  </Typography.Text>
                                </div>
                              </Flex>
                              <Typography.Text
                                strong
                                style={{
                                  color: isCredit
                                    ? token.colorPrimary
                                    : token.colorText,
                                }}
                              >
                                {isCredit ? "+" : "-"}{" "}
                                {formatCurrency(tx.amount, {
                                  locale: bcp47Locale,
                                })}
                              </Typography.Text>
                            </Flex>
                          </List.Item>
                        );
                      }}
                    />
                  )}
                  <div style={{ marginTop: "auto", paddingTop: 24 }}>
                    <Link href="/statements">
                      <Button block>Lihat Rekening Koran</Button>
                    </Link>
                  </div>
                </Card>
              </Space>
            </Col>
          </Row>

          {/* Shared pockets */}
          <Space
            id="shared-pockets"
            direction="vertical"
            size={16}
            style={{ width: "100%" }}
          >
            <Flex justify="space-between" align="center">
              <Flex align="center" gap={12}>
                <Users style={{ color: token.colorPrimary }} />
                <Typography.Title level={3} style={{ marginBottom: 0 }}>
                  Kantong Bersama
                </Typography.Title>
              </Flex>
              <Button
                icon={<UserPlus />}
                onClick={() => setIsCreateModalOpen(true)}
              >
                Buat Kantong Baru
              </Button>
            </Flex>

            <Row gutter={[16, 16]}>
              {sharedPockets.map((pocket) => {
                const percentage = pocket.target
                  ? Math.round((Number(pocket.balance) / pocket.target) * 100)
                  : 0;
                const isSelected = selectedPocket === pocket.id;

                return (
                  <Col key={pocket.id} xs={24} md={12}>
                    <Card
                      style={{ height: "100%" }}
                      styles={{ body: { padding: 0 } }}
                    >
                      <div
                        role="button"
                        tabIndex={0}
                        onClick={() =>
                          setSelectedPocket(isSelected ? null : pocket.id)
                        }
                        onKeyDown={(e) => {
                          if (e.key === "Enter" || e.key === " ") {
                            e.preventDefault();
                            setSelectedPocket(isSelected ? null : pocket.id);
                          }
                        }}
                        style={{ padding: 24, cursor: "pointer" }}
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
                                background: `${token.colorPrimary}1A`,
                                color: token.colorPrimary,
                              }}
                            >
                              <Users />
                            </Flex>
                            <div>
                              <Typography.Text strong>
                                {pocket.name}
                              </Typography.Text>
                              <Typography.Text
                                type="secondary"
                                style={labelStyle}
                              >
                                {pocket.sharedMembers?.length} Anggota
                              </Typography.Text>
                            </div>
                          </Flex>
                          <ChevronRight
                            style={{
                              color: token.colorTextSecondary,
                              transform: isSelected
                                ? "rotate(90deg)"
                                : undefined,
                            }}
                          />
                        </Flex>

                        <Space
                          direction="vertical"
                          size={12}
                          style={{ width: "100%", marginTop: 16 }}
                        >
                          <Flex justify="space-between" align="flex-end">
                            <Typography.Title
                              level={3}
                              style={{ marginBottom: 0 }}
                            >
                              Rp{" "}
                              {formatCurrencyWithoutSymbol(pocket.balance, {
                                locale: bcp47Locale,
                              })}
                            </Typography.Title>
                            <Typography.Text
                              strong
                              style={{
                                color: token.colorPrimary,
                                fontSize: 12,
                              }}
                            >
                              {percentage}%
                            </Typography.Text>
                          </Flex>
                          <Progress
                            percent={percentage}
                            showInfo={false}
                            strokeColor={token.colorPrimary}
                            trailColor={token.colorFillTertiary}
                            size="small"
                          />
                          {pocket.target && (
                            <Typography.Text
                              type="secondary"
                              style={{ ...labelStyle, textAlign: "right" }}
                            >
                              Target:{" "}
                              {formatCurrency(pocket.target, {
                                locale: bcp47Locale,
                              })}
                            </Typography.Text>
                          )}
                        </Space>
                      </div>

                      {isSelected && pocket.sharedMembers && (
                        <div
                          style={{
                            borderTop: `1px solid ${token.colorBorderSecondary}`,
                            padding: 16,
                            background: token.colorFillQuaternary,
                          }}
                        >
                          <Flex justify="space-between" align="center">
                            <Typography.Text
                              type="secondary"
                              style={labelStyle}
                            >
                              Anggota
                            </Typography.Text>
                            <Button
                              type="link"
                              icon={<UserPlus />}
                              onClick={() =>
                                toast.info(
                                  "Undangan anggota kantong bersama belum tersedia",
                                )
                              }
                              style={{ padding: 0 }}
                            >
                              Undang
                            </Button>
                          </Flex>
                          <Space
                            direction="vertical"
                            size={8}
                            style={{ width: "100%", marginTop: 12 }}
                          >
                            {pocket.sharedMembers.map((member, i) => (
                              <Flex
                                key={i}
                                justify="space-between"
                                align="center"
                                style={{
                                  padding: 8,
                                  borderRadius: 12,
                                  background: token.colorBgContainer,
                                  border: `1px solid ${token.colorBorderSecondary}`,
                                }}
                              >
                                <Flex align="center" gap={8}>
                                  <Flex
                                    align="center"
                                    justify="center"
                                    style={{
                                      width: 24,
                                      height: 24,
                                      borderRadius: "50%",
                                      background: `${token.colorPrimary}1A`,
                                      color: token.colorPrimary,
                                      fontSize: 12,
                                      fontWeight: 700,
                                    }}
                                  >
                                    {member.fullName.charAt(0)}
                                  </Flex>
                                  <Typography.Text
                                    strong
                                    style={{ fontSize: 12 }}
                                  >
                                    {member.fullName}
                                  </Typography.Text>
                                </Flex>
                                <Flex align="center" gap={8}>
                                  <Tag
                                    bordered={false}
                                    color={
                                      member.role === "OWNER"
                                        ? token.colorPrimary
                                        : member.role === "ADMIN"
                                          ? "hsl(145 50% 35%)"
                                          : token.colorFill
                                    }
                                    style={{
                                      ...labelStyle,
                                      borderRadius: 4,
                                    }}
                                  >
                                    {member.role}
                                  </Tag>
                                  <Button
                                    type="text"
                                    icon={<MoreVertical />}
                                    onClick={() =>
                                      toast.info(
                                        "Kelola anggota kantong bersama belum tersedia",
                                      )
                                    }
                                    aria-label="Aksi lainnya"
                                    style={{ minWidth: 44, minHeight: 44 }}
                                  />
                                </Flex>
                              </Flex>
                            ))}
                          </Space>
                        </div>
                      )}
                    </Card>
                  </Col>
                );
              })}
            </Row>
          </Space>

          {/* Bottom banner */}
          <Card
            style={{
              background: token.colorText,
              color: token.colorBgContainer,
              border: "none",
            }}
          >
            <Flex justify="space-between" align="center" gap={24} wrap>
              <Space direction="vertical" size={16} style={{ maxWidth: 640 }}>
                <Flex align="center" gap={16}>
                  <Flex
                    align="center"
                    justify="center"
                    style={{
                      width: 48,
                      height: 48,
                      borderRadius: 12,
                      background: token.colorPrimary,
                      color: token.colorBgContainer,
                    }}
                  >
                    <TrendingUp />
                  </Flex>
                  <Typography.Title
                    level={3}
                    style={{ marginBottom: 0, color: token.colorBgContainer }}
                  >
                    Akselerasi Kekayaan Anda.
                  </Typography.Title>
                </Flex>
                <Typography.Text
                  style={{
                    color: token.colorBgContainer,
                    opacity: 0.5,
                    fontSize: 14,
                  }}
                >
                  Pindahkan dana mengendap dari kantong ke reksa dana yield
                  tinggi atau emas digital. AI kami menyarankan Anda bisa
                  berhemat hingga{" "}
                  <Typography.Text strong style={{ color: token.colorPrimary }}>
                    Rp 12,5 Juta
                  </Typography.Text>{" "}
                  lebih per tahun.
                </Typography.Text>
              </Space>
              <Link href="/investments">
                <Button type="primary" size="large">
                  Jelajahi Marketplace
                </Button>
              </Link>
            </Flex>
          </Card>
        </Space>

        {/* Create Pocket Modal */}
        <Modal
          open={isCreateModalOpen}
          onCancel={() => setIsCreateModalOpen(false)}
          footer={null}
          centered
          width={512}
          title={undefined}
        >
          <Space direction="vertical" size={16} style={{ width: "100%" }}>
            <div>
              <Typography.Title level={4} style={{ marginBottom: 0 }}>
                Buat Kantong Baru
              </Typography.Title>
              <Typography.Text type="secondary">
                Buat kantong untuk mengalokasikan dana sesuai tujuan Anda
              </Typography.Text>
            </div>
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
                  <label htmlFor="name">Nama Kantong</label>
                </Typography.Text>
                <Input
                  id="name"
                  placeholder="Contoh: Dana Darurat, Liburan"
                  value={newPocketName}
                  onChange={(e) => setNewPocketName(e.target.value)}
                />
              </div>
              <div>
                <Typography.Text
                  strong
                  style={{ display: "block", marginBottom: 8 }}
                >
                  <label htmlFor="target">Target Dana (Opsional)</label>
                </Typography.Text>
                <Input
                  id="target"
                  type="number"
                  placeholder="5000000"
                  value={newPocketTarget}
                  onChange={(e) => setNewPocketTarget(e.target.value)}
                />
              </div>
              <div>
                <Typography.Text
                  strong
                  style={{ display: "block", marginBottom: 8 }}
                >
                  Tipe Kantong
                </Typography.Text>
                <Flex gap={8}>
                  <Button
                    htmlType="button"
                    block
                    type={newPocketType === "SAVINGS" ? "primary" : "default"}
                    icon={<Wallet />}
                    onClick={() => setNewPocketType("SAVINGS")}
                  >
                    Tabungan
                  </Button>
                  <Button
                    htmlType="button"
                    block
                    type={newPocketType === "GOAL" ? "primary" : "default"}
                    icon={<Target />}
                    onClick={() => setNewPocketType("GOAL")}
                  >
                    Target
                  </Button>
                </Flex>
              </div>
            </Space>
            <Flex justify="flex-end" gap={8}>
              <Button onClick={() => setIsCreateModalOpen(false)}>Batal</Button>
              <Button
                type="primary"
                onClick={handleCreatePocket}
                disabled={createPocket.isPending || !newPocketName.trim()}
              >
                {createPocket.isPending ? "Membuat..." : "Buat Kantong"}
              </Button>
            </Flex>
          </Space>
        </Modal>

        {/* Credit Modal */}
        <Modal
          open={isCreditModalOpen}
          onCancel={() => setIsCreditModalOpen(false)}
          footer={null}
          centered
          width={512}
          title={undefined}
        >
          <Space direction="vertical" size={16} style={{ width: "100%" }}>
            <div>
              <Typography.Title level={4} style={{ marginBottom: 0 }}>
                Tambah Dana
              </Typography.Title>
              <Typography.Text type="secondary">
                Tambahkan dana ke {selectedPocketForAction?.name}
              </Typography.Text>
            </div>
            <Space
              direction="vertical"
              size={16}
              style={{ width: "100%", paddingBlock: 16 }}
            >
              <Card
                styles={{ body: { padding: 16 } }}
                style={{ background: token.colorFillQuaternary }}
              >
                <Typography.Text type="secondary">
                  Saldo Saat Ini
                </Typography.Text>
                <Typography.Title level={3} style={{ marginBottom: 0 }}>
                  {formatCurrency(selectedPocketForAction?.balance ?? "0", {
                    locale: bcp47Locale,
                  })}
                </Typography.Title>
              </Card>
              <div>
                <Typography.Text
                  strong
                  style={{ display: "block", marginBottom: 8 }}
                >
                  <label htmlFor="amount">Jumlah Dana</label>
                </Typography.Text>
                <Input
                  id="amount"
                  type="number"
                  placeholder="100000"
                  value={amount}
                  onChange={(e) => setAmount(e.target.value)}
                />
              </div>
            </Space>
            <Flex justify="flex-end" gap={8}>
              <Button onClick={() => setIsCreditModalOpen(false)}>Batal</Button>
              <Button
                type="primary"
                onClick={handleCredit}
                disabled={creditPocket.isPending || !amount}
              >
                {creditPocket.isPending ? "Memproses..." : "Tambah Dana"}
              </Button>
            </Flex>
          </Space>
        </Modal>

        {/* Debit Modal */}
        <Modal
          open={isDebitModalOpen}
          onCancel={() => setIsDebitModalOpen(false)}
          footer={null}
          centered
          width={512}
          title={undefined}
        >
          <Space direction="vertical" size={16} style={{ width: "100%" }}>
            <div>
              <Typography.Title level={4} style={{ marginBottom: 0 }}>
                Ambil Dana
              </Typography.Title>
              <Typography.Text type="secondary">
                Ambil dana dari {selectedPocketForAction?.name}
              </Typography.Text>
            </div>
            <Space
              direction="vertical"
              size={16}
              style={{ width: "100%", paddingBlock: 16 }}
            >
              <Card
                styles={{ body: { padding: 16 } }}
                style={{ background: token.colorFillQuaternary }}
              >
                <Typography.Text type="secondary">
                  Saldo Tersedia
                </Typography.Text>
                <Typography.Title level={3} style={{ marginBottom: 0 }}>
                  {formatCurrency(selectedPocketForAction?.balance ?? "0", {
                    locale: bcp47Locale,
                  })}
                </Typography.Title>
              </Card>
              <div>
                <Typography.Text
                  strong
                  style={{ display: "block", marginBottom: 8 }}
                >
                  <label htmlFor="debit-amount">Jumlah Dana</label>
                </Typography.Text>
                <Input
                  id="debit-amount"
                  type="number"
                  placeholder="100000"
                  value={amount}
                  onChange={(e) => setAmount(e.target.value)}
                />
              </div>
            </Space>
            <Flex justify="flex-end" gap={8}>
              <Button onClick={() => setIsDebitModalOpen(false)}>Batal</Button>
              <Button
                onClick={handleDebit}
                disabled={debitPocket.isPending || !amount}
                danger
                type="primary"
              >
                {debitPocket.isPending ? "Memproses..." : "Ambil Dana"}
              </Button>
            </Flex>
          </Space>
        </Modal>

        {/* Close Modal */}
        <Modal
          open={isCloseModalOpen}
          onCancel={() => setIsCloseModalOpen(false)}
          footer={null}
          centered
          width={512}
          title={undefined}
        >
          <Space direction="vertical" size={16} style={{ width: "100%" }}>
            <div>
              <Typography.Title
                level={4}
                style={{ marginBottom: 0, color: token.colorError }}
              >
                <Flex align="center" gap={8}>
                  <Trash2 />
                  Tutup Kantong?
                </Flex>
              </Typography.Title>
              <Typography.Text type="secondary">
                Apakah Anda yakin ingin menutup kantong &ldquo;
                {selectedPocketForAction?.name}&rdquo;? Dana yang tersisa akan
                dikembalikan ke dompet utama.
              </Typography.Text>
            </div>
            <Card
              styles={{ body: { padding: 16 } }}
              style={{
                background: `${token.colorError}1A`,
                border: `1px solid ${token.colorError}`,
              }}
            >
              <Typography.Text
                style={{
                  color: token.colorError,
                  fontWeight: 500,
                }}
              >
                <Flex align="center" gap={8}>
                  <TriangleAlert aria-hidden="true" /> Tindakan ini tidak dapat
                  dibatalkan
                </Flex>
              </Typography.Text>
            </Card>
            <Flex justify="flex-end" gap={8}>
              <Button onClick={() => setIsCloseModalOpen(false)}>Batal</Button>
              <Button
                onClick={handleClose}
                disabled={closePocket.isPending}
                danger
                type="primary"
              >
                {closePocket.isPending ? "Menutup..." : "Ya, Tutup Kantong"}
              </Button>
            </Flex>
          </Space>
        </Modal>
      </main>
    </DashboardLayout>
  );
}

// Unlock icon component
function UnlockIcon({ style }: { style?: React.CSSProperties }) {
  return (
    <svg
      xmlns="http://www.w3.org/2000/svg"
      viewBox="0 0 24 24"
      width="1em"
      height="1em"
      fill="none"
      stroke="currentColor"
      strokeWidth="2"
      strokeLinecap="round"
      strokeLinejoin="round"
      style={style}
    >
      <rect x="3" y="11" width="18" height="11" rx="2" ry="2" />
      <path d="M7 11V7a5 5 0 0 1 9.9-1" />
    </svg>
  );
}
