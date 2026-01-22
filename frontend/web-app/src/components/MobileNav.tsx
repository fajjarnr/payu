'use client';

import Link from 'next/link';
import { usePathname } from 'next/navigation';
import { Home, Wallet, Repeat, Receipt } from 'lucide-react';
import clsx from 'clsx';


export default function MobileNav() {
  const pathname = usePathname();

  const navItems = [
    { href: '/', icon: Home, label: 'Home' },
    { href: '/transfer', icon: Repeat, label: 'Transfer' },
    { href: '/pockets', icon: Wallet, label: 'Pockets' },
    { href: '/bills', icon: Receipt, label: 'Bills' },
  ];

  // Don't show nav on login or onboarding
  if (pathname === '/login' || pathname === '/onboarding') return null;

  // Don't show if not authenticated
  const token = typeof window !== 'undefined' ? localStorage.getItem('token') : null;
  if (!token) return null;

  return (
    <div className={clsx(
      "fixed bottom-0 left-0 right-0 bg-white dark:bg-gray-800 border-t border-gray-100 dark:border-gray-700 pb-safe pt-2 px-6 z-50",
      "md:hidden" // Hide on desktop
    )}>
      <div className="flex justify-between items-center max-w-md mx-auto">
        {navItems.map((item) => {
          const isActive = pathname === item.href;
          return (
            <Link 
              key={item.href} 
              href={item.href}
              className={clsx(
                "flex flex-col items-center gap-1 p-2 transition-colors",
                isActive ? "text-green-600 dark:text-green-400" : "text-gray-400 hover:text-gray-600 dark:text-gray-500"
              )}
            >
              <item.icon className={clsx("h-6 w-6", isActive && "fill-current")} />
              <span className="text-[10px] font-medium">{item.label}</span>
            </Link>
          );
        })}
      </div>
    </div>
  );
}
