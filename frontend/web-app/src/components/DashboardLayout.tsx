'use client';

import React, { useState } from 'react';
import Link from 'next/link';
import { usePathname } from 'next/navigation';
import {
  LayoutDashboard,
  BarChart3,
  ArrowRightLeft,
  Wallet,
  CreditCard,
  Gift,
  ShieldCheck,
  Settings,
  LifeBuoy,
  Bell,
  Search,
  Menu,
  X,
  User,
  LogOut,
  QrCode,
  Receipt,
  TrendingUp,
  Briefcase
} from 'lucide-react';
import clsx from 'clsx';
import MobileNav from './MobileNav';
import LanguageSwitcher from './LanguageSwitcher';
import { PersonalizedGreeting } from './personalization';

interface SidebarItemProps {
  href: string;
  icon: React.ElementType;
  label: string;
  active?: boolean;
}

const SidebarItem = ({ href, icon: Icon, label, active }: SidebarItemProps) => (
  <Link
    href={href}
    className={clsx(
      "flex items-center gap-3 px-4 py-3 rounded-xl transition-all duration-200 group text-sm font-medium",
      active
        ? "bg-accent text-accent-foreground shadow-sm"
        : "text-muted-foreground hover:bg-muted/50 hover:text-foreground"
    )}
    aria-label={label}
    aria-current={active ? 'page' : undefined}
  >
    <Icon className={clsx(
      "h-5 w-5 transition-colors",
      active ? "text-primary" : "text-muted-foreground group-hover:text-foreground"
    )} aria-hidden="true" />
    <span className="tracking-tight">{label}</span>
  </Link>
);

interface DashboardLayoutProps {
  children: React.ReactNode;
  username?: string;
  onLogout?: () => void;
}

