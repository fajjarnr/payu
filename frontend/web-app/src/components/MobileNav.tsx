'use client';

import { usePathname } from '@/lib/navigation';
import { useTranslations, useLocale } from 'next-intl';
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
  const locale = useLocale();
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
        "fixed bottom-0 left-0 right-0 bg-card/70 backdrop-blur-2xl border-t border-border pb-[env(safe-area-inset-bottom,1.5rem)] pt-3 px-8 z-50",
        "lg:hidden shadow-[0_-10px_40px_rgba(0,0,0,0.15)] rounded-t-3xl"
      )} role="navigation" aria-label={t('mobileNavigation')}>
      <div className="flex justify-between items-center max-w-lg mx-auto h-16">
        {navItems.map((item) => {
          const isActive = pathname === item.href || (item.href.endsWith('/dashboard') && pathname.endsWith('/dashboard'));
          return (
            <Link
              key={item.href}
              href={item.href}
              data-testid={`mobile-nav-${item.label.toLowerCase()}`}
              className={clsx(
                "flex flex-col items-center gap-1.5 transition-all relative group",
                isActive ? "text-primary scale-105" : "text-foreground/40 hover:text-foreground"
              )}
              aria-current={isActive ? 'page' : undefined}
            >
              <div className={clsx(
                "p-3 rounded-2xl transition-all duration-300",
                isActive ? "bg-primary/10 shadow-[inset_0_0_15px_rgba(16,185,129,0.1)] border border-primary/20" : "group-hover:bg-foreground/5 "
              )}>
                <item.icon className={clsx("h-6 w-6 transition-all", isActive ? "stroke-[2.5px]" : "stroke-[2px]")} aria-hidden="true" />
              </div>
              <span className={clsx(
                "text-xs font-bold uppercase tracking-[0.15em] transition-all duration-300 transform",
                isActive ? "opacity-100 translate-y-0" : "opacity-0 -translate-y-1 h-0 overflow-hidden"
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
