'use client';

import { useEffect, useState } from 'react';
import { PartnerService, Partner } from '@/services/PartnerService';
import { Link } from '@/lib/navigation';
import { useTranslations } from 'next-intl';
import DashboardLayout from "@/components/DashboardLayout";
import { PageTransition, StaggerContainer, StaggerItem } from '@/components/ui/Motion';
import { Button } from '@/components/ui/button';
import { Building2, Key, ShieldCheck, Loader2 } from 'lucide-react';
import { Badge } from '@/components/ui/badge';
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
    <div className="flex items-center justify-center min-h-[400px]">
     <Loader2 className="h-8 w-8 animate-spin text-primary" />
    </div>
   </DashboardLayout>
  );
 }

 if (!partner) {
  return (
   <DashboardLayout>
    <PageTransition>
     <div className="flex flex-col items-center justify-center min-h-[400px] space-y-6">
      <Building2 className="h-16 w-16 text-muted-foreground/50" />
      <h1 className="text-2xl font-bold">{t('title')}</h1>
      <p className="text-muted-foreground">{t('notRegistered')}</p>
      <Button asChild>
       <Link href="/merchant/register">{t('register')}</Link>
      </Button>
     </div>
    </PageTransition>
   </DashboardLayout>
  );
 }

 return (
  <DashboardLayout>
   <PageTransition>
    <div className="space-y-8">
     <StaggerContainer>
      <StaggerItem>
       <div className="flex flex-col sm:flex-row justify-between items-start sm:items-end gap-6 mb-8">
        <div>
         <h2 className="text-3xl font-bold text-foreground">{t('dashboard')}</h2>
         <p className="text-sm text-muted-foreground font-medium mt-1">{t('subtitle')}</p>
        </div>
       </div>
      </StaggerItem>

      <StaggerItem>
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
          <Badge variant={partner.active ? 'default' : 'destructive'} className={partner.active ? 'bg-emerald-500/10 text-emerald-500 border-emerald-500/20' : ''}>
           {partner.active ? t('active') : t('inactive')}
          </Badge>
         </div>
        </div>
       </div>
      </StaggerItem>

      <StaggerItem>
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
          <textarea
           readOnly
           className="w-full bg-muted/30 p-3 rounded-lg mt-1 h-24 font-mono text-sm resize-none border-0"
           value={partner.publicKey || t('noPublicKey')}
          />
         </div>
        </div>
       </div>
      </StaggerItem>
     </StaggerContainer>
    </div>
   </PageTransition>
  </DashboardLayout>
 );
}
