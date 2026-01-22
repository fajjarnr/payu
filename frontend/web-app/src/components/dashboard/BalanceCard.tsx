'use client';

import React from 'react';
import { ArrowUpRight, ArrowDownRight, RefreshCw, MoreVertical } from 'lucide-react';
import clsx from 'clsx';

interface BalanceCardProps {
    balance: number;
    percentage?: number;
    currency?: string;
}

export default function BalanceCard({ balance, percentage = 45.2, currency = 'Rp' }: BalanceCardProps) {
    return (
        <div className="grid grid-cols-1 md:grid-cols-12 gap-6">
            {/* Main Balance Info */}
            <div className="md:col-span-4 space-y-6">
                <div className="bg-card p-6 rounded-3xl border border-border shadow-sm">
                    <div className="flex justify-between items-start mb-4">
                        <p className="text-sm font-medium text-gray-500">Main Balance</p>
                        <div className="h-6 w-6 bg-green-100 dark:bg-green-900/30 rounded-full flex items-center justify-center">
                            <div className="h-2 w-2 bg-bank-green rounded-full shadow-[0_0_8px_rgba(16,185,129,0.8)]" />
                        </div>
                    </div>

                    <div className="space-y-1 mb-4">
                        <h2 className="text-4xl font-black tracking-tighter text-foreground">
                            {currency} {balance.toLocaleString('id-ID')}
                        </h2>
                        <div className="flex items-center gap-2">
                            <span className="flex items-center gap-0.5 text-[10px] font-black text-bank-green bg-bank-green/10 px-2 py-1 rounded-full uppercase tracking-widest">
                                <ArrowUpRight className="h-3 w-3" />
                                +{percentage}%
                            </span>
                            <span className="text-[10px] text-gray-400 font-bold uppercase tracking-widest">Growth Factor</span>
                        </div>
                    </div>

                    <p className="text-[10px] text-gray-400 font-semibold uppercase tracking-widest">Feb 1, 2026</p>
                </div>

                <div className="bg-card p-6 rounded-3xl border border-border shadow-sm">
                    <p className="text-[10px] font-black text-gray-400 uppercase tracking-widest mb-2">Net Financial Worth</p>
                    <div className="flex items-end justify-between">
                        <h3 className="text-2xl font-black tracking-tight text-foreground italic">
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
                <div className="aspect-[1.6/1] bg-gradient-to-br from-bank-green to-bank-emerald rounded-[2.5rem] p-8 text-white relative overflow-hidden shadow-2xl shadow-bank-green/20 group">
                    {/* Decorative patterns */}
                    <div className="absolute top-0 right-0 w-64 h-64 bg-white/10 rounded-full -translate-y-1/2 translate-x-1/2 blur-3xl" />
                    <div className="absolute bottom-0 left-0 w-32 h-32 bg-black/10 rounded-full translate-y-1/2 -translate-x-1/2 blur-2xl" />

                    <div className="relative h-full flex flex-col justify-between">
                        <div className="flex justify-between items-start">
                            <div className="flex items-center gap-2">
                                <div className="h-10 w-10 bg-white/20 backdrop-blur-md rounded-xl flex items-center justify-center text-white font-bold text-xl border border-white/20">
                                    U
                                </div>
                                <span className="text-2xl font-bold tracking-tight">PayU</span>
                            </div>
                            <button className="h-8 w-8 bg-white/20 backdrop-blur-md rounded-full flex items-center justify-center border border-white/20 hover:bg-white/30 transition-colors">
                                <RefreshCw className="h-4 w-4" />
                            </button>
                        </div>

                        <div className="space-y-4">
                            <div className="text-xl md:text-2xl font-medium tracking-[0.2em] font-mono">
                                2984 5678 9838 3723
                            </div>
                            <div className="flex justify-between items-end">
                                <div>
                                    <p className="text-[10px] text-white/60 font-bold uppercase tracking-widest mb-1">Card Holder</p>
                                    <p className="text-sm font-semibold">PayU User</p>
                                </div>
                                <div className="flex -space-x-4">
                                    <div className="w-10 h-10 rounded-full bg-red-500/90 shadow-lg" />
                                    <div className="w-10 h-10 rounded-full bg-yellow-400/90 shadow-lg" />
                                </div>
                            </div>
                        </div>

                        <div className="absolute top-0 right-0 text-[10px] font-mono opacity-60">07/28</div>
                    </div>
                </div>
            </div>

            {/* Summary Stats */}
            <div className="md:col-span-3 flex flex-col gap-6">
                <SummaryItem
                    label="Total Income"
                    amount={75200000}
                    change={+6.5}
                    isPositive={true}
                    currency={currency}
                />
                <SummaryItem
                    label="Total Expenses"
                    amount={42750000}
                    change={-4.2}
                    isPositive={false}
                    currency={currency}
                />
            </div>
        </div>
    );
}

function SummaryItem({ label, amount, change, isPositive, currency }: any) {
    return (
        <div className="bg-card p-6 rounded-3xl border border-border shadow-sm flex flex-col justify-between flex-1">
            <div className="flex justify-between items-start">
                <p className="text-sm font-medium text-gray-500">{label}</p>
                <span className={clsx(
                    "text-[10px] font-bold px-2 py-0.5 rounded-md",
                    isPositive ? "text-bank-green bg-bank-green/10" : "text-red-500 bg-red-500/10"
                )}>
                    {isPositive ? '+' : ''}{change}%
                </span>
            </div>
            <div className="space-y-1">
                <h4 className="text-2xl font-bold tracking-tight">
                    {currency} {amount.toLocaleString()}
                </h4>
                <p className="text-[10px] text-gray-400 font-medium">Last Month</p>
            </div>
            <div className="mt-2">
                {isPositive ? <ArrowUpRight className="h-4 w-4 text-bank-green" /> : <ArrowDownRight className="h-4 w-4 text-red-500" />}
            </div>
        </div>
    );
}
