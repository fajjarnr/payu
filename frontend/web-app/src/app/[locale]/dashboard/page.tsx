'use client';

import { Link } from '@/lib/navigation';
import { ChevronRight } from 'lucide-react';
import { motion } from 'framer-motion';
import { useTranslations } from 'next-intl';
import dynamic from 'next/dynamic';
import { useLogout, useBalance, useUserMetrics, useSpendingTrends, useCashFlow, useInvestmentAccount } from '@/hooks';
import { useAuthStore } from '@/stores';
import DashboardLayout from '@/components/DashboardLayout';
import BalanceCard from '@/components/dashboard/BalanceCard';
import QuickActions from '@/components/dashboard/QuickActions';
import { StaggerContainer, StaggerItem } from '@/components/ui/Motion';
import { Skeleton } from '@/components/ui/skeleton';
import BannerCarousel from '@/components/cms/BannerCarousel';
import PromoPopup from '@/components/cms/PromoPopup';
import { SkipLink } from '@/lib/a11y';

// Lazy load below-the-fold components
const StatsCharts = dynamic(() => import('@/components/dashboard/StatsCharts'), {
  loading: () => <Skeleton className="h-[300px] w-full rounded-2xl" />,
  ssr: false
});
const TransferActivity = dynamic(() => import('@/components/dashboard/TransferActivity'), {
  loading: () => <Skeleton className="h-[200px] w-full rounded-2xl" />
});
const FinancialHealthScore = dynamic(() => import('@/components/dashboard/FinancialHealthScore'), {
  loading: () => <Skeleton className="h-[150px] w-full rounded-2xl" />
});
const SpendingInsights = dynamic(() => import('@/components/dashboard/SpendingInsights'), {
  loading: () => <Skeleton className="h-[200px] w-full rounded-2xl" />
});
const BudgetTracking = dynamic(() => import('@/components/dashboard/BudgetTracking'), {
  loading: () => <Skeleton className="h-[150px] w-full rounded-2xl" />
});
const InvestmentPerformance = dynamic(() => import('@/components/dashboard/InvestmentPerformance'), {
  loading: () => <Skeleton className="h-[250px] w-full rounded-2xl" />,
  ssr: false
});
const SegmentedOffers = dynamic(() => import('@/components/personalization/SegmentedOffers'), {
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
 const userId = useAuthStore((state) => state.user?.id);
 const { data: balance, isLoading: balanceLoading } = useBalance(accountId || undefined);
 const { isLoading: metricsLoading } = useUserMetrics(userId);
 const { data: cashFlow } = useCashFlow(userId);
 const { isLoading: spendingLoading } = useSpendingTrends(userId);
 const { isLoading: investmentLoading } = useInvestmentAccount();

 return (
  <DashboardLayout username={username} onLogout={handleLogout}>
   {/* Accessibility Skip Link */}
   <SkipLink href="#main-content" />

   {/* Promo Popup */}
   <PromoPopup delay={3000} />

   <main id="main-content" className="overflow-x-hidden">
    <div className="space-y-4 sm:space-y-6 lg:space-y-8">
     {/* Banner Carousel - LCP Element 1 */}
     <BannerCarousel autoPlayInterval={6000} />

     {/* Balance Card - LCP Element 2 (Priority Content) */}
     <div>
      {balanceLoading ? <Skeleton className="h-64 rounded-2xl" /> : (
        <BalanceCard
         balance={balance?.balance ?? '0'}
         income={cashFlow?.income}
         expense={cashFlow?.expenses}
        />
       )}
     </div>

     <StaggerContainer className="grid grid-cols-1 lg:grid-cols-12 gap-4 sm:gap-6 lg:gap-8">
      {/* High Priority Actions & Health - 4/8 Split (Aligned with Balance) */}
      <StaggerItem className="lg:col-span-4">
        <FinancialHealthScore isLoading={metricsLoading} className="h-full" />
      </StaggerItem>

      <StaggerItem className="lg:col-span-8">
       <QuickActions maxActions={8} className="h-full" />
      </StaggerItem>

      {/* Activity & Insights - 4/8 Split (Consistent Sidebar) */}
      <StaggerItem className="lg:col-span-4">
        <SpendingInsights isLoading={spendingLoading} className="h-full" />
      </StaggerItem>

      <StaggerItem className="lg:col-span-8">
       <TransferActivity className="h-full" />
      </StaggerItem>

      {/* Charts & Investment - 4/8 Split (Consistent Sidebar) */}
      <StaggerItem className="lg:col-span-4">
        <InvestmentPerformance isLoading={investmentLoading} className="h-full" />
      </StaggerItem>

      <StaggerItem className="lg:col-span-8">
        <StatsCharts isLoading={spendingLoading} className="h-full" />
      </StaggerItem>

      {/* Budget & Offers - 4/8 Split */}
      <StaggerItem className="lg:col-span-4">
        <BudgetTracking isLoading={spendingLoading} className="h-full" />
      </StaggerItem>

      <StaggerItem className="lg:col-span-8">
       <SegmentedOffers maxOffers={3} />
      </StaggerItem>

      {/* Investment CTA - Full Width */}
      <StaggerItem className="lg:col-span-12">
       <div className="card-gradient rounded-xl sm:rounded-2xl p-5 sm:p-6 lg:p-8 text-primary-foreground relative overflow-hidden group shadow-card border border-white/10">
        <div className="relative z-10 flex flex-col lg:flex-row items-center justify-between gap-8">
         <div className="space-y-6 text-center lg:text-left">
          <h3 className="text-2xl sm:text-3xl lg:text-4xl xl:text-5xl font-bold uppercase tracking-tight leading-none">{t('futureTitle')}</h3>
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
    </div>
   </main>
  </DashboardLayout>
 );
}
