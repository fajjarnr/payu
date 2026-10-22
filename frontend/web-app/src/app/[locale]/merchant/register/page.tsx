'use client';

import { useState } from 'react';
import { useRouter } from '@/lib/navigation';
import { useMutation } from '@tanstack/react-query';
import { Building2, Mail, Phone, User, CreditCard, ArrowRight, ShieldCheck, CheckCircle2, FileText } from '@/components/icons';
import { PartnerService } from '@/services/PartnerService';
import { z } from 'zod';
import { notify as toast } from '@/lib/notify';
import { Button, Card, Col, Input, Row, Space, Typography } from 'antd';
import { useTranslations } from 'next-intl';
import { BRAND_PANEL_BG, BRAND_PANEL_FG, INK_ON_EMERALD } from '@/theme/tokens';

interface MerchantFormData {
  name: string;
  email: string;
  phone: string;
  type: string;
  publicKey?: string;
}

const buildMerchantSchema = (t: (key: string) => string) =>
  z.object({
    name: z.string().min(3, t('nameMinLength')),
    email: z.string().email(t('emailInvalid')),
    phone: z.string().min(10, t('phoneMinLength')),
    type: z.string().min(1, t('typeRequired')),
    publicKey: z.string().optional(),
  });

const labelStyle: React.CSSProperties = {
  fontSize: 12,
  fontWeight: 700,
  color: 'var(--ant-color-text-secondary)',
  textTransform: 'uppercase',
  letterSpacing: '0.1em',
};

