'use client';

import React from 'react';
import DashboardLayout from "@/components/DashboardLayout";
import { LifeBuoy, MessageCircle, Mail, Phone, ExternalLink, HelpCircle, FileText, CheckCircle2 } from 'lucide-react';
import clsx from 'clsx';
import { PageTransition, StaggerContainer, StaggerItem, ButtonMotion } from '@/components/ui/Motion';

export default function SupportPage() {
  const supportChannels = [
    { label: 'Bantuan Langsung', desc: 'Obrolan real-time dengan agen dukungan kami.', icon: MessageCircle, action: 'Hubungkan Sekarang', color: 'primary' },
    { label: 'Protokol Email', desc: 'Dukungan email asinkron yang aman.', icon: Mail, action: 'Kirim Pesan', color: 'blue-600' },
    { label: 'Panggilan Suara', desc: 'Dukungan telepon global secara instan.', icon: Phone, action: 'Hubungi Kami', color: 'gray-900' },
  ];

  const faqs = [
    { title: 'Sinkronisasi Identitas', desc: 'Memperbarui eKYC dan pemetaan biometrik Anda.', icon: HelpCircle },
    { title: 'Batas Transaksi Global', desc: 'Memahami batas tingkat kredit akun Anda.', icon: FileText },
    { title: 'Masalah Token Perangkat', desc: 'Pemecahan masalah MFA dan kesalahan kunci aman.', icon: HelpCircle },
    { title: 'Protokol Pencegahan Penipuan', desc: 'Langkah tindakan untuk aktivitas akun mencurigakan.', icon: FileText },
  ];

  return (
    <DashboardLayout>
      <PageTransition>
        <div className="space-y-12">
          {/* Header */}
          <StaggerContainer>
            <StaggerItem>
              <div className="flex flex-col sm:flex-row justify-between items-start sm:items-end gap-6 mb-8">
                <div>
                  <h2 className="text-3xl font-black text-foreground">Terminal Bantuan</h2>
                  <p className="text-sm text-muted-foreground font-medium mt-1">Ada yang bisa kami bantu? Tim spesialis kami siap melayani 24/7.</p>
                </div>
              </div>
            </StaggerItem>

            {/* Support Channels */}
            <div className="grid grid-cols-1 md:grid-cols-3 gap-8">
              {supportChannels.map((channel, i) => (
                <StaggerItem key={i}>
                  <div className="bg-card rounded-xl p-8 sm:p-10 border border-border shadow-card flex flex-col items-center text-center group hover:shadow-xl transition-all duration-500">
                    <div className={clsx(
                      "h-20 w-20 mb-8 rounded-2xl flex items-center justify-center text-white shadow-lg transition-transform group-hover:scale-110",
                      channel.color === 'primary' ? "bg-primary shadow-primary/20" :
                        channel.color === 'blue-600' ? "bg-blue-600 shadow-blue-600/20" : "bg-foreground shadow-foreground/10"
                    )}>
                      <channel.icon className="h-10 w-10" />
                    </div>
                    <h3 className="text-lg font-black text-foreground mb-3">{channel.label}</h3>
                    <p className="text-[11px] text-muted-foreground font-medium leading-relaxed mb-10 max-w-[200px]">{channel.desc}</p>
                    <button className="w-full py-4 bg-muted/50 border border-border rounded-xl font-bold text-[10px] tracking-widest uppercase hover:bg-primary hover:text-white hover:border-primary transition-all text-muted-foreground">
                      {channel.action}
                    </button>
                  </div>
                </StaggerItem>
              ))}
            </div>

            {/* Knowledge Base */}
            <StaggerItem className="mt-4">
              <div className="bg-card rounded-xl p-8 sm:p-12 border border-border shadow-card relative overflow-hidden">
                <div className="absolute top-0 right-0 w-80 h-80 bg-primary/5 rounded-full blur-3xl" />
                <h3 className="text-xl font-black text-foreground mb-10 relative z-10">Repositori Inteligensi</h3>
                <div className="grid grid-cols-1 md:grid-cols-2 gap-6 relative z-10">
                  {faqs.map((faq, i) => (
                    <div key={i} className="flex gap-6 p-6 bg-muted/30 rounded-xl border border-transparent hover:border-border transition-all cursor-pointer group hover:bg-card duration-300">
                      <div className="h-14 w-14 bg-card rounded-xl flex items-center justify-center shadow-md border border-border shrink-0 transition-transform group-hover:rotate-6">
                        <faq.icon className="h-6 w-6 text-primary" />
                      </div>
                      <div>
                        <div className="flex items-center gap-3 mb-1">
                          <h4 className="font-bold text-foreground text-sm">{faq.title}</h4>
                          <ExternalLink className="h-3 w-3 text-primary opacity-0 group-hover:opacity-100 transition-opacity" />
                        </div>
                        <p className="text-[10px] text-muted-foreground font-medium leading-relaxed uppercase tracking-wider">{faq.desc}</p>
                      </div>
                    </div>
                  ))}
                </div>
              </div>
            </StaggerItem>

            {/* System Status Banner */}
            <StaggerItem className="mt-4">
              <div className="bg-gradient-to-br from-gray-900 to-gray-800 rounded-xl p-8 sm:p-12 text-white relative overflow-hidden shadow-2xl group">
                <div className="relative z-10 flex flex-col lg:flex-row items-center justify-between gap-12 text-center lg:text-left">
                  <div className="space-y-6 max-w-2xl">
                    <h3 className="text-3xl font-black">Integritas Sistem Aktif.</h3>
                    <div className="flex flex-wrap justify-center lg:justify-start gap-3">
                      {['Gateway', 'Backend', 'Database', 'Streaming'].map((svc, i) => (
                        <div key={i} className="flex items-center gap-2 bg-white/10 px-4 py-2 rounded-xl border border-white/10 shadow-sm backdrop-blur-md">
                          <CheckCircle2 className="h-4 w-4 text-bank-green" />
                          <span className="text-[10px] font-bold tracking-widest uppercase">{svc} OK</span>
                        </div>
                      ))}
                    </div>
                    <p className="text-sm text-gray-400 font-medium pt-2 leading-relaxed">Seluruh node infrastruktur saat ini melaporkan uptime 100%. Tidak ada gangguan atau latensi yang terdeteksi dalam 24 jam terakhir.</p>
                  </div>
                  <ButtonMotion>
                    <button className="whitespace-nowrap px-10 py-5 bg-bank-green text-white rounded-xl font-bold text-xs tracking-widest uppercase hover:bg-bank-emerald transition-all shadow-2xl shadow-bank-green/20">
                      Cek Detail Infrastruktur
                    </button>
                  </ButtonMotion>
                </div>
                <LifeBuoy className="absolute bottom-[-60px] right-[-60px] h-72 w-72 text-white/5 -rotate-12 group-hover:rotate-12 transition-transform duration-[3000ms]" />
              </div>
            </StaggerItem>
          </StaggerContainer>
        </div>
      </PageTransition>
    </DashboardLayout>
  );
}
