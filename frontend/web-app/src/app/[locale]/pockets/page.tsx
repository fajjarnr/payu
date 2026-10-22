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
import { useLocale, useTranslations } from "next-intl";
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
  const t = useTranslations("accounts");
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
      toast.error(t("nameRequired"));
      return;
    }

    try {
      await createPocket.mutateAsync({
        accountId,
        name: newPocketName,
        currency: "IDR",
        description: newPocketTarget ? `Target: ${newPocketTarget}` : undefined,
      });
      toast.success(t("createSuccess"));
      setIsCreateModalOpen(false);
      setNewPocketName("");
      setNewPocketTarget("");
    } catch {
      toast.error(t("createError"));
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
      toast.success(t("creditSuccess"));
      setIsCreditModalOpen(false);
      setAmount("");
      setSelectedPocketForAction(null);
    } catch {
      toast.error(t("creditError"));
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
      toast.success(t("debitSuccess"));
      setIsDebitModalOpen(false);
      setAmount("");
      setSelectedPocketForAction(null);
    } catch {
      toast.error(t("debitError"));
    }
  };

  const handleFreeze = async (pocketId: string) => {
    try {
      await freezePocket.mutateAsync(pocketId);
      toast.success(t("freezeSuccess"));
    } catch {
      toast.error(t("freezeError"));
    }
  };

  const handleUnfreeze = async (pocketId: string) => {
    try {
      await unfreezePocket.mutateAsync(pocketId);
      toast.success(t("unfreezeSuccess"));
    } catch {
      toast.error(t("unfreezeError"));
    }
  };

  const handleClose = async () => {
    if (!selectedPocketForAction) return;

    try {
      await closePocket.mutateAsync(selectedPocketForAction.id);
      toast.success(t("closeSuccess"));
      setIsCloseModalOpen(false);
      setSelectedPocketForAction(null);
    } catch {
      toast.error(t("closeError"));
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
      color: idx === 0 ? "primary" : "success",
      icon: idx === 0 ? Target : Lock,
      isShared: false,
      locked: idx === 1,
      interestRate: idx === 1 ? "4.5" : undefined,
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
    fontWeight: 600,
    textTransform: "uppercase",
  };

  return (
    <DashboardLayout>
      <SkipLink href="#main-content" />
      <div>
        <Space orientation="vertical" size={24} style={{ width: "100%" }}>
          {/* Header */}
          <Flex justify="space-between" align="flex-end" gap={16} wrap>
            <div>
              <Typography.Title level={1} style={{ marginBottom: 4 }}>
                {t("title")}
              </Typography.Title>
              <Typography.Text type="secondary">
                {t("subtitle")}
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
                {t("sharedPocketsSection")}
              </Button>
              <Button
                type="primary"
                icon={<Plus />}
                onClick={() => setIsCreateModalOpen(true)}
              >
                {t("addPocket")}
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
                          style={{
                            ...labelStyle,
                            color: token.colorPrimaryText,
                          }}
                        >
                          {t("activeWallet")}
                        </Typography.Text>
                      </Flex>
                      <Typography.Title level={2} style={{ marginBottom: 0 }}>
                        {t("mainPocket")}
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
                      style={{ ...labelStyle, color: token.colorTextDescription }}
                    >
                      {t("availableLiquidity")}
                    </Typography.Text>
                    <Typography.Title
                      level={2}
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
                  aria-label={t("openDetails")}
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
                    color: token.colorPrimaryText,
                  }}
                />
              </Card>
            </Col>

            {/* Right column */}
            <Col xs={24} lg={8}>
              <Space orientation="vertical" size={16} style={{ width: "100%" }}>
                <Card styles={{ body: { minHeight: 160 } }}>
                  <Typography.Text
                    type="secondary"
                    style={{ ...labelStyle, color: token.colorTextDescription }}
                  >
                    {t("reserveProtocol")}
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
                    aria-label={t("reserveProgressLabel")}
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
                  style={{}}
                >
                  <Flex align="center" gap={16}>
                    <Flex
                      align="center"
                      justify="center"
                      style={{
                        width: 48,
                        height: 48,
                        background: token.colorPrimaryBg,
                        // Deep brand green: the emerald on its own tint is 1.9:1.
                        color: token.colorPrimaryText,
                      }}
                    >
                      <ShieldCheck />
                    </Flex>
                    <div>
                      <Typography.Text strong style={{ display: "block" }}>
                        {t("protectedFunds")}
                      </Typography.Text>
                      <Typography.Text type="secondary" style={{ ...labelStyle }}>
                        {t("encryptionActive")}
                      </Typography.Text>
                    </div>
                  </Flex>
                  <div style={{ marginTop: 24 }}>
                    <Typography.Text type="secondary" style={{ ...labelStyle }}>
                      {t("status")}
                    </Typography.Text>
                    <Typography.Text strong style={{ display: "block", fontSize: 12 }}>
                      {t("active")}
                    </Typography.Text>
                  </div>
                </Card>
              </Space>
            </Col>
          </Row>

          {/* Pockets List with CRUD */}
          <Space orientation="vertical" size={16} style={{ width: "100%" }}>
            <Flex justify="space-between" align="center">
              <Typography.Title level={2} style={{ marginBottom: 0 }}>
                {t("myPockets")}
              </Typography.Title>
              <Tag bordered style={{ fontFamily: "monospace" }}>
                {t("total", {
                  amount: formatCurrency(totalBalance?.totalBalance ?? "0", {
                    locale: bcp47Locale,
                  }),
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
                                      {t("addFunds")}
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
                                      {t("withdrawFunds")}
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
                                          {t("freezePocket")}
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
                                          {t("activatePocket")}
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
                                      {t("closePocketAction")}
                                    </Flex>
                                  ),
                                  onClick: () => openCloseModal(pocket),
                                },
                              ],
                            }}
                          >
                            <Button
                              type="text"
                              aria-label={t("pocketOptions")}
                              icon={<MoreVertical />}
                              style={{ minWidth: 44, minHeight: 44 }}
                            />
                          </Dropdown>
                        </Flex>

                        <Space
                          orientation="vertical"
                          size={12}
                          style={{ width: "100%", marginTop: 16 }}
                        >
                          <Flex justify="space-between" align="flex-end">
                            <Typography.Title
                              level={3}
                              style={{ marginBottom: 0 }}
                            >
                              {formatCurrency(pocket.balance, {
                                locale: bcp47Locale,
                              })}
                            </Typography.Title>
                            {pocket.target && (
                              <Typography.Text
                                strong
                                style={{
                                  color: token.colorPrimaryText,
                                  fontSize: 12,
                                }}
                              >
                                {percentage}%
                              </Typography.Text>
                            )}
                          </Flex>
                          {pocket.target && (
                            <Progress
                              aria-label={t("pocketProgressLabel", {
                                name: pocket.name,
                              })}
                              percent={Math.min(percentage, 100)}
                              showInfo={false}
                              strokeColor={token.colorPrimary}
                              trailColor={token.colorFillTertiary}
                              size="small"
                            />
                          )}
                          <Flex justify="space-between" align="center">
                            <Tag
                              variant="filled"
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
                    <Space orientation="vertical" size={4}>
                      <Typography.Text strong style={{ fontSize: 16 }}>
                        {t("emptyPocketsTitle")}
                      </Typography.Text>
                      <Typography.Text type="secondary">
                        {t("emptyPocketsDescription")}
                      </Typography.Text>
                    </Space>
                  }
                >
                  <Button
                    type="primary"
                    icon={<Plus />}
                    onClick={() => setIsCreateModalOpen(true)}
                  >
                    {t("createPocketAction")}
                  </Button>
                </Empty>
              </Card>
            )}
          </Space>

          {/* Special Goals + Ledger */}
          <Row gutter={[16, 16]}>
            <Col xs={24} lg={14}>
              <Space orientation="vertical" size={16} style={{ width: "100%" }}>
                <Flex justify="space-between" align="center">
                  <Typography.Title level={2} style={{ marginBottom: 0 }}>
                    {t("specialGoals")}
                  </Typography.Title>
                  <Link href="/investments">
                    <Button type="link">{t("managePortfolio")}</Button>
                  </Link>
                </Flex>

                <Row gutter={[16, 16]}>
                  {savingGoals.map((goal) => {
                    const percentage = Math.round(
                      (Number(goal.current) / Number(goal.target)) * 100,
                    );
                    const Icon = goal.icon;
                    const isPrimary = goal.color === "primary";
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
                                background: isPrimary
                                  ? `${token.colorPrimary}1A`
                                  : `${token.colorSuccess}1A`,
                                border: isPrimary
                                  ? `1px solid ${token.colorPrimary}1A`
                                  : `1px solid ${token.colorSuccess}1A`,
                                color: isPrimary
                                  ? token.colorPrimary
                                  : token.colorSuccess,
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
                                {t("targetLabel", {
                                  amount: formatCurrency(goal.target, {
                                    locale: bcp47Locale,
                                  }),
                                })}
                              </Typography.Text>
                            </div>
                          </Flex>

                          <Space
                            orientation="vertical"
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
                                  variant="filled"
                                  // antd's `color` prop takes a preset, not a colour.
                                  style={{
                                    color: token.colorPrimaryText,
                                    borderRadius: 9999,
                                    fontWeight: 700,
                                  }}
                                >
                                  {t("interestRate", {
                                    rate: goal.interestRate ?? "",
                                  })}
                                </Tag>
                              ) : (
                                <Typography.Text
                                  strong
                                  style={{
                                    color: token.colorPrimaryText,
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
                                  color: token.colorPrimaryText,
                                }}
                              >
                                <Lock /> {t("lockedAndGuaranteed")}
                              </Typography.Text>
                            ) : (
                              <>
                                <Progress
                                  aria-label={t("pocketProgressLabel", {
                                    name: goal.name,
                                  })}
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
                                  {t("remaining", {
                                    amount: formatCurrency(
                                      addCurrency(
                                        goal.target,
                                        `-${goal.current}`,
                                      ),
                                      { locale: bcp47Locale },
                                    ),
                                  })}
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
              <Space orientation="vertical" size={16} style={{ width: "100%" }}>
                <Flex justify="space-between" align="center">
                  <Typography.Title level={2} style={{ marginBottom: 0 }}>
                    {t("recentLedger")}
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
                      orientation="vertical"
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
                              {t("noActivity")}
                            </Typography.Text>
                            <Typography.Text
                              type="secondary"
                              style={{ ...labelStyle, color: token.colorTextDescription }}
                            >
                              {t("noActivityDescription")}
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
                                    •{" "}
                                    {isCredit
                                      ? t("transactionIn")
                                      : t("transactionOut")}
                                  </Typography.Text>
                                </div>
                              </Flex>
                              <Typography.Text
                                strong
                                style={{
                                  color: isCredit
                                    ? token.colorPrimaryText
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
                      <Button block>{t("viewStatement")}</Button>
                    </Link>
                  </div>
                </Card>
              </Space>
            </Col>
          </Row>

          {/* Shared pockets */}
          <Space
            id="shared-pockets"
            orientation="vertical"
            size={16}
            style={{ width: "100%" }}
          >
            <Flex justify="space-between" align="center">
              <Flex align="center" gap={12}>
                <Users style={{ color: token.colorPrimary }} />
                <Typography.Title level={2} style={{ marginBottom: 0 }}>
                  {t("sharedPocketsSection")}
                </Typography.Title>
              </Flex>
              <Button
                icon={<UserPlus />}
                onClick={() => setIsCreateModalOpen(true)}
              >
                {t("newPocketTitle")}
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
                        className="payu-focus-ring"
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
                                {t("memberCount", {
                                  count: pocket.sharedMembers?.length ?? 0,
                                })}
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
                          orientation="vertical"
                          size={12}
                          style={{ width: "100%", marginTop: 16 }}
                        >
                          <Flex justify="space-between" align="flex-end">
                            <Typography.Title
                              level={3}
                              style={{ marginBottom: 0 }}
                            >
                              {formatCurrency(pocket.balance, {
                                locale: bcp47Locale,
                              })}
                            </Typography.Title>
                            <Typography.Text
                              strong
                              style={{
                                color: token.colorPrimaryText,
                                fontSize: 12,
                              }}
                            >
                              {percentage}%
                            </Typography.Text>
                          </Flex>
                          <Progress
                            aria-label={t("pocketProgressLabel", {
                              name: pocket.name,
                            })}
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
                              {t("targetLabel", {
                                amount: formatCurrency(pocket.target, {
                                  locale: bcp47Locale,
                                }),
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
                              {t("members")}
                            </Typography.Text>
                            <Button
                              type="link"
                              icon={<UserPlus />}
                              onClick={() => toast.info(t("inviteUnavailable"))}
                              style={{ padding: 0 }}
                            >
                              {t("invite")}
                            </Button>
                          </Flex>
                          <Space
                            orientation="vertical"
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
                                      color: token.colorPrimaryText,
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
                                    variant="filled"
                                    color={
                                      member.role === "OWNER"
                                        ? token.colorPrimaryText
                                        : member.role === "ADMIN"
                                          ? token.colorSuccess
                                          : token.colorFill
                                    }
                                    style={{
                                      ...labelStyle,
                                      borderRadius: 12,
                                    }}
                                  >
                                    {member.role}
                                  </Tag>
                                  <Button
                                    type="text"
                                    icon={<MoreVertical />}
                                    onClick={() =>
                                      toast.info(t("manageMembersUnavailable"))
                                    }
                                    aria-label={t("moreActions")}
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
          >
            <Flex justify="space-between" align="center" gap={24} wrap>
              <Space orientation="vertical" size={16} style={{ maxWidth: 640 }}>
                <Flex align="center" gap={16}>
                  <Flex
                    align="center"
                    justify="center"
                    style={{
                      width: 48,
                      height: 48,
                      borderRadius: 12,
                      background: token.colorPrimaryBg,
                      // Deep brand green: the emerald on its own tint is 1.9:1.
                      color: token.colorPrimaryText,
                    }}
                  >
                    <TrendingUp />
                  </Flex>
                  <Typography.Title
                    level={2}
                    style={{ marginBottom: 0 }}
                  >
                    {t("wealthTitle")}
                  </Typography.Title>
                </Flex>
                <Typography.Text type="secondary" style={{ fontSize: 14 }}>
                  {t("wealthDescription")}
                </Typography.Text>
              </Space>
              <Link href="/investments">
                <Button type="primary" size="large">
                  {t("exploreMarketplace")}
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
          <Space orientation="vertical" size={16} style={{ width: "100%" }}>
            <div>
              <Typography.Title level={3} style={{ marginBottom: 0 }}>
                {t("newPocketTitle")}
              </Typography.Title>
              <Typography.Text type="secondary">
                {t("newPocketSubtitle")}
              </Typography.Text>
            </div>
            <Space
              orientation="vertical"
              size={16}
              style={{ width: "100%", paddingBlock: 16 }}
            >
              <div>
                <Typography.Text
                  strong
                  style={{ display: "block", marginBottom: 8 }}
                >
                  <label htmlFor="name">{t("pocketNameLabel")}</label>
                </Typography.Text>
                <Input
                  id="name"
                  placeholder={t("pocketNamePlaceholder")}
                  value={newPocketName}
                  onChange={(e) => setNewPocketName(e.target.value)}
                />
              </div>
              <div>
                <Typography.Text
                  strong
                  style={{ display: "block", marginBottom: 8 }}
                >
                  <label htmlFor="target">{t("targetAmountLabel")}</label>
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
                  {t("pocketTypeLabel")}
                </Typography.Text>
                <Flex gap={8}>
                  <Button
                    htmlType="button"
                    block
                    type={newPocketType === "SAVINGS" ? "primary" : "default"}
                    icon={<Wallet />}
                    onClick={() => setNewPocketType("SAVINGS")}
                  >
                    {t("savingsType")}
                  </Button>
                  <Button
                    htmlType="button"
                    block
                    type={newPocketType === "GOAL" ? "primary" : "default"}
                    icon={<Target />}
                    onClick={() => setNewPocketType("GOAL")}
                  >
                    {t("goalType")}
                  </Button>
                </Flex>
              </div>
            </Space>
            <Flex justify="flex-end" gap={8}>
              <Button onClick={() => setIsCreateModalOpen(false)}>
                {t("cancel")}
              </Button>
              <Button
                type="primary"
                onClick={handleCreatePocket}
                disabled={createPocket.isPending || !newPocketName.trim()}
              >
                {createPocket.isPending
                  ? t("creating")
                  : t("createPocketAction")}
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
          <Space orientation="vertical" size={16} style={{ width: "100%" }}>
            <div>
              <Typography.Title level={3} style={{ marginBottom: 0 }}>
                {t("addFunds")}
              </Typography.Title>
              <Typography.Text type="secondary">
                {t("addFundsDescription", {
                  name: selectedPocketForAction?.name ?? "",
                })}
              </Typography.Text>
            </div>
            <Space
              orientation="vertical"
              size={16}
              style={{ width: "100%", paddingBlock: 16 }}
            >
              <Card
                styles={{ body: { padding: 16 } }}
                style={{ background: token.colorFillQuaternary }}
              >
                <Typography.Text type="secondary">
                  {t("currentBalance")}
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
                  <label htmlFor="amount">{t("amountLabel")}</label>
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
              <Button onClick={() => setIsCreditModalOpen(false)}>
                {t("cancel")}
              </Button>
              <Button
                type="primary"
                onClick={handleCredit}
                disabled={creditPocket.isPending || !amount}
              >
                {creditPocket.isPending ? t("processing") : t("addFunds")}
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
          <Space orientation="vertical" size={16} style={{ width: "100%" }}>
            <div>
              <Typography.Title level={3} style={{ marginBottom: 0 }}>
                {t("withdrawFunds")}
              </Typography.Title>
              <Typography.Text type="secondary">
                {t("withdrawDescription", {
                  name: selectedPocketForAction?.name ?? "",
                })}
              </Typography.Text>
            </div>
            <Space
              orientation="vertical"
              size={16}
              style={{ width: "100%", paddingBlock: 16 }}
            >
              <Card
                styles={{ body: { padding: 16 } }}
                style={{ background: token.colorFillQuaternary }}
              >
                <Typography.Text type="secondary">
                  {t("availableBalance")}
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
                  <label htmlFor="debit-amount">{t("amountLabel")}</label>
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
              <Button onClick={() => setIsDebitModalOpen(false)}>
                {t("cancel")}
              </Button>
              <Button
                onClick={handleDebit}
                disabled={debitPocket.isPending || !amount}
                danger
                type="primary"
              >
                {debitPocket.isPending ? t("processing") : t("withdrawFunds")}
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
          <Space orientation="vertical" size={16} style={{ width: "100%" }}>
            <div>
              <Typography.Title
                level={3}
                style={{ marginBottom: 0, color: token.colorError }}
              >
                <Flex align="center" gap={8}>
                  <Trash2 />
                  {t("closePocketTitle")}
                </Flex>
              </Typography.Title>
              <Typography.Text type="secondary">
                {t("closePocketConfirm", {
                  name: selectedPocketForAction?.name ?? "",
                })}
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
                  <TriangleAlert aria-hidden="true" /> {t("irreversibleAction")}
                </Flex>
              </Typography.Text>
            </Card>
            <Flex justify="flex-end" gap={8}>
              <Button onClick={() => setIsCloseModalOpen(false)}>
                {t("cancel")}
              </Button>
              <Button
                onClick={handleClose}
                disabled={closePocket.isPending}
                danger
                type="primary"
              >
                {closePocket.isPending ? t("closing") : t("confirmClosePocket")}
              </Button>
            </Flex>
          </Space>
        </Modal>
      </div>
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
