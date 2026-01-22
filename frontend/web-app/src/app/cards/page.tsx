'use client';

import React, { useState } from 'react';
import DashboardLayout from "@/components/DashboardLayout";
import { CreditCard, Eye, EyeOff, Lock, RefreshCw, Sliders, ShieldCheck, Zap } from 'lucide-react';
import clsx from 'clsx';

export default function CardsPage() {
    const [showFullDetails, setShowFullDetails] = useState(false);

    return (
        <DashboardLayout>
            <div className="max-w-5xl mx-auto space-y-10 pb-10">
                <div className="flex justify-between items-end">
                    <div>
                        <h2 className="text-3xl font-black text-foreground uppercase tracking-tight">Virtual Cards</h2>
                        <p className="text-sm text-gray-500 font-medium">Secure online payments with disposable card details.</p>
                    </div>
                </div>

                <div className="grid grid-cols-1 lg:grid-cols-12 gap-10">
                    {/* Main Card View */}
                    <div className="lg:col-span-12">
                        <div className="bg-card rounded-[3rem] p-12 border border-border shadow-sm relative overflow-hidden flex flex-col items-center">
                            <div className="absolute top-0 right-0 w-96 h-96 bg-bank-green/5 rounded-full blur-3xl -z-0" />

                            <div className="w-full max-w-lg aspect-[1.6/1] bg-gradient-to-br from-bank-green to-bank-emerald rounded-[2.5rem] p-10 text-white relative overflow-hidden shadow-2xl shadow-bank-green/30 group">
                                <div className="absolute top-0 right-0 w-80 h-80 bg-white/10 rounded-full -translate-y-1/2 translate-x-1/2 blur-3xl group-hover:scale-110 transition-transform duration-700" />

                                <div className="relative z-10 h-full flex flex-col justify-between">
                                    <div className="flex justify-between items-start">
                                        <div className="flex items-center gap-2">
                                            <div className="h-12 w-12 bg-white/20 backdrop-blur-md rounded-2xl flex items-center justify-center text-white font-bold text-2xl border border-white/20">U</div>
                                            <span className="text-3xl font-black tracking-tighter italic">PayU</span>
                                        </div>
                                        <div className="h-4 w-4 bg-white rounded-full animate-pulse shadow-[0_0_15px_rgba(255,255,255,0.8)]" />
                                    </div>

                                    <div className="space-y-6">
                                        <div className="text-3xl md:text-4xl font-black tracking-[0.25em] font-mono">
                                            {showFullDetails ? "4829 5678 9032 4410" : "•••• •••• •••• 4410"}
                                        </div>
                                        <div className="flex justify-between items-end">
                                            <div className="space-y-1">
                                                <p className="text-[10px] text-white/60 font-black uppercase tracking-widest">Card Holder</p>
                                                <p className="text-lg font-bold">FAJAR NUR ROMADHON</p>
                                            </div>
                                            <div className="flex gap-8">
                                                <div className="text-right space-y-1">
                                                    <p className="text-[10px] text-white/60 font-black uppercase tracking-widest">Expiry</p>
                                                    <p className="font-mono font-black">08 / 29</p>
                                                </div>
                                                <div className="text-right space-y-1">
                                                    <p className="text-[10px] text-white/60 font-black uppercase tracking-widest">CVV</p>
                                                    <p className="font-mono font-black">{showFullDetails ? "892" : "•••"}</p>
                                                </div>
                                            </div>
                                        </div>
                                    </div>
                                </div>
                            </div>

                            <div className="flex gap-4 mt-12 w-full max-w-sm">
                                <button
                                    onClick={() => setShowFullDetails(!showFullDetails)}
                                    className="flex-1 bg-foreground text-background py-5 rounded-2xl font-black text-[10px] uppercase tracking-widest hover:bg-bank-green hover:text-white transition-all active:scale-95 flex items-center justify-center gap-2"
                                >
                                    {showFullDetails ? <EyeOff className="h-4 w-4" /> : <Eye className="h-4 w-4" />}
                                    {showFullDetails ? "Hide Content" : "Reveal Details"}
                                </button>
                                <button className="flex-1 bg-gray-50 dark:bg-gray-900 border border-border py-5 rounded-2xl font-black text-[10px] uppercase tracking-widest hover:bg-gray-100 dark:hover:bg-gray-800 transition-all active:scale-95 flex items-center justify-center gap-2">
                                    <Lock className="h-4 w-4" /> Freeze Card
                                </button>
                            </div>
                        </div>
                    </div>

                    {/* Controls & Security */}
                    <div className="lg:col-span-7">
                        <div className="bg-card rounded-[2.5rem] p-10 border border-border shadow-sm h-full">
                            <h3 className="text-xl font-black text-foreground mb-8 uppercase tracking-tight">Security Controls</h3>
                            <div className="space-y-8">
                                {[
                                    { label: 'Online Transactions', desc: 'Allow payments on retail websites', icon: Zap, status: true },
                                    { label: 'International Limit', desc: 'Permit transactions across borders', icon: ShieldCheck, status: false },
                                    { label: 'Merchant Subscription', desc: 'Allow monthly recurring billing', icon: RefreshCw, status: true },
                                    { label: 'Withdrawal Limit', desc: 'ATM and withdrawal permissions', icon: Sliders, status: false },
                                ].map((control, i) => (
                                    <div key={i} className="flex items-center justify-between group">
                                        <div className="flex gap-5">
                                            <div className="h-12 w-12 bg-gray-50 dark:bg-gray-800 rounded-2xl flex items-center justify-center border border-transparent group-hover:border-bank-green/20 transition-all">
                                                <control.icon className="h-5 w-5 text-gray-400 group-hover:text-bank-green transition-colors" />
                                            </div>
                                            <div>
                                                <p className="font-black text-foreground uppercase tracking-tight text-sm">{control.label}</p>
                                                <p className="text-[10px] text-gray-500 font-bold uppercase tracking-widest mt-1">{control.desc}</p>
                                            </div>
                                        </div>
                                        <div className={clsx(
                                            "w-12 h-6 rounded-full relative p-1 transition-all",
                                            control.status ? "bg-bank-green" : "bg-gray-200 dark:bg-gray-700"
                                        )}>
                                            <div className={clsx(
                                                "w-4 h-4 bg-white rounded-full transition-all",
                                                control.status ? "translate-x-6" : "translate-x-0"
                                            )} />
                                        </div>
                                    </div>
                                ))}
                            </div>
                        </div>
                    </div>

                    <div className="lg:col-span-5">
                        <div className="bg-gradient-to-br from-gray-900 to-gray-800 rounded-[3rem] p-10 text-white h-full flex flex-col justify-between relative overflow-hidden">
                            <div className="relative z-10">
                                <h3 className="text-2xl font-black italic mb-4">Upgrade to Premium.</h3>
                                <p className="text-sm text-gray-400 font-medium mb-10 leading-relaxed">
                                    Get unlimited virtual cards, higher transaction limits, and 2% cashback on all online purchases.
                                </p>
                                <button className="w-full bg-bank-green text-white py-5 rounded-[1.5rem] font-black uppercase text-xs tracking-widest hover:bg-bank-emerald transition-all shadow-xl shadow-bank-green/20">
                                    Unlock Premium Now
                                </button>
                            </div>
                            <CreditCard className="absolute top-[-20px] left-[-20px] h-48 w-48 text-white/5 -rotate-12" />
                            <div className="mt-12 text-center relative z-10">
                                <p className="text-[10px] font-black text-gray-500 uppercase tracking-widest">Secure by Mastercard Protocol</p>
                            </div>
                        </div>
                    </div>
                </div>
            </div>
        </DashboardLayout>
    );
}
