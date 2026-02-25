'use client';

import React, { useState, useEffect } from 'react';
import { Download, FileText, Calendar, RefreshCw, AlertCircle, CheckCircle2, Clock } from 'lucide-react';
import clsx from 'clsx';
import StatementService, { Statement, PeriodType, StatementFormat, StatementStatus } from '@/services/StatementService';
import { StaggerContainer, StaggerItem } from '@/components/ui/Motion';
import { getA11yProps } from '@/lib/a11y';

/**
 * Statement Downloader Component
 *
 * Features:
 * - Period selector (Monthly/Quarterly/Annually)
 * - Date range picker for statement generation
 * - Statement history with download capability
 * - Loading states and error handling
 * - WCAG AA compliant accessibility
 */
export default function StatementDownloader() {
  const [statements, setStatements] = useState<Statement[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [isDownloading, setIsDownloading] = useState<string | null>(null);
  const [isGenerating, setIsGenerating] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState<string | null>(null);

  // Form state
  const [selectedPeriod, setSelectedPeriod] = useState<PeriodType>('monthly');
  const [selectedYear, setSelectedYear] = useState<number>(new Date().getFullYear());
  const [selectedMonth, setSelectedMonth] = useState<number>(new Date().getMonth() + 1);

  // Available years for statement generation
  const currentYear = new Date().getFullYear();
  const years = Array.from({ length: currentYear - 2019 }, (_, i) => currentYear - i);

  // Month names in Indonesian
  const monthNames = [
    'Januari', 'Februari', 'Maret', 'April', 'Mei', 'Juni',
    'Juli', 'Agustus', 'September', 'Oktober', 'November', 'Desember'
  ];

  // Load statements on mount
  useEffect(() => {
    loadStatements();
  }, []);

  const loadStatements = async () => {
    try {
      setIsLoading(true);
      setError(null);
      const data = await StatementService.listStatements(0, 10);
      setStatements(data.content);
    } catch (err) {
      console.error('Failed to load statements:', err);
      setError('Gagal memuat daftar e-statement. Silakan coba lagi.');
    } finally {
      setIsLoading(false);
    }
  };

  const handleGenerateStatement = async () => {
    try {
      setIsGenerating(true);
      setError(null);
      setSuccess(null);

      await StatementService.generateAndDownload({
        customerId: '', // TODO: populate from auth context
        accountNumber: 'default', // Will be populated from user context
        year: selectedYear,
        month: selectedMonth
      });

      setSuccess('E-Statement berhasil dibuat dan diunduh!');
      await loadStatements();
    } catch (err) {
      console.error('Failed to generate statement:', err);
      setError(err instanceof Error ? err.message : 'Gagal membuat e-statement. Silakan coba lagi.');
    } finally {
      setIsGenerating(false);
    }
  };

  const handleDownload = async (statement: Statement) => {
    if (statement.status !== 'COMPLETED') {
      setError('E-Statement belum siap untuk diunduh.');
      return;
    }

    try {
      setIsDownloading(statement.id);
      setError(null);
      setSuccess(null);

      await StatementService.downloadStatementWithFilename(statement.id);
      setSuccess('E-Statement berhasil diunduh!');
    } catch (err) {
      console.error('Failed to download statement:', err);
      setError('Gagal mengunduh e-statement. Silakan coba lagi.');
    } finally {
      setIsDownloading(null);
    }
  };

  const getStatusIcon = (status: StatementStatus) => {
    switch (status) {
      case 'COMPLETED':
        return <CheckCircle2 className="h-4 w-4" />;
      case 'GENERATING':
        return <Clock className="h-4 w-4" />;
      case 'FAILED':
        return <AlertCircle className="h-4 w-4" />;
    }
  };

  return (
    <StaggerContainer className="space-y-8">
      {/* Statement Generator Section */}
      <StaggerItem>
      <div className="bg-card rounded-xl p-8 border border-border shadow-card space-y-8 relative overflow-hidden">
        <div className="absolute top-0 right-0 w-32 h-32 bg-primary/5 rounded-full blur-3xl" />

        {/* Section Header */}
        <div className="flex items-center gap-4 relative z-10">
          <div className="h-12 w-12 bg-primary/10 rounded-xl flex items-center justify-center border border-primary/10">
            <FileText className="h-6 w-6 text-primary" />
          </div>
          <div>
            <h3 className="text-xl font-bold text-foreground">E-Statement Generator</h3>
            <p className="text-xs text-muted-foreground font-medium uppercase tracking-tight mt-0.5">
              Buat dan unduh laporan transaksi Anda
            </p>
          </div>
        </div>

        {/* Success Message */}
        {success && (
          <div className="relative z-10 bg-success-light/10 border border-primary/20 rounded-xl p-4 flex items-start gap-3">
            <CheckCircle2 className="h-5 w-5 text-primary flex-shrink-0 mt-0.5" />
            <div>
              <p className="text-sm font-bold text-foreground">Berhasil</p>
              <p className="text-xs text-muted-foreground mt-0.5">{success}</p>
            </div>
          </div>
        )}

        {/* Error Message */}
        {error && (
          <div className="relative z-10 bg-destructive/10 border border-destructive/20 rounded-xl p-4 flex items-start gap-3">
            <AlertCircle className="h-5 w-5 text-destructive flex-shrink-0 mt-0.5" />
            <div>
              <p className="text-sm font-bold text-destructive">Error</p>
              <p className="text-xs text-muted-foreground mt-0.5">{error}</p>
            </div>
          </div>
        )}

        {/* Generator Form */}
        <div className="grid grid-cols-1 md:grid-cols-3 gap-6 relative z-10">
          {/* Period Type Selector */}
          <div className="space-y-3">
            <label className="text-xs font-bold text-muted-foreground tracking-widest uppercase ml-1">
              Jenis Periode
            </label>
            <div className="grid grid-cols-3 gap-2">
              {(['monthly', 'quarterly', 'annually'] as PeriodType[]).map((period) => (
                <button
                  key={period}
                  onClick={() => setSelectedPeriod(period)}
                  className={clsx(
                    'px-3 py-3 rounded-xl text-xs font-bold tracking-widest uppercase transition-all border',
                    selectedPeriod === period
                      ? 'bg-primary text-primary-foreground border-primary shadow-lg shadow-primary/20'
                      : 'bg-muted/30 text-muted-foreground border-border hover:bg-muted/50 hover:text-foreground'
                  )}
                  aria-pressed={selectedPeriod === period}
                  {...getA11yProps({ label: `Pilih periode ${StatementService.formatPeriodType(period)}` })}
                >
                  {period === 'monthly' ? 'Bulan' : period === 'quarterly' ? 'Kuartal' : 'Tahun'}
                </button>
              ))}
            </div>
          </div>

          {/* Year Selector */}
          <div className="space-y-3">
            <label htmlFor="statement-year" className="text-xs font-bold text-muted-foreground tracking-widest uppercase ml-1">
              Tahun
            </label>
            <select
              id="statement-year"
              value={selectedYear}
              onChange={(e) => setSelectedYear(Number(e.target.value))}
              className="w-full rounded-xl border border-border bg-muted/30 p-4 text-sm font-bold text-foreground outline-none focus:ring-4 focus:ring-primary/10 focus:border-primary transition-all appearance-none cursor-pointer"
              {...getA11yProps({ label: 'Pilih tahun statement' })}
            >
              {years.map((year) => (
                <option key={year} value={year}>
                  {year}
                </option>
              ))}
            </select>
          </div>

          {/* Month Selector (for monthly/quarterly) */}
          {selectedPeriod !== 'annually' && (
            <div className="space-y-3">
              <label htmlFor="statement-month" className="text-xs font-bold text-muted-foreground tracking-widest uppercase ml-1">
                {selectedPeriod === 'quarterly' ? 'Kuartal' : 'Bulan'}
              </label>
              {selectedPeriod === 'monthly' ? (
                <select
                  id="statement-month"
                  value={selectedMonth}
                  onChange={(e) => setSelectedMonth(Number(e.target.value))}
                  className="w-full rounded-xl border border-border bg-muted/30 p-4 text-sm font-bold text-foreground outline-none focus:ring-4 focus:ring-primary/10 focus:border-primary transition-all appearance-none cursor-pointer"
                  {...getA11yProps({ label: 'Pilih bulan statement' })}
                >
                  {monthNames.map((name, index) => (
                    <option key={index + 1} value={index + 1}>
                      {name}
                    </option>
                  ))}
                </select>
              ) : (
                <div className="grid grid-cols-4 gap-2">
                  {[1, 2, 3, 4].map((quarter) => (
                    <button
                      key={quarter}
                      onClick={() => setSelectedMonth(quarter * 3)}
                      className={clsx(
                        'px-3 py-3 rounded-xl text-xs font-bold transition-all border',
                        selectedMonth === quarter * 3
                          ? 'bg-primary text-primary-foreground border-primary shadow-lg shadow-primary/20'
                          : 'bg-muted/30 text-muted-foreground border-border hover:bg-muted/50 hover:text-foreground'
                      )}
                      aria-pressed={selectedMonth === quarter * 3}
                      {...getA11yProps({ label: `Pilih kuartal ${quarter}` })}
                    >
                      Q{quarter}
                    </button>
                  ))}
                </div>
              )}
            </div>
          )}
        </div>

        {/* Generate Button */}
        <div className="relative z-10">
          <button
            onClick={handleGenerateStatement}
            disabled={isGenerating}
            className={clsx(
              'w-full flex items-center justify-center gap-3 py-5 rounded-xl font-bold text-xs tracking-widest uppercase transition-all',
              isGenerating
                ? 'bg-muted text-muted-foreground cursor-not-allowed'
                : 'bg-primary text-primary-foreground hover:bg-bank-emerald shadow-xl shadow-primary/20'
            )}
            {...getA11yProps({ label: 'Buat dan unduh e-statement' })}
          >
            {isGenerating ? (
              <>
                <RefreshCw className="h-5 w-5 animate-spin" />
                <span>Memproses E-Statement...</span>
              </>
            ) : (
              <>
                <FileText className="h-5 w-5" />
                <span>Buat & Unduh E-Statement</span>
              </>
            )}
          </button>
        </div>
      </div>
      </StaggerItem>

      {/* Statement History Section */}
      <StaggerItem>
      <div className="bg-card rounded-xl p-8 border border-border shadow-card space-y-6 relative overflow-hidden">
        <div className="absolute bottom-0 left-0 w-64 h-64 bg-primary/5 rounded-full blur-3xl -z-0" />

        {/* Section Header */}
        <div className="flex items-center justify-between relative z-10">
          <div className="flex items-center gap-4">
            <div className="h-12 w-12 bg-primary/10 rounded-xl flex items-center justify-center border border-primary/10">
              <Calendar className="h-6 w-6 text-primary" />
            </div>
            <div>
              <h3 className="text-xl font-bold text-foreground">Riwayat E-Statement</h3>
              <p className="text-xs text-muted-foreground font-medium uppercase tracking-tight mt-0.5">
                Daftar statement yang tersedia
              </p>
            </div>
          </div>
        </div>

        {/* Statements List */}
        <div className="relative z-10 space-y-3">
          {isLoading ? (
            <div className="flex items-center justify-center py-12">
              <RefreshCw className="h-8 w-8 text-primary animate-spin" />
            </div>
          ) : statements.length === 0 ? (
            <div className="text-center py-12">
              <FileText className="h-12 w-12 text-muted-foreground mx-auto mb-4 opacity-50" />
              <p className="text-sm text-muted-foreground font-medium">Belum ada e-statement tersedia</p>
              <p className="text-xs text-muted-foreground mt-1">Buat e-statement pertama Anda di atas</p>
            </div>
          ) : (
            statements.map((statement) => (
              <div
                key={statement.id}
                className="group bg-muted/20 rounded-xl p-5 border border-border hover:border-primary/30 transition-all"
              >
                <div className="flex items-center justify-between gap-4">
                  <div className="flex items-center gap-4 flex-1 min-w-0">
                    <div
                      className={clsx(
                        'h-10 w-10 rounded-lg flex items-center justify-center border transition-all',
                        StatementService.getStatusColor(statement.status)
                      )}
                    >
                      {getStatusIcon(statement.status)}
                    </div>
                    <div className="flex-1 min-w-0">
                      <p className="text-sm font-bold text-foreground truncate">
                        {statement.periodFormatted}
                      </p>
                      <div className="flex items-center gap-3 mt-1">
                        <span
                          className={clsx(
                            'text-xs px-2 py-0.5 rounded-full font-medium border',
                            StatementService.getStatusColor(statement.status)
                          )}
                        >
                          {StatementService.formatStatementStatus(statement.status)}
                        </span>
                        <span className="text-xs text-muted-foreground">
                          {statement.transactionCount} transaksi
                        </span>
                      </div>
                    </div>
                  </div>

                  <div className="flex items-center gap-3">
                    {statement.status === 'COMPLETED' && (
                      <div className="text-right hidden sm:block">
                        <p className="text-sm font-bold text-foreground">
                          {statement.closingBalanceFormatted}
                        </p>
                        <p className="text-xs text-muted-foreground">Saldo Akhir</p>
                      </div>
                    )}
                    <button
                      onClick={() => handleDownload(statement)}
                      disabled={statement.status !== 'COMPLETED' || isDownloading === statement.id}
                      className={clsx(
                        'h-10 px-4 rounded-lg flex items-center gap-2 text-xs font-bold uppercase tracking-wider transition-all',
                        statement.status === 'COMPLETED' && !isDownloading
                          ? 'bg-primary text-primary-foreground hover:bg-bank-emerald shadow-md hover:shadow-lg'
                          : 'bg-muted text-muted-foreground cursor-not-allowed'
                      )}
                      aria-label={`Download e-statement ${statement.periodFormatted}`}
                      {...getA11yProps({ label: `Download e-statement ${statement.periodFormatted}` })}
                    >
                      {isDownloading === statement.id ? (
                        <RefreshCw className="h-4 w-4 animate-spin" />
                      ) : (
                        <Download className="h-4 w-4" />
                      )}
                      <span className="hidden sm:inline">Unduh</span>
                    </button>
                  </div>
                </div>

                {/* Transaction Summary (expandable on mobile) */}
                {statement.status === 'COMPLETED' && (
                  <div className="mt-4 pt-4 border-t border-border/50 grid grid-cols-2 sm:grid-cols-4 gap-4">
                    <div>
                      <p className="text-xs text-muted-foreground uppercase tracking-wider">Saldo Awal</p>
                      <p className="text-sm font-bold text-foreground mt-1">
                        {statement.openingBalanceFormatted}
                      </p>
                    </div>
                    <div>
                      <p className="text-xs text-muted-foreground uppercase tracking-wider">Total Kredit</p>
                      <p className="text-sm font-bold text-primary mt-1">
                        +{statement.totalCreditsFormatted}
                      </p>
                    </div>
                    <div>
                      <p className="text-xs text-muted-foreground uppercase tracking-wider">Total Debit</p>
                      <p className="text-sm font-bold text-destructive mt-1">
                        -{statement.totalDebitsFormatted}
                      </p>
                    </div>
                    <div>
                      <p className="text-xs text-muted-foreground uppercase tracking-wider">Saldo Akhir</p>
                      <p className="text-sm font-bold text-foreground mt-1">
                        {statement.closingBalanceFormatted}
                      </p>
                    </div>
                  </div>
                )}
              </div>
            ))
          )}
        </div>

        {/* Load More Button */}
        {!isLoading && statements.length > 0 && (
          <div className="relative z-10 pt-4 border-t border-border">
            <button
              onClick={loadStatements}
              className="w-full py-4 text-center text-xs font-bold text-primary hover:text-bank-emerald transition-all uppercase tracking-wider"
              {...getA11yProps({ label: 'Muat lebih banyak e-statement' })}
            >
              Muat Lebih Banyak
            </button>
          </div>
        )}
      </div>
      </StaggerItem>
    </StaggerContainer>
  );
}
