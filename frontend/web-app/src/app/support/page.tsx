'use client';

import React from 'react';
import DashboardLayout from "@/components/DashboardLayout";
import { LifeBuoy, MessageCircle, Mail, Phone, ExternalLink, HelpCircle, FileText, CheckCircle2 } from 'lucide-react';
import clsx from 'clsx';

export default function SupportPage() {
    return (
        <DashboardLayout>
            <div className="max-w-4xl mx-auto space-y-12 pb-10">
                <div className="flex justify-between items-end">
                    <div>
                        <h2 className="text-3xl font-black text-foreground uppercase tracking-tight italic">Terminal Bantuan</h2>
                        <p className="text-sm text-gray-500 font-medium">Ada yang bisa kami bantu hari ini? Tim spesialis kami siap melayani 24/7.</p>
                    </div>
                </div>

                <div className="grid grid-cols-1 md:grid-cols-3 gap-8">
                    {[
                        { label: 'Bantuan Langsung', desc: 'Obrolan real-time dengan agen dukungan kami.', icon: MessageCircle, action: 'Hubungkan Sekarang', color: 'bg-bank-green' },
                        { label: 'Protokol Sinyal', desc: 'Dukungan email asinkron yang aman.', icon: Mail, action: 'Kirim Sinyal', color: 'bg-blue-600' },
                        { label: 'Prioritas Langsung', desc: 'Dukungan telepon global secara instan.', icon: Phone, action: 'Minta Panggilan', color: 'bg-foreground' },
                    ].map((channel, i) => (
                        <div key={i} className="bg-card rounded-[2.5rem] p-10 border border-border shadow-sm flex flex-col items-center text-center group hover:shadow-2xl hover:shadow-bank-green/10 transition-all duration-500">
                            <div className={clsx("h-20 w-20 mb-8 rounded-[1.5rem] flex items-center justify-center text-white shadow-2xl transition-transform group-hover:scale-110", channel.color)}>
                                <channel.icon className="h-10 w-10" />
                            </div>
                            <h3 className="text-lg font-black text-foreground uppercase tracking-tight mb-3 italic">{channel.label}</h3>
                            <p className="text-[10px] text-gray-400 font-bold uppercase tracking-[0.2em] leading-relaxed mb-10">{channel.desc}</p>
                            <button className="w-full py-5 bg-gray-50 dark:bg-gray-900 border border-border rounded-2xl font-black text-[10px] uppercase tracking-[0.2em] hover:bg-bank-green hover:text-white hover:border-bank-green transition-all active:scale-95 shadow-sm">
                                {channel.action}
                            </button>
                        </div>
                    ))}
                </div>

                <div className="grid grid-cols-1 lg:grid-cols-12 gap-10">
                    {/* Knowledge Base */}
                    <div className="lg:col-span-12">
                        <div className="bg-card rounded-[3rem] p-12 border border-border shadow-sm relative overflow-hidden">
                            <div className="absolute top-0 right-0 w-64 h-64 bg-bank-green/5 rounded-full blur-3xl" />
                            <h3 className="text-xl font-black text-foreground uppercase tracking-tight mb-10 italic relative z-10">Repositori Inteligensi</h3>
                            <div className="grid grid-cols-1 md:grid-cols-2 gap-8 relative z-10">
                                {[
                                    { title: 'Sinkronisasi Identitas', desc: 'Memperbarui eKYC dan pemetaan biometrik Anda.', icon: HelpCircle },
                                    { title: 'Batas Transaksi Global', desc: 'Memahami batas tingkat kredit akun Anda.', icon: FileText },
                                    { title: 'Masalah Token Perangkat', desc: 'Pemecahan masalah MFA dan kesalahan kunci aman.', icon: HelpCircle },
                                    { title: 'Protokol Pencegahan Penipuan', desc: 'Langkah tindakan untuk aktivitas akun mencurigakan.', icon: FileText },
                                ].map((faq, i) => (
                                    <div key={i} className="flex gap-6 p-8 bg-gray-50 dark:bg-gray-900/50 rounded-[2rem] border border-transparent hover:border-border transition-all cursor-pointer group hover:bg-white dark:hover:bg-gray-900 duration-300">
                                        <div className="h-14 w-14 bg-white dark:bg-gray-800 rounded-2xl flex items-center justify-center shadow-lg border border-border shrink-0 transition-transform group-hover:rotate-12 duration-500">
                                            <faq.icon className="h-6 w-6 text-bank-green" />
                                        </div>
                                        <div>
                                            <div className="flex items-center gap-3 mb-2">
                                                <h4 className="font-black text-foreground text-sm uppercase tracking-tight italic">{faq.title}</h4>
                                                <ExternalLink className="h-3 w-3 text-bank-green opacity-0 group-hover:opacity-100 transition-opacity" />
                                            </div>
                                            <p className="text-[10px] text-gray-400 font-bold uppercase tracking-widest leading-relaxed">{faq.desc}</p>
                                        </div>
                                    </div>
                                ))}
                            </div>
                        </div>
                    </div>

                    <div className="lg:col-span-12">
                        <div className="bg-gradient-to-br from-gray-900 to-gray-800 rounded-[3rem] p-12 text-white relative overflow-hidden shadow-2xl group">
                            <div className="relative z-10 flex flex-col md:flex-row items-center justify-between gap-12 text-center md:text-left">
                                <div className="space-y-6 max-w-2xl">
                                    <h3 className="text-4xl font-black italic tracking-tighter">Status Integritas Sistem.</h3>
                                    <div className="flex flex-wrap justify-center md:justify-start gap-4">
                                        {['Gateway', 'Transaksi', 'Dompet', 'Auth'].map((svc, i) => (
                                            <div key={i} className="flex items-center gap-2 bg-white/10 px-5 py-2.5 rounded-xl border border-white/10 shadow-sm">
                                                <CheckCircle2 className="h-4 w-4 text-bank-green" />
                                                <span className="text-[10px] font-black uppercase tracking-widest">{svc} Operasional</span>
                                            </div>
                                        ))}
                                    </div>
                                    <p className="text-sm text-gray-500 font-medium pt-2 uppercase tracking-wide">Semua node infrastruktur saat ini melaporkan uptime 100% dengan latensi nol yang terdeteksi.</p>
                                </div>
                                <button className="whitespace-nowrap px-12 py-6 bg-bank-green text-white rounded-3xl font-black uppercase text-xs tracking-[0.2em] hover:bg-bank-emerald transition-all active:scale-95 shadow-2xl shadow-bank-green/20 italic">
                                    Laporkan Masalah Node
                                </button>
                            </div>
                            <LifeBuoy className="absolute top-[-40px] left-[-40px] h-72 w-72 text-white/5 -rotate-12 group-hover:rotate-12 transition-transform duration-[2000ms]" />
                        </div>
                    </div>
                </div>
            </div>
        </DashboardLayout>
    );
}
