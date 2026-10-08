'use client';

import React from 'react';
import DashboardLayout from "@/components/DashboardLayout";
import { Card, Space, Typography } from 'antd';
import { Shield, Eye, Database, Lock, RefreshCw, CheckCircle2 } from '@/components/icons';

const { Title, Text, Paragraph } = Typography;

export default function PrivacyPage() {
  return (
    <DashboardLayout>
      <Space direction="vertical" size={24} style={{ width: '100%' }}>
        <Space size={16}>
          <div style={{ width: 56, height: 56, backgroundColor: 'var(--ant-color-primary)', borderRadius: 16, display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
            <Shield style={{ width: 28, height: 28, color: 'var(--ant-color-text-light-solid)' }} />
          </div>
          <div>
            <Title level={2} style={{ margin: 0 }}>Kebijakan Privasi</Title>
            <Text type="secondary">Versi 1.0 - Terakhir diperbarui: Januari 2026</Text>
          </div>
        </Space>

        <Card>
          <Space direction="vertical" size={24} style={{ width: '100%' }}>
            <Space direction="vertical" size={12} style={{ width: '100%' }}>
              <Space size={12}>
                <Eye style={{ width: 24, height: 24, color: 'var(--ant-color-primary)' }} />
                <Title level={3} style={{ margin: 0 }}>1. Pengumpulan Informasi</Title>
              </Space>
              <Paragraph type="secondary" style={{ margin: 0 }}>
                Kami mengumpulkan informasi yang Anda berikan secara langsung, termasuk data pribadi, informasi keuangan,
                dan data identitas. Kami juga mengumpulkan informasi secara otomatis melalui penggunaan layanan kami.
              </Paragraph>
            </Space>

            <Space direction="vertical" size={12} style={{ width: '100%' }}>
              <Space size={12}>
                <Database style={{ width: 24, height: 24, color: 'var(--ant-color-primary)' }} />
                <Title level={3} style={{ margin: 0 }}>2. Penggunaan Informasi</Title>
              </Space>
              <Paragraph type="secondary" style={{ margin: 0 }}>
                Informasi yang dikumpulkan digunakan untuk menyediakan, meningkatkan, dan mengamankan layanan kami.
                Kami menggunakan data untuk verifikasi identitas, analisis risiko, dan personalisasi pengalaman pengguna.
              </Paragraph>
            </Space>

            <Space direction="vertical" size={12} style={{ width: '100%' }}>
              <Space size={12}>
                <Lock style={{ width: 24, height: 24, color: 'var(--ant-color-primary)' }} />
                <Title level={3} style={{ margin: 0 }}>3. Keamanan Data</Title>
              </Space>
              <Paragraph type="secondary" style={{ margin: 0 }}>
                Kami menerapkan standar keamanan industri yang ketat untuk melindungi data Anda. Semua data dienkripsi
                menggunakan protokol SSL/TLS dan disimpan dalam database yang aman dengan kontrol akses berlapis.
              </Paragraph>
            </Space>

            <Space direction="vertical" size={12} style={{ width: '100%' }}>
              <Space size={12}>
                <Shield style={{ width: 24, height: 24, color: 'var(--ant-color-primary)' }} />
                <Title level={3} style={{ margin: 0 }}>4. Berbagi Informasi</Title>
              </Space>
              <Paragraph type="secondary" style={{ margin: 0 }}>
                Kami tidak menjual data Anda kepada pihak ketiga. Informasi hanya dibagikan dengan pihak ketiga yang
                tepercaya yang membantu kami menyediakan layanan, atau ketika diwajibkan oleh hukum.
              </Paragraph>
            </Space>

            <Space direction="vertical" size={12} style={{ width: '100%' }}>
              <Space size={12}>
                <RefreshCw style={{ width: 24, height: 24, color: 'var(--ant-color-primary)' }} />
                <Title level={3} style={{ margin: 0 }}>5. Hak Pengguna</Title>
              </Space>
              <Paragraph type="secondary" style={{ margin: 0 }}>
                Anda berhak mengakses, memperbaiki, menghapus, atau membatasi pemrosesan data pribadi Anda.
                Anda juga berhak menolak pemrosesan tertentu dan menarik persetujuan yang telah diberikan.
              </Paragraph>
            </Space>

            <Space direction="vertical" size={12} style={{ width: '100%' }}>
              <Space size={12}>
                <CheckCircle2 style={{ width: 24, height: 24, color: 'var(--ant-color-primary)' }} />
                <Title level={3} style={{ margin: 0 }}>6. Kepatuhan Regulasi</Title>
              </Space>
              <Paragraph type="secondary" style={{ margin: 0 }}>
                Kebijakan privasi ini dirancang untuk mematuhi regulasi perlindungan data yang berlaku, termasuk
                UU Perlindungan Data Pribadi dan standar internasional lainnya.
              </Paragraph>
            </Space>

            <div style={{ paddingTop: 32, borderTop: '1px solid var(--ant-color-border)' }}>
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
