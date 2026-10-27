'use client';

import React from 'react';
import DashboardLayout from "@/components/DashboardLayout";
import { Card, Space, Typography, theme } from 'antd';
import { Shield, Eye, Database, Lock, RefreshCw, CheckCircle2 } from '@/components/icons';
import { SkipLink } from "@/lib/a11y";
import { ON_PRIMARY } from '@/theme/tokens';

const { Title, Text, Paragraph } = Typography;

const SECTION_ICON_STYLE = { width: 24, height: 24 } as const;
const SECTION_TITLE_STYLE = { margin: 0, fontWeight: 600 } as const;

export default function PrivacyPage() {
  const { token } = theme.useToken();
  const iconColor = token.colorPrimaryText;
  return (
    <DashboardLayout>
      <SkipLink />
      <Space orientation="vertical" size={24} style={{ width: '100%' }}>
        <Space size={16}>
          <div style={{ width: 56, height: 56, background: token.colorPrimary, borderRadius: 24, display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
            <Shield style={{ width: 28, height: 28, color: ON_PRIMARY }} />
          </div>
          <div>
            <Title level={1} style={{ margin: 0, fontWeight: 600 }}>Kebijakan Privasi</Title>
            <Text type="secondary">Versi 1.0 - Terakhir diperbarui: Januari 2026</Text>
          </div>
        </Space>

        <Card style={{ borderRadius: 24 }}>
          <Space orientation="vertical" size={24} style={{ width: '100%' }}>
            <Space orientation="vertical" size={12} style={{ width: '100%' }}>
              <Space size={12}>
                <Eye style={{ ...SECTION_ICON_STYLE, color: iconColor }} />
                <Title level={2} style={SECTION_TITLE_STYLE}>1. Pengumpulan Informasi</Title>
              </Space>
              <Paragraph type="secondary" style={{ margin: 0 }}>
                Kami mengumpulkan informasi yang Anda berikan secara langsung, termasuk data pribadi, informasi keuangan,
                dan data identitas. Kami juga mengumpulkan informasi secara otomatis melalui penggunaan layanan kami.
              </Paragraph>
            </Space>

            <Space orientation="vertical" size={12} style={{ width: '100%' }}>
              <Space size={12}>
                <Database style={{ ...SECTION_ICON_STYLE, color: iconColor }} />
                <Title level={2} style={SECTION_TITLE_STYLE}>2. Penggunaan Informasi</Title>
              </Space>
              <Paragraph type="secondary" style={{ margin: 0 }}>
                Informasi yang dikumpulkan digunakan untuk menyediakan, meningkatkan, dan mengamankan layanan kami.
                Kami menggunakan data untuk verifikasi identitas, analisis risiko, dan personalisasi pengalaman pengguna.
              </Paragraph>
            </Space>

            <Space orientation="vertical" size={12} style={{ width: '100%' }}>
              <Space size={12}>
                <Lock style={{ ...SECTION_ICON_STYLE, color: iconColor }} />
                <Title level={2} style={SECTION_TITLE_STYLE}>3. Keamanan Data</Title>
              </Space>
              <Paragraph type="secondary" style={{ margin: 0 }}>
                Kami menerapkan standar keamanan industri yang ketat untuk melindungi data Anda. Semua data dienkripsi
                menggunakan protokol SSL/TLS dan disimpan dalam database yang aman dengan kontrol akses berlapis.
              </Paragraph>
            </Space>

            <Space orientation="vertical" size={12} style={{ width: '100%' }}>
              <Space size={12}>
                <Shield style={{ ...SECTION_ICON_STYLE, color: iconColor }} />
                <Title level={2} style={SECTION_TITLE_STYLE}>4. Berbagi Informasi</Title>
              </Space>
              <Paragraph type="secondary" style={{ margin: 0 }}>
                Kami tidak menjual data Anda kepada pihak ketiga. Informasi hanya dibagikan dengan pihak ketiga yang
                tepercaya yang membantu kami menyediakan layanan, atau ketika diwajibkan oleh hukum.
              </Paragraph>
            </Space>

            <Space orientation="vertical" size={12} style={{ width: '100%' }}>
              <Space size={12}>
                <RefreshCw style={{ ...SECTION_ICON_STYLE, color: iconColor }} />
                <Title level={2} style={SECTION_TITLE_STYLE}>5. Hak Pengguna</Title>
              </Space>
              <Paragraph type="secondary" style={{ margin: 0 }}>
                Anda berhak mengakses, memperbaiki, menghapus, atau membatasi pemrosesan data pribadi Anda.
                Anda juga berhak menolak pemrosesan tertentu dan menarik persetujuan yang telah diberikan.
              </Paragraph>
            </Space>

            <Space orientation="vertical" size={12} style={{ width: '100%' }}>
              <Space size={12}>
                <CheckCircle2 style={{ ...SECTION_ICON_STYLE, color: iconColor }} />
                <Title level={2} style={SECTION_TITLE_STYLE}>6. Kepatuhan Regulasi</Title>
              </Space>
              <Paragraph type="secondary" style={{ margin: 0 }}>
                Kebijakan privasi ini dirancang untuk mematuhi regulasi perlindungan data yang berlaku, termasuk
                UU Perlindungan Data Pribadi dan standar internasional lainnya.
              </Paragraph>
            </Space>

            <div style={{ paddingTop: 32, borderTop: `1px solid ${token.colorBorderSecondary}` }}>
              <Text type="secondary" style={{ textAlign: 'center', display: 'block' }}>
                Untuk pertanyaan atau permintaan terkait privasi, hubungi privacy@payu.fajjjar.my.id
              </Text>
            </div>
          </Space>
        </Card>
      </Space>
    </DashboardLayout>
  );
}
