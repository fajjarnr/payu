'use client';

import React from 'react';
import DashboardLayout from "@/components/DashboardLayout";
import { Card, Divider, Space, Typography } from 'antd';
import { FileText, Shield, AlertCircle, CheckCircle2 } from '@/components/icons';

const { Title, Text, Paragraph } = Typography;

export default function TermsPage() {
  return (
    <DashboardLayout>
      <Space direction="vertical" size={24} style={{ width: '100%' }}>
        <Space size={16}>
          <div style={{ width: 56, height: 56, backgroundColor: 'var(--ant-color-primary)', borderRadius: 16, display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
            <FileText style={{ width: 28, height: 28, color: 'var(--ant-color-text-light-solid)' }} />
          </div>
          <div>
            <Title level={2} style={{ margin: 0 }}>Syarat dan Ketentuan</Title>
            <Text type="secondary">Versi 1.0 - Terakhir diperbarui: Januari 2026</Text>
          </div>
        </Space>

        <Card>
          <Space direction="vertical" size={24} style={{ width: '100%' }}>
            <Space direction="vertical" size={12} style={{ width: '100%' }}>
              <Space size={12}>
                <Shield style={{ width: 24, height: 24, color: 'var(--ant-color-primary)' }} />
                <Title level={3} style={{ margin: 0 }}>1. Penerimaan Ketentuan</Title>
              </Space>
              <Paragraph type="secondary" style={{ margin: 0 }}>
                Dengan mengakses dan menggunakan layanan PayU, Anda setuju untuk terikat oleh syarat dan ketentuan ini.
                Jika Anda tidak setuju dengan bagian manapun dari ketentuan ini, Anda tidak boleh menggunakan layanan kami.
              </Paragraph>
            </Space>

            <Space direction="vertical" size={12} style={{ width: '100%' }}>
              <Space size={12}>
                <AlertCircle style={{ width: 24, height: 24, color: 'var(--ant-color-primary)' }} />
                <Title level={3} style={{ margin: 0 }}>2. Deskripsi Layanan</Title>
              </Space>
              <Paragraph type="secondary" style={{ margin: 0 }}>
                PayU menyediakan platform perbankan digital yang mencakup layanan manajemen rekening, transfer dana,
                pembayaran tagihan, dan layanan keuangan lainnya. Layanan ini disediakan sebagaimana adanya tanpa
                jaminan apapun.
              </Paragraph>
            </Space>

            <Space direction="vertical" size={12} style={{ width: '100%' }}>
              <Space size={12}>
                <CheckCircle2 style={{ width: 24, height: 24, color: 'var(--ant-color-primary)' }} />
                <Title level={3} style={{ margin: 0 }}>3. Tanggung Jawab Pengguna</Title>
              </Space>
              <Paragraph type="secondary" style={{ margin: 0 }}>
                Pengguna bertanggung jawab untuk menjaga kerahasiaan kredensial akun dan semua aktivitas yang terjadi
                di bawah akun mereka. Pengguna juga setuju untuk memberikan informasi yang akurat dan terkini.
              </Paragraph>
            </Space>

            <Space direction="vertical" size={12} style={{ width: '100%' }}>
              <Space size={12}>
                <Shield style={{ width: 24, height: 24, color: 'var(--ant-color-primary)' }} />
                <Title level={3} style={{ margin: 0 }}>4. Privasi dan Keamanan</Title>
              </Space>
              <Paragraph type="secondary" style={{ margin: 0 }}>
                Kami berkomitmen untuk melindungi privasi dan keamanan data pengguna sesuai dengan Kebijakan Privasi
                kami. Harap tinjau Kebijakan Privasi kami untuk informasi lebih lanjut.
              </Paragraph>
            </Space>

            <Space direction="vertical" size={12} style={{ width: '100%' }}>
              <Space size={12}>
                <AlertCircle style={{ width: 24, height: 24, color: 'var(--ant-color-primary)' }} />
                <Title level={3} style={{ margin: 0 }}>5. Batasan Tanggung Jawab</Title>
              </Space>
              <Paragraph type="secondary" style={{ margin: 0 }}>
                PayU tidak bertanggung jawab atas kerugian langsung, tidak langsung, insidental, atau konsekuensial
                yang timbul dari penggunaan atau ketidakmampuan menggunakan layanan kami.
              </Paragraph>
            </Space>

            <Space direction="vertical" size={12} style={{ width: '100%' }}>
              <Space size={12}>
                <CheckCircle2 style={{ width: 24, height: 24, color: 'var(--ant-color-primary)' }} />
                <Title level={3} style={{ margin: 0 }}>6. Perubahan Ketentuan</Title>
              </Space>
              <Paragraph type="secondary" style={{ margin: 0 }}>
                Kami berhak mengubah syarat dan ketentuan ini kapan saja dengan memberikan pemberitahuan kepada
                pengguna melalui aplikasi atau email. Penggunaan lanjutan layanan setelah perubahan dianggap
                sebagai penerimaan ketentuan yang diperbarui.
              </Paragraph>
            </Space>

            <Divider style={{ margin: '32px 0 0' }} />
            <Text type="secondary" style={{ textAlign: 'center', display: 'block' }}>
              Untuk pertanyaan lebih lanjut, hubungi tim dukungan kami di support@payu.fajjjar.my.id
            </Text>
          </Space>
        </Card>
      </Space>
    </DashboardLayout>
  );
}
