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
  TrendingUp
} from 'lucide-react';
import clsx from 'clsx';
import MobileNav from './MobileNav';
import LanguageSwitcher from './LanguageSwitcher';
import ThemeToggle from './ThemeToggle';
import { useTranslations, useLocale } from 'next-intl';
import { Avatar, AvatarImage, AvatarFallback } from './ui/avatar';
import { Button } from './ui/button';
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
      "flex items-center gap-4 px-6 py-4 rounded-2xl transition-all duration-300 group text-base font-bold cursor-pointer",
      active
        ? "bg-emerald-500/10 text-emerald-500 shadow-[inset_0_0_20px_rgba(16,185,129,0.05)] border border-emerald-500/20"
        : "text-foreground/40 hover:bg-foreground/5 hover:text-foreground"
    )}
    aria-label={label}
    aria-current={active ? 'page' : undefined}
  >
    <Icon className={clsx(
      "h-6 w-6 transition-colors",
      active ? "text-emerald-500" : "text-foreground/30 group-hover:text-foreground"
    )} aria-hidden="true" />
    <span className="tracking-tight uppercase text-xs sm:text-sm tracking-[0.12em]">{label}</span>
  </Link>
);

interface DashboardLayoutProps {
  children: React.ReactNode;
  username?: string;
  onLogout?: () => void;
}

