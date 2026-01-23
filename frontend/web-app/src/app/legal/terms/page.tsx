'use client';

import React from 'react';
import DashboardLayout from "@/components/DashboardLayout";
import { FileText, Shield, AlertCircle, CheckCircle2 } from 'lucide-react';
import { PageTransition, StaggerContainer, StaggerItem } from '@/components/ui/Motion';

export default function TermsPage() {
  return (
    <DashboardLayout>
      <PageTransition>
        <div className="space-y-12">
          <StaggerContainer>
            <StaggerItem>
              <div className="flex flex-col gap-4 mb-8">
                <div className="flex items-center gap-3">
                  <div className="h-14 w-14 bg-primary rounded-2xl flex items-center justify-center shadow-lg shadow-primary/20">
                    <FileText className="h-7 w-7 text-white" />
                  </div>
                  <div>
                    <h2 className="text-3xl font-black text-foreground">Syarat dan Ketentuan</h2>
                    <p className="text-sm text-muted-foreground font-medium">Versi 1.0 - Terakhir diperbarui: Januari 2026</p>
                  </div>
                </div>
              </div>
            </StaggerItem>

            <StaggerItem>
              <div className="bg-card rounded-2xl p-8 sm:p-12 border border-border shadow-card space-y-8">
                <div className="prose prose-sm max-w-none">
                  <section className="space-y-4">
                    <h3 className="text-xl font-black text-foreground flex items-center gap-3">
                      <Shield className="h-6 w-6 text-primary" />
                      1. Penerimaan Ketentuan
                    </h3>
                    <p className="text-sm text-muted-foreground font-medium leading-relaxed">
                      Dengan mengakses dan menggunakan layanan PayU, Anda setuju untuk terikat oleh syarat dan ketentuan ini. 
                      Jika Anda tidak setuju dengan bagian manapun dari ketentuan ini, Anda tidak boleh menggunakan layanan kami.
                    </p>
                  </section>

                  <section className="space-y-4">
                    <h3 className="text-xl font-black text-foreground flex items-center gap-3">
                      <AlertCircle className="h-6 w-6 text-primary" />
                      2. Deskripsi Layanan
                    </h3>
                    <p className="text-sm text-muted-foreground font-medium leading-relaxed">
                      PayU menyediakan platform perbankan digital yang mencakup layanan manajemen rekening, transfer dana,
                      pembayaran tagihan, dan layanan keuangan lainnya. Layanan ini disediakan &ldquo;sebagaimana adanya&rdquo; tanpa
                      jaminan apapun.
                    </p>
                  </section>

                  <section className="space-y-4">
                    <h3 className="text-xl font-black text-foreground flex items-center gap-3">
                      <CheckCircle2 className="h-6 w-6 text-primary" />
                      3. Tanggung Jawab Pengguna
                    </h3>
                    <p className="text-sm text-muted-foreground font-medium leading-relaxed">
                      Pengguna bertanggung jawab untuk menjaga kerahasiaan kredensial akun dan semua aktivitas yang terjadi 
                      di bawah akun mereka. Pengguna juga setuju untuk memberikan informasi yang akurat dan terkini.
                    </p>
                  </section>

                  <section className="space-y-4">
                    <h3 className="text-xl font-black text-foreground flex items-center gap-3">
                      <Shield className="h-6 w-6 text-primary" />
                      4. Privasi dan Keamanan
                    </h3>
                    <p className="text-sm text-muted-foreground font-medium leading-relaxed">
                      Kami berkomitmen untuk melindungi privasi dan keamanan data pengguna sesuai dengan Kebijakan Privasi 
                      kami. Harap tinjau Kebijakan Privasi kami untuk informasi lebih lanjut.
                    </p>
                  </section>

                  <section className="space-y-4">
                    <h3 className="text-xl font-black text-foreground flex items-center gap-3">
                      <AlertCircle className="h-6 w-6 text-primary" />
                      5. Batasan Tanggung Jawab
                    </h3>
                    <p className="text-sm text-muted-foreground font-medium leading-relaxed">
                      PayU tidak bertanggung jawab atas kerugian langsung, tidak langsung, insidental, atau konsekuensial 
                      yang timbul dari penggunaan atau ketidakmampuan menggunakan layanan kami.
                    </p>
                  </section>

                  <section className="space-y-4">
                    <h3 className="text-xl font-black text-foreground flex items-center gap-3">
                      <CheckCircle2 className="h-6 w-6 text-primary" />
                      6. Perubahan Ketentuan
                    </h3>
                    <p className="text-sm text-muted-foreground font-medium leading-relaxed">
                      Kami berhak mengubah syarat dan ketentuan ini kapan saja dengan memberikan pemberitahuan kepada 
                      pengguna melalui aplikasi atau email. Penggunaan lanjutan layanan setelah perubahan dianggap 
                      sebagai penerimaan ketentuan yang diperbarui.
                    </p>
                  </section>
                </div>

                <div className="pt-8 border-t border-border">
                  <p className="text-xs text-muted-foreground font-medium text-center">
                    Untuk pertanyaan lebih lanjut, hubungi tim dukungan kami di support@payu.id
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
