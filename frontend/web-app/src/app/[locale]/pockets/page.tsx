'use client';

import React, { useState } from 'react';
import { Plus, Target, Lock, TrendingUp, ChevronRight, Wallet, History, ArrowUpRight, ShieldCheck, Coins, Users, UserPlus, MoreVertical, X, ArrowDownLeft, Trash2, CreditCard as CardIcon, Edit3 } from "lucide-react"; // eslint-disable-line @typescript-eslint/no-unused-vars
import { useQuery } from '@tanstack/react-query';
import { useLocale } from 'next-intl';
import { BalanceResponse, WalletTransaction, Pocket } from '@/types';
import api from '@/lib/api';
import DashboardLayout from "@/components/DashboardLayout";
import clsx from 'clsx';
import { PageTransition, StaggerContainer, StaggerItem, ButtonMotion } from '@/components/ui/Motion';
import { SkeletonBalance, SkeletonTransaction } from '@/components/ui/skeleton';
import { useAuthStore } from '@/stores';
import {
  usePockets,
  usePocketsTotalBalance,
  useCreatePocket,
  useCreditPocket,
  useDebitPocket,
  useFreezePocket,
  useUnfreezePocket,
  useClosePocket
} from '@/hooks';
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Badge } from "@/components/ui/badge";
import { toast } from 'sonner';

interface SharedMember {
  accountId: string;
  fullName: string;
  role: 'OWNER' | 'ADMIN' | 'MEMBER';
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
    const accountId = useAuthStore((state) => state.accountId) || '';
    const locale = useLocale();
    const bcp47Locale = locale === 'id' ? 'id-ID' : 'en-US';
    const [selectedPocket, setSelectedPocket] = useState<string | null>(null);
    const [isCreateModalOpen, setIsCreateModalOpen] = useState(false);
    const [isCreditModalOpen, setIsCreditModalOpen] = useState(false);
    const [isDebitModalOpen, setIsDebitModalOpen] = useState(false);
    const [isCloseModalOpen, setIsCloseModalOpen] = useState(false);
    const [selectedPocketForAction, setSelectedPocketForAction] = useState<Pocket | null>(null);

    // Form states
    const [newPocketName, setNewPocketName] = useState('');
    const [newPocketTarget, setNewPocketTarget] = useState('');
    const [newPocketType, setNewPocketType] = useState<'SAVINGS' | 'GOAL'>('SAVINGS');
    const [amount, setAmount] = useState('');

    const { data: pocketsData, isLoading: pocketsLoading } = usePockets();
    const { data: totalBalance } = usePocketsTotalBalance('IDR');
    const createPocket = useCreatePocket();
    const creditPocket = useCreditPocket();
    const debitPocket = useDebitPocket();
    const freezePocket = useFreezePocket();
    const unfreezePocket = useUnfreezePocket();
    const closePocket = useClosePocket();

    const { data: balance, isLoading: balanceLoading } = useQuery({
        queryKey: ['wallet-balance', accountId],
        queryFn: async () => {
            const response = await api.get<BalanceResponse>(`/wallets/${accountId}/balance`);
            return response.data;
        },
        enabled: !!accountId
    });

    const { data: transactions, isLoading: transactionsLoading } = useQuery({
        queryKey: ['wallet-transactions', accountId],
        queryFn: async () => {
            const response = await api.get<WalletTransaction[]>(`/wallets/${accountId}/transactions`);
            return response.data;
        },
        enabled: !!accountId
    });

    const handleCreatePocket = async () => {
        if (!newPocketName.trim()) {
            toast.error('Nama kantong wajib diisi');
            return;
        }

        try {
            await createPocket.mutateAsync({
                accountId,
                name: newPocketName,
                currency: 'IDR',
                description: newPocketTarget ? `Target: ${newPocketTarget}` : undefined,
            });
            toast.success('Kantong berhasil dibuat');
            setIsCreateModalOpen(false);
            setNewPocketName('');
            setNewPocketTarget('');
        } catch (error) { // eslint-disable-line @typescript-eslint/no-unused-vars
            toast.error('Gagal membuat kantong');
        }
    };

    const handleCredit = async () => {
        if (!selectedPocketForAction || !amount) return;

        try {
            await creditPocket.mutateAsync({
                pocketId: selectedPocketForAction.id,
                amount: parseFloat(amount),
                description: 'Top up pocket'
            });
            toast.success('Berhasil menambah dana');
            setIsCreditModalOpen(false);
            setAmount('');
            setSelectedPocketForAction(null);
        } catch (error) { // eslint-disable-line @typescript-eslint/no-unused-vars
            toast.error('Gagal menambah dana');
        }
    };

    const handleDebit = async () => {
        if (!selectedPocketForAction || !amount) return;

        try {
            await debitPocket.mutateAsync({
                pocketId: selectedPocketForAction.id,
                amount: parseFloat(amount),
                description: 'Withdraw from pocket'
            });
            toast.success('Berhasil mengambil dana');
            setIsDebitModalOpen(false);
            setAmount('');
            setSelectedPocketForAction(null);
        } catch (error) { // eslint-disable-line @typescript-eslint/no-unused-vars
            toast.error('Gagal mengambil dana');
        }
    };

