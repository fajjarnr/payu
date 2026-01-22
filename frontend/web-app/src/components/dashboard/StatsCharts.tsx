'use client';

import React from 'react';
import { Search, ChevronDown, ArrowUpRight } from 'lucide-react';
import clsx from 'clsx';

export default function StatsCharts() {
    return (
        <div className="grid grid-cols-1 md:grid-cols-12 gap-4 sm:gap-6 mt-6">
            {/* Investment Performance */}
            <div className="md:col-span-5 bg-card p-5 sm:p-8 rounded-[2rem] sm:rounded-[2.5rem] border border-border shadow-sm">
                <div className="flex justify-between items-center mb-6 sm:mb-10">
                    <h3 className="text-[10px] font-black text-foreground uppercase tracking-[0.2em]">Performa Investasi</h3>
                    <div className="flex items-center gap-1 text-[10px] font-black text-gray-400 bg-gray-50 dark:bg-gray-900/50 px-3 py-1.5 rounded-xl cursor-pointer hover:bg-gray-100 dark:hover:bg-gray-800 transition-all">
                        1 Jan - 22 Jan <ChevronDown className="h-3 w-3" />
                    </div>
                </div>

                <div className="flex flex-col sm:flex-row items-center justify-between gap-6 sm:gap-0">
                    <div className="space-y-4 w-full sm:w-auto">
                        <div className="flex items-center gap-3">
                            <div className="w-2.5 h-2.5 rounded-full bg-bank-green" />
                            <div>
                                <p className="text-[10px] text-gray-400 font-black uppercase tracking-widest">Saham</p>
                                <p className="text-xs font-black tracking-tight">(60%)</p>
                            </div>
                        </div>
                        <div className="flex items-center gap-3">
                            <div className="w-2.5 h-2.5 rounded-full bg-blue-500" />
                            <div>
                                <p className="text-[10px] text-gray-400 font-black uppercase tracking-widest">Obligasi</p>
                                <p className="text-xs font-black tracking-tight">(25%)</p>
                            </div>
                        </div>
                        <div className="flex items-center gap-3">
                            <div className="w-2.5 h-2.5 rounded-full bg-orange-400" />
                            <div>
                                <p className="text-[10px] text-gray-400 font-black uppercase tracking-widest">Emas</p>
                                <p className="text-xs font-black tracking-tight">(15%)</p>
                            </div>
                        </div>
                    </div>

                    <div className="relative h-28 w-28 sm:h-32 sm:w-32 flex items-center justify-center">
                        <svg viewBox="0 0 36 36" className="w-full h-full transform -rotate-90">
                            <circle cx="18" cy="18" r="15.9155" fill="none" stroke="currentColor" strokeWidth="4" className="text-orange-400" strokeDasharray="100 100" />
                            <circle cx="18" cy="18" r="15.9155" fill="none" stroke="currentColor" strokeWidth="4" className="text-blue-500" strokeDasharray="85 100" />
                            <circle cx="18" cy="18" r="15.9155" fill="none" stroke="currentColor" strokeWidth="4" className="text-bank-green" strokeDasharray="60 100" />
                        </svg>
                        <div className="absolute inset-0 flex flex-col items-center justify-center">
                            <ArrowUpRight className="h-4 w-4 text-bank-green mb-1" />
                            <span className="text-[10px] font-black text-foreground">+12.5%</span>
                        </div>
                    </div>

                    <div className="text-right w-full sm:w-auto">
                        <p className="text-[10px] text-gray-400 font-black uppercase tracking-widest mb-1">Total Nilai</p>
                        <h4 className="text-lg sm:text-xl font-black text-foreground italic tracking-tighter">Rp 8.750.000</h4>
                    </div>
                </div>
            </div>

            {/* Spending Overview */}
            <div className="md:col-span-7 bg-card p-5 sm:p-8 rounded-[2rem] sm:rounded-[2.5rem] border border-border shadow-sm">
                <div className="flex justify-between items-center mb-6 sm:mb-10">
                    <h3 className="text-[10px] font-black text-foreground uppercase tracking-[0.2em]">Ikhtisar Pengeluaran</h3>
                    <div className="flex items-center gap-1 text-[10px] font-black text-gray-400 bg-gray-50 dark:bg-gray-900/50 px-3 py-1.5 rounded-xl cursor-pointer hover:bg-gray-100 dark:hover:bg-gray-800 transition-all">
                        2026 <ChevronDown className="h-3 w-3" />
                    </div>
                </div>

                <div className="flex items-end justify-between h-28 sm:h-32 gap-2 sm:gap-3 mt-4 px-2 sm:px-4">
                    {[
                        { label: 'Jan', value: 30 },
                        { label: 'Feb', value: 45 },
                        { label: 'Mar', value: 60 },
                        { label: 'Apr', value: 80 },
                        { label: 'Mei', value: 100, active: true },
                        { label: 'Jun', value: 70 },
                        { label: 'Jul', value: 90 },
                    ].map((bar, i) => (
                        <div key={i} className="flex-1 flex flex-col items-center gap-3 sm:gap-4">
                            <div className="w-full relative group">
                                <div
                                    className={clsx(
                                        "w-full rounded-full transition-all duration-500",
                                        bar.active
                                            ? "bg-gradient-to-t from-bank-green to-bank-emerald shadow-[0_0_20px_rgba(16,185,129,0.3)]"
                                            : "bg-gray-100 dark:bg-gray-800 group-hover:bg-bank-green/20"
                                    )}
                                    style={{ height: `${bar.value}%` }}
                                />
                                {bar.active && (
                                    <div className="absolute -top-8 sm:-top-10 left-1/2 -translate-x-1/2 bg-foreground text-background text-[7px] sm:text-[8px] font-black px-2 py-1 rounded-lg whitespace-nowrap z-10 shadow-xl">
                                        Rp 3.500.000
                                    </div>
                                )}
                            </div>
                            <span className="text-[9px] sm:text-[10px] font-black text-gray-400 uppercase tracking-tighter">{bar.label}</span>
                        </div>
                    ))}
                </div>
            </div>
        </div>
    );
}
