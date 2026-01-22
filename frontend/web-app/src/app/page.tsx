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
  Smartphone
} from 'lucide-react';
import { useRouter } from 'next/navigation';
import { useEffect } from 'react';
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

  const token = typeof window !== 'undefined' ? localStorage.getItem('token') : null;
  const userStr = typeof window !== 'undefined' ? localStorage.getItem('user') : null;
  const user = userStr ? JSON.parse(userStr) : null;
  const username = user?.username || 'User';

  // TEMPORARY: Disable login check for UI testing
  /*
  if (!token) {
    return <LandingPage />;
  }
  */

  return <Dashboard username={username} handleLogout={handleLogout} />;
}

function LandingPage() {
  return (
    <div className="min-h-screen bg-background text-foreground font-sans selection:bg-bank-green/20 flex flex-col relative overflow-hidden">
      {/* Decorative Background Elements */}
      <div className="absolute top-0 left-0 w-full h-full -z-10 pointer-events-none">
        <div className="absolute top-[-10%] left-[-10%] w-[40%] h-[40%] bg-bank-green/5 rounded-full blur-[120px]" />
        <div className="absolute bottom-[-10%] right-[-10%] w-[40%] h-[40%] bg-bank-emerald/5 rounded-full blur-[120px]" />
      </div>

      {/* Navbar */}
      <nav className="max-w-7xl mx-auto px-6 lg:px-10 py-8 flex items-center justify-between w-full relative z-10">
        <div className="flex items-center gap-3">
          <div className="relative h-11 w-11 bg-gradient-to-br from-bank-green to-bank-emerald rounded-2xl flex items-center justify-center text-white font-bold text-2xl shadow-lg shadow-bank-green/20">
            U
            <div className="absolute top-2 right-2 h-2 w-2 bg-white rounded-full opacity-40 animate-pulse"></div>
          </div>
          <span className="text-2xl font-black tracking-tight text-foreground">Pay<span className="text-bank-green">U</span></span>
        </div>

        <div className="hidden md:flex items-center gap-10 text-sm font-bold text-gray-500 uppercase tracking-widest">
          <a href="#" className="hover:text-bank-green transition-colors">Platform</a>
          <a href="#" className="hover:text-bank-green transition-colors">About</a>
          <a href="#" className="hover:text-bank-green transition-colors">Support</a>
        </div>

        <div className="flex items-center gap-6">
          <Link href="/login" className="hidden md:block text-sm font-bold text-gray-700 hover:text-bank-green transition-colors uppercase tracking-widest">
            Log in
          </Link>
          <Link href="/onboarding" className="px-8 py-3 rounded-2xl bg-foreground text-background text-sm font-bold hover:bg-bank-green hover:text-white transition-all shadow-xl shadow-gray-200 active:scale-95">
            Get started
          </Link>
        </div>
      </nav>

      {/* Hero Section */}
      <main className="flex-1 max-w-7xl mx-auto px-6 lg:px-10 grid lg:grid-cols-2 gap-20 items-center w-full pb-20 relative z-10">

        {/* Left Column: Text */}
        <div className="space-y-8 max-w-xl">
          <span className="inline-block text-green-500 font-bold tracking-wider text-xs uppercase">Mobile Bank</span>

          <h1 className="text-5xl md:text-6xl font-extrabold leading-[1.1] tracking-tight text-black">
            PayU: Platform Digital Banking yang Mudah, Cepat, dan Aman.
          </h1>

          <p className="text-lg text-gray-500 leading-relaxed max-w-md">
            Platform perbankan digital mandiri yang cepat, mudah, dan aman. Infrastruktur pembayaran untuk berbagai proyek Anda.
          </p>

          <div className="flex flex-wrap gap-4 pt-4">
            <Link href="/login" className="px-10 py-4 rounded-2xl bg-bank-green text-white font-bold shadow-2xl shadow-bank-green/30 hover:bg-bank-emerald transition-all active:scale-95">
              Go to Dashboard
            </Link>
            <button className="px-10 py-4 rounded-2xl bg-white dark:bg-gray-800 border border-border text-foreground font-bold hover:bg-gray-50 transition-all active:scale-95">
              Learn More
            </button>
          </div>
        </div>

        {/* Right Column: Phones Visual */}
        <div className="relative h-[480px] md:h-[600px] w-full flex items-center justify-center">
          {/* Background Circle */}
          <div className="absolute top-1/2 left-1/2 -translate-x-1/2 -translate-y-1/2 w-[120%] h-[120%] bg-indigo-50/50 rounded-full blur-3xl -z-10"></div>

          {/* Back Phone (Wallet List) */}
          <div className="absolute top-10 left-10 md:left-20 w-[180px] h-[350px] md:w-[280px] md:h-[550px] bg-black rounded-[2rem] border-[6px] md:border-[8px] border-gray-900 shadow-2xl overflow-hidden transform -rotate-6 scale-95 opacity-90 hidden md:block">
            <div className="bg-white w-full h-full p-4 md:p-6 pt-8 md:pt-12">
              <h3 className="text-sm md:text-xl font-bold mb-4 md:mb-6">Wallet</h3>
              <div className="space-y-3 md:space-y-4">
                {[1, 2, 3].map((i) => (
                  <div key={i} className="flex items-center gap-3 md:gap-4 p-2 md:p-3 rounded-lg md:rounded-xl border border-gray-100 shadow-sm">
                    <div className={`w-8 h-8 md:w-10 md:h-10 rounded-lg ${i === 1 ? 'bg-green-500' : i === 2 ? 'bg-blue-500' : 'bg-yellow-500'}`}></div>
                    <div>
                      <div className="h-1.5 md:h-2 w-16 md:w-20 bg-gray-200 rounded mb-1"></div>
                      <div className="h-1.5 md:h-2 w-10 md:w-12 bg-gray-100 rounded"></div>
                    </div>
                  </div>
                ))}
              </div>
            </div>
          </div>

          {/* Front Phone (Main Dashboard) */}
          <div className="absolute top-0 right-4 md:right-20 w-[240px] h-[480px] md:w-[300px] md:h-[600px] bg-black rounded-[2.5rem] md:rounded-[3rem] border-[8px] md:border-[10px] border-gray-900 shadow-2xl overflow-hidden z-20">
            {/* Notch */}
            <div className="absolute top-0 left-1/2 -translate-x-1/2 h-6 w-32 bg-black rounded-b-xl z-20"></div>

            {/* Screen Content */}
            <div className="bg-gray-50 w-full h-full pt-10 px-5 flex flex-col">

              {/* App Header */}
              <div className="flex justify-between items-center mb-6">
                <div className="flex items-center gap-2">
                  <ArrowRightLeft className="h-5 w-5 text-gray-500" />
                  <span className="font-semibold text-gray-700">Cards and accounts</span>
                </div>
                <MoreVertical className="h-5 w-5 text-gray-400" />
              </div>

              {/* Card */}
              <div className="bg-gradient-to-br from-green-500 to-emerald-700 rounded-2xl p-5 text-white shadow-lg mb-6 relative overflow-hidden">
                <div className="absolute top-0 right-0 w-32 h-32 bg-white/10 rounded-full -translate-y-1/2 translate-x-1/2 blur-2xl"></div>

                <div className="flex justify-between items-start mb-8">
                  <div className="flex items-center gap-1 font-bold">
                    <span>Pay</span><span className="text-green-200">U</span>
                    <span className="text-[10px] align-top relative -top-1">+</span>
                  </div>
                  <div className="flex -space-x-1">
                    <div className="w-6 h-6 rounded-full bg-red-500/80"></div>
                    <div className="w-6 h-6 rounded-full bg-yellow-500/80"></div>
                  </div>
                </div>

                <div className="text-green-100 text-xs mb-1">Main Balance</div>
                <div className="text-2xl font-bold tracking-tight mb-4">86 353,23 P</div>
                <div className="flex justify-end text-xs text-green-200 tracking-widest">•••• 6273</div>
              </div>

              {/* Action Buttons */}
              <div className="grid grid-cols-2 gap-3 mb-8">
                <button className="bg-white py-3 rounded-xl shadow-sm border border-gray-100 font-semibold text-sm flex items-center justify-center gap-2 text-gray-700">
                  <PlusCircle className="h-4 w-4" /> Top up
                </button>
                <button className="bg-white py-3 rounded-xl shadow-sm border border-gray-100 font-semibold text-sm flex items-center justify-center gap-2 text-gray-700">
                  <Send className="h-4 w-4" /> Send
                </button>
              </div>

              {/* Transactions Sheet */}
              <div className="bg-white rounded-t-3xl flex-1 shadow-[0_-5px_20px_-5px_rgba(0,0,0,0.05)] p-5">
                <div className="w-10 h-1 bg-gray-200 rounded-full mx-auto mb-6"></div>

                <div className="flex justify-between items-center mb-6">
                  <h4 className="font-bold text-gray-800">Transactions</h4>
                  <span className="text-gray-400 text-sm">Statistics</span>
                </div>

                <div className="space-y-6">
                  <TransactionItem
                    icon={<ShoppingBag className="h-4 w-4 text-white" />}
                    color="bg-blue-600"
                    name="Metra"
                    category="Food"
                    amount="- 1 354.4 P"
                  />
                  <TransactionItem
                    icon={<User className="h-4 w-4 text-white" />}
                    color="bg-gray-800"
                    name="Anya"
                    category="Transfer"
                    amount="+ 9 900 P"
                    isPositive
                  />
                  <TransactionItem
                    icon={<Smartphone className="h-4 w-4 text-white" />}
                    color="bg-red-500"
                    name="M Video"
                    category="Electronics"
                    amount="- 14 950 P"
                  />
                </div>
              </div>

            </div>
          </div>
        </div>
      </main>
    </div>
  );
}

