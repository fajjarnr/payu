'use client';

import { useEffect } from 'react';

import { useMutation } from '@tanstack/react-query';
import KYCService from '@/services/KYCService';
import { Button, Form, Input, Steps, Card, Space, Row, Col, Badge, Typography, theme } from 'antd';
import { registerUserSchema, type RegisterUserRequest } from '@/types';
import { zodFieldRule } from '@/lib/zodForm';
import api from '@/lib/api';
import { useRouter } from '@/lib/navigation';
import { 
  Camera, 
  ChevronRight, 
  CheckCircle2, 
  ShieldCheck, 
  ArrowLeft,
  Loader2,
  ScanFace,
  Fingerprint,
  Eye,
  EyeOff,
  AlertCircle
} from '@/components/icons';
import { useState, useRef } from 'react';
import { Link } from '@/lib/navigation';

import { useTranslations } from 'next-intl';
import { notify as toast } from '@/lib/notify';

const { Title, Text } = Typography;

// Branding panel is always dark (same surface as login-form.tsx), so it uses the
// DESIGN.md surface tokens rather than light-mode antd tokens.
const BRAND_BG = '#1A1A2E'; // branding panel: always dark navy (was bg-text-primary), both modes
const BRAND_FG = 'var(--color-surface)';
const BRAND_MUTED = 'var(--color-border)';
const BRAND_TILE_BG = 'rgba(255, 255, 255, 0.1)';
const BRAND_TILE_BORDER = 'rgba(255, 255, 255, 0.3)';
const BRAND_CARD_BG = 'rgba(255, 255, 255, 0.05)';
const BRAND_CARD_BORDER = '1px solid rgba(255, 255, 255, 0.1)';

