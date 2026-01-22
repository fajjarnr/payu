'use client';

import React from 'react';
import { Search, ChevronDown, ArrowUpRight } from 'lucide-react';
import clsx from 'clsx';

export default function StatsCharts() {
    return (
        <div className="grid grid-cols-1 md:grid-cols-12 gap-6 mt-6">
            {/* Investment Performance */}
            <div className="md:col-span-5 bg-card p-6 rounded-3xl border border-border shadow-sm">
                <div className="flex justify-between items-center mb-6">
                    <h3 className="text-sm font-bold text-gray-900 dark:text-white uppercase tracking-wider">Investment Performance</h3>
                    <div className="flex items-center gap-1 text-[10px] font-bold text-gray-400 bg-gray-50 dark:bg-gray-800 px-2 py-1 rounded-md cursor-pointer hover:bg-gray-100 dark:hover:bg-gray-700 transition-colors">
                        Feb 1 - Feb 22 <ChevronDown className="h-3 w-3" />
                    </div>
                </div>

                <div className="flex items-center justify-between">
                    <div className="space-y-4">
                        <div className="flex items-center gap-3">
                            <div className="w-2.5 h-2.5 rounded-full bg-bank-green" />
                            <div>
                                <p className="text-[10px] text-gray-400 font-bold uppercase">Saham</p>
                                <p className="text-xs font-bold">(60%)</p>
                            </div>
                        </div>
                        <div className="flex items-center gap-3">
                            <div className="w-2.5 h-2.5 rounded-full bg-blue-500" />
                            <div>
                                <p className="text-[10px] text-gray-400 font-bold uppercase">Obligasi</p>
                                <p className="text-xs font-bold">(25%)</p>
                            </div>
                        </div>
                        <div className="flex items-center gap-3">
                            <div className="w-2.5 h-2.5 rounded-full bg-orange-400" />
                            <div>
                                <p className="text-[10px] text-gray-400 font-bold uppercase">Reksa Dana</p>
                                <p className="text-xs font-bold">(15%)</p>
                            </div>
                        </div>
                    </div>

                    <div className="relative h-32 w-32 flex items-center justify-center">
                        {/* Simple CSS-only Donut Chart */}
                        <svg viewBox="0 0 36 36" className="w-full h-full transform -rotate-90">
                            <path
                                className="text-orange-400 stroke-current"
                                strokeWidth="3.8"
                                fill="none"
                                strokeDasharray="100, 100"
                                d="M18 2.0845
                  a 15.9155 15.9155 0 0 1 0 31.831
                  a 15.9155 15.9155 0 0 1 0 -31.831"
                            />
                            <path
                                className="text-blue-500 stroke-current"
                                strokeWidth="3.8"
                                fill="none"
                                strokeDasharray="85, 100"
                                d="M18 2.0845
                  a 15.9155 15.9155 0 0 1 0 31.831
                  a 15.9155 15.9155 0 0 1 0 -31.831"
                            />
                            <path
                                className="text-bank-green stroke-current"
                                strokeWidth="3.8"
                                strokeLinecap="round"
                                fill="none"
                                strokeDasharray="60, 100"
                                d="M18 2.0845
                  a 15.9155 15.9155 0 0 1 0 31.831
                  a 15.9155 15.9155 0 0 1 0 -31.831"
                            />
                        </svg>
                        <div className="absolute inset-0 flex flex-col items-center justify-center">
                            <ArrowUpRight className="h-4 w-4 text-bank-green mb-1" />
                            <span className="text-xs font-bold">+12.5%</span>
                        </div>
                    </div>

                    <div className="text-right">
                        <p className="text-[10px] text-gray-400 font-bold uppercase mb-1">Total Value</p>
                        <h4 className="text-xl font-bold tracking-tight">Rp 8.750,00</h4>
                    </div>
                </div>
            </div>

            {/* Spending Overview */}
            <div className="md:col-span-7 bg-card p-6 rounded-3xl border border-border shadow-sm">
                <div className="flex justify-between items-center mb-6">
                    <h3 className="text-sm font-bold text-gray-900 dark:text-white uppercase tracking-wider">Spending Overview</h3>
                    <div className="flex items-center gap-1 text-[10px] font-bold text-gray-400 bg-gray-50 dark:bg-gray-800 px-2 py-1 rounded-md cursor-pointer hover:bg-gray-100 dark:hover:bg-gray-700 transition-colors">
                        2025 <ChevronDown className="h-3 w-3" />
                    </div>
                </div>

                <div className="flex items-end justify-between h-32 gap-2 mt-4">
                    {[
                        { label: 'Jan', value: 30 },
                        { label: 'Feb', value: 45 },
                        { label: 'Mar', value: 60 },
                        { label: 'Apr', value: 80 },
                        { label: 'May', value: 100, active: true },
                        { label: 'Jun', value: 70 },
                        { label: 'July', value: 90 },
                    ].map((bar, i) => (
                        <div key={i} className="flex-1 flex flex-col items-center gap-3">
                            <div className="w-full relative group">
                                <div
                                    className={clsx(
                                        "w-full rounded-full transition-all duration-300",
                                        bar.active
                                            ? "bg-gradient-to-t from-bank-green to-bank-emerald shadow-[0_0_15px_rgba(16,185,129,0.5)]"
                                            : "bg-bank-green/20 group-hover:bg-bank-green/40"
                                    )}
                                    style={{ height: `${bar.value}%` }}
                                />
                                {bar.active && (
                                    <div className="absolute -top-10 left-1/2 -translate-x-1/2 bg-gray-900 text-white text-[10px] px-2 py-1 rounded-md font-bold whitespace-nowrap z-10">
                                        Rp 3.500
                                    </div>
                                )}
                            </div>
                            <span className="text-[10px] font-bold text-gray-400 uppercase tracking-tighter">{bar.label}</span>
                        </div>
                    ))}
                </div>
            </div>
        </div>
    );
}
