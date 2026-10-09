"use client";

import React, { useState } from "react";
import type { CSSProperties } from "react";
import {
  Shield,
  Search,
  FileText,
  ClipboardCheck,
  AlertTriangle,
  Lock,
  UserCheck,
  Download,
  History,
  Settings,
} from "@/components/icons";
import {
  Avatar,
  Button,
  Card,
  Divider,
  Flex,
  Input,
  Space,
  Table,
  Tag,
  Typography,
} from "antd";
import type { TableColumnsType } from "antd";
import { useAuditReports, useFailedAccessAudits } from "@/hooks";
import type { AuditReport } from "@/services";
import { notify } from "@/lib/notify";
import StatCards from "../_components/StatCards";

const labelStyle: CSSProperties = {
  fontSize: 12,
  fontWeight: 700,
  color: "var(--ant-color-text-secondary)",
  textTransform: "uppercase",
  letterSpacing: "0.1em",
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
  const risk =
    report.overallStatus === "FAIL"
      ? "HIGH"
      : report.overallStatus === "WARNING"
        ? "MEDIUM"
        : "LOW";

  return {
    id: report.id,
    event: `AUDIT_${report.standard}`,
    resource: report.transactionId || report.merchantId,
    user: report.createdBy,
    ip: "N/A",
    risk,
    timestamp: report.createdAt,
  };
}

