'use client';

import { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { BackofficeService, CustomerCaseStatus, CustomerCasePriority } from '@/services';
import { Link } from '@/lib/navigation';
import { Button } from '@/components/ui/button';
import { Badge } from '@/components/ui/badge';
import { Table, TableHeader, TableRow, TableHead, TableBody, TableCell } from '@/components/ui/table';
import { Search, Filter, Headphones, ChevronLeft, ChevronRight, MessageSquare, AlertTriangle, CheckCircle2 } from 'lucide-react'; // eslint-disable-line @typescript-eslint/no-unused-vars
import { Input } from '@/components/ui/input';
import clsx from 'clsx';

export default function CustomerCasesPage() {
 const [status, setStatus] = useState<string>('');
 const [priority, setPriority] = useState<string>('');
 const [page, setPage] = useState(0);

  const { data: rawCases, isLoading } = useQuery({
   queryKey: ['customer-cases', status, priority, page],
   queryFn: () => BackofficeService.getCustomerCases(status || undefined, priority || undefined, page),
  });
  const cases = Array.isArray(rawCases) ? rawCases : [];

 return (
    <div className="space-y-6">
      <div className="flex flex-col md:flex-row justify-between items-start md:items-end gap-6 mb-8">
        <div>
          <h2 className="text-3xl font-bold text-foreground tracking-tight">Customer Operations</h2>
          <p className="text-sm text-muted-foreground font-medium mt-1">Kelola tiket dukungan, keluhan, dan bantuan nasabah.</p>
        </div>
        <div className="flex items-center gap-3">
          <div className="bg-primary/10 px-4 py-2 rounded-lg border border-primary/20">
            <span className="text-xs font-bold text-primary tracking-widest uppercase">Open: 42</span>
          </div>
        </div>
      </div>

      <div className="flex flex-col md:flex-row gap-4 bg-card p-4 rounded-2xl border border-border shadow-sm">
        <div className="relative flex-1">
          <Search className="absolute left-4 top-1/2 -translate-y-1/2 h-4 w-4 text-muted-foreground" />
          <Input placeholder="Cari tiket atau ID nasabah..." className="pl-12 h-12" />
        </div>
        <div className="flex gap-4">
          <select
            value={priority}
            onChange={(e) => setPriority(e.target.value)}
            className="h-12 rounded-xl border-border bg-muted/20 px-4 text-sm font-bold tracking-widest uppercase focus:ring-2 focus:ring-primary/20 outline-none"
          >
            <option value="">Semua Prioritas</option>
            {Object.values(CustomerCasePriority).map((s) => (
              <option key={s} value={s}>{s}</option>
            ))}
          </select>
          <select
            value={status}
            onChange={(e) => setStatus(e.target.value)}
            className="h-12 rounded-xl border-border bg-muted/20 px-4 text-sm font-bold tracking-widest uppercase focus:ring-2 focus:ring-primary/20 outline-none"
          >
            <option value="">Semua Status</option>
            {Object.values(CustomerCaseStatus).map((s) => (
              <option key={s} value={s}>{s}</option>
            ))}
          </select>
        </div>
      </div>

      <div className="bg-card rounded-2xl border border-border shadow-sm overflow-hidden">
        <Table>
          <TableHeader className="bg-muted/30">
            <TableRow>
              <TableHead className="text-xs font-bold tracking-widest uppercase">No. Tiket</TableHead>
              <TableHead className="text-xs font-bold tracking-widest uppercase">Subjek & Nasabah</TableHead>
              <TableHead className="text-xs font-bold tracking-widest uppercase">Prioritas</TableHead>
              <TableHead className="text-xs font-bold tracking-widest uppercase">Status</TableHead>
              <TableHead className="text-right text-xs font-bold tracking-widest uppercase">Aksi</TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            {isLoading ? (
              <TableRow>
                <TableCell colSpan={5} className="h-40 text-center text-muted-foreground font-bold tracking-widest uppercase">Memuat data...</TableCell>
              </TableRow>
            ) : cases.length === 0 ? (
              <TableRow>
                <TableCell colSpan={5} className="h-40 text-center text-muted-foreground font-bold tracking-widest uppercase">Tidak ada tiket ditemukan</TableCell>
              </TableRow>
            ) : (
              cases.map((c) => (
                <TableRow key={c.id} className="group cursor-pointer">
                  <TableCell className="font-bold text-muted-foreground tabular-nums">#{c.caseNumber}</TableCell>
                  <TableCell>
                    <div className="text-sm font-bold text-foreground">{c.subject}</div>
                    <div className="text-[10px] font-bold text-muted-foreground tracking-widest uppercase">{c.userId}</div>
                  </TableCell>
                  <TableCell>
                    <Badge
                      variant={c.priority === CustomerCasePriority.URGENT ? "destructive" : "outline"}
                      className={clsx(
                        "font-bold uppercase tracking-widest",
                        c.priority === CustomerCasePriority.HIGH && "border-orange-500 text-orange-500 bg-orange-500/5",
                        c.priority === CustomerCasePriority.MEDIUM && "border-blue-500 text-blue-500 bg-blue-500/5",
                        c.priority === CustomerCasePriority.LOW && "border-slate-500 text-slate-500 bg-slate-500/5"
                      )}
                    >
                      {c.priority}
                    </Badge>
                  </TableCell>
                  <TableCell>
                    <Badge variant="secondary" className={clsx(
                      "font-bold uppercase tracking-widest",
                      c.status === CustomerCaseStatus.OPEN && "text-blue-600 bg-blue-600/5 border-blue-600/10",
                      c.status === CustomerCaseStatus.RESOLVED && "text-emerald-600 bg-emerald-600/5 border-emerald-600/10"
                    )}>
                      {c.status}
                    </Badge>
                  </TableCell>
                  <TableCell className="text-right">
                    <Link href={`/backoffice/customers/${c.id}`}>
                      <Button variant="ghost" size="sm" className="h-9 gap-2 font-bold uppercase tracking-widest">
                        <MessageSquare className="h-4 w-4" /> Buka
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
