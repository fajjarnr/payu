'use client';

import React, { useState } from 'react';
import { Link } from '@/lib/navigation';
import { usePathname } from '@/lib/navigation';
import { 
  ShieldCheck, // eslint-disable-line @typescript-eslint/no-unused-vars
  Users, 
  AlertTriangle, 
  Headphones, 
  FileText, 
  FlaskConical, 
  ClipboardCheck, 
  Gift, 
  TrendingUp, 
  BellRing, 
  Store,
  LayoutDashboard,
  Search,
  Bell,
  User, // eslint-disable-line @typescript-eslint/no-unused-vars
  Menu,
  X, // eslint-disable-line @typescript-eslint/no-unused-vars
  ChevronRight
} from 'lucide-react';
import clsx from 'clsx';
import { useLocale } from 'next-intl'; // eslint-disable-line @typescript-eslint/no-unused-vars
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Avatar, AvatarFallback } from '@/components/ui/avatar';
import { PageTransition } from '@/components/ui/Motion';

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
      "flex items-center justify-between px-6 py-3.5 rounded-xl transition-all duration-300 group font-bold",
      active
        ? "bg-primary text-white shadow-lg shadow-primary/20"
        : "text-muted-foreground hover:bg-primary/5 hover:text-primary"
    )}
  >
    <div className="flex items-center gap-3">
      <Icon className={clsx(
        "h-5 w-5 transition-colors",
        active ? "text-white" : "text-muted-foreground group-hover:text-primary"
      )} />
      <span className="text-xs tracking-widest uppercase">{label}</span>
    </div>
    {active && <ChevronRight className="h-4 w-4" />}
  </Link>
);

export default function BackofficeLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  const pathname = usePathname();
  const [isSidebarOpen, setIsSidebarOpen] = useState(false); // eslint-disable-line @typescript-eslint/no-unused-vars

  const navigation = [
    { name: 'Command Center', href: '/backoffice', icon: LayoutDashboard, group: 'CORE' },
    { name: 'KYC Reviews', href: '/backoffice/kyc', icon: Users, group: 'OPERATIONS' },
    { name: 'Fraud Monitoring', href: '/backoffice/fraud', icon: AlertTriangle, group: 'OPERATIONS' },
    { name: 'Customer Service', href: '/backoffice/customers', icon: Headphones, group: 'OPERATIONS' },
    { name: 'CMS Content', href: '/backoffice/cms', icon: FileText, group: 'PLATFORM' },
    { name: 'A/B Experiments', href: '/backoffice/ab-testing', icon: FlaskConical, group: 'PLATFORM' },
    { name: 'Compliance Audit', href: '/backoffice/compliance', icon: ClipboardCheck, group: 'GOVERNANCE' },
    { name: 'Campaigns', href: '/backoffice/campaigns', icon: Gift, group: 'GROWTH' },
    { name: 'FX Rates', href: '/backoffice/fx-rates', icon: TrendingUp, group: 'FINANCIAL' },
    { name: 'Broadcast', href: '/backoffice/broadcast', icon: BellRing, group: 'PLATFORM' },
    { name: 'Partners', href: '/backoffice/partners', icon: Store, group: 'ECOSYSTEM' },
  ];

  const groupedNav = navigation.reduce((acc, item) => {
    if (!acc[item.group]) acc[item.group] = [];
    acc[item.group].push(item);
    return acc;
  }, {} as Record<string, typeof navigation>);

  const activeNav = navigation.find(item => pathname === item.href || (item.href !== '/backoffice' && pathname.startsWith(item.href)));

  return (
    <div className="min-h-screen bg-background flex font-inter text-foreground">
      {/* Sidebar Desktop */}
      <aside className="hidden lg:flex flex-col w-80 bg-card border-r border-border h-screen sticky top-0 overflow-y-auto scrollbar-hide">
        <div className="p-8 border-b border-border mb-8">
          <Link href="/" className="flex items-center gap-4 group">
            <div className="h-12 w-12 bg-primary rounded-2xl flex items-center justify-center text-white font-bold text-2xl shadow-xl shadow-primary/20 transition-transform group-hover:scale-105">
              U
            </div>
            <div>
              <span className="text-2xl font-bold text-foreground uppercase tracking-tighter block leading-none">PayU</span>
              <span className="text-xs font-bold text-primary uppercase tracking-widest mt-1 block">Backoffice</span>
            </div>
          </Link>
        </div>

        <nav className="flex-1 px-4 space-y-8 pb-10">
          {Object.entries(groupedNav).map(([group, items]) => (
            <div key={group} className="space-y-2">
              <h3 className="text-xs font-bold text-muted-foreground uppercase tracking-widest px-6 mb-4 opacity-70">
                {group}
              </h3>
              <div className="space-y-1">
                {items.map((item) => (
                  <SidebarItem
                    key={item.href}
                    href={item.href}
                    icon={item.icon}
                    label={item.name}
                    active={pathname === item.href || (item.href !== '/backoffice' && pathname.startsWith(item.href))}
                  />
                ))}
              </div>
            </div>
          ))}
        </nav>
      </aside>

      {/* Main Content Area */}
      <div className="flex-1 flex flex-col min-w-0">
        <header className="h-24 bg-card/50 backdrop-blur-3xl border-b border-border sticky top-0 z-30">
          <div className="px-8 h-full flex items-center justify-between">
            <div className="flex items-center gap-6">
              <Button
                variant="ghost"
                size="icon"
                className="lg:hidden"
                onClick={() => setIsSidebarOpen(true)}
              >
                <Menu className="h-6 w-6" />
              </Button>
              <div className="hidden md:flex flex-col">
                <h2 className="text-xl font-bold text-foreground">
                  {activeNav?.name || 'Dashboard'}
                </h2>
                <p className="text-xs font-bold text-muted-foreground uppercase tracking-widest mt-1 opacity-60">
                  Secured Administrator Access
                </p>
              </div>
            </div>

            <div className="flex items-center gap-6">
              <div className="hidden xl:flex items-center bg-muted/30 rounded-xl px-4 w-80 gap-3 border border-border focus-within:border-primary/30 transition-all">
                <Search className="h-4 w-4 text-muted-foreground" />
                <Input
                  type="text"
                  placeholder="Universal Admin Search..."
                  className="bg-transparent border-none focus-visible:ring-0 text-xs font-bold uppercase tracking-widest w-full h-12"
                />
              </div>

              <Button variant="ghost" size="icon" className="relative h-12 w-12 rounded-xl bg-muted/30 border border-border">
                <Bell className="h-5 w-5 text-muted-foreground" />
                <span className="absolute top-3 right-3 h-2 w-2 bg-primary rounded-full border-2 border-background" />
              </Button>

              <div className="flex items-center gap-4 pl-6 border-l border-border">
                <div className="text-right hidden sm:block">
                  <p className="text-xs font-bold text-foreground uppercase">Administrator</p>
                  <p className="text-xs font-bold text-primary uppercase tracking-widest">Super User</p>
                </div>
                <Avatar className="h-12 w-12 border-2 border-primary/20 shadow-lg shadow-primary/10">
                  <AvatarFallback className="bg-primary/5 text-primary font-bold">AD</AvatarFallback>
                </Avatar>
              </div>
            </div>
          </div>
        </header>

        <main className="p-8 lg:p-8 overflow-y-auto">
          <PageTransition>
            {children}
          </PageTransition>
        </main>
      </div>
    </div>
  );
}
