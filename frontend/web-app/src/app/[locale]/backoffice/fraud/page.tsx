/* eslint-disable no-restricted-syntax -- display percentage */
'use client';

import { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { BackofficeService, FraudCaseStatus, FraudRiskLevel } from '@/services';
import type { FraudCaseResponse } from '@/services';
import { Link } from '@/lib/navigation';
import { Search, ChevronLeft, ChevronRight, Eye } from '@/components/icons';
import { Button, Card, Divider, Flex, Input, Select, Space, Table, Tag, Typography } from 'antd';
import type { TableColumnsType } from 'antd';

 export default function FraudCasesPage() {
 const [status, setStatus] = useState<string>('');
 const [riskLevel, setRiskLevel] = useState<string>('');
 const [page, setPage] = useState(0);

  const { data: rawCases, isLoading, isError } = useQuery({
   queryKey: ['fraud-cases', status, riskLevel, page],
   queryFn: () => BackofficeService.getFraudCases(status || undefined, riskLevel || undefined, page),
  });
  const cases = Array.isArray(rawCases) ? rawCases : [];
  const criticalCount = cases.filter((c) => c.riskLevel === FraudRiskLevel.CRITICAL).length;
  const columns: TableColumnsType<FraudCaseResponse> = [
    {
      key: 'risk',
      title: 'Risiko',
      render: (_, c) => (
        <Tag color={c.riskLevel === FraudRiskLevel.CRITICAL ? 'red' : c.riskLevel === FraudRiskLevel.HIGH ? 'orange' : c.riskLevel === FraudRiskLevel.MEDIUM ? 'gold' : 'green'}>
          {c.riskLevel}
        </Tag>
      ),
    },
    { key: 'type', title: 'Tipe Kecurangan', render: (_, c) => <Typography.Text strong>{c.fraudType}</Typography.Text> },
    { key: 'amount', title: 'Jumlah', render: (_, c) => <Typography.Text strong style={{ fontVariantNumeric: 'tabular-nums' }}>Rp {Number(c.amount).toLocaleString('id-ID')}</Typography.Text> },
    {
      key: 'status',
      title: 'Status',
      render: (_, c) => <Tag>{c.status}</Tag>,
    },
    {
      key: 'date',
      title: 'Tanggal',
      render: (_, c) => (
        <Typography.Text type="secondary" strong style={{ fontSize: 12 }}>
          {new Date(c.createdAt).toLocaleDateString(undefined, { day: 'numeric', month: 'short', year: 'numeric' })}
        </Typography.Text>
      ),
    },
    {
      key: 'actions',
      title: 'Aksi',
      align: 'right',
      render: (_, c) => (
        <Link href={`/backoffice/fraud/${c.id}`}>
          <Button type="text" size="small" icon={<Eye style={{ fontSize: 16 }} />} aria-label={`Detail kasus ${c.id}`}>
            Detail
          </Button>
        </Link>
      ),
    },
  ];

 return (
  <Space direction="vertical" size={24} style={{ width: '100%' }}>
    <Flex align="flex-start" justify="space-between" gap={24} wrap>
      <Space direction="vertical" size={4}>
        <Typography.Title level={2} style={{ margin: 0 }}>Fraud Monitoring</Typography.Title>
        <Typography.Text type="secondary">Sistem deteksi risiko dan investigasi kecurangan transaksi.</Typography.Text>
      </Space>
      <Tag color="red">Kritis: {isLoading ? '…' : criticalCount}</Tag>
    </Flex>

    <Card>
      <Flex gap={16} wrap>
        <Input
          aria-label="Cari kasus"
          placeholder="Cari kasus..."
          prefix={<Search style={{ fontSize: 16, color: 'var(--ant-color-text-secondary)' }} />}
          style={{ flex: 1, minWidth: 200 }}
        />
        <Select
          aria-label="Filter tingkat risiko"
          value={riskLevel || undefined}
          placeholder="Semua Risiko"
          onChange={(value: string) => setRiskLevel(value ?? '')}
          style={{ minWidth: 176 }}
          options={[{ value: '', label: 'Semua Risiko' }, ...Object.values(FraudRiskLevel).map((s) => ({ value: s, label: s }))]}
        />
        <Select
          aria-label="Filter status"
          value={status || undefined}
          placeholder="Semua Status"
          onChange={(value: string) => setStatus(value ?? '')}
          style={{ minWidth: 176 }}
          options={[{ value: '', label: 'Semua Status' }, ...Object.values(FraudCaseStatus).map((s) => ({ value: s, label: s }))]}
        />
      </Flex>
    </Card>

    <Card styles={{ body: { padding: 0, overflow: 'hidden' } }}>
      <Table<FraudCaseResponse>
        columns={columns}
        dataSource={cases}
        rowKey="id"
        pagination={false}
        loading={isLoading}
        locale={{ emptyText: isError ? 'Akses ditolak — hubungi administrator' : 'Tidak ada kasus ditemukan' }}
      />
      <Divider style={{ margin: 0 }} />
      <Flex align="center" justify="space-between" style={{ padding: '24px 32px' }}>
        <Button
          onClick={() => setPage(p => Math.max(0, p - 1))}
          disabled={page === 0}
          icon={<ChevronLeft style={{ fontSize: 16 }} />}
        >
          Sebelumnya
        </Button>
        <Typography.Text type="secondary" strong style={{ fontSize: 12, letterSpacing: '0.1em', textTransform: 'uppercase' }}>Halaman {page + 1}</Typography.Text>
        <Button
          onClick={() => setPage(p => p + 1)}
          disabled={cases.length < 20}
        >
          Selanjutnya <ChevronRight style={{ fontSize: 16 }} />
        </Button>
      </Flex>
    </Card>
  </Space>
 );
}
