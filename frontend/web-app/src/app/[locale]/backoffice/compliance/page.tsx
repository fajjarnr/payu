'use client';

import React, { useState } from 'react';
import {
  Shield,
  Search,
  FileText,
  ClipboardCheck,
  AlertTriangle,
  Lock,
  UserCheck,
  Download,
  Filter,
  Calendar,
  History,
  ChevronLeft,
  ChevronRight,
  Settings,
} from '@/components/icons';
import { Badge, Button, Input, Table } from 'antd';
import type { TableColumnsType } from 'antd';
import { useAuditReports, useFailedAccessAudits } from '@/hooks';
import type { AuditReport } from '@/services';

type ComplianceAuditRow = {
  id: string;
  event: string;
  resource: string;
  user: string;
  ip: string;
  risk: string;
  timestamp: string;
};

function toAuditRow(report: AuditReport): ComplianceAuditRow {
  const risk = report.overallStatus === 'FAIL'
    ? 'HIGH'
    : report.overallStatus === 'WARNING'
      ? 'MEDIUM'
      : 'LOW';

  return {
    id: report.id,
    event: `AUDIT_${report.standard}`,
    resource: report.transactionId || report.merchantId,
    user: report.createdBy,
    ip: 'N/A',
    risk,
    timestamp: report.createdAt,
  };
}

