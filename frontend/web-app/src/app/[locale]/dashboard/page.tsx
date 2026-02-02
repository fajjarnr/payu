'use client';

import Link from 'next/link';
import { ChevronRight } from 'lucide-react';
import { motion } from 'framer-motion';
import { useTranslations } from 'next-intl';
import dynamic from 'next/dynamic';
import { useLogout, useBalance } from '@/hooks';
import { useAuthStore } from '@/stores';
import DashboardLayout from '@/components/DashboardLayout';
import {
  BalanceCard,
  QuickActions,
} from '@/components/dashboard';
import { PageTransition, StaggerContainer, StaggerItem } from '@/components/ui/Motion';
import { Skeleton, SkeletonCard, SkeletonStatsGrid } from '@/components/ui/skeleton';
import { BannerCarousel, PromoPopup } from '@/components/cms';
import { SkipLink } from '@/lib/a11y';

// Lazy load below-the-fold components
const StatsCharts = dynamic(() => import('@/components/dashboard').then(mod => mod.StatsCharts), {
  loading: () => <Skeleton className="h-[300px] w-full rounded-2xl" />,
  ssr: false
});
const TransferActivity = dynamic(() => import('@/components/dashboard').then(mod => mod.TransferActivity), {
  loading: () => <Skeleton className="h-[200px] w-full rounded-2xl" />
});
const FinancialHealthScore = dynamic(() => import('@/components/dashboard').then(mod => mod.FinancialHealthScore), {
  loading: () => <Skeleton className="h-[150px] w-full rounded-2xl" />
});
const SpendingInsights = dynamic(() => import('@/components/dashboard').then(mod => mod.SpendingInsights), {
  loading: () => <Skeleton className="h-[200px] w-full rounded-2xl" />
});
const BudgetTracking = dynamic(() => import('@/components/dashboard').then(mod => mod.BudgetTracking), {
  loading: () => <Skeleton className="h-[150px] w-full rounded-2xl" />
});
const InvestmentPerformance = dynamic(() => import('@/components/dashboard').then(mod => mod.InvestmentPerformance), {
  loading: () => <Skeleton className="h-[250px] w-full rounded-2xl" />,
  ssr: false
});
const SegmentedOffers = dynamic(() => import('@/components/personalization').then(mod => mod.SegmentedOffers), {
  loading: () => <Skeleton className="h-[200px] w-full rounded-2xl" />
});

export default function Home() {
 const logout = useLogout();
 const user = useAuthStore((state) => state.user);
 const t = useTranslations();

 const handleLogout = () => {
  logout.mutate();
 };

 const username = user?.fullName || t('common.user');

 return <Dashboard username={username} handleLogout={handleLogout} />;
}

function Dashboard({ username, handleLogout }: { username: string; handleLogout: () => void }) {
 const t = useTranslations('dashboard');
 const accountId = useAuthStore((state) => state.accountId);
 const { data: balance, isLoading: balanceLoading } = useBalance(accountId || undefined);

 return (
  <DashboardLayout username={username} onLogout={handleLogout}>
   {/* Accessibility Skip Link */}
   <SkipLink href="#main-content" />

   {/* Promo Popup */}
   <PromoPopup delay={3000} />

   <main id="main-content">
    <PageTransition>
     <StaggerContainer className="grid grid-cols-1 md:grid-cols-12 lg:grid-cols-12 gap-6 md:gap-8 lg:gap-8">
      {/* Banner Carousel - Full Width */}
      <StaggerItem className="md:col-span-12 lg:col-span-12">
       <BannerCarousel autoPlayInterval={6000} />
      </StaggerItem>

      {/* Balance Card - Priority Content */}
      <StaggerItem className="md:col-span-12 lg:col-span-12">
       {balanceLoading ? <Skeleton className="h-64 rounded-2xl" /> : (
        <BalanceCard
         balance={balance?.balance || 0}
         percentage={45.2}
        />
       )}
      </StaggerItem>

      {/* High Priority Actions & Health - 4/8 Split (Aligned with Balance) */}
      <StaggerItem className="md:col-span-12 lg:col-span-4">
       <FinancialHealthScore score={78} previousScore={72} className="h-full" />
      </StaggerItem>

      <StaggerItem className="md:col-span-12 lg:col-span-8">
       <QuickActions maxActions={8} className="h-full" />
      </StaggerItem>

      {/* Activity & Insights - 4/8 Split (Consistent Sidebar) */}
      <StaggerItem className="md:col-span-12 lg:col-span-4">
       <SpendingInsights className="h-full" />
      </StaggerItem>

      <StaggerItem className="md:col-span-12 lg:col-span-8">
       <TransferActivity className="h-full" />
      </StaggerItem>

      {/* Charts & Investment - 4/8 Split (Consistent Sidebar) */}
      <StaggerItem className="md:col-span-12 lg:col-span-4">
       <InvestmentPerformance className="h-full" />
      </StaggerItem>

      <StaggerItem className="md:col-span-12 lg:col-span-8">
       <StatsCharts className="h-full" />
      </StaggerItem>

      {/* Budget & Offers - 4/8 Split */}
      <StaggerItem className="md:col-span-12 lg:col-span-4">
       <BudgetTracking className="h-full" />
      </StaggerItem>

      <StaggerItem className="md:col-span-12 lg:col-span-8">
       <SegmentedOffers maxOffers={3} />
      </StaggerItem>

      {/* Investment CTA - Full Width */}
      <StaggerItem className="md:col-span-12 lg:col-span-12">
       <div className="card-gradient rounded-2xl p-8 sm:p-8 lg:p-8 text-primary-foreground relative overflow-hidden group shadow-card border border-white/10">
        <div className="relative z-10 flex flex-col lg:flex-row items-center justify-between gap-8">
         <div className="space-y-6 text-center lg:text-left">
          <h3 className="text-3xl sm:text-4xl lg:text-5xl font-bold uppercase tracking-tight leading-none">{t('futureTitle')}</h3>
          <p className="text-base sm:text-xl font-medium opacity-90 max-w-2xl leading-relaxed">
           {t('futureDesc')}
          </p>
         </div>
         <motion.div
          whileHover={{ scale: 1.05 }}
          whileTap={{ scale: 0.95 }}
          className="shrink-0"
         >
          <Link href="/investments" className="bg-white text-emerald-600 px-10 py-5 rounded-2xl font-bold text-sm uppercase tracking-widest transition-all shadow-2xl flex items-center gap-3 hover:bg-emerald-50 focus:outline-none focus:ring-4 focus:ring-white/30">
           {t('startInvesting')} <ChevronRight className="h-6 w-6" />
          </Link>
         </motion.div>
        </div>
        <div className="absolute top-0 right-0 w-96 h-96 bg-white/10 rounded-full blur-[120px] -translate-y-1/2 translate-x-1/2 group-hover:scale-125 transition-transform duration-1000" />
        <div className="absolute bottom-0 left-0 w-64 h-64 bg-emerald-400/10 rounded-full blur-[80px] translate-y-1/2 -translate-x-1/2" />
       </div>
      </StaggerItem>
     </StaggerContainer>
    </PageTransition>
   </main>
  </DashboardLayout>
 );
}
