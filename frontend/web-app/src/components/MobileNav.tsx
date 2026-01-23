'use client';

import Link from 'next/link';
import { usePathname } from 'next/navigation';
import { Home, Wallet, Repeat, Receipt } from 'lucide-react';
import clsx from 'clsx';


export default function MobileNav() {
 const pathname = usePathname();

 const navItems = [
  { href: '/', icon: Home, label: 'Beranda' },
  { href: '/transfer', icon: Repeat, label: 'Transfer' },
  { href: '/pockets', icon: Wallet, label: 'Kantong' },
  { href: '/bills', icon: Receipt, label: 'Tagihan' },
 ];

 // Don't show nav on login or onboarding
 if (pathname === '/login' || pathname === '/onboarding') return null;

 // Don't show if not authenticated
 const token = typeof window !== 'undefined' ? localStorage.getItem('token') : null;
 if (!token) return null;

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
