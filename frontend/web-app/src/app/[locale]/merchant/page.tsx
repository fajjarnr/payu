'use client';

import { useEffect, useState } from 'react';
import { PartnerService, Partner } from '@/services/PartnerService';
import { Link } from '@/lib/navigation';
import { useTranslations } from 'next-intl';
import DashboardLayout from "@/components/DashboardLayout";
import { Input, Tag } from 'antd';
import { Building2, Key, ShieldCheck, Loader2 } from '@/components/icons';
import { useAuthStore } from '@/stores/authStore';

export default function MerchantDashboard() {
 const t = useTranslations('merchant');
 const { user: _user } = useAuthStore();
 const [partner, setPartner] = useState<Partner | null>(null);
 const [loading, setLoading] = useState(true);

  useEffect(() => {
   const fetchPartner = async () => {
    try {
     // ponytail: email-based /me lookup; upgrade to owner_user_id if multi-tenant per user
     const data = await PartnerService.getMyPartner();
     setPartner(data);
    } catch (error) {
     console.error('Failed to fetch partner', error);
    } finally {
     setLoading(false);
    }
   };

   fetchPartner();
  }, []);

 if (loading) {
  return (
   <DashboardLayout>
    <div className="flex items-center justify-center min-h-[320px]">
     <Loader2 className="h-8 w-8 animate-spin text-primary" />
    </div>
   </DashboardLayout>
  );
 }

 if (!partner) {
  return (
   <DashboardLayout>
     <div className="flex flex-col items-center justify-center min-h-[320px] space-y-6">
      <Building2 className="h-16 w-16 text-muted-foreground/50" />
      <h1 className="text-2xl font-bold">{t('title')}</h1>
      <p className="text-muted-foreground">{t('notRegistered')}</p>
      <Link href="/merchant/register" className="inline-flex items-center justify-center gap-2 whitespace-nowrap rounded-xl bg-primary px-6 py-2 h-12 text-xs font-bold uppercase tracking-[0.15em] text-primary-foreground shadow-lg shadow-primary/20 transition-all hover:bg-primary/90 active:scale-95">{t('register')}</Link>
     </div>
   </DashboardLayout>
  );
 }

 return (
  <DashboardLayout>
   <div className="space-y-8">
       <div className="flex flex-col sm:flex-row justify-between items-start sm:items-end gap-6 mb-8">
        <div>
         <h1 className="text-2xl font-bold text-foreground tracking-tight">{t('dashboard')}</h1>
         <p className="text-sm text-muted-foreground font-medium mt-1">{t('subtitle')}</p>
        </div>
       </div>

     <div>
       <div className="bg-card rounded-xl p-5 sm:p-6 lg:p-8 border border-border shadow-sm mb-6">
        <div className="flex items-center gap-3 mb-6">
         <ShieldCheck className="h-5 w-5 text-primary" />
         <h3 className="text-lg font-bold">{t('profile')}</h3>
        </div>
        <div className="grid grid-cols-1 sm:grid-cols-2 gap-6">
         <div>
          <p className="text-xs font-bold text-muted-foreground tracking-widest uppercase mb-1">{t('merchantName')}</p>
          <p className="font-medium">{partner.name}</p>
         </div>
         <div>
          <p className="text-xs font-bold text-muted-foreground tracking-widest uppercase mb-1">{t('email')}</p>
          <p className="font-medium">{partner.email}</p>
         </div>
         <div>
          <p className="text-xs font-bold text-muted-foreground tracking-widest uppercase mb-1">{t('type')}</p>
          <p className="font-medium">{partner.type}</p>
         </div>
         <div>
          <p className="text-xs font-bold text-muted-foreground tracking-widest uppercase mb-1">{t('status')}</p>
          <Tag bordered={false} color={partner.active ? 'green' : 'red'} className={partner.active ? 'bg-primary/10 text-primary border-primary/20' : ''}>
           {partner.active ? t('active') : t('inactive')}
          </Tag>
         </div>
        </div>
       </div>
     </div>

     <div>
       <div className="bg-card rounded-xl p-5 sm:p-6 lg:p-8 border border-border shadow-sm">
        <div className="flex items-center gap-3 mb-6">
         <Key className="h-5 w-5 text-primary" />
         <h3 className="text-lg font-bold">{t('apiCredentials')}</h3>
        </div>
        <div className="space-y-4">
         <div>
          <p className="text-xs font-bold text-muted-foreground tracking-widest uppercase mb-1">{t('clientId')}</p>
          <code className="block bg-muted/30 p-3 rounded-lg mt-1 text-sm">{partner.clientId || 'N/A'}</code>
         </div>
         <div>
          <p className="text-xs font-bold text-muted-foreground tracking-widest uppercase mb-1">{t('publicKey')}</p>
          <Input.TextArea
            aria-label="Public key"
            readOnly
            className="w-full bg-muted/30 p-3 rounded-lg mt-1 h-24 font-mono text-sm resize-none border-0"
            value={partner.publicKey || t('noPublicKey')}
          />
         </div>
        </div>
       </div>
     </div>
   </div>
  </DashboardLayout>
 );
}
