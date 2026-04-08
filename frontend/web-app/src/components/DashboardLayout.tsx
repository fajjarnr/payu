'use client';

import React, { useState } from 'react';
import { Link } from '@/lib/navigation';
import { usePathname, useRouter } from '@/lib/navigation';
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
  TrendingUp,
  Calendar,
  History
} from 'lucide-react';
import clsx from 'clsx';
import MobileNav from './MobileNav';
import LanguageSwitcher from './LanguageSwitcher';
import ThemeToggle from './ThemeToggle';
import { useTranslations, useLocale } from 'next-intl';
import { Avatar, AvatarImage, AvatarFallback } from './ui/avatar';
import { Button } from './ui/button';
import { Input } from './ui/input';
import { PersonalizedGreeting } from './personalization';
import { useLogout } from '@/hooks';
import {
  Sheet,
  SheetContent,
  SheetHeader,
  SheetTitle,
  SheetTrigger,
} from "@/components/ui/sheet";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";

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
        ? "bg-primary/10 text-primary shadow-[inset_0_0_20px_rgba(16,185,129,0.05)] border border-primary/20"
        : "text-foreground/40 hover:bg-foreground/5 hover:text-foreground"
    )}
    aria-label={label}
    aria-current={active ? 'page' : undefined}
  >
    <Icon className={clsx(
      "h-6 w-6 transition-colors",
      active ? "text-primary" : "text-foreground/30 group-hover:text-foreground"
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
  const router = useRouter();
  const logoutMutation = useLogout();
  const [isSidebarOpen, setIsSidebarOpen] = useState(false);

  const mainMenu = [
    { href: '/dashboard', icon: LayoutDashboard, label: t('dashboard') },
    { href: '/pockets', icon: Wallet, label: t('accounts') },
    { href: '/transfer', icon: ArrowRightLeft, label: t('transfers') },
    { href: '/transactions', icon: History, label: t('history') || 'Riwayat' },
    { href: '/scheduled-transfers', icon: Calendar, label: t('scheduled') || 'Terjadwal' },
    { href: '/exchange', icon: TrendingUp, label: t('exchange') },
    { href: '/qris', icon: QrCode, label: t('qrPayment') },
    { href: '/bills', icon: Receipt, label: t('bills') },
    { href: '/cards', icon: CreditCard, label: t('cards') },
    { href: '/investments', icon: TrendingUp, label: t('investments') },
    { href: '/analytics', icon: BarChart3, label: t('analytics') },
  ];

  const otherMenu = [
    { href: '/security', icon: ShieldCheck, label: t('security') },
    { href: '/settings', icon: Settings, label: t('settings') },
    { href: '/support', icon: LifeBuoy, label: t('support') },
  ];

  return (
    <div className="h-screen bg-background flex overflow-hidden font-inter text-foreground">
      {/* Desktop Sidebar - Increased spacing and font weight */}
      <aside
        className="hidden lg:flex flex-col w-[320px] 2xl:w-[380px] border-r border-border bg-background p-8 2xl:p-8 h-screen overflow-y-auto shrink-0 sticky top-0"
        aria-label="Sidebar Navigasi Desktop"
      >
        <div className="flex items-center gap-5 mb-16 px-2 group cursor-pointer">
          <Link href="/" className="flex items-center gap-5">
            <div className="h-14 w-14 bg-primary rounded-2xl flex items-center justify-center text-white font-bold text-3xl shadow-xl shadow-primary/20 rotate-3 transition-transform group-hover:rotate-0">
              U
            </div>
            <span className="text-4xl 2xl:text-5xl font-bold text-foreground uppercase tracking-tighter">PayU</span>
          </Link>
        </div>

        <div className="space-y-6 2xl:space-y-8 mb-14">
          <p className="text-xs sm:text-sm font-bold text-primary/60 uppercase tracking-[0.3em] px-6 mb-6 opacity-70">{t('main')}</p>
          {mainMenu.map((item) => (
            <SidebarItem
              key={item.href}
              {...item}
              active={pathname === item.href || (item.href.endsWith('/dashboard') && pathname.endsWith('/dashboard'))}
            />
          ))}
        </div>

        <div className="space-y-6 2xl:space-y-8 mt-auto">
          <p className="text-xs sm:text-sm font-bold text-primary/60 uppercase tracking-[0.3em] px-6 mb-6 opacity-70">{t('others')}</p>
          {otherMenu.map((item) => (
            <SidebarItem
              key={item.href}
              {...item}
              active={pathname.includes(item.href)}
            />
          ))}
        </div>
      </aside>


      {/* Main Content Area */}
      <div className="flex-1 flex flex-col min-w-0 h-screen overflow-hidden">
        <header className="h-28 border-b border-border bg-background/80 backdrop-blur-3xl sticky top-0 z-30 shrink-0">
          <div className="w-full px-6 sm:px-10 lg:px-12 h-full flex items-center justify-between">
            <div className="flex items-center gap-8">
              <Sheet open={isSidebarOpen} onOpenChange={setIsSidebarOpen}>
                <SheetTrigger asChild>
                  <Button
                    variant="ghost"
                    size="icon"
                    data-testid="mobile-menu-trigger"
                    className="lg:hidden w-14 h-14 -ml-2 text-foreground/60 hover:text-foreground hover:bg-foreground/5 rounded-2xl"
                    aria-label="Buka menu navigasi"
                  >
                    <Menu className="h-8 w-8" aria-hidden="true" />
                  </Button>
                </SheetTrigger>
                <SheetContent side="left" className="w-80 p-0 border-r border-border bg-background shadow-3xl">
                  <SheetHeader className="p-8 pb-4">
                    <SheetTitle className="sr-only">Navigasi Utama</SheetTitle>
                    <div className="flex items-center gap-4">
                      <div className="h-10 w-10 bg-primary rounded-xl flex items-center justify-center text-white font-bold text-xl shadow-lg">
                        U
                      </div>
                      <span className="text-2xl font-bold text-foreground uppercase tracking-tighter">PayU</span>
                    </div>
                  </SheetHeader>
                  <nav className="px-4 py-4 space-y-2 overflow-y-auto h-[calc(100vh-120px)] scrollbar-hide">
                    <p className="text-xs font-bold text-emerald-500/60 uppercase tracking-[0.25em] px-6 mb-4">{t('main')}</p>
                    {mainMenu.map((item) => (
                      <SidebarItem
                        key={item.href}
                        {...item}
                        active={pathname === item.href || (item.href.endsWith('/dashboard') && pathname.endsWith('/dashboard'))}
                      />
                    ))}

                    <div className="h-8" />

                    <p className="text-xs font-bold text-emerald-500/60 uppercase tracking-[0.25em] px-6 mb-4">{t('others')}</p>
                    {otherMenu.map((item) => (
                      <SidebarItem
                        key={item.href}
                        {...item}
                        active={pathname.includes(item.href)}
                      />
                    ))}
                  </nav>
                </SheetContent>
              </Sheet>
              <div className="hidden sm:flex flex-col justify-center">
                <PersonalizedGreeting showTimeBased={true} showSegment={true} className="leading-tight text-lg font-bold" />
                <p className="text-xs sm:text-xs font-bold text-muted-foreground uppercase tracking-[0.2em] mt-2 ml-0.5 opacity-60">
                  AI Financial Forecaster Active
                </p>
              </div>
            </div>

            <div className="flex items-center gap-8">
               <ThemeToggle />
               <LanguageSwitcher />

                <div className="hidden xl:flex items-center bg-card rounded-2xl px-6 w-96 gap-4 border border-primary/10 focus-within:border-primary/40 focus-within:ring-4 focus-within:ring-primary/5 transition-all shadow-sm focus-within:shadow-md group">
                  <Search className="h-5 w-5 text-primary/40 group-focus-within:text-primary transition-colors" />
                  <Input
                    type="text"
                    data-testid="search-input"
                    placeholder="Pencarian cerdas..."
                    className="bg-transparent border-none focus-visible:ring-0 text-sm font-bold uppercase tracking-widest w-full placeholder:text-muted-foreground/40 text-foreground h-14 shadow-none"
                  />
                </div>

               <Button
                 variant="ghost"
                 size="icon"
                 data-testid="notification-button"
                 className="w-14 h-14 bg-card text-foreground/60 hover:text-primary hover:bg-primary/5 rounded-2xl relative shadow-sm border border-primary/10 hover:border-primary/30 transition-all"
                 aria-label="Notifikasi"
                 onClick={() => router.push(`/${locale}/notifications`)}
               >
                 <div className="absolute top-5 right-5 h-2.5 w-2.5 bg-primary rounded-full border-2 border-card shadow-sm" aria-label="Notifikasi baru" />
                 <Bell className="h-6 w-6" aria-hidden="true" />
               </Button>

                <DropdownMenu>
                  <DropdownMenuTrigger asChild>
                    <Button
                      variant="ghost"
                      data-testid="profile-menu-trigger"
                      className="p-0 h-auto rounded-full ring-offset-background transition-all hover:ring-2 hover:ring-primary shadow-lg border border-primary/10"
                      aria-label="Menu profil pengguna"
                    >
                      <Avatar className="h-14 w-14 border-2 border-card shadow-md">
                        <AvatarFallback className="bg-primary/5">
                          <User className="h-7 w-7 text-primary" aria-hidden="true" />
                        </AvatarFallback>
                      </Avatar>
                    </Button>
                  </DropdownMenuTrigger>
                  <DropdownMenuContent className="w-80 bg-card border border-border rounded-2xl shadow-[0_40px_80px_rgba(0,0,0,0.25)] py-6 glass" align="end" sideOffset={16}>
                    <div className="px-8 py-5 border-b border-border/10 mb-4">
                      <p className="text-xs font-bold text-primary/50 uppercase tracking-[0.3em] mb-2">Authenticated User</p>
                      <p className="text-xl font-bold truncate text-foreground uppercase tracking-tight">{username}</p>
                    </div>
                    <DropdownMenuItem className="p-0">
                      <Button
                        variant="ghost"
                        onClick={() => onLogout ? onLogout() : logoutMutation.mutate()}
                        data-testid="logout-button"
                        className="w-full text-left px-8 py-5 text-xs sm:text-sm text-red-500 hover:bg-red-500/10 hover:text-red-500 font-bold uppercase tracking-[0.3em] flex items-center justify-between transition-colors h-auto border-none focus-visible:ring-0"
                      >
                        <span>{t('logout')}</span>
                        <LogOut className="h-6 w-6 opacity-70" />
                      </Button>
                    </DropdownMenuItem>
                  </DropdownMenuContent>
                </DropdownMenu>
            </div>
          </div>
        </header>

        <main className="flex-1 overflow-y-auto scrollbar-hide bg-background">
          <div className="w-full px-6 sm:px-10 lg:px-12 py-8 lg:py-10 pb-32 transition-all duration-500">
            {children}
          </div>
        </main>
      </div>
      <MobileNav />
    </div>
  );
}
