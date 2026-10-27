'use client';

import React from 'react';
import DashboardLayout from "@/components/DashboardLayout";
import { Card, Divider, Space, Typography, theme } from 'antd';
import { FileText, Shield, AlertCircle, CheckCircle2 } from '@/components/icons';
import { SkipLink } from "@/lib/a11y";
import { ON_PRIMARY } from '@/theme/tokens';

const { Title, Text, Paragraph } = Typography;

const SECTION_ICON_STYLE = { width: 24, height: 24 } as const;
const SECTION_TITLE_STYLE = { margin: 0, fontWeight: 600 } as const;

export default function TermsPage() {
  const { token } = theme.useToken();
  const iconColor = token.colorPrimaryText;
  return (
    <DashboardLayout>
      <SkipLink />
      <Space orientation="vertical" size={24} style={{ width: '100%' }}>
        <Space size={16}>
          <div style={{ width: 56, height: 56, background: token.colorPrimary, borderRadius: 24, display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
            <FileText style={{ width: 28, height: 28, color: ON_PRIMARY }} />
          </div>
          <div>
            <Title level={1} style={{ margin: 0, fontWeight: 600 }}>Syarat dan Ketentuan</Title>
            <Text type="secondary">Versi 1.0 - Terakhir diperbarui: Januari 2026</Text>
          </div>
        </Space>

        <Card style={{ borderRadius: 24 }}>
          <Space orientation="vertical" size={24} style={{ width: '100%' }}>
            <Space orientation="vertical" size={12} style={{ width: '100%' }}>
              <Space size={12}>
                <Shield style={{ ...SECTION_ICON_STYLE, color: iconColor }} />
                <Title level={2} style={SECTION_TITLE_STYLE}>1. Penerimaan Ketentuan</Title>
              </Space>
              <Paragraph type="secondary" style={{ margin: 0 }}>
                Dengan mengakses dan menggunakan layanan PayU, Anda setuju untuk terikat oleh syarat dan ketentuan ini.
                Jika Anda tidak setuju dengan bagian manapun dari ketentuan ini, Anda tidak boleh menggunakan layanan kami.
              </Paragraph>
            </Space>

            <Space orientation="vertical" size={12} style={{ width: '100%' }}>
              <Space size={12}>
                <AlertCircle style={{ ...SECTION_ICON_STYLE, color: iconColor }} />
                <Title level={2} style={SECTION_TITLE_STYLE}>2. Deskripsi Layanan</Title>
              </Space>
              <Paragraph type="secondary" style={{ margin: 0 }}>
                PayU menyediakan platform perbankan digital yang mencakup layanan manajemen rekening, transfer dana,
                pembayaran tagihan, dan layanan keuangan lainnya. Layanan ini disediakan sebagaimana adanya tanpa
                jaminan apapun.
              </Paragraph>
            </Space>

            <Space orientation="vertical" size={12} style={{ width: '100%' }}>
              <Space size={12}>
                <CheckCircle2 style={{ ...SECTION_ICON_STYLE, color: iconColor }} />
                <Title level={2} style={SECTION_TITLE_STYLE}>3. Tanggung Jawab Pengguna</Title>
              </Space>
              <Paragraph type="secondary" style={{ margin: 0 }}>
                Pengguna bertanggung jawab untuk menjaga kerahasiaan kredensial akun dan semua aktivitas yang terjadi
                di bawah akun mereka. Pengguna juga setuju untuk memberikan informasi yang akurat dan terkini.
              </Paragraph>
            </Space>

            <Space orientation="vertical" size={12} style={{ width: '100%' }}>
              <Space size={12}>
                <Shield style={{ ...SECTION_ICON_STYLE, color: iconColor }} />
                <Title level={2} style={SECTION_TITLE_STYLE}>4. Privasi dan Keamanan</Title>
              </Space>
              <Paragraph type="secondary" style={{ margin: 0 }}>
                Kami berkomitmen untuk melindungi privasi dan keamanan data pengguna sesuai dengan Kebijakan Privasi
                kami. Harap tinjau Kebijakan Privasi kami untuk informasi lebih lanjut.
              </Paragraph>
            </Space>

            <Space orientation="vertical" size={12} style={{ width: '100%' }}>
              <Space size={12}>
                <AlertCircle style={{ ...SECTION_ICON_STYLE, color: iconColor }} />
                <Title level={2} style={SECTION_TITLE_STYLE}>5. Batasan Tanggung Jawab</Title>
              </Space>
              <Paragraph type="secondary" style={{ margin: 0 }}>
                PayU tidak bertanggung jawab atas kerugian langsung, tidak langsung, insidental, atau konsekuensial
                yang timbul dari penggunaan atau ketidakmampuan menggunakan layanan kami.
              </Paragraph>
            </Space>

            <Space orientation="vertical" size={12} style={{ width: '100%' }}>
              <Space size={12}>
                <CheckCircle2 style={{ ...SECTION_ICON_STYLE, color: iconColor }} />
                <Title level={2} style={SECTION_TITLE_STYLE}>6. Perubahan Ketentuan</Title>
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
