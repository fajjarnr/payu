'use client';

import Link from 'next/link';
import { usePathname } from 'next/navigation';
import { Home, Wallet, Repeat, Receipt } from 'lucide-react';
import clsx from 'clsx';
import { useAuthStore } from '@/stores';

/**
 * SECURITY NOTICE: Authentication Check
 * ================================
 * This component uses the auth store to check authentication status.
 * It does NOT access tokens from localStorage (security vulnerability).
 *
 * Tokens are managed exclusively via httpOnly cookies from the backend.
 * The auth store only tracks authentication state, not actual tokens.
 */
export default function MobileNav() {
 const pathname = usePathname();
 const isAuthenticated = useAuthStore((state) => state.isAuthenticated);

 const navItems = [
  { href: '/', icon: Home, label: 'Beranda' },
  { href: '/transfer', icon: Repeat, label: 'Transfer' },
  { href: '/pockets', icon: Wallet, label: 'Kantong' },
  { href: '/bills', icon: Receipt, label: 'Tagihan' },
 ];

 // Don't show nav on login or onboarding
 if (pathname === '/login' || pathname === '/onboarding') return null;

 // Don't show if not authenticated
 // SECURITY: Uses auth store state, NOT localStorage tokens
 if (!isAuthenticated) return null;

 return (
  <div className={clsx(
   "fixed bottom-0 left-0 right-0 bg-card/80 backdrop-blur-xl border-t border-border pb-safe pt-2 px-6 z-50",
   "lg:hidden" // Hide on desktop
  )}>
   <div className="flex justify-between items-center max-w-md mx-auto h-16">
    {navItems.map((item) => {
     const isActive = pathname === item.href;
     return (
      <Link
       key={item.href}
       href={item.href}
       className={clsx(
        "flex flex-col items-center gap-1 p-2 transition-all relative group",
        isActive ? "text-primary" : "text-muted-foreground hover:text-foreground"
       )}
      >
       <div className={clsx(
        "p-2 rounded-xl transition-all",
        isActive ? "bg-accent shadow-sm" : "group-hover:bg-muted"
       )}>
        <item.icon className={clsx("h-5 w-5 transition-transform", isActive ? "stroke-[2.5px] scale-110" : "scale-100")} />
       </div>
       <span className={clsx("text-[9px] font-bold tracking-wider transition-all", isActive ? "opacity-100" : "opacity-0 h-0 overflow-hidden")}>
        {item.label}
       </span>
      </Link>
     );
    })}
   </div>
  </div>
 );
}
