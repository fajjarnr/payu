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
            "flex items-center gap-3 px-4 py-3 rounded-xl transition-all duration-200 group",
            active
                ? "bg-bank-green/10 text-bank-green font-semibold"
                : "text-gray-500 hover:bg-gray-50 dark:hover:bg-gray-800 hover:text-gray-900 dark:hover:text-gray-200"
        )}
    >
        <Icon className={clsx(
            "h-5 w-5",
            active ? "text-bank-green" : "text-gray-400 group-hover:text-gray-900 dark:group-hover:text-gray-200"
        )} />
        <span>{label}</span>
    </Link>
);

interface DashboardLayoutProps {
    children: React.ReactNode;
    username?: string;
    onLogout?: () => void;
}

export default function DashboardLayout({ children, username = 'User', onLogout }: DashboardLayoutProps) {
    const pathname = usePathname();
    const [isSidebarOpen, setIsSidebarOpen] = useState(false);

    const mainMenu = [
        { href: '/', icon: LayoutDashboard, label: 'Dashboard' },
        { href: '/pockets', icon: Wallet, label: 'Pockets' },
        { href: '/transfer', icon: ArrowRightLeft, label: 'Transfers' },
        { href: '/qris', icon: QrCode, label: 'QRIS Payments' },
        { href: '/bills', icon: Receipt, label: 'Bills & Top-up' },
        { href: '/cards', icon: CreditCard, label: 'Virtual Card' },
        { href: '/investments', icon: TrendingUp, label: 'Investments' },
        { href: '/analytics', icon: BarChart3, label: 'Financial Analytics' },
    ];

    const otherMenu = [
        { href: '/security', icon: ShieldCheck, label: 'Security & MFA' },
        { href: '/settings', icon: Settings, label: 'Account Settings' },
        { href: '/support', icon: LifeBuoy, label: 'Help & Support' },
    ];

    return (
        <div className="h-screen bg-background flex overflow-hidden">
            {/* Desktop Sidebar */}
            <aside className="hidden lg:flex flex-col w-64 border-r border-border bg-sidebar p-6 h-full overflow-y-auto">
                <div className="flex items-center gap-2 mb-10 px-2">
                    <div className="h-10 w-10 bg-bank-green rounded-xl flex items-center justify-center text-white font-bold text-xl">
                        U
                    </div>
                    <span className="text-2xl font-bold tracking-tight text-bank-green">PayU</span>
                </div>

                <div className="space-y-1 mb-8">
                    <p className="text-[10px] font-bold text-gray-400 uppercase tracking-widest px-4 mb-2">Main Menu</p>
                    {mainMenu.map((item) => (
                        <SidebarItem
                            key={item.href}
                            {...item}
                            active={pathname === item.href}
                        />
                    ))}
                </div>

                <div className="space-y-1 mt-auto">
                    <p className="text-[10px] font-bold text-gray-400 uppercase tracking-widest px-4 mb-2">Others</p>
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
                    className="fixed inset-0 bg-black/50 z-50 lg:hidden backdrop-blur-sm"
                    onClick={() => setIsSidebarOpen(false)}
                />
            )}

            {/* Mobile Sidebar */}
            <aside className={clsx(
                "fixed inset-y-0 left-0 w-72 bg-sidebar z-50 transform transition-transform duration-300 lg:hidden p-6 shadow-2xl",
                isSidebarOpen ? "translate-x-0" : "-translate-x-full"
            )}>
                <div className="flex justify-between items-center mb-10 px-2">
                    <div className="flex items-center gap-2">
                        <div className="h-10 w-10 bg-bank-green rounded-xl flex items-center justify-center text-white font-bold text-xl">
                            U
                        </div>
                        <span className="text-2xl font-bold tracking-tight text-bank-green">PayU</span>
                    </div>
                    <button onClick={() => setIsSidebarOpen(false)} className="p-2 text-gray-500 hover:bg-gray-100 rounded-lg">
                        <X className="h-6 w-6" />
                    </button>
                </div>

                <nav className="space-y-1 overflow-y-auto max-h-[calc(100vh-140px)]">
                    <p className="text-[10px] font-bold text-gray-400 uppercase tracking-widest px-4 mb-2">Main Menu</p>
                    {mainMenu.map((item) => (
                        <SidebarItem
                            key={item.href}
                            {...item}
                            active={pathname === item.href}
                        />
                    ))}

                    <div className="h-8" />

                    <p className="text-[10px] font-bold text-gray-400 uppercase tracking-widest px-4 mb-2">Others</p>
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
            <div className="flex-1 flex flex-col min-w-0 h-screen">
                <header className="h-20 border-b border-border bg-card/80 backdrop-blur-md sticky top-0 z-30 px-6 sm:px-10 flex items-center justify-between shrink-0">
                    <div className="flex items-center gap-4">
                        <button
                            onClick={() => setIsSidebarOpen(true)}
                            className="lg:hidden p-2 -ml-2 text-gray-600 hover:bg-gray-100 dark:hover:bg-gray-800 rounded-lg"
                        >
                            <Menu className="h-6 w-6" />
                        </button>
                        <div className="hidden sm:block">
                            <h1 className="text-xl font-bold text-gray-900 dark:text-white">Welcome, {username}!</h1>
                            <p className="text-xs text-gray-500">Effortlessly manage your finances with real-time insights</p>
                        </div>
                    </div>

                    <div className="flex items-center gap-2 sm:gap-4">
                        <div className="hidden md:flex items-center bg-gray-100 dark:bg-gray-800 rounded-xl px-3 py-2 w-64 gap-2 border border-transparent focus-within:border-bank-green transition-all">
                            <Search className="h-4 w-4 text-gray-400" />
                            <input
                                type="text"
                                placeholder="Search anything..."
                                className="bg-transparent border-none focus:ring-0 text-sm w-full placeholder:text-gray-500 text-foreground"
                            />
                        </div>

                        <button className="p-2 text-gray-500 hover:bg-gray-100 dark:hover:bg-gray-800 rounded-full relative">
                            <div className="absolute top-2 right-2 h-2 w-2 bg-red-500 rounded-full border-2 border-white dark:border-gray-900" />
                            <Bell className="h-5 w-5" />
                        </button>

                        <div className="relative group">
                            <button className="h-10 w-10 bg-gradient-to-tr from-gray-200 to-gray-100 dark:from-gray-700 dark:to-gray-600 rounded-full flex items-center justify-center border-2 border-white dark:border-gray-800 shadow-sm overflow-hidden group-hover:ring-2 ring-bank-green transition-all">
                                <User className="h-6 w-6 text-gray-500" />
                            </button>

                            <div className="absolute right-0 mt-2 w-48 bg-white dark:bg-gray-800 rounded-xl shadow-xl py-2 ring-1 ring-black ring-opacity-5 hidden group-hover:block z-50 overflow-hidden">
                                <div className="px-4 py-2 border-b border-gray-100 dark:border-gray-700 mb-1">
                                    <p className="text-xs font-bold text-gray-400 uppercase">Account</p>
                                    <p className="text-sm font-bold truncate">{username}</p>
                                </div>
                                <button
                                    onClick={onLogout}
                                    className="w-full text-left px-4 py-2 text-sm text-red-500 hover:bg-gray-50 dark:hover:bg-gray-700 font-semibold flex items-center gap-2 transition-colors"
                                >
                                    <LogOut className="h-4 w-4" /> Sign out
                                </button>
                            </div>
                        </div>
                    </div>
                </header>

                <main className="flex-1 p-6 sm:p-10 overflow-y-auto pb-24 sm:pb-10">
                    {children}
                </main>
            </div>
            <MobileNav />
        </div>
    );
}
