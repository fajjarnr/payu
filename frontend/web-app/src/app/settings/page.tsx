'use client';

import React from 'react';
import DashboardLayout from "@/components/DashboardLayout";
import { User, Mail, Smartphone, MapPin, Globe, Bell, Moon, Sun, Trash2, Shield, CreditCard, LogOut, ChevronRight } from 'lucide-react';
import clsx from 'clsx';
import { PageTransition, StaggerContainer, StaggerItem, ButtonMotion } from '@/components/ui/Motion';

export default function SettingsPage() {
  const menuItems = [
    { label: 'Profil Umum', icon: User, active: true },
    { label: 'Tagihan & Paket', icon: CreditCard, active: false },
    { label: 'Privasi & Keamanan', icon: Shield, active: false },
    { label: 'Pengaturan Lanjut', icon: Globe, active: false },
  ];

  const preferences = [
    { label: 'Notifikasi Push', desc: 'Peringatan transaksi & status real-time', icon: Bell, active: true },
    { label: 'Grafis Mode Gelap', desc: 'Antarmuka visual kontras tinggi', icon: Moon, active: false },
    { label: 'Wawasan Pemasaran', desc: 'Pembaruan promosi, berita, dan hadiah', icon: Globe, active: true },
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
                  <h2 className="text-3xl font-black text-foreground">Ekosistem Akun</h2>
                  <p className="text-sm text-muted-foreground font-medium mt-1">Kelola profil pribadi, preferensi sistem, dan tata kelola akun.</p>
                </div>
              </div>
            </StaggerItem>

            <div className="grid grid-cols-1 lg:grid-cols-12 gap-8">
              {/* Sidebar Profiles */}
              <StaggerItem className="lg:col-span-4 space-y-6">
                <div className="bg-card rounded-xl p-10 border border-border shadow-card flex flex-col items-center text-center relative overflow-hidden group">
                  <div className="absolute top-0 right-0 w-32 h-32 bg-primary/5 rounded-full blur-3xl" />

                  <div className="relative w-24 h-24 bg-primary rounded-2xl flex items-center justify-center text-primary-foreground font-black text-4xl shadow-xl shadow-primary/20 mb-8 transition-transform group-hover:scale-110">
                    P
                  </div>
                  <h3 className="text-xl font-black text-foreground">PENGGUNA PAYU</h3>
                  <p className="text-[10px] font-bold text-primary tracking-widest uppercase mt-3 bg-success-light px-4 py-1.5 rounded-full border border-primary/10">Premium Member</p>

                  <div className="w-full h-[1px] bg-border my-10" />

                  <div className="w-full space-y-4 px-2">
                    <div className="flex items-center justify-between">
                      <span className="text-[10px] font-bold text-muted-foreground tracking-widest uppercase">ID Akun</span>
                      <span className="text-xs font-bold text-foreground font-mono">PAYU-09228373</span>
                    </div>
                    <div className="flex items-center justify-between">
                      <span className="text-[10px] font-bold text-muted-foreground tracking-widest uppercase">Status</span>
                      <span className="text-xs font-bold text-primary">eKYC Terverifikasi</span>
                    </div>
                  </div>
                </div>

                <div className="bg-card rounded-xl p-3 border border-border shadow-card">
                  <div className="space-y-2">
                    {menuItems.map((item, i) => (
                      <button
                        key={i}
                        className={clsx(
                          "w-full flex items-center justify-between px-5 py-4 rounded-xl transition-all",
                          item.active
                            ? "bg-primary text-primary-foreground shadow-lg shadow-primary/20"
                            : "text-muted-foreground hover:bg-muted/50 hover:text-foreground"
                        )}
                      >
                        <div className="flex items-center gap-4">
                          <item.icon className="h-5 w-5" />
                          <span className="text-[10px] font-bold tracking-widest uppercase">{item.label}</span>
                        </div>
                        {item.active && <ChevronRight className="h-4 w-4" />}
                      </button>
                    ))}
                  </div>
                </div>
              </StaggerItem>

              {/* Main Settings Form */}
              <StaggerItem className="lg:col-span-8">
                <div className="bg-card rounded-xl p-8 sm:p-12 border border-border shadow-card space-y-12 relative overflow-hidden h-full">
                  <div className="absolute bottom-0 left-0 w-64 h-64 bg-primary/5 rounded-full blur-3xl -z-0" />

                  {/* Personal Details */}
                  <section className="space-y-10 relative z-10">
                    <div className="flex items-center gap-4">
                      <div className="h-12 w-12 bg-primary/10 rounded-xl flex items-center justify-center border border-primary/10">
                        <User className="h-6 w-6 text-primary" />
                      </div>
                      <h3 className="text-xl font-black text-foreground">Kredensial Profil</h3>
                    </div>

                    <div className="grid grid-cols-1 md:grid-cols-2 gap-8">
                      {[
                        { label: 'Nama Lengkap (Sesuai KTP)', val: 'PENGGUNA PAYU', type: 'text' },
                        { label: 'Email Kontak', val: 'user@payu.id', type: 'email' },
                        { label: 'Protokol Telepon', val: '+62 812-3456-7890', type: 'text' },
                        { label: 'Domisili Saat Ini', val: 'Jakarta, Indonesia', type: 'text' },
                      ].map((field, i) => (
                        <div key={i} className="space-y-3">
                          <label className="text-[10px] font-bold text-muted-foreground tracking-widest uppercase ml-1">{field.label}</label>
                          <input
                            type={field.type}
                            defaultValue={field.val}
                            className="w-full rounded-xl border border-border bg-muted/30 p-4 text-sm font-bold text-foreground outline-none focus:ring-4 focus:ring-primary/10 focus:border-primary transition-all"
                          />
                        </div>
                      ))}
                    </div>
                  </section>

                  <div className="h-[1px] w-full bg-border" />

                  {/* Preferences */}
                  <section className="space-y-10 relative z-10">
                    <div className="flex items-center gap-4">
                      <div className="h-12 w-12 bg-primary/10 rounded-xl flex items-center justify-center border border-primary/10">
                        <Bell className="h-6 w-6 text-primary" />
                      </div>
                      <h3 className="text-xl font-black text-foreground">Preferensi Sistem</h3>
                    </div>

                    <div className="grid grid-cols-1 gap-8">
                      {preferences.map((pref, i) => (
                        <div key={i} className="flex items-center justify-between group p-2 hover:bg-muted/20 rounded-xl transition-all">
                          <div>
                            <p className="font-bold text-foreground text-sm">{pref.label}</p>
                            <p className="text-[10px] text-muted-foreground font-medium uppercase tracking-tight mt-0.5">{pref.desc}</p>
                          </div>
                          <div className={clsx(
                            "w-12 h-6 rounded-full relative p-1 transition-all cursor-pointer",
                            pref.active ? "bg-primary shadow-lg shadow-primary/20" : "bg-muted"
                          )}>
                            <div className={clsx(
                              "w-4 h-4 bg-white rounded-full transition-all shadow-md",
                              pref.active ? "translate-x-6" : "translate-x-0"
                            )} />
                          </div>
                        </div>
                      ))}
                    </div>
                  </section>

                  <div className="flex flex-col sm:flex-row gap-4 pt-10 relative z-10">
                    <button className="flex-1 bg-primary text-primary-foreground py-5 rounded-xl font-bold text-xs tracking-widest uppercase hover:bg-bank-emerald transition-all shadow-xl shadow-primary/20">
                      Sinkronisasi Profil
                    </button>
                    <button className="px-8 py-5 bg-destructive/5 text-destructive rounded-xl font-bold border border-destructive/10 hover:bg-destructive hover:text-white transition-all flex items-center justify-center gap-2 group">
                      <Trash2 className="h-5 w-5" />
                      <span className="text-[10px] font-bold tracking-widest uppercase">Hapus Sesi</span>
                    </button>
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
