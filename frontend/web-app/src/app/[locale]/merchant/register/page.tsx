'use client';

import { useState } from 'react';
import { useRouter } from '@/lib/navigation';
import { useMutation } from '@tanstack/react-query';
import { Building2, Mail, Phone, User, CreditCard, ArrowRight, ShieldCheck, CheckCircle2, FileText } from '@/components/icons';
import { PartnerService } from '@/services/PartnerService';
import { z } from 'zod';
import { notify as toast } from '@/lib/notify';
import { Button, Card, Col, Input, Row, Space, Typography } from 'antd';

const merchantSchema = z.object({
  name: z.string().min(3, 'Nama merchant minimal 3 karakter'),
  email: z.string().email('Format email tidak valid'),
  phone: z.string().min(10, 'Nomor telepon minimal 10 digit'),
  type: z.string().min(1, 'Tipe merchant wajib dipilih'),
  publicKey: z.string().optional(),
});

type MerchantFormData = z.infer<typeof merchantSchema>;

const merchantTypes = [
  { value: 'RETAIL', label: 'Retail', description: 'Toko fisik atau online dengan transaksi reguler' },
  { value: 'FOOD_BEVERAGE', label: 'Food & Beverage', description: 'Restoran, kafe, dan layanan makanan' },
  { value: 'TRANSPORTATION', label: 'Transportation', description: 'Ojek online, logistik, dan pengiriman' },
  { value: 'MARKETPLACE', label: 'Marketplace', description: 'Platform e-commerce multi-vendor' },
  { value: 'UTILITY', label: 'Utility', description: 'Pembayaran tagihan dan layanan utilitas' },
];

const labelStyle: React.CSSProperties = {
  fontSize: 12,
  fontWeight: 700,
  color: 'var(--ant-color-text-secondary)',
  textTransform: 'uppercase',
  letterSpacing: '0.1em',
};

