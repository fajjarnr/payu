'use client';

import React, { useState } from 'react';
import { Link } from '@/lib/navigation';
import { usePathname } from '@/lib/navigation';
import {
  Users,
  AlertTriangle,
  Headphones,
  FileText,
  ClipboardCheck,
  Gift,
  TrendingUp,
  BellRing,
  Store,
  LayoutDashboard,
  Search,
  Bell,
  Menu,
} from '@/components/icons';
import { Avatar, Badge, Button, Flex, Input, Layout, Menu as AntMenu, Space, Typography } from 'antd';

const GROUP_ORDER = ['CORE', 'OPERATIONS', 'PLATFORM', 'GOVERNANCE', 'GROWTH', 'FINANCIAL', 'ECOSYSTEM'];

export default function BackofficeLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  const pathname = usePathname();
  const [collapsed, setCollapsed] = useState(false);

  const navigation = [
    { name: 'Command Center', href: '/backoffice', icon: LayoutDashboard, group: 'CORE' },
    { name: 'KYC Reviews', href: '/backoffice/kyc', icon: Users, group: 'OPERATIONS' },
    { name: 'Fraud Monitoring', href: '/backoffice/fraud', icon: AlertTriangle, group: 'OPERATIONS' },
    { name: 'Customer Service', href: '/backoffice/customers', icon: Headphones, group: 'OPERATIONS' },
    { name: 'CMS Content', href: '/backoffice/cms', icon: FileText, group: 'PLATFORM' },
    { name: 'Compliance Audit', href: '/backoffice/compliance', icon: ClipboardCheck, group: 'GOVERNANCE' },
    { name: 'Campaigns', href: '/backoffice/campaigns', icon: Gift, group: 'GROWTH' },
    { name: 'FX Rates', href: '/backoffice/fx-rates', icon: TrendingUp, group: 'FINANCIAL' },
    { name: 'Broadcast', href: '/backoffice/broadcast', icon: BellRing, group: 'PLATFORM' },
    { name: 'Partners', href: '/backoffice/partners', icon: Store, group: 'ECOSYSTEM' },
  ];

  const activeNav = navigation.find(item => pathname === item.href || (item.href !== '/backoffice' && pathname.startsWith(item.href)));
  const selectedKey = activeNav?.href ?? '/backoffice';

  return (
    <Layout style={{ minHeight: '100vh' }}>
      <Layout.Sider
        collapsible
        collapsed={collapsed}
        onCollapse={setCollapsed}
        breakpoint="lg"
        width={320}
        style={{ background: 'var(--ant-color-bg-container)' }}
      >
        <Flex align="center" gap={16} style={{ padding: 32 }}>
          <Avatar
            shape="square"
            size={48}
            style={{ background: 'var(--ant-color-primary)', color: 'var(--ant-color-text-light-solid)', fontSize: 24, fontWeight: 700, flexShrink: 0 }}
          >
            U
          </Avatar>
          {!collapsed && (
            <Space direction="vertical" size={4}>
              <Typography.Title level={4} style={{ margin: 0, textTransform: 'uppercase' }}>PayU</Typography.Title>
              <Typography.Text strong style={{ fontSize: 12, letterSpacing: '0.1em', textTransform: 'uppercase', color: 'var(--ant-color-primary)' }}>Backoffice</Typography.Text>
            </Space>
          )}
        </Flex>
        <AntMenu
          mode="inline"
          selectedKeys={[selectedKey]}
          style={{ borderInlineEnd: 'none' }}
          items={GROUP_ORDER.flatMap((group) => {
            const items = navigation.filter((n) => n.group === group);
            if (items.length === 0) return [];
            return [{
              key: `group-${group}`,
              type: 'group' as const,
              label: collapsed ? undefined : group,
              children: items.map((item) => ({
                key: item.href,
                icon: <item.icon style={{ fontSize: 20 }} />,
                label: <Link href={item.href}>{item.name}</Link>,
              })),
            }];
          })}
        />
      </Layout.Sider>

      <Layout>
        <Layout.Header style={{ background: 'var(--ant-color-bg-container)', padding: '0 32px', height: 96, lineHeight: '96px' }}>
          <Flex align="center" justify="space-between" style={{ height: '100%' }}>
            <Flex align="center" gap={24}>
              <Button
                type="text"
                onClick={() => setCollapsed((c) => !c)}
                icon={<Menu style={{ fontSize: 24 }} />}
                aria-label="Buka menu navigasi"
              />
              <Space direction="vertical" size={4} style={{ lineHeight: 1.4 }}>
                <Typography.Title level={4} style={{ margin: 0 }}>
                  {activeNav?.name || 'Dashboard'}
                </Typography.Title>
                <Typography.Text type="secondary" strong style={{ fontSize: 12, letterSpacing: '0.1em', textTransform: 'uppercase' }}>
                  Secured Administrator Access
                </Typography.Text>
              </Space>
            </Flex>

            <Flex align="center" gap={24}>
              <Input
                aria-label="Pencarian Universal Admin"
                placeholder="Universal Admin Search..."
                prefix={<Search style={{ fontSize: 16, color: 'var(--ant-color-text-secondary)' }} />}
                style={{ width: 320 }}
              />
              <Badge dot>
                <Button
                  type="text"
                  icon={<Bell style={{ fontSize: 20, color: 'var(--ant-color-text-secondary)' }} />}
                  aria-label="Notifikasi"
                />
              </Badge>
              <Flex align="center" gap={16}>
                <Space direction="vertical" size={0} style={{ textAlign: 'right', lineHeight: 1.4 }}>
                  <Typography.Text strong style={{ fontSize: 12, textTransform: 'uppercase' }}>Administrator</Typography.Text>
                  <Typography.Text strong style={{ fontSize: 12, letterSpacing: '0.1em', textTransform: 'uppercase', color: 'var(--ant-color-primary)' }}>Super User</Typography.Text>
                </Space>
                <Avatar size={48} style={{ background: 'var(--ant-color-primary-bg)', color: 'var(--ant-color-primary)', fontWeight: 700 }}>
                  AD
                </Avatar>
              </Flex>
            </Flex>
          </Flex>
        </Layout.Header>

        <Layout.Content style={{ padding: 32, overflowY: 'auto' }}>
          {children}
        </Layout.Content>
      </Layout>
    </Layout>
  );
}
