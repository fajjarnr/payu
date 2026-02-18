'use client';

import React, { useState, useEffect } from 'react';
import DashboardLayout from "@/components/DashboardLayout";
import { User, Globe, Bell, Moon, Trash2, Shield, CreditCard, ChevronRight, FileText, Loader2, CheckCircle } from 'lucide-react';
import clsx from 'clsx';
import { PageTransition, StaggerContainer, StaggerItem } from '@/components/ui/Motion';
import StatementDownloader from '@/components/settings/statement-downloader';
import { Input } from '@/components/ui/input';
import { Switch } from '@/components/ui/switch';
import { Button } from '@/components/ui/button';
import { useAuth, useUpdateUser } from '@/hooks';
import { useAuthStore } from '@/stores';
import { Alert, AlertDescription } from '@/components/ui/alert';

export default function SettingsPage() {
  const [activeTab, setActiveTab] = useState<'profile' | 'statements'>('profile');
  const { user } = useAuth();
  const updateUser = useUpdateUser();
  const logout = useAuthStore((state) => state.logout);

  // Form state
  const [formData, setFormData] = useState({
    fullName: '',
    email: '',
    phoneNumber: '',
  });

  // Initialize form with user data
  useEffect(() => {
    if (user) {
      setFormData({
        fullName: user.fullName || '',
        email: user.email || '',
        phoneNumber: user.phoneNumber || '',
      });
    }
  }, [user]);

  const menuItems = [
    { label: 'Profil Umum', icon: User, active: activeTab === 'profile', onClick: () => setActiveTab('profile') },
    { label: 'E-Statement', icon: FileText, active: activeTab === 'statements', onClick: () => setActiveTab('statements') },
    { label: 'Tagihan & Paket', icon: CreditCard, active: false },
    { label: 'Privasi & Keamanan', icon: Shield, active: false },
    { label: 'Pengaturan Lanjut', icon: Globe, active: false },
  ];

  const preferences = [
    { label: 'Notifikasi Push', desc: 'Peringatan transaksi & status real-time', icon: Bell, active: true },
    { label: 'Grafis Mode Gelap', desc: 'Antarmuka visual kontras tinggi', icon: Moon, active: false },
    { label: 'Wawasan Pemasaran', desc: 'Pembaruan promosi, berita, dan hadiah', icon: Globe, active: true },
  ];

  const handleInputChange = (field: string, value: string) => {
    setFormData(prev => ({ ...prev, [field]: value }));
  };

  const handleSubmit = async () => {
    if (!user?.id) return;

    await updateUser.mutateAsync({
      userId: user.id,
      data: formData,
    });
  };

  const handleClearSession = () => {
    logout();
    window.location.href = '/login';
  };

  return (
    <DashboardLayout>
      <PageTransition>
        <div className="space-y-12">
          {/* Header */}
          <StaggerContainer>
            <StaggerItem>
              <div className="flex flex-col sm:flex-row justify-between items-start sm:items-end gap-6 mb-8">
                <div>
                  <h2 className="text-3xl font-bold text-foreground">Ekosistem Akun</h2>
                  <p className="text-sm text-muted-foreground font-medium mt-1">Kelola profil pribadi, preferensi sistem, dan tata kelola akun.</p>
                </div>
              </div>
            </StaggerItem>

            <div className="grid grid-cols-1 md:grid-cols-12 lg:grid-cols-12 gap-8">
              {/* Sidebar Profiles */}
              <StaggerItem className="md:col-span-6 lg:col-span-4 space-y-6">
                <div className="bg-card rounded-xl p-8 border border-border shadow-card flex flex-col items-center text-center relative overflow-hidden group">
                  <div className="absolute top-0 right-0 w-32 h-32 bg-primary/5 rounded-full blur-3xl" />

                  <div className="relative w-24 h-24 bg-primary rounded-2xl flex items-center justify-center text-primary-foreground font-bold text-4xl shadow-xl shadow-primary/20 mb-8 transition-transform group-hover:scale-110">
                    {formData.fullName ? formData.fullName.charAt(0).toUpperCase() : 'P'}
                  </div>
                  <h3 className="text-xl font-bold text-foreground">{formData.fullName || 'PENGGUNA PAYU'}</h3>
                  <p className="text-xs font-bold text-primary tracking-widest uppercase mt-3 bg-success-light px-4 py-1.5 rounded-full border border-primary/10">Premium Member</p>

                  <div className="w-full h-[1px] bg-border my-10" />

                  <div className="w-full space-y-4 px-2">
                    <div className="flex items-center justify-between">
                      <span className="text-xs font-bold text-muted-foreground tracking-widest uppercase">ID Akun</span>
                      <span className="text-xs font-bold text-foreground font-mono">{user?.id?.slice(0, 12) || 'PAYU-09228373'}</span>
                    </div>
                    <div className="flex items-center justify-between">
                      <span className="text-xs font-bold text-muted-foreground tracking-widest uppercase">Status</span>
                      <span className="text-xs font-bold text-primary">eKYC Terverifikasi</span>
                    </div>
                  </div>
                </div>

                <div className="bg-card rounded-xl p-3 border border-border shadow-card">
                  <div className="space-y-2">
                    {menuItems.map((item, i) => (
                      <button
                        key={i}
                        onClick={item.onClick}
                        className={clsx(
                          "w-full flex items-center justify-between px-5 py-4 rounded-xl transition-all",
                          item.active
                            ? "bg-primary text-primary-foreground shadow-lg shadow-primary/20"
                            : "text-muted-foreground hover:bg-muted/50 hover:text-foreground"
                        )}
                      >
                        <div className="flex items-center gap-4">
                          <item.icon className="h-5 w-5" />
                          <span className="text-xs font-bold tracking-widest uppercase">{item.label}</span>
                        </div>
                        {item.active && <ChevronRight className="h-4 w-4" />}
                      </button>
                    ))}
                  </div>
                </div>
              </StaggerItem>

              {/* Main Settings Form */}
              <StaggerItem className="md:col-span-6 lg:col-span-8">
                {activeTab === 'profile' ? (
                  <div className="bg-card rounded-xl p-8 sm:p-8 border border-border shadow-card space-y-12 relative overflow-hidden h-full">
                    <div className="absolute bottom-0 left-0 w-64 h-64 bg-primary/5 rounded-full blur-3xl -z-0" />

                    {/* Success Alert */}
                    {updateUser.isSuccess && (
                      <Alert className="bg-green-500/10 border-green-500/20 relative z-10">
                        <CheckCircle className="h-4 w-4 text-green-500" />
                        <AlertDescription className="text-green-500">
                          Profil berhasil diperbarui!
                        </AlertDescription>
                      </Alert>
                    )}

                    {/* Error Alert */}
                    {updateUser.isError && (
                      <Alert className="bg-red-500/10 border-red-500/20 relative z-10">
                        <AlertDescription className="text-red-500">
                          Gagal memperbarui profil. Silakan coba lagi.
                        </AlertDescription>
                      </Alert>
                    )}

                    {/* Personal Details */}
                    <section className="space-y-12 relative z-10">
                      <div className="flex items-center gap-4">
                        <div className="h-12 w-12 bg-primary/10 rounded-xl flex items-center justify-center border border-primary/10">
                          <User className="h-6 w-6 text-primary" />
                        </div>
                        <h3 className="text-xl font-bold text-foreground">Kredensial Profil</h3>
                      </div>

                      <div className="grid grid-cols-1 md:grid-cols-2 gap-8">
                        <div className="space-y-3">
                          <label className="text-xs font-bold text-muted-foreground tracking-widest uppercase ml-1">
                            Nama Lengkap (Sesuai KTP)
                          </label>
                          <Input
                            type="text"
                            value={formData.fullName}
                            onChange={(e) => handleInputChange('fullName', e.target.value)}
                            placeholder="Nama lengkap"
                            className="font-bold"
                            disabled={updateUser.isPending}
                          />
                        </div>
                        <div className="space-y-3">
                          <label className="text-xs font-bold text-muted-foreground tracking-widest uppercase ml-1">
                            Email Kontak
                          </label>
                          <Input
                            type="email"
                            value={formData.email}
                            onChange={(e) => handleInputChange('email', e.target.value)}
                            placeholder="email@contoh.com"
                            className="font-bold"
                            disabled={updateUser.isPending}
                          />
                        </div>
                        <div className="space-y-3">
                          <label className="text-xs font-bold text-muted-foreground tracking-widest uppercase ml-1">
                            Protokol Telepon
                          </label>
                          <Input
                            type="text"
                            value={formData.phoneNumber}
                            onChange={(e) => handleInputChange('phoneNumber', e.target.value)}
                            placeholder="+62 812-3456-7890"
                            className="font-bold"
                            disabled={updateUser.isPending}
                          />
                        </div>
                        <div className="space-y-3">
                          <label className="text-xs font-bold text-muted-foreground tracking-widest uppercase ml-1">
                            Domisili Saat Ini
                          </label>
                          <Input
                            type="text"
                            value={formData.address}
                            onChange={(e) => handleInputChange('address', e.target.value)}
                            placeholder="Jakarta, Indonesia"
                            className="font-bold"
                            disabled={updateUser.isPending}
                          />
                        </div>
                      </div>
                    </section>

                    <div className="h-[1px] w-full bg-border" />

                    {/* Preferences */}
                    <section className="space-y-12 relative z-10">
                      <div className="flex items-center gap-4">
                        <div className="h-12 w-12 bg-primary/10 rounded-xl flex items-center justify-center border border-primary/10">
                          <Bell className="h-6 w-6 text-primary" />
                        </div>
                        <h3 className="text-xl font-bold text-foreground">Preferensi Sistem</h3>
                      </div>

                      <div className="grid grid-cols-1 gap-8">
                        {preferences.map((pref, i) => (
                          <div key={i} className="flex items-center justify-between group p-2 hover:bg-muted/20 rounded-xl transition-all">
                            <div>
                              <p className="font-bold text-foreground text-sm">{pref.label}</p>
                              <p className="text-xs text-muted-foreground font-medium uppercase tracking-widest mt-0.5">{pref.desc}</p>
                            </div>
                            <Switch defaultChecked={pref.active} />
                          </div>
                        ))}
                      </div>
                    </section>

                    <div className="flex flex-col sm:flex-row gap-4 pt-10 relative z-10">
                      <Button
                        className="flex-1 shadow-xl"
                        onClick={handleSubmit}
                        disabled={updateUser.isPending || !user?.id}
                      >
                        {updateUser.isPending ? (
                          <>
                            <Loader2 className="h-4 w-4 mr-2 animate-spin" />
                            Menyimpan...
                          </>
                        ) : (
                          'Sinkronisasi Profil'
                        )}
                      </Button>
                      <Button
                        variant="outline"
                        className="text-red-500 hover:bg-red-500/10 hover:text-red-500 hover:border-red-500/20"
                        onClick={handleClearSession}
                      >
                        <Trash2 className="h-5 w-5 mr-1" />
                        Hapus Sesi
                      </Button>
                    </div>
                  </div>
                ) : (
                  <div className="space-y-8">
                    <StaggerItem>
                      <div className="flex flex-col sm:flex-row justify-between items-start sm:items-end gap-6">
                        <div>
                          <h2 className="text-3xl font-bold text-foreground">E-Statement</h2>
                          <p className="text-sm text-muted-foreground font-medium mt-1">
                            Kelola dan unduh laporan transaksi bulanan Anda
                          </p>
                        </div>
                      </div>
                    </StaggerItem>
                    <StatementDownloader />
                  </div>
                )}
              </StaggerItem>
            </div>
          </StaggerContainer>
        </div>
      </PageTransition>
    </DashboardLayout>
  );
}
