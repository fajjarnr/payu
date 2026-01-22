'use client';

import React from 'react';
import DashboardLayout from "@/components/DashboardLayout";
import { ShieldCheck, Fingerprint, Key, Smartphone, Lock, Eye, AlertCircle, History } from 'lucide-react';
import clsx from 'clsx';

export default function SecurityPage() {
    return (
        <DashboardLayout>
            <div className="max-w-4xl mx-auto space-y-10 pb-10">
                <div className="flex justify-between items-end">
                    <div>
                        <h2 className="text-3xl font-black text-foreground uppercase tracking-tight">Security & Governance</h2>
                        <p className="text-sm text-gray-500 font-medium">Protect your assets with military-grade authentication and monitoring.</p>
                    </div>
                    <div className="flex items-center gap-2 bg-bank-green/10 px-4 py-2 rounded-full border border-bank-green/20">
                        <ShieldCheck className="h-4 w-4 text-bank-green" />
                        <span className="text-[10px] font-black text-bank-green uppercase tracking-widest">Level 4 Secured</span>
                    </div>
                </div>

                <div className="grid grid-cols-1 md:grid-cols-2 gap-8">
                    {/* Primary MFA */}
                    <div className="bg-card rounded-[2.5rem] p-10 border border-border shadow-sm relative overflow-hidden group">
                        <div className="absolute top-0 right-0 w-32 h-32 bg-bank-green/5 rounded-full blur-2xl transition-all group-hover:bg-bank-green/10" />

                        <div className="flex items-center gap-5 mb-10">
                            <div className="h-14 w-14 bg-bank-green/10 rounded-2xl flex items-center justify-center shadow-lg shadow-bank-green/5 transition-transform group-hover:scale-110">
                                <Fingerprint className="h-7 w-7 text-bank-green" />
                            </div>
                            <div>
                                <h3 className="text-xl font-black text-foreground uppercase tracking-tight">Biometric MFA</h3>
                                <p className="text-[10px] text-gray-400 font-bold uppercase tracking-widest">Two-Factor Authentication</p>
                            </div>
                        </div>

                        <div className="space-y-6">
                            <p className="text-sm text-gray-500 font-medium leading-relaxed">
                                Require fingerprint or FaceID for every transaction over <span className="font-black text-foreground">Rp 1.000.000</span>.
                            </p>
                            <div className="flex items-center justify-between p-4 bg-gray-50 dark:bg-gray-900/50 rounded-2xl border border-border">
                                <span className="text-[10px] font-black text-foreground uppercase tracking-widest">Status: Active</span>
                                <div className="w-10 h-5 bg-bank-green rounded-full p-1 relative">
                                    <div className="w-3 h-3 bg-white rounded-full translate-x-5" />
                                </div>
                            </div>
                        </div>
                    </div>

                    <div className="bg-card rounded-[2.5rem] p-10 border border-border shadow-sm relative overflow-hidden group">
                        <div className="flex items-center gap-5 mb-10">
                            <div className="h-14 w-14 bg-blue-100 dark:bg-blue-900/20 rounded-2xl flex items-center justify-center shadow-lg shadow-blue-500/5 transition-transform group-hover:scale-110">
                                <Key className="h-7 w-7 text-blue-600" />
                            </div>
                            <div>
                                <h3 className="text-xl font-black text-foreground uppercase tracking-tight">Access Token</h3>
                                <p className="text-[10px] text-gray-400 font-bold uppercase tracking-widest">Hardware Security Key</p>
                            </div>
                        </div>

                        <div className="space-y-6">
                            <p className="text-sm text-gray-500 font-medium leading-relaxed">
                                Use a physical security key or digital authenticator app for logging into new devices.
                            </p>
                            <button className="w-full py-4 bg-foreground text-background rounded-2xl font-black text-[10px] uppercase tracking-widest hover:bg-bank-green hover:text-white transition-all active:scale-95">
                                Setup Authenticator
                            </button>
                        </div>
                    </div>
                </div>

                <div className="bg-card rounded-[2.5rem] p-10 border border-border shadow-sm">
                    <div className="flex justify-between items-center mb-10">
                        <h3 className="text-xl font-black text-foreground uppercase tracking-tight">Active Sessions</h3>
                        <div className="flex items-center gap-2">
                            <AlertCircle className="h-4 w-4 text-orange-400" />
                            <span className="text-[10px] font-black text-orange-400 uppercase tracking-widest">2 Unknown Devices Detected</span>
                        </div>
                    </div>

                    <div className="space-y-6">
                        {[
                            { device: 'MacBook Pro 16"', location: 'Jakarta, ID', status: 'Current Session', icon: Smartphone, active: true },
                            { device: 'iPhone 15 Pro', location: 'Surabaya, ID', status: 'Last active: 2h ago', icon: Smartphone, active: false },
                            { device: 'Linux Workstation', location: 'Singapore, SG', status: 'Last active: 1d ago', icon: Smartphone, active: false },
                        ].map((session, i) => (
                            <div key={i} className="flex items-center justify-between p-6 bg-gray-50 dark:bg-gray-900/50 rounded-3xl border border-transparent hover:border-border transition-all group">
                                <div className="flex items-center gap-5">
                                    <div className="h-12 w-12 bg-white dark:bg-gray-800 rounded-2xl flex items-center justify-center shadow-sm group-hover:scale-110 transition-transform">
                                        <session.icon className={clsx("h-6 w-6", session.active ? "text-bank-green" : "text-gray-400")} />
                                    </div>
                                    <div>
                                        <p className="font-black text-foreground text-sm uppercase tracking-tight">{session.device}</p>
                                        <p className="text-[10px] font-bold text-gray-400 uppercase tracking-widest">{session.location} • {session.status}</p>
                                    </div>
                                </div>
                                <button className="text-[10px] font-black text-red-500 uppercase tracking-widest hover:underline">Revoke Access</button>
                            </div>
                        ))}
                    </div>
                </div>

                <div className="bg-gradient-to-br from-red-500 to-red-600 rounded-[2.5rem] p-10 text-white relative overflow-hidden shadow-2xl shadow-red-500/20">
                    <div className="relative z-10 flex flex-col md:flex-row items-center justify-between gap-8">
                        <div>
                            <h3 className="text-2xl font-black italic mb-2 tracking-tight">Panic Button.</h3>
                            <p className="text-xs font-medium opacity-80 max-w-md">Instantly freeze all wallets, virtual cards, and revoke every active session. Use only in case of emergency.</p>
                        </div>
                        <button className="whitespace-nowrap bg-white text-red-600 px-10 py-5 rounded-2xl font-black uppercase text-[10px] tracking-widest hover:bg-gray-100 transition-all active:scale-95 shadow-xl">
                            Initialize Global Lockdown
                        </button>
                    </div>
                    <Lock className="absolute bottom-[-10px] right-[-10px] h-32 w-32 text-white/10" />
                </div>
            </div>
        </DashboardLayout>
    );
}
