'use client';

import React from 'react';
import DashboardLayout from "@/components/DashboardLayout";
import { TrendingUp, PieChart, Landmark, ArrowUpRight, ShieldCheck, Briefcase, Plus, Coins } from 'lucide-react';
import clsx from 'clsx';

export default function InvestmentsPage() {
    return (
        <DashboardLayout>
            <div className="max-w-6xl mx-auto space-y-12 pb-10">
                <div className="flex justify-between items-end">
                    <div>
                        <h2 className="text-3xl font-black text-foreground uppercase tracking-tight italic">Manajemen Kekayaan</h2>
                        <p className="text-sm text-gray-500 font-medium">Tumbuhkan aset Anda dengan produk investasi kelas institusi.</p>
                    </div>
                    <button className="bg-bank-green text-white px-10 py-5 rounded-2xl font-black text-[10px] uppercase tracking-[0.2em] hover:bg-bank-emerald transition-all active:scale-95 shadow-2xl shadow-bank-green/20 flex items-center gap-2">
                        <Plus className="h-4 w-4" /> Investasi Baru
                    </button>
                </div>

                {/* Portfolio Summary */}
                <div className="grid grid-cols-1 lg:grid-cols-12 gap-10">
                    <div className="lg:col-span-8">
                        <div className="bg-card rounded-[3rem] p-10 border border-border shadow-sm h-full relative overflow-hidden group">
                            <div className="absolute top-0 right-0 w-64 h-64 bg-bank-green/5 rounded-full blur-3xl" />

                            <div className="relative z-10 flex flex-col md:flex-row justify-between gap-10">
                                <div className="space-y-8">
                                    <div>
                                        <p className="text-[10px] font-black text-gray-400 uppercase tracking-[0.2em] mb-3">Total Portofolio Bersih</p>
                                        <h3 className="text-5xl font-black text-foreground tracking-tighter italic">Rp 152.800.000</h3>
                                    </div>
                                    <div className="flex gap-4">
                                        <div className="bg-bank-green/10 px-5 py-2.5 rounded-xl flex items-center gap-2 border border-bank-green/10">
                                            <TrendingUp className="h-4 w-4 text-bank-green" />
                                            <span className="text-[10px] font-black text-bank-green uppercase tracking-widest">+Rp 12,4 Juta (8.2%)</span>
                                        </div>
                                        <div className="bg-gray-50 dark:bg-gray-900 px-5 py-2.5 rounded-xl flex items-center gap-2 border border-border">
                                            <ShieldCheck className="h-4 w-4 text-gray-400" />
                                            <span className="text-[10px] font-black text-gray-400 uppercase tracking-widest italic">Portofolio Terjamin</span>
                                        </div>
                                    </div>
                                </div>
                                <div className="flex-1 flex items-end justify-end gap-2.5 h-44">
                                    {[30, 45, 35, 60, 50, 80, 75, 95].map((h, i) => (
                                        <div key={i} className="w-full bg-bank-green/20 rounded-t-xl relative group/bar transition-all duration-500" style={{ height: `${h}%` }}>
                                            <div className="absolute top-0 left-0 w-full h-1 bg-bank-green/60 rounded-full" />
                                            <div className="absolute -top-12 left-1/2 -translate-x-1/2 bg-foreground text-background text-[8px] font-black px-2 py-1.5 rounded-lg opacity-0 group-hover/bar:opacity-100 transition-all shadow-xl">
                                                +{h}%
                                            </div>
                                        </div>
                                    ))}
                                </div>
                            </div>
                        </div>
                    </div>

                    <div className="lg:col-span-4">
                        <div className="bg-gradient-to-br from-gray-900 to-gray-800 rounded-[3rem] p-10 text-white h-full relative overflow-hidden flex flex-col justify-between shadow-2xl group">
                            <div className="relative z-10">
                                <h3 className="text-xl font-black uppercase tracking-tight mb-6 italic">Profil Risiko</h3>
                                <div className="flex items-center gap-5 mb-10">
                                    <div className="h-16 w-16 rounded-[1.25rem] bg-white/10 flex items-center justify-center border border-white/10 shadow-xl group-hover:scale-110 transition-transform duration-500">
                                        <Briefcase className="h-8 w-8 text-bank-green" />
                                    </div>
                                    <div>
                                        <p className="text-xl font-black italic tracking-tight">Moderat-Agresif</p>
                                        <p className="text-[10px] text-gray-400 font-bold uppercase tracking-widest">Optimasi ROI 15% / Thn</p>
                                    </div>
                                </div>
                                <div className="w-full bg-white/10 h-2.5 rounded-full overflow-hidden">
                                    <div className="bg-bank-green h-full rounded-full animate-in slide-in-from-left duration-1000" style={{ width: '75%' }} />
                                </div>
                            </div>
                            <button className="relative z-10 w-full py-5 bg-white/5 border border-white/10 rounded-2xl font-black text-[10px] uppercase tracking-[0.2em] hover:bg-white/10 transition-all mt-10">Seimbangkan Ulang Portofolio</button>
                            <PieChart className="absolute bottom-[-40px] right-[-40px] h-48 w-48 text-white/5 -rotate-12 group-hover:rotate-0 transition-transform duration-1000" />
                        </div>
                    </div>
                </div>

                {/* Investment Products */}
                <div className="space-y-10">
                    <h3 className="text-[10px] font-black text-gray-400 uppercase tracking-[0.2em] text-center">Marketplace Produk Investasi</h3>
                    <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-10">
                        {[
                            { name: 'Suku Bunga Tetap Plus', type: 'Risiko Rendah', return: '5.5% p.a', icon: Landmark, color: 'text-blue-500', bg: 'bg-blue-50' },
                            { name: 'Equity Growth Fund', type: 'Risiko Tinggi', return: '18.2% p.a', icon: TrendingUp, color: 'text-bank-green', bg: 'bg-bank-green/10' },
                            { name: 'Emas Digital (XAU)', type: 'Stabil', return: 'Harga Pasar', icon: Coins, color: 'text-yellow-500', bg: 'bg-yellow-50' },
                        ].map((prod, i) => (
                            <div key={i} className="bg-card p-10 rounded-[3rem] border border-border shadow-sm hover:shadow-2xl transition-all group cursor-pointer active:scale-[0.98] duration-500">
                                <div className="flex justify-between items-start mb-10">
                                    <div className={clsx("h-16 w-16 rounded-[1.25rem] flex items-center justify-center shadow-xl transition-transform group-hover:scale-110", prod.bg)}>
                                        <prod.icon className={clsx("h-8 w-8", prod.color)} />
                                    </div>
                                    <ArrowUpRight className="h-6 w-6 text-gray-200 group-hover:text-bank-green group-hover:translate-x-1 group-hover:-translate-y-1 transition-all duration-300" />
                                </div>
                                <p className="text-[10px] font-black text-gray-400 uppercase tracking-widest mb-2">{prod.type}</p>
                                <h4 className="text-xl font-black text-foreground uppercase tracking-tight mb-4 italic leading-tight">{prod.name}</h4>
                                <div className="flex items-center gap-3 bg-gray-50 dark:bg-gray-900 w-fit px-4 py-2 rounded-xl border border-border group-hover:border-bank-green/20 transition-colors">
                                    <span className="text-sm font-black text-bank-green italic">{prod.return}</span>
                                    <span className="text-[8px] font-black text-gray-400 uppercase tracking-widest">Estimasi Imbal Hasil</span>
                                </div>
                            </div>
                        ))}
                    </div>
                </div>
            </div>
        </DashboardLayout>
    );
}