export default function DashboardLayout({ children, username = 'Pengguna', onLogout }: DashboardLayoutProps) {
  const t = useTranslations('nav');
  const locale = useLocale();
  const pathname = usePathname();
  const [isSidebarOpen, setIsSidebarOpen] = useState(false);

  // Helper to localize paths
  const l = (path: string) => locale === 'id' ? path : `/${locale}${path}`;

  const mainMenu = [
    { href: l('/dashboard'), icon: LayoutDashboard, label: t('dashboard') },
    { href: l('/pockets'), icon: Wallet, label: t('accounts') },
    { href: l('/transfer'), icon: ArrowRightLeft, label: t('transfers') },
    { href: l('/qris'), icon: QrCode, label: t('qrPayment') },
    { href: l('/bills'), icon: Receipt, label: t('bills') },
    { href: l('/cards'), icon: CreditCard, label: t('cards') },
    { href: l('/investments'), icon: TrendingUp, label: t('investments') },
    { href: l('/analytics'), icon: BarChart3, label: t('analytics') },
  ];

  const otherMenu = [
    { href: l('/security'), icon: ShieldCheck, label: t('security') },
    { href: l('/settings'), icon: Settings, label: t('settings') },
    { href: l('/support'), icon: LifeBuoy, label: t('support') },
  ];

  return (
    <div className="h-screen bg-background flex overflow-hidden font-inter text-foreground">
      {/* Desktop Sidebar - Increased spacing and font weight */}
      <aside
        className="hidden lg:flex flex-col w-72 border-r border-border bg-background p-8 h-full overflow-y-auto"
        aria-label="Sidebar Navigasi Desktop"
      >
        <div className="flex items-center gap-4 mb-14 px-2 group cursor-pointer">
          <Link href={l('/')} className="flex items-center gap-4">
            <div className="h-12 w-12 bg-emerald-500 rounded-2xl flex items-center justify-center text-white font-black text-2xl shadow-lg shadow-emerald-500/20 rotate-3 transition-transform group-hover:rotate-0">
              U
            </div>
            <span className="text-3xl font-black text-foreground uppercase tracking-tighter">PayU</span>
          </Link>
        </div>

        <div className="space-y-4 mb-12">
          <p className="text-xs sm:text-sm font-black text-emerald-500/60 uppercase tracking-[0.25em] px-6 mb-4">{t('main')}</p>
          {mainMenu.map((item) => (
            <SidebarItem
              key={item.href}
              {...item}
              active={pathname === item.href || (item.href.endsWith('/dashboard') && pathname.endsWith('/dashboard'))}
            />
          ))}
        </div>

        <div className="space-y-4 mt-auto">
          <p className="text-xs sm:text-sm font-black text-emerald-500/60 uppercase tracking-[0.25em] px-6 mb-4">{t('others')}</p>
          {otherMenu.map((item) => (
            <SidebarItem
              key={item.href}
              {...item}
              active={pathname.includes(item.href)}
            />
          ))}
        </div>
      </aside>

      {/* Mobile Sidebar Overlay */}
      {isSidebarOpen && (
        <div
          className="fixed inset-0 bg-background/70 z-50 lg:hidden backdrop-blur-xl animate-in fade-in duration-500"
          onClick={() => setIsSidebarOpen(false)}
        />
      )}

      {/* Mobile Sidebar */}
      <aside className={clsx(
        "fixed inset-y-0 left-0 w-80 bg-background z-50 transform transition-transform duration-500 lg:hidden p-8 border-r border-border shadow-[20px_0_50px_rgba(0,0,0,0.2)]",
        isSidebarOpen ? "translate-x-0" : "-translate-x-full"
      )} aria-label="Sidebar Navigasi Mobile">
        <div className="flex justify-between items-center mb-14 px-2">
          <div className="flex items-center gap-4">
            <div className="h-12 w-12 bg-emerald-500 rounded-2xl flex items-center justify-center text-white font-black text-2xl shadow-lg shadow-emerald-500/20">
              U
            </div>
            <span className="text-3xl font-black text-foreground uppercase tracking-tighter">PayU</span>
          </div>
          <Button 
            variant="ghost" 
            size="icon"
            onClick={() => setIsSidebarOpen(false)} 
            className="w-12 h-12 text-foreground/40 hover:text-foreground hover:bg-foreground/5 rounded-2xl" 
            aria-label="Tutup menu navigasi"
          >
            <X className="h-8 w-8" aria-hidden="true" />
          </Button>
        </div>

        <nav className="space-y-3 overflow-y-auto max-h-[calc(100vh-180px)] scrollbar-hide">
          <p className="text-xs sm:text-sm font-black text-emerald-500/60 uppercase tracking-[0.25em] px-6 mb-4">{t('main')}</p>
          {mainMenu.map((item) => (
            <SidebarItem
              key={item.href}
              {...item}
              active={pathname === item.href || (item.href.endsWith('/dashboard') && pathname.endsWith('/dashboard'))}
            />
          ))}

          <div className="h-12" />

          <p className="text-xs sm:text-sm font-black text-emerald-500/60 uppercase tracking-[0.25em] px-6 mb-4">{t('others')}</p>
          {otherMenu.map((item) => (
            <SidebarItem
              key={item.href}
              {...item}
              active={pathname.includes(item.href)}
            />
          ))}
        </nav>
      </aside>

      {/* Main Content Area */}
      <div className="flex-1 flex flex-col min-w-0 h-screen overflow-hidden">
        <header className="h-24 border-b border-border bg-background/80 backdrop-blur-2xl sticky top-0 z-30 shrink-0">
          <div className="w-full px-4 sm:px-6 lg:px-8 h-full flex items-center justify-between">
            <div className="flex items-center gap-8">
              <Button
                variant="ghost"
                size="icon"
                onClick={() => setIsSidebarOpen(true)}
                className="lg:hidden w-12 h-12 -ml-2 text-foreground/60 hover:text-foreground hover:bg-foreground/5 rounded-2xl"
                aria-label="Buka menu navigasi"
                aria-expanded={isSidebarOpen}
              >
                <Menu className="h-7 w-7" aria-hidden="true" />
              </Button>
              <div className="hidden sm:flex flex-col justify-center">
                <PersonalizedGreeting showTimeBased={true} showSegment={true} className="leading-tight" />
                <p className="text-[10px] font-black text-muted-foreground uppercase tracking-[0.2em] mt-1.5 ml-0.5">
                  AI Financial Forecaster Active
                </p>
              </div>
            </div>

            <div className="flex items-center gap-6">
               <ThemeToggle />
               <LanguageSwitcher />

               <div className="hidden xl:flex items-center bg-foreground/[0.03] rounded-2xl px-6 py-3 w-80 gap-4 border border-border focus-within:border-emerald-500/40 focus-within:bg-foreground/[0.06] transition-all shadow-inner">
                 <Search className="h-5 w-5 text-foreground/30" />
                 <input
                   type="text"
                   placeholder="Universal search..."
                   className="bg-transparent border-none focus:ring-0 text-sm font-bold w-full placeholder:text-foreground/20 text-foreground tracking-wide"
                 />
               </div>

               <Button 
                 variant="ghost"
                 size="icon"
                 className="w-12 h-12 text-foreground/40 hover:text-foreground hover:bg-foreground/5 rounded-full relative shadow-sm border border-border" 
                 aria-label="Notifikasi"
               >
                 <div className="absolute top-4 right-4 h-2.5 w-2.5 bg-emerald-500 rounded-full border-2 border-background" aria-label="Notifikasi baru" />
                 <Bell className="h-6 w-6" aria-hidden="true" />
               </Button>

              <div className="relative group">
                <Button 
                  asChild
                  variant="ghost"
                  className="p-0 h-auto rounded-full dropdown-trigger" 
                  aria-label="Menu profil pengguna" 
                  aria-haspopup="true" 
                  aria-expanded="false"
                >
                  <Avatar className="h-12 w-12 border border-border shadow-[0_4px_20px_rgba(0,0,0,0.1)] group-hover:ring-2 ring-emerald-500 transition-all">
                    <AvatarFallback className="bg-foreground/[0.05]">
                      <User className="h-6 w-6 text-emerald-500" aria-hidden="true" />
                    </AvatarFallback>
                  </Avatar>
                </Button>

                <div className="absolute right-0 mt-5 w-72 bg-card border border-border rounded-2xl shadow-[0_30px_60px_rgba(0,0,0,0.2)] py-4 hidden group-hover:block z-50 glass animate-in fade-in slide-in-from-top-4 duration-300">
                  <div className="px-6 py-4 border-b border-border/10 mb-3">
                    <p className="text-xs font-black text-emerald-500/50 uppercase tracking-[0.25em] mb-2">Authenticated SVR</p>
                    <p className="text-lg font-black truncate text-foreground uppercase tracking-tight">{username}</p>
                  </div>
                  <Button
                    variant="ghost"
                    onClick={onLogout}
                    className="w-full text-left px-6 py-4 text-xs text-red-400 hover:bg-red-500/10 font-black uppercase tracking-[0.25em] flex items-center justify-between transition-colors h-auto"
                  >
                    <span>{t('logout')}</span>
                    <LogOut className="h-5 w-5 opacity-70" />
                  </Button>
                </div>
              </div>
            </div>
          </div>
        </header>

        <main className="flex-1 overflow-y-auto scrollbar-hide bg-background">
          <div className="w-full px-4 sm:px-6 lg:px-8 py-8 sm:py-10 lg:py-12 pb-32 transition-all duration-500">
            {children}
          </div>
        </main>
      </div>
      <MobileNav />
    </div>
  );
}
