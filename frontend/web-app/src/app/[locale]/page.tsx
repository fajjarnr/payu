'use client';

import Link from 'next/link';
import { ChevronRight } from 'lucide-react';
import { motion } from 'framer-motion';
import { useTranslations } from 'next-intl';
import { useLogout, useBalance } from '@/hooks';
import { useAuthStore } from '@/stores';
import DashboardLayout from '@/components/DashboardLayout';
import BalanceCard from '@/components/dashboard/BalanceCard';
import StatsCharts from '@/components/dashboard/StatsCharts';
import TransferActivity from '@/components/dashboard/TransferActivity';
import { PageTransition, StaggerContainer, StaggerItem, ButtonMotion } from '@/components/ui/Motion';
import { Skeleton } from '@/components/ui/Skeleton';

export default function Home() {
 const logout = useLogout();
 const user = useAuthStore((state) => state.user);
 const t = useTranslations();

 const handleLogout = () => {
  logout.mutate();
 };

 const username = user?.fullName || t('common.user', 'Pengguna');

 return <Dashboard username={username} handleLogout={handleLogout} />;
}

function Dashboard({ username, handleLogout }: { username: string; handleLogout: () => void }) {
 const t = useTranslations('dashboard');
 const accountId = useAuthStore((state) => state.accountId);
 const { data: balance, isLoading: balanceLoading } = useBalance(accountId || undefined);

 return (
  <DashboardLayout username={username} onLogout={handleLogout}>
   <PageTransition>
    <StaggerContainer className="space-y-12">
     <StaggerItem>
      {balanceLoading ? <Skeleton className="h-64 rounded-xl" /> : (
       <BalanceCard
        balance={balance?.balance || 0}
        percentage={45.2}
       />
      )}
     </StaggerItem>

     <StaggerItem>
      <StatsCharts />
     </StaggerItem>

     <StaggerItem>
      <TransferActivity />
     </StaggerItem>

      <StaggerItem>
       <div className="card-gradient rounded-xl p-8 sm:p-12 text-primary-foreground relative overflow-hidden group shadow-card">
        <div className="relative z-10 flex flex-col md:flex-row items-center justify-between gap-8">
         <div className="space-y-12 text-center md:text-left">
          <h3 className="text-2xl sm:text-3xl font-black">{t('futureTitle')}</h3>
          <p className="text-sm font-medium opacity-80 max-w-xl leading-relaxed">
           {t('futureDesc')}
          </p>
         </div>
         <motion.div
          whileHover={{ scale: 1.05 }}
          whileTap={{ scale: 0.95 }}
         >
          <Link href="/investments" className="bg-white text-primary px-8 py-4 rounded-xl font-bold text-xs tracking-widest transition-all shadow-lg flex items-center gap-2">
           {t('startInvesting')} <ChevronRight className="h-4 w-4" />
          </Link>
         </motion.div>
        </div>
        <div className="absolute top-0 right-0 w-64 h-64 bg-white/10 rounded-full blur-3xl -translate-y-1/2 translate-x-1/2 group-hover:scale-110 transition-transform duration-700" />
       </div>
      </StaggerItem>
    </StaggerContainer>
   </PageTransition>
  </DashboardLayout>
 );
}
