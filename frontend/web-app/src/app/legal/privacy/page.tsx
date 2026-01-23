'use client';

import React from 'react';
import DashboardLayout from "@/components/DashboardLayout";
import { Shield, Eye, Database, Lock, RefreshCw, CheckCircle2 } from 'lucide-react';
import { PageTransition, StaggerContainer, StaggerItem } from '@/components/ui/Motion';

export default function PrivacyPage() {
  return (
    <DashboardLayout>
      <PageTransition>
        <div className="space-y-12">
          <StaggerContainer>
            <StaggerItem>
              <div className="flex flex-col gap-4 mb-8">
                <div className="flex items-center gap-3">
                  <div className="h-14 w-14 bg-primary rounded-2xl flex items-center justify-center shadow-lg shadow-primary/20">
                    <Shield className="h-7 w-7 text-white" />
                  </div>
                  <div>
                    <h2 className="text-3xl font-black text-foreground">Kebijakan Privasi</h2>
                    <p className="text-sm text-muted-foreground font-medium">Versi 1.0 - Terakhir diperbarui: Januari 2026</p>
                  </div>
                </div>
              </div>
            </StaggerItem>

            <StaggerItem>
              <div className="bg-gradient-to-br from-primary/5 to-bank-emerald/5 rounded-2xl p-8 sm:p-12 border border-border shadow-card space-y-8 relative overflow-hidden">
                <div className="absolute top-0 right-0 w-64 h-64 bg-primary/10 rounded-full blur-3xl" />
                
                <div className="prose prose-sm max-w-none relative z-10">
                  <section className="space-y-4">
                    <h3 className="text-xl font-black text-foreground flex items-center gap-3">
                      <Eye className="h-6 w-6 text-primary" />
                      1. Pengumpulan Informasi
                    </h3>
                    <p className="text-sm text-muted-foreground font-medium leading-relaxed">
                      Kami mengumpulkan informasi yang Anda berikan secara langsung, termasuk data pribadi, informasi keuangan, 
                      dan data identitas. Kami juga mengumpulkan informasi secara otomatis melalui penggunaan layanan kami.
                    </p>
                  </section>

                  <section className="space-y-4">
                    <h3 className="text-xl font-black text-foreground flex items-center gap-3">
                      <Database className="h-6 w-6 text-primary" />
                      2. Penggunaan Informasi
                    </h3>
                    <p className="text-sm text-muted-foreground font-medium leading-relaxed">
                      Informasi yang dikumpulkan digunakan untuk menyediakan, meningkatkan, dan mengamankan layanan kami. 
                      Kami menggunakan data untuk verifikasi identitas, analisis risiko, dan personalisasi pengalaman pengguna.
                    </p>
                  </section>

                  <section className="space-y-4">
                    <h3 className="text-xl font-black text-foreground flex items-center gap-3">
                      <Lock className="h-6 w-6 text-primary" />
                      3. Keamanan Data
                    </h3>
                    <p className="text-sm text-muted-foreground font-medium leading-relaxed">
                      Kami menerapkan standar keamanan industri yang ketat untuk melindungi data Anda. Semua data dienkripsi 
                      menggunakan protokol SSL/TLS dan disimpan dalam database yang aman dengan kontrol akses berlapis.
                    </p>
                  </section>

                  <section className="space-y-4">
                    <h3 className="text-xl font-black text-foreground flex items-center gap-3">
                      <Shield className="h-6 w-6 text-primary" />
                      4. Berbagi Informasi
                    </h3>
                    <p className="text-sm text-muted-foreground font-medium leading-relaxed">
                      Kami tidak menjual data Anda kepada pihak ketiga. Informasi hanya dibagikan dengan pihak ketiga yang 
                      tepercaya yang membantu kami menyediakan layanan, atau ketika diwajibkan oleh hukum.
                    </p>
                  </section>

                  <section className="space-y-4">
                    <h3 className="text-xl font-black text-foreground flex items-center gap-3">
                      <RefreshCw className="h-6 w-6 text-primary" />
                      5. Hak Pengguna
                    </h3>
                    <p className="text-sm text-muted-foreground font-medium leading-relaxed">
                      Anda berhak mengakses, memperbaiki, menghapus, atau membatasi pemrosesan data pribadi Anda. 
                      Anda juga berhak menolak pemrosesan tertentu dan menarik persetujuan yang telah diberikan.
                    </p>
                  </section>

                  <section className="space-y-4">
                    <h3 className="text-xl font-black text-foreground flex items-center gap-3">
                      <CheckCircle2 className="h-6 w-6 text-primary" />
                      6. Kepatuhan Regulasi
                    </h3>
                    <p className="text-sm text-muted-foreground font-medium leading-relaxed">
                      Kebijakan privasi ini dirancang untuk mematuhi regulasi perlindungan data yang berlaku, termasuk 
                      UU Perlindungan Data Pribadi dan standar internasional lainnya.
                    </p>
                  </section>
                </div>

                <div className="pt-8 border-t border-border relative z-10">
                  <p className="text-xs text-muted-foreground font-medium text-center">
                    Untuk pertanyaan atau permintaan terkait privasi, hubungi privacy@payu.id
                  </p>
                </div>
              </div>
            </StaggerItem>
          </StaggerContainer>
        </div>
      </PageTransition>
    </DashboardLayout>
  );
}
