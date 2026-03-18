'use client';

import { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { BackofficeService, BackofficeKycStatus } from '@/services';
import { Link } from '@/lib/navigation';
import { Button } from '@/components/ui/button';
import { Badge } from '@/components/ui/badge';
import { Table, TableHeader, TableRow, TableHead, TableBody, TableCell } from '@/components/ui/table';
import { Search, Filter, Users, ChevronLeft, ChevronRight, CheckCircle2, XCircle, Clock } from 'lucide-react';
import { Input } from '@/components/ui/input';
import clsx from 'clsx';

export default function KycReviewsPage() {
 const [status, setStatus] = useState<string>('');
 const [page, setPage] = useState(0);

  const { data: rawReviews, isLoading } = useQuery({
   queryKey: ['kyc-reviews', status, page],
   queryFn: () => BackofficeService.getKycReviews(status || undefined, page),
  });
  const reviews = Array.isArray(rawReviews) ? rawReviews : [];

 return (
  <div className="space-y-6">
    <div className="flex flex-col md:flex-row justify-between items-start md:items-end gap-6 mb-8">
      <div>
        <h2 className="text-3xl font-bold text-foreground tracking-tight">KYC Reviews</h2>
        <p className="text-sm text-muted-foreground font-medium mt-1">Review verifikasi identitas dan dokumen nasabah.</p>
      </div>
      <div className="flex items-center gap-3">
        <div className="bg-amber-500/10 px-4 py-2 rounded-lg border border-amber-500/20">
          <span className="text-xs font-bold text-amber-500 tracking-widest uppercase">Tertunda: 24</span>
        </div>
      </div>
    </div>

    <div className="flex flex-col md:flex-row gap-4 bg-card p-4 rounded-2xl border border-border shadow-sm">
      <div className="relative flex-1">
        <Search className="absolute left-4 top-1/2 -translate-y-1/2 h-4 w-4 text-muted-foreground" />
        <Input placeholder="Cari nasabah atau nomor dokumen..." className="pl-12 h-12" />
      </div>
      <div className="flex gap-4">
        <select
          value={status}
          onChange={(e) => setStatus(e.target.value)}
          className="h-12 rounded-xl border-border bg-muted/20 px-4 text-sm font-bold tracking-widest uppercase focus:ring-2 focus:ring-primary/20 outline-none"
        >
          <option value="">Semua Status</option>
          {Object.values(BackofficeKycStatus).map((s) => (
            <option key={s} value={s}>{s}</option>
          ))}
        </select>
      </div>
    </div>

    <div className="bg-card rounded-2xl border border-border shadow-sm overflow-hidden">
      <Table>
        <TableHeader className="bg-muted/30">
          <TableRow>
            <TableHead className="text-xs font-bold tracking-widest uppercase">Nasabah</TableHead>
            <TableHead className="text-xs font-bold tracking-widest uppercase">Dokumen</TableHead>
            <TableHead className="text-xs font-bold tracking-widest uppercase">Tanggal Kirim</TableHead>
            <TableHead className="text-xs font-bold tracking-widest uppercase">Status</TableHead>
            <TableHead className="text-right text-xs font-bold tracking-widest uppercase">Aksi</TableHead>
          </TableRow>
        </TableHeader>
        <TableBody>
          {isLoading ? (
            <TableRow>
              <TableCell colSpan={5} className="h-40 text-center text-muted-foreground font-bold tracking-widest uppercase">Memuat data...</TableCell>
            </TableRow>
          ) : reviews.length === 0 ? (
            <TableRow>
              <TableCell colSpan={5} className="h-40 text-center text-muted-foreground font-bold tracking-widest uppercase">Tidak ada review ditemukan</TableCell>
            </TableRow>
          ) : (
            reviews.map((review) => (
              <TableRow key={review.id} className="group cursor-pointer">
                <TableCell>
                  <div className="flex items-center gap-3">
                    <div className="h-10 w-10 rounded-full bg-muted flex items-center justify-center font-bold text-xs text-muted-foreground border border-border">
                      {review.fullName.slice(0, 2).toUpperCase()}
                    </div>
                    <div>
                      <div className="text-sm font-bold text-foreground">{review.fullName}</div>
                      <div className="text-[10px] font-bold text-muted-foreground tracking-widest uppercase">{review.userId}</div>
                    </div>
                  </div>
                </TableCell>
                <TableCell>
                  <div className="text-sm font-bold text-foreground">{review.documentType}</div>
                  <div className="text-[10px] font-bold text-muted-foreground tracking-widest uppercase">{review.documentNumber}</div>
                </TableCell>
                <TableCell className="text-muted-foreground font-bold text-xs">
                  {new Date(review.createdAt).toLocaleDateString('id-ID', { day: 'numeric', month: 'short', year: 'numeric' })}
                </TableCell>
                <TableCell>
                  <Badge
                    variant={review.status === BackofficeKycStatus.REJECTED ? "destructive" : "outline"}
                    className={clsx(
                      "font-bold uppercase tracking-widest",
                      review.status === BackofficeKycStatus.APPROVED && "border-emerald-500 text-emerald-500 bg-emerald-500/5",
                      review.status === BackofficeKycStatus.PENDING && "border-amber-500 text-amber-500 bg-amber-500/5"
                    )}
                  >
                    {review.status}
                  </Badge>
                </TableCell>
                <TableCell className="text-right">
                  <Link href={`/backoffice/kyc/${review.id}`}>
                    <Button variant="ghost" size="sm" className="h-9 gap-2 font-bold uppercase tracking-widest group-hover:bg-primary group-hover:text-white">
                      Review <ChevronRight className="h-4 w-4" />
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