export default function MerchantRegisterPage() {
  const router = useRouter();
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
      toast.success('Registrasi merchant berhasil! Silakan tunggu verifikasi.');
      router.push('/merchant');
    },
    onError: () => {
      toast.error('Registrasi gagal. Silakan coba lagi.');
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
        <Space direction="vertical" size={24} style={{ width: '100%' }}>
          <Space direction="vertical" size={16} style={{ width: '100%', textAlign: 'center', alignItems: 'center' }}>
            <div style={{ width: 80, height: 80, backgroundColor: 'var(--ant-color-primary-bg)', borderRadius: 16, display: 'inline-flex', alignItems: 'center', justifyContent: 'center' }}>
              <Building2 style={{ width: 40, height: 40, color: 'var(--ant-color-primary)' }} />
            </div>
            <Typography.Title level={1} style={{ margin: 0 }}>Daftar Merchant Baru</Typography.Title>
            <Typography.Text type="secondary" style={{ maxWidth: 576 }}>
              Bergabunglah dengan ekosistem pembayaran PayU dan terima pembayaran instan dari jutaan pengguna.
            </Typography.Text>
          </Space>

          <form onSubmit={handleSubmit}>
            <Space direction="vertical" size={24} style={{ width: '100%' }}>
              <Card>
                <Space direction="vertical" size={24} style={{ width: '100%' }}>
                  <Space direction="vertical" size={8} style={{ width: '100%' }}>
                    <label htmlFor="merchant-name" style={labelStyle}>
                      Nama Merchant
                    </label>
                    <Input
                      id="merchant-name"
                      value={formData.name}
                      onChange={(e) => handleChange('name', e.target.value)}
                      placeholder="Masukkan nama bisnis Anda"
                      size="large"
                      prefix={<User style={{ width: 20, height: 20, color: 'var(--ant-color-text-tertiary)' }} />}
                      status={errors.name ? 'error' : undefined}
                    />
                    {errors.name && <Typography.Text type="danger">{errors.name}</Typography.Text>}
                  </Space>

                  <Row gutter={[32, 24]}>
                    <Col xs={24} md={12}>
                      <Space direction="vertical" size={8} style={{ width: '100%' }}>
                        <label htmlFor="merchant-email" style={labelStyle}>
                          Email Bisnis
                        </label>
                        <Input
                          id="merchant-email"
                          type="email"
                          value={formData.email}
                          onChange={(e) => handleChange('email', e.target.value)}
                          placeholder="email@perusahaan.com"
                          size="large"
                          prefix={<Mail style={{ width: 20, height: 20, color: 'var(--ant-color-text-tertiary)' }} />}
                          status={errors.email ? 'error' : undefined}
                        />
                        {errors.email && <Typography.Text type="danger">{errors.email}</Typography.Text>}
                      </Space>
                    </Col>
                    <Col xs={24} md={12}>
                      <Space direction="vertical" size={8} style={{ width: '100%' }}>
                        <label htmlFor="merchant-phone" style={labelStyle}>
                          Nomor Telepon
                        </label>
                        <Input
                          id="merchant-phone"
                          type="tel"
                          value={formData.phone}
                          onChange={(e) => handleChange('phone', e.target.value)}
                          placeholder="+62 812-3456-7890"
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

              <Card title={<Typography.Title level={4} style={{ margin: 0 }}>Tipe Merchant</Typography.Title>}>
                <Row gutter={[24, 24]}>
                  {merchantTypes.map((type) => {
                    const selected = formData.type === type.value;
                    return (
                      <Col xs={24} md={12} key={type.value}>
                        <button
                          type="button"
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
                          <Space direction="vertical" size={16} style={{ width: '100%' }}>
                            <Space size={16}>
                              <div style={{ width: 40, height: 40, borderRadius: 12, display: 'flex', alignItems: 'center', justifyContent: 'center', backgroundColor: selected ? 'var(--ant-color-primary)' : 'var(--ant-color-fill-secondary)' }}>
                                <Building2 style={{ width: 20, height: 20, color: selected ? 'var(--ant-color-text-light-solid)' : 'var(--ant-color-text-tertiary)' }} />
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
                <Space direction="vertical" size={8} style={{ width: '100%' }}>
                  <label htmlFor="merchant-public-key" style={labelStyle}>
                    Public Key (Opsional)
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
                    Diperlukan untuk integrasi API custom
                  </Typography.Text>
                </Space>
              </Card>

              <Card style={{ backgroundColor: 'var(--ant-color-text)', border: 'none', position: 'relative', overflow: 'hidden' }}>
                <Row justify="space-between" align="middle" gutter={[32, 24]}>
                  <Col xs={24} md={16}>
                    <Space direction="vertical" size={16}>
                      <Space size={12}>
                        <div style={{ width: 40, height: 40, backgroundColor: 'var(--ant-color-primary)', borderRadius: 12, display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
                          <ShieldCheck style={{ width: 20, height: 20, color: 'var(--ant-color-text-light-solid)' }} />
                        </div>
                        <Typography.Title level={3} style={{ margin: 0, color: 'var(--ant-color-text-light-solid)' }}>Siap untuk Mulai?</Typography.Title>
                      </Space>
                      <Typography.Text style={{ color: 'var(--ant-color-text-light-solid)', opacity: 0.7 }}>
                        Dengan mendaftar, Anda menyetujui <Typography.Text strong style={{ color: 'var(--ant-color-primary)' }}>Syarat & Ketentuan</Typography.Text> serta <Typography.Text strong style={{ color: 'var(--ant-color-primary)' }}>Kebijakan Privasi</Typography.Text> PayU.
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
                      {registerMutation.isPending ? 'Sedang Memproses...' : 'Daftar Sekarang'}
                    </Button>
                  </Col>
                </Row>
                <CreditCard style={{ position: 'absolute', bottom: -30, right: -30, width: 192, height: 192, opacity: 0.08, transform: 'rotate(-12deg)', color: 'var(--ant-color-text-light-solid)' }} />
              </Card>
            </Space>
          </form>

          <div style={{ textAlign: 'center' }}>
            <Button type="link" onClick={() => router.push('/merchant')}>
              Kembali ke Dashboard Merchant
            </Button>
          </div>
        </Space>
      </div>
    </div>
  );
}
