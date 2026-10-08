'use client';

import React, { useState } from 'react';
import type { CSSProperties } from 'react';
import {
  TrendingUp,
  Search,
  RefreshCw,
  ArrowRightLeft,
  CheckCircle2,
  Edit,
  History,
  Lock,
  ChevronLeft,
  ChevronRight
} from '@/components/icons';
import { Avatar, Button, Card, Divider, Flex, Input, Space, Table, Tag, Typography } from 'antd';
import type { TableColumnsType } from 'antd';
import type { FxRateResponse } from '@/services';
import { useAllFxRates } from '@/hooks/useFx';
import StatCards from '../_components/StatCards';

const labelStyle: CSSProperties = {
  fontSize: 12,
  fontWeight: 700,
  color: 'var(--ant-color-text-secondary)',
  textTransform: 'uppercase',
  letterSpacing: '0.1em',
};

export default function FxRatesAdminPage() {
  const [searchTerm, setSearchTerm] = useState('');
  const { data: fxRates, isLoading, error } = useAllFxRates();
  const filteredRates = (fxRates ?? []).filter((fx) => `${fx.fromCurrency}/${fx.toCurrency}`.toLowerCase().includes(searchTerm.toLowerCase()));
  const marketLive = !error && (fxRates?.length ?? 0) > 0;
  const columns: TableColumnsType<FxRateResponse> = [
    {
      key: 'pair',
      title: 'Currency Pair',
      render: (_, fx) => (
        <Flex align="center" gap={12}>
          <Avatar
            shape="square"
            size={40}
            style={{ background: 'var(--ant-color-primary-bg)', color: 'var(--ant-color-primary)' }}
            icon={<ArrowRightLeft style={{ fontSize: 20 }} />}
          />
          <Typography.Text strong style={{ fontSize: 14, letterSpacing: '0.1em' }}>{fx.fromCurrency}/{fx.toCurrency}</Typography.Text>
        </Flex>
      ),
    },
    {
      key: 'rate',
      title: 'Current Rate',
      render: (_, fx) => <Typography.Text code strong style={{ fontSize: 14 }}>{fx.rate}</Typography.Text>,
    },
    {
      key: 'trend',
      title: 'Trend',
      render: () => <Typography.Text type="secondary" strong style={labelStyle}>—</Typography.Text>,
    },
    {
      key: 'spread',
      title: 'Spread (%)',
      render: () => <Typography.Text strong style={{ fontSize: 12 }}>—</Typography.Text>,
    },
    {
      key: 'mode',
      title: 'Sync Mode',
      render: () => <Tag color="green">Auto</Tag>,
    },
    {
      key: 'updated',
      title: 'Last Update',
      render: (_, fx) => (
        <Space direction="vertical" size={2}>
          <Typography.Text style={{ fontSize: 12 }}>{new Date(fx.validFrom).toLocaleTimeString()}</Typography.Text>
          <Typography.Text type="secondary" strong style={labelStyle}>{new Date(fx.validFrom).toLocaleDateString()}</Typography.Text>
        </Space>
      ),
    },
    {
      key: 'actions',
      title: 'Actions',
      align: 'right',
      render: () => (
        <Flex align="center" justify="flex-end" gap={8}>
          <Button type="text" icon={<Edit style={{ fontSize: 16 }} />} aria-label="Edit kurs" />
          <Button type="text" icon={<Lock style={{ fontSize: 16 }} />} aria-label="Kunci kurs" />
        </Flex>
      ),
    },
  ];
  return (
    <Space direction="vertical" size={24} style={{ width: '100%' }}>
      <StatCards stats={[
        { label: 'Active Currencies', value: isLoading ? '…' : String(fxRates?.length ?? 0), icon: TrendingUp },
        { label: 'Auto-Sync Provider', value: isLoading ? '…' : (error ? 'DOWN' : 'LIVE'), icon: RefreshCw },
        { label: 'Manual Overrides', value: '—', icon: Lock },
        { label: 'Market Status', value: isLoading ? '…' : (marketLive ? 'LIVE' : 'DEGRADED'), icon: CheckCircle2 },
      ]} />

      <Card>
        <Flex gap={16} wrap align="center" justify="space-between">
          <Input
            aria-label="Cari pasangan mata uang"
            placeholder="Search currency pairs..."
            prefix={<Search style={{ fontSize: 16, color: 'var(--ant-color-text-secondary)' }} />}
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
            style={{ flex: 1, minWidth: 200, maxWidth: 384 }}
          />
          <Flex gap={16} wrap>
            <Button icon={<History style={{ fontSize: 16 }} />}>
              Rate History
            </Button>
            <Button type="primary" icon={<RefreshCw style={{ fontSize: 16 }} />}>
              Sync All Rates
            </Button>
          </Flex>
        </Flex>
      </Card>

      <Card styles={{ body: { padding: 0, overflow: 'hidden' } }}>
        <Table<FxRateResponse>
          columns={columns}
          dataSource={filteredRates}
          rowKey="id"
          pagination={false}
          loading={isLoading}
          locale={{ emptyText: error ? 'Failed to load rates' : 'No rates found' }}
        />
        <Divider style={{ margin: 0 }} />
        <Flex align="center" justify="space-between" wrap gap={16} style={{ padding: 24 }}>
          <Typography.Text type="secondary" strong style={labelStyle}>
            Market Connector Status: {isLoading ? '…' : marketLive ? <Typography.Text strong style={{ color: 'var(--ant-color-primary)' }}>Connected</Typography.Text> : <Typography.Text strong style={{ color: 'var(--ant-color-error)' }}>Degraded</Typography.Text>}
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
