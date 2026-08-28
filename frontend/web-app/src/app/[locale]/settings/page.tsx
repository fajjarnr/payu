'use client';

import React, { useState } from 'react';
import DashboardLayout from "@/components/DashboardLayout";
import { User, Globe, Bell, Moon, Trash2, Shield, CreditCard, ChevronRight, FileText, Loader2, CheckCircle } from 'lucide-react';
import clsx from 'clsx';
import { PageTransition, StaggerContainer, StaggerItem } from '@/components/ui/Motion';
import StatementDownloader from '@/components/settings/statement-downloader';
import { Input } from '@/components/ui/input';
import { Switch } from '@/components/ui/switch';
import { Button } from '@/components/ui/button';
import { useAuth, useLogout, useUpdateUser } from '@/hooks';
import { Alert, AlertDescription } from '@/components/ui/alert';
import { useTranslations } from 'next-intl';

export default function SettingsPage() {
  const t = useTranslations('settings');
  const [activeTab, setActiveTab] = useState<'profile' | 'statements'>('profile');
  const { user } = useAuth();
  const updateUser = useUpdateUser();
  const logoutMutation = useLogout();

  // Form state
  const [formData, setFormData] = useState({
    fullName: '',
    email: '',
    phoneNumber: '',
  });

  // React 19 "adjusting state during render" pattern: when the user changes
  // (login/logout/swap), re-seed the form fields. This avoids the cascading
  // render warning that comes from setState-in-effect.
  const [prevUserId, setPrevUserId] = useState(user?.id);
  if (user?.id !== prevUserId) {
    setPrevUserId(user?.id);
    setFormData({
      fullName: user?.fullName || '',
      email: user?.email || '',
      phoneNumber: user?.phoneNumber || '',
    });
  }

  const menuItems = [
    { label: t('menu.generalProfile'), icon: User, active: activeTab === 'profile', onClick: () => setActiveTab('profile') },
    { label: t('menu.eStatement'), icon: FileText, active: activeTab === 'statements', onClick: () => setActiveTab('statements') },
    { label: t('menu.billingPlan'), icon: CreditCard, active: false },
    { label: t('menu.privacySecurity'), icon: Shield, active: false },
    { label: t('menu.advanced'), icon: Globe, active: false },
  ];

  const preferences = [
    { label: t('pref.pushNotifications'), desc: t('pref.pushNotificationsDesc'), icon: Bell, active: true },
    { label: t('pref.darkMode'), desc: t('pref.darkModeDesc'), icon: Moon, active: false },
    { label: t('pref.marketingInsights'), desc: t('pref.marketingInsightsDesc'), icon: Globe, active: true },
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
    logoutMutation.mutate();
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
                  <h2 className="text-3xl font-bold text-foreground">{t('header.title')}</h2>
                  <p className="text-sm text-muted-foreground font-medium mt-1">{t('header.subtitle')}</p>
                </div>
              </div>
            </StaggerItem>

            <div className="grid grid-cols-1 md:grid-cols-12 lg:grid-cols-12 gap-8">
              {/* Sidebar Profiles */}
              <StaggerItem className="md:col-span-6 lg:col-span-4 space-y-6">
                <div className="bg-card rounded-xl p-5 sm:p-6 lg:p-8 border border-border shadow-card flex flex-col items-center text-center relative overflow-hidden group">
                  <div className="absolute top-0 right-0 w-32 h-32 bg-primary/5 rounded-full blur-3xl" />

                  <div className="relative w-24 h-24 bg-primary rounded-2xl flex items-center justify-center text-primary-foreground font-bold text-4xl shadow-xl shadow-primary/20 mb-8 transition-transform group-hover:scale-110">
                    {formData.fullName ? formData.fullName.charAt(0).toUpperCase() : 'P'}
                  </div>
                  <h3 className="text-xl font-bold text-foreground">{formData.fullName || 'PENGGUNA PAYU'}</h3>
                  <p className="text-xs font-bold text-primary tracking-widest uppercase mt-3 bg-success-light px-4 py-1.5 rounded-full border border-primary/10">{t('premiumMember')}</p>

                  <div className="w-full h-[1px] bg-border my-10" />

                  <div className="w-full space-y-4 px-2">
                    <div className="flex items-center justify-between">
                      <span className="text-xs font-bold text-muted-foreground tracking-widest uppercase">{t('accountId')}</span>
                      <span className="text-xs font-bold text-foreground font-mono">{user?.id?.slice(0, 12) || 'PAYU-09228373'}</span>
                    </div>
                    <div className="flex items-center justify-between">
                      <span className="text-xs font-bold text-muted-foreground tracking-widest uppercase">{t('status')}</span>
                      <span className="text-xs font-bold text-primary">{t('ekycVerified')}</span>
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
                          {t('profileUpdateSuccess')}
                        </AlertDescription>
                      </Alert>
                    )}

                    {/* Error Alert */}
                    {updateUser.isError && (
                      <Alert className="bg-red-500/10 border-red-500/20 relative z-10">
                         <AlertDescription className="text-red-500">
                          {t('profileUpdateError')}
                        </AlertDescription>
                      </Alert>
                    )}

                    {/* Personal Details */}
                    <section className="space-y-12 relative z-10">
                      <div className="flex items-center gap-4">
                        <div className="h-12 w-12 bg-primary/10 rounded-xl flex items-center justify-center border border-primary/10">
                          <User className="h-6 w-6 text-primary" />
                        </div>
                        <h3 className="text-xl font-bold text-foreground">{t('profileCredentials')}</h3>
                      </div>

                      <div className="grid grid-cols-1 md:grid-cols-2 gap-8">
                        <div className="space-y-3">
                          <label className="text-xs font-bold text-muted-foreground tracking-widest uppercase ml-1">
                            {t('form.fullName')}
                          </label>
                          <Input
                            type="text"
                            value={formData.fullName}
                            onChange={(e) => handleInputChange('fullName', e.target.value)}
                            placeholder={t('form.fullNamePlaceholder')}
                            className="font-bold"
                            disabled={updateUser.isPending}
                          />
                        </div>
                        <div className="space-y-3">
                          <label className="text-xs font-bold text-muted-foreground tracking-widest uppercase ml-1">
                            {t('form.contactEmail')}
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
                            {t('form.phone')}
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
                      </div>
                    </section>

                    <div className="h-[1px] w-full bg-border" />

                    {/* Preferences */}
                    <section className="space-y-12 relative z-10">
                      <div className="flex items-center gap-4">
                        <div className="h-12 w-12 bg-primary/10 rounded-xl flex items-center justify-center border border-primary/10">
                          <Bell className="h-6 w-6 text-primary" />
                        </div>
                        <h3 className="text-xl font-bold text-foreground">{t('systemPreferences')}</h3>
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
                            {t('saving')}
                          </>
                        ) : (
                          t('syncProfile')
                        )}
                      </Button>
                      <Button
                        variant="outline"
                        className="text-red-500 hover:bg-red-500/10 hover:text-red-500 hover:border-red-500/20"
                        onClick={handleClearSession}
                      >
                        <Trash2 className="h-5 w-5 mr-1" />
                        {t('clearSession')}
                      </Button>
                    </div>
                  </div>
                ) : (
                  <div className="space-y-8">
                    <StaggerItem>
                      <div className="flex flex-col sm:flex-row justify-between items-start sm:items-end gap-6">
                        <div>
                          <h2 className="text-3xl font-bold text-foreground">{t('menu.eStatement')}</h2>
                          <p className="text-sm text-muted-foreground font-medium mt-1">
                            {t('eStatementSubtitle')}
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
