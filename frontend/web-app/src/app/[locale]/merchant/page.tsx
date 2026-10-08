'use client';

import { useEffect, useState } from 'react';
import { PartnerService, Partner } from '@/services/PartnerService';
import { Link } from '@/lib/navigation';
import { useTranslations } from 'next-intl';
import DashboardLayout from "@/components/DashboardLayout";
import { Card, Col, Input, Row, Space, Tag, Typography } from 'antd';
import { Building2, Key, ShieldCheck, Loader2 } from '@/components/icons';
import { useAuthStore } from '@/stores/authStore';

const labelStyle: React.CSSProperties = {
  fontSize: 12,
  fontWeight: 700,
  color: 'var(--ant-color-text-secondary)',
  textTransform: 'uppercase',
  letterSpacing: '0.1em',
};

export default function MerchantDashboard() {
  const t = useTranslations('merchant');
  const { user: _user } = useAuthStore();
  const [partner, setPartner] = useState<Partner | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const fetchPartner = async () => {
      try {
        // ponytail: email-based /me lookup; upgrade to owner_user_id if multi-tenant per user
        const data = await PartnerService.getMyPartner();
        setPartner(data);
      } catch (error) {
        console.error('Failed to fetch partner', error);
      } finally {
        setLoading(false);
      }
    };

    fetchPartner();
  }, []);

  if (loading) {
    return (
      <DashboardLayout>
        <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'center', minHeight: 320 }}>
          <Loader2 spin style={{ fontSize: 32, color: 'var(--ant-color-primary)' }} />
        </div>
      </DashboardLayout>
    );
  }

  if (!partner) {
    return (
      <DashboardLayout>
        <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'center', minHeight: 320, textAlign: 'center' }}>
          <Space direction="vertical" size={24} style={{ alignItems: 'center' }}>
            <Building2 style={{ width: 64, height: 64, color: 'var(--ant-color-text-tertiary)' }} />
            <Typography.Title level={2} style={{ margin: 0 }}>{t('title')}</Typography.Title>
            <Typography.Text type="secondary">{t('notRegistered')}</Typography.Text>
            <Link
              href="/merchant/register"
              style={{
                display: 'inline-flex',
                alignItems: 'center',
                justifyContent: 'center',
                height: 48,
                padding: '0 24px',
                borderRadius: 12,
                backgroundColor: 'var(--ant-color-primary)',
                color: 'var(--ant-color-text-light-solid)',
                fontSize: 12,
                fontWeight: 700,
                textTransform: 'uppercase',
                letterSpacing: '0.15em',
                textDecoration: 'none',
              }}
            >
              {t('register')}
            </Link>
          </Space>
        </div>
      </DashboardLayout>
    );
  }

  return (
    <DashboardLayout>
      <Space direction="vertical" size={24} style={{ width: '100%' }}>
        <Space direction="vertical" size={4}>
          <Typography.Title level={2} style={{ margin: 0 }}>{t('dashboard')}</Typography.Title>
          <Typography.Text type="secondary">{t('subtitle')}</Typography.Text>
        </Space>

        <Card
          title={
            <Space size={8}>
              <ShieldCheck style={{ width: 20, height: 20, color: 'var(--ant-color-primary)' }} />
              <Typography.Text strong>{t('profile')}</Typography.Text>
            </Space>
          }
        >
          <Row gutter={[24, 24]}>
            <Col xs={24} sm={12}>
              <Space direction="vertical" size={4}>
                <Typography.Text strong style={labelStyle}>{t('merchantName')}</Typography.Text>
                <Typography.Text>{partner.name}</Typography.Text>
              </Space>
            </Col>
            <Col xs={24} sm={12}>
              <Space direction="vertical" size={4}>
                <Typography.Text strong style={labelStyle}>{t('email')}</Typography.Text>
                <Typography.Text>{partner.email}</Typography.Text>
              </Space>
            </Col>
            <Col xs={24} sm={12}>
              <Space direction="vertical" size={4}>
                <Typography.Text strong style={labelStyle}>{t('type')}</Typography.Text>
                <Typography.Text>{partner.type}</Typography.Text>
              </Space>
            </Col>
            <Col xs={24} sm={12}>
              <Space direction="vertical" size={4}>
                <Typography.Text strong style={labelStyle}>{t('status')}</Typography.Text>
                <div>
                  <Tag bordered={false} color={partner.active ? 'green' : 'red'}>
                    {partner.active ? t('active') : t('inactive')}
                  </Tag>
                </div>
              </Space>
            </Col>
          </Row>
        </Card>

        <Card
          title={
            <Space size={8}>
              <Key style={{ width: 20, height: 20, color: 'var(--ant-color-primary)' }} />
              <Typography.Text strong>{t('apiCredentials')}</Typography.Text>
            </Space>
          }
        >
          <Space direction="vertical" size={16} style={{ width: '100%' }}>
            <Space direction="vertical" size={4} style={{ width: '100%' }}>
              <Typography.Text strong style={labelStyle}>{t('clientId')}</Typography.Text>
              <Typography.Text code style={{ display: 'block', padding: 12 }}>
                {partner.clientId || 'N/A'}
              </Typography.Text>
            </Space>
            <Space direction="vertical" size={4} style={{ width: '100%' }}>
              <Typography.Text strong style={labelStyle}>{t('publicKey')}</Typography.Text>
              <Input.TextArea
                aria-label="Public key"
                readOnly
                rows={4}
                value={partner.publicKey || t('noPublicKey')}
                style={{ backgroundColor: 'var(--ant-color-fill-tertiary)', fontFamily: 'ui-monospace, monospace' }}
              />
            </Space>
          </Space>
        </Card>
      </Space>
    </DashboardLayout>
  );
}
