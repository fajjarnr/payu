'use client';

import React, { useState } from 'react';
import DashboardLayout from "@/components/DashboardLayout";
import { QrCode, Camera, History, Image as ImageIcon, ShieldCheck, Info } from 'lucide-react';
import clsx from 'clsx';

export default function QRISPage() {
    const [isScanning, setIsScanning] = useState(false);

    return (
        <DashboardLayout>
            <div className="max-w-4xl mx-auto space-y-10 pb-10">
                <div className="flex justify-between items-end">
                    <div>
                        <h2 className="text-3xl font-black text-foreground uppercase tracking-tight">QRIS Payments</h2>
                        <p className="text-sm text-gray-500 font-medium">Scan any QRIS merchant or P2P code to pay instantly.</p>
                    </div>
                </div>

                <div className="grid grid-cols-1 lg:grid-cols-12 gap-10">
                    {/* QR Scanner Area */}
                    <div className="lg:col-span-8">
                        <div className="bg-card rounded-[2.5rem] border border-border overflow-hidden shadow-sm flex flex-col items-center justify-center p-12 min-h-[500px] relative">
                            {/* Decorative Background */}
                            <div className="absolute top-0 right-0 w-64 h-64 bg-bank-green/5 rounded-full blur-3xl -z-0" />

                            <div className="relative z-10 w-full max-w-sm space-y-8 text-center">
                                <div className="aspect-square bg-gray-50 dark:bg-gray-900/50 rounded-[3rem] border-2 border-dashed border-border flex flex-col items-center justify-center relative group hover:border-bank-green transition-all">
                                    <div className="absolute inset-8 border-2 border-bank-green/20 rounded-[2rem] animate-pulse" />

                                    <div className="w-24 h-24 bg-white dark:bg-gray-800 rounded-3xl flex items-center justify-center mb-4 shadow-xl group-hover:scale-110 transition-transform">
                                        <Camera className="h-10 w-10 text-bank-green" />
                                    </div>
                                    <p className="text-[10px] font-black text-gray-400 uppercase tracking-[0.2em]">Align QR code within frame</p>
                                </div>

                                <div className="flex gap-4">
                                    <button className="flex-1 bg-foreground text-background py-5 rounded-2xl font-black text-sm uppercase tracking-widest hover:bg-bank-green hover:text-white transition-all active:scale-95 flex items-center justify-center gap-2">
                                        <Camera className="h-4 w-4" /> Start Camera
                                    </button>
                                    <button className="flex-1 bg-gray-50 dark:bg-gray-900 text-foreground py-5 rounded-2xl font-black text-sm uppercase tracking-widest border border-border hover:bg-gray-100 dark:hover:bg-gray-800 transition-all active:scale-95 flex items-center justify-center gap-2">
                                        <ImageIcon className="h-4 w-4" /> Upload
                                    </button>
                                </div>
                            </div>
                        </div>
                    </div>

                    {/* Quick Info & History */}
                    <div className="lg:col-span-4 space-y-8">
                        <div className="bg-card rounded-[2.5rem] p-8 shadow-sm border border-border">
                            <h3 className="text-lg font-black text-foreground mb-6 uppercase tracking-widest">Payment Security</h3>
                            <div className="space-y-6">
                                <div className="flex gap-4">
                                    <div className="h-10 w-10 bg-bank-green/10 rounded-xl flex items-center justify-center shrink-0">
                                        <ShieldCheck className="h-5 w-5 text-bank-green" />
                                    </div>
                                    <div>
                                        <p className="text-xs font-black text-foreground uppercase tracking-tight">Encrypted Pay</p>
                                        <p className="text-[10px] text-gray-500 font-medium">Every QRIS transaction is signed with unique hardware tokens.</p>
                                    </div>
                                </div>
                                <div className="flex gap-4">
                                    <div className="h-10 w-10 bg-bank-green/10 rounded-xl flex items-center justify-center shrink-0">
                                        <Info className="h-5 w-5 text-bank-green" />
                                    </div>
                                    <div>
                                        <p className="text-xs font-black text-foreground uppercase tracking-tight">OJK Standard</p>
                                        <p className="text-[10px] text-gray-500 font-medium">Fully compliant with ASPI and Bank Indonesia QRIS protocols.</p>
                                    </div>
                                </div>
                            </div>
                        </div>

                        <div className="bg-gradient-to-br from-gray-900 to-gray-800 rounded-[2.5rem] p-8 text-white relative overflow-hidden">
                            <div className="relative z-10">
                                <h4 className="font-black text-lg mb-2">My QRIS Code</h4>
                                <p className="text-xs text-gray-400 font-medium mb-6">Receive money instantly from any banking app using your unique code.</p>
                                <button className="w-full py-4 bg-white/10 rounded-2xl font-black text-[10px] uppercase tracking-[0.2em] hover:bg-white/20 transition-all border border-white/10">Show My Code</button>
                            </div>
                            <QrCode className="absolute bottom-[-20px] right-[-20px] h-32 w-32 text-white/5 rotate-12" />
                        </div>
                    </div>
                </div>

                {/* Recent Transactions Footer */}
                <div className="bg-card rounded-[2.5rem] p-10 border border-border shadow-sm">
                    <div className="flex justify-between items-center mb-8">
                        <h3 className="text-xl font-black text-foreground uppercase tracking-tight">Recent QR Payments</h3>
                        <button className="text-[10px] font-black text-bank-green uppercase tracking-widest border-b-2 border-bank-green/20 hover:border-bank-green transition-all">View All History</button>
                    </div>

                    <div className="text-center py-12">
                        <History className="h-12 w-12 text-gray-100 dark:text-gray-800 mx-auto mb-4" />
                        <p className="text-[10px] font-black text-gray-400 uppercase tracking-widest leading-relaxed">No QRIS transactions recorded in the last 30 days.</p>
                    </div>
                </div>
            </div>
        </DashboardLayout>
    );
}
