'use client';

import React from 'react';
import DashboardLayout from "@/components/DashboardLayout";
import { ShieldCheck, Fingerprint, Key, Smartphone, Lock, Monitor, ShieldAlert } from 'lucide-react';
import clsx from 'clsx';
import { PageTransition, StaggerContainer, StaggerItem, ButtonMotion } from '@/components/ui/Motion';
import { Button } from '@/components/ui/button';
import { Switch } from '@/components/ui/switch';
import { useBiometricRegistrations, useRegisterBiometric, useRevokeBiometric } from '@/hooks';
import { useAuthStore } from '@/stores/authStore';
import { toast } from 'sonner';

export default function SecurityPage() {
  const { user } = useAuthStore();
  const username = user?.username ?? '';
  const { data: biometricRegs } = useBiometricRegistrations(username);
  const registerBiometric = useRegisterBiometric();
  const revokeBiometric = useRevokeBiometric();

  const hasBiometric = Array.isArray(biometricRegs) && biometricRegs.length > 0;

  const handleBiometricToggle = (checked: boolean) => {
    if (checked) {
      registerBiometric.mutate({ username, challengeId: '', credential: '', deviceName: 'web-browser' }, {
        onError: () => toast.error('Gagal mengaktifkan biometrik'),
      });
    } else if (biometricRegs?.[0]?.registrationId) {
      revokeBiometric.mutate(biometricRegs[0].registrationId, {
        onError: () => toast.error('Gagal menonaktifkan biometrik'),
      });
    }
  };

  const sessions: Array<{ device: string; location: string; status: string; icon: typeof Monitor; active: boolean }> = [];

  return (
    <DashboardLayout>
      <PageTransition>
        <div className="space-y-12">
          {/* Header */}
          <StaggerContainer>
            <StaggerItem>
              <div className="flex flex-col sm:flex-row justify-between items-start sm:items-end gap-6 mb-8">
                <div>
                  <h2 className="text-3xl font-bold text-foreground">Keamanan & Tata Kelola</h2>
                  <p className="text-sm text-muted-foreground font-medium mt-1">Proteksi aset dengan sistem enkripsi dan pemantauan aktif.</p>
                </div>
                <div className="flex items-center gap-3 bg-success-light px-5 py-3 rounded-xl border border-primary/20 shadow-sm">
                  <ShieldCheck className="h-5 w-5 text-primary" />
                  <span className="text-xs font-bold text-primary tracking-widest uppercase">Proteksi Level 4 Aktif</span>
                </div>
              </div>
            </StaggerItem>

            {/* Security Options */}
            <div className="grid grid-cols-1 md:grid-cols-2 gap-8">
              <StaggerItem>
                <div className="bg-card rounded-xl p-8 sm:p-8 border border-border shadow-card relative overflow-hidden group h-full">
                  <div className="absolute top-0 right-0 w-32 h-32 bg-primary/5 rounded-full blur-2xl group-hover:bg-primary/10 transition-all" />

                  <div className="flex items-center gap-6 mb-8 relative z-10">
                    <div className="h-16 w-16 bg-primary/10 rounded-xl flex items-center justify-center shadow-lg transition-transform group-hover:scale-110">
                      <Fingerprint className="h-8 w-8 text-primary" />
                    </div>
                    <div>
                      <h3 className="text-xl font-bold text-foreground">MFA Biometrik</h3>
                      <p className="text-xs text-muted-foreground font-bold tracking-widest uppercase mt-0.5">Autentikasi Dua Faktor</p>
                    </div>
                  </div>

                  <div className="space-y-8 relative z-10">
                    <p className="text-sm text-muted-foreground font-medium leading-relaxed">
                      Wajibkan sidik jari atau FaceID untuk setiap transaksi di atas <span className="font-bold text-foreground">Rp 1.000.000</span>.
                    </p>
                    <div className="flex items-center justify-between p-5 bg-muted/20 rounded-xl border border-border group-hover:border-primary/20 transition-all">
                      <span className="text-xs font-bold text-foreground tracking-widest uppercase">Status Keamanan: {hasBiometric ? 'Aktif' : 'Non-aktif'}</span>
                       <Switch checked={hasBiometric} onCheckedChange={handleBiometricToggle} />
                    </div>
                  </div>
                </div>
              </StaggerItem>

              <StaggerItem>
                <div className="bg-card rounded-xl p-8 sm:p-8 border border-border shadow-card relative overflow-hidden group h-full">
                  <div className="flex items-center gap-6 mb-8 relative z-10">
                    <div className="h-16 w-16 bg-blue-500/10 rounded-xl flex items-center justify-center shadow-lg transition-transform group-hover:scale-110">
                      <Key className="h-8 w-8 text-blue-600" />
                    </div>
                    <div>
                      <h3 className="text-xl font-bold text-foreground">Token Perangkat</h3>
                      <p className="text-xs text-muted-foreground font-bold tracking-widest uppercase mt-0.5">Enkripsi Hardware</p>
                    </div>
                  </div>

                  <div className="space-y-8 relative z-10">
                    <p className="text-sm text-muted-foreground font-medium leading-relaxed">
                      Gunakan kunci keamanan fisik atau aplikasi autentikator digital untuk login pada perangkat baru.
                    </p>
                    <Button className="w-full h-14 rounded-xl shadow-xl">
                      Atur Autentikator Sekarang
                    </Button>
                  </div>
                </div>
              </StaggerItem>
            </div>

            {/* Active Sessions */}
            <StaggerItem className="mt-8">
              <div className="bg-card rounded-xl p-8 sm:p-8 border border-border shadow-card relative overflow-hidden">
                <div className="flex flex-col md:flex-row justify-between items-center mb-10 gap-6 relative z-10">
                  <h3 className="text-xl font-bold text-foreground">Sesi Terautentikasi</h3>
                  <div className="flex items-center gap-3 px-4 py-2 bg-amber-500/10 rounded-xl border border-amber-500/20">
                    <ShieldAlert className="h-4 w-4 text-amber-600 animate-pulse" />
                    <span className="text-xs font-bold text-amber-700 tracking-widest uppercase">Deteksi Sesi Tidak Normal</span>
                  </div>
                </div>

                <div className="space-y-4 relative z-10">
                  {sessions.length === 0 ? (
                    <div className="text-center py-12">
                      <Monitor className="h-12 w-12 mx-auto text-muted-foreground/30 mb-4" />
                      <p className="text-sm text-muted-foreground font-bold tracking-widest uppercase">Tidak ada sesi aktif yang terdeteksi</p>
                    </div>
                  ) : (
                  sessions.map((session, i) => (
                    <div key={i} className="flex flex-col sm:flex-row items-center justify-between p-6 bg-muted/30 rounded-xl border border-transparent hover:border-border transition-all group hover:bg-card">
                      <div className="flex items-center gap-6 w-full">
                        <div className="h-14 w-14 bg-card rounded-xl flex items-center justify-center shadow-md border border-border group-hover:scale-105 transition-all">
                          <session.icon className={clsx("h-6 w-6", session.active ? "text-primary" : "text-muted-foreground")} />
                        </div>
                        <div>
                          <p className="font-bold text-foreground text-sm">{session.device}</p>
                          <p className="text-xs font-medium text-muted-foreground tracking-widest uppercase mt-0.5">{session.location} • {session.status}</p>
                        </div>
                      </div>
                      <Button variant="ghost" className="sm:mt-0 mt-4 text-xs font-bold text-destructive tracking-widest uppercase hover:bg-destructive/5 px-4 h-10 border border-transparent hover:border-destructive/10 whitespace-nowrap">Putuskan Sesi</Button>
                    </div>
                  )))}
                </div>
              </div>
            </StaggerItem>

            {/* Panic Button Section */}
            <StaggerItem className="mt-8">
              <div className="bg-destructive rounded-xl p-8 sm:p-8 text-white relative overflow-hidden shadow-card group">
                <div className="relative z-10 flex flex-col lg:flex-row items-center justify-between gap-8">
                  <div className="text-center lg:text-left space-y-4">
                    <h3 className="text-3xl font-bold">Protokol Panic.</h3>
                    <p className="text-sm font-medium text-white/70 max-w-xl leading-relaxed">Membekukan semua dompet, menonaktifkan kartu virtual, dan mencabut semua sesi aktif secara instan. Gunakan hanya jika akun Anda dalam bahaya besar.</p>
                  </div>
                  <ButtonMotion className="w-full lg:w-auto">
                    <Button variant="secondary" className="px-12 h-16 rounded-xl shadow-2xl text-destructive hover:bg-white bg-white">
                      Inisialisasi Lockdown Global
                    </Button>
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
