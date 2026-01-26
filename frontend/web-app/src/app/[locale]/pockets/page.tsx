'use client';

import React, { useState, useEffect } from 'react';
import { Plus, Target, Lock, TrendingUp, ChevronRight, Wallet, History, ArrowUpRight, ShieldCheck, Activity, Landmark, Coins, Users, UserPlus, MoreVertical } from "lucide-react";
import { useQuery } from '@tanstack/react-query';
import { BalanceResponse, WalletTransaction, Pocket, SharedMember } from '@/types';
import api from '@/lib/api';
import DashboardLayout from "@/components/DashboardLayout";
import clsx from 'clsx';
import { PageTransition, StaggerContainer, StaggerItem, ButtonMotion } from '@/components/ui/Motion';
import { SkeletonBalance, SkeletonTransaction } from '@/components/ui/Skeleton';

interface SharedPocket extends Pocket {
  sharedMembers?: SharedMember[];
  isShared?: boolean;
}

export default function PocketsPage() {
    const [accountId, setAccountId] = useState('');
    const [selectedPocket, setSelectedPocket] = useState<string | null>(null);
    const [showMemberModal, setShowMemberModal] = useState(false);

    // Load accountId from localStorage on client side only
    useEffect(() => {
        const storedAccountId = localStorage.getItem('accountId') || '';
        setAccountId(storedAccountId);
    }, []);

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

    const activePocket = selectedPocket 
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
                                    <h2 className="text-3xl font-black text-foreground">Manajemen Kantong</h2>
                                    <p className="text-sm text-muted-foreground font-medium mt-1">Kelola dan alokasikan dana Anda dengan presisi tinggi.</p>
                                </div>
                                <div className="flex gap-3">
                                    <ButtonMotion>
                                        <button 
                                            onClick={() => setShowMemberModal(true)}
                                            className="bg-muted text-foreground px-8 py-4 rounded-xl font-bold text-xs tracking-widest border border-border shadow-lg hover:bg-muted/80 transition-all flex items-center gap-2"
                                        >
                                            <Users className="h-4 w-4" /> Kantong Bersama
                                        </button>
                                    </ButtonMotion>
                                    <ButtonMotion>
                                        <button className="bg-primary text-primary-foreground px-8 py-4 rounded-xl font-bold text-xs tracking-widest shadow-xl shadow-primary/20 flex items-center gap-2 hover:bg-bank-emerald transition-all">
                                            <Plus className="h-4 w-4" /> Tambah Kantong
                                        </button>
                                    </ButtonMotion>
                                </div>
                            </div>
                        </StaggerItem>

                        <div className="grid grid-cols-1 lg:grid-cols-12 gap-6">
                            <StaggerItem className="lg:col-span-8">
                                <div className="bg-card rounded-xl p-8 sm:p-10 border border-border shadow-card flex flex-col justify-between h-full relative overflow-hidden group">
                                    <div className="absolute top-0 right-0 w-80 h-80 bg-primary/5 rounded-full blur-3xl -z-0" />

                                    <div className="relative z-10 flex flex-col h-full">
                                        <div className="flex justify-between items-start mb-10">
                                            <div className="space-y-1">
                                                <div className="flex items-center gap-2 mb-2">
                                                    <div className="h-2 w-2 bg-primary rounded-full shadow-[0_0_8px_hsl(var(--primary))] animate-pulse" />
                                                    <p className="text-[10px] font-bold text-primary tracking-widest uppercase">Dompet Aktif</p>
                                                </div>
                                                <h3 className="text-3xl font-black text-foreground">Kantong Utama Cair</h3>
                                            </div>
                                            <div className="h-12 w-12 bg-muted/50 rounded-xl flex items-center justify-center border border-border transition-transform group-hover:scale-110">
                                                <Wallet className="h-6 w-6 text-primary" />
                                            </div>
                                        </div>

                                        <div className="mt-auto">
                                            <p className="text-[10px] font-bold text-muted-foreground tracking-widest uppercase mb-2">Likuiditas Tersedia</p>
                                            <h4 className="text-5xl sm:text-6xl font-black text-foreground">
                                                {balanceLoading ? (
                                                    <SkeletonBalance />
                                                ) : (
                                                    `Rp ${(balance?.balance ?? 0).toLocaleString('id-ID')}`
                                                )}
                                            </h4>
                                        </div>
                                    </div>

                                    <div className="absolute bottom-6 right-6">
                                        <button className="p-4 bg-primary text-primary-foreground rounded-xl shadow-xl hover:bg-bank-emerald transition-all active:scale-95">
                                            <ArrowUpRight className="h-6 w-6" />
                                        </button>
                                    </div>
                                </div>
                            </StaggerItem>

                            <StaggerItem className="lg:col-span-4 grid grid-cols-1 gap-6">
                                <div className="bg-card p-6 rounded-xl border border-border shadow-card flex flex-col justify-center relative overflow-hidden group">
                                    <div className="absolute top-0 right-0 w-32 h-32 bg-primary/5 rounded-full blur-2xl" />
                                    <p className="text-[10px] font-bold text-muted-foreground tracking-widest uppercase mb-1">Protokol Cadangan</p>
                                    <p className="text-2xl font-black text-foreground">Rp {(balance?.reservedBalance ?? 0).toLocaleString('id-ID')}</p>
                                    <div className="h-1 w-full bg-muted rounded-full mt-4 overflow-hidden">
                                        <div className="h-full bg-primary/40" style={{ width: '15%' }} />
                                    </div>
                                </div>
                                <div className="bg-gradient-to-br from-gray-900 to-gray-800 p-8 rounded-xl text-white relative overflow-hidden shadow-2xl group flex flex-col justify-between">
                                    <div className="relative z-10 flex items-center gap-4 mb-6">
                                        <div className="h-12 w-12 bg-white/10 rounded-xl flex items-center justify-center border border-white/10">
                                            <ShieldCheck className="h-6 w-6 text-bank-green" />
                                        </div>
                                        <div>
                                            <h3 className="text-sm font-bold">Keamanan Tier-1</h3>
                                            <p className="text-[10px] text-gray-400">Terdaftar OJK & ASPI</p>
                                        </div>
                                    </div>
                                    <div className="relative z-10">
                                        <p className="text-[10px] font-bold text-gray-500 tracking-widest uppercase mb-1">Status Enkripsi</p>
                                        <p className="text-xs font-mono text-bank-green">RESP-V3 ACTIVE</p>
                                    </div>
                                    <Coins className="absolute bottom-[-30px] right-[-30px] h-48 w-48 text-white/5 -rotate-12 group-hover:rotate-0 transition-transform duration-1000" />
                                </div>
                            </StaggerItem>
                        </div>

                        <div className="grid grid-cols-1 lg:grid-cols-12 gap-8 mt-12">
                            <div className="lg:col-span-7 space-y-8">
                                <div className="flex justify-between items-center">
                                    <h3 className="text-xl font-black text-foreground">Tujuan Khusus</h3>
                                    <button className="text-xs font-bold text-primary hover:underline">Kelola Portofolio</button>
                                </div>

                                <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
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
                                                        <h4 className="font-black text-foreground text-base">{goal.name}</h4>
                                                        <p className="text-[10px] font-bold text-muted-foreground tracking-widest uppercase">Target: Rp {goal.target.toLocaleString('id-ID')}</p>
                                                    </div>
                                                </div>

                                                {goal.locked ? (
                                                    <div className="space-y-4">
                                                        <div className="flex justify-between items-end">
                                                            <p className="text-2xl font-black text-foreground">Rp {goal.current.toLocaleString('id-ID')}</p>
                                                            <div className="bg-success-light text-primary px-3 py-1 rounded-full text-[10px] font-bold border border-primary/10">{goal.interestRate}</div>
                                                        </div>
                                                        <div className="flex items-center gap-2 text-[10px] font-bold text-primary tracking-widest uppercase">
                                                            <Lock className="h-3 w-3" /> Dana Terkunci & Dijamin
                                                        </div>
                                                    </div>
                                                ) : (
                                                    <div className="space-y-4">
                                                        <div className="flex justify-between items-end mb-1">
                                                            <p className="text-2xl font-black text-foreground">Rp {goal.current.toLocaleString('id-ID')}</p>
                                                            <span className="text-xs font-bold text-primary">+{percentage}%</span>
                                                        </div>
                                                        <div className="h-2 w-full bg-muted rounded-full overflow-hidden">
                                                            <div className="h-full bg-primary rounded-full" style={{ width: `${percentage}%` }} />
                                                        </div>
                                                        <p className="text-[10px] font-bold text-muted-foreground tracking-widest text-right uppercase">Sisa: Rp {(goal.target - goal.current).toLocaleString('id-ID')}</p>
                                                    </div>
                                                )}
                                            </div>
                                        );
                                    })}
                                </div>
                            </div>

                            <div className="lg:col-span-5 space-y-8">
                                <div className="flex justify-between items-center">
                                    <h3 className="text-xl font-black text-foreground">Buku Besar Terakhir</h3>
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
                                                                <p className="text-[10px] font-medium text-muted-foreground tracking-tight">
                                                                    {new Date(tx.createdAt).toLocaleDateString('id-ID', { day: '2-digit', month: 'short' })} • {tx.type === 'CREDIT' ? 'Masuk' : 'Keluar'}
                                                                </p>
                                                            </div>
                                                        </div>
                                                        <p className={clsx(
                                                            "text-sm font-bold tracking-tight",
                                                            tx.type === 'CREDIT' ? "text-primary" : "text-foreground"
                                                        )}>
                                                            {tx.type === 'CREDIT' ? '+' : '-'} Rp {tx.amount.toLocaleString('id-ID')}
                                                        </p>
                                                    </div>
                                                ))}
                                            </div>

                                            {(!transactions || transactions.length === 0) && (
                                                <div className="h-full flex flex-col items-center justify-center text-center py-20 px-10">
                                                    <History className="h-12 w-12 text-muted/20 mb-4" />
                                                    <p className="text-sm font-bold text-muted-foreground">Tidak Ada Aktivitas</p>
                                                    <p className="text-[10px] text-muted-foreground/60 mt-1 uppercase tracking-widest">Aktivitas keuangan Anda akan muncul di sini</p>
                                                </div>
                                            )}
                                        </div>
                                    )}
                                    <div className="p-6 mt-auto">
                                        <button className="w-full py-4 bg-muted/50 rounded-xl font-bold text-[10px] tracking-widest uppercase border border-border hover:bg-muted transition-all text-muted-foreground">Lihat Rekening Koran</button>
                                    </div>
                                </div>
                            </div>
                        </div>

                        <div className="mt-12">
                            <div className="flex justify-between items-center mb-8">
                                <h3 className="text-xl font-black text-foreground flex items-center gap-3">
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
                                    const percentage = Math.round((pocket.balance / pocket.target) * 100);
                                    const isSelected = selectedPocket === pocket.id;

                                    return (
                                        <div key={pocket.id} className="bg-card rounded-xl border border-border shadow-sm overflow-hidden group">
                                            <div 
                                                onClick={() => setSelectedPocket(isSelected ? null : pocket.id)}
                                                className="p-6 cursor-pointer transition-colors hover:bg-muted/30"
                                            >
                                                <div className="flex justify-between items-start mb-4">
                                                    <div className="flex items-center gap-3">
                                                        <div className="h-10 w-10 bg-primary/10 rounded-lg flex items-center justify-center">
                                                            <Users className="h-5 w-5 text-primary" />
                                                        </div>
                                                        <div>
                                                            <h4 className="font-black text-foreground text-sm">{pocket.name}</h4>
                                                            <p className="text-[10px] text-muted-foreground tracking-widest uppercase">{pocket.sharedMembers?.length} Anggota</p>
                                                        </div>
                                                    </div>
                                                    <ChevronRight className={clsx("h-5 w-5 text-muted-foreground transition-transform", isSelected ? "rotate-90" : "")} />
                                                </div>

                                                <div className="space-y-3">
                                                    <div className="flex justify-between items-end">
                                                        <p className="text-2xl font-black text-foreground">Rp {pocket.balance.toLocaleString('id-ID')}</p>
                                                        <span className="text-xs font-bold text-primary">{percentage}%</span>
                                                    </div>
                                                    <div className="h-2 w-full bg-muted rounded-full overflow-hidden">
                                                        <div className="h-full bg-primary rounded-full transition-all duration-500" style={{ width: `${percentage}%` }} />
                                                    </div>
                                                    <p className="text-[10px] font-bold text-muted-foreground tracking-widest text-right uppercase">Target: Rp {pocket.target.toLocaleString('id-ID')}</p>
                                                </div>
                                            </div>

                                            {isSelected && pocket.sharedMembers && (
                                                <div className="border-t border-border p-4 bg-muted/20">
                                                    <div className="flex justify-between items-center mb-3">
                                                        <p className="text-[10px] font-bold text-muted-foreground tracking-widest uppercase">Anggota</p>
                                                        <button className="text-[10px] font-bold text-primary hover:underline flex items-center gap-1">
                                                            <UserPlus className="h-3 w-3" /> Undang
                                                        </button>
                                                    </div>
                                                    <div className="space-y-2">
                                                        {pocket.sharedMembers.map((member, i) => (
                                                            <div key={i} className="flex items-center justify-between p-2 bg-background rounded-lg border border-border">
                                                                <div className="flex items-center gap-2">
                                                                    <div className="h-6 w-6 bg-primary/10 rounded-full flex items-center justify-center text-[10px] font-bold text-primary">
                                                                        {member.fullName.charAt(0)}
                                                                    </div>
                                                                    <span className="text-xs font-bold text-foreground">{member.fullName}</span>
                                                                </div>
                                                                <div className="flex items-center gap-2">
                                                                    <span className={clsx(
                                                                        "text-[8px] font-bold px-2 py-0.5 rounded uppercase tracking-wider",
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
                            <div className="bg-foreground text-background rounded-xl p-8 sm:p-12 relative overflow-hidden group shadow-card">
                                <div className="absolute top-0 right-0 w-80 h-80 bg-white/5 rounded-full blur-3xl -z-0" />
                                <div className="relative z-10 flex flex-col md:flex-row items-center justify-between gap-10">
                                    <div className="space-y-4 max-w-xl text-center md:text-left">
                                        <div className="flex items-center justify-center md:justify-start gap-4">
                                            <div className="h-12 w-12 bg-primary rounded-xl flex items-center justify-center shadow-lg shadow-primary/20">
                                                <TrendingUp className="h-6 w-6 text-white" />
                                            </div>
                                            <h3 className="text-2xl sm:text-3xl font-black">Akselerasi Kekayaan Anda.</h3>
                                        </div>
                                        <p className="text-sm text-gray-400 font-medium leading-relaxed">
                                            Pindahkan dana mengendap dari kantong ke reksa dana yield tinggi atau emas digital. AI kami menyarankan Anda bisa berhemat hingga <span className="text-bank-green font-black">Rp 12,5 Juta</span> lebih per tahun.
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
        </DashboardLayout>
    );
}
