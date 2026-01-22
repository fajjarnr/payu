'use client';

import { useState } from 'react';
import { useRouter } from 'next/navigation';
import { useMutation } from '@tanstack/react-query';
import { Building2, Mail, Phone, User, CreditCard, ArrowRight, ShieldCheck, CheckCircle2, FileText } from 'lucide-react';
import { PartnerService } from '@/services/PartnerService';
import { z } from 'zod';
import clsx from 'clsx';

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
      alert('Registrasi merchant berhasil! Silakan tunggu verifikasi.');
      router.push('/merchant');
    },
    onError: (error: Error) => {
      alert('Registrasi gagal. Silakan coba lagi.');
    }
  });

  const validateForm = (): boolean => {
    const result = merchantSchema.safeParse(formData);
    if (!result.success) {
      const newErrors: Partial<Record<keyof MerchantFormData, string>> = {};
      result.success === false && result.error.issues.forEach((err) => {
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
    <div className="min-h-screen bg-gray-50 dark:bg-gray-950 py-12 px-4">
      <div className="max-w-4xl mx-auto space-y-10">
        <div className="text-center space-y-4">
          <div className="inline-flex items-center justify-center h-20 w-20 bg-bank-green/10 rounded-3xl mb-4">
            <Building2 className="h-10 w-10 text-bank-green" />
          </div>
          <h1 className="text-4xl font-black text-foreground uppercase tracking-tight italic">Daftar Merchant Baru</h1>
          <p className="text-sm text-gray-500 font-medium max-w-xl mx-auto">
            Bergabunglah dengan ekosistem pembayaran PayU dan terima pembayaran instan dari jutaan pengguna.
          </p>
        </div>

        <form onSubmit={handleSubmit} className="space-y-8">
          <div className="bg-card rounded-[3rem] p-10 border border-border shadow-sm relative overflow-hidden">
            <div className="absolute top-0 right-0 w-64 h-64 bg-bank-green/5 rounded-full blur-3xl -z-0" />

            <div className="relative z-10 space-y-8">
              <div>
                <label className="text-[10px] font-black text-gray-400 uppercase tracking-widest ml-1 block mb-3">
                  Nama Merchant
                </label>
                <div className="relative">
                  <User className="absolute left-6 top-1/2 -translate-y-1/2 h-5 w-5 text-gray-400" />
                  <input
                    type="text"
                    value={formData.name}
                    onChange={(e) => handleChange('name', e.target.value)}
                    placeholder="Masukkan nama bisnis Anda"
                    className={clsx(
                      "w-full pl-16 pr-6 py-6 rounded-2xl border-border bg-gray-50 dark:bg-gray-900/50 text-lg font-black text-foreground placeholder:text-gray-300 focus:ring-4 focus:ring-bank-green/10 focus:border-bank-green transition-all outline-none uppercase tracking-tight",
                      errors.name && "border-red-500 focus:ring-red-500/10 focus:border-red-500"
                    )}
                  />
                </div>
                {errors.name && <p className="text-red-500 text-[10px] mt-2 ml-4 font-black uppercase tracking-widest">{errors.name}</p>}
              </div>

              <div className="grid grid-cols-1 md:grid-cols-2 gap-8">
                <div>
                  <label className="text-[10px] font-black text-gray-400 uppercase tracking-widest ml-1 block mb-3">
                    Email Bisnis
                  </label>
                  <div className="relative">
                    <Mail className="absolute left-6 top-1/2 -translate-y-1/2 h-5 w-5 text-gray-400" />
                    <input
                      type="email"
                      value={formData.email}
                      onChange={(e) => handleChange('email', e.target.value)}
                      placeholder="email@perusahaan.com"
                      className={clsx(
                        "w-full pl-16 pr-6 py-6 rounded-2xl border-border bg-gray-50 dark:bg-gray-900/50 text-lg font-black text-foreground placeholder:text-gray-300 focus:ring-4 focus:ring-bank-green/10 focus:border-bank-green transition-all outline-none lowercase",
                        errors.email && "border-red-500 focus:ring-red-500/10 focus:border-red-500"
                      )}
                    />
                  </div>
                  {errors.email && <p className="text-red-500 text-[10px] mt-2 ml-4 font-black uppercase tracking-widest">{errors.email}</p>}
                </div>

                <div>
                  <label className="text-[10px] font-black text-gray-400 uppercase tracking-widest ml-1 block mb-3">
                    Nomor Telepon
                  </label>
                  <div className="relative">
                    <Phone className="absolute left-6 top-1/2 -translate-y-1/2 h-5 w-5 text-gray-400" />
                    <input
                      type="tel"
                      value={formData.phone}
                      onChange={(e) => handleChange('phone', e.target.value)}
                      placeholder="+62 812-3456-7890"
                      className={clsx(
                        "w-full pl-16 pr-6 py-6 rounded-2xl border-border bg-gray-50 dark:bg-gray-900/50 text-lg font-black text-foreground placeholder:text-gray-300 focus:ring-4 focus:ring-bank-green/10 focus:border-bank-green transition-all outline-none uppercase tracking-tight",
                        errors.phone && "border-red-500 focus:ring-red-500/10 focus:border-red-500"
                      )}
                    />
                  </div>
                  {errors.phone && <p className="text-red-500 text-[10px] mt-2 ml-4 font-black uppercase tracking-widest">{errors.phone}</p>}
                </div>
              </div>
            </div>
          </div>

          <div className="bg-card rounded-[3rem] p-10 border border-border shadow-sm relative overflow-hidden">
            <div className="absolute top-0 right-0 w-64 h-64 bg-bank-green/5 rounded-full blur-3xl -z-0" />
            <h3 className="text-xl font-black text-foreground uppercase tracking-tight mb-10 italic relative z-10">Tipe Merchant</h3>
            <div className="grid grid-cols-1 md:grid-cols-2 gap-6 relative z-10">
              {merchantTypes.map((type) => (
                <button
                  key={type.value}
                  type="button"
                  onClick={() => handleChange('type', type.value)}
                  className={clsx(
                    "flex flex-col items-start gap-4 p-6 rounded-2xl border-2 transition-all text-left group",
                    formData.type === type.value
                      ? "bg-bank-green/5 border-bank-green"
                      : "bg-gray-50 dark:bg-gray-900/50 border-border hover:border-bank-green/50 hover:bg-gray-100 dark:hover:bg-gray-800"
                  )}
                >
                  <div className="flex items-center gap-4">
                    <div className={clsx(
                      "h-10 w-10 rounded-xl flex items-center justify-center transition-all",
                      formData.type === type.value ? "bg-bank-green" : "bg-gray-200 dark:bg-gray-700 group-hover:bg-bank-green"
                    )}>
                      <Building2 className={clsx("h-5 w-5", formData.type === type.value ? "text-white" : "text-gray-400 group-hover:text-white")} />
                    </div>
                    <span className="font-black text-foreground uppercase tracking-tight text-sm">{type.label}</span>
                  </div>
                  <p className="text-[10px] font-bold text-gray-400 uppercase tracking-widest leading-relaxed">{type.description}</p>
                  {formData.type === type.value && (
                    <CheckCircle2 className="h-5 w-5 text-bank-green self-end" />
                  )}
                </button>
              ))}
            </div>
            {errors.type && <p className="text-red-500 text-[10px] mt-4 font-black uppercase tracking-widest">{errors.type}</p>}
          </div>

          <div className="bg-card rounded-[3rem] p-10 border border-border shadow-sm relative overflow-hidden">
            <div className="absolute top-0 right-0 w-64 h-64 bg-bank-green/5 rounded-full blur-3xl -z-0" />
            <div className="relative z-10 space-y-8">
              <div>
                <label className="text-[10px] font-black text-gray-400 uppercase tracking-widest ml-1 block mb-3">
                  Public Key (Opsional)
                </label>
                <div className="relative">
                  <FileText className="absolute left-6 top-6 h-5 w-5 text-gray-400" />
                  <textarea
                    value={formData.publicKey}
                    onChange={(e) => handleChange('publicKey', e.target.value)}
                    placeholder="-----BEGIN PUBLIC KEY-----"
                    rows={4}
                    className="w-full pl-16 pr-6 py-6 rounded-2xl border-border bg-gray-50 dark:bg-gray-900/50 text-sm font-mono text-foreground placeholder:text-gray-300 focus:ring-4 focus:ring-bank-green/10 focus:border-bank-green transition-all outline-none resize-none"
                  />
                </div>
                <p className="text-[10px] text-gray-400 font-bold uppercase tracking-widest mt-2 ml-4">
                  Diperlukan untuk integrasi API custom
                </p>
              </div>
            </div>
          </div>

          <div className="bg-foreground text-background rounded-[3rem] p-12 relative overflow-hidden shadow-2xl">
            <div className="absolute top-0 right-0 w-80 h-80 bg-white/5 rounded-full blur-3xl -z-0" />
            <div className="relative z-10 flex flex-col md:flex-row items-center justify-between gap-8">
              <div className="space-y-4 max-w-xl text-center md:text-left">
                <div className="flex items-center gap-3">
                  <div className="h-10 w-10 bg-bank-green rounded-2xl flex items-center justify-center shadow-lg shadow-bank-green/20">
                    <ShieldCheck className="h-5 w-5 text-white" />
                  </div>
                  <h3 className="text-2xl font-black italic tracking-tighter">Siap untuk Mulai?</h3>
                </div>
                <p className="text-sm text-gray-400 font-medium leading-relaxed uppercase tracking-wide">
                  Dengan mendaftar, Anda menyetujui <span className="text-bank-green font-black">Syarat & Ketentuan</span> serta <span className="text-bank-green font-black">Kebijakan Privasi</span> PayU.
                </p>
              </div>
              <button
                type="submit"
                disabled={registerMutation.isPending}
                className="whitespace-nowrap bg-bank-green text-white px-12 py-6 rounded-3xl font-black uppercase text-xs tracking-widest hover:bg-bank-emerald transition-all active:scale-95 shadow-2xl shadow-bank-green/20 disabled:bg-bank-green/50 disabled:active:scale-100 flex items-center gap-3"
              >
                {registerMutation.isPending ? 'Sedang Memproses...' : (
                  <>
                    Daftar Sekarang
                    <ArrowRight className="h-4 w-4" />
                  </>
                )}
              </button>
            </div>
            <CreditCard className="absolute bottom-[-30px] right-[-30px] h-48 w-48 text-white/5 -rotate-12" />
          </div>
        </form>

        <div className="text-center">
          <button
            onClick={() => router.push('/merchant')}
            className="text-[10px] font-black text-gray-400 uppercase tracking-widest hover:text-foreground transition-colors"
          >
            Kembali ke Dashboard Merchant
          </button>
        </div>
      </div>
    </div>
  );
}