export default function MerchantRegisterPage() {
  const router = useRouter();
  const t = useTranslations('merchantRegister');
  const merchantSchema = buildMerchantSchema(t);
  const merchantTypes = [
    { value: 'RETAIL', label: t('typeRetail'), description: t('typeRetailDescription') },
    { value: 'FOOD_BEVERAGE', label: t('typeFoodBeverage'), description: t('typeFoodBeverageDescription') },
    { value: 'TRANSPORTATION', label: t('typeTransportation'), description: t('typeTransportationDescription') },
    { value: 'MARKETPLACE', label: t('typeMarketplace'), description: t('typeMarketplaceDescription') },
    { value: 'UTILITY', label: t('typeUtility'), description: t('typeUtilityDescription') },
  ];
  const [formData, setFormData] = useState<MerchantFormData>({
    name: '',
    email: '',
    phone: '',
    type: '',
    publicKey: '',
  });
  const [errors, setErrors] = useState<Partial<Record<keyof MerchantFormData, string>>>({});

  const registerMutation = useMutation({
    mutationFn: (data: MerchantFormData) => PartnerService.register(data),
    onSuccess: () => {
      toast.success(t('successToast'));
      router.push('/merchant');
    },
    onError: () => {
      toast.error(t('errorToast'));
    }
  });

  const validateForm = (): boolean => {
    const result = merchantSchema.safeParse(formData);
    if (!result.success) {
      const newErrors: Partial<Record<keyof MerchantFormData, string>> = {};
      result.error.issues.forEach((err) => {
        if (err.path[0]) {
          newErrors[err.path[0] as keyof MerchantFormData] = err.message;
        }
      });
      setErrors(newErrors);
      return false;
    }
    setErrors({});
    return true;
  };

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (validateForm()) {
      registerMutation.mutate(formData);
    }
  };

  const handleChange = (field: keyof MerchantFormData, value: string) => {
    setFormData(prev => ({ ...prev, [field]: value }));
    if (errors[field]) {
      setErrors(prev => ({ ...prev, [field]: undefined }));
    }
  };

  return (
    <div style={{ minHeight: '100vh', backgroundColor: 'var(--ant-color-fill-tertiary)', padding: '48px 16px' }}>
      <div style={{ maxWidth: 896, margin: '0 auto' }}>
        <Space orientation="vertical" size={24} style={{ width: '100%' }}>
          <Space orientation="vertical" size={16} style={{ width: '100%', textAlign: 'center', alignItems: 'center' }}>
            <div style={{ width: 80, height: 80, backgroundColor: 'var(--ant-color-primary-bg)', borderRadius: 16, display: 'inline-flex', alignItems: 'center', justifyContent: 'center' }}>
              <Building2 style={{ width: 40, height: 40, color: 'var(--ant-color-primary)' }} />
            </div>
            <Typography.Title level={1} style={{ margin: 0 }}>{t('title')}</Typography.Title>
            <Typography.Text type="secondary" style={{ maxWidth: 576 }}>
              {t('subtitle')}
            </Typography.Text>
          </Space>

          <form onSubmit={handleSubmit}>
            <Space orientation="vertical" size={24} style={{ width: '100%' }}>
              <Card>
                <Space orientation="vertical" size={24} style={{ width: '100%' }}>
                  <Space orientation="vertical" size={8} style={{ width: '100%' }}>
                    <label htmlFor="merchant-name" style={labelStyle}>
                      {t('nameLabel')}
                    </label>
                    <Input
                      id="merchant-name"
                      value={formData.name}
                      onChange={(e) => handleChange('name', e.target.value)}
                      placeholder={t('namePlaceholder')}
                      size="large"
                      prefix={<User style={{ width: 20, height: 20, color: 'var(--ant-color-text-tertiary)' }} />}
                      status={errors.name ? 'error' : undefined}
                    />
                    {errors.name && <Typography.Text type="danger">{errors.name}</Typography.Text>}
                  </Space>

                  <Row gutter={[32, 24]}>
                    <Col xs={24} md={12}>
                      <Space orientation="vertical" size={8} style={{ width: '100%' }}>
                        <label htmlFor="merchant-email" style={labelStyle}>
                          {t('emailLabel')}
                        </label>
                        <Input
                          id="merchant-email"
                          type="email"
                          value={formData.email}
                          onChange={(e) => handleChange('email', e.target.value)}
                          placeholder={t('emailPlaceholder')}
                          size="large"
                          prefix={<Mail style={{ width: 20, height: 20, color: 'var(--ant-color-text-tertiary)' }} />}
                          status={errors.email ? 'error' : undefined}
                        />
                        {errors.email && <Typography.Text type="danger">{errors.email}</Typography.Text>}
                      </Space>
                    </Col>
                    <Col xs={24} md={12}>
                      <Space orientation="vertical" size={8} style={{ width: '100%' }}>
                        <label htmlFor="merchant-phone" style={labelStyle}>
                          {t('phoneLabel')}
                        </label>
                        <Input
                          id="merchant-phone"
                          type="tel"
                          value={formData.phone}
                          onChange={(e) => handleChange('phone', e.target.value)}
                          placeholder={t('phonePlaceholder')}
                          size="large"
                          prefix={<Phone style={{ width: 20, height: 20, color: 'var(--ant-color-text-tertiary)' }} />}
                          status={errors.phone ? 'error' : undefined}
                        />
                        {errors.phone && <Typography.Text type="danger">{errors.phone}</Typography.Text>}
                      </Space>
                    </Col>
                  </Row>
                </Space>
              </Card>

              <Card title={<Typography.Title level={4} style={{ margin: 0 }}>{t('typeTitle')}</Typography.Title>}>
                <Row gutter={[24, 24]}>
                  {merchantTypes.map((type) => {
                    const selected = formData.type === type.value;
                    return (
                      <Col xs={24} md={12} key={type.value}>
                        <button
                          type="button"
                          className="payu-focus-ring"
                          onClick={() => handleChange('type', type.value)}
                          aria-pressed={selected}
                          style={{
                            width: '100%',
                            display: 'flex',
                            flexDirection: 'column',
                            alignItems: 'flex-start',
                            textAlign: 'left',
                            cursor: 'pointer',
                            padding: 24,
                            borderRadius: 16,
                            borderWidth: 2,
                            borderStyle: 'solid',
                            borderColor: selected ? 'var(--ant-color-primary)' : 'var(--ant-color-border)',
                            backgroundColor: selected ? 'var(--ant-color-primary-bg)' : 'var(--ant-color-bg-container)',
                          }}
                        >
                          <Space orientation="vertical" size={16} style={{ width: '100%' }}>
                            <Space size={16}>
                              <div style={{ width: 40, height: 40, borderRadius: 12, display: 'flex', alignItems: 'center', justifyContent: 'center', backgroundColor: selected ? 'var(--ant-color-primary)' : 'var(--ant-color-fill-secondary)' }}>
                                <Building2 style={{ width: 20, height: 20, color: selected ? INK_ON_EMERALD : 'var(--ant-color-text-tertiary)' }} />
                              </div>
                              <Typography.Text strong>{type.label}</Typography.Text>
                            </Space>
                            <Typography.Text type="secondary">{type.description}</Typography.Text>
                            {selected && (
                              <CheckCircle2 style={{ width: 20, height: 20, color: 'var(--ant-color-primary)', alignSelf: 'flex-end' }} />
                            )}
                          </Space>
                        </button>
                      </Col>
                    );
                  })}
                </Row>
                {errors.type && <Typography.Text type="danger">{errors.type}</Typography.Text>}
              </Card>

              <Card>
                <Space orientation="vertical" size={8} style={{ width: '100%' }}>
                  <label htmlFor="merchant-public-key" style={labelStyle}>
                    {t('publicKeyLabel')}
                  </label>
                  <Input.TextArea
                    id="merchant-public-key"
                    value={formData.publicKey}
                    onChange={(e) => handleChange('publicKey', e.target.value)}
                    placeholder="-----BEGIN PUBLIC KEY-----"
                    rows={4}
                    style={{ fontFamily: 'ui-monospace, monospace' }}
                  />
                  <Typography.Text type="secondary">
                    {t('publicKeyHint')}
                  </Typography.Text>
                </Space>
              </Card>

              <Card style={{ backgroundColor: BRAND_PANEL_BG, border: 'none', position: 'relative', overflow: 'hidden' }}>
                <Row justify="space-between" align="middle" gutter={[32, 24]}>
                  <Col xs={24} md={16}>
                    <Space orientation="vertical" size={16}>
                      <Space size={12}>
                        <div style={{ width: 40, height: 40, backgroundColor: 'var(--ant-color-primary)', borderRadius: 12, display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
                          <ShieldCheck style={{ width: 20, height: 20, color: 'var(--ant-color-text-light-solid)' }} />
                        </div>
                        <Typography.Title level={3} style={{ margin: 0, color: 'var(--ant-color-text-light-solid)' }}>{t('readyTitle')}</Typography.Title>
                      </Space>
                      <Typography.Text style={{ color: `color-mix(in srgb, ${BRAND_PANEL_FG} 85%, ${BRAND_PANEL_BG})` }}>
                        {t('termsIntro')}{' '}
                        <Typography.Text strong style={{ color: 'var(--ant-color-primary)' }}>{t('termsLink')}</Typography.Text>{' '}
                        {t('termsConjunction')}{' '}
                        <Typography.Text strong style={{ color: 'var(--ant-color-primary)' }}>{t('privacyLink')}</Typography.Text>{' '}
                        {t('termsPayU')}
                      </Typography.Text>
                    </Space>
                  </Col>
                  <Col xs={24} md={8} style={{ textAlign: 'right' }}>
                    <Button
                      type="primary"
                      htmlType="submit"
                      loading={registerMutation.isPending}
                      size="large"
                      icon={!registerMutation.isPending ? <ArrowRight style={{ width: 16, height: 16 }} /> : undefined}
                    >
                      {registerMutation.isPending ? t('submitting') : t('submit')}
                    </Button>
                  </Col>
                </Row>
                <CreditCard style={{ position: 'absolute', bottom: -30, right: -30, width: 192, height: 192, opacity: 0.08, transform: 'rotate(-12deg)', color: 'var(--ant-color-text-light-solid)' }} />
              </Card>
            </Space>
          </form>

          <div style={{ textAlign: 'center' }}>
            <Button type="link" onClick={() => router.push('/merchant')}>
              {t('backToDashboard')}
            </Button>
          </div>
        </Space>
      </div>
    </div>
  );
}
