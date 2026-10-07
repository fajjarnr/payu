'use client';
import { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { BackofficeService, CustomerCaseStatus, CustomerCasePriority } from '@/services';
import type { CustomerCaseResponse } from '@/services';
import { Link } from '@/lib/navigation';
import { Search, ChevronLeft, ChevronRight, MessageSquare } from '@/components/icons';
import { Badge, Button, Input, Select, Table } from 'antd';
import type { TableColumnsType } from 'antd';
import clsx from 'clsx';

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
      render: (_, c) => <span className="font-bold text-muted-foreground tabular-nums">#{c.caseNumber}</span>,
    },
    {
      key: 'subject',
      title: 'Subjek & Nasabah',
      render: (_, c) => (
        <div>
          <div className="text-sm font-bold text-foreground">{c.subject}</div>
          <div className="text-xs font-bold text-muted-foreground tracking-widest uppercase">{c.userId}</div>
        </div>
      ),
    },
    {
      key: 'priority',
      title: 'Prioritas',
      render: (_, c) => (
        <Badge
          count={c.priority}
          color={c.priority === CustomerCasePriority.URGENT ? 'red' : undefined}
          showZero
          className={clsx(
            "font-bold uppercase tracking-widest",
            c.priority === CustomerCasePriority.HIGH && "[&_sup]:border-accent [&_sup]:text-accent [&_sup]:bg-accent/5 [&_sup]:border [&_sup]:px-2.5 [&_sup]:py-0.5 [&_sup]:rounded-full",
            c.priority === CustomerCasePriority.MEDIUM && "[&_sup]:border-primary [&_sup]:text-primary [&_sup]:bg-primary/5 [&_sup]:border [&_sup]:px-2.5 [&_sup]:py-0.5 [&_sup]:rounded-full",
            c.priority === CustomerCasePriority.LOW && "[&_sup]:border-text-secondary [&_sup]:text-text-secondary [&_sup]:bg-text-secondary/5 [&_sup]:border [&_sup]:px-2.5 [&_sup]:py-0.5 [&_sup]:rounded-full",
          )}
        />
      ),
    },
    {
      key: 'status',
      title: 'Status',
      render: (_, c) => (
        <Badge
          count={c.status}
          showZero
          className={clsx(
            "font-bold uppercase tracking-widest",
            c.status === CustomerCaseStatus.OPEN && "[&_sup]:text-secondary [&_sup]:bg-secondary/5 [&_sup]:border-secondary/10 [&_sup]:border [&_sup]:px-2.5 [&_sup]:py-0.5 [&_sup]:rounded-full",
            c.status === CustomerCaseStatus.RESOLVED && "[&_sup]:text-primary-dark [&_sup]:bg-primary-dark/5 [&_sup]:border-primary-dark/10 [&_sup]:border [&_sup]:px-2.5 [&_sup]:py-0.5 [&_sup]:rounded-full",
          )}
        />
      ),
    },
    {
      key: 'actions',
      title: 'Aksi',
      align: 'right',
      render: (_, c) => (
        <Link href={`/backoffice/customers/${c.id}`}>
          <Button type="text" size="small" className="min-h-[44px] gap-2 font-bold uppercase tracking-widest">
            <MessageSquare className="h-4 w-4" /> Buka
          </Button>
        </Link>
      ),
    },
  ];

 return (
    <div className="space-y-6">
      <div className="flex flex-col md:flex-row justify-between items-start md:items-end gap-6 mb-8">
        <div>
          <h2 className="text-3xl font-bold text-foreground tracking-tight">Customer Operations</h2>
          <p className="text-sm text-muted-foreground font-medium mt-1">Kelola tiket dukungan, keluhan, dan bantuan nasabah.</p>
        </div>
        <div className="flex items-center gap-3">
          <div className="bg-primary/10 px-4 py-2 rounded-lg border border-primary/20">
            <span className="text-xs font-bold text-primary tracking-widest uppercase">Open: {isLoading ? '…' : openCount}</span>
          </div>
        </div>
      </div>

      <div className="flex flex-col md:flex-row gap-4 bg-card p-4 rounded-2xl border border-border shadow-sm">
        <div className="relative flex-1 flex items-center">
          <Search className="absolute left-4 top-1/2 -translate-y-1/2 h-4 w-4 text-muted-foreground z-10 pointer-events-none" />
          <Input aria-label="Cari tiket atau ID nasabah" placeholder="Cari tiket atau ID nasabah..." className="pl-12 h-12 w-full" />
        </div>
        <div className="flex gap-4">
          <Select
            aria-label="Filter prioritas"
            value={priority || undefined}
            placeholder="Semua Prioritas"
            onChange={(value: string) => setPriority(value ?? '')}
            className="min-w-44 h-12"
            options={[{ value: '', label: 'Semua Prioritas' }, ...Object.values(CustomerCasePriority).map((s) => ({ value: s, label: s }))]}
          />
          <Select
            aria-label="Filter status"
            value={status || undefined}
            placeholder="Semua Status"
            onChange={(value: string) => setStatus(value ?? '')}
            className="min-w-44 h-12"
            options={[{ value: '', label: 'Semua Status' }, ...Object.values(CustomerCaseStatus).map((s) => ({ value: s, label: s }))]}
          />
        </div>
      </div>

      <div className="bg-card rounded-2xl border border-border shadow-sm overflow-hidden">
        <Table<CustomerCaseResponse>
          columns={columns}
          dataSource={cases}
          rowKey="id"
          pagination={false}
          loading={isLoading}
          locale={{ emptyText: isError ? 'Akses ditolak — hubungi administrator' : 'Tidak ada tiket ditemukan' }}
        />

        <div className="px-8 py-6 border-t border-border flex justify-between items-center bg-muted/10">
          <Button
            onClick={() => setPage(p => Math.max(0, p - 1))}
            disabled={page === 0}
            className="h-11 px-6 gap-2 font-bold uppercase tracking-widest"
          >
            <ChevronLeft className="h-4 w-4" /> Sebelumnya
          </Button>
          <span className="text-xs font-bold text-muted-foreground tracking-widest uppercase">Halaman {page + 1}</span>
          <Button
            onClick={() => setPage(p => p + 1)}
            disabled={cases.length < 20}
            className="h-11 px-6 gap-2 font-bold uppercase tracking-widest"
          >
            Selanjutnya <ChevronRight className="h-4 w-4" />
          </Button>
        </div>
      </div>
    </div>
  );
}
