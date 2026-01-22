'use client';

import React from 'react';
import DashboardLayout from "@/components/DashboardLayout";
import { TrendingUp, PieChart, Landmark, ArrowUpRight, ShieldCheck, Briefcase, Plus, Coins } from 'lucide-react';
import clsx from 'clsx';

export default function InvestmentsPage() {
    return (
        <DashboardLayout>
            <div className="max-w-6xl mx-auto space-y-10 pb-10">
                <div className="flex justify-between items-end">
                    <div>
                        <h2 className="text-3xl font-black text-foreground uppercase tracking-tight">Wealth Management</h2>
                        <p className="text-sm text-gray-500 font-medium">Grow your assets with institutional-grade investment products.</p>
                    </div>
                    <button className="bg-bank-green text-white px-8 py-4 rounded-2xl font-black text-xs uppercase tracking-widest hover:bg-bank-emerald transition-all active:scale-95 shadow-xl shadow-bank-green/20 flex items-center gap-2">
                        <Plus className="h-4 w-4" /> New Investment
                    </button>
                </div>

                {/* Portfolio Summary */}
                <div className="grid grid-cols-1 lg:grid-cols-12 gap-8">
                    <div className="lg:col-span-8">
                        <div className="bg-card rounded-[3rem] p-10 border border-border shadow-sm h-full relative overflow-hidden group">
                            <div className="absolute top-0 right-0 w-64 h-64 bg-bank-green/5 rounded-full blur-3xl" />

                            <div className="relative z-10 flex flex-col md:flex-row justify-between gap-10">
                                <div className="space-y-6">
                                    <div>
                                        <p className="text-[10px] font-black text-gray-400 uppercase tracking-[0.2em] mb-2">Total Net Portfolio</p>
                                        <h3 className="text-5xl font-black text-foreground tracking-tighter italic">Rp 152.800.000</h3>
                                    </div>
                                    <div className="flex gap-4">
                                        <div className="bg-bank-green/10 px-4 py-2 rounded-xl flex items-center gap-2">
                                            <TrendingUp className="h-4 w-4 text-bank-green" />
                                            <span className="text-xs font-black text-bank-green">+Rp 12,4M (8.2%)</span>
                                        </div>
                                        <div className="bg-gray-100 dark:bg-gray-800 px-4 py-2 rounded-xl flex items-center gap-2">
                                            <ShieldCheck className="h-4 w-4 text-gray-400" />
                                            <span className="text-xs font-black text-gray-400 uppercase tracking-widest">Insured Portfolio</span>
                                        </div>
                                    </div>
                                </div>
                                <div className="flex-1 flex items-end justify-end gap-2 h-40">
                                    {[30, 45, 35, 60, 50, 80, 75, 95].map((h, i) => (
                                        <div key={i} className="w-full bg-bank-green/20 rounded-t-lg relative group/bar transition-all" style={{ height: `${h}%` }}>
                                            <div className="absolute top-0 left-0 w-full h-1 bg-bank-green rounded-full opacity-60" />
                                            <div className="absolute -top-10 left-1/2 -translate-x-1/2 bg-foreground text-background text-[8px] font-black px-2 py-1 rounded opacity-0 group-hover/bar:opacity-100 transition-opacity">
                                                +{h}%
                                            </div>
                                        </div>
                                    ))}
                                </div>
                            </div>
                        </div>
                    </div>

                    <div className="lg:col-span-4">
                        <div className="bg-gradient-to-br from-gray-900 to-gray-800 rounded-[3rem] p-10 text-white h-full relative overflow-hidden flex flex-col justify-between">
                            <div className="relative z-10">
                                <h3 className="text-xl font-black uppercase tracking-tight mb-4">Risk Profile</h3>
                                <div className="flex items-center gap-4 mb-8">
                                    <div className="h-14 w-14 rounded-2xl bg-white/10 flex items-center justify-center border border-white/10">
                                        <Briefcase className="h-7 w-7 text-bank-green" />
                                    </div>
                                    <div>
                                        <p className="text-lg font-black italic">Moderate-Aggressive</p>
                                        <p className="text-[10px] text-gray-400 font-bold uppercase tracking-widest">Optimized for 15% ROI</p>
                                    </div>
                                </div>
                                <div className="w-full bg-white/10 h-2 rounded-full overflow-hidden">
                                    <div className="bg-bank-green h-full rounded-full" style={{ width: '75%' }} />
                                </div>
                            </div>
                            <button className="relative z-10 w-full py-4 bg-white/5 border border-white/10 rounded-2xl font-black text-[10px] uppercase tracking-widest hover:bg-white/10 transition-all">Rebalance Portfolio</button>
                            <PieChart className="absolute bottom-[-30px] right-[-30px] h-48 w-48 text-white/5 -rotate-12" />
                        </div>
                    </div>
                </div>

                {/* Investment Products */}
                <div className="space-y-8">
                    <h3 className="text-2xl font-black text-foreground uppercase tracking-tight">Marketplace</h3>
                    <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-8">
                        {[
                            { name: 'Fixed Deposit Plus', type: 'Low Risk', return: '5.5% p.a', icon: Landmark, color: 'text-blue-500', bg: 'bg-blue-50' },
                            { name: 'Equity Growth Fund', type: 'High Risk', return: '18.2% p.a', icon: TrendingUp, color: 'text-bank-green', bg: 'bg-bank-green/10' },
                            { name: 'Digital Gold (XAU)', type: 'Stable', return: 'Market Price', icon: Coins, color: 'text-yellow-500', bg: 'bg-yellow-50' },
                        ].map((prod, i) => (
                            <div key={i} className="bg-card p-8 rounded-[2.5rem] border border-border shadow-sm hover:shadow-xl transition-all group cursor-pointer active:scale-[0.98]">
                                <div className="flex justify-between items-start mb-8">
                                    <div className={clsx("h-14 w-14 rounded-2xl flex items-center justify-center shadow-lg transition-transform group-hover:scale-110", prod.bg)}>
                                        <prod.icon className={clsx("h-7 w-7", prod.color)} />
                                    </div>
                                    <ArrowUpRight className="h-5 w-5 text-gray-300 group-hover:text-bank-green group-hover:translate-x-1 group-hover:-translate-y-1 transition-all" />
                                </div>
                                <p className="text-[10px] font-black text-gray-400 uppercase tracking-widest mb-1">{prod.type}</p>
                                <h4 className="text-xl font-black text-foreground uppercase tracking-tight mb-2">{prod.name}</h4>
                                <div className="flex items-center gap-2">
                                    <span className="text-xs font-black text-bank-green">{prod.return}</span>
                                    <span className="text-[10px] font-bold text-gray-400 uppercase tracking-widest">Est. Return</span>
                                </div>
                            </div>
                        ))}
                    </div>
                </div>
            </div>
        </DashboardLayout>
    );
}
