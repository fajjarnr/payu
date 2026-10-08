'use client';

import React, { useState } from 'react';
import type { CSSProperties } from 'react';
import {
  Plus,
  Search,
  Gift,
  Tag as TagIcon,
  Users,
  Calendar,
  BarChart3,
  MoreHorizontal,
  Edit,
  Copy,
  CheckCircle2,
  Timer,
  BadgePercent,
  ChevronLeft,
  ChevronRight
} from '@/components/icons';
import { Button, Card, Divider, Dropdown, Flex, Input, Progress, Space, Table, Tag, Typography } from 'antd';
import type { MenuProps, TableColumnsType } from 'antd';
import type { Promotion } from '@/services';
import { useActivePromotions } from '@/hooks/useRewards';
import StatCards from '../_components/StatCards';

const labelStyle: CSSProperties = {
  fontSize: 12,
  fontWeight: 700,
  color: 'var(--ant-color-text-secondary)',
  textTransform: 'uppercase',
  letterSpacing: '0.1em',
};

 export default function CampaignsPage() {
  const [searchTerm, setSearchTerm] = useState('');
  const { data: campaigns, isLoading, error } = useActivePromotions();
  const filteredCampaigns = (campaigns ?? []).filter((cmp) => cmp.name.toLowerCase().includes(searchTerm.toLowerCase()));
  const campaignMenuItems: MenuProps['items'] = [
    { key: 'analytics', label: (<Space><BarChart3 style={{ fontSize: 16, color: 'var(--ant-color-primary)' }} /><Typography.Text strong style={labelStyle}>View Analytics</Typography.Text></Space>) },
    { key: 'end', label: (<Space><CheckCircle2 style={{ fontSize: 16, color: 'var(--ant-color-error)' }} /><Typography.Text strong style={{ ...labelStyle, color: 'var(--ant-color-error)' }}>End Campaign</Typography.Text></Space>) },
  ];
  const columns: TableColumnsType<Promotion> = [
    {
      key: 'name',
      title: 'Campaign Name',
      render: (_, cmp) => (
        <Space direction="vertical" size={4}>
          <Typography.Text strong style={{ fontSize: 14 }}>{cmp.name}</Typography.Text>
          <Typography.Text type="secondary" strong style={labelStyle}>{cmp.id}</Typography.Text>
          <Space size={8}>
            <Calendar style={{ fontSize: 12, color: 'var(--ant-color-text-secondary)' }} />
            <Typography.Text type="secondary" style={{ fontSize: 12 }}>{new Date(cmp.startDate).toLocaleDateString()} - {new Date(cmp.endDate).toLocaleDateString()}</Typography.Text>
          </Space>
        </Space>
      ),
    },
    {
      key: 'type',
      title: 'Type',
      render: (_, cmp) => (
        <Space>
          {campaignTypeIcon(cmp.type)}
          <Typography.Text strong style={labelStyle}>{cmp.type.replace('_', ' ')}</Typography.Text>
        </Space>
      ),
    },
    { key: 'status', title: 'Status', render: (_, cmp) => campaignStatusBadge(cmp.status) },
    { key: 'claims', title: 'Rewards Sent', render: (_, cmp) => <Typography.Text strong style={{ fontSize: 12 }}>{cmp.currentClaims.toLocaleString()}</Typography.Text> },
    {
      key: 'budget',
      title: 'Budget Spent',
      render: (_, cmp) => (
        <Space direction="vertical" size={8} style={{ minWidth: 128 }}>
          <Flex align="center" justify="space-between">
            <Typography.Text strong style={{ fontSize: 12, color: 'var(--ant-color-primary)' }}>{cmp.value}</Typography.Text>
            <Typography.Text type="secondary" style={{ fontSize: 12 }}>/ {cmp.maxClaims ?? '∞'}</Typography.Text>
          </Flex>
          <Progress percent={cmp.maxClaims ? Math.min(100, (cmp.currentClaims / cmp.maxClaims) * 100) : 0} showInfo={false} size="small" />
        </Space>
      ),
    },
    {
      key: 'actions',
      title: 'Actions',
      align: 'right',
      render: () => (
        <Flex align="center" justify="flex-end" gap={8}>
          <Button type="text" icon={<Edit style={{ fontSize: 16 }} />} aria-label="Edit kampanye" />
          <Button type="text" icon={<Copy style={{ fontSize: 16 }} />} aria-label="Salin kampanye" />
          <Dropdown trigger={['click']} placement="bottomRight" menu={{ items: campaignMenuItems }}>
            <Button type="text" icon={<MoreHorizontal style={{ fontSize: 16 }} />} aria-label="Aksi lainnya" />
          </Dropdown>
        </Flex>
      ),
    },
  ];

   return (
    <Space direction="vertical" size={24} style={{ width: '100%' }}>
      <StatCards stats={[
        { label: 'Total Campaigns', value: isLoading ? '…' : String(campaigns?.length ?? 0), icon: Gift },
        { label: 'Rewards Sent', value: isLoading ? '…' : String((campaigns ?? []).reduce((s, c) => s + (c.currentClaims ?? 0), 0)), icon: CheckCircle2 },
        { label: 'Active Campaigns', value: isLoading ? '…' : String((campaigns ?? []).filter((c) => c.status === 'ACTIVE').length), icon: Timer },
        { label: 'Draft Campaigns', value: isLoading ? '…' : String((campaigns ?? []).filter((c) => c.status === 'DRAFT').length), icon: BarChart3 },
      ]} />

      <Card>
        <Flex gap={16} wrap align="center" justify="space-between">
          <Input
            aria-label="Cari kampanye"
            placeholder="Search campaigns..."
            prefix={<Search style={{ fontSize: 16, color: 'var(--ant-color-text-secondary)' }} />}
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
            style={{ flex: 1, minWidth: 200, maxWidth: 384 }}
          />
          <Flex gap={16} wrap>
            <Button icon={<BarChart3 style={{ fontSize: 16 }} />}>
              Performance Report
            </Button>
            <Button type="primary" icon={<Plus style={{ fontSize: 16 }} />}>
              Launch New Campaign
            </Button>
          </Flex>
        </Flex>
      </Card>

      <Card styles={{ body: { padding: 0, overflow: 'hidden' } }}>
        <Table<Promotion>
          columns={columns}
          dataSource={filteredCampaigns}
          rowKey="id"
          pagination={false}
          loading={isLoading}
          locale={{ emptyText: error ? 'Failed to load campaigns' : 'No campaigns found' }}
        />
        <Divider style={{ margin: 0 }} />
        <Flex align="center" justify="space-between" wrap gap={16} style={{ padding: 24 }}>
          <p style={{ margin: 0, fontSize: 12, fontWeight: 700, color: 'var(--ant-color-text-secondary)', textTransform: 'uppercase', letterSpacing: '0.1em' }}>
            Showing <span style={{ color: 'var(--ant-color-text)' }}>{filteredCampaigns.length}</span> campaigns
          </p>
          <Flex align="center" gap={8}>
            <Button icon={<ChevronLeft style={{ fontSize: 16 }} />} aria-label="Halaman sebelumnya" />
            <Button type="primary">1</Button>
            <Button icon={<ChevronRight style={{ fontSize: 16 }} />} aria-label="Halaman berikutnya" />
          </Flex>
        </Flex>
      </Card>
    </Space>
  );
}
function campaignStatusBadge(status: string) {
  switch (status) {
    case 'ACTIVE':
      return <Tag color="green">Active</Tag>;
    case 'PAUSED':
      return <Tag color="gold">Paused</Tag>;
    case 'DRAFT':
      return <Tag>DRAFT</Tag>;
    default:
      return <Tag>{status}</Tag>;
  }
}

function campaignTypeIcon(type: string) {
  switch (type) {
    case 'CASHBACK': return <BadgePercent style={{ fontSize: 16, color: 'var(--ant-color-primary)' }} />;
    case 'SIGNUP_BONUS': return <Gift style={{ fontSize: 16 }} />;
    case 'REFERRAL': return <Users style={{ fontSize: 16, color: 'var(--ant-color-primary)' }} />;
    default: return <TagIcon style={{ fontSize: 16, color: 'var(--ant-color-text-secondary)' }} />;
  }
}
