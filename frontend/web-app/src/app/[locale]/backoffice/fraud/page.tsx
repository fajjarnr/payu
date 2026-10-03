/* eslint-disable no-restricted-syntax -- display percentage */
'use client';

import { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { BackofficeService, FraudCaseStatus, FraudRiskLevel } from '@/services';
import type { FraudCaseResponse } from '@/services';
import { Link } from '@/lib/navigation';
import { Search, ChevronLeft, ChevronRight, Eye } from '@/components/icons';
import { Badge, Button, Input, Select, Table } from 'antd';
import type { TableColumnsType } from 'antd';
import clsx from 'clsx';

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
        <Badge
          count={c.riskLevel}
          color={c.riskLevel === FraudRiskLevel.CRITICAL ? 'red' : undefined}
          showZero
          className={clsx(
            "font-bold uppercase tracking-widest",
            c.riskLevel === FraudRiskLevel.HIGH && "[&_sup]:border-orange-500 [&_sup]:text-orange-500 [&_sup]:bg-orange-500/5 [&_sup]:border [&_sup]:px-2.5 [&_sup]:py-0.5 [&_sup]:rounded-full",
            c.riskLevel === FraudRiskLevel.MEDIUM && "[&_sup]:border-amber-500 [&_sup]:text-amber-500 [&_sup]:bg-amber-500/5 [&_sup]:border [&_sup]:px-2.5 [&_sup]:py-0.5 [&_sup]:rounded-full",
            c.riskLevel === FraudRiskLevel.LOW && "[&_sup]:border-emerald-500 [&_sup]:text-emerald-500 [&_sup]:bg-emerald-500/5 [&_sup]:border [&_sup]:px-2.5 [&_sup]:py-0.5 [&_sup]:rounded-full",
          )}
        />
      ),
    },
    { key: 'type', title: 'Tipe Kecurangan', render: (_, c) => <span className="font-bold text-foreground">{c.fraudType}</span> },
    { key: 'amount', title: 'Jumlah', render: (_, c) => <span className="font-bold tabular-nums">Rp {Number(c.amount).toLocaleString('id-ID')}</span> },
    {
      key: 'status',
      title: 'Status',
      render: (_, c) => <Badge count={c.status} showZero className="font-bold uppercase tracking-widest opacity-70 [&_sup]:px-2.5 [&_sup]:py-0.5 [&_sup]:rounded-full" />,
    },
    {
      key: 'date',
      title: 'Tanggal',
      render: (_, c) => (
        <span className="text-muted-foreground font-bold text-xs">
          {new Date(c.createdAt).toLocaleDateString(undefined, { day: 'numeric', month: 'short', year: 'numeric' })}
        </span>
      ),
    },
    {
      key: 'actions',
      title: 'Aksi',
      align: 'right',
      render: (_, c) => (
        <Link href={`/backoffice/fraud/${c.id}`}>
          <Button type="text" size="small" className="h-9 gap-2 font-bold uppercase tracking-widest">
            <Eye className="h-4 w-4" /> Detail
          </Button>
        </Link>
      ),
    },
  ];

 return (
  <div className="space-y-6">
    <div className="flex flex-col md:flex-row justify-between items-start md:items-end gap-6 mb-8">
      <div>
        <h2 className="text-3xl font-bold text-foreground tracking-tight">Fraud Monitoring</h2>
        <p className="text-sm text-muted-foreground font-medium mt-1">Sistem deteksi risiko dan investigasi kecurangan transaksi.</p>
      </div>
      <div className="flex items-center gap-3">
        <div className="bg-rose-500/10 px-4 py-2 rounded-lg border border-rose-500/20">
          <span className="text-xs font-bold text-rose-500 tracking-widest uppercase">Kritis: {isLoading ? '…' : criticalCount}</span>
        </div>
      </div>
    </div>

    <div className="flex flex-col md:flex-row gap-4 bg-card p-4 rounded-2xl border border-border shadow-sm">
      <div className="relative flex-1 flex items-center">
        <Search className="absolute left-4 top-1/2 -translate-y-1/2 h-4 w-4 text-muted-foreground z-10 pointer-events-none" />
        <Input placeholder="Cari kasus..." className="pl-12 h-12 w-full" />
      </div>
      <div className="flex gap-4">
        <Select
          value={riskLevel || undefined}
          placeholder="Semua Risiko"
          onChange={(value: string) => setRiskLevel(value ?? '')}
          className="min-w-44 h-12"
          options={[{ value: '', label: 'Semua Risiko' }, ...Object.values(FraudRiskLevel).map((s) => ({ value: s, label: s }))]}
        />
        <Select
          value={status || undefined}
          placeholder="Semua Status"
          onChange={(value: string) => setStatus(value ?? '')}
          className="min-w-44 h-12"
          options={[{ value: '', label: 'Semua Status' }, ...Object.values(FraudCaseStatus).map((s) => ({ value: s, label: s }))]}
        />
      </div>
    </div>

    <div className="bg-card rounded-2xl border border-border shadow-sm overflow-hidden">
      <Table<FraudCaseResponse>
        columns={columns}
        dataSource={cases}
        rowKey="id"
        pagination={false}
        loading={isLoading}
        locale={{ emptyText: isError ? 'Akses ditolak — hubungi administrator' : 'Tidak ada kasus ditemukan' }}
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
          disabled={cases.length < 20}
          className="h-10 px-6 gap-2 font-bold uppercase tracking-widest"
        >
          Selanjutnya <ChevronRight className="h-4 w-4" />
        </Button>
      </div>
    </div>
  </div>
 );
}
