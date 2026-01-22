'use client';

import React from 'react';
import { ArrowUpRight, ArrowDownRight, RefreshCw } from 'lucide-react';
import clsx from 'clsx';

interface BalanceCardProps {
    balance: number;
    percentage?: number;
    currency?: string;
}

export default function BalanceCard({ balance, percentage = 45.2, currency = 'Rp' }: BalanceCardProps) {
    return (
        <div className="grid grid-cols-1 md:grid-cols-12 gap-4 sm:gap-6">
            {/* Main Balance Info */}
            <div className="md:col-span-4 space-y-4 sm:space-y-6">
                <div className="bg-card p-5 sm:p-6 rounded-[2rem] sm:rounded-[2.5rem] border border-border shadow-sm">
                    <div className="flex justify-between items-start mb-3 sm:mb-4">
                        <p className="text-[10px] font-black text-gray-400 uppercase tracking-widest">Saldo Utama</p>
                        <div className="h-6 w-6 bg-bank-green/10 rounded-full flex items-center justify-center">
                            <div className="h-2 w-2 bg-bank-green rounded-full shadow-[0_0_8px_rgba(16,185,129,0.8)]" />
                        </div>
                    </div>

                    <div className="space-y-1 mb-3 sm:mb-4">
                        <h2 className="text-2xl sm:text-3xl md:text-4xl font-black tracking-tighter text-foreground italic">
                            {currency} {balance.toLocaleString('id-ID')}
                        </h2>
                        <div className="flex items-center gap-2 flex-wrap">
                            <span className="flex items-center gap-0.5 text-[10px] font-black text-bank-green bg-bank-green/10 px-2 py-1 rounded-full uppercase tracking-widest">
                                <ArrowUpRight className="h-3 w-3" />
                                +{percentage}%
                            </span>
                            <span className="text-[10px] text-gray-400 font-bold uppercase tracking-widest">Faktor Pertumbuhan</span>
                        </div>
                    </div>

                    <p className="text-[10px] text-gray-400 font-black uppercase tracking-widest">22 Jan 2026</p>
                </div>

                <div className="bg-card p-5 sm:p-6 rounded-[1.75rem] sm:rounded-[2rem] border border-border shadow-sm">
                    <p className="text-[10px] font-black text-gray-400 uppercase tracking-widest mb-2">Total Kekayaan Bersih</p>
                    <div className="flex items-end justify-between">
                        <h3 className="text-xl sm:text-2xl font-black tracking-tight text-foreground italic">
                            {currency} {(balance * 1.5).toLocaleString('id-ID')}
                        </h3>
                        <span className="text-[10px] font-black text-bank-green bg-bank-green/10 px-2 py-1 rounded-full uppercase tracking-widest">
                            +18.4%
                        </span>
                    </div>
                </div>
            </div>

            {/* Visual Card Representation */}
            <div className="md:col-span-5">
                <div className="aspect-[1.6/1] sm:aspect-[1.6/1] bg-gradient-to-br from-bank-green to-bank-emerald rounded-[1.75rem] sm:rounded-[2.5rem] p-5 sm:p-8 text-white relative overflow-hidden shadow-2xl shadow-bank-green/20 group">
                    {/* Decorative patterns */}
                    <div className="absolute top-0 right-0 w-48 sm:w-64 h-48 sm:h-64 bg-white/10 rounded-full -translate-y-1/2 translate-x-1/2 blur-3xl" />
                    <div className="absolute bottom-0 left-0 w-24 sm:w-32 h-24 sm:h-32 bg-black/10 rounded-full translate-y-1/2 -translate-x-1/2 blur-2xl" />

                    <div className="relative h-full flex flex-col justify-between">
                        <div className="flex justify-between items-start">
                            <div className="flex items-center gap-2">
                                <div className="h-9 w-9 sm:h-10 sm:w-10 bg-white/20 backdrop-blur-md rounded-xl flex items-center justify-center text-white font-black text-lg sm:text-xl border border-white/20">
                                    U
                                </div>
                                <span className="text-xl sm:text-2xl font-black tracking-tighter italic">PayU</span>
                            </div>
                            <button className="h-7 w-7 sm:h-8 sm:w-8 bg-white/20 backdrop-blur-md rounded-full flex items-center justify-center border border-white/20 hover:bg-white/30 transition-colors">
                                <RefreshCw className="h-3.5 w-3.5 sm:h-4 sm:w-4" />
                            </button>
                        </div>

                        <div className="space-y-3 sm:space-y-4">
                            <div className="text-base sm:text-xl md:text-2xl font-black tracking-[0.15em] sm:tracking-[0.2em] font-mono">
                                2984 5678 9838 3723
                            </div>
                            <div className="flex justify-between items-end">
                                <div>
                                    <p className="text-[10px] text-white/60 font-black uppercase tracking-widest mb-1">Pemegang Kartu</p>
                                    <p className="text-xs sm:text-sm font-black uppercase tracking-tight text-white">PENGGUNA PAYU</p>
                                </div>
                                <div className="flex -space-x-3 sm:-space-x-4">
                                    <div className="w-8 h-8 sm:w-10 sm:h-10 rounded-full bg-red-500/90 shadow-lg" />
                                    <div className="w-8 h-8 sm:w-10 sm:h-10 rounded-full bg-yellow-400/90 shadow-lg" />
                                </div>
                            </div>
                        </div>

                        <div className="absolute top-0 right-0 text-[9px] sm:text-[10px] font-black font-mono opacity-60">07/28</div>
                    </div>
                </div>
            </div>

            {/* Summary Stats */}
            <div className="md:col-span-3 flex flex-col gap-4 sm:gap-6">
                <SummaryItem
                    label="Total Pemasukan"
                    amount={75200000}
                    change={+6.5}
                    isPositive={true}
                    currency={currency}
                />
                <SummaryItem
                    label="Total Pengeluaran"
                    amount={42750000}
                    change={-4.2}
                    isPositive={false}
                    currency={currency}
                />
            </div>
        </div>
    );
}

function SummaryItem({ label, amount, change, isPositive, currency }: { label: string; amount: number; change: number; isPositive: boolean; currency: string }) {
    return (
        <div className="bg-card p-5 sm:p-6 rounded-[1.75rem] sm:rounded-[2rem] border border-border shadow-sm flex flex-col justify-between flex-1">
            <div className="flex justify-between items-start">
                <p className="text-[10px] font-black text-gray-400 uppercase tracking-widest">{label}</p>
                <span className={clsx(
                    "text-[10px] font-black px-2 py-1 rounded-full uppercase tracking-widest",
                    isPositive ? "text-bank-green bg-bank-green/10" : "text-red-500 bg-red-500/10"
                )}>
                    {isPositive ? '+' : ''}{change}%
                </span>
            </div>
            <div className="space-y-1">
                <h4 className="text-xl sm:text-2xl font-black tracking-tight text-foreground italic">
                    {currency} {amount.toLocaleString('id-ID')}
                </h4>
                <p className="text-[10px] text-gray-400 font-bold uppercase tracking-widest">Bulan Lalu</p>
            </div>
            <div className="mt-2 text-bank-green">
                {isPositive ? <ArrowUpRight className="h-4 w-4" /> : <ArrowDownRight className="h-4 w-4 text-red-500" />}
            </div>
        </div>
    );
}