function TransactionItem({ icon, color, name, category, amount, isPositive }: { icon: React.ReactNode; color: string; name: string; category: string; amount: string; isPositive?: boolean }) {
  return (
    <div className="flex items-center justify-between">
      <div className="flex items-center gap-4">
        <div className={`w-10 h-10 rounded-full ${color} flex items-center justify-center shadow-md`}>
          {icon}
        </div>
        <div>
          <div className="font-bold text-gray-800 text-sm">{name}</div>
          <div className="text-xs text-gray-400">{category}</div>
        </div>
      </div>
      <div className={`font-bold text-sm ${isPositive ? 'text-green-600' : 'text-gray-800'}`}>
        {amount}
      </div>
    </div>
  );
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
      <div className="space-y-6">
        <BalanceCard
          balance={balance?.balance || 0}
          percentage={45.2}
        />
        <StatsCharts />
        <TransferActivity />
      </div>
    </DashboardLayout>
  );
}

function QuickActionCard({ href, icon, title, desc }: { href: string; icon: React.ReactNode; title: string; desc: string }) {
  return (
    <Link href={href} className="bg-white dark:bg-gray-800 p-6 rounded-xl shadow-sm border border-gray-100 dark:border-gray-700 flex flex-col items-center text-center hover:shadow-md transition-all group">
      <div className="p-3 bg-blue-50 dark:bg-blue-900/20 rounded-full mb-3 group-hover:bg-blue-100 dark:group-hover:bg-blue-900/40 transition-colors">
        {icon}
      </div>
      <h4 className="font-semibold text-gray-900 dark:text-white">{title}</h4>
      <p className="text-xs text-gray-500 dark:text-gray-400 mt-1">{desc}</p>
    </Link>
  );
}