export default function DashboardLayout({ children, username = 'Pengguna', onLogout }: DashboardLayoutProps) {
  const pathname = usePathname();
  const [isSidebarOpen, setIsSidebarOpen] = useState(false);

  const mainMenu = [
    { href: '/', icon: LayoutDashboard, label: 'Beranda' },
    { href: '/pockets', icon: Wallet, label: 'Kantong' },
    { href: '/transfer', icon: ArrowRightLeft, label: 'Transfer' },
    { href: '/qris', icon: QrCode, label: 'Pembayaran QRIS' },
    { href: '/bills', icon: Receipt, label: 'Tagihan & Top-up' },
    { href: '/cards', icon: CreditCard, label: 'Kartu Virtual' },
    { href: '/investments', icon: TrendingUp, label: 'Investasi' },
    { href: '/analytics', icon: BarChart3, label: 'Analitik Keuangan' },
  ];

  const otherMenu = [
    { href: '/security', icon: ShieldCheck, label: 'Keamanan & MFA' },
    { href: '/settings', icon: Settings, label: 'Pengaturan Akun' },
    { href: '/support', icon: LifeBuoy, label: 'Bantuan & Support' },
  ];

  return (
    <div className="h-screen bg-background flex overflow-hidden font-sans">
      {/* Desktop Sidebar - Width 56 as per layout structure */}
      <aside className="hidden lg:flex flex-col w-56 border-r border-border bg-card p-6 h-full overflow-y-auto">
        <div className="flex items-center gap-2 mb-10 px-2 group">
          <div className="h-10 w-10 bg-primary rounded-xl flex items-center justify-center text-primary-foreground font-black text-xl shadow-lg shadow-primary/20 rotate-3 transition-transform group-hover:rotate-0">
            U
          </div>
          <span className="text-2xl font-black text-primary">PayU</span>
        </div>

        <div className="space-y-1.5 mb-8">
          <p className="text-[10px] font-semibold text-muted-foreground tracking-widest px-4 mb-2">Menu Utama</p>
          {mainMenu.map((item) => (
            <SidebarItem
              key={item.href}
              {...item}
              active={pathname === item.href}
            />
          ))}
        </div>

        <div className="space-y-1.5 mt-auto">
          <p className="text-[10px] font-semibold text-muted-foreground tracking-widest px-4 mb-2">Lainnya</p>
          {otherMenu.map((item) => (
            <SidebarItem
              key={item.href}
              {...item}
              active={pathname === item.href}
            />
          ))}
        </div>
      </aside>

      {/* Mobile Sidebar Overlay */}
      {isSidebarOpen && (
        <div
          className="fixed inset-0 bg-foreground/20 z-50 lg:hidden backdrop-blur-sm animate-in fade-in duration-200"
          onClick={() => setIsSidebarOpen(false)}
        />
      )}

      {/* Mobile Sidebar */}
      <aside className={clsx(
        "fixed inset-y-0 left-0 w-72 bg-card z-50 transform transition-transform duration-300 lg:hidden p-6 shadow-2xl",
        isSidebarOpen ? "translate-x-0" : "-translate-x-full"
      )}>
        <div className="flex justify-between items-center mb-10 px-2">
          <div className="flex items-center gap-2">
            <div className="h-10 w-10 bg-primary rounded-xl flex items-center justify-center text-primary-foreground font-black text-xl">
              U
            </div>
            <span className="text-2xl font-black text-primary">PayU</span>
          </div>
          <button onClick={() => setIsSidebarOpen(false)} className="p-2 text-muted-foreground hover:bg-muted rounded-lg transition-colors" aria-label="Tutup menu navigasi">
            <X className="h-6 w-6" aria-hidden="true" />
          </button>
        </div>

        <nav className="space-y-1.5 overflow-y-auto max-h-[calc(100vh-140px)]">
          <p className="text-[10px] font-semibold text-muted-foreground tracking-widest px-4 mb-2">Menu Utama</p>
          {mainMenu.map((item) => (
            <SidebarItem
              key={item.href}
              {...item}
              active={pathname === item.href}
            />
          ))}

          <div className="h-8" />

          <p className="text-[10px] font-semibold text-muted-foreground tracking-widest px-4 mb-2">Lainnya</p>
          {otherMenu.map((item) => (
            <SidebarItem
              key={item.href}
              {...item}
              active={pathname === item.href}
            />
          ))}
        </nav>
      </aside>

      {/* Main Content Area */}
      <div className="flex-1 flex flex-col min-w-0 h-screen overflow-hidden">
        <header className="h-20 border-b border-border bg-card/80 backdrop-blur-md sticky top-0 z-30 shrink-0">
          <div className="max-w-[1600px] mx-auto px-6 sm:px-10 md:px-12 h-full flex items-center justify-between">
            <div className="flex items-center gap-4">
              <button
                onClick={() => setIsSidebarOpen(true)}
                className="lg:hidden p-2 -ml-2 text-muted-foreground hover:bg-muted rounded-lg transition-colors"
                aria-label="Buka menu navigasi"
                aria-expanded={isSidebarOpen}
              >
                <Menu className="h-6 w-6" aria-hidden="true" />
              </button>
              <div className="hidden sm:block">
                <PersonalizedGreeting showTimeBased={true} showSegment={true} />
                <p className="text-[10px] text-muted-foreground font-bold tracking-widest">Kelola finansial Anda dengan wawasan real-time</p>
              </div>
            </div>

            <div className="flex items-center gap-3 sm:gap-4">
               <LanguageSwitcher />

               <div className="hidden md:flex items-center bg-muted/50 rounded-xl px-4 py-2.5 w-64 gap-3 border border-transparent focus-within:border-primary/30 focus-within:bg-card focus-within:shadow-sm transition-all">
                 <Search className="h-4 w-4 text-muted-foreground" />
                 <input
                   type="text"
                   placeholder="Cari apapun..."
                   className="bg-transparent border-none focus:ring-0 text-xs font-semibold w-full placeholder:text-muted-foreground/60 text-foreground tracking-widest"
                 />
               </div>

               <button className="p-3 text-muted-foreground hover:bg-muted rounded-full relative transition-colors" aria-label="Notifikasi">
                 <div className="absolute top-3.5 right-3.5 h-2 w-2 bg-destructive rounded-full border-2 border-card" aria-label="Notifikasi baru" />
                 <Bell className="h-5 w-5" aria-hidden="true" />
               </button>

              <div className="relative group">
                <button className="h-10 w-10 bg-accent rounded-full flex items-center justify-center border-2 border-card shadow-sm overflow-hidden group-hover:ring-2 ring-primary transition-all" aria-label="Menu profil pengguna" aria-haspopup="true" aria-expanded="false">
                  <User className="h-5 w-5 text-primary" aria-hidden="true" />
                </button>

                <div className="absolute right-0 mt-3 w-52 bg-card rounded-xl shadow-xl py-2 border border-border hidden group-hover:block z-50 overflow-hidden animate-in fade-in slide-in-from-top-2 duration-200">
                  <div className="px-4 py-2 border-b border-border mb-1">
                    <p className="text-[10px] font-semibold text-muted-foreground tracking-widest">Akun</p>
                    <p className="text-sm font-bold truncate text-foreground">{username}</p>
                  </div>
                  <button
                    onClick={onLogout}
                    className="w-full text-left px-4 py-3 text-[10px] text-destructive hover:bg-destructive/10 font-black tracking-widest flex items-center gap-2 transition-colors"
                  >
                    <LogOut className="h-4 w-4" /> Keluar Sesi
                  </button>
                </div>
              </div>
            </div>
          </div>
        </header>

        <main className="flex-1 overflow-y-auto animate-fade-in scrollbar-hide">
          <div className="max-w-[1600px] mx-auto px-6 sm:px-10 md:px-12 py-8 sm:py-10 md:py-12 pb-40 lg:pb-16 transition-all duration-300">
            {children}
          </div>
        </main>
      </div>
      <MobileNav />
    </div>
  );
}