    const handleFreeze = async (pocketId: string) => {
        try {
            await freezePocket.mutateAsync(pocketId);
            toast.success('Kantong berhasil dibekukan');
        } catch (error) { // eslint-disable-line @typescript-eslint/no-unused-vars
            toast.error('Gagal membekukan kantong');
        }
    };

    const handleUnfreeze = async (pocketId: string) => {
        try {
            await unfreezePocket.mutateAsync(pocketId);
            toast.success('Kantong berhasil diaktifkan kembali');
        } catch (error) { // eslint-disable-line @typescript-eslint/no-unused-vars
            toast.error('Gagal mengaktifkan kantong');
        }
    };

    const handleClose = async () => {
        if (!selectedPocketForAction) return;

        try {
            await closePocket.mutateAsync(selectedPocketForAction.id);
            toast.success('Kantong berhasil ditutup');
            setIsCloseModalOpen(false);
            setSelectedPocketForAction(null);
        } catch (error) { // eslint-disable-line @typescript-eslint/no-unused-vars
            toast.error('Gagal menutup kantong');
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

    const savingGoals = [
        {
            id: 1,
            name: 'Liburan Akhir Tahun',
            target: 10000000,
            current: 2500000,
            color: 'bank-green',
            icon: Target,
            isShared: false
        },
        {
            id: 2,
            name: 'Dana Darurat',
            target: 50000000,
            current: 50000000,
            color: 'bank-emerald',
            icon: Lock,
            interestRate: '4.5% p.a',
            locked: true,
            isShared: false
        }
    ];

    const sharedPockets: SharedPocket[] = [
        {
            id: 'shared-1',
            accountId: accountId,
            name: 'Tabungan Keluarga',
            balance: 15000000,
            target: 50000000,
            type: 'SHARED',
            isShared: true,
            createdAt: '2026-01-01T00:00:00Z',
            updatedAt: '2026-01-24T00:00:00Z',
            sharedMembers: [
                { accountId: 'acc-any123', fullName: 'Anya', role: 'OWNER', joinedAt: '2026-01-01T00:00:00Z' },
                { accountId: 'acc-bud456', fullName: 'Budi', role: 'ADMIN', joinedAt: '2026-01-02T00:00:00Z' },
                { accountId: 'acc-cit789', fullName: 'Citra', role: 'MEMBER', joinedAt: '2026-01-05T00:00:00Z' }
            ]
        },
        {
            id: 'shared-2',
            accountId: accountId,
            name: 'Dana Rekreasi Kantor',
            balance: 8500000,
            target: 30000000,
            type: 'SHARED',
            isShared: true,
            createdAt: '2026-01-10T00:00:00Z',
            updatedAt: '2026-01-24T00:00:00Z',
            sharedMembers: [
                { accountId: 'acc-any123', fullName: 'Anya', role: 'ADMIN', joinedAt: '2026-01-10T00:00:00Z' },
                { accountId: 'acc-dod012', fullName: 'Dodi', role: 'OWNER', joinedAt: '2026-01-10T00:00:00Z' }
            ]
        }
    ];

    const activePocket = selectedPocket // eslint-disable-line @typescript-eslint/no-unused-vars
        ? sharedPockets.find(p => p.id === selectedPocket)
        : null;

    return (
        <DashboardLayout>
            <PageTransition>
                <div className="space-y-12">
                    <StaggerContainer>
                        <StaggerItem>
                            <div className="flex flex-col sm:flex-row justify-between items-start sm:items-end gap-6 mb-8">
                                <div>
                                    <h2 className="text-3xl font-bold text-foreground">Manajemen Kantong</h2>
                                    <p className="text-sm text-muted-foreground font-medium mt-1">Kelola dan alokasikan dana Anda dengan presisi tinggi.</p>
                                </div>
                                <div className="flex gap-3">
                                    <ButtonMotion>
                                        <button
                                            className="bg-muted lg:bg-card text-foreground px-8 py-4 rounded-xl font-bold text-xs tracking-widest border border-border shadow-lg hover:bg-muted/80 transition-all flex items-center gap-2 uppercase"
                                        >
                                            <Users className="h-4 w-4 text-emerald-500" /> Kantong Bersama
                                        </button>
                                    </ButtonMotion>
                                    <ButtonMotion>
                                        <button
                                            onClick={() => setIsCreateModalOpen(true)}
                                            className="bg-emerald-600 text-white px-8 py-4 rounded-xl font-bold text-xs tracking-widest shadow-xl shadow-emerald-500/20 flex items-center gap-2 hover:bg-emerald-500 transition-all uppercase"
                                        >
                                            <Plus className="h-4 w-4" /> Tambah Kantong
                                        </button>
                                    </ButtonMotion>
                                </div>
                            </div>
                        </StaggerItem>

                        <div className="grid grid-cols-1 md:grid-cols-12 lg:grid-cols-12 gap-8">
                            <StaggerItem className="md:col-span-12 lg:col-span-8">
                                <div className="bg-card rounded-2xl p-8 sm:p-8 border border-border shadow-card flex flex-col justify-between min-h-[400px] relative overflow-hidden group shadow-2xl">
                                    <div className="absolute top-0 right-0 w-80 h-80 bg-emerald-500/5 rounded-full blur-3xl -z-0" />

                                    <div className="relative z-10 flex flex-col h-full">
                                        <div className="flex justify-between items-start mb-10">
                                            <div className="space-y-1">
                                                <div className="flex items-center gap-2 mb-2">
                                                    <div className="h-2 w-2 bg-emerald-500 rounded-full shadow-[0_0_8px_hsl(var(--primary))] animate-pulse" />
                                                    <p className="text-xs font-bold text-emerald-500 tracking-widest uppercase">Dompet Aktif</p>
                                                </div>
                                                <h3 className="text-3xl font-bold text-foreground">Kantong Utama Cair</h3>
                                            </div>
                                            <div className="h-12 w-12 bg-muted/50 rounded-xl flex items-center justify-center border border-border transition-transform group-hover:scale-110 shadow-inner">
                                                <Wallet className="h-6 w-6 text-emerald-500" />
                                            </div>
                                        </div>

                                        <div className="mt-auto">
                                            <p className="text-xs font-bold text-muted-foreground tracking-widest uppercase mb-2 opacity-60">Likuiditas Tersedia</p>
                                            <h4 className="text-5xl sm:text-5xl lg:text-6xl font-bold text-foreground tracking-tighter tabular-nums">
                                                {balanceLoading ? (
                                                    <SkeletonBalance />
                                                ) : (
                                                    `Rp ${(balance?.balance ?? 0).toLocaleString(bcp47Locale)}`
                                                )}
                                            </h4>
                                        </div>
                                    </div>

                                    <div className="absolute bottom-6 right-6">
                                        <button className="p-4 bg-emerald-600/10 text-emerald-500 rounded-xl shadow-sm border border-emerald-500/20 hover:bg-emerald-600 hover:text-white transition-all active:scale-95">
                                            <ArrowUpRight className="h-6 w-6" />
                                        </button>
                                    </div>
                                </div>
                            </StaggerItem>

                            <StaggerItem className="md:col-span-12 lg:col-span-4 grid grid-cols-1 gap-8">
                                <div className="bg-card p-8 rounded-2xl border border-border shadow-card flex flex-col justify-center relative overflow-hidden group min-h-[180px]">
                                    <div className="absolute top-0 right-0 w-32 h-32 bg-emerald-500/5 rounded-full blur-2xl" />
                                    <p className="text-xs font-bold text-muted-foreground tracking-widest uppercase mb-1 opacity-60">Protokol Cadangan</p>
                                    <p className="text-2xl font-bold text-foreground tabular-nums">Rp {(balance?.reservedBalance ?? 0).toLocaleString(bcp47Locale)}</p>
                                    <div className="h-1.5 w-full bg-muted rounded-full mt-4 overflow-hidden shadow-inner">
                                        <div className="h-full bg-emerald-500/40" style={{ width: '15%' }} />
                                    </div>
                                </div>
                                <div className="bg-gray-900 p-8 rounded-2xl text-white relative overflow-hidden shadow-2xl group flex flex-col justify-between min-h-[180px] border border-white/5">
                                    <div className="relative z-10 flex items-center gap-4 mb-6">
                                        <div className="h-12 w-12 bg-white/10 rounded-xl flex items-center justify-center border border-white/10 backdrop-blur-md">
                                            <ShieldCheck className="h-6 w-6 text-emerald-500" />
                                        </div>
                                        <div>
                                            <h3 className="text-sm font-bold">Keamanan Tier-1</h3>
                                            <p className="text-xs text-gray-400 font-bold tracking-widest uppercase opacity-60">OJK & ASPI Compliant</p>
                                        </div>
                                    </div>
                                    <div className="relative z-10">
                                        <p className="text-xs font-bold text-white/20 tracking-widest uppercase mb-1">Status Enkripsi</p>
                                        <p className="text-xs font-mono text-emerald-500/80">RESP-V3 ACTIVE</p>
                                    </div>
                                    <Coins className="absolute bottom-[-30px] right-[-30px] h-48 w-48 text-white/[0.03] -rotate-12 group-hover:rotate-0 transition-transform duration-1000" />
                                </div>
                            </StaggerItem>
                        </div>

                        {/* Pockets List with CRUD */}
                        <div className="mt-12">
                            <div className="flex justify-between items-center mb-8">
                                <h3 className="text-xl font-bold text-foreground">Kantong Saya</h3>
                                <Badge variant="outline" className="font-mono">
                                    Total: Rp {totalBalance?.totalBalance?.toLocaleString(bcp47Locale) || 0}
                                </Badge>
                            </div>

                            {pocketsLoading ? (
                                <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-6">
                                    {[1, 2, 3].map((i) => (
                                        <div key={i} className="bg-card rounded-xl p-6 border border-border shadow-sm h-40 animate-pulse" />
                                    ))}
                                </div>
                            ) : pocketsData && pocketsData.length > 0 ? (
                                <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-6">
                                    {pocketsData.map((_pocket) => {
                                        const pocket = _pocket as PocketWithGoal;
                                        const percentage = pocket.target ? Math.round((pocket.balance / pocket.target) * 100) : 0;
                                        return (
                                            <div key={pocket.id} className="bg-card rounded-xl p-6 border border-border shadow-sm hover:shadow-card transition-all group">
                                                <div className="flex justify-between items-start mb-4">
                                                    <div className="flex items-center gap-3">
                                                        <div className={clsx(
                                                            "h-10 w-10 rounded-xl flex items-center justify-center",
                                                            pocket.status === 'FROZEN' ? "bg-yellow-500/10 text-yellow-500" : "bg-emerald-500/10 text-emerald-500"
                                                        )}>
                                                            {pocket.type === 'GOAL' ? <Target className="h-5 w-5" /> : <Wallet className="h-5 w-5" />}
                                                        </div>
                                                        <div>
                                                            <h4 className="font-bold text-foreground text-sm">{pocket.name}</h4>
                                                            <Badge variant="outline" className="text-xs mt-1">
                                                                {pocket.type}
                                                            </Badge>
                                                        </div>
                                                    </div>
                                                    <DropdownMenu>
                                                        <DropdownMenuTrigger asChild>
                                                            <button className="p-2 hover:bg-muted rounded-lg transition-colors">
                                                                <MoreVertical className="h-4 w-4 text-muted-foreground" />
                                                            </button>
                                                        </DropdownMenuTrigger>
                                                        <DropdownMenuContent align="end" className="w-48">
                                                            <DropdownMenuItem onClick={() => openCreditModal(pocket)} className="cursor-pointer">
                                                                <ArrowDownLeft className="h-4 w-4 mr-2 text-emerald-500" />
                                                                Tambah Dana
                                                            </DropdownMenuItem>
                                                            <DropdownMenuItem onClick={() => openDebitModal(pocket)} className="cursor-pointer">
                                                                <ArrowUpRight className="h-4 w-4 mr-2 text-blue-500" />
                                                                Ambil Dana
                                                            </DropdownMenuItem>
                                                            {pocket.status === 'ACTIVE' ? (
                                                                <DropdownMenuItem onClick={() => handleFreeze(pocket.id)} className="cursor-pointer">
                                                                    <Lock className="h-4 w-4 mr-2 text-yellow-500" />
                                                                    Bekukan
                                                                </DropdownMenuItem>
                                                            ) : (
                                                                <DropdownMenuItem onClick={() => handleUnfreeze(pocket.id)} className="cursor-pointer">
                                                                    <UnlockIcon className="h-4 w-4 mr-2 text-emerald-500" />
                                                                    Aktifkan
                                                                </DropdownMenuItem>
                                                            )}
                                                            <DropdownMenuItem onClick={() => openCloseModal(pocket)} className="cursor-pointer text-red-600 focus:text-red-600">
                                                                <Trash2 className="h-4 w-4 mr-2" />
                                                                Tutup Kantong
                                                            </DropdownMenuItem>
                                                        </DropdownMenuContent>
                                                    </DropdownMenu>
                                                </div>

                                                <div className="space-y-3">
                                                    <div className="flex justify-between items-end">
                                                        <p className="text-2xl font-bold text-foreground">Rp {pocket.balance.toLocaleString(bcp47Locale)}</p>
                                                        {pocket.target && <span className="text-xs font-bold text-primary">{percentage}%</span>}
                                                    </div>
                                                    {pocket.target && (
                                                        <div className="h-2 w-full bg-muted rounded-full overflow-hidden">
                                                            <div className="h-full bg-emerald-500 rounded-full transition-all" style={{ width: `${Math.min(percentage, 100)}%` }} />
                                                        </div>
                                                    )}
                                                    <div className="flex items-center justify-between pt-2">
                                                        <Badge variant={pocket.status === 'ACTIVE' ? 'default' : 'secondary'} className="text-xs">
                                                            {pocket.status}
                                                        </Badge>
                                                        <span className="text-xs text-muted-foreground">{pocket.currency}</span>
                                                    </div>
                                                </div>
                                            </div>
                                        );
                                    })}
                                </div>
                            ) : (
                                <div className="text-center py-12 bg-muted/30 rounded-2xl border border-border">
                                    <Wallet className="h-12 w-12 text-muted-foreground mx-auto mb-4" />
                                    <h4 className="text-lg font-bold text-foreground mb-2">Belum Ada Kantong</h4>
                                    <p className="text-sm text-muted-foreground mb-4">Buat kantong pertama Anda untuk mulai mengalokasikan dana</p>
                                    <Button onClick={() => setIsCreateModalOpen(true)}>
                                        <Plus className="h-4 w-4 mr-2" /> Buat Kantong
                                    </Button>
                                </div>
                            )}
                        </div>

                        <div className="md:col-span-12 lg:col-span-12 gap-8 mt-12 grid grid-cols-1 lg:grid-cols-12">
                            <div className="lg:col-span-7 space-y-8">
                                <div className="flex justify-between items-center">
                                    <h3 className="text-xl font-bold text-foreground">Tujuan Khusus</h3>
                                    <button className="text-xs font-bold text-primary hover:underline">Kelola Portofolio</button>
                                </div>

                                <div className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-2 lg:grid-cols-2 xl:grid-cols-2 gap-6">
                                    {savingGoals.map((goal) => {
                                        const percentage = Math.round((goal.current / goal.target) * 100);
                                        const Icon = goal.icon;
                                        return (
                                            <div key={goal.id} className="bg-card rounded-xl p-8 border border-border shadow-sm group hover:shadow-card hover:-translate-y-1 transition-all duration-300">
                                                <div className="flex items-center gap-5 mb-8">
                                                    <div className={clsx(
                                                        "h-14 w-14 rounded-xl flex items-center justify-center shadow-lg transition-transform group-hover:scale-110",
                                                        goal.color === 'bank-green' ? "bg-primary/10 text-primary border border-primary/10" : "bg-bank-emerald/10 text-bank-emerald border border-bank-emerald/10"
                                                    )}>
                                                        <Icon className="h-7 w-7" />
                                                    </div>
                                                    <div>
                                                        <h4 className="font-bold text-foreground text-base">{goal.name}</h4>
                                                        <p className="text-xs font-bold text-muted-foreground tracking-widest uppercase">Target: Rp {goal.target.toLocaleString(bcp47Locale)}</p>
                                                    </div>
                                                </div>

                                                {goal.locked ? (
                                                    <div className="space-y-4">
                                                        <div className="flex justify-between items-end">
                                                            <p className="text-2xl font-bold text-foreground">Rp {goal.current.toLocaleString(bcp47Locale)}</p>
                                                            <div className="bg-success-light text-primary px-3 py-1 rounded-full text-xs font-bold border border-primary/10">{goal.interestRate}</div>
                                                        </div>
                                                        <div className="flex items-center gap-2 text-xs font-bold text-primary tracking-widest uppercase">
                                                            <Lock className="h-3 w-3" /> Dana Terkunci & Dijamin
                                                        </div>
                                                    </div>
                                                ) : (
                                                    <div className="space-y-4">
                                                        <div className="flex justify-between items-end mb-1">
                                                            <p className="text-2xl font-bold text-foreground">Rp {goal.current.toLocaleString(bcp47Locale)}</p>
                                                            <span className="text-xs font-bold text-primary">+{percentage}%</span>
                                                        </div>
                                                        <div className="h-2 w-full bg-muted rounded-full overflow-hidden">
                                                            <div className="h-full bg-primary rounded-full" style={{ width: `${percentage}%` }} />
                                                        </div>
                                                        <p className="text-xs font-bold text-muted-foreground tracking-widest text-right uppercase">Sisa: Rp {(goal.target - goal.current).toLocaleString(bcp47Locale)}</p>
                                                    </div>
                                                )}
                                            </div>
                                        );
                                    })}
                                </div>
                            </div>

                            <div className="lg:col-span-5 space-y-8">
                                <div className="flex justify-between items-center">
                                    <h3 className="text-xl font-bold text-foreground">Buku Besar Terakhir</h3>
                                    <div className="h-10 w-10 bg-muted/50 rounded-xl flex items-center justify-center border border-border">
                                        <History className="h-5 w-5 text-muted-foreground" />
                                    </div>
                                </div>

                                <div className="bg-card rounded-xl border border-border shadow-sm min-h-[400px] flex flex-col">
                                    {transactionsLoading ? (
                                        <div className="p-8 space-y-6">
                                            {[1, 2, 3, 4, 5].map(i => <SkeletonTransaction key={i} />)}
                                        </div>
                                    ) : (
                                        <div className="flex-1">
                                            <div className="divide-y divide-border">
                                                {transactions?.map((tx) => (
                                                    <div key={tx.id} className="p-6 flex items-center justify-between group hover:bg-muted/30 transition-all">
                                                        <div className="flex gap-4">
                                                            <div className={clsx(
                                                                "h-12 w-12 rounded-xl flex items-center justify-center border transition-all group-hover:scale-105",
                                                                tx.type === 'CREDIT' ? "bg-success-light border-primary/10" : "bg-destructive/5 border-destructive/10"
                                                            )}>
                                                                {tx.type === 'CREDIT' ? (
                                                                    <TrendingUp className="h-5 w-5 text-primary" />
                                                                ) : (
                                                                    <ChevronRight className="h-5 w-5 text-destructive rotate-90" />
                                                                )}
                                                            </div>
                                                            <div>
                                                                <p className="text-sm font-bold text-foreground mb-0.5">{tx.description}</p>
                                                                <p className="text-xs font-medium text-muted-foreground tracking-tight">
                                                                    {new Date(tx.createdAt).toLocaleDateString(undefined, { day: '2-digit', month: 'short' })} • {tx.type === 'CREDIT' ? 'Masuk' : 'Keluar'}
                                                                </p>
                                                            </div>
                                                        </div>
                                                        <p className={clsx(
                                                            "text-sm font-bold tracking-tight",
                                                            tx.type === 'CREDIT' ? "text-primary" : "text-foreground"
                                                        )}>
                                                            {tx.type === 'CREDIT' ? '+' : '-'} Rp {tx.amount.toLocaleString(bcp47Locale)}
                                                        </p>
                                                    </div>
                                                ))}
                                            </div>

                                            {(!transactions || transactions.length === 0) && (
                                                <div className="h-full flex flex-col items-center justify-center text-center py-20 px-10">
                                                    <History className="h-12 w-12 text-muted/20 mb-4" />
                                                    <p className="text-sm font-bold text-muted-foreground">Tidak Ada Aktivitas</p>
                                                    <p className="text-xs text-muted-foreground/60 mt-1 uppercase tracking-widest">Aktivitas keuangan Anda akan muncul di sini</p>
                                                </div>
                                            )}
                                        </div>
                                    )}
                                    <div className="p-6 mt-auto">
                                        <button className="w-full py-4 bg-muted/50 rounded-xl font-bold text-xs tracking-widest uppercase border border-border hover:bg-muted transition-all text-muted-foreground">Lihat Rekening Koran</button>
                                    </div>
                                </div>
                            </div>
                        </div>

                        <div className="mt-12">
                            <div className="flex justify-between items-center mb-8">
                                <h3 className="text-xl font-bold text-foreground flex items-center gap-3">
                                    <Users className="h-5 w-5 text-primary" />
                                    Kantong Bersama
                                </h3>
                                <ButtonMotion>
                                    <button className="bg-primary/10 text-primary px-6 py-3 rounded-xl font-bold text-xs tracking-widest border border-primary/10 hover:bg-primary/20 transition-all flex items-center gap-2">
                                        <UserPlus className="h-4 w-4" /> Buat Kantong Baru
                                    </button>
                                </ButtonMotion>
                            </div>

                            <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                                {sharedPockets.map((pocket) => {
                                    const percentage = pocket.target ? Math.round((pocket.balance / pocket.target) * 100) : 0;
                                    const isSelected = selectedPocket === pocket.id;

                                    return (
                                        <div key={pocket.id} className="bg-card rounded-xl border border-border shadow-sm overflow-hidden group">
                                            <div
                                                role="button"
                                                tabIndex={0}
                                                onClick={() => setSelectedPocket(isSelected ? null : pocket.id)}
                                                onKeyDown={(e) => { if (e.key === 'Enter' || e.key === ' ') { e.preventDefault(); setSelectedPocket(isSelected ? null : pocket.id); } }}
                                                className="p-6 cursor-pointer transition-colors hover:bg-muted/30"
                                            >
                                                <div className="flex justify-between items-start mb-4">
                                                    <div className="flex items-center gap-3">
                                                        <div className="h-10 w-10 bg-primary/10 rounded-lg flex items-center justify-center">
                                                            <Users className="h-5 w-5 text-primary" />
                                                        </div>
                                                        <div>
                                                            <h4 className="font-bold text-foreground text-sm">{pocket.name}</h4>
                                                            <p className="text-xs text-muted-foreground tracking-widest uppercase">{pocket.sharedMembers?.length} Anggota</p>
                                                        </div>
                                                    </div>
                                                    <ChevronRight className={clsx("h-5 w-5 text-muted-foreground transition-transform", isSelected ? "rotate-90" : "")} />
                                                </div>

                                                <div className="space-y-3">
                                                    <div className="flex justify-between items-end">
                                                        <p className="text-2xl font-bold text-foreground">Rp {pocket.balance.toLocaleString(bcp47Locale)}</p>
                                                        <span className="text-xs font-bold text-primary">{percentage}%</span>
                                                    </div>
                                                    <div className="h-2 w-full bg-muted rounded-full overflow-hidden">
                                                        <div className="h-full bg-primary rounded-full transition-all duration-500" style={{ width: `${percentage}%` }} />
                                                    </div>
                                                    {pocket.target && <p className="text-xs font-bold text-muted-foreground tracking-widest text-right uppercase">Target: Rp {pocket.target.toLocaleString(bcp47Locale)}</p>}
                                                </div>
                                            </div>

                                            {isSelected && pocket.sharedMembers && (
                                                <div className="border-t border-border p-4 bg-muted/20">
                                                    <div className="flex justify-between items-center mb-3">
                                                        <p className="text-xs font-bold text-muted-foreground tracking-widest uppercase">Anggota</p>
                                                        <button className="text-xs font-bold text-primary hover:underline flex items-center gap-1">
                                                            <UserPlus className="h-3 w-3" /> Undang
                                                        </button>
                                                    </div>
                                                    <div className="space-y-2">
                                                        {pocket.sharedMembers.map((member, i) => (
                                                            <div key={i} className="flex items-center justify-between p-2 bg-background rounded-lg border border-border">
                                                                <div className="flex items-center gap-2">
                                                                    <div className="h-6 w-6 bg-primary/10 rounded-full flex items-center justify-center text-xs font-bold text-primary">
                                                                        {member.fullName.charAt(0)}
                                                                    </div>
                                                                    <span className="text-xs font-bold text-foreground">{member.fullName}</span>
                                                                </div>
                                                                <div className="flex items-center gap-2">
                                                                    <span className={clsx(
                                                                        "text-xs font-bold px-2 py-0.5 rounded uppercase tracking-widest",
                                                                        member.role === 'OWNER' ? "bg-primary/10 text-primary" : member.role === 'ADMIN' ? "bg-bank-emerald/10 text-bank-emerald" : "bg-muted/50 text-muted-foreground"
                                                                    )}>
                                                                        {member.role}
                                                                    </span>
                                                                    <button className="p-1 hover:bg-muted rounded transition-colors">
                                                                        <MoreVertical className="h-3 w-3 text-muted-foreground" />
                                                                    </button>
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

                        <StaggerItem className="mt-12">
                            <div className="bg-foreground text-background rounded-xl p-8 sm:p-8 relative overflow-hidden group shadow-card">
                                <div className="absolute top-0 right-0 w-80 h-80 bg-white/5 rounded-full blur-3xl -z-0" />
                                <div className="relative z-10 flex flex-col md:flex-row items-center justify-between gap-8">
                                    <div className="space-y-4 max-w-xl text-center md:text-left">
                                        <div className="flex items-center justify-center md:justify-start gap-4">
                                            <div className="h-12 w-12 bg-primary rounded-xl flex items-center justify-center shadow-lg shadow-primary/20">
                                                <TrendingUp className="h-6 w-6 text-white" />
                                            </div>
                                            <h3 className="text-2xl sm:text-3xl font-bold">Akselerasi Kekayaan Anda.</h3>
                                        </div>
                                        <p className="text-sm text-gray-400 font-medium leading-relaxed">
                                            Pindahkan dana mengendap dari kantong ke reksa dana yield tinggi atau emas digital. AI kami menyarankan Anda bisa berhemat hingga <span className="text-bank-green font-bold">Rp 12,5 Juta</span> lebih per tahun.
                                        </p>
                                    </div>
                                    <ButtonMotion>
                                        <button className="whitespace-nowrap px-10 py-5 bg-bank-green text-white rounded-xl font-bold text-xs tracking-widest shadow-2xl shadow-bank-green/40 hover:bg-bank-emerald transition-all">
                                            Jelajahi Marketplace
                                        </button>
                                    </ButtonMotion>
                                </div>
                            </div>
                        </StaggerItem>
                    </StaggerContainer>
                </div>
            </PageTransition>

            {/* Create Pocket Modal */}
            <Dialog open={isCreateModalOpen} onOpenChange={setIsCreateModalOpen}>
                <DialogContent className="sm:max-w-md">
                    <DialogHeader>
                        <DialogTitle>Buat Kantong Baru</DialogTitle>
                        <DialogDescription>
                            Buat kantong untuk mengalokasikan dana sesuai tujuan Anda
                        </DialogDescription>
                    </DialogHeader>
                    <div className="space-y-4 py-4">
                        <div className="space-y-2">
                            <Label htmlFor="name">Nama Kantong</Label>
                            <Input
                                id="name"
                                placeholder="Contoh: Dana Darurat, Liburan"
                                value={newPocketName}
                                onChange={(e) => setNewPocketName(e.target.value)}
                            />
                        </div>
                        <div className="space-y-2">
                            <Label htmlFor="target">Target Dana (Opsional)</Label>
                            <Input
                                id="target"
                                type="number"
                                placeholder="5000000"
                                value={newPocketTarget}
                                onChange={(e) => setNewPocketTarget(e.target.value)}
                            />
                        </div>
                        <div className="space-y-2">
                            <Label>Tipe Kantong</Label>
                            <div className="flex gap-2">
                                <Button
                                    type="button"
                                    variant={newPocketType === 'SAVINGS' ? 'default' : 'outline'}
                                    className="flex-1"
                                    onClick={() => setNewPocketType('SAVINGS')}
                                >
                                    <Wallet className="h-4 w-4 mr-2" /> Tabungan
                                </Button>
                                <Button
                                    type="button"
                                    variant={newPocketType === 'GOAL' ? 'default' : 'outline'}
                                    className="flex-1"
                                    onClick={() => setNewPocketType('GOAL')}
                                >
                                    <Target className="h-4 w-4 mr-2" /> Target
                                </Button>
                            </div>
                        </div>
                    </div>
                    <DialogFooter>
                        <Button variant="outline" onClick={() => setIsCreateModalOpen(false)}>
                            Batal
                        </Button>
                        <Button
                            onClick={handleCreatePocket}
                            disabled={createPocket.isPending || !newPocketName.trim()}
                        >
                            {createPocket.isPending ? 'Membuat...' : 'Buat Kantong'}
                        </Button>
                    </DialogFooter>
                </DialogContent>
            </Dialog>

            {/* Credit Modal */}
            <Dialog open={isCreditModalOpen} onOpenChange={setIsCreditModalOpen}>
                <DialogContent className="sm:max-w-md">
                    <DialogHeader>
                        <DialogTitle>Tambah Dana</DialogTitle>
                        <DialogDescription>
                            Tambahkan dana ke {selectedPocketForAction?.name}
                        </DialogDescription>
                    </DialogHeader>
                    <div className="space-y-4 py-4">
                        <div className="p-4 bg-muted rounded-xl">
                            <p className="text-sm text-muted-foreground">Saldo Saat Ini</p>
                            <p className="text-2xl font-bold">Rp {selectedPocketForAction?.balance.toLocaleString(bcp47Locale)}</p>
                        </div>
                        <div className="space-y-2">
                            <Label htmlFor="amount">Jumlah Dana</Label>
                            <Input
                                id="amount"
                                type="number"
                                placeholder="100000"
                                value={amount}
                                onChange={(e) => setAmount(e.target.value)}
                            />
                        </div>
                    </div>
                    <DialogFooter>
                        <Button variant="outline" onClick={() => setIsCreditModalOpen(false)}>
                            Batal
                        </Button>
                        <Button
                            onClick={handleCredit}
                            disabled={creditPocket.isPending || !amount}
                        >
                            {creditPocket.isPending ? 'Memproses...' : 'Tambah Dana'}
                        </Button>
                    </DialogFooter>
                </DialogContent>
            </Dialog>

            {/* Debit Modal */}
            <Dialog open={isDebitModalOpen} onOpenChange={setIsDebitModalOpen}>
                <DialogContent className="sm:max-w-md">
                    <DialogHeader>
                        <DialogTitle>Ambil Dana</DialogTitle>
                        <DialogDescription>
                            Ambil dana dari {selectedPocketForAction?.name}
                        </DialogDescription>
                    </DialogHeader>
                    <div className="space-y-4 py-4">
                        <div className="p-4 bg-muted rounded-xl">
                            <p className="text-sm text-muted-foreground">Saldo Tersedia</p>
                            <p className="text-2xl font-bold">Rp {selectedPocketForAction?.balance.toLocaleString(bcp47Locale)}</p>
                        </div>
                        <div className="space-y-2">
                            <Label htmlFor="debit-amount">Jumlah Dana</Label>
                            <Input
                                id="debit-amount"
                                type="number"
                                placeholder="100000"
                                value={amount}
                                onChange={(e) => setAmount(e.target.value)}
                            />
                        </div>
                    </div>
                    <DialogFooter>
                        <Button variant="outline" onClick={() => setIsDebitModalOpen(false)}>
                            Batal
                        </Button>
                        <Button
                            onClick={handleDebit}
                            disabled={debitPocket.isPending || !amount}
                            variant="destructive"
                        >
                            {debitPocket.isPending ? 'Memproses...' : 'Ambil Dana'}
                        </Button>
                    </DialogFooter>
                </DialogContent>
            </Dialog>

            {/* Close Modal */}
            <Dialog open={isCloseModalOpen} onOpenChange={setIsCloseModalOpen}>
                <DialogContent className="sm:max-w-md">
                    <DialogHeader>
                        <DialogTitle className="text-red-600 flex items-center gap-2">
                            <Trash2 className="h-5 w-5" />
                            Tutup Kantong?
                        </DialogTitle>
                        <DialogDescription>
                            Apakah Anda yakin ingin menutup kantong &ldquo;{selectedPocketForAction?.name}&rdquo;? Dana yang tersisa akan dikembalikan ke dompet utama.
                        </DialogDescription>
                    </DialogHeader>
                    <div className="p-4 bg-red-50 rounded-xl border border-red-100">
                        <p className="text-sm text-red-600 font-medium">⚠️ Tindakan ini tidak dapat dibatalkan</p>
                    </div>
                    <DialogFooter>
                        <Button variant="outline" onClick={() => setIsCloseModalOpen(false)}>
                            Batal
                        </Button>
                        <Button
                            onClick={handleClose}
                            disabled={closePocket.isPending}
                            variant="destructive"
                        >
                            {closePocket.isPending ? 'Menutup...' : 'Ya, Tutup Kantong'}
                        </Button>
                    </DialogFooter>
                </DialogContent>
            </Dialog>
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
