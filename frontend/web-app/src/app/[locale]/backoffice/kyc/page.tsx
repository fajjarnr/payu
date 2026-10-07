'use client';

import { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { BackofficeService, BackofficeKycStatus } from '@/services';
import type { KycReviewResponse } from '@/services';
import { Link } from '@/lib/navigation';
import { Search, ChevronLeft, ChevronRight } from '@/components/icons';
import { Badge, Button, Input, Select, Table } from 'antd';
import type { TableColumnsType } from 'antd';
import clsx from 'clsx';

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
        <div className="flex items-center gap-3">
          <div className="h-10 w-10 rounded-full bg-muted flex items-center justify-center font-bold text-xs text-muted-foreground border border-border">
            {review.fullName.slice(0, 2).toUpperCase()}
          </div>
          <div>
            <div className="text-sm font-bold text-foreground">{review.fullName}</div>
            <div className="text-xs font-bold text-muted-foreground tracking-widest uppercase">{review.userId}</div>
          </div>
        </div>
      ),
    },
    {
      key: 'document',
      title: 'Dokumen',
      render: (_, review) => (
        <div>
          <div className="text-sm font-bold text-foreground">{review.documentType}</div>
          <div className="text-xs font-bold text-muted-foreground tracking-widest uppercase">{review.documentNumber}</div>
        </div>
      ),
    },
    {
      key: 'date',
      title: 'Tanggal Kirim',
      render: (_, review) => (
        <span className="text-muted-foreground font-bold text-xs">
          {new Date(review.createdAt).toLocaleDateString(undefined, { day: 'numeric', month: 'short', year: 'numeric' })}
        </span>
      ),
    },
    {
      key: 'status',
      title: 'Status',
      render: (_, review) => (
        <Badge
          count={review.status}
          color={review.status === BackofficeKycStatus.REJECTED ? 'red' : undefined}
          showZero
          className={clsx(
            "font-bold uppercase tracking-widest",
            review.status === BackofficeKycStatus.APPROVED && "[&_sup]:border-primary [&_sup]:text-primary [&_sup]:bg-primary/5 [&_sup]:border [&_sup]:px-2.5 [&_sup]:py-0.5 [&_sup]:rounded-full",
            review.status === BackofficeKycStatus.PENDING && "[&_sup]:border-warning [&_sup]:text-warning [&_sup]:bg-warning/5 [&_sup]:border [&_sup]:px-2.5 [&_sup]:py-0.5 [&_sup]:rounded-full",
          )}
        />
      ),
    },
    {
      key: 'actions',
      title: 'Aksi',
      align: 'right',
      render: (_, review) => (
        <Link href={`/backoffice/kyc/${review.id}`}>
          <Button type="text" size="small" className="min-h-[44px] gap-2 font-bold uppercase tracking-widest group-hover:bg-primary group-hover:text-surface">
            Review <ChevronRight className="h-4 w-4" />
          </Button>
        </Link>
      ),
    },
  ];

 return (
  <div className="space-y-6">
    <div className="flex flex-col md:flex-row justify-between items-start md:items-end gap-6 mb-8">
      <div>
        <h2 className="text-3xl font-bold text-foreground tracking-tight">KYC Reviews</h2>
        <p className="text-sm text-muted-foreground font-medium mt-1">Review verifikasi identitas dan dokumen nasabah.</p>
      </div>
      <div className="flex items-center gap-3">
        <div className="bg-warning/10 px-4 py-2 rounded-lg border border-warning/20">
          <span className="text-xs font-bold text-warning tracking-widest uppercase">Tertunda: {isLoading ? '…' : pendingCount}</span>
        </div>
      </div>
    </div>

    <div className="flex flex-col md:flex-row gap-4 bg-card p-4 rounded-2xl border border-border shadow-sm">
      <div className="relative flex-1 flex items-center">
        <Search className="absolute left-4 top-1/2 -translate-y-1/2 h-4 w-4 text-muted-foreground z-10 pointer-events-none" />
        <Input aria-label="Cari nasabah atau nomor dokumen" placeholder="Cari nasabah atau nomor dokumen..." className="pl-12 h-12 w-full" />
      </div>
      <div className="flex gap-4">
        <Select
          aria-label="Filter status"
          value={status || undefined}
          placeholder="Semua Status"
          onChange={(value: string) => setStatus(value ?? '')}
          className="min-w-44 h-12"
          options={[{ value: '', label: 'Semua Status' }, ...Object.values(BackofficeKycStatus).map((s) => ({ value: s, label: s }))]}
        />
      </div>
    </div>

    <div className="bg-card rounded-2xl border border-border shadow-sm overflow-hidden">
      <Table<KycReviewResponse>
        columns={columns}
        dataSource={reviews}
        rowKey="id"
        pagination={false}
        loading={isLoading}
        locale={{ emptyText: isError ? 'Akses ditolak — hubungi administrator' : 'Tidak ada review ditemukan' }}
      />

      <div className="px-8 py-6 border-t border-border flex justify-between items-center bg-muted/10">
        <Button
          onClick={() => setPage(p => Math.max(0, p - 1))}
          disabled={page === 0}
          className="h-10 px-6 gap-2 font-bold uppercase tracking-widest"
        >
          <ChevronLeft className="h-4 w-4" /> Sebelumnya
        </Button>
        <span className="text-xs font-bold text-muted-foreground tracking-widest uppercase">Halaman {page + 1}</span>
        <Button
          onClick={() => setPage(p => p + 1)}
          disabled={reviews.length < 20}
          className="h-10 px-6 gap-2 font-bold uppercase tracking-widest"
        >
          Selanjutnya <ChevronRight className="h-4 w-4" />
        </Button>
      </div>
    </div>
  </div>
 );
}