export default function OnboardingPage() {
  const t = useTranslations('auth.onboarding');
  const router = useRouter();
  const { token } = theme.useToken();
  const [step, setStep] = useState(1);
  const [showPassword, setShowPassword] = useState(false);
  const [showConfirmPassword, setShowConfirmPassword] = useState(false);
  const [ktpFile, setKtpFile] = useState<File | null>(null);
  const fileInputRef = useRef<HTMLInputElement>(null);
  // Stable per-mount external ID — computed once via useState lazy initializer
  // (acceptable to the React 19 linter, unlike Date.now() in useRef or useMemo).
  const [stableExternalId] = useState(() => {
    const rnd = typeof crypto !== 'undefined' && typeof crypto.randomUUID === 'function'
      ? crypto.randomUUID().substring(0, 8)
      : Math.random().toString(36).substring(2, 10);
    return `KTP-${Date.now()}-${rnd}`;
  });

  const fileToBase64 = (file: File) =>
    new Promise<string>((resolve, reject) => {
      const reader = new FileReader();
      reader.onload = () => {
        const result = reader.result as string;
        resolve(result.includes(',') ? result.split(',')[1] : result);
      };
      reader.onerror = () => reject(new Error('Failed to read file'));
      reader.readAsDataURL(file);
    });

  const [form] = Form.useForm<RegisterUserRequest>();
  const rule = (field: string) => zodFieldRule(form, registerUserSchema, field);
  const onValid = (values: RegisterUserRequest) => {
    mutation.mutate(registerUserSchema.parse({ ...values, externalId: values.externalId || stableExternalId }));
  };

  const mutation = useMutation({
    mutationFn: async (data: RegisterUserRequest) => {
      const { confirmPassword: _confirmPassword, ...payload } = data as RegisterUserRequest & { confirmPassword?: string };
      const res = await api.post('/accounts/register', payload);
      // ponytail: upload KTP to kyc-service if present — Flow #28 minimal, non-blocking KYC
      if (ktpFile) {
        try {
          const base64 = await fileToBase64(ktpFile);
          const userId = (res.data as unknown as { data?: { id?: string }; id?: string })?.data?.id || (res.data as unknown as { id?: string })?.id || payload.username;
          const start = await KYCService.startVerification({
            userId: String(userId),
            fullName: payload.fullName,
            nik: payload.nik,
            dateOfBirth: '1990-01-01',
            address: 'Indonesia',
            phone: (payload as unknown as { phoneNumber?: string }).phoneNumber,
          });
          await KYCService.uploadKtp({ verificationId: start.verificationId, ktpImage: base64, nik: payload.nik });
        } catch (kycErr) {
          console.warn('KYC upload failed (non-blocking):', kycErr);
        }
      }
      return res;
    },
    onSuccess: () => {
      setStep(3);
      setTimeout(() => router.push('/login'), 2500);
    },
    onError: (error) => {
      console.error('Registration failed:', error instanceof Error ? error.message : 'Unknown error');
      toast.error(t('registrationFailed'));
    }
  });

  useEffect(() => {
    const stepTitles = [t('steps.identity'), t('steps.profile'), t('steps.complete')];
    document.title = `${stepTitles[step - 1]} | PayU Digital Banking`;
  }, [step, t]);

  const labelStyle: React.CSSProperties = { fontSize: token.fontSize, fontWeight: 700 };
  const iconTile = (size: number): React.CSSProperties => ({
    width: size,
    height: size,
    borderRadius: '50%',
    background: token.colorPrimaryBg,
    color: token.colorPrimary,
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center',
  });

  return (
    <Row style={{ minHeight: '100vh' }}>
      {/* Left Panel - Branding (hidden on mobile) */}
      <Col
        xs={0}
        lg={10}
        style={{
          background: BRAND_BG,
          padding: 24,
          position: 'relative',
          overflow: 'hidden',
          color: BRAND_FG,
          display: 'flex',
          flexDirection: 'column',
          justifyContent: 'space-between',
        }}
        aria-label="Branding"
      >
        {/* Background Effects */}
        <div style={{ position: 'absolute', top: 0, right: 0, width: 800, height: 800, background: token.colorPrimary, opacity: 0.1, borderRadius: '50%', filter: 'blur(120px)', transform: 'translate(50%, -50%)' }} aria-hidden="true" />
        <div style={{ position: 'absolute', bottom: 0, left: 0, width: 600, height: 600, background: token.colorPrimary, opacity: 0.1, borderRadius: '50%', filter: 'blur(100px)', transform: 'translate(-25%, 50%)' }} aria-hidden="true" />
        <div style={{ position: 'absolute', inset: 0, opacity: 0.03, backgroundImage: "url('https://grainy-gradients.vercel.app/noise.svg')" }} aria-hidden="true" />

        <div style={{ position: 'relative', zIndex: 1 }}>
          <Link href="/" style={{ display: 'inline-flex', alignItems: 'center', gap: 12, width: 'fit-content', color: BRAND_FG }} aria-label={t('back')}>
            <ArrowLeft style={{ width: 20, height: 20, opacity: 0.8 }} />
            <Text strong style={{ color: BRAND_FG }}>{t('back')}</Text>
          </Link>
        </div>

        <div style={{ position: 'relative', zIndex: 1, maxWidth: 480 }}>
          <Space direction="vertical" size={24}>
            <Space direction="vertical" size={16}>
              <div style={{ width: 64, height: 64, borderRadius: token.borderRadiusLG, background: BRAND_TILE_BG, border: `1px solid ${BRAND_TILE_BORDER}`, boxShadow: token.boxShadow, display: 'flex', alignItems: 'center', justifyContent: 'center' }} aria-hidden="true">
                <ScanFace style={{ width: 32, height: 32, color: token.colorPrimary }} />
              </div>
              <Title level={1} style={{ fontSize: 36, lineHeight: 1.2, letterSpacing: '-0.025em', margin: 0, color: BRAND_FG }}>
                {t('branding.title')}
              </Title>
              <Text style={{ color: BRAND_MUTED, fontSize: 18, lineHeight: 1.625 }}>
                {t('branding.desc')}
              </Text>
            </Space>

            <Space direction="vertical" size={16}>
              <Card size="small" style={{ background: BRAND_CARD_BG, border: BRAND_CARD_BORDER }}>
                <Space align="start" size={16}>
                  <Fingerprint style={{ width: 24, height: 24, color: token.colorPrimary, flexShrink: 0, marginTop: 4 }} aria-hidden="true" />
                  <div>
                    <Text strong style={{ color: BRAND_FG, display: 'block', marginBottom: 4 }}>{t('branding.features.ekyc.title')}</Text>
                    <Text style={{ fontSize: 14, color: BRAND_MUTED }}>{t('branding.features.ekyc.desc')}</Text>
                  </div>
                </Space>
              </Card>
              <Card size="small" style={{ background: BRAND_CARD_BG, border: BRAND_CARD_BORDER }}>
                <Space align="start" size={16}>
                  <ShieldCheck style={{ width: 24, height: 24, color: token.colorPrimary, flexShrink: 0, marginTop: 4 }} aria-hidden="true" />
                  <div>
                    <Text strong style={{ color: BRAND_FG, display: 'block', marginBottom: 4 }}>{t('branding.features.data.title')}</Text>
                    <Text style={{ fontSize: 14, color: BRAND_MUTED }}>{t('branding.features.data.desc')}</Text>
                  </div>
                </Space>
              </Card>
            </Space>
          </Space>
        </div>

        <div style={{ position: 'relative', zIndex: 1 }}>
          <Space size={8} align="center" style={{ color: BRAND_MUTED, fontSize: token.fontSizeSM, fontFamily: 'monospace' }}>
            <Badge status="processing" color={token.colorPrimary} />
            {t('branding.system')} • v2.4.0
          </Space>
        </div>
      </Col>

      {/* Right Panel - Form Flow */}
      <Col
        xs={24}
        lg={14}
        style={{
          display: 'flex',
          flexDirection: 'column',
          alignItems: 'center',
          justifyContent: 'center',
          padding: 24,
          background: token.colorBgLayout,
          position: 'relative',
        }}
        aria-labelledby="onboarding-title"
      >
        <div style={{ width: '100%', maxWidth: 520 }}>
          {/* Mobile back link (hidden at lg) */}
          <Row>
            <Col xs={24} lg={0} style={{ marginBottom: 16 }}>
              <Link href="/login" style={{ display: 'inline-flex', alignItems: 'center', gap: 8, fontSize: token.fontSize, color: token.colorTextSecondary }}>
                <ArrowLeft style={{ width: 16, height: 16 }} />
                <span>{t('back')}</span>
              </Link>
            </Col>
          </Row>

          {/* Progress Steps */}
          <nav style={{ marginBottom: 48 }} aria-label="Registration Progress">
            <Steps
              current={step - 1}
              items={[t('steps.identity'), t('steps.profile'), t('steps.complete')].map((title) => ({ title }))}
            />
          </nav>

          {step === 1 && (
            <Space direction="vertical" size={24} style={{ width: '100%' }}>
              <div style={{ textAlign: 'center' }}>
                <Space direction="vertical" size={8}>
                  <Title id="onboarding-title" level={3} style={{ margin: 0 }}>{t('step1.title')}</Title>
                  <Text type="secondary">{t('step1.subtitle')}</Text>
                </Space>
              </div>

              <input
                type="file"
                accept="image/*"
                ref={fileInputRef}
                style={{ display: 'none' }}
                aria-label="Unggah file"
                onChange={(e) => {
                  if (e.target.files?.[0]) {
                    setKtpFile(e.target.files[0]);
                  }
                }}
              />

              <div
                style={{
                  border: `2px dashed ${ktpFile ? token.colorBorder : token.colorErrorBorder}`,
                  borderRadius: token.borderRadiusLG,
                  padding: 32,
                  cursor: 'pointer',
                  display: 'flex',
                  flexDirection: 'column',
                  alignItems: 'center',
                  justifyContent: 'center',
                  textAlign: 'center',
                  gap: 16,
                  background: ktpFile ? token.colorFillQuaternary : token.colorErrorBg,
                }}
                tabIndex={0}
                role="button"
                aria-label={t('step1.clickToUpload')}
                onClick={() => fileInputRef.current?.click()}
                onKeyDown={(e) => {
                  if (e.key === 'Enter' || e.key === ' ') {
                    e.preventDefault();
                    fileInputRef.current?.click();
                  }
                }}
              >
                <div style={iconTile(64)}>
                  <Camera style={{ width: 32, height: 32 }} aria-hidden="true" />
                </div>
                <Space direction="vertical" size={4}>
                  <Text strong>{t('step1.clickToUpload')}</Text>
                  <Text type="secondary" style={{ fontSize: token.fontSizeSM }}>{t('step1.formats')}</Text>
                  {ktpFile && (
                    <Text style={{ fontSize: token.fontSizeSM, color: token.colorPrimary, fontWeight: 500 }}>{ktpFile.name}</Text>
                  )}
                </Space>
              </div>

              <Button
                type="primary"
                block
                size="large"
                onClick={() => setStep(2)}
                disabled={!ktpFile}
              >
                {t('step1.button')} <ChevronRight style={{ marginLeft: 8, width: 16, height: 16 }} />
              </Button>
              {!ktpFile && (
                <Text type="danger" role="alert" style={{ display: 'flex', alignItems: 'center', gap: 8, fontWeight: 500 }}>
                  <AlertCircle style={{ width: 16, height: 16, flexShrink: 0 }} />
                  <span>{t('step1.uploadRequiredHint')}</span>
                </Text>
              )}
            </Space>
          )}

          {step === 2 && (
            <Space direction="vertical" size={32} style={{ width: '100%' }}>
              <div style={{ textAlign: 'center' }}>
                <Space direction="vertical" size={8}>
                  <Title id="onboarding-title" level={3} style={{ margin: 0 }}>{t('step2.title')}</Title>
                  <Text type="secondary">{t('step2.subtitle')}</Text>
                </Space>
              </div>

              <Form form={form} onFinish={onValid} layout="vertical" initialValues={{ externalId: stableExternalId }}>
                <Row gutter={[20, 0]}>
                  <Col span={24}>
                    <Form.Item name="nik" rules={[rule('nik')]} label={<label htmlFor="onboarding-nik" style={labelStyle}>{t('step2.nik')}</label>}>
                      <Input id="onboarding-nik" placeholder={t('step2.nikPlaceholder')} maxLength={16} inputMode="numeric" onKeyPress={(e) => { if (!/[0-9]/.test(e.key)) { e.preventDefault(); } }} />
                    </Form.Item>
                  </Col>
                  <Col span={24}>
                    <Form.Item name="fullName" rules={[rule('fullName')]} label={<label htmlFor="onboarding-fullname" style={labelStyle}>{t('step2.fullName')}</label>}>
                      <Input id="onboarding-fullname" placeholder={t('step2.fullNamePlaceholder')} />
                    </Form.Item>
                  </Col>
                  <Col xs={24} md={12}>
                    <Form.Item name="email" rules={[rule('email')]} label={<label htmlFor="onboarding-email" style={labelStyle}>{t('step2.email')}</label>}>
                      <Input id="onboarding-email" type="email" placeholder={t('step2.emailPlaceholder')} />
                    </Form.Item>
                  </Col>
                  <Col xs={24} md={12}>
                    <Form.Item name="username" rules={[rule('username')]} label={<label htmlFor="onboarding-username" style={labelStyle}>{t('step2.username')}</label>}>
                      <Input id="onboarding-username" placeholder={t('step2.usernamePlaceholder')} />
                    </Form.Item>
                  </Col>
                  <Col xs={24} md={12}>
                    <Form.Item name="password" rules={[rule('password')]} label={<label htmlFor="onboarding-password" style={labelStyle}>{t('step2.password')}</label>}>
                      <Input
                        id="onboarding-password"
                        type={showPassword ? 'text' : 'password'}
                        placeholder={t('step2.passwordPlaceholder')}
                        autoComplete="new-password"
                        suffix={
                          <Button
                            type="text"
                            shape="circle"
                            icon={showPassword ? <EyeOff style={{ width: 20, height: 20 }} /> : <Eye style={{ width: 20, height: 20 }} />}
                            onClick={() => setShowPassword(!showPassword)}
                            aria-label={showPassword ? 'Hide password' : 'Show password'}
                          />
                        }
                      />
                    </Form.Item>
                  </Col>
                  <Col xs={24} md={12}>
                    <Form.Item name="confirmPassword" rules={[rule('confirmPassword')]} dependencies={['password']} label={<label htmlFor="onboarding-confirm-password" style={labelStyle}>{t('step2.confirmPassword')}</label>}>
                      <Input
                        id="onboarding-confirm-password"
                        type={showConfirmPassword ? 'text' : 'password'}
                        placeholder={t('step2.confirmPasswordPlaceholder')}
                        autoComplete="new-password"
                        suffix={
                          <Button
                            type="text"
                            shape="circle"
                            icon={showConfirmPassword ? <EyeOff style={{ width: 20, height: 20 }} /> : <Eye style={{ width: 20, height: 20 }} />}
                            onClick={() => setShowConfirmPassword(!showConfirmPassword)}
                            aria-label={showConfirmPassword ? 'Hide password' : 'Show password'}
                          />
                        }
                      />
                    </Form.Item>
                  </Col>
                </Row>
                <Form.Item name="externalId" initialValue={stableExternalId} hidden>
                  <Input type="hidden" />
                </Form.Item>
                <Space size={16} style={{ paddingTop: 24, width: '100%' }}>
                  <Button type="default" htmlType="button" size="large" onClick={() => setStep(1)}>
                    {t('step2.backButton')}
                  </Button>
                  <Button type="primary" htmlType="submit" size="large" style={{ flex: 1 }} disabled={mutation.isPending}>
                    {mutation.isPending ? <Loader2 className="animate-spin" /> : t('step2.submitButton')}
                  </Button>
                </Space>
              </Form>
            </Space>
          )}

          {step === 3 && (
            <Space direction="vertical" size={16} style={{ width: '100%', textAlign: 'center', padding: '40px 0' }}>
              <div style={{ ...iconTile(96), margin: '0 auto' }}>
                <CheckCircle2 style={{ width: 48, height: 48 }} />
              </div>
              <Space direction="vertical" size={8}>
                <Title id="onboarding-title" level={2} style={{ margin: 0 }}>{t('step3.title')}</Title>
                <Text type="secondary" style={{ maxWidth: 320, margin: '0 auto', display: 'block' }}>
                  {t('step3.subtitle')}
                </Text>
              </Space>
              <Loader2 className="animate-spin" style={{ width: 24, height: 24, color: token.colorPrimary, margin: '16px auto 0' }} aria-label="Processing..." />
            </Space>
          )}
        </div>
      </Col>
    </Row>
  );
}
