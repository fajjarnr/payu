'use client';

import Image from 'next/image';
import Link from 'next/link';
import {
  ArrowRightLeft,
  Receipt,
  Wallet,
  PlusCircle,
  History,
  LogOut,
  User,
  Bell,
  MoreVertical,
  Send,
  ShoppingBag,
  Smartphone,
  ChevronRight
} from 'lucide-react';
import { useRouter } from 'next/navigation';
import { useEffect, useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { WalletTransaction } from '@/types';
import WalletService from '@/services/WalletService';
import DashboardLayout from '@/components/DashboardLayout';
import BalanceCard from '@/components/dashboard/BalanceCard';
import StatsCharts from '@/components/dashboard/StatsCharts';
import TransferActivity from '@/components/dashboard/TransferActivity';

export default function Home() {
  const router = useRouter();

  const handleLogout = () => {
    localStorage.removeItem('token');
    localStorage.removeItem('refreshToken');
    localStorage.removeItem('user');
    localStorage.removeItem('accountId');
    router.push('/login');
  };

  const userStr = typeof window !== 'undefined' ? localStorage.getItem('user') : null;
  const user = userStr ? JSON.parse(userStr) : null;
  const username = user?.fullName || 'Pengguna';

  return <Dashboard username={username} handleLogout={handleLogout} />;
}

function Dashboard({ username, handleLogout }: { username: string; handleLogout: () => void }) {
  const accountId = typeof window !== 'undefined' ? localStorage.getItem('accountId') || '' : '';
  const walletService = WalletService;

  const { data: balance, isLoading: balanceLoading } = useQuery({
    queryKey: ['wallet-balance', accountId],
    queryFn: () => walletService.getBalance(accountId),
    enabled: !!accountId
  });

  return (
    <DashboardLayout username={username} onLogout={handleLogout}>
      <div className="space-y-10 animate-in fade-in slide-in-from-bottom-4 duration-700">
        <BalanceCard
          balance={balance?.balance || 0}
          percentage={45.2}
        />
        <StatsCharts />
        <TransferActivity />

        {/* Banner Tambahan */}
        <div className="bg-gradient-to-r from-bank-green to-bank-emerald rounded-[3rem] p-12 text-white relative overflow-hidden group shadow-2xl shadow-bank-green/10">
          <div className="relative z-10 flex flex-col md:flex-row items-center justify-between gap-10">
            <div className="space-y-4">
              <h3 className="text-4xl font-black italic tracking-tighter">Masa Depan Finansial Anda.</h3>
              <p className="text-sm font-medium opacity-80 max-w-xl leading-relaxed">
                Bangun portofolio investasi yang kuat langsung dari satu aplikasi. Dari reksa dana hingga emas digital, kami menyediakan instrumen terbaik untuk pertumbuhan aset Anda.
              </p>
            </div>
            <Link href="/investments" className="bg-white text-bank-green px-10 py-5 rounded-3xl font-black uppercase text-xs tracking-widest hover:bg-gray-50 transition-all active:scale-95 shadow-xl shadow-black/10 flex items-center gap-2">
              Mulai Berinvestasi <ChevronRight className="h-4 w-4" />
            </Link>
          </div>
          <div className="absolute top-0 right-0 w-64 h-64 bg-white/10 rounded-full blur-3xl -z-0 translate-x-1/2 -translate-y-1/2" />
        </div>
      </div>
    </DashboardLayout>
  );
}