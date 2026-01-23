'use client';

import Link from 'next/link';
import { ChevronRight } from 'lucide-react';
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

  const handleLogout = () => {
    logout.mutate();
  };

  const username = user?.fullName || 'Pengguna';

  return <Dashboard username={username} handleLogout={handleLogout} />;
}

function Dashboard({ username, handleLogout }: { username: string; handleLogout: () => void }) {
  const accountId = useAuthStore((state) => state.accountId);
  const { data: balance, isLoading: balanceLoading } = useBalance(accountId || undefined);

  return (
    <DashboardLayout username={username} onLogout={handleLogout}>
      <PageTransition>
        <StaggerContainer className="space-y-10">
          <StaggerItem>
            {balanceLoading ? <Skeleton className="h-64 rounded-[3rem]" /> : (
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
            <div className="bg-gradient-to-r from-bank-green to-bank-emerald rounded-[1.75rem] sm:rounded-[2rem] md:rounded-[3rem] p-6 sm:p-8 md:p-12 text-white relative overflow-hidden group shadow-2xl shadow-bank-green/10">
              <div className="relative z-10 flex flex-col md:flex-row items-center justify-between gap-6 sm:gap-10">
                <div className="space-y-3 sm:space-y-4 text-center md:text-left">
                  <h3 className="text-2xl sm:text-3xl md:text-4xl font-black italic tracking-tighter">Masa Depan Finansial Anda.</h3>
                  <p className="text-xs sm:text-sm font-medium opacity-80 max-w-xl leading-relaxed">
                    Bangun portofolio investasi yang kuat langsung dari satu aplikasi. Dari reksa dana hingga emas digital, kami menyediakan instrumen terbaik untuk pertumbuhan aset Anda.
                  </p>
                </div>
                <ButtonMotion asChild>
                  <Link href="/investments" className="bg-white text-bank-green px-6 sm:px-8 md:px-10 py-4 sm:py-5 rounded-[1.5rem] sm:rounded-2xl md:rounded-3xl font-black uppercase text-xs tracking-widest hover:bg-gray-50 transition-all shadow-xl shadow-black/10 flex items-center gap-2">
                    Mulai Berinvestasi <ChevronRight className="h-4 w-4" />
                  </Link>
                </ButtonMotion>
              </div>
              <div className="absolute top-0 right-0 w-48 h-48 sm:w-64 sm:h-64 bg-white/10 rounded-full blur-3xl -z-0 translate-x-1/2 -translate-y-1/2" />
            </div>
          </StaggerItem>
        </StaggerContainer>
      </PageTransition>
    </DashboardLayout>
  );
}
