'use client';

import React from 'react';
import DashboardLayout from "@/components/DashboardLayout";
import { BarChart3, TrendingUp, TrendingDown, Calendar, ArrowUpRight, ArrowDownRight, PieChart, Activity } from 'lucide-center';
import clsx from 'clsx';
// Use local lucide-react imports if provided or correct library
import { TrendingUp as TrendingUpIcon, TrendingDown as TrendingDownIcon, Calendar as CalendarIcon, ArrowUpRight as ArrowUpRightIcon, Activity as ActivityIcon } from 'lucide-react';

export default function AnalyticsPage() {
    return (
        <DashboardLayout>
            <div className="max-w-6xl mx-auto space-y-12 pb-10">
                <div className="flex justify-between items-end">
                    <div>
                        <h2 className="text-3xl font-black text-foreground uppercase tracking-tight italic">Intelijen Keuangan</h2>
                        <p className="text-sm text-gray-500 font-medium">Wawasan mendalam tentang kebiasaan pengeluaran dan pertumbuhan kekayaan Anda.</p>
                    </div>
                    <button className="bg-gray-50 dark:bg-gray-900 border border-border px-6 py-3 rounded-2xl font-black text-[10px] uppercase tracking-widest flex items-center gap-2 hover:bg-gray-100 transition-all shadow-sm">
                        <CalendarIcon className="h-4 w-4" /> Januari 2026
                    </button>
                </div>

                <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
                    {[
                        { label: 'Total Pemasukan', amount: 'Rp 42.500.000', change: '+12.5%', isPos: true, icon: TrendingUpIcon },
                        { label: 'Total Pengeluaran', amount: 'Rp 12.800.000', change: '-4.2%', isPos: false, icon: TrendingDownIcon },
                        { label: 'Tabungan Bulanan', amount: 'Rp 29.700.000', change: '+18.1%', isPos: true, icon: ActivityIcon },
                        { label: 'ROI Investasi', amount: 'Rp 5.200.000', change: '+2.4%', isPos: true, icon: ArrowUpRightIcon },
                    ].map((stat, i) => (
                        <div key={i} className="bg-card p-8 rounded-[2.5rem] border border-border shadow-sm group hover:shadow-xl hover:shadow-bank-green/5 transition-all duration-500">
                            <div className="flex justify-between items-start mb-6">
                                <div className="h-12 w-12 bg-gray-50 dark:bg-gray-900 rounded-[1.25rem] flex items-center justify-center border border-border group-hover:border-bank-green/20 transition-all">
                                    <stat.icon className={clsx("h-6 w-6", stat.isPos ? "text-bank-green" : "text-red-500")} />
                                </div>
                                <span className={clsx(
                                    "text-[10px] font-black uppercase px-3 py-1 rounded-full leading-none tracking-widest",
                                    stat.isPos ? "bg-bank-green/10 text-bank-green" : "bg-red-50 text-red-500"
                                )}>
                                    {stat.change}
                                </span>
                            </div>
                            <p className="text-[10px] font-black text-gray-400 uppercase tracking-[0.2em] mb-2">{stat.label}</p>
                            <h3 className="text-2xl font-black text-foreground italic tracking-tighter">{stat.amount}</h3>
                        </div>
                    ))}
                </div>

                <div className="grid grid-cols-1 lg:grid-cols-12 gap-8">
                    {/* Spending Chart Mock */}
                    <div className="lg:col-span-8">
                        <div className="bg-card rounded-[3rem] p-10 border border-border shadow-sm h-full relative overflow-hidden group">
                            <div className="absolute top-0 right-0 w-64 h-64 bg-bank-green/5 rounded-full blur-3xl" />
                            <div className="flex justify-between items-center mb-10 relative z-10">
                                <div>
                                    <h3 className="text-xl font-black text-foreground uppercase tracking-tight italic">Trajektori Pengeluaran</h3>
                                    <p className="text-[10px] text-gray-400 font-black uppercase tracking-widest mt-1">Analisis arus kas harian periode ini</p>
                                </div>
                                <div className="flex gap-4">
                                    <div className="flex items-center gap-2 px-4 py-2 bg-bank-green/10 rounded-xl border border-bank-green/10">
                                        <div className="h-2 w-2 bg-bank-green rounded-full animate-pulse" />
                                        <span className="text-[10px] font-black text-bank-green uppercase tracking-widest">Masuk</span>
                                    </div>
                                    <div className="flex items-center gap-2 px-4 py-2 bg-gray-50 dark:bg-gray-900 rounded-xl border border-border">
                                        <div className="h-2 w-2 bg-gray-400 rounded-full" />
                                        <span className="text-[10px] font-black text-gray-400 uppercase tracking-widest">Keluar</span>
                                    </div>
                                </div>
                            </div>

                            <div className="h-[300px] flex items-end justify-between gap-3 px-4 relative z-10">
                                {[65, 45, 80, 55, 90, 75, 40, 85, 30, 95, 20, 100].map((h, i) => (
                                    <div key={i} className="flex-1 flex flex-col gap-3 items-center group/bar">
                                        <div className="w-full flex flex-col gap-1.5 items-center h-full justify-end">
                                            <div
                                                className="w-full bg-bank-green/20 rounded-t-xl group-hover/bar:bg-bank-green/40 transition-all duration-500 cursor-pointer relative"
                                                style={{ height: `${h}%` }}
                                            >
                                                <div className="absolute top-0 left-0 w-full h-1 bg-bank-green/60 rounded-full" />
                                            </div>
                                            <div
                                                className="w-full bg-gray-100 dark:bg-gray-800 rounded-t-lg group-hover/bar:bg-gray-200 transition-all duration-500 cursor-pointer"
                                                style={{ height: `${h * 0.4}%` }}
                                            />
                                        </div>
                                        <span className="text-[8px] font-black text-gray-300 uppercase tracking-tighter">Hari {i + 1}</span>
                                    </div>
                                ))}
                            </div>
                        </div>
                    </div>

                    {/* Category Distribution */}
                    <div className="lg:col-span-4">
                        <div className="bg-card rounded-[3rem] p-10 border border-border shadow-sm h-full flex flex-col group">
                            <h3 className="text-xl font-black text-foreground uppercase tracking-tight mb-10 italic">Rincian Pengeluaran</h3>

                            <div className="relative aspect-square mb-12 flex items-center justify-center">
                                <div className="absolute inset-0 flex flex-col items-center justify-center z-10 transition-transform group-hover:scale-110 duration-700">
                                    <p className="text-[10px] font-black text-gray-400 uppercase tracking-widest">Total Keluar</p>
                                    <p className="text-2xl font-black text-foreground italic tracking-tighter">Rp 12,8 Jt</p>
                                </div>
                                <div className="w-full h-full rounded-full border-[20px] border-gray-50 dark:border-gray-900 relative">
                                    <div className="absolute -inset-[20px] rounded-full border-[20px] border-bank-green border-r-transparent border-b-transparent -rotate-45 group-hover:rotate-0 transition-transform duration-1000" />
                                    <div className="absolute -inset-[20px] rounded-full border-[20px] border-bank-emerald border-l-transparent border-t-transparent rotate-12 group-hover:rotate-[50deg] transition-transform duration-1000" />
                                </div>
                            </div>

                            <div className="space-y-6 flex-1">
                                {[
                                    { label: 'Belanja', amount: 'Rp 4,2 Jt', color: 'bg-bank-green', pct: 60 },
                                    { label: 'Utilitas', amount: 'Rp 2,8 Jt', color: 'bg-bank-emerald', pct: 30 },
                                    { label: 'Lain-lain', amount: 'Rp 1,5 Jt', color: 'bg-gray-400', pct: 10 },
                                ].map((cat, i) => (
                                    <div key={i} className="flex items-center justify-between group/cat cursor-pointer">
                                        <div className="flex items-center gap-4">
                                            <div className={clsx("h-3 w-3 rounded-full transition-transform group-hover/cat:scale-150 duration-300", cat.color)} />
                                            <span className="text-xs font-black text-foreground uppercase tracking-widest">{cat.label}</span>
                                        </div>
                                        <span className="text-[10px] font-black text-gray-400 uppercase tracking-[0.1em]">{cat.amount}</span>
                                    </div>
                                ))}
                            </div>
                        </div>
                    </div>
                </div>

                <div className="bg-foreground text-background rounded-[3rem] p-12 relative overflow-hidden group shadow-2xl">
                    <div className="absolute top-0 right-0 w-80 h-80 bg-white/5 rounded-full blur-3xl -z-0" />
                    <div className="relative z-10 flex flex-col md:flex-row items-center justify-between gap-10">
                        <div className="space-y-4 max-w-xl text-center md:text-left">
                            <h3 className="text-3xl font-black italic tracking-tighter text-white">Siap untuk menabung otomatis?</h3>
                            <p className="text-sm font-medium text-gray-400 leading-relaxed uppercase tracking-wide">
                                Sistem AI kami mendeteksi Anda dapat menabung tambahan <span className="text-bank-green font-black">Rp 2.500.000</span> setiap bulan dengan mengoptimalkan tagihan utilitas dan langganan berulang Anda.
                            </p>
                        </div>
                        <button className="whitespace-nowrap bg-bank-green text-white px-10 py-6 rounded-3xl font-black uppercase text-xs tracking-[0.2em] hover:bg-bank-emerald transition-all active:scale-95 shadow-xl shadow-bank-green/20">
                            Terapkan Optimasi
                        </button>
                    </div>
                    <ActivityIcon className="absolute bottom-[-30px] right-[-30px] h-48 w-48 text-white/5 -rotate-12 group-hover:rotate-0 transition-transform duration-1000" />
                </div>
            </div>
        </DashboardLayout>
    );
}
