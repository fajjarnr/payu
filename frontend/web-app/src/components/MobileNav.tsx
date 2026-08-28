'use client';

import { usePathname } from '@/lib/navigation';
import { useTranslations } from 'next-intl';
import { Home, Wallet, Repeat, Receipt } from 'lucide-react';
import clsx from 'clsx';
import { useIsAuthenticated } from '@/stores';
import { Link } from '@/lib/navigation';

/**
 * SECURITY NOTICE: Authentication Check
 * ================================
 * This component uses the auth store to check authentication status.
 * It does NOT access tokens from localStorage (security vulnerability).
 */
export default function MobileNav() {
  const t = useTranslations('nav');
  const pathname = usePathname();
  const isAuthenticated = useIsAuthenticated();

  const navItems = [
    { href: '/dashboard', icon: Home, label: t('dashboard') },
    { href: '/transfer', icon: Repeat, label: t('transfers') },
    { href: '/pockets', icon: Wallet, label: t('accounts') },
    { href: '/bills', icon: Receipt, label: t('bills') },
  ];

  // Don't show nav on login or onboarding
  if (pathname.includes('/login') || pathname.includes('/onboarding')) return null;

  // Don't show if not authenticated
  if (!isAuthenticated) return null;

  return (
    <div
      data-testid="mobile-nav"
      className={clsx(
        "fixed bottom-0 left-0 right-0 bg-card/95 backdrop-blur-2xl border-t border-border pt-2 pb-[max(0.5rem,env(safe-area-inset-bottom))] px-2 sm:px-4 z-50",
        "lg:hidden shadow-[0_-8px_30px_rgba(0,0,0,0.12)] rounded-t-2xl sm:rounded-t-3xl"
      )} role="navigation" aria-label={t('mobileNavigation')}>
      <div className="flex justify-around sm:justify-between items-center max-w-lg mx-auto h-14 sm:h-16 gap-1">
        {navItems.map((item) => {
          const isActive = pathname === item.href || (item.href.endsWith('/dashboard') && pathname.endsWith('/dashboard'));
          return (
            <Link
              key={item.href}
              href={item.href}
              data-testid={`mobile-nav-${item.label.toLowerCase()}`}
              className={clsx(
                "flex flex-col items-center justify-center gap-1 min-w-0 flex-1 py-1 px-1 sm:px-2 rounded-xl transition-all cursor-pointer",
                isActive ? "text-primary" : "text-foreground/40 hover:text-foreground active:scale-95"
              )}
              aria-current={isActive ? 'page' : undefined}
            >
              <div className={clsx(
                "p-2 sm:p-2.5 rounded-xl sm:rounded-2xl transition-all duration-200 flex items-center justify-center",
                isActive ? "bg-primary/10 border border-primary/20" : "group-hover:bg-foreground/5"
              )}>
                <item.icon className={clsx("h-5 w-5 sm:h-6 sm:w-6 transition-all shrink-0", isActive ? "stroke-[2.5px]" : "stroke-[2px]")} aria-hidden="true" />
              </div>
              <span className={clsx(
                "text-[10px] sm:text-xs font-bold uppercase tracking-[0.08em] sm:tracking-[0.12em] leading-none text-center truncate w-full px-0.5",
                isActive ? "opacity-100" : "opacity-70"
              )}>
                {item.label}
              </span>
            </Link>
          );
        })}
      </div>
    </div>
  );
}
