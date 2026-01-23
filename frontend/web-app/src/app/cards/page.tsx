'use client';

import React, { useState } from 'react';
import DashboardLayout from "@/components/DashboardLayout";
import { CreditCard, Eye, EyeOff, Lock, RefreshCw, Sliders, ShieldCheck, Zap, Plus } from 'lucide-react';
import clsx from 'clsx';
import { PageTransition, StaggerContainer, StaggerItem, ButtonMotion } from '@/components/ui/Motion';

export default function CardsPage() {
    const [showFullDetails, setShowFullDetails] = useState(false);

    return (
        <DashboardLayout>
            <PageTransition>
                <div className="space-y-12">
                    {/* Header */}
                    <StaggerContainer>
                        <StaggerItem>
                            <div className="flex flex-col sm:flex-row justify-between items-start sm:items-end gap-6 mb-8">
                                <div>
                                    <h2 className="text-3xl font-black text-foreground">Kartu Virtual</h2>
                                    <p className="text-sm text-muted-foreground font-medium mt-1">Pembayaran online yang aman dengan rincian kartu instan.</p>
                                </div>
                                <ButtonMotion>
                                    <button className="bg-primary text-primary-foreground px-8 py-4 rounded-xl font-bold text-xs tracking-widest shadow-xl shadow-primary/20 flex items-center gap-2 hover:bg-bank-emerald transition-all">
                                        <Plus className="h-4 w-4" /> Kartu Baru
                                    </button>
                                </ButtonMotion>
                            </div>
                        </StaggerItem>

                        {/* Main Content */}
                        <div className="grid grid-cols-1 lg:grid-cols-12 gap-8">
                            {/* Main Card View */}
                            <StaggerItem className="lg:col-span-12">
                                <div className="bg-card rounded-xl p-8 sm:p-12 border border-border shadow-card relative overflow-hidden flex flex-col items-center group">
                                    <div className="absolute top-0 right-0 w-96 h-96 bg-primary/5 rounded-full blur-3xl -z-0" />

                                    <div className="w-full max-w-lg aspect-[1.6/1] rounded-xl relative overflow-hidden shadow-glass group-hover:scale-[1.02] transition-transform duration-500">
                                        <div className="absolute inset-0 card-gradient" />
                                        <div className="absolute inset-0 bg-white/5 backdrop-blur-md" />
                                        <div className="absolute -top-10 -right-10 w-80 h-80 bg-white/10 rounded-full blur-3xl group-hover:scale-110 transition-transform duration-1000" />

                                        <div className="relative z-10 h-full p-8 sm:p-10 flex flex-col justify-between text-white">
                                            <div className="flex justify-between items-start">
                                                <div className="flex items-center gap-3">
                                                    <div className="h-12 w-12 bg-white/20 backdrop-blur-md rounded-xl flex items-center justify-center text-white font-black text-2xl border border-white/20 shadow-lg">U</div>
                                                    <span className="text-3xl font-black">PayU</span>
                                                </div>
                                                <div className="h-4 w-4 bg-white rounded-full animate-pulse shadow-[0_0_15px_rgba(255,255,255,0.8)]" />
                                            </div>

                                            <div className="space-y-8">
                                                <div className="text-2xl sm:text-4xl font-bold tracking-[0.2em] font-mono">
                                                    {showFullDetails ? "4829 5678 9032 4410" : "•••• •••• •••• 4410"}
                                                </div>
                                                <div className="flex justify-between items-end">
                                                    <div className="space-y-1">
                                                        <p className="text-[10px] text-white/60 font-bold tracking-widest uppercase">Pemegang Kartu</p>
                                                        <p className="text-base font-bold uppercase">PENGGUNA PAYU</p>
                                                    </div>
                                                    <div className="flex gap-8">
                                                        <div className="text-right space-y-1">
                                                            <p className="text-[10px] text-white/60 font-bold tracking-widest uppercase">Berlaku</p>
                                                            <p className="font-mono font-bold">08 / 29</p>
                                                        </div>
                                                        <div className="text-right space-y-1">
                                                            <p className="text-[10px] text-white/60 font-bold tracking-widest uppercase">CVV</p>
                                                            <p className="font-mono font-bold">{showFullDetails ? "892" : "•••"}</p>
                                                        </div>
                                                    </div>
                                                </div>
                                            </div>
                                        </div>
                                    </div>

                                    <div className="flex flex-col sm:flex-row gap-4 mt-12 w-full max-w-sm relative z-10">
                                        <button
                                            onClick={() => setShowFullDetails(!showFullDetails)}
                                            className="flex-1 bg-foreground text-background px-6 py-4 rounded-xl font-bold text-[10px] tracking-widest uppercase hover:bg-primary hover:text-white transition-all shadow-xl flex items-center justify-center gap-2"
                                        >
                                            {showFullDetails ? <EyeOff className="h-4 w-4" /> : <Eye className="h-4 w-4" />}
                                            {showFullDetails ? "Sembunyikan" : "Tampilkan Detail"}
                                        </button>
                                        <button className="flex-1 bg-muted/50 border border-border px-6 py-4 rounded-xl font-bold text-[10px] tracking-widest uppercase hover:bg-destructive hover:text-white hover:border-destructive transition-all flex items-center justify-center gap-2 text-muted-foreground">
                                            <Lock className="h-4 w-4" /> Bekukan Kartu
                                        </button>
                                    </div>
                                </div>
                            </StaggerItem>

                            {/* Controls Section */}
                            <StaggerItem className="lg:col-span-12 grid grid-cols-1 md:grid-cols-2 gap-8">
                                <div className="bg-card rounded-xl p-8 sm:p-10 border border-border shadow-card flex flex-col">
                                    <h3 className="text-xl font-black text-foreground mb-10">Kontrol Operasional</h3>
                                    <div className="space-y-8">
                                        {[
                                            { label: 'Transaksi Online', desc: 'Izinkan pembayaran di situs web retail', icon: Zap, status: true },
                                            { label: 'Batas Internasional', desc: 'Izinkan transaksi lintas negara', icon: ShieldCheck, status: false },
                                            { label: 'Langganan Merchant', desc: 'Izinkan tagihan bulanan berulang', icon: RefreshCw, status: true },
                                            { label: 'Batas Penarikan', desc: 'Izin ATM dan penarikan tunai', icon: Sliders, status: false },
                                        ].map((control, i) => (
                                            <div key={i} className="flex items-center justify-between group">
                                                <div className="flex gap-4">
                                                    <div className="h-12 w-12 bg-muted/50 rounded-xl flex items-center justify-center border border-border group-hover:border-primary/20 transition-all">
                                                        <control.icon className="h-5 w-5 text-muted-foreground group-hover:text-primary transition-colors" />
                                                    </div>
                                                    <div>
                                                        <p className="font-bold text-foreground text-sm">{control.label}</p>
                                                        <p className="text-[10px] text-muted-foreground font-medium mt-0.5">{control.desc}</p>
                                                    </div>
                                                </div>
                                                <div className={clsx(
                                                    "w-12 h-6 rounded-full relative p-1 transition-all cursor-pointer",
                                                    control.status ? "bg-primary" : "bg-muted"
                                                )}>
                                                    <div className={clsx(
                                                        "w-4 h-4 bg-white rounded-full transition-all shadow-sm",
                                                        control.status ? "translate-x-6" : "translate-x-0"
                                                    )} />
                                                </div>
                                            </div>
                                        ))}
                                    </div>
                                </div>

                                <div className="bg-gradient-to-br from-gray-900 to-gray-800 rounded-xl p-8 sm:p-10 text-white h-full flex flex-col justify-between relative overflow-hidden shadow-2xl group">
                                    <div className="relative z-10">
                                        <h3 className="text-3xl font-black mb-4">Upgrade ke Premium.</h3>
                                        <p className="text-sm text-gray-400 font-medium mb-10 leading-relaxed max-w-sm">
                                            Dapatkan kartu virtual tak terbatas, limit transaksi lebih tinggi, dan cashback 2% untuk semua pembelian online.
                                        </p>
                                        <button className="px-8 py-4 bg-primary text-primary-foreground rounded-xl font-bold text-xs tracking-widest uppercase shadow-xl shadow-primary/40 hover:bg-bank-emerald transition-all">
                                            Buka Akses Premium
                                        </button>
                                    </div>
                                    <CreditCard className="absolute bottom-[-30px] right-[-30px] h-48 w-48 text-white/5 -rotate-12 group-hover:rotate-0 transition-transform duration-1000" />
                                    <div className="mt-12 relative z-10 border-t border-white/5 pt-6 text-center">
                                        <p className="text-[10px] font-bold text-gray-500 tracking-widest uppercase">Proteksi Real-time oleh Protokol PayU</p>
                                    </div>
                                </div>
                            </StaggerItem>
                        </div>
                    </StaggerContainer>
                </div>
            </PageTransition>
        </DashboardLayout>
    );
}
