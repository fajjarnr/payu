'use client';

import React from 'react';
import DashboardLayout from "@/components/DashboardLayout";
import { ShieldCheck, Fingerprint, Key, Smartphone, Lock, Eye, AlertCircle, History, Monitor, ShieldAlert } from 'lucide-react';
import clsx from 'clsx';
import { PageTransition, StaggerContainer, StaggerItem, ButtonMotion } from '@/components/ui/Motion';

export default function SecurityPage() {
  const sessions = [
    { device: 'MacBook Pro 16"', location: 'Jakarta, ID', status: 'Sesi Saat Ini', icon: Monitor, active: true },
    { device: 'iPhone 15 Pro', location: 'Surabaya, ID', status: 'Aktif: 2 jam lalu', icon: Smartphone, active: false },
    { device: 'Linux Workstation', location: 'Singapore, SG', status: 'Aktif: 1 hari lalu', icon: Monitor, active: false },
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
                  <h2 className="text-3xl font-black text-foreground">Keamanan & Tata Kelola</h2>
                  <p className="text-sm text-muted-foreground font-medium mt-1">Proteksi aset dengan sistem enkripsi dan pemantauan aktif.</p>
                </div>
                <div className="flex items-center gap-3 bg-success-light px-5 py-3 rounded-xl border border-primary/20 shadow-sm">
                  <ShieldCheck className="h-5 w-5 text-primary" />
                  <span className="text-[10px] font-bold text-primary tracking-widest uppercase">Proteksi Level 4 Aktif</span>
                </div>
              </div>
            </StaggerItem>

            {/* Security Options */}
            <div className="grid grid-cols-1 md:grid-cols-2 gap-8">
              <StaggerItem>
                <div className="bg-card rounded-xl p-8 sm:p-10 border border-border shadow-card relative overflow-hidden group h-full">
                  <div className="absolute top-0 right-0 w-32 h-32 bg-primary/5 rounded-full blur-2xl group-hover:bg-primary/10 transition-all" />

                  <div className="flex items-center gap-6 mb-8 relative z-10">
                    <div className="h-16 w-16 bg-primary/10 rounded-xl flex items-center justify-center shadow-lg transition-transform group-hover:scale-110">
                      <Fingerprint className="h-8 w-8 text-primary" />
                    </div>
                    <div>
                      <h3 className="text-xl font-black text-foreground">MFA Biometrik</h3>
                      <p className="text-[10px] text-muted-foreground font-bold tracking-widest uppercase mt-0.5">Autentikasi Dua Faktor</p>
                    </div>
                  </div>

                  <div className="space-y-8 relative z-10">
                    <p className="text-sm text-muted-foreground font-medium leading-relaxed">
                      Wajibkan sidik jari atau FaceID untuk setiap transaksi di atas <span className="font-bold text-foreground">Rp 1.000.000</span>.
                    </p>
                    <div className="flex items-center justify-between p-5 bg-muted/30 rounded-xl border border-border group-hover:border-primary/20 transition-all">
                      <span className="text-[10px] font-bold text-foreground tracking-widest uppercase">Status Keamanan: Aktif</span>
                      <div className="w-12 h-6 bg-primary rounded-full p-1 relative cursor-pointer shadow-inner shadow-black/10">
                        <div className="w-4 h-4 bg-white rounded-full translate-x-6 shadow-md transition-all" />
                      </div>
                    </div>
                  </div>
                </div>
              </StaggerItem>

              <StaggerItem>
                <div className="bg-card rounded-xl p-8 sm:p-10 border border-border shadow-card relative overflow-hidden group h-full">
                  <div className="flex items-center gap-6 mb-8 relative z-10">
                    <div className="h-16 w-16 bg-blue-500/10 rounded-xl flex items-center justify-center shadow-lg transition-transform group-hover:scale-110">
                      <Key className="h-8 w-8 text-blue-600" />
                    </div>
                    <div>
                      <h3 className="text-xl font-black text-foreground">Token Perangkat</h3>
                      <p className="text-[10px] text-muted-foreground font-bold tracking-widest uppercase mt-0.5">Enkripsi Hardware</p>
                    </div>
                  </div>

                  <div className="space-y-8 relative z-10">
                    <p className="text-sm text-muted-foreground font-medium leading-relaxed">
                      Gunakan kunci keamanan fisik atau aplikasi autentikator digital untuk login pada perangkat baru.
                    </p>
                    <button className="w-full py-4 bg-foreground text-background rounded-xl font-bold text-[10px] tracking-widest uppercase hover:bg-primary hover:text-white transition-all shadow-xl">
                      Atur Autentikator Sekarang
                    </button>
                  </div>
                </div>
              </StaggerItem>
            </div>

            {/* Active Sessions */}
            <StaggerItem className="mt-8">
              <div className="bg-card rounded-xl p-8 sm:p-12 border border-border shadow-card relative overflow-hidden">
                <div className="flex flex-col md:flex-row justify-between items-center mb-10 gap-6 relative z-10">
                  <h3 className="text-xl font-black text-foreground">Sesi Terautentikasi</h3>
                  <div className="flex items-center gap-3 px-4 py-2 bg-amber-500/10 rounded-xl border border-amber-500/20">
                    <ShieldAlert className="h-4 w-4 text-amber-600 animate-pulse" />
                    <span className="text-[10px] font-bold text-amber-700 tracking-widest uppercase">Deteksi Sesi Tidak Normal</span>
                  </div>
                </div>

                <div className="space-y-4 relative z-10">
                  {sessions.map((session, i) => (
                    <div key={i} className="flex flex-col sm:flex-row items-center justify-between p-6 bg-muted/30 rounded-xl border border-transparent hover:border-border transition-all group hover:bg-card">
                      <div className="flex items-center gap-6 w-full">
                        <div className="h-14 w-14 bg-card rounded-xl flex items-center justify-center shadow-md border border-border group-hover:scale-105 transition-all">
                          <session.icon className={clsx("h-6 w-6", session.active ? "text-primary" : "text-muted-foreground")} />
                        </div>
                        <div>
                          <p className="font-bold text-foreground text-sm">{session.device}</p>
                          <p className="text-[10px] font-medium text-muted-foreground tracking-widest uppercase mt-0.5">{session.location} • {session.status}</p>
                        </div>
                      </div>
                      <button className="sm:mt-0 mt-4 text-[10px] font-bold text-destructive tracking-widest uppercase hover:bg-destructive/5 px-4 py-2 rounded-lg transition-all border border-transparent hover:border-destructive/10 whitespace-nowrap">Putuskan Sesi</button>
                    </div>
                  ))}
                </div>
              </div>
            </StaggerItem>

            {/* Panic Button Section */}
            <StaggerItem className="mt-8">
              <div className="bg-destructive rounded-xl p-8 sm:p-12 text-white relative overflow-hidden shadow-card group">
                <div className="relative z-10 flex flex-col lg:flex-row items-center justify-between gap-10">
                  <div className="text-center lg:text-left space-y-4">
                    <h3 className="text-3xl font-black">Protokol Panic.</h3>
                    <p className="text-sm font-medium text-white/70 max-w-xl leading-relaxed">Membekukan semua dompet, menonaktifkan kartu virtual, dan mencabut semua sesi aktif secara instan. Gunakan hanya jika akun Anda dalam bahaya besar.</p>
                  </div>
                  <ButtonMotion>
                    <button className="whitespace-nowrap bg-white text-destructive px-12 py-6 rounded-xl font-bold text-xs tracking-widest uppercase hover:bg-muted transition-all shadow-2xl">
                      Inisialisasi Lockdown Global
                    </button>
                  </ButtonMotion>
                </div>
                <Lock className="absolute bottom-[-60px] right-[-60px] h-72 w-72 text-white/5 -rotate-12 group-hover:rotate-0 transition-transform duration-1000" />
              </div>
            </StaggerItem>
          </StaggerContainer>
        </div>
      </PageTransition>
    </DashboardLayout>
  );
}
