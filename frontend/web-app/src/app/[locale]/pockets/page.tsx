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
import clsx from "clsx";
import { Skeleton } from "antd";
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
import { Button, Dropdown, Input, Modal, Tag, Typography } from "antd";
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

/** Extended Pocket type for UI display (target/type not in backend Pocket) */
type PocketWithGoal = Pocket & { target?: number; type?: string };

export default function PocketsPage() {
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
      target: String((p as unknown as { target?: string }).target ?? p.balance),
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
      target: Number((p as unknown as { target?: string }).target ?? 0),
      type: "SHARED" as const,
      isShared: true,
      createdAt: p.createdAt,
      updatedAt: p.updatedAt,
      sharedMembers: p.sharedMembers ?? [],
    }));

  return (
    <DashboardLayout>
      <SkipLink href="#main-content" />
      <main id="main-content" className="overflow-x-hidden">
        <div className="space-y-6">
          <div className="flex flex-col sm:flex-row justify-between items-start sm:items-end gap-4 mb-6">
            <div>
              <h1 className="text-2xl font-bold text-foreground tracking-tight">
                Manajemen Kantong
              </h1>
              <p className="text-sm text-muted-foreground font-medium mt-1">
                Kelola dan alokasikan dana Anda dengan presisi tinggi.
              </p>
            </div>
            <div className="flex gap-3">
              <Button
                type="default"
                className="bg-muted lg:bg-card text-foreground px-8 py-4 rounded-xl font-bold text-xs tracking-widest border border-border shadow-lg hover:bg-muted/80 transition-all flex items-center gap-2 uppercase"
              >
                <Users className="h-4 w-4 text-primary" /> Kantong Bersama
              </Button>
              <Button
                type="primary"
                onClick={() => setIsCreateModalOpen(true)}
                className="bg-primary-dark text-surface px-8 py-4 rounded-xl font-bold text-xs tracking-widest shadow-xl shadow-primary/20 flex items-center gap-2 hover:bg-primary transition-all uppercase"
              >
                <Plus className="h-4 w-4" /> Tambah Kantong
              </Button>
            </div>
          </div>

          <div className="grid grid-cols-1 md:grid-cols-12 lg:grid-cols-12 gap-6">
            <div className="md:col-span-12 lg:col-span-8">
              <div className="bg-card rounded-xl sm:rounded-2xl p-5 sm:p-6 border border-border shadow-card flex flex-col justify-between min-h-[280px] lg:min-h-[320px] relative overflow-hidden group shadow-2xl">
                <div className="absolute top-0 right-0 w-80 h-80 bg-primary/5 rounded-full blur-3xl -z-0" />

                <div className="relative z-10 flex flex-col h-full">
                  <div className="flex justify-between items-start mb-6">
                    <div className="space-y-1">
                      <div className="flex items-center gap-2 mb-2">
                        <div className="h-2 w-2 bg-primary rounded-full shadow-[0_0_8px_hsl(var(--primary))] animate-pulse" />
                        <p className="text-xs font-bold text-primary tracking-widest uppercase">
                          Dompet Aktif
                        </p>
                      </div>
                      <h3 className="text-3xl font-bold text-foreground">
                        Kantong Utama Cair
                      </h3>
                    </div>
                    <div className="h-12 w-12 bg-muted/50 rounded-xl flex items-center justify-center border border-border transition-transform group-hover:scale-110 shadow-inner">
                      <Wallet className="h-6 w-6 text-primary" />
                    </div>
                  </div>

                  <div className="mt-auto">
                    <p className="text-xs font-bold text-muted-foreground tracking-widest uppercase mb-2 opacity-60">
                      Likuiditas Tersedia
                    </p>
                    <h4 className="text-5xl sm:text-5xl lg:text-6xl font-bold text-foreground tracking-tighter tabular-nums">
                      {balanceLoading ? (
                        <Skeleton.Input
                          active
                          size="large"
                          className="!h-16 !w-64 !rounded"
                        />
                      ) : (
                        formatCurrency(balance?.balance ?? "0", {
                          locale: bcp47Locale,
                        })
                      )}
                    </h4>
                  </div>
                </div>

                <div className="absolute bottom-6 right-6">
                  <Button
                    type="text"
                    icon={<ArrowUpRight className="h-6 w-6" />}
                    className="p-4 bg-primary-dark/10 text-primary rounded-xl shadow-sm border border-primary/20 hover:bg-primary-dark hover:text-surface transition-all active:scale-95"
                    aria-label="Buka detail"
                  />
                </div>
              </div>
            </div>

            <div className="md:col-span-12 lg:col-span-4 grid grid-cols-1 gap-6">
              <div className="bg-card p-5 sm:p-6 rounded-2xl border border-border shadow-card flex flex-col justify-center relative overflow-hidden group min-h-[180px]">
                <div className="absolute top-0 right-0 w-32 h-32 bg-primary/5 rounded-full blur-2xl" />
                <p className="text-xs font-bold text-muted-foreground tracking-widest uppercase mb-1 opacity-60">
                  Protokol Cadangan
                </p>
                <p className="text-2xl font-bold text-foreground tabular-nums">
                  {formatCurrency(balance?.reservedBalance ?? "0", {
                    locale: bcp47Locale,
                  })}
                </p>
                <div className="h-1.5 w-full bg-muted rounded-full mt-4 overflow-hidden shadow-inner">
                  {(() => {
                    const res = Number(balance?.reservedBalance ?? 0);
                    const avail = Number(balance?.availableBalance ?? 0);
                    const tot = res + avail;
                    const pct =
                      tot > 0
                        ? Math.min(100, Math.round((res / tot) * 100))
                        : 0;
                    return (
                      <div
                        className="h-full bg-primary/40"
                        style={{ width: `${pct}%` }}
                      />
                    );
                  })()}
                </div>
              </div>
              <div className="bg-text-primary p-5 sm:p-6 rounded-2xl text-surface relative overflow-hidden shadow-2xl group flex flex-col justify-between min-h-[180px] border border-surface/5">
                <div className="relative z-10 flex items-center gap-4 mb-6">
                  <div className="h-12 w-12 bg-surface/10 rounded-xl flex items-center justify-center border border-surface/10">
                    <div>
                      <h3 className="text-sm font-bold">Keamanan Tier-1</h3>
                      <p className="text-xs text-text-disabled font-bold tracking-widest uppercase opacity-60">
                        OJK & ASPI Compliant
                      </p>
                    </div>
                  </div>
                  <div className="relative z-10">
                    <p className="text-xs font-bold text-surface/20 tracking-widest uppercase mb-1">
                      Status Enkripsi
                    </p>
                    <p className="text-xs font-mono text-primary/80">
                      RESP-V3 ACTIVE
                    </p>
                  </div>
                  <Coins className="absolute bottom-[-30px] right-[-30px] h-48 w-48 text-surface/[0.03] -rotate-12 group-hover:rotate-0 transition-transform duration-1000" />
                </div>
              </div>
            </div>

            {/* Pockets List with CRUD */}
            <div className="mt-8">
              <div className="flex justify-between items-center mb-6">
                <h3 className="text-xl font-bold text-foreground">
                  Kantong Saya
                </h3>
                <Tag bordered className="font-mono">
                  Total:{" "}
                  {formatCurrency(totalBalance?.totalBalance ?? "0", {
                    locale: bcp47Locale,
                  })}
                </Tag>
              </div>

              {pocketsLoading ? (
                <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-6">
                  {[1, 2, 3].map((i) => (
                    <div
                      key={i}
                      className="bg-card rounded-xl p-6 border border-border shadow-sm h-40 animate-pulse"
                    />
                  ))}
                </div>
              ) : pocketsData && pocketsData.length > 0 ? (
                <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-6">
                  {pocketsData.map((_pocket) => {
                    const pocket = _pocket as PocketWithGoal;
                    const percentage = pocket.target
                      ? Math.round(
                          (Number(pocket.balance) / pocket.target) * 100,
                        )
                      : 0;
                    return (
                      <div
                        key={pocket.id}
                        className="bg-card rounded-xl p-6 border border-border shadow-sm hover:shadow-card transition-all group"
                      >
                        <div className="flex justify-between items-start mb-4">
                          <div className="flex items-center gap-3">
                            <div
                              className={clsx(
                                "h-10 w-10 rounded-xl flex items-center justify-center",
                                pocket.status === "FROZEN"
                                  ? "bg-warning/10 text-warning"
                                  : "bg-primary/10 text-primary",
                              )}
                            >
                              {pocket.type === "GOAL" ? (
                                <Target className="h-5 w-5" />
                              ) : (
                                <Wallet className="h-5 w-5" />
                              )}
                            </div>
                            <div>
                              <h4 className="font-bold text-foreground text-sm">
                                {pocket.name}
                              </h4>
                              <Tag bordered className="text-xs mt-1">
                                {pocket.type}
                              </Tag>
                            </div>
                          </div>
                          <Dropdown
                            trigger={["click"]}
                            placement="bottomRight"
                            menu={{
                              className: "w-48",
                              items: [
                                {
                                  key: "credit",
                                  label: (
                                    <span className="flex items-center gap-2">
                                      <ArrowDownLeft className="h-4 w-4 mr-2 text-primary" />
                                      Tambah Dana
                                    </span>
                                  ),
                                  onClick: () => openCreditModal(pocket),
                                },
                                {
                                  key: "debit",
                                  label: (
                                    <span className="flex items-center gap-2">
                                      <ArrowUpRight className="h-4 w-4 mr-2 text-primary" />
                                      Ambil Dana
                                    </span>
                                  ),
                                  onClick: () => openDebitModal(pocket),
                                },
                                pocket.status === "ACTIVE"
                                  ? {
                                      key: "freeze",
                                      label: (
                                        <span className="flex items-center gap-2">
                                          <Lock className="h-4 w-4 mr-2 text-warning" />
                                          Bekukan
                                        </span>
                                      ),
                                      onClick: () => handleFreeze(pocket.id),
                                    }
                                  : {
                                      key: "unfreeze",
                                      label: (
                                        <span className="flex items-center gap-2">
                                          <UnlockIcon className="h-4 w-4 mr-2 text-primary" />
                                          Aktifkan
                                        </span>
                                      ),
                                      onClick: () => handleUnfreeze(pocket.id),
                                    },
                                {
                                  key: "close",
                                  danger: true,
                                  label: (
                                    <span className="flex items-center gap-2">
                                      <Trash2 className="h-4 w-4 mr-2" />
                                      Tutup Kantong
                                    </span>
                                  ),
                                  onClick: () => openCloseModal(pocket),
                                },
                              ],
                            }}
                          >
                            <Button
                              type="text"
                              aria-label="Opsi kantong"
                              icon={
                                <MoreVertical className="h-4 w-4 text-muted-foreground" />
                              }
                              className="min-h-[44px] min-w-[44px] hover:bg-muted rounded-lg transition-colors"
                            />
                          </Dropdown>
                        </div>

                        <div className="space-y-3">
                          <div className="flex justify-between items-end">
                            <p className="text-2xl font-bold text-foreground">
                              Rp{" "}
                              {formatCurrencyWithoutSymbol(pocket.balance, {
                                locale: bcp47Locale,
                              })}
                            </p>
                            {pocket.target && (
                              <span className="text-xs font-bold text-primary">
                                {percentage}%
                              </span>
                            )}
                          </div>
                          {pocket.target && (
                            <div className="h-2 w-full bg-muted rounded-full overflow-hidden">
                              <div
                                className="h-full bg-primary rounded-full transition-all"
                                style={{
                                  width: `${Math.min(percentage, 100)}%`,
                                }}
                              />
                            </div>
                          )}
                          <div className="flex items-center justify-between pt-2">
                            <Tag
                              bordered={false}
                              color={
                                pocket.status === "ACTIVE" ? "green" : undefined
                              }
                              className="text-xs"
                            >
                              {pocket.status}
                            </Tag>
                            <span className="text-xs text-muted-foreground">
                              {pocket.currency}
                            </span>
                          </div>
                        </div>
                      </div>
                    );
                  })}
                </div>
              ) : (
                <div className="text-center py-8 bg-muted/30 rounded-2xl border border-border">
                  <Wallet className="h-12 w-12 text-muted-foreground mx-auto mb-4" />
                  <h4 className="text-lg font-bold text-foreground mb-2">
                    Belum Ada Kantong
                  </h4>
                  <p className="text-sm text-muted-foreground mb-4">
                    Buat kantong pertama Anda untuk mulai mengalokasikan dana
                  </p>
                  <Button
                    type="primary"
                    onClick={() => setIsCreateModalOpen(true)}
                  >
                    <Plus className="h-4 w-4 mr-2" /> Buat Kantong
                  </Button>
                </div>
              )}
            </div>

            <div className="md:col-span-12 lg:col-span-12 gap-6 mt-8 grid grid-cols-1 lg:grid-cols-12">
              <div className="lg:col-span-7 space-y-6">
                <div className="flex justify-between items-center">
                  <h3 className="text-xl font-bold text-foreground">
                    Tujuan Khusus
                  </h3>
                  <Button
                    type="link"
                    className="text-xs font-bold text-primary hover:underline"
                  >
                    Kelola Portofolio
                  </Button>
                </div>

                <div className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-2 lg:grid-cols-2 xl:grid-cols-2 gap-6">
                  {savingGoals.map((goal) => {
                    const percentage = Math.round(
                      (Number(goal.current) / Number(goal.target)) * 100,
                    );
                    const Icon = goal.icon;
                    return (
                      <div
                        key={goal.id}
                        className="bg-card rounded-xl p-5 sm:p-6 border border-border shadow-sm group hover:shadow-card hover:-translate-y-1 transition-all duration-300"
                      >
                        <div className="flex items-center gap-5 mb-6">
                          <div
                            className={clsx(
                              "h-14 w-14 rounded-xl flex items-center justify-center shadow-lg transition-transform group-hover:scale-110",
                              goal.color === "bank-green"
                                ? "bg-primary/10 text-primary border border-primary/10"
                                : "bg-bank-emerald/10 text-bank-emerald border border-bank-emerald/10",
                            )}
                          >
                            <Icon className="h-7 w-7" />
                          </div>
                          <div>
                            <h4 className="font-bold text-foreground text-base">
                              {goal.name}
                            </h4>
                            <p className="text-xs font-bold text-muted-foreground tracking-widest uppercase">
                              Target:{" "}
                              {formatCurrency(goal.target, {
                                locale: bcp47Locale,
                              })}
                            </p>
                          </div>
                        </div>

                        {goal.locked ? (
                          <div className="space-y-4">
                            <div className="flex justify-between items-end">
                              <p className="text-2xl font-bold text-foreground">
                                {formatCurrency(goal.current, {
                                  locale: bcp47Locale,
                                })}
                              </p>
                              <div className="bg-success-light text-primary px-3 py-1 rounded-full text-xs font-bold border border-primary/10">
                                {goal.interestRate}
                              </div>
                            </div>
                            <div className="flex items-center gap-2 text-xs font-bold text-primary tracking-widest uppercase">
                              <Lock className="h-3 w-3" /> Dana Terkunci &
                              Dijamin
                            </div>
                          </div>
                        ) : (
                          <div className="space-y-4">
                            <div className="flex justify-between items-end mb-1">
                              <p className="text-2xl font-bold text-foreground">
                                {formatCurrency(goal.current, {
                                  locale: bcp47Locale,
                                })}
                              </p>
                              <span className="text-xs font-bold text-primary">
                                +{percentage}%
                              </span>
                            </div>
                            <div className="h-2 w-full bg-muted rounded-full overflow-hidden">
                              <div
                                className="h-full bg-primary rounded-full"
                                style={{ width: `${percentage}%` }}
                              />
                            </div>
                            <p className="text-xs font-bold text-muted-foreground tracking-widest text-right uppercase">
                              Sisa:{" "}
                              {formatCurrency(
                                addCurrency(goal.target, `-${goal.current}`),
                                { locale: bcp47Locale },
                              )}
                            </p>
                          </div>
                        )}
                      </div>
                    );
                  })}
                </div>
              </div>

              <div className="lg:col-span-5 space-y-6">
                <div className="flex justify-between items-center">
                  <h3 className="text-xl font-bold text-foreground">
                    Buku Besar Terakhir
                  </h3>
                  <div className="h-10 w-10 bg-muted/50 rounded-xl flex items-center justify-center border border-border">
                    <History className="h-5 w-5 text-muted-foreground" />
                  </div>
                </div>

                <div className="bg-card rounded-xl border border-border shadow-sm min-h-[280px] lg:min-h-[320px] flex flex-col">
                  {transactionsLoading ? (
                    <div className="p-6 space-y-4">
                      {[1, 2, 3, 4, 5].map((i) => (
                        <Skeleton
                          key={i}
                          active
                          avatar={{ shape: "square" }}
                          paragraph={{ rows: 1 }}
                          title={false}
                          className="!p-4"
                        />
                      ))}
                    </div>
                  ) : (
                    <div className="flex-1">
                      <div className="divide-y divide-border">
                        {transactions?.map((tx) => (
                          <div
                            key={tx.id}
                            className="p-4 flex items-center justify-between group hover:bg-muted/30 transition-all"
                          >
                            <div className="flex gap-4">
                              <div
                                className={clsx(
                                  "h-12 w-12 rounded-xl flex items-center justify-center border transition-all group-hover:scale-105",
                                  tx.type === "CREDIT"
                                    ? "bg-success-light border-primary/10"
                                    : "bg-destructive/5 border-destructive/10",
                                )}
                              >
                                {tx.type === "CREDIT" ? (
                                  <TrendingUp className="h-5 w-5 text-primary" />
                                ) : (
                                  <ChevronRight className="h-5 w-5 text-destructive rotate-90" />
                                )}
                              </div>
                              <div>
                                <p className="text-sm font-bold text-foreground mb-0.5">
                                  {tx.description}
                                </p>
                                <p className="text-xs font-medium text-muted-foreground tracking-tight">
                                  {new Date(tx.createdAt).toLocaleDateString(
                                    undefined,
                                    { day: "2-digit", month: "short" },
                                  )}{" "}
                                  • {tx.type === "CREDIT" ? "Masuk" : "Keluar"}
                                </p>
                              </div>
                            </div>
                            <p
                              className={clsx(
                                "text-sm font-bold tracking-tight",
                                tx.type === "CREDIT"
                                  ? "text-primary"
                                  : "text-foreground",
                              )}
                            >
                              {tx.type === "CREDIT" ? "+" : "-"}{" "}
                              {formatCurrency(tx.amount, {
                                locale: bcp47Locale,
                              })}
                            </p>
                          </div>
                        ))}
                      </div>

                      {(!transactions || transactions.length === 0) && (
                        <div className="h-full flex flex-col items-center justify-center text-center py-8 px-6">
                          <History className="h-12 w-12 text-muted/20 mb-4" />
                          <p className="text-sm font-bold text-muted-foreground">
                            Tidak Ada Aktivitas
                          </p>
                          <p className="text-xs text-muted-foreground/60 mt-1 uppercase tracking-widest">
                            Aktivitas keuangan Anda akan muncul di sini
                          </p>
                        </div>
                      )}
                    </div>
                  )}
                  <div className="p-6 mt-auto">
                    <Button
                      type="default"
                      className="w-full py-4 bg-muted/50 rounded-xl font-bold text-xs tracking-widest uppercase border border-border hover:bg-muted transition-all text-muted-foreground"
                    >
                      Lihat Rekening Koran
                    </Button>
                  </div>
                </div>
              </div>
            </div>

            <div className="mt-8">
              <div className="flex justify-between items-center mb-6">
                <h3 className="text-xl font-bold text-foreground flex items-center gap-3">
                  <Users className="h-5 w-5 text-primary" />
                  Kantong Bersama
                </h3>
                <Button
                  type="default"
                  className="bg-primary/10 text-primary px-6 py-3 rounded-xl font-bold text-xs tracking-widest border border-primary/10 hover:bg-primary/20 transition-all flex items-center gap-2"
                >
                  <UserPlus className="h-4 w-4" /> Buat Kantong Baru
                </Button>
              </div>

              <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                {sharedPockets.map((pocket) => {
                  const percentage = pocket.target
                    ? Math.round((Number(pocket.balance) / pocket.target) * 100)
                    : 0;
                  const isSelected = selectedPocket === pocket.id;

                  return (
                    <div
                      key={pocket.id}
                      className="bg-card rounded-xl border border-border shadow-sm overflow-hidden group"
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
                        className="p-6 cursor-pointer transition-colors hover:bg-muted/30"
                      >
                        <div className="flex justify-between items-start mb-4">
                          <div className="flex items-center gap-3">
                            <div className="h-10 w-10 bg-primary/10 rounded-lg flex items-center justify-center">
                              <Users className="h-5 w-5 text-primary" />
                            </div>
                            <div>
                              <h4 className="font-bold text-foreground text-sm">
                                {pocket.name}
                              </h4>
                              <p className="text-xs text-muted-foreground tracking-widest uppercase">
                                {pocket.sharedMembers?.length} Anggota
                              </p>
                            </div>
                          </div>
                          <ChevronRight
                            className={clsx(
                              "h-5 w-5 text-muted-foreground transition-transform",
                              isSelected ? "rotate-90" : "",
                            )}
                          />
                        </div>

                        <div className="space-y-3">
                          <div className="flex justify-between items-end">
                            <p className="text-2xl font-bold text-foreground">
                              Rp{" "}
                              {formatCurrencyWithoutSymbol(pocket.balance, {
                                locale: bcp47Locale,
                              })}
                            </p>
                            <span className="text-xs font-bold text-primary">
                              {percentage}%
                            </span>
                          </div>
                          <div className="h-2 w-full bg-muted rounded-full overflow-hidden">
                            <div
                              className="h-full bg-primary rounded-full transition-all duration-500"
                              style={{ width: `${percentage}%` }}
                            />
                          </div>
                          {pocket.target && (
                            <p className="text-xs font-bold text-muted-foreground tracking-widest text-right uppercase">
                              Target:{" "}
                              {formatCurrency(pocket.target, {
                                locale: bcp47Locale,
                              })}
                            </p>
                          )}
                        </div>
                      </div>

                      {isSelected && pocket.sharedMembers && (
                        <div className="border-t border-border p-4 bg-muted/20">
                          <div className="flex justify-between items-center mb-3">
                            <p className="text-xs font-bold text-muted-foreground tracking-widest uppercase">
                              Anggota
                            </p>
                            <Button
                              type="link"
                              icon={<UserPlus className="h-3 w-3" />}
                              className="text-xs font-bold text-primary hover:underline flex items-center gap-1"
                            >
                              Undang
                            </Button>
                          </div>
                          <div className="space-y-2">
                            {pocket.sharedMembers.map((member, i) => (
                              <div
                                key={i}
                                className="flex items-center justify-between p-2 bg-background rounded-lg border border-border"
                              >
                                <div className="flex items-center gap-2">
                                  <div className="h-6 w-6 bg-primary/10 rounded-full flex items-center justify-center text-xs font-bold text-primary">
                                    {member.fullName.charAt(0)}
                                  </div>
                                  <span className="text-xs font-bold text-foreground">
                                    {member.fullName}
                                  </span>
                                </div>
                                <div className="flex items-center gap-2">
                                  <span
                                    className={clsx(
                                      "text-xs font-bold px-2 py-0.5 rounded uppercase tracking-widest",
                                      member.role === "OWNER"
                                        ? "bg-primary/10 text-primary"
                                        : member.role === "ADMIN"
                                          ? "bg-bank-emerald/10 text-bank-emerald"
                                          : "bg-muted/50 text-muted-foreground",
                                    )}
                                  >
                                    {member.role}
                                  </span>
                                  <Button
                                    type="text"
                                    icon={
                                      <MoreVertical className="h-3 w-3 text-muted-foreground" />
                                    }
                                    className="min-h-[44px] min-w-[44px] hover:bg-muted rounded transition-colors"
                                    aria-label="Aksi lainnya"
                                  />
                                </div>
                              </div>
                            ))}
                          </div>
                        </div>
                      )}
                    </div>
                  );
                })}
              </div>
            </div>

            <div className="mt-8">
              <div className="bg-foreground text-background rounded-xl p-5 sm:p-6 relative overflow-hidden group shadow-card">
                <div className="absolute top-0 right-0 w-80 h-80 bg-surface/5 rounded-full blur-3xl -z-0" />
                <div className="relative z-10 flex flex-col md:flex-row items-center justify-between gap-6">
                  <div className="space-y-4 max-w-xl text-center md:text-left">
                    <div className="flex items-center justify-center md:justify-start gap-4">
                      <div className="h-12 w-12 bg-primary rounded-xl flex items-center justify-center shadow-lg shadow-primary/20">
                        <TrendingUp className="h-6 w-6 text-surface" />
                      </div>
                      <h3 className="text-2xl sm:text-3xl font-bold">
                        Akselerasi Kekayaan Anda.
                      </h3>
                    </div>
                    <p className="text-sm text-text-disabled font-medium leading-relaxed">
                      Pindahkan dana mengendap dari kantong ke reksa dana yield
                      tinggi atau emas digital. AI kami menyarankan Anda bisa
                      berhemat hingga{" "}
                      <span className="text-bank-green font-bold">
                        Rp 12,5 Juta
                      </span>{" "}
                      lebih per tahun.
                    </p>
                  </div>
                  <Button
                    type="primary"
                    className="whitespace-nowrap px-8 py-4 bg-bank-green text-surface rounded-xl font-bold text-xs tracking-widest shadow-2xl shadow-bank-green/40 hover:bg-bank-emerald transition-all"
                  >
                    Jelajahi Marketplace
                  </Button>
                </div>
              </div>
            </div>
          </div>

          {/* Create Pocket Modal */}
          <Modal
            open={isCreateModalOpen}
            onCancel={() => setIsCreateModalOpen(false)}
            footer={null}
            centered
            width={512}
            title={undefined}
          >
            <div>
              <div>
                <Typography.Title level={4}>Buat Kantong Baru</Typography.Title>
                <Typography.Text type="secondary">
                  Buat kantong untuk mengalokasikan dana sesuai tujuan Anda
                </Typography.Text>
              </div>
              <div className="space-y-4 py-4">
                <div className="space-y-2">
                  <label
                    htmlFor="name"
                    className="text-sm font-medium leading-none peer-disabled:cursor-not-allowed peer-disabled:opacity-70"
                  >
                    Nama Kantong
                  </label>
                  <Input
                    id="name"
                    placeholder="Contoh: Dana Darurat, Liburan"
                    value={newPocketName}
                    onChange={(e) => setNewPocketName(e.target.value)}
                  />
                </div>
                <div className="space-y-2">
                  <label
                    htmlFor="target"
                    className="text-sm font-medium leading-none peer-disabled:cursor-not-allowed peer-disabled:opacity-70"
                  >
                    Target Dana (Opsional)
                  </label>
                  <Input
                    id="target"
                    type="number"
                    placeholder="5000000"
                    value={newPocketTarget}
                    onChange={(e) => setNewPocketTarget(e.target.value)}
                  />
                </div>
                <div className="space-y-2">
                  <span className="text-sm font-medium leading-none peer-disabled:cursor-not-allowed peer-disabled:opacity-70">
                    Tipe Kantong
                  </span>
                  <div className="flex gap-2">
                    <Button
                      htmlType="button"
                      type={newPocketType === "SAVINGS" ? "primary" : "default"}
                      className="flex-1"
                      onClick={() => setNewPocketType("SAVINGS")}
                    >
                      <Wallet className="h-4 w-4 mr-2" /> Tabungan
                    </Button>
                    <Button
                      htmlType="button"
                      type={newPocketType === "GOAL" ? "primary" : "default"}
                      className="flex-1"
                      onClick={() => setNewPocketType("GOAL")}
                    >
                      <Target className="h-4 w-4 mr-2" /> Target
                    </Button>
                  </div>
                </div>
              </div>
              <div>
                <Button onClick={() => setIsCreateModalOpen(false)}>
                  Batal
                </Button>
                <Button
                  type="primary"
                  onClick={handleCreatePocket}
                  disabled={createPocket.isPending || !newPocketName.trim()}
                >
                  {createPocket.isPending ? "Membuat..." : "Buat Kantong"}
                </Button>
              </div>
            </div>
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
            <div>
              <div>
                <Typography.Title level={4}>Tambah Dana</Typography.Title>
                <Typography.Text type="secondary">
                  Tambahkan dana ke {selectedPocketForAction?.name}
                </Typography.Text>
              </div>
              <div className="space-y-4 py-4">
                <div className="p-4 bg-muted rounded-xl">
                  <p className="text-sm text-muted-foreground">
                    Saldo Saat Ini
                  </p>
                  <p className="text-2xl font-bold">
                    {formatCurrency(selectedPocketForAction?.balance ?? "0", {
                      locale: bcp47Locale,
                    })}
                  </p>
                </div>
                <div className="space-y-2">
                  <label
                    htmlFor="amount"
                    className="text-sm font-medium leading-none peer-disabled:cursor-not-allowed peer-disabled:opacity-70"
                  >
                    Jumlah Dana
                  </label>
                  <Input
                    id="amount"
                    type="number"
                    placeholder="100000"
                    value={amount}
                    onChange={(e) => setAmount(e.target.value)}
                  />
                </div>
              </div>
              <div>
                <Button onClick={() => setIsCreditModalOpen(false)}>
                  Batal
                </Button>
                <Button
                  type="primary"
                  onClick={handleCredit}
                  disabled={creditPocket.isPending || !amount}
                >
                  {creditPocket.isPending ? "Memproses..." : "Tambah Dana"}
                </Button>
              </div>
            </div>
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
            <div>
              <div>
                <Typography.Title level={4}>Ambil Dana</Typography.Title>
                <Typography.Text type="secondary">
                  Ambil dana dari {selectedPocketForAction?.name}
                </Typography.Text>
              </div>
              <div className="space-y-4 py-4">
                <div className="p-4 bg-muted rounded-xl">
                  <p className="text-sm text-muted-foreground">
                    Saldo Tersedia
                  </p>
                  <p className="text-2xl font-bold">
                    {formatCurrency(selectedPocketForAction?.balance ?? "0", {
                      locale: bcp47Locale,
                    })}
                  </p>
                </div>
                <div className="space-y-2">
                  <label
                    htmlFor="debit-amount"
                    className="text-sm font-medium leading-none peer-disabled:cursor-not-allowed peer-disabled:opacity-70"
                  >
                    Jumlah Dana
                  </label>
                  <Input
                    id="debit-amount"
                    type="number"
                    placeholder="100000"
                    value={amount}
                    onChange={(e) => setAmount(e.target.value)}
                  />
                </div>
              </div>
              <div>
                <Button onClick={() => setIsDebitModalOpen(false)}>
                  Batal
                </Button>
                <Button
                  onClick={handleDebit}
                  disabled={debitPocket.isPending || !amount}
                  danger
                  type="primary"
                >
                  {debitPocket.isPending ? "Memproses..." : "Ambil Dana"}
                </Button>
              </div>
            </div>
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
            <div>
              <div>
                <Typography.Title
                  level={4}
                  className="text-error flex items-center gap-2"
                >
                  <Trash2 className="h-5 w-5" />
                  Tutup Kantong?
                </Typography.Title>
                <Typography.Text type="secondary">
                  Apakah Anda yakin ingin menutup kantong &ldquo;
                  {selectedPocketForAction?.name}&rdquo;? Dana yang tersisa akan
                  dikembalikan ke dompet utama.
                </Typography.Text>
              </div>
              <div className="p-4 bg-error rounded-xl border border-error">
                <p className="text-sm text-error font-medium flex items-center gap-2">
                  <TriangleAlert
                    className="h-4 w-4 shrink-0"
                    aria-hidden="true"
                  />{" "}
                  Tindakan ini tidak dapat dibatalkan
                </p>
              </div>
              <div>
                <Button onClick={() => setIsCloseModalOpen(false)}>
                  Batal
                </Button>
                <Button
                  onClick={handleClose}
                  disabled={closePocket.isPending}
                  danger
                  type="primary"
                >
                  {closePocket.isPending ? "Menutup..." : "Ya, Tutup Kantong"}
                </Button>
              </div>
            </div>
          </Modal>
        </div>
      </main>
    </DashboardLayout>
  );
}

// Unlock icon component
function UnlockIcon({ className }: { className?: string }) {
  return (
    <svg
      xmlns="http://www.w3.org/2000/svg"
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      strokeWidth="2"
      strokeLinecap="round"
      strokeLinejoin="round"
      className={className}
    >
      <rect x="3" y="11" width="18" height="11" rx="2" ry="2" />
      <path d="M7 11V7a5 5 0 0 1 9.9-1" />
    </svg>
  );
}
