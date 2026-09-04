'use client';

import React from 'react';
import DashboardLayout from "@/components/DashboardLayout";
import { LifeBuoy, MessageCircle, Mail, Phone, ExternalLink, HelpCircle, FileText } from 'lucide-react';
import clsx from 'clsx';
import { PageTransition, StaggerContainer, StaggerItem, ButtonMotion } from '@/components/ui/Motion';
import { Button } from '@/components/ui/button';
import { useTranslations } from 'next-intl';

export default function SupportPage() {
  const t = useTranslations('support');

  const supportChannels = [
    { label: t('liveChat'), desc: t('liveChatDesc'), icon: MessageCircle, action: t('contactUs'), color: 'primary' },
    { label: t('email'), desc: t('emailDesc'), icon: Mail, action: t('sendMessage'), color: 'blue-600' },
    { label: t('phone'), desc: t('phoneDesc'), icon: Phone, action: t('callUs'), color: 'gray-900' },
  ];

  const faqs = [
    { title: t('faqItems.identitySync'), desc: t('faqItems.identitySyncDesc'), icon: HelpCircle },
    { title: t('faqItems.transactionLimits'), desc: t('faqItems.transactionLimitsDesc'), icon: FileText },
    { title: t('faqItems.deviceToken'), desc: t('faqItems.deviceTokenDesc'), icon: HelpCircle },
    { title: t('faqItems.fraudPrevention'), desc: t('faqItems.fraudPreventionDesc'), icon: FileText },
  ];

  return (
    <DashboardLayout>
      <PageTransition>
        <div className="space-y-6 lg:space-y-8">
          {/* Header */}
          <StaggerContainer>
            <StaggerItem>
              <div className="flex flex-col sm:flex-row justify-between items-start sm:items-end gap-4 mb-6">
                <div>
                  <h2 className="text-3xl font-bold text-foreground tracking-tight">{t('title')}</h2>
                  <p className="text-sm text-muted-foreground font-medium mt-1">{t('subtitle')}</p>
                </div>
              </div>
            </StaggerItem>

            {/* Support Channels */}
            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
              {supportChannels.map((channel, i) => (
                <StaggerItem key={i}>
                  <div className="bg-card rounded-xl p-5 sm:p-6 lg:p-8 border border-border shadow-card flex flex-col items-center text-center group hover:shadow-xl transition-all duration-500">
                    <div className={clsx(
                      "h-20 w-20 mb-6 rounded-2xl flex items-center justify-center text-white shadow-lg transition-transform group-hover:scale-110",
                      channel.color === 'primary' ? "bg-primary shadow-primary/20" :
                        channel.color === 'blue-600' ? "bg-emerald-600 shadow-emerald-600/20" : "bg-foreground shadow-foreground/10"
                    )}>
                      <channel.icon className="h-10 w-10" />
                    </div>
                    <h3 className="text-lg font-bold text-foreground mb-3">{channel.label}</h3>
                    <p className="text-xs text-muted-foreground font-medium leading-relaxed mb-6 max-w-[200px]">{channel.desc}</p>
                    <Button variant="outline" className="w-full h-14 rounded-xl mt-10">
                      {channel.action}
                    </Button>
                  </div>
                </StaggerItem>
              ))}
            </div>

            {/* Knowledge Base */}
            <StaggerItem className="mt-4">
              <div className="bg-card rounded-xl p-5 sm:p-6 lg:p-8 border border-border shadow-card relative overflow-hidden">
                <div className="absolute top-0 right-0 w-80 h-80 bg-primary/5 rounded-full blur-3xl" />
                <h3 className="text-xl font-bold text-foreground mb-6 relative z-10">{t('faqs')}</h3>
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
                        <p className="text-xs text-muted-foreground font-medium leading-relaxed uppercase tracking-widest">{faq.desc}</p>
                      </div>
                    </div>
                  ))}
                </div>
              </div>
            </StaggerItem>

            {/* System Status Banner */}
            <StaggerItem className="mt-4">
              <div className="bg-gradient-to-br from-gray-900 to-gray-800 rounded-xl p-5 sm:p-6 lg:p-8 text-white relative overflow-hidden shadow-2xl group">
                <div className="relative z-10 flex flex-col lg:flex-row items-center justify-between gap-6 text-center lg:text-left">
                  <div className="space-y-6 max-w-2xl">
                    <h3 className="text-3xl font-bold">Integritas Sistem Aktif.</h3>
                    <div className="flex flex-wrap justify-center lg:justify-start gap-3">
                      {['Gateway', 'Backend', 'Database', 'Streaming'].map((svc, i) => (
                        <div key={i} className="flex items-center gap-2 bg-white/10 px-4 py-2 rounded-xl border border-white/10 shadow-sm backdrop-blur-md">
                          <span className="text-xs font-bold tracking-widest uppercase">{svc}: —</span>
                        </div>
                      ))}
                    </div>
                    <p className="text-sm text-gray-400 font-medium pt-2 leading-relaxed">Status infrastruktur belum tersedia. Hubungi tim operasional untuk informasi real-time.</p>
                  </div>
                  <ButtonMotion className="w-full lg:w-auto">
                    <Button className="h-16 px-10 shadow-2xl shadow-bank-green/20">
                      Cek Detail Infrastruktur
                    </Button>
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
