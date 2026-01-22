'use client';

import React from 'react';
import { Search, ChevronDown, MoreHorizontal, RotateCcw, ArrowRight } from 'lucide-react';
import clsx from 'clsx';
import Image from 'next/image';

interface TransferItem {
    id: string;
    name: string;
    date: string;
    amount: number;
    category: string;
    account: string;
    avatar?: string;
}

export default function TransferActivity() {
    const transfers: TransferItem[] = [
        { id: '1', name: 'Alex Johnson', date: 'Feb 2, 2025, 09:30 AM', amount: -7500000, category: 'Transfer to', account: 'Savings (****5678)' },
        { id: '2', name: 'Netflix Billing', date: 'Feb 4, 2025, 03:45 AM', amount: -159000, category: 'Subscription', account: 'Netflix' },
        { id: '3', name: 'John Doe', date: 'Feb 3, 2025, 11:10 AM', amount: -4500000, category: 'Transfer to', account: 'Savings (****9876)' },
        { id: '4', name: 'Maria Garcia', date: 'Feb 1, 2025, 07:45 AM', amount: -350000, category: 'Transfer to', account: 'Savings (****4321)' },
    ];

    return (
        <div className="grid grid-cols-1 lg:grid-cols-12 gap-6 mt-6">
            {/* Recent Transfer Activity List */}
            <div className="lg:col-span-8 bg-card p-6 rounded-3xl border border-border shadow-sm">
                <div className="flex justify-between items-center mb-6">
                    <h3 className="text-xl font-bold tracking-tight">Recent Transfer Activity</h3>
                    <div className="flex items-center gap-1 text-sm font-bold text-gray-900 bg-gray-50 dark:bg-gray-800 px-4 py-2 rounded-xl cursor-pointer">
                        February <ChevronDown className="h-4 w-4" />
                    </div>
                </div>

                <div className="overflow-x-auto">
                    <table className="w-full">
                        <thead>
                            <tr className="text-left">
                                <th className="pb-4 text-[10px] font-bold text-gray-400 uppercase tracking-widest">Date & Time</th>
                                <th className="pb-4 text-[10px] font-bold text-gray-400 uppercase tracking-widest text-center">Description</th>
                                <th className="pb-4 text-[10px] font-bold text-gray-400 uppercase tracking-widest text-center">Account</th>
                                <th className="pb-4 text-[10px] font-bold text-gray-400 uppercase tracking-widest text-right">Amount</th>
                                <th className="pb-4"></th>
                            </tr>
                        </thead>
                        <tbody className="divide-y divide-gray-100 dark:divide-gray-800">
                            {transfers.map((item) => (
                                <tr key={item.id} className="group hover:bg-gray-50/50 dark:hover:bg-gray-800/50 transition-colors">
                                    <td className="py-4 whitespace-nowrap text-xs font-medium text-gray-500">{item.date}</td>
                                    <td className="py-4">
                                        <div className="flex items-center gap-3 justify-center">
                                            <div className="h-8 w-8 rounded-full bg-gray-200 dark:bg-gray-700 flex items-center justify-center overflow-hidden">
                                                <span className="text-[10px] font-bold">{item.name[0]}</span>
                                            </div>
                                            <div className="text-left">
                                                <p className="text-[10px] text-gray-400 font-bold uppercase tracking-tight">{item.category}</p>
                                                <p className="text-xs font-bold">{item.name}</p>
                                            </div>
                                        </div>
                                    </td>
                                    <td className="py-4 text-center">
                                        <p className="text-xs font-bold text-gray-700 dark:text-gray-300">{item.account}</p>
                                    </td>
                                    <td className="py-4 text-right">
                                        <p className="text-xs font-bold tracking-tight">Rp {Math.abs(item.amount).toLocaleString()}</p>
                                    </td>
                                    <td className="py-4 text-right pl-4">
                                        <button className="p-1 text-gray-400 hover:text-gray-900 hover:bg-gray-100 rounded-lg">
                                            <MoreHorizontal className="h-4 w-4" />
                                        </button>
                                    </td>
                                </tr>
                            ))}
                        </tbody>
                    </table>
                </div>

                <div className="mt-8 flex justify-between items-center text-[10px] font-bold">
                    <button className="flex items-center gap-2 text-bank-green hover:underline uppercase tracking-widest">
                        <RotateCcw className="h-4 w-4" /> Repeat Transfer
                    </button>
                    <button className="flex items-center gap-2 text-bank-green hover:underline uppercase tracking-widest">
                        View Full Transaction History <ArrowRight className="h-4 w-4" />
                    </button>
                </div>
            </div>

            {/* Quick Transfer Section */}
            <div className="lg:col-span-4 flex flex-col gap-6">
                <div className="bg-card p-6 rounded-3xl border border-border shadow-sm flex-1">
                    <div className="flex justify-between items-center mb-6">
                        <h3 className="text-xl font-bold tracking-tight">Quick Transfer</h3>
                        <Search className="h-5 w-5 text-gray-400" />
                    </div>

                    <div className="space-y-8">
                        <div>
                            <p className="text-[10px] font-bold text-gray-400 uppercase tracking-widest mb-4">Transfer to</p>
                            <div className="grid grid-cols-4 gap-4">
                                {[
                                    { icon: '🏦', label: 'Bankio' },
                                    { icon: '📱', label: 'Top Up' },
                                    { icon: '📄', label: 'Pay Bills' },
                                    { icon: '•••', label: 'Others' },
                                ].map((item, i) => (
                                    <button key={i} className="flex flex-col items-center gap-2 group">
                                        <div className="h-12 w-12 rounded-2xl bg-gray-50 dark:bg-gray-800 flex items-center justify-center text-xl group-hover:bg-bank-green/10 group-hover:scale-105 transition-all outline outline-1 outline-gray-200 dark:outline-gray-700 group-hover:outline-bank-green">
                                            {item.icon}
                                        </div>
                                        <span className="text-[10px] font-bold text-gray-500 group-hover:text-bank-green">{item.label}</span>
                                    </button>
                                ))}
                            </div>
                        </div>

                        <div>
                            <p className="text-[10px] font-bold text-gray-400 uppercase tracking-widest mb-4">Recent User Transfer</p>
                            <div className="flex justify-between items-center">
                                {[1, 2, 3, 4, 5, 6].map((i) => (
                                    <div key={i} className="h-10 w-10 rounded-full bg-gray-200 dark:bg-gray-700 border-2 border-white dark:border-gray-900 shadow-sm flex items-center justify-center overflow-hidden cursor-pointer hover:scale-110 transition-transform">
                                        <span className="text-[10px] font-bold">U{i}</span>
                                    </div>
                                ))}
                            </div>
                        </div>

                        <button className="w-full bg-bank-green text-white py-4 rounded-2xl font-bold shadow-lg shadow-bank-green/20 hover:bg-bank-emerald transition-all active:scale-[0.98]">
                            Quick Transfer Now
                        </button>
                    </div>
                </div>
            </div>
        </div>
    );
}
