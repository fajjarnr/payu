'use client';

import React from 'react';
import { Search, ChevronDown, MoreHorizontal, RotateCcw, ArrowRight, User } from 'lucide-react';

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
        { id: '1', name: 'Alex Johnson', date: '22 Jan 2026, 09:30 AM', amount: -7500000, category: 'Transfer ke', account: 'Tabungan (****5678)' },
        { id: '2', name: 'Tagihan Netflix', date: '21 Jan 2026, 03:45 AM', amount: -159000, category: 'Langganan', account: 'Netflix' },
        { id: '3', name: 'John Doe', date: '20 Jan 2026, 11:10 AM', amount: -4500000, category: 'Transfer ke', account: 'Tabungan (****9876)' },
        { id: '4', name: 'Maria Garcia', date: '19 Jan 2026, 07:45 AM', amount: -350000, category: 'Transfer ke', account: 'Tabungan (****4321)' },
    ];

    return (
        <div className="grid grid-cols-1 lg:grid-cols-12 gap-8 mt-8">
            {/* Recent Transfer Activity List */}
            <div className="lg:col-span-8 bg-card p-6 sm:p-10 rounded-[2.5rem] border border-border shadow-sm">
                <div className="flex justify-between items-center mb-8">
                    <h3 className="text-lg sm:text-xl font-black text-foreground uppercase tracking-tight italic">Aktivitas Transfer Terakhir</h3>
                    <div className="flex items-center gap-1 text-[10px] font-black text-foreground bg-gray-50 dark:bg-gray-900/50 px-3 sm:px-5 py-2 sm:py-2.5 rounded-xl sm:rounded-2xl cursor-pointer border border-border hover:bg-gray-100 transition-all uppercase tracking-widest">
                        Januari <ChevronDown className="h-3 sm:h-4 w-3 sm:w-4" />
                    </div>
                </div>

                {/* Card-based layout for mobile, table for desktop */}
                <div className="hidden md:block overflow-x-auto">
                    <table className="w-full">
                        <thead>
                            <tr className="text-left">
                                <th className="pb-6 text-[10px] font-black text-gray-400 uppercase tracking-widest">Tanggal & Waktu</th>
                                <th className="pb-6 text-[10px] font-black text-gray-400 uppercase tracking-widest text-center">Deskripsi</th>
                                <th className="pb-6 text-[10px] font-black text-gray-400 uppercase tracking-widest text-center">Rekening</th>
                                <th className="pb-6 text-[10px] font-black text-gray-400 uppercase tracking-widest text-right">Jumlah</th>
                                <th className="pb-6"></th>
                            </tr>
                        </thead>
                        <tbody className="divide-y divide-gray-50 dark:divide-gray-900">
                            {transfers.map((item) => (
                                <tr key={item.id} className="group hover:bg-gray-50/50 dark:hover:bg-gray-800/50 transition-colors">
                                    <td className="py-5 whitespace-nowrap text-[10px] font-black text-gray-400 uppercase tracking-tight">{item.date}</td>
                                    <td className="py-5">
                                        <div className="flex items-center gap-4 justify-center">
                                            <div className="h-10 w-10 rounded-2xl bg-gray-100 dark:bg-gray-800 flex items-center justify-center overflow-hidden border border-border shadow-sm group-hover:scale-110 transition-transform">
                                                <User className="h-5 w-5 text-gray-400" />
                                            </div>
                                            <div className="text-left">
                                                <p className="text-[10px] text-gray-400 font-black uppercase tracking-widest">{item.category}</p>
                                                <p className="text-xs font-black text-foreground uppercase tracking-tight">{item.name}</p>
                                            </div>
                                        </div>
                                    </td>
                                    <td className="py-5 text-center">
                                        <p className="text-[10px] font-black text-foreground uppercase tracking-widest">{item.account}</p>
                                    </td>
                                    <td className="py-5 text-right">
                                        <p className="text-xs font-black tracking-tight text-foreground italic">Rp {Math.abs(item.amount).toLocaleString('id-ID')}</p>
                                    </td>
                                    <td className="py-5 text-right pl-4">
                                        <button className="p-2 text-gray-400 hover:text-foreground hover:bg-gray-100 dark:hover:bg-gray-800 rounded-xl transition-all">
                                            <MoreHorizontal className="h-4 w-4" />
                                        </button>
                                    </td>
                                </tr>
                            ))}
                        </tbody>
                    </table>
                </div>

                {/* Mobile card layout */}
                <div className="md:hidden space-y-4">
                    {transfers.map((item) => (
                        <div key={item.id} className="bg-gray-50 dark:bg-gray-900/50 p-5 rounded-2xl border border-border hover:border-bank-green/30 transition-all">
                            <div className="flex items-center justify-between mb-4">
                                <div className="flex items-center gap-3">
                                    <div className="h-10 w-10 rounded-2xl bg-white dark:bg-gray-800 flex items-center justify-center border border-border shadow-sm">
                                        <User className="h-5 w-5 text-gray-400" />
                                    </div>
                                    <div>
                                        <p className="text-xs font-black text-foreground uppercase tracking-tight">{item.name}</p>
                                        <p className="text-[10px] font-black text-gray-400 uppercase tracking-widest">{item.category}</p>
                                    </div>
                                </div>
                                <p className="text-sm font-black text-foreground italic">Rp {Math.abs(item.amount).toLocaleString('id-ID')}</p>
                            </div>
                            <div className="flex items-center justify-between pt-3 border-t border-border">
                                <p className="text-[10px] font-black text-gray-400 uppercase tracking-widest">{item.date}</p>
                                <div className="flex items-center gap-2">
                                    <p className="text-[10px] font-black text-foreground uppercase tracking-widest">{item.account}</p>
                                    <button className="p-2 text-gray-400 hover:text-foreground hover:bg-gray-100 dark:hover:bg-gray-800 rounded-xl transition-all">
                                        <MoreHorizontal className="h-4 w-4" />
                                    </button>
                                </div>
                            </div>
                        </div>
                    ))}
                </div>

                <div className="mt-8 flex flex-col sm:flex-row justify-between items-center gap-4">
                    <button className="flex items-center gap-2 text-[10px] font-black text-bank-green hover:underline uppercase tracking-widest transition-all">
                        <RotateCcw className="h-4 w-4" /> Ulangi Transfer
                    </button>
                    <button className="flex items-center gap-2 text-[10px] font-black text-bank-green hover:underline uppercase tracking-widest transition-all">
                        Riwayat Transaksi Lengkap <ArrowRight className="h-4 w-4" />
                    </button>
                </div>
            </div>

            {/* Quick Transfer Section */}
            <div className="lg:col-span-4 flex flex-col gap-6">
                <div className="bg-card p-6 sm:p-10 rounded-[2.5rem] border border-border shadow-sm flex-1 relative overflow-hidden">
                    <div className="absolute top-0 right-0 w-32 h-32 bg-bank-green/5 rounded-full blur-3xl" />

                    <div className="flex justify-between items-center mb-8">
                        <h3 className="text-lg sm:text-xl font-black text-foreground uppercase tracking-tight italic">Kirim Cepat</h3>
                        <div className="p-2 bg-gray-50 dark:bg-gray-900 rounded-xl">
                            <Search className="h-5 w-5 text-gray-400" />
                        </div>
                    </div>

                    <div className="space-y-8 relative z-10">
                        <div>
                            <p className="text-[10px] font-black text-gray-400 uppercase tracking-[0.2em] mb-6 text-center">Penerima Favorit</p>
                            <div className="grid grid-cols-4 gap-4 sm:gap-6">
                                {[
                                    { icon: '🏦', label: 'Bank' },
                                    { icon: '📱', label: 'E-Wallet' },
                                    { icon: '📄', label: 'Tagihan' },
                                    { icon: '•••', label: 'Lainnya' },
                                ].map((item, i) => (
                                    <button key={i} className="flex flex-col items-center gap-2 sm:gap-3 group">
                                        <div className="h-12 w-12 sm:h-14 sm:w-14 rounded-[1.25rem] bg-gray-50 dark:bg-gray-900 flex items-center justify-center text-xl sm:text-2xl group-hover:bg-bank-green/10 group-hover:scale-105 transition-all border border-border group-hover:border-bank-green animate-in zoom-in duration-300">
                                            {item.icon}
                                        </div>
                                        <span className="text-[8px] sm:text-[10px] font-black text-gray-400 group-hover:text-bank-green uppercase tracking-widest">{item.label}</span>
                                    </button>
                                ))}
                            </div>
                        </div>

                        <div>
                            <p className="text-[10px] font-black text-gray-400 uppercase tracking-[0.2em] mb-6 text-center">Transfer Terakhir</p>
                            <div className="flex justify-between items-center px-1 sm:px-2">
                                {[1, 2, 3, 4, 5, 6].map((i) => (
                                    <div key={i} className="h-9 w-9 sm:h-10 sm:w-10 rounded-2xl bg-white dark:bg-gray-800 border-2 border-border shadow-sm flex items-center justify-center overflow-hidden cursor-pointer hover:scale-110 active:scale-95 transition-all hover:border-bank-green group">
                                        <User className="h-4 w-4 sm:h-5 sm:w-5 text-gray-300 group-hover:text-bank-green transition-colors" />
                                    </div>
                                ))}
                            </div>
                        </div>

                        <button className="w-full bg-bank-green text-white py-5 sm:py-6 rounded-[1.25rem] font-black text-xs uppercase tracking-[0.2em] shadow-2xl shadow-bank-green/20 hover:bg-bank-emerald transition-all active:scale-[0.98]">
                            Kirim Sekarang
                        </button>
                    </div>
                </div>
            </div>
        </div>
    );
}
