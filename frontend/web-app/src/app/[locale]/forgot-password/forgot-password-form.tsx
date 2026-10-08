'use client';

import { useState } from 'react';
import { useTranslations } from 'next-intl';
import { Link } from '@/lib/navigation';
import { Button, Card, Input, Space, Typography } from 'antd';
import { ArrowLeft } from '@/components/icons';
import { notify as toast } from '@/lib/notify';

const { Title, Text } = Typography;

export default function ForgotPasswordPage() {
  const t = useTranslations('auth');
  const [email, setEmail] = useState('');

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!email) {
      toast.error('Email wajib diisi');
      return;
    }
    try {
      const res = await fetch('/api/auth/forgot-password', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json', 'X-Idempotency-Key': typeof crypto !== 'undefined' && typeof crypto.randomUUID === 'function' ? crypto.randomUUID() : `${Date.now()}-${Math.random().toString(36).substring(2, 10)}` },
        body: JSON.stringify({ email }),
      });
      if (res.ok) toast.success('Instruksi reset telah dikirim');
      else toast.error('Gagal mengirim instruksi');
    } catch {
      toast.error('Gagal mengirim instruksi');
    }
  };

  return (
    <div style={{ minHeight: '100vh', display: 'flex', alignItems: 'center', justifyContent: 'center', padding: 32 }}>
      <Card style={{ width: '100%', maxWidth: 420 }}>
        <Space direction="vertical" size={24} style={{ width: '100%' }}>
          <Space direction="vertical" size={8} style={{ width: '100%' }}>
            <Link href="/login">
              <Space size={8}>
                <ArrowLeft style={{ width: 16, height: 16 }} />
                <Text type="secondary">Kembali ke login</Text>
              </Space>
            </Link>
            <Title level={2} style={{ margin: 0 }}>{t('forgotPassword')}</Title>
            <Text type="secondary">Masukkan email Anda untuk menerima instruksi reset password.</Text>
          </Space>
          <Space direction="vertical" size={16} style={{ width: '100%' }}>
            <Space direction="vertical" size={8} style={{ width: '100%' }}>
              <label htmlFor="email" style={{ fontSize: 14, fontWeight: 500 }}>{t('email')}</label>
              <Input
                id="email"
                type="email"
                placeholder="nama@email.com"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                size="large"
              />
            </Space>
            <Button type="primary" htmlType="submit" size="large" block onClick={handleSubmit}>
              Kirim Instruksi
            </Button>
          </Space>
        </Space>
      </Card>
    </div>
  );
}
