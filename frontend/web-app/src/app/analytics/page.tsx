'use client';

import React from 'react';
import DashboardLayout from "@/components/DashboardLayout";
import { BarChart3, TrendingUp, TrendingDown, Calendar, ArrowUpRight, ArrowDownRight, PieChart, Activity } from 'lucide-react';
import clsx from 'clsx';

export default function AnalyticsPage() {
    return (
        <DashboardLayout>
            <div className="max-w-6xl mx-auto space-y-10 pb-10">
                <div className="flex justify-between items-end">
                    <div>
                        <h2 className="text-3xl font-black text-foreground uppercase tracking-tight">Financial Intelligence</h2>
                        <p className="text-sm text-gray-500 font-medium">Deep insights into your spending habits and net worth growth.</p>
                    </div>
                    <button className="bg-gray-50 dark:bg-gray-900 border border-border px-6 py-3 rounded-2xl font-black text-[10px] uppercase tracking-widest flex items-center gap-2 hover:bg-gray-100 transition-all">
                        <Calendar className="h-4 w-4" /> February 2026
                    </button>
                </div>

                <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
                    {[
                        { label: 'Total Inflow', amount: 'Rp 42.500.000', change: '+12.5%', isPos: true, icon: TrendingUp },
                        { label: 'Total Outflow', amount: 'Rp 12.800.000', change: '-4.2%', isPos: false, icon: TrendingDown },
                        { label: 'Monthly Savings', amount: 'Rp 29.700.000', change: '+18.1%', isPos: true, icon: Activity },
                        { label: 'Investment ROI', amount: 'Rp 5.200.000', change: '+2.4%', isPos: true, icon: ArrowUpRight },
                    ].map((stat, i) => (
                        <div key={i} className="bg-card p-8 rounded-[2rem] border border-border shadow-sm group hover:shadow-xl hover:shadow-bank-green/5 transition-all">
                            <div className="flex justify-between items-start mb-6">
                                <div className="h-10 w-10 bg-gray-50 dark:bg-gray-800 rounded-xl flex items-center justify-center border border-transparent group-hover:border-bank-green/20 transition-all">
                                    <stat.icon className={clsx("h-5 w-5", stat.isPos ? "text-bank-green" : "text-red-500")} />
                                </div>
                                <span className={clsx(
                                    "text-[10px] font-black uppercase px-2 py-0.5 rounded-full leading-none",
                                    stat.isPos ? "bg-bank-green/10 text-bank-green" : "bg-red-50 text-red-500"
                                )}>
                                    {stat.change}
                                </span>
                            </div>
                            <p className="text-[10px] font-black text-gray-400 uppercase tracking-widest mb-1">{stat.label}</p>
                            <h3 className="text-xl font-black text-foreground">{stat.amount}</h3>
                        </div>
                    ))}
                </div>

                <div className="grid grid-cols-1 lg:grid-cols-12 gap-8">
                    {/* Spending Chart Mock */}
                    <div className="lg:col-span-8">
                        <div className="bg-card rounded-[2.5rem] p-10 border border-border shadow-sm h-full relative overflow-hidden">
                            <div className="flex justify-between items-center mb-10">
                                <div>
                                    <h3 className="text-xl font-black text-foreground uppercase tracking-tight">Spending Trajectory</h3>
                                    <p className="text-[10px] text-gray-400 font-bold uppercase tracking-widest mt-1">Cashflow analysis for the last 6 months</p>
                                </div>
                                <div className="flex gap-2">
                                    <div className="flex items-center gap-1.5 px-3 py-1.5 bg-bank-green/10 rounded-lg">
                                        <div className="h-2 w-2 bg-bank-green rounded-full" />
                                        <span className="text-[10px] font-black text-bank-green uppercase">Income</span>
                                    </div>
                                    <div className="flex items-center gap-1.5 px-3 py-1.5 bg-gray-100 dark:bg-gray-800 rounded-lg">
                                        <div className="h-2 w-2 bg-gray-400 rounded-full" />
                                        <span className="text-[10px] font-black text-gray-500 uppercase">Expense</span>
                                    </div>
                                </div>
                            </div>

                            <div className="h-[300px] flex items-end justify-between gap-4 px-4">
                                {[65, 45, 80, 55, 90, 75, 40, 85, 30, 95, 20, 100].map((h, i) => (
                                    <div key={i} className="flex-1 flex flex-col gap-2 items-center group">
                                        <div className="w-full flex flex-col gap-1 items-center">
                                            <div
                                                className="w-full bg-bank-green/20 rounded-t-lg group-hover:bg-bank-green/40 transition-all cursor-pointer relative"
                                                style={{ height: `${h}%` }}
                                            >
                                                <div className="absolute top-0 left-0 w-full h-1 bg-bank-green rounded-full opacity-60" />
                                            </div>
                                            <div
                                                className="w-full bg-gray-100 dark:bg-gray-800 rounded-t-sm group-hover:bg-gray-200 transition-all cursor-pointer"
                                                style={{ height: `${h * 0.4}%` }}
                                            />
                                        </div>
                                        <span className="text-[8px] font-black text-gray-300 uppercase tracking-tighter">Day {i + 1}</span>
                                    </div>
                                ))}
                            </div>
                        </div>
                    </div>

                    {/* Category Distribution */}
                    <div className="lg:col-span-4">
                        <div className="bg-card rounded-[2.5rem] p-10 border border-border shadow-sm h-full flex flex-col">
                            <h3 className="text-xl font-black text-foreground uppercase tracking-tight mb-8">Outflow Breakdown</h3>

                            <div className="relative aspect-square mb-10 group">
                                <div className="absolute inset-0 flex flex-col items-center justify-center z-10">
                                    <p className="text-[10px] font-black text-gray-400 uppercase tracking-widest">Total Spent</p>
                                    <p className="text-xl font-black text-foreground">Rp 12,8M</p>
                                </div>
                                <div className="w-full h-full rounded-full border-[15px] border-gray-100 dark:border-gray-800 relative">
                                    <div className="absolute -inset-[15px] rounded-full border-[15px] border-bank-green border-r-transparent border-b-transparent -rotate-45" />
                                    <div className="absolute -inset-[15px] rounded-full border-[15px] border-bank-emerald border-l-transparent border-t-transparent rotate-12" />
                                </div>
                            </div>

                            <div className="space-y-5">
                                {[
                                    { label: 'Shopping', amount: 'Rp 4,2M', color: 'bg-bank-green', pct: 60 },
                                    { label: 'Utilities', amount: 'Rp 2,8M', color: 'bg-bank-emerald', pct: 30 },
                                    { label: 'Others', amount: 'Rp 1,5M', color: 'bg-gray-400', pct: 10 },
                                ].map((cat, i) => (
                                    <div key={i} className="flex items-center justify-between">
                                        <div className="flex items-center gap-3">
                                            <div className={clsx("h-2.5 w-2.5 rounded-full", cat.color)} />
                                            <span className="text-xs font-black text-foreground uppercase tracking-tight">{cat.label}</span>
                                        </div>
                                        <span className="text-[10px] font-black text-gray-500 uppercase">{cat.amount}</span>
                                    </div>
                                ))}
                            </div>
                        </div>
                    </div>
                </div>

                <div className="bg-gradient-to-r from-bank-green to-bank-emerald rounded-[2.5rem] p-12 text-white relative overflow-hidden group shadow-2xl shadow-bank-green/20">
                    <div className="relative z-10 flex flex-col md:flex-row items-center justify-between gap-10">
                        <div className="space-y-3 max-w-xl">
                            <h3 className="text-3xl font-black italic tracking-tighter">Ready for automated saving?</h3>
                            <p className="text-sm font-medium opacity-80 leading-relaxed">
                                Our AI system detected you could save an additional <span className="font-black">Rp 2.500.000</span> every month by optimizing your utility bills and recurring subscriptions.
                            </p>
                        </div>
                        <button className="whitespace-nowrap bg-white text-bank-green px-10 py-5 rounded-3xl font-black uppercase text-xs tracking-widest hover:bg-gray-50 transition-all active:scale-95 shadow-xl shadow-black/10">
                            Apply Optimization
                        </button>
                    </div>
                    <Activity className="absolute bottom-[-40px] right-[-20px] h-48 w-48 text-white/10 -rotate-12 group-hover:rotate-0 transition-transform duration-1000" />
                </div>
            </div>
        </DashboardLayout>
    );
}
