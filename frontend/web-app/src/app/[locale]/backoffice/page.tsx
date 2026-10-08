'use client';

import { Link } from '@/lib/navigation';
import { useQuery } from '@tanstack/react-query';
import { Users, AlertTriangle, Headphones, FileText, ClipboardCheck, ArrowUpRight } from '@/components/icons';
import { BackofficeService, BackofficeKycStatus, FraudCaseStatus, CustomerCaseStatus } from '@/services';
import { Card, Col, Row, Space, Tag, Typography } from 'antd';
import type { CSSProperties } from 'react';

const tileStyle: CSSProperties = {
  width: 48,
  height: 48,
  borderRadius: 12,
  display: 'flex',
  alignItems: 'center',
  justifyContent: 'center',
};

const labelStyle: CSSProperties = {
  fontSize: 12,
  fontWeight: 700,
  color: 'var(--ant-color-text-secondary)',
  textTransform: 'uppercase',
  letterSpacing: '0.1em',
};
export default function BackofficeDashboard() {
  const { data: kyc } = useQuery({
    queryKey: ['kyc-reviews', 'PENDING', 0],
    queryFn: () => BackofficeService.getKycReviews(BackofficeKycStatus.PENDING, 0),
  });
  const { data: fraud } = useQuery({
    queryKey: ['fraud-cases', 'OPEN', 0],
    queryFn: () => BackofficeService.getFraudCases(FraudCaseStatus.OPEN, undefined, 0),
  });
  const { data: tickets } = useQuery({
    queryKey: ['customer-cases', 'OPEN', 0],
    queryFn: () => BackofficeService.getCustomerCases(CustomerCaseStatus.OPEN, undefined, 0),
  });
  const kycCount = Array.isArray(kyc) ? kyc.length : 0;
  const fraudCount = Array.isArray(fraud) ? fraud.length : 0;
  const ticketCount = Array.isArray(tickets) ? tickets.length : 0;
  const stats = [
    { label: 'KYC Tertunda', value: String(kycCount), change: 'ANTRIAN', icon: Users, bg: 'var(--ant-color-primary-bg)', fg: 'var(--ant-color-primary)' },
    { label: 'Fraud Terbuka', value: String(fraudCount), change: 'ANTRIAN', icon: AlertTriangle, bg: 'var(--ant-color-error-bg)', fg: 'var(--ant-color-error)' },
    { label: 'Tiket Terbuka', value: String(ticketCount), change: 'ANTRIAN', icon: Headphones, bg: 'var(--ant-color-primary-bg)', fg: 'var(--ant-color-primary)' },
  ];

  const quickLinks = [
    { name: 'KYC Reviews', description: 'Review pending customer verifications', href: '/backoffice/kyc', icon: Users },
    { name: 'Fraud Monitoring', description: 'Investigate suspicious activities', href: '/backoffice/fraud', icon: AlertTriangle },
    { name: 'Customer Ops', description: 'Manage support cases and inquiries', href: '/backoffice/customers', icon: Headphones },
    { name: 'CMS Content', description: 'Manage banners and dynamic content', href: '/backoffice/cms', icon: FileText },
    { name: 'Audit Logs', description: 'Review system changes and audits', href: '/backoffice/compliance', icon: ClipboardCheck },
  ];

  return (
    <Space direction="vertical" size={32} style={{ width: '100%', paddingBottom: 48 }}>
      <div>
        <Typography.Title level={2} style={{ margin: 0 }}>Command Center</Typography.Title>
        <Typography.Text type="secondary">Sistem orkestrasi internal PayU Digital Banking.</Typography.Text>
      </div>

      <Row gutter={[24, 24]}>
        {stats.map((stat, i) => (
          <Col key={i} xs={24} md={8}>
            <Card>
              <Space direction="vertical" size={24} style={{ width: '100%' }}>
                <Space align="start" style={{ width: '100%', justifyContent: 'space-between' }}>
                  <div style={{ ...tileStyle, background: stat.bg, color: stat.fg }}>
                    <stat.icon style={{ fontSize: 24 }} />
                  </div>
                  <Tag color="green">{stat.change}</Tag>
                </Space>
                <div>
                  <Typography.Text strong style={labelStyle}>{stat.label}</Typography.Text>
                  <Typography.Title level={3} style={{ margin: 0, fontVariantNumeric: 'tabular-nums' }}>{stat.value}</Typography.Title>
                </div>
              </Space>
            </Card>
          </Col>
        ))}
      </Row>

      <Row gutter={[24, 24]}>
        {quickLinks.map((link, i) => (
          <Col key={i} xs={24} md={12} lg={8}>
            <Link href={link.href} style={{ textDecoration: 'none' }}>
              <Card hoverable style={{ height: '100%' }}>
                <Space direction="vertical" size={24} style={{ width: '100%' }}>
                  <Space align="start" style={{ width: '100%', justifyContent: 'space-between' }}>
                    <div style={{ ...tileStyle, width: 56, height: 56, background: 'var(--ant-color-primary)', color: 'var(--ant-color-text-light-solid)' }}>
                      <link.icon style={{ fontSize: 28 }} />
                    </div>
                    <div style={{ ...tileStyle, width: 40, height: 40, background: 'var(--ant-color-fill-tertiary)', color: 'var(--ant-color-text-secondary)' }}>
                      <ArrowUpRight style={{ fontSize: 20 }} />
                    </div>
                  </Space>
                  <div>
                    <Typography.Title level={4} style={{ marginBottom: 8 }}>{link.name}</Typography.Title>
                    <Typography.Text type="secondary">{link.description}</Typography.Text>
                  </div>
                  <Typography.Text strong style={labelStyle}>Open Management &rarr;</Typography.Text>
                </Space>
              </Card>
            </Link>
          </Col>
        ))}
      </Row>
    </Space>
  );
}
