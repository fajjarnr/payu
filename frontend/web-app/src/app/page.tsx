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
  Bell
} from 'lucide-react';
import { useRouter } from 'next/navigation';
import { useEffect, useState } from 'react';

export default function Home() {
  const router = useRouter();
  const [isAuthenticated, setIsAuthenticated] = useState(false);
  const [username, setUsername] = useState('');

  useEffect(() => {
    const token = localStorage.getItem('token');
    // Simple check - in a real app we'd validate the token or decode it
    if (token) {
      setIsAuthenticated(true);
      // Mock username extraction or fetch
      setUsername('User'); 
    }
  }, []);

  const handleLogout = () => {
    localStorage.removeItem('token');
    setIsAuthenticated(false);
    router.push('/login');
  };

  return (
    <div className="min-h-screen bg-gray-50 dark:bg-gray-900">
      {/* Header / Navbar */}
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
              {isAuthenticated ? (
                <>
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
                    {/* Dropdown */}
                    <div className="absolute right-0 mt-2 w-48 bg-white dark:bg-gray-800 rounded-md shadow-lg py-1 ring-1 ring-black ring-opacity-5 hidden group-hover:block">
                      <button 
                        onClick={handleLogout}
                        className="w-full text-left px-4 py-2 text-sm text-gray-700 dark:text-gray-200 hover:bg-gray-100 dark:hover:bg-gray-700 flex items-center gap-2"
                      >
                        <LogOut className="h-4 w-4" /> Sign out
                      </button>
                    </div>
                  </div>
                </>
              ) : (
                <div className="flex gap-2">
                  <Link href="/login" className="px-4 py-2 text-sm font-medium text-gray-700 dark:text-gray-200 hover:bg-gray-100 dark:hover:bg-gray-800 rounded-md">
                    Log in
                  </Link>
                  <Link href="/onboarding" className="px-4 py-2 text-sm font-medium text-white bg-green-600 hover:bg-green-700 rounded-md shadow-sm">
                    Sign up
                  </Link>
                </div>
              )}
            </div>
          </div>
        </div>
      </nav>

      {/* Main Content */}
      <main className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
        
        {/* Welcome Section (for authenticated users) */}
        {isAuthenticated ? (
          <div className="space-y-8">
            {/* Balance Card */}
            <div className="bg-gradient-to-r from-green-600 to-emerald-600 rounded-2xl shadow-lg p-6 text-white md:p-8">
              <div className="flex justify-between items-start mb-4">
                <div>
                  <p className="text-green-100 text-sm font-medium">Total Balance</p>
                  <h2 className="text-4xl font-bold mt-1">Rp 12.500.000</h2>
                </div>
                <div className="bg-white/20 p-2 rounded-lg backdrop-blur-sm">
                  <Wallet className="h-6 w-6 text-white" />
                </div>
              </div>
              <div className="flex gap-4 mt-6">
                <Link href="/pockets" className="flex items-center gap-2 text-sm bg-white/20 hover:bg-white/30 px-4 py-2 rounded-full backdrop-blur-sm transition-colors">
                   <PlusCircle className="h-4 w-4" /> Top Up
                </Link>
                <Link href="/history" className="flex items-center gap-2 text-sm bg-white/20 hover:bg-white/30 px-4 py-2 rounded-full backdrop-blur-sm transition-colors">
                   <History className="h-4 w-4" /> History
                </Link>
              </div>
            </div>

            {/* Quick Actions Grid */}
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

            {/* Recent Transactions (Placeholder) */}
            <div className="bg-white dark:bg-gray-800 rounded-xl shadow-sm border border-gray-100 dark:border-gray-700 overflow-hidden">
               <div className="px-6 py-4 border-b border-gray-100 dark:border-gray-700 flex justify-between items-center">
                 <h3 className="text-lg font-semibold text-gray-900 dark:text-white">Recent Transactions</h3>
                 <button className="text-sm text-green-600 hover:text-green-700 font-medium">View All</button>
               </div>
               <div className="divide-y divide-gray-100 dark:divide-gray-700">
                 {[1, 2, 3].map((i) => (
                   <div key={i} className="px-6 py-4 flex justify-between items-center hover:bg-gray-50 dark:hover:bg-gray-750 transition-colors">
                      <div className="flex items-center gap-4">
                        <div className="h-10 w-10 rounded-full bg-gray-100 dark:bg-gray-700 flex items-center justify-center">
                          <ArrowRightLeft className="h-5 w-5 text-gray-500" />
                        </div>
                        <div>
                          <p className="font-medium text-gray-900 dark:text-white">Transfer to John Doe</p>
                          <p className="text-xs text-gray-500">Today, 10:30 AM</p>
                        </div>
                      </div>
                      <span className="font-medium text-gray-900 dark:text-white">- Rp 150.000</span>
                   </div>
                 ))}
               </div>
            </div>
          </div>
        ) : (
          /* Landing Page Content (Unauthenticated) */
          <div className="text-center py-20">
             <div className="relative h-24 w-24 mx-auto mb-6">
                 <Image src="/logo.png" alt="PayU Logo" fill className="object-contain" />
             </div>
             <h1 className="text-4xl md:text-6xl font-extrabold text-gray-900 dark:text-white mb-6">
               Banking for the <span className="text-green-600">Digital Generation</span>
             </h1>
             <p className="text-xl text-gray-600 dark:text-gray-300 mb-10 max-w-2xl mx-auto">
               Experience seamless transfers, smart savings pockets, and instant bill payments. 
               All in one secure app.
             </p>
             <div className="flex justify-center gap-4">
                <Link href="/onboarding" className="px-8 py-3 bg-green-600 text-white rounded-lg font-semibold hover:bg-green-700 transition-all shadow-lg hover:shadow-green-500/30">
                   Open Account
                </Link>
                <Link href="/login" className="px-8 py-3 bg-white dark:bg-gray-800 text-gray-900 dark:text-white border border-gray-200 dark:border-gray-700 rounded-lg font-semibold hover:bg-gray-50 dark:hover:bg-gray-700 transition-all">
                   Login
                </Link>
             </div>
          </div>
        )}
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
