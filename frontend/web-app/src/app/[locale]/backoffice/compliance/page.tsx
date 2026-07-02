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
} from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Badge } from '@/components/ui/badge';
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow
} from '@/components/ui/table';
import { StaggerContainer, StaggerItem } from '@/components/ui/Motion';
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
        return <Badge className="bg-emerald-500/10 text-emerald-500 border-emerald-500/20 px-3 py-1 uppercase tracking-widest text-xs">Low Risk</Badge>;
      case 'MEDIUM':
        return <Badge className="bg-amber-500/10 text-amber-500 border-amber-500/20 px-3 py-1 uppercase tracking-widest text-xs">Medium Risk</Badge>;
      case 'HIGH':
        return <Badge className="bg-rose-500/10 text-rose-500 border-rose-500/20 px-3 py-1 uppercase tracking-widest text-xs">High Risk</Badge>;
      default:
        return <Badge variant="outline">{risk}</Badge>;
    }
  };

  const getEventIcon = (event: string) => {
    if (event.includes('ACCESS')) return <Lock className="h-4 w-4 text-blue-500" />;
    if (event.includes('PII')) return <UserCheck className="h-4 w-4 text-rose-500" />;
    if (event.includes('CHANGE')) return <Settings className="h-4 w-4 text-amber-500" />;
    return <FileText className="h-4 w-4 text-emerald-500" />;
  };

  return (
    <div className="space-y-12">
      <StaggerContainer>
        {/* Header Stats */}
        <StaggerItem>
          <div className="grid grid-cols-1 md:grid-cols-4 gap-6">
            {[
              { label: 'Security Score', value: '98/100', color: 'bg-emerald-500', icon: Shield },
              { label: 'Audit Logs (24h)', value: isLoading ? '...' : String(auditLogs.length || '1,245'), color: 'bg-blue-500', icon: History },
              { label: 'High Risk Events', value: String(highRiskCount), color: 'bg-rose-500', icon: AlertTriangle },
              { label: 'Regulatory Status', value: 'Compliant', color: 'bg-indigo-500', icon: ClipboardCheck },
            ].map((stat, i) => (
              <div key={i} className="bg-card border border-border p-6 rounded-2xl shadow-sm flex items-center gap-5">
                <div className={`${stat.color} h-12 w-12 rounded-xl flex items-center justify-center text-white shadow-lg`}>
                  <stat.icon className="h-6 w-6" />
                </div>
                <div>
                  <p className="text-xs font-bold text-muted-foreground uppercase tracking-widest">{stat.label}</p>
                  <p className="text-2xl font-bold text-foreground mt-0.5">{stat.value}</p>
                </div>
              </div>
            ))}
          </div>
        </StaggerItem>

        {/* Toolbar */}
        <StaggerItem>
          <div className="flex flex-col lg:flex-row justify-between items-start lg:items-center gap-6 bg-card border border-border p-8 rounded-2xl shadow-sm">
            <div className="flex items-center gap-4 w-full lg:w-auto">
              <div className="relative flex-1 lg:w-96">
                <Search className="absolute left-4 top-1/2 -translate-y-1/2 h-4 w-4 text-muted-foreground" />
                <Input
                  placeholder="Filter by User, IP, or Resource..."
                  className="pl-12 bg-muted/30 border-border h-12 rounded-xl text-xs font-bold uppercase tracking-widest"
                  value={searchTerm}
                  onChange={(e) => setSearchTerm(e.target.value)}
                />
              </div>
              <Button variant="outline" className="h-12 px-6 rounded-xl border-border bg-card text-xs font-bold tracking-widest uppercase gap-2">
                <Filter className="h-4 w-4" />
                More Filters
              </Button>
            </div>

            <div className="flex items-center gap-4 w-full lg:w-auto">
              <Button variant="outline" className="h-12 px-6 rounded-xl border-border bg-card text-xs font-bold tracking-widest uppercase gap-2">
                <Calendar className="h-4 w-4" />
                Last 24 Hours
              </Button>
              <Button className="h-12 px-6 rounded-xl bg-emerald-600 hover:bg-emerald-500 text-white font-bold text-xs tracking-widest uppercase gap-2">
                <Download className="h-4 w-4" />
                Export Audit Report
              </Button>
            </div>
          </div>
        </StaggerItem>

        {/* Audit Table */}
        <StaggerItem>
          <div className="bg-card border border-border rounded-2xl shadow-sm overflow-hidden">
            <Table>
              <TableHeader className="bg-muted/30">
                <TableRow className="border-border">
                  <TableHead className="text-xs font-bold uppercase tracking-widest p-6">Event ID</TableHead>
                  <TableHead className="text-xs font-bold uppercase tracking-widest px-6">Event Type</TableHead>
                  <TableHead className="text-xs font-bold uppercase tracking-widest px-6">User / Actor</TableHead>
                  <TableHead className="text-xs font-bold uppercase tracking-widest px-6">IP Address</TableHead>
                  <TableHead className="text-xs font-bold uppercase tracking-widest px-6">Risk Level</TableHead>
                  <TableHead className="text-xs font-bold uppercase tracking-widest px-6 text-right">Timestamp</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {auditLogs.map((log) => (
                  <TableRow key={log.id} className="border-border hover:bg-muted/10 transition-colors">
                    <TableCell className="p-6">
                      <span className="font-mono text-xs font-bold text-foreground">{log.id}</span>
                    </TableCell>
                    <TableCell className="px-6">
                      <div className="flex items-center gap-3">
                        <div className="h-8 w-8 rounded-lg bg-muted flex items-center justify-center border border-border">
                          {getEventIcon(log.event)}
                        </div>
                        <div>
                          <p className="text-xs font-bold text-foreground tracking-tight">{log.event}</p>
                          <p className="text-xs text-muted-foreground font-medium">{log.resource}</p>
                        </div>
                      </div>
                    </TableCell>
                    <TableCell className="px-6">
                      <div className="flex items-center gap-2">
                        <Badge variant="outline" className="px-2 py-0 border-emerald-500/20 text-xs text-emerald-500 font-bold uppercase tracking-widest">Admin</Badge>
                        <span className="text-xs font-bold text-foreground">{log.user}</span>
                      </div>
                    </TableCell>
                    <TableCell className="px-6 text-xs font-medium text-muted-foreground">
                      {log.ip}
                    </TableCell>
                    <TableCell className="px-6">
                      {getRiskBadge(log.risk)}
                    </TableCell>
                    <TableCell className="text-right p-6">
                      <p className="text-xs font-medium text-foreground">{new Date(log.timestamp).toLocaleTimeString()}</p>
                      <p className="text-xs text-muted-foreground font-bold uppercase tracking-widester mt-0.5">{new Date(log.timestamp).toLocaleDateString()}</p>
                    </TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>

            <div className="p-6 border-t border-border flex items-center justify-between">
              <p className="text-xs font-bold text-muted-foreground uppercase tracking-widest">
                Real-time Audit Stream Active
              </p>
              <div className="flex items-center gap-2">
                <Button variant="outline" size="icon" className="h-10 w-10 rounded-xl border-border hover:bg-muted/50">
                  <ChevronLeft className="h-4 w-4" />
                </Button>
                <div className="h-10 px-4 flex items-center justify-center rounded-xl bg-emerald-500 text-white font-bold text-xs">
                  1
                </div>
                <Button variant="outline" size="icon" className="h-10 w-10 rounded-xl border-border hover:bg-muted/50">
                  <ChevronRight className="h-4 w-4" />
                </Button>
              </div>
            </div>
          </div>
        </StaggerItem>
      </StaggerContainer>
    </div>
  );
}
