'use client';

import React from 'react';
import DashboardLayout from "@/components/DashboardLayout";
import { User, Mail, Smartphone, MapPin, Globe, Bell, Moon, Sun, Trash2, Shield, CreditCard } from 'lucide-react';
import clsx from 'clsx';

export default function SettingsPage() {
    return (
        <DashboardLayout>
            <div className="max-w-4xl mx-auto space-y-12 pb-10">
                <div className="flex justify-between items-end">
                    <div>
                        <h2 className="text-3xl font-black text-foreground uppercase tracking-tight italic">Ekosistem Akun</h2>
                        <p className="text-sm text-gray-500 font-medium">Kelola profil pribadi, preferensi regional, dan pemicu notifikasi Anda.</p>
                    </div>
                </div>

                <div className="grid grid-cols-1 lg:grid-cols-12 gap-10">
                    {/* Sidebar Profiles */}
                    <div className="lg:col-span-4 space-y-8">
                        <div className="bg-card rounded-[3rem] p-10 border border-border shadow-sm flex flex-col items-center text-center relative overflow-hidden group">
                            <div className="absolute top-0 right-0 w-32 h-32 bg-bank-green/5 rounded-full blur-3xl" />

                            <div className="relative w-28 h-28 bg-gradient-to-br from-bank-green to-bank-emerald rounded-[2.5rem] flex items-center justify-center text-white font-black text-4xl shadow-2xl shadow-bank-green/20 mb-8 transition-transform group-hover:scale-110 duration-500">
                                P
                            </div>
                            <h3 className="text-xl font-black text-foreground uppercase tracking-tight italic">PENGGUNA PAYU</h3>
                            <p className="text-[10px] font-black text-gray-400 uppercase tracking-[0.2em] mt-2 italic shadow-sm bg-gray-50 dark:bg-gray-900 px-3 py-1 rounded-full border border-border">Anggota Tingkat Premium</p>

                            <div className="w-full h-[1px] bg-gray-100 dark:bg-gray-900 my-10" />

                            <div className="w-full space-y-5">
                                <div className="flex items-center justify-between text-[10px] font-black uppercase tracking-[0.2em]">
                                    <span className="text-gray-400">ID Akun</span>
                                    <span className="text-foreground">PAYU-09228373</span>
                                </div>
                                <div className="flex items-center justify-between text-[10px] font-black uppercase tracking-[0.2em]">
                                    <span className="text-gray-400">Status</span>
                                    <span className="text-bank-green italic">eKYC Terverifikasi</span>
                                </div>
                            </div>
                        </div>

                        <div className="bg-card rounded-[2.5rem] p-5 border border-border shadow-sm">
                            <div className="space-y-2">
                                {[
                                    { label: 'Profil Umum', icon: User, active: true },
                                    { label: 'Tagihan & Paket', icon: CreditCard, active: false },
                                    { label: 'Privasi & Keamanan', icon: Shield, active: false },
                                    { label: 'Pengaturan Lanjut', icon: Globe, active: false },
                                ].map((item, i) => (
                                    <button
                                        key={i}
                                        className={clsx(
                                            "w-full flex items-center gap-5 p-5 rounded-2xl transition-all font-black text-[10px] uppercase tracking-[0.2em]",
                                            item.active ? "bg-bank-green text-white shadow-xl shadow-bank-green/20 italic" : "text-gray-400 hover:bg-gray-50 dark:hover:bg-gray-900 hover:text-foreground"
                                        )}
                                    >
                                        <item.icon className="h-5 w-5" />
                                        {item.label}
                                    </button>
                                ))}
                            </div>
                        </div>
                    </div>

                    {/* Main Settings Form */}
                    <div className="lg:col-span-8">
                        <div className="bg-card rounded-[3rem] p-10 border border-border shadow-sm space-y-12 relative overflow-hidden">
                            <div className="absolute bottom-0 left-0 w-64 h-64 bg-bank-green/5 rounded-full blur-3xl -z-0" />

                            {/* Personal Details */}
                            <section className="space-y-10 relative z-10">
                                <h3 className="text-sm font-black text-foreground uppercase tracking-[0.2em] flex items-center gap-4 italic">
                                    <div className="h-10 w-10 bg-gray-50 dark:bg-gray-900 rounded-[1.25rem] flex items-center justify-center border border-border">
                                        <User className="h-5 w-5 text-bank-green" />
                                    </div>
                                    Kredensial Pribadi
                                </h3>

                                <div className="grid grid-cols-1 md:grid-cols-2 gap-10">
                                    <div className="space-y-4">
                                        <label className="text-[10px] font-black text-gray-400 uppercase tracking-widest ml-1">Nama Lengkap (Sesuai KTP)</label>
                                        <input type="text" defaultValue="PENGGUNA PAYU" className="w-full rounded-2xl border-border bg-gray-50 dark:bg-gray-900/50 p-5 text-sm font-black text-foreground outline-none focus:ring-4 focus:ring-bank-green/10 focus:border-bank-green transition-all uppercase tracking-tight" />
                                    </div>
                                    <div className="space-y-4">
                                        <label className="text-[10px] font-black text-gray-400 uppercase tracking-widest ml-1">Email Kontak</label>
                                        <input type="email" defaultValue="user@payu.id" className="w-full rounded-2xl border-border bg-gray-50 dark:bg-gray-900/50 p-5 text-sm font-black text-foreground outline-none focus:ring-4 focus:ring-bank-green/10 focus:border-bank-green transition-all italic" />
                                    </div>
                                    <div className="space-y-4">
                                        <label className="text-[10px] font-black text-gray-400 uppercase tracking-widest ml-1">Protokol Telepon</label>
                                        <input type="text" defaultValue="+62 812-3456-7890" className="w-full rounded-2xl border-border bg-gray-50 dark:bg-gray-900/50 p-5 text-sm font-black text-foreground outline-none focus:ring-4 focus:ring-bank-green/10 focus:border-bank-green transition-all" />
                                    </div>
                                    <div className="space-y-4">
                                        <label className="text-[10px] font-black text-gray-400 uppercase tracking-widest ml-1">Domisili Saat Ini</label>
                                        <input type="text" defaultValue="Jakarta, Indonesia" className="w-full rounded-2xl border-border bg-gray-50 dark:bg-gray-900/50 p-5 text-sm font-black text-foreground outline-none focus:ring-4 focus:ring-bank-green/10 focus:border-bank-green transition-all uppercase tracking-tight" />
                                    </div>
                                </div>
                            </section>

                            <div className="h-[1px] w-full bg-gray-100 dark:bg-gray-900" />

                            {/* Preferences */}
                            <section className="space-y-10 relative z-10">
                                <h3 className="text-sm font-black text-foreground uppercase tracking-[0.2em] flex items-center gap-4 italic">
                                    <div className="h-10 w-10 bg-gray-50 dark:bg-gray-900 rounded-[1.25rem] flex items-center justify-center border border-border">
                                        <Bell className="h-5 w-5 text-bank-green" />
                                    </div>
                                    Preferensi Sistem
                                </h3>

                                <div className="space-y-8">
                                    {[
                                        { label: 'Notifikasi Push', desc: 'Peringatan transaksi & status real-time', icon: Bell, active: true },
                                        { label: 'Grafis Mode Gelap', desc: 'Antarmuka visual kontras tinggi', icon: Moon, active: false },
                                        { label: 'Wawasan Pemasaran', desc: 'Pembaruan promosi, berita, dan hadiah', icon: Globe, active: true },
                                    ].map((pref, i) => (
                                        <div key={i} className="flex items-center justify-between group cursor-pointer">
                                            <div>
                                                <p className="font-black text-foreground uppercase tracking-tight text-sm italic">{pref.label}</p>
                                                <p className="text-[10px] text-gray-400 font-bold uppercase tracking-widest mt-1">{pref.desc}</p>
                                            </div>
                                            <div className={clsx(
                                                "w-12 h-6 rounded-full relative p-1 transition-all",
                                                pref.active ? "bg-bank-green shadow-lg shadow-bank-green/20" : "bg-gray-200 dark:bg-gray-700"
                                            )}>
                                                <div className={clsx(
                                                    "w-4 h-4 bg-white rounded-full transition-all shadow-sm",
                                                    pref.active ? "translate-x-6" : "translate-x-0"
                                                )} />
                                            </div>
                                        </div>
                                    ))}
                                </div>
                            </section>

                            <div className="h-[1px] w-full bg-gray-100 dark:bg-gray-900" />

                            <div className="flex gap-6 pt-4 relative z-10">
                                <button className="flex-1 bg-bank-green text-white py-6 rounded-[1.5rem] font-black text-[10px] uppercase tracking-[0.2em] hover:bg-bank-emerald transition-all active:scale-95 shadow-2xl shadow-bank-green/20 italic">
                                    Sinkronisasi Profil
                                </button>
                                <button className="px-8 bg-red-50 text-red-500 rounded-[1.5rem] font-black border border-red-100 hover:bg-red-500 hover:text-white transition-all active:scale-95 flex items-center justify-center group">
                                    <Trash2 className="h-6 w-6 transition-transform group-hover:scale-110" />
                                </button>
                            </div>
                        </div>
                    </div>
                </div>
            </div>
        </DashboardLayout>
    );
}
