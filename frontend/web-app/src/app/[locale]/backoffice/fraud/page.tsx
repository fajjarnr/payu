/* eslint-disable no-restricted-syntax -- display percentage */
'use client';

import { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { BackofficeService, FraudCaseStatus, FraudRiskLevel } from '@/services';
import { Link } from '@/lib/navigation';
import { Button } from '@/components/ui/button';
import { Badge } from '@/components/ui/badge';
import { Table, TableHeader, TableRow, TableHead, TableBody, TableCell } from '@/components/ui/table';
import { Search, ChevronLeft, ChevronRight, Eye } from 'lucide-react';
import { Input } from '@/components/ui/input';
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
      <div className="relative flex-1">
        <Search className="absolute left-4 top-1/2 -translate-y-1/2 h-4 w-4 text-muted-foreground" />
        <Input placeholder="Cari kasus..." className="pl-12 h-12" />
      </div>
      <div className="flex gap-4">
        <select
          value={riskLevel}
          onChange={(e) => setRiskLevel(e.target.value)}
          className="h-12 rounded-xl border-border bg-muted/20 px-4 text-sm font-bold tracking-widest uppercase focus:ring-2 focus:ring-primary/20 outline-none"
        >
          <option value="">Semua Risiko</option>
          {Object.values(FraudRiskLevel).map((s) => (
            <option key={s} value={s}>{s}</option>
          ))}
        </select>
        <select
          value={status}
          onChange={(e) => setStatus(e.target.value)}
          className="h-12 rounded-xl border-border bg-muted/20 px-4 text-sm font-bold tracking-widest uppercase focus:ring-2 focus:ring-primary/20 outline-none"
        >
          <option value="">Semua Status</option>
          {Object.values(FraudCaseStatus).map((s) => (
            <option key={s} value={s}>{s}</option>
          ))}
        </select>
      </div>
    </div>

    <div className="bg-card rounded-2xl border border-border shadow-sm overflow-hidden">
      <Table>
        <TableHeader className="bg-muted/30">
          <TableRow>
            <TableHead className="text-xs font-bold tracking-widest uppercase">Risiko</TableHead>
            <TableHead className="text-xs font-bold tracking-widest uppercase">Tipe Kecurangan</TableHead>
            <TableHead className="text-xs font-bold tracking-widest uppercase">Jumlah</TableHead>
            <TableHead className="text-xs font-bold tracking-widest uppercase">Status</TableHead>
            <TableHead className="text-xs font-bold tracking-widest uppercase">Tanggal</TableHead>
            <TableHead className="text-right text-xs font-bold tracking-widest uppercase">Aksi</TableHead>
          </TableRow>
        </TableHeader>
        <TableBody>
          {isLoading ? (
            <TableRow>
              <TableCell colSpan={6} className="h-40 text-center text-muted-foreground font-bold tracking-widest uppercase">Memuat data...</TableCell>
            </TableRow>
          ) : isError ? (
            <TableRow>
              <TableCell colSpan={6} className="h-40 text-center text-muted-foreground font-bold tracking-widest uppercase">Akses ditolak — hubungi administrator</TableCell>
            </TableRow>
          ) : cases.length === 0 ? (
            <TableRow>
              <TableCell colSpan={6} className="h-40 text-center text-muted-foreground font-bold tracking-widest uppercase">Tidak ada kasus ditemukan</TableCell>
            </TableRow>
          ) : (
            cases.map((c) => (
              <TableRow key={c.id} className="group cursor-pointer">
                <TableCell>
                  <Badge
                    variant={c.riskLevel === FraudRiskLevel.CRITICAL ? "destructive" : "outline"}
                    className={clsx(
                      "font-bold uppercase tracking-widest",
                      c.riskLevel === FraudRiskLevel.HIGH && "border-orange-500 text-orange-500 bg-orange-500/5",
                      c.riskLevel === FraudRiskLevel.MEDIUM && "border-amber-500 text-amber-500 bg-amber-500/5",
                      c.riskLevel === FraudRiskLevel.LOW && "border-emerald-500 text-emerald-500 bg-emerald-500/5"
                    )}
                  >
                    {c.riskLevel}
                  </Badge>
                </TableCell>
                <TableCell className="font-bold text-foreground">{c.fraudType}</TableCell>
                <TableCell className="font-bold tabular-nums">Rp {Number(c.amount).toLocaleString('id-ID')}</TableCell>
                <TableCell>
                  <Badge variant="secondary" className="font-bold uppercase tracking-widest opacity-70">
                    {c.status}
                  </Badge>
                </TableCell>
                <TableCell className="text-muted-foreground font-bold text-xs">
                  {new Date(c.createdAt).toLocaleDateString(undefined, { day: 'numeric', month: 'short', year: 'numeric' })}
                </TableCell>
                <TableCell className="text-right">
                  <Link href={`/backoffice/fraud/${c.id}`}>
                    <Button variant="ghost" size="sm" className="h-9 gap-2 font-bold uppercase tracking-widest">
                      <Eye className="h-4 w-4" /> Detail
                    </Button>
                  </Link>
                </TableCell>
              </TableRow>
            ))
          )}
        </TableBody>
      </Table>

      <div className="px-8 py-6 border-t border-border flex justify-between items-center bg-muted/10">
        <Button
          variant="outline"
          onClick={() => setPage(p => Math.max(0, p - 1))}
          disabled={page === 0}
          className="h-10 px-6 gap-2 font-bold uppercase tracking-widest"
        >
          <ChevronLeft className="h-4 w-4" /> Sebelumnya
        </Button>
        <span className="text-xs font-bold text-muted-foreground tracking-widest uppercase">Halaman {page + 1}</span>
        <Button
          variant="outline"
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
