'use client';

import React, { useState } from 'react';
import type { CSSProperties } from 'react';
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
import { Avatar, Button, Card, Divider, Flex, Input, Space, Table, Tag, Typography } from 'antd';
import type { TableColumnsType } from 'antd';
import { useAuditReports, useFailedAccessAudits } from '@/hooks';
import type { AuditReport } from '@/services';
import StatCards from '../_components/StatCards';

const labelStyle: CSSProperties = {
  fontSize: 12,
  fontWeight: 700,
  color: 'var(--ant-color-text-secondary)',
  textTransform: 'uppercase',
  letterSpacing: '0.1em',
};

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

  const columns: TableColumnsType<ComplianceAuditRow> = [
    {
      key: 'id',
      title: 'Event ID',
      render: (_, log) => <Typography.Text code strong style={{ fontSize: 12 }}>{log.id}</Typography.Text>,
    },
    {
      key: 'event',
      title: 'Event Type',
      render: (_, log) => (
        <Flex align="center" gap={12}>
          <Avatar
            shape="square"
            size={32}
            style={{ background: 'var(--ant-color-fill-tertiary)', color: 'var(--ant-color-text-secondary)' }}
            icon={eventIcon(log.event)}
          />
          <Space direction="vertical" size={0}>
            <Typography.Text strong style={{ fontSize: 12 }}>{log.event}</Typography.Text>
            <Typography.Text type="secondary" style={{ fontSize: 12 }}>{log.resource}</Typography.Text>
          </Space>
        </Flex>
      ),
    },
    {
      key: 'user',
      title: 'User / Actor',
      render: (_, log) => (
        <Space>
          <Tag color="blue">Admin</Tag>
          <Typography.Text strong style={{ fontSize: 12 }}>{log.user}</Typography.Text>
        </Space>
      ),
    },
    {
      key: 'ip',
      title: 'IP Address',
      render: (_, log) => <Typography.Text type="secondary" style={{ fontSize: 12 }}>{log.ip}</Typography.Text>,
    },
    { key: 'risk', title: 'Risk Level', render: (_, log) => riskBadge(log.risk) },
    {
      key: 'timestamp',
      title: 'Timestamp',
      align: 'right',
      render: (_, log) => (
        <Space direction="vertical" size={2} style={{ textAlign: 'right' }}>
          <Typography.Text style={{ fontSize: 12 }}>{new Date(log.timestamp).toLocaleTimeString()}</Typography.Text>
          <Typography.Text type="secondary" strong style={labelStyle}>{new Date(log.timestamp).toLocaleDateString()}</Typography.Text>
        </Space>
      ),
    },
  ];

  return (
    <Space direction="vertical" size={24} style={{ width: '100%' }}>
      <StatCards stats={[
        { label: 'Security Score', value: '—', icon: Shield },
        { label: 'Audit Logs (24h)', value: isLoading ? '...' : String(auditLogs.length), icon: History },
        { label: 'High Risk Events', value: String(highRiskCount), icon: AlertTriangle },
        { label: 'Regulatory Status', value: 'Compliant', icon: ClipboardCheck },
      ]} />

      <Card>
        <Flex gap={16} wrap align="center" justify="space-between">
          <Flex gap={16} wrap style={{ flex: 1 }}>
            <Input
              aria-label="Filter berdasarkan User, IP, atau Resource"
              placeholder="Filter by User, IP, or Resource..."
              prefix={<Search style={{ fontSize: 16, color: 'var(--ant-color-text-secondary)' }} />}
              value={searchTerm}
              onChange={(e) => setSearchTerm(e.target.value)}
              style={{ flex: 1, minWidth: 200, maxWidth: 384 }}
            />
            <Button icon={<Filter style={{ fontSize: 16 }} />}>
              More Filters
            </Button>
          </Flex>
          <Flex gap={16} wrap>
            <Button icon={<Calendar style={{ fontSize: 16 }} />}>
              Last 24 Hours
            </Button>
            <Button type="primary" icon={<Download style={{ fontSize: 16 }} />}>
              Export Audit Report
            </Button>
          </Flex>
        </Flex>
      </Card>

      <Card styles={{ body: { padding: 0, overflow: 'hidden' } }}>
        <Table<ComplianceAuditRow>
          columns={columns}
          dataSource={auditLogs}
          rowKey="id"
          pagination={false}
          loading={isLoading}
          locale={{ emptyText: 'No audit logs found' }}
        />
        <Divider style={{ margin: 0 }} />
        <Flex align="center" justify="space-between" wrap gap={16} style={{ padding: 24 }}>
          <Typography.Text type="secondary" strong style={labelStyle}>
            Real-time Audit Stream Active
          </Typography.Text>
          <Flex align="center" gap={8}>
            <Button icon={<ChevronLeft style={{ fontSize: 16 }} />} aria-label="Halaman sebelumnya" />
            <Button type="primary">1</Button>
            <Button icon={<ChevronRight style={{ fontSize: 16 }} />} aria-label="Halaman berikutnya" />
          </Flex>
        </Flex>
      </Card>
    </Space>
  );
}

function riskBadge(risk: string) {
  switch (risk) {
    case 'LOW':
      return <Tag color="green">Low Risk</Tag>;
    case 'MEDIUM':
      return <Tag color="gold">Medium Risk</Tag>;
    case 'HIGH':
      return <Tag color="red">High Risk</Tag>;
    default:
      return <Tag>{risk}</Tag>;
  }
}

function eventIcon(event: string) {
  if (event.includes('ACCESS')) return <Lock style={{ fontSize: 16, color: 'var(--ant-color-primary)' }} />;
  if (event.includes('PII')) return <UserCheck style={{ fontSize: 16, color: 'var(--ant-color-error)' }} />;
  if (event.includes('CHANGE')) return <Settings style={{ fontSize: 16, color: 'var(--ant-color-warning)' }} />;
  return <FileText style={{ fontSize: 16, color: 'var(--ant-color-primary)' }} />;
}
