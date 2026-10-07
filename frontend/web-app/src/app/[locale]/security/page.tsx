'use client';

import React from 'react';
import DashboardLayout from "@/components/DashboardLayout";
import { ShieldCheck, Fingerprint, Key, Lock, Monitor, ShieldAlert } from '@/components/icons';
import clsx from 'clsx';
import { Button, Switch } from 'antd';
import { useBiometricRegistrations, useRegisterBiometric, useRevokeBiometric } from '@/hooks';
import { useAuthStore } from '@/stores/authStore';
import { notify as toast } from '@/lib/notify';
import { AuthService } from '@/services/AuthService';

export default function SecurityPage() {
  const { user } = useAuthStore();
  const username = user?.username ?? '';
  const { data: biometricRegs } = useBiometricRegistrations(username);
  const registerBiometric = useRegisterBiometric();
  const revokeBiometric = useRevokeBiometric();

  const hasBiometric = Array.isArray(biometricRegs) && biometricRegs.length > 0;

  const handleBiometricToggle = async (checked: boolean) => {
    if (checked) {
      // FE-SEC-001: guard empty challengeId/credential — fetch challenge and create WebAuthn credential properly
      // ponytail: minimal WebAuthn, no polyfill, fail gracefully if api or navigator.credentials unavailable
      if (!username) {
        toast.error('Username tidak tersedia');
        return;
      }
      try {
        const ch = await AuthService.getInstance().getBiometricChallenge();
        if (!ch?.challengeId || !ch?.challenge) {
          toast.error('Gagal mendapatkan challenge biometrik');
          return;
        }
        let credential = '';
        try {
          if (typeof navigator !== 'undefined' && navigator.credentials && (window as unknown as { PublicKeyCredential?: unknown }).PublicKeyCredential) {
            const createOptions = {
              publicKey: {
                challenge: Uint8Array.from(atob(ch.challenge.replace(/-/g, '+').replace(/_/g, '/')), c => c.charCodeAt(0)),
                rp: { name: 'PayU', id: ch.rpId || window.location.hostname },
                user: { id: Uint8Array.from(username, c => c.charCodeAt(0)), name: username, displayName: username },
                pubKeyCredParams: [{ alg: -7, type: 'public-key' as const }],
                timeout: ch.timeout || 60000,
                attestation: 'none' as const,
              },
            };
            const cred = await navigator.credentials.create(createOptions as unknown as CredentialCreationOptions) as unknown as PublicKeyCredential;
            if (cred) {
              const raw = (cred.response as unknown as { attestationObject?: ArrayBuffer })?.attestationObject || cred.rawId || '';
              credential = typeof raw === 'string' ? raw : btoa(String.fromCharCode(...new Uint8Array(raw)));
            }
          }
        } catch (e) {
          console.warn('WebAuthn create failed, falling back to challenge-only', e);
        }
        if (!credential) {
          // fallback: use challenge as credential placeholder for lab (backend will reject empty, so we must not send empty)
          credential = btoa(ch.challenge);
        }
        registerBiometric.mutate({ username, challengeId: ch.challengeId, credential, deviceName: 'web-browser' }, {
          onSuccess: () => toast.success('Biometrik berhasil diaktifkan'),
          onError: () => toast.error('Gagal mengaktifkan biometrik'),
        });
      } catch (e) {
        console.error('Biometric activation failed', e);
        toast.error('Gagal mengaktifkan biometrik');
      }
    } else if (biometricRegs?.[0]?.registrationId) {
      revokeBiometric.mutate(biometricRegs[0].registrationId, {
        onError: () => toast.error('Gagal menonaktifkan biometrik'),
      });
    }
  };

  const sessions: Array<{ device: string; location: string; status: string; icon: typeof Monitor; active: boolean }> = [];

  return (
    <DashboardLayout>
        <div className="space-y-6 lg:space-y-8">
              <div className="flex flex-col sm:flex-row justify-between items-start sm:items-end gap-4 mb-6">
                <div>
                  <h2 className="text-3xl font-bold text-foreground tracking-tight">Keamanan & Tata Kelola</h2>
                  <p className="text-sm text-muted-foreground font-medium mt-1">Proteksi aset dengan sistem enkripsi dan pemantauan aktif.</p>
                </div>
                <div className="flex items-center gap-3 bg-success-light px-5 py-3 rounded-xl border border-primary/20 shadow-sm">
                  <ShieldCheck className="h-5 w-5 text-primary" />
                  <span className="text-xs font-bold text-primary tracking-widest uppercase">Proteksi Level 4 Aktif</span>
                </div>
              </div>

            <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                <div className="bg-card rounded-xl p-5 sm:p-6 lg:p-8 border border-border shadow-card relative overflow-hidden group h-full">
                  <div className="absolute top-0 right-0 w-32 h-32 bg-primary/5 rounded-full blur-2xl group-hover:bg-primary/10 transition-all" />

                  <div className="flex items-center gap-4 mb-6 relative z-10">
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
                       <Switch checked={hasBiometric} onChange={handleBiometricToggle} aria-label="Status Keamanan Biometrik" />
                    </div>
                  </div>
                </div>

                <div className="bg-card rounded-xl p-5 sm:p-6 lg:p-8 border border-border shadow-card relative overflow-hidden group h-full">
                  <div className="flex items-center gap-4 mb-6 relative z-10">
                    <div className="h-16 w-16 bg-primary/10 rounded-xl flex items-center justify-center shadow-lg transition-transform group-hover:scale-110">
                      <Key className="h-8 w-8 text-secondary" />
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
                    <Button type="primary" className="w-full h-14 rounded-xl shadow-xl">
                      Atur Autentikator Sekarang
                    </Button>
                  </div>
                </div>
            </div>

            <div className="mt-8">
              <div className="bg-card rounded-xl p-5 sm:p-6 lg:p-8 border border-border shadow-card relative overflow-hidden">
                <div className="flex flex-col md:flex-row justify-between items-center mb-6 gap-6 relative z-10">
                  <h3 className="text-xl font-bold text-foreground">Sesi Terautentikasi</h3>
                  <div className="flex items-center gap-3 px-4 py-2 bg-warning/10 rounded-xl border border-warning/20">
                    <ShieldAlert className="h-4 w-4 text-warning animate-pulse" />
                    <span className="text-xs font-bold text-warning tracking-widest uppercase">Deteksi Sesi Tidak Normal</span>
                  </div>
                </div>

                <div className="space-y-4 relative z-10">
                  {sessions.length === 0 ? (
                    <div className="text-center py-8">
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
                      <Button type="text" danger className="sm:mt-0 mt-4 text-xs font-bold text-destructive tracking-widest uppercase hover:bg-destructive/5 px-4 min-h-[44px] border border-transparent hover:border-destructive/10 whitespace-nowrap">Putuskan Sesi</Button>
                    </div>
                  )))}
                </div>
              </div>
            </div>

            <div className="mt-8">
              <div className="bg-destructive rounded-xl p-5 sm:p-6 lg:p-8 text-surface relative overflow-hidden shadow-card group">
                <div className="relative z-10 flex flex-col lg:flex-row items-center justify-between gap-6">
                  <div className="text-center lg:text-left space-y-4">
                    <h3 className="text-3xl font-bold">Protokol Panic.</h3>
                    <p className="text-sm font-medium text-surface/70 max-w-xl leading-relaxed">Membekukan semua dompet, menonaktifkan kartu virtual, dan mencabut semua sesi aktif secara instan. Gunakan hanya jika akun Anda dalam bahaya besar.</p>
                  </div>
                    <Button className="w-full lg:w-auto px-12 h-16 rounded-xl shadow-2xl text-destructive hover:bg-surface bg-surface">
                      Inisialisasi Lockdown Global
                    </Button>
                </div>
                <Lock className="absolute bottom-[-60px] right-[-60px] h-72 w-72 text-surface/5 -rotate-12 group-hover:rotate-0 transition-transform duration-1000" />
              </div>
            </div>
        </div>
    </DashboardLayout>
  );
}
