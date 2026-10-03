'use client';

import React, { useState, useSyncExternalStore } from 'react';
import { Link, usePathname, useRouter } from '@/lib/navigation';
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
  Menu as MenuIcon,
  User,
  LogOut,
  QrCode,
  Receipt,
  TrendingUp,
  Calendar,
  History,
} from '@/components/icons';
import { useTranslations, useLocale } from 'next-intl';
import { useUIStore } from '@/stores';
import { Layout, Menu, Dropdown, Badge, Avatar, Input, Button, Drawer, theme } from 'antd';
import type { MenuProps } from 'antd';
import MobileNav from './MobileNav';
import LanguageSwitcher from './LanguageSwitcher';
import ThemeToggle from './ThemeToggle';
import { PersonalizedGreeting } from './personalization';
import { useLogout } from '@/hooks';

const { Sider, Header, Content } = Layout;

const emptySubscribe = () => () => {};

interface DashboardLayoutProps {
  children?: React.ReactNode;
  username?: string;
  onLogout?: () => void;
}

export default function DashboardLayout({
  children,
  username = 'Pengguna',
  onLogout,
}: DashboardLayoutProps) {
  const t = useTranslations('nav');
  const locale = useLocale();
  const pathname = usePathname();
  const router = useRouter();
  const logoutMutation = useLogout();
  const mode = useUIStore((s) => s.theme);
  const { token } = theme.useToken();
  const mounted = useSyncExternalStore(emptySubscribe, () => true, () => false);
  const [drawerOpen, setDrawerOpen] = useState(false);

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

  const selectedKeys = [
    ...mainMenu
      .filter(
        (item) =>
          pathname === item.href ||
          (item.href.endsWith('/dashboard') && pathname.endsWith('/dashboard')),
      )
      .map((item) => item.href),
    ...otherMenu.filter((item) => pathname.includes(item.href)).map((item) => item.href),
  ];

  const menuItems: MenuProps['items'] = [
    {
      type: 'group',
      label: t('main'),
      children: mainMenu.map(({ href, icon: Icon, label }) => ({
        key: href,
        icon: <Icon className="h-5 w-5" aria-hidden="true" />,
        label,
      })),
    },
    {
      type: 'group',
      label: t('others'),
      children: otherMenu.map(({ href, icon: Icon, label }) => ({
        key: href,
        icon: <Icon className="h-5 w-5" aria-hidden="true" />,
        label,
      })),
    },
  ];

  const handleMenuClick: MenuProps['onClick'] = ({ key }) => {
    setDrawerOpen(false);
    router.push(`/${locale}${key}`);
  };

  const profileItems: MenuProps['items'] = [
    {
      key: 'identity',
      disabled: true,
      label: (
        <div className="py-1">
          <p className="mb-1 text-[10px] font-bold uppercase tracking-[0.3em] text-primary/50">
            Authenticated User
          </p>
          <p className="truncate text-lg font-bold uppercase tracking-tight">{username}</p>
        </div>
      ),
    },
    { type: 'divider' },
    {
      key: 'logout',
      danger: true,
      icon: <LogOut className="h-5 w-5" aria-hidden="true" />,
      label: t('logout'),
      onClick: () => (onLogout ? onLogout() : logoutMutation.mutate()),
    },
  ];

  const logoMark = (
    <div className="flex h-11 w-11 shrink-0 items-center justify-center rounded-2xl bg-primary text-2xl font-bold text-white shadow-lg">
      U
    </div>
  );

  const sidebarMenu = (
    <Menu
      mode="inline"
      selectedKeys={selectedKeys}
      items={menuItems}
      onClick={handleMenuClick}
      style={{ borderInlineEnd: 'none', background: 'transparent' }}
    />
  );

  return (
    <Layout hasSider style={{ height: '100vh', overflow: 'hidden' }}>
      <Sider
        breakpoint="lg"
        collapsedWidth={0}
        collapsible
        trigger={null}
        width={288}
        theme={mounted && mode === 'dark' ? 'dark' : 'light'}
        style={{
          height: '100vh',
          overflowY: 'auto',
          background: token.colorBgContainer,
          borderRight: `1px solid ${token.colorBorderSecondary}`,
        }}
      >
        <Link href="/" className="flex items-center gap-4 px-2 py-4" aria-label="PayU">
          {logoMark}
          <span className="text-2xl font-bold uppercase tracking-tighter">PayU</span>
        </Link>
        {sidebarMenu}
      </Sider>

      <Layout style={{ height: '100vh', overflow: 'hidden' }}>
        <Header
          style={{
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'space-between',
            gap: 12,
            height: 80,
            lineHeight: 'normal',
            padding: '0 24px',
            background: token.colorBgContainer,
            borderBottom: `1px solid ${token.colorBorderSecondary}`,
            position: 'sticky',
            top: 0,
            zIndex: 30,
          }}
        >
          <div className="flex min-w-0 flex-1 items-center gap-3">
            <Button
              type="text"
              className="lg:hidden"
              icon={<MenuIcon className="h-5 w-5" aria-hidden="true" />}
              onClick={() => setDrawerOpen(true)}
              aria-label="Buka menu navigasi"
              data-testid="mobile-menu-trigger"
            />
            <div className="hidden flex-col justify-center sm:flex">
              <PersonalizedGreeting
                showTimeBased={true}
                showSegment={true}
                className="text-lg font-bold leading-tight"
              />
              <p className="ml-0.5 mt-1 text-[10px] font-bold uppercase tracking-[0.2em] text-muted-foreground opacity-60">
                AI Financial Forecaster Active
              </p>
            </div>
          </div>

          <div className="flex shrink-0 items-center gap-2 sm:gap-4">
            <ThemeToggle />
            <LanguageSwitcher />

            <Input.Search
              data-testid="search-input"
              placeholder="Pencarian cerdas..."
              allowClear
              className="hidden w-80 xl:flex"
            />

            <Badge dot color={token.colorPrimary} offset={[-4, 4]}>
              <Button
                type="text"
                data-testid="notification-button"
                icon={<Bell className="h-5 w-5" aria-hidden="true" />}
                aria-label="Notifikasi"
                onClick={() => router.push(`/${locale}/notifications`)}
                style={{ width: 44, height: 44 }}
              />
            </Badge>

            <Dropdown menu={{ items: profileItems }} trigger={['click']} placement="bottomRight">
              <Button
                type="text"
                data-testid="profile-menu-trigger"
                aria-label="Menu profil pengguna"
                style={{ width: 48, height: 48, padding: 0 }}
              >
                <Avatar
                  size={40}
                  icon={<User className="h-5 w-5" aria-hidden="true" />}
                  style={{ background: token.colorPrimaryBg, color: token.colorPrimary }}
                />
              </Button>
            </Dropdown>
          </div>
        </Header>

        <Content
          style={{
            flex: 1,
            overflowY: 'auto',
            overflowX: 'hidden',
            background: token.colorBgLayout,
            padding: '24px',
          }}
        >
          {children}
        </Content>
      </Layout>

      <Drawer
        placement="left"
        open={drawerOpen}
        onClose={() => setDrawerOpen(false)}
        width={288}
        title={
          <Link href="/" className="flex items-center gap-4 px-2 py-4" aria-label="PayU">
            {logoMark}
          </Link>
        }
        styles={{
          header: { borderBottom: `1px solid ${token.colorBorderSecondary}` },
          body: { padding: 0, background: token.colorBgContainer },
        }}
      >
        {sidebarMenu}
      </Drawer>

      <MobileNav />
    </Layout>
  );
}
