'use client';

import React, { useState } from 'react';
import type { CSSProperties } from 'react';
import {
  Send,
  Search,
  Users,
  MessageSquare,
  Mail,
  Smartphone,
  Bell,
  CheckCircle2,
  AlertCircle,
  ChevronLeft,
  ChevronRight,
  Plus
} from '@/components/icons';
import { Avatar, Button, Card, Divider, Flex, Input, Space, Table, Tag, Typography } from 'antd';
import type { TableColumnsType } from 'antd';
import type { Notification } from '@/services';
import { useAuthStore } from '@/stores/authStore';
import { useNotifications } from '@/hooks/useNotifications';
import StatCards from '../_components/StatCards';

const labelStyle: CSSProperties = {
  fontSize: 12,
  fontWeight: 700,
  color: 'var(--ant-color-text-secondary)',
  textTransform: 'uppercase',
  letterSpacing: '0.1em',
};

export default function BroadcastPage() {
  const [searchTerm, setSearchTerm] = useState('');
  const { accountId } = useAuthStore();
  const { data: notifications, isLoading, error } = useNotifications(accountId ?? '', 20);

  const filteredBroadcasts = (notifications ?? []).filter((bc) =>
    (bc.title ?? '').toLowerCase().includes(searchTerm.toLowerCase()),
  );

  const columns: TableColumnsType<Notification> = [
    {
      key: 'title',
      title: 'Broadcast Title',
      render: (_, bc) => (
        <Space direction="vertical" size={4}>
          <Typography.Text strong style={{ fontSize: 14, textTransform: 'uppercase' }}>{bc.title}</Typography.Text>
          <Space size={8}>
            <Users style={{ fontSize: 12, color: 'var(--ant-color-text-secondary)' }} />
            <Typography.Text type="secondary" strong style={labelStyle}>{bc.recipient}</Typography.Text>
          </Space>
        </Space>
      ),
    },
    {
      key: 'channels',
      render: (_, bc) => (
        <Avatar
          shape="square"
          size={28}
          style={{ background: 'var(--ant-color-fill-tertiary)', color: 'var(--ant-color-text-secondary)' }}
          icon={channelIcon(bc.channel)}
        />
      ),
    },
    {
      key: 'status',
      title: 'Status',
      render: (_, bc) => broadcastStatusBadge(bc.status),
    },
    {
      key: 'reach',
      title: 'Reach',
      render: () => <Typography.Text strong style={{ fontSize: 12 }}>—</Typography.Text>,
    },
    {
      key: 'engagement',
      title: 'Engagement',
      render: () => <Typography.Text strong style={{ fontSize: 12, color: 'var(--ant-color-primary)' }}>—</Typography.Text>,
    },
    {
      key: 'date',
      title: 'Date',
      align: 'right',
      render: (_, bc) => (
        <Space direction="vertical" size={2} style={{ textAlign: 'right' }}>
          <Typography.Text style={{ fontSize: 12 }}>{new Date(bc.createdAt).toLocaleTimeString()}</Typography.Text>
          <Typography.Text type="secondary" strong style={labelStyle}>{new Date(bc.createdAt).toLocaleDateString()}</Typography.Text>
        </Space>
      ),
    },
  ];

  return (
    <Space direction="vertical" size={24} style={{ width: '100%' }}>
      <StatCards stats={[
        { label: 'Broadcasts Sent', value: isLoading ? '…' : String(notifications?.length ?? 0), icon: Send },
        { label: 'Total Messages', value: '—', icon: Smartphone },
        { label: 'Avg Open Rate', value: '—', icon: CheckCircle2 },
        { label: 'Unsubscribe Rate', value: '—', icon: AlertCircle },
      ]} />

      <Card>
        <Flex gap={16} wrap align="center" justify="space-between">
          <Input
            aria-label="Cari siaran"
            placeholder="Search broadcasts..."
            prefix={<Search style={{ fontSize: 16, color: 'var(--ant-color-text-secondary)' }} />}
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
            style={{ flex: 1, minWidth: 200, maxWidth: 384 }}
          />
          <Flex gap={16} wrap>
            <Button icon={<Users style={{ fontSize: 16 }} />}>
              Targeting Rules
            </Button>
            <Button type="primary" icon={<Plus style={{ fontSize: 16 }} />}>
              Create Broadcast
            </Button>
          </Flex>
        </Flex>
      </Card>

      <Card styles={{ body: { padding: 0, overflow: 'hidden' } }}>
        <Table<Notification>
          columns={columns}
          dataSource={filteredBroadcasts}
          rowKey="id"
          pagination={false}
          loading={isLoading}
          locale={{ emptyText: error ? 'Failed to load broadcasts' : 'No broadcasts found' }}
        />
        <Divider style={{ margin: 0 }} />
        <Flex align="center" justify="space-between" wrap gap={16} style={{ padding: 24 }}>
          <Typography.Text type="secondary" strong style={labelStyle}>
            Multi-channel Delivery Engine Active
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

function broadcastStatusBadge(status: string) {
  switch (status) {
    case 'SENT':
      return <Tag color="green">Sent</Tag>;
    case 'SCHEDULED':
      return <Tag color="gold">Scheduled</Tag>;
    case 'FAILED':
      return <Tag color="red">Failed</Tag>;
    default:
      return <Tag>{status}</Tag>;
  }
}

function channelIcon(channel: string) {
  switch (channel) {
    case 'PUSH': return <Smartphone style={{ fontSize: 12 }} />;
    case 'SMS': return <MessageSquare style={{ fontSize: 12 }} />;
    case 'EMAIL': return <Mail style={{ fontSize: 12 }} />;
    case 'WHATSAPP': return <Bell style={{ fontSize: 12, color: 'var(--ant-color-primary)' }} />;
    default: return null;
  }
}
