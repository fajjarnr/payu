'use client';

import React, { useState } from 'react';
import type { CSSProperties } from 'react';
import Image from 'next/image';
import {
  Plus,
  Search,
  MoreHorizontal,
  Edit,
  Trash2,
  Eye,
  CheckCircle2,
  Clock,
  AlertCircle,
  Image as ImageIcon,
  ExternalLink,
  ChevronLeft,
  ChevronRight,
  FileText,
  Gift
} from '@/components/icons';
import { Button, Card, Divider, Dropdown, Flex, Input, Progress, Select, Space, Table, Tag, Typography } from 'antd';
import type { MenuProps, TableColumnsType } from 'antd';
import type { Content } from '@/services';
import { useActiveContent } from '@/hooks/useCMS';
import StatCards from '../_components/StatCards';

const labelStyle: CSSProperties = {
  fontSize: 12,
  fontWeight: 700,
  color: 'var(--ant-color-text-secondary)',
  textTransform: 'uppercase',
  letterSpacing: '0.1em',
};

export default function CMSPage() {
  const [searchTerm, setSearchTerm] = useState('');
  const [activeTab, setActiveTab] = useState<string>('ALL');
  const contentMenuItems: MenuProps['items'] = [
    { key: 'activate', label: (<Space><CheckCircle2 style={{ fontSize: 16, color: 'var(--ant-color-primary)' }} /><Typography.Text strong style={labelStyle}>Activate</Typography.Text></Space>) },
    { key: 'pause', label: (<Space><Clock style={{ fontSize: 16, color: 'var(--ant-color-warning)' }} /><Typography.Text strong style={labelStyle}>Pause</Typography.Text></Space>) },
    { key: 'archive', label: (<Space><Trash2 style={{ fontSize: 16, color: 'var(--ant-color-error)' }} /><Typography.Text strong style={{ ...labelStyle, color: 'var(--ant-color-error)' }}>Archive</Typography.Text></Space>) },
  ];

  const { data: banners, isLoading: bannersLoading, error: bannersError } = useActiveContent('BANNER');
  const { data: promos, isLoading: promosLoading } = useActiveContent('PROMO');
  const { data: alerts, isLoading: alertsLoading } = useActiveContent('ALERT');
  const { data: popups, isLoading: popupsLoading } = useActiveContent('POPUP');
  const isLoading = bannersLoading || promosLoading || alertsLoading || popupsLoading;
  const error = bannersError;
  const allContent = [...(banners ?? []), ...(promos ?? []), ...(alerts ?? []), ...(popups ?? [])];
  const filteredContent = allContent.filter(content => {
    const matchesSearch = content.title.toLowerCase().includes(searchTerm.toLowerCase()) ||
                          content.description.toLowerCase().includes(searchTerm.toLowerCase());
    const matchesTab = activeTab === 'ALL' || content.contentType === activeTab;
    return matchesSearch && matchesTab;
  });

  const columns: TableColumnsType<Content> = [
    {
      key: 'content',
      title: 'Content',
      render: (_, item) => (
        <Flex align="flex-start" gap={16}>
          <div style={{ width: 64, height: 64, borderRadius: 8, background: 'var(--ant-color-fill-tertiary)', overflow: 'hidden', position: 'relative', flexShrink: 0, display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
            {item.imageUrl ? (
              <Image src={item.imageUrl} alt={item.title} fill sizes="64px" style={{ objectFit: 'cover' }} loading="lazy" />
            ) : (
              contentTypeIcon(item.contentType)
            )}
          </div>
          <Space direction="vertical" size={4}>
            <Typography.Text strong style={{ fontSize: 14 }}>{item.title}</Typography.Text>
            <Typography.Paragraph type="secondary" style={{ fontSize: 12, maxWidth: 200, margin: 0 }} ellipsis={{ rows: 2 }}>{item.description}</Typography.Paragraph>
          </Space>
        </Flex>
      ),
    },
    {
      key: 'type',
      title: 'Type',
      render: (_, item) => (
        <Space>
          {contentTypeIcon(item.contentType)}
          <Typography.Text strong style={labelStyle}>{item.contentType}</Typography.Text>
        </Space>
      ),
    },
    { key: 'status', title: 'Status', render: (_, item) => statusBadge(item.status) },
    {
      key: 'schedule',
      title: 'Schedule',
      render: (_, item) => (
        <Space direction="vertical" size={4}>
          <Typography.Text style={{ fontSize: 12 }}>S: {new Date(item.startDate).toLocaleDateString()}</Typography.Text>
          <Typography.Text type="secondary" style={{ fontSize: 12 }}>E: {new Date(item.endDate).toLocaleDateString()}</Typography.Text>
        </Space>
      ),
    },
    {
      key: 'priority',
      title: 'Priority',
      render: (_, item) => (
        <Flex align="center" gap={8}>
          <Progress percent={item.priority * 10} showInfo={false} size="small" style={{ width: 48 }} />
          <Typography.Text strong style={{ fontSize: 12 }}>{item.priority}</Typography.Text>
        </Flex>
      ),
    },
    {
      key: 'actions',
      title: 'Actions',
      align: 'right',
      render: () => (
        <Flex align="center" justify="flex-end" gap={8}>
          <Button type="text" icon={<Eye style={{ fontSize: 16 }} />} aria-label="Lihat detail" />
          <Button type="text" icon={<Edit style={{ fontSize: 16 }} />} aria-label="Edit konten" />
          <Dropdown trigger={['click']} placement="bottomRight" menu={{ items: contentMenuItems }}>
            <Button type="text" icon={<MoreHorizontal style={{ fontSize: 16 }} />} aria-label="Aksi lainnya" />
          </Dropdown>
        </Flex>
      ),
    },
  ];

  return (
    <Space direction="vertical" size={24} style={{ width: '100%' }}>
      <StatCards stats={[
        { label: 'Total Content', value: String(allContent.length), icon: FileText },
        { label: 'Active Now', value: String(allContent.filter((c) => c.status === 'ACTIVE').length), icon: CheckCircle2 },
        { label: 'Scheduled', value: String(allContent.filter((c) => c.status === 'SCHEDULED').length), icon: Clock },
        { label: 'Pending Review', value: String(allContent.filter((c) => c.status === 'DRAFT').length), icon: AlertCircle },
      ]} />

      <Card>
        <Flex gap={16} wrap align="center" justify="space-between">
          <Select
            aria-label="Pilih tab konten"
            value={activeTab}
            onChange={(value: string) => setActiveTab(value)}
            style={{ minWidth: 160 }}
            options={['ALL', 'BANNER', 'PROMO', 'ALERT', 'POPUP'].map((tab) => ({ value: tab, label: tab }))}
          />
          <Flex gap={16} wrap style={{ flex: 1 }} justify="flex-end">
            <Input
              aria-label="Cari konten"
              placeholder="Search content..."
              prefix={<Search style={{ fontSize: 16, color: 'var(--ant-color-text-secondary)' }} />}
              value={searchTerm}
              onChange={(e) => setSearchTerm(e.target.value)}
              style={{ flex: 1, minWidth: 200, maxWidth: 320 }}
            />
            <Button type="primary" icon={<Plus style={{ fontSize: 16 }} />}>
              New Content
            </Button>
          </Flex>
        </Flex>
      </Card>

      <Card styles={{ body: { padding: 0, overflow: 'hidden' } }}>
        <Table<Content>
          columns={columns}
          dataSource={filteredContent}
          rowKey="id"
          pagination={false}
          loading={isLoading}
          locale={{ emptyText: error ? 'Failed to load content' : 'No content found' }}
        />
        <Divider style={{ margin: 0 }} />
        <Flex align="center" justify="space-between" wrap gap={16} style={{ padding: 24 }}>
          <Typography.Text type="secondary" strong style={labelStyle}>
            Showing <Typography.Text strong>{filteredContent.length}</Typography.Text> results
          </Typography.Text>
          <Flex align="center" gap={8}>
            <Button icon={<ChevronLeft style={{ fontSize: 16 }} />} aria-label="Halaman sebelumnya" />
            <Button type="primary">1</Button>
            <Button>2</Button>
            <Button>3</Button>
            <Button icon={<ChevronRight style={{ fontSize: 16 }} />} aria-label="Halaman berikutnya" />
          </Flex>
        </Flex>
      </Card>
    </Space>
  );
}

function statusBadge(status: string) {
  switch (status) {
    case 'ACTIVE':
      return <Tag color="green">Active</Tag>;
    case 'SCHEDULED':
      return <Tag color="gold">Scheduled</Tag>;
    case 'DRAFT':
      return <Tag>DRAFT</Tag>;
    case 'ARCHIVED':
      return <Tag color="red">Archived</Tag>;
    default:
      return <Tag>{status}</Tag>;
  }
}

function contentTypeIcon(type: string) {
  switch (type) {
    case 'BANNER':
      return <ImageIcon style={{ fontSize: 16, color: 'var(--ant-color-primary)' }} />;
    case 'PROMO':
      return <Gift style={{ fontSize: 16, color: 'var(--ant-color-primary)' }} />;
    case 'ALERT':
      return <AlertCircle style={{ fontSize: 16, color: 'var(--ant-color-error)' }} />;
    case 'POPUP':
      return <ExternalLink style={{ fontSize: 16 }} />;
    default:
      return <FileText style={{ fontSize: 16, color: 'var(--ant-color-text-secondary)' }} />;
  }
}
