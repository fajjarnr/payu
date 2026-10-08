'use client';

import React, { useState } from 'react';
import type { CSSProperties } from 'react';
import {
  Store,
  Search,
  Plus,
  CheckCircle2,
  AlertCircle,
  Globe,
  ShieldCheck,
  Key,
  Settings,
  MoreHorizontal,
  ChevronLeft,
  ChevronRight,
  ExternalLink,
} from '@/components/icons';
import { Avatar, Button, Card, Divider, Flex, Input, Space, Table, Tag, Typography } from 'antd';
import type { TableColumnsType } from 'antd';
import { usePartners } from '@/hooks';
import type { Partner } from '@/services';
import StatCards from '../_components/StatCards';

const labelStyle: CSSProperties = {
  fontSize: 12,
  fontWeight: 700,
  color: 'var(--ant-color-text-secondary)',
  textTransform: 'uppercase',
  letterSpacing: '0.1em',
};

type PartnerRow = {
  id: number;
  name: string;
  type: string;
  status: 'ACTIVE' | 'UNDER_REVIEW';
  apiLevel: string;
  transactions: string;
  volume: string;
};

function toPartnerRow(partner: Partner): PartnerRow {
  return {
    id: partner.id,
    name: partner.name,
    type: partner.type,
    status: partner.active ? 'ACTIVE' : 'UNDER_REVIEW',
    apiLevel: partner.publicKey ? 'SNAP BI Ready' : 'Pending Setup',
    transactions: '--',
    volume: 'N/A',
  };
}

export default function PartnersPage() {
  const [searchTerm, setSearchTerm] = useState('');
  const { data: partnersData, isLoading } = usePartners();

  const partners = (Array.isArray(partnersData) ? partnersData.map(toPartnerRow) : []).filter((p) => {
    if (!searchTerm) return true;
    const term = searchTerm.toLowerCase();
    return p.name?.toLowerCase().includes(term) || String(p.id ?? '').toLowerCase().includes(term);
  });

  const activeCount = partners.filter(p => p.status === 'ACTIVE').length;
  const pendingCount = partners.filter(p => p.status === 'UNDER_REVIEW').length;

  const columns: TableColumnsType<PartnerRow> = [
    {
      key: 'org',
      title: 'Partner Org',
      render: (_, partner) => (
        <Flex align="center" gap={12}>
          <Avatar
            shape="square"
            size={40}
            style={{ background: 'var(--ant-color-fill-tertiary)', color: 'var(--ant-color-text-secondary)' }}
            icon={<Store style={{ fontSize: 20 }} />}
          />
          <Space direction="vertical" size={0}>
            <Typography.Text strong style={{ fontSize: 14 }}>{partner.name}</Typography.Text>
            <Typography.Text type="secondary" strong style={labelStyle}>{partner.id}</Typography.Text>
          </Space>
        </Flex>
      ),
    },
    {
      key: 'type',
      title: 'Type',
      render: (_, partner) => <Typography.Text strong style={labelStyle}>{partner.type}</Typography.Text>,
    },
    { key: 'status', title: 'Status', render: (_, partner) => partnerStatusBadge(partner.status) },
    {
      key: 'api',
      title: 'API Integration',
      render: (_, partner) => (
        <Tag color="blue" icon={<ShieldCheck style={{ fontSize: 12 }} />}>{partner.apiLevel}</Tag>
      ),
    },
    {
      key: 'volume',
      title: 'Volume',
      render: (_, partner) => (
        <Space direction="vertical" size={0}>
          <Typography.Text strong style={{ fontSize: 12 }}>{partner.volume}</Typography.Text>
          <Typography.Text type="secondary" style={{ fontSize: 12 }}>{partner.transactions} txns</Typography.Text>
        </Space>
      ),
    },
    {
      key: 'actions',
      title: 'Actions',
      align: 'right',
      render: () => (
        <Flex align="center" justify="flex-end" gap={8}>
          <Button type="text" icon={<ExternalLink style={{ fontSize: 16 }} />} aria-label="Buka tautan eksternal" />
          <Button type="text" icon={<Settings style={{ fontSize: 16 }} />} aria-label="Pengaturan partner" />
          <Button type="text" icon={<MoreHorizontal style={{ fontSize: 16 }} />} aria-label="Aksi lainnya" />
        </Flex>
      ),
    },
  ];

  return (
    <Space direction="vertical" size={24} style={{ width: '100%' }}>
      <StatCards stats={[
        { label: 'Total Partners', value: isLoading ? '…' : String(partners.length), icon: Store },
        { label: 'Active Merchants', value: isLoading ? '…' : String(activeCount), icon: CheckCircle2 },
        { label: 'Pending Apps', value: isLoading ? '…' : String(pendingCount), icon: AlertCircle },
        { label: 'SNAP BI Volume', value: '—', icon: Globe },
      ]} />

      <Card>
        <Flex gap={16} wrap align="center" justify="space-between">
          <Input
            aria-label="Cari mitra berdasarkan nama atau ID"
            placeholder="Search partners by name or ID..."
            prefix={<Search style={{ fontSize: 16, color: 'var(--ant-color-text-secondary)' }} />}
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
            style={{ flex: 1, minWidth: 200, maxWidth: 384 }}
          />
          <Flex gap={16} wrap>
            <Button icon={<Key style={{ fontSize: 16 }} />}>
              Manage API Keys
            </Button>
            <Button type="primary" icon={<Plus style={{ fontSize: 16 }} />}>
              Register New Partner
            </Button>
          </Flex>
        </Flex>
      </Card>

      <Card styles={{ body: { padding: 0, overflow: 'hidden' } }}>
        <Table<PartnerRow>
          columns={columns}
          dataSource={partners}
          rowKey="id"
          pagination={false}
          loading={isLoading}
          locale={{ emptyText: 'No partners found' }}
        />
        <Divider style={{ margin: 0 }} />
        <Flex align="center" justify="space-between" wrap gap={16} style={{ padding: 24 }}>
          <Typography.Text type="secondary" strong style={labelStyle}>
            Partner Portal & SNAP BI Registry Syncing
          </Typography.Text>
          <Flex align="center" gap={8}>
            <Button icon={<ChevronLeft style={{ fontSize: 16 }} />} aria-label="Halaman sebelumnya" disabled />
            <Button type="primary">1</Button>
            <Button icon={<ChevronRight style={{ fontSize: 16 }} />} aria-label="Halaman berikutnya" disabled />
          </Flex>
        </Flex>
      </Card>
    </Space>
  );
}

function partnerStatusBadge(status: string) {
  switch (status) {
    case 'ACTIVE':
      return <Tag color="green">Active</Tag>;
    case 'UNDER_REVIEW':
      return <Tag color="gold">Reviewing</Tag>;
    case 'SUSPENDED':
      return <Tag color="red">Suspended</Tag>;
    default:
      return <Tag>{status}</Tag>;
  }
}
