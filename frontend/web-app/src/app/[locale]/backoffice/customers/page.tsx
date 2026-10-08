'use client';

import { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { BackofficeService, CustomerCaseStatus, CustomerCasePriority } from '@/services';
import type { CustomerCaseResponse } from '@/services';
import { Link } from '@/lib/navigation';
import { Search, ChevronLeft, ChevronRight, MessageSquare } from '@/components/icons';
import { Button, Card, Divider, Flex, Input, Select, Space, Table, Tag, Typography } from 'antd';
import type { TableColumnsType } from 'antd';

export default function CustomerCasesPage() {
 const [status, setStatus] = useState<string>('');
 const [priority, setPriority] = useState<string>('');
 const [page, setPage] = useState(0);

  const { data: rawCases, isLoading, isError } = useQuery({
   queryKey: ['customer-cases', status, priority, page],
   queryFn: () => BackofficeService.getCustomerCases(status || undefined, priority || undefined, page),
  });
  const cases = Array.isArray(rawCases) ? rawCases : [];
  const openCount = cases.filter((c) => c.status === CustomerCaseStatus.OPEN).length;
  const columns: TableColumnsType<CustomerCaseResponse> = [
    {
      key: 'caseNumber',
      title: 'No. Tiket',
      render: (_, c) => <Typography.Text type="secondary" strong style={{ fontVariantNumeric: 'tabular-nums' }}>#{c.caseNumber}</Typography.Text>,
    },
    {
      key: 'subject',
      title: 'Subjek & Nasabah',
      render: (_, c) => (
        <Space direction="vertical" size={0}>
          <Typography.Text strong style={{ fontSize: 14 }}>{c.subject}</Typography.Text>
          <Typography.Text type="secondary" strong style={{ fontSize: 12, letterSpacing: '0.1em', textTransform: 'uppercase' }}>{c.userId}</Typography.Text>
        </Space>
      ),
    },
    {
      key: 'priority',
      title: 'Prioritas',
      render: (_, c) => (
        <Tag color={c.priority === CustomerCasePriority.URGENT ? 'red' : c.priority === CustomerCasePriority.HIGH ? 'orange' : c.priority === CustomerCasePriority.MEDIUM ? 'blue' : undefined}>
          {c.priority}
        </Tag>
      ),
    },
    {
      key: 'status',
      title: 'Status',
      render: (_, c) => (
        <Tag color={c.status === CustomerCaseStatus.OPEN ? 'blue' : c.status === CustomerCaseStatus.RESOLVED ? 'green' : undefined}>
          {c.status}
        </Tag>
      ),
    },
    {
      key: 'actions',
      title: 'Aksi',
      align: 'right',
      render: (_, c) => (
        <Link href={`/backoffice/customers/${c.id}`}>
          <Button type="text" size="small" icon={<MessageSquare style={{ fontSize: 16 }} />} aria-label={`Buka tiket ${c.caseNumber}`}>
            Buka
          </Button>
        </Link>
      ),
    },
  ];

 return (
    <Space direction="vertical" size={24} style={{ width: '100%' }}>
      <Flex align="flex-start" justify="space-between" gap={24} wrap>
        <Space direction="vertical" size={4}>
          <Typography.Title level={2} style={{ margin: 0 }}>Customer Operations</Typography.Title>
          <Typography.Text type="secondary">Kelola tiket dukungan, keluhan, dan bantuan nasabah.</Typography.Text>
        </Space>
        <Tag color="blue">Open: {isLoading ? '…' : openCount}</Tag>
      </Flex>

      <Card>
        <Flex gap={16} wrap>
          <Input
            aria-label="Cari tiket atau ID nasabah"
            placeholder="Cari tiket atau ID nasabah..."
            prefix={<Search style={{ fontSize: 16, color: 'var(--ant-color-text-secondary)' }} />}
            style={{ flex: 1, minWidth: 200 }}
          />
          <Select
            aria-label="Filter prioritas"
            value={priority || undefined}
            placeholder="Semua Prioritas"
            onChange={(value: string) => setPriority(value ?? '')}
            style={{ minWidth: 176 }}
            options={[{ value: '', label: 'Semua Prioritas' }, ...Object.values(CustomerCasePriority).map((s) => ({ value: s, label: s }))]}
          />
          <Select
            aria-label="Filter status"
            value={status || undefined}
            placeholder="Semua Status"
            onChange={(value: string) => setStatus(value ?? '')}
            style={{ minWidth: 176 }}
            options={[{ value: '', label: 'Semua Status' }, ...Object.values(CustomerCaseStatus).map((s) => ({ value: s, label: s }))]}
          />
        </Flex>
      </Card>

      <Card styles={{ body: { padding: 0, overflow: 'hidden' } }}>
        <Table<CustomerCaseResponse>
          columns={columns}
          dataSource={cases}
          rowKey="id"
          pagination={false}
          loading={isLoading}
          locale={{ emptyText: isError ? 'Akses ditolak — hubungi administrator' : 'Tidak ada tiket ditemukan' }}
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
