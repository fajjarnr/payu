'use client';

import { useState } from 'react';
import type { CSSProperties } from 'react';
import { useQuery } from '@tanstack/react-query';
import { BackofficeService, BackofficeKycStatus } from '@/services';
import type { KycReviewResponse } from '@/services';
import { Link } from '@/lib/navigation';
import { Search, ChevronLeft, ChevronRight } from '@/components/icons';
import { Avatar, Button, Card, Divider, Flex, Input, Select, Space, Table, Tag, Typography } from 'antd';
import type { TableColumnsType } from 'antd';

const labelStyle: CSSProperties = {
  fontSize: 12,
  fontWeight: 700,
  color: 'var(--ant-color-text-secondary)',
  textTransform: 'uppercase',
  letterSpacing: '0.1em',
};

export default function KycReviewsPage() {
 const [status, setStatus] = useState<string>('');
 const [page, setPage] = useState(0);

  const { data: rawReviews, isLoading, isError } = useQuery({
   queryKey: ['kyc-reviews', status, page],
   queryFn: () => BackofficeService.getKycReviews(status || undefined, page),
  });
  const reviews = Array.isArray(rawReviews) ? rawReviews : [];
  const pendingCount = reviews.filter((r) => r.status === BackofficeKycStatus.PENDING).length;
  const columns: TableColumnsType<KycReviewResponse> = [
    {
      key: 'customer',
      title: 'Nasabah',
      render: (_, review) => (
        <Flex align="center" gap={12}>
          <Avatar style={{ background: 'var(--ant-color-fill-tertiary)', color: 'var(--ant-color-text-secondary)' }}>
            {review.fullName.slice(0, 2).toUpperCase()}
          </Avatar>
          <Space direction="vertical" size={0}>
            <Typography.Text strong style={{ fontSize: 14 }}>{review.fullName}</Typography.Text>
            <Typography.Text type="secondary" strong style={{ fontSize: 12, letterSpacing: '0.1em', textTransform: 'uppercase' }}>{review.userId}</Typography.Text>
          </Space>
        </Flex>
      ),
    },
    {
      key: 'document',
      title: 'Dokumen',
      render: (_, review) => (
        <Space direction="vertical" size={0}>
          <Typography.Text strong style={{ fontSize: 14 }}>{review.documentType}</Typography.Text>
          <Typography.Text type="secondary" strong style={{ fontSize: 12, letterSpacing: '0.1em', textTransform: 'uppercase' }}>{review.documentNumber}</Typography.Text>
        </Space>
      ),
    },
    {
      key: 'date',
      title: 'Tanggal Kirim',
      render: (_, review) => (
        <Typography.Text type="secondary" strong style={{ fontSize: 12 }}>
          {new Date(review.createdAt).toLocaleDateString(undefined, { day: 'numeric', month: 'short', year: 'numeric' })}
        </Typography.Text>
      ),
    },
    {
      key: 'status',
      title: 'Status',
      render: (_, review) => (
        <Tag color={review.status === BackofficeKycStatus.REJECTED ? 'red' : review.status === BackofficeKycStatus.APPROVED ? 'green' : 'gold'}>
          {review.status}
        </Tag>
      ),
    },
    {
      key: 'actions',
      title: 'Aksi',
      align: 'right',
      render: (_, review) => (
        <Link href={`/backoffice/kyc/${review.id}`}>
          <Button type="text" size="small" icon={<ChevronRight style={{ fontSize: 16 }} />} aria-label={`Review ${review.fullName}`}>
            Review
          </Button>
        </Link>
      ),
    },
  ];

 return (
  <Space direction="vertical" size={24} style={{ width: '100%' }}>
    <Flex align="flex-start" justify="space-between" gap={24} wrap>
      <Space direction="vertical" size={4}>
        <Typography.Title level={2} style={{ margin: 0 }}>KYC Reviews</Typography.Title>
        <Typography.Text type="secondary">Review verifikasi identitas dan dokumen nasabah.</Typography.Text>
      </Space>
      <Tag color="gold">Tertunda: {isLoading ? '…' : pendingCount}</Tag>
    </Flex>

    <Card>
      <Flex gap={16} wrap>
        <Input
          aria-label="Cari nasabah atau nomor dokumen"
          placeholder="Cari nasabah atau nomor dokumen..."
          prefix={<Search style={{ fontSize: 16, color: 'var(--ant-color-text-secondary)' }} />}
          style={{ flex: 1, minWidth: 200 }}
        />
        <Select
          aria-label="Filter status"
          value={status || undefined}
          placeholder="Semua Status"
          onChange={(value: string) => setStatus(value ?? '')}
          style={{ minWidth: 176 }}
          options={[{ value: '', label: 'Semua Status' }, ...Object.values(BackofficeKycStatus).map((s) => ({ value: s, label: s }))]}
        />
      </Flex>
    </Card>

    <Card styles={{ body: { padding: 0, overflow: 'hidden' } }}>
      <Table<KycReviewResponse>
        columns={columns}
        dataSource={reviews}
        rowKey="id"
        pagination={false}
        loading={isLoading}
        locale={{ emptyText: isError ? 'Akses ditolak — hubungi administrator' : 'Tidak ada review ditemukan' }}
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
        <Typography.Text type="secondary" strong style={labelStyle}>Halaman {page + 1}</Typography.Text>
        <Button
          onClick={() => setPage(p => p + 1)}
          disabled={reviews.length < 20}
        >
          Selanjutnya <ChevronRight style={{ fontSize: 16 }} />
        </Button>
      </Flex>
    </Card>
  </Space>
 );
}
