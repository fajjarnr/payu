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
import TransactionService from '@/services/TransactionService';

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

  if (!token) {
    return <LandingPage />;
  }

  return <Dashboard username={username} handleLogout={handleLogout} />;
}

function LandingPage() {
  return (
    <div className="h-screen overflow-hidden bg-white text-gray-900 font-sans selection:bg-green-100 flex flex-col">
      {/* Navbar */}
      <nav className="max-w-7xl mx-auto px-6 py-6 flex items-center justify-between">
        <div className="flex items-center gap-2">
          <div className="relative h-10 w-10 bg-green-500 rounded-xl flex items-center justify-center text-white font-bold text-xl shadow-lg shadow-green-200">
            <span className="relative top-[1px]">U</span>
            <div className="absolute top-2 right-2 h-2 w-2 bg-white rounded-full opacity-50"></div>
          </div>
          <span className="text-xl font-bold tracking-tight">PayU</span>
        </div>

        <div className="hidden md:flex items-center gap-10 text-sm font-medium text-gray-600">
          <a href="#" className="hover:text-black transition-colors">App</a>
          <a href="#" className="hover:text-black transition-colors">About</a>
          <a href="#" className="hover:text-black transition-colors">Support</a>
          <a href="#" className="hover:text-black transition-colors">Contact</a>
        </div>

        <div className="flex items-center gap-4">
          <Link href="/login" className="hidden md:block text-sm font-medium text-gray-900 hover:text-green-600 transition-colors">
            Log in
          </Link>
          <Link href="/onboarding" className="px-6 py-2.5 rounded-full border border-gray-200 text-sm font-semibold hover:border-gray-400 hover:bg-gray-50 transition-all">
            Get started
          </Link>
        </div>
      </nav>

      {/* Hero Section */}
      {/* Hero Section */}
      <main className="flex-1 max-w-7xl mx-auto px-6 grid lg:grid-cols-2 gap-12 items-center w-full">

        {/* Left Column: Text */}
        <div className="space-y-8 max-w-xl">
          <span className="inline-block text-green-500 font-bold tracking-wider text-xs uppercase">Mobile Bank</span>

          <h1 className="text-5xl md:text-6xl font-extrabold leading-[1.1] tracking-tight text-black">
            PayU: Platform Digital Banking yang Mudah, Cepat, dan Aman.
          </h1>

          <p className="text-lg text-gray-500 leading-relaxed max-w-md">
            Platform perbankan digital mandiri yang cepat, mudah, dan aman. Infrastruktur pembayaran untuk berbagai proyek Anda.
          </p>

          <div className="pt-4 flex gap-4">
            <button className="bg-black text-white px-6 py-2 rounded-lg flex items-center gap-3 hover:bg-gray-800 transition-colors shadow-xl shadow-gray-200">
              <div className="h-8 w-auto">
                <Image
                  src="/google-play-badge.svg"
                  alt="Get it on Google Play"
                  width={135}
                  height={40}
                  className="h-8 w-auto"
                />
              </div>
            </button>
            <button className="bg-black text-white px-6 py-2 rounded-lg flex items-center gap-3 hover:bg-gray-800 transition-colors shadow-xl shadow-gray-200">
              <div className="h-8 w-auto">
                <Image
                  src="/app-store-badge.svg"
                  alt="Download on the App Store"
                  width={120}
                  height={40}
                  className="h-8 w-auto"
                />
              </div>
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
  const transactionService = TransactionService;

  const { data: balance, isLoading: balanceLoading } = useQuery({
    queryKey: ['wallet-balance', accountId],
    queryFn: () => walletService.getBalance(accountId),
    enabled: !!accountId
  });

  const { data: transactions, isLoading: transactionsLoading } = useQuery({
    queryKey: ['recent-transactions', accountId],
    queryFn: () => walletService.getTransactionHistory(accountId, 0, 5),
    enabled: !!accountId
  });

  return (
    <div className="min-h-screen bg-gray-50 dark:bg-gray-900">
      <nav className="bg-white dark:bg-gray-800 shadow-sm sticky top-0 z-10">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="flex justify-between h-16">
            <div className="flex items-center">
              <div className="flex-shrink-0 flex items-center gap-2">
                <div className="relative h-10 w-10 overflow-hidden rounded-lg">
                  <Image
                    src="/logo.png"
                    alt="PayU Logo"
                    fill
                    className="object-cover"
                  />
                </div>
                <span className="text-2xl font-bold text-green-600 dark:text-green-400 tracking-tight">PayU</span>
              </div>
            </div>

            <div className="flex items-center gap-4">
              <button className="p-2 rounded-full hover:bg-gray-100 dark:hover:bg-gray-700 text-gray-600 dark:text-gray-300">
                <Bell className="h-6 w-6" />
              </button>
              <div className="relative group">
                <button className="flex items-center gap-2 text-sm font-medium text-gray-700 dark:text-gray-200 hover:text-gray-900 dark:hover:text-white">
                  <div className="h-8 w-8 bg-blue-100 dark:bg-blue-900 rounded-full flex items-center justify-center text-blue-700 dark:text-blue-300">
                    <User className="h-5 w-5" />
                  </div>
                  <span className="hidden md:block">Hi, {username}</span>
                </button>
                <div className="absolute right-0 mt-2 w-48 bg-white dark:bg-gray-800 rounded-md shadow-lg py-1 ring-1 ring-black ring-opacity-5 hidden group-hover:block">
                  <button
                    onClick={handleLogout}
                    className="w-full text-left px-4 py-2 text-sm text-gray-700 dark:text-gray-200 hover:bg-gray-100 dark:hover:bg-gray-700 flex items-center gap-2"
                  >
                    <LogOut className="h-4 w-4" /> Sign out
                  </button>
                </div>
              </div>
            </div>
          </div>
        </div>
      </nav>

      <main className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
        <div className="space-y-8">
          <div className="bg-gradient-to-r from-green-600 to-emerald-600 rounded-2xl shadow-lg p-6 text-white md:p-8">
            <div className="flex justify-between items-start mb-4">
              <div>
                <p className="text-green-100 text-sm font-medium">Total Balance</p>
                {balanceLoading ? (
                  <h2 className="text-4xl font-bold mt-1">Loading...</h2>
                ) : (
                  <h2 className="text-4xl font-bold mt-1">Rp {balance?.balance.toLocaleString() || '0'}</h2>
                )}
              </div>
              <div className="bg-white/20 p-2 rounded-lg backdrop-blur-sm">
                <Wallet className="h-6 w-6 text-white" />
              </div>
            </div>
            <div className="flex gap-4 mt-6">
              <Link href="/pockets" className="flex items-center gap-2 text-sm bg-white/20 hover:bg-white/30 px-4 py-2 rounded-full backdrop-blur-sm transition-colors">
                <PlusCircle className="h-4 w-4" /> Top Up
              </Link>
              <Link href="/pockets" className="flex items-center gap-2 text-sm bg-white/20 hover:bg-white/30 px-4 py-2 rounded-full backdrop-blur-sm transition-colors">
                <History className="h-4 w-4" /> History
              </Link>
            </div>
          </div>

          <div>
            <h3 className="text-lg font-semibold text-gray-900 dark:text-white mb-4">Quick Actions</h3>
            <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
              <QuickActionCard
                href="/transfer"
                icon={<ArrowRightLeft className="h-6 w-6 text-blue-600 dark:text-blue-400" />}
                title="Transfer"
                desc="Send money instantly"
              />
              <QuickActionCard
                href="/bills"
                icon={<Receipt className="h-6 w-6 text-orange-600 dark:text-orange-400" />}
                title="Pay Bills"
                desc="Electricity, Water, Data"
              />
              <QuickActionCard
                href="/pockets"
                icon={<Wallet className="h-6 w-6 text-purple-600 dark:text-purple-400" />}
                title="Pockets"
                desc="Manage your savings"
              />
              <div className="bg-white dark:bg-gray-800 p-6 rounded-xl shadow-sm border border-gray-100 dark:border-gray-700 flex flex-col items-center text-center justify-center cursor-pointer hover:shadow-md transition-shadow group">
                <div className="p-3 bg-gray-100 dark:bg-gray-700 rounded-full mb-3 group-hover:bg-gray-200 dark:group-hover:bg-gray-600 transition-colors">
                  <PlusCircle className="h-6 w-6 text-gray-600 dark:text-gray-300" />
                </div>
                <h4 className="font-semibold text-gray-900 dark:text-white">More</h4>
                <p className="text-xs text-gray-500 dark:text-gray-400 mt-1">View all features</p>
              </div>
            </div>
          </div>

          <div className="bg-white dark:bg-gray-800 rounded-xl shadow-sm border border-gray-100 dark:border-gray-700 overflow-hidden">
            <div className="px-6 py-4 border-b border-gray-100 dark:border-gray-700 flex justify-between items-center">
              <h3 className="text-lg font-semibold text-gray-900 dark:text-white">Recent Transactions</h3>
              <button className="text-sm text-green-600 hover:text-green-700 font-medium">View All</button>
            </div>
            {transactionsLoading ? (
              <div className="p-6 space-y-4">
                {[1, 2, 3].map((i) => (
                  <div key={i} className="flex gap-4 animate-pulse">
                    <div className="h-10 w-10 bg-gray-100 dark:bg-gray-700 rounded-full"></div>
                    <div className="flex-1 space-y-2">
                      <div className="h-4 bg-gray-100 dark:bg-gray-700 rounded w-32"></div>
                      <div className="h-3 bg-gray-100 dark:bg-gray-700 rounded w-24"></div>
                    </div>
                    <div className="h-4 bg-gray-100 dark:bg-gray-700 rounded w-20"></div>
                  </div>
                ))}
              </div>
            ) : transactions && transactions.length > 0 ? (
              <div className="divide-y divide-gray-100 dark:divide-gray-700">
                {transactions.map((tx: WalletTransaction) => (
                  <div key={tx.id} className="px-6 py-4 flex justify-between items-center hover:bg-gray-50 dark:hover:bg-gray-750 transition-colors">
                    <div className="flex items-center gap-4">
                      <div className={`h-10 w-10 rounded-full ${tx.type === 'CREDIT' ? 'bg-green-100 dark:bg-green-900/20 text-green-600' : 'bg-red-100 dark:bg-red-900/20 text-red-600'} flex items-center justify-center`}>
                        {tx.type === 'CREDIT' ? '+' : '-'}
                      </div>
                      <div>
                        <p className="font-medium text-gray-900 dark:text-white">{tx.description}</p>
                        <p className="text-xs text-gray-500">{new Date(tx.createdAt).toLocaleDateString()}</p>
                      </div>
                    </div>
                    <span className={`font-medium ${tx.type === 'CREDIT' ? 'text-green-600' : 'text-gray-900 dark:text-white'}`}>
                      {tx.type === 'CREDIT' ? '+' : '-'} Rp {tx.amount.toLocaleString()}
                    </span>
                  </div>
                ))}
              </div>
            ) : (
              <div className="p-6 text-center text-gray-500">
                No transactions yet
              </div>
            )}
          </div>
        </div>
      </main>
    </div>
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