export default function CompliancePage() {
  const [auditFilter, setAuditFilter] = useState<{
    transactionId?: string;
    merchantId?: string;
  }>({});
  const {
    data: auditReportsData,
    isLoading,
    isError: auditError,
  } = useAuditReports(auditFilter);
  const { data: failedAccessData, isLoading: isFailedAccessLoading } =
    useFailedAccessAudits();

  const hasAuditFilter = Object.keys(auditFilter).length > 0;

  // The search box accepts either identifier: a UUID-shaped value is a
  // transaction ID, anything else is a merchant ID. Both map 1:1 onto the
  // controller's search parameters.
  const handleAuditSearch = (value: string) => {
    const term = value.trim();
    if (!term) {
      setAuditFilter({});
      return;
    }
    setAuditFilter(
      /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i.test(
        term,
      )
        ? { transactionId: term }
        : { merchantId: term },
    );
  };

  const auditLogs = Array.isArray(auditReportsData)
    ? auditReportsData.map(toAuditRow)
    : [];

  // No invented number while the failed-access stream is still loading.
  const highRiskCount = Array.isArray(failedAccessData)
    ? failedAccessData.length
    : 0;

  const handleExport = () => {
    const data = Array.isArray(auditReportsData) ? auditReportsData : [];
    const blob = new Blob([JSON.stringify(data, null, 2)], {
      type: "application/json",
    });
    const url = URL.createObjectURL(blob);
    const a = document.createElement("a");
    a.href = url;
    a.download = `audit-report-${new Date().toISOString().split("T")[0]}.json`;
    a.click();
    URL.revokeObjectURL(url);
    notify.success("Audit report exported");
  };

  const columns: TableColumnsType<ComplianceAuditRow> = [
    {
      key: "id",
      title: "Event ID",
      render: (_, log) => (
        <Typography.Text code strong style={{ fontSize: 12 }}>
          {log.id}
        </Typography.Text>
      ),
    },
    {
      key: "event",
      title: "Event Type",
      render: (_, log) => (
        <Flex align="center" gap={12}>
          <Avatar
            shape="square"
            size={32}
            style={{
              background: "var(--ant-color-fill-tertiary)",
              color: "var(--ant-color-text-secondary)",
            }}
            icon={eventIcon(log.event)}
          />
          <Space direction="vertical" size={0}>
            <Typography.Text strong style={{ fontSize: 12 }}>
              {log.event}
            </Typography.Text>
            <Typography.Text type="secondary" style={{ fontSize: 12 }}>
              {log.resource}
            </Typography.Text>
          </Space>
        </Flex>
      ),
    },
    {
      key: "user",
      title: "User / Actor",
      render: (_, log) => (
        <Space>
          <Tag color="blue">Admin</Tag>
          <Typography.Text strong style={{ fontSize: 12 }}>
            {log.user}
          </Typography.Text>
        </Space>
      ),
    },
    {
      key: "ip",
      title: "IP Address",
      render: (_, log) => (
        <Typography.Text type="secondary" style={{ fontSize: 12 }}>
          {log.ip}
        </Typography.Text>
      ),
    },
    {
      key: "risk",
      title: "Risk Level",
      render: (_, log) => riskBadge(log.risk),
    },
    {
      key: "timestamp",
      title: "Timestamp",
      align: "right",
      render: (_, log) => (
        <Space direction="vertical" size={2} style={{ textAlign: "right" }}>
          <Typography.Text style={{ fontSize: 12 }}>
            {new Date(log.timestamp).toLocaleTimeString()}
          </Typography.Text>
          <Typography.Text type="secondary" strong style={labelStyle}>
            {new Date(log.timestamp).toLocaleDateString()}
          </Typography.Text>
        </Space>
      ),
    },
  ];

  return (
    <Space direction="vertical" size={24} style={{ width: "100%" }}>
      <StatCards
        stats={[
          { label: "Security Score", value: "—", icon: Shield },
          {
            label: "Audit Logs (24h)",
            value: isLoading ? "..." : String(auditLogs.length),
            icon: History,
          },
          {
            label: "High Risk Events",
            value: String(highRiskCount),
            icon: AlertTriangle,
          },
          {
            label: "Regulatory Status",
            value: "—",
            icon: ClipboardCheck,
          },
        ]}
      />

      <Card>
        <Flex gap={16} wrap align="center" justify="space-between">
          <Flex gap={16} wrap style={{ flex: 1 }}>
            <Input.Search
              allowClear
              aria-label="Cari audit berdasarkan Transaction ID atau Merchant ID"
              placeholder="Cari Transaction ID atau Merchant ID..."
              prefix={
                <Search
                  style={{
                    fontSize: 16,
                    color: "var(--ant-color-text-secondary)",
                  }}
                />
              }
              onSearch={handleAuditSearch}
              style={{ flex: 1, minWidth: 200, maxWidth: 384 }}
            />
          </Flex>
          <Flex gap={16} wrap>
            <Button
              type="primary"
              icon={<Download style={{ fontSize: 16 }} />}
              onClick={handleExport}
            >
              Export Audit Report
            </Button>
          </Flex>
        </Flex>
      </Card>

      <Card styles={{ body: { padding: 0, overflow: "hidden" } }}>
        <Table<ComplianceAuditRow>
          columns={columns}
          dataSource={auditLogs}
          rowKey="id"
          pagination={false}
          loading={isLoading}
          locale={{
            emptyText: auditError ? (
              "Audit stream tidak tersedia — periksa koneksi gateway"
            ) : !hasAuditFilter ? (
              <Space direction="vertical" size={4} align="center">
                <Typography.Text strong>
                  Belum ada pencarian audit
                </Typography.Text>
                <Typography.Text type="secondary">
                  Masukkan Transaction ID atau Merchant ID untuk mencari laporan
                  audit
                </Typography.Text>
              </Space>
            ) : (
              "Tidak ada laporan audit untuk pencarian ini"
            ),
          }}
        />
        <Divider style={{ margin: 0 }} />
        <Flex
          align="center"
          justify="space-between"
          wrap
          gap={16}
          style={{ padding: 24 }}
        >
          <Typography.Text
            type={auditError ? "danger" : "secondary"}
            strong={!auditError}
            style={labelStyle}
          >
            {auditError
              ? "Audit stream tidak tersedia"
              : hasAuditFilter
                ? "Audit Stream Active"
                : "Audit Stream Idle — masukkan pencarian"}
          </Typography.Text>
        </Flex>
      </Card>
    </Space>
  );
}

function riskBadge(risk: string) {
  switch (risk) {
    case "LOW":
      return <Tag color="green">Low Risk</Tag>;
    case "MEDIUM":
      return <Tag color="gold">Medium Risk</Tag>;
    case "HIGH":
      return <Tag color="red">High Risk</Tag>;
    default:
      return <Tag>{risk}</Tag>;
  }
}

function eventIcon(event: string) {
  if (event.includes("ACCESS"))
    return <Lock style={{ fontSize: 16, color: "var(--ant-color-primary)" }} />;
  if (event.includes("PII"))
    return (
      <UserCheck style={{ fontSize: 16, color: "var(--ant-color-error)" }} />
    );
  if (event.includes("CHANGE"))
    return (
      <Settings style={{ fontSize: 16, color: "var(--ant-color-warning)" }} />
    );
  return (
    <FileText style={{ fontSize: 16, color: "var(--ant-color-primary)" }} />
  );
}