export default function CompliancePage() {
  const [searchTerm, setSearchTerm] = useState('');
  const { data: auditReportsData, isLoading } = useAuditReports();
  const { data: failedAccessData } = useFailedAccessAudits();

  const auditLogs = (Array.isArray(auditReportsData) ? auditReportsData.map(toAuditRow) : []).filter((log) => {
    if (!searchTerm) return true;
    const term = searchTerm.toLowerCase();
    return log.user?.toLowerCase().includes(term) || log.ip?.toLowerCase().includes(term) || log.resource?.toLowerCase().includes(term);
  });

  const highRiskCount = Array.isArray(failedAccessData) ? failedAccessData.length : 4;

  const getRiskBadge = (risk: string) => {
    switch (risk) {
      case 'LOW':
        return <Badge count="Low Risk" color="green" className="uppercase tracking-widest text-xs [&_sup]:bg-primary/10 [&_sup]:text-primary [&_sup]:border [&_sup]:border-primary/20 [&_sup]:px-3 [&_sup]:py-1 [&_sup]:rounded-full [&_sup]:font-semibold" />;
      case 'MEDIUM':
        return <Badge count="Medium Risk" color="gold" className="uppercase tracking-widest text-xs [&_sup]:bg-warning/10 [&_sup]:text-warning [&_sup]:border [&_sup]:border-warning/20 [&_sup]:px-3 [&_sup]:py-1 [&_sup]:rounded-full [&_sup]:font-semibold" />;
      case 'HIGH':
        return <Badge count="High Risk" color="red" className="uppercase tracking-widest text-xs [&_sup]:bg-error/10 [&_sup]:text-error [&_sup]:border [&_sup]:border-error/20 [&_sup]:px-3 [&_sup]:py-1 [&_sup]:rounded-full [&_sup]:font-semibold" />;
      default:
        return <span className="inline-flex items-center rounded-full border px-2.5 py-0.5 text-xs font-semibold">{risk}</span>;
    }
  };

  const getEventIcon = (event: string) => {
    if (event.includes('ACCESS')) return <Lock className="h-4 w-4 text-primary" />;
    if (event.includes('PII')) return <UserCheck className="h-4 w-4 text-error" />;
    if (event.includes('CHANGE')) return <Settings className="h-4 w-4 text-warning" />;
    return <FileText className="h-4 w-4 text-primary" />;
  };
  const columns: TableColumnsType<ComplianceAuditRow> = [
    {
      key: 'id',
      title: 'Event ID',
      render: (_, log) => <span className="font-mono text-xs font-bold text-foreground">{log.id}</span>,
    },
    {
      key: 'event',
      title: 'Event Type',
      render: (_, log) => (
        <div className="flex items-center gap-3">
          <div className="h-8 w-8 rounded-lg bg-muted flex items-center justify-center border border-border">
            {getEventIcon(log.event)}
          </div>
          <div>
            <p className="text-xs font-bold text-foreground tracking-tight">{log.event}</p>
            <p className="text-xs text-muted-foreground font-medium">{log.resource}</p>
          </div>
        </div>
      ),
    },
    {
      key: 'user',
      title: 'User / Actor',
      render: (_, log) => (
        <div className="flex items-center gap-2">
          <span className="inline-flex items-center rounded-full border px-2.5 py-0.5 text-xs font-semibold px-2 py-0 border-primary/20 text-primary font-bold uppercase tracking-widest">Admin</span>
          <span className="text-xs font-bold text-foreground">{log.user}</span>
        </div>
      ),
    },
    {
      key: 'ip',
      title: 'IP Address',
      render: (_, log) => <span className="text-xs font-medium text-muted-foreground">{log.ip}</span>,
    },
    { key: 'risk', title: 'Risk Level', render: (_, log) => getRiskBadge(log.risk) },
    {
      key: 'timestamp',
      title: 'Timestamp',
      align: 'right',
      render: (_, log) => (
        <div>
          <p className="text-xs font-medium text-foreground">{new Date(log.timestamp).toLocaleTimeString()}</p>
          <p className="text-xs text-muted-foreground font-bold uppercase tracking-widester mt-0.5">{new Date(log.timestamp).toLocaleDateString()}</p>
        </div>
      ),
    },
  ];

  return (
    <div className="space-y-6 lg:space-y-8">
      <>
        {/* Header Stats */}
        <>
          <div className="grid grid-cols-1 md:grid-cols-4 gap-6">
            {[
              { label: 'Security Score', value: '—', color: 'bg-primary', icon: Shield },
              { label: 'Audit Logs (24h)', value: isLoading ? '...' : String(auditLogs.length), color: 'bg-primary', icon: History },
              { label: 'High Risk Events', value: String(Array.isArray(failedAccessData) ? failedAccessData.length : 0), color: 'bg-error', icon: AlertTriangle },
              { label: 'Regulatory Status', value: 'Compliant', color: 'bg-secondary', icon: ClipboardCheck },
            ].map((stat, i) => (
              <div key={i} className="bg-card border border-border p-6 rounded-2xl shadow-sm flex items-center gap-5">
                <div className={`${stat.color} h-12 w-12 rounded-xl flex items-center justify-center text-surface shadow-lg`}>
                  <stat.icon className="h-6 w-6" />
                </div>
                <div>
                  <p className="text-xs font-bold text-muted-foreground uppercase tracking-widest">{stat.label}</p>
                  <p className="text-2xl font-bold text-foreground mt-0.5">{stat.value}</p>
                </div>
              </div>
            ))}
          </div>
        </>

        {/* Toolbar */}
        <>
          <div className="flex flex-col lg:flex-row justify-between items-start lg:items-center gap-6 bg-card border border-border p-5 sm:p-6 lg:p-8 rounded-2xl shadow-sm">
            <div className="flex items-center gap-4 w-full lg:w-auto">
              <div className="relative flex-1 lg:w-96 flex items-center">
                <Search className="absolute left-4 top-1/2 -translate-y-1/2 h-4 w-4 text-muted-foreground z-10 pointer-events-none" />
                <Input
                  aria-label="Filter berdasarkan User, IP, atau Resource"
                  placeholder="Filter by User, IP, or Resource..."
                  className="pl-12 bg-muted/30 border-border h-12 rounded-xl text-xs font-bold uppercase tracking-widest w-full"
                  value={searchTerm}
                  onChange={(e) => setSearchTerm(e.target.value)}
                />
              </div>
              <Button className="h-12 px-6 rounded-xl border-border bg-card text-xs font-bold tracking-widest uppercase gap-2">
                <Filter className="h-4 w-4" />
                More Filters
              </Button>
            </div>

            <div className="flex items-center gap-4 w-full lg:w-auto">
              <Button className="h-12 px-6 rounded-xl border-border bg-card text-xs font-bold tracking-widest uppercase gap-2">
                <Calendar className="h-4 w-4" />
                Last 24 Hours
              </Button>
              <Button type="primary" className="h-12 px-6 rounded-xl bg-primary-dark hover:bg-primary text-surface font-bold text-xs tracking-widest uppercase gap-2">
                <Download className="h-4 w-4" />
                Export Audit Report
              </Button>
            </div>
          </div>
        </>

        {/* Audit Table */}
        <>
          <div className="bg-card border border-border rounded-2xl shadow-sm overflow-hidden">
            <Table<ComplianceAuditRow>
              columns={columns}
              dataSource={auditLogs}
              rowKey="id"
              pagination={false}
              loading={isLoading}
              locale={{ emptyText: 'No audit logs found' }}
            />

            <div className="p-6 border-t border-border flex items-center justify-between">
              <p className="text-xs font-bold text-muted-foreground uppercase tracking-widest">
                Real-time Audit Stream Active
              </p>
              <div className="flex items-center gap-2">
                <Button className="min-h-[44px] min-w-[44px] rounded-xl border-border hover:bg-muted/50 p-0 flex items-center justify-center" icon={<ChevronLeft className="h-4 w-4" />} aria-label="Halaman sebelumnya" />
                <div className="h-10 px-4 flex items-center justify-center rounded-xl bg-primary text-surface font-bold text-xs">
                  1
                </div>
                <Button className="min-h-[44px] min-w-[44px] rounded-xl border-border hover:bg-muted/50 p-0 flex items-center justify-center" icon={<ChevronRight className="h-4 w-4" />} aria-label="Halaman berikutnya" />
              </div>
            </div>
          </div>
        </>
      </>
    </div>
  );
}
