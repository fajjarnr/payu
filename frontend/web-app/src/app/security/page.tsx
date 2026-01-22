'use client';

import React from 'react';
import DashboardLayout from "@/components/DashboardLayout";
import { ShieldCheck, Fingerprint, Key, Smartphone, Lock, Eye, AlertCircle, History } from 'lucide-react';
import clsx from 'clsx';

export default function SecurityPage() {
    return (
        <DashboardLayout>
            <div className="max-w-4xl mx-auto space-y-12 pb-10">
                <div className="flex justify-between items-end">
                    <div>
                        <h2 className="text-3xl font-black text-foreground uppercase tracking-tight italic">Keamanan & Tata Kelola</h2>
                        <p className="text-sm text-gray-500 font-medium">Lindungi aset Anda dengan autentikasi dan pemantauan tingkat militer.</p>
                    </div>
                    <div className="flex items-center gap-3 bg-bank-green/10 px-5 py-2.5 rounded-full border border-bank-green/20 shadow-sm animate-in fade-in zoom-in duration-700">
                        <ShieldCheck className="h-5 w-5 text-bank-green" />
                        <span className="text-[10px] font-black text-bank-green uppercase tracking-[0.2em] italic">Terproteksi Level 4</span>
                    </div>
                </div>

                <div className="grid grid-cols-1 md:grid-cols-2 gap-10">
                    {/* Primary MFA */}
                    <div className="bg-card rounded-[3rem] p-10 border border-border shadow-sm relative overflow-hidden group">
                        <div className="absolute top-0 right-0 w-32 h-32 bg-bank-green/5 rounded-full blur-2xl transition-all group-hover:bg-bank-green/10" />

                        <div className="flex items-center gap-6 mb-10 relative z-10">
                            <div className="h-16 w-16 bg-bank-green/10 rounded-[1.25rem] flex items-center justify-center shadow-xl shadow-bank-green/5 transition-transform group-hover:scale-110 duration-500">
                                <Fingerprint className="h-8 w-8 text-bank-green" />
                            </div>
                            <div>
                                <h3 className="text-xl font-black text-foreground uppercase tracking-tight italic">MFA Biometrik</h3>
                                <p className="text-[10px] text-gray-400 font-black uppercase tracking-widest mt-1">Autentikasi Dua Faktor</p>
                            </div>
                        </div>

                        <div className="space-y-8 relative z-10">
                            <p className="text-sm text-gray-400 font-medium leading-relaxed">
                                Wajibkan sidik jari atau FaceID untuk setiap transaksi di atas <span className="font-black text-foreground italic">Rp 1.000.000</span>.
                            </p>
                            <div className="flex items-center justify-between p-5 bg-gray-50 dark:bg-gray-900/50 rounded-2xl border border-border group-hover:border-bank-green/20 transition-all">
                                <span className="text-[10px] font-black text-foreground uppercase tracking-[0.2em]">Status: Aktif</span>
                                <div className="w-12 h-6 bg-bank-green rounded-full p-1 relative cursor-pointer">
                                    <div className="w-4 h-4 bg-white rounded-full translate-x-6 shadow-sm" />
                                </div>
                            </div>
                        </div>
                    </div>

                    <div className="bg-card rounded-[3rem] p-10 border border-border shadow-sm relative overflow-hidden group">
                        <div className="flex items-center gap-6 mb-10 relative z-10">
                            <div className="h-16 w-16 bg-blue-100 dark:bg-blue-900/20 rounded-[1.25rem] flex items-center justify-center shadow-xl shadow-blue-500/5 transition-transform group-hover:scale-110 duration-500">
                                <Key className="h-8 w-8 text-blue-600" />
                            </div>
                            <div>
                                <h3 className="text-xl font-black text-foreground uppercase tracking-tight italic">Token Akses</h3>
                                <p className="text-[10px] text-gray-400 font-black uppercase tracking-widest mt-1">Kunci Keamanan Perangkat</p>
                            </div>
                        </div>

                        <div className="space-y-8 relative z-10">
                            <p className="text-sm text-gray-400 font-medium leading-relaxed">
                                Gunakan kunci keamanan fisik atau aplikasi autentikator digital untuk login di perangkat baru.
                            </p>
                            <button className="w-full py-5 bg-foreground text-background rounded-2xl font-black text-[10px] uppercase tracking-[0.2em] hover:bg-bank-green hover:text-white transition-all active:scale-95 shadow-xl">
                                Atur Autentikator
                            </button>
                        </div>
                    </div>
                </div>

                <div className="bg-card rounded-[3rem] p-12 border border-border shadow-sm relative overflow-hidden">
                    <div className="flex justify-between items-center mb-10 relative z-10">
                        <h3 className="text-xl font-black text-foreground uppercase tracking-tight italic">Sesi Aktif</h3>
                        <div className="flex items-center gap-2 px-4 py-2 bg-orange-50 dark:bg-orange-900/10 rounded-xl border border-orange-100 dark:border-orange-900/20">
                            <AlertCircle className="h-4 w-4 text-orange-500 animate-pulse" />
                            <span className="text-[10px] font-black text-orange-500 uppercase tracking-widest">2 Perangkat Tidak Dikenal Terdeteksi</span>
                        </div>
                    </div>

                    <div className="space-y-6 relative z-10">
                        {[
                            { device: 'MacBook Pro 16"', location: 'Jakarta, ID', status: 'Sesi Saat Ini', icon: Smartphone, active: true },
                            { device: 'iPhone 15 Pro', location: 'Surabaya, ID', status: 'Aktif terakhir: 2 jam lalu', icon: Smartphone, active: false },
                            { device: 'Linux Workstation', location: 'Singapore, SG', status: 'Aktif terakhir: 1 hari lalu', icon: Smartphone, active: false },
                        ].map((session, i) => (
                            <div key={i} className="flex items-center justify-between p-6 bg-gray-50 dark:bg-gray-900/50 rounded-[1.5rem] border border-transparent hover:border-border transition-all group">
                                <div className="flex items-center gap-6">
                                    <div className="h-14 w-14 bg-white dark:bg-gray-800 rounded-2xl flex items-center justify-center shadow-lg border border-border group-hover:scale-110 transition-transform">
                                        <session.icon className={clsx("h-6 w-6", session.active ? "text-bank-green" : "text-gray-400")} />
                                    </div>
                                    <div>
                                        <p className="font-black text-foreground text-sm uppercase tracking-tight italic">{session.device}</p>
                                        <p className="text-[10px] font-black text-gray-400 uppercase tracking-widest mt-1">{session.location} • {session.status}</p>
                                    </div>
                                </div>
                                <button className="text-[10px] font-black text-red-500 uppercase tracking-[0.2em] hover:bg-red-50 px-4 py-2 rounded-lg transition-all border border-transparent hover:border-red-100">Cabut Akses</button>
                            </div>
                        ))}
                    </div>
                </div>

                <div className="bg-red-600 rounded-[3rem] p-12 text-white relative overflow-hidden shadow-2xl shadow-red-500/30 group">
                    <div className="relative z-10 flex flex-col md:flex-row items-center justify-between gap-10">
                        <div className="text-center md:text-left">
                            <h3 className="text-3xl font-black italic mb-3 tracking-tighter">Tombol Panic.</h3>
                            <p className="text-[10px] font-black uppercase tracking-widest opacity-80 max-w-md leading-relaxed">Membekukan semua dompet, kartu virtual, dan mencabut setiap sesi aktif secara instan. Gunakan hanya dalam keadaan darurat.</p>
                        </div>
                        <button className="whitespace-nowrap bg-white text-red-600 px-12 py-6 rounded-3xl font-black uppercase text-[10px] tracking-[0.2em] hover:bg-gray-50 transition-all active:scale-95 shadow-2xl shadow-black/20 italic">
                            Inisialisasi Lockdown Global
                        </button>
                    </div>
                    <Lock className="absolute bottom-[-30px] right-[-30px] h-48 w-48 text-white/10 group-hover:rotate-12 transition-transform duration-700" />
                </div>
            </div>
        </DashboardLayout>
    );
